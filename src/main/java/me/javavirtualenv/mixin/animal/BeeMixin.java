package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Bee.
 * Bees maintain herd cohesion with other bees and seek water when thirsty.
 * Note: Vanilla bee behavior (pollination, hive return) is preserved.
 */
@Mixin(Bee.class)
public abstract class BeeMixin {

    /**
     * Register ecology goals after the bee's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Bee bee = (Bee) (Object) this;
        var goalSelector = ((MobAccessor) bee).getGoalSelector();

        // Priority 3: Seek water when thirsty
        // Bees need to drink water in real life
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(bee, 1.0, 16)
        );

        // Priority 5: Maintain cohesion with other bees
        // Bees tend to stay near each other when not pollinating
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(bee, Bee.class)
        );
    }
}
