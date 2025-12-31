package me.javavirtualenv.ecology.handles;

import java.util.List;
import java.util.Map;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Handles temporal behaviors - daily cycle, weather, seasons.
 * Activity multipliers apply to movement speed, detection range, etc.
 */
public final class TemporalHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:temporal-cache";
    private static final String NBT_CURRENT_ACTIVITY = "currentActivity";

    @Override
    public String id() {
        return "temporal";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        TemporalCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && (cache.hasDailyCycle() || cache.hasWeatherResponses);
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        TemporalCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        Level level = mob.level();
        EntityState state = component.state();
        CompoundTag tag = component.getHandleTag(id());

        // Get current time-based activity level
        double dailyActivity = getActivityLevel(cache, level.getDayTime() % 24000);
        tag.putDouble("dailyActivity", dailyActivity);

        // Get weather-based activity modifier
        double weatherModifier = getWeatherModifier(cache, level);
        tag.putDouble("weatherModifier", weatherModifier);

        // Combined activity affects behavior
        double combinedActivity = dailyActivity * weatherModifier;
        tag.putDouble("combinedActivity", combinedActivity);

        // Low activity means resting/reduced awareness
        if (combinedActivity < cache.lowActivityThreshold()) {
            tag.putBoolean("isLowActivity", true);
            // Could apply reduced awareness, seek shelter, etc.
        } else {
            tag.putBoolean("isLowActivity", false);
        }
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Could register RestGoal, SeekShelterGoal, etc.
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Loaded automatically
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private TemporalCache buildCache(EcologyProfile profile) {
        List<Map<String, Object>> schedule = profile.getMapList("temporal.daily_cycle.schedule");

        Map<String, Object> weather = profile.getMap("temporal.weather");
        boolean hasWeather = weather != null && !weather.isEmpty();

        boolean lowActivitySeekShelter = profile.getBool("temporal.daily_cycle.low_activity_behavior.seek_shelter", false);
        double lowActivityThreshold = profile.getDouble("temporal.daily_cycle.low_activity_threshold", 0.5);

        return new TemporalCache(schedule, hasWeather, lowActivitySeekShelter, lowActivityThreshold);
    }

    private double getActivityLevel(TemporalCache cache, long timeOfDay) {
        if (!cache.hasDailyCycle()) {
            return 1.0;
        }

        for (Map<String, Object> entry : cache.schedule()) {
            Object timeObj = entry.get("time");
            Object activityObj = entry.get("activity");

            if (timeObj instanceof List<?> timeRange && activityObj instanceof Number activity) {
                // Parse [start, end] time range
                if (timeRange.size() == 2) {
                    Object startObj = timeRange.get(0);
                    Object endObj = timeRange.get(1);
                    if (startObj instanceof Number start && endObj instanceof Number end) {
                        long startTime = start.longValue();
                        long endTime = end.longValue();

                        if (isTimeInRange(timeOfDay, startTime, endTime)) {
                            return activity.doubleValue();
                        }
                    }
                }
            }
        }

        return 1.0; // Default full activity
    }

    private boolean isTimeInRange(long time, long start, long end) {
        if (start <= end) {
            return time >= start && time < end;
        } else {
            // Handle wrap-around (e.g., 22000-2400 spans midnight)
            return time >= start || time < end;
        }
    }

    private double getWeatherModifier(TemporalCache cache, Level level) {
        if (!cache.hasWeatherResponses()) {
            return 1.0;
        }

        if (level.isThundering()) {
            return 0.3; // Reduced activity in thunder
        } else if (level.isRaining()) {
            return 0.6; // Moderately reduced in rain
        } else {
            return 1.0; // Full activity in clear weather
        }
    }

    /**
     * Get the current activity multiplier for this entity.
     * Use in other handlers to scale behavior intensity.
     */
    public static double getActivityMultiplier(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("temporal");
        if (!tag.contains("combinedActivity")) {
            return 1.0;
        }
        return tag.getDouble("combinedActivity");
    }

    /**
     * Check if entity is in low activity mode (resting).
     */
    public static boolean isLowActivity(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("temporal");
        return tag.getBoolean("isLowActivity");
    }

    private record TemporalCache(
            List<Map<String, Object>> schedule,
            boolean hasWeatherResponses,
            boolean lowActivitySeekShelter,
            double lowActivityThreshold
    ) {
        public boolean hasDailyCycle() { return schedule != null && !schedule.isEmpty(); }
        public boolean hasWeatherResponses() { return hasWeatherResponses; }
        public boolean lowActivitySeekShelter() { return lowActivitySeekShelter; }
        public double lowActivityThreshold() { return lowActivityThreshold; }
    }
}
