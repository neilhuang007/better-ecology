package me.javavirtualenv.ecology;

import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Map;
import me.javavirtualenv.ecology.handles.SpawnHandle;
import me.javavirtualenv.ecology.handles.SpawnHandle.CapacityMethod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks and enforces spawn density limits for entities.
 * Prevents overpopulation by checking carrying capacity before allowing spawns.
 */
public final class SpawnDensityTracker {
	private SpawnDensityTracker() {
	}

	/**
	 * Check if a spawn should be allowed based on density limits.
	 *
	 * @param level The server level
	 * @param entityType The type of entity being spawned
	 * @param pos The spawn position
	 * @return true if the spawn should be allowed, false otherwise
	 */
	public static boolean canSpawn(ServerLevel level, EntityType<?> entityType, ChunkPos pos) {
		EcologyProfile profile = getProfileForEntityType(entityType);
		if (profile == null) {
			return true;
		}

		SpawnHandle.SpawnConfig config = SpawnHandle.getConfig(profile);
		if (config == null) {
			return true;
		}

		return switch (config.capacityMethod()) {
			case HARD_CAP -> checkHardCap(level, entityType, pos, config.perChunk());
			case SOFT_CAP -> checkSoftCap(level, entityType, pos, config.perChunk());
			case RESOURCE_BASED -> checkResourceBasedCapacity(level, entityType, pos, config);
		};
	}

	/**
	 * Get ecology profile for an entity type.
	 * Looks up the entity type in the registry.
	 */
	@Nullable
	private static EcologyProfile getProfileForEntityType(EntityType<?> entityType) {
		ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
		if (key == null) {
			return null;
		}
		return EcologyProfileRegistry.get(key);
	}

	/**
	 * Check if a spawn should be allowed based on density limits.
	 * This overload uses the mob's position to determine the chunk.
	 *
	 * @param mob The mob being spawned
	 * @return true if the spawn should be allowed, false otherwise
	 */
	public static boolean canSpawn(Mob mob) {
		if (!(mob.level() instanceof ServerLevel level)) {
			return true;
		}

		ChunkPos pos = new ChunkPos(mob.blockPosition());
		return canSpawn(level, mob.getType(), pos);
	}

	/**
	 * Hard cap: prevent spawning if at or above capacity.
	 */
	private static boolean checkHardCap(ServerLevel level, EntityType<?> entityType, ChunkPos pos, int capacity) {
		int currentCount = countEntitiesInChunk(level, entityType, pos);
		return currentCount < capacity;
	}

	/**
	 * Soft cap: reduce spawn probability as density approaches capacity.
	 */
	private static boolean checkSoftCap(ServerLevel level, EntityType<?> entityType, ChunkPos pos, int capacity) {
		int currentCount = countEntitiesInChunk(level, entityType, pos);
		if (currentCount >= capacity) {
			return false;
		}

		// Calculate probability based on remaining capacity
		double fillRatio = (double) currentCount / capacity;
		double spawnChance = 1.0 - fillRatio;

		// At 80% capacity, spawn chance is 20%
		// At 50% capacity, spawn chance is 50%
		return level.random.nextDouble() < spawnChance;
	}

	/**
	 * Resource-based capacity: check available food/water in area.
	 * This is a placeholder for future implementation.
	 */
	private static boolean checkResourceBasedCapacity(ServerLevel level, EntityType<?> entityType, ChunkPos pos, SpawnHandle.SpawnConfig config) {
		// TODO: Implement resource-based checking
		// For now, fall back to soft cap behavior
		return checkSoftCap(level, entityType, pos, config.perChunk());
	}

	/**
	 * Count entities of a specific type in a chunk.
	 * Uses AABB to query entities in the chunk bounds.
	 */
	private static int countEntitiesInChunk(ServerLevel level, EntityType<?> entityType, ChunkPos pos) {
		try {
			// Create AABB for the entire chunk
			AABB box = new AABB(
				pos.x * 16, level.getMinBuildHeight(), pos.z * 16,
				(pos.x + 1) * 16, level.getMaxBuildHeight(), (pos.z + 1) * 16
			);

			// Get all entities in the chunk bounds, then filter by type
			var entities = level.getEntitiesOfClass(entityType.getBaseClass(), box);

			int count = 0;
			for (var entity : entities) {
				if (entity.getType() == entityType) {
					count++;
				}
			}
			return count;
		} catch (ConcurrentModificationException e) {
			// If the entity list is being modified, retry once
			try {
				AABB box = new AABB(
					pos.x * 16, level.getMinBuildHeight(), pos.z * 16,
					(pos.x + 1) * 16, level.getMaxBuildHeight(), (pos.z + 1) * 16
				);

				var entities = level.getEntitiesOfClass(entityType.getBaseClass(), box);

				int count = 0;
				for (var entity : entities) {
					if (entity.getType() == entityType) {
						count++;
					}
				}
				return count;
			} catch (Exception ex) {
				return 0;
			}
		}
	}

	/**
	 * Cache for density calculations to avoid repeated lookups.
	 * Maps dimension + chunk + entity type to count.
	 */
	private static final Map<String, DensityCacheEntry> densityCache = new IdentityHashMap<>();

	/**
	 * Get cached density count, calculating if not cached.
	 */
	private static int getCachedCount(ServerLevel level, EntityType<?> entityType, ChunkPos pos) {
		String cacheKey = buildCacheKey(level, entityType, pos);
		DensityCacheEntry entry = densityCache.get(cacheKey);

		if (entry != null && entry.isValid(level)) {
			return entry.count();
		}

		int count = countEntitiesInChunk(level, entityType, pos);
		densityCache.put(cacheKey, new DensityCacheEntry(count, level.getGameTime()));
		return count;
	}

	/**
	 * Build cache key for density tracking.
	 */
	private static String buildCacheKey(ServerLevel level, EntityType<?> entityType, ChunkPos pos) {
		return level.dimension().location() + ":" + pos.x + ":" + pos.z + ":" + entityType.toString();
	}

	/**
	 * Clear density cache for a chunk.
	 * Call this when entities are added/removed.
	 */
	public static void invalidateCache(ServerLevel level, ChunkPos pos) {
		// Remove all cache entries for this chunk
		densityCache.entrySet().removeIf(entry -> {
			String key = entry.getKey();
			String prefix = level.dimension().location() + ":" + pos.x + ":" + pos.z + ":";
			return key.startsWith(prefix);
		});
	}

	/**
	 * Clear all density cache.
	 * Call this periodically to prevent memory leaks.
	 */
	public static void clearCache() {
		densityCache.clear();
	}

	/**
	 * Cache entry for density counts.
	 */
	private record DensityCacheEntry(int count, long timestamp) {
		boolean isValid(ServerLevel level) {
			// Cache is valid for 5 seconds (100 ticks)
			return level.getGameTime() - timestamp < 100;
		}
	}
}
