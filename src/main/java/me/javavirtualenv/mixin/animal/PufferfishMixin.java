package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Pufferfish.
 * Pufferfish are defensive fish that flee from axolotls, their primary predator.
 * Note: Pufferfish don't have baby/adult distinction in vanilla.
 */
@Mixin(Pufferfish.class)
public abstract class PufferfishMixin {

    /**
     * Register ecology goals after the pufferfish's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Pufferfish pufferfish = (Pufferfish) (Object) this;
        var goalSelector = ((MobAccessor) pufferfish).getGoalSelector();

        // Priority 1: Flee from axolotls (primary predator of pufferfish)
        // Pufferfish are defensive but will flee from axolotls
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                pufferfish,
                1.4,  // speed multiplier when fleeing
                10,   // detection range
                16,   // flee distance
                Axolotl.class
            )
        );
    }
}
