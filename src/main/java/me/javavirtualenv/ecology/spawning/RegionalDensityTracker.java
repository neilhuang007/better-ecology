package me.javavirtualenv.ecology.spawning;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Regional density tracker that accounts for cross-chunk mob movement.
 * <p>
 * Instead of tracking per-chunk density (which fails for flying/swimming mobs),
 * this system uses a hierarchical approach:
 * <p>
 * 1. Micro-level: Per-chunk entity counts (for quick lookup)
 * 2. Macro-level: Per-region aggregate counts (for spawning decisions)
 * 3. Mobility-aware: Considers entity type movement capabilities
 * <p>
 * Regions are 8x8 chunks (128x128 blocks) - large enough to account for
 * mob movement but small enough for localized spawning control.
 * <p>
 * For each entity type, tracks:
 * - Terrestrial mobs (affected by terrain barriers)
 * - Flying mobs (can access entire region)
 * - Aquatic mobs (water-connected areas)
 */
public final class RegionalDensityTracker {

    // Region size in chunks (8x8 chunks = 128x128 blocks)
    private static final int REGION_SIZE_CHUNKS = 8;
    private static final int REGION_SIZE_BLOCKS = REGION_SIZE_CHUNKS * 16;

    // Update interval to balance accuracy and performance
    private static final int UPDATE_INTERVAL_TICKS = 100; // 5 seconds

    // Entity type categories for mobility-aware tracking
    private enum MobilityCategory {
        TERRESTRIAL,  // Land animals, affected by terrain
        FLYING,       // Birds, bats, ghasts - access entire region
        AQUATIC,      // Fish, dolphins - water-connected
        AMPHIBIOUS    // Frogs, turtles - both land and water
    }

    // Per-chunk entity counts: chunk_key -> (entity_type_id -> count)
    private final Long2ObjectMap<Int2IntMap> chunkEntityCounts;

    // Per-region aggregate counts: region_key -> (entity_type_id -> count)
    private final Long2IntMap regionCounts;

    // Cache of entity mobility categories
    private final Int2IntMap entityMobilityCache;

    // Last update timestamp
    private long lastUpdateTick;

    public RegionalDensityTracker() {
        this.chunkEntityCounts = new Long2ObjectOpenHashMap<>();
        this.regionCounts = new Long2IntOpenHashMap();
        this.entityMobilityCache = new Int2IntOpenHashMap();
        this.lastUpdateTick = 0;
    }

    /**
     * Get the mob count for a given entity type in a region.
     * Accounts for mobility - flying mobs affect larger area.
     */
    public int getCountInRegion(ServerLevel level, BlockPos center, EntityType<?> type) {
        long regionKey = getRegionKey(center);

        MobilityCategory mobility = getMobilityCategory(type);
        int multiplier = getMobilityMultiplier(mobility);

        // Base count from primary region
        int count = getCountInRegionKey(regionKey, type);

        // For mobile entities, include adjacent regions
        if (multiplier > 1) {
            count += countInAdjacentRegions(regionKey, type, level, center);
        }

        // Adjust for mobility (flying mobs have more "effective" space)
        return (count + multiplier - 1) / multiplier; // Ceiling division
    }

    /**
     * Get the mob count specifically within a chunk.
     */
    public int getCountInChunk(ServerLevel level, BlockPos pos, EntityType<?> type) {
        long chunkKey = getChunkKey(pos);
        Int2IntMap typeCounts = chunkEntityCounts.get(chunkKey);
        if (typeCounts == null) {
            return 0;
        }
        return typeCounts.get(getEntityTypeId(type));
    }

    /**
     * Check if spawning is allowed at a position based on regional density.
     */
    public boolean canSpawnAt(ServerLevel level, BlockPos pos, EntityType<?> type, int maxPerRegion) {
        int currentCount = getCountInRegion(level, pos, type);
        return currentCount < maxPerRegion;
    }

    /**
     * Update all entity counts.
     * Should be called periodically (every ~100 ticks).
     */
    public void updateCounts(ServerLevel level) {
        long currentTick = level.getGameTime();
        if (currentTick - lastUpdateTick < UPDATE_INTERVAL_TICKS) {
            return;
        }
        lastUpdateTick = currentTick;

        // Clear old data
        chunkEntityCounts.clear();
        regionCounts.clear();

        // Count entities by iterating through loaded entities
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }

