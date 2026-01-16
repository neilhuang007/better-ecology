package me.javavirtualenv.behavior.pathfinding.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Custom movement controller that provides smooth, momentum-based movement with realistic
 * acceleration, deceleration, and slope-aware speed modification.
 *
 * <p>This controller replaces vanilla MoveControl to provide:
 * <ul>
 *   <li>Gradual acceleration and deceleration instead of instant speed changes</li>
 *   <li>Smooth rotation with maximum turn rate limiting</li>
 *   <li>Momentum-based movement that feels natural</li>
 *   <li>Slope-aware speed modification (slower uphill, faster downhill)</li>
 * </ul>
 *
 * <p>The movement parameters are based on research into realistic animal locomotion
 * patterns documented in the Better Ecology behavior research.
 */
public class RealisticMoveControl extends MoveControl {

    // Current and target speeds for smooth interpolation
    private float currentSpeed = 0.0f;
    private float targetSpeed = 0.0f;

    // Current and target yaw for smooth rotation
    private float currentYaw = 0.0f;
    private float targetYaw = 0.0f;

    // Movement physics constants based on research
    /** Speed increase per tick when accelerating */
    private static final float ACCELERATION = 0.15f;

    /** Speed decrease per tick when decelerating */
    private static final float DECELERATION = 0.20f;

    /** Maximum rotation change per tick in degrees */
    private static final float MAX_TURN_SPEED = 10.0f;

    /** Momentum retention factor (0.0 = no momentum, 1.0 = full momentum) */
    private float momentumFactor = 0.85f;

    /**
     * Creates a new RealisticMoveControl for the specified mob.
     *
     * @param mob The mob this controller will manage
     */
    public RealisticMoveControl(Mob mob) {
        super(mob);
        this.currentYaw = mob.getYRot();
    }

    /**
     * Main update method called every tick to update movement and rotation.
     *
     * <p>This method handles:
     * <ul>
     *   <li>Calculating target yaw from desired position</li>
     *   <li>Applying smooth acceleration/deceleration</li>
     *   <li>Applying slope-based speed modification</li>
     *   <li>Smooth rotation toward target yaw</li>
     *   <li>Momentum-based movement application</li>
     * </ul>
     */
    @Override
    public void tick() {
        if (this.operation == Operation.STRAFE) {
            // Handle strafing (default behavior for combat)
            super.tick();
            return;
        }

        if (this.operation != Operation.WAIT) {
            // Calculate target yaw from wanted position
            double dx = this.wantedX - this.mob.getX();
            double dz = this.wantedZ - this.mob.getZ();
            this.targetYaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;

            // Set target speed based on speed modifier and movement speed attribute
            this.targetSpeed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        } else {
            this.targetSpeed = 0.0f;
        }

        // Apply slope modifier to adjust speed based on terrain
        float slopeModifier = getSlopeSpeedModifier();
        float adjustedTargetSpeed = this.targetSpeed * slopeModifier;

        // Smooth acceleration/deceleration
        float accel = (this.currentSpeed < adjustedTargetSpeed) ? ACCELERATION : DECELERATION;
        this.currentSpeed = approachSpeed(this.currentSpeed, adjustedTargetSpeed, accel);

        // Smooth rotation toward target
        this.currentYaw = approachAngle(this.currentYaw, this.targetYaw, MAX_TURN_SPEED);
        this.mob.setYRot(this.currentYaw);
        this.mob.yHeadRot = this.currentYaw;

        // Apply movement in facing direction
        if (this.currentSpeed > 0.01f && this.operation != Operation.WAIT) {
            float yawRad = this.currentYaw * ((float)Math.PI / 180F);
            double moveX = -Mth.sin(yawRad) * this.currentSpeed;
            double moveZ = Mth.cos(yawRad) * this.currentSpeed;

            // Apply momentum to blend old and new motion
            Vec3 currentMotion = this.mob.getDeltaMovement();
            Vec3 newMotion = new Vec3(
                currentMotion.x * momentumFactor + moveX * (1 - momentumFactor),
                currentMotion.y,
                currentMotion.z * momentumFactor + moveZ * (1 - momentumFactor)
            );

            this.mob.setDeltaMovement(newMotion);

            // Check if we should stop (reached destination)
            double distToTarget = Math.sqrt(
                (this.wantedX - this.mob.getX()) * (this.wantedX - this.mob.getX()) +
                (this.wantedZ - this.mob.getZ()) * (this.wantedZ - this.mob.getZ())
            );

            if (distToTarget < 0.5) {
                this.operation = Operation.WAIT;
            }
        }
    }

