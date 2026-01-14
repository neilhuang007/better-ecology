package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Camel.
 * Camels are desert animals that travel in caravans and need water.
 * They are prey animals that flee from predators like wolves.
 */
@Mixin(Camel.class)
public abstract class CamelMixin {

    /**
     * Register ecology goals after the camel's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Camel camel = (Camel) (Object) this;
        var goalSelector = ((MobAccessor) camel).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                camel,
                1.5,  // speed multiplier when fleeing
                24,   // detection range
                32,   // flee distance
                Wolf.class
            )
        );

        // Priority 2: Adult camels protect baby camels from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(camel, Camel.class, 12.0, 16.0, 1.4, Wolf.class)
        );

        // Priority 3: Seek water when thirsty (desert animals need water)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(camel, 1.0, 32)
        );

        // Priority 3: Graze when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                camel,
                1.0,
                20,
                SeekFoodGoal.FoodMode.GRAZER,
                CamelMixin::isValidCamelFood
            )
        );

        // Priority 4: Baby camels follow adult camels
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(camel, Camel.class)
        );

        // Priority 5: Herd cohesion - camels travel in caravans
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(camel, Camel.class)
        );
    }

    /**
     * Check if an item is valid food for camels.
     */
    private static boolean isValidCamelFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.CACTUS);
    }
}
