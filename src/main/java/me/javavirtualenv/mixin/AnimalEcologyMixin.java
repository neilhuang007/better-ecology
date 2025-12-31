package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.EcologyHooks;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Animal.class)
public abstract class AnimalEcologyMixin {
	@Redirect(
			method = "mobInteract",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Animal;isFood(Lnet/minecraft/world/item/ItemStack;)Z")
	)
	private boolean betterEcology$redirectIsFood(Animal instance, ItemStack stack) {
		boolean original = instance.isFood(stack);
		return EcologyHooks.overrideIsFood(instance, stack, original);
	}
}
