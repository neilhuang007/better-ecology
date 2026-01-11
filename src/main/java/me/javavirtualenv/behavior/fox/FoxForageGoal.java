package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal for foxes to seek out and eat sweet berries from bushes.
 * <p>
 * Foxes will forage for berries when:
 * <ul>
 *   <li>They are hungry (hunger below threshold)</li>
 *   <li>A sweet berry bush is reachable</li>
 * </ul>
 * <p>
 * Foxes prefer bushes with more berries and will eat from the bush
 * to restore hunger.
 */
public class FoxForageGoal extends Goal {

    // Configuration constants
    private static final int SEARCH_RADIUS = 24;
    private static final double MOVE_SPEED = 1.0;
    private static final double EAT_DISTANCE = 1.5;
    private static final int HUNGRY_THRESHOLD = 70;
    private static final int EAT_DURATION_TICKS = 40;
    private static final int COOLDOWN_TICKS = 200;

    // Instance fields
    private final Fox fox;
    private BlockPos targetBush;
    private int eatTicks;
    private int cooldownTicks;
    private Path currentPath;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasHungryLastCheck = false;

    public FoxForageGoal(Fox fox) {
        this.fox = fox;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Legacy constructor for backward compatibility.
     * @param fox The fox (must be Fox type)
     * @param behavior Ignored (behavior is now internal)
     * @param searchRadius Ignored (uses internal SEARCH_RADIUS)
     */
    public FoxForageGoal(net.minecraft.world.entity.PathfinderMob fox, Object behavior, int searchRadius) {
        this((Fox) fox);
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (fox.level().isClientSide) {
            return false;
        }

        // Cooldown prevents spamming the goal
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Don't forage if sleeping
        if (fox.isSleeping()) {
            return false;
        }

        // Check if fox is hungry (direct NBT check for reliability)
        int hunger = getHungerLevel();
        boolean isHungry = hunger < HUNGRY_THRESHOLD;

        // Log state change for debugging
        if (isHungry != wasHungryLastCheck) {
            debug("hunger state changed: " + wasHungryLastCheck + " -> " + isHungry + " (hunger=" + hunger + ")");
            wasHungryLastCheck = isHungry;
        }

        if (!isHungry) {
            return false;
        }

        // Find nearest berry bush
        targetBush = findNearestReachableBush();
        if (targetBush == null) {
            debug("hungry but no reachable berry bush found (hunger=" + hunger + ")");
            return false;
        }

        debug("STARTING: seeking berries at " + targetBush.getX() + "," + targetBush.getY() + "," + targetBush.getZ() + " (hunger=" + hunger + ")");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (cooldownTicks > 0) {
            return false;
        }

        if (targetBush == null) {
            return false;
        }

        // Stop if we're no longer hungry
        int hunger = getHungerLevel();
        if (hunger >= 90) {
            debug("hunger satisfied (" + hunger + "), stopping");
            return false;
        }

        // Continue if we're still pathfinding or eating
        return fox.getNavigation().isInProgress() || eatTicks > 0;
    }

    @Override
    public void start() {
        debug("goal started, pathfinding to berry bush");
        eatTicks = 0;
        moveToBush();
    }

    @Override
    public void stop() {
        debug("goal stopped (eatTicks=" + eatTicks + ", cooldown=" + cooldownTicks + ")");
        targetBush = null;
        eatTicks = 0;
        currentPath = null;
        fox.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetBush == null) {
            return;
        }

        // Check if bush still has berries
        Level level = fox.level();
        BlockState blockState = level.getBlockState(targetBush);
        if (!hasBerries(blockState)) {
            debug("bush no longer has berries");
            targetBush = null;
            return;
        }

        double distSq = fox.distanceToSqr(targetBush.getX() + 0.5, targetBush.getY(), targetBush.getZ() + 0.5);

        // If far from bush, keep moving
        if (distSq > EAT_DISTANCE * EAT_DISTANCE) {
            // Re-path if we're not moving or lost our path
            if (!fox.getNavigation().isInProgress() || currentPath == null || !currentPath.canReach()) {
                moveToBush();
            }
            fox.getLookControl().setLookAt(targetBush.getX() + 0.5, targetBush.getY() + 0.5, targetBush.getZ() + 0.5);
            return;
        }

        // We're at bush, start eating
        eatTicks++;

        // Look at the bush while eating
        fox.getLookControl().setLookAt(targetBush.getX() + 0.5, targetBush.getY() + 0.5, targetBush.getZ() + 0.5);

        // Every second, log eating progress
        if (eatTicks % 20 == 0) {
            debug("eating berries... (" + eatTicks + "/" + EAT_DURATION_TICKS + ", hunger=" + getHungerLevel() + ")");
        }

        // Eat a berry every tick
        if (eatTicks % 10 == 0) {
            eatBerry(level, blockState);
        }

        // Done eating
        if (eatTicks >= EAT_DURATION_TICKS) {
            restoreHunger();
            eatTicks = 0;
            cooldownTicks = COOLDOWN_TICKS;
            targetBush = null;
            debug("finished eating, cooldown started");
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Get the current hunger level from NBT data.
     */
    private int getHungerLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100;
        }
        CompoundTag tag = component.getHandleTag("hunger");
        return tag.contains("hunger") ? tag.getInt("hunger") : 100;
    }

