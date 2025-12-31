package me.javavirtualenv.ecology.handles.reproduction;

import me.javavirtualenv.behavior.reproduction.NestBuildingGoal;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Handle for managing nest building behavior in animals.
 * <p>
 * This handle tracks nest locations, quality, materials, and building progress.
 * It provides the data layer for the NestBuildingGoal AI.
 */
public final class NestBuildingHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:nest-building-cache";
    private static final String NEST_DATA_KEY = "nestData";

    @Override
    public String id() {
        return "nest_building";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return profile.getBool("nest_building.enabled", false);
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Animal animal)) {
            return;
        }

        NestBuildingConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
        int priority = profile.getInt("ai_priority_framework.reproduction.nest_building", 7);
        MobAccessor accessor = (MobAccessor) mob;
        accessor.betterEcology$getGoalSelector().addGoal(priority,
            new NestBuildingGoal(animal, config));
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (!tag.contains(id())) {
            return;
        }

        CompoundTag handleTag = tag.getCompound(id());
        if (handleTag.contains(NEST_DATA_KEY)) {
            NestData nestData = NestData.fromNbt(handleTag.getCompound(NEST_DATA_KEY));
            component.setData(id() + "." + NEST_DATA_KEY, nestData);
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        NestData nestData = getNestData(component);
        if (nestData == null || !nestData.hasNest()) {
            return;
        }

        CompoundTag handleTag = new CompoundTag();
        handleTag.put(NEST_DATA_KEY, nestData.toNbt());
        tag.put(id(), handleTag);
    }

    /**
     * Get or create nest data for an entity.
     */
    public static NestData getOrCreateNestData(EcologyComponent component, NestBuildingConfig config) {
        String key = "nest_building." + NEST_DATA_KEY;
        NestData nestData = component.getData(key);

        if (nestData == null) {
            nestData = new NestData(config);
            component.setData(key, nestData);
        }

        return nestData;
    }

    /**
     * Get nest data for an entity without creating.
     */
    public static NestData getNestData(EcologyComponent component) {
        return component.getData("nest_building." + NEST_DATA_KEY);
    }

    private NestBuildingConfig buildConfig(EcologyProfile profile) {
        String nestType = profile.getString("nest_building.nest_type", "ground");
        double buildingSpeed = profile.getDouble("nest_building.building_speed", 1.0);
        int nestSize = profile.getInt("nest_building.nest_size", 3);
        int searchRadius = profile.getInt("nest_building.search_radius", 32);
        int maxMaterials = profile.getInt("nest_building.max_materials", 64);
        boolean preferShelter = profile.getBool("nest_building.prefer_shelter", true);
        boolean territorialDefense = profile.getBool("nest_building.territorial_defense", true);

        Map<String, Integer> requiredMaterials = new HashMap<>();
        var materialsList = profile.getList("nest_building.required_materials");
        for (var entry : materialsList.entrySet()) {
            if (entry.getValue() instanceof Number) {
                requiredMaterials.put(entry.getKey(), ((Number) entry.getValue()).intValue());
            }
        }

        return new NestBuildingConfig(
            nestType,
            buildingSpeed,
            nestSize,
            searchRadius,
            maxMaterials,
            preferShelter,
            territorialDefense,
            requiredMaterials
        );
    }
}

/**
 * Configuration for nest building behavior.
 */
record NestBuildingConfig(
    String nestType,
    double buildingSpeed,
    int nestSize,
    int searchRadius,
    int maxMaterials,
    boolean preferShelter,
    boolean territorialDefense,
    Map<String, Integer> requiredMaterials
) {
    public boolean isTreeNest() {
        return "tree".equals(nestType) || "canopy".equals(nestType);
    }

    public boolean isGroundNest() {
        return "ground".equals(nestType) || "scrape".equals(nestType);
    }

    public boolean isBurrowNest() {
        return "burrow".equals(nestType) || "underground".equals(nestType);
    }

    public boolean isSandNest() {
        return "sand".equals(nestType) || "beach".equals(nestType);
    }
}

/**
 * Tracks nest state for an entity.
 */
public class NestData {
    private BlockPos nestLocation;
    private double nestQuality;
    private int buildingProgress;
    private int collectedMaterials;
    private final Map<String, Integer> materials;
    private final NestBuildingConfig config;
    private int disturbanceCount;
    private long lastDisturbanceTime;
    private boolean isAbandoned;

    public NestData(NestBuildingConfig config) {
        this.config = config;
        this.nestQuality = 0.0;
        this.buildingProgress = 0;
        this.collectedMaterials = 0;
        this.materials = new HashMap<>();
        this.disturbanceCount = 0;
        this.isAbandoned = false;
    }

