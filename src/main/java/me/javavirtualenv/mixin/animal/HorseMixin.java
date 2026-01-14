package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Horse.
 * Horses are herd animals that graze and flee from predators.
 * Adult horses protect foals from wolves.
 */
@Mixin(Horse.class)
public abstract class HorseMixin {

    /**
     * Register ecology goals when the horse is constructed.
     * Since Horse doesn't override registerGoals, we inject into the constructor.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterEcology$init(EntityType entityType, Level level, CallbackInfo ci) {
        Horse horse = (Horse) (Object) this;
        var goalSelector = ((MobAccessor) horse).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                horse,
                1.5,  // speed multiplier when fleeing
                24,   // detection range
                32,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(horse, 1.0, 24)
        );

        // Priority 3: Graze grass when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                horse,
                1.0,
                20,
                SeekFoodGoal.FoodMode.GRAZER,
                HorseMixin::isValidHorseFood
            )
        );

        // Priority 4: Baby foals follow adult horses
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(horse, Horse.class)
        );

        // Priority 2: Adult horses protect foals from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(horse, Horse.class, 12.0, 20.0, 1.4, Wolf.class)
        );

        // Priority 5: Herd cohesion - stay near other horses
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(horse, Horse.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(horse, 1.0)
        );
    }

    /**
     * Check if an item is valid food for horses.
     */
    private static boolean isValidHorseFood(ItemStack stack) {
        return stack.is(Items.WHEAT) ||
               stack.is(Items.SUGAR) ||
               stack.is(Items.APPLE) ||
               stack.is(Items.GOLDEN_APPLE) ||
               stack.is(Items.GOLDEN_CARROT) ||
               stack.is(Items.HAY_BLOCK);
    }
}
