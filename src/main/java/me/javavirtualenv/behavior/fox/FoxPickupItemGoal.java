package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.behavior.shared.AnimalItemStorage;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal for foxes to pick up food items from the ground.
 * <p>
 * Foxes will pick up food items when:
 * <ul>
 *   <li>They are hungry (hunger below threshold)</li>
 *   <li>The item is reachable (path validation)</li>
 * </ul>
 * <p>
 * Foxes prefer berries and meat, and will carry food for later consumption.
 */
public class FoxPickupItemGoal extends Goal {

    // Configuration constants
    private static final String STORAGE_KEY = "fox_item_storage";
    private static final double SEARCH_RADIUS = 16.0;
    private static final double PICKUP_DISTANCE = 1.5;
    private static final double MOVE_SPEED = 1.2;
    private static final int HUNGRY_THRESHOLD = 70;
    private static final int COOLDOWN_TICKS = 60;

    // Instance fields
    private final Fox fox;
    private final AnimalItemStorage storage;
    private ItemEntity targetItem;
    private Path currentPath;
    private int cooldownTicks;
    private int ticksSinceTargetCheck;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasHungryLastCheck = false;

    public FoxPickupItemGoal(Fox fox) {
        this.fox = fox;
        this.storage = AnimalItemStorage.get(fox, STORAGE_KEY);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /**
     * Legacy constructor for backward compatibility.
     * @param fox The fox (must be Fox type)
     * @param behavior Ignored (behavior is now internal)
     * @param speedModifier Ignored (uses internal MOVE_SPEED)
     */
    public FoxPickupItemGoal(net.minecraft.world.entity.PathfinderMob fox, Object behavior, double speedModifier) {
        this((Fox) fox);
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (fox.level().isClientSide) {
            return false;
        }

        // Cooldown after failed pickup attempt
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Already carrying an item
        if (storage.hasItem()) {
            return false;
        }

        // Don't pick up items if sleeping
        if (fox.isSleeping()) {
            return false;
        }

        // Check if we should pick up food
        if (!shouldPickupFood()) {
            return false;
        }

        // Find nearest food item
        ticksSinceTargetCheck++;
        targetItem = findNearestFood();

        if (targetItem == null) {
            // Log every 5 seconds that we're looking for food
            if (ticksSinceTargetCheck % 100 == 0) {
                debug("hungry but no food found nearby");
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
        // Stop if item is gone or picked up
        if (targetItem == null || !targetItem.isAlive()) {
            debug("item no longer available");
            return false;
        }

        // Stop if we picked up the item
        if (storage.hasItem()) {
            debug("item picked up");
            return false;
        }

        // Stop if cooldown triggered
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
        fox.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetItem == null || !targetItem.isAlive()) {
            targetItem = null;
            return;
        }

        if (storage.hasItem()) {
            return;
        }

        double distance = fox.position().distanceTo(targetItem.position());

        // If far from item, keep moving
        if (distance > PICKUP_DISTANCE) {
            // Re-path if we're not moving or lost our path
            if (!fox.getNavigation().isInProgress() ||
                currentPath == null ||
                !currentPath.canReach() ||
                targetItem.hasImpulse) {

                moveToItem();
            }

            // Look at the item while approaching (re-check if still alive)
            if (targetItem != null && targetItem.isAlive()) {
                fox.getLookControl().setLookAt(targetItem, 30.0f, 30.0f);
            }

            // Log progress every second
            if (fox.tickCount % 20 == 0) {
                debug("moving to item, distance=" + String.format("%.1f", distance) + " blocks");
            }
            return;
        }

        // Close enough to pick up
        debug("picked up item (distance=" + String.format("%.1f", distance) + ")");
        pickupItem(targetItem);
        targetItem = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Check if fox should pick up food based on hunger.
     */
    private boolean shouldPickupFood() {
        int hunger = getHungerLevel();
        boolean isHungry = hunger < HUNGRY_THRESHOLD;

        // Log state change
        if (isHungry != wasHungryLastCheck) {
            debug("hunger state changed: " + wasHungryLastCheck + " -> " + isHungry + " (hunger=" + hunger + ")");
            wasHungryLastCheck = isHungry;
        }

        return isHungry;
    }

    /**
     * Find the nearest food item entity.
     */
    private ItemEntity findNearestFood() {
        List<ItemEntity> nearbyItems = fox.level().getEntitiesOfClass(
            ItemEntity.class,
            fox.getBoundingBox().inflate(SEARCH_RADIUS)
        );

        ItemEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (ItemEntity itemEntity : nearbyItems) {
            // Skip items not on ground (falling, thrown, etc.)
            if (!itemEntity.onGround()) {
                continue;
            }

            ItemStack itemStack = itemEntity.getItem();
            if (!isFood(itemStack)) {
                continue;
            }

            // Check if we can reach this item
            Path path = fox.getNavigation().createPath(itemEntity.blockPosition(), 0);
            if (path == null || !path.canReach()) {
                continue; // Unreachable
            }

            double distance = fox.position().distanceTo(itemEntity.position());
            if (distance < nearestDist) {
                nearestDist = distance;
                nearest = itemEntity;
            }
        }

        return nearest;
    }

    /**
     * Check if an item stack is food.
     */
    private boolean isFood(ItemStack stack) {
        // Check for food component
        if (stack.has(DataComponents.FOOD)) {
            return true;
        }

        // Fox-specific foods (berries)
        return stack.is(Items.SWEET_BERRIES) ||
               stack.is(Items.GLOW_BERRIES);
    }

    /**
     * Pick up an item and store it.
     */
    private void pickupItem(ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();
        ItemStack singleItem = itemStack.split(1);

        if (itemStack.isEmpty()) {
            itemEntity.discard();
        }

        storage.setItem(singleItem);
        debug("stored " + singleItem.getItem().toString() + " (" + singleItem.getCount() + ")");
    }

    /**
     * Move towards the target item.
     */
    private void moveToItem() {
        if (targetItem == null) {
            return;
        }

        PathNavigation navigation = fox.getNavigation();
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

    /**
     * Get the current hunger level for this fox.
     */
    private int getHungerLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100; // Not hungry if component doesn't exist
        }
        CompoundTag tag = component.getHandleTag("hunger");
        if (!tag.contains("hunger")) {
            return 70; // Starting hunger
        }
        return tag.getInt("hunger");
    }

    /**
     * Get the ecology component for this fox.
     */
    private EcologyComponent getComponent() {
        if (!(fox instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[FoxPickupItem] Fox #" + fox.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        ItemStack carried = storage.getItem();
        return String.format("hunger=%d, carrying=%s, target=%s, cooldown=%d, path=%s",
            getHungerLevel(),
            !carried.isEmpty() ? carried.getItem().toString() : "none",
            targetItem != null ? targetItem.getItem().getItem().toString() : "none",
            cooldownTicks,
            fox.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
