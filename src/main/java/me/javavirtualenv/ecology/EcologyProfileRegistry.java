package me.javavirtualenv.ecology;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

public final class EcologyProfileRegistry {
	private static final Map<ResourceLocation, EcologyProfile> PROFILES = new HashMap<>();
	private static final Map<ResourceLocation, List<EcologyHandle>> HANDLE_CACHE = new HashMap<>();
	private static int generation = 0;

	private EcologyProfileRegistry() {
	}

	public static void reload(List<EcologyProfile> profiles) {
		PROFILES.clear();
		HANDLE_CACHE.clear();
		for (EcologyProfile profile : profiles) {
			PROFILES.put(profile.id(), profile);
			HANDLE_CACHE.put(profile.id(), EcologyHandleRegistry.resolve(profile));
		}
		generation++;
	}

	public static int generation() {
		return generation;
	}

	public static Collection<EcologyProfile> getAllProfiles() {
		return PROFILES.values();
	}

	@Nullable
	public static EcologyProfile get(ResourceLocation id) {
		return PROFILES.get(id);
	}

	@Nullable
	public static EcologyProfile getForMob(Mob mob) {
		ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
		if (key == null) {
			return null;
		}
		return PROFILES.get(key);
	}

	public static List<EcologyHandle> getHandles(EcologyProfile profile) {
		return HANDLE_CACHE.getOrDefault(profile.id(), List.of());
	}

	public static List<EcologyHandle> getHandlesForMob(Mob mob) {
		EcologyProfile profile = getForMob(mob);
		if (profile == null) {
			return List.of();
		}
		return getHandles(profile);
	}
}
