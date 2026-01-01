package me.javavirtualenv.behavior;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Central registry for behavior rules in the ecology system.
 * <p>
 * This class manages the registration, weighting, and application of behavior rules.
 * It serves as the main entry point for the behavior system, allowing entities
 * to query and apply weighted combinations of behavioral forces.
 * <p>
 * The registry uses a thread-safe design to support concurrent access from
 * multiple entities. Behavior rules are organized by type for efficient lookup.
 * <p>
 * Example usage:
 * <pre>{@code
 * BehaviorRegistry registry = new BehaviorRegistry();
 * registry.register("separation", new SeparationBehavior());
 * registry.register("cohesion", new CohesionBehavior());
 *
 * BehaviorWeights weights = new BehaviorWeights();
 * weights.setSeparation(1.5);
 * weights.setCohesion(1.0);
 *
 * Vec3d steering = registry.calculate(entity, weights);
 * }</pre>
 *
 * @see BehaviorRule
 * @see BehaviorWeights
 * @see BehaviorContext
 */
public class BehaviorRegistry {

    /**
     * Storage for registered behavior rules.
     * Uses ConcurrentHashMap for thread-safe access.
     */
    private final ConcurrentHashMap<String, BehaviorEntry> rules;

    /**
     * Internal class representing a registered behavior rule with its metadata.
     */
    private static class BehaviorEntry {
        final BehaviorRule rule;
        final String category;
        boolean enabled;

        BehaviorEntry(BehaviorRule rule, String category) {
            this.rule = rule;
            this.category = category;
            this.enabled = true;
        }
    }

    /**
     * Creates a new empty BehaviorRegistry.
     */
    public BehaviorRegistry() {
        this.rules = new ConcurrentHashMap<>();
    }

    /**
     * Registers a behavior rule with a given identifier.
     *
     * @param identifier Unique identifier for this behavior (e.g., "separation", "pursuit")
     * @param rule The behavior rule implementation
     * @throws IllegalArgumentException if a rule with this identifier already exists
     * @throws NullPointerException if rule is null
     */
    public void register(String identifier, BehaviorRule rule) {
        register(identifier, rule, "general");
    }

    /**
     * Registers a behavior rule with a given identifier and category.
     *
     * @param identifier Unique identifier for this behavior
     * @param rule The behavior rule implementation
     * @param category Category for grouping related behaviors (e.g., "flocking", "predation")
     * @throws IllegalArgumentException if a rule with this identifier already exists
     * @throws NullPointerException if rule or category is null
     */
    public void register(String identifier, BehaviorRule rule, String category) {
        if (rule == null) {
            throw new NullPointerException("Behavior rule cannot be null");
        }
        if (category == null) {
            throw new NullPointerException("Category cannot be null");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }

        BehaviorEntry entry = new BehaviorEntry(rule, category);
        BehaviorEntry previous = rules.putIfAbsent(identifier, entry);
        if (previous != null) {
            throw new IllegalArgumentException("Behavior rule '" + identifier + "' is already registered");
        }
    }

    /**
     * Unregisters a behavior rule.
     *
     * @param identifier The identifier of the rule to remove
     * @return true if the rule was removed, false if it wasn't registered
     */
    public boolean unregister(String identifier) {
        return rules.remove(identifier) != null;
    }

    /**
     * Enables or disables a behavior rule.
     * Disabled rules return zero force when calculated.
     *
     * @param identifier The identifier of the rule
     * @param enabled true to enable, false to disable
     * @return true if the rule exists and was updated, false otherwise
     */
    public boolean setEnabled(String identifier, boolean enabled) {
        BehaviorEntry entry = rules.get(identifier);
        if (entry != null) {
            entry.enabled = enabled;
            return true;
        }
        return false;
    }

    /**
     * Checks if a behavior rule is enabled.
     *
     * @param identifier The identifier of the rule
     * @return true if the rule exists and is enabled, false otherwise
     */
    public boolean isEnabled(String identifier) {
        BehaviorEntry entry = rules.get(identifier);
        return entry != null && entry.enabled;
    }

    /**
     * Calculates the weighted behavior force for a single rule.
     *
     * @param identifier The behavior rule identifier
     * @param context The behavior context
     * @param weight The weight to apply to the result
     * @return The weighted force vector, or zero if rule doesn't exist or is disabled
     */
    public Vec3d calculate(String identifier, BehaviorContext context, double weight) {
        BehaviorEntry entry = rules.get(identifier);
        if (entry == null || !entry.enabled || weight == 0.0) {
            return new Vec3d();
        }

        Vec3d force = entry.rule.calculate(context);
        if (force != null) {
            force.mult(weight);
        }
        return force;
    }

