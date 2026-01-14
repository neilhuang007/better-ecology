package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.HerdFollowGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Llama.
 * Llamas are pack animals that form caravans, spit at predators defensively,
 * and protect their young. They exhibit strong herd cohesion.
 */
@Mixin(Llama.class)
public abstract class LlamaMixin {

    /**
     * Register ecology goals when the llama is constructed.
     * Since Llama doesn't override registerGoals, we inject into the constructor.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType entityType, Level level, CallbackInfo ci) {
        Llama llama = (Llama) (Object) this;
        var goalSelector = ((MobAccessor) llama).getGoalSelector();

        // Priority 1: Flee from predators (wolves) - but less timid than horses
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                llama,
                1.3,  // speed multiplier when fleeing (slower than horses)
                16,   // detection range (shorter, llamas are braver)
                24,   // flee distance (shorter, llamas stand their ground more)
                Wolf.class
            )
        );

        // Priority 2: Adult llamas aggressively protect young from predators
        // This represents their defensive spitting behavior
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(llama, Llama.class, 16.0, 24.0, 1.5, Wolf.class)
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(llama, 1.0, 20)
        );

        // Priority 3: Graze when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                llama,
                1.0,
                16,
                SeekFoodGoal.FoodMode.GRAZER,
                LlamaMixin::isValidLlamaFood
            )
        );

        // Priority 4: Baby llamas follow adult llamas
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(llama, Llama.class)
        );

        // Priority 5: Caravan formation - llamas follow each other in a line
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdFollowGoal(llama, Llama.class, 1.0, 24, 2, 10.0)
        );

        // Priority 5: Pack animal herd cohesion - stay near other llamas
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(llama, Llama.class)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(llama, 1.0)
        );
    }

    /**
     * Check if an item is valid food for llamas.
     */
    private static boolean isValidLlamaFood(ItemStack stack) {
        return stack.is(Items.WHEAT) ||
               stack.is(Items.HAY_BLOCK);
    }
}
