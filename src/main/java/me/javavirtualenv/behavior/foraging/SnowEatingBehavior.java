package me.javavirtualenv.behavior.foraging;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.ThirstHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Snow eating behavior for herbivores in cold/snowy environments.
 * Animals seek and consume snow blocks (powder snow, snow layers) for hydration.
 * Implements the compensation hypothesis where herbivores obtain water from alternative sources.
 *
 * Scientific context: In cold environments, snow becomes a critical water source.
 * Animals can eat snow but NOT ice (ice is too dense and cold).
 */
public class SnowEatingBehavior extends SteeringBehavior {
    private final SnowEatingConfig config;
    private final List<Block> targetSnowBlocks;
    private final int hydrationRestoreAmount;

    private SnowEatingState state;
    private BlockPos currentSnowTarget;
    private int ticksSinceLastSearch;
    private int ticksEating;

    private static final double MAX_SPEED = 0.4;
    private static final double SLOWING_RADIUS = 3.0;
    private static final int EATING_DURATION = 60;
    private static final int SEARCH_INTERVAL = 40;

    public SnowEatingBehavior(SnowEatingConfig config) {
        this.config = config;
        this.targetSnowBlocks = createTargetSnowBlocks();
        this.hydrationRestoreAmount = config.hydrationRestoreAmount;
        this.state = SnowEatingState.SEARCHING;
        this.currentSnowTarget = null;
        this.ticksSinceLastSearch = 0;
        this.ticksEating = 0;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        // Only activate if thirsty
        if (!context.getEntityState().isThirsty()) {
            return new Vec3d();
        }

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

        if (state == SnowEatingState.EATING) {
            ticksEating++;
            if (ticksEating >= EATING_DURATION) {
                consumeSnow(context);
                state = SnowEatingState.SEARCHING;
                currentSnowTarget = null;
                ticksEating = 0;
            }
            return;
        }

        if (state == SnowEatingState.APPROACHING && currentSnowTarget != null) {
            BlockPos entityPos = context.getBlockPos();
            if (entityPos.distSqr(currentSnowTarget) < 4.0) {
                state = SnowEatingState.EATING;
                ticksEating = 0;
            }
        }

        if (ticksSinceLastSearch >= SEARCH_INTERVAL || currentSnowTarget == null) {
            searchForSnow(context);
            ticksSinceLastSearch = 0;
        }
    }

    private Vec3d handleSearching(BehaviorContext context) {
        if (currentSnowTarget != null) {
            state = SnowEatingState.APPROACHING;
            return handleApproaching(context);
        }
        return new Vec3d();
    }

    private Vec3d handleApproaching(BehaviorContext context) {
        if (currentSnowTarget == null) {
            state = SnowEatingState.SEARCHING;
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            currentSnowTarget.getX() + 0.5,
            currentSnowTarget.getY(),
            currentSnowTarget.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, MAX_SPEED, SLOWING_RADIUS);
    }

    private Vec3d handleEating(BehaviorContext context) {
        return new Vec3d();
    }

    private void searchForSnow(BehaviorContext context) {
        BlockPos nearest = findNearestSnow(context.getLevel(), context.getBlockPos());
        if (nearest != null) {
            currentSnowTarget = nearest;
            state = SnowEatingState.APPROACHING;
        } else {
            currentSnowTarget = null;
            state = SnowEatingState.SEARCHING;
        }
    }

    private BlockPos findNearestSnow(Level level, BlockPos center) {
        BlockPos nearest = null;
        double minDistSquared = Double.MAX_VALUE;
        int radius = config.searchRadius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distSquared = center.distSqr(pos);

                    if (distSquared < minDistSquared && distSquared <= radius * radius) {
                        BlockState state = level.getBlockState(pos);
                        if (isSnowBlock(state.getBlock())) {
                            nearest = pos;
                            minDistSquared = distSquared;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private void consumeSnow(BehaviorContext context) {
        if (currentSnowTarget == null) {
            return;
        }

        Level level = context.getLevel();
        BlockState state = level.getBlockState(currentSnowTarget);
        Block block = state.getBlock();

        // Remove the snow block
        if (block == Blocks.POWDER_SNOW) {
            level.setBlock(currentSnowTarget, Blocks.AIR.defaultBlockState(), 3);
        } else if (block == Blocks.SNOW) {
            // Reduce snow layers or remove if single layer
            int layers = state.getValue(net.minecraft.world.level.block.SnowBlock.LAYERS);
            if (layers > 1) {
                level.setBlock(currentSnowTarget, state.setValue(net.minecraft.world.level.block.SnowBlock.LAYERS, layers - 1), 3);
            } else {
                level.setBlock(currentSnowTarget, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // Restore hydration
        Mob mob = context.getMob();
        EcologyComponent component = getEcologyComponent(mob);
        if (component != null && component.hasProfile()) {
            EcologyProfile profile = component.profile();
            ThirstHandle thirstHandle = new ThirstHandle();
            thirstHandle.restoreHydration(mob, component, profile, hydrationRestoreAmount);
        }
    }

    private boolean isSnowBlock(Block block) {
        return targetSnowBlocks.contains(block);
    }

    private List<Block> createTargetSnowBlocks() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(Blocks.SNOW);
        blocks.add(Blocks.POWDER_SNOW);
        // Explicitly NOT ice or packed ice
        return blocks;
    }

    private EcologyComponent getEcologyComponent(Mob mob) {
        return me.javavirtualenv.ecology.EcologyHooks.getEcologyComponent(mob);
    }

    public SnowEatingState getState() {
        return state;
    }

    public BlockPos getCurrentTarget() {
        return currentSnowTarget;
    }

    public enum SnowEatingState {
        SEARCHING,
        APPROACHING,
        EATING
    }

    /**
     * Configuration for snow eating behavior.
     */
    public static class SnowEatingConfig {
        private final int searchRadius;
        private final int hydrationRestoreAmount;

        public SnowEatingConfig(int searchRadius, int hydrationRestoreAmount) {
            this.searchRadius = searchRadius;
            this.hydrationRestoreAmount = hydrationRestoreAmount;
        }

        public int getSearchRadius() {
            return searchRadius;
        }

        public int getHydrationRestoreAmount() {
            return hydrationRestoreAmount;
        }
    }
}
