package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.HuntPreyGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.behavior.core.WolfPackData;
import me.javavirtualenv.behavior.core.WolfPackHuntCoordinationGoal;
import me.javavirtualenv.behavior.core.WolfPickupMeatGoal;
import me.javavirtualenv.behavior.core.WolfShareFoodGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Wolf.
 * Wolves are pack predators that:
 * - Hunt prey cooperatively
 * - Pick up and carry meat items
 * - Share food with hungry pack members
 * - Maintain pack hierarchy (alpha/beta/omega)
 */
@Mixin(Wolf.class)
public abstract class WolfMixin {

    /**
     * Initialize pack data when wolf loads/spawns.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void betterEcology$initPackData(CallbackInfo ci) {
        Wolf wolf = (Wolf) (Object) this;
        if (!wolf.level().isClientSide()) {
            // Tick pack share cooldown
            WolfPackData.tickSharesCooldown(wolf);
        }
    }

    /**
     * Register ecology goals after the wolf's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Wolf wolf = (Wolf) (Object) this;
        var goalSelector = ((MobAccessor) wolf).getGoalSelector();

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(wolf, 1.2, 24)
        );

        // Priority 3: Pick up meat items (replaces SeekFoodGoal with more features)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new WolfPickupMeatGoal(wolf)
        );

        // Priority 4: Pack hunt coordination (for 3+ wolves hunting large prey)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new WolfPackHuntCoordinationGoal(wolf)
        );

        // Priority 4: Hunt prey when hungry and no meat items available (solo hunting)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new HuntPreyGoal(
                wolf,
                1.3,  // speed when hunting
                32,   // hunt range
                Sheep.class,
                Rabbit.class,
                Chicken.class,
                Pig.class
            )
        );

        // Priority 5: Share food with hungry pack members
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new WolfShareFoodGoal(wolf)
        );
    }
}
