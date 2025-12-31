package me.javavirtualenv.ecology;

import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import me.javavirtualenv.ecology.state.SimulatedTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;

/**
 * Core hooks for integrating ecology system into entity lifecycle.
 *
 * ===============================================================================
 * OPTIMIZATION BOUNDARIES - What CAN and CANNOT be time-dilated
 * ===============================================================================
 *
 * REAL-TIME (cannot optimize, must update every tick near players):
 * - AI goals (pathfinding, targeting, fleeing, breeding)
 * - Movement/position (vanilla handles this)
 * - Interaction responses (player right-click, attacks)
 *
 * TIME-DILATED (can update infrequently, math-based):
 * - Hunger/thirst decay (simple subtraction)
 * - Age progression (tick counter)
 * - Breeding cooldowns (timestamp comparison)
 * - Condition tracking (slowly changing value)
 * - Social loneliness (slowly changing value)
 *
 * ===============================================================================
 * EDGE CASES AND SOLUTIONS
 * ===============================================================================
 *
 * 1. TELEPORTING DEATH:
 *    Problem: Animal at hunger=20, sleeps 1000 ticks, player approaches,
 *            we fast-forward to hunger=0, animal suddenly dies.
 *    Solution: Don't fast-forward critical thresholds. Only apply decay
 *             that keeps animal in "safe" state. If wake would cause death,
 *             clamp to safe minimum and mark for next update.
 *
 * 2. BREEDING WINDOWS:
 *    Problem: Animal ready to breed at tick 500, sleeping until tick 1000,
 *            misses breeding window.
 *    Solution: Breeding is AI-goal based, must be real-time near players.
 *             Distant animals can miss windows (acceptable - no one sees).
 *
 * 3. AGE TRANSITIONS:
 *    Problem: Baby should become adult at tick 24000, sleeping until 25000.
 *    Solution: Age is a tick counter, can fast-forward. At wake, if age > 24000,
 *             trigger adult transition immediately.
 *
 * 4. CHUNK LOADING:
 *    Problem: Chunk loads with sleeping entities, they're in unknown state.
 *    Solution: On chunk load, wake all entities once to initialize state.
 *
 * 5. MULTIPLE PLAYERS:
 *    Problem: Entity far from all players, then one approaches.
 *    Solution: Each entity tracks nearest player. When distance < 64, wake up.
 *
 * 6. STATE TRANSITIONS DURING SLEEP:
 *    Problem: Healthy → injured during sleep, decay rate changes.
 *    Solution: On wake, re-calculate everything with CURRENT modifiers.
 *             Since max sleep is 20 ticks, error is bounded.
 *
 * 7. SOCIAL CHANGES:
 *    Problem: Group member leaves/arrives during sleep.
 *    Solution: Social check is infrequent (20 ticks). Acceptable delay.
 *
 * ===============================================================================
 * SCHEDULING STRATEGY
 * ===============================================================================
 *
 * Player nearby (< 64 blocks):  Update every tick
 *   - All systems run normally
 *   - AI goals update every tick
 *   - No time dilation
 *
 * Distant (> 64 blocks):      Update every 20 ticks (staggered)
 *   - Hunger/thirst: fast-forward with CURRENT modifiers
 *   - Age: fast-forward tick counter
 *   - Breeding: check cooldown timestamp
 *   - Social: check with spatial index
 *   - AI goals: DON'T update (not relevant, no players nearby)
 *
 * ===============================================================================
 */
public final class EcologyHooks {

	private static final double ACTIVE_UPDATE_DISTANCE = 64.0;
	private static final int DISTANT_UPDATE_INTERVAL = 20; // 1 second
	private static final int MAX_SLEEP_TICKS = 40; // 2 seconds - bounds prediction error

	private EcologyHooks() {
	}

	public static void onRegisterGoals(Mob mob) {
		EcologyComponent component = component(mob);
		component.refreshIfNeeded();
		if (!component.hasProfile() && component.handles().isEmpty()) {
			return;
		}
		if (!component.markGoalsRegistered()) {
			return;
		}

		// Register mob in spatial index when goals are registered
		// This happens early in entity lifecycle (spawn or first tick)
		SpatialIndex.register(mob);

		EcologyProfile profile = component.profile();
		for (EcologyHandle handle : component.handles()) {
			handle.registerGoals(mob, component, profile);
		}
	}

	/**
	 * Main tick hook with bounded, consistent scheduling.
	 */
	public static void onTick(Mob mob) {
		EcologyComponent component = component(mob);
		component.refreshIfNeeded();
		if (!component.hasProfile() && component.handles().isEmpty()) {
			return;
		}

		// Robustness: Skip dead or removed entities
		if (mob.isRemoved() || !mob.isAlive()) {
			return;
		}

		// Update spatial index for this mob
		SpatialIndex.update(mob);

		// Determine update mode
		UpdateMode mode = determineUpdateMode(mob, component);

		if (mode == UpdateMode.SKIP) {
			return; // Sleeping this tick
		}

		// Calculate elapsed ticks
		CompoundTag timeTag = component.getHandleTag("time");
		long lastUpdate = SimulatedTime.getLastUpdateTick(timeTag);
		long elapsedTicks = lastUpdate < 0 ? 1 : mob.tickCount - lastUpdate;

		// Robustness: Guard against negative elapsed (NBT corruption, time edge cases)
		if (elapsedTicks < 0) {
			elapsedTicks = 1; // Reset to safe default
		}

		// Bound elapsed ticks for safety
		elapsedTicks = Math.min(elapsedTicks, MAX_SLEEP_TICKS);

		component.setElapsedTicks(elapsedTicks);
		component.setUpdateMode(mode);
		component.state().prepareForTick();

		EcologyProfile profile = component.profile();

		// Update all handlers
		for (EcologyHandle handle : component.handles()) {
			int interval = handle.tickInterval();
			if (mob.tickCount % interval != 0) {
				continue;
			}
			handle.tick(mob, component, profile);
		}

		// Mark as updated
		SimulatedTime.markUpdated(timeTag, mob.tickCount);
	}

