package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.core.HungerThirstTriggerGoal;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Handle for registering hunger/thirst trigger behavior.
 * <p>
 * This handle registers a goal that monitors hunger and thirst levels,
 * and sets state flags to indicate when animals should prioritize
 * foraging or drinking over other activities.
 * <p>
 * Scientific basis: Hunger and thirst are primary physiological triggers
 * that cause adaptive prioritization of goals. When these needs become
 * urgent, animals shift focus from other activities to satisfy them.
 */
public final class HungerThirstTriggerHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:hunger-thirst-trigger-cache";
    private static final int DEFAULT_PRIORITY = 4;
    private static final int DEFAULT_HUNGER_THRESHOLD = 50;
    private static final int DEFAULT_THIRST_THRESHOLD = 40;

    @Override
    public String id() {
        return "hunger_thirst_trigger";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        TriggerCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled;
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        TriggerCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null || !cache.enabled) {
            return;
        }

        HungerThirstTriggerGoal goal = new HungerThirstTriggerGoal(
            mob,
            cache.hungerThreshold,
            cache.thirstThreshold,
            cache.hungerEnabled,
            cache.thirstEnabled
        );

        MobAccessor accessor = (MobAccessor) mob;
        accessor.betterEcology$getGoalSelector().addGoal(cache.priority, goal);
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // State updates are handled by the goal
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // NBT data is automatically loaded via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private TriggerCache buildCache(EcologyProfile profile) {
        // Check if the trigger system is enabled (reads from internal_state.hunger_thirst_trigger.enabled)
        boolean enabled = profile.getBool("internal_state.hunger_thirst_trigger.enabled", false);

        if (!enabled) {
            return null;
        }

        // Get priority - should be high but below survival needs (breathe, escape_danger, flee_predator)
        // Default to 4, which is above critical_health (3) but below eat_food (5) in the AI priority framework
        int priority = profile.getInt("internal_state.hunger_thirst_trigger.priority", DEFAULT_PRIORITY);

        // Get thresholds - configurable per mob type
        int hungerThreshold = profile.getInt("internal_state.hunger_thirst_trigger.hunger_threshold", DEFAULT_HUNGER_THRESHOLD);
        int thirstThreshold = profile.getInt("internal_state.hunger_thirst_trigger.thirst_threshold", DEFAULT_THIRST_THRESHOLD);

        // Check if hunger/thirst systems are enabled for this mob
        boolean hungerEnabled = profile.getBool("internal_state.hunger.enabled", false);
        boolean thirstEnabled = profile.getBool("internal_state.thirst.enabled", false);

        // At least one must be enabled for this handle to be useful
        if (!hungerEnabled && !thirstEnabled) {
            return null;
        }

        return new TriggerCache(enabled, priority, hungerThreshold, thirstThreshold, hungerEnabled, thirstEnabled);
    }

    private record TriggerCache(
        boolean enabled,
        int priority,
        int hungerThreshold,
        int thirstThreshold,
        boolean hungerEnabled,
        boolean thirstEnabled
    ) {}
}
