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
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Sheep.
 * Sheep are prey animals that graze grass and flee from predators.
 */
@Mixin(Sheep.class)
public abstract class SheepMixin {

    /**
     * Register ecology goals after the sheep's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Sheep sheep = (Sheep) (Object) this;
        var goalSelector = ((MobAccessor) sheep).getGoalSelector();

        // Priority 1: Flee from predators (wolves, foxes)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                sheep,
                1.5,  // speed multiplier when fleeing
                24,   // detection range
                32,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(sheep, 1.0, 20)
        );

        // Priority 3: Graze grass when hungry (sheep already have EatBlockGoal, this supplements it)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                sheep,
                1.0,
                16,
                SeekFoodGoal.FoodMode.GRAZER,
                SheepMixin::isValidSheepFood
            )
        );

        // Priority 5: Baby sheep follow adult sheep
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new FollowParentGoal(sheep, Sheep.class)
        );

        // Priority 5: Baby sheep call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(sheep, Sheep.class, 8.0, 32.0, 1.2)
        );

        // Priority 2: Adult sheep protect baby sheep from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(sheep, Sheep.class, 12.0, 16.0, 1.3, Wolf.class)
        );

        // Priority 5: Herd cohesion - stay near other sheep
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(sheep, Sheep.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(sheep, 1.0)
        );
    }

    /**
     * Check if an item is valid food for sheep.
     */
    private static boolean isValidSheepFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }
}
