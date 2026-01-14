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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Mooshroom.
 * Mooshrooms are similar to cows but live in mushroom biomes.
 * They graze on both grass and mushrooms, and exhibit herd behavior.
 */
@Mixin(MushroomCow.class)
public abstract class MooshroomMixin {

    /**
     * Register ecology goals after the mooshroom's constructor completes.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        MushroomCow mooshroom = (MushroomCow) (Object) this;
        var goalSelector = ((MobAccessor) mooshroom).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                mooshroom,
                1.4,  // speed multiplier when fleeing
                20,   // detection range
                28,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(mooshroom, 1.0, 24)
        );

        // Priority 3: Graze grass and mushrooms when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                mooshroom,
                1.0,
                20,
                SeekFoodGoal.FoodMode.GRAZER,
                MooshroomMixin::isValidMooshroomFood
            )
        );

        // Priority 4: Baby mooshrooms follow adult mooshrooms
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(mooshroom, MushroomCow.class)
        );

        // Priority 5: Baby mooshrooms call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(mooshroom, MushroomCow.class, 8.0, 32.0, 1.2)
        );

        // Priority 2: Adult mooshrooms protect baby mooshrooms from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(mooshroom, MushroomCow.class, 12.0, 16.0, 1.3, Wolf.class)
        );

        // Priority 5: Herd cohesion - stay near other mooshrooms
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(mooshroom, MushroomCow.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(mooshroom, 1.0)
        );
    }

    /**
     * Check if an item is valid food for mooshrooms.
     */
    private static boolean isValidMooshroomFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }
}
