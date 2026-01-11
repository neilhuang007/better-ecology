package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.behavior.predation.PreySelector;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Goal for foxes to hunt prey (chickens, rabbits, etc.).
 * <p>
 * Foxes will hunt when:
 * <ul>
 *   <li>They are hungry (hunger below threshold)</li>
 *   <li>Prey is within detection range</li>
 *   <li>The prey is reachable (path validation)</li>
 * </ul>
 * <p>
 * Uses a stalking behavior: slow approach, crouch, then pounce.
 */
public class FoxHuntGoal extends Goal {

    // Configuration constants
    private static final double DETECTION_RADIUS = 32.0;
    private static final double STALK_SPEED = 0.4;
    private static final double POUNCE_SPEED = 1.5;
    private static final double POUNCE_RANGE = 5.0;
    private static final double CROUCH_SPEED = 0.2;
    private static final int HUNGRY_THRESHOLD = 60;
    private static final int STALK_DURATION = 60;
    private static final int CROUCH_DURATION = 40;
    private static final int COOLDOWN_TICKS = 300;

    // Hunting states
    private enum HuntingState {
        IDLE,
        STALKING,
        CROUCHING,
        POUNCING,
        ATTACKING
    }

    // Instance fields
    private final Fox fox;
    private final PreySelector preySelector;
    private LivingEntity targetPrey;
    private HuntingState currentState;
    private int stateTimer;
    private Path currentPath;
    private int cooldownTicks;
    private Vec3 pounceTarget;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasHungryLastCheck = false;

