package me.javavirtualenv.ecology.spawning;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.EcologyProfileRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;

public final class BiomeSpawnModifier {
    private static final List<ResourceLocation> registeredModifierIds = new ArrayList<>();
    private static int generation = 0;

    private BiomeSpawnModifier() {
    }

    public static void registerAll() {
        int currentGeneration = EcologyProfileRegistry.generation();
        if (generation == currentGeneration && !registeredModifierIds.isEmpty()) {
            return;
        }
        generation = currentGeneration;

        for (EcologyProfile profile : EcologyProfileRegistry.getAllProfiles()) {
            registerProfile(profile);
        }
    }

    private static void registerProfile(EcologyProfile profile) {
        // Check if spawning is enabled for this profile
        boolean spawningEnabled = profile.getBool("population.spawning.enabled", false);
        if (!spawningEnabled) {
            return;
        }

        // Get biome tags from correct path
        List<String> biomeTags = profile.getStringList("population.spawning.conditions.biomes");
        if (biomeTags.isEmpty()) {
            return;
        }

        // Get mob_id from correct path (identity.mob_id)
        String mobIdStr = profile.getString("identity.mob_id", null);
        if (mobIdStr == null) {
            BetterEcology.LOGGER.warn("Profile {} missing identity.mob_id, skipping spawn registration", profile.id());
            return;
        }

        ResourceLocation entityTypeKey = ResourceLocation.tryParse(mobIdStr);
        if (entityTypeKey == null) {
            BetterEcology.LOGGER.warn("Profile {} has invalid mob_id: {}, skipping spawn registration", profile.id(), mobIdStr);
            return;
        }

        EntityType<?> entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(entityTypeKey);
        if (entityType == null) {
            BetterEcology.LOGGER.warn("Profile {} references unknown entity type: {}, skipping spawn registration", profile.id(), entityTypeKey);
            return;
        }

        MobCategory spawnGroup = entityType.getCategory();
        if (spawnGroup == null) {
            BetterEcology.LOGGER.warn("Entity type {} has no spawn category, skipping spawn registration", entityTypeKey);
            return;
        }

        // Get spawn weight
        int weight = profile.getInt("population.spawning.weight", 100);

        // Get group size as array [min, max]
        List<?> groupSizeList = profile.getList("population.spawning.group_size");
        int minCount;
        int maxCount;
        if (groupSizeList != null && groupSizeList.size() == 2) {
            Object min = groupSizeList.get(0);
            Object max = groupSizeList.get(1);
            minCount = min instanceof Number n ? n.intValue() : 1;
            maxCount = max instanceof Number n ? n.intValue() : 4;
        } else {
            minCount = 1;
            maxCount = 4;
        }

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(BetterEcology.MOD_ID, entityTypeKey.getPath() + "_spawn_" + generation);

        Predicate<BiomeSelectionContext> selector = buildBiomeSelector(biomeTags);

        BiomeModifications.create(modifierId)
            .add(ModificationPhase.ADDITIONS, selector, context -> {
                context.getSpawnSettings().addSpawn(
                    spawnGroup,
                    new SpawnerData(entityType, weight, minCount, maxCount)
                );
            });

        registeredModifierIds.add(modifierId);
        BetterEcology.LOGGER.debug("Registered spawn modifier {} for {} in {} biomes", modifierId, entityTypeKey, biomeTags.size());
    }

    private static Predicate<BiomeSelectionContext> buildBiomeSelector(List<String> biomeTags) {
        Predicate<BiomeSelectionContext> selector = context -> false;
        for (String tag : biomeTags) {
            ResourceLocation tagKey = ResourceLocation.tryParse(tag);
            if (tagKey != null) {
                TagKey<Biome> biomeTag = TagKey.create(Registries.BIOME, tagKey);
                Predicate<BiomeSelectionContext> tagSelector = BiomeSelectors.tag(biomeTag);
                selector = selector.or(tagSelector);
            }
        }
        return selector;
    }

    public static void clearAll() {
        BetterEcology.LOGGER.debug("Spawn modifiers will be cleared on next reload ({} registered)", registeredModifierIds.size());
        registeredModifierIds.clear();
        generation = 0;
    }
}
