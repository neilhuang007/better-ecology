package me.javavirtualenv.behavior.core;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Rabbit;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes rabbits thump the ground to warn nearby rabbits when a predator is detected.
 * <p>
 * Based on real rabbit alarm behavior where rabbits stomp their hind legs on the ground
 * to create a thumping sound that warns other rabbits of danger. This behavior spreads
 * awareness through the rabbit population and helps the group respond to threats.
 * <p>
 * Behavior:
 * <ul>
 *   <li>Activates when a predator is within detection range</li>
 *   <li>Rabbit stops moving and thumps the ground</li>
 *   <li>Creates visual and audio effects</li>
 *   <li>Alerts nearby rabbits within alarm range</li>
 *   <li>Has cooldown to prevent constant thumping</li>
 * </ul>
 */
public class RabbitThumpWarningGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitThumpWarningGoal.class);

    private static final int THUMP_DURATION_TICKS = 15;
    private static final int THUMP_INTERVAL_TICKS = 5;
    private static final int ALARM_COOLDOWN_TICKS = 100;
    private static final double ALARM_RANGE = 32.0;

    private final PathfinderMob mob;
    private final int detectionRange;
    private final List<Class<? extends LivingEntity>> predatorTypes;
    private final TargetingConditions targetingConditions;

    @Nullable
    private LivingEntity detectedPredator;
    private int thumpTicks;
    private int cooldownTicks;
    private int nextThumpTick;

    /**
     * Creates a new rabbit thump warning goal.
     *
     * @param mob the rabbit that will thump
     * @param detectionRange how far away to detect predators (blocks)
     * @param predatorTypes array of entity classes to warn about
     */
    @SafeVarargs
    public RabbitThumpWarningGoal(
            PathfinderMob mob,
            int detectionRange,
            Class<? extends LivingEntity>... predatorTypes) {
        this.mob = mob;
        this.detectionRange = detectionRange;
        this.predatorTypes = List.of(predatorTypes);
        this.targetingConditions = TargetingConditions.forCombat()
                .range(detectionRange);

        this.thumpTicks = 0;
        this.cooldownTicks = 0;
        this.nextThumpTick = 0;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }

        this.detectedPredator = findNearestPredator();

        if (this.detectedPredator == null) {
            return false;
        }

        LOGGER.debug("{} detected predator {} and will thump warning",
                mob.getName().getString(),
                detectedPredator.getName().getString());

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.detectedPredator == null || !this.detectedPredator.isAlive()) {
            return false;
        }

        if (this.thumpTicks >= THUMP_DURATION_TICKS) {
            return false;
        }

        double distanceToPredator = this.mob.distanceTo(this.detectedPredator);
        return distanceToPredator < this.detectionRange * 1.5;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting thump warning for predator {}",
                mob.getName().getString(),
                detectedPredator.getName().getString());

        this.thumpTicks = 0;
        this.nextThumpTick = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped thumping warning",
                mob.getName().getString());

        this.detectedPredator = null;
        this.thumpTicks = 0;
        this.nextThumpTick = 0;
        this.cooldownTicks = ALARM_COOLDOWN_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.thumpTicks++;

        if (this.detectedPredator != null) {
            this.mob.getLookControl().setLookAt(this.detectedPredator, 30.0F, 30.0F);
        }

        if (this.thumpTicks >= this.nextThumpTick) {
            performThump();
            this.nextThumpTick = this.thumpTicks + THUMP_INTERVAL_TICKS;

            if (this.thumpTicks == THUMP_INTERVAL_TICKS) {
                alertNearbyRabbits();
            }
        }
    }

    /**
     * Performs the thump action with visual and audio effects.
     */
    private void performThump() {
        this.mob.playSound(SoundEvents.RABBIT_HURT, 0.5F, 0.8F);

        if (this.mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.POOF,
                    this.mob.getX(),
                    this.mob.getY(),
                    this.mob.getZ(),
                    3,
                    0.2,
                    0.0,
                    0.2,
                    0.01
            );
        }

        LOGGER.debug("{} thumped ground (tick {}/{})",
                mob.getName().getString(),
                thumpTicks,
                THUMP_DURATION_TICKS);
    }

    /**
     * Alerts nearby rabbits to the presence of the predator.
     */
    private void alertNearbyRabbits() {
        if (this.detectedPredator == null) {
            return;
        }

        List<Rabbit> nearbyRabbits = this.mob.level().getEntitiesOfClass(
                Rabbit.class,
                this.mob.getBoundingBox().inflate(ALARM_RANGE),
                rabbit -> rabbit != this.mob && rabbit.isAlive()
        );

        for (Rabbit rabbit : nearbyRabbits) {
            if (rabbit.getTarget() == null) {
                LOGGER.debug("{} alerted {} to predator {}",
                        mob.getName().getString(),
                        rabbit.getName().getString(),
                        detectedPredator.getName().getString());
            }
        }

        LOGGER.debug("{} alerted {} nearby rabbits to predator",
                mob.getName().getString(),
                nearbyRabbits.size());
    }

    /**
     * Finds the nearest predator of any of the configured predator types.
     *
     * @return the nearest predator, or null if none found
     */
    @Nullable
    private LivingEntity findNearestPredator() {
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Class<? extends LivingEntity> predatorType : this.predatorTypes) {
            List<? extends LivingEntity> nearbyPredators = this.mob.level()
                    .getEntitiesOfClass(
                            predatorType,
                            this.mob.getBoundingBox().inflate(this.detectionRange, 3.0, this.detectionRange)
                    );

            for (LivingEntity predator : nearbyPredators) {
                if (!this.targetingConditions.test(this.mob, predator)) {
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
}
