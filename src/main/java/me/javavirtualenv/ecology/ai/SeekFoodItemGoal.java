package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Generic goal for animals to seek and consume food items when hungry.
 * <p>
 * Animals will pathfind to dropped food items when their hunger falls below
 * the threshold, eat the food to restore hunger, then continue normal behavior.
 */
public class SeekFoodItemGoal extends Goal {

    // Configuration constants
    private static final int SEARCH_RADIUS = 16; // Search 16 blocks for food
    private static final double MOVE_SPEED = 1.0; // Default movement speed
    private static final int HUNGRY_THRESHOLD = 50; // Seek food when hunger < 50
    private static final int HUNGER_SATISFIED = 80; // Stop eating when hunger >= 80
    private static final int EAT_DURATION_TICKS = 20; // How long to eat (1 second)
    private static final int COOLDOWN_TICKS = 60; // Cooldown after eating
    private static final int HUNGER_RESTORE = 20; // Hunger restored per food item

    // Instance fields
    private final PathfinderMob mob;
    private final double moveSpeed;
    private final int searchRadius;
    private final Predicate<ItemStack> foodPredicate;
    private ItemEntity targetItem;
    private int eatTicks = 0;
    private int cooldownTicks = 0;
    private Path currentPath;

    // Debug info
    private String lastDebugMessage = "";
    private int ticksSinceLastCheck = 0;
    private boolean wasHungryLastCheck = false;

    public SeekFoodItemGoal(PathfinderMob mob, double moveSpeed, int searchRadius, Predicate<ItemStack> foodPredicate) {
        this.mob = mob;
        this.moveSpeed = moveSpeed;
        this.searchRadius = searchRadius;
        this.foodPredicate = foodPredicate;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public SeekFoodItemGoal(PathfinderMob mob, double moveSpeed, Predicate<ItemStack> foodPredicate) {
        this(mob, moveSpeed, SEARCH_RADIUS, foodPredicate);
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (mob.level().isClientSide) {
            return false;
        }

        // Cooldown prevents spamming the goal
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Check if animal is hungry (direct NBT check for reliability)
        int hunger = getHungerLevel();
        boolean isHungry = hunger < HUNGRY_THRESHOLD;

        ticksSinceLastCheck++;

        // Log state change for debugging
        if (isHungry != wasHungryLastCheck) {
            debug("hungry state changed: " + wasHungryLastCheck + " -> " + isHungry + " (hunger=" + hunger + ")");
            wasHungryLastCheck = isHungry;
        }

        if (!isHungry) {
            return false;
        }

        // Find nearest reachable food
        targetItem = findNearestReachableFood();
        if (targetItem == null) {
            debug("hungry but no reachable food found (hunger=" + hunger + ")");
            return false;
        }

        debug("STARTING: seeking food at " + targetItem.getX() + "," + targetItem.getY() + "," + targetItem.getZ() + " (hunger=" + hunger + ")");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (cooldownTicks > 0) {
            return false;
        }

        if (targetItem == null || !targetItem.isAlive()) {
            return false;
        }

        // Stop if we're no longer hungry
        int hunger = getHungerLevel();
        if (hunger >= HUNGER_SATISFIED) {
            debug("hunger satisfied (" + hunger + " >= " + HUNGER_SATISFIED + "), stopping");
            return false;
        }

        // Continue if we're still pathfinding or eating
        return mob.getNavigation().isInProgress() || eatTicks > 0;
    }

    @Override
    public void start() {
        debug("goal started, pathfinding to food");
        eatTicks = 0;
        moveToFood();
    }

    @Override
    public void stop() {
        debug("goal stopped (eatTicks=" + eatTicks + ", cooldown=" + cooldownTicks + ")");
        targetItem = null;
        eatTicks = 0;
        currentPath = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetItem == null || !targetItem.isAlive()) {
            return;
        }

        double distSq = mob.distanceToSqr(targetItem);

        // If far from food, keep moving
        if (distSq > 2.25) { // 1.5 blocks distance squared
            // Re-path if we're not moving or lost our path
            if (!mob.getNavigation().isInProgress() || currentPath == null || !currentPath.canReach()) {
                moveToFood();
            }
            // Re-check if still alive before looking
            if (targetItem != null && targetItem.isAlive()) {
                mob.getLookControl().setLookAt(targetItem);
            }
            return;
        }

        // We're at food, start eating
        eatTicks++;

        // Look at the food while eating (re-check if still alive)
        if (targetItem != null && targetItem.isAlive()) {
            mob.getLookControl().setLookAt(targetItem);
        }

        // Done eating, consume item and restore hunger
        if (eatTicks >= EAT_DURATION_TICKS) {
            consumeItem();
            eatTicks = 0;
            cooldownTicks = COOLDOWN_TICKS;
            targetItem = null;
            debug("finished eating, cooldown started");
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Get the current hunger level from NBT data.
     * @return hunger value (0-100), defaults to 100 if not set
     */
    private int getHungerLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100;
        }
        var tag = component.getHandleTag("hunger");
        return tag.contains("hunger") ? tag.getInt("hunger") : 100;
    }

