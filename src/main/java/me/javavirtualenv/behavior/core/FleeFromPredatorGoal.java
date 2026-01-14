package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes prey animals flee from nearby predators.
 * <p>
 * This goal scans for predators within a detection range and causes the mob to flee
 * at increased speed when a predator is detected. The mob continues fleeing until
 * the predator is beyond the flee distance threshold.
 * <p>
 * Priority is set to {@link AnimalThresholds#PRIORITY_FLEE} to override other goals
 * during active predator encounters.
 */
public class FleeFromPredatorGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleeFromPredatorGoal.class);

    protected final PathfinderMob mob;
    protected final double speedModifier;
    protected final int detectionRange;
    protected final int fleeDistance;
    protected final List<Class<? extends LivingEntity>> predatorTypes;
    protected final PathNavigation pathNav;
    protected final TargetingConditions targetingConditions;

    @Nullable
    protected LivingEntity nearestPredator;
    @Nullable
    protected Path escapePath;

    /**
     * Creates a new flee from predator goal.
     *
     * @param mob the mob that will flee
     * @param speedModifier speed multiplier when fleeing (1.0 = normal speed, 1.5 = 150% speed)
     * @param detectionRange how far away to detect predators (blocks)
     * @param fleeDistance how far to maintain distance from predators (blocks)
     * @param predatorTypes array of entity classes to flee from
     */
    @SafeVarargs
    public FleeFromPredatorGoal(
            PathfinderMob mob,
            double speedModifier,
            int detectionRange,
            int fleeDistance,
            Class<? extends LivingEntity>... predatorTypes) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.detectionRange = detectionRange;
        this.fleeDistance = fleeDistance;
        this.predatorTypes = List.of(predatorTypes);
        this.pathNav = mob.getNavigation();

        this.targetingConditions = TargetingConditions.forCombat()
                .range(detectionRange);

        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.nearestPredator = findNearestPredator();

        if (this.nearestPredator == null) {
            LOGGER.debug("{} canUse: no predator found within range {}",
                    mob.getName().getString(), detectionRange);
            return false;
        }

        LOGGER.debug("{} canUse: found predator {} at distance {}",
                mob.getName().getString(),
                nearestPredator.getName().getString(),
                String.format("%.1f", mob.distanceTo(nearestPredator)));

        Vec3 escapePosition = calculateEscapePosition();
        if (escapePosition == null) {
            LOGGER.debug("{} canUse: Could not find escape position fleeing from {}",
                    mob.getName().getString(), nearestPredator.getName().getString());
            return false;
        }

        if (!isEscapePositionBetter(escapePosition)) {
            LOGGER.debug("{} canUse: escape position not better than current",
                    mob.getName().getString());
            return false;
        }

        this.escapePath = this.pathNav.createPath(escapePosition.x, escapePosition.y, escapePosition.z, 0);
        boolean canFlee = this.escapePath != null;

        if (canFlee) {
            LOGGER.debug("{} fleeing from {} at distance {}",
                    mob.getName().getString(),
                    nearestPredator.getName().getString(),
                    String.format("%.1f", mob.distanceTo(nearestPredator)));
        } else {
            LOGGER.debug("{} canUse: could not create path to escape position {}",
                    mob.getName().getString(), escapePosition);
        }

        return canFlee;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.nearestPredator == null || !this.nearestPredator.isAlive()) {
            return false;
        }

        double distanceToPredator = this.mob.distanceTo(this.nearestPredator);
        boolean stillInDanger = distanceToPredator < this.fleeDistance;

        if (!stillInDanger) {
            LOGGER.debug("{} stopped fleeing - safe distance reached ({} blocks)",
                    mob.getName().getString(),
                    String.format("%.1f", distanceToPredator));
        }

        return stillInDanger;
    }

    @Override
    public void start() {
        if (this.escapePath != null) {
            this.pathNav.moveTo(this.escapePath, this.speedModifier);
            LOGGER.debug("{} started fleeing at {}x speed",
                    mob.getName().getString(),
                    speedModifier);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        if (this.nearestPredator != null) {
            LOGGER.debug("{} stopped fleeing from {}",
                    mob.getName().getString(),
                    nearestPredator.getName().getString());
        }
        this.nearestPredator = null;
        this.escapePath = null;
    }

    @Override
    public void tick() {
        if (this.nearestPredator == null) {
            return;
        }

        double distanceSq = this.mob.distanceToSqr(this.nearestPredator);
        double criticalDistanceSq = 49.0;

        if (distanceSq < criticalDistanceSq) {
            this.mob.getNavigation().setSpeedModifier(this.speedModifier * 1.2);
        } else {
            this.mob.getNavigation().setSpeedModifier(this.speedModifier);
        }

        // Recalculate escape path if navigation stopped or stuck
        if (this.pathNav.isDone() && this.mob.distanceTo(this.nearestPredator) < this.fleeDistance) {
            Vec3 newEscape = calculateEscapePosition();
            if (newEscape != null) {
                this.escapePath = this.pathNav.createPath(newEscape.x, newEscape.y, newEscape.z, 0);
                if (this.escapePath != null) {
                    this.pathNav.moveTo(this.escapePath, this.speedModifier);
                }
            }
        }
    }

    /**
     * Finds the nearest predator of any of the configured predator types.
     *
     * @return the nearest predator, or null if none found
     */
    @Nullable
    protected LivingEntity findNearestPredator() {
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Class<? extends LivingEntity> predatorType : this.predatorTypes) {
            List<? extends LivingEntity> nearbyPredators = this.mob.level()
                    .getEntitiesOfClass(
                            predatorType,
                            this.mob.getBoundingBox().inflate(this.detectionRange, 3.0, this.detectionRange)
                    );

            LOGGER.debug("{} findNearestPredator: found {} {} within detection range",
                    mob.getName().getString(), nearbyPredators.size(), predatorType.getSimpleName());

            for (LivingEntity predator : nearbyPredators) {
                boolean passesTargeting = this.targetingConditions.test(this.mob, predator);
                LOGGER.debug("{} findNearestPredator: testing {} - passesTargeting: {}",
                        mob.getName().getString(), predator.getName().getString(), passesTargeting);

                if (!passesTargeting) {
                    continue;
                }

                double distSq = this.mob.distanceToSqr(predator);
                if (distSq < nearestDistSq) {
                    nearest = predator;
                    nearestDistSq = distSq;
                }
            }
        }

        return nearest;
    }

    /**
     * Calculates an escape position away from the predator.
     *
     * @return escape position, or null if none found
     */
    @Nullable
    protected Vec3 calculateEscapePosition() {
        if (this.nearestPredator == null) {
            return null;
        }

        return DefaultRandomPos.getPosAway(
                this.mob,
                16,
                7,
                this.nearestPredator.position()
        );
    }

    /**
     * Checks if the escape position is actually farther from the predator than current position.
     *
     * @param escapePosition the proposed escape position
     * @return true if the escape position is better than staying put
     */
    protected boolean isEscapePositionBetter(Vec3 escapePosition) {
        if (this.nearestPredator == null) {
            return false;
        }

        double currentDistSq = this.nearestPredator.distanceToSqr(this.mob);
        double escapeDistSq = this.nearestPredator.distanceToSqr(escapePosition.x, escapePosition.y, escapePosition.z);

        return escapeDistSq > currentDistSq;
    }
}
