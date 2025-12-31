package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class HealthHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:health-cache";

	@Override
	public String id() {
		return "health";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		HealthCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		return cache != null;
	}

	@Override
	public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
		HealthCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		if (cache == null) {
			return;
		}

		AttributeInstance healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
		if (healthAttribute != null) {
			double healthValue = cache.baseHealth();
			if (mob.isBaby()) {
				healthValue *= cache.babyMultiplier();
			}
			healthAttribute.setBaseValue(healthValue);
		}
	}

	private HealthCache buildCache(EcologyProfile profile) {
		double baseHealth = profile.getDouble("physical.health.base", 0.0);
		double babyMultiplier = profile.getDouble("physical.health.baby_multiplier", 1.0);

		if (baseHealth <= 0.0) {
			return null;
		}

		return new HealthCache(baseHealth, babyMultiplier);
	}

	private static final class HealthCache {
		private final double baseHealth;
		private final double babyMultiplier;

		private HealthCache(double baseHealth, double babyMultiplier) {
			this.baseHealth = baseHealth;
			this.babyMultiplier = babyMultiplier;
		}

		private double baseHealth() {
			return baseHealth;
		}

		private double babyMultiplier() {
			return babyMultiplier;
		}
	}
}
