package me.javavirtualenv.ecology.handles;

import java.util.Map;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

public final class SizeHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:size-cache";
	private static final String HANDLE_TAG_KEY = "size";

	@Override
	public String id() {
		return "size";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		SizeConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
		return config != null;
	}

	@Override
	public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
		SizeConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
		if (config == null) {
			return;
		}

		applySize(mob, component, config);
	}

	private SizeConfig buildConfig(EcologyProfile profile) {
		Map<String, Object> sizeMap = profile.getMap("physical.size");
		if (sizeMap == null || sizeMap.isEmpty()) {
			return null;
		}

		double width = profile.getDouble("physical.size.width", 0.0);
		double height = profile.getDouble("physical.size.height", 0.0);
		double babyScale = profile.getDouble("physical.size.baby_scale", 0.5);

		if (width <= 0.0 || height <= 0.0) {
			return null;
		}

		return new SizeConfig((float) width, (float) height, (float) babyScale);
	}

	private void applySize(Mob mob, EcologyComponent component, SizeConfig config) {
		float finalWidth = config.width;
		float finalHeight = config.height;

		if (mob.isBaby()) {
			finalWidth *= config.babyScale;
			finalHeight *= config.babyScale;
		}

		// Store the size data in the component for use by mixins
		// A mixin should override LivingEntity.getDefaultDimensions() to check
		// if the entity has an EcologyComponent with size data and return those dimensions
		CompoundTag sizeTag = component.getHandleTag(HANDLE_TAG_KEY);
		sizeTag.putFloat("width", finalWidth);
		sizeTag.putFloat("height", finalHeight);

		// Trigger dimension refresh
		// This will cause getDefaultDimensions() to be called again
		mob.refreshDimensions();
	}

	private static final class SizeConfig {
		private final float width;
		private final float height;
		private final float babyScale;

		private SizeConfig(float width, float height, float babyScale) {
			this.width = width;
			this.height = height;
			this.babyScale = babyScale;
		}
	}
}
