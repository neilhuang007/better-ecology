package me.javavirtualenv.behavior.pathfinding.steering;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Flee behavior: moves directly away from a threat position.
 * This is the inverse of seek behavior, with distance-based scaling for urgency.
 *
 * Algorithm:
 * 1. Only flee if within panic distance
 * 2. Calculate desired velocity: normalize(position - threat) * maxSpeed
 * 3. Scale force by proximity (closer threats = stronger response)
 * 4. Calculate steering force: desired - currentVelocity
 *
 * The panic distance prevents continuous fleeing from distant threats.
 */
public class FleeBehavior implements SteeringBehavior {
    private final float weight;
    private final float panicDistance;
    private boolean active;

    /**
     * Creates a flee behavior with default panic distance of 16 blocks.
     */
    public FleeBehavior() {
        this(2.0f, 16.0f);
    }

    /**
     * Creates a flee behavior with custom parameters.
     *
     * @param weight multiplier for blending with other behaviors
     * @param panicDistance only flee if threat is within this distance
     */
    public FleeBehavior(float weight, float panicDistance) {
        this.weight = weight;
        this.panicDistance = panicDistance;
        this.active = true;
    }

    @Override
    public Vec3 calculate(Mob mob, SteeringContext context) {
        if (context.getTargetPosition() == null) {
            return Vec3.ZERO;
        }

        Vec3 currentPosition = mob.position();
        Vec3 currentVelocity = mob.getDeltaMovement();
        Vec3 threatPosition = context.getTargetPosition();

        // Calculate distance to threat
        Vec3 offset = currentPosition.subtract(threatPosition);
        double distance = offset.length();

        // Only flee if within panic distance
        if (distance > panicDistance || distance < 0.01) {
            return Vec3.ZERO;
        }

        // Calculate desired velocity away from threat
        Vec3 desired = offset.normalize().scale(context.getMaxSpeed());

        // Scale force by proximity (closer = stronger)
        float proximityScale = (float) (1.0 - (distance / panicDistance));
        desired = desired.scale(proximityScale);

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

    public float getPanicDistance() {
        return panicDistance;
    }
}
