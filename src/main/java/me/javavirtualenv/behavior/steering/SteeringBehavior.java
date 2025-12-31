package me.javavirtualenv.behavior.steering;

import me.javavirtualenv.behavior.core.Vec3d;

/**
 * Abstract base class for all steering behaviors.
 * Each behavior calculates a steering force that influences entity movement.
 */
public abstract class SteeringBehavior {
    private double weight;
    private boolean enabled;

    public SteeringBehavior() {
        this(1.0, true);
    }

    public SteeringBehavior(double weight) {
        this(weight, true);
    }

    public SteeringBehavior(double weight, boolean enabled) {
        this.weight = weight;
        this.enabled = enabled;
    }

    /**
     * Calculate the steering force for this behavior.
     * 
     * @param context The behavior context containing entity state and environment
     * @return The calculated steering force vector
     */
    public abstract Vec3d calculate(BehaviorContext context);

    /**
     * Get the weight/priority of this behavior.
     * Higher weights have more influence on the final steering force.
     * 
     * @return The behavior weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Set the weight/priority of this behavior.
     * 
     * @param weight The new weight value
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Check if this behavior is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable this behavior.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Calculate the weighted steering force.
     * Returns zero vector if behavior is disabled.
     * 
     * @param context The behavior context
     * @return The weighted steering force
     */
    public Vec3d calculateWeighted(BehaviorContext context) {
        if (!enabled) {
            return new Vec3d();
        }
        Vec3d force = calculate(context);
        force.mult(weight);
        return force;
    }

    /**
     * Utility method to limit a steering force to maxForce.
     * 
     * @param force The force to limit
     * @param maxForce The maximum force magnitude
     * @return The limited force vector
     */
    protected Vec3d limitForce(Vec3d force, double maxForce) {
        Vec3d limited = force.copy();
        limited.limit(maxForce);
        return limited;
    }
}
