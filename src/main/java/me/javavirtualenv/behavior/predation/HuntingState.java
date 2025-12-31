package me.javavirtualenv.behavior.predation;

/**
 * Represents the current state of a predator's hunting behavior.
 * Used to track hunt progress and enable state-dependent decision making.
 */
public enum HuntingState {
    /**
     * Predator is not actively hunting, may be resting or wandering.
     */
    IDLE,

    /**
     * Predator is looking for prey, scanning environment.
     */
    SEARCHING,

    /**
     * Predator has spotted prey and is approaching stealthily.
     */
    STALKING,

    /**
     * Predator is actively pursuing prey at full speed.
     */
    CHASING,

    /**
     * Predator is in attack range and attempting to catch prey.
     */
    ATTACKING,

    /**
     * Predator has caught prey and is consuming it.
     */
    EATING,

    /**
     * Predator is recovering energy after hunt or meal.
     */
    RESTING
}
