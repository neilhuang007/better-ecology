package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Donkey.
 * Donkeys are herbivores that graze grass and flee from predators.
 * They are more stubborn than horses (slower response to goals).
 */
@Mixin(Donkey.class)
public abstract class DonkeyMixin {

    /**
     * Register ecology goals when the donkey is constructed.
     * Since Donkey doesn't override registerGoals, we inject into the constructor.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterEcology$init(EntityType entityType, Level level, CallbackInfo ci) {
        Donkey donkey = (Donkey) (Object) this;
        var goalSelector = ((MobAccessor) donkey).getGoalSelector();

        // Priority 1: Flee from predators (wolves) - slower than horses
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                donkey,
                1.2,  // speed multiplier when fleeing (slower than horses)
                18,   // detection range
                24,   // flee distance (shorter than horses)
                Wolf.class
            )
        );

        // Priority 3: Graze grass when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                donkey,
                0.9,  // slower movement to food (stubborn)
                18,
                SeekFoodGoal.FoodMode.GRAZER,
                DonkeyMixin::isValidDonkeyFood
            )
        );

        // Priority 4: Baby donkeys follow adult donkeys
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(donkey, Donkey.class)
        );

        // Priority 2: Adult donkeys protect baby donkeys from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(donkey, Donkey.class, 12.0, 16.0, 1.2, Wolf.class)
        );
    }

    /**
     * Check if an item is valid food for donkeys.
     */
    private static boolean isValidDonkeyFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.APPLE) || stack.is(Items.GOLDEN_APPLE);
    }
}
