package me.javavirtualenv.behavior.foraging;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implements patch selection based on the Marginal Value Theorem.
 * Animals leave patches when intake rate drops below the habitat average.
 */
public class PatchSelectionBehavior {
    private final ForagingConfig config;
    private final FoodMemory foodMemory;
    private final List<Block> targetBlocks;

    private BlockPos currentPatchCenter;
    private double currentPatchQuality;
    private int timeInPatch;
    private final double habitatIntakeRate;

    private static final int PATCH_ASSESSMENT_INTERVAL = 20;
    private static final double HABITAT_QUALITY_SAMPLE_SIZE = 0.3;

    public PatchSelectionBehavior(ForagingConfig config, FoodMemory foodMemory, List<Block> targetBlocks) {
        this.config = config;
        this.foodMemory = foodMemory;
        this.targetBlocks = new ArrayList<>(targetBlocks);
        this.currentPatchCenter = null;
        this.currentPatchQuality = 0.0;
        this.timeInPatch = 0;
        this.habitatIntakeRate = 0.5;
    }

    /**
     * Evaluates if the animal should stay in current patch or move to a new one.
     * Based on Marginal Value Theorem: leave when intake rate < average habitat rate.
     */
    public PatchDecision evaluatePatch(BehaviorContext context) {
        timeInPatch++;

        if (timeInPatch % PATCH_ASSESSMENT_INTERVAL != 0) {
            return PatchDecision.STAY;
        }

        double patchQuality = assessCurrentPatchQuality(context);
        currentPatchQuality = patchQuality;

        if (shouldAbandonPatch(patchQuality)) {
            BlockPos newPatch = findBestAlternativePatch(context);
            if (newPatch != null) {
                resetPatchTracking();
                currentPatchCenter = newPatch;
                return PatchDecision.MOVE;
            }
        }

        return PatchDecision.STAY;
    }

    /**
     * Calculates the quality of the current patch (grass density 0-1).
     */
    public double assessCurrentPatchQuality(BehaviorContext context) {
        BlockPos center = getCurrentPatchCenter(context);
        return assessPatchQuality(context.getLevel(), center);
    }

    /**
     * Assesses patch quality at a specific location.
     */
    public double assessPatchQuality(Level level, BlockPos center) {
        if (center == null || level == null) {
            return 0.0;
        }

        int patchSize = config.getPatchSize();
        int grassBlocks = 0;
        int totalBlocks = 0;

        for (int x = -patchSize; x <= patchSize; x++) {
            for (int z = -patchSize; z <= patchSize; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distance = Math.sqrt(x * x + z * z);

                    if (distance <= patchSize) {
                        totalBlocks++;
                        BlockState state = level.getBlockState(pos);

                        if (state != null && isTargetBlock(state.getBlock())) {
                            grassBlocks++;
                        }
                    }
                }
            }
        }

