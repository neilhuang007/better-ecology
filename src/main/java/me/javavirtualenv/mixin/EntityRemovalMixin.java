package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.conservation.PopulationRegistry;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle entity removal from ecology systems.
 * Hooks into Entity.setRemoved() to clean up:
 * - Spatial index entries (for all mobs)
 * - Population registry (for deaths and discards)
 * - Predation hunger restoration (for predators that kill prey)
 */
@Mixin(Entity.class)
public class EntityRemovalMixin {

	private static final int PREDATOR_HUNGER_RESTORE = 20;
	private static final double BABY_PREY_MULTIPLIER = 0.25;

	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void betterEcology$onRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (!(entity instanceof Mob mob)) {
			return;
		}

		// Always clean up spatial index, regardless of removal reason
		SpatialIndex.unregister(mob);

		// Only track actual deaths for population statistics
		if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
			PopulationRegistry.onEntityDespawned(mob);
			// Handle predation hunger restoration
			handlePredationHungerRestore(mob);
		}
	}

	/**
	 * Restores hunger to the predator when they kill prey.
	 * Baby prey provide 25% of the hunger restoration compared to adults,
	 * even if no meat drops.
	 *
	 * @param victim The mob that was killed
	 */
	private void handlePredationHungerRestore(Mob victim) {
		LivingEntity killer = victim.getLastHurtByMob();

		if (killer == null || !(killer instanceof Mob predator)) {
			return;
		}

		// Check if predator has the hunger system
		EcologyComponent predatorComponent = getEcologyComponent(predator);
		if (predatorComponent == null) {
			return;
		}

		EcologyProfile profile = predatorComponent.profile();
		if (profile == null || !profile.getBoolFast("predation", "as_predator.enabled", false)) {
			return;
		}

		// Calculate hunger restoration amount
		int hungerRestore = PREDATOR_HUNGER_RESTORE;

		// Check if victim is a baby and apply 25% multiplier
		if (victim instanceof Animal animal && animal.isBaby()) {
			hungerRestore = (int) Math.ceil(hungerRestore * BABY_PREY_MULTIPLIER);
		}

		// Restore hunger to predator
		HungerHandle.restoreHunger(predator, hungerRestore);
	}

	/**
	 * Gets the EcologyComponent from a mob if it has one.
	 */
	private EcologyComponent getEcologyComponent(Mob mob) {
		if (mob instanceof EcologyAccess access) {
			return access.betterEcology$getEcologyComponent();
		}
		return EcologyHooks.getEcologyComponent(mob);
	}
}
