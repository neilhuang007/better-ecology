package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.AgeHandle;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to set random age for entities spawned from spawn eggs.
 * This ensures spawn eggs produce animals at various life stages (0-100% adult),
 * rather than always spawning babies as vanilla does.
 */
@Mixin(targets = "net.minecraft.world.item.SpawnEggItem")
public class SpawnEggMixin {

    /**
     * Injects into spawnBaby method to set random age for egg-spawned entities.
     * This only affects entities spawned from spawn eggs, not natural spawns or breeding.
     */
    @Inject(
            method = "spawnBaby",
            at = @At("RETURN")
    )
    private void betterEcology$setRandomAgeForEggSpawn(
            CallbackInfoReturnable<java.util.Optional<Mob>> cir
    ) {
        java.util.Optional<Mob> result = cir.getReturnValue();
        if (result.isEmpty()) {
            return;
        }

        Mob mob = result.get();
        if (!(mob instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        // Only apply to AgeableMob entities (animals that have age)
        if (!(mob instanceof AgeableMob ageable)) {
            return;
        }

        // Set random age (0-100% of maturity age) and sync vanilla age state
        int randomAge = AgeHandle.setRandomAgeForEggSpawn(component, mob.getRandom());
        int babyDuration = 24000;
        if (component.profile() != null) {
            babyDuration = component.profile().getIntFast("age", "baby_duration", 24000);
        }
        if (randomAge < babyDuration) {
            ageable.setBaby(true);
        } else {
            ageable.setAge(0);
        }
    }
}
