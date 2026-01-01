package me.javavirtualenv.ecology.spawning;

import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.EcologyProfileRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Spawns pre-computed mobs when chunks load near players.
 * Similar to Distant Horizons approach - chunks are populated when
 * they come within render distance of a player.
 */
public final class ChunkLoadSpawner {

    private static final int SPAWN_DISTANCE_CHUNKS = 8;
    private static final int SPAWN_DISTANCE_BLOCKS = SPAWN_DISTANCE_CHUNKS * 16;

    private ChunkLoadSpawner() {
    }

    /**
     * Called when a chunk is loaded.
     * Spawns pre-computed mobs if chunk is within spawn distance of any player.
     *
     * @param level The server level
     * @param chunk The chunk being loaded
     */
    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (!hasNearbyPlayer(level, chunk)) {
            return;
        }

        ChunkSpawnData spawnData = getSpawnData(chunk);
        if (spawnData == null || !spawnData.hasAnySpawns()) {
            return;
        }

        spawnMobsFromData(level, chunk, spawnData);
    }

    /**
     * Check if any player is within spawn distance of this chunk.
     */
    private static boolean hasNearbyPlayer(ServerLevel level, LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (ServerPlayer player : level.players()) {
            int playerChunkX = player.chunkPosition().x;
            int playerChunkZ = player.chunkPosition().z;

            int distChunks = Math.max(
                Math.abs(chunkX - playerChunkX),
                Math.abs(chunkZ - playerChunkZ)
            );

            if (distChunks <= SPAWN_DISTANCE_CHUNKS) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get spawn data from chunk's persistent data.
     */
    private static ChunkSpawnData getSpawnData(LevelChunk chunk) {
        return ChunkSpawnDataStorage.get(chunk);
    }

    /**
     * Spawn mobs from pre-computed spawn data.
     * Validates each position before spawning and removes used positions.
     * Cleans up spawn data storage when all spawns are complete.
     */
    private static void spawnMobsFromData(
        ServerLevel level,
        LevelChunk chunk,
        ChunkSpawnData spawnData
    ) {
        for (EntityType<?> type : spawnData.getEntityTypes()) {
            for (BlockPos pos : spawnData.getSpawnPositions(type)) {
                if (canSpawnNow(level, pos, type)) {
                    spawnMob(level, type, pos, spawnData);
                }
            }
        }

        // Clean up storage if all spawns are complete
        if (!spawnData.hasAnySpawns()) {
            ChunkSpawnDataStorage.remove(chunk);
        }
    }

    /**
     * Check if a mob can be spawned at the given position right now.
     * Uses vanilla-equivalent spawn checks including SpawnPlacements, light, time, and mob caps.
     */
    private static boolean canSpawnNow(ServerLevel level, BlockPos pos, EntityType<?> type) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        if (!checkVanillaSpawnPlacement(level, pos, type)) {
            return false;
        }

        if (!checkLightLevel(level, pos, type)) {
            return false;
        }

        if (!checkTimeOfDay(level, type)) {
            return false;
        }

        if (!checkMobCap(level, pos, type)) {
            return false;
        }

        return true;
    }

    /**
     * Check vanilla spawn placement rules using SpawnPlacements API.
     * This validates position validity (ground, water, etc.) and spawn rules.
     */
    private static boolean checkVanillaSpawnPlacement(ServerLevel level, BlockPos pos, EntityType<?> type) {
        if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)) {
            return false;
        }

        RandomSource random = level.getRandom();
        return SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, random);
    }

    /**
     * Check light level conditions from profile configuration.
     * Uses profile's population.spawning.conditions.light range.
     * Uses combined light (block + sky) to match vanilla spawn behavior.
     */
    private static boolean checkLightLevel(ServerLevel level, BlockPos pos, EntityType<?> type) {
        EcologyProfile profile = getProfileForType(type);
        if (profile == null) {
            return true;
        }

        Object lightCondition = profile.getFast("population", "spawning", "conditions");
        if (!(lightCondition instanceof java.util.Map<?, ?> conditionsMap)) {
            return true;
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> conditions = (java.util.Map<String, Object>) conditionsMap;
        Object lightObj = conditions.get("light");

        if (!(lightObj instanceof java.util.List<?> lightList) || lightList.size() != 2) {
            return true;
        }

        int minLight = ((Number) lightList.get(0)).intValue();
        int maxLight = ((Number) lightList.get(1)).intValue();
        int actualLight = level.getRawBrightness(pos, 0);

        return actualLight >= minLight && actualLight <= maxLight;
    }

    /**
     * Check time of day based on activity pattern from profile.
     * Diurnal mobs spawn during day, nocturnal during night, crepuscular at dawn/dusk.
     */
    private static boolean checkTimeOfDay(ServerLevel level, EntityType<?> type) {
        EcologyProfile profile = getProfileForType(type);
        if (profile == null) {
            return true;
        }

        String activityPattern = profile.getString("identity.classification.activity_pattern", "ANY");
        long timeOfDay = level.getDayTime() % 24000;

        return switch (activityPattern) {
            case "DIURNAL" -> timeOfDay >= 0 && timeOfDay < 12000;
            case "NOCTURNAL" -> timeOfDay >= 12000 && timeOfDay < 24000;
            case "CREPUSCULAR" -> (timeOfDay >= 22000 || timeOfDay < 2000) || (timeOfDay >= 11000 && timeOfDay < 13000);
            default -> true;
        };
    }

    /**
     * Check mob cap using SpawnDensityTracker.
     * Uses per-chunk cap from profile's population.carrying_capacity.caps.per_chunk.
     */
    private static boolean checkMobCap(ServerLevel level, BlockPos pos, EntityType<?> type) {
        EcologyProfile profile = getProfileForType(type);
        if (profile == null) {
            return true;
        }

        int perChunkCap = profile.getInt("population.carrying_capacity.caps.per_chunk", Integer.MAX_VALUE);
        if (perChunkCap == Integer.MAX_VALUE) {
            return true;
        }

        SpawnDensityTracker tracker = SpawnDensityTracker.getInstance(level);
        if (tracker == null) {
            return true;
        }

        SpawnDensityTracker.SpawnResult result = tracker.canSpawn(level, pos, type, perChunkCap);
        return result.isAllowed();
    }

    /**
     * Get EcologyProfile for an EntityType.
     */
    private static EcologyProfile getProfileForType(EntityType<?> type) {
        net.minecraft.resources.ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key == null) {
            return null;
        }
        return EcologyProfileRegistry.get(key);
    }

    /**
     * Spawn a single mob at the given position.
     * Removes the position from spawn data to prevent re-spawning.
     */
    private static void spawnMob(
        ServerLevel level,
        EntityType<?> type,
        BlockPos pos,
        ChunkSpawnData spawnData
    ) {
        Mob mob = (Mob) type.create(level);
        if (mob == null) {
            spawnData.removeSpawnPosition(type, pos);
            return;
        }

        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), null, null);

        if (level.addFreshEntity(mob)) {
            spawnData.removeSpawnPosition(type, pos);
        }
    }

    /**
     * Get the configured spawn distance in chunks.
     */
    public static int getSpawnDistanceChunks() {
        return SPAWN_DISTANCE_CHUNKS;
    }

    /**
     * Get the configured spawn distance in blocks.
     */
    public static int getSpawnDistanceBlocks() {
        return SPAWN_DISTANCE_BLOCKS;
    }
}
