package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle entity removal from the spatial index.
 * Hooks into Entity.setRemoved() to clean up spatial index entries.
 */
@Mixin(Entity.class)
public class EntityRemovalMixin {

	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void betterEcology$onRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (entity instanceof Mob mob) {
			SpatialIndex.unregister(mob);
		}
	}
}
