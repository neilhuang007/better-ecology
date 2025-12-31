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
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Main grazing behavior for grass-eating herbivores.
 * Implements bimodal grazing patterns, midday rest, and gradual grass consumption.
 * Restores hydration when eating vegetation based on compensation hypothesis.
 *
 * Scientific context: Herbivores obtain water from vegetation - grass ~70-85% water content.
 * Animals increase plant consumption specifically to obtain water during dry periods.
 */
public class GrazingBehavior extends SteeringBehavior {
    private final ForagingConfig config;
    private final ForagingScheduler scheduler;
    private final PatchSelectionBehavior patchSelector;
    private final FoodMemory foodMemory;
    private final List<Block> targetBlocks;
    private final int hydrationRestorePerBite;

    private GrazingState state;
    private BlockPos currentGrazeTarget;
    private int ticksSinceLastGraze;
    private int ticksInCurrentState;
    private int grassConsumedInPatch;

    private static final int GRAZE_INTERVAL = 30;
    private static final int REGENERATION_CHECK_INTERVAL = 100;

    public GrazingBehavior(ForagingConfig config, ForagingScheduler.ForagingPattern pattern) {
        this.config = config;
        this.scheduler = new ForagingScheduler(config, pattern);
        this.foodMemory = new FoodMemory(config.getMemoryDuration());
        this.targetBlocks = createTargetBlocks();
        this.patchSelector = new PatchSelectionBehavior(config, foodMemory, targetBlocks);
        this.state = GrazingState.SEARCHING;
        this.currentGrazeTarget = null;
        this.ticksSinceLastGraze = 0;
        this.ticksInCurrentState = 0;
        this.grassConsumedInPatch = 0;
        this.hydrationRestorePerBite = config.getHydrationRestoreAmount();
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        updateState(context);
        patchSelector.updateCurrentPatch(context);

        switch (state) {
            case SEARCHING:
                return handleSearching(context);
            case MOVING_TO_PATCH:
                return handleMovingToPatch(context);
            case GRAZING:
                return handleGrazing(context);
            case RESTING:
                return handleResting(context);
            case REGENERATING:
                return handleRegenerating(context);
            default:
                return new Vec3d();
        }
    }

    private void updateState(BehaviorContext context) {
        Level level = context.getLevel();
        ticksInCurrentState++;
        ticksSinceLastGraze++;
        foodMemory.forgetOldPatches();

        if (!scheduler.isForagingTime(level)) {
            if (state != GrazingState.RESTING && state != GrazingState.REGENERATING) {
                beginResting();
            }
            return;
        }

        if (state == GrazingState.RESTING || state == GrazingState.REGENERATING) {
            if (scheduler.isForagingTime(level)) {
                startSearching(context);
            }
            return;
        }

        if (state == GrazingState.GRAZING) {
            if (ticksSinceLastGraze >= GRAZE_INTERVAL) {
                grazeCurrentTarget(context);
                ticksSinceLastGraze = 0;
            }

            if (shouldLeavePatch(context)) {
                startSearching(context);
            }
        }

        if (state == GrazingState.SEARCHING || state == GrazingState.MOVING_TO_PATCH) {
            PatchSelectionBehavior.PatchDecision decision = patchSelector.evaluatePatch(context);

            if (decision == PatchSelectionBehavior.PatchDecision.MOVE) {
                state = GrazingState.MOVING_TO_PATCH;
            }
        }
    }

    private Vec3d handleSearching(BehaviorContext context) {
        BlockPos targetPatch = patchSelector.getCurrentPatchCenter();

        if (targetPatch != null && !shouldLeavePatch(context)) {
            state = GrazingState.GRAZING;
            return handleGrazing(context);
        }

        targetPatch = patchSelector.findBestAlternativePatch(context);
        if (targetPatch != null) {
            state = GrazingState.MOVING_TO_PATCH;
            Vec3d targetPos = new Vec3d(
                targetPatch.getX() + 0.5,
                targetPatch.getY(),
                targetPatch.getZ() + 0.5
            );
            return arrive(context.getPosition(), context.getVelocity(), targetPos, config.getGrazingSpeed(), 3.0);
        }

        return performSlowWander(context);
    }

    private Vec3d handleMovingToPatch(BehaviorContext context) {
        BlockPos patchCenter = patchSelector.getCurrentPatchCenter();

        if (patchCenter == null) {
            startSearching(context);
            return new Vec3d();
        }

        double distance = context.getBlockPos().distSqr(patchCenter);

        if (distance < 9.0) {
            state = GrazingState.GRAZING;
            ticksInCurrentState = 0;
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            patchCenter.getX() + 0.5,
            patchCenter.getY(),
            patchCenter.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, config.getGrazingSpeed(), 5.0);
    }

    private Vec3d handleGrazing(BehaviorContext context) {
        if (currentGrazeTarget == null || !isValidGrazeTarget(context)) {
            selectNewGrazeTarget(context);
        }

        if (currentGrazeTarget == null) {
            startSearching(context);
            return performSlowWander(context);
        }

        BlockPos entityPos = context.getBlockPos();
        double distanceToTarget = entityPos.distSqr(currentGrazeTarget);

        if (distanceToTarget < 2.5) {
            if (ticksSinceLastGraze >= GRAZE_INTERVAL) {
                grazeCurrentTarget(context);
                ticksSinceLastGraze = 0;
            }
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            currentGrazeTarget.getX() + 0.5,
            currentGrazeTarget.getY(),
            currentGrazeTarget.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, config.getGrazingSpeed() * 0.7, 2.0);
    }

