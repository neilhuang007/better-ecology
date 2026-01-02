package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.steering.BehaviorContext;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.predation.PreySelector;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.EnergyHandle;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Pack hunting behavior for wolves.
 * Coordinates hunting with pack members, using flanking strategies and
 * alpha-led pursuit. Wolves communicate through howling and position themselves
 * to surround prey before attacking.
 * <p>
 * Based on research into wolf pack hunting strategies:
 * - Alpha leads the hunt
 * - Pack members flank to surround prey
 * - Howling coordinates pack position
 * - Pack size affects hunting success rate
 */
public class PackHuntingBehavior extends SteeringBehavior {

    private final double maxPursuitSpeed;
    private final double maxPursuitForce;
    private final double flankingAngle;
    private final double coordinationRange;
    private final int minPackSize;
    private final PreySelector preySelector;

    // Sustainability parameters
    private static final int ENERGY_COST_PER_HUNT = 30; // 30% of energy bar
    private static final int HANDLING_TIME_TICKS = 600; // 30 seconds after kill
    private static final int HUNGER_TRIGGER_THRESHOLD = 80; // Hunt when hunger < 80%
    private static final int SATIATION_THRESHOLD = 80; // Stop when hunger > 80%

    private Entity currentPrey;
    private PackHuntingState huntingState = PackHuntingState.IDLE;
    private UUID packId;
    private int huntTimer = 0;
    private int handlingTimer = 0; // Time since last successful kill
    private boolean hasKilledRecently = false;

    // Cache for pack sorting optimization
    private List<Wolf> cachedPackOrder;
    private long lastCacheTime = 0;
    private static final long CACHE_TTL_TICKS = 100; // 5 seconds (20 ticks per second)

    public PackHuntingBehavior(double maxPursuitSpeed, double maxPursuitForce,
                               double flankingAngle, double coordinationRange, int minPackSize,
                               PreySelector preySelector) {
        this.maxPursuitSpeed = maxPursuitSpeed;
        this.maxPursuitForce = maxPursuitForce;
        this.flankingAngle = flankingAngle;
        this.coordinationRange = coordinationRange;
        this.minPackSize = minPackSize;
        this.preySelector = preySelector != null ? preySelector : new PreySelector();
    }

    public PackHuntingBehavior(double maxPursuitSpeed, double maxPursuitForce,
                               double flankingAngle, double coordinationRange, int minPackSize) {
        this(maxPursuitSpeed, maxPursuitForce, flankingAngle, coordinationRange, minPackSize,
             new PreySelector());
    }

    public PackHuntingBehavior() {
        this(1.3, 0.18, Math.PI / 3, 48.0, 2, new PreySelector());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!(context.getSelf() instanceof Wolf wolf)) {
            return new Vec3d();
        }

        // Skip if tamed
        if (wolf.isTame()) {
            return new Vec3d();
        }

        // Get pack ID from NBT or create new pack
        if (packId == null) {
            packId = getPackId(wolf);
        }

        // Update handling timer
        updateHandlingTimer(wolf);

        // Check energy and hunger before hunting
        if (!hasEnergyForHunt(wolf)) {
            huntingState = PackHuntingState.RESTING;
            return new Vec3d();
        }

        if (!isHungryEnoughToHunt(wolf)) {
            huntingState = PackHuntingState.IDLE;
            return new Vec3d();
        }

        // Update hunting state
        updateHuntState(wolf);

        // Find or validate prey
        Entity prey = findPrey(wolf, context);
        if (prey == null || !prey.isAlive()) {
            currentPrey = null;
            huntingState = PackHuntingState.IDLE;
            return new Vec3d();
        }

        currentPrey = prey;

