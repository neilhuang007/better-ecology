package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal for wolves to seek and drink water when thirsty.
 * <p>
 * Wolves will pathfind to the nearest water block when their thirst
 * falls below the threshold, drink to restore thirst, then continue
 * their normal behavior.
 */
public class WolfDrinkWaterGoal extends Goal {

    // Configuration constants
    private static final int SEARCH_RADIUS = 20; // Search 20 blocks for water
    private static final double MOVE_SPEED = 1.3; // Wolf running speed
    private static final int THIRST_THRESHOLD = 30; // Seek water when thirst < 30
    private static final int THIRST_SATISFIED = 80; // Stop drinking when thirst >= 80
    private static final int DRINK_DURATION_TICKS = 40; // How long to drink (2 seconds)
    private static final int COOLDOWN_TICKS = 100; // Cooldown after drinking

    // Instance fields
    private final Wolf wolf;
    private BlockPos targetWaterPos;      // The actual water block to look at
    private BlockPos drinkingPosition;    // Where the wolf stands to drink (adjacent to water)
    private int drinkTicks = 0;
    private int cooldownTicks = 0;
    private Path currentPath;

    // Debug info
    private String lastDebugMessage = "";
    private int ticksSinceLastCheck = 0;
    private boolean wasThirstyLastCheck = false;

    public WolfDrinkWaterGoal(Wolf wolf) {
        this.wolf = wolf;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (wolf.level().isClientSide) {
            return false;
        }

        // Cooldown prevents spamming the goal
        if (cooldownTicks > 0) {
            cooldownTicks--;
            if (cooldownTicks % 20 == 0) {
                debug("on cooldown: " + cooldownTicks + " ticks remaining");
            }
            return false;
        }

        // Check if wolf is thirsty (direct NBT check for reliability)
        int thirst = getThirstLevel();
        boolean isThirsty = thirst < THIRST_THRESHOLD;

        ticksSinceLastCheck++;

        // Log state change for debugging
        if (isThirsty != wasThirstyLastCheck) {
            debug("thirsty state changed: " + wasThirstyLastCheck + " -> " + isThirsty + " (thirst=" + thirst + ")");
            wasThirstyLastCheck = isThirsty;
        }

        if (!isThirsty) {
            // Every 5 seconds, log that we're not thirsty
            if (ticksSinceLastCheck % 100 == 0) {
                debug("not thirsty (thirst=" + thirst + ", threshold=" + THIRST_THRESHOLD + ")");
            }
            return false;
        }

        // Tamed wolves don't need to seek water (player feeds them)
        if (wolf.isTame()) {
            debug("tamed wolf, skipping water seeking");
            return false;
        }

        // Find nearest water - this sets both drinkingPosition and targetWaterPos
        targetWaterPos = null;
        drinkingPosition = findNearestReachableWater();
        if (drinkingPosition == null) {
            debug("thirsty but no reachable water found (thirst=" + thirst + ")");
            return false;
        }

        debug("STARTING: seeking drinking spot at " + drinkingPosition.getX() + "," + drinkingPosition.getY() + "," + drinkingPosition.getZ() +
              " (water at " + targetWaterPos.getX() + "," + targetWaterPos.getY() + "," + targetWaterPos.getZ() + ", thirst=" + thirst + ")");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (cooldownTicks > 0) {
            return false;
        }

        if (drinkingPosition == null) {
            return false;
        }

        // Stop if we're no longer thirsty
        int thirst = getThirstLevel();
        if (thirst >= THIRST_SATISFIED) {
            debug("thirst satisfied (" + thirst + " >= " + THIRST_SATISFIED + "), stopping");
            return false;
        }

        // Continue if we're still pathfinding or drinking
        return wolf.getNavigation().isInProgress() || drinkTicks > 0;
    }

    @Override
    public void start() {
        debug("goal started, pathfinding to drinking position");
        drinkTicks = 0;
        moveToWater();
    }