        return totalBlocks > 0 ? (double) grassBlocks / totalBlocks : 0.0;
    }

    /**
     * Determines if the patch should be abandoned based on Giving-Up Density.
     */
    public boolean shouldAbandonPatch(double currentQuality) {
        double givingUpDensity = config.getGivingUpDensity();
        return currentQuality < givingUpDensity;
    }

    /**
     * Finds the best alternative patch within search radius.
     */
    public BlockPos findBestAlternativePatch(BehaviorContext context) {
        List<PatchCandidate> candidates = findPatchCandidates(context);

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparingDouble(PatchCandidate::getNetValue).reversed());

        PatchCandidate best = candidates.get(0);
        foodMemory.rememberPatch(best.location, (int) (best.quality * 100));

        return best.location;
    }

    /**
     * Finds all patch candidates within search radius.
     */
    private List<PatchCandidate> findPatchCandidates(BehaviorContext context) {
        List<PatchCandidate> candidates = new ArrayList<>();
        Level level = context.getLevel();
        BlockPos center = context.getBlockPos();
        double searchRadius = config.getSearchRadius();
        int intRadius = (int) searchRadius;

        for (int x = -intRadius; x <= intRadius; x += 2) {
            for (int z = -intRadius; z <= intRadius; z += 2) {
                BlockPos candidatePos = center.offset(x, 0, z);
                double distance = Math.sqrt(x * x + z * z);

                if (distance <= searchRadius && distance > 2.0) {
                    double quality = assessPatchQuality(level, candidatePos);

                    if (quality > config.getGivingUpDensity()) {
                        double travelCost = calculateTravelCost(distance);
                        double netValue = quality - travelCost;

                        candidates.add(new PatchCandidate(candidatePos, quality, netValue));
                    }
                }
            }
        }

        List<PatchCandidate> memoryCandidates = getMemoryPatchCandidates(context);
        candidates.addAll(memoryCandidates);

        return candidates;
    }

    /**
     * Gets patch candidates from memory.
     */
    private List<PatchCandidate> getMemoryPatchCandidates(BehaviorContext context) {
        List<PatchCandidate> candidates = new ArrayList<>();
        BlockPos center = context.getBlockPos();

        for (FoodMemory.FoodPatchInfo patch : foodMemory.getNearbyRememberedPatches(center, config.getSearchRadius())) {
            double distance = patch.distanceTo(center);
            double travelCost = calculateTravelCost(distance);
            double quality = patch.estimatedFood / 100.0;
            double netValue = quality - travelCost;

            candidates.add(new PatchCandidate(patch.location, quality, netValue));
        }

        return candidates;
    }

    /**
     * Calculates the cost of traveling to a patch.
     * Based on distance and time cost.
     */
    private double calculateTravelCost(double distance) {
        double travelTime = distance / config.getGrazingSpeed();
        return travelTime * 0.01;
    }

    /**
     * Updates the current patch center based on entity position.
     */
    public void updateCurrentPatch(BehaviorContext context) {
        if (currentPatchCenter == null) {
            currentPatchCenter = context.getBlockPos();
            return;
        }

        double distance = context.getBlockPos().distSqr(currentPatchCenter);
        int patchRadius = config.getPatchSize();

        if (distance > patchRadius * patchRadius) {
            currentPatchCenter = context.getBlockPos();
            timeInPatch = 0;
        }
    }

    /**
     * Resets patch tracking when moving to a new patch.
     */
    public void resetPatchTracking() {
        timeInPatch = 0;
        currentPatchQuality = 0.0;
    }

    /**
     * Gets the current patch center, or entity position if none set.
     */
    private BlockPos getCurrentPatchCenter(BehaviorContext context) {
        if (currentPatchCenter == null) {
            currentPatchCenter = context.getBlockPos();
        }
        return currentPatchCenter;
    }

    /**
     * Gets a random position within the current patch for grazing.
     */
    public Vec3d getRandomPositionInPatch(BehaviorContext context) {
        BlockPos center = getCurrentPatchCenter(context);
        int patchSize = config.getPatchSize();

        double angle = Math.random() * 2.0 * Math.PI;
        double radius = Math.random() * patchSize;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;

        return new Vec3d(
            center.getX() + 0.5 + offsetX,
            center.getY(),
            center.getZ() + 0.5 + offsetZ
        );
    }

    /**
     * Checks if a block is a target food block.
     */
    private boolean isTargetBlock(net.minecraft.world.level.block.Block block) {
        return targetBlocks.contains(block);
    }

    public BlockPos getCurrentPatchCenter() {
        return currentPatchCenter;
    }

    public double getCurrentPatchQuality() {
        return currentPatchQuality;
    }

    public int getTimeInPatch() {
        return timeInPatch;
    }

    /**
     * Represents a candidate patch for foraging.
     */
    private static class PatchCandidate {
        final BlockPos location;
        final double quality;
        final double netValue;

        PatchCandidate(BlockPos location, double quality, double netValue) {
            this.location = location;
            this.quality = quality;
            this.netValue = netValue;
        }

        double getNetValue() {
            return netValue;
        }
    }

    /**
     * Decision outcomes for patch selection.
     */
    public enum PatchDecision {
        STAY,   // Continue in current patch
        MOVE,   // Move to a new patch
        WAIT    // Wait before deciding
    }
}