        // Calculate hunting force based on pack position and role
        return calculateHuntingForce(wolf, context, prey);
    }

    /**
     * Calculates hunting force based on pack coordination.
     * Alpha wolves pursue directly, while pack members flank.
     */
    private Vec3d calculateHuntingForce(Wolf wolf, BehaviorContext context, Entity prey) {
        Vec3d wolfPos = context.getPosition();
        Vec3d preyPos = new Vec3d(prey.getX(), prey.getY(), prey.getZ());
        double distanceToPrey = wolfPos.distanceTo(preyPos);

        // Get pack members
        List<Wolf> packMembers = getPackMembers(wolf, context);

        // Determine if this wolf is alpha
        boolean isAlpha = isAlpha(wolf, packMembers);
        int packSize = packMembers.size();

        // Too few pack members - wait
        if (packSize < minPackSize && distanceToPrey < 24.0) {
            huntingState = PackHuntingState.WAITING;
            return new Vec3d(); // Wait for pack to assemble
        }

        // Close enough to attack
        if (distanceToPrey < 1.5) {
            huntingState = PackHuntingState.ATTACKING;
            return new Vec3d();
        }

        // Alpha pursues directly
        if (isAlpha) {
            huntingState = PackHuntingState.LEADING;
            return calculatePursuit(wolfPos, context.getVelocity(), preyPos, true);
        }

        // Pack members flank
        huntingState = PackHuntingState.FLANKING;
        return calculateFlanking(wolf, wolfPos, preyPos, packMembers, context);
    }

    /**
     * Calculates direct pursuit force with interception.
     */
    private Vec3d calculatePursuit(Vec3d wolfPos, Vec3d velocity, Vec3d preyPos, boolean isAlpha) {
        // Predict prey position
        Vec3d toPrey = Vec3d.sub(preyPos, wolfPos);
        double distance = toPrey.magnitude();

        // Simple interception
        toPrey.normalize();
        toPrey.mult(maxPursuitSpeed);

        Vec3d steer = Vec3d.sub(toPrey, velocity);
        steer.limit(maxPursuitForce);

        return steer;
    }

    /**
     * Calculates flanking maneuver to surround prey.
     * Pack members position themselves at angles around the prey.
     */
    private Vec3d calculateFlanking(Wolf self, Vec3d wolfPos, Vec3d preyPos,
                                   List<Wolf> packMembers, BehaviorContext context) {
        // Calculate desired flanking position
        Vec3d toPrey = Vec3d.sub(preyPos, wolfPos);
        double distance = toPrey.magnitude();

        if (distance < 0.001) {
            return new Vec3d();
        }

        // Find our position in the pack (based on distance to prey)
        int packIndex = getPackPosition(self, packMembers, preyPos);

        // Calculate flanking angle
        // Spread pack members evenly around prey
        double angleOffset = (packIndex % 2 == 0 ? 1 : -1) * flankingAngle * (1 + packIndex / 2);
        double desiredAngle = Math.atan2(toPrey.x, toPrey.z) + angleOffset;

        // Calculate desired position at flanking angle
        double flankingDistance = Math.max(8.0, distance * 0.7);
        double desiredX = preyPos.x + Math.sin(desiredAngle) * flankingDistance;
        double desiredZ = preyPos.z + Math.cos(desiredAngle) * flankingDistance;
        double desiredY = preyPos.y;

        Vec3d desiredPos = new Vec3d(desiredX, desiredY, desiredZ);

        // Seek flanking position
        Vec3d desiredVelocity = Vec3d.sub(desiredPos, wolfPos);
        desiredVelocity.normalize();
        desiredVelocity.mult(maxPursuitSpeed * 0.9);

        Vec3d steer = Vec3d.sub(desiredVelocity, context.getVelocity());
        steer.limit(maxPursuitForce);

        return steer;
    }

    /**
     * Find valid prey for the pack to hunt.
     * Uses PreySelector for optimal prey selection, then filters for wolf-specific prey.
     * Includes prey density checking for sustainability.
     */
    private Entity findPrey(Wolf wolf, BehaviorContext context) {
        // Keep current prey if valid
        if (currentPrey != null && currentPrey.isAlive()) {
            double distance = wolf.position().distanceTo(currentPrey.position());
            if (distance < coordinationRange) {
                return currentPrey;
            }
        }

        // Use PreySelector to find optimal prey
        Entity selectedPrey = preySelector.selectPrey(wolf);

        // Filter to wolf-specific prey with density checking
        if (selectedPrey != null && isValidPrey(wolf, selectedPrey)) {
            return selectedPrey;
        }

        return null;
    }

    /**
     * Checks if an entity is valid prey.
     * Includes prey density checking for sustainability.
     */
    private boolean isValidPrey(Wolf wolf, Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity.equals(wolf)) {
            return false;
        }

        // Don't hunt players or tamed animals
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        if (entity instanceof TamableAnimal tameable && tameable.isTame()) {
            return false;
        }

        // Don't hunt other wolves (unless from different pack)
        if (entity instanceof Wolf otherWolf) {
            UUID otherPackId = getPackId(otherWolf);
            return packId != null && !packId.equals(otherPackId);
        }

        // Check prey density before validating
        if (!preySelector.isPreyPopulationHealthy(wolf, (net.minecraft.world.entity.LivingEntity) entity)) {
            return false;
        }

        // Valid prey: sheep, rabbits, foxes, chickens
        return entity instanceof Sheep || entity instanceof Rabbit || entity instanceof Fox || entity instanceof Chicken;
    }

    /**
     * Gets all pack members within coordination range.
     * Uses SpatialIndex for efficient O(1) + O(k) queries instead of iterating all neighbors.
     */
    private List<Wolf> getPackMembers(Wolf wolf, BehaviorContext context) {
        List<Wolf> pack = new ArrayList<>();
        List<Mob> nearbyWolves = SpatialIndex.getNearbySameType(wolf, (int) coordinationRange);

        for (Mob mob : nearbyWolves) {
            if (!(mob instanceof Wolf otherWolf)) {
                continue;
            }

            if (otherWolf.isTame()) {
                continue;
            }

            UUID otherPackId = getPackId(otherWolf);
            if (packId != null && packId.equals(otherPackId)) {
                pack.add(otherWolf);
            }
        }

        return pack;
    }

    /**
     * Determines this wolf's position in the pack for flanking coordination.
     * Returns index based on distance to prey (closer = lower index).
     * Uses cached sorted order to avoid re-sorting every tick.
     */
    private int getPackPosition(Wolf self, List<Wolf> packMembers, Vec3d preyPos) {
        long currentTime = System.currentTimeMillis();

        // Check if cache is valid
        if (cachedPackOrder != null && !shouldInvalidateCache(packMembers, currentTime)) {
            // Cache hit - find our index in cached order
            return findIndexInCachedOrder(self);
        }

        // Cache miss or invalidated - rebuild cache
        invalidatePackCache();
        cachedPackOrder = new ArrayList<>(packMembers);
        cachedPackOrder.sort(Comparator.comparingDouble(w -> {
            Vec3d wolfPos = new Vec3d(w.getX(), w.getY(), w.getZ());
            return wolfPos.distanceTo(preyPos);
        }));
        lastCacheTime = currentTime;

        return findIndexInCachedOrder(self);
    }

    /**
     * Checks if the cache should be invalidated based on pack composition or TTL.
     */
    private boolean shouldInvalidateCache(List<Wolf> currentPackMembers, long currentTime) {
        // Check TTL
        if (currentTime - lastCacheTime > CACHE_TTL_TICKS * 50) {
            return true;
        }

        // Check if pack composition changed
        if (cachedPackOrder == null || cachedPackOrder.size() != currentPackMembers.size()) {
            return true;
        }

        // Check if all current members are in cache
        for (Wolf wolf : currentPackMembers) {
            if (!cachedPackOrder.contains(wolf)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds this wolf's index in the cached pack order.
     */
    private int findIndexInCachedOrder(Wolf self) {
        if (cachedPackOrder == null) {
            return 0;
        }

        for (int i = 0; i < cachedPackOrder.size(); i++) {
            if (cachedPackOrder.get(i).getUUID().equals(self.getUUID())) {
                return i;
            }
        }

        return 0;
    }

    /**
     * Invalidates the pack order cache.
     * Call this when pack composition changes.
     */
    private void invalidatePackCache() {
        cachedPackOrder = null;
        lastCacheTime = 0;
    }

    /**
     * Checks if this wolf is the alpha of the pack.
     * Alpha is determined by health and age.
     */
    private boolean isAlpha(Wolf wolf, List<Wolf> packMembers) {
        List<Wolf> allWolves = new ArrayList<>(packMembers);
        allWolves.add(wolf);

        Wolf strongest = wolf;
        double maxStrength = calculateStrength(wolf);

        for (Wolf other : allWolves) {
            double strength = calculateStrength(other);
            if (strength > maxStrength) {
                maxStrength = strength;
                strongest = other;
            }
        }

        return strongest.getUUID().equals(wolf.getUUID());
    }

    /**
     * Calculates wolf strength for alpha determination.
     */
    private double calculateStrength(Wolf wolf) {
        double health = wolf.getHealth();
        double maxHealth = wolf.getMaxHealth();
        double ageBonus = wolf.isBaby() ? 0 : 1.5;

        return (health / maxHealth) * ageBonus;
    }

    /**
     * Updates hunt state and timers.
     */
    private void updateHuntState(Wolf wolf) {
        if (huntingState == PackHuntingState.LEADING ||
            huntingState == PackHuntingState.FLANKING ||
            huntingState == PackHuntingState.ATTACKING) {
            huntTimer++;

            // Give up after 30 seconds of hunting
            if (huntTimer > 600) {
                huntingState = PackHuntingState.RESTING;
                huntTimer = 0;
                currentPrey = null;
            }
        } else if (huntingState == PackHuntingState.RESTING) {
            huntTimer++;
            if (huntTimer > 200) { // 10 seconds rest
                huntingState = PackHuntingState.SEARCHING;
                huntTimer = 0;
            }
        }
    }

    /**
     * Updates handling timer after successful kills.
     * Wolves rest after killing to handle prey (eating, resting).
     */
    private void updateHandlingTimer(Wolf wolf) {
        if (hasKilledRecently) {
            handlingTimer++;

            if (handlingTimer >= HANDLING_TIME_TICKS) {
                hasKilledRecently = false;
                handlingTimer = 0;
            }
        }
    }

    /**
     * Checks if wolf has enough energy to hunt.
     * Wolves need at least 30% energy to start a hunt.
     */
    private boolean hasEnergyForHunt(Wolf wolf) {
        EcologyComponent component = getEcologyComponent(wolf);
        if (component == null) {
            return true; // Default to true if no energy system
        }

        int energyLevel = EnergyHandle.getEnergyLevel(component);
        return energyLevel >= ENERGY_COST_PER_HUNT;
    }

    /**
     * Checks if wolf is hungry enough to hunt.
     * Wolves only hunt when hunger is below 50%.
     */
    private boolean isHungryEnoughToHunt(Wolf wolf) {
        EcologyComponent component = getEcologyComponent(wolf);
        if (component == null) {
            return true; // Default to true if no hunger system
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (!hungerTag.contains("hunger")) {
            return true; // Default to true if no hunger data
        }

        int currentHunger = hungerTag.getInt("hunger");
        return currentHunger < HUNGER_TRIGGER_THRESHOLD;
    }

    /**
     * Gets EcologyComponent from an entity.
     */
    private EcologyComponent getEcologyComponent(Wolf wolf) {
        if (wolf instanceof EcologyAccess access) {
            return access.betterEcology$getEcologyComponent();
        }
        return null;
    }

    /**
     * Called when wolf successfully kills prey.
     * Sets handling timer and reduces energy.
     */
    public void onSuccessfulKill(Wolf wolf) {
        hasKilledRecently = true;
        handlingTimer = 0;

        // Reduce energy for the hunt
        EcologyComponent component = getEcologyComponent(wolf);
        if (component != null) {
            CompoundTag energyTag = component.getHandleTag("energy");
            int currentEnergy = energyTag.getInt("energy");
            int newEnergy = Math.max(0, currentEnergy - ENERGY_COST_PER_HUNT);
            energyTag.putInt("energy", newEnergy);
        }

        huntingState = PackHuntingState.RESTING;
        huntTimer = 0;
    }

    /**
     * Gets or generates pack ID.
     */
    private UUID getPackId(Wolf wolf) {
        if (packId == null) {
            packId = wolf.getUUID();
        }
        return packId;
    }

    public Entity getCurrentPrey() {
        return currentPrey;
    }

    public PackHuntingState getHuntingState() {
        return huntingState;
    }

    public UUID getPackId() {
        return packId;
    }

    /**
     * Hunting states for pack behavior.
     */
    public enum PackHuntingState {
        IDLE,           // No prey detected
        SEARCHING,      // Looking for prey
        WAITING,        // Waiting for pack to assemble
        LEADING,        // Alpha leading the hunt
        FLANKING,       // Pack member flanking prey
        ATTACKING,      // Attacking prey
        RESTING         // Recovering after hunt
    }
}
