package me.javavirtualenv.behavior.foraging;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.handles.ThirstHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Browsing behavior for non-grass herbivores (deer, goats).
 * Animals browse leaves from trees with more selective foraging.
 * Restores hydration when eating leaves based on compensation hypothesis.
 *
 * Scientific context: Leaves contain ~60-80% water content.
 * Herbivores obtain water from vegetation during dry periods.
 */
public class BrowsingBehavior extends SteeringBehavior {
    private final ForagingConfig config;
    private final ForagingScheduler scheduler;
    private final PatchSelectionBehavior patchSelector;
    private final FoodMemory foodMemory;
    private final List<Block> targetBlocks;
    private final int hydrationRestorePerBite;

    private BrowsingState state;
    private BlockPos currentBrowseTarget;
    private int ticksSinceLastBrowse;
    private int ticksInCurrentState;
    private int leavesConsumedInPatch;

    private static final int BROWSE_INTERVAL = 40;
    private static final int BROWSE_HEIGHT = 4;
    private static final double SELECTIVITY_THRESHOLD = 0.7;

    public BrowsingBehavior(ForagingConfig config) {
        this.config = config;
        this.scheduler = new ForagingScheduler(config, ForagingScheduler.ForagingPattern.SELECTIVE);
        this.foodMemory = new FoodMemory(config.getMemoryDuration());
        this.targetBlocks = createTargetBlocks();
        this.patchSelector = new PatchSelectionBehavior(config, foodMemory, targetBlocks);
        this.state = BrowsingState.SEARCHING;
        this.currentBrowseTarget = null;
        this.ticksSinceLastBrowse = 0;
        this.ticksInCurrentState = 0;
        this.leavesConsumedInPatch = 0;
        this.hydrationRestorePerBite = config.getHydrationRestoreAmount();
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        updateState(context);
        patchSelector.updateCurrentPatch(context);

        switch (state) {
            case SEARCHING:
                return handleSearching(context);
            case MOVING_TO_TREE:
                return handleMovingToTree(context);
            case BROWSING:
                return handleBrowsing(context);
            case RESTING:
                return handleResting(context);
            default:
                return new Vec3d();
        }
    }

    private void updateState(BehaviorContext context) {
        Level level = context.getLevel();
        ticksInCurrentState++;
        ticksSinceLastBrowse++;
        foodMemory.forgetOldPatches();

        if (!scheduler.isForagingTime(level)) {
            if (state != BrowsingState.RESTING) {
                state = BrowsingState.RESTING;
                ticksInCurrentState = 0;
                currentBrowseTarget = null;
            }
            return;
        }

        if (state == BrowsingState.RESTING) {
            if (scheduler.isForagingTime(level)) {
                startSearching(context);
            }
            return;
        }

        if (state == BrowsingState.BROWSING) {
            if (ticksSinceLastBrowse >= BROWSE_INTERVAL) {
                browseCurrentTarget(context);
                ticksSinceLastBrowse = 0;
            }

            if (shouldLeavePatch(context)) {
                startSearching(context);
            }
        }

        if (state == BrowsingState.SEARCHING || state == BrowsingState.MOVING_TO_TREE) {
            PatchSelectionBehavior.PatchDecision decision = patchSelector.evaluatePatch(context);

            if (decision == PatchSelectionBehavior.PatchDecision.MOVE) {
                state = BrowsingState.MOVING_TO_TREE;
            }
        }
    }

    private Vec3d handleSearching(BehaviorContext context) {
        BlockPos targetTree = findNearestTree(context);

        if (targetTree != null) {
            state = BrowsingState.MOVING_TO_TREE;
            Vec3d targetPos = new Vec3d(
                targetTree.getX() + 0.5,
                context.getPosition().y,
                targetTree.getZ() + 0.5
            );
            return arrive(context.getPosition(), context.getVelocity(), targetPos, config.getGrazingSpeed(), 4.0);
        }

        return performSelectiveWander(context);
    }

