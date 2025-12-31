package me.javavirtualenv.behavior.aquatic;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ink cloud release behavior for squid and glow squid.
 * When threatened by predators, squid release ink to create a visual distraction
 * and escape in the opposite direction.
 * <p>
 * Scientific basis: Cephalopods use ink as a defense mechanism to confuse predators
 * and mask their escape trajectory. The ink can also contain chemicals that
 * temporarily blind or disorient attackers.
 */
public class InkCloudBehavior extends SteeringBehavior {
    private final AquaticConfig config;
    private long lastInkReleaseTime = 0;
    private boolean inkReleased = false;
    private final Map<UUID, Long> inkBlindnessTimers = new HashMap<>();
    private static final int BLINDNESS_DURATION = 80; // 4 seconds of blindness

    public InkCloudBehavior(AquaticConfig config) {
        super(1.5, true);
        this.config = config;
    }

    public InkCloudBehavior() {
        this(AquaticConfig.createForSquid());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();
        Level level = context.getLevel();
        long currentTime = level.getGameTime();

        // Update blindness timers
        updateBlindnessEffects(currentTime);

        // Check if enough time has passed since last ink release
        if (currentTime - lastInkReleaseTime < config.getInkCloudDuration()) {
            inkReleased = false;
            return new Vec3d();
        }

        // Find nearest threat
        Entity threat = findNearestThreat(context);

        if (threat == null) {
            inkReleased = false;
            return new Vec3d();
        }

        double distanceToThreat = context.getPosition().distanceTo(
            new Vec3d(threat.getX(), threat.getY(), threat.getZ())
        );

        // Release ink if threat is close and enough time has passed
        if (distanceToThreat < config.getInkReleaseThreshold() && !inkReleased) {
            releaseInkCloud(context, threat);
            inkReleased = true;
            lastInkReleaseTime = currentTime;

            // Flee in opposite direction
            return calculateEscapeVector(context, threat);
        }

        inkReleased = false;
        return new Vec3d();
    }

    private Entity findNearestThreat(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Level level = context.getLevel();
        double detectionRange = 16.0;

        AABB searchBox = new AABB(
            position.x - detectionRange, position.y - detectionRange, position.z - detectionRange,
            position.x + detectionRange, position.y + detectionRange, position.z + detectionRange
        );

        List<Entity> potentialThreats = level.getEntities(self, searchBox, entity -> {
            if (entity == self) return false;
            if (!entity.isAlive()) return false;

            // Player is a threat
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                return true;
            }

            // Common aquatic predators
            String entityId = net.minecraft.core.Registry.ENTITY_TYPE.getKey(entity.getType()).toString();
            return entityId.equals("minecraft:dolphin") ||
                   entityId.equals("minecraft:drowned") ||
                   entityId.equals("minecraft:guardian") ||
                   entityId.equals("minecraft:elder_guardian");
        });

        Entity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity threat : potentialThreats) {
            Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
            double distance = position.distanceTo(threatPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestThreat = threat;
            }
        }

        return nearestThreat;
    }

    private Vec3d calculateEscapeVector(BehaviorContext context, Entity threat) {
        Vec3d position = context.getPosition();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());

        // Calculate direction away from threat
        Vec3d escapeDir = Vec3d.sub(position, threatPos);
        escapeDir.normalize();

        // Add some randomness to escape path
        escapeDir.x += (Math.random() - 0.5) * 0.5;
        escapeDir.y += (Math.random() - 0.5) * 0.5;
        escapeDir.z += (Math.random() - 0.5) * 0.5;
        escapeDir.normalize();

        escapeDir.mult(config.getMaxSpeed() * 2.0);

        return escapeDir;
    }

    private void releaseInkCloud(BehaviorContext context, Entity threat) {
        Entity self = context.getEntity();
        Level level = context.getLevel();
        Vec3d position = context.getPosition();

        if (level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;

        // Spawn ink particles
        Vec3 mcPos = position.toMinecraftVec3();
        int particleCount = 30;

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (Math.random() - 0.5) * config.getInkCloudRadius();
            double offsetY = (Math.random() - 0.5) * config.getInkCloudRadius();
            double offsetZ = (Math.random() - 0.5) * config.getInkCloudRadius();

            serverLevel.sendParticles(
                self instanceof net.minecraft.world.entity.animal.GlowSquid
                    ? ParticleTypes.GLOW
                    : ParticleTypes.SQUID_INK,
                mcPos.x + offsetX,
                mcPos.y + offsetY,
                mcPos.z + offsetZ,
                1,
                0, 0, 0,
                0.02
            );
        }

        // Play ink release sound
        level.playSound(
            null,
            mcPos.x, mcPos.y, mcPos.z,
            SoundEvents.SQUID_SQUIRT,
            SoundSource.NEUTRAL,
            1.0F,
            1.0F
        );

        // Apply blindness effect to nearby entities
        applyInkBlindness(context);
    }

    /**
     * Apply blindness effect to entities within ink cloud radius.
     * The ink temporarily blinds attackers, allowing the squid to escape.
     */
    private void applyInkBlindness(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Level level = context.getLevel();

        if (level.isClientSide) return;

        // Find entities within ink cloud
        double effectRadius = config.getInkCloudRadius() * 0.7;
        AABB effectBox = new AABB(
            position.x - effectRadius, position.y - effectRadius, position.z - effectRadius,
            position.x + effectRadius, position.y + effectRadius, position.z + effectRadius
        );

        List<Entity> affectedEntities = level.getEntities(self, effectBox, entity -> {
            if (entity == self) return false;
            if (!entity.isAlive()) return false;
            // Only affect living entities that can be blinded
            return entity instanceof LivingEntity;
        });

        long currentTime = level.getGameTime();

        // Apply blindness effect
        for (Entity entity : affectedEntities) {
            if (entity instanceof LivingEntity living) {
                // Add blindness effect
                living.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    BLINDNESS_DURATION,
                    0,
                    false,
                    false
                ));

                // Also add slight slowness to simulate ink cloud resistance
                living.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    BLINDNESS_DURATION / 2,
                    0,
                    false,
                    false
                ));

                // Track affected entity for cleanup
                inkBlindnessTimers.put(entity.getUUID(), currentTime + BLINDNESS_DURATION);
            }
        }
    }

    /**
     * Update and remove expired blindness timers.
     */
    private void updateBlindnessEffects(long currentTime) {
        // Remove expired entries
        inkBlindnessTimers.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue() < currentTime;
            return expired;
        });
    }

    public boolean canReleaseInk(BehaviorContext context) {
        long currentTime = context.getLevel().getGameTime();
        return currentTime - lastInkReleaseTime >= config.getInkCloudDuration();
    }

    public void resetCooldown() {
        this.lastInkReleaseTime = 0;
        this.inkReleased = false;
    }
}
