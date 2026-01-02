package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.seasonal.WinterSiegeScheduler.SiegeType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;

/**
 * Wolf siege attack goal for winter village attacks.
 * <p>
 * Integrates with the ecology system to execute attacks when wolves are
 * in siege mode. This goal only activates when the siege state is set
 * by WinterSiegeScheduler and targets are within attack range.
 * <p>
 * Target selection is based on siege type:
 * - LIVESTOCK_RAID: Only targets livestock (sheep, cows, pigs, chickens)
 * - FULL_ASSAULT: Targets livestock, villagers, and iron golems
 */
public class WolfSiegeAttackGoal extends MeleeAttackGoal {

    private final Wolf wolf;
    private final double attackSpeedMultiplier;
    private long lastAttackTick = 0;
    private final long attackCooldownTicks;

    /**
     * Creates a siege attack goal.
     *
     * @param wolf                         The wolf entity
     * @param speedModifier                Speed modifier when chasing (default 1.0)
     * @param followingTargetEvenIfNotSeen Whether to follow target if not visible
     */
    public WolfSiegeAttackGoal(Wolf wolf, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(wolf, speedModifier, followingTargetEvenIfNotSeen);
        this.wolf = wolf;
        this.attackSpeedMultiplier = speedModifier;
        // Attack every 15 ticks (0.75 seconds) when in siege mode
        this.attackCooldownTicks = 15;
    }

    @Override
    public boolean canUse() {
        // Skip if tamed
        if (wolf.isTame()) {
            return false;
        }

        // Check if wolf is in siege mode
        if (!isSieging()) {
            return false;
        }

        // Get siege type from component
        SiegeType siegeType = getSiegeType();

        // Check if current target is valid for this siege type
        LivingEntity target = wolf.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Validate target based on siege type
        if (!isValidSiegeTarget(target, siegeType)) {
            return false;
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Continue only if still sieging and target is valid
        if (!isSieging()) {
            return false;
        }

        LivingEntity target = wolf.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        SiegeType siegeType = getSiegeType();
        if (!isValidSiegeTarget(target, siegeType)) {
            return false;
        }

        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        // Set aggressive mode for siege
        wolf.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        // Don't clear aggressive state immediately - let siege end naturally
        // WolfSiegeBehavior handles clearing siege state
    }

    @Override
    public void tick() {
        super.tick();
        // Keep target set from siege behavior
        LivingEntity target = wolf.getTarget();
        if (target != null && target.isAlive()) {
            // Update aggressiveness based on target type
            if (target instanceof Villager) {
                // More aggressive against villagers during full assault
                wolf.setAggressive(true);
            }
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
     * Gets the current siege type.
     */
    private SiegeType getSiegeType() {
        if (!(wolf instanceof EcologyAccess access)) {
            return SiegeType.LIVESTOCK_RAID;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return SiegeType.LIVESTOCK_RAID;
        }

        var tag = component.getHandleTag("siege");
        String siegeTypeStr = tag.getString("siege_type");

        if (!siegeTypeStr.isEmpty()) {
            try {
                return SiegeType.valueOf(siegeTypeStr);
            } catch (IllegalArgumentException e) {
                return SiegeType.LIVESTOCK_RAID;
            }
        }

        return SiegeType.LIVESTOCK_RAID;
    }

    /**
     * Checks if an entity is a valid target for the current siege type.
     */
    private boolean isValidSiegeTarget(Entity entity, SiegeType siegeType) {
        if (!entity.isAlive()) {
            return false;
        }

        // Don't target other wolves
        if (entity instanceof Wolf) {
            return false;
        }

        // Don't target players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        // Don't target tamed animals
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable && tameable.isTame()) {
            return false;
        }

        // Check based on siege type
        if (siegeType == SiegeType.LIVESTOCK_RAID) {
            // Livestock raid: only target livestock
            return entity instanceof net.minecraft.world.entity.animal.Sheep ||
                    entity instanceof net.minecraft.world.entity.animal.Cow ||
                    entity instanceof net.minecraft.world.entity.animal.Pig ||
                    entity instanceof net.minecraft.world.entity.animal.Chicken;
        } else {
            // Full assault: target livestock, villagers, or golems
            return entity instanceof net.minecraft.world.entity.animal.Sheep ||
                    entity instanceof net.minecraft.world.entity.animal.Cow ||
                    entity instanceof net.minecraft.world.entity.animal.Pig ||
                    entity instanceof net.minecraft.world.entity.animal.Chicken ||
                    entity instanceof Villager ||
                    entity instanceof net.minecraft.world.entity.animal.IronGolem;
        }
    }

    @Override
    public int getAttackInterval() {
        // Faster attacks during siege
        return 15;
    }
}
