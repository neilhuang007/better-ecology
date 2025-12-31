package me.javavirtualenv.ecology.spawning;

import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.EcologyProfileRegistry;
import me.javavirtualenv.ecology.handles.SpawnHandle;
import me.javavirtualenv.ecology.handles.SpawnHandle.SpawnConditions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.List;

/**
 * Optimized spawn position calculator using region-based detection.
 * <p>
 * Key optimizations:
 * 1. Cave detection uses cached scanline algorithm (O(n) vs O(n³))
 * 2. Region-based flood-fill identifies spawn areas efficiently
 * 3. Regional density tracking prevents overpopulation
 * 4. Memoized validity grids avoid redundant condition checks
 * <p>
 * Time Complexity: O(chunk_size) for scanning + O(regions) for spawn selection
 * Space Complexity: O(chunk_size) for validity grid + O(regions) for results
 */
public final class SpawnPositionCalculator {

    private static final int CHUNK_SIZE = 16;
    private static final int MAX_SAMPLES_PER_REGION = 4;

    // Shared regional tracker instance
    private static RegionalDensityTracker regionalTracker;

    private SpawnPositionCalculator() {
    }

    /**
     * Set the regional density tracker for cross-chunk awareness.
     */
    public static void setRegionalTracker(RegionalDensityTracker tracker) {
        regionalTracker = tracker;
    }

    /**
     * Calculate spawn positions for a chunk using optimized region detection.
     * Uses region-based approach instead of random sampling for efficiency.
     *
     * @param level  The server level
     * @param chunk  The chunk being generated
     * @param random Random source for sampling
     * @return ChunkSpawnData containing all valid spawn positions
     */
    public static ChunkSpawnData calculateSpawnPositions(
        ServerLevel level,
        ChunkAccess chunk,
        RandomSource random
    ) {
        ChunkSpawnData data = new ChunkSpawnData();

        for (EcologyProfile profile : EcologyProfileRegistry.getAllProfiles()) {
            if (!profile.getBool("population.spawning.enabled", false)) {
                continue;
            }

            SpawnHandle.SpawnConfig config = SpawnHandle.getConfig(profile);
            if (config == null) {
                continue;
            }

            EntityType<?> entityType = getEntityTypeForProfile(profile);
            if (entityType == null) {
                continue;
            }

            // Check regional density limit before processing
            int maxPerRegion = config.maxPerRegion() != null ? config.maxPerRegion() : 10;
            if (regionalTracker != null) {
                BlockPos chunkCenter = new BlockPos(
                    chunk.getPos().getMinBlockX() + 8,
                    chunk.getMinBuildHeight() + 32,
                    chunk.getPos().getMinBlockZ() + 8
                );
                if (!regionalTracker.canSpawnAt(level, chunkCenter, entityType, maxPerRegion)) {
                    continue; // Region full
                }
            }

            // Use region-based detection for efficiency
            calculateForEntity(level, chunk, entityType, config.conditions(),
                config, random, data, maxPerRegion);
        }

        return data;
    }

    /**
     * Calculate spawn positions for a single entity type using region detection.
     */
    private static void calculateForEntity(
        ServerLevel level,
        ChunkAccess chunk,
        EntityType<?> entityType,
        SpawnConditions conditions,
        SpawnHandle.SpawnConfig config,
        RandomSource random,
        ChunkSpawnData data,
        int maxPerRegion
    ) {
        // Fast path for cave-only entities (use cached cave detection)
        if (conditions.inCaves()) {
            calculateCaveSpawns(level, chunk, entityType, config, random, data, maxPerRegion);
            return;
        }

        // Surface entities: use region detection
        List<SpawnRegionDetector.SpawnRegion> regions =
            SpawnRegionDetector.detectRegions(level, chunk, entityType, conditions, MAX_SAMPLES_PER_REGION);

        for (SpawnRegionDetector.SpawnRegion region : regions) {
            // Calculate how many spawns this region can support
            double density = config.density() != null ? config.density() : 0.1;
            int capacity = Math.min(region.calculateCapacity(density), maxPerRegion);

            for (int i = 0; i < capacity; i++) {
                BlockPos pos = region.getPositionForSpawn();
                if (pos != null && isValidSpawnPosition(level, pos, entityType, conditions)) {
                    data.addSpawnPosition(entityType, pos);
                }
            }
        }
    }

    /**
     * Optimized cave spawn calculation using cached cave detection.
     */
    private static void calculateCaveSpawns(
        ServerLevel level,
        ChunkAccess chunk,
        EntityType<?> entityType,
        SpawnHandle.SpawnConfig config,
        RandomSource random,
        ChunkSpawnData data,
        int maxPerRegion
    ) {
        CaveDetector.CaveScanResult caves = CaveDetector.scanChunkForCaves(chunk);

        if (caves.isEmpty()) {
            return;
        }

        int spawnCount = Math.min(caves.getCount() / 10, maxPerRegion); // 10% of cave positions
        spawnCount = Math.max(1, spawnCount);

        for (int i = 0; i < spawnCount; i++) {
            BlockPos pos = caves.getRandomCavePosition(
                chunk.getPos().getMinBlockX(),
                chunk.getPos().getMinBlockZ(),
                random
            );

            if (pos != null) {
                data.addSpawnPosition(entityType, pos);
            }
        }
    }

    /**
     * Calculate spawn positions for a chunk and store them.
     * Convenience method called from chunk generation event.
     */
    public static void calculateForChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
        RandomSource random = RandomSource.create();
        random.setSeed(chunk.getPos().toLong());

        ChunkSpawnData spawnData = calculateSpawnPositions(level, chunk, random);

        if (spawnData.hasAnySpawns()) {
            ChunkSpawnDataStorage.set(chunk, spawnData);
        }
    }

    /**
     * Get the EntityType for a given ecology profile.
     */
    private static EntityType<?> getEntityTypeForProfile(EcologyProfile profile) {
        ResourceLocation profileId = profile.id();
        return BuiltInRegistries.ENTITY_TYPE.get(profileId);
    }

    /**
     * Check if a position is valid for spawning.
     * Validates biome and spawn conditions (memoized in region detector).
     */
    private static boolean isValidSpawnPosition(
        ServerLevel level,
        BlockPos pos,
        EntityType<?> entityType,
        SpawnConditions conditions
    ) {
        return isBiomeValid(level, pos, conditions);
    }

    /**
     * Check if the biome at position matches spawn requirements.
     */
    private static boolean isBiomeValid(ServerLevel level, BlockPos pos, SpawnConditions conditions) {
        List<String> biomes = conditions.biomes();
        if (biomes == null || biomes.isEmpty()) {
            return true;
        }

        Biome currentBiome = level.getBiome(pos).value();
        ResourceLocation biomeId = level.registryAccess().registryOrThrow(
            net.minecraft.core.registries.Registries.BIOME
        ).getKey(currentBiome);

        if (biomeId == null) {
            return false;
        }

        for (String biomeFilter : biomes) {
            if (matchesBiome(biomeId, biomeFilter)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a biome ID matches a filter string.
     * Supports exact match and namespace:* wildcard patterns.
     */
    private static boolean matchesBiome(ResourceLocation biomeId, String filter) {
        if (filter.equals(biomeId.toString())) {
            return true;
        }

        if (filter.endsWith("/*")) {
            String namespace = filter.substring(0, filter.length() - 2);
            return namespace.equals(biomeId.getNamespace());
        }

        if (filter.startsWith("#")) {
            String tagPath = filter.substring(1);
            return biomeId.toString().contains(tagPath);
        }

        return false;
    }
}
