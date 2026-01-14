package me.javavirtualenv.behavior.core;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes squid and glow squid eject ink clouds as defense when threatened.
 * <p>
 * Based on real squid defense behavior where squid eject ink clouds to create visual
 * obstruction while escaping from predators. The ink cloud blinds nearby entities
 * while the squid gains a speed boost to escape.
 * <p>
 * Behavior:
 * <ul>
 *   <li>Activates when health drops below 50% OR predator is within 4 blocks</li>
 *   <li>Creates ink particle cloud (3x3x3 area) using squid ink particles</li>
 *   <li>Cloud lasts 8-12 seconds (visual particles)</li>
 *   <li>Applies Blindness I effect to entities in cloud for 3 seconds</li>
 *   <li>Squid gains Speed II boost during ink duration</li>
 *   <li>Cooldown: 20-30 seconds between uses</li>
 * </ul>
 */
public class SquidInkCloudDefenseGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(SquidInkCloudDefenseGoal.class);

    private static final float HEALTH_THRESHOLD = 0.5f;
    private static final double PREDATOR_PROXIMITY_THRESHOLD = 4.0;
    private static final int CLOUD_SIZE = 3;
    private static final int CLOUD_DURATION_MIN = 160;
    private static final int CLOUD_DURATION_MAX = 240;
    private static final int BLINDNESS_DURATION_TICKS = 60;
    private static final int SPEED_BOOST_DURATION_TICKS = 200;
    private static final int COOLDOWN_MIN = 400;
    private static final int COOLDOWN_MAX = 600;
    private static final int PARTICLE_SPAWN_INTERVAL = 5;

    private final PathfinderMob mob;
    private final int detectionRange;
    private final List<Class<? extends LivingEntity>> predatorTypes;
    private final TargetingConditions targetingConditions;

    @Nullable
    private LivingEntity detectedPredator;
    private int cooldownTicks;
    private int inkCloudTicks;
    private int cloudDuration;

    /**
     * Creates a new squid ink cloud defense goal.
     *
     * @param mob the squid that will eject ink
     * @param detectionRange how far away to detect predators (blocks)
     * @param predatorTypes array of entity classes to defend against
     */
    @SafeVarargs
    public SquidInkCloudDefenseGoal(
            PathfinderMob mob,
            int detectionRange,
            Class<? extends LivingEntity>... predatorTypes) {
        this.mob = mob;
        this.detectionRange = detectionRange;
        this.predatorTypes = List.of(predatorTypes);
        this.targetingConditions = TargetingConditions.forCombat()
                .range(detectionRange);

        this.cooldownTicks = 0;
        this.inkCloudTicks = 0;
        this.cloudDuration = 0;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }

        boolean isLowHealth = isHealthBelowThreshold();
        this.detectedPredator = findNearbyPredator();
        boolean isPredatorClose = this.detectedPredator != null;

        if (!isLowHealth && !isPredatorClose) {
            return false;
        }

        LOGGER.debug("{} activating ink cloud defense (lowHealth: {}, predatorNearby: {})",
                mob.getName().getString(),
                isLowHealth,
                isPredatorClose);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.inkCloudTicks < this.cloudDuration;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting ink cloud defense",
                mob.getName().getString());

        this.inkCloudTicks = 0;
        this.cloudDuration = CLOUD_DURATION_MIN +
                this.mob.getRandom().nextInt(CLOUD_DURATION_MAX - CLOUD_DURATION_MIN);

        applySpeedBoost();
        createInkCloud();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped ink cloud defense",
                mob.getName().getString());

        this.detectedPredator = null;
        this.inkCloudTicks = 0;
        this.cloudDuration = 0;
        this.cooldownTicks = COOLDOWN_MIN +
                this.mob.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.inkCloudTicks++;

        if (this.inkCloudTicks % PARTICLE_SPAWN_INTERVAL == 0) {
            spawnInkParticles();
        }

        if (this.inkCloudTicks % 20 == 0) {
            applyBlindnessToNearbyEntities();
        }
    }

    /**
     * Checks if the squid's health is below the threshold.
     *
     * @return true if health is below 50%
     */
    private boolean isHealthBelowThreshold() {
        float healthRatio = this.mob.getHealth() / this.mob.getMaxHealth();
        return healthRatio < HEALTH_THRESHOLD;
    }

    /**
     * Finds a nearby predator within proximity threshold.
     *
     * @return the nearest predator within 4 blocks, or null if none found
     */
    @Nullable
    private LivingEntity findNearbyPredator() {
        LivingEntity nearest = null;
        double nearestDistSq = PREDATOR_PROXIMITY_THRESHOLD * PREDATOR_PROXIMITY_THRESHOLD;

        for (Class<? extends LivingEntity> predatorType : this.predatorTypes) {
            List<? extends LivingEntity> nearbyPredators = this.mob.level()
                    .getEntitiesOfClass(
                            predatorType,
                            this.mob.getBoundingBox().inflate(PREDATOR_PROXIMITY_THRESHOLD)
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

    /**
     * Applies Speed II boost to the squid for escape.
     */
    private void applySpeedBoost() {
        MobEffectInstance speedEffect = new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                SPEED_BOOST_DURATION_TICKS,
                1,
                false,
                false
        );
        this.mob.addEffect(speedEffect);

        LOGGER.debug("{} gained Speed II boost for escape",
                mob.getName().getString());
    }

    /**
     * Creates the initial ink cloud explosion effect.
     */
    private void createInkCloud() {
        if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double cloudRadius = CLOUD_SIZE / 2.0;

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.mob.getX(),
                this.mob.getY() + 0.5,
                this.mob.getZ(),
                50,
                cloudRadius,
                cloudRadius,
                cloudRadius,
                0.05
        );

        LOGGER.debug("{} created initial ink cloud explosion",
                mob.getName().getString());
    }

    /**
     * Spawns ink particles to maintain the cloud effect.
     */
    private void spawnInkParticles() {
        if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double cloudRadius = CLOUD_SIZE / 2.0;

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.mob.getX(),
                this.mob.getY() + 0.5,
                this.mob.getZ(),
                8,
                cloudRadius,
                cloudRadius,
                cloudRadius,
                0.02
        );
    }

    /**
     * Applies Blindness I effect to entities within the ink cloud.
     */
    private void applyBlindnessToNearbyEntities() {
        double cloudRadius = CLOUD_SIZE / 2.0;
        AABB cloudArea = new AABB(
                this.mob.getX() - cloudRadius,
                this.mob.getY() - cloudRadius,
                this.mob.getZ() - cloudRadius,
                this.mob.getX() + cloudRadius,
                this.mob.getY() + cloudRadius,
                this.mob.getZ() + cloudRadius
        );

        List<LivingEntity> entitiesInCloud = this.mob.level().getEntitiesOfClass(
                LivingEntity.class,
                cloudArea,
                entity -> entity != this.mob && entity.isAlive()
        );

        for (LivingEntity entity : entitiesInCloud) {
            MobEffectInstance blindnessEffect = new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    BLINDNESS_DURATION_TICKS,
                    0,
                    false,
                    true
            );
            entity.addEffect(blindnessEffect);

            LOGGER.debug("{} applied blindness to {} in ink cloud",
                    mob.getName().getString(),
                    entity.getName().getString());
        }
    }
}
