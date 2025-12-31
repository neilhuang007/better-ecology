package me.javavirtualenv.behavior.chicken;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Egg laying behavior for chickens.
 * Chickens seek out appropriate nesting locations to lay eggs.
 * They prefer hay blocks, grass, and soft materials.
 */
public class EggLayingBehavior extends SteeringBehavior {
    private final double searchRadius;
    private final int searchInterval;
    private final List<Block> preferredNestingBlocks;
    private final int nestingDuration;

    private NestingState state;
    private BlockPos currentNest;
    private int ticksSinceLastSearch;
    private int ticksAtNest;
    private boolean readyToLay;

    private static final double APPROACH_SPEED = 0.25;
    private static final double SLOWING_RADIUS = 2.0;

    public EggLayingBehavior(double searchRadius, int searchInterval,
                            List<Block> preferredNestingBlocks, int nestingDuration) {
        this.searchRadius = searchRadius;
        this.searchInterval = searchInterval;
        this.preferredNestingBlocks = new ArrayList<>(preferredNestingBlocks);
        this.nestingDuration = nestingDuration;
        this.state = NestingState.NOT_READY;
        this.currentNest = null;
        this.ticksSinceLastSearch = 0;
        this.ticksAtNest = 0;
        this.readyToLay = false;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        updateState(context);

        switch (state) {
            case NOT_READY:
                return new Vec3d();
            case SEARCHING:
                return handleSearching(context);
            case APPROACHING_NEST:
                return handleApproaching(context);
            case NESTING:
                return handleNesting(context);
            default:
                return new Vec3d();
        }
    }

    private void updateState(BehaviorContext context) {
        ticksSinceLastSearch++;

        if (state == NestingState.NESTING && currentNest != null) {
            ticksAtNest++;
            if (ticksAtNest >= nestingDuration) {
                readyToLay = true;
                state = NestingState.NOT_READY;
                currentNest = null;
                ticksAtNest = 0;
            }
            return;
        }

        if (state == NestingState.APPROACHING_NEST && currentNest != null) {
            BlockPos entityPos = context.getBlockPos();
            if (entityPos.distSqr(currentNest) < 2.0) {
                state = NestingState.NESTING;
                ticksAtNest = 0;
            }
        }

        if (state == NestingState.SEARCHING || currentNest == null) {
            if (ticksSinceLastSearch >= searchInterval) {
                searchForNest(context);
                ticksSinceLastSearch = 0;
            }
        }
    }

    private Vec3d handleSearching(BehaviorContext context) {
        if (currentNest != null) {
            state = NestingState.APPROACHING_NEST;
            return handleApproaching(context);
        }
        return new Vec3d();
    }

    private Vec3d handleApproaching(BehaviorContext context) {
        if (currentNest == null) {
            state = NestingState.SEARCHING;
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            currentNest.getX() + 0.5,
            currentNest.getY(),
            currentNest.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, APPROACH_SPEED, SLOWING_RADIUS);
    }

    private Vec3d handleNesting(BehaviorContext context) {
        return new Vec3d();
    }

    private void searchForNest(BehaviorContext context) {
        BlockPos bestNest = findNearestNest(context.getLevel(), context.getBlockPos());
        if (bestNest != null) {
            currentNest = bestNest;
            state = NestingState.APPROACHING_NEST;
        } else {
            currentNest = null;
            state = NestingState.SEARCHING;
        }
    }

    public BlockPos findNearestNest(Level level, BlockPos center) {
        BlockPos nearest = null;
        double minDistSquared = Double.MAX_VALUE;
        int radius = (int) searchRadius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distSquared = center.distSqr(pos);

                    if (distSquared < minDistSquared && distSquared <= searchRadius * searchRadius) {
                        BlockState state = level.getBlockState(pos);
                        if (isPreferredNestingBlock(state.getBlock())) {
                            nearest = pos;
                            minDistSquared = distSquared;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private boolean isPreferredNestingBlock(Block block) {
        return preferredNestingBlocks.contains(block);
    }

    public void setReadyToLay(boolean ready) {
        this.readyToLay = ready;
        if (ready) {
            state = NestingState.SEARCHING;
        } else {
            state = NestingState.NOT_READY;
            currentNest = null;
        }
    }

    public boolean isReadyToLay() {
        return readyToLay;
    }

    public boolean shouldLayNow() {
        return readyToLay && state == NestingState.NESTING && ticksAtNest >= nestingDuration;
    }

    public NestingState getState() {
        return state;
    }

    public BlockPos getCurrentNest() {
        return currentNest;
    }

    public void resetAfterLaying() {
        readyToLay = false;
        state = NestingState.NOT_READY;
        currentNest = null;
        ticksAtNest = 0;
    }

    public enum NestingState {
        NOT_READY,
        SEARCHING,
        APPROACHING_NEST,
        NESTING
    }
}
