package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEcologyMixin implements EcologyAccess {
	@Unique
	private EcologyComponent betterEcology$component;

	@Override
	public EcologyComponent betterEcology$getEcologyComponent() {
		if (betterEcology$component == null) {
			betterEcology$component = new EcologyComponent((Mob) (Object) this);
		}
		return betterEcology$component;
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void betterEcology$registerGoals(CallbackInfo ci) {
		EcologyHooks.onRegisterGoals((Mob) (Object) this);
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
