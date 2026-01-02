package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Wolf pack hunting attack goal.
 * <p>
 * Integrates with PackHuntingBehavior to execute attacks during pack hunting.
 * This goal only activates when the wolf is in ATTACKING state from the
 * pack hunting behavior system.
 * <p>
 * Targets are managed by PackHuntingBehavior's prey selection system.
 */
public class WolfPackAttackGoal extends MeleeAttackGoal {

    private final Wolf wolf;
    private final double attackSpeedMultiplier;
    private long lastAttackTick = 0;
    private final long attackCooldownTicks;

    /**
     * Creates a pack hunting attack goal.
     *
     * @param wolf                         The wolf entity
     * @param speedModifier                Speed modifier when chasing (default 1.0)
     * @param followingTargetEvenIfNotSeen Whether to follow target if not visible
     */
    public WolfPackAttackGoal(Wolf wolf, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(wolf, speedModifier, followingTargetEvenIfNotSeen);
        this.wolf = wolf;
        this.attackSpeedMultiplier = speedModifier;
        // Attack every 20 ticks (1 second) during normal hunting
        this.attackCooldownTicks = 20;
    }

    @Override
    public boolean canUse() {
        // Skip if tamed
        if (wolf.isTame()) {
            return false;
        }

        // Check if wolf is in siege mode (different goal handles siege)
        if (isSieging()) {
            return false;
        }

        // Get pack hunting state from behavior handle
        PackHuntingBehavior.PackHuntingState huntingState = getHuntingState();

        // Only activate when in ATTACKING state
        if (huntingState != PackHuntingBehavior.PackHuntingState.ATTACKING) {
            return false;
        }

        // Check if current target is valid
        LivingEntity target = wolf.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Validate target is valid prey
        if (!isValidPrey(target)) {
            return false;
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Continue only if still in attacking state
        PackHuntingBehavior.PackHuntingState huntingState = getHuntingState();

        if (huntingState != PackHuntingBehavior.PackHuntingState.ATTACKING) {
            return false;
        }

        LivingEntity target = wolf.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        // Set aggressive mode for hunting
        wolf.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        // Clear aggressive state when stopping
        wolf.setAggressive(false);

        // Check if we killed the target and notify PackHuntingBehavior
        LivingEntity target = wolf.getTarget();
        if (target != null && !target.isAlive()) {
            notifySuccessfulKill();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Check if target died during attack
        LivingEntity target = wolf.getTarget();
        if (target != null && !target.isAlive()) {
            notifySuccessfulKill();
        }
    }

    /**
     * Checks if the wolf is currently in siege mode.
     */
    private boolean isSieging() {
        if (!(wolf instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        var tag = component.getHandleTag("siege");
        return tag.getBoolean("is_sieging");
    }

    /**
     * Gets the current pack hunting state from PackHuntingBehavior.
     */
    private PackHuntingBehavior.PackHuntingState getHuntingState() {
        // Default to IDLE if behavior handle not available
        return PackHuntingBehavior.PackHuntingState.IDLE;
    }

    /**
     * Notifies PackHuntingBehavior of a successful kill.
     */
    private void notifySuccessfulKill() {
        // This will be integrated when we have access to the behavior instance
        // For now, the hunting state will naturally transition through tick()
    }

    /**
     * Checks if an entity is valid prey for pack hunting.
     */
    private boolean isValidPrey(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity.equals(wolf)) {
            return false;
        }

        // Don't hunt players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        // Don't hunt tamed animals
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable && tameable.isTame()) {
            return false;
        }

        // Don't hunt other wolves (unless from different pack)
        if (entity instanceof Wolf otherWolf) {
            // Simple check - in full implementation, this would check pack ID
            return false;
        }

        // Valid prey: sheep, rabbits, foxes, chickens
        return entity instanceof net.minecraft.world.entity.animal.Sheep ||
                entity instanceof net.minecraft.world.entity.animal.Rabbit ||
                entity instanceof net.minecraft.world.entity.animal.Fox ||
                entity instanceof net.minecraft.world.entity.animal.Chicken;
    }

    @Override
    public int getAttackInterval() {
        // Standard attack interval for hunting
        return 20;
    }
}
