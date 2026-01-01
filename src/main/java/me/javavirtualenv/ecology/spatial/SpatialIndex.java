package me.javavirtualenv.ecology.spatial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Chunk-based spatial index for fast entity proximity queries.
 * Uses the existing Minecraft chunk grid for spatial partitioning.
 *
 * Based on spatial partitioning patterns from:
 * - https://gameprogrammingpatterns.com/spatial-partition.html
 * - https://kirbysayshi.com/broad-phase-bng/broad-phase-collision-detection-using-spatial-partitioning.html
 *
 * Performance: O(1) lookup by chunk, O(k) iteration where k = entities in nearby chunks
 * vs O(n) for getEntitiesOfClass with AABB search
 */
public final class SpatialIndex {
	// Spatial hash: chunk key -> list of entities in that chunk
	// Using ConcurrentHashMap for thread-safe access
	private static final Map<ChunkKey, List<Mob>> index = new ConcurrentHashMap<>();

	// Track last known chunk position per mob for efficient updates
	private static final Map<UUID, ChunkKey> lastKnownChunk = new ConcurrentHashMap<>();

	// Clear index on world unload
	public static void clear() {
		index.clear();
		lastKnownChunk.clear();
	}

	/**
	 * Register an entity in the spatial index.
	 * Call when entity spawns or is loaded.
	 * Idempotent - safe to call multiple times.
	 */
	public static void register(Mob mob) {
		ChunkKey key = getKey(mob);
		List<Mob> chunk = index.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

		// Only add if not already present (idempotent)
		if (!chunk.contains(mob)) {
			chunk.add(mob);
		}

		// Track last known chunk
		lastKnownChunk.put(mob.getUUID(), key);
	}

	/**
	 * Unregister an entity from the spatial index.
	 * Call when entity despawns or is unloaded.
	 */
	public static void unregister(Mob mob) {
		ChunkKey key = lastKnownChunk.remove(mob.getUUID());
		if (key == null) {
			key = getKey(mob);
		}
		unregisterFromChunk(mob, key);
	}

	/**
	 * Unregister an entity from a specific chunk key.
	 * Used for efficient chunk-crossing updates.
	 */
	private static void unregisterFromChunk(Mob mob, ChunkKey key) {
		List<Mob> chunk = index.get(key);
		if (chunk != null) {
			chunk.remove(mob);
			if (chunk.isEmpty()) {
				index.remove(key);
			}
		}
	}

	/**
	 * Update entity's position in the index when it crosses chunk boundaries.
	 * Returns true if entity moved to a different chunk.
	 */
	public static boolean update(Mob mob) {
		ChunkKey currentKey = getKey(mob);
		ChunkKey lastKey = lastKnownChunk.get(mob.getUUID());

		if (lastKey == null) {
			// First time seeing this mob, register it
			register(mob);
			return true;
		}

		if (!currentKey.equals(lastKey)) {
			// Mob crossed chunk boundary - move it to new chunk
			unregisterFromChunk(mob, lastKey);
			register(mob);
			return true;
		}

		return false;
	}

