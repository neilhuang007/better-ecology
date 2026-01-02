package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.state.EntityState;
import me.javavirtualenv.ecology.state.StateModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Handles thirst mechanics - decay rate varies by state (baby, injured, hunting, etc.)
 */
public final class ThirstHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:thirst-cache";
    private static final String NBT_THIRST = "thirst";
    private static final String NBT_LAST_DRINK_TICK = "lastDrinkTick";
    // Maximum ticks to simulate during catch-up (1 Minecraft day)
    private static final long MAX_CATCH_UP_TICKS = 24000L;

    @Override
    public String id() {
        return "thirst";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        ThirstCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled;
    }

    @Override
    public int tickInterval() {
        return 5;
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        ThirstCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        EntityState state = component.state();
        CompoundTag tag = component.getHandleTag(id());

        int currentThirst = getCurrentThirst(tag, cache);

        // Apply state-based decay modifier, scaled by elapsed ticks (catch-up simulation)
        double decayModifier = StateModifier.get(profile, "internal_state.thirst.conditional_modifiers",
                state, cache.isAquatic);
        double decayRate = cache.baseDecayRate * decayModifier;

        // Scale by elapsed ticks - cap catch-up to prevent infinite decay from long offline periods
        long elapsedTicks = component.elapsedTicks();
        long effectiveTicks = Math.min(Math.max(1, elapsedTicks), MAX_CATCH_UP_TICKS);
        double scaledDecay = decayRate * effectiveTicks;

        int newThirst = (int) Math.floor(currentThirst - scaledDecay);

        // Prevent jarring death: clamp to safe minimum during catch-up
        // During active updates, allow thirst to reach 0 and trigger damage
        // Catch-up should only drain resources, not kill entities
        boolean isCatchUp = elapsedTicks > 1;
        int safeMinimum = cache.dehydratedThreshold + 1;

        if (isCatchUp) {
            // Catch-up: keep thirst above dehydrated threshold
            // Entity won't die suddenly when player approaches
            newThirst = Math.max(safeMinimum, newThirst);
        } else {
            // Active update: allow normal decay to 0
            newThirst = Math.max(0, newThirst);
        }
        setThirst(tag, newThirst);

        // Update state flags
        boolean isThirsty = newThirst < cache.thirstyThreshold;
        boolean isDehydrated = newThirst < cache.dehydratedThreshold;
        state.setIsThirsty(isThirsty || isDehydrated);

        // Check for dehydration damage (only during active updates)
        if (elapsedTicks <= 1 && isDehydrated) {
            int currentTick = mob.tickCount;
            int lastDamageTick = getLastDamageTick(tag);
            if (currentTick - lastDamageTick >= cache.damageInterval) {
                mob.hurt(mob.level().damageSources().dryOut(), cache.damageAmount);
                setLastDamageTick(tag, currentTick);
            }
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Loaded automatically via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private ThirstCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBoolFast("thirst", "enabled", false);
        if (!enabled) {
            return null;
        }

        int maxValue = profile.getIntFast("thirst", "max_value", 100);
        int startingValue = profile.getIntFast("thirst", "starting_value", maxValue);
        double baseDecayRate = profile.getDoubleFast("thirst", "decay_rate", 0.01);
        int satisfiedThreshold = profile.getIntFast("thirst", "satisfied", 70);
        int thirstyThreshold = profile.getIntFast("thirst", "thirsty", 40);
        int dehydratedThreshold = profile.getIntFast("thirst", "dehydrated", 15);
        int damageInterval = profile.getIntFast("thirst", "damage_interval", 200);
        float damageAmount = (float) profile.getDoubleFast("thirst", "damage_amount", 1.0);

        boolean isAquatic = profile.getBool("tags.aquatic", false);

        return new ThirstCache(enabled, maxValue, startingValue, baseDecayRate,
                satisfiedThreshold, thirstyThreshold, dehydratedThreshold,
                damageInterval, damageAmount, isAquatic);
    }

    private int getCurrentThirst(CompoundTag tag, ThirstCache cache) {
        if (!tag.contains(NBT_THIRST)) {
            return cache.startingValue;
        }
        return tag.getInt(NBT_THIRST);
    }

    private void setThirst(CompoundTag tag, int value) {
        tag.putInt(NBT_THIRST, value);
    }

    private int getLastDamageTick(CompoundTag tag) {
        return tag.getInt(NBT_LAST_DRINK_TICK);
    }

    private void setLastDamageTick(CompoundTag tag, int tick) {
        tag.putInt(NBT_LAST_DRINK_TICK, tick);
    }

    /**
     * Restores hydration for an entity by increasing thirst value.
     * Called when animals consume water-containing vegetation or snow.
     *
     * @param mob The entity to restore hydration for
     * @param component The entity's ecology component
     * @param profile The entity's ecology profile
     * @param amount The amount of thirst to restore (will be clamped to maxValue)
     */
    public void restoreHydration(Mob mob, EcologyComponent component, EcologyProfile profile, int amount) {
        ThirstCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null || amount <= 0) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());
        int currentThirst = getCurrentThirst(tag, cache);
        int newThirst = Math.min(cache.maxValue, currentThirst + amount);

        setThirst(tag, newThirst);

        // Update state flags after restoration
        EntityState state = component.state();
        boolean isThirsty = newThirst < cache.thirstyThreshold;
        boolean isDehydrated = newThirst < cache.dehydratedThreshold;
        state.setIsThirsty(isThirsty || isDehydrated);
    }

    private record ThirstCache(
            boolean enabled,
            int maxValue,
            int startingValue,
            double baseDecayRate,
            int satisfiedThreshold,
            int thirstyThreshold,
            int dehydratedThreshold,
            int damageInterval,
            float damageAmount,
            boolean isAquatic
    ) {}
}
