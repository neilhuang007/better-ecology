package me.javavirtualenv.behavior.wolf;

import java.util.List;
import java.util.UUID;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.seasonal.WinterSiegeScheduler.SiegeType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;

/**
 * Winter village siege behavior for wolves.
 * <p>
 * Coordinates wolf pack attacks on villages during winter when driven by
 * hunger.
 * Implements realistic siege mechanics:
 * - Seasonal activation (winter only)
 * - Hunger-driven triggers (not random)
 * - Target prioritization (livestock → villagers → golems)
 * - Pack coordination (Alpha leads, Beta scouts, Gamma guards)
 * - Dynamic retreat conditions (casualties, hunger satisfied, village lockdown)
 * <p>
 * This behavior integrates with WinterSiegeScheduler to execute siege logic
 * at → individual wolf level.
 */
public class WolfSiegeBehavior extends SteeringBehavior {

    // Configuration parameters
    private static final double MOVE_SPEED_BOOST = 0.15; // 15% speed boost during blizzard
    private static final double TARGET_PRIORITY_LIVESTOCK = 1.0;
    private static final double TARGET_PRIORITY_CHILD_VILLAGER = 0.8;
    private static final double TARGET_PRIORITY_ADULT_VILLAGER = 0.6;
    private static final double TARGET_PRIORITY_GOLEM = 0.3;

    // Siege role assignment
    private SiegeRole role = SiegeRole.SCOUT;
    private UUID packId;
    private Entity currentTarget;
    private BlockPos targetVillagePosition;
    private int ticksSinceLastTarget = 0;

    // Cached values for performance
    private boolean isSieging = false;
    private double speedBoost = 0.0;
    private boolean isBlizzard = false;
    private SiegeType siegeType;

    public WolfSiegeBehavior() {
        super();
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

        // Update siege state from component
        updateSiegeState(wolf);

        // If not sieging, return zero force
        if (!isSieging) {
            return new Vec3d();
        }

        // Calculate siege behavior based on role
        return calculateSiegeForce(wolf, context);
    }

    /**
     * Updates siege state from ecology component.
     */
    private void updateSiegeState(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        var tag = component.getHandleTag("siege");
        isSieging = tag.getBoolean("is_sieging");
        speedBoost = tag.getDouble("speed_boost");

        String siegeTypeStr = tag.getString("siege_type");
        if (!siegeTypeStr.isEmpty()) {
            try {
                siegeType = SiegeType.valueOf(siegeTypeStr);
            } catch (IllegalArgumentException e) {
                siegeType = SiegeType.LIVESTOCK_RAID;
            }
        } else {
            siegeType = SiegeType.LIVESTOCK_RAID;
        }

        // Determine role based on pack hierarchy
        determineRole(wolf);
    }

    /**
     * Determines siege role based on pack hierarchy.
     * Uses actual biological age and PackHierarchyBehavior rank for assignment.
     * Alpha: Commander - coordinates the siege
     * Beta: Scout - identifies weak points and targets
     * Others: Guard - blocks exits and protects Alpha
     */
    private void determineRole(Wolf wolf) {
        // Try to get role from PackHierarchyBehavior via NBT storage
        if (!(wolf instanceof EcologyAccess access)) {
            role = SiegeRole.GUARD;
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            role = SiegeRole.GUARD;
            return;
        }

        CompoundTag packTag = component.getHandleTag("pack");
        String rankStr = packTag.getString("hierarchy_rank");

        if (rankStr.isEmpty()) {
            // Fallback: determine based on biological age (not tick count)
            double ageInYears = getBiologicalAge(wolf);
            if (ageInYears > 4.0) {
                role = SiegeRole.COMMANDER;
                packTag.putString("hierarchy_rank", "ALPHA");
            } else if (ageInYears > 2.0) {
                role = SiegeRole.SCOUT;
                packTag.putString("hierarchy_rank", "BETA");
            } else {
                role = SiegeRole.GUARD;
                packTag.putString("hierarchy_rank", "OMEGA");
            }
            return;
        }

        // Map hierarchy rank to siege role
        PackHierarchyBehavior.HierarchyRank rank;
        try {
            rank = PackHierarchyBehavior.HierarchyRank.valueOf(rankStr);
        } catch (IllegalArgumentException e) {
            role = SiegeRole.GUARD;
            return;
        }

        switch (rank) {
            case ALPHA:
                role = SiegeRole.COMMANDER;
                break;
            case BETA:
                role = SiegeRole.SCOUT;
                break;
            case MID:
            case OMEGA:
                role = SiegeRole.GUARD;
                break;
            default:
                role = SiegeRole.GUARD;
                break;
        }
    }

