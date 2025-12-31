package me.javavirtualenv.ecology;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * Registry for code-based animal behavior configurations.
 * Provides direct registration alternative to YAML-based profiles.
 */
public final class AnimalBehaviorRegistry {
    private static final Map<ResourceLocation, AnimalConfig> REGISTRY = new HashMap<>();

    private AnimalBehaviorRegistry() {
    }

    /**
     * Register an animal configuration.
     *
     * @param entityId The entity type ID (e.g., "minecraft:pig")
     * @param config The configuration containing handles and behaviors
     */
    public static void register(String entityId, AnimalConfig config) {
        ResourceLocation key = ResourceLocation.parse(entityId);
        REGISTRY.put(key, config);
    }

    /**
     * Register an animal configuration with ResourceLocation.
     *
     * @param entityId The entity type resource location
     * @param config The configuration containing handles and behaviors
     */
    public static void register(ResourceLocation entityId, AnimalConfig config) {
        REGISTRY.put(entityId, config);
    }

    /**
     * Get configuration for a specific entity ID.
     *
     * @param entityId The entity type ID
     * @return The configuration, or null if not registered
     */
    @Nullable
    public static AnimalConfig get(String entityId) {
        ResourceLocation key = ResourceLocation.parse(entityId);
        return REGISTRY.get(key);
    }

    /**
     * Get configuration for a specific entity ID.
     *
     * @param entityId The entity type resource location
     * @return The configuration, or null if not registered
     */
    @Nullable
    public static AnimalConfig get(ResourceLocation entityId) {
        return REGISTRY.get(entityId);
    }

    /**
     * Get configuration for a mob instance.
     *
     * @param mob The mob to look up
     * @return The configuration, or null if not registered
     */
    @Nullable
    public static AnimalConfig getForMob(Mob mob) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (key == null) {
            return null;
        }
        return REGISTRY.get(key);
    }

    /**
     * Check if a mob has a code-based configuration.
     *
     * @param mob The mob to check
     * @return true if the mob has a registered configuration
     */
    public static boolean hasConfigForMob(Mob mob) {
        return getForMob(mob) != null;
    }

    /**
     * Get all registered configurations.
     *
     * @return Collection of all registered configurations
     */
    public static Collection<AnimalConfig> getAllConfigs() {
        return REGISTRY.values();
    }

    /**
     * Clear all registered configurations.
     *主要用于测试和资源重载。
     */
    public static void clear() {
        REGISTRY.clear();
    }

    /**
     * Get the number of registered configurations.
     */
    public static int size() {
        return REGISTRY.size();
    }
}
