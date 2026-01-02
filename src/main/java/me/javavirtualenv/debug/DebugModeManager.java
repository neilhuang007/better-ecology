package me.javavirtualenv.debug;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages debug mode state for the Better Ecology mod.
 * Tracks which players have debug mode enabled and provides
 * utility methods for checking debug state.
 */
public final class DebugModeManager {

    private static final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private static boolean globalDebugEnabled = false;

    private DebugModeManager() {
    }

    /**
     * Enables debug mode for a specific player.
     *
     * @param playerUuid The UUID of the player
     */
    public static void enableForPlayer(UUID playerUuid) {
        if (playerUuid != null) {
            enabledPlayers.add(playerUuid);
        }
    }

    /**
     * Disables debug mode for a specific player.
     *
     * @param playerUuid The UUID of the player
     */
    public static void disableForPlayer(UUID playerUuid) {
        if (playerUuid != null) {
            enabledPlayers.remove(playerUuid);
        }
    }

    /**
     * Toggles debug mode for a specific player.
     *
     * @param playerUuid The UUID of the player
     * @return true if debug mode is now enabled, false if disabled
     */
    public static boolean toggleForPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        if (enabledPlayers.contains(playerUuid)) {
            enabledPlayers.remove(playerUuid);
            return false;
        } else {
            enabledPlayers.add(playerUuid);
            return true;
        }
    }

    /**
     * Checks if debug mode is enabled for a specific player.
     *
     * @param playerUuid The UUID of the player
     * @return true if debug mode is enabled for this player
     */
    public static boolean isEnabledForPlayer(UUID playerUuid) {
        return playerUuid != null && enabledPlayers.contains(playerUuid);
    }

    /**
     * Enables global debug mode for all players.
     */
    public static void enableGlobal() {
        globalDebugEnabled = true;
    }

    /**
     * Disables global debug mode.
     */
    public static void disableGlobal() {
        globalDebugEnabled = false;
    }

    /**
     * Toggles global debug mode.
     *
     * @return true if global debug mode is now enabled
     */
    public static boolean toggleGlobal() {
        globalDebugEnabled = !globalDebugEnabled;
        return globalDebugEnabled;
    }

    /**
     * Checks if global debug mode is enabled.
     *
     * @return true if global debug mode is enabled
     */
    public static boolean isGlobalEnabled() {
        return globalDebugEnabled;
    }

    /**
     * Checks if debug mode is active (either global or for any player).
     *
     * @return true if any debug mode is active
     */
    public static boolean isAnyDebugActive() {
        return globalDebugEnabled || !enabledPlayers.isEmpty();
    }

    /**
     * Gets an unmodifiable view of all players with debug mode enabled.
     *
     * @return Set of player UUIDs with debug enabled
     */
    public static Set<UUID> getEnabledPlayers() {
        return Collections.unmodifiableSet(enabledPlayers);
    }

    /**
     * Clears all debug state. Called on server shutdown.
     */
    public static void reset() {
        enabledPlayers.clear();
        globalDebugEnabled = false;
    }
}
