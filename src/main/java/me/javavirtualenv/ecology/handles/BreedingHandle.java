package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.EcologyBreedGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

public final class BreedingHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:breeding-cache";

	@Override
	public String id() {
		return "breeding";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		return profile.getBool("reproduction.enabled", false);
	}

	@Override
	public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
		if (!(mob instanceof Animal animal)) {
			return;
		}
		BreedingConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
		int priority = profile.getInt("ai_priority_framework.reproduction.breed", 8);
		MobAccessor accessor = (MobAccessor) mob;
		accessor.betterEcology$getGoalSelector().addGoal(priority,
			new EcologyBreedGoal(animal, config.moveSpeed(),
				config.minAge(), config.minHealth(), (int) config.minCondition(), config.cooldown()));
	}

	private BreedingConfig buildConfig(EcologyProfile profile) {
		int minAge = profile.getInt("reproduction.requirements.min_age", 0);
		double minHealth = profile.getDouble("reproduction.requirements.min_health", 0.0);
		double minCondition = profile.getDouble("reproduction.requirements.min_condition", 0.0);
		int cooldown = profile.getInt("reproduction.breeding.cooldown", 6000);
		double moveSpeed = profile.getDouble("reproduction.breeding.move_speed", 1.0);
		return new BreedingConfig(minAge, minHealth, minCondition, cooldown, moveSpeed);
	}

	private record BreedingConfig(
		int minAge,
		double minHealth,
		double minCondition,
		int cooldown,
		double moveSpeed
	) {}
}
