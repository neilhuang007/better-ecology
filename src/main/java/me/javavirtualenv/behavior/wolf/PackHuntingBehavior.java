package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.predation.PreySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Fox;

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

    private Entity currentPrey;
    private PackHuntingState huntingState = PackHuntingState.IDLE;
    private UUID packId;
    private int huntTimer = 0;

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
        Mob entity = context.getEntity();
        if (!(entity instanceof Wolf wolf)) {
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
        int totalPackMembers = packMembers.size();

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

        // Filter to wolf-specific prey
        if (selectedPrey != null && isValidPrey(wolf, selectedPrey)) {
            return selectedPrey;
        }

        return null;
    }

    /**
     * Checks if an entity is valid prey.
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

        // Valid prey: sheep, rabbits, foxes
        return entity instanceof Sheep || entity instanceof Rabbit || entity instanceof Fox;
    }

    /**
     * Gets all pack members within coordination range.
     */
    private List<Wolf> getPackMembers(Wolf wolf, BehaviorContext context) {
        List<Wolf> pack = new ArrayList<>();

        for (Entity entity : context.getNeighbors()) {
            if (!(entity instanceof Wolf otherWolf)) {
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
     */
    private int getPackPosition(Wolf self, List<Wolf> packMembers, Vec3d preyPos) {
        Vec3d selfPos = new Vec3d(self.getX(), self.getY(), self.getZ());

        // Sort pack members by distance to prey
        List<Wolf> sorted = new ArrayList<>(packMembers);
        sorted.sort(Comparator.comparingDouble(w -> {
            Vec3d wolfPos = new Vec3d(w.getX(), w.getY(), w.getZ());
            return wolfPos.distanceTo(preyPos);
        }));

        // Find our index
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(self.getId())) {
                return i;
            }
        }

        return 0;
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

        return strongest.getId().equals(wolf.getId());
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
        if (huntingState == PackHuntingState.HUNTING ||
            huntingState == PackHuntingState.LEADING ||
            huntingState == PackHuntingState.FLANKING) {
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
     * Gets or generates pack ID from NBT.
     */
    private UUID getPackId(Wolf wolf) {
        if (packId != null) {
            return packId;
        }

        // Try to get from persistent data
        var persistentData = wolf.getPersistentData();
        if (persistentData.contains("WolfPackId")) {
            packId = persistentData.getUUID("WolfPackId");
            return packId;
        }

        // Generate new pack ID
        packId = UUID.randomUUID();
        persistentData.putUUID("WolfPackId", packId);
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

    public void setPackId(UUID packId) {
        this.packId = packId;
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
