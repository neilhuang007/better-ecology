package me.javavirtualenv.behavior.pathfinding.steering;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Seek behavior: moves directly toward a target position at maximum speed.
 * This is the most basic steering behavior, producing movement directly toward a goal.
 *
 * Algorithm:
 * 1. Calculate desired velocity: normalize(target - position) * maxSpeed
 * 2. Calculate steering force: desired - currentVelocity
 *
 * The steering force represents the change needed to reach the target optimally.
 */
public class SeekBehavior implements SteeringBehavior {
    private final float weight;
    private boolean active;

    public SeekBehavior() {
        this(1.0f);
    }

    public SeekBehavior(float weight) {
        this.weight = weight;
        this.active = true;
    }

    @Override
    public Vec3 calculate(Mob mob, SteeringContext context) {
        if (context.getTargetPosition() == null) {
            return Vec3.ZERO;
        }

        Vec3 currentPosition = mob.position();
        Vec3 currentVelocity = mob.getDeltaMovement();
        Vec3 targetPosition = context.getTargetPosition();

        // Calculate desired velocity toward target
        Vec3 desired = targetPosition.subtract(currentPosition);
        double distance = desired.length();

        if (distance < 0.01) {
            return Vec3.ZERO;
        }

        // Normalize and scale to max speed
        desired = desired.normalize().scale(context.getMaxSpeed());

        // Steering force = desired velocity - current velocity
        Vec3 steering = desired.subtract(currentVelocity);

        return steering;
    }

    @Override
    public float getWeight() {
        return weight;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
