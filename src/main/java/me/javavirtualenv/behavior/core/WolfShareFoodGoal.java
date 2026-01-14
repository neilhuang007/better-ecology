package me.javavirtualenv.behavior.core;

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
 * Goal that makes wolves share food with hungry pack members.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when wolf is holding food and a hungry packmate is nearby</li>
 *   <li>Prioritizes sharing based on pack rank (alpha > beta > omega)</li>
 *   <li>Wolf moves toward hungry packmate and drops food near them</li>
 *   <li>Has a cooldown between sharing attempts</li>
 * </ul>
 */
public class WolfShareFoodGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(WolfShareFoodGoal.class);

    private static final int SEARCH_RADIUS = 16;
    private static final int SHARE_COOLDOWN_TICKS = 600; // 30 seconds
    private static final double SHARE_DISTANCE = 2.0;
    private static final int SEARCH_INTERVAL = 40;

    private final Wolf wolf;
    private Wolf targetPackmate;
    private int searchCooldown;

    public WolfShareFoodGoal(Wolf wolf) {
        this.wolf = wolf;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Must be holding food
        if (!isHoldingFood()) {
            return false;
        }

        // Must not be on share cooldown
        if (!WolfPackData.canShare(wolf)) {
            return false;
        }

        // Search for hungry packmate
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }

        searchCooldown = SEARCH_INTERVAL;
        return findHungryPackmate();
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPackmate == null || !targetPackmate.isAlive()) {
            return false;
        }

        if (!isHoldingFood()) {
            return false;
        }

        // Continue until we're close enough to share
        return wolf.distanceToSqr(targetPackmate) > 1.0;
    }

    @Override
    public void start() {
        if (targetPackmate != null) {
            wolf.getNavigation().moveTo(targetPackmate, 1.0);
            LOGGER.debug("{} moving to share food with {}",
                wolf.getName().getString(), targetPackmate.getName().getString());
        }
    }

    @Override
    public void stop() {
        // If we're close enough, drop the food for the packmate
        if (targetPackmate != null && targetPackmate.isAlive() &&
            wolf.distanceToSqr(targetPackmate) <= SHARE_DISTANCE * SHARE_DISTANCE) {

            dropFoodForPackmate();
        }

        targetPackmate = null;
        wolf.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPackmate == null) {
            return;
        }

        wolf.getLookControl().setLookAt(targetPackmate, 30.0F, 30.0F);

        // Check if close enough to share
        if (wolf.distanceToSqr(targetPackmate) <= SHARE_DISTANCE * SHARE_DISTANCE) {
            dropFoodForPackmate();
            wolf.getNavigation().stop();
        } else if (wolf.getNavigation().isDone()) {
            wolf.getNavigation().moveTo(targetPackmate, 1.0);
        }
    }

    /**
     * Checks if the wolf is holding a food item.
     */
    private boolean isHoldingFood() {
        ItemStack held = wolf.getItemBySlot(EquipmentSlot.MAINHAND);
        return !held.isEmpty() && isMeatItem(held);
    }

    /**
     * Checks if an item stack is a meat item.
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
     * Finds a hungry packmate to share food with.
     * Prioritizes by rank (alpha > beta > omega) and then by hunger level.
     */
    private boolean findHungryPackmate() {
        AABB searchBox = wolf.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(Wolf.class, searchBox, w ->
            w != wolf &&
            w.isAlive() &&
            WolfPackData.arePackmates(wolf, w) &&
            AnimalNeeds.isHungry(w)
        );

        if (nearbyWolves.isEmpty()) {
            return false;
        }

        // Sort by rank (higher rank = higher priority) then by hunger (lower = more urgent)
        targetPackmate = nearbyWolves.stream()
            .min(Comparator
                .comparingInt((Wolf w) -> -WolfPackData.getPackData(w).rank().getPriority())
                .thenComparingDouble(AnimalNeeds::getHunger))
            .orElse(null);

        if (targetPackmate != null) {
            LOGGER.debug("{} found hungry packmate: {} (rank: {}, hunger: {})",
                wolf.getName().getString(),
                targetPackmate.getName().getString(),
                WolfPackData.getPackData(targetPackmate).rank(),
                AnimalNeeds.getHunger(targetPackmate));
        }

        return targetPackmate != null;
    }

    /**
     * Drops the held food item for the packmate to pick up.
     */
    private void dropFoodForPackmate() {
        ItemStack held = wolf.getItemBySlot(EquipmentSlot.MAINHAND);
        if (held.isEmpty()) {
            return;
        }

        // Drop the item at the packmate's position
        ItemEntity itemEntity = new ItemEntity(
            wolf.level(),
            targetPackmate.getX(),
            targetPackmate.getY(),
            targetPackmate.getZ(),
            held.copy()
        );
        itemEntity.setNoPickUpDelay();
        wolf.level().addFreshEntity(itemEntity);

        // Clear the wolf's held item
        wolf.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        // Set share cooldown
        WolfPackData.setSharesCooldown(wolf, SHARE_COOLDOWN_TICKS);

        LOGGER.debug("{} shared {} with {}",
            wolf.getName().getString(),
            held.getItem(),
            targetPackmate.getName().getString());
    }
}
