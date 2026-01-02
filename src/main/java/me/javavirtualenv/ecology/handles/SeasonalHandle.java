package me.javavirtualenv.ecology.handles;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.seasonal.SeasonalContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Handle for seasonal and time-of-day behavior variations.
 * Provides cached seasonal context to behaviors and other handles.
 */
public final class SeasonalHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:seasonal-cache";
    private static final String NBT_LAST_UPDATE_DAY = "lastUpdateDay";
    private static final String NBT_CURRENT_SEASON = "currentSeason";

    @Override
    public String id() {
        return "seasonal";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        SeasonalConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
        return config != null && config.enabled();
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        SeasonalConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
        if (config == null || !config.enabled()) {
            return;
        }

        // Only update season once per in-game day for performance
        if (mob.level() instanceof ServerLevel serverLevel) {
            long currentDay = serverLevel.getDayTime() / 24000L;
            CompoundTag handleTag = component.getHandleTag(id());
            long lastUpdateDay = handleTag.getLong(NBT_LAST_UPDATE_DAY);

            if (currentDay != lastUpdateDay) {
                SeasonalContext.Season currentSeason = SeasonalContext.getSeason(serverLevel);
                handleTag.putLong(NBT_LAST_UPDATE_DAY, currentDay);
                handleTag.putString(NBT_CURRENT_SEASON, currentSeason.name());
            }
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // NBT data is automatically loaded via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    /**
     * Get the current season for an entity.
     *
     * @param mob The entity
     * @return Current season, or null if unable to determine
     */
    @Nullable
    public static SeasonalContext.Season getSeason(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        // Try to get cached season from component
        if (mob instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            EcologyComponent component = access.betterEcology$getEcologyComponent();
            if (component != null) {
                CompoundTag handleTag = component.getHandleTag("seasonal");
                String seasonName = handleTag.getString(NBT_CURRENT_SEASON);
                if (!seasonName.isEmpty()) {
                    try {
                        return SeasonalContext.Season.valueOf(seasonName);
                    } catch (IllegalArgumentException e) {
                        // Invalid season name, fall through to recalculation
                    }
                }
            }
        }

        // Calculate fresh if cache not available
        return SeasonalContext.getSeason(serverLevel);
    }

    /**
     * Get the current time period for an entity.
     *
     * @param mob The entity
     * @return Current time period, or null if unable to determine
     */
    @Nullable
    public static SeasonalContext.TimePeriod getTimePeriod(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return SeasonalContext.getTimePeriod(serverLevel);
    }

    /**
     * Get activity multiplier based on season and time of day.
     * Combines seasonal and time-of-day multipliers.
     *
     * @param mob             The entity
     * @param activityPattern The entity's activity pattern
     * @return Combined activity multiplier (0.0 = inactive, 1.0 = normal, >1.0 =
     *         extra active)
     */
    public static double getActivityMultiplier(Mob mob, SeasonalContext.ActivityPattern activityPattern) {
        SeasonalContext.Season season = getSeason(mob);
        SeasonalContext.TimePeriod timePeriod = getTimePeriod(mob);

        if (season == null || timePeriod == null) {
            return 1.0; // Default if unable to determine
        }

        double seasonalMultiplier = SeasonalContext.getSeasonalActivityMultiplier(season);
        double timeMultiplier = SeasonalContext.getActivityMultiplier(activityPattern, timePeriod);

        return seasonalMultiplier * timeMultiplier;
    }

    /**
     * Check if seasonal breeding is enabled for an entity.
     *
     * @param mob The entity
     * @return true if seasonal breeding is enabled
     */
    public static boolean isSeasonalBreedingEnabled(Mob mob) {
        if (!(mob instanceof me.javavirtualenv.ecology.api.EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null || component.profile() == null) {
            return false;
        }

        SeasonalConfig config = component.profile().cached(CACHE_KEY, () -> null);
        return config != null && config.seasonalBreeding();
    }

    /**
     * Check if current season is a breeding season for an entity.
     *
     * @param mob The entity
     * @return true if current season is a breeding season
     */
    public static boolean isBreedingSeason(Mob mob) {
        SeasonalContext.Season currentSeason = getSeason(mob);
        if (currentSeason == null) {
            return true; // Default to allowing breeding if season unknown
        }

        if (!(mob instanceof me.javavirtualenv.ecology.api.EcologyAccess access)) {
            return true;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null || component.profile() == null) {
            return true;
        }

        SeasonalConfig config = component.profile().cached(CACHE_KEY, () -> null);
        if (config == null || !config.seasonalBreeding()) {
            return true; // Breeding not seasonal, always allowed
        }

        return config.breedingSeasons().contains(currentSeason);
    }

    /**
     * Get breeding multiplier for current season.
     *
     * @param mob The entity
     * @return Breeding multiplier (1.0 = normal, higher = increased breeding
     *         chance)
     */
    public static double getBreedingMultiplier(Mob mob) {
        if (!isSeasonalBreedingEnabled(mob)) {
            return 1.0;
        }

        SeasonalContext.Season season = getSeason(mob);
        if (season == null) {
            return 1.0;
        }

        return SeasonalContext.getSeasonalBreedingMultiplier(season);
    }

    private SeasonalConfig buildConfig(EcologyProfile profile) {
        boolean enabled = profile.getBool("seasonal.enabled", false);
        boolean seasonalBreeding = profile.getBool("seasonal.breeding_seasons_enabled", false);
        boolean winterDormancy = profile.getBool("seasonal.winter_dormancy", false);

        @SuppressWarnings("unchecked")
        List<String> breedingSeasonsList = (List<String>) profile.getList("seasonal.breeding_seasons");
        List<SeasonalContext.Season> breedingSeasons = parseBreedingSeasons(breedingSeasonsList);

        String activityPatternStr = profile.getString("seasonal.activity_pattern", "diurnal");
        SeasonalContext.ActivityPattern activityPattern = parseActivityPattern(activityPatternStr);

        return new SeasonalConfig(enabled, seasonalBreeding, winterDormancy, breedingSeasons, activityPattern);
    }

    private List<SeasonalContext.Season> parseBreedingSeasons(List<String> seasonList) {
        if (seasonList == null || seasonList.isEmpty()) {
            return List.of(SeasonalContext.Season.SPRING, SeasonalContext.Season.AUTUMN);
        }

        return seasonList.stream()
                .map(String::toUpperCase)
                .map(seasonStr -> {
                    try {
                        return SeasonalContext.Season.valueOf(seasonStr);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(season -> season != null)
                .toList();
    }

    private SeasonalContext.ActivityPattern parseActivityPattern(String patternStr) {
        if (patternStr == null || patternStr.isEmpty()) {
            return SeasonalContext.ActivityPattern.DIURNAL;
        }

        try {
            return SeasonalContext.ActivityPattern.valueOf(patternStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SeasonalContext.ActivityPattern.DIURNAL;
        }
    }

    private record SeasonalConfig(
            boolean enabled,
            boolean seasonalBreeding,
            boolean winterDormancy,
            List<SeasonalContext.Season> breedingSeasons,
            SeasonalContext.ActivityPattern activityPattern) {
    }
}
