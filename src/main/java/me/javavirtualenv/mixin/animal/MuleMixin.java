package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Mule.
 * Mules are sterile hybrids between horses and donkeys.
 * They graze grass and flee from predators but cannot breed.
 * Baby mules follow adult horses, donkeys, and mules.
 */
@Mixin(Mule.class)
public abstract class MuleMixin {

    /**
     * Register ecology goals after the mule's constructor completes.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Mule mule = (Mule) (Object) this;
        var goalSelector = ((MobAccessor) mule).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                mule,
                1.3,  // speed multiplier when fleeing (between horse and donkey)
                19,   // detection range
                26,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Graze grass when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                mule,
                1.0,  // normal movement speed
                19,
                SeekFoodGoal.FoodMode.GRAZER,
                MuleMixin::isValidMuleFood
            )
        );

        // Priority 4: Baby mules follow adult horses, donkeys, and mules
        // Note: Mules follow any adult horse (AbstractHorse includes horses, donkeys, mules)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(mule, AbstractHorse.class)
        );
    }

    /**
     * Check if an item is valid food for mules.
     */
    private static boolean isValidMuleFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.APPLE) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.SUGAR);
    }
}
