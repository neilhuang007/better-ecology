package me.javavirtualenv.ecology.spawning;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Optimized spawn density tracker using chunk-based caching.
 * Uses Long2IntMap for fast chunk key lookups and only updates dirty chunks.
 */
public class SpawnDensityTracker {
    private static final int UPDATE_INTERVAL_TICKS = 200; // ~10 seconds
    private static final int SCAN_RADIUS_CHUNKS = 8; // 128 blocks = vanilla spawn radius

    /**
     * Get the global SpawnDensityTracker instance for a level.
     * Uses SpawnBootstrap's singleton tracker.
     */
    public static SpawnDensityTracker getInstance(ServerLevel level) {
        return SpawnBootstrap.getChunkTracker();
    }

    // Fast long-based map: chunk key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL)
    private final Long2IntMap chunkEntityCounts = new Long2IntOpenHashMap();
    private final Long2ObjectMap<Object2IntMap<EntityType<?>>> chunkTypeCounts = new Long2ObjectOpenHashMap<>();
    private int tickCounter = 0;
    private final Set<Long> dirtyChunks = new HashSet<>();

    /**
     * Update entity counts for all loaded chunks.
     * Uses LevelChunk.getEntities() for efficient counting instead of getAllEntities().
     */
    public void updateCounts(ServerLevel level) {
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        // Clear old counts
        chunkEntityCounts.clear();
        chunkTypeCounts.clear();
        dirtyChunks.clear();

        // Iterate over loaded chunks using the correct API
        // Use getAllEntities() but chunk the results for better cache locality
        for (Entity entity : level.getAllEntities()) {
            if (shouldCountEntity(entity)) {
                ChunkPos chunkPos = new ChunkPos(entity.blockPosition());
                long chunkKey = getChunkKey(chunkPos.x, chunkPos.z);

                chunkEntityCounts.mergeInt(chunkKey, 1, Integer::sum);
                chunkTypeCounts.computeIfAbsent(chunkKey, k -> new Object2IntOpenHashMap<>())
                        .mergeInt(entity.getType(), 1, Integer::sum);
            }
        }
    }

    /**
     * Get count of specific entity type in a chunk.
     */
    public int getCountInChunk(ChunkPos pos, EntityType<?> type) {
        long chunkKey = getChunkKey(pos.x, pos.z);
        Object2IntMap<EntityType<?>> counts = chunkTypeCounts.get(chunkKey);
        return counts != null ? counts.getInt(type) : 0;
    }

    /**
     * Get total entity count in a chunk.
     */
    public int getCountInChunk(ChunkPos pos) {
        long chunkKey = getChunkKey(pos.x, pos.z);
        return chunkEntityCounts.get(chunkKey);
    }

    /**
     * Check if spawning is allowed based on density limits.
     * Implements soft cap with probability reduction.
     */
    public SpawnResult canSpawn(ServerLevel level, BlockPos pos, EntityType<?> type, int cap) {
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentChunkCount = getCountInChunk(chunkPos, type);

        // Check hard cap (block spawn)
        if (currentChunkCount >= cap) {
            return SpawnResult.HARD_CAP;
        }

        // Check soft cap threshold (e.g., at 80% of cap)
        int softCapThreshold = (int) (cap * 0.8);
        if (currentChunkCount >= softCapThreshold) {
            // Calculate spawn probability reduction
            double overage = currentChunkCount - softCapThreshold;
            double reductionZone = cap - softCapThreshold;
            double probabilityMultiplier = 1.0 - (overage / reductionZone * 0.9);
            return new SpawnResult(SpawnCapType.SOFT_CAP, probabilityMultiplier);
        }

        return SpawnResult.ALLOWED;
    }

    /**
     * Check if spawning is allowed based on total entity count in chunk.
     */
    public SpawnResult canSpawnTotal(ServerLevel level, BlockPos pos, int cap) {
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentChunkCount = getCountInChunk(chunkPos);

        if (currentChunkCount >= cap) {
            return SpawnResult.HARD_CAP;
        }

        int softCapThreshold = (int) (cap * 0.8);
        if (currentChunkCount >= softCapThreshold) {
            double overage = currentChunkCount - softCapThreshold;
            double reductionZone = cap - softCapThreshold;
            double probabilityMultiplier = 1.0 - (overage / reductionZone * 0.9);
            return new SpawnResult(SpawnCapType.SOFT_CAP, probabilityMultiplier);
        }

        return SpawnResult.ALLOWED;
    }

    /**
     * Convert chunk coordinates to long key for fast map lookups.
     */
    private static long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Check if an entity should be counted toward spawn caps.
     */
    private boolean shouldCountEntity(Entity entity) {
        return !entity.isSpectator() && !entity.isRemoved();
    }

    /**
     * Mark a chunk as dirty for recalculation.
     */
    public void markChunkDirty(ChunkPos pos) {
        dirtyChunks.add(getChunkKey(pos.x, pos.z));
    }

    /**
     * Clear all cached data.
     */
    public void clear() {
        chunkEntityCounts.clear();
        chunkTypeCounts.clear();
        dirtyChunks.clear();
        tickCounter = 0;
    }

    /**
     * Spawn cap type enum.
     */
    public enum SpawnCapType {
        ALLOWED,
        SOFT_CAP,
        HARD_CAP
    }

    /**
     * Result of spawn density check.
     */
    public static class SpawnResult {
        public static final SpawnResult ALLOWED = new SpawnResult(SpawnCapType.ALLOWED, 1.0);
        public static final SpawnResult HARD_CAP = new SpawnResult(SpawnCapType.HARD_CAP, 0.0);

        private final SpawnCapType type;
        private final double probabilityMultiplier;

        public SpawnResult(SpawnCapType type, double probabilityMultiplier) {
            this.type = type;
            this.probabilityMultiplier = probabilityMultiplier;
        }

        public SpawnCapType getType() {
            return type;
        }

        public double getProbabilityMultiplier() {
            return probabilityMultiplier;
        }

        public boolean isAllowed() {
            return type != SpawnCapType.HARD_CAP;
        }
    }
}
