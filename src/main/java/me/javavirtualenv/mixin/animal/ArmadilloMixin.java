package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Armadillo.
 * Armadillos are prey animals that forage for insects and flee from predators.
 * Baby armadillos follow adults, and adults protect babies from threats.
 */
@Mixin(Armadillo.class)
public abstract class ArmadilloMixin {

    /**
     * Register ecology goals after the armadillo's constructor completes.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Armadillo armadillo = (Armadillo) (Object) this;
        var goalSelector = ((MobAccessor) armadillo).getGoalSelector();

        // Priority 1: Flee from predators (wolves, foxes)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                armadillo,
                1.5,  // speed multiplier when fleeing
                16,   // detection range
                24,   // flee distance
                Wolf.class,
                Fox.class
            )
        );

        // Priority 3: Forage for insects/food on ground
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                armadillo,
                1.0,
                16,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                ArmadilloMixin::isValidArmadilloFood
            )
        );

        // Priority 4: Baby armadillos follow adult armadillos
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(armadillo, Armadillo.class)
        );

        // Priority 2: Adult armadillos protect baby armadillos from predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(armadillo, Armadillo.class, 12.0, 16.0, 1.3, Wolf.class, Fox.class)
        );
    }

    /**
     * Check if an item is valid food for armadillos.
     */
    private static boolean isValidArmadilloFood(ItemStack stack) {
        return stack.is(Items.SPIDER_EYE);
    }
}
