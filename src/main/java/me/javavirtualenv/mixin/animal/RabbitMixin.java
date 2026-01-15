package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.BreedingBehaviorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.RabbitBinkyGoal;
import me.javavirtualenv.behavior.core.RabbitFreezeBeforeFleeGoal;
import me.javavirtualenv.behavior.core.RabbitThumpWarningGoal;
import me.javavirtualenv.behavior.core.RabbitZigzagFleeGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.behavior.core.SeparationDistressGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Rabbit.
 * Rabbits are small prey animals that flee from many predators.
 * They have excellent speed and rely on quick escapes with zigzag patterns.
 * They also warn other rabbits by thumping the ground when predators are near.
 */
@Mixin(Rabbit.class)
public abstract class RabbitMixin {

    /**
     * Register ecology goals after the rabbit's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Rabbit rabbit = (Rabbit) (Object) this;
        var goalSelector = ((MobAccessor) rabbit).getGoalSelector();

        // Remove vanilla AvoidEntityGoal to prevent conflicts with our custom freeze/flee behavior
        goalSelector.removeAllGoals(goal -> goal instanceof AvoidEntityGoal);

        // Priority 0: Freeze before fleeing when predator detected at medium distance
        // Rabbits freeze motionless for camouflage before deciding to flee
        goalSelector.addGoal(
            0,
            new RabbitFreezeBeforeFleeGoal(
                rabbit,
                Fox.class,
                Wolf.class,
                Cat.class,
                Ocelot.class
            )
        );

        // Priority 0: Thump warning when predator detected (before fleeing)
        // Rabbits warn others before they flee
        goalSelector.addGoal(
            0,
            new RabbitThumpWarningGoal(
                rabbit,
                12,   // detection range for warning
                Fox.class,
                Wolf.class,
                Cat.class,
                Ocelot.class
            )
        );

        // Priority 1: Zigzag flee from predators (foxes, wolves, cats, ocelots)
        // Rabbits use unpredictable zigzag patterns to evade pursuers
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new RabbitZigzagFleeGoal(
                rabbit,
                2.0,  // speed multiplier when fleeing (rabbits are very fast)
                16,   // wide detection range
                24,   // flee distance
                Fox.class,
                Wolf.class,
                Cat.class,
                Ocelot.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(rabbit, 1.2, 16)
        );

        // Priority 3: Seek carrots and vegetables when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                rabbit,
                1.2,
                14,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                RabbitMixin::isValidRabbitFood
            )
        );

        // Priority 5: Baby rabbits follow adult rabbits
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new FollowParentGoal(rabbit, Rabbit.class)
        );

        // Priority 5: Baby rabbits call out when separated from adults
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new SeparationDistressGoal(rabbit, Rabbit.class, 6.0, 24.0, 1.4)
        );

        // Priority 6: Enhanced breeding behavior with mate selection
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE,
            new BreedingBehaviorGoal(rabbit, 1.2)
        );

        // Priority 7: Binky jump when safe and happy
        // Rabbits perform happy jumps when feeling safe, well-fed, and social
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_IDLE + 1,
            new RabbitBinkyGoal(
                rabbit,
                Fox.class,
                Wolf.class,
                Cat.class,
                Ocelot.class
            )
        );
    }

    /**
     * Check if an item is valid food for rabbits.
     * Rabbits eat carrots, dandelions, and golden carrots.
     */
    private static boolean isValidRabbitFood(ItemStack stack) {
        if (stack.is(Items.CARROT)) return true;
        if (stack.is(Items.GOLDEN_CARROT)) return true;
        if (stack.is(Items.DANDELION)) return true;
        return false;
    }
}
