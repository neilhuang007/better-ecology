package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HuntPreyGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Frog.
 * Frogs are amphibians that hunt slimes with tongue attacks and need water.
 * They flee from larger predators but are effective hunters of small prey.
 */
@Mixin(Frog.class)
public abstract class FrogMixin {

    /**
     * Register ecology goals during construction.
     * This is necessary because Frog uses brain-based AI instead of goal-based AI,
     * so we inject our goals manually during initialization.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Frog frog = (Frog) (Object) this;
        var goalSelector = ((MobAccessor) frog).getGoalSelector();

        // Priority 1: Flee from predators (wolves and other large threats)
        // Frogs are vulnerable and must escape quickly
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                frog,
                1.5,  // speed multiplier when fleeing
                10,   // detection range
                16,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        // Frogs are amphibians and need regular access to water
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(frog, 1.0, 16)
        );

        // Priority 4: Hunt small prey (slimes and small magma cubes)
        // Frogs use their tongue to catch small slime-like creatures
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new HuntPreyGoal(
                frog,
                1.0,  // speed when hunting
                12,   // hunt range
                Slime.class,
                MagmaCube.class
            )
        );
    }
}
