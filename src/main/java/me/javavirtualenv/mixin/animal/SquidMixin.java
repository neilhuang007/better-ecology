package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Dolphin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Squid.
 * Squids are aquatic prey animals that school together and flee from predators.
 * They exhibit schooling behavior and flee from dolphins and axolotls.
 */
@Mixin(Squid.class)
public abstract class SquidMixin {

    /**
     * Register ecology goals after the squid's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Squid squid = (Squid) (Object) this;
        var goalSelector = ((MobAccessor) squid).getGoalSelector();

        // Priority 1: Flee from predators (dolphins, axolotls)
        // Squids are prey for these aquatic hunters
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                squid,
                1.5,  // speed multiplier when fleeing
                10,   // detection range
                16,   // flee distance
                Dolphin.class,
                Axolotl.class
            )
        );

        // Priority 5: Schooling behavior (herd cohesion)
        // Squids naturally school together for protection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(squid, Squid.class)
        );
    }
}