    private Vec3d handleMovingToTree(BehaviorContext context) {
        if (currentBrowseTarget == null) {
            startSearching(context);
            return new Vec3d();
        }

        BlockPos entityPos = context.getBlockPos();
        double distance = Math.sqrt(entityPos.distSqr(currentBrowseTarget));

        if (distance < 5.0) {
            state = BrowsingState.BROWSING;
            ticksInCurrentState = 0;
            selectNewBrowseTarget(context);
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            currentBrowseTarget.getX() + 0.5,
            context.getPosition().y,
            currentBrowseTarget.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, config.getGrazingSpeed(), 4.0);
    }

    private Vec3d handleBrowsing(BehaviorContext context) {
        if (currentBrowseTarget == null || !isValidBrowseTarget(context)) {
            selectNewBrowseTarget(context);
        }

        if (currentBrowseTarget == null) {
            startSearching(context);
            return performSelectiveWander(context);
        }

        BlockPos entityPos = context.getBlockPos();
        double horizontalDistance = Math.sqrt(
            Math.pow(entityPos.getX() - currentBrowseTarget.getX(), 2) +
            Math.pow(entityPos.getZ() - currentBrowseTarget.getZ(), 2)
        );

        if (horizontalDistance < 3.5) {
            if (ticksSinceLastBrowse >= BROWSE_INTERVAL) {
                browseCurrentTarget(context);
                ticksSinceLastBrowse = 0;
            }
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            currentBrowseTarget.getX() + 0.5,
            currentBrowseTarget.getY(),
            currentBrowseTarget.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, config.getGrazingSpeed() * 0.8, 2.5);
    }

    private Vec3d handleResting(BehaviorContext context) {
        return new Vec3d();
    }

    private BlockPos findNearestTree(BehaviorContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getBlockPos();
        double searchRadius = config.getSearchRadius();
        int intRadius = (int) searchRadius;

        BlockPos nearestTree = null;
        double minDistance = Double.MAX_VALUE;

        for (int x = -intRadius; x <= intRadius; x++) {
            for (int z = -intRadius; z <= intRadius; z++) {
                for (int y = -2; y <= BROWSE_HEIGHT; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distance = Math.sqrt(x * x + z * z);

                    if (distance < minDistance && distance <= searchRadius) {
                        BlockState state = level.getBlockState(pos);

                        if (isTargetBlock(state.getBlock())) {
                            nearestTree = pos;
                            minDistance = distance;
                        }
                    }
                }
            }
        }

        if (nearestTree != null) {
            currentBrowseTarget = nearestTree;
        }

        return nearestTree;
    }

