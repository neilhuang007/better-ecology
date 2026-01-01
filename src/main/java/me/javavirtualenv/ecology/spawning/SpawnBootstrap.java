package me.javavirtualenv.ecology.spawning;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Bootstrap for the optimized spawning system.
 * <p>
 * Integrates:
 * - Pre-computed spawn positions (calculated once during chunk generation)
 * - Region-based detection (flood-fill algorithm)
 * - Regional density tracking (cross-chunk awareness)
 * - Cave detection (cached scanline algorithm)
 */
public final class SpawnBootstrap {
    // Regional tracker for cross-chunk density awareness
    private static final RegionalDensityTracker REGIONAL_TRACKER = new RegionalDensityTracker();

    // Legacy tracker for backward compatibility
    private static final SpawnDensityTracker CHUNK_TRACKER = new SpawnDensityTracker();

    private static boolean initialized = false;

    // Cleanup every 20 seconds (400 ticks at 20 tps)
    private static final int CLEANUP_INTERVAL_TICKS = 400;
    private static int tickCounter = 0;

    private SpawnBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Wire regional tracker to position calculator
        SpawnPositionCalculator.setRegionalTracker(REGIONAL_TRACKER);

        ServerChunkEvents.CHUNK_GENERATE.register(SpawnBootstrap::onChunkGenerate);
        ServerChunkEvents.CHUNK_LOAD.register(SpawnBootstrap::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(SpawnBootstrap::onChunkUnload);
        ServerTickEvents.END_SERVER_TICK.register(SpawnBootstrap::onEndServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(SpawnBootstrap::onServerStopped);
    }

    /**
     * Get the regional density tracker for cross-chunk awareness.
     */
    public static RegionalDensityTracker getRegionalTracker() {
        return REGIONAL_TRACKER;
    }

    /**
     * Get the chunk-level density tracker (legacy).
     */
    public static SpawnDensityTracker getChunkTracker() {
        return CHUNK_TRACKER;
    }

    /**
     * Called when a chunk is generated.
     * Calculates and stores valid spawn positions using region-based detection.
     */
    private static void onChunkGenerate(ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        if (!(chunk instanceof LevelChunk levelChunk)) {
            return;
        }

        SpawnPositionCalculator.calculateForChunk(level, levelChunk);
    }

    /**
     * Called when a chunk is loaded.
     * Triggers spawning of pre-computed mobs if players are nearby.
     */
    private static void onChunkLoad(ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        if (!(chunk instanceof LevelChunk levelChunk)) {
            return;
        }

        ChunkLoadSpawner.onChunkLoad(level, levelChunk);
    }

    /**
     * Called when a chunk is unloaded.
     * Cleans up spawn data to prevent memory leaks.
     */
    private static void onChunkUnload(ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        ChunkSpawnDataStorage.onChunkUnload(chunk.getPos());
    }

    /**
     * Called at end of server tick.
     * Updates regional entity density counts and performs periodic cleanup.
     */
    private static void onEndServerTick(MinecraftServer server) {
        server.getAllLevels().forEach(REGIONAL_TRACKER::updateCounts);

        // Periodically clean up expired spawn data
        tickCounter++;
        if (tickCounter >= CLEANUP_INTERVAL_TICKS) {
            tickCounter = 0;
            ChunkSpawnDataStorage.cleanupExpired();
        }
    }

    /**
     * Called when server stops.
     * Clean up tracking data.
     */
    private static void onServerStopped(MinecraftServer server) {
        REGIONAL_TRACKER.clear();
        CHUNK_TRACKER.clear();
        ChunkSpawnDataStorage.clear();
    }
}