    /**
     * Consume the target food item and restore hunger.
     */
    private void consumeItem() {
        if (targetItem == null || !targetItem.isAlive()) {
            return;
        }

        EcologyComponent component = getComponent();
        if (component == null) {
            return;
        }

        // Remove one item from the stack
        ItemStack stack = targetItem.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            targetItem.discard();
        }

        // Restore hunger
        int currentHunger = getHungerLevel();
        int newHunger = Math.min(100, currentHunger + HUNGER_RESTORE);

        var tag = component.getHandleTag("hunger");
        tag.putInt("hunger", newHunger);

        // Update the entity state flag
        component.state().setIsHungry(newHunger < HUNGRY_THRESHOLD);

        debug("hunger restored: " + currentHunger + " -> " + newHunger + " (+" + (newHunger - currentHunger) + ")");
    }

    /**
     * Move towards the food target.
     */
    private void moveToFood() {
        PathNavigation navigation = mob.getNavigation();
        currentPath = navigation.createPath(targetItem, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(targetItem, moveSpeed);
            debug("path found to food, distance=" + (int) Math.sqrt(mob.distanceToSqr(targetItem)) + " blocks");
        } else {
            debug("NO PATH to food at " + targetItem.getX() + "," + targetItem.getY() + "," + targetItem.getZ());
            targetItem = null; // Give up on this target
        }
    }

    /**
     * Find the nearest reachable food item within search radius using spiral search.
     */
    private ItemEntity findNearestReachableFood() {
        AABB searchBox = mob.getBoundingBox().inflate(searchRadius);
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, searchBox,
                item -> item.isAlive() && foodPredicate.test(item.getItem()));

        if (items.isEmpty()) {
            return null;
        }

        ItemEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        // Filter by reachability and find closest
        for (ItemEntity item : items) {
            Path path = mob.getNavigation().createPath(item, 0);
            if (path == null || !path.canReach()) {
                continue; // Unreachable
            }

            double dist = mob.distanceToSqr(item);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = item;
            }
        }

        return nearest;
    }

    /**
     * Get the ecology component for this mob.
     */
    private EcologyComponent getComponent() {
        if (!(mob instanceof EcologyAccess access)) {
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
            String prefix = "[SeekFood] " + getMobType() + " #" + mob.getId() + " ";
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
        return String.format("hunger=%d, target=%s, eating=%d, cooldown=%d, path=%s",
            getHungerLevel(),
            targetItem != null ? (int) targetItem.getX() + "," + (int) targetItem.getZ() : "none",
            eatTicks,
            cooldownTicks,
            mob.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }

    /**
     * Get a readable mob type name for logging.
     */
    private String getMobType() {
        return mob.getType().toShortString();
    }
}
