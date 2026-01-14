package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Cod.
 * Cod are schooling fish that flee from aquatic predators.
 * They use herd cohesion to stay together in schools.
 */
@Mixin(Cod.class)
public abstract class CodMixin {

    /**
     * Register ecology goals after the cod is constructed.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Cod cod = (Cod) (Object) this;
        var goalSelector = ((MobAccessor) cod).getGoalSelector();

        // Priority 1: Flee from predators (dolphins, axolotls)
        // Cod flee from aquatic predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                cod,
                1.4,  // speed multiplier when fleeing (fish are fast when scared)
                10,   // detection range
                16,   // flee distance
                Dolphin.class,
                Axolotl.class
            )
        );

        // Priority 5: Schooling behavior (herd cohesion with other cod)
        // Cod naturally form schools for protection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(
                cod,
                Cod.class,
                1.0,  // speed modifier
                12,   // cohesion radius
                2,    // minimum school size
                8.0   // max distance from school
            )
        );
    }
}