    /**
     * Smoothly approaches a target speed with a maximum change per tick.
     *
     * @param current Current speed value
     * @param target Target speed value
     * @param maxDelta Maximum speed change allowed per tick
     * @return New speed value closer to target
     */
    private float approachSpeed(float current, float target, float maxDelta) {
        if (current < target) {
            return Math.min(current + maxDelta, target);
        } else if (current > target) {
            return Math.max(current - maxDelta, target);
        }
        return target;
    }

    /**
     * Smoothly approaches a target angle with proper wrapping and maximum turn rate.
     *
     * @param current Current angle in degrees
     * @param target Target angle in degrees
     * @param maxDelta Maximum angle change in degrees per tick
     * @return New angle value closer to target
     */
    private float approachAngle(float current, float target, float maxDelta) {
        float diff = Mth.wrapDegrees(target - current);
        float clampedDiff = Mth.clamp(diff, -maxDelta, maxDelta);
        return Mth.wrapDegrees(current + clampedDiff);
    }

    /**
     * Calculates speed modifier based on slope of terrain ahead.
     *
     * <p>Movement is:
     * <ul>
     *   <li>Slower when moving uphill (70-100% speed depending on steepness)</li>
     *   <li>Faster when moving downhill on gentle slopes (100-120% speed)</li>
     *   <li>Normal speed on steep downhill (controlled descent)</li>
     *   <li>Normal speed on flat terrain</li>
     * </ul>
     *
     * @return Speed modifier multiplier (0.7 to 1.2)
     */
    private float getSlopeSpeedModifier() {
        // Check the block we're moving toward
        Vec3 movement = mob.getDeltaMovement();
        if (movement.lengthSqr() < 0.001) {
            return 1.0f;
        }

        BlockPos currentPos = mob.blockPosition();
        BlockPos targetPos = new BlockPos(
            Mth.floor(mob.getX() + movement.x * 5),
            Mth.floor(mob.getY() + movement.y * 5),
            Mth.floor(mob.getZ() + movement.z * 5)
        );

        int heightDiff = targetPos.getY() - currentPos.getY();

        if (heightDiff > 0) {
            // Uphill - slower speed based on steepness
            return 0.7f + (0.3f / (1 + heightDiff * 0.5f));
        } else if (heightDiff < 0) {
            // Downhill - faster on gentle slopes, controlled on steep
            int absHeightDiff = Math.abs(heightDiff);
            if (absHeightDiff <= 2) {
                // Gentle downhill - slight speed boost
                return 1.1f + (absHeightDiff * 0.05f);
            } else {
                // Steep downhill - need to control descent
                return 1.0f;
            }
        }

        return 1.0f;
    }

    /**
     * Sets the momentum retention factor.
     *
     * @param factor Momentum factor between 0.0 (no momentum) and 1.0 (full momentum)
     */
    public void setMomentumFactor(float factor) {
        this.momentumFactor = Mth.clamp(factor, 0.0f, 1.0f);
    }

    /**
     * Checks if the mob is currently moving.
     *
     * @return true if current speed is above minimal threshold
     */
    public boolean isMoving() {
        return this.currentSpeed > 0.01f;
    }

    /**
     * Gets the current movement speed.
     *
     * @return Current speed value
     */
    public float getCurrentSpeed() {
        return this.currentSpeed;
    }

    /**
     * Immediately stops all movement by setting both current and target speed to zero.
     * Useful for emergency stops or state transitions.
     */
    public void stopImmediately() {
        this.currentSpeed = 0.0f;
        this.targetSpeed = 0.0f;
        this.operation = Operation.WAIT;
    }
}
