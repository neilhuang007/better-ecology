package me.javavirtualenv.behavior;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;

/**
 * Functional interface for behavior rules in the ecology system.
 * <p>
 * Behavior rules calculate steering forces that influence entity movement.
 * Each rule takes a context containing entity state and returns a Vec3d
 * representing the desired force or velocity change.
 * <p>
 * This design follows a simpler functional approach compared to the
 * four-phase Rule interface (reset/consider/process/finish) from the
 * reference implementation, making it easier to compose and optimize
 * for Minecraft's entity system.
 *
 * @see BehaviorContext
 * @see BehaviorRegistry
 */
@FunctionalInterface
public interface BehaviorRule {

    /**
     * Calculates the behavioral force for an entity given its context.
     * <p>
     * Implementations should:
     * <ul>
     *   <li>Return a zero vector if the behavior doesn't apply</li>
     *   <li>Handle null/edge cases gracefully</li>
     *   <li>Avoid excessive object allocations for performance</li>
     *   <li>Consider the entity's current state when calculating forces</li>
     * </ul>
     *
     * @param context The behavior context containing entity position, velocity, world data
     * @return A Vec3d representing the calculated steering force (may be zero)
     */
    Vec3d calculate(BehaviorContext context);
}
