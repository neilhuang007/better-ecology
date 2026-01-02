package me.javavirtualenv.ecology.seasonal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.level.ServerLevel;

/**
 * Global season manager for Better Ecology.
 * Provides manual season override functionality and persists season state.
 */
public final class SeasonManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonManager.class);

    // Dimension-specific season overrides
    private static final Map<String, SeasonOverride> dimensionOverrides = new HashMap<>();

    // Event listeners for season changes
    private static final Map<UUID, SeasonChangeListener> listeners = new HashMap<>();

    private SeasonManager() {
        // Utility class
    }

    /**
     * Sets a season override for a specific dimension.
     *
     * @param dimensionId The dimension ID (e.g., "minecraft:overworld")
     * @param season      The season to set, or null for auto mode
     */
    public static void setSeason(String dimensionId, SeasonalContext.Season season) {
        SeasonOverride previous = dimensionOverrides.get(dimensionId);
        SeasonOverride override = season != null ? new SeasonOverride(season) : null;

        if (season != null) {
            dimensionOverrides.put(dimensionId, override);
            LOGGER.info("Season override set for dimension {}: {}", dimensionId, season);
        } else {
            dimensionOverrides.remove(dimensionId);
            LOGGER.info("Auto mode enabled for dimension {}", dimensionId);
        }

        // Notify listeners of season change
        if (previous == null || !previous.equals(override)) {
            notifySeasonChange(dimensionId, previous != null ? previous.season : null, season);
        }
    }

    /**
     * Sets a season override for the current server level.
     *
     * @param level  The server level
     * @param season The season to set, or null for auto mode
     */
    public static void setSeason(ServerLevel level, SeasonalContext.Season season) {
        String dimensionId = level.dimension().location().toString();
        setSeason(dimensionId, season);
    }

    /**
     * Enables auto mode for a dimension (uses world time to determine season).
     *
     * @param dimensionId The dimension ID
     */
    public static void setAutoMode(String dimensionId) {
        setSeason(dimensionId, null);
    }

    /**
     * Enables auto mode for a server level.
     *
     * @param level The server level
     */
    public static void setAutoMode(ServerLevel level) {
        setSeason(level, null);
    }

    /**
     * Gets the current season for a dimension.
     * Returns override if set, otherwise calculates from world time.
     *
     * @param level The server level
     * @return Current season
     */
    public static SeasonalContext.Season getCurrentSeason(ServerLevel level) {
        String dimensionId = level.dimension().location().toString();
        SeasonOverride override = dimensionOverrides.get(dimensionId);

        if (override != null) {
            return override.season;
        }

        // Auto mode: calculate from world time
        return SeasonalContext.getSeason(level);
    }

    /**
     * Checks if a dimension has a season override.
     *
     * @param dimensionId The dimension ID
     * @return true if override is active
     */
    public static boolean hasOverride(String dimensionId) {
        return dimensionOverrides.containsKey(dimensionId);
    }

    /**
     * Checks if a level has a season override.
     *
     * @param level The server level
     * @return true if override is active
     */
    public static boolean hasOverride(ServerLevel level) {
        String dimensionId = level.dimension().location().toString();
        return hasOverride(dimensionId);
    }

    /**
     * Gets the override season for a dimension.
     *
     * @param dimensionId The dimension ID
     * @return Override season, or null if auto mode
     */
    public static SeasonalContext.Season getOverrideSeason(String dimensionId) {
        SeasonOverride override = dimensionOverrides.get(dimensionId);
        return override != null ? override.season : null;
    }

    /**
     * Gets the override season for a level.
     *
     * @param level The server level
     * @return Override season, or null if auto mode
     */
    public static SeasonalContext.Season getOverrideSeason(ServerLevel level) {
        String dimensionId = level.dimension().location().toString();
        return getOverrideSeason(dimensionId);
    }

    /**
     * Clears all season overrides (resets to auto mode for all dimensions).
     */
    public static void clearAllOverrides() {
        dimensionOverrides.clear();
        LOGGER.info("All season overrides cleared, auto mode enabled for all dimensions");
    }

    /**
     * Registers a listener for season changes.
     *
     * @param listener The listener to register
     * @return Unique ID for this listener
     */
    public static UUID registerListener(SeasonChangeListener listener) {
        UUID id = UUID.randomUUID();
        listeners.put(id, listener);
        return id;
    }

    /**
     * Unregisters a season change listener.
     *
     * @param listenerId The listener ID
     */
    public static void unregisterListener(UUID listenerId) {
        listeners.remove(listenerId);
    }

    /**
     * Notifies all listeners of a season change.
     */
    private static void notifySeasonChange(String dimensionId, SeasonalContext.Season previous,
            SeasonalContext.Season current) {
        SeasonChangeEvent event = new SeasonChangeEvent(dimensionId, previous, current);

        for (SeasonChangeListener listener : listeners.values()) {
            try {
                listener.onSeasonChange(event);
            } catch (Exception e) {
                LOGGER.error("Error notifying season change listener", e);
            }
        }
    }

    /**
     * Season override data holder.
     */
    private static class SeasonOverride {
        final SeasonalContext.Season season;
        final long setTime;

        SeasonOverride(SeasonalContext.Season season) {
            this.season = season;
            this.setTime = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj instanceof SeasonOverride other) {
                return this.season == other.season;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return season.hashCode();
        }
    }

    /**
     * Event representing a season change.
     */
    public static class SeasonChangeEvent {
        private final String dimensionId;
        private final SeasonalContext.Season previousSeason;
        private final SeasonalContext.Season currentSeason;

        public SeasonChangeEvent(String dimensionId, SeasonalContext.Season previousSeason,
                SeasonalContext.Season currentSeason) {
            this.dimensionId = dimensionId;
            this.previousSeason = previousSeason;
            this.currentSeason = currentSeason;
        }

        public String getDimensionId() {
            return dimensionId;
        }

        public SeasonalContext.Season getPreviousSeason() {
            return previousSeason;
        }

        public SeasonalContext.Season getCurrentSeason() {
            return currentSeason;
        }

        public boolean isEntering(SeasonalContext.Season season) {
            return currentSeason == season;
        }

        public boolean isLeaving(SeasonalContext.Season season) {
            return previousSeason == season;
        }
    }

    /**
     * Listener interface for season changes.
     */
    public interface SeasonChangeListener {
        void onSeasonChange(SeasonChangeEvent event);
    }
}
