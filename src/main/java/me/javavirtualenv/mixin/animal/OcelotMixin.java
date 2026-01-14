package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.CatAmbushFromHidingGoal;
import me.javavirtualenv.behavior.core.CatStalkingAmbushGoal;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HuntPreyGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.behavior.core.SeekFoodGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Ocelot.
 * Ocelots are wild jungle cats that hunt chickens and are naturally skittish around players.
 */
@Mixin(Ocelot.class)
public abstract class OcelotMixin {

    /**
     * Register ecology goals after the ocelot's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Ocelot ocelot = (Ocelot) (Object) this;
        var goalSelector = ((MobAccessor) ocelot).getGoalSelector();

        // Priority 1: Flee from players (ocelots are skittish wild cats)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                ocelot,
                1.4,  // speed multiplier when fleeing
                12,   // detection range
                20,   // flee distance
                Player.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(ocelot, 1.1, 20)
        );

        // Priority 3: Seek food items when hungry (raw fish/chicken)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekFoodGoal(
                ocelot,
                1.2,
                18,
                SeekFoodGoal.FoodMode.ITEM_SEEKER,
                OcelotMixin::isValidOcelotFood
            )
        );

        // Priority 4: Stalk and pounce hunting behavior
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new CatStalkingAmbushGoal(
                ocelot,
                Chicken.class
            )
        );

        // Priority 4: Ambush from hiding behavior
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new CatAmbushFromHidingGoal(
                ocelot,
                Chicken.class
            )
        );

        // Priority 4: Hunt chickens when hungry (fallback)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new HuntPreyGoal(
                ocelot,
                1.3,  // speed when hunting
                24,   // hunt range
                Chicken.class
            )
        );

        // Priority 4: Baby ocelots (kittens) follow adult ocelots
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(ocelot, Ocelot.class)
        );

        // Priority 2: Mother ocelots protect kittens from players
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(ocelot, Ocelot.class, 10.0, 14.0, 1.4, Player.class)
        );
    }

    /**
     * Check if an item is valid food for ocelots.
     * Ocelots are carnivores that eat fish and chicken.
     */
    private static boolean isValidOcelotFood(ItemStack stack) {
        // Fish items (raw preferred)
        if (stack.is(Items.COD)) return true;
        if (stack.is(Items.SALMON)) return true;
        if (stack.is(Items.TROPICAL_FISH)) return true;
        if (stack.is(Items.PUFFERFISH)) return true;

        // Chicken meat
        if (stack.is(Items.CHICKEN)) return true;
        if (stack.is(Items.COOKED_CHICKEN)) return true;

        return false;
    }
}
