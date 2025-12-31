package me.javavirtualenv.behavior.rabbit;

import me.javavirtualenv.behavior.core.BehaviorContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Thump behavior for rabbits to warn other rabbits of danger.
 * <p>
 * Rabbits thump their hind legs on the ground to create both
 * audible warnings and ground vibrations that other rabbits can detect.
 * This is a critical anti-predator communication mechanism.
 * <p>
 * Features:
 * - Foot thumping with configurable intensity
 * - Chain reaction through nearby rabbits
 * - Visual and audio warning signals
 * - Cooldown system to prevent spam
 */
public class ThumpBehavior {

    private final RabbitThumpConfig config;

    // Thump state
    private int thumpCooldown = 0;
    private int thumpCount = 0;
    private int maxThumps = 3;
    private long lastThumpTime = 0;
    private LivingEntity lastThreat = null;

    // Statistics
    private int totalThumps = 0;
    private int rabbitsAlerted = 0;

    public ThumpBehavior(RabbitThumpConfig config) {
        this.config = config;
    }

    public ThumpBehavior() {
        this(RabbitThumpConfig.createDefault());
    }

    /**
     * Checks if thump should be performed and executes if conditions are met.
     *
     * @param context Behavior context
     * @param threat  Detected threat entity
     * @return true if thump was performed
     */
    public boolean thumpIfNeeded(BehaviorContext context, LivingEntity threat) {
        Mob entity = context.getEntity();
        long currentTime = entity.level().getGameTime();

        // Update cooldown
        if (thumpCooldown > 0) {
            thumpCooldown--;
            return false;
        }

        // Check if thumping is warranted
        if (!shouldThump(context, threat)) {
            thumpCount = 0;
            return false;
        }

        // Perform thump
        performThump(context, threat);

        // Update state
        thumpCount++;
        lastThumpTime = currentTime;
        lastThreat = threat;

        // Check if we should thump again
        if (thumpCount < maxThumps) {
            thumpCooldown = config.getThumpInterval();
        } else {
            thumpCooldown = config.getThumpCooldown();
            thumpCount = 0;
        }

        return true;
    }

    /**
     * Determines if thumping is appropriate based on threat assessment.
     */
    private boolean shouldThump(BehaviorContext context, LivingEntity threat) {
        // No threat detected
        if (threat == null || !threat.isAlive()) {
            return false;
        }

        // Check distance to threat
        Vec3d position = context.getPosition();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        double distance = position.distanceTo(threatPos);

        // Only thump if threat is within detection range but not too close
        if (distance > config.getThumpDetectionRange()) {
            return false; // Too far
        }

        if (distance < config.getThumpMinDistance()) {
            return false; // Too close, focus on fleeing
        }

        // Check if threat is significant enough
        return isSignificantThreat(context, threat);
    }

    /**
     * Performs the thumping behavior with audio and visual effects.
     */
    private void performThump(BehaviorContext context, LivingEntity threat) {
        Mob entity = context.getEntity();
        Vec3d position = context.getPosition();

        // Play thump sound
        playThumpSound(context);

        // Create visual effect (particles)
        emitThumpParticles(context);

        // Alert nearby rabbits
        alertNearbyRabbits(context, threat);

        // Record statistics
        totalThumps++;
    }

