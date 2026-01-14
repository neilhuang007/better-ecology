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
 * Goal that implements herd cohesion with quorum-based movement initiation.
 *
 * <p>Based on research from docs/behaviours/01-herd-movement-leadership.md:
 * <ul>
 *   <li>Quorum sensing: Animals wait for a threshold percentage of the herd to move before following</li>
 *   <li>Cohesion: Stay near other herd members of the same type</li>
 *   <li>Democratic consensus: Movement emerges from collective behavior</li>
 * </ul>
 *
 * <p>Implementation follows the research findings:
 * <ul>
 *   <li>Bison herds show ~47% quorum threshold before movement initiates</li>
 *   <li>Shared decision-making reduces conflicts during movement</li>
 *   <li>Individuals respond to local neighbors rather than centralized control</li>
 * </ul>
 */
public class HerdCohesionGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(HerdCohesionGoal.class);

    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final double MIN_SEPARATION_DISTANCE = 2.5;
    private static final double MOVEMENT_THRESHOLD = 0.5;

    private final Mob mob;
    private final Class<? extends Mob> herdMateType;
    private final double speedModifier;
    private final int cohesionRadius;
    private final int minHerdSize;
    private final double maxDistanceFromHerd;
    private final float quorumThreshold;

    private Vec3 targetPosition;
    private int checkCooldown;
    private int cohesionTicks;

    /**
     * Creates a new HerdCohesionGoal.
     *
     * @param mob the mob that will stay with the herd
     * @param herdMateType the class of entities to herd with
     * @param speedModifier movement speed multiplier
     * @param cohesionRadius radius to search for herd members
     * @param minHerdSize minimum nearby members to form a herd
     * @param maxDistanceFromHerd distance at which to rejoin herd
     * @param quorumThreshold percentage of herd that must be moving to follow (0.0-1.0)
     */
    public HerdCohesionGoal(
            Mob mob,
            Class<? extends Mob> herdMateType,
            double speedModifier,
            int cohesionRadius,
            int minHerdSize,
            double maxDistanceFromHerd,
            float quorumThreshold) {
        this.mob = mob;
        this.herdMateType = herdMateType;
        this.speedModifier = speedModifier;
        this.cohesionRadius = cohesionRadius;
        this.minHerdSize = minHerdSize;
        this.maxDistanceFromHerd = maxDistanceFromHerd;
        this.quorumThreshold = Math.max(0.0f, Math.min(1.0f, quorumThreshold));
        this.checkCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Creates a new HerdCohesionGoal with default quorum threshold (0.47 based on bison research).
     *
     * @param mob the mob that will stay with the herd
     * @param herdMateType the class of entities to herd with
     * @param speedModifier movement speed multiplier
     * @param cohesionRadius radius to search for herd members
     * @param minHerdSize minimum nearby members to form a herd
     * @param maxDistanceFromHerd distance at which to rejoin herd
     */
    public HerdCohesionGoal(
            Mob mob,
            Class<? extends Mob> herdMateType,
            double speedModifier,
            int cohesionRadius,
            int minHerdSize,
            double maxDistanceFromHerd) {
        this(mob, herdMateType, speedModifier, cohesionRadius, minHerdSize, maxDistanceFromHerd, 0.47f);
    }

    /**
     * Creates a new HerdCohesionGoal with default parameters.
     * Cohesion radius: 20 blocks, min herd size: 2, max distance: 12 blocks, quorum: 0.47
     *
     * @param mob the mob that will stay with the herd
     * @param herdMateType the class of entities to herd with
     */
    public HerdCohesionGoal(Mob mob, Class<? extends Mob> herdMateType) {
        this(mob, herdMateType, 1.0, 20, 2, 12.0, 0.47f);
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

        // First priority: Check if we need to separate from crowding
        Mob tooClose = findTooCloseNeighbor(nearbyHerdMates);
        if (tooClose != null) {
            // Move away from the too-close neighbor
            Vec3 awayDirection = this.mob.position().subtract(tooClose.position()).normalize();
            if (awayDirection.length() < 0.1) {
                // If we're exactly on top, pick a random direction
                double angle = this.mob.getRandom().nextDouble() * Math.PI * 2;
                awayDirection = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            }
            this.targetPosition = this.mob.position().add(awayDirection.scale(MIN_SEPARATION_DISTANCE * 2));
            LOGGER.debug("{} is too close to {}, moving away for separation",
                this.mob.getName().getString(), tooClose.getName().getString());
            return true;
        }

        Vec3 herdCenter = calculateHerdCenter(nearbyHerdMates);
        double distanceToHerd = this.mob.position().distanceTo(herdCenter);

        if (distanceToHerd < this.maxDistanceFromHerd) {
            // Within acceptable distance, check if quorum is moving
            if (!isQuorumMoving(nearbyHerdMates)) {
                return false;
            }
        }

        this.targetPosition = calculateTargetPosition(nearbyHerdMates, herdCenter);

        LOGGER.debug("{} is {} blocks from herd, moving to maintain cohesion",
            this.mob.getName().getString(), String.format("%.1f", distanceToHerd));

        return true;
    }

    /**
     * Finds a herd mate that is too close (within minimum separation distance).
     *
     * @param herdMates list of herd members
     * @return the first herd mate that is too close, or null if none
     */
    private Mob findTooCloseNeighbor(List<? extends Mob> herdMates) {
        for (Mob mate : herdMates) {
            double distance = this.mob.distanceTo(mate);
            if (distance < MIN_SEPARATION_DISTANCE) {
                return mate;
            }
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cohesionTicks > 200) {
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
        this.cohesionTicks = 0;
        navigateToTarget();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped herd cohesion movement", this.mob.getName().getString());
        this.targetPosition = null;
        this.cohesionTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.cohesionTicks++;

        if (this.cohesionTicks % 20 == 0) {
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
        AABB searchBox = this.mob.getBoundingBox().inflate(this.cohesionRadius);
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
     * Checks if enough of the herd is moving to trigger quorum response.
     * Based on research showing bison herds wait for ~47% quorum before moving.
     *
     * @param herdMates list of herd members
     * @return true if quorum threshold is met
     */
    private boolean isQuorumMoving(List<? extends Mob> herdMates) {
        if (herdMates.isEmpty()) {
            return false;
        }

        int movingCount = 0;

        for (Mob mate : herdMates) {
            Vec3 deltaMovement = mate.getDeltaMovement();
            double speed = Math.sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z);

            if (speed > MOVEMENT_THRESHOLD) {
                movingCount++;
            }
        }

        float movingRatio = (float) movingCount / herdMates.size();
        boolean quorumMet = movingRatio >= this.quorumThreshold;

        if (quorumMet) {
            LOGGER.debug("{} detected quorum: {}/{} moving ({}%)",
                this.mob.getName().getString(),
                movingCount,
                herdMates.size(),
                String.format("%.0f", movingRatio * 100));
        }

        return quorumMet;
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
