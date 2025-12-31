package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Mob;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Quiet movement behavior for felines.
 * <p>
 * Cats move silently to avoid detection and startle prey.
 * This behavior:
 * - Reduces footstep sound volume
 * - Reduces detection radius by other mobs
 * - Slows movement slightly when in stealth mode
 * - Can be toggled on/off based on situation
 */
public class QuietMovementBehavior extends SteeringBehavior {

    private final double stealthModifier;
    private final double soundReduction;
    private boolean isQuiet = true;

    public QuietMovementBehavior(double stealthModifier, double soundReduction) {
        super(0.5);
        this.stealthModifier = stealthModifier;
        this.soundReduction = soundReduction;
    }

    public QuietMovementBehavior() {
        this(0.7, 0.3);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        if (!isQuiet) {
            return new Vec3d();
        }

        // Apply stealth modifier to reduce detection
        // This is handled by modifying the movement vector slightly
        Vec3d currentVelocity = context.getVelocity();

        // Reduce movement noise by slowing slightly
        Vec3d quietMovement = currentVelocity.copy();
        quietMovement.mult(stealthModifier);

        return quietMovement;
    }

    /**
     * Get the sound reduction multiplier for footstep sounds.
     */
    public double getSoundReduction() {
        return isQuiet ? soundReduction : 1.0;
    }

    /**
     * Get the stealth modifier for detection calculations.
     */
    public double getStealthModifier() {
        return isQuiet ? stealthModifier : 1.0;
    }

    public boolean isQuiet() {
        return isQuiet;
    }

    public void setQuiet(boolean quiet) {
        isQuiet = quiet;
    }

    public void toggleQuiet() {
        isQuiet = !isQuiet;
    }
}
