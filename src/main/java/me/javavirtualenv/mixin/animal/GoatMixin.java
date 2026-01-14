package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.GoatClimbingExplorationGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.behavior.core.SeparationDistressGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Goat.
 * Goats are mountain-dwelling herbivores that graze, flee from predators,
 * exhibit strong herd cohesion, and are exceptional climbers that seek
 * elevated positions for predator detection and accessing browse vegetation.
 */
@Mixin(Goat.class)
public abstract class GoatMixin {

    /**
     * Register ecology goals when the goat is constructed.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Goat goat = (Goat) (Object) this;
        var goalSelector = ((MobAccessor) goat).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                goat,
                1.5,  // speed multiplier when fleeing - goats are fast on slopes
                20,   // detection range
                28,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(goat, 1.0, 24)
        );

        // Priority 3: Graze grass when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                goat,
                1.0,
                20,
                SeekFoodGoal.FoodMode.GRAZER,
                GoatMixin::isValidGoatFood
            )
        );

        // Priority 4: Baby goats follow adult goats
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(goat, Goat.class)
        );

        // Priority 5: Baby goats call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(goat, Goat.class, 8.0, 32.0, 1.2)
        );

        // Priority 2: Adult goats protect baby goats from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(goat, Goat.class, 12.0, 16.0, 1.4, Wolf.class)
        );

        // Priority 5: Herd cohesion - stay near other goats
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(goat, Goat.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(goat, 1.0)
        );

        // Priority 6: Climbing exploration - seek elevated positions for lookout
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new GoatClimbingExplorationGoal(goat, 1.0)
        );
    }

    /**
     * Check if an item is valid food for goats.
     * Goats eat wheat and various plant materials.
     */
    private static boolean isValidGoatFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }
}