    private Vec3d handleResting(BehaviorContext context) {
        return new Vec3d();
    }

    private Vec3d handleRegenerating(BehaviorContext context) {
        if (ticksInCurrentState > REGENERATION_CHECK_INTERVAL) {
            checkAndRegenerateGrass(context);
            ticksInCurrentState = 0;
        }

        return performSlowWander(context);
    }

    private void selectNewGrazeTarget(BehaviorContext context) {
        Vec3d randomPos = patchSelector.getRandomPositionInPatch(context);
        BlockPos blockPos = new BlockPos((int) randomPos.x, (int) randomPos.y, (int) randomPos.z);

        for (int y = -1; y <= 1; y++) {
            BlockPos checkPos = blockPos.offset(0, y, 0);
            BlockState state = context.getLevel().getBlockState(checkPos);

            if (isTargetBlock(state.getBlock())) {
                currentGrazeTarget = checkPos;
                return;
            }
        }

        currentGrazeTarget = null;
    }

    private boolean isValidGrazeTarget(BehaviorContext context) {
        if (currentGrazeTarget == null) {
            return false;
        }

        BlockState state = context.getLevel().getBlockState(currentGrazeTarget);
        return isTargetBlock(state.getBlock());
    }

    private void grazeCurrentTarget(BehaviorContext context) {
        if (currentGrazeTarget == null) {
            return;
        }

        Level level = context.getLevel();
        BlockState state = level.getBlockState(currentGrazeTarget);
        int biteSize = config.getBiteSize();

        if (state.is(Blocks.GRASS_BLOCK)) {
            consumeGrassBlock(level, currentGrazeTarget, biteSize);
            grassConsumedInPatch += biteSize;
        } else if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)) {
            level.setBlock(currentGrazeTarget, Blocks.AIR.defaultBlockState(), 3);
            grassConsumedInPatch += biteSize;
        }

        foodMemory.consumeFromPatch(patchSelector.getCurrentPatchCenter(), biteSize);

        // Restore hydration from vegetation (compensation hypothesis)
        restoreHydration(context);

        // Restore hunger when food is consumed
        HungerHandle.restoreHunger(context.getMob(), config.getHungerRestore());
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

    private void consumeGrassBlock(Level level, BlockPos pos, int amount) {
        BlockState currentState = level.getBlockState(pos);

        if (!currentState.is(Blocks.GRASS_BLOCK)) {
            return;
        }

        if (amount >= 1) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    private void checkAndRegenerateGrass(BehaviorContext context) {
        Level level = context.getLevel();
        BlockPos patchCenter = patchSelector.getCurrentPatchCenter();
        int patchSize = config.getPatchSize();

        if (patchCenter == null) {
            return;
        }

        int regenerated = 0;
        int maxRegenerate = grassConsumedInPatch / 2;

        for (int x = -patchSize; x <= patchSize && regenerated < maxRegenerate; x++) {
            for (int z = -patchSize; z <= patchSize && regenerated < maxRegenerate; z++) {
                BlockPos pos = patchCenter.offset(x, -1, z);
                BlockState state = level.getBlockState(pos);

                if (state.is(Blocks.DIRT)) {
                    BlockPos abovePos = pos.above();
                    BlockState above = level.getBlockState(abovePos);

                    if (above.is(Blocks.AIR) || above.is(Blocks.SHORT_GRASS)) {
                        level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                        regenerated++;
                    }
                }
            }
        }

        if (regenerated > 0) {
            grassConsumedInPatch -= regenerated;
        }
    }

    private boolean shouldLeavePatch(BehaviorContext context) {
        double patchQuality = patchSelector.assessCurrentPatchQuality(context);
        return patchSelector.shouldAbandonPatch(patchQuality);
    }

    private void startSearching(BehaviorContext context) {
        state = GrazingState.SEARCHING;
        ticksInCurrentState = 0;
        grassConsumedInPatch = 0;
        currentGrazeTarget = null;
        patchSelector.resetPatchTracking();
    }

    private void beginResting() {
        state = GrazingState.RESTING;
        ticksInCurrentState = 0;
        currentGrazeTarget = null;
    }

    private Vec3d performSlowWander(BehaviorContext context) {
        Vec3d currentPos = context.getPosition();
        double wanderRadius = 3.0;
        double angle = Math.random() * 2.0 * Math.PI;

        Vec3d wanderTarget = new Vec3d(
            currentPos.x + Math.cos(angle) * wanderRadius,
            currentPos.y,
            currentPos.z + Math.sin(angle) * wanderRadius
        );

        return arrive(currentPos, context.getVelocity(), wanderTarget, config.getGrazingSpeed() * 0.5, 2.0);
    }

    private boolean isTargetBlock(net.minecraft.world.level.block.Block block) {
        return targetBlocks.contains(block);
    }

    private List<Block> createTargetBlocks() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(Blocks.GRASS_BLOCK);
        blocks.add(Blocks.SHORT_GRASS);
        blocks.add(Blocks.TALL_GRASS);
        return blocks;
    }

    public GrazingState getState() {
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

    public enum GrazingState {
        SEARCHING,
        MOVING_TO_PATCH,
        GRAZING,
        RESTING,
        REGENERATING
    }
}
