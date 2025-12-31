package me.javavirtualenv.ecology.state;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Event-driven wake scheduler for ecology entities.
 *
 * Instead of ticking every N ticks, each state registers when it NEXT needs attention:
 * - Hunger: When will it starve? (current / decay_rate ticks from now)
 * - Breeding: When cooldown ends?
 * - Age: When baby becomes adult?
 *
 * The entity only wakes at the earliest registered wake time.
 *
 * Example:
 * - Hunger 18/20, decay 1/20sec: Will starve in 360 ticks → wake at tick_now + 360
 * - Breeding cooldown 200 ticks → wake at tick_now + 200
 * - Earliest wake = min(360, 200) = 200 ticks
 *
 * While sleeping: ZERO CPU usage
 * On wake: Calculate exact new state values, schedule next wake
 */
public final class ScheduledWake {

	// Wake event types
	public static final String WAKE_HUNGER = "hunger";
	public static final String WAKE_THIRST = "thirst";
	public static final String WAKE_BREEDING = "breeding";
	public static final String WAKE_AGE = "age";
	public static final String WAKE_SOCIAL = "social";

	// NBT keys
	private static final String NBT_NEXT_WAKE_TICK = "nextWakeTick";
	private static final String NBT_WAKE_REASON = "wakeReason";

	/**
	 * Calculate when the next wake is needed across all registered events.
	 * Returns the earliest wake tick, or Long.MAX_VALUE if no events scheduled.
	 */
	public static long getNextWakeTick(CompoundTag timeTag) {
		if (!timeTag.contains(NBT_NEXT_WAKE_TICK)) {
			return Long.MAX_VALUE;
		}
		return timeTag.getLong(NBT_NEXT_WAKE_TICK);
	}

	/**
	 * Set the next wake tick and reason.
	 */
	public static void scheduleWake(CompoundTag timeTag, long wakeTick, String reason) {
		timeTag.putLong(NBT_NEXT_WAKE_TICK, wakeTick);
		timeTag.putString(NBT_WAKE_REASON, reason);
	}

	/**
	 * Check if entity needs to wake up at current tick.
	 */
	public static boolean shouldWake(CompoundTag timeTag, long currentTick) {
		long nextWake = getNextWakeTick(timeTag);
		return nextWake <= currentTick;
	}

	/**
	 * Get the reason for the next scheduled wake.
	 */
	public static String getWakeReason(CompoundTag timeTag) {
		if (!timeTag.contains(NBT_WAKE_REASON)) {
			return "unknown";
		}
		return timeTag.getString(NBT_WAKE_REASON);
	}

	/**
	 * Clear the wake schedule (entity is going back to sleep).
	 */
	public static void clearSchedule(CompoundTag timeTag) {
		timeTag.remove(NBT_NEXT_WAKE_TICK);
		timeTag.remove(NBT_WAKE_REASON);
	}

	// ========== Event-specific scheduling helpers ==========

	/**
	 * Schedule wake for hunger event.
	 * @param currentHunger Current hunger value
	 * @param maxHunger Maximum hunger
	 * @param decayPerTick Hunger loss per tick
	 * @param threshold Wake when hunger reaches this level
	 * @param currentTick Current world tick
	 * @return Tick when wake is needed
	 */
	public static long scheduleHungerWake(CompoundTag timeTag, int currentHunger, int maxHunger,
										  double decayPerTick, int threshold, long currentTick) {
		if (decayPerTick <= 0) {
			return Long.MAX_VALUE; // Never decays, never needs wake
		}

		// Ticks until threshold reached
		double ticksUntilThreshold = (currentHunger - threshold) / decayPerTick;

		if (ticksUntilThreshold <= 0) {
			// Already at/below threshold - need immediate update
			return currentTick;
		}

		long wakeTick = currentTick + (long) Math.ceil(ticksUntilThreshold);
		return wakeTick;
	}

	/**
	 * Schedule wake for thirst event.
	 */
	public static long scheduleThirstWake(CompoundTag timeTag, int currentThirst, int maxThirst,
										  double decayPerTick, int threshold, long currentTick) {
		if (decayPerTick <= 0) {
			return Long.MAX_VALUE;
		}

		double ticksUntilThreshold = (currentThirst - threshold) / decayPerTick;

		if (ticksUntilThreshold <= 0) {
			return currentTick;
		}

		long wakeTick = currentTick + (long) Math.ceil(ticksUntilThreshold);
		return wakeTick;
	}