    @Override
    public void stop() {
        debug("goal stopped (drinkTicks=" + drinkTicks + ", cooldown=" + cooldownTicks + ")");
        targetWaterPos = null;
        drinkingPosition = null;
        drinkTicks = 0;
        currentPath = null;
        wolf.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (drinkingPosition == null) {
            return;
        }

        // Calculate distance to drinking position (where wolf should stand)
        double distSq = wolf.distanceToSqr(drinkingPosition.getX() + 0.5, drinkingPosition.getY(), drinkingPosition.getZ() + 0.5);

        // If far from drinking position, keep moving
        if (distSq > 4.0) { // 2 blocks distance squared
            // Re-path if we're not moving or lost our path
            if (!wolf.getNavigation().isInProgress() || currentPath == null || !currentPath.canReach()) {
                debug("re-pathfinding to drinking spot (dist=" + (int) Math.sqrt(distSq) + " blocks)");
                moveToWater();
            }
            // Look at water while moving
            if (targetWaterPos != null) {
                wolf.getLookControl().setLookAt(targetWaterPos.getX() + 0.5, targetWaterPos.getY(), targetWaterPos.getZ() + 0.5);
            }
            return;
        }

        // We're at drinking position, start drinking
        drinkTicks++;

        // Look at the water while drinking
        if (targetWaterPos != null) {
            wolf.getLookControl().setLookAt(targetWaterPos.getX() + 0.5, targetWaterPos.getY(), targetWaterPos.getZ() + 0.5);
        }

        // Every second, log drinking progress
        if (drinkTicks % 20 == 0) {
            debug("drinking... (" + drinkTicks + "/" + DRINK_DURATION_TICKS + ", thirst=" + getThirstLevel() + ")");
        }

        // Done drinking, restore thirst
        if (drinkTicks >= DRINK_DURATION_TICKS) {
            restoreThirst();
            drinkTicks = 0;
            cooldownTicks = COOLDOWN_TICKS;
            targetWaterPos = null;
            drinkingPosition = null;
            debug("finished drinking, cooldown started");
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Get the current thirst level from NBT data.
     * @return thirst value (0-100), defaults to 100 if not set
     */
    private int getThirstLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            // CRITICAL: Component is null - this means the mixin failed to apply
            // or EcologyAccess interface is not implemented on this wolf
            BehaviorLogger.info("[WolfDrinkWater] Wolf #" + wolf.getId() +
                " has NULL EcologyComponent! Mixin may not have applied. " +
                "Wolf class: " + wolf.getClass().getName() +
                ", implements EcologyAccess: " + (wolf instanceof EcologyAccess));
            return 100;
        }
        var tag = component.getHandleTag("thirst");
        return tag.contains("thirst") ? tag.getInt("thirst") : 100;
    }

