package me.javavirtualenv.behavior.crepuscular;

/**
 * Configuration for crepuscular behavior patterns.
 * Provides configurable parameters for dawn/dusk activity cycles.
 */
public class CrepuscularConfig {
    // Light level thresholds
    private int emergenceLightLevel = 4;
    private int returnLightLevel = 4;

    // Temperature sensitivity
    private double temperatureModifier = 1.0;
    private boolean useTemperatureModifier = true;

    // Social behavior
    private double groupEmergenceChance = 0.7;
    private int groupDetectionRange = 16;

    // Roosting behavior
    private int roostClusterDistance = 3;
    private int ceilingAttractionRange = 5;

    // Foraging range
    private int foragingRange = 64;

    // Environmental factors
    private boolean affectedByWeather = true;
    private boolean affectedByMoonPhase = true;

    // Time-based thresholds (in ticks, 0-24000)
    private int duskStartTick = 12000;
    private int dawnEndTick = 0;

    /**
     * Creates a new crepuscular configuration with default values.
     */
    public CrepuscularConfig() {
    }

    /**
     * Creates a new crepuscular configuration with custom values.
     */
    public CrepuscularConfig(int emergenceLightLevel, int returnLightLevel,
                            double groupEmergenceChance, double temperatureModifier) {
        this.emergenceLightLevel = emergenceLightLevel;
        this.returnLightLevel = returnLightLevel;
        this.groupEmergenceChance = groupEmergenceChance;
        this.temperatureModifier = temperatureModifier;
    }

    // Getters and Setters

    public int getEmergenceLightLevel() {
        return emergenceLightLevel;
    }

    public void setEmergenceLightLevel(int emergenceLightLevel) {
        this.emergenceLightLevel = Math.max(0, Math.min(15, emergenceLightLevel));
    }

    public int getReturnLightLevel() {
        return returnLightLevel;
    }

    public void setReturnLightLevel(int returnLightLevel) {
        this.returnLightLevel = Math.max(0, Math.min(15, returnLightLevel));
    }

    public double getTemperatureModifier() {
        return temperatureModifier;
    }

    public void setTemperatureModifier(double temperatureModifier) {
        this.temperatureModifier = Math.max(0.5, Math.min(2.0, temperatureModifier));
    }

    public boolean isUseTemperatureModifier() {
        return useTemperatureModifier;
    }

    public void setUseTemperatureModifier(boolean useTemperatureModifier) {
        this.useTemperatureModifier = useTemperatureModifier;
    }

    public double getGroupEmergenceChance() {
        return groupEmergenceChance;
    }

    public void setGroupEmergenceChance(double groupEmergenceChance) {
        this.groupEmergenceChance = Math.max(0.0, Math.min(1.0, groupEmergenceChance));
    }

    public int getGroupDetectionRange() {
        return groupDetectionRange;
    }

    public void setGroupDetectionRange(int groupDetectionRange) {
        this.groupDetectionRange = Math.max(1, Math.min(64, groupDetectionRange));
    }

    public int getRoostClusterDistance() {
        return roostClusterDistance;
    }

    public void setRoostClusterDistance(int roostClusterDistance) {
        this.roostClusterDistance = Math.max(1, Math.min(10, roostClusterDistance));
    }

    public int getCeilingAttractionRange() {
        return ceilingAttractionRange;
    }

    public void setCeilingAttractionRange(int ceilingAttractionRange) {
        this.ceilingAttractionRange = Math.max(1, Math.min(16, ceilingAttractionRange));
    }

    public int getForagingRange() {
        return foragingRange;
    }

    public void setForagingRange(int foragingRange) {
        this.foragingRange = Math.max(16, Math.min(256, foragingRange));
    }

    public boolean isAffectedByWeather() {
        return affectedByWeather;
    }

    public void setAffectedByWeather(boolean affectedByWeather) {
        this.affectedByWeather = affectedByWeather;
    }

    public boolean isAffectedByMoonPhase() {
        return affectedByMoonPhase;
    }

    public void setAffectedByMoonPhase(boolean affectedByMoonPhase) {
        this.affectedByMoonPhase = affectedByMoonPhase;
    }

    public int getDuskStartTick() {
        return duskStartTick;
    }

    public void setDuskStartTick(int duskStartTick) {
        this.duskStartTick = Math.max(11000, Math.min(14000, duskStartTick));
    }

    public int getDawnEndTick() {
        return dawnEndTick;
    }

    public void setDawnEndTick(int dawnEndTick) {
        this.dawnEndTick = Math.max(0, Math.min(2000, dawnEndTick));
    }

    /**
     * Creates a builder pattern for fluent configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CrepuscularConfig config = new CrepuscularConfig();

        public Builder emergenceLightLevel(int level) {
            config.setEmergenceLightLevel(level);
            return this;
        }

        public Builder returnLightLevel(int level) {
            config.setReturnLightLevel(level);
            return this;
        }

        public Builder groupEmergenceChance(double chance) {
            config.setGroupEmergenceChance(chance);
            return this;
        }

        public Builder temperatureModifier(double modifier) {
            config.setTemperatureModifier(modifier);
            return this;
        }

        public Builder foragingRange(int range) {
            config.setForagingRange(range);
            return this;
        }

        public Builder roostClusterDistance(int distance) {
            config.setRoostClusterDistance(distance);
            return this;
        }

        public Builder useTemperature(boolean use) {
            config.setUseTemperatureModifier(use);
            return this;
        }

        public Builder affectedByWeather(boolean affected) {
            config.setAffectedByWeather(affected);
            return this;
        }

        public Builder affectedByMoonPhase(boolean affected) {
            config.setAffectedByMoonPhase(affected);
            return this;
        }

        public CrepuscularConfig build() {
            return config;
        }
    }
}