    /**
     * Restore hunger when eating completes.
     */
    private void restoreHunger() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return;
        }

        int currentHunger = getHungerLevel();
        int restoreAmount = 25; // Restore significant amount from berries
        int newHunger = Math.min(100, currentHunger + restoreAmount);

        CompoundTag tag = component.getHandleTag("hunger");
        tag.putInt("hunger", newHunger);

        debug("hunger restored: " + currentHunger + " -> " + newHunger + " (+ " + (newHunger - currentHunger) + ")");
    }

    /**
     * Eat a berry from the bush.
     */
    private void eatBerry(Level level, BlockState blockState) {
        IntegerProperty ageProperty = SweetBerryBushBlock.AGE;
        int currentAge = blockState.getValue(ageProperty);

        if (currentAge > 0) {
            // Reduce berry stage
            BlockState newState = blockState.setValue(ageProperty, currentAge - 1);
            level.setBlock(targetBush, newState, 3);

            // Play eating sound
            playEatSound();
        }
    }

    /**
     * Move towards the berry bush.
     */
    private void moveToBush() {
        PathNavigation navigation = fox.getNavigation();
        currentPath = navigation.createPath(targetBush, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(targetBush.getX() + 0.5, targetBush.getY(), targetBush.getZ() + 0.5, MOVE_SPEED);
            debug("path found to bush, distance=" + currentPath.getNodeCount() + " nodes");
        } else {
            debug("NO PATH to bush at " + targetBush);
            targetBush = null; // Give up on this target
        }
    }

    /**
     * Find the nearest reachable sweet berry bush within search radius.
     */
    private BlockPos findNearestReachableBush() {
        Level level = fox.level();
        BlockPos foxPos = fox.blockPosition();

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        // Search in expanding radius
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos pos = foxPos.offset(dx, dy, dz);

                        // Skip if not sweet berry bush
                        BlockState blockState = level.getBlockState(pos);
                        if (!blockState.is(Blocks.SWEET_BERRY_BUSH)) {
                            continue;
                        }

                        // Skip if no berries
                        if (!hasBerries(blockState)) {
                            continue;
                        }

                        // Check if we can reach this bush
                        Path path = fox.getNavigation().createPath(pos, 0);
                        if (path == null || !path.canReach()) {
                            continue; // Unreachable
                        }

                        // Prioritize bushes with more berries
                        int berryStage = blockState.getValue(SweetBerryBushBlock.AGE);
                        double dist = foxPos.distSqr(pos);
                        double adjustedDist = dist - (berryStage * 4.0);

                        if (adjustedDist < nearestDist) {
                            nearestDist = adjustedDist;
                            nearest = pos;
                        }
                    }
                }
            }

            // If we found bush at this radius, use it
            if (nearest != null) {
                break;
            }
        }

        return nearest;
    }

    /**
     * Check if a berry bush has berries.
     */
    private boolean hasBerries(BlockState blockState) {
        if (!blockState.is(Blocks.SWEET_BERRY_BUSH)) {
            return false;
        }
        int age = blockState.getValue(SweetBerryBushBlock.AGE);
        return age > 0;
    }

    /**
     * Play eating sound.
     */
    private void playEatSound() {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_EAT,
            SoundSource.NEUTRAL, 0.8f, 1.0f);
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
            String prefix = "[FoxForage] Fox #" + fox.getId() + " ";
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
            targetBush != null ? targetBush.getX() + "," + targetBush.getZ() : "none",
            eatTicks,
            cooldownTicks,
            fox.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
