package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.shared.AnimalItemStorage;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.WolfBehaviorHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Goal for wolves to pick up meat items.
 * <p>
 * Wolves will pick up meat when:
 * <ul>
 *   <li>They are hungry (low hunger)</li>
 *   <li>A pack member is hungry (altruistic behavior)</li>
 * </ul>
 * <p>
 * This behavior supports pack food sharing, where wolves can carry food
 * to hungry pack members.
 */
public class WolfPickupItemGoal extends Goal {

    private static final String STORAGE_KEY = "wolf_item_storage";
    private static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM,
        net.minecraft.resources.ResourceLocation.parse("minecraft:meat"));

    private static final double SEARCH_RADIUS = 16.0;
    private static final double PICKUP_DISTANCE = 1.5;
    private static final int COOLDOWN_TICKS = 40;

    private final Wolf wolf;
    private final AnimalItemStorage storage;
    private ItemEntity targetItem;
    private int cooldownTicks;

    public WolfPickupItemGoal(Wolf wolf) {
        this.wolf = wolf;
        this.storage = AnimalItemStorage.get(wolf, STORAGE_KEY);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        if (wolf.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }

        if (storage.hasItem()) {
            return false;
        }

        if (!shouldPickupFood()) {
            return false;
        }

        targetItem = findNearestMeat();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetItem != null && targetItem.isAlive() && !storage.hasItem();
    }

    @Override
    public void start() {
        cooldownTicks = COOLDOWN_TICKS;
    }

    @Override
    public void stop() {
        targetItem = null;
    }

    @Override
    public void tick() {
        if (targetItem == null || storage.hasItem()) {
            return;
        }

        wolf.getLookControl().setLookAt(targetItem, 30.0f, 30.0f);

        double distance = wolf.position().distanceTo(targetItem.position());
        if (distance > PICKUP_DISTANCE) {
            wolf.getNavigation().moveTo(targetItem, 1.2);
        } else {
            pickupItem(targetItem);
            targetItem = null;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Check if wolf should pick up food based on hunger or pack needs.
     */
    private boolean shouldPickupFood() {
        boolean isSelfHungry = WolfBehaviorHandle.isHungry(wolf);

        if (isSelfHungry) {
            return true;
        }

        return hasHungryPackMember();
    }

    /**
     * Check if any pack member is hungry.
     */
    private boolean hasHungryPackMember() {
        var nearbyWolves = wolf.level().getEntitiesOfClass(
            Wolf.class,
            wolf.getBoundingBox().inflate(32.0)
        );

        for (Wolf other : nearbyWolves) {
            if (other.equals(wolf) || other.isTame()) {
                continue;
            }

            if (WolfBehaviorHandle.isHungry(other)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find the nearest meat item entity.
     */
    private ItemEntity findNearestMeat() {
        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity itemEntity : wolf.level().getEntitiesOfClass(
                ItemEntity.class,
                wolf.getBoundingBox().inflate(SEARCH_RADIUS))) {

            if (!itemEntity.onGround()) {
                continue;
            }

            ItemStack itemStack = itemEntity.getItem();
            if (!isMeat(itemStack)) {
                continue;
            }

            double distance = wolf.position().distanceTo(itemEntity.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = itemEntity;
            }
        }

        return nearest;
    }

    /**
     * Check if an item stack is meat.
     */
    private boolean isMeat(ItemStack stack) {
        return stack.is(MEAT_TAG);
    }

    /**
     * Pick up an item and store it.
     */
    private void pickupItem(ItemEntity itemEntity) {
        if (!wolf.level().isClientSide) {
            ItemStack itemStack = itemEntity.getItem();
            ItemStack singleItem = itemStack.split(1);

            if (itemStack.isEmpty()) {
                itemEntity.discard();
            }

            storage.setItem(singleItem);
        }
    }
}