    /**
     * Calculates the combined weighted behavior force using all registered rules.
     * This method applies all rules and sums their weighted contributions.
     *
     * @param context The behavior context
     * @param weights The weights to apply to each behavior type
     * @return The combined weighted force vector
     */
    public Vec3d calculate(BehaviorContext context, BehaviorWeights weights) {
        Vec3d totalForce = new Vec3d();

        // Flocking behaviors
        addWeightedForce(totalForce, "separation", context, weights.getSeparation());
        addWeightedForce(totalForce, "alignment", context, weights.getAlignment());
        addWeightedForce(totalForce, "cohesion", context, weights.getCohesion());

        // Foraging behaviors
        addWeightedForce(totalForce, "foodSeek", context, weights.getFoodSeek());
        addWeightedForce(totalForce, "waterSeek", context, weights.getWaterSeek());
        addWeightedForce(totalForce, "shelterSeek", context, weights.getShelterSeek());

        // Predation behaviors
        addWeightedForce(totalForce, "pursuit", context, weights.getPursuit());
        addWeightedForce(totalForce, "evasion", context, weights.getEvasion());

        // Territorial behaviors
        addWeightedForce(totalForce, "territorialDefense", context, weights.getTerritorialDefense());
        addWeightedForce(totalForce, "wander", context, weights.getWander());

        // Environmental behaviors
        addWeightedForce(totalForce, "obstacleAvoidance", context, weights.getObstacleAvoidance());
        addWeightedForce(totalForce, "lightPreference", context, weights.getLightPreference());

        // Bee behaviors
        addWeightedForce(totalForce, "pollination", context, weights.getPollination());
        addWeightedForce(totalForce, "hive_return", context, weights.getHiveReturn());
        addWeightedForce(totalForce, "waggle_dance", context, weights.getWaggleDance());
        addWeightedForce(totalForce, "hive_defense", context, weights.getHiveDefense());

        // Aquatic behaviors
        addWeightedForce(totalForce, "schooling", context, weights.getSchooling());
        addWeightedForce(totalForce, "current_riding", context, weights.getCurrentRiding());
        addWeightedForce(totalForce, "escape", context, weights.getEscape());
        addWeightedForce(totalForce, "panic", context, weights.getPanic());
        addWeightedForce(totalForce, "upstream", context, weights.getUpstream());
        addWeightedForce(totalForce, "ink_cloud", context, weights.getInkCloud());
        addWeightedForce(totalForce, "vertical_migration", context, weights.getVerticalMigration());
        addWeightedForce(totalForce, "prey_attraction", context, weights.getPreyAttraction());
        addWeightedForce(totalForce, "inflate", context, weights.getInflate());
        addWeightedForce(totalForce, "hunting", context, weights.getHunting());
        addWeightedForce(totalForce, "play_dead", context, weights.getPlayDead());
        addWeightedForce(totalForce, "wave_riding", context, weights.getWaveRiding());
        addWeightedForce(totalForce, "treasure_hunt", context, weights.getTreasureHunt());
        addWeightedForce(totalForce, "metamorphosis", context, weights.getMetamorphosis());
        addWeightedForce(totalForce, "home_range", context, weights.getHomeRange());
        addWeightedForce(totalForce, "roosting", context, weights.getRoosting());

        return totalForce;
    }

    /**
     * Calculates combined force using a custom weight lookup function.
     * This allows for more flexible weight selection strategies.
     *
     * @param context The behavior context
     * @param weightLookup Function that maps behavior identifiers to weights
     * @return The combined weighted force vector
     */
    public Vec3d calculate(BehaviorContext context, Function<String, Double> weightLookup) {
        Vec3d totalForce = new Vec3d();

        rules.forEach((identifier, entry) -> {
            if (entry.enabled) {
                double weight = weightLookup.apply(identifier);
                if (weight != 0.0) {
                    Vec3d force = entry.rule.calculate(context);
                    if (force != null) {
                        force.mult(weight);
                        totalForce.add(force);
                    }
                }
            }
        });

        return totalForce;
    }

    /**
     * Calculates combined force and converts to Minecraft's Vec3 format.
     * This is the primary method intended for use with entity movement.
     *
     * @param context The behavior context
     * @param weights The weights to apply to each behavior type
     * @return The combined weighted force as a Minecraft Vec3
     */
    public Vec3 calculateMinecraftVec3(BehaviorContext context, BehaviorWeights weights) {
        Vec3d force = calculate(context, weights);
        return force.toMinecraftVec3();
    }

    /**
     * Helper method to add weighted force to total.
     * Handles null checks and avoids unnecessary allocations.
     */
    private void addWeightedForce(Vec3d total, String identifier, BehaviorContext context, double weight) {
        if (weight == 0.0) {
            return;
        }

        BehaviorEntry entry = rules.get(identifier);
        if (entry != null && entry.enabled) {
            Vec3d force = entry.rule.calculate(context);
            if (force != null && (force.x != 0 || force.y != 0 || force.z != 0)) {
                force.mult(weight);
                total.add(force);
            }
        }
    }

    /**
     * Gets a list of all registered behavior identifiers.
     *
     * @return List of registered identifiers
     */
    public List<String> getRegisteredBehaviors() {
        return new ArrayList<>(rules.keySet());
    }

    /**
     * Checks if a behavior rule is registered.
     *
     * @param identifier The identifier to check
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(String identifier) {
        return rules.containsKey(identifier);
    }

    /**
     * Gets the number of registered behavior rules.
     *
     * @return The count of registered rules
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * Clears all registered behavior rules.
     */
    public void clear() {
        rules.clear();
    }
}