    public FoxHuntGoal(Fox fox) {
        this.fox = fox;
        this.preySelector = new PreySelector();
        this.currentState = HuntingState.IDLE;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Legacy constructor for backward compatibility.
     * @param fox The fox (must be Fox type)
     * @param behavior Ignored (behavior is now internal)
     * @param speedModifier Ignored (uses internal POUNCE_SPEED)
     */
    public FoxHuntGoal(net.minecraft.world.entity.PathfinderMob fox, Object behavior, double speedModifier) {
        this((Fox) fox);
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (fox.level().isClientSide) {
            return false;
        }

        // Cooldown prevents spamming the goal
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Don't hunt if sleeping
        if (fox.isSleeping()) {
            return false;
        }

        // Check if fox is hungry (direct NBT check for reliability)
        int hunger = getHungerLevel();
        boolean isHungry = hunger < HUNGRY_THRESHOLD;

        // Log state change for debugging
        if (isHungry != wasHungryLastCheck) {
            debug("hunger state changed: " + wasHungryLastCheck + " -> " + isHungry + " (hunger=" + hunger + ")");
            wasHungryLastCheck = isHungry;
        }

        if (!isHungry) {
            return false;
        }

        // Find prey
        targetPrey = findReachablePrey();
        if (targetPrey == null) {
            return false;
        }

        debug("STARTING: hunting " + getPreyName(targetPrey) + " #" + targetPrey.getId() +
              " (hunger=" + hunger + ", state=" + currentState + ")");
        currentState = HuntingState.STALKING;
        stateTimer = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (cooldownTicks > 0) {
            return false;
        }

        // Stop if prey is gone
        if (targetPrey == null || !targetPrey.isAlive()) {
            debug("prey no longer available");
            return false;
        }

        // Stop if prey is too far
        double distance = fox.position().distanceTo(targetPrey.position());
        if (distance > DETECTION_RADIUS * 1.5) {
            debug("prey too far, giving up");
            return false;
        }

        // Continue hunting based on state
        return currentState != HuntingState.IDLE;
    }

    @Override
    public void start() {
        debug("hunt started, state=" + currentState);
    }

    @Override
    public void stop() {
        debug("hunt stopped (state=" + currentState + ", cooldown=" + cooldownTicks + ")");
        targetPrey = null;
        currentState = HuntingState.IDLE;
        stateTimer = 0;
        currentPath = null;
        pounceTarget = null;
        fox.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPrey == null || !targetPrey.isAlive()) {
            targetPrey = null;
            currentState = HuntingState.IDLE;
            return;
        }

        stateTimer++;
        double distance = fox.position().distanceTo(targetPrey.position());

        // Look at prey
        fox.getLookControl().setLookAt(targetPrey, 30.0f, 30.0f);

        switch (currentState) {
            case STALKING -> handleStalking(distance);
            case CROUCHING -> handleCrouching(distance);
            case POUNCING -> handlePouncing();
            case ATTACKING -> handleAttacking();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Handle stalking behavior - slow approach toward prey.
     */
    private void handleStalking(double distance) {
        // Move to crouch if close enough
        if (distance < 10.0 && stateTimer > STALK_DURATION) {
            currentState = HuntingState.CROUCHING;
            stateTimer = 0;
            debug("transitioning to CROUCHING");
            playStalkSound();
            return;
        }

        // Re-path if needed
        if (!fox.getNavigation().isInProgress() || currentPath == null || !currentPath.canReach()) {
            moveToPrey(STALK_SPEED);
        }
    }

    /**
     * Handle crouching behavior - prepare to pounce.
     */
    private void handleCrouching(double distance) {
        // Pounce if in range and waited long enough
        if (distance < POUNCE_RANGE && stateTimer > CROUCH_DURATION) {
            currentState = HuntingState.POUNCING;
            stateTimer = 0;
            initiatePounce();
            return;
        }

        // Very slow movement while crouching
        if (distance > 3.0) {
            if (!fox.getNavigation().isInProgress() || currentPath == null || !currentPath.canReach()) {
                moveToPrey(CROUCH_SPEED);
            }
        }

        // Log progress every second
        if (stateTimer % 20 == 0) {
            debug("crouching, distance=" + String.format("%.1f", distance) + " blocks");
        }
    }

    /**
     * Handle pouncing behavior - leap toward prey.
     */
    private void handlePouncing() {
        // Check if landed
        if (fox.onGround()) {
            currentState = HuntingState.ATTACKING;
            stateTimer = 0;
            debug("landed pounce, attacking");
            return;
        }

        // Continue pounce trajectory
        if (stateTimer % 10 == 0) {
            debug("mid-pounce, timer=" + stateTimer);
        }
    }

    /**
     * Handle attacking behavior - attack the prey.
     */
    private void handleAttacking() {
        // Try to attack the prey
        if (stateTimer == 0) {
            fox.doHurtTarget(targetPrey);
            debug("attacked prey");
        }

        stateTimer++;

        // Done after a brief attack
        if (stateTimer > 20) {
            // Check if prey died
            if (!targetPrey.isAlive()) {
                restoreHungerFromKill();
                cooldownTicks = COOLDOWN_TICKS;
                debug("prey killed, hunt complete");
            } else {
                // Prey survived, may continue or flee
                debug("prey survived, ending hunt");
            }
            currentState = HuntingState.IDLE;
        }
    }

    /**
     * Initiate the pounce attack.
     */
    private void initiatePounce() {
        Vec3 foxPos = fox.position();
        Vec3 preyPos = targetPrey.position();

        // Calculate pounce direction
        Vec3 toPrey = preyPos.subtract(foxPos).normalize();
        pounceTarget = preyPos;

        // Launch fox toward prey
        Vec3 pounceVec = new Vec3(toPrey.x * POUNCE_SPEED, 0.6, toPrey.z * POUNCE_SPEED);
        fox.setDeltaMovement(pounceVec);
        fox.hasImpulse = true;

        playPounceSound();
        spawnPounceParticles();
        debug("pounced toward prey");
    }

    /**
     * Move toward the prey.
     */
    private void moveToPrey(double speed) {
        PathNavigation navigation = fox.getNavigation();
        currentPath = navigation.createPath(targetPrey, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(targetPrey, speed);
            if (stateTimer % 40 == 0) {
                debug("path found to prey, nodes=" + currentPath.getNodeCount());
            }
        } else {
            debug("NO PATH to prey, giving up");
            currentState = HuntingState.IDLE;
            cooldownTicks = COOLDOWN_TICKS / 2;
        }
    }

    /**
     * Find reachable prey within detection radius.
     */
    private LivingEntity findReachablePrey() {
        // Check current target first
        if (targetPrey != null && targetPrey.isAlive() && isFoxPrey(targetPrey)) {
            double distance = fox.position().distanceTo(targetPrey.position());
            if (distance <= DETECTION_RADIUS && canReachPrey(targetPrey)) {
                return targetPrey;
            }
        }

        // Use prey selector to find new prey
        Entity selected = preySelector.selectPrey(fox);
        if (selected instanceof LivingEntity living && isFoxPrey(living)) {
            double distance = fox.position().distanceTo(living.position());
            if (distance <= DETECTION_RADIUS && canReachPrey(living)) {
                return living;
            }
        }

        return null;
    }

    /**
     * Check if prey is reachable via pathfinding.
     */
    private boolean canReachPrey(LivingEntity prey) {
        if (prey instanceof Fox) {
            return false; // Don't hunt other foxes
        }

        BlockPos preyPos = prey.blockPosition();
        Path path = fox.getNavigation().createPath(preyPos, 0);
        return path != null && path.canReach();
    }

    /**
     * Check if an entity is valid fox prey.
     */
    private boolean isFoxPrey(Entity entity) {
        return entity instanceof Chicken ||
               entity instanceof Rabbit ||
               entity instanceof Fox == false && entity instanceof Animal;
    }

    /**
     * Get the current hunger level from NBT data.
     */
    private int getHungerLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100;
        }
        CompoundTag tag = component.getHandleTag("hunger");
        return tag.contains("hunger") ? tag.getInt("hunger") : 100;
    }

