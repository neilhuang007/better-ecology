package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

/**
 * Purr behavior for content felines.
 * <p>
 * Cats purr when:
 * - Being petted by a trusted player
 * - Resting comfortably
 * - Near trusted entities
 * - After eating
 * <p>
 * This behavior manages the purring state and sound effects.
 */
public class PurrBehavior extends SteeringBehavior {

    private boolean isPurring = false;
    private int purrTicks = 0;
    private final int maxPurrDuration;
    private final double affectionThreshold;

    public PurrBehavior(int maxPurrDuration, double affectionThreshold) {
        super(0.0); // This behavior doesn't affect movement
        this.maxPurrDuration = maxPurrDuration;
        this.affectionThreshold = affectionThreshold;
    }

    public PurrBehavior() {
        this(200, 0.6);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Check if should purr
        if (shouldPurr(mob, context)) {
            if (!isPurring) {
                startPurring(mob);
            }
            purrTicks++;

            // Play purr sound periodically
            if (purrTicks % 60 == 0) {
                playPurrSound(mob);
            }

            // Stop purring after duration
            if (purrTicks > maxPurrDuration) {
                stopPurring();
            }
        } else {
            if (isPurring) {
                stopPurring();
            }
        }

        return new Vec3d(); // No movement effect
    }

    private boolean shouldPurr(Mob mob, BehaviorContext context) {
        // Purr if being petted by trusted player
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat) {
            // Check if cat is tamed and being petted
            if (cat.isTame() && cat.isInSittingPose()) {
                return true;
            }
        }

        // Purr if health and hunger are good
        double healthPercent = mob.getHealth() / mob.getMaxHealth();
        return healthPercent > 0.8;
    }

    private void startPurring(Mob mob) {
        isPurring = true;
        purrTicks = 0;
        playPurrSound(mob);
    }

    private void stopPurring() {
        isPurring = false;
        purrTicks = 0;
    }

    private void playPurrSound(Mob mob) {
        Level level = mob.level();
        if (!level.isClientSide) {
            level.playSound(null, mob.blockPosition(), SoundEvents.CAT_PURR,
                SoundSource.NEUTRAL, 0.5f, 1.0f);
        }
    }

    public boolean isPurring() {
        return isPurring;
    }

    public void startPurr() {
        isPurring = true;
        purrTicks = 0;
    }

    public void stopPurr() {
        stopPurring();
    }

    public int getPurrTicks() {
        return purrTicks;
    }
}