    private void selectNewBrowseTarget(BehaviorContext context) {
        if (currentBrowseTarget == null) {
            findNearestTree(context);
            return;
        }

        Level level = context.getLevel();
        BlockPos searchCenter = currentBrowseTarget;
        BlockPos bestTarget = null;
        double bestQuality = 0.0;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= BROWSE_HEIGHT; y++) {
                    BlockPos pos = searchCenter.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (isTargetBlock(state.getBlock())) {
                        double quality = assessLeafQuality(state);

                        if (quality > bestQuality && quality >= SELECTIVITY_THRESHOLD) {
                            bestQuality = quality;
                            bestTarget = pos;
                        }
                    }
                }
            }
        }

        if (bestTarget != null) {
            currentBrowseTarget = bestTarget;
        } else {
            currentBrowseTarget = null;
        }
    }

    private boolean isValidBrowseTarget(BehaviorContext context) {
        if (currentBrowseTarget == null) {
            return false;
        }

        BlockState state = context.getLevel().getBlockState(currentBrowseTarget);
        return isTargetBlock(state.getBlock());
    }

    private void browseCurrentTarget(BehaviorContext context) {
        if (currentBrowseTarget == null) {
            return;
        }

        Level level = context.getLevel();
        BlockState state = level.getBlockState(currentBrowseTarget);
        int biteSize = config.getBiteSize();

        if (isTargetBlock(state.getBlock())) {
            level.setBlock(currentBrowseTarget, Blocks.AIR.defaultBlockState(), 3);
            leavesConsumedInPatch += biteSize;
            foodMemory.consumeFromPatch(patchSelector.getCurrentPatchCenter(), biteSize);

            // Restore hydration from vegetation (compensation hypothesis)
            restoreHydration(context);

            // Restore hunger when food is consumed
            HungerHandle.restoreHunger(context.getEntity(), config.getHungerRestore());
        }
    }

    private void restoreHydration(BehaviorContext context) {
        Mob mob = context.getMob();
        EcologyComponent component = me.javavirtualenv.ecology.EcologyHooks.getEcologyComponent(mob);

        if (component != null && component.hasProfile()) {
            EcologyProfile profile = component.profile();
            ThirstHandle thirstHandle = new ThirstHandle();
            thirstHandle.restoreHydration(mob, component, profile, hydrationRestorePerBite);
        }
    }

    private double assessLeafQuality(BlockState state) {
        if (!state.is(Blocks.ACACIA_LEAVES) &&
            !state.is(Blocks.BIRCH_LEAVES) &&
            !state.is(Blocks.DARK_OAK_LEAVES) &&
            !state.is(Blocks.JUNGLE_LEAVES) &&
            !state.is(Blocks.OAK_LEAVES) &&
            !state.is(Blocks.SPRUCE_LEAVES)) {
            return 0.5;
        }

        boolean isPersistent = state.hasProperty(LeavesBlock.PERSISTENT) &&
                             state.getValue(LeavesBlock.PERSISTENT);

        if (isPersistent) {
            return 0.6;
        }

        double distance = state.hasProperty(LeavesBlock.DISTANCE) ?
                         state.getValue(LeavesBlock.DISTANCE) : 1;

        return 1.0 - (distance * 0.1);
    }

    private boolean shouldLeavePatch(BehaviorContext context) {
        double patchQuality = patchSelector.assessCurrentPatchQuality(context);
        return patchSelector.shouldAbandonPatch(patchQuality);
    }

    private void startSearching(BehaviorContext context) {
        state = BrowsingState.SEARCHING;
        ticksInCurrentState = 0;
        leavesConsumedInPatch = 0;
        currentBrowseTarget = null;
        patchSelector.resetPatchTracking();
    }

    private Vec3d performSelectiveWander(BehaviorContext context) {
        Vec3d currentPos = context.getPosition();
        double wanderRadius = 5.0;
        double angle = Math.random() * 2.0 * Math.PI;

        Vec3d wanderTarget = new Vec3d(
            currentPos.x + Math.cos(angle) * wanderRadius,
            currentPos.y,
            currentPos.z + Math.sin(angle) * wanderRadius
        );

        return arrive(currentPos, context.getVelocity(), wanderTarget, config.getGrazingSpeed() * 0.6, 2.0);
    }

    private boolean isTargetBlock(net.minecraft.world.level.block.Block block) {
        return targetBlocks.contains(block);
    }

    private List<Block> createTargetBlocks() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(Blocks.ACACIA_LEAVES);
        blocks.add(Blocks.BIRCH_LEAVES);
        blocks.add(Blocks.DARK_OAK_LEAVES);
        blocks.add(Blocks.JUNGLE_LEAVES);
        blocks.add(Blocks.OAK_LEAVES);
        blocks.add(Blocks.SPRUCE_LEAVES);
        blocks.add(Blocks.AZALEA_LEAVES);
        blocks.add(Blocks.FLOWERING_AZALEA_LEAVES);
        return blocks;
    }

    public BrowsingState getState() {
        return state;
    }

    public ForagingScheduler getScheduler() {
        return scheduler;
    }

    public PatchSelectionBehavior getPatchSelector() {
        return patchSelector;
    }

    public FoodMemory getFoodMemory() {
        return foodMemory;
    }

    public enum BrowsingState {
        SEARCHING,
        MOVING_TO_TREE,
        BROWSING,
        RESTING
    }
}
