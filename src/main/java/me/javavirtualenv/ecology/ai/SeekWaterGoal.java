package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Generic goal for herbivores to seek and drink water when thirsty.
 * <p>
 * Animals will pathfind to the nearest water block when their thirst
 * falls below the threshold, drink to restore thirst, then continue
 * their normal behavior.
 */
public class SeekWaterGoal extends Goal {

    // Configuration constants
    private static final int SEARCH_RADIUS = 20; // Search 20 blocks for water
    private static final double MOVE_SPEED = 1.0; // Default movement speed
    private static final int THIRST_THRESHOLD = 30; // Seek water when thirst < 30
    private static final int THIRST_SATISFIED = 80; // Stop drinking when thirst >= 80
    private static final int DRINK_DURATION_TICKS = 40; // How long to drink (2 seconds)
    private static final int COOLDOWN_TICKS = 100; // Cooldown after drinking

    // Instance fields
    private final PathfinderMob mob;
    private final double moveSpeed;
    private final int searchRadius;
    private BlockPos targetWaterPos;
    private int drinkTicks = 0;
    private int cooldownTicks = 0;
    private Path currentPath;

    // Debug info
    private String lastDebugMessage = "";
    private int ticksSinceLastCheck = 0;
    private boolean wasThirstyLastCheck = false;

    public SeekWaterGoal(PathfinderMob mob, double moveSpeed, int searchRadius) {
        this.mob = mob;
        this.moveSpeed = moveSpeed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public SeekWaterGoal(PathfinderMob mob, double moveSpeed) {
        this(mob, moveSpeed, SEARCH_RADIUS);
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
            if (cooldownTicks % 20 == 0) {
                debug("on cooldown: " + cooldownTicks + " ticks remaining");
            }
            return false;
        }

        // Check if animal is thirsty (direct NBT check for reliability)
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

        // Find nearest reachable water
        targetWaterPos = findNearestReachableWater();
        if (targetWaterPos == null) {
            debug("thirsty but no reachable water found (thirst=" + thirst + ")");
            return false;
        }

        debug("STARTING: seeking water at " + targetWaterPos.getX() + "," + targetWaterPos.getY() + "," + targetWaterPos.getZ() + " (thirst=" + thirst + ")");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (cooldownTicks > 0) {
            return false;
        }

        if (targetWaterPos == null) {
            return false;
        }

        // Stop if we're no longer thirsty
        int thirst = getThirstLevel();
        if (thirst >= THIRST_SATISFIED) {
            debug("thirst satisfied (" + thirst + " >= " + THIRST_SATISFIED + "), stopping");
            return false;
        }

        // Continue if we're still pathfinding or drinking
        return mob.getNavigation().isInProgress() || drinkTicks > 0;
    }

    @Override
    public void start() {
        debug("goal started, pathfinding to water");
        drinkTicks = 0;
        moveToWater();
    }

    @Override
    public void stop() {
        debug("goal stopped (drinkTicks=" + drinkTicks + ", cooldown=" + cooldownTicks + ")");
        targetWaterPos = null;
        drinkTicks = 0;
        currentPath = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetWaterPos == null) {
            return;
        }

        double distSq = mob.distanceToSqr(targetWaterPos.getX() + 0.5, targetWaterPos.getY(), targetWaterPos.getZ() + 0.5);

        // If far from water, keep moving
        if (distSq > 4.0) { // 2 blocks distance squared
            // Re-path if we're not moving or lost our path
            if (!mob.getNavigation().isInProgress() || currentPath == null || !currentPath.canReach()) {
                debug("re-pathfinding to water (dist=" + (int) Math.sqrt(distSq) + " blocks)");
                moveToWater();
            }
            mob.getLookControl().setLookAt(targetWaterPos.getX() + 0.5, targetWaterPos.getY(), targetWaterPos.getZ() + 0.5);
            return;
        }

        // We're at water, start drinking
        drinkTicks++;

        // Look at the water while drinking
        mob.getLookControl().setLookAt(targetWaterPos.getX() + 0.5, targetWaterPos.getY(), targetWaterPos.getZ() + 0.5);

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

        debug("thirst restored: " + currentThirst + " -> " + newThirst + " (+" + (newThirst - currentThirst) + ")");
    }

    /**
     * Move towards the water target.
     */
    private void moveToWater() {
        PathNavigation navigation = mob.getNavigation();
        currentPath = navigation.createPath(targetWaterPos, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(targetWaterPos.getX() + 0.5, targetWaterPos.getY(), targetWaterPos.getZ() + 0.5, moveSpeed);
            debug("path found to water, distance=" + currentPath.getNodeCount() + " nodes");
        } else {
            debug("NO PATH to water at " + targetWaterPos);
            targetWaterPos = null; // Give up on this target
        }
    }

    /**
     * Find the nearest reachable water block within search radius using spiral search.
     */
    private BlockPos findNearestReachableWater() {
        Level level = mob.level();
        BlockPos mobPos = mob.blockPosition();

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        // Search in a spiral pattern (find closer water first)
        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos pos = mobPos.offset(dx, dy, dz);

                        // Skip if not water
                        if (!level.getBlockState(pos).is(Blocks.WATER)) {
                            continue;
                        }

                        // Check if we can reach this water
                        Path path = mob.getNavigation().createPath(pos, 0);
                        if (path == null || !path.canReach()) {
                            continue; // Unreachable
                        }

                        double dist = mobPos.distSqr(pos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }

            // If we found water at this radius, use it (closer is better)
            if (nearest != null) {
                break;
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
            String prefix = "[SeekWater] " + getMobType() + " #" + mob.getId() + " ";
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
        return String.format("thirst=%d, target=%s, drinking=%d, cooldown=%d, path=%s",
            getThirstLevel(),
            targetWaterPos != null ? targetWaterPos.getX() + "," + targetWaterPos.getZ() : "none",
            drinkTicks,
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
