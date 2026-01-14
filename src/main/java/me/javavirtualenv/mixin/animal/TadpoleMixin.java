package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Tadpole.
 * Tadpoles are juvenile frogs that school together and flee from axolotls.
 */
@Mixin(Tadpole.class)
public abstract class TadpoleMixin {

    /**
     * Register ecology goals after the tadpole is initialized.
     * Tadpoles use the Brain AI system but still have a goalSelector.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Tadpole tadpole = (Tadpole) (Object) this;
        var goalSelector = ((MobAccessor) tadpole).getGoalSelector();

        // Priority 1: Flee from axolotls (natural predator)
        // Tadpoles are vulnerable and must escape quickly
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                tadpole,
                1.5,  // speed multiplier when fleeing
                10,   // detection range
                16,   // flee distance
                Axolotl.class
            )
        );

        // Priority 5: Schooling behavior with other tadpoles
        // Tadpoles naturally group together for safety
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(
                tadpole,
                Tadpole.class,
                1.0,  // normal speed
                12,   // cohesion radius
                2,    // minimum school size
                8.0   // max distance from school
            )
        );
    }
}
