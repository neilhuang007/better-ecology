package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Salmon.
 * Salmon are schooling fish that flee from aquatic predators.
 * They exhibit group cohesion behavior and rapid escape responses.
 */
@Mixin(Salmon.class)
public abstract class SalmonMixin {

    /**
     * Register ecology goals after the salmon is constructed.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Salmon salmon = (Salmon) (Object) this;
        var goalSelector = ((MobAccessor) salmon).getGoalSelector();

        // Priority 1: Flee from predators (dolphins, axolotls, polar bears)
        // Salmon are hunted by aquatic and semi-aquatic predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                salmon,
                1.8,  // speed multiplier when fleeing (salmon are very fast swimmers)
                16,   // detection range
                24,   // flee distance
                Dolphin.class,
                Axolotl.class,
                PolarBear.class
            )
        );

        // Priority 5: School with other salmon (herd cohesion)
        // Salmon are schooling fish that stay together for protection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(
                salmon,
                Salmon.class,
                1.2,  // speed modifier for schooling movement
                12,   // cohesion radius (schools stay fairly tight)
                2,    // minimum school size
                8.0,  // max distance from school
                0.4f  // quorum threshold (fish schools have lower thresholds)
            )
        );
    }
}
