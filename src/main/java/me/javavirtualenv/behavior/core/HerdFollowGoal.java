package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes animals stay together in herds/flocks.
 *
 * <p>Implements simplified boids-like behavior:
 * <ul>
 *   <li>Cohesion: Move toward the center of nearby group members</li>
 *   <li>Separation: Avoid getting too close to neighbors</li>
 *   <li>Alignment: Match the general movement direction of the group</li>
 * </ul>
 *
 * <p>Only activates when the animal is somewhat isolated from its herd.
 */
public class HerdFollowGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(HerdFollowGoal.class);

    private static final int CHECK_INTERVAL_TICKS = 60;
    private static final double MIN_SEPARATION_DISTANCE = 2.0;

    private final Mob mob;
    private final Class<? extends Mob> herdMateType;
    private final double speedModifier;
    private final int searchRadius;
    private final int minHerdSize;
    private final double maxDistanceFromHerd;

    private Vec3 targetPosition;
    private int checkCooldown;
    private int followTicks;

    /**
     * Creates a new HerdFollowGoal.
     *
     * @param mob the mob that will follow the herd
     * @param herdMateType the class of entities to herd with
     * @param speedModifier movement speed multiplier
     * @param searchRadius radius to search for herd members
     * @param minHerdSize minimum nearby members to form a herd
     * @param maxDistanceFromHerd distance at which to start following
     */
    public HerdFollowGoal(
            Mob mob,
            Class<? extends Mob> herdMateType,
            double speedModifier,
            int searchRadius,
            int minHerdSize,
            double maxDistanceFromHerd) {
        this.mob = mob;
        this.herdMateType = herdMateType;
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.minHerdSize = minHerdSize;
        this.maxDistanceFromHerd = maxDistanceFromHerd;
        this.checkCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Creates a new HerdFollowGoal with default parameters.
     *
     * @param mob the mob that will follow the herd
     * @param herdMateType the class of entities to herd with
     */
    public HerdFollowGoal(Mob mob, Class<? extends Mob> herdMateType) {
        this(mob, herdMateType, 1.0, 24, 2, 8.0);
    }

    @Override
    public boolean canUse() {
        if (this.checkCooldown > 0) {
            this.checkCooldown--;
            return false;
        }

        this.checkCooldown = reducedTickDelay(CHECK_INTERVAL_TICKS);

        List<? extends Mob> nearbyHerdMates = findNearbyHerdMates();

        if (nearbyHerdMates.size() < this.minHerdSize) {
            return false;
        }

        Vec3 herdCenter = calculateHerdCenter(nearbyHerdMates);
        double distanceToHerd = this.mob.position().distanceTo(herdCenter);

        if (distanceToHerd < this.maxDistanceFromHerd) {
            return false;
        }

        this.targetPosition = calculateTargetPosition(nearbyHerdMates, herdCenter);
        LOGGER.debug("{} is {} blocks from herd, moving to rejoin",
            this.mob.getName().getString(), String.format("%.1f", distanceToHerd));

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.followTicks > 200) {
            return false;
        }

        if (this.targetPosition == null) {
            return false;
        }

        double distanceToTarget = this.mob.position().distanceTo(this.targetPosition);
        return distanceToTarget > MIN_SEPARATION_DISTANCE;
    }

    @Override
    public void start() {
        this.followTicks = 0;
        navigateToTarget();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped following herd", this.mob.getName().getString());
        this.targetPosition = null;
        this.followTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.followTicks++;

        if (this.followTicks % 20 == 0) {
            List<? extends Mob> nearbyHerdMates = findNearbyHerdMates();
            if (!nearbyHerdMates.isEmpty()) {
                Vec3 herdCenter = calculateHerdCenter(nearbyHerdMates);
                this.targetPosition = calculateTargetPosition(nearbyHerdMates, herdCenter);
                navigateToTarget();
            }
        }
    }

    /**
     * Finds nearby herd mates of the same type.
     *
     * @return list of nearby herd members
     */
    private List<? extends Mob> findNearbyHerdMates() {
        AABB searchBox = this.mob.getBoundingBox().inflate(this.searchRadius);
        return this.mob.level().getEntitiesOfClass(this.herdMateType, searchBox, this::isValidHerdMate);
    }

    /**
     * Validates if an entity is a valid herd mate.
     *
     * @param entity the entity to check
     * @return true if this is a valid herd mate
     */
    private boolean isValidHerdMate(Mob entity) {
        if (entity == this.mob) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        return true;
    }

    /**
     * Calculates the center position of the herd.
     *
     * @param herdMates list of herd members
     * @return center position of the herd
     */
    private Vec3 calculateHerdCenter(List<? extends Mob> herdMates) {
        double sumX = 0, sumY = 0, sumZ = 0;

        for (Mob mate : herdMates) {
            sumX += mate.getX();
            sumY += mate.getY();
            sumZ += mate.getZ();
        }

        int count = herdMates.size();
        return new Vec3(sumX / count, sumY / count, sumZ / count);
    }

    /**
     * Calculates the target position considering cohesion and separation.
     *
     * @param herdMates list of herd members
     * @param herdCenter center of the herd
     * @return optimal target position
     */
    private Vec3 calculateTargetPosition(List<? extends Mob> herdMates, Vec3 herdCenter) {
        // Start with cohesion (move toward center)
        Vec3 cohesion = herdCenter;

        // Find the nearest herd mate to avoid crowding
        Mob nearest = herdMates.stream()
            .min(Comparator.comparingDouble(mate -> this.mob.distanceToSqr(mate)))
            .orElse(null);

        if (nearest != null) {
            double distToNearest = this.mob.distanceTo(nearest);
            if (distToNearest < MIN_SEPARATION_DISTANCE * 2) {
                // Adjust target to maintain some separation
                Vec3 awayFromNearest = this.mob.position().subtract(nearest.position()).normalize();
                cohesion = cohesion.add(awayFromNearest.scale(MIN_SEPARATION_DISTANCE));
            }
        }

        return cohesion;
    }

    /**
     * Navigates the mob toward the target position.
     */
    private void navigateToTarget() {
        if (this.targetPosition == null) {
            return;
        }

        this.mob.getNavigation().moveTo(
            this.targetPosition.x,
            this.targetPosition.y,
            this.targetPosition.z,
            this.speedModifier
        );
    }
}
