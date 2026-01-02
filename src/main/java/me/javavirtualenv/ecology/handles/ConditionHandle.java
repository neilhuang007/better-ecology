package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Handles body condition - overall fitness that affects breeding and survival.
 * Condition improves when satiated, degrades when hungry/starving.
 */
public final class ConditionHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:condition-cache";
    private static final String NBT_CONDITION = "condition";
    private static final long MAX_CATCH_UP_TICKS = 24000L;

    @Override
    public String id() {
        return "condition";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        ConditionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled;
    }

    @Override
    public int tickInterval() {
        return 10;
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        ConditionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());
        int currentCondition = getCurrentCondition(tag, cache);

        // Determine hunger state from other handlers
        // (we assume HungerHandle has already ticked and set the state)
        EntityState state = component.state();

        double change;
        if (state.isStarving()) {
            change = -cache.lossWhenStarving;
        } else if (state.isHungry()) {
            change = -cache.lossWhenHungry;
        } else if (isSatiated(component)) {
            change = cache.gainWhenSatiated;
        } else {
            change = 0.0;
        }

        long elapsed = component.elapsedTicks();
        long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
        boolean isCatchUp = elapsed > 1;
        change *= effectiveTicks;

        int newCondition = (int) Math.round(Math.min(cache.maxValue, Math.max(0, currentCondition + change)));

        // During catch-up, keep above critical threshold
        if (isCatchUp && newCondition < cache.criticalThreshold) {
            newCondition = cache.criticalThreshold;
        }
        setCondition(tag, newCondition);

        // Update state flags based on condition thresholds
        boolean isPoorCondition = newCondition < cache.poorThreshold;
        boolean isCriticalCondition = newCondition < cache.criticalThreshold;

        // Poor condition animals count as "injured" for modifier purposes
        if (isPoorCondition) {
            state.setIsHungry(true); // Reuse hunger flag for condition-based modifiers
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Loaded automatically
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private ConditionCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBool("internal_state.condition.enabled", false);
        if (!enabled) {
            return null;
        }

        int maxValue = profile.getInt("internal_state.condition.max_value", 100);
        int startingValue = profile.getInt("internal_state.condition.starting_value", 70);
        double gainWhenSatiated = profile.getDouble("internal_state.condition.gain_when_satiated", 0.01);
        double lossWhenHungry = profile.getDouble("internal_state.condition.loss_when_hungry", 0.02);
        double lossWhenStarving = profile.getDouble("internal_state.condition.loss_when_starving", 0.1);
        int excellentThreshold = profile.getInt("internal_state.condition.thresholds.excellent", 85);
        int goodThreshold = profile.getInt("internal_state.condition.thresholds.good", 65);
        int fairThreshold = profile.getInt("internal_state.condition.thresholds.fair", 45);
        int poorThreshold = profile.getInt("internal_state.condition.thresholds.poor", 25);
        int criticalThreshold = profile.getInt("internal_state.condition.thresholds.critical", 10);

        return new ConditionCache(enabled, maxValue, startingValue, gainWhenSatiated,
                lossWhenHungry, lossWhenStarving, excellentThreshold, goodThreshold,
                fairThreshold, poorThreshold, criticalThreshold);
    }

    private int getCurrentCondition(CompoundTag tag, ConditionCache cache) {
        if (!tag.contains(NBT_CONDITION)) {
            return cache.startingValue;
        }
        return tag.getInt(NBT_CONDITION);
    }

    private void setCondition(CompoundTag tag, int value) {
        tag.putInt(NBT_CONDITION, value);
    }

    private boolean isSatiated(EcologyComponent component) {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (!hungerTag.contains("hunger")) {
            return false;
        }
        int hunger = hungerTag.getInt("hunger");
        // Satiated is typically above 75% of max
        return hunger > 75;
    }

    public static boolean canBreed(EcologyComponent component, int minCondition) {
        CompoundTag tag = component.getHandleTag("condition");
        if (!tag.contains(NBT_CONDITION)) {
            return true; // No condition system = no restriction
        }
        return tag.getInt(NBT_CONDITION) >= minCondition;
    }

    public static int getConditionLevel(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("condition");
        if (!tag.contains(NBT_CONDITION)) {
            return 70; // Default "good" condition
        }
        return tag.getInt(NBT_CONDITION);
    }

    private record ConditionCache(
            boolean enabled,
            int maxValue,
            int startingValue,
            double gainWhenSatiated,
            double lossWhenHungry,
            double lossWhenStarving,
            int excellentThreshold,
            int goodThreshold,
            int fairThreshold,
            int poorThreshold,
            int criticalThreshold
    ) {}
}
