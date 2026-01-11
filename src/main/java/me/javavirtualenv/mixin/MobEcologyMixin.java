package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEcologyMixin implements EcologyAccess {
	private static final Logger LOGGER = LoggerFactory.getLogger("BetterEcology/MobEcology");

	// Static initializer to verify the mixin is loaded
	static {
		System.out.println("[BetterEcology/MobEcologyMixin] Mixin class loaded!");
		LOGGER.info("MobEcologyMixin class loaded");
	}

	@Unique
	private EcologyComponent betterEcology$component;

	@Override
	public EcologyComponent betterEcology$getEcologyComponent() {
		System.out.println("[BetterEcology/MobEcologyMixin] betterEcology$getEcologyComponent called!");
		if (betterEcology$component == null) {
			betterEcology$component = new EcologyComponent((Mob) (Object) this);
		}
		return betterEcology$component;
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void betterEcology$registerGoals(CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;

		System.out.println("[BetterEcology/MobEcologyMixin] registerGoals called for " + mob.getType());
		LOGGER.info("MobEcologyMixin.registerGoals called for {}", mob.getType());

		// Ensure wolf behaviors are registered before processing goals
		// This is needed because WolfMixin.onInit() is called AFTER registerGoals()
		AnimalConfig config = AnimalBehaviorRegistry.getForMob(mob);
		if (config != null && !config.getHandles().isEmpty()) {
			LOGGER.info("Found code-based config for {} with {} handles",
				mob.getType(), config.getHandles().size());
		}

		EcologyHooks.onRegisterGoals(mob);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void betterEcology$onTick(CallbackInfo ci) {
		EcologyHooks.onTick((Mob) (Object) this);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void betterEcology$save(CompoundTag tag, CallbackInfo ci) {
		EcologyHooks.onSave((Mob) (Object) this, tag);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void betterEcology$load(CompoundTag tag, CallbackInfo ci) {
		EcologyHooks.onLoad((Mob) (Object) this, tag);
	}
}
