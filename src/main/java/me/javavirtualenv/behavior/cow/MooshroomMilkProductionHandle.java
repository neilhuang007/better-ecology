package me.javavirtualenv.behavior.cow;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Extended milk production for mooshrooms.
 * <p>
 * Mooshrooms produce:
 * - Standard milk (with bucket)
 * - Mushroom stew (with bowl)
 * - Suspicious stew (with bowl and flower)
 * <p>
 * Suspicious stew effects vary based on flower type:
 * - Allium: Fire resistance
 * - Azure Bluet: Blindness
 * - Red Tulip: Weakness
 * - Cornflower: Jump boost
 * - Lily of the Valley: Poison
 * - Oxeye Daisy: Regeneration
 * - Poppy: Night vision
 * - Dandelion: Saturation
 * - White Tulip: Weakness
 * - Pink Tulip: Weakness
 * - Orange Tulip: Weakness
 * - Blue Orchid: Saturation
 * - Withers Rose: Wither
 */
public class MooshroomMilkProductionHandle extends MilkProductionHandle {

    private static final int SUSPICIOUS_STEW_COOLDOWN = 24000; // 20 minutes

    @Override
    public String id() {
        return "mooshroom_milk_production";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return profile.getBool("mooshroom_milk_production.enabled", false);
    }

    /**
     * Get milk or stew based on item used.
     */
    public ItemStack onMooshroomMilked(Mob mob, EcologyComponent component, ItemStack usedItem) {
        if (!(mob instanceof MushroomCow)) {
            return ItemStack.EMPTY;
        }

        // Check if player is using bowl (stew)
        if (usedItem.is(Items.BOWL)) {
            return getStew(mob, component);
        }

        // Otherwise return milk bucket
        return super.onMilked(mob, component, null);
    }

    private ItemStack getStew(Mob mob, EcologyComponent component) {
        CompoundTag tag = component.getHandleTag(id());
        int milkAmount = getMilkAmount(tag);

        if (milkAmount < 30) {
            return ItemStack.EMPTY;
        }

        // Check cooldown for suspicious stew
        int lastStew = tag.getInt("lastStewTick");
        int currentTick = mob.tickCount;

        boolean canMakeSuspicious = (currentTick - lastStew) >= SUSPICIOUS_STEW_COOLDOWN;

        ItemStack stew;
        if (canMakeSuspicious && mob.getPersistentData().contains("better-ecology:stewFlower")) {
            // Create suspicious stew with effect
            String flowerType = mob.getPersistentData().getString("better-ecology:stewFlower");
            stew = createSuspiciousStew(flowerType);
            tag.putInt("lastStewTick", currentTick);
            mob.getPersistentData().remove("better-ecology:stewFlower");
        } else {
            // Regular mushroom stew
            stew = new ItemStack(Items.MUSHROOM_STEW);
        }

        // Consume milk
        setMilkAmount(tag, Math.max(0, milkAmount - 30));
        setLastMilkedTick(tag, currentTick);

        // Play sound
        mob.level().playSound(null, mob.blockPosition(),
                net.minecraft.sounds.SoundEvents.MOOSHROOM_MILK,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);

        return stew;
    }

    private ItemStack createSuspiciousStew(String flowerType) {
        ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
        net.minecraft.world.item.alchemy.SuspiciousStewEffect.addToStew(stew,
                getEffectForFlower(flowerType), 160);

        return stew;
    }

    private net.minecraft.world.effect.MobEffect getEffectForFlower(String flowerType) {
        return switch (flowerType) {
            case "minecraft:allium" -> net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE;
            case "minecraft:azure_bluet" -> net.minecraft.world.effect.MobEffects.BLINDNESS;
            case "minecraft:red_tulip" -> net.minecraft.world.effect.MobEffects.WEAKNESS;
            case "minecraft:cornflower" -> net.minecraft.world.effect.MobEffects.JUMP;
            case "minecraft:lily_of_the_valley" -> net.minecraft.world.effect.MobEffects.POISON;
            case "minecraft:oxeye_daisy" -> net.minecraft.world.effect.MobEffects.REGENERATION;
            case "minecraft:poppy" -> net.minecraft.world.effect.MobEffects.NIGHT_VISION;
            case "minecraft:dandelion" -> net.minecraft.world.effect.MobEffects.SATURATION;
            case "minecraft:white_tulip", "minecraft:pink_tulip",
                 "minecraft:orange_tulip" -> net.minecraft.world.effect.MobEffects.WEAKNESS;
            case "minecraft:blue_orchid" -> net.minecraft.world.effect.MobEffects.SATURATION;
            case "minecraft:wither_rose" -> net.minecraft.world.effect.MobEffects.WITHER;
            default -> net.minecraft.world.effect.MobEffects.SATURATION;
        };
    }

    /**
     * Feed flower to mooshroom for suspicious stew.
     */
    public boolean onFlowerFed(Mob mob, ItemStack flower) {
        if (!isFlower(flower)) {
            return false;
        }

        CompoundTag tag = mob.getPersistentData();
        tag.putString("better-ecology:stewFlower", flower.getItem().toString());

        // Play eating sound
        mob.level().playSound(null, mob.blockPosition(),
                net.minecraft.sounds.SoundEvents.MOOSHROOM_EAT,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);

        return true;
    }

    private boolean isFlower(ItemStack stack) {
        return stack.is(Items.ALLIUM) ||
               stack.is(Items.AZURE_BLUET) ||
               stack.is(Items.RED_TULIP) ||
               stack.is(Items.CORNFLOWER) ||
               stack.is(Items.LILY_OF_THE_VALLEY) ||
               stack.is(Items.OXEYE_DAISY) ||
               stack.is(Items.POPPY) ||
               stack.is(Items.DANDELION) ||
               stack.is(Items.WHITE_TULIP) ||
               stack.is(Items.PINK_TULIP) ||
               stack.is(Items.ORANGE_TULIP) ||
               stack.is(Items.BLUE_ORCHID) ||
               stack.is(Items.WITHER_ROSE);
    }
}
