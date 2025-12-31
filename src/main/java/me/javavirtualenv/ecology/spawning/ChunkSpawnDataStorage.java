package me.javavirtualenv.ecology.spawning;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage accessor for ChunkSpawnData attached to chunks.
 * Uses in-memory map to store spawn positions during server runtime.
 * Similar to Distant Horizons - data is calculated once and kept in memory.
 */
public final class ChunkSpawnDataStorage {

    private static final Map<ChunkPos, ChunkSpawnData> SPAWN_DATA_MAP = new ConcurrentHashMap<>();

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
        SPAWN_DATA_MAP.put(chunk.getPos(), spawnData);
    }

    /**
     * Remove spawn data from a chunk.
     * Called after all spawns have been processed.
     */
    public static void remove(LevelChunk chunk) {
        SPAWN_DATA_MAP.remove(chunk.getPos());
    }

    /**
     * Clear all spawn data.
     * Called on server shutdown.
     */
    public static void clear() {
        SPAWN_DATA_MAP.clear();
    }

    /**
     * Check if spawn data exists for a chunk.
     */
    public static boolean has(LevelChunk chunk) {
        return SPAWN_DATA_MAP.containsKey(chunk.getPos());
    }
}
