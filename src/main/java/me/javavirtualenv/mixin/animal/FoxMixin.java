package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.FoxPounceHuntingGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.behavior.core.SeparationDistressGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Fox.
 * Foxes are cunning predators that use their signature pounce-hunting technique
 * to catch small prey like chickens and rabbits with high-arc jumps.
 * They also scavenge for food items and flee from larger predators like wolves.
 */
@Mixin(Fox.class)
public abstract class FoxMixin {

    /**
     * Register ecology goals after the fox's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Fox fox = (Fox) (Object) this;
        var goalSelector = ((MobAccessor) fox).getGoalSelector();
        var targetSelector = ((MobAccessor) fox).getTargetSelector();

        // Remove ALL vanilla fox attack and stalk goals to prevent close-range attacks
        // Our FoxPounceHuntingGoal handles hunting with proper distance requirements
        goalSelector.removeAllGoals(goal -> {
            String className = goal.getClass().getName();
            // Only remove VANILLA goals (from net.minecraft or Fox inner classes)
            // Don't remove our ecology goals
            if (className.startsWith("me.javavirtualenv")) {
                return false;
            }
            // Remove ALL vanilla attack-related goals including Fox inner classes
            return goal instanceof MeleeAttackGoal ||
                   className.contains("MeleeAttack") ||
                   className.contains("Attack") ||
                   className.contains("FoxAttack") ||
                   className.contains("StalkPrey") ||
                   className.contains("Stalk") ||
                   className.contains("Pounce") ||
                   className.contains("FoxPounce");
        });

        // Remove ALL vanilla target selectors that make fox target chickens/rabbits
        // Our FoxPounceHuntingGoal handles targeting with proper distance requirements
        targetSelector.removeAllGoals(goal -> {
            String className = goal.getClass().getName();
            // Only remove vanilla goals, not our own
            return !className.startsWith("me.javavirtualenv");
        });

        // Priority 1: Flee from wolves (foxes avoid larger predators)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                fox,
                1.4,  // speed multiplier when fleeing
                16,   // detection range
                24,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(fox, 1.1, 20)
        );

        // Priority 3: Seek food items when hungry (foxes are opportunistic)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                fox,
                1.2,
                18,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                FoxMixin::isValidFoxFood
            )
        );

        // Priority 4: Pounce-hunt small prey when hungry (signature fox behavior)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FoxPounceHuntingGoal(
                fox,
                Chicken.class,
                Rabbit.class
            )
        );

        // Priority 5: Baby foxes follow adult foxes
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new FollowParentGoal(fox, Fox.class)
        );

        // Priority 5: Baby foxes call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(fox, Fox.class, 8.0, 32.0, 1.3)
        );

        // Priority 2: Adult foxes protect baby foxes from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(fox, Fox.class, 10.0, 14.0, 1.4, Wolf.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(fox, 1.1)
        );
    }

    /**
     * Check if an item is valid food for foxes.
     * Foxes are omnivores that eat meat and berries.
     */
    private static boolean isValidFoxFood(ItemStack stack) {
        // Meat items
        if (stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN)) return true;
        if (stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT)) return true;
        if (stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF)) return true;
        if (stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON)) return true;
        if (stack.is(Items.ROTTEN_FLESH)) return true;

        // Berries and fruits (foxes love sweet berries)
        if (stack.is(Items.SWEET_BERRIES)) return true;
        if (stack.is(Items.GLOW_BERRIES)) return true;
        if (stack.is(Items.APPLE)) return true;

        return false;
    }
}
