package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.production.MilkProductionHandle;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for cow and mooshroom interaction with milking.
 *
 * Handles:
 * - Bucket milking for cows and mooshrooms
 * - Bowl milking for mooshrooms (mushroom stew)
 * - Integration with milk production handles
 * - Quality-based milk production
 */
@Mixin(value = {Cow.class, MushroomCow.class})
public abstract class CowInteractionMixin {

    /**
     * Inject into mobInteract to handle custom milking behavior.
     * This runs before the vanilla milking logic, allowing us to override it.
     */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void betterEcology$handleMilking(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(this instanceof Cow cow)) {
            return;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // Handle empty bucket (standard milking)
        if (heldItem.is(Items.BUCKET)) {
            handleBucketMilking(cow, player, hand, cir);
            return;
        }

        // Handle mooshroom-specific interactions with bowl
        if (cow instanceof MushroomCow mooshroom && heldItem.is(Items.BOWL)) {
            handleBowlMilking(mooshroom, player, hand, cir);
        }
    }

    /**
     * Handle bucket milking for both cows and mooshrooms.
     */
    private void handleBucketMilking(Cow cow, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        EcologyComponent component = EcologyHooks.getComponent(cow);
        if (component == null) {
            return; // Fall back to vanilla behavior
        }

        EcologyProfile profile = EcologyHooks.getProfile(cow);
        if (profile == null) {
            return;
        }

        // Find the milk production handle
        MilkProductionHandle milkHandle = findMilkProductionHandle(component);
        if (milkHandle == null) {
            return; // Fall back to vanilla
        }

        // Check if can be milked
        if (!milkHandle.canBeMilked(cow, component, profile)) {
            return; // Let vanilla handle the failure case
        }

        // Perform milking
        ItemStack milkProduct = milkHandle.milk(cow, player, component, profile);
        if (!milkProduct.isEmpty()) {
            // Give milk product to player
            if (!player.getAbilities().instabuild) {
                ItemStack heldItem = player.getItemInHand(hand);
                heldItem.shrink(1);
                if (heldItem.isEmpty()) {
                    player.setItemInHand(hand, milkProduct);
                } else if (!player.getInventory().add(milkProduct)) {
                    player.drop(milkProduct, false);
                }
            }

            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    /**
     * Handle bowl milking for mooshrooms (mushroom stew).
     */
    private void handleBowlMilking(MushroomCow mooshroom, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        EcologyComponent component = EcologyHooks.getComponent(mooshroom);
        if (component == null) {
            return;
        }

        EcologyProfile profile = EcologyHooks.getProfile(mooshroom);
        if (profile == null) {
            return;
        }

        MilkProductionHandle milkHandle = findMilkProductionHandle(component);
        if (milkHandle == null) {
            return;
        }

        // Check if can be milked
        if (!milkHandle.canBeMilked(mooshroom, component, profile)) {
            return;
        }

        // Perform milking (returns mushroom stew for mooshrooms)
        ItemStack stew = milkHandle.milk(mooshroom, player, component, profile);
        if (!stew.isEmpty()) {
            if (!player.getAbilities().instabuild) {
                ItemStack heldItem = player.getItemInHand(hand);
                heldItem.shrink(1);
                if (heldItem.isEmpty()) {
                    player.setItemInHand(hand, stew);
                } else if (!player.getInventory().add(stew)) {
                    player.drop(stew, false);
                }
            }

            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    /**
     * Find the milk production handle from the component.
     */
    private MilkProductionHandle findMilkProductionHandle(EcologyComponent component) {
        // Check if component has milk production handle registered
        for (var handle : component.handles()) {
            if (handle instanceof MilkProductionHandle milkHandle) {
                return milkHandle;
            }
        }
        return null;
    }
}
