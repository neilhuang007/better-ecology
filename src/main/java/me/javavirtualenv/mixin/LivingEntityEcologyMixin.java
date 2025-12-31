package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityEcologyMixin {

	@Inject(
		method = "getDefaultDimensions",
		at = @At("TAIL"),
		cancellable = true
	)
	private void betterEcology$applyCustomDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;

		if (!(entity instanceof EcologyAccess ecologyAccess)) {
			return;
		}

		EcologyComponent component = ecologyAccess.betterEcology$getEcologyComponent();
		CompoundTag sizeTag = component.getHandleTag("size");

		if (!sizeTag.contains("width") || !sizeTag.contains("height")) {
			return;
		}

		float customWidth = sizeTag.getFloat("width");
		float customHeight = sizeTag.getFloat("height");

		EntityDimensions customDimensions = EntityDimensions.scalable(customWidth, customHeight);
		cir.setReturnValue(customDimensions);
	}
}