    public boolean hasNest() {
        return nestLocation != null && !isAbandoned;
    }

    public BlockPos getNestLocation() {
        return nestLocation;
    }

    public void setNestLocation(BlockPos location) {
        this.nestLocation = location;
    }

    public double getNestQuality() {
        return nestQuality;
    }

    public void setNestQuality(double quality) {
        this.nestQuality = Math.max(0.0, Math.min(1.0, quality));
    }

    public int getBuildingProgress() {
        return buildingProgress;
    }

    public void addProgress(int progress) {
        this.buildingProgress = Math.min(100, this.buildingProgress + progress);
    }

    public int getCollectedMaterials() {
        return collectedMaterials;
    }

    public void addMaterial(String materialType, int amount) {
        int current = materials.getOrDefault(materialType, 0);
        materials.put(materialType, current + amount);
        collectedMaterials += amount;

        // Update quality based on materials
        recalculateQuality();
    }

    public int getMaterialCount(String materialType) {
        return materials.getOrDefault(materialType, 0);
    }

    public Map<String, Integer> getMaterials() {
        return new HashMap<>(materials);
    }

    public boolean isComplete() {
        return buildingProgress >= 100;
    }

    public void recordDisturbance(Level level) {
        disturbanceCount++;
        lastDisturbanceTime = level.getGameTime();

        // Abandon nest if disturbed too often
        if (disturbanceCount > 5) {
            isAbandoned = true;
        }
    }

    public boolean isAbandoned() {
        return isAbandoned;
    }

    public long getTimeSinceLastDisturbance(Level level) {
        return level.getGameTime() - lastDisturbanceTime;
    }

    public boolean canUseNest(Level level) {
        return !isAbandoned && hasNest() &&
               getTimeSinceLastDisturbance(level) > 600; // 30 seconds cooldown
    }

    private void recalculateQuality() {
        if (config.requiredMaterials().isEmpty()) {
            nestQuality = 0.5;
            return;
        }

        int totalRequired = 0;
        int totalCollected = 0;

        for (var entry : config.requiredMaterials().entrySet()) {
            String material = entry.getKey();
            int required = entry.getValue();
            int collected = materials.getOrDefault(material, 0);

            totalRequired += required;
            totalCollected += Math.min(collected, required);
        }

        if (totalRequired > 0) {
            nestQuality = (double) totalCollected / totalRequired;
        }

        // Bonus for having all required materials
        if (totalCollected >= totalRequired) {
            nestQuality = Math.min(1.0, nestQuality + 0.2);
        }
    }

    public void reset() {
        nestLocation = null;
        nestQuality = 0.0;
        buildingProgress = 0;
        collectedMaterials = 0;
        materials.clear();
        disturbanceCount = 0;
        isAbandoned = false;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        if (nestLocation != null) {
            tag.putInt("NestX", nestLocation.getX());
            tag.putInt("NestY", nestLocation.getY());
            tag.putInt("NestZ", nestLocation.getZ());
        }
        tag.putDouble("NestQuality", nestQuality);
        tag.putInt("BuildingProgress", buildingProgress);
        tag.putInt("CollectedMaterials", collectedMaterials);
        tag.putInt("DisturbanceCount", disturbanceCount);
        tag.putLong("LastDisturbanceTime", lastDisturbanceTime);
        tag.putBoolean("IsAbandoned", isAbandoned);

        CompoundTag materialsTag = new CompoundTag();
        for (var entry : materials.entrySet()) {
            materialsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Materials", materialsTag);

        return tag;
    }

    public static NestData fromNbt(CompoundTag tag, NestBuildingConfig config) {
        NestData data = new NestData(config);

        if (tag.contains("NestX")) {
            int x = tag.getInt("NestX");
            int y = tag.getInt("NestY");
            int z = tag.getInt("NestZ");
            data.nestLocation = new BlockPos(x, y, z);
        }

        data.nestQuality = tag.getDouble("NestQuality");
        data.buildingProgress = tag.getInt("BuildingProgress");
        data.collectedMaterials = tag.getInt("CollectedMaterials");
        data.disturbanceCount = tag.getInt("DisturbanceCount");
        data.lastDisturbanceTime = tag.getLong("LastDisturbanceTime");
        data.isAbandoned = tag.getBoolean("IsAbandoned");

        if (tag.contains("Materials")) {
            CompoundTag materialsTag = tag.getCompound("Materials");
            for (String key : materialsTag.getAllKeys()) {
                data.materials.put(key, materialsTag.getInt(key));
            }
        }

        return data;
    }

    public static NestData fromNbt(CompoundTag tag) {
        return fromNbt(tag, new NestBuildingConfig("ground", 1.0, 3, 32, 64, true, true, Map.of()));
    }
}
