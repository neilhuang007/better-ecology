package me.javavirtualenv.behavior.foraging;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.handles.HungerHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Foraging behavior that searches for and approaches food resources.
 * Implements Optimal Foraging Theory principles for efficient food acquisition.
 */
public class ForagingBehavior extends SteeringBehavior {
    private final double searchRadius;
    private final int searchInterval;
    private final List<Block> targetFoodBlocks;
    private final FoodMemory foodMemory;
    private final int hungerRestore;

    private ForagingState state;
    private BlockPos currentTarget;
    private int ticksSinceLastSearch;
    private int ticksEating;

    private static final double MAX_SPEED = 0.3;
    private static final double SLOWING_RADIUS = 3.0;
    private static final int EATING_DURATION = 40;

    public ForagingBehavior(double searchRadius, int searchInterval, List<Block> targetFoodBlocks, int memoryDuration, int hungerRestore) {
        this.searchRadius = searchRadius;
        this.searchInterval = searchInterval;
        this.targetFoodBlocks = new ArrayList<>(targetFoodBlocks);
        this.foodMemory = new FoodMemory(memoryDuration);
        this.hungerRestore = hungerRestore;
        this.state = ForagingState.SEARCHING;
        this.currentTarget = null;
        this.ticksSinceLastSearch = 0;
        this.ticksEating = 0;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        updateState(context);

        switch (state) {
            case SEARCHING:
                return handleSearching(context);
            case APPROACHING:
                return handleApproaching(context);
            case EATING:
                return handleEating(context);
            default:
                return new Vec3d();
        }
    }

    private void updateState(BehaviorContext context) {
        ticksSinceLastSearch++;
        foodMemory.forgetOldPatches();

        if (state == ForagingState.EATING) {
            ticksEating++;
            if (ticksEating >= EATING_DURATION) {
                consumeFood(context, currentTarget);
                state = ForagingState.SEARCHING;
                currentTarget = null;
                ticksEating = 0;
            }
            return;
        }

        if (state == ForagingState.APPROACHING && currentTarget != null) {
            BlockPos entityPos = context.getBlockPos();
            if (entityPos.distSqr(currentTarget) < 4.0) {
                state = ForagingState.EATING;
                ticksEating = 0;
            }
        }

        if (ticksSinceLastSearch >= searchInterval || currentTarget == null) {
            searchForFood(context);
            ticksSinceLastSearch = 0;
        }
    }

    private Vec3d handleSearching(BehaviorContext context) {
        if (currentTarget != null) {
            state = ForagingState.APPROACHING;
            return handleApproaching(context);
        }
        return new Vec3d();
    }

    private Vec3d handleApproaching(BehaviorContext context) {
        if (currentTarget == null) {
            state = ForagingState.SEARCHING;
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            currentTarget.getX() + 0.5,
            currentTarget.getY(),
            currentTarget.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, MAX_SPEED, SLOWING_RADIUS);
    }

    private Vec3d handleEating(BehaviorContext context) {
        return new Vec3d();
    }

    private void searchForFood(BehaviorContext context) {
        BlockPos bestMemory = foodMemory.getBestPatch();
        if (bestMemory != null && context.getBlockPos().distSqr(bestMemory) < searchRadius * searchRadius) {
            currentTarget = bestMemory;
            state = ForagingState.APPROACHING;
            return;
        }

        BlockPos nearest = findNearestFood(context.getLevel(), context.getBlockPos());
        if (nearest != null) {
            currentTarget = nearest;
            foodMemory.rememberPatch(nearest, estimateFoodAtLocation(context.getLevel(), nearest));
            state = ForagingState.APPROACHING;
        } else {
            currentTarget = null;
            state = ForagingState.SEARCHING;
        }
    }

    public BlockPos findNearestFood(Level level, BlockPos center) {
        BlockPos nearest = null;
        double minDistSquared = Double.MAX_VALUE;
        int radius = (int) searchRadius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distSquared = center.distSqr(pos);

                    if (distSquared < minDistSquared && distSquared <= searchRadius * searchRadius) {
                        BlockState state = level.getBlockState(pos);
                        if (isFoodBlock(state.getBlock())) {
                            nearest = pos;
                            minDistSquared = distSquared;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    public void consumeFood(BehaviorContext context, BlockPos pos) {
        if (pos == null) {
            return;
        }

        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (block == Blocks.TALL_GRASS || block == Blocks.SHORT_GRASS) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            level.destroyBlock(pos, false);
        }

        foodMemory.consumeFromPatch(pos, 1);

        // Restore hunger when food is consumed
        HungerHandle.restoreHunger(context.getEntity(), hungerRestore);
    }

    private boolean isFoodBlock(Block block) {
        return targetFoodBlocks.contains(block);
    }

    private int estimateFoodAtLocation(Level level, BlockPos center) {
        int count = 0;
        int radius = 3;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (isFoodBlock(state.getBlock())) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public ForagingState getState() {
        return state;
    }

    public BlockPos getCurrentTarget() {
        return currentTarget;
    }

    public FoodMemory getFoodMemory() {
        return foodMemory;
    }

    public enum ForagingState {
        SEARCHING,
        APPROACHING,
        EATING
    }
}
