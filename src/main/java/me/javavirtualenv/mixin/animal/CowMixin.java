package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.behavior.core.SeparationDistressGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Cow.
 * Cows are prey animals that graze grass and flee from predators.
 * They are herd animals that prefer to stay in groups.
 */
@Mixin(Cow.class)
public abstract class CowMixin {

    /**
     * Register ecology goals after the cow's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Cow cow = (Cow) (Object) this;
        var goalSelector = ((MobAccessor) cow).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                cow,
                1.4,  // speed multiplier when fleeing
                20,   // detection range
                28,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(cow, 1.0, 24)
        );

        // Priority 3: Graze grass when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                cow,
                1.0,
                20,
                SeekFoodGoal.FoodMode.GRAZER,
                CowMixin::isValidCowFood
            )
        );

        // Priority 5: Baby cows follow adult cows
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new FollowParentGoal(cow, Cow.class)
        );

        // Priority 5: Baby cows call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(cow, Cow.class, 8.0, 32.0, 1.2)
        );

        // Priority 2: Adult cows protect baby cows from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(cow, Cow.class, 12.0, 16.0, 1.3, Wolf.class)
        );

        // Priority 5: Herd cohesion - stay near other cows
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(cow, Cow.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(cow, 1.0)
        );
    }

    /**
     * Check if an item is valid food for cows.
     */
    private static boolean isValidCowFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }
}
