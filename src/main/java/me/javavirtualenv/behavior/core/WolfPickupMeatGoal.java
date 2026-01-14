package me.javavirtualenv.behavior.core;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes wolves pick up and carry meat items.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when wolf is not holding an item and meat is nearby</li>
 *   <li>Wolf pathfinds to the meat and picks it up</li>
 *   <li>If wolf is hungry, it eats the meat immediately</li>
 *   <li>If wolf is not hungry, it carries the meat for sharing</li>
 * </ul>
 *
 * <p>Priority: Same as SeekFoodGoal but with LOOK flag to not conflict
 */
public class WolfPickupMeatGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(WolfPickupMeatGoal.class);

    private static final int SEARCH_RADIUS = 16;
    private static final double PICKUP_DISTANCE = 1.5;
    private static final int SEARCH_INTERVAL = 5;
    private static final int EAT_DURATION = 40;

    private final Wolf wolf;
    private ItemEntity targetItem;
    private int searchCooldown;
    private int eatTimer;

    public WolfPickupMeatGoal(Wolf wolf) {
        this.wolf = wolf;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Don't pick up more if already holding something
        if (!wolf.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            return false;
        }

        // Search for meat on cooldown
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }

        searchCooldown = SEARCH_INTERVAL;
        return findNearestMeat();
    }

    @Override
    public boolean canContinueToUse() {
        // Continue if eating
        if (eatTimer > 0) {
            return true;
        }

        // Continue if moving to item
        if (targetItem != null && targetItem.isAlive()) {
            return wolf.distanceToSqr(targetItem) > PICKUP_DISTANCE * PICKUP_DISTANCE;
        }

        return false;
    }

    @Override
    public void start() {
        if (targetItem != null) {
            wolf.getNavigation().moveTo(targetItem, 1.2);
            LOGGER.debug("{} moving to pick up {}",
                wolf.getName().getString(), targetItem.getItem().getItem());
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        eatTimer = 0;
        wolf.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Handle eating animation
        if (eatTimer > 0) {
            eatTimer--;
            if (eatTimer <= 0) {
                finishEating();
            }
            return;
        }

        if (targetItem == null || !targetItem.isAlive()) {
            return;
        }

        wolf.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);

        // Check if close enough to pick up
        if (wolf.distanceToSqr(targetItem) <= PICKUP_DISTANCE * PICKUP_DISTANCE) {
            pickupItem();
        } else if (wolf.getNavigation().isDone()) {
            wolf.getNavigation().moveTo(targetItem, 1.2);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return eatTimer > 0;
    }

    /**
     * Finds the nearest meat item on the ground.
     */
    private boolean findNearestMeat() {
        AABB searchBox = wolf.getBoundingBox().inflate(SEARCH_RADIUS);
        List<ItemEntity> items = wolf.level().getEntitiesOfClass(ItemEntity.class, searchBox,
            item -> item.isAlive() && isMeatItem(item.getItem()));

        if (items.isEmpty()) {
            return false;
        }

        targetItem = items.stream()
            .min(Comparator.comparingDouble(item -> wolf.distanceToSqr(item)))
            .orElse(null);

        return targetItem != null;
    }

    /**
     * Checks if an item is a meat item wolves can eat/carry.
     */
    private boolean isMeatItem(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF) ||
               stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP) ||
               stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON) ||
               stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN) ||
               stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT) ||
               stack.is(Items.ROTTEN_FLESH);
    }

    /**
     * Picks up the target item.
     * If hungry, starts eating. Otherwise, carries for sharing.
     */
    private void pickupItem() {
        if (targetItem == null || !targetItem.isAlive()) {
            return;
        }

        ItemStack stack = targetItem.getItem().copy();
        targetItem.discard();

        // If hungry, start eating
        if (AnimalNeeds.isHungry(wolf)) {
            wolf.setItemSlot(EquipmentSlot.MAINHAND, stack);
            eatTimer = EAT_DURATION;
            wolf.getNavigation().stop();
            LOGGER.debug("{} picked up {} and is eating (hungry: {})",
                wolf.getName().getString(), stack.getItem(), AnimalNeeds.getHunger(wolf));
        } else {
            // Carry for sharing
            wolf.setItemSlot(EquipmentSlot.MAINHAND, stack);
            LOGGER.debug("{} picked up {} and is carrying (not hungry: {})",
                wolf.getName().getString(), stack.getItem(), AnimalNeeds.getHunger(wolf));
        }

        targetItem = null;
    }

    /**
     * Finishes eating the held item and restores hunger.
     */
    private void finishEating() {
        ItemStack held = wolf.getItemBySlot(EquipmentSlot.MAINHAND);
        if (held.isEmpty()) {
            return;
        }

        // Calculate hunger restoration
        var foodProps = held.get(DataComponents.FOOD);
        float restoreAmount;
        if (foodProps != null) {
            restoreAmount = foodProps.nutrition() * AnimalThresholds.FOOD_HUNGER_RESTORE_PER_NUTRITION;
        } else {
            restoreAmount = AnimalThresholds.FOOD_HUNGER_RESTORE_PER_NUTRITION * 2; // Default for meat
        }

        AnimalNeeds.modifyHunger(wolf, restoreAmount);

        // Clear the held item
        wolf.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        LOGGER.debug("{} finished eating and restored {} hunger (now: {})",
            wolf.getName().getString(), restoreAmount, AnimalNeeds.getHunger(wolf));
    }
}
