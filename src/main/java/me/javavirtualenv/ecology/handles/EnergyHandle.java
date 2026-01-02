package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.state.EntityState;
import me.javavirtualenv.ecology.state.StateModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Handles energy/stamina for actions like sprinting, hunting, fleeing.
 * Energy costs vary by state - exhausted animals move slower.
 */
public final class EnergyHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:energy-cache";
    private static final String NBT_ENERGY = "energy";
    private static final String NBT_IS_EXHAUSTED = "isExhausted";
    private static final long MAX_CATCH_UP_TICKS = 24000L;

    @Override
    public String id() {
        return "energy";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        EnergyCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled;
    }

    @Override
    public int tickInterval() {
        return 2;
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        EnergyCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        EntityState state = component.state();
        CompoundTag tag = component.getHandleTag(id());
        int currentEnergy = getCurrentEnergy(tag, cache);

        // Determine energy cost based on current activity
        double cost = determineEnergyCost(state, cache);
        double recovery = cache.recoveryRate;

        long elapsed = component.elapsedTicks();
        long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
        boolean isCatchUp = elapsed > 1;

        // Apply energy change (cost or recovery)
        int newEnergy;
        if (cost > 0) {
            newEnergy = Math.max(0, (int) Math.floor(currentEnergy - cost * effectiveTicks));
            // During catch-up, keep above exhaustion threshold
            if (isCatchUp) {
                newEnergy = Math.max(cache.exhaustionThreshold + 1, newEnergy);
            }
        } else {
            newEnergy = Math.min(cache.maxValue, (int) Math.ceil(currentEnergy + recovery * effectiveTicks));
        }
        setEnergy(tag, newEnergy);

        // Update exhausted state
        boolean isExhausted = newEnergy < cache.exhaustionThreshold;
        tag.putBoolean(NBT_IS_EXHAUSTED, isExhausted);

        // Exhausted animals can't sprint/hunt effectively
        if (isExhausted && (state.isHunting() || state.isFleeing())) {
            // Force stop high-energy activities
            state.setIsHunting(false);
            state.setIsFleeing(false);
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

    private EnergyCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBool("internal_state.energy.enabled", false);
        if (!enabled) {
            return null;
        }

        int maxValue = profile.getInt("internal_state.energy.max_value", 100);
        int startingValue = maxValue; // Start at full energy
        double recoveryRate = profile.getDouble("internal_state.energy.recovery_rate", 0.5);
        int exhaustionThreshold = profile.getInt("internal_state.energy.exhaustion_threshold", 10);

        // Energy costs for different activities
        double sprintingCost = profile.getDouble("internal_state.energy.costs.sprinting", 0.3);
        double huntingCost = profile.getDouble("internal_state.energy.costs.hunting", 0.5);
        double fleeingCost = profile.getDouble("internal_state.energy.costs.fleeing", 0.4);
        double flyingCost = profile.getDouble("internal_state.energy.costs.flying", 0.6);
        double swimmingCost = profile.getDouble("internal_state.energy.costs.swimming", 0.2);

        return new EnergyCache(enabled, maxValue, startingValue, recoveryRate, exhaustionThreshold,
                sprintingCost, huntingCost, fleeingCost, flyingCost, swimmingCost);
    }

    private int getCurrentEnergy(CompoundTag tag, EnergyCache cache) {
        if (!tag.contains(NBT_ENERGY)) {
            return cache.startingValue;
        }
        return tag.getInt(NBT_ENERGY);
    }

    private void setEnergy(CompoundTag tag, int value) {
        tag.putInt(NBT_ENERGY, value);
    }

    private double determineEnergyCost(EntityState state, EnergyCache cache) {
        // Priority order of activities
        if (state.isFleeing()) {
            return cache.fleeingCost;
        }
        if (state.isHunting()) {
            return cache.huntingCost;
        }
        if (state.isInWater()) {
            return cache.swimmingCost;
        }
        // Sprinting is determined by movement attribute being higher than base
        // For now, assume small base cost for general activity
        return 0.01;
    }

    public static boolean isExhausted(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("energy");
        return tag.getBoolean(NBT_IS_EXHAUSTED);
    }

    public static int getEnergyLevel(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("energy");
        if (!tag.contains(NBT_ENERGY)) {
            return 100;
        }
        return tag.getInt(NBT_ENERGY);
    }

    private record EnergyCache(
            boolean enabled,
            int maxValue,
            int startingValue,
            double recoveryRate,
            int exhaustionThreshold,
            double sprintingCost,
            double huntingCost,
            double fleeingCost,
            double flyingCost,
            double swimmingCost
    ) {}
}
