package me.javavirtualenv.ecology.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

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

        // Play start sound
        level.playSound(null, mob.blockPosition(),
                SoundEvents.COW_EAT,
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

        // Periodic chewing sounds
        if (chewingTicks % 40 == 0) {
            level.playSound(null, mob.blockPosition(),
                    SoundEvents.COW_EAT,
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

        serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.DUST,
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
