package me.javavirtualenv.debug;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.function.Supplier;

/**
 * Centralized debug logging for Better Ecology behaviors.
 * Uses lazy evaluation to avoid string formatting overhead when logging is disabled.
 */
public final class BehaviorLogger {

    /**
     * Log level for behavior debugging.
     */
    public enum LogLevel {
        /** No logging output */
        OFF,
        /** Log only significant events (goal registration, state changes) */
        MINIMAL,
        /** Log all details including per-tick updates and force vectors */
        VERBOSE
    }

    private static LogLevel logLevel = LogLevel.OFF;

    private BehaviorLogger() {
    }

    /**
     * Gets the current log level.
     */
    public static LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the log level for behavior debugging.
     */
    public static void setLogLevel(LogLevel level) {
        logLevel = level;
        if (level != LogLevel.OFF) {
            BetterEcology.LOGGER.info("[BetterEcology] Debug logging set to {}", level);
        }
    }

    /**
     * Checks if logging is enabled at any level.
     */
    public static boolean isEnabled() {
        return logLevel != LogLevel.OFF;
    }

    /**
     * Checks if verbose logging is enabled.
     */
    public static boolean isVerbose() {
        return logLevel == LogLevel.VERBOSE;
    }

    /**
     * Checks if minimal logging is enabled.
     */
    public static boolean isMinimal() {
        return logLevel == LogLevel.MINIMAL || logLevel == LogLevel.VERBOSE;
    }

    /**
     * Logs goal registration events.
     * Logged at MINIMAL level and above.
     *
     * @param mob The mob registering the goal
     * @param goalName The name of the goal being registered
     * @param priority The priority of the goal
     */
    public static void logGoalRegistration(Mob mob, String goalName, int priority) {
        if (!isMinimal()) {
            return;
        }
        BetterEcology.LOGGER.info("[BetterEcology/{}@{}] Registered goal: {} (priority: {})",
                getEntityType(mob), mob.getId(), goalName, priority);
    }

    /**
     * Logs behavior tick events.
     * Logged at VERBOSE level only.
     *
     * @param mob The mob being ticked
     * @param behaviorId The behavior identifier
     * @param message The message to log
     */
    public static void logBehaviorTick(Mob mob, String behaviorId, String message) {
        if (!isVerbose()) {
            return;
        }
        BetterEcology.LOGGER.debug("[BetterEcology/{}@{}] {}: {}",
                getEntityType(mob), mob.getId(), behaviorId, message);
    }

    /**
     * Logs behavior tick events with lazy message evaluation.
     * Logged at VERBOSE level only.
     *
     * @param mob The mob being ticked
     * @param behaviorId The behavior identifier
     * @param messageSupplier Supplier for the message (only evaluated if logging is enabled)
     */
    public static void logBehaviorTick(Mob mob, String behaviorId, Supplier<String> messageSupplier) {
        if (!isVerbose()) {
            return;
        }
        BetterEcology.LOGGER.debug("[BetterEcology/{}@{}] {}: {}",
                getEntityType(mob), mob.getId(), behaviorId, messageSupplier.get());
    }

    /**
     * Logs flocking force vectors.
     * Logged at VERBOSE level only.
     *
     * @param entity The entity experiencing the force
     * @param forceType The type of force (separation, alignment, cohesion, etc.)
     * @param force The force vector
     */
    public static void logFlockingForce(Entity entity, String forceType, Vec3d force) {
        if (!isVerbose()) {
            return;
        }
        BetterEcology.LOGGER.debug("[BetterEcology/{}@{}] Flocking {}: (x={}, y={}, z={}, mag={})",
                getEntityType(entity), entity.getId(), forceType,
                formatDouble(force.x), formatDouble(force.y), formatDouble(force.z),
                formatDouble(force.magnitude()));
    }

    /**
     * Logs state changes for an entity.
     * Logged at MINIMAL level and above.
     *
     * @param mob The mob whose state changed
     * @param stateKey The key of the state that changed
     * @param oldValue The previous value
     * @param newValue The new value
     */
    public static void logStateChange(Mob mob, String stateKey, Object oldValue, Object newValue) {
        if (!isMinimal()) {
            return;
        }
        BetterEcology.LOGGER.info("[BetterEcology/{}@{}] State change: {} = {} -> {}",
                getEntityType(mob), mob.getId(), stateKey, oldValue, newValue);
    }

    /**
     * Logs handle registration events.
     * Logged at MINIMAL level and above.
     *
     * @param handleId The handle identifier
     */
    public static void logHandleRegistration(String handleId) {
        if (!isMinimal()) {
            return;
        }
        BetterEcology.LOGGER.info("[BetterEcology] Registered handle: {}", handleId);
    }

    /**
     * Logs handle goal registration for a specific mob.
     * Logged at VERBOSE level only.
     *
     * @param mob The mob for which goals are being registered
     * @param handleId The handle identifier
     */
    public static void logHandleGoalsRegistered(Mob mob, String handleId) {
        if (!isVerbose()) {
            return;
        }
        BetterEcology.LOGGER.debug("[BetterEcology/{}@{}] Handle {} registered goals",
                getEntityType(mob), mob.getId(), handleId);
    }

    /**
     * Logs a general debug message.
     * Logged at VERBOSE level only.
     *
     * @param message The message to log
     */
    public static void debug(String message) {
        if (!isVerbose()) {
            return;
        }
        BetterEcology.LOGGER.debug("[BetterEcology] {}", message);
    }

    /**
     * Logs a general debug message with lazy evaluation.
     * Logged at VERBOSE level only.
     *
     * @param messageSupplier Supplier for the message (only evaluated if logging is enabled)
     */
    public static void debug(Supplier<String> messageSupplier) {
        if (!isVerbose()) {
            return;
        }
        BetterEcology.LOGGER.debug("[BetterEcology] {}", messageSupplier.get());
    }

    /**
     * Logs an info-level message.
     * Logged at MINIMAL level and above.
     *
     * @param message The message to log
     */
    public static void info(String message) {
        if (!isMinimal()) {
            return;
        }
        BetterEcology.LOGGER.info("[BetterEcology] {}", message);
    }

    /**
     * Extracts the entity type name from an entity.
     */
    private static String getEntityType(Entity entity) {
        return entity.getType().toShortString();
    }

    /**
     * Formats a double to 3 decimal places for readable logging.
     */
    private static String formatDouble(double value) {
        return String.format("%.3f", value);
    }
}
