package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.HuntPreyGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Salmon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Dolphin.
 * Dolphins are intelligent aquatic mammals that:
 * - Form pods (social groups)
 * - Hunt fish cooperatively
 * - Display playful behavior with players (vanilla)
 */
@Mixin(Dolphin.class)
public abstract class DolphinMixin {

    /**
     * Register ecology goals after the dolphin's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Dolphin dolphin = (Dolphin) (Object) this;
        var goalSelector = ((MobAccessor) dolphin).getGoalSelector();

        // Priority 4: Hunt fish when hungry
        // Dolphins are active predators that hunt cod and salmon
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new HuntPreyGoal(
                dolphin,
                1.4,  // speed when hunting (dolphins are fast swimmers)
                24,   // hunt range (dolphins can spot prey from distance)
                Cod.class,
                Salmon.class
            )
        );

        // Priority 5: Pod behavior (stay with other dolphins)
        // Dolphins naturally form social pods
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(
                dolphin,
                Dolphin.class,
                1.2,  // speed modifier (dolphins swim quickly to stay together)
                20,   // cohesion radius
                2,    // minimum pod size
                12.0  // max distance from pod
            )
        );
    }
}
