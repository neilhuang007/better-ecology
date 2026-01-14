package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.ChickenDustBathingGoal;
import me.javavirtualenv.behavior.core.ChickenPeckingGoal;
import me.javavirtualenv.behavior.core.ChickenRoostingGoal;
import me.javavirtualenv.behavior.core.EnhancedEggLayingGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.behavior.core.SeparationDistressGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Chicken.
 * Chickens are prey animals that peck at seeds and flee from multiple predators.
 * They have a high flee speed and wide predator detection.
 */
@Mixin(Chicken.class)
public abstract class ChickenMixin {

    @Shadow
    public int eggTime;

    /**
     * Cancel vanilla egg laying behavior completely.
     * Our EnhancedEggLayingGoal handles all egg laying instead.
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void betterEcology$cancelVanillaEggLaying(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;

        if (!chicken.level().isClientSide) {
            // Always reset eggTime to prevent vanilla egg laying
            // Our EnhancedEggLayingGoal handles egg laying with hunger checks
            this.eggTime = Integer.MAX_VALUE / 2;
        }
    }

    /**
     * Register ecology goals after the chicken's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        var goalSelector = ((MobAccessor) chicken).getGoalSelector();

        // Priority 1: Flee from predators (foxes, wolves, cats, ocelots)
        // Chickens are hunted by many predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                chicken,
                1.6,  // speed multiplier when fleeing (chickens are fast when scared)
                12,   // detection range
                20,   // flee distance
                Fox.class,
                Wolf.class,
                Cat.class,
                Ocelot.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(chicken, 1.0, 16)
        );

        // Priority 3: Seek seeds when hungry (chickens peck at seeds)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                chicken,
                1.0,
                12,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                ChickenMixin::isValidChickenFood
            )
        );

        // Priority 5: Peck at the ground for seeds and grubs
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new ChickenPeckingGoal(chicken)
        );

        // Priority 5: Dust bathing behavior for feather maintenance
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new ChickenDustBathingGoal(chicken)
        );

        // Priority 4: Roosting behavior at night
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new ChickenRoostingGoal(chicken)
        );

        // Priority 6: Enhanced egg laying (replaces vanilla egg laying)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new EnhancedEggLayingGoal(chicken)
        );

        // Priority 5: Chicks follow adult chickens
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new FollowParentGoal(chicken, Chicken.class)
        );

        // Priority 5: Chicks call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(chicken, Chicken.class, 6.0, 24.0, 1.3)
        );

        // Priority 2: Adult chickens protect chicks from predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(chicken, Chicken.class, 8.0, 12.0, 1.2,
                Fox.class, Wolf.class, Cat.class, Ocelot.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(chicken, 1.0)
        );
    }

    /**
     * Check if an item is valid food for chickens.
     * Chickens eat various seeds.
     */
    private static boolean isValidChickenFood(ItemStack stack) {
        if (stack.is(Items.WHEAT_SEEDS)) return true;
        if (stack.is(Items.MELON_SEEDS)) return true;
        if (stack.is(Items.PUMPKIN_SEEDS)) return true;
        if (stack.is(Items.BEETROOT_SEEDS)) return true;
        if (stack.is(Items.TORCHFLOWER_SEEDS)) return true;
        if (stack.is(Items.PITCHER_POD)) return true;
        return false;
    }
}