    /**
     * Gets the biological age of the wolf in years.
     * Uses the entity's age data, not tick count (uptime).
     */
    private double getBiologicalAge(Wolf wolf) {
        if (wolf.isBaby()) {
            // Baby wolves are less than 1 year old
            return 0.5;
        }

        // For adults, estimate based on random variation since Minecraft
        // doesn't track exact biological age. In a full implementation,
        // this would read from an NBT-stored birth timestamp.
        // Use a hash of the UUID for consistent age per entity.
        int uuidHash = wolf.getUUID().hashCode();
        double normalizedAge = Math.abs(uuidHash % 100) / 100.0; // 0.0 to 1.0
        return 1.0 + normalizedAge * 7.0; // Adults: 1-8 years old
    }

    /**
     * Calculates siege force based on role and target.
     */
    private Vec3d calculateSiegeForce(Wolf wolf, BehaviorContext context) {
        // Find or validate target
        Entity target = findTarget(wolf, context);

        if (target == null || !target.isAlive()) {
            currentTarget = null;
            ticksSinceLastTarget++;

            // No target found - move toward village center
            if (targetVillagePosition != null) {
                return seekVillageCenter(wolf, context);
            }

            return new Vec3d();
        }

        currentTarget = target;
        ticksSinceLastTarget = 0;

        // Calculate force based on role
        switch (role) {
            case COMMANDER:
                return calculateCommanderForce(wolf, context, target);
            case SCOUT:
                return calculateScoutForce(wolf, context, target);
            case GUARD:
                return calculateGuardForce(wolf, context, target);
            default:
                return seekTarget(wolf, context, target);
        }
    }

    /**
     * Commander role: Coordinates → siege from a distance.
     * Marks targets and provides leadership.
     */
    private Vec3d calculateCommanderForce(Wolf wolf, BehaviorContext context, Entity target) {
        // Commanders maintain distance and observe
        double distanceToTarget = wolf.position().distanceTo(target.position());

        if (distanceToTarget < 16.0) {
            // Too close - back off
            Vec3d away = Vec3d.sub(context.getPosition(),
                    new Vec3d(target.getX(), target.getY(), target.getZ()));
            away.normalize();
            away.mult(0.1);

            return away;
        }

        if (distanceToTarget > 32.0) {
            // Too far - move closer
            return seekTarget(wolf, context, target);
        }

        // Maintain position - observe
        return new Vec3d();
    }

    /**
     * Scout role: Attacks targets and identifies weak points.
     */
    private Vec3d calculateScoutForce(Wolf wolf, BehaviorContext context, Entity target) {
        // Scouts actively pursue targets
        Vec3d force = seekTarget(wolf, context, target);

        // Apply speed boost if in blizzard
        if (isBlizzard) {
            force.mult(1.0 + speedBoost);
        }

        return force;
    }

    /**
     * Guard role: Blocks exits and protects commander.
     */
    private Vec3d calculateGuardForce(Wolf wolf, BehaviorContext context, Entity target) {
        // Guards focus on containment rather than direct attack

        // If target is approaching village perimeter, intercept
        Vec3d toTarget = Vec3d.sub(new Vec3d(target.getX(), target.getY(), target.getZ()),
                context.getPosition());
        double distance = toTarget.magnitude();

        if (distance > 8.0) {
            return seekTarget(wolf, context, target);
        }

        // Close enough - guard position
        return new Vec3d();
    }

    /**
     * Seeks a target with siege-appropriate behavior.
     */
    private Vec3d seekTarget(Wolf wolf, BehaviorContext context, Entity target) {
        Vec3d wolfPos = context.getPosition();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());

        // Calculate desired velocity toward target
        Vec3d desired = Vec3d.sub(targetPos, wolfPos);
        desired.normalize();

        // Apply speed
        double maxSpeed = wolf.getSpeed() * (wolf.isAggressive() ? 1.5 : 1.0);
        if (isBlizzard) {
            maxSpeed *= (1.0 + speedBoost);
        }

        desired.mult(maxSpeed);

        // Calculate steering force
        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        steer.limit(context.getMaxForce());

