package me.javavirtualenv.behavior.pathfinding.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Controls smooth entity rotation for realistic turning behavior.
 * Prevents instant direction changes by limiting turn speed per tick.
 */
public class TurningController {
    private final Mob mob;
    private float maxTurnSpeed;
    private float currentYaw;
    private float targetYaw;

    /**
     * Creates a turning controller for the given mob.
     *
     * @param mob the entity to control
     * @param maxTurnSpeed maximum degrees per tick the entity can turn (default: 10.0f)
     */
    public TurningController(Mob mob, float maxTurnSpeed) {
        this.mob = mob;
        this.maxTurnSpeed = maxTurnSpeed;
        this.currentYaw = mob.getYRot();
        this.targetYaw = this.currentYaw;
    }

    /**
     * Sets target yaw to face a position in the world.
     *
     * @param target the position to face
     */
    public void turnToward(Vec3 target) {
        Vec3 from = mob.position();
        this.targetYaw = calculateYawToward(from, target);
    }

    /**
     * Sets target yaw to face an entity.
     *
     * @param target the entity to face
     */
    public void turnToward(Entity target) {
        turnToward(target.position());
    }

    /**
     * Sets the target yaw directly.
     *
     * @param yaw the desired yaw in degrees
     */
    public void setTargetYaw(float yaw) {
        this.targetYaw = wrapDegrees(yaw);
    }

    /**
     * Updates rotation smoothly toward target. Call this every tick.
     */
    public void tick() {
        float diff = wrapDegrees(targetYaw - currentYaw);
        float turnAmount = Mth.clamp(diff, -maxTurnSpeed, maxTurnSpeed);

        currentYaw = wrapDegrees(currentYaw + turnAmount);
        applyToEntity();
    }

    /**
     * Checks if the entity is facing the target within a tolerance.
     *
     * @param tolerance acceptable angle difference in degrees
     * @return true if within tolerance of target yaw
     */
    public boolean isFacingTarget(float tolerance) {
        return Math.abs(getAngleToTarget()) <= tolerance;
    }

    /**
     * Sets the maximum turn speed.
     *
     * @param speed maximum degrees per tick
     */
    public void setMaxTurnSpeed(float speed) {
        this.maxTurnSpeed = speed;
    }

    /**
     * Gets the maximum turn speed.
     *
     * @return maximum degrees per tick
     */
    public float getMaxTurnSpeed() {
        return maxTurnSpeed;
    }

    /**
     * Gets the current yaw.
     *
     * @return current rotation in degrees
     */
    public float getCurrentYaw() {
        return currentYaw;
    }

    /**
     * Gets the target yaw.
     *
     * @return target rotation in degrees
     */
    public float getTargetYaw() {
        return targetYaw;
    }

    /**
     * Syncs current yaw from the entity's actual rotation.
     */
    public void syncFromEntity() {
        this.currentYaw = mob.getYRot();
    }

    /**
     * Applies current yaw to the entity's rotation.
     */
    public void applyToEntity() {
        mob.setYRot(currentYaw);
        mob.yHeadRot = currentYaw;
    }

    /**
     * Wraps angle to -180 to 180 degree range.
     *
     * @param degrees the angle to wrap
     * @return wrapped angle
     */
    private float wrapDegrees(float degrees) {
        return Mth.wrapDegrees(degrees);
    }

    /**
     * Calculates yaw angle to face from one position to another.
     *
     * @param from starting position
     * @param to target position
     * @return yaw in degrees
     */
    private float calculateYawToward(Vec3 from, Vec3 to) {
        double deltaX = to.x - from.x;
        double deltaZ = to.z - from.z;
        return (float) (Mth.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0f;
    }

    /**
     * Gets the current angle difference to target.
     *
     * @return angle difference in degrees (positive = clockwise, negative = counter-clockwise)
     */
    public float getAngleToTarget() {
        return wrapDegrees(targetYaw - currentYaw);
    }

    /**
     * Adjusts turn speed based on movement speed for realistic physics.
     * Faster movement typically means slower turning capability.
     *
     * @param movementSpeed current movement speed
     * @param baseSpeed base turn speed at rest
     * @param speedPenalty penalty factor (0.0-1.0, higher = more penalty)
     * @return adjusted turn speed
     */
    public static float calculateSpeedAdjustedTurnRate(float movementSpeed, float baseSpeed, float speedPenalty) {
        return baseSpeed * (1.0f - (movementSpeed * speedPenalty));
    }
}
