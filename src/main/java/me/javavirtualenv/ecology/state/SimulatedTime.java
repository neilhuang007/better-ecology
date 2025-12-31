package me.javavirtualenv.ecology.state;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.nbt.CompoundTag;

/**
 * Predictive simulation tracker.
 * Stores the last time an entity was updated and allows fast-forwarding
 * state changes to catch up to current time.
 *
 * This enables "sleep mode" for distant entities - they consume ZERO CPU
 * until a player approaches or their state is needed.
 */
public final class SimulatedTime {
	private static final String NBT_LAST_UPDATE_TICK = "lastUpdateTick";
	private static final String NBT_SIMULATED_TICKS = "simulatedTicks";

	/**
	 * Get the last tick when this entity's ecology state was updated.
	 */
	public static long getLastUpdateTick(CompoundTag tag) {
		if (!tag.contains(NBT_LAST_UPDATE_TICK)) {
			return -1; // Never updated
		}
		return tag.getLong(NBT_LAST_UPDATE_TICK);
	}

	/**
	 * Set the last update tick timestamp.
	 */
	public static void setLastUpdateTick(CompoundTag tag, long tick) {
		tag.putLong(NBT_LAST_UPDATE_TICK, tick);
	}

	/**
	 * Get the total number of simulated ticks (for debugging/stats).
	 */
	public static long getSimulatedTicks(CompoundTag tag) {
		if (!tag.contains(NBT_SIMULATED_TICKS)) {
			return 0;
		}
		return tag.getLong(NBT_SIMULATED_TICKS);
	}

	/**
	 * Add to the total simulated ticks counter.
	 */
	public static void addSimulatedTicks(CompoundTag tag, long ticks) {
		long current = getSimulatedTicks(tag);
		tag.putLong(NBT_SIMULATED_TICKS, current + ticks);
	}

	/**
	 * Calculate how many ticks have passed since last update.
	 * Returns 0 if this is the first update.
	 */
	public static long getElapsedTicks(CompoundTag tag, long currentTick) {
		long lastUpdate = getLastUpdateTick(tag);
		if (lastUpdate < 0) {
			return 0; // First update
		}
		return Math.max(0, currentTick - lastUpdate);
	}

	/**
	 * Check if an entity needs an update based on player proximity.
	 * Returns false if entity is "asleep" (no players nearby).
	 */
	public static boolean needsUpdate(EcologyComponent component, long currentTick, long sleepThreshold) {
		CompoundTag tag = component.getHandleTag("time");
		long lastUpdate = getLastUpdateTick(tag);
		long elapsed = lastUpdate < 0 ? 0 : currentTick - lastUpdate;

		// Always update if enough time has passed (minimum update frequency)
		if (elapsed >= sleepThreshold) {
			return true;
		}

		// Check if player is nearby
		return isPlayerNearby(component, 64.0);
	}

	/**
	 * Fast-forward state changes to catch up to current time.
	 * This is called when waking up a "sleeping" entity.
	 *
	 * @param tag The time tracking tag
	 * @param currentTick Current world tick
	 * @param decayRatePerTick Decay rate per tick
	 * @param currentValue Current state value
	 * @param minValue Minimum allowed value
	 * @return The new value after catch-up
	 */
	public static int catchUpInt(CompoundTag tag, long currentTick, double decayRatePerTick, int currentValue, int minValue) {
		long elapsed = getElapsedTicks(tag, currentTick);
		if (elapsed <= 0) {
			return currentValue;
		}

		// Calculate total decay over all elapsed ticks
		double totalDecay = decayRatePerTick * elapsed;
		int newValue = Math.max(minValue, (int) Math.floor(currentValue - totalDecay));

		// Track stats
		addSimulatedTicks(tag, elapsed);

		// Update timestamp
		setLastUpdateTick(tag, currentTick);

		return newValue;
	}

	/**
	 * Fast-forward a floating-point state value.
	 */
	public static double catchUpDouble(CompoundTag tag, long currentTick, double decayRatePerTick, double currentValue, double minValue) {
		long elapsed = getElapsedTicks(tag, currentTick);
		if (elapsed <= 0) {
			return currentValue;
		}

		double totalDecay = decayRatePerTick * elapsed;
		double newValue = Math.max(minValue, currentValue - totalDecay);

		addSimulatedTicks(tag, elapsed);
		setLastUpdateTick(tag, currentTick);

		return newValue;
	}

	/**
	 * Mark an entity as just updated.
	 */
	public static void markUpdated(CompoundTag tag, long currentTick) {
		setLastUpdateTick(tag, currentTick);
	}

	/**
	 * Check if any player is within the given radius.
	 * Used to determine if entity needs active updates.
	 */
	private static boolean isPlayerNearby(EcologyComponent component, double radius) {
		var mob = component.state().mob();
		var nearestPlayer = mob.level().getNearestPlayer(mob, radius);
		return nearestPlayer != null;
	}
}
