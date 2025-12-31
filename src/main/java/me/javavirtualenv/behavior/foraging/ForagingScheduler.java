package me.javavirtualenv.behavior.foraging;

import net.minecraft.world.level.Level;

/**
 * Manages time-based foraging activity patterns.
 * Implements diurnal patterns, seasonal variations, and species-specific schedules.
 */
public class ForagingScheduler {
    private final ForagingConfig config;
    private final ForagingPattern pattern;
    private final int baseForagingTime;

    private long lastCheckTime;
    private boolean wasForagingLastCheck;

    public ForagingScheduler(ForagingConfig config, ForagingPattern pattern) {
        this.config = config;
        this.pattern = pattern;
        this.baseForagingTime = calculateBaseForagingTime();
        this.lastCheckTime = -1;
        this.wasForagingLastCheck = false;
    }

    /**
     * Checks if the current time is appropriate for foraging.
     * Considers day/night cycle and rest periods.
     */
    public boolean isForagingTime(Level level) {
        long dayTime = getDayTime(level);
        lastCheckTime = dayTime;

        boolean isForaging = checkForagingWindow(dayTime);
        wasForagingLastCheck = isForaging;

        return isForaging;
    }

    /**
     * Gets the current foraging intensity (0.0 to 1.0).
     * Higher during peak foraging times.
     */
    public double getForagingIntensity(Level level) {
        if (!isForagingTime(level)) {
            return 0.0;
        }

        long dayTime = getDayTime(level);
        return calculateIntensity(dayTime);
    }

    /**
     * Gets seasonal modifier for foraging behavior.
     * Returns > 1.0 in spring/winter (more foraging needed), < 1.0 in summer/autumn.
     */
    public double getSeasonalModifier(Level level) {
        // Minecraft doesn't have real seasons, but we can use day cycle
        // to simulate seasonal-like variations in behavior
        long dayTime = getDayTime(level);
        long dayNumber = level.getGameTime() / 24000;

        // Simple seasonal simulation based on in-game days
        // Assumes 128-day cycle for "seasons"
        double seasonPhase = (dayNumber % 128) / 128.0 * 2.0 * Math.PI;

        // Spring/winter: higher foraging (resources scarcer)
        // Summer/autumn: lower foraging (resources abundant)
        double seasonalFactor = 1.0 + 0.3 * Math.sin(seasonPhase);

        return Math.max(0.7, Math.min(1.4, seasonalFactor));
    }

    /**
     * Checks if the animal is in a rest period.
     */
    public boolean isRestPeriod(Level level) {
        long dayTime = getDayTime(level);

        if (pattern == ForagingPattern.CONTINUOUS) {
            return false;
        }

        long restStart = config.getMiddayRestStart();
        long restEnd = config.getMiddayRestEnd();

        return dayTime >= restStart && dayTime < restEnd;
    }

    /**
     * Gets the time until next foraging period.
     */
    public long getTimeUntilNextForaging(Level level) {
        long dayTime = getDayTime(level);
        long startTime = config.getGrazingStartTime();

        if (dayTime < startTime) {
            return startTime - dayTime;
        }

        long endTime = config.getGrazingEndTime();
        if (dayTime > endTime) {
            return (24000 - dayTime) + startTime;
        }

        return 0;
    }

    private boolean checkForagingWindow(long dayTime) {
        long startTime = config.getGrazingStartTime();
        long endTime = config.getGrazingEndTime();

        switch (pattern) {
            case BIMODAL:
                return checkBimodalPattern(dayTime, startTime, endTime);
            case CONTINUOUS:
                return dayTime >= startTime && dayTime < endTime;
            case NOCTURNAL:
                return checkNocturnalPattern(dayTime);
            case CREPUSCULAR:
                return checkCrepuscularPattern(dayTime);
            default:
                return dayTime >= startTime && dayTime < endTime;
        }
    }

    private boolean checkBimodalPattern(long dayTime, long startTime, long endTime) {
        long restStart = config.getMiddayRestStart();
        long restEnd = config.getMiddayRestEnd();

        boolean morningPeriod = dayTime >= startTime && dayTime < restStart;
        boolean afternoonPeriod = dayTime >= restEnd && dayTime < endTime;

        return morningPeriod || afternoonPeriod;
    }

