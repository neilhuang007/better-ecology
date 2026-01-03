package me.javavirtualenv.ecology.ai;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * AI goal for cows to chew their cud.
 *
 * Cud chewing (rumination) is a natural behavior where cows:
 * - Regurgitate partially digested food
 * - Chew it again to break down fiber
 * - Re-swallow for further digestion
 * - This happens 8-10 hours per day for real cattle
 *
 * This goal makes cows pause and perform chewing animations periodically.
 */
public class CowCudChewGoal extends Goal {
    private final PathfinderMob mob;
    private final Level level;
    private int chewingTicks;
    private int chewingDuration;
    private boolean isChewing;

    public CowCudChewGoal(PathfinderMob mob) {
        this.mob = mob;
        this.level = mob.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.chewingTicks = 0;
        this.chewingDuration = 0;
        this.isChewing = false;
    }

    @Override
    public boolean canUse() {
        // Random chance to start chewing (higher when not moving)
        if (mob.getRandom().nextFloat() < 0.005f) {
            // Only chew if not already doing something important
            return !mob.isAggressive() && !mob.isInWater();
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return isChewing && chewingTicks < chewingDuration;
    }

    @Override
    public void start() {
        isChewing = true;
        chewingTicks = 0;
        chewingDuration = 100 + mob.getRandom().nextInt(200); // 5-15 seconds

        // Stop moving
        mob.getNavigation().stop();

        // Broadcast eating animation event to clients (event ID 10 = eating)
        level.broadcastEntityEvent(mob, (byte) 10);

        // Play start sound (use COW_AMBIENT since COW_EAT doesn't exist in 1.21.1)
        level.playSound(null, mob.blockPosition(),
                SoundEvents.COW_AMBIENT,
                SoundSource.NEUTRAL,
                0.8F, 0.9F);
    }

    @Override
    public void stop() {
        isChewing = false;
        chewingTicks = 0;
        chewingDuration = 0;

        // Look at a random nearby block
        mob.getLookControl().setLookAt(
                mob.getX() + (mob.getRandom().nextFloat() - 0.5) * 10,
                mob.getY() + mob.getEyeHeight(),
                mob.getZ() + (mob.getRandom().nextFloat() - 0.5) * 10
        );
    }

    @Override
    public void tick() {
        chewingTicks++;

        // Re-broadcast animation every 40 ticks to keep it visible
        // Vanilla eating animation lasts 40 ticks, so we refresh it
        if (chewingTicks % 40 == 0) {
            level.broadcastEntityEvent(mob, (byte) 10);

            // Play periodic chewing sounds
            level.playSound(null, mob.blockPosition(),
                    SoundEvents.COW_AMBIENT,
                    SoundSource.NEUTRAL,
                    0.6F, 1.0F);
        }

        // Jaw movement particles every 20 ticks
        if (chewingTicks % 20 == 0 && level instanceof ServerLevel serverLevel) {
            spawnChewParticles(serverLevel);
        }

        // Slight head bobbing animation (look around while chewing)
        if (chewingTicks % 60 == 0) {
            mob.getLookControl().setLookAt(
                    mob.getX() + (mob.getRandom().nextFloat() - 0.5) * 8,
                    mob.getY() + mob.getEyeHeight() + (mob.getRandom().nextFloat() - 0.5) * 0.5,
                    mob.getZ() + (mob.getRandom().nextFloat() - 0.5) * 8
            );
        }

        // Keep stopping any movement attempts
        if (!mob.getNavigation().isDone()) {
            mob.getNavigation().stop();
        }
    }

    /**
     * Spawn small particle effects to simulate jaw movement.
     */
    private void spawnChewParticles(ServerLevel serverLevel) {
        // Small "dust" particles from mouth area
        double offsetX = serverLevel.getRandom().nextDouble() * 0.3 - 0.15;
        double offsetY = serverLevel.getRandom().nextDouble() * 0.2;
        double offsetZ = serverLevel.getRandom().nextDouble() * 0.3 - 0.15;

        // Create dust particle options (green-ish color for grass)
        DustParticleOptions dustOptions = new DustParticleOptions(
                new Vector3f(0.4f, 0.6f, 0.3f), 1.0f);

        serverLevel.sendParticles(
                dustOptions,
                mob.getX() + offsetX,
                mob.getY() + mob.getEyeHeight() * 0.8 + offsetY,
                mob.getZ() + offsetZ,
                1, 0, 0, 0, 0.02
        );
    }

    public boolean isChewing() {
        return isChewing;
    }

    public int getChewingTicks() {
        return chewingTicks;
    }
}