    /**
     * Restore hunger after killing prey.
     */
    private void restoreHungerFromKill() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return;
        }

        int currentHunger = getHungerLevel();
        int restoreAmount = 50; // Large restore from killing prey
        int newHunger = Math.min(100, currentHunger + restoreAmount);

        CompoundTag tag = component.getHandleTag("hunger");
        tag.putInt("hunger", newHunger);

        debug("hunger restored from kill: " + currentHunger + " -> " + newHunger);
    }

    /**
     * Get the ecology component for this fox.
     */
    private EcologyComponent getComponent() {
        if (!(fox instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Get a readable name for the prey entity.
     */
    private String getPreyName(Entity prey) {
        String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(prey.getType()).toString();
        return typeId.replace("minecraft:", "");
    }

    /**
     * Play stalk sound.
     */
    private void playStalkSound() {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_SNIFF,
            SoundSource.NEUTRAL, 0.5f, 1.0f);
    }

    /**
     * Play pounce sound.
     */
    private void playPounceSound() {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_BITE,
            SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    /**
     * Spawn pounce particles.
     */
    private void spawnPounceParticles() {
        if (fox.level().isClientSide) {
            return;
        }

        Vec3 pos = fox.position();
        for (int i = 0; i < 10; i++) {
            fox.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                pos.x, pos.y + 0.5, pos.z,
                (fox.getRandom().nextDouble() - 0.5) * 0.2,
                fox.getRandom().nextDouble() * 0.2,
                (fox.getRandom().nextDouble() - 0.5) * 0.2
            );
        }
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[FoxHunt] Fox #" + fox.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        String preyName = targetPrey != null ? getPreyName(targetPrey) : "none";
        return String.format("hunger=%d, prey=%s, state=%s, timer=%d, cooldown=%d, path=%s",
            getHungerLevel(),
            preyName,
            currentState,
            stateTimer,
            cooldownTicks,
            fox.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
