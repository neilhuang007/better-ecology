package me.javavirtualenv.behavior.sleeping;

/**
 * Configuration for sleeping and resting behaviors.
 * Provides configurable parameters for activity cycles, sleep site selection,
 * vigilance, social sleeping, and awakening behaviors.
 */
public class SleepingConfig {

    // Activity cycle settings
    private ActivityCycle activityCycle = ActivityCycle.DIURNAL;
    private int sleepStart = 13000;
    private int sleepEnd = 1000;
    private int sleepDuration = 6000;

    // Vigilance settings
    private double vigilanceThreshold = 0.5;
    private double socialSleepBonus = 0.3;
    private double lightSleepThreshold = 0.3;
    private double deepSleepThreshold = 0.7;

    // Sentinel behavior
    private boolean sentinelDuty = true;
    private int sentinelRotationInterval = 1200;

    // Sleep site selection
    private int maxSleepSiteDistance = 32;
    private boolean preferCoveredSites = true;
    private boolean preferElevatedSites = false;

    // Social sleeping
    private double groupSleepRadius = 8.0;

    // Posture and awakening
    private SleepPosture posture = SleepPosture.LYING_DOWN;
    private int rapidAwakeningResponseTicks = 5;

    private SleepingConfig() {
    }

    public ActivityCycle getActivityCycle() {
        return activityCycle;
    }

    public int getSleepStart() {
        return sleepStart;
    }

    public int getSleepEnd() {
        return sleepEnd;
    }

    public int getSleepDuration() {
        return sleepDuration;
    }

    public double getVigilanceThreshold() {
        return vigilanceThreshold;
    }

    public double getSocialSleepBonus() {
        return socialSleepBonus;
    }

    public boolean isSentinelDuty() {
        return sentinelDuty;
    }

    public int getSentinelRotationInterval() {
        return sentinelRotationInterval;
    }

    public int getMaxSleepSiteDistance() {
        return maxSleepSiteDistance;
    }

    public boolean isPreferCoveredSites() {
        return preferCoveredSites;
    }

    public boolean isPreferElevatedSites() {
        return preferElevatedSites;
    }

    public double getGroupSleepRadius() {
        return groupSleepRadius;
    }

    public SleepPosture getPosture() {
        return posture;
    }

    public int getRapidAwakeningResponseTicks() {
        return rapidAwakeningResponseTicks;
    }

    public double getLightSleepThreshold() {
        return lightSleepThreshold;
    }

    public double getDeepSleepThreshold() {
        return deepSleepThreshold;
    }

    public enum ActivityCycle {
        DIURNAL,    // Active day, sleep night
        NOCTURNAL,  // Active night, sleep day
        CREPUSCULAR // Active dawn/dusk
    }

    public enum SleepPosture {
        LYING_DOWN,
        SITTING,
        STANDING,
        CURLING_UP,
        ALERT_REST,
        SITTING_ALERT,
        CURLED_UP
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a species-specific sleeping configuration.
     */
    public static SleepingConfig forSpecies(String species) {
        if (species == null) {
            return builder().build();
        }

        String speciesLower = species.toLowerCase();

        return switch (speciesLower) {
            case "cow", "sheep", "pig", "chicken" -> builder()
                    .activityCycle(ActivityCycle.DIURNAL)
                    .sleepStart(13000)
                    .sleepEnd(1000)
                    .sleepDuration(6000)
                    .sentinelDuty(true)
                    .socialSleepBonus(speciesLower.equals("sheep") ? 0.5 : 0.3)
                    .groupSleepRadius(8.0)
                    .posture(SleepPosture.LYING_DOWN)
                    .build();
            case "wolf" -> builder()
                    .activityCycle(ActivityCycle.NOCTURNAL)
                    .sleepStart(6000)
                    .sleepEnd(18000)
                    .sleepDuration(7000)
                    .sentinelDuty(true)
                    .socialSleepBonus(0.5)
                    .groupSleepRadius(12.0)
                    .posture(SleepPosture.LYING_DOWN)
                    .build();
            case "rabbit", "fox", "deer" -> builder()
                    .activityCycle(ActivityCycle.CREPUSCULAR)
                    .sleepStart(4000)
                    .sleepEnd(9000)
                    .sleepDuration(5000)
                    .sentinelDuty(false)
                    .vigilanceThreshold(0.3)
                    .posture(SleepPosture.SITTING_ALERT)
                    .build();
            case "cat", "ocelot" -> builder()
                    .activityCycle(ActivityCycle.CREPUSCULAR)
                    .sleepStart(12000)
                    .sleepEnd(2000)
                    .sleepDuration(10000)
                    .sentinelDuty(false)
                    .posture(SleepPosture.CURLED_UP)
                    .build();
            default -> builder()
                    .activityCycle(ActivityCycle.DIURNAL)
                    .sleepDuration(6000)
                    .build();
        };
    }

    public static class Builder {
        private SleepingConfig config;

        private Builder() {
            this.config = new SleepingConfig();
        }

        public Builder activityCycle(ActivityCycle cycle) {
            config.activityCycle = cycle;
            return this;
        }

        public Builder sleepStart(int ticks) {
            config.sleepStart = ticks;
            return this;
        }

        public Builder sleepEnd(int ticks) {
            config.sleepEnd = ticks;
            return this;
        }

        public Builder sleepDuration(int ticks) {
            config.sleepDuration = ticks;
            return this;
        }

        public Builder vigilanceThreshold(double threshold) {
            config.vigilanceThreshold = threshold;
            return this;
        }

        public Builder socialSleepBonus(double bonus) {
            config.socialSleepBonus = bonus;
            return this;
        }

        public Builder sentinelDuty(boolean enabled) {
            config.sentinelDuty = enabled;
            return this;
        }

        public Builder sentinelRotationInterval(int ticks) {
            config.sentinelRotationInterval = ticks;
            return this;
        }

        public Builder maxSleepSiteDistance(int distance) {
            config.maxSleepSiteDistance = distance;
            return this;
        }

        public Builder preferCoveredSites(boolean prefer) {
            config.preferCoveredSites = prefer;
            return this;
        }

        public Builder preferElevatedSites(boolean prefer) {
            config.preferElevatedSites = prefer;
            return this;
        }

        public Builder groupSleepRadius(double radius) {
            config.groupSleepRadius = radius;
            return this;
        }

        public Builder posture(SleepPosture posture) {
            config.posture = posture;
            return this;
        }

        public Builder rapidAwakeningResponseTicks(int ticks) {
            config.rapidAwakeningResponseTicks = ticks;
            return this;
        }

        public Builder lightSleepThreshold(double threshold) {
            config.lightSleepThreshold = threshold;
            return this;
        }

        public Builder deepSleepThreshold(double threshold) {
            config.deepSleepThreshold = threshold;
            return this;
        }

        public SleepingConfig build() {
            SleepingConfig result = config;
            config = new SleepingConfig();
            return result;
        }
    }
}