    /**
     * Restore thirst when drinking completes.
     */
    private void restoreThirst() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return;
        }

        int currentThirst = getThirstLevel();
        int restoreAmount = THIRST_SATISFIED - currentThirst + 10; // Restore to satisfied + buffer
        int newThirst = Math.min(100, currentThirst + restoreAmount);

        var tag = component.getHandleTag("thirst");
        tag.putInt("thirst", newThirst);

        // Update the entity state flag
        component.state().setIsThirsty(newThirst < THIRST_THRESHOLD);

        debug("thirst restored: " + currentThirst + " -> " + newThirst + " (+ " + (newThirst - currentThirst) + ")");
    }

    /**
     * Move towards the drinking position (adjacent to water).
     */
    private void moveToWater() {
        if (drinkingPosition == null) {
            debug("moveToWater called but drinkingPosition is null");
            return;
        }

        PathNavigation navigation = wolf.getNavigation();
        currentPath = navigation.createPath(drinkingPosition, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(drinkingPosition.getX() + 0.5, drinkingPosition.getY(), drinkingPosition.getZ() + 0.5, MOVE_SPEED);
            debug("path found to drinking spot, distance=" + currentPath.getNodeCount() + " nodes");
        } else {
            debug("NO PATH to drinking spot at " + drinkingPosition);
            drinkingPosition = null; // Give up on this target
            targetWaterPos = null;
        }
    }

    /**
     * Find the nearest reachable water block within search radius.
     * Returns a position ADJACENT to water that the wolf can stand on to drink.
     */
    private BlockPos findNearestReachableWater() {
        Level level = wolf.level();
        BlockPos wolfPos = wolf.blockPosition();

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        // Search in a spiral pattern (find closer water first)
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos waterPos = wolfPos.offset(dx, dy, dz);

                        // Skip if not water
                        if (!level.getBlockState(waterPos).is(Blocks.WATER)) {
                            continue;
                        }

                        // Find a solid block adjacent to water that the wolf can stand on
                        BlockPos drinkPos = findDrinkingPosition(level, waterPos);
                        if (drinkPos == null) {
                            continue; // No accessible drinking spot near this water
                        }

                        // Check if we can reach the drinking position
                        Path path = wolf.getNavigation().createPath(drinkPos, 0);
                        if (path == null || !path.canReach()) {
                            continue; // Unreachable
                        }

                        double dist = wolfPos.distSqr(drinkPos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = drinkPos;
                            // Store water position for looking at while drinking
                            targetWaterPos = waterPos;
                        }
                    }
                }
            }

            // If we found water at this radius, use it (closer is better)
            if (nearest != null) {
                // Return the drinking position (adjacent to water)
                BlockPos drinkPos = nearest;
                debug("found drinking spot at " + drinkPos.getX() + "," + drinkPos.getY() + "," + drinkPos.getZ() +
                    " (water at " + targetWaterPos.getX() + "," + targetWaterPos.getY() + "," + targetWaterPos.getZ() + ")");
                return drinkPos;
            }
        }

        return null;
    }

    /**
     * Find a solid block adjacent to water where the wolf can stand to drink.
     * Returns null if no valid drinking position exists.
     */
    private BlockPos findDrinkingPosition(Level level, BlockPos waterPos) {
        // Check all 4 horizontal directions for a solid block next to water
        BlockPos[] adjacentPositions = {
            waterPos.north(),
            waterPos.south(),
            waterPos.east(),
            waterPos.west()
        };

        for (BlockPos adjacent : adjacentPositions) {
            // Check if there's a solid block below (ground to stand on)
            BlockPos groundPos = adjacent.below();
            if (!level.getBlockState(groundPos).isSolid()) {
                continue; // No ground to stand on
            }

            // Check if the adjacent position is air (wolf can stand there)
            if (!level.getBlockState(adjacent).isAir()) {
                continue; // Blocked
            }

            // Check if there's headroom (wolf needs 2 blocks of space)
            if (!level.getBlockState(adjacent.above()).isAir()) {
                continue; // No headroom
            }

            return adjacent;
        }

        // Also check one block up from water (for water at ground level)
        BlockPos aboveWater = waterPos.above();
        for (BlockPos adjacent : adjacentPositions) {
            BlockPos adjacentUp = adjacent.above();
            BlockPos groundPos = adjacent;

            if (!level.getBlockState(groundPos).isSolid()) {
                continue;
            }

            if (!level.getBlockState(adjacentUp).isAir()) {
                continue;
            }

            if (!level.getBlockState(adjacentUp.above()).isAir()) {
                continue;
            }

            return adjacentUp;
        }

        return null;
    }

    /**
     * Get the ecology component for this wolf.
     */
    private EcologyComponent getComponent() {
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
            String prefix = "[WolfDrinkWater] Wolf #" + wolf.getId() + " ";
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
        return String.format("thirst=%d, drinkPos=%s, water=%s, drinking=%d, cooldown=%d, path=%s",
            getThirstLevel(),
            drinkingPosition != null ? drinkingPosition.getX() + "," + drinkingPosition.getZ() : "none",
            targetWaterPos != null ? targetWaterPos.getX() + "," + targetWaterPos.getZ() : "none",
            drinkTicks,
            cooldownTicks,
            wolf.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
