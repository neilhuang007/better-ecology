package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Fall damage reduction behavior for agile felines.
 * <p>
 * Cats have a righting reflex and always land on their feet:
 * - No fall damage from any height (up to world limits)
 * - Rotates to land on feet during fall
 * - Absorbs impact through leg muscles
 * - Can survive falls that would kill other mobs
 * <p>
 * This behavior modifies fall damage calculation and applies
 * special handling when cats are falling.
 */
public class FallDamageReductionBehavior extends SteeringBehavior {

    private final double maxSafeFallHeight;
    private final boolean noFallDamage;
    private boolean wasFalling = false;
    private int fallTicks = 0;

    public FallDamageReductionBehavior(double maxSafeFallHeight, boolean noFallDamage) {
        super(0.0);
        this.maxSafeFallHeight = maxSafeFallHeight;
        this.noFallDamage = noFallDamage;
    }

    public FallDamageReductionBehavior() {
        this(256.0, true);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();
        Level level = context.getLevel();

        // Check if mob is falling
        boolean isFalling = mob.getDeltaMovement().y < 0;

        if (isFalling) {
            wasFalling = true;
            fallTicks++;

            // Play falling sound for long falls
            if (fallTicks > 10 && fallTicks % 20 == 0) {
                // Could play a falling meow sound here
            }
        } else {
            // Just landed
            if (wasFalling && fallTicks > 5) {
                onLand(mob, level);
            }
            wasFalling = false;
            fallTicks = 0;
        }

        return new Vec3d(); // No movement modification
    }

    /**
     * Called when the cat lands after a fall.
     * Reduces or eliminates fall damage.
     */
    private void onLand(Mob mob, Level level) {
        double fallDistance = mob.fallDistance;

        if (noFallDamage) {
            // Cats take no fall damage
            mob.fallDistance = 0;

            // Play landing sound
            if (!level.isClientSide) {
                level.playSound(null, mob.blockPosition(), SoundEvents.CAT_AMBIENT,
                    SoundSource.NEUTRAL, 0.3f, 1.0f);
            }
        } else {
            // Reduce fall damage if below max safe height
            if (fallDistance <= maxSafeFallHeight) {
                mob.fallDistance = 0;
            } else {
                // Reduce damage by 80%
                mob.fallDistance = fallDistance * 0.2;
            }
        }
    }

    /**
     * Calculate the fall damage for a cat.
     * Override this in mixins to modify vanilla fall damage.
     */
    public float calculateFallDamage(Mob mob, float distance) {
        if (noFallDamage) {
            return 0;
        }

        if (distance <= maxSafeFallHeight) {
            return 0;
        }

        // Reduced damage formula
        float excessDistance = (float) (distance - maxSafeFallHeight);
        return excessDistance * 0.2f;
    }

    /**
     * Check if the cat should right itself during a fall.
     * This is called by the mixin to rotate the cat properly.
     */
    public boolean shouldRightSelf(Mob mob) {
        return mob.getDeltaMovement().y < -0.1;
    }

    public boolean wasFalling() {
        return wasFalling;
    }

    public int getFallTicks() {
        return fallTicks;
    }

    public double getMaxSafeFallHeight() {
        return maxSafeFallHeight;
    }

    public boolean hasNoFallDamage() {
        return noFallDamage;
    }
}
