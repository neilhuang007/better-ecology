package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal for predator feeding behavior.
 * <p>
 * Predators seek out and eat meat items from the ground to restore hunger.
 * This simulates scavenging behavior when hunting is not immediately available.
 * Works for all predators: wolves, cats, foxes, and ocelots.
 */
public class PredatorFeedingGoal extends Goal {

    private static final int HUNGER_THRESHOLD = 75;
    private static final double SEARCH_RADIUS = 16.0;
    private static final double EAT_DISTANCE = 1.5;
    private static final double MOVE_SPEED = 1.2;
    private static final int COOLDOWN_TICKS = 60;

    private static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("better-ecology", "meat"));

    private final PathfinderMob mob;
    private final String mobType;
    private ItemEntity targetItem;
    private Path currentPath;
    private int cooldownTicks;
    private int ticksSinceTargetCheck;

    private String lastDebugMessage = "";
    private boolean wasHungryLastCheck = false;

    public PredatorFeedingGoal(PathfinderMob mob) {
        this.mob = mob;
        this.mobType = mob.getType().toShortString();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public PredatorFeedingGoal(PathfinderMob mob, double speedModifier) {
        this(mob);
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide) {
            return false;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        if (!shouldFeed()) {
            return false;
        }

        ticksSinceTargetCheck++;
        targetItem = findNearestMeat();

        if (targetItem == null) {
            if (ticksSinceTargetCheck % 100 == 0) {
                debug("hungry but no meat found nearby");
            }
            return false;
        }

        ticksSinceTargetCheck = 0;
        ItemStack stack = targetItem.getItem();
        debug("STARTING: seeking " + stack.getItem().toString() + " at " +
              targetItem.blockPosition().getX() + "," + targetItem.blockPosition().getZ() +
              " (hunger=" + getHungerLevel() + ")");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetItem == null || !targetItem.isAlive()) {
            debug("item no longer available");
            return false;
        }

        if (cooldownTicks > 0) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        debug("goal started, pathfinding to item");
        moveToItem();
    }

    @Override
    public void stop() {
        debug("goal stopped");
        targetItem = null;
        currentPath = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetItem == null || !targetItem.isAlive()) {
            targetItem = null;
            return;
        }

        double distance = mob.position().distanceTo(targetItem.position());

        if (distance > EAT_DISTANCE) {
            if (!mob.getNavigation().isInProgress() ||
                currentPath == null ||
                !currentPath.canReach()) {
                moveToItem();
            }
            // Re-check if still alive before looking
            if (targetItem != null && targetItem.isAlive()) {
                mob.getLookControl().setLookAt(targetItem, 30.0f, 30.0f);
            }
            return;
        }

        debug("ate item (distance=" + String.format("%.1f", distance) + ")");
        eatItem(targetItem);
        targetItem = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean shouldFeed() {
        int hunger = getHungerLevel();
        boolean isHungry = hunger < HUNGER_THRESHOLD;

        if (isHungry != wasHungryLastCheck) {
            debug("hunger state changed: " + wasHungryLastCheck + " -> " + isHungry + " (hunger=" + hunger + ")");
            wasHungryLastCheck = isHungry;
        }

        return isHungry;
    }

    private ItemEntity findNearestMeat() {
        var nearbyItems = mob.level().getEntitiesOfClass(
            ItemEntity.class,
            mob.getBoundingBox().inflate(SEARCH_RADIUS)
        );

        ItemEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (ItemEntity itemEntity : nearbyItems) {
            if (!itemEntity.onGround()) {
                continue;
            }

            ItemStack itemStack = itemEntity.getItem();
            if (!isMeat(itemStack)) {
                continue;
            }

            Path path = mob.getNavigation().createPath(itemEntity.blockPosition(), 0);
            if (path == null || !path.canReach()) {
                continue;
            }

            double dist = mob.position().distanceTo(itemEntity.position());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = itemEntity;
            }
        }

        return nearest;
    }

    private boolean isMeat(ItemStack stack) {
        return stack.is(MEAT_TAG);
    }

    private void eatItem(ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();
        itemStack.shrink(1);

        if (itemStack.isEmpty()) {
            itemEntity.discard();
        }

        restoreHunger();
    }

    private void moveToItem() {
        if (targetItem == null) {
            return;
        }

        PathNavigation navigation = mob.getNavigation();
        currentPath = navigation.createPath(targetItem, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(targetItem, MOVE_SPEED);
            debug("path found to item, distance=" + currentPath.getNodeCount() + " nodes");
        } else {
            debug("NO PATH to item, giving up");
            targetItem = null;
            cooldownTicks = COOLDOWN_TICKS;
        }
    }

    private void restoreHunger() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return;
        }

        int currentHunger = getHungerLevel();
        int restoreAmount = 30;
        int newHunger = Math.min(100, currentHunger + restoreAmount);

        var tag = component.getHandleTag("hunger");
        tag.putInt("hunger", newHunger);

        debug("hunger restored: " + currentHunger + " -> " + newHunger + " (+ " + (newHunger - currentHunger) + ")");
    }

    private int getHungerLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100;
        }
        CompoundTag tag = component.getHandleTag("hunger");
        if (!tag.contains("hunger")) {
            return 70;
        }
        return tag.getInt("hunger");
    }

    private EcologyComponent getComponent() {
        if (!(mob instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[PredatorFeeding] " + mobType + " #" + mob.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    public String getDebugState() {
        return String.format("hunger=%d, target=%s, cooldown=%d, path=%s",
            getHungerLevel(),
            targetItem != null ? targetItem.getItem().getItem().toString() : "none",
            cooldownTicks,
            mob.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
