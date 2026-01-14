package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.PigMudBathingGoal;
import me.javavirtualenv.behavior.core.PigRootingGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.behavior.core.SeparationDistressGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Pig.
 * Pigs are omnivores that forage for food items and root in dirt.
 * They flee from predators and seek water to cool down.
 */
@Mixin(Pig.class)
public abstract class PigMixin {

    /**
     * Register ecology goals after the pig's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Pig pig = (Pig) (Object) this;
        var goalSelector = ((MobAccessor) pig).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                pig,
                1.3,  // speed multiplier when fleeing
                16,   // detection range
                24,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(pig, 1.0, 20)
        );

        // Priority 3: Seek food items when hungry (pigs are omnivores)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                pig,
                1.1,
                16,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                PigMixin::isValidPigFood
            )
        );

        // Priority 4: Root in dirt/grass for food when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new PigRootingGoal(pig, 1.0, 8)
        );

        // Priority 5: Mud bathing for temperature regulation (low priority, comfort behavior)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new PigMudBathingGoal(pig, 1.0, 16)
        );

        // Priority 5: Baby pigs follow adult pigs
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new FollowParentGoal(pig, Pig.class)
        );

        // Priority 5: Baby pigs call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(pig, Pig.class, 8.0, 32.0, 1.2)
        );

        // Priority 2: Adult pigs protect baby pigs from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(pig, Pig.class, 12.0, 16.0, 1.3, Wolf.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(pig, 1.0)
        );
    }

    /**
     * Check if an item is valid food for pigs.
     * Pigs eat carrots, potatoes, beetroots, and various vegetables.
     */
    private static boolean isValidPigFood(ItemStack stack) {
        if (stack.is(Items.CARROT)) return true;
        if (stack.is(Items.POTATO)) return true;
        if (stack.is(Items.BEETROOT)) return true;
        if (stack.is(Items.APPLE)) return true;
        if (stack.is(Items.BREAD)) return true;
        return false;
    }
}
