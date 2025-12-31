package me.javavirtualenv.behavior.core;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Utility class for checking hunger/thirst priority state.
 * <p>
 * Other goals can use these static methods to check if they should
 * yield priority to foraging or drinking behaviors based on the
 * entity's current hunger and thirst levels.
 * <p>
 * Example usage in a goal's canUse() method:
 * <pre>{@code
 * public boolean canUse() {
 *     // Don't socialize if starving
 *     if (HungerThirstPriority.isStarving(mob)) {
 *         return false;
 *     }
 *     // Defer to foraging if very hungry
 *     if (HungerThirstPriority.getHungerPriority(mob) > 0.7) {
 *         return false;
 *     }
 *     return true;
 * }
 * }</pre>
 */
public final class HungerThirstPriority {
    private static final String STATE_HANDLE = "hunger_thirst_state";
    private static final String HUNGER_HANDLE = "hunger";
    private static final String THIRST_HANDLE = "thirst";
    private static final String NBT_HUNGER = "hunger";
    private static final String NBT_THIRST = "thirst";

    private HungerThirstPriority() {
        // Utility class - no instances
    }

    /**
     * Gets the state tag containing hunger/thirst priority information.
     * Returns empty tag if component or access is not available.
     */
    private static CompoundTag getStateTag(Mob mob) {
        if (!(mob instanceof EcologyAccess access)) {
            return new CompoundTag();
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return new CompoundTag();
        }

        return component.getHandleTag(STATE_HANDLE);
    }

    /**
     * Gets the hunger handle tag.
     * Returns empty tag if component or access is not available.
     */
    private static CompoundTag getHungerTag(Mob mob) {
        if (!(mob instanceof EcologyAccess access)) {
            return new CompoundTag();
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return new CompoundTag();
        }

        return component.getHandleTag(HUNGER_HANDLE);
    }

    /**
     * Gets the thirst handle tag.
     * Returns empty tag if component or access is not available.
     */
    private static CompoundTag getThirstTag(Mob mob) {
        if (!(mob instanceof EcologyAccess access)) {
            return new CompoundTag();
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return new CompoundTag();
        }

        return component.getHandleTag(THIRST_HANDLE);
    }

    /**
     * Checks if the mob currently needs food.
     *
     * @return true if hunger is below threshold
     */
    public static boolean needsFood(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getBoolean("needs_food");
    }

    /**
     * Checks if the mob currently needs water.
     *
     * @return true if thirst is below threshold
     */
    public static boolean needsWater(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getBoolean("needs_water");
    }

    /**
     * Checks if the mob is starving (critical hunger).
     *
     * @return true if hunger is at critical levels
     */
    public static boolean isStarving(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getBoolean("starving");
    }

    /**
     * Checks if the mob is dehydrated (critical thirst).
     *
     * @return true if thirst is at critical levels
     */
    public static boolean isDehydrated(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getBoolean("dehydrated");
    }

    /**
     * Gets the current hunger level.
     *
     * @return hunger value (0-100), or 100 if not available
     */
    public static int getHungerLevel(Mob mob) {
        CompoundTag hungerTag = getHungerTag(mob);
        if (!hungerTag.contains(NBT_HUNGER)) {
            return 100;
        }
        return hungerTag.getInt(NBT_HUNGER);
    }

    /**
     * Gets the current thirst level.
     *
     * @return thirst value (0-100), or 100 if not available
     */
    public static int getThirstLevel(Mob mob) {
        CompoundTag thirstTag = getThirstTag(mob);
        if (!thirstTag.contains(NBT_THIRST)) {
            return 100;
        }
        return thirstTag.getInt(NBT_THIRST);
    }

    /**
     * Gets the hunger priority (0.0 to 1.0, higher is more urgent).
     * <p>
     * This value can be used to compare urgency of different needs.
     * Values above 0.7 indicate high urgency.
     *
     * @return hunger priority value
     */
    public static double getHungerPriority(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getDouble("hunger_priority");
    }

    /**
     * Gets the thirst priority (0.0 to 1.0, higher is more urgent).
     * <p>
     * This value can be used to compare urgency of different needs.
     * Values above 0.7 indicate high urgency.
     *
     * @return thirst priority value
     */
    public static double getThirstPriority(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getDouble("thirst_priority");
    }

    /**
     * Checks if thirst is more critical than hunger right now.
     *
     * @return true if thirst should take priority over hunger
     */
    public static boolean isThirstMoreCritical(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getBoolean("thirst_is_critical");
    }

    /**
     * Checks if hunger is more critical than thirst right now.
     *
     * @return true if hunger should take priority over thirst
     */
    public static boolean isHungerMoreCritical(Mob mob) {
        CompoundTag stateTag = getStateTag(mob);
        return stateTag.getBoolean("hunger_is_critical");
    }

    /**
     * Gets the maximum urgency between hunger and thirst.
     * <p>
     * Useful for determining if any physiological need is urgent.
     *
     * @return max priority value (0.0 to 1.0)
     */
    public static double getMaxPhysiologicalPriority(Mob mob) {
        return Math.max(getHungerPriority(mob), getThirstPriority(mob));
    }

    /**
     * Checks if the mob should prioritize survival needs over the given activity.
     * <p>
     * Returns true if either hunger or thirst priority is above 0.7.
     * Use this in canUse() methods to defer non-essential activities.
     *
     * @return true if survival needs are urgent
     */
    public static boolean shouldPrioritizeSurvival(Mob mob) {
        return getMaxPhysiologicalPriority(mob) > 0.7;
    }
}
