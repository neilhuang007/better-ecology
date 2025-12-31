package me.javavirtualenv.behavior.camel;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Spitting defense behavior for camels.
 * <p>
 * Camels spit at threats when provoked, with the following characteristics:
 * - Accurate aim up to 10 blocks
 * - Warning animation before firing (head tilt, mouth opening)
 * - Spit causes minor damage and slow effect
 * - Cooldown between spits to prevent spam
 * <p>
 * Scientific basis: Camels use spitting as a defense mechanism when threatened,
 * projecting partially digested stomach contents with remarkable accuracy.
 */
public class SpittingDefenseBehavior extends SteeringBehavior {

    private final CamelConfig config;

    // Spitting state
    private boolean isSpitting = false;
    private int windupTicks = 0;
    private int cooldownTicks = 0;
    private UUID targetId = null;
    private Vec3d targetPosition = null;

    public SpittingDefenseBehavior(CamelConfig config) {
        this.config = config;
    }

    public SpittingDefenseBehavior() {
        this(CamelConfig.createDefault());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!enabled) {
            return new Vec3d();
        }

        // Update cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return new Vec3d();
        }

        // If currently spitting, continue the windup
        if (isSpitting) {
            return handleSpittingState(context);
        }

        // Check for threats to spit at
        LivingEntity threat = detectThreat(context);
        if (threat != null && shouldSpit(context, threat)) {
            startSpitting(context, threat);
        }

        return new Vec3d();
    }

    /**
     * Handles the spitting state, including windup and projectile launch.
     */
    private Vec3d handleSpittingState(BehaviorContext context) {
        windupTicks--;

        // Spawn warning particles during windup
        if (windupTicks > 0 && windupTicks % 5 == 0) {
            spawnWarningParticles(context);
        }

        // Fire spit when windup completes
        if (windupTicks <= 0) {
            fireSpit(context);
            isSpitting = false;
            cooldownTicks = config.getSpittingCooldown();
            targetId = null;
            targetPosition = null;
        }

        return new Vec3d();
    }

    /**
     * Starts the spitting sequence with warning animation.
     */
    private void startSpitting(BehaviorContext context, LivingEntity threat) {
        isSpitting = true;
        windupTicks = config.getSpittingWindupTicks();
        targetId = threat.getUUID();
        targetPosition = new Vec3d(threat.getX(), threat.getY(), threat.getZ());

        // Play warning sound
        Level level = context.getLevel();
        if (!level.isClientSide) {
            level.playSound(null, context.getBlockPos(),
                SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE,
                1.0f, 1.0f);
        }
    }

    /**
     * Fires the spit projectile at the target.
     */
    private void fireSpit(BehaviorContext context) {
        if (targetPosition == null) {
            return;
        }

        Level level = context.getLevel();
        Vec3 entityPos = new Vec3(context.getPosition().x,
            context.getPosition().y + 1.5, // Eye level
            context.getPosition().z);

        // Calculate direction to target with accuracy variation
        Vec3 direction = targetPosition.toMinecraftVec3()
            .subtract(entityPos)
            .normalize();

        // Apply accuracy variation
        double accuracy = config.getSpittingAccuracy();
        double variation = (1.0 - accuracy) * 0.5;
        direction = direction.add(
            (Math.random() - 0.5) * variation,
            (Math.random() - 0.5) * variation * 0.5,
            (Math.random() - 0.5) * variation
        ).normalize();

        // Create and launch spit projectile
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            CamelSpitEntity spit = new CamelSpitEntity(level, context.getEntity());
            spit.setPos(entityPos);
            spit.shoot(direction.x, direction.y, direction.z, 0.8f, 0.1f);

            // Apply damage and slow effect on hit
            spit.setDamage(config.getSpittingDamage());
            spit.setSlowDuration(config.getSpittingSlowDuration());

            serverLevel.addFreshEntity(spit);

            // Play spit sound
            level.playSound(null, context.getBlockPos(),
                SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE,
                1.2f, 0.8f);
        }
    }

    /**
     * Spawns warning particles during windup phase.
     */
    private void spawnWarningParticles(BehaviorContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return;
        }

        Vec3d pos = context.getPosition();
        for (int i = 0; i < 3; i++) {
            double offsetX = (Math.random() - 0.5) * 0.3;
            double offsetY = (Math.random() - 0.5) * 0.3 + 1.5;
            double offsetZ = (Math.random() - 0.5) * 0.3;

            ((ServerLevel) level).sendParticles(
                ParticleTypes.SPIT,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                1, 0, 0, 0, 0.02
            );
        }
    }

    /**
     * Detects potential threats to spit at.
     */
    private LivingEntity detectThreat(BehaviorContext context) {
        Level level = context.getLevel();
        double range = config.getSpittingRange();

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(
                context.getPosition().x - range,
                context.getPosition().y - range,
                context.getPosition().z - range,
                context.getPosition().x + range,
                context.getPosition().y + range,
                context.getPosition().z + range
            )
        );

        LivingEntity closestThreat = null;
        double closestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : nearbyEntities) {
            if (isThreat(context, entity)) {
                Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                double distance = context.getPosition().distanceTo(entityPos);

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestThreat = entity;
                }
            }
        }

        return closestThreat;
    }

    /**
     * Determines if camel should spit at this entity.
     */
    private boolean shouldSpit(BehaviorContext context, LivingEntity threat) {
        // Don't spit if already spitting at this target
        if (targetId != null && targetId.equals(threat.getUUID())) {
            return false;
        }

        // Check if threat is in range
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        double distance = context.getPosition().distanceTo(threatPos);
        if (distance > config.getSpittingRange()) {
            return false;
        }

        // Only spit if provoked (threat is close and aggressive)
        return isProvoking(context, threat);
    }

    /**
     * Determines if an entity is a threat.
     */
    private boolean isThreat(BehaviorContext context, LivingEntity entity) {
        // Players are threats if not sneaking
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && !player.isShiftKeyDown();
        }

        // Hostile mobs are threats
        String typeName = entity.getType().toString().toLowerCase();
        if (typeName.contains("zombie") || typeName.contains("skeleton") ||
            typeName.contains("spider") || typeName.contains("creeper") ||
            typeName.contains("phantom") || typeName.contains("drowned") ||
            typeName.contains("pillager") || typeName.contains("vindicator")) {
            return true;
        }

        // Predators are threats
        if (typeName.contains("wolf") || typeName.contains("fox") ||
            typeName.contains("cat") || typeName.contains("bee")) {
            return true;
        }

        return false;
    }

    /**
     * Determines if a threat is actively provoking the camel.
     */
    private boolean isProvoking(BehaviorContext context, LivingEntity threat) {
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        double distance = context.getPosition().distanceTo(threatPos);

        // Very close threats always provoke
        if (distance < 4.0) {
            return true;
        }

        // Players provoke if not sneaking and within range
        if (threat instanceof Player player) {
            return !player.isShiftKeyDown() && distance < config.getSpittingRange() * 0.8;
        }

        // Aggressive mobs provoke if targeting this camel
        if (threat instanceof Mob mob && mob.isAggressive()) {
            return mob.getTarget() == context.getEntity();
        }

        return false;
    }

    // Getters for external query
    public boolean isSpitting() {
        return isSpitting;
    }

    public int getWindupTicks() {
        return windupTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    /**
     * Force start spitting (for testing or triggered events).
     */
    public void forceSpit(BehaviorContext context, LivingEntity target) {
        if (!isSpitting && cooldownTicks <= 0) {
            startSpitting(context, target);
        }
    }
}
