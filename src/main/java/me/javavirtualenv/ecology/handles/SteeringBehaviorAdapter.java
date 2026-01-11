package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.BehaviorRule;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.SteeringBehavior;

/**
 * Adapter that wraps a SteeringBehavior to implement BehaviorRule.
 * <p>
 * This adapter bridges the compatibility between the behavior system:
 * - Uses me.javavirtualenv.behavior.core.SteeringBehavior from the core package
 * - Implements the BehaviorRule interface for integration with the behavior registry
 * <p>
 * The adapter configures behavior context with appropriate speed/force parameters
 * and delegates the calculate call to the wrapped steering behavior.
 */
public class SteeringBehaviorAdapter implements BehaviorRule {

    private final SteeringBehavior wrappedBehavior;
    private final double maxSpeed;
    private final double maxForce;

    public SteeringBehaviorAdapter(SteeringBehavior behavior) {
        this(behavior, 1.0, 0.15);
    }

    public SteeringBehaviorAdapter(SteeringBehavior behavior, double maxSpeed, double maxForce) {
        this.wrappedBehavior = behavior;
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
    }

    @Override
    public Vec3d calculate(me.javavirtualenv.behavior.core.BehaviorContext context) {
        if (!wrappedBehavior.isEnabled()) {
            return new Vec3d();
        }

        // Create behavior context with configured parameters
        me.javavirtualenv.behavior.core.BehaviorContext steeringContext =
            new me.javavirtualenv.behavior.core.BehaviorContext.Builder()
                .self(context.getEntity())
                .position(context.getPosition())
                .velocity(context.getVelocity())
                .maxSpeed(maxSpeed)
                .maxForce(maxForce)
                .nearbyEntities(context.getNeighbors())
                .world(context.getLevel())
                .build();

        // Delegate to wrapped behavior
        // Note: We call calculate() (not calculateWeighted()) to get the unweighted result.
        // The weight will be applied externally by BehaviorRegistry.addWeightedForce()
        // to avoid double weight application (weight²).
        Vec3d result = wrappedBehavior.calculate(steeringContext);

        return result;
    }

    /**
     * Gets the wrapped steering behavior.
     */
    public SteeringBehavior getWrappedBehavior() {
        return wrappedBehavior;
    }

    /**
     * Sets the weight on the wrapped behavior.
     */
    public void setWeight(double weight) {
        wrappedBehavior.setWeight(weight);
    }

    /**
     * Gets the weight from the wrapped behavior.
     */
    public double getWeight() {
        return wrappedBehavior.getWeight();
    }

    /**
     * Sets enabled state on the wrapped behavior.
     */
    public void setEnabled(boolean enabled) {
        wrappedBehavior.setEnabled(enabled);
    }

    /**
     * Gets enabled state from the wrapped behavior.
     */
    public boolean isEnabled() {
        return wrappedBehavior.isEnabled();
    }
}