            BlockPos pos = mob.blockPosition();
            long chunkKey = getChunkKey(pos);
            long regionKey = getRegionKey(pos);
            int typeId = getEntityTypeId(mob.getType());

            // Update chunk counts
            chunkEntityCounts.computeIfAbsent(chunkKey, k -> new Int2IntOpenHashMap())
                .mergeInt(typeId, 1, Integer::sum);

            // Update region counts
            regionCounts.merge(encodeRegionKeyWithType(regionKey, typeId), 1, Integer::sum);
        }
    }

    /**
     * Increment count when an entity spawns.
     */
    public void onEntitySpawned(Mob entity) {
        BlockPos pos = entity.blockPosition();
        long chunkKey = getChunkKey(pos);
        long regionKey = getRegionKey(pos);
        int typeId = getEntityTypeId(entity.getType());

        chunkEntityCounts.computeIfAbsent(chunkKey, k -> new Int2IntOpenHashMap())
            .mergeInt(typeId, 1, Integer::sum);
        regionCounts.merge(encodeRegionKeyWithType(regionKey, typeId), 1, Integer::sum);
    }

    /**
     * Decrement count when an entity despawns.
     */
    public void onEntityDespawned(Mob entity) {
        BlockPos pos = entity.blockPosition();
        long chunkKey = getChunkKey(pos);
        long regionKey = getRegionKey(pos);
        int typeId = getEntityTypeId(entity.getType());

        Int2IntMap typeCounts = chunkEntityCounts.get(chunkKey);
        if (typeCounts != null) {
            typeCounts.mergeInt(typeId, -1, Integer::sum);
            if (typeCounts.get(typeId) <= 0) {
                typeCounts.remove(typeId);
            }
        }

        long encodedKey = encodeRegionKeyWithType(regionKey, typeId);
        int newCount = regionCounts.get(encodedKey) - 1;
        if (newCount <= 0) {
            regionCounts.remove(encodedKey);
        } else {
            regionCounts.put(encodedKey, newCount);
        }
    }

    /**
     * Track entity movement for flying mobs (they affect regional density).
     */
    public void onEntityMoved(Mob entity, Vec3 from, Vec3 to) {
        MobilityCategory mobility = getMobilityCategory(entity.getType());
        if (mobility != MobilityCategory.FLYING && mobility != MobilityCategory.AQUATIC) {
            return; // Only track mobile entities
        }

        long fromChunk = getChunkKey(from.x, from.z);
        long toChunk = getChunkKey(to.x, to.z);

        if (fromChunk != toChunk) {
            // Entity moved between chunks - update density
            int typeId = getEntityTypeId(entity.getType());

            Int2IntMap fromCounts = chunkEntityCounts.get(fromChunk);
            if (fromCounts != null) {
                fromCounts.mergeInt(typeId, -1, Integer::sum);
            }

            chunkEntityCounts.computeIfAbsent(toChunk, k -> new Int2IntOpenHashMap())
                .mergeInt(typeId, 1, Integer::sum);
        }
    }

    /**
     * Get the region key for a position.
     * Region = 8x8 chunks
     */
    public static long getRegionKey(BlockPos pos) {
        int regionX = pos.getX() / REGION_SIZE_BLOCKS;
        int regionZ = pos.getZ() / REGION_SIZE_BLOCKS;
        return ((long) regionX & 0xFFFFFFFFL) << 32 | (long) regionZ & 0xFFFFFFFFL;
    }

    /**
     * Get the chunk key for a position.
     */
    public static long getChunkKey(BlockPos pos) {
        return getChunkKey(pos.getX(), pos.getZ());
    }

    public static long getChunkKey(double x, double z) {
        long chunkX = (long) java.lang.Math.floor(x / 16.0);
        long chunkZ = (long) java.lang.Math.floor(z / 16.0);
        return (chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Get entity type ID for fast comparison.
     */
    private int getEntityTypeId(EntityType<?> type) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getId(type);
    }

    /**
     * Get mobility category for an entity type.
     * Cached for performance.
     */
    private MobilityCategory getMobilityCategory(EntityType<?> type) {
        int typeId = getEntityTypeId(type);
        int cached = entityMobilityCache.get(typeId);
        if (cached != 0) {
            return MobilityCategory.values()[cached - 1];
        }

        MobilityCategory category = determineMobilityCategory(type);
        entityMobilityCache.put(typeId, category.ordinal() + 1);
        return category;
    }

    /**
     * Determine mobility category based on entity type.
     */
    private MobilityCategory determineMobilityCategory(EntityType<?> type) {
        String typeId = EntityType.getKey(type).toString();

        // Flying mobs
        if (typeId.equals("minecraft:bat") ||
            typeId.equals("minecraft:parrot") ||
            typeId.equals("minecraft:bee") ||
            typeId.equals("minecraft:ghast") ||
            typeId.equals("minecraft:phantom") ||
            typeId.equals("minecraft:allay") ||
            typeId.equals("minecraft:vex")) {
            return MobilityCategory.FLYING;
        }

        // Aquatic mobs
        if (typeId.equals("minecraft:cod") ||
            typeId.equals("minecraft:salmon") ||
            typeId.equals("minecraft:pufferfish") ||
            typeId.equals("minecraft:tropical_fish") ||
            typeId.equals("minecraft:squid") ||
            typeId.equals("minecraft:glow_squid") ||
            typeId.equals("minecraft:dolphin") ||
            typeId.equals("minecraft:guardian") ||
            typeId.equals("minecraft:elder_guardian")) {
            return MobilityCategory.AQUATIC;
        }

        // Amphibious
        if (typeId.equals("minecraft:turtle") ||
            typeId.equals("minecraft:frog")) {
            return MobilityCategory.AMPHIBIOUS;
        }

        // Default to terrestrial
        return MobilityCategory.TERRESTRIAL;
    }

    /**
     * Get mobility multiplier for density calculation.
     * Flying mobs need larger areas, so counts are "spread out".
     */
    private int getMobilityMultiplier(MobilityCategory mobility) {
        return switch (mobility) {
            case FLYING -> 4;    // Spread over 4x the area
            case AQUATIC -> 2;   // Spread over 2x the area
            case AMPHIBIOUS -> 2;
            case TERRESTRIAL -> 1;
        };
    }

    /**
     * Get count in a specific region for a specific entity type.
     */
    private int getCountInRegionKey(long regionKey, EntityType<?> type) {
        int typeId = getEntityTypeId(type);
        return regionCounts.get(encodeRegionKeyWithType(regionKey, typeId));
    }

    /**
     * Count entities in adjacent regions (for mobile entities).
     */
    private int countInAdjacentRegions(long centerRegion, EntityType<?> type,
                                       ServerLevel level, BlockPos center) {
        int regionX = (int) (centerRegion >>> 32);
        int regionZ = (int) (centerRegion & 0xFFFFFFFFL);
        int typeId = getEntityTypeId(type);
        int total = 0;

        // Check 8 adjacent regions (3x3 grid minus center)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                long adjRegionKey = ((long) (regionX + dx) & 0xFFFFFFFFL) << 32
                    | (long) (regionZ + dz) & 0xFFFFFFFFL;
                total += regionCounts.get(encodeRegionKeyWithType(adjRegionKey, typeId)) / 4;
            }
        }

        return total;
    }

    /**
     * Encode region key with entity type for combined storage.
     */
    private long encodeRegionKeyWithType(long regionKey, int typeId) {
        return regionKey | ((long) typeId << 48);
    }

    /**
     * Clear all tracking data.
     */
    public void clear() {
        chunkEntityCounts.clear();
        regionCounts.clear();
        entityMobilityCache.clear();
    }

    /**
     * Get statistics about tracked entities.
     */
    public String getStats() {
        int totalChunks = chunkEntityCounts.size();
        int totalRegions = regionCounts.size();
        int totalEntities = 0;
        for (Int2IntMap counts : chunkEntityCounts.values()) {
            for (int count : counts.values()) {
                totalEntities += count;
            }
        }
        return String.format("Tracking %d entities in %d chunks, %d regions",
            totalEntities, totalChunks, totalRegions);
    }
}
