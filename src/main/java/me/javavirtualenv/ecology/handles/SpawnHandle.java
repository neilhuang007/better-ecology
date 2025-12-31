package me.javavirtualenv.ecology.handles;

import java.util.List;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.world.entity.Mob;

public final class SpawnHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:spawn-cache";

	@Override
	public String id() {
		return "spawn";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		return profile.getMap("population.spawning") != null;
	}

	@Override
	public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
		// Spawn configuration is used by biome modifiers, not during entity tick
	}

	/**
	 * Get the spawn configuration for this profile.
	 * This is called by biome modifiers to set up spawn rules.
	 */
	public static SpawnConfig getConfig(EcologyProfile profile) {
		return profile.cached(CACHE_KEY, () -> buildSpawnConfig(profile));
	}

	private static SpawnConfig buildSpawnConfig(EcologyProfile profile) {
		// Spawn weight and group size
		int weight = profile.getInt("population.spawning.weight", 10);
		int[] groupSize = parseIntArray(profile.getList("population.spawning.group_size"), new int[]{1, 3});

		// Spawn conditions
		SpawnConditions conditions = buildSpawnConditions(profile);

		// Carrying capacity
		int perChunk = profile.getInt("population.carrying_capacity.caps.per_chunk", 10);
		CapacityMethod capacityMethod = parseCapacityMethod(
			profile.getString("population.carrying_capacity.method", "SOFT_CAP")
		);

		// Regional settings (for optimized spawning)
		Double density = profile.getDouble("population.spawning.density", 0.1);
		Integer maxPerRegion = profile.getInt("population.spawning.max_per_region", 10);

		return new SpawnConfig(weight, groupSize, conditions, perChunk, capacityMethod, density, maxPerRegion);
	}

	private static SpawnConditions buildSpawnConditions(EcologyProfile profile) {
		List<String> blocks = profile.getStringList("population.spawning.conditions.blocks");
		List<String> biomes = profile.getStringList("population.spawning.conditions.biomes");
		int[] light = parseIntArray(profile.getList("population.spawning.conditions.light"), new int[]{0, 15});
		int[] altitude = parseIntArray(profile.getList("population.spawning.conditions.altitude"), new int[]{-64, 320});
		int[] time = parseIntArray(profile.getList("population.spawning.conditions.time"), null);
		String weather = profile.getString("population.spawning.conditions.weather", "any");
		boolean inCaves = profile.getBool("population.spawning.conditions.in_caves", false);

		return new SpawnConditions(blocks, biomes, light, altitude, time, weather, inCaves);
	}

	private static int[] parseIntArray(List<?> list, int[] defaultValue) {
		if (list == null || list.isEmpty()) {
			return defaultValue;
		}
		if (list.size() != 2) {
			return defaultValue;
		}
		Object min = list.get(0);
		Object max = list.get(1);
		int minVal = min instanceof Number n ? n.intValue() : 0;
		int maxVal = max instanceof Number n ? n.intValue() : 0;
		return new int[]{minVal, maxVal};
	}

	private static CapacityMethod parseCapacityMethod(String method) {
		if (method == null) {
			return CapacityMethod.SOFT_CAP;
		}
		return switch (method.toUpperCase()) {
			case "HARD_CAP" -> CapacityMethod.HARD_CAP;
			case "RESOURCE_BASED" -> CapacityMethod.RESOURCE_BASED;
			default -> CapacityMethod.SOFT_CAP;
		};
	}

	/**
	 * Spawn configuration record.
	 * Contains all spawn-related data for an entity type.
	 */
	public record SpawnConfig(
		int weight,
		int[] groupSize,
		SpawnConditions conditions,
		int perChunk,
		CapacityMethod capacityMethod,
		Double density,
		Integer maxPerRegion
	) {
		public Double density() {
			return density != null ? density : 0.1;
		}

		public Integer maxPerRegion() {
			return maxPerRegion != null ? maxPerRegion : 10;
		}
	}

	/**
	 * Spawn conditions record.
	 * Contains environmental requirements for spawning.
	 */
	public record SpawnConditions(
		List<String> blocks,
		List<String> biomes,
		int[] light,
		int[] altitude,
		int[] time,
		String weather,
		boolean inCaves
	) {
		public SpawnConditions(
			List<String> blocks,
			List<String> biomes,
			int[] light,
			int[] altitude,
			int[] time,
			String weather
		) {
			this(blocks, biomes, light, altitude, time, weather, false);
		}
	}

	/**
	 * Capacity method enum.
	 * Defines how population limits are enforced.
	 */
	public enum CapacityMethod {
		HARD_CAP,
		SOFT_CAP,
		RESOURCE_BASED
	}
}
