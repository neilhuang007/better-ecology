package me.javavirtualenv.ecology.state;

import java.util.Map;
import me.javavirtualenv.ecology.EcologyProfile;

/**
 * Applies conditional modifiers from YAML profile based on entity state.
 *
 * Modifiers in YAML follow this pattern:
 * ```yaml
 * conditional_modifiers:
 *   baby: 0.7
 *   elderly: 0.8
 *   injured: 0.6
 *   hungry: 0.8
 *   fleeing: 1.5
 *   hunting: 1.3
 *   carrying_offspring: 0.7
 *   in_water_nonaquatic: 0.4
 *   schooling: 1.1
 * ```
 */
public final class StateModifier {

    /**
     * Get the appropriate modifier for the current entity state.
     * Returns 1.0 if no modifier applies.
     *
     * @param profile The ecology profile containing modifier config
     * @param modifierPath YAML path to conditional_modifiers map
     * @param state Current entity state
     * @param isAquatic Whether this entity is aquatic (affects in_water check)
     * @return The modifier multiplier
     */
    public static double get(EcologyProfile profile, String modifierPath, EntityState state, boolean isAquatic) {
        Map<String, Object> modifiers = profile.getMap(modifierPath);
        if (modifiers == null || modifiers.isEmpty()) {
            return 1.0;
        }

        // Check each state in priority order (some states override others)
        Double modifier = findModifier(modifiers, state, isAquatic);
        return modifier != null ? modifier : 1.0;
    }

    /**
     * Apply modifier to a base value.
     */
    public static double apply(double base, EcologyProfile profile, String modifierPath,
                               EntityState state, boolean isAquatic) {
        return base * get(profile, modifierPath, state, isAquatic);
    }

    /**
     * Check if a specific modifier exists for the current state.
     */
    public static boolean hasModifier(EcologyProfile profile, String modifierPath,
                                      String modifierKey) {
        Map<String, Object> modifiers = profile.getMap(modifierPath);
        if (modifiers == null) {
            return false;
        }
        return modifiers.containsKey(modifierKey);
    }

    /**
     * Get a specific modifier value regardless of state.
     */
    public static double getModifierValue(EcologyProfile profile, String modifierPath,
                                          String modifierKey, double defaultValue) {
        Map<String, Object> modifiers = profile.getMap(modifierPath);
        if (modifiers == null) {
            return defaultValue;
        }
        Object value = modifiers.get(modifierKey);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    private static Double findModifier(Map<String, Object> modifiers, EntityState state, boolean isAquatic) {
        // Priority order: most restrictive states first

        // Non-aquatic in water (usually 0.0 - they die or are very slow)
        if (!isAquatic && state.isInWater()) {
            Double mod = getDouble(modifiers, "in_water_nonaquatic");
            if (mod != null) return mod;
        }

        // Fleeing takes priority over hunting
        if (state.isFleeing()) {
            Double mod = getDouble(modifiers, "fleeing");
            if (mod != null) return mod;
        }

        // Hunting
        if (state.isHunting()) {
            Double mod = getDouble(modifiers, "hunting");
            if (mod != null) return mod;
        }

        // Carrying offspring (e.g., fox carrying baby)
        if (state.isCarryingOffspring()) {
            Double mod = getDouble(modifiers, "carrying_offspring");
            if (mod != null) return mod;
        }

        // Starving (worst hunger state)
        if (state.isStarving()) {
            Double mod = getDouble(modifiers, "starving");
            if (mod != null) return mod;
        }

        // Dehydrated (worst thirst state)
        if (state.isDehydrated()) {
            Double mod = getDouble(modifiers, "dehydrated");
            if (mod != null) return mod;
        }

        // Injured (low health)
        if (state.isInjured()) {
            Double mod = getDouble(modifiers, "injured");
            if (mod != null) return mod;
        }

        // Hungry (but not starving)
        if (state.isHungry()) {
            Double mod = getDouble(modifiers, "hungry");
            if (mod != null) return mod;
        }

        // Thirsty
        if (state.isThirsty()) {
            Double mod = getDouble(modifiers, "thirsty");
            if (mod != null) return mod;
        }

        // Elderly
        if (state.isElderly()) {
            Double mod = getDouble(modifiers, "elderly");
            if (mod != null) return mod;
        }

        // Baby (youngest state)
        if (state.isBaby()) {
            Double mod = getDouble(modifiers, "baby");
            if (mod != null) return mod;
        }

        // Schooling (fish schools)
        if (modifiers.containsKey("schooling")) {
            // Would need to check if in school - for now skip
        }

        return null;
    }

    private static Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    /**
     * Parse a modifier value from various types.
     */
    public static double parseModifier(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
