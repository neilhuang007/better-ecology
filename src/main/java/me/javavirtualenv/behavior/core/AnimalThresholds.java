package me.javavirtualenv.behavior.core;

/**
 * Unified thresholds for animal hunger and thirst.
 * All values are on a 0-100 scale where lower = more urgent need.
 */
public final class AnimalThresholds {

    private AnimalThresholds() {
        // Utility class
    }

    // ========== HUNGER THRESHOLDS ==========
    // 0 = completely starving, 100 = completely full

    /**
     * Below this threshold, the animal actively seeks food.
     */
    public static final float HUNGRY = 50f;

    /**
     * Below this threshold, the animal is desperate and takes damage.
     */
    public static final float STARVING = 20f;

    /**
     * Above this threshold, the animal stops seeking food.
     */
    public static final float SATISFIED = 80f;

    // ========== THIRST THRESHOLDS ==========
    // 0 = completely dehydrated, 100 = completely hydrated

    /**
     * Below this threshold, the animal actively seeks water.
     */
    public static final float THIRSTY = 45f;

    /**
     * Below this threshold, the animal is desperate and takes damage.
     */
    public static final float DEHYDRATED = 15f;

    /**
     * Above this threshold, the animal stops seeking water.
     */
    public static final float HYDRATED = 75f;

    // ========== DEFAULT DECAY RATES ==========
    // Per-tick decay (applied every tick when enabled)

    /**
     * Default hunger decay per tick (0.01 = loses 1 hunger per 100 ticks = 5 seconds).
     */
    public static final float DEFAULT_HUNGER_DECAY = 0.01f;

    /**
     * Default thirst decay per tick (0.015 = loses 1.5 thirst per 100 ticks).
     */
    public static final float DEFAULT_THIRST_DECAY = 0.015f;

    // ========== DAMAGE SETTINGS ==========

    /**
     * Default damage taken when starving/dehydrated.
     */
    public static final float DEFAULT_DAMAGE = 1.0f;

    /**
     * Default interval between damage ticks when starving/dehydrated.
     */
    public static final int DEFAULT_DAMAGE_INTERVAL = 200; // 10 seconds

    // ========== RESTORATION VALUES ==========

    /**
     * Hunger restored when eating grass (herbivores).
     */
    public static final float GRASS_HUNGER_RESTORE = 5f;

    /**
     * Hunger restored per food item quality point.
     */
    public static final float FOOD_HUNGER_RESTORE_PER_NUTRITION = 5f;

    /**
     * Thirst restored per tick while drinking.
     */
    public static final float DRINKING_THIRST_RESTORE = 2f;

    /**
     * Duration of drinking in ticks.
     */
    public static final int DRINKING_DURATION = 40; // 2 seconds

    // ========== GOAL PRIORITIES ==========

    /**
     * Priority for emergency flee goals (highest priority).
     */
    public static final int PRIORITY_FLEE = 1;

    /**
     * Priority for critical needs (dehydrated/starving).
     */
    public static final int PRIORITY_CRITICAL = 2;

    /**
     * Priority for normal needs (thirsty/hungry).
     */
    public static final int PRIORITY_NORMAL = 3;

    /**
     * Priority for hunting.
     */
    public static final int PRIORITY_HUNT = 4;

    /**
     * Priority for social behaviors.
     */
    public static final int PRIORITY_SOCIAL = 5;

    /**
     * Priority for idle behaviors.
     */
    public static final int PRIORITY_IDLE = 6;
}
