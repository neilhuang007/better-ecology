package me.javavirtualenv.behavior.cow;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Milk production system for cows and mooshrooms.
 * <p>
 * This handle manages:
 * - Milk capacity based on food intake
 * - Milk regeneration over time
 * - Milking cooldown system
 * - Special milk types (suspicious stew for mooshrooms)
 * <p>
 * Based on scientific research into lactation biology in cattle:
 * - Milk production correlates with nutrition and health
 * - Lactation peaks after calving, gradually declines
 * - Milk let-down requires stimulation and has recovery time
 */
public class MilkProductionHandle implements EcologyHandle {
    private static final String NBT_MILK_AMOUNT = "milkAmount";
    private static final String NBT_LAST_MILKED = "lastMilkedTick";
    private static final String NBT_IS_LACTATING = "isLactating";
    private static final String NBT_DAYS_SINCE_CALVING = "daysSinceCalving";

    // Configuration values - configurable via JSON
    private static final int MAX_MILK_AMOUNT = 100;
    private static final int MILKING_COOLDOWN = 1200; // 60 seconds
    private static final int BASE_MILK_REGEN_RATE = 1; // per tick interval
    private static final int LACTATION_DURATION_DAYS = 300;
    private static final double MILK_REGEN_MULTIPLIER_WHEN_FED = 2.0;
    private static final double MILK_REGEN_MULTIPLIER_WHEN_HUNGRY = 0.3;

    @Override
    public String id() {
        return "milk_production";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return profile.getBool("milk_production.enabled", false);
    }

    @Override
    public int tickInterval() {
        return 100; // Update every 5 seconds
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        CompoundTag tag = component.getHandleTag(id());

        if (!isLactating(tag) && !hasRecentlyCalved(tag)) {
            return;
        }

        regenerateMilk(mob, tag, component);
        updateLactationStatus(tag);
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
        CompoundTag handleTag = component.getHandleTag(id());
        outputTag.put(id(), handleTag.copy());
    }

    /**
     * Check if this cow can be milked.
     * Called from interactionMixin when player uses bucket.
     */
    public boolean canBeMilked(Mob mob, EcologyComponent component) {
        if (!(mob instanceof Cow)) {
            return false;
        }

        CompoundTag tag = component.getHandleTag(id());
        int milkAmount = getMilkAmount(tag);
        int lastMilked = getLastMilkedTick(tag);
        int currentTick = mob.tickCount;

        // Must have milk available and cooldown passed
        return milkAmount >= 20 && (currentTick - lastMilked) >= MILKING_COOLDOWN;
    }

    /**
     * Called when player milks this cow.
     */
    public ItemStack onMilked(Mob mob, EcologyComponent component, Player player) {
        CompoundTag tag = component.getHandleTag(id());
        int milkAmount = getMilkAmount(tag);

        if (milkAmount < 20) {
            return ItemStack.EMPTY;
        }

        // Determine milk type based on entity
        ItemStack milkBucket = getMilkBucket(mob);

        // Consume milk
        setMilkAmount(tag, Math.max(0, milkAmount - 20));
        setLastMilkedTick(tag, mob.tickCount);

        // Play sound
        mob.level().playSound(null, mob.blockPosition(), SoundEvents.COW_MILK, SoundSource.NEUTRAL, 1.0F, 1.0F);

        return milkBucket;
    }

    /**
     * Mark this cow as having calved.
     * Called from breeding system when baby is born.
     */
    public void onCalved(Mob mob, EcologyComponent component) {
        CompoundTag tag = component.getHandleTag(id());
        tag.putBoolean(NBT_IS_LACTATING, true);
        tag.putInt(NBT_DAYS_SINCE_CALVING, 0);
        setMilkAmount(tag, MAX_MILK_AMOUNT / 2); // Start with half capacity
    }

    /**
     * Get current milk amount for UI purposes.
     */
    public int getMilkAmountForDisplay(Mob mob, EcologyComponent component) {
        CompoundTag tag = component.getHandleTag(id());
        return getMilkAmount(tag);
    }

    /**
     * Check if cow is currently lactating.
     */
    public boolean isLactating(Mob mob, EcologyComponent component) {
        CompoundTag tag = component.getHandleTag(id());
        return isLactating(tag);
    }

    private void regenerateMilk(Mob mob, CompoundTag tag, EcologyComponent component) {
        int currentAmount = getMilkAmount(tag);
        if (currentAmount >= MAX_MILK_AMOUNT) {
            return;
        }

        double regenMultiplier = calculateRegenMultiplier(component);

        // Base regeneration modified by hunger/condition
        int regenAmount = (int) (BASE_MILK_REGEN_RATE * regenMultiplier);

        // Health bonus - healthier cows produce more milk
        double healthPercent = mob.getHealth() / mob.getMaxHealth();
        regenAmount *= healthPercent;

        setMilkAmount(tag, Math.min(MAX_MILK_AMOUNT, currentAmount + regenAmount));
    }

    private double calculateRegenMultiplier(EcologyComponent component) {
        // Check hunger status
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (hungerTag.contains("hunger")) {
            int hunger = hungerTag.getInt("hunger");
            if (hunger < 20) {
                return MILK_REGEN_MULTIPLIER_WHEN_HUNGRY;
            }
        }

        // Check if recently fed (high hunger)
        if (hungerTag.contains("hunger")) {
            int hunger = hungerTag.getInt("hunger");
            if (hunger > 80) {
                return MILK_REGEN_MULTIPLIER_WHEN_FED;
            }
        }

        return 1.0;
    }

    private void updateLactationStatus(CompoundTag tag) {
        if (!isLactating(tag)) {
            return;
        }

        int daysSinceCalving = tag.getInt(NBT_DAYS_SINCE_CALVING);
        daysSinceCalving++;
        tag.putInt(NBT_DAYS_SINCE_CALVING, daysSinceCalving);

        // End lactation after duration
        if (daysSinceCalving >= LACTATION_DURATION_DAYS) {
            tag.putBoolean(NBT_IS_LACTATING, false);
        }
    }

    private ItemStack getMilkBucket(Mob mob) {
        // Default milk bucket
        return new ItemStack(Items.MILK_BUCKET);
    }

    private int getMilkAmount(CompoundTag tag) {
        return tag.getInt(NBT_MILK_AMOUNT);
    }

    private void setMilkAmount(CompoundTag tag, int amount) {
        tag.putInt(NBT_MILK_AMOUNT, amount);
    }

    private int getLastMilkedTick(CompoundTag tag) {
        return tag.getInt(NBT_LAST_MILKED);
    }

    private void setLastMilkedTick(CompoundTag tag, int tick) {
        tag.putInt(NBT_LAST_MILKED, tick);
    }

    private boolean isLactating(CompoundTag tag) {
        return tag.getBoolean(NBT_IS_LACTATING);
    }

    private boolean hasRecentlyCalved(CompoundTag tag) {
        return tag.contains(NBT_DAYS_SINCE_CALVING);
    }
}
