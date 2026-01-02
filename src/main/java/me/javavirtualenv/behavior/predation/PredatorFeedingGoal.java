package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.ecology.handles.HungerHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Goal for predator feeding behavior.
 * <p>
 * Predators seek out and eat meat items from the ground to restore hunger.
 * This simulates scavenging behavior when hunting is not immediately available.
 */
public class PredatorFeedingGoal extends Goal {

    private static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM, net.minecraft.resources.ResourceLocation.parse("minecraft:meat"));
    private static final double SEARCH_RADIUS = 16.0;
    private static final double EAT_DISTANCE = 1.5;
    private static final int HUNGER_RESTORE_AMOUNT = 20;
    private static final double SPEED_MODIFIER = 1.2;

    private final PathfinderMob mob;
    private final double speedModifier;
    private ItemEntity targetItem;

    public PredatorFeedingGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public PredatorFeedingGoal(PathfinderMob mob) {
        this(mob, SPEED_MODIFIER);
    }

    @Override
    public boolean canUse() {
        if (mob.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }

        targetItem = findNearestMeat();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetItem != null && targetItem.isAlive();
    }

    @Override
    public void start() {
        mob.setSpeed((float) speedModifier);
    }

    @Override
    public void stop() {
        targetItem = null;
        mob.setSpeed(1.0f);
    }

    @Override
    public void tick() {
        if (targetItem == null) {
            return;
        }

        mob.getLookControl().setLookAt(targetItem, 30.0f, 30.0f);

        double distance = mob.position().distanceTo(targetItem.position());
        if (distance > EAT_DISTANCE) {
            mob.getNavigation().moveTo(targetItem, speedModifier);
        } else {
            eatItem(targetItem);
            targetItem = null;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private ItemEntity findNearestMeat() {
        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity itemEntity : mob.level().getEntitiesOfClass(
                ItemEntity.class,
                mob.getBoundingBox().inflate(SEARCH_RADIUS))) {

            if (!itemEntity.onGround()) {
                continue;
            }

            ItemStack itemStack = itemEntity.getItem();
            if (!isMeat(itemStack)) {
                continue;
            }

            double distance = mob.position().distanceTo(itemEntity.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = itemEntity;
            }
        }

        return nearest;
    }

    private boolean isMeat(ItemStack stack) {
        return stack.is(MEAT_TAG);
    }

    private void eatItem(ItemEntity itemEntity) {
        if (!mob.level().isClientSide) {
            itemEntity.discard();
            HungerHandle.restoreHunger(mob, HUNGER_RESTORE_AMOUNT);
        }
    }
}
