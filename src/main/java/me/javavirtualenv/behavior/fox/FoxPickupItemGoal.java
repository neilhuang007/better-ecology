package me.javavirtualenv.behavior.fox;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Goal for fox item pickup behavior.
 * <p>
 * Foxes pick up items from the ground, prioritizing food.
 */
public class FoxPickupItemGoal extends Goal {

    private final PathfinderMob fox;
    private final FoxItemCarryBehavior carryBehavior;
    private final FoxItemStorage storage;
    private final double speedModifier;
    private ItemEntity targetItem;

    public FoxPickupItemGoal(PathfinderMob fox, FoxItemCarryBehavior carryBehavior,
                            double speedModifier) {
        this.fox = fox;
        this.carryBehavior = carryBehavior;
        this.storage = FoxItemStorage.get(fox);
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Don't pick up items if already carrying one
        if (storage.hasItem()) {
            return false;
        }

        // Don't pick up items if sleeping
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            if (minecraftFox.isSleeping()) {
                return false;
            }
        }

        // Find nearby item to pick up
        targetItem = findNearestItem();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue if item is still valid and not picked up yet
        return targetItem != null &&
               targetItem.isAlive() &&
               !storage.hasItem();
    }

    @Override
    public void start() {
        fox.setSpeed(speedModifier);
    }

    @Override
    public void stop() {
        targetItem = null;
        fox.setSpeed(1.0);
    }

    @Override
    public void tick() {
        if (targetItem == null) {
            return;
        }

        // Look at item
        fox.getLookControl().setLookAt(targetItem, 30.0f, 30.0f);

        // Move toward item
        double distance = fox.position().distanceTo(targetItem.position());
        if (distance > 1.5) {
            fox.getNavigation().moveTo(targetItem, speedModifier);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private ItemEntity findNearestItem() {
        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity itemEntity : fox.level().getEntitiesOfClass(
                ItemEntity.class,
                fox.getBoundingBox().inflate(16.0))) {

            // Skip if not on ground
            if (!itemEntity.isOnGround()) {
                continue;
            }

            ItemStack itemStack = itemEntity.getItem();

            // Skip non-food items
            if (!itemStack.isEdible() &&
                !itemStack.is(net.minecraft.world.item.Items.SWEET_BERRIES) &&
                !itemStack.is(net.minecraft.world.item.Items.GLOW_BERRIES)) {
                continue;
            }

            double distance = fox.position().distanceTo(itemEntity.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = itemEntity;
            }
        }

        return nearest;
    }
}
