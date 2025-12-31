package me.javavirtualenv.ecology;

import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.state.SimulatedTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Consistent tick scheduler that maintains simulation accuracy.
 *
 * Key insight: We can't predict the future because state changes dynamically.
 * But we CAN:
 * 1. Skip ticks when NOTHING interesting can happen (no players nearby, state is stable)
 * 2. Wake up when player approaches (interaction needed)
 * 3. Wake up at MAX_SLEEP intervals to catch unexpected state changes
 *
 * This ensures consistency because:
 * - Player-interactive entities always tick normally
 * - Distant entities tick at least every 60 seconds
 * - On wake, we fast-forward with the CURRENT state's decay rate
 */
public final class EcologyScheduler {

	// Distance threshold for active updates
	private static final double ACTIVE_UPDATE_DISTANCE = 64.0;

	// Maximum sleep time - guarantees consistency check
	private static final int MAX_SLEEP_TICKS = 1200; // 60 seconds

	// Minimum ticks between full updates for distant entities
	// This bounds how "wrong" our prediction can be
	private static final int CONSISTENCY_TICKS = 600; // 30 seconds

	private EcologyScheduler() {
	}

	/**
	 * Check if entity should update this tick.
	 *
	 * Consistency guarantee: An entity will always update at least once every
	 * CONSISTENCY_TICKS, so even if our predictions are wrong, we correct them quickly.
	 */
	public static boolean shouldUpdate(Mob mob) {
		// Tamed animals and passengers always update (player interaction)
		if (mob instanceof TamableAnimal tamable && tamable.isTame()) {
			return true;
		}
		if (mob.isPassenger()) {
			return true;
		}

		// Client-side always updates
		if (mob.level().isClientSide) {
			return true;
		}

		// Get ecology component
		var component = getComponent(mob);
		if (component == null || !component.hasProfile()) {
			return false;
		}

		CompoundTag timeTag = component.getHandleTag("time");
		long currentTick = mob.tickCount;

		// First update ever
		long lastUpdate = SimulatedTime.getLastUpdateTick(timeTag);
		if (lastUpdate < 0) {
			return true;
		}

		// Check if max sleep time reached (consistency guarantee)
		long elapsed = currentTick - lastUpdate;
		if (elapsed >= MAX_SLEEP_TICKS) {
			return true;
		}

		// Check if player is nearby (interaction needed)
		Player nearestPlayer = mob.level().getNearestPlayer(mob, ACTIVE_UPDATE_DISTANCE);
		if (nearestPlayer != null) {
			return true;
		}

		// Staggered updates: tick every 20 ticks (1/sec) for distant entities
		// This bounds the maximum "error" in our predictions
		return (currentTick % 20) == (mob.getId() % 20);
	}

	/**
	 * Calculate elapsed ticks since last update.
	 * Used for catch-up simulation when waking from sleep.
	 */
	public static long calculateElapsedTicks(Mob mob) {
		var component = getComponent(mob);
		if (component == null) {
			return 1;
		}

		CompoundTag timeTag = component.getHandleTag("time");
		long lastUpdate = SimulatedTime.getLastUpdateTick(timeTag);
		if (lastUpdate < 0) {
			return 1;
		}

		long elapsed = mob.tickCount - lastUpdate;
		// Bound elapsed to prevent integer overflow issues
		return Math.min(elapsed, MAX_SLEEP_TICKS);
	}

	/**
	 * Mark entity as updated.
	 */
	public static void markUpdated(Mob mob) {
		var component = getComponent(mob);
		if (component == null) {
			return;
		}

		CompoundTag timeTag = component.getHandleTag("time");
		SimulatedTime.markUpdated(timeTag, mob.tickCount);
	}

	private static me.javavirtualenv.ecology.EcologyComponent getComponent(Mob mob) {
		if (mob instanceof EcologyAccess access) {
			return access.betterEcology$getEcologyComponent();
		}
		return null;
	}
}
