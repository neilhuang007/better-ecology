package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced flee goal that makes rabbits use zigzag (protean) movement patterns when fleeing.
 * <p>
 * Based on research showing that zigzagging/unpredictable paths are an effective escape strategy.
 * This "protean movement" creates adaptively unpredictable behavior that makes it harder for
 * predators to intercept fleeing prey. Rabbits are particularly known for this behavior.
 * <p>
 * Behavior:
 * <ul>
 *   <li>Extends the standard FleeFromPredatorGoal with zigzag patterns</li>
 *   <li>Adds perpendicular movement components to create unpredictable paths</li>
 *   <li>Intensity varies based on distance to predator (more erratic when closer)</li>
 *   <li>Balances between escape direction and unpredictability</li>
 * </ul>
 */
public class RabbitZigzagFleeGoal extends FleeFromPredatorGoal {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitZigzagFleeGoal.class);

    private static final double BASE_ZIGZAG_INTENSITY = 0.4;
    private static final double CLOSE_RANGE_INTENSITY_MULTIPLIER = 1.8;
    private static final double CLOSE_RANGE_THRESHOLD = 7.0;
    private static final int DIRECTION_CHANGE_INTERVAL = 8;

    private int ticksSinceFlee;
    private int zigzagDirection;

    /**
     * Creates a new rabbit zigzag flee goal.
     *
     * @param mob the rabbit that will flee
     * @param speedModifier speed multiplier when fleeing
     * @param detectionRange how far away to detect predators (blocks)
     * @param fleeDistance how far to maintain distance from predators (blocks)
     * @param predatorTypes array of entity classes to flee from
     */
    @SafeVarargs
    public RabbitZigzagFleeGoal(
            PathfinderMob mob,
            double speedModifier,
            int detectionRange,
            int fleeDistance,
            Class<? extends LivingEntity>... predatorTypes) {
        super(mob, speedModifier, detectionRange, fleeDistance, predatorTypes);
        this.ticksSinceFlee = 0;
        this.zigzagDirection = 1;
    }

    @Override
    public void start() {
        super.start();
        this.ticksSinceFlee = 0;
        this.zigzagDirection = this.mob.getRandom().nextBoolean() ? 1 : -1;

        LOGGER.debug("{} started zigzag fleeing with initial direction {}",
                mob.getName().getString(),
                zigzagDirection);

        AnimalAnimations.playStartledJump(this.mob);
    }

    @Override
    public void tick() {
        this.ticksSinceFlee++;

        if (this.nearestPredator == null || !this.nearestPredator.isAlive()) {
            super.tick();
            return;
        }

        this.mob.getLookControl().setLookAt(this.nearestPredator, 30.0F, 30.0F);

        double distanceToPredator = this.mob.distanceTo(this.nearestPredator);

        if (this.ticksSinceFlee % DIRECTION_CHANGE_INTERVAL == 0) {
            this.zigzagDirection *= -1;
            navigateWithZigzag(distanceToPredator);
        }

        double speedModifier = calculateSpeedModifier(distanceToPredator);
        this.mob.getNavigation().setSpeedModifier(speedModifier);

        AnimalAnimations.spawnFleeingDustParticles(this.mob, this.ticksSinceFlee);
        AnimalAnimations.playDistressSound(this.mob, this.ticksSinceFlee);
        AnimalAnimations.applyFleeingLookBack(this.mob, this.nearestPredator, this.ticksSinceFlee);
    }

    /**
     * Navigates with a zigzag pattern away from the predator.
     *
     * @param distanceToPredator current distance to the predator
     */
    private void navigateWithZigzag(double distanceToPredator) {
        if (this.nearestPredator == null) {
            return;
        }

        Vec3 escapePosition = calculateZigzagEscapePosition(distanceToPredator);

        if (escapePosition != null) {
            PathNavigation pathNav = this.mob.getNavigation();
            this.escapePath = pathNav.createPath(escapePosition.x, escapePosition.y, escapePosition.z, 0);

            if (this.escapePath != null) {
                pathNav.moveTo(this.escapePath, this.speedModifier);

                LOGGER.debug("{} navigating zigzag to ({}, {}, {}) away from predator at distance {}",
                        mob.getName().getString(),
                        String.format("%.1f", escapePosition.x),
                        String.format("%.1f", escapePosition.y),
                        String.format("%.1f", escapePosition.z),
                        String.format("%.1f", distanceToPredator));
            }
        }
    }

    /**
     * Calculates a zigzag escape position with unpredictable movement.
     *
     * @param distanceToPredator current distance to the predator
     * @return escape position with zigzag applied, or null if none found
     */
    @Nullable
    private Vec3 calculateZigzagEscapePosition(double distanceToPredator) {
        if (this.nearestPredator == null) {
            return null;
        }

        Vec3 mobPos = this.mob.position();
        Vec3 predatorPos = this.nearestPredator.position();

        Vec3 awayFromPredator = mobPos.subtract(predatorPos).normalize();

        Vec3 perpendicular = new Vec3(-awayFromPredator.z, 0, awayFromPredator.x);

        double intensity = calculateZigzagIntensity(distanceToPredator);
        Vec3 zigzagOffset = perpendicular.scale(intensity * this.zigzagDirection);

        Vec3 combinedDirection = awayFromPredator.add(zigzagOffset).normalize();

        double escapeDistance = 8.0;
        Vec3 targetPos = mobPos.add(combinedDirection.scale(escapeDistance));

        return targetPos;
    }

    /**
     * Calculates the intensity of the zigzag based on distance to predator.
     * Closer predators result in more erratic zigzag patterns.
     *
     * @param distanceToPredator current distance to the predator
     * @return zigzag intensity multiplier
     */
    private double calculateZigzagIntensity(double distanceToPredator) {
        if (distanceToPredator < CLOSE_RANGE_THRESHOLD) {
            return BASE_ZIGZAG_INTENSITY * CLOSE_RANGE_INTENSITY_MULTIPLIER;
        }
        return BASE_ZIGZAG_INTENSITY;
    }

    /**
     * Calculates speed modifier based on distance to predator.
     * Rabbits run faster when predators are closer.
     *
     * @param distanceToPredator current distance to the predator
     * @return speed modifier
     */
    private double calculateSpeedModifier(double distanceToPredator) {
        if (distanceToPredator < CLOSE_RANGE_THRESHOLD) {
            return this.speedModifier * 1.3;
        }
        return this.speedModifier;
    }

    @Override
    protected Vec3 calculateEscapePosition() {
        if (this.nearestPredator == null) {
            return null;
        }

        double distanceToPredator = this.mob.distanceTo(this.nearestPredator);
        return calculateZigzagEscapePosition(distanceToPredator);
    }
}