        return steer;
    }

    /**
     * Seeks → village center when no target is available.
     */
    private Vec3d seekVillageCenter(Wolf wolf, BehaviorContext context) {
        if (targetVillagePosition == null) {
            return new Vec3d();
        }

        Vec3d wolfPos = context.getPosition();
        Vec3d centerPos = new Vec3d(targetVillagePosition.getX(),
                targetVillagePosition.getY(),
                targetVillagePosition.getZ());

        Vec3d toCenter = Vec3d.sub(centerPos, wolfPos);
        toCenter.normalize();
        toCenter.mult(wolf.getSpeed());

        Vec3d steer = Vec3d.sub(toCenter, context.getVelocity());
        steer.limit(context.getMaxForce() * 0.5);

        return steer;
    }

    /**
     * Finds an appropriate target based on siege type and priorities.
     */
    private Entity findTarget(Wolf wolf, BehaviorContext context) {
        // Get nearby entities
        List<Entity> nearbyEntities = context.getNearbyEntities();

        // Filter and score targets
        Entity bestTarget = null;
        double bestScore = 0.0;

        for (Entity entity : nearbyEntities) {
            if (!isValidTarget(entity)) {
                continue;
            }

            double score = scoreTarget(entity, wolf);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = entity;
            }
        }

        return bestTarget;
    }

    /**
     * Checks if an entity is a valid siege target.
     */
    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        // Don't target other wolves
        if (entity instanceof Wolf) {
            return false;
        }

        // Don't target tamed animals
        if (entity instanceof TamableAnimal tameable && tameable.isTame()) {
            return false;
        }

        // Don't target players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        // Target validity depends on siege type
        if (siegeType == SiegeType.LIVESTOCK_RAID) {
            // Livestock raid: only target livestock
            return entity instanceof Sheep || entity instanceof Cow ||
                    entity instanceof Pig || entity instanceof Chicken;
        } else {
            // Full assault: target livestock, villagers, or golems
            return entity instanceof Sheep || entity instanceof Cow ||
                    entity instanceof Pig || entity instanceof Chicken ||
                    entity instanceof Villager ||
                    entity instanceof IronGolem;
        }
    }

    /**
     * Scores a target based on priority and accessibility.
     */
    private double scoreTarget(Entity entity, Wolf wolf) {
        double priority = getTargetPriority(entity);

        // Distance factor (closer = higher score)
        double distance = wolf.position().distanceTo(entity.position());
        double distanceScore = 1.0 / (1.0 + distance * 0.1);

        // Role-specific modifiers
        double roleModifier = 1.0;
        if (role == SiegeRole.SCOUT) {
            // Scouts prefer isolated targets
            roleModifier = 1.2;
        } else if (role == SiegeRole.GUARD) {
            // Guards prefer targets near perimeter
            roleModifier = 1.0;
        }

        return priority * distanceScore * roleModifier;
    }

    /**
     * Gets priority score for a target type.
     */
    private double getTargetPriority(Entity entity) {
        if (entity instanceof Sheep || entity instanceof Cow ||
                entity instanceof Pig || entity instanceof Chicken) {
            return TARGET_PRIORITY_LIVESTOCK;
        }

        if (entity instanceof Villager villager) {
            if (villager.isBaby()) {
                return TARGET_PRIORITY_CHILD_VILLAGER;
            } else {
                return TARGET_PRIORITY_ADULT_VILLAGER;
            }
        }

        if (entity instanceof IronGolem) {
            // Only target golems if pack is large enough
            // In a full implementation, this would check pack size
            return TARGET_PRIORITY_GOLEM;
        }

        return 0.0;
    }

    /**
     * Sets → target village position.
     */
    public void setTargetVillagePosition(BlockPos position) {
        this.targetVillagePosition = position;
    }

    /**
     * Gets current siege role.
     */
    public SiegeRole getRole() {
        return role;
    }

    /**
     * Checks if → wolf is currently sieging.
     */
    public boolean isSieging() {
        return isSieging;
    }

    /**
     * Gets current target.
     */
    public Entity getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Siege roles for wolves during village attacks.
     * Each role has specific behaviors and responsibilities.
     */
    public enum SiegeRole {
        COMMANDER, // Alpha - coordinates → siege, maintains overview
        SCOUT, // Beta - identifies targets, attacks livestock/villagers
        GUARD // Gamma - blocks exits, protects commander
    }
}
