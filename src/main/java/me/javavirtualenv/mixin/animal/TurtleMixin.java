package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Turtle.
 * Turtles are aquatic animals that seek water, flee from predators,
 * and exhibit parent-offspring behaviors.
 */
@Mixin(Turtle.class)
public abstract class TurtleMixin {

    /**
     * Register ecology goals after the turtle's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Turtle turtle = (Turtle) (Object) this;
        var goalSelector = ((MobAccessor) turtle).getGoalSelector();

        // Priority 1: Flee from predators (foxes, wolves, cats)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                turtle,
                1.5,  // speed multiplier when fleeing (faster than normal)
                16,   // detection range
                24,   // flee distance
                Fox.class,
                Wolf.class,
                Cat.class
            )
        );

        // Priority 2: Adult turtles protect baby turtles from predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(turtle, Turtle.class, 10.0, 14.0, 1.2, Fox.class, Wolf.class, Cat.class)
        );

        // Priority 3: Seek water (aquatic animal)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(turtle, 1.0, 32)
        );

        // Priority 3: Graze seagrass when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                turtle,
                1.0,
                16,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                TurtleMixin::isValidTurtleFood
            )
        );

        // Priority 4: Baby turtles follow adult turtles
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(turtle, Turtle.class)
        );
    }

    /**
     * Check if an item is valid food for turtles.
     */
    private static boolean isValidTurtleFood(ItemStack stack) {
        return stack.is(Items.SEAGRASS);
    }
}
