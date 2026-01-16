package me.javavirtualenv.behavior.pathfinding.steering;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines multiple steering behaviors into a single control system.
 * Blends behavior forces using weighted sums to create complex movement patterns.
 *
 * The controller:
 * 1. Collects forces from all active behaviors
 * 2. Applies behavior-specific weights
 * 3. Sums weighted forces
 * 4. Truncates result to maximum force magnitude
 *
 * This allows emergent behavior from simple rules (flocking, predator avoidance, etc).
 */
public class SteeringController {
    private final List<SteeringBehavior> behaviors;
    private float maxForce;

    public SteeringController() {
        this.behaviors = new ArrayList<>();
        this.maxForce = 0.5f;
    }

    public SteeringController(float maxForce) {
        this.behaviors = new ArrayList<>();
        this.maxForce = maxForce;
    }

    /**
     * Adds a steering behavior to the controller.
     *
     * @param behavior behavior to add
     */
    public void addBehavior(SteeringBehavior behavior) {
        if (behavior != null && !behaviors.contains(behavior)) {
            behaviors.add(behavior);
        }
    }

    /**
     * Removes a steering behavior from the controller.
     *
     * @param behavior behavior to remove
     */
    public void removeBehavior(SteeringBehavior behavior) {
        behaviors.remove(behavior);
    }

    /**
     * Removes all steering behaviors from the controller.
     */
    public void clearBehaviors() {
        behaviors.clear();
    }

    /**
     * Calculates the combined steering force from all active behaviors.
     * Uses weighted sum blending: each behavior contributes based on its weight.
     *
     * @param mob entity being steered
     * @param context environmental information for calculations
     * @return combined steering force, truncated to maxForce
     */
    public Vec3 calculateSteering(Mob mob, SteeringContext context) {
        if (behaviors.isEmpty()) {
            return Vec3.ZERO;
        }

        Vec3 totalForce = Vec3.ZERO;

        // Sum weighted forces from all active behaviors
        for (SteeringBehavior behavior : behaviors) {
            if (!behavior.isActive()) {
                continue;
            }

            Vec3 force = behavior.calculate(mob, context);
            Vec3 weightedForce = force.scale(behavior.getWeight());
            totalForce = totalForce.add(weightedForce);
        }

        // Truncate to maximum force magnitude
        return truncate(totalForce, context.getMaxForce());
    }

    /**
     * Limits a vector to a maximum magnitude.
     * Preserves direction but caps length.
     *
     * @param vector vector to truncate
     * @param max maximum magnitude
     * @return truncated vector
     */
    public Vec3 truncate(Vec3 vector, float max) {
        double length = vector.length();

        if (length > max && length > 0.01) {
            return vector.normalize().scale(max);
        }

        return vector;
    }

    /**
     * Gets all registered behaviors.
     *
     * @return list of behaviors (unmodifiable view)
     */
    public List<SteeringBehavior> getBehaviors() {
        return new ArrayList<>(behaviors);
    }

    /**
     * Gets the maximum force magnitude for combined steering.
     *
     * @return maximum force
     */
    public float getMaxForce() {
        return maxForce;
    }

    /**
     * Sets the maximum force magnitude for combined steering.
     *
     * @param maxForce maximum force
     */
    public void setMaxForce(float maxForce) {
        this.maxForce = maxForce;
    }

    /**
     * Gets the number of registered behaviors.
     *
     * @return behavior count
     */
    public int getBehaviorCount() {
        return behaviors.size();
    }

    /**
     * Gets the number of active behaviors.
     *
     * @return active behavior count
     */
    public int getActiveBehaviorCount() {
        return (int) behaviors.stream().filter(SteeringBehavior::isActive).count();
    }
}
