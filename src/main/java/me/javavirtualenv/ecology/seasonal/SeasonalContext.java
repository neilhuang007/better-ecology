package me.javavirtualenv.ecology.seasonal;

import net.minecraft.server.level.ServerLevel;

/**
 * Provides seasonal and time-of-day context for entity behaviors.
 * Determines current season and time period from world time.
 */
public final class SeasonalContext {

    private SeasonalContext() {
        // Utility class
    }

    /**
     * Seasons in Minecraft world.
     * Each season lasts 20 in-game days by default.
     */
    public enum Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    /**
     * Time periods during a day.
     * Based on Minecraft's day/night cycle (0 = dawn, 6000 = noon, 12000 = dusk,
     * 18000 = midnight)
     */
    public enum TimePeriod {
        DAWN(0, 2000),
        DAY(2000, 12000),
        DUSK(12000, 14000),
        NIGHT(14000, 24000);

        private final int startTime;
        private final int endTime;

        TimePeriod(int startTime, int endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public int getStartTime() {
            return startTime;
        }

        public int getEndTime() {
            return endTime;
        }
    }

    /**
     * Activity pattern types for different animals.
     */
    public enum ActivityPattern {
        DIURNAL, // Active during day
        NOCTURNAL, // Active during night
        CREPUSCULAR, // Active at dawn and dusk
        CATHEMERAL // Active throughout day and night
    }

    /**
     * Get the current season based on world time.
     * Each season lasts 20 days (240000 ticks).
     *
     * @param level The server level
     * @return Current season
     */
    public static Season getSeason(ServerLevel level) {
        long dayCount = level.getDayTime() / 24000L;
        long yearDay = dayCount % 80L; // 80 days per year (20 days per season)

        int seasonIndex = (int) (yearDay / 20L);
        return Season.values()[seasonIndex];
    }

    /**
     * Get the current season.
     * Uses SeasonManager if available, otherwise calculates from world time.
     *
     * @param level The server level
     * @return Current season
     */
    public static Season getCurrentSeason(ServerLevel level) {
        return SeasonManager.getCurrentSeason(level);
    }

    /**
     * Get the current time period based on world time.
     *
     * @param level The server level
     * @return Current time period
     */
    public static TimePeriod getTimePeriod(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;

        for (TimePeriod period : TimePeriod.values()) {
            if (dayTime >= period.getStartTime() && dayTime < period.getEndTime()) {
                return period;
            }
        }

        return TimePeriod.DAY; // Default fallback
    }

    /**
     * Get activity multiplier based on season.
     * Spring: +20% activity
     * Summer: +10% activity
     * Autumn: normal activity
     * Winter: -30% activity
     *
     * @param season The season
     * @return Activity multiplier (1.0 = normal, higher = more active)
     */
    public static double getSeasonalActivityMultiplier(Season season) {
        return switch (season) {
            case SPRING -> 1.2;
            case SUMMER -> 1.1;
            case AUTUMN -> 1.0;
            case WINTER -> 0.7;
        };
    }

    /**
     * Get breeding multiplier based on season.
     * Spring: +50% breeding
     * Summer: normal breeding
     * Autumn: +20% breeding (preparing for winter)
     * Winter: -80% breeding (dormant)
     *
     * @param season The season
     * @return Breeding multiplier (1.0 = normal, higher = more breeding)
     */
    public static double getSeasonalBreedingMultiplier(Season season) {
        return switch (season) {
            case SPRING -> 1.5;
            case SUMMER -> 1.0;
            case AUTUMN -> 1.2;
            case WINTER -> 0.2;
        };
    }

    /**
     * Get movement speed multiplier based on season.
     * Spring: +10% movement
     * Summer: normal movement
     * Autumn: normal movement
     * Winter: -20% movement (snow, cold)
     *
     * @param season The season
     * @return Movement multiplier (1.0 = normal)
     */
    public static double getSeasonalMovementMultiplier(Season season) {
        return switch (season) {
            case SPRING -> 1.1;
            case SUMMER -> 1.0;
            case AUTUMN -> 1.0;
            case WINTER -> 0.8;
        };
    }

    /**
     * Get hunger burn rate multiplier based on season.
     * Spring: normal hunger burn
     * Summer: -20% hunger burn (abundant food)
     * Autumn: normal hunger burn
     * Winter: +50% hunger burn (cold stress)
     *
     * @param season The season
     * @return Hunger burn multiplier (1.0 = normal, higher = faster burn)
     */
    public static double getSeasonalHungerMultiplier(Season season) {
        return switch (season) {
            case SPRING -> 1.0;
            case SUMMER -> 0.8;
            case AUTUMN -> 1.0;
            case WINTER -> 1.5;
        };
    }

    /**
     * Check if an entity is active based on its activity pattern and current time.
     *
     * @param pattern The entity's activity pattern
     * @param period  The current time period
     * @return true if the entity should be active
     */
    public static boolean isActive(ActivityPattern pattern, TimePeriod period) {
        return switch (pattern) {
            case DIURNAL -> period == TimePeriod.DAY || period == TimePeriod.DAWN;
            case NOCTURNAL -> period == TimePeriod.NIGHT || period == TimePeriod.DUSK;
            case CREPUSCULAR -> period == TimePeriod.DAWN || period == TimePeriod.DUSK;
            case CATHEMERAL -> true; // Always active
        };
    }

    /**
     * Get activity multiplier based on activity pattern and current time.
     * Returns values between 0.0 (inactive) and 1.0 (fully active).
     *
     * @param pattern The entity's activity pattern
     * @param period  The current time period
     * @return Activity multiplier
     */
    public static double getActivityMultiplier(ActivityPattern pattern, TimePeriod period) {
        if (!isActive(pattern, period)) {
            return 0.0;
        }

        // Crepuscular animals are most active at dawn/dusk
        if (pattern == ActivityPattern.CREPUSCULAR) {
            if (period == TimePeriod.DAWN || period == TimePeriod.DUSK) {
                return 1.5; // Extra active during crepuscular periods
            }
        }

        return 1.0;
    }
}
