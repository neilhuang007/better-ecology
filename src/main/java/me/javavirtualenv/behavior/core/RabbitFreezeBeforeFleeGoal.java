package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes rabbits freeze motionless when predators are detected at medium distance.
 * <p>
 * Based on research showing rabbits use "freeze camouflage" response when predators are first
 * detected at a distance. The rabbit remains completely motionless for 1.5-3 seconds, relying
 * on camouflage to avoid detection. If the predator approaches closer during the freeze,
 * the rabbit immediately abandons camouflage and flees.
 * <p>
 * Behavior:
 * <ul>
 *   <li>Activates when predator detected at 10-14 blocks (beyond immediate flee threshold)</li>
 *   <li>Rabbit freezes motionless for 1.5-3 seconds (30-60 ticks)</li>
 *   <li>If predator approaches within 8 blocks during freeze, immediately flee</li>
 *   <li>If predator doesn't approach, stay frozen then resume normal behavior</li>
 *   <li>20% chance to skip freeze and panic immediately (individual variation)</li>
 * </ul>
 */
public class RabbitFreezeBeforeFleeGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitFreezeBeforeFleeGoal.class);

    private static final int MIN_FREEZE_DISTANCE = 10;
    private static final int MAX_FREEZE_DISTANCE = 14;
    private static final int FLEE_ACTIVATION_DISTANCE = 8;
    private static final int MIN_FREEZE_DURATION = 30;
    private static final int MAX_FREEZE_DURATION = 60;
    private static final float PANIC_CHANCE = 0.2F;

    private final PathfinderMob mob;
    private final List<Class<? extends LivingEntity>> predatorTypes;
    private final TargetingConditions targetingConditions;

    @Nullable
    private LivingEntity detectedPredator;
    private int freezeTicks;
    private int freezeDuration;
    private boolean isPanicked;

    /**
     * Creates a new rabbit freeze before flee goal.
     *
     * @param mob the rabbit that will freeze
     * @param predatorTypes array of entity classes to freeze from
     */
    @SafeVarargs
    public RabbitFreezeBeforeFleeGoal(
            PathfinderMob mob,
            Class<? extends LivingEntity>... predatorTypes) {
        this.mob = mob;
        this.predatorTypes = List.of(predatorTypes);
        this.targetingConditions = TargetingConditions.forCombat()
                .range(MAX_FREEZE_DISTANCE);

        this.freezeTicks = 0;
        this.freezeDuration = 0;
        this.isPanicked = false;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        this.detectedPredator = findPredatorInFreezeRange();

        if (this.detectedPredator == null) {
            return false;
        }

        double distanceToPredator = this.mob.distanceTo(this.detectedPredator);

        if (distanceToPredator < MIN_FREEZE_DISTANCE || distanceToPredator > MAX_FREEZE_DISTANCE) {
            return false;
        }

        this.isPanicked = this.mob.getRandom().nextFloat() < PANIC_CHANCE;

        if (this.isPanicked) {
            LOGGER.debug("{} is too panicked to freeze, will flee immediately from {}",
                    mob.getName().getString(),
                    detectedPredator.getName().getString());
            return false;
        }

        LOGGER.debug("{} detected predator {} at freeze distance {}, will freeze",
                mob.getName().getString(),
                detectedPredator.getName().getString(),
                String.format("%.1f", distanceToPredator));

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.detectedPredator == null || !this.detectedPredator.isAlive()) {
            LOGGER.debug("{} stopped freezing - predator gone", mob.getName().getString());
            return false;
        }

        double distanceToPredator = this.mob.distanceTo(this.detectedPredator);

        if (distanceToPredator < FLEE_ACTIVATION_DISTANCE) {
            LOGGER.debug("{} predator too close ({} blocks), aborting freeze to flee",
                    mob.getName().getString(),
                    String.format("%.1f", distanceToPredator));
            return false;
        }

        if (this.freezeTicks >= this.freezeDuration) {
            LOGGER.debug("{} freeze duration complete, resuming normal behavior",
                    mob.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.freezeTicks = 0;
        this.freezeDuration = MIN_FREEZE_DURATION +
                this.mob.getRandom().nextInt(MAX_FREEZE_DURATION - MIN_FREEZE_DURATION + 1);

        this.mob.getNavigation().stop();

        LOGGER.debug("{} freezing for {} ticks to avoid detection by {}",
                mob.getName().getString(),
                freezeDuration,
                detectedPredator != null ? detectedPredator.getName().getString() : "unknown");
    }

    @Override
    public void stop() {
        if (this.detectedPredator != null) {
            double distanceToPredator = this.mob.distanceTo(this.detectedPredator);

            if (distanceToPredator < FLEE_ACTIVATION_DISTANCE) {
                LOGGER.debug("{} freeze interrupted by approaching predator at {} blocks",
                        mob.getName().getString(),
                        String.format("%.1f", distanceToPredator));
            }
        }

        this.detectedPredator = null;
        this.freezeTicks = 0;
        this.freezeDuration = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.freezeTicks++;

        if (this.detectedPredator != null) {
            this.mob.getLookControl().setLookAt(this.detectedPredator, 10.0F, 10.0F);
        }

        this.mob.getNavigation().stop();
        this.mob.setDeltaMovement(this.mob.getDeltaMovement().multiply(0.0, 1.0, 0.0));
    }

    /**
     * Finds predators within the freeze detection range (10-14 blocks).
     *
     * @return the nearest predator in range, or null if none found
     */
    @Nullable
    private LivingEntity findPredatorInFreezeRange() {
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Class<? extends LivingEntity> predatorType : this.predatorTypes) {
            List<? extends LivingEntity> nearbyPredators = this.mob.level()
                    .getEntitiesOfClass(
                            predatorType,
                            this.mob.getBoundingBox().inflate(MAX_FREEZE_DISTANCE, 3.0, MAX_FREEZE_DISTANCE)
                    );

            for (LivingEntity predator : nearbyPredators) {
                if (!this.targetingConditions.test(this.mob, predator)) {
                    continue;
                }

                double distSq = this.mob.distanceToSqr(predator);
                double minDistSq = MIN_FREEZE_DISTANCE * MIN_FREEZE_DISTANCE;
                double maxDistSq = MAX_FREEZE_DISTANCE * MAX_FREEZE_DISTANCE;

                if (distSq >= minDistSq && distSq <= maxDistSq && distSq < nearestDistSq) {
                    nearest = predator;
                    nearestDistSq = distSq;
                }
            }
        }

        return nearest;
    }
}
