package me.javavirtualenv.ecology.spawning;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage accessor for ChunkSpawnData attached to chunks.
 * Uses in-memory map to store spawn positions during server runtime.
 * Similar to Distant Horizons - data is calculated once and kept in memory.
 * <p>
 * Memory Management:
 * - Data is automatically cleaned up when chunks unload
 * - Entries expire after 10 minutes if spawning hasn't completed
 * - Empty spawn data is immediately removed
 */
public final class ChunkSpawnDataStorage {

    private static final Map<ChunkPos, ChunkSpawnData> SPAWN_DATA_MAP = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, Long> CREATION_TIME_MAP = new ConcurrentHashMap<>();

    // Expire spawn data after 10 minutes if not used (in milliseconds)
    private static final long EXPIRATION_TIME_MS = 10 * 60 * 1000;

    private ChunkSpawnDataStorage() {
    }

    /**
     * Get spawn data from a chunk.
     * Returns null if no spawn data exists.
     */
    public static ChunkSpawnData get(LevelChunk chunk) {
        return SPAWN_DATA_MAP.get(chunk.getPos());
    }

    /**
     * Set spawn data for a chunk.
     * This should be called during chunk generation.
     */
    public static void set(LevelChunk chunk, ChunkSpawnData spawnData) {
        ChunkPos pos = chunk.getPos();
        SPAWN_DATA_MAP.put(pos, spawnData);
        CREATION_TIME_MAP.put(pos, System.currentTimeMillis());
    }

    /**
     * Remove spawn data from a chunk.
     * Called after all spawns have been processed or when chunk unloads.
     */
    public static void remove(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        SPAWN_DATA_MAP.remove(pos);
        CREATION_TIME_MAP.remove(pos);
    }

    /**
     * Remove spawn data by chunk position.
     * Used for cleanup when chunk is unloaded.
     */
    public static void remove(ChunkPos pos) {
        SPAWN_DATA_MAP.remove(pos);
        CREATION_TIME_MAP.remove(pos);
    }

    /**
     * Clear all spawn data.
     * Called on server shutdown.
     */
    public static void clear() {
        SPAWN_DATA_MAP.clear();
        CREATION_TIME_MAP.clear();
    }

    /**
     * Check if spawn data exists for a chunk.
     */
    public static boolean has(LevelChunk chunk) {
        return SPAWN_DATA_MAP.containsKey(chunk.getPos());
    }

    /**
     * Set spawn data for a proto chunk (during loading).
     * Uses ChunkPos directly since proto chunk may not be upgraded yet.
     */
    public static void setForProtoChunk(ChunkPos chunkPos, ChunkSpawnData spawnData) {
        SPAWN_DATA_MAP.put(chunkPos, spawnData);
        CREATION_TIME_MAP.put(chunkPos, System.currentTimeMillis());
    }

    /**
     * Get spawn data by chunk position.
     * Returns null if no spawn data exists.
     */
    public static ChunkSpawnData get(ChunkPos chunkPos) {
        return SPAWN_DATA_MAP.get(chunkPos);
    }

    /**
     * Clean up expired spawn data entries.
     * Removes entries that:
     * - Have no remaining spawn positions
     * - Have existed for longer than EXPIRATION_TIME_MS without being used
     * <p>
     * Should be called periodically (e.g., every server tick).
     */
    public static void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<ChunkPos, ChunkSpawnData>> iterator = SPAWN_DATA_MAP.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, ChunkSpawnData> entry = iterator.next();
            ChunkPos pos = entry.getKey();
            ChunkSpawnData data = entry.getValue();

            // Remove if no spawns remain
            if (!data.hasAnySpawns()) {
                iterator.remove();
                CREATION_TIME_MAP.remove(pos);
                continue;
            }

            // Remove if expired (created more than EXPIRATION_TIME_MS ago)
            Long creationTime = CREATION_TIME_MAP.get(pos);
            if (creationTime != null && (currentTime - creationTime) > EXPIRATION_TIME_MS) {
                iterator.remove();
                CREATION_TIME_MAP.remove(pos);
            }
        }
    }

    /**
     * Clean up spawn data for a specific chunk when it unloads.
     * This prevents memory leaks from chunks that are unloaded before spawning completes.
     */
    public static void onChunkUnload(ChunkPos pos) {
        remove(pos);
    }

    /**
     * Get the number of chunks with pending spawn data.
     * Useful for monitoring memory usage.
     */
    public static int getStoredChunkCount() {
        return SPAWN_DATA_MAP.size();
    }

    /**
     * Get total number of pending spawns across all chunks.
     * Useful for monitoring and debugging.
     */
    public static int getTotalPendingSpawns() {
        return SPAWN_DATA_MAP.values().stream()
            .mapToInt(ChunkSpawnData::getTotalSpawnCount)
            .sum();
    }
}
