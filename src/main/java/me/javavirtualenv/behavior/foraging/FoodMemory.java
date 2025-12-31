package me.javavirtualenv.behavior.foraging;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages memory of food patch locations for foraging animals.
 * Implements spatial memory with decay based on Optimal Foraging Theory.
 */
public class FoodMemory {
    private final Map<BlockPos, FoodPatchInfo> knownPatches;
    private final int memoryDuration;

    public FoodMemory(int memoryDuration) {
        this.knownPatches = new HashMap<>();
        this.memoryDuration = memoryDuration;
    }

    /**
     * Records a food patch in memory.
     */
    public void rememberPatch(BlockPos location, int foodAmount) {
        FoodPatchInfo existing = knownPatches.get(location);
        if (existing != null) {
            existing.estimatedFood = foodAmount;
            existing.lastVisited = System.currentTimeMillis();
            existing.depleted = false;
        } else {
            knownPatches.put(location, new FoodPatchInfo(location, foodAmount));
        }
    }

    /**
     * Removes patches that haven't been visited within the memory duration.
     */
    public void forgetOldPatches() {
        long currentTime = System.currentTimeMillis();
        long memoryThreshold = memoryDuration * 50L; // Convert ticks to milliseconds (20 ticks/sec)

        knownPatches.entrySet().removeIf(entry -> {
            long timeSinceVisit = currentTime - entry.getValue().lastVisited;
            return timeSinceVisit > memoryThreshold;
        });
    }

    /**
     * Returns the best remembered patch (highest food, not depleted).
     */
    public BlockPos getBestPatch() {
        return knownPatches.values().stream()
            .filter(patch -> !patch.depleted && patch.estimatedFood > 0)
            .max((a, b) -> Integer.compare(a.estimatedFood, b.estimatedFood))
            .map(patch -> patch.location)
            .orElse(null);
    }

    /**
     * Marks a patch as depleted.
     */
    public void markDepleted(BlockPos location) {
        FoodPatchInfo patch = knownPatches.get(location);
        if (patch != null) {
            patch.depleted = true;
            patch.estimatedFood = 0;
        }
    }

    /**
     * Decreases the estimated food at a patch.
     */
    public void consumeFromPatch(BlockPos location, int amount) {
        FoodPatchInfo patch = knownPatches.get(location);
        if (patch != null) {
            patch.estimatedFood = Math.max(0, patch.estimatedFood - amount);
            patch.lastVisited = System.currentTimeMillis();
            if (patch.estimatedFood == 0) {
                patch.depleted = true;
            }
        }
    }

    /**
     * Gets all remembered patches within a radius of the center position.
     */
    public List<FoodPatchInfo> getNearbyRememberedPatches(BlockPos center, double radius) {
        List<FoodPatchInfo> nearby = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (FoodPatchInfo patch : knownPatches.values()) {
            if (!patch.depleted && patch.estimatedFood > 0) {
                double distSquared = center.distSqr(patch.location);
                if (distSquared <= radiusSquared) {
                    nearby.add(patch);
                }
            }
        }

        return nearby;
    }

    /**
     * Gets the estimated food at a specific location.
     */
    public int getEstimatedFood(BlockPos location) {
        FoodPatchInfo patch = knownPatches.get(location);
        return patch != null ? patch.estimatedFood : 0;
    }

    /**
     * Checks if a patch is known and not depleted.
     */
    public boolean hasViablePatch(BlockPos location) {
        FoodPatchInfo patch = knownPatches.get(location);
        return patch != null && !patch.depleted && patch.estimatedFood > 0;
    }

    /**
     * Clears all memory.
     */
    public void clear() {
        knownPatches.clear();
    }

    /**
     * Inner class representing information about a food patch.
     */
    public static class FoodPatchInfo {
        public final BlockPos location;
        public long lastVisited;
        public int estimatedFood;
        public boolean depleted;

        public FoodPatchInfo(BlockPos location, int estimatedFood) {
            this.location = location;
            this.lastVisited = System.currentTimeMillis();
            this.estimatedFood = estimatedFood;
            this.depleted = false;
        }

        public double distanceTo(BlockPos pos) {
            return Math.sqrt(location.distSqr(pos));
        }
    }
}
