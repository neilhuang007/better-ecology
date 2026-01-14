package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.CatAmbushFromHidingGoal;
import me.javavirtualenv.behavior.core.CatStalkingAmbushGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HuntPreyGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Cat.
 * Cats are small predators that hunt rabbits and chickens.
 * They seek fish and raw meat when hungry.
 */
@Mixin(Cat.class)
public abstract class CatMixin {

    /**
     * Register ecology goals after the cat's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Cat cat = (Cat) (Object) this;
        var goalSelector = ((MobAccessor) cat).getGoalSelector();

        // Priority 3: Seek food items when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                cat,
                1.2,
                16,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                CatMixin::isValidCatFood
            )
        );

        // Priority 4: Stalk and pounce hunting behavior
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new CatStalkingAmbushGoal(
                cat,
                Chicken.class,
                Rabbit.class
            )
        );

        // Priority 4: Ambush from hiding behavior
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new CatAmbushFromHidingGoal(
                cat,
                Chicken.class,
                Rabbit.class
            )
        );

        // Priority 4: Hunt small prey when hungry (fallback)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new HuntPreyGoal(
                cat,
                1.3,  // speed when hunting
                20,   // hunt range
                Chicken.class,
                Rabbit.class
            )
        );

        // Priority 4: Baby cats follow adult cats
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(cat, Cat.class)
        );

        // Priority 2: Adult cats protect kittens from foxes
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(cat, Cat.class, 8.0, 12.0, 1.4, Fox.class)
        );
    }

    /**
     * Check if an item is valid food for cats.
     * Cats prefer fish and raw meat.
     */
    private static boolean isValidCatFood(ItemStack stack) {
        // Fish items (cats love fish)
        if (stack.is(Items.COD) || stack.is(Items.COOKED_COD)) return true;
        if (stack.is(Items.SALMON) || stack.is(Items.COOKED_SALMON)) return true;
        if (stack.is(Items.TROPICAL_FISH)) return true;
        if (stack.is(Items.PUFFERFISH)) return true;

        // Raw meat items
        if (stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN)) return true;
        if (stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT)) return true;
        if (stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF)) return true;
        if (stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON)) return true;

        return false;
    }
}
