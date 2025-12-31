package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;

public final class HungerHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:hunger-cache";
	private static final String NBT_HUNGER = "hunger";
	private static final String NBT_LAST_DAMAGE_TICK = "lastDamageTick";

	@Override
	public String id() {
		return "hunger";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		HungerCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		return cache != null && cache.enabled();
	}

	@Override
	public int tickInterval() {
		return 20; // Update hunger once per second
	}

	@Override
	public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
		HungerCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		if (cache == null) {
			return;
		}

		CompoundTag handleTag = component.getHandleTag(id());
		int currentHunger = getCurrentHunger(handleTag, cache);

		// Apply decay scaled by elapsed ticks (catch-up simulation)
		// If 100 ticks have passed since last update, decay 100x
		long elapsedTicks = component.elapsedTicks();
		long effectiveTicks = Math.max(1, elapsedTicks); // At least 1 tick of decay
		long scaledDecay = cache.decayRate() * effectiveTicks;
		int newHunger = (int) (currentHunger - scaledDecay);

		// Prevent jarring death: clamp to safe minimum during catch-up
		// During active updates, allow hunger to reach 0 and trigger damage
		if (elapsedTicks > 1) {
			// Catch-up: keep hunger above damage threshold
			// Entity won't die suddenly when player approaches
			int safeMinimum = cache.damageThreshold() + 1;
			newHunger = Math.max(safeMinimum, newHunger);
		} else {
			// Active update: allow normal decay to 0
			newHunger = Math.max(0, newHunger);
		}
		setHunger(handleTag, newHunger);

		// Check for starvation damage (only during active updates, not catch-up)
		if (elapsedTicks <= 1 && shouldApplyStarvation(mob, newHunger, cache)) {
			int currentTick = mob.tickCount;
			int lastDamageTick = getLastDamageTick(handleTag);
			int ticksSinceDamage = currentTick - lastDamageTick;

			if (ticksSinceDamage >= cache.damageInterval()) {
				applyStarvationDamage(mob, cache);
				setLastDamageTick(handleTag, currentTick);
			}
		}
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

	private HungerCache buildCache(EcologyProfile profile) {
		boolean enabled = profile.getBoolFast("hunger", "enabled", false);

		if (!enabled) {
			return null;
		}

		int maxValue = profile.getIntFast("hunger", "max_value", 20);
		int startingValue = profile.getIntFast("hunger", "starting_value", maxValue);
		int decayRate = profile.getIntFast("hunger", "decay_rate", 1);
		int damageThreshold = profile.getIntFast("hunger", "damage_threshold", 0);
		float damageAmount = (float) profile.getDoubleFast("hunger", "damage_amount", 1.0);
		int damageInterval = profile.getIntFast("hunger", "damage_interval", 20);

		return new HungerCache(enabled, maxValue, startingValue, decayRate, damageThreshold, damageAmount, damageInterval);
	}

	private int getCurrentHunger(CompoundTag handleTag, HungerCache cache) {
		if (!handleTag.contains(NBT_HUNGER)) {
			return cache.startingValue();
		}
		return handleTag.getInt(NBT_HUNGER);
	}

	private void setHunger(CompoundTag handleTag, int value) {
		handleTag.putInt(NBT_HUNGER, value);
	}

	private int getLastDamageTick(CompoundTag handleTag) {
		return handleTag.getInt(NBT_LAST_DAMAGE_TICK);
	}

	private void setLastDamageTick(CompoundTag handleTag, int tick) {
		handleTag.putInt(NBT_LAST_DAMAGE_TICK, tick);
	}

	private boolean shouldApplyStarvation(Mob mob, int hunger, HungerCache cache) {
		if (mob.level().getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		return hunger <= cache.damageThreshold();
	}

	private void applyStarvationDamage(Mob mob, HungerCache cache) {
		mob.hurt(mob.level().damageSources().starve(), cache.damageAmount());
	}

	/**
	 * Restores hunger for an entity when it consumes food.
	 * This method is called by foraging behaviors when animals eat.
	 *
	 * @param mob The mob to restore hunger for
	 * @param amount The amount of hunger to restore
	 */
	public static void restoreHunger(Mob mob, int amount) {
		if (mob == null || amount <= 0) {
			return;
		}

		// Get EcologyComponent from the mob
		EcologyComponent component;
		if (mob instanceof EcologyAccess access) {
			component = access.betterEcology$getEcologyComponent();
		} else {
			return;
		}

		// Get hunger handle data
		CompoundTag handleTag = component.getHandleTag("hunger");

		// Get current hunger
		int currentHunger = handleTag.getInt(NBT_HUNGER);

		// Get max value from cache
		HungerCache cache = null;
		if (component.profile() != null) {
			cache = component.profile().cached(CACHE_KEY, () -> {
				// Build cache inline since we're in static context
				boolean enabled = component.profile().getBoolFast("hunger", "enabled", false);
				if (!enabled) {
					return null;
				}
				int maxValue = component.profile().getIntFast("hunger", "max_value", 20);
				int startingValue = component.profile().getIntFast("hunger", "starting_value", maxValue);
				int decayRate = component.profile().getIntFast("hunger", "decay_rate", 1);
				int damageThreshold = component.profile().getIntFast("hunger", "damage_threshold", 0);
				float damageAmount = (float) component.profile().getDoubleFast("hunger", "damage_amount", 1.0);
				int damageInterval = component.profile().getIntFast("hunger", "damage_interval", 20);
				return new HungerCache(enabled, maxValue, startingValue, decayRate, damageThreshold, damageAmount, damageInterval);
			});
		}

		if (cache == null) {
			return;
		}

		// Calculate new hunger value, clamped to max
		int newHunger = Math.min(cache.maxValue(), currentHunger + amount);

		// Update hunger value
		handleTag.putInt(NBT_HUNGER, newHunger);
	}

	private static final class HungerCache {
		private final boolean enabled;
		private final int maxValue;
		private final int startingValue;
		private final int decayRate;
		private final int damageThreshold;
		private final float damageAmount;
		private final int damageInterval;

		private HungerCache(boolean enabled, int maxValue, int startingValue, int decayRate, int damageThreshold, float damageAmount, int damageInterval) {
			this.enabled = enabled;
			this.maxValue = maxValue;
			this.startingValue = startingValue;
			this.decayRate = decayRate;
			this.damageThreshold = damageThreshold;
			this.damageAmount = damageAmount;
			this.damageInterval = damageInterval;
		}

		private boolean enabled() {
			return enabled;
		}

		private int maxValue() {
			return maxValue;
		}

		private int startingValue() {
			return startingValue;
		}

		private int decayRate() {
			return decayRate;
		}

		private int damageThreshold() {
			return damageThreshold;
		}

		private float damageAmount() {
			return damageAmount;
		}

		private int damageInterval() {
			return damageInterval;
		}
	}
}
