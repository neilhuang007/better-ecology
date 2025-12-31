package me.javavirtualenv.behavior.rabbit;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.HungerHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Goal for rabbits to cache food in their burrows.
 * <p>
 * Rabbits will:
 * - Collect extra food when they find it
 * - Bring it back to their burrow for storage
 * - Store food for winter or scarce times
 * - Prioritize caching in snow biomes
 */
public class RabbitBurrowCachingGoal extends Goal {

    private final Mob mob;
    private final BurrowSystem burrowSystem;
    private final EcologyComponent component;

    private BlockPos targetFoodPos;
    private RabbitBurrow targetBurrow;
    private int cacheCooldown = 0;

    // State
    private enum State {
        IDLE,
        SEEKING_FOOD,
        MOVING_TO_FOOD,
        MOVING_TO_BURROW
    }

    private State currentState = State.IDLE;

    public RabbitBurrowCachingGoal(Mob mob, EcologyComponent component) {
        this.mob = mob;
        this.component = component;
        this.burrowSystem = BurrowSystem.get(mob.level());
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only cache if not currently fleeing
        if (cacheCooldown > 0) {
            cacheCooldown--;
            return false;
        }

        // Check if rabbit should cache food
        return shouldCacheFood();
    }

    @Override
    public boolean canContinueToUse() {
        return targetFoodPos != null || targetBurrow != null;
    }

    @Override
    public void start() {
        currentState = State.SEEKING_FOOD;
        targetFoodPos = null;
        targetBurrow = null;
    }

    @Override
    public void stop() {
        currentState = State.IDLE;
        targetFoodPos = null;
        targetBurrow = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        switch (currentState) {
            case SEEKING_FOOD:
                findNearbyFood();
                break;
            case MOVING_TO_FOOD:
                moveToFood();
                break;
            case MOVING_TO_BURROW:
                moveToBurrow();
                break;
        }
    }

    /**
     * Determines if rabbit should cache food.
     */
    private boolean shouldCacheFood() {
        Level level = mob.level();

        // Always cache in snow biomes
        BlockPos pos = mob.blockPosition();
        BlockState feetState = level.getBlockState(pos);
        boolean isSnowy = feetState.is(Blocks.SNOW);

        if (isSnowy) {
            return true;
        }

        // Cache if hunger is high (planning ahead)
        HungerHandle hungerHandle = component.getHandle(HungerHandle.class);
        if (hungerHandle != null) {
            int hunger = hungerHandle.getHunger(mob);
            // Cache if not starving but could use food later
            if (hunger > 30 && hunger < 60) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds nearby food to cache.
     */
    private void findNearbyFood() {
        Level level = mob.level();
        BlockPos pos = mob.blockPosition();
        int searchRadius = 8;

        // Find nearest burrow first
        targetBurrow = burrowSystem.findNearestBurrow(pos, 32.0);

        if (targetBurrow == null || !targetBurrow.hasFoodStorageSpace()) {
            // No burrow or full, don't cache
            stop();
            return;
        }

        // Find nearby food
        BlockPos nearestFood = null;
        double nearestDist = searchRadius;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (isCacheableFood(state)) {
                        double dist = pos.distSqr(checkPos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearestFood = checkPos;
                        }
                    }
                }
            }
        }

        if (nearestFood != null) {
            targetFoodPos = nearestFood;
            currentState = State.MOVING_TO_FOOD;
        } else {
            // No food found, wait before trying again
            cacheCooldown = 200; // 10 seconds
            stop();
        }
    }

    /**
     * Moves to collect food.
     */
    private void moveToFood() {
        if (targetFoodPos == null) {
            currentState = State.SEEKING_FOOD;
            return;
        }

        double dist = mob.blockPosition().distSqr(targetFoodPos);

        if (dist < 2.25) { // Within 1.5 blocks
            // Collect the food
            collectFood();
            currentState = State.MOVING_TO_BURROW;
        } else {
            // Move toward food
            mob.getNavigation().moveTo(targetFoodPos.getX(), targetFoodPos.getY(),
                                       targetFoodPos.getZ(), 1.0);
        }
    }

    /**
     * Moves to burrow to store food.
     */
    private void moveToBurrow() {
        if (targetBurrow == null) {
            stop();
            return;
        }

        BlockPos burrowPos = targetBurrow.getPosition();
        double dist = mob.blockPosition().distSqr(burrowPos);

        if (dist < 4.0) { // Within 2 blocks
            // Store the food
            storeFoodInBurrow();
            stop();
        } else {
            // Move toward burrow
            mob.getNavigation().moveTo(burrowPos.getX(), burrowPos.getY(),
                                       burrowPos.getZ(), 1.0);
        }
    }

    /**
     * Collects food from the world.
     */
    private void collectFood() {
        if (targetFoodPos == null) {
            return;
        }

        Level level = mob.level();
        BlockState state = level.getBlockState(targetFoodPos);

        if (isCacheableFood(state)) {
            // Remove the food block
            level.destroyBlock(targetFoodPos, false);
        }
    }

    /**
     * Stores collected food in the burrow.
     */
    private void storeFoodInBurrow() {
        if (targetBurrow == null || targetFoodPos == null) {
            return;
        }

        // Determine what food was collected
        // For simplicity, assume carrot (can be enhanced to track actual item)
        String foodItem = "minecraft:carrot";

        // Store in burrow
        int stored = targetBurrow.storeFood(foodItem, 1);

        if (stored > 0) {
            // Success! Set cooldown before caching again
            cacheCooldown = 600; // 30 seconds
        }
    }

    /**
     * Checks if a block is cacheable food.
     */
    private boolean isCacheableFood(BlockState state) {
        return state.is(Blocks.CARROTS) ||
               state.is(Blocks.POTATOES) ||
               state.is(Blocks.WHEAT) ||
               state.is(Blocks.BEETROOTS) ||
               state.is(Blocks.DANDELION);
    }
}
