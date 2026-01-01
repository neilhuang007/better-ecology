package me.javavirtualenv.ecology.spawning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * Manages biome spawn modifications for Better Ecology profiles.
 *
 * IMPORTANT: Fabric's BiomeModifications API does not support removal of modifiers
 * after they are registered. This class uses a generation-based approach as a workaround:
 *
 * 1. Each modifier stores the generation number it was registered with
 * 2. The modifier's predicate checks if its generation matches the current active generation
 * 3. When profiles are reloaded, the generation increments
 * 4. Old modifiers become no-ops because their generation check fails
 * 5. New modifiers are registered with the new generation
 *
 * This means old modifiers remain in memory but are effectively disabled.
 */
public final class BiomeSpawnModifier {
    private static final List<ResourceLocation> registeredModifierIds = new ArrayList<>();

    /**
     * Maps modifier IDs to the generation they were registered with.
     * Used to validate if a modifier should be active.
     */
    private static final Map<ResourceLocation, Integer> modifierGenerations = new HashMap<>();

    /**
     * The current active generation. Modifiers only execute if their
     * registered generation matches this value.
     */
    private static int currentActiveGeneration = 0;

    /**
     * Tracks the last registry generation we processed to avoid redundant registration.
     */
    private static int lastProcessedRegistryGeneration = -1;

    private BiomeSpawnModifier() {
    }

    public static void registerAll() {
        int registryGeneration = EcologyProfileRegistry.generation();

        // Skip if we already processed this registry generation
        if (lastProcessedRegistryGeneration == registryGeneration && !registeredModifierIds.isEmpty()) {
            return;
        }

        // Increment our active generation to invalidate all previous modifiers.
        // Old modifiers will fail their generation check and become no-ops.
        currentActiveGeneration++;
        lastProcessedRegistryGeneration = registryGeneration;

        BetterEcology.LOGGER.debug("Registering spawn modifiers for generation {} (registry gen {})",
            currentActiveGeneration, registryGeneration);

        for (EcologyProfile profile : EcologyProfileRegistry.getAllProfiles()) {
            registerProfile(profile, currentActiveGeneration);
        }
    }

    /**
     * Registers a spawn modifier for a profile with the given generation number.
     * The modifier's predicate includes a generation check that ensures it only
     * executes when its generation matches the current active generation.
     *
     * @param profile The ecology profile to register spawns for
     * @param registrationGeneration The generation number this modifier belongs to
     */
    private static void registerProfile(EcologyProfile profile, int registrationGeneration) {
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

        // Include generation in the modifier ID to ensure uniqueness across reloads
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
            BetterEcology.MOD_ID,
            entityTypeKey.getPath() + "_spawn_gen" + registrationGeneration
        );

        // Build the biome selector predicate
        Predicate<BiomeSelectionContext> biomeSelector = buildBiomeSelector(biomeTags);

        // Wrap the biome selector with a generation check.
        // This is the key to the reload workaround: when a new generation is registered,
        // old modifiers will fail this check and become no-ops.
        Predicate<BiomeSelectionContext> generationAwareSelector = context -> {
            // Only apply this modifier if it belongs to the current active generation
            if (registrationGeneration != currentActiveGeneration) {
                return false;
            }
            return biomeSelector.test(context);
        };

        BiomeModifications.create(modifierId)
            .add(ModificationPhase.ADDITIONS, generationAwareSelector, context -> {
                context.getSpawnSettings().addSpawn(
                    spawnGroup,
                    new SpawnerData(entityType, weight, minCount, maxCount)
                );
            });

        // Track this modifier and its generation
        registeredModifierIds.add(modifierId);
        modifierGenerations.put(modifierId, registrationGeneration);

        BetterEcology.LOGGER.debug("Registered spawn modifier {} (gen {}) for {} in {} biomes",
            modifierId, registrationGeneration, entityTypeKey, biomeTags.size());
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

    /**
     * Invalidates all current spawn modifiers by incrementing the generation.
     *
     * Note: This does not actually remove the BiomeModifications from Fabric's registry
     * (which is not supported by the API). Instead, it increments the generation counter
     * so that existing modifiers will fail their generation check and become no-ops.
     *
     * The tracking lists are cleared for bookkeeping purposes, but the actual
     * invalidation happens through the generation mismatch in the predicates.
     */
    public static void clearAll() {
        BetterEcology.LOGGER.debug(
            "Invalidating {} spawn modifiers (generation {} -> {})",
            registeredModifierIds.size(),
            currentActiveGeneration,
            currentActiveGeneration + 1
        );

        // Increment generation to invalidate all existing modifiers.
        // Their predicates will now return false because registrationGeneration != currentActiveGeneration.
        currentActiveGeneration++;

        // Clear tracking lists for bookkeeping (modifiers remain in Fabric but are now no-ops)
        registeredModifierIds.clear();
        modifierGenerations.clear();

        // Reset the processed generation so registerAll() will re-register on next call
        lastProcessedRegistryGeneration = -1;
    }

    /**
     * Returns the current active generation number.
     * Useful for debugging and testing.
     */
    public static int getCurrentGeneration() {
        return currentActiveGeneration;
    }

    /**
     * Returns the number of currently tracked (active) modifiers.
     * Useful for debugging and testing.
     */
    public static int getActiveModifierCount() {
        return registeredModifierIds.size();
    }
}
