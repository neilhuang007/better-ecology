package me.javavirtualenv.behavior.crepuscular;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Determines dawn/dusk activity patterns for crepuscular creatures.
 * Calculates optimal activity times based on light levels and time of day.
 */
public class CrepuscularActivity {
    private final CrepuscularConfig config;

    // Time constants (in ticks, 0-24000 per day)
    private static final int DAY_START = 0;
    private static final int SUNRISE_START = 23000;
    private static final int SUNRISE_END = 1000;
    private static final int DAY_END = 12000;
    private static final int SUNSET_START = 12000;
    private static final int SUNSET_END = 13800;
    private static final int NIGHT_START = 14000;

    public CrepuscularActivity(CrepuscularConfig config) {
        this.config = config;
    }

    public CrepuscularActivity() {
        this(new CrepuscularConfig());
    }

    /**
     * Checks if a creature should be active based on current conditions.
     * Returns true during twilight hours (dawn and dusk).
     */
    public boolean isActive(Mob entity) {
        Level level = entity.level();
        long dayTime = level.getDayTime() % 24000;

        return isDawn(dayTime) || isDusk(dayTime);
    }

    /**
     * Checks if it is currently dawn (sunrise time).
     */
    public boolean isDawn(long dayTime) {
        return dayTime >= SUNRISE_START || dayTime <= SUNRISE_END;
    }

    /**
     * Checks if it is currently dusk (sunset time).
     */
    public boolean isDusk(long dayTime) {
        return dayTime >= SUNSET_START && dayTime <= SUNSET_END;
    }

    /**
     * Checks if it is currently nighttime (beyond twilight).
     */
    public boolean isNighttime(long dayTime) {
        return dayTime > SUNSET_END && dayTime < SUNRISE_START;
    }

    /**
     * Checks if it is currently daytime (midday).
     */
    public boolean isDaytime(long dayTime) {
        return dayTime > SUNRISE_END && dayTime < SUNSET_START;
    }

    /**
     * Calculates the current light level from the sky at entity position.
     */
    public int getSkyLightLevel(Mob entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (level.canSeeSky(pos)) {
            return level.getRawBrightness(pos, 0);
        }

        return level.getMaxLocalRawBrightness(pos);
    }

    /**
     * Determines if light conditions are suitable for emergence.
     */
    public boolean isEmergenceLightLevel(Mob entity) {
        int lightLevel = getSkyLightLevel(entity);
        return lightLevel <= config.getEmergenceLightLevel();
    }

    /**
     * Determines if light conditions require return to roost.
     */
    public boolean isReturnLightLevel(Mob entity) {
        int lightLevel = getSkyLightLevel(entity);
        return lightLevel > config.getReturnLightLevel();
    }

    /**
     * Calculates activity intensity based on time of day.
     * Returns 0.0 (inactive) to 1.0 (fully active).
     */
    public double getActivityIntensity(Mob entity) {
        Level level = entity.level();
        long dayTime = level.getDayTime() % 24000;
        int lightLevel = getSkyLightLevel(entity);

        // Inactive during bright midday
        if (isDaytime(dayTime) && lightLevel > 8) {
            return 0.0;
        }

        // Inactive during deep night
        if (isNighttime(dayTime) && lightLevel < 3) {
            return 0.2;
        }

        // Peak activity during twilight
        if (isDawn(dayTime) || isDusk(dayTime)) {
            double targetLight = config.getEmergenceLightLevel();
            double lightDiff = Math.abs(lightLevel - targetLight);
            double intensity = 1.0 - (lightDiff / 10.0);
            return Math.max(0.0, Math.min(1.0, intensity));
        }

        return 0.5;
    }

    /**
     * Gets the time until next dawn in ticks.
     */
    public long getTimeUntilDawn(Level level) {
        long dayTime = level.getDayTime() % 24000;

        if (dayTime <= SUNRISE_END) {
            return SUNRISE_END - dayTime;
        }

        return (24000 - dayTime) + SUNRISE_END;
    }

    /**
     * Gets the time until next dusk in ticks.
     */
    public long getTimeUntilDusk(Level level) {
        long dayTime = level.getDayTime() % 24000;

        if (dayTime >= SUNSET_START) {
            return 0L;
        }

        return SUNSET_START - dayTime;
    }

    /**
     * Calculates the moon phase brightness modifier.
     * Brighter moon = later emergence.
     */
    public double getMoonPhaseModifier(Level level) {
        if (!config.isAffectedByMoonPhase()) {
            return 1.0;
        }

        int moonPhase = level.getMoonPhase();
        // Moon phases: 0 (full) to 7 (waxing crescent)
        // Full moon (0) is brightest, new moon (4) is darkest
        double brightness = 1.0 - (Math.abs(moonPhase - 4) / 4.0);
        return 0.8 + (brightness * 0.4);
    }

    /**
     * Calculates temperature-based activity modifier.
     * Warmer biomes lead to earlier emergence.
     */
    public double getTemperatureModifier(Mob entity) {
        if (!config.isUseTemperatureModifier()) {
            return 1.0;
        }

        float biomeTemp = entity.level().getBiome(entity.blockPosition()).value().getBaseTemperature();
        // Normalize temperature (-0.5 to 1.0) to modifier
        double normalizedTemp = (biomeTemp + 0.5) / 1.5;
        return 0.8 + (normalizedTemp * 0.4);
    }

    /**
     * Determines if weather delays emergence.
     */
    public boolean isWeatherDelayingEmergence(Level level) {
        if (!config.isAffectedByWeather()) {
            return false;
        }

        return level.isRaining() || level.isThundering();
    }

    /**
     * Calculates the combined modifier for emergence timing.
     */
    public double calculateEmergenceModifier(Mob entity) {
        double moonMod = getMoonPhaseModifier(entity.level());
        double tempMod = getTemperatureModifier(entity);
        double baseMod = config.getTemperatureModifier();

        return baseMod * moonMod * tempMod;
    }

    /**
     * Checks if all conditions are met for activity.
     */
    public boolean canBeActive(Mob entity) {
        if (isWeatherDelayingEmergence(entity.level())) {
            return false;
        }

        return isActive(entity);
    }

    /**
     * Gets the configuration for this activity calculator.
     */
    public CrepuscularConfig getConfig() {
        return config;
    }

    /**
     * Updates the configuration.
     */
    public void setConfig(CrepuscularConfig config) {
        if (config != null) {
            this.config.setEmergenceLightLevel(config.getEmergenceLightLevel());
            this.config.setReturnLightLevel(config.getReturnLightLevel());
            this.config.setGroupEmergenceChance(config.getGroupEmergenceChance());
            this.config.setTemperatureModifier(config.getTemperatureModifier());
            this.config.setUseTemperatureModifier(config.isUseTemperatureModifier());
        }
    }
}
