package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.ParrotCanopyPerchingGoal;
import me.javavirtualenv.behavior.core.ParrotContactCallingGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Parrot.
 * Parrots are social birds that flock together, communicate via contact calls,
 * perch in tree canopies, seek seeds for food, and flee from predators.
 */
@Mixin(Parrot.class)
public abstract class ParrotMixin {

    /**
     * Register ecology goals after the parrot's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Parrot parrot = (Parrot) (Object) this;
        var goalSelector = ((MobAccessor) parrot).getGoalSelector();

        // Priority 1: Flee from predators (cats and ocelots)
        // Parrots are prey animals and flee from feline predators
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                parrot,
                1.5,  // speed multiplier when fleeing (parrots fly fast when scared)
                12,   // detection range
                20,   // flee distance
                Cat.class,
                Ocelot.class
            )
        );

        // Priority 3: Seek seeds when hungry (parrots eat various seeds)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                parrot,
                1.0,
                12,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                ParrotMixin::isValidParrotFood
            )
        );

        // Priority 5: Flock together with other parrots (social behavior)
        // Parrots are highly social birds that prefer staying in groups
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(parrot, Parrot.class)
        );

        // Priority 6: Contact calling for flock cohesion
        // Parrots emit contact calls to maintain communication with flock members
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new ParrotContactCallingGoal(parrot)
        );

        // Priority 7: Seek canopy perches
        // Parrots prefer high tree positions for safety and visibility
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE + 1,
            new ParrotCanopyPerchingGoal(parrot, 1.0)
        );
    }

    /**
     * Check if an item is valid food for parrots.
     * Parrots eat various seeds.
     */
    private static boolean isValidParrotFood(ItemStack stack) {
        if (stack.is(Items.WHEAT_SEEDS)) return true;
        if (stack.is(Items.MELON_SEEDS)) return true;
        if (stack.is(Items.PUMPKIN_SEEDS)) return true;
        if (stack.is(Items.BEETROOT_SEEDS)) return true;
        if (stack.is(Items.TORCHFLOWER_SEEDS)) return true;
        if (stack.is(Items.PITCHER_POD)) return true;
        return false;
    }
}