    /**
     * Plays the thump sound effect.
     */
    private void playThumpSound(BehaviorContext context) {
        Mob entity = context.getEntity();

        if (!entity.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) entity.level();

            // Play a thud/thump sound
            // Using generic step sound as base - pitch it lower for thump effect
            serverLevel.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.RABBIT_JUMP, // Reuse rabbit jump sound
                SoundSource.NEUTRAL,
                0.5F, // Volume
                0.6F  // Lower pitch for thud effect
            );
        }
    }

    /**
     * Emits particle effects to visualize the thump.
     */
    private void emitThumpParticles(BehaviorContext context) {
        Mob entity = context.getEntity();

        if (!entity.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) entity.level();

            // Spawn dust particles at feet
            BlockPos pos = entity.blockPosition();
            for (int i = 0; i < 5; i++) {
                double offsetX = (entity.getRandom().nextDouble() - 0.5) * 0.5;
                double offsetZ = (entity.getRandom().nextDouble() - 0.5) * 0.5;
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                    entity.getX() + offsetX,
                    entity.getY(),
                    entity.getZ() + offsetZ,
                    3,
                    0.0, 0.0, 0.0,
                    0.02
                );
            }
        }
    }

    /**
     * Alerts nearby rabbits to the threat through chain reaction.
     */
    private void alertNearbyRabbits(BehaviorContext context, LivingEntity threat) {
        Mob entity = context.getEntity();
        Vec3d position = context.getPosition();
        double alertRange = config.getThumpAlertRange();

        // Find nearby rabbits
        List<Rabbit> nearbyRabbits = entity.level().getEntitiesOfClass(
            Rabbit.class,
            entity.getBoundingBox().inflate(alertRange)
        );

        int alertedCount = 0;

        for (Rabbit rabbit : nearbyRabbits) {
            // Skip self
            if (rabbit == entity) {
                continue;
            }

            // Check if rabbit already knows about threat
            Vec3d rabbitPos = new Vec3d(rabbit.getX(), rabbit.getY(), rabbit.getZ());
            double distance = rabbitPos.distanceTo(position);

            if (distance > alertRange) {
                continue;
            }

            // Alert the rabbit
            alertRabbit(rabbit, threat);
            alertedCount++;
        }

        rabbitsAlerted += alertedCount;
    }

    /**
     * Alerts a single rabbit to the threat.
     */
    private void alertRabbit(Rabbit rabbit, LivingEntity threat) {
        // Make rabbit look at threat
        rabbit.getLookControl().setLookAt(threat, 30.0F, 30.0F);

        // If this rabbit has thump behavior, trigger it
        // This creates chain reaction of warnings
        if (rabbit.level().getGameTime() - lastThumpTime > 20) {
            // Only chain if enough time has passed to prevent instant cascade
            // The actual thumping will be handled by that rabbit's behavior system
        }
    }

    /**
     * Determines if a threat is significant enough to warrant thumping.
     */
    private boolean isSignificantThreat(BehaviorContext context, LivingEntity threat) {
        // Players are always significant
        if (threat instanceof net.minecraft.world.entity.player.Player player) {
            return !player.isShiftKeyDown(); // Don't thump at sneaking players
        }

        // Predators are significant
        String typeName = threat.getType().toString().toLowerCase();
        if (typeName.contains("wolf") || typeName.contains("fox") ||
            typeName.contains("cat") || typeName.contains("ocelot")) {
            return true;
        }

        // Aggressive mobs are significant
        if (threat instanceof Mob mob && mob.isAggressive()) {
            return true;
        }

        return false;
    }

    // Getters

    public int getThumpCooldown() {
        return thumpCooldown;
    }

    public int getThumpCount() {
        return thumpCount;
    }

    public LivingEntity getLastThreat() {
        return lastThreat;
    }

    public long getLastThumpTime() {
        return lastThumpTime;
    }

    public int getTotalThumps() {
        return totalThumps;
    }

    public int getRabbitsAlerted() {
        return rabbitsAlerted;
    }

    public RabbitThumpConfig getConfig() {
        return config;
    }

    /**
     * Forces a thump immediately (useful for testing).
     */
    public void forceThump(BehaviorContext context, LivingEntity threat) {
        performThump(context, threat);
        thumpCount++;
    }

    /**
     * Resets thump state.
     */
    public void reset() {
        thumpCooldown = 0;
        thumpCount = 0;
        lastThumpTime = 0;
        lastThreat = null;
        totalThumps = 0;
        rabbitsAlerted = 0;
    }

    // Helper class for Vec3d compatibility
    private static class Vec3d {
        public final double x, y, z;

        public Vec3d(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static Vec3d fromMinecraftVec3(Vec3 vec) {
            return new Vec3d(vec.x, vec.y, vec.z);
        }

        public double distanceTo(Vec3d other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}
