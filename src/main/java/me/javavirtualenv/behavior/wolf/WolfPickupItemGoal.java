package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.behavior.shared.AnimalItemStorage;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Goal for wolves to pick up meat items from the ground.
 * <p>
 * Wolves will pick up meat when:
 * <ul>
 *   <li>They are hungry (hunger below threshold)</li>
 *   <li>A pack member is hungry (altruistic behavior)</li>
 * </ul>
 * <p>
 * The wolf will pathfind to the meat, pick it up, and carry it
 * for sharing with other pack members.
 */
public class WolfPickupItemGoal extends Goal {

    // Configuration constants
    private static final String STORAGE_KEY = "wolf_item_storage";
    private static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("better-ecology", "meat"));

    // Fallback list of meat items for gametest environment where tags may not load
    private static final Set<Item> MEAT_ITEMS = Set.of(
        Items.BEEF, Items.COOKED_BEEF,
        Items.PORKCHOP, Items.COOKED_PORKCHOP,
        Items.MUTTON, Items.COOKED_MUTTON,
        Items.CHICKEN, Items.COOKED_CHICKEN,
        Items.RABBIT, Items.COOKED_RABBIT,
        Items.COD, Items.COOKED_COD,
        Items.SALMON, Items.COOKED_SALMON,
        Items.TROPICAL_FISH, Items.PUFFERFISH,
        Items.ROTTEN_FLESH
    );

    private static final double SEARCH_RADIUS = 20.0; // Search 20 blocks for meat
    private static final double PICKUP_DISTANCE = 1.5; // Distance to pick up item
    private static final double MOVE_SPEED = 1.3; // Wolf running speed
    private static final int HUNGRY_THRESHOLD = 50; // Pick up food when hunger < 50
    private static final int PACK_CHECK_RADIUS = 32; // Check pack members within 32 blocks
    private static final int COOLDOWN_TICKS = 60; // Cooldown after failing to pickup

    // Instance fields
    private final Wolf wolf;
    private final AnimalItemStorage storage;
    private ItemEntity targetItem;
    private Path currentPath;
    private int cooldownTicks;
    private int ticksSinceTargetCheck;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasHungryLastCheck = false;

    public WolfPickupItemGoal(Wolf wolf) {
        this.wolf = wolf;
        this.storage = AnimalItemStorage.get(wolf, STORAGE_KEY);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (wolf.level().isClientSide) {
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

        // Tamed wolves don't need to forage (player feeds them)
        if (wolf.isTame()) {
            return false;
        }

        // Check if we should pick up food (self is hungry OR pack member is hungry)
        if (!shouldPickupFood()) {
            return false;
        }

        // Find nearest meat item
        ticksSinceTargetCheck++;
        targetItem = findNearestMeat();

        // Debug: Log when we're looking for food
        if (targetItem != null) {
            debug("found meat item at distance " + wolf.position().distanceTo(targetItem.position()));
        } else if (ticksSinceTargetCheck % 100 == 0) {
            debug("no meat found nearby (hunger=" + getHungerLevel() + ")");
        }

        if (targetItem == null) {
            // Log every 5 seconds that we're looking for food
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
        wolf.getNavigation().stop();
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

        double distance = wolf.position().distanceTo(targetItem.position());

        // If far from item, keep moving
        if (distance > PICKUP_DISTANCE) {
            // Re-path if we're not moving or lost our path
            if (!wolf.getNavigation().isInProgress() ||
                currentPath == null ||
                !currentPath.canReach() ||
                targetItem.hasImpulse) { // Item moved, recalculate path

                moveToItem();
            }

            // Look at the item while approaching (re-check if still alive)
            if (targetItem != null && targetItem.isAlive()) {
                wolf.getLookControl().setLookAt(targetItem, 30.0f, 30.0f);
            }

            // Log progress every second
            if (wolf.tickCount % 20 == 0) {
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
     * Check if wolf should pick up food based on hunger or pack needs.
     */
    private boolean shouldPickupFood() {
        int hunger = getHungerLevel();
        boolean isSelfHungry = hunger < HUNGRY_THRESHOLD;

        // Log state change
        if (isSelfHungry != wasHungryLastCheck) {
            debug("hunger state changed: " + wasHungryLastCheck + " -> " + isSelfHungry + " (hunger=" + hunger + ")");
            wasHungryLastCheck = isSelfHungry;
        }

        if (isSelfHungry) {
            return true;
        }

        // Check if any pack member is hungry (altruistic behavior)
        return hasHungryPackMember();
    }

    /**
     * Check if any pack member is hungry.
     */
    private boolean hasHungryPackMember() {
        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(
            Wolf.class,
            wolf.getBoundingBox().inflate(PACK_CHECK_RADIUS)
        );

        for (Wolf other : nearbyWolves) {
            if (other.equals(wolf) || other.isTame()) {
                continue;
            }

            int otherHunger = getHungerLevel(other);
            if (otherHunger < HUNGRY_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find the nearest meat item entity.
     */
    private ItemEntity findNearestMeat() {
        List<ItemEntity> nearbyItems = wolf.level().getEntitiesOfClass(
            ItemEntity.class,
            wolf.getBoundingBox().inflate(SEARCH_RADIUS)
        );

        ItemEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        debug("found " + nearbyItems.size() + " items nearby");

        for (ItemEntity itemEntity : nearbyItems) {
            // Skip items that are too old (about to despawn) or not pickup-able
            if (itemEntity.getItem().isEmpty()) {
                continue;
            }

            // Note: NOT checking onGround() here because dropped items
            // might not immediately be "on the ground" when dropped
            // We'll let the pathfinding determine reachability instead

            ItemStack itemStack = itemEntity.getItem();
            boolean isMeat = isMeat(itemStack);
            debug("checking item: " + itemStack.getItem().toString() + " isMeat=" + isMeat);

            if (!isMeat) {
                continue;
            }

            // Check if we can reach this item
            Path path = wolf.getNavigation().createPath(itemEntity.blockPosition(), 0);
            boolean pathReachable = path != null && path.canReach();
            debug("path to " + itemEntity.blockPosition() + " reachable=" + pathReachable);

            if (!pathReachable) {
                continue; // Unreachable
            }

            double distance = wolf.position().distanceTo(itemEntity.position());
            if (distance < nearestDist) {
                nearestDist = distance;
                nearest = itemEntity;
            }
        }

        return nearest;
    }

    /**
     * Check if an item stack is meat.
     */
    private boolean isMeat(ItemStack stack) {
        // Try tag first, fallback to item list for gametest environment
        return stack.is(MEAT_TAG) || MEAT_ITEMS.contains(stack.getItem());
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

        PathNavigation navigation = wolf.getNavigation();
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
     * Get the current hunger level for this wolf.
     */
    private int getHungerLevel() {
        return getHungerLevel(wolf);
    }

    /**
     * Get the hunger level for a specific wolf.
     */
    private int getHungerLevel(Wolf wolf) {
        EcologyComponent component = getComponent(wolf);
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
     * Get the ecology component for this wolf.
     */
    private EcologyComponent getComponent() {
        return getComponent(wolf);
    }

    /**
     * Get the ecology component for a specific wolf.
     */
    private EcologyComponent getComponent(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
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
            String prefix = "[WolfPickupItem] Wolf #" + wolf.getId() + " ";
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
            wolf.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