	/**
	 * Schedule wake for age milestone (baby → adult, adult → elderly).
	 * @param currentAge Current age in ticks
	 * @param milestoneAge Age at which to wake
	 * @param currentTick Current world tick
	 * @return Tick when wake is needed
	 */
	public static long scheduleAgeWake(long currentAge, long milestoneAge, long currentTick) {
		if (currentAge >= milestoneAge) {
			return currentTick; // Already past milestone
		}
		long ticksUntilMilestone = milestoneAge - currentAge;
		return currentTick + ticksUntilMilestone;
	}

	/**
	 * Schedule wake for breeding cooldown end.
	 * @param cooldownEndTick When breeding cooldown ends
	 * @param currentTick Current world tick
	 * @return Tick when wake is needed
	 */
	public static long scheduleBreedingWake(long cooldownEndTick, long currentTick) {
		return Math.max(currentTick, cooldownEndTick);
	}

	/**
	 * Main scheduling function - calculates earliest wake time across all states.
	 * Called after each update to schedule the next sleep period.
	 *
	 * @param timeTag Time tracking tag
	 * @param component Entity component
	 * @param currentTick Current world tick
	 */
	public static void scheduleNextWake(CompoundTag timeTag, EcologyComponent component, long currentTick) {
		long earliestWake = Long.MAX_VALUE;
		String earliestReason = "none";

		// Check hunger wake time
		long hungerWake = calculateHungerWake(component, currentTick);
		if (hungerWake < earliestWake) {
			earliestWake = hungerWake;
			earliestReason = WAKE_HUNGER;
		}

		// Check thirst wake time
		long thirstWake = calculateThirstWake(component, currentTick);
		if (thirstWake < earliestWake) {
			earliestWake = thirstWake;
			earliestReason = WAKE_THIRST;
		}

		// Check age wake time
		long ageWake = calculateAgeWake(component, currentTick);
		if (ageWake < earliestWake) {
			earliestWake = ageWake;
			earliestReason = WAKE_AGE;
		}

		// Check breeding wake time
		long breedingWake = calculateBreedingWake(component, currentTick);
		if (breedingWake < earliestWake) {
			earliestWake = breedingWake;
			earliestReason = WAKE_BREEDING;
		}

		// Also wake if player is nearby (checked externally)
		// This just sets the schedule for autonomous events

		if (earliestWake != Long.MAX_VALUE) {
			scheduleWake(timeTag, earliestWake, earliestReason);
		}
	}

	private static long calculateHungerWake(EcologyComponent component, long currentTick) {
		CompoundTag hungerTag = component.getHandleTag("hunger");
		if (!hungerTag.contains("hunger")) {
			return currentTick + 1200; // Default: check in 60 seconds if unknown
		}
		int hunger = hungerTag.getInt("hunger");
		// Decay rate is typically 1 per 20 ticks, schedule wake when at 5 (starvation threshold)
		// This would need the cache, but for simplicity use a conservative estimate
		if (hunger <= 10) {
			return currentTick + 200; // Soon - check frequently
		}
		// Approximate: 1 hunger per second, wake when at 10
		return currentTick + (hunger - 10) * 20L;
	}

	private static long calculateThirstWake(EcologyComponent component, long currentTick) {
		CompoundTag thirstTag = component.getHandleTag("thirst");
		if (!thirstTag.contains("thirst")) {
			return Long.MAX_VALUE; // No thirst system
		}
		int thirst = thirstTag.getInt("thirst");
		if (thirst <= 20) {
			return currentTick + 100;
		}
		return currentTick + (thirst - 20) * 5L;
	}

	private static long calculateAgeWake(EcologyComponent component, long currentTick) {
		CompoundTag ageTag = component.getHandleTag("age");
		if (!ageTag.contains("ageTicks")) {
			return Long.MAX_VALUE;
		}
		long age = ageTag.getLong("ageTicks");
		// Wake at adulthood (24000 ticks) or elderly age
		long adulthood = 24000;
		if (age < adulthood) {
			return currentTick + (adulthood - age);
		}
		return Long.MAX_VALUE; // Adult, no age milestone
	}

	private static long calculateBreedingWake(EcologyComponent component, long currentTick) {
		CompoundTag breedingTag = component.getHandleTag("breeding");
		if (!breedingTag.contains("cooldownEnd")) {
			return Long.MAX_VALUE;
		}
		return breedingTag.getLong("cooldownEnd");
	}
}