    private boolean checkNocturnalPattern(long dayTime) {
        // Active during night (12000-24000)
        return dayTime >= 12000 || dayTime < 4000;
    }

    private boolean checkCrepuscularPattern(long dayTime) {
        // Active at dawn and dusk
        boolean dawn = dayTime >= 22000 || dayTime < 3000;
        boolean dusk = dayTime >= 11000 && dayTime < 14000;
        return dawn || dusk;
    }

    private double calculateIntensity(long dayTime) {
        switch (pattern) {
            case BIMODAL:
                return calculateBimodalIntensity(dayTime);
            case CONTINUOUS:
                return 0.8;
            case NOCTURNAL:
                return calculateNocturnalIntensity(dayTime);
            case CREPUSCULAR:
                return calculateCrepuscularIntensity(dayTime);
            default:
                return 0.7;
        }
    }

    private double calculateBimodalIntensity(long dayTime) {
        long restStart = config.getMiddayRestStart();
        long restEnd = config.getMiddayRestEnd();
        long startTime = config.getGrazingStartTime();
        long endTime = config.getGrazingEndTime();

        if (dayTime >= restStart && dayTime < restEnd) {
            return 0.0;
        }

        double morningPeak = startTime + (restStart - startTime) * 0.4;
        double afternoonPeak = restEnd + (endTime - restEnd) * 0.5;

        double morningIntensity = 0.0;
        double afternoonIntensity = 0.0;

        if (dayTime >= startTime && dayTime < restStart) {
            double distanceFromPeak = Math.abs(dayTime - morningPeak);
            double maxDistance = (restStart - startTime) * 0.5;
            morningIntensity = 1.0 - (distanceFromPeak / maxDistance);
        }

        if (dayTime >= restEnd && dayTime < endTime) {
            double distanceFromPeak = Math.abs(dayTime - afternoonPeak);
            double maxDistance = (endTime - restEnd) * 0.5;
            afternoonIntensity = 1.0 - (distanceFromPeak / maxDistance);
        }

        return Math.max(0.0, Math.max(morningIntensity, afternoonIntensity));
    }

    private double calculateNocturnalIntensity(long dayTime) {
        double midnight = 0;
        double distanceFromMidnight = Math.abs(dayTime - midnight);
        if (distanceFromMidnight > 12000) {
            distanceFromMidnight = 24000 - distanceFromMidnight;
        }

        return Math.max(0.3, 1.0 - (distanceFromMidnight / 6000.0));
    }

    private double calculateCrepuscularIntensity(long dayTime) {
        double dawnPeak = 24000;
        double duskPeak = 12500;

        double dawnDistance = Math.min(Math.abs(dayTime - dawnPeak), Math.abs(dayTime - (dawnPeak - 24000)));
        double duskDistance = Math.abs(dayTime - duskPeak);

        double dawnIntensity = Math.max(0, 1.0 - (dawnDistance / 3000.0));
        double duskIntensity = Math.max(0, 1.0 - (duskDistance / 3000.0));

        return Math.max(dawnIntensity, duskIntensity);
    }

    private long getDayTime(Level level) {
        return level.getDayTime() % 24000;
    }

    private int calculateBaseForagingTime() {
        switch (pattern) {
            case CONTINUOUS:
                return 480; // 24 seconds (50-80% of day for horses)
            case BIMODAL:
                return 342; // ~17 seconds (8.57 hours equivalent for sheep)
            case SELECTIVE:
                return 300;
            case NOCTURNAL:
                return 360;
            case CREPUSCULAR:
                return 240;
            default:
                return 300;
        }
    }

    public boolean wasRecentlyForaging() {
        return wasForagingLastCheck;
    }

    public enum ForagingPattern {
        BIMODAL,      // Morning and afternoon peaks (cattle, sheep)
        CONTINUOUS,   // Steady grazing throughout day (horses)
        SELECTIVE,    // More selective, intermittent (deer, goats)
        NOCTURNAL,    // Active at night
        CREPUSCULAR   // Active at dawn and dusk
    }
}
