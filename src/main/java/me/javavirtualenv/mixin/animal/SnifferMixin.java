package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Sniffer.
 * Sniffers are large herbivores that dig for seeds and exhibit protective behaviors.
 * They are herd animals that prefer to stay in groups.
 */
@Mixin(Sniffer.class)
public abstract class SnifferMixin {

    /**
     * Register ecology goals after the sniffer's constructor completes.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Sniffer sniffer = (Sniffer) (Object) this;
        var goalSelector = ((MobAccessor) sniffer).getGoalSelector();

        // Priority 1: Flee from predators (wolves)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                sniffer,
                1.3,  // speed multiplier when fleeing
                16,   // detection range
                24,   // flee distance
                Wolf.class
            )
        );

        // Priority 3: Graze grass when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                sniffer,
                1.0,
                20,
                SeekFoodGoal.FoodMode.GRAZER,
                SnifferMixin::isValidSnifferFood
            )
        );

        // Priority 4: Baby sniffers follow adult sniffers
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(sniffer, Sniffer.class)
        );

        // Priority 2: Adult sniffers protect baby sniffers from wolves
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(sniffer, Sniffer.class, 12.0, 16.0, 1.3, Wolf.class)
        );

        // Priority 5: Herd cohesion - stay near other sniffers
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(sniffer, Sniffer.class)
        );
    }

    /**
     * Check if an item is valid food for sniffers.
     * Sniffers can eat torchflower seeds and pitcher pods.
     */
    private static boolean isValidSnifferFood(ItemStack stack) {
        return stack.is(Items.TORCHFLOWER_SEEDS) || stack.is(Items.PITCHER_POD);
    }
}
