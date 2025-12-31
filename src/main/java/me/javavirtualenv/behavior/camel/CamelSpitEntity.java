package me.javavirtualenv.behavior.camel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spit projectile fired by camels as a defense mechanism.
 * <p>
 * This projectile:
 * - Deals minor physical damage
 * - Applies a slow effect to hinder movement
 * - Creates splash particles on impact
 * - Has gravity and limited range
 */
public class CamelSpitEntity extends Projectile {

    private static final float GRAVITY = 0.05f;
    private float damage = 1.0f;
    private int slowDuration = 100;

    public CamelSpitEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public CamelSpitEntity(Level level, Entity owner) {
        super(EntityType.LLAMA_SPIT, level);
        setOwner(owner);
    }

    public CamelSpitEntity(Level level, double x, double y, double z) {
        super(EntityType.LLAMA_SPIT, level);
        setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();

        // Apply gravity
        Vec3 velocity = getDeltaMovement();
        setDeltaMovement(
            velocity.x * 0.99,
            velocity.y * 0.99 - GRAVITY,
            velocity.z * 0.99
        );

        // Spawn trail particles
        if (level().isClientSide) {
            spawnTrailParticles();
        }

        // Check if in ground or too slow
        if (isInGround() || getDeltaMovement().lengthSqr() < 0.01) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (!level().isClientSide) {
            // Create splash effect
            level().broadcastEntityEvent(this, (byte) 3);

            if (result.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) result;
                onEntityHit(entityHit);
            }
        }

        discard();
    }

    /**
     * Handles hitting an entity.
     */
    private void onEntityHit(EntityHitResult entityHit) {
        Entity target = entityHit.getEntity();

        if (target instanceof LivingEntity livingTarget) {
            // Apply damage
            DamageSource damageSource = damageSources().mobProjectile(this, getOwner());
            livingTarget.hurt(damageSource, damage);

            // Apply slow effect
            if (slowDuration > 0) {
                livingTarget.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    slowDuration,
                    0, // Amplifier 0 = 15% slowdown
                    false, false
                ));
            }
        }
    }

    /**
     * Spawns trail particles during flight.
     */
    private void spawnTrailParticles() {
        Vec3 pos = position();
        for (int i = 0; i < 2; i++) {
            double offsetX = (Math.random() - 0.5) * 0.1;
            double offsetY = (Math.random() - 0.5) * 0.1;
            double offsetZ = (Math.random() - 0.5) * 0.1;

            level().addParticle(
                ParticleTypes.SPIT,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                -getDeltaMovement().x * 0.1,
                -getDeltaMovement().y * 0.1,
                -getDeltaMovement().z * 0.1
            );
        }
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setSlowDuration(int duration) {
        this.slowDuration = duration;
    }

    public float getDamage() {
        return damage;
    }

    public int getSlowDuration() {
        return slowDuration;
    }
}
