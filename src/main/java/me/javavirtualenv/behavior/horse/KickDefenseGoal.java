package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal for horse kick defense behavior.
 * Horses will kick behind them when threatened from the rear.
 */
public class KickDefenseGoal extends Goal {
    private final AbstractHorse horse;
    private final KickConfig config;
    private LivingEntity kickTarget;
    private int kickWarmupTicks;
    private int kickCooldownTicks;
    private boolean isDonkey;
    private boolean isMule;

    public KickDefenseGoal(AbstractHorse horse, KickConfig config) {
        this.horse = horse;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        // Determine horse type for kick strength
        EntityType<?> type = horse.getType();
        this.isDonkey = type == EntityType.DONKEY;
        this.isMule = type == EntityType.MULE;
    }

    @Override
    public boolean canUse() {
        if (kickCooldownTicks > 0) {
            kickCooldownTicks--;
            return false;
        }

        if (!horse.isAlive()) {
            return false;
        }

        // Cannot kick while being ridden
        if (horse.isVehicle()) {
            return false;
        }

        // Cannot kick while rearing
        if (horse.isStepping()) {
            return false;
        }

        // Find threats from behind
        kickTarget = findRearThreat();
        return kickTarget != null && kickTarget.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (kickTarget == null || !kickTarget.isAlive()) {
            return false;
        }

        if (horse.isVehicle()) {
            return false;
        }

        return kickWarmupTicks < config.kickWarmupDuration;
    }

    @Override
    public void start() {
        kickWarmupTicks = 0;
        // Stop movement when preparing to kick
        horse.getNavigation().stop();
    }

    @Override
    public void stop() {
        kickTarget = null;
        kickWarmupTicks = 0;
        kickCooldownTicks = config.kickCooldown;
    }

    @Override
    public void tick() {
        if (kickTarget == null) {
            return;
        }

        // Look at the target
        horse.getLookControl().setLookAt(kickTarget, 30.0f, 30.0f);

        kickWarmupTicks++;

        // Check if target is still behind and in range
        double distance = horse.distanceToSqr(kickTarget);
        if (!isTargetBehind(kickTarget) || distance > config.kickRange * config.kickRange) {
            stop();
            return;
        }

        // Execute kick when warmup completes
        if (kickWarmupTicks >= config.kickWarmupDuration) {
            executeKick();
            stop();
        }
    }

    private LivingEntity findRearThreat() {
        Vec3 horsePos = horse.position();
        Vec3 horseLook = horse.getLookAngle();

        // Check for entities behind the horse
        AABB searchArea = new AABB(
            horsePos.x - config.detectionRange,
            horsePos.y - 1,
            horsePos.z - config.detectionRange,
            horsePos.x + config.detectionRange,
            horsePos.y + 2,
            horsePos.z + config.detectionRange
        );

        List<LivingEntity> nearbyEntities = horse.level().getEntitiesOfClass(
            LivingEntity.class,
            searchArea,
            entity -> entity != horse && entity.isAlive() && isThreat(entity)
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity entity : nearbyEntities) {
            if (isTargetBehind(entity)) {
                double distance = horse.distanceToSqr(entity);
                // Prefer closer targets
                double score = distance;
                if (score < bestScore) {
                    bestScore = score;
                    bestTarget = entity;
                }
            }
        }

        return bestTarget;
    }

    private boolean isTargetBehind(Entity target) {
        Vec3 toTarget = target.position().subtract(horse.position()).normalize();
        Vec3 horseLook = horse.getLookAngle();

        // Target is behind if dot product is negative
        double dot = toTarget.dot(horseLook);
        return dot < -0.3; // Target is mostly behind
    }

    private boolean isThreat(Entity entity) {
        // Players are threats if they're attacking
        if (entity.getType() == EntityType.PLAYER) {
            return horse.getLastHurtByMob() == entity ||
                   horse.getTarget() == entity;
        }

        // Predators are always threats
        if (entity.getType() == EntityType.WOLF ||
            entity.getType() == EntityType.POLAR_BEAR ||
            entity.getType() == EntityType.ZOMBIE ||
            entity.getType() == EntityType.DROWNED ||
            entity.getType() == EntityType.VINDICATOR ||
            entity.getType() == EntityType.EVOKER ||
            entity.getType() == EntityType.PILLAGER ||
            entity.getType() == EntityType.RAVAGER) {
            return true;
        }

        // Hostile to this horse
        return horse.getLastHurtByMob() == entity;
    }

    private void executeKick() {
        Level level = horse.level();

        // Play kick sound
        level.playSound(null, horse.blockPosition(),
            isDonkey ? net.minecraft.sounds.SoundEvents.DONKEY_CHEST :
            isMule ? net.minecraft.sounds.SoundEvents.DONKEY_CHEST :
            net.minecraft.sounds.SoundEvents.HORSE_GALLOP,
            net.minecraft.sounds.SoundSource.HOSTILE,
            1.0f, 1.0f
        );

        // Calculate kick damage
        double baseDamage = config.baseKickDamage;
        if (isDonkey) {
            baseDamage *= 1.3; // Donkeys kick harder
        } else if (isMule) {
            baseDamage *= 1.5; // Mules kick hardest
        }

        // Apply knockback and damage
        Vec3 kickDirection = horse.position().subtract(kickTarget.position()).normalize();
        Vec3 kickVelocity = kickDirection.scale(config.knockbackStrength);

        kickTarget.hurt(level.damageSources().mobAttack(horse), (float) baseDamage);
        kickTarget.setDeltaMovement(kickTarget.getDeltaMovement().add(kickVelocity));
        kickTarget.hurtMarked = true;

        // Create particle effect
        if (!level.isClientSide) {
            for (int i = 0; i < 10; i++) {
                double x = horse.getX() + (level.random.nextDouble() - 0.5) * 0.5;
                double y = horse.getY() + horse.getBbHeight() * 0.5;
                double z = horse.getZ() + (level.random.nextDouble() - 0.5) * 0.5;
                level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CLOUD,
                    x, y, z,
                    1, 0, 0, 0, 0.02
                );
            }
        }

        // Small cooldown for the target
        kickTarget.knockback(0.5f, kickDirection.x, kickDirection.z);
    }

    public static class KickConfig {
        public double kickRange = 3.0;
        public double detectionRange = 4.0;
        public int kickWarmupDuration = 15; // ticks
        public int kickCooldown = 60; // ticks
        public double baseKickDamage = 4.0;
        public double knockbackStrength = 0.6;

        public static KickConfig createDefault() {
            return new KickConfig();
        }

        public static KickConfig createDonkeyConfig() {
            KickConfig config = new KickConfig();
            config.baseKickDamage = 5.0; // Donkeys kick harder
            config.knockbackStrength = 0.7;
            return config;
        }

        public static KickConfig createMuleConfig() {
            KickConfig config = new KickConfig();
            config.baseKickDamage = 6.0; // Mules kick hardest
            config.knockbackStrength = 0.8;
            return config;
        }
    }
}
