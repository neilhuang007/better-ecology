package me.javavirtualenv.ecology.handles;

import java.util.Map;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.PathType;

public final class MovementHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:movement-cache";

	@Override
	public String id() {
		return "movement";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		MovementCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		return cache != null;
	}

	@Override
	public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
		MovementCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		if (cache == null) {
			return;
		}

		applyMovementSpeed(mob, cache);
		configurePathfinding(mob, cache);
		registerStrollGoal(mob, cache);
		registerFloatGoal(mob);
	}

	private MovementCache buildCache(EcologyProfile profile) {
		java.util.Map<String, Object> capabilities = profile.getMap("physical.movement.capabilities");
		if (capabilities == null) {
			return null;
		}

		double walkSpeed = profile.getDouble("physical.movement.speeds.walk", 0.0);
		double runSpeed = profile.getDouble("physical.movement.speeds.run", walkSpeed);
		boolean avoidsCliffs = profile.getBool("physical.movement.capabilities.avoids_cliffs", false);
		double cliffThreshold = profile.getDouble("physical.movement.capabilities.cliff_threshold", 0.0);

		if (walkSpeed <= 0.0) {
			return null;
		}

		return new MovementCache(walkSpeed, runSpeed, avoidsCliffs, cliffThreshold);
	}

	private void applyMovementSpeed(Mob mob, MovementCache cache) {
		AttributeInstance movementAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementAttribute != null) {
			double baseValue = movementAttribute.getBaseValue();
			if (baseValue != cache.walkSpeed) {
				movementAttribute.setBaseValue(cache.walkSpeed);
			}
		}
	}

	private void configurePathfinding(Mob mob, MovementCache cache) {
		if (cache.avoidsCliffs) {
			mob.setPathfindingMalus(PathType.DANGER_OTHER, (float) cache.cliffThreshold);
		}
	}

	private void registerStrollGoal(Mob mob, MovementCache cache) {
		if (!(mob instanceof PathfinderMob pathfinderMob)) {
			return;
		}
		int goalPriority = 5;
		MobAccessor accessor = (MobAccessor) mob;
		if (cache.avoidsCliffs) {
			accessor.betterEcology$getGoalSelector().addGoal(goalPriority, new WaterAvoidingRandomStrollGoal(pathfinderMob, cache.runSpeed));
		} else {
			accessor.betterEcology$getGoalSelector().addGoal(goalPriority, new RandomStrollGoal(pathfinderMob, cache.runSpeed));
		}
	}

	private void registerFloatGoal(Mob mob) {
		int goalPriority = 0;
		MobAccessor accessor = (MobAccessor) mob;
		accessor.betterEcology$getGoalSelector().addGoal(goalPriority, new FloatGoal(mob));
	}

	private static final class MovementCache {
		private final double walkSpeed;
		private final double runSpeed;
		private final boolean avoidsCliffs;
		private final double cliffThreshold;

		private MovementCache(double walkSpeed, double runSpeed, boolean avoidsCliffs, double cliffThreshold) {
			this.walkSpeed = walkSpeed;
			this.runSpeed = runSpeed;
			this.avoidsCliffs = avoidsCliffs;
			this.cliffThreshold = cliffThreshold;
		}
	}
}
