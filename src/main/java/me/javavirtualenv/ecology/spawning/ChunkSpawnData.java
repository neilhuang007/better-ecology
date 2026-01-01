package me.javavirtualenv.ecology.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data structure for storing pre-computed spawn positions in a chunk.
 * Similar to Distant Horizons chunk loading - positions are calculated once
 * during chunk generation and stored for later use.
 */
public final class ChunkSpawnData {
    private static final String SPAWN_POSITIONS_KEY = "SpawnPositions";
    private static final String ENTITY_TYPE_KEY = "EntityType";
    private static final String POSITION_KEY = "Position";

    /**
     * Map of entity type to list of spawn positions.
     * Uses HashMap for O(1) lookups during spawn operations.
     */
    private final Map<EntityType<?>, Set<BlockPos>> spawnPositions = new ConcurrentHashMap<>();

    /**
     * Add a spawn position for an entity type.
     * Uses Set to prevent duplicate positions.
     */
    public void addSpawnPosition(EntityType<?> type, BlockPos pos) {
        spawnPositions.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    /**
     * Get all spawn positions for an entity type.
     * Returns an empty list if no positions exist.
     */
    public List<BlockPos> getSpawnPositions(EntityType<?> type) {
        Set<BlockPos> positions = spawnPositions.get(type);
        return positions != null ? new ArrayList<>(positions) : List.of();
    }

    /**
     * Remove a spawn position after spawning the entity.
     * This prevents re-spawning at the same location.
     */
    public void removeSpawnPosition(EntityType<?> type, BlockPos pos) {
        Set<BlockPos> positions = spawnPositions.get(type);
        if (positions != null) {
            positions.remove(pos);
            if (positions.isEmpty()) {
                spawnPositions.remove(type);
            }
        }
    }

    /**
     * Check if there are remaining spawn positions for an entity type.
     */
    public boolean hasRemainingSpawns(EntityType<?> type) {
        Set<BlockPos> positions = spawnPositions.get(type);
        return positions != null && !positions.isEmpty();
    }

    /**
     * Get all entity types that have pending spawn positions.
     */
    public Collection<EntityType<?>> getEntityTypes() {
        return spawnPositions.keySet();
    }

    /**
     * Check if any spawn positions remain in this chunk.
     */
    public boolean hasAnySpawns() {
        return !spawnPositions.isEmpty();
    }

    /**
     * Get total count of pending spawns across all entity types.
     */
    public int getTotalSpawnCount() {
        return spawnPositions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Serialize spawn data to NBT for storage in chunk file.
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag positionsList = new ListTag();

        for (Map.Entry<EntityType<?>, Set<BlockPos>> entry : spawnPositions.entrySet()) {
            EntityType<?> entityType = entry.getKey();
            Set<BlockPos> positions = entry.getValue();

            for (BlockPos pos : positions) {
                CompoundTag positionTag = new CompoundTag();
                positionTag.putString(ENTITY_TYPE_KEY, EntityType.getKey(entityType).toString());
                positionTag.putInt(POSITION_KEY, pos.getX());
                positionTag.putInt(POSITION_KEY + 1, pos.getY());
                positionTag.putInt(POSITION_KEY + 2, pos.getZ());
                positionsList.add(positionTag);
            }
        }

        tag.put(SPAWN_POSITIONS_KEY, positionsList);
        return tag;
    }

    /**
     * Deserialize spawn data from NBT.
     * Returns empty data if tag is invalid.
     */
    public static ChunkSpawnData fromNbt(CompoundTag tag) {
        ChunkSpawnData data = new ChunkSpawnData();

        if (!tag.contains(SPAWN_POSITIONS_KEY, Tag.TAG_LIST)) {
            return data;
        }

        ListTag positionsList = tag.getList(SPAWN_POSITIONS_KEY, Tag.TAG_COMPOUND);

        for (int i = 0; i < positionsList.size(); i++) {
            CompoundTag positionTag = positionsList.getCompound(i);

            String entityTypeId = positionTag.getString(ENTITY_TYPE_KEY);
            int x = positionTag.getInt(POSITION_KEY);
            int y = positionTag.getInt(POSITION_KEY + 1);
            int z = positionTag.getInt(POSITION_KEY + 2);

            EntityType.byString(entityTypeId).ifPresent(type -> {
                BlockPos pos = new BlockPos(x, y, z);
                data.addSpawnPosition(type, pos);
            });
        }

        return data;
    }
}