	public static void onSave(Mob mob, CompoundTag tag) {
		EcologyComponent component = component(mob);
		component.refreshIfNeeded();
		if (!component.hasProfile() && component.handles().isEmpty()) {
			return;
		}
		EcologyProfile profile = component.profile();
		CompoundTag root = new CompoundTag();
		for (EcologyHandle handle : component.handles()) {
			String id = handle.id();
			CompoundTag handleTag = component.getHandleTag(id);
			handle.writeNbt(mob, component, profile, handleTag);
			if (!handleTag.isEmpty()) {
				root.put(id, handleTag);
			}
		}
		CompoundTag timeTag = component.getHandleTag("time");
		if (!timeTag.isEmpty()) {
			root.put("time", timeTag.copy());
		}
		if (!root.isEmpty()) {
			tag.put("BetterEcology", root);
		}
	}

	public static void onLoad(Mob mob, CompoundTag tag) {
		EcologyComponent component = component(mob);
		component.refreshIfNeeded();
		if (!component.hasProfile() && component.handles().isEmpty()) {
			return;
		}
		EcologyProfile profile = component.profile();
		if (!tag.contains("BetterEcology", Tag.TAG_COMPOUND)) {
			return;
		}
		CompoundTag root = tag.getCompound("BetterEcology");
		for (EcologyHandle handle : component.handles()) {
			String id = handle.id();
			CompoundTag handleTag = root.getCompound(id);
			component.setHandleTag(id, handleTag);
			handle.readNbt(mob, component, profile, handleTag);
		}
		if (root.contains("time", Tag.TAG_COMPOUND)) {
			component.setHandleTag("time", root.getCompound("time"));
		}

		// Chunk load edge case: Mark last update as current tick
		// This prevents large catch-up on newly loaded chunks
		CompoundTag timeTag = component.getHandleTag("time");
		SimulatedTime.markUpdated(timeTag, mob.tickCount);

		// Register mob in spatial index on load
		SpatialIndex.register(mob);
	}

	public static boolean overrideIsFood(Animal animal, ItemStack stack, boolean original) {
		EcologyComponent component = component(animal);
		component.refreshIfNeeded();
		if (!component.hasProfile() && component.handles().isEmpty()) {
			return original;
		}
		EcologyProfile profile = component.profile();
		boolean value = original;
		for (EcologyHandle handle : component.handles()) {
			value = handle.overrideIsFood(animal, component, profile, stack, value);
		}
		return value;
	}

	/**
	 * Determine how this entity should update this tick.
	 */
	private static UpdateMode determineUpdateMode(Mob mob, EcologyComponent component) {
		// Tamed animals and passengers: always update (interaction expected)
		if (mob instanceof TamableAnimal tamable && tamable.isTame()) {
			return UpdateMode.ACTIVE;
		}
		if (mob.isPassenger()) {
			return UpdateMode.ACTIVE;
		}

		// Client-side: always update
		if (mob.level().isClientSide) {
			return UpdateMode.ACTIVE;
		}

		// Check player proximity
		var nearestPlayer = mob.level().getNearestPlayer(mob, ACTIVE_UPDATE_DISTANCE);
		boolean playerNearby = nearestPlayer != null;

		if (playerNearby) {
			return UpdateMode.ACTIVE; // Player nearby - real-time
		}

		// Distant: use staggered pattern
		int stagger = (int) (mob.getId() % DISTANT_UPDATE_INTERVAL);
		boolean shouldTick = (mob.tickCount % DISTANT_UPDATE_INTERVAL) == stagger;

		return shouldTick ? UpdateMode.CATCH_UP : UpdateMode.SKIP;
	}

	/**
	 * Update mode for this tick.
	 */
	public enum UpdateMode {
		/** Skip all processing - entity is "asleep" */
		SKIP,
		/** Normal update - no catch-up needed */
		ACTIVE,
		/** Fast-forward state - entity waking from sleep */
		CATCH_UP
	}

	private static EcologyComponent component(Mob mob) {
		if (mob instanceof EcologyAccess access) {
			return access.betterEcology$getEcologyComponent();
		}
		return new EcologyComponent(mob);
	}

	/**
	 * Public accessor for the ecology component of a mob.
	 * This can be used by behaviors to access the ecology system.
	 *
	 * @param mob The mob to get the component for
	 * @return The EcologyComponent for the mob
	 */
	public static EcologyComponent getEcologyComponent(Mob mob) {
		return component(mob);
	}
}
