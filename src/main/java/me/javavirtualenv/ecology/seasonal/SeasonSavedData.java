package me.javavirtualenv.ecology.seasonal;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Saved data for storing season overrides across world reloads.
 */
public class SeasonSavedData extends SavedData {

    private static final String DATA_NAME = "better_ecology_season_data";

    private final Map<String, String> seasonOverrides = new HashMap<>();

    private SeasonSavedData() {
    }

    /**
     * Loads season data from NBT.
     */
    public static SeasonSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        SeasonSavedData data = new SeasonSavedData();
        data.load(tag);
        return data;
    }

    /**
     * Creates factory for SavedData.
     */
    public static SavedData.Factory<SeasonSavedData> factory() {
        return new SavedData.Factory<>(
                SeasonSavedData::new,
                SeasonSavedData::load,
                null // No data fixer needed
        );
    }

    /**
     * Gets or creates season saved data for a server level.
     *
     * @param level The server level
     * @return The season saved data
     */
    public static SeasonSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    /**
     * Loads data from NBT tag.
     */
    private void load(CompoundTag nbt) {
        seasonOverrides.clear();

        if (nbt.contains("overrides")) {
            CompoundTag overridesTag = nbt.getCompound("overrides");
            for (String key : overridesTag.getAllKeys()) {
                String seasonName = overridesTag.getString(key);
                seasonOverrides.put(key, seasonName);
            }
        }
    }

    /**
     * Saves data to NBT tag.
     */
    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        CompoundTag overridesTag = new CompoundTag();

        for (Map.Entry<String, String> entry : seasonOverrides.entrySet()) {
            overridesTag.put(entry.getKey(), StringTag.valueOf(entry.getValue()));
        }

        nbt.put("overrides", overridesTag);
        return nbt;
    }

    /**
     * Sets a season override for a dimension.
     *
     * @param dimensionId The dimension ID
     * @param season      The season, or null to clear
     */
    public void setSeasonOverride(String dimensionId, SeasonalContext.Season season) {
        if (season != null) {
            seasonOverrides.put(dimensionId, season.name());
        } else {
            seasonOverrides.remove(dimensionId);
        }
        setDirty();
    }

    /**
     * Gets the season override for a dimension.
     *
     * @param dimensionId The dimension ID
     * @return The season, or null if no override
     */
    public SeasonalContext.Season getSeasonOverride(String dimensionId) {
        String seasonName = seasonOverrides.get(dimensionId);
        if (seasonName != null) {
            try {
                return SeasonalContext.Season.valueOf(seasonName);
            } catch (IllegalArgumentException e) {
                // Invalid season name, remove it
                seasonOverrides.remove(dimensionId);
                setDirty();
            }
        }
        return null;
    }

    /**
     * Gets all dimension season overrides.
     *
     * @return Map of dimension ID to season
     */
    public Map<String, SeasonalContext.Season> getAllOverrides() {
        Map<String, SeasonalContext.Season> result = new HashMap<>();
        for (Map.Entry<String, String> entry : seasonOverrides.entrySet()) {
            try {
                SeasonalContext.Season season = SeasonalContext.Season.valueOf(entry.getValue());
                result.put(entry.getKey(), season);
            } catch (IllegalArgumentException e) {
                // Skip invalid entries
            }
        }
        return result;
    }

    /**
     * Clears all season overrides.
     */
    public void clearAll() {
        seasonOverrides.clear();
        setDirty();
    }
}