	/**
	 * Find nearby entities of the same type within radius.
	 * Uses chunk-based lookup for O(1) + O(k) performance.
	 *
	 * @param mob Center entity
	 * @param radius Search radius in blocks
	 * @return List of nearby same-type entities
	 */
	public static List<Mob> getNearbySameType(Mob mob, int radius) {
		List<Mob> result = new ArrayList<>();
		int chunkX = mob.chunkPosition().x;
		int chunkZ = mob.chunkPosition().z;
		int chunkRadius = (radius + 15) / 16; // Convert to chunk radius

		Class<?> entityType = mob.getClass();
		String dimension = getDimensionId(mob.level());

		// Search nearby chunks
		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
				ChunkKey key = new ChunkKey(dimension, chunkX + dx, chunkZ + dz);
				List<Mob> chunkEntities = index.get(key);
				if (chunkEntities != null) {
					for (Mob other : chunkEntities) {
						if (other != mob && other.isAlive() && other.getClass() == entityType) {
							double distSq = mob.distanceToSqr(other);
							if (distSq <= radius * radius) {
								result.add(other);
							}
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Count nearby entities of the same type.
	 * Faster than getNearbySameType when you only need the count.
	 */
	public static int countNearbySameType(Mob mob, int radius) {
		int count = 0;
		int chunkX = mob.chunkPosition().x;
		int chunkZ = mob.chunkPosition().z;
		int chunkRadius = (radius + 15) / 16;

		Class<?> entityType = mob.getClass();
		String dimension = getDimensionId(mob.level());
		double radiusSq = radius * radius;

		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
				ChunkKey key = new ChunkKey(dimension, chunkX + dx, chunkZ + dz);
				List<Mob> chunkEntities = index.get(key);
				if (chunkEntities != null) {
					for (Mob other : chunkEntities) {
						if (other != mob && other.isAlive() && other.getClass() == entityType) {
							if (mob.distanceToSqr(other) <= radiusSq) {
								count++;
							}
						}
					}
				}
			}
		}

		return count;
	}

	/**
	 * Check if there's at least one nearby same-type entity.
	 * Early-exit optimization for "has group nearby" queries.
	 */
	public static boolean hasNearbySameType(Mob mob, int radius) {
		int chunkX = mob.chunkPosition().x;
		int chunkZ = mob.chunkPosition().z;
		int chunkRadius = (radius + 15) / 16;

		Class<?> entityType = mob.getClass();
		String dimension = getDimensionId(mob.level());
		double radiusSq = radius * radius;

		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
				ChunkKey key = new ChunkKey(dimension, chunkX + dx, chunkZ + dz);
				List<Mob> chunkEntities = index.get(key);
				if (chunkEntities != null) {
					for (Mob other : chunkEntities) {
						if (other != mob && other.isAlive() && other.getClass() == entityType) {
							if (mob.distanceToSqr(other) <= radiusSq) {
								return true; // Early exit on first found
							}
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Get all entities in a specific chunk.
	 */
	public static List<Mob> getInChunk(Level level, int chunkX, int chunkZ) {
		ChunkKey key = new ChunkKey(getDimensionId(level), chunkX, chunkZ);
		List<Mob> result = index.get(key);
		return result != null ? new ArrayList<>(result) : List.of();
	}

	/**
	 * Find nearby mobs of any type within radius.
	 * Returns all nearby mobs regardless of species.
	 *
	 * @param mob Center entity
	 * @param radius Search radius in blocks
	 * @return List of nearby mobs of any type
	 */
	public static List<Mob> getNearbyMobs(Mob mob, int radius) {
		List<Mob> result = new ArrayList<>();
		int chunkX = mob.chunkPosition().x;
		int chunkZ = mob.chunkPosition().z;
		int chunkRadius = (radius + 15) / 16;

		String dimension = getDimensionId(mob.level());

		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
				ChunkKey key = new ChunkKey(dimension, chunkX + dx, chunkZ + dz);
				List<Mob> chunkEntities = index.get(key);
				if (chunkEntities != null) {
					for (Mob other : chunkEntities) {
						if (other != mob && other.isAlive()) {
							double distSq = mob.distanceToSqr(other);
							if (distSq <= radius * radius) {
								result.add(other);
							}
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Get dimension ID as string for use in chunk key.
	 * Compatible across different Minecraft versions.
	 */
	private static String getDimensionId(Level level) {
		// Use the world itself as identifier - each world has its own spatial index
		// For multi-world servers, we can use the world's registry key
		if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			return serverLevel.dimension().location().toString();
		}
		// For client level, use a simple identifier
		return "client:" + level.hashCode();
	}

	/**
	 * Chunk-based hash key for spatial indexing.
	 * Uses dimension + chunk coordinates for unique identification.
	 */
	private static ChunkKey getKey(Mob mob) {
		return new ChunkKey(getDimensionId(mob.level()), mob.chunkPosition().x, mob.chunkPosition().z);
	}

	/**
	 * Immutable key for chunk-based spatial hashing.
	 */
	private static final class ChunkKey {
		private final String dimensionId;
		private final int chunkX;
		private final int chunkZ;

		ChunkKey(String dimensionId, int chunkX, int chunkZ) {
			this.dimensionId = dimensionId;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof ChunkKey other)) return false;
			return chunkX == other.chunkX &&
					chunkZ == other.chunkZ &&
					dimensionId.equals(other.dimensionId);
		}

		@Override
		public int hashCode() {
			int result = dimensionId.hashCode();
			result = 31 * result + chunkX;
			result = 31 * result + chunkZ;
			return result;
		}
	}
}
