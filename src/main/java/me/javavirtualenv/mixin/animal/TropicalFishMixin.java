package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FlashExpansionFleeGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for TropicalFish.
 * Tropical fish are reef-dwelling schooling fish that flee from predators
 * and use flash expansion to rapidly scatter when attacked.
 */
@Mixin(TropicalFish.class)
public abstract class TropicalFishMixin {

    /**
     * Register ecology goals after the tropical fish is constructed.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        TropicalFish tropicalFish = (TropicalFish) (Object) this;
        var goalSelector = ((MobAccessor) tropicalFish).getGoalSelector();

        // Priority 1: Flash expansion flee (coordinated school scatter when attacked)
        // When one fish is hurt, the entire school bursts away in a coordinated panic response
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FlashExpansionFleeGoal(tropicalFish, TropicalFish.class)
        );

        // Priority 1: Flee from predators (axolotls and pufferfish)
        // Tropical fish are prey for these aquatic predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                tropicalFish,
                1.5,  // speed multiplier when fleeing
                12,   // detection range
                20,   // flee distance
                Axolotl.class,
                Pufferfish.class
            )
        );

        // Priority 5: Schooling behavior - tropical fish stay together
        // Coral reef fish exhibit strong schooling behavior for safety
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(
                tropicalFish,
                TropicalFish.class,
                1.0,  // normal speed for schooling
                16,   // cohesion radius
                2,    // minimum school size
                10.0, // max distance from school
                0.3f  // quorum threshold (30% moving triggers following)
            )
        );
    }
}
