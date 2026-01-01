package me.javavirtualenv.ecology.conservation;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages safe zones for endangered species.
 * Reduces predation risk in refuge areas and improves breeding success.
 * Refuges can be created dynamically or defined in configuration.
 */
public final class RefugeSystem {
    private static final Long2ObjectMap<Refuge> REFUGES = new Long2ObjectOpenHashMap<>();
    private static final Set<ChunkPos> REFUGE_CHUNKS = new HashSet<>();

    /**
     * Check if a position is within a refuge area.
     *
     * @param level The server level
     * @param pos The position to check
     * @return true if position is in a refuge
     */
    public static boolean isInRefuge(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        return REFUGE_CHUNKS.contains(chunkPos);
    }

    /**
     * Check if a position is within a refuge for a specific species.
     *
     * @param level The server level
     * @param pos The position to check
     * @param entityType The entity type
     * @return true if position is in a refuge for this species
     */
    public static boolean isInRefuge(ServerLevel level, BlockPos pos, EntityType<?> entityType) {
        if (!isInRefuge(level, pos)) {
            return false;
        }

        Refuge refuge = getRefugeAt(level, pos);
        return refuge != null && refuge.appliesTo(entityType);
    }

    /**
     * Get the predation reduction multiplier for a position.
     * Refuges reduce predation risk by 50-90% depending on refuge level.
     *
     * @param level The server level
     * @param pos The position to check
     * @return The predation multiplier (0.1-1.0, where lower is safer)
     */
    public static double getPredationReduction(ServerLevel level, BlockPos pos) {
        Refuge refuge = getRefugeAt(level, pos);
        if (refuge == null) {
            return 1.0;
        }

        // Higher level refuges provide more protection
        return switch (refuge.refugeLevel()) {
            case HIGH -> 0.1;
            case MEDIUM -> 0.3;
            case LOW -> 0.5;
        };
    }

    /**
     * Get the breeding bonus for a position.
     * Refuges increase breeding success by 20-50%.
     *
     * @param level The server level
     * @param pos The position to check
     * @return The breeding multiplier (1.0-1.5)
     */
    public static double getBreedingBonus(ServerLevel level, BlockPos pos) {
        Refuge refuge = getRefugeAt(level, pos);
        if (refuge == null) {
            return 1.0;
        }

        return switch (refuge.refugeLevel()) {
            case HIGH -> 1.5;
            case MEDIUM -> 1.3;
            case LOW -> 1.2;
        };
    }

    /**
     * Create a new refuge area.
     *
     * @param level The server level
     * @param center The center position of the refuge
     * @param radius The radius of the refuge in blocks
     * @param refugeLevel The protection level of the refuge
     * @param entityType The entity type this refuge is for (null for all species)
     * @return The created refuge
     */
    public static Refuge createRefuge(ServerLevel level, BlockPos center, int radius, RefugeLevel refugeLevel, EntityType<?> entityType) {
        BoundingBox bounds = new BoundingBox(
            center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius
        );

        Refuge refuge = new Refuge(bounds, refugeLevel, entityType);
        long key = getRefugeKey(level, center);

        REFUGES.put(key, refuge);

        // Mark all chunks in refuge
        ChunkPos minChunk = new ChunkPos(bounds.minX(), bounds.minZ());
        ChunkPos maxChunk = new ChunkPos(bounds.maxX(), bounds.maxZ());

        for (int x = minChunk.x; x <= maxChunk.x; x++) {
            for (int z = minChunk.z; z <= maxChunk.z; z++) {
                REFUGE_CHUNKS.add(new ChunkPos(x, z));
            }
        }

        return refuge;
    }

    /**
     * Remove a refuge at a position.
     *
     * @param level The server level
     * @param pos The position within the refuge
     */
    public static void removeRefuge(ServerLevel level, BlockPos pos) {
        long key = getRefugeKey(level, pos);
        Refuge refuge = REFUGES.remove(key);

        if (refuge != null) {
            // Unmark chunks
            BoundingBox bounds = refuge.bounds();
            ChunkPos minChunk = new ChunkPos(bounds.minX(), bounds.minZ());
            ChunkPos maxChunk = new ChunkPos(bounds.maxX(), bounds.maxZ());

            for (int x = minChunk.x; x <= maxChunk.x; x++) {
                for (int z = minChunk.z; z <= maxChunk.z; z++) {
                    ChunkPos chunkPos = new ChunkPos(x, z);
                    // Only remove if no other refuge covers this chunk
                    if (!isChunkCoveredByOtherRefuge(chunkPos)) {
                        REFUGE_CHUNKS.remove(chunkPos);
                    }
                }
            }
        }
    }

    /**
     * Get the refuge at a specific position.
     *
     * @param level The server level
     * @param pos The position to check
     * @return The refuge, or null if not in a refuge
     */
    public static Refuge getRefugeAt(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        for (Refuge refuge : new java.util.ArrayList<>(REFUGES.values())) {
            if (refuge.bounds().isInside(pos)) {
                return refuge;
            }
        }

        return null;
    }

    /**
     * Clear all refuges (useful for testing or world reset).
     */
    public static void clearAll() {
        REFUGES.clear();
        REFUGE_CHUNKS.clear();
    }

    /**
     * Get all refuges.
     *
     * @return All active refuges
     */
    public static Long2ObjectMap<Refuge> getAllRefuges() {
        return REFUGES;
    }

    /**
     * Check if a chunk is covered by any refuge other than the one being removed.
     */
    private static boolean isChunkCoveredByOtherRefuge(ChunkPos chunkPos) {
        for (Refuge refuge : new java.util.ArrayList<>(REFUGES.values())) {
            BoundingBox bounds = refuge.bounds();
            ChunkPos minChunk = new ChunkPos(bounds.minX(), bounds.minZ());
            ChunkPos maxChunk = new ChunkPos(bounds.maxX(), bounds.maxZ());

            if (chunkPos.x >= minChunk.x && chunkPos.x <= maxChunk.x &&
                chunkPos.z >= minChunk.z && chunkPos.z <= maxChunk.z) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a unique key for a refuge based on dimension and position.
     */
    private static long getRefugeKey(ServerLevel level, BlockPos pos) {
        long dimensionKey = level.dimension().location().toString().hashCode();
        long posKey = ((long) pos.getX() << 32) | (pos.getZ() & 0xFFFFFFFFL);
        return dimensionKey ^ posKey;
    }

    /**
     * Represents a refuge area with protection level.
     */
    public record Refuge(BoundingBox bounds, RefugeLevel refugeLevel, EntityType<?> entityType) {
        /**
         * Check if this refuge applies to a specific entity type.
         */
        public boolean appliesTo(EntityType<?> type) {
            return entityType == null || entityType.equals(type);
        }
    }

    /**
     * Refuge protection levels.
     */
    public enum RefugeLevel {
        /**
         * High protection - 90% predation reduction, 50% breeding bonus.
         */
        HIGH,

        /**
         * Medium protection - 70% predation reduction, 30% breeding bonus.
         */
        MEDIUM,

        /**
         * Low protection - 50% predation reduction, 20% breeding bonus.
         */
        LOW
    }
}
