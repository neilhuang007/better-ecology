package me.javavirtualenv.ecology.handles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DietHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:diet-cache";

	@Override
	public String id() {
		return "diet";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		DietCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		return !cache.isEmpty();
	}

	@Override
	public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile, ItemStack stack, boolean original) {
		DietCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		if (cache.isEmpty()) {
			return original;
		}
		if (cache.matches(stack)) {
			return true;
		}
		return original;
	}

	private DietCache buildCache(EcologyProfile profile) {
		Set<Item> items = new HashSet<>();
		Set<TagKey<Item>> tags = new HashSet<>();
		List<String> breedingItems = profile.getStringList("player_interaction.player_breeding.items");
		for (String entry : breedingItems) {
			addTarget(items, tags, entry);
		}
		List<java.util.Map<String, Object>> primaryFoods = profile.getMapList("diet.food_sources.primary");
		for (java.util.Map<String, Object> entry : primaryFoods) {
			Object type = entry.get("type");
			Object target = entry.get("target");
			if (type instanceof String typeString && "ITEM".equalsIgnoreCase(typeString) && target instanceof String targetString) {
				addTarget(items, tags, targetString);
			}
		}
		return new DietCache(items, tags);
	}

	private void addTarget(Set<Item> items, Set<TagKey<Item>> tags, String id) {
		if (id == null || id.isBlank()) {
			return;
		}
		if (id.startsWith("#")) {
			ResourceLocation tagId = ResourceLocation.tryParse(id.substring(1));
			if (tagId != null) {
				tags.add(TagKey.create(Registries.ITEM, tagId));
			}
			return;
		}
		ResourceLocation itemId = ResourceLocation.tryParse(id);
		if (itemId == null) {
			return;
		}
		BuiltInRegistries.ITEM.getOptional(itemId).ifPresent(items::add);
	}

	private static final class DietCache {
		private final Set<Item> items;
		private final Set<TagKey<Item>> tags;

		private DietCache(Set<Item> items, Set<TagKey<Item>> tags) {
			this.items = items;
			this.tags = tags;
		}

		private boolean isEmpty() {
			return items.isEmpty() && tags.isEmpty();
		}

		private boolean matches(ItemStack stack) {
			for (Item item : items) {
				if (stack.is(item)) {
					return true;
				}
			}
			for (TagKey<Item> tag : tags) {
				if (stack.is(tag)) {
					return true;
				}
			}
			return false;
		}
	}
}
