package me.javavirtualenv.behavior.pathfinding.steering;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Base interface for steering behaviors based on Craig Reynolds' model.
 * Each behavior calculates a force vector that influences entity movement.
 * Steering behaviors can be combined to create complex, emergent movement patterns.
 */
public interface SteeringBehavior {
    /**
     * Calculates the steering force for this behavior.
     * The force represents the desired change in velocity to achieve the behavior's goal.
     * Forces from multiple behaviors are combined (weighted sum) to produce final movement.
     *
     * @param mob the entity being steered
     * @param context contextual information for calculations (target, neighbors, constraints)
     * @return force vector to apply (will be combined with other behaviors)
     */
    Vec3 calculate(Mob mob, SteeringContext context);

    /**
     * Weight of this behavior for blending with others.
     * Higher weights give this behavior more influence on final movement.
     * Typical range: 0.5 - 2.0
     *
     * @return weight multiplier for this behavior's force
     */
    float getWeight();

    /**
     * Whether this behavior is currently active.
     * Inactive behaviors are not calculated or blended.
     *
     * @return true if this behavior should influence movement
     */
    default boolean isActive() {
        return true;
    }
}
