package me.javavirtualenv.behavior.territorial;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks an animal's home range - the non-defended area where it regularly moves and forages.
 * Animals become familiar with their home range through repeated visits.
 */
public class HomeRange {
    private BlockPos centerPoint;
    private double radius;
    private final Set<BlockPos> visitedLocations;
    private final Map<BlockPos, Integer> visitFrequency;

    public HomeRange(BlockPos centerPoint, double radius) {
        this.centerPoint = centerPoint;
        this.radius = radius;
        this.visitedLocations = new HashSet<>();
        this.visitFrequency = new HashMap<>();
    }

    /**
     * Records a visit to a location, updating visited locations and frequency
     */
    public void recordVisit(BlockPos position) {
        visitedLocations.add(position);
        visitFrequency.put(position, visitFrequency.getOrDefault(position, 0) + 1);
    }

    /**
     * Checks if a position is within the current home range
     */
    public boolean isWithinRange(BlockPos position) {
        return distanceToCenter(position) <= radius;
    }

    /**
     * Gets the center point of the home range
     */
    public BlockPos getCenter() {
        return centerPoint;
    }

    /**
     * Gets the radius of the home range in blocks
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Finds the most familiar area based on visit frequency
     */
    public BlockPos getMostFamiliarArea() {
        if (visitFrequency.isEmpty()) {
            return centerPoint;
        }

        BlockPos mostFamiliar = centerPoint;
        int maxVisits = 0;

        for (Map.Entry<BlockPos, Integer> entry : visitFrequency.entrySet()) {
            if (entry.getValue() > maxVisits) {
                maxVisits = entry.getValue();
                mostFamiliar = entry.getKey();
            }
        }

        return mostFamiliar;
    }

    /**
     * Gradually expands the home range if the animal visits areas outside of it
     */
    public void expandRange(BlockPos newPosition) {
        double distanceToNew = distanceToCenter(newPosition);

        if (distanceToNew > radius) {
            // Gradually expand radius towards the new position
            double expansion = (distanceToNew - radius) * 0.1; // 10% of excess distance
            radius += expansion;
        }
    }

    /**
     * Calculates familiarity at a position based on visit frequency (0-1 scale)
     * Returns higher values for frequently visited locations.
     * Familiarity decays with distance from actual visited locations.
     */
    public double getFamiliarityAt(BlockPos position) {
        if (visitFrequency.isEmpty()) {
            return 0.0;
        }

        int maxVisits = getMaxVisitFrequency();
        if (maxVisits == 0) {
            return 0.0;
        }

        double maxFamiliarity = 0.0;
        double familiarityRadius = 5.0; // Blocks within which familiarity spreads

        for (Map.Entry<BlockPos, Integer> entry : visitFrequency.entrySet()) {
            BlockPos visitedPos = entry.getKey();
            int visits = entry.getValue();
            double distance = distanceBetween(position, visitedPos);

            if (distance <= familiarityRadius) {
                // Familiarity decreases with distance from visited location
                double distanceDecay = 1.0 - (distance / familiarityRadius);
                double locationFamiliarity = ((double) visits / maxVisits) * distanceDecay;
                maxFamiliarity = Math.max(maxFamiliarity, locationFamiliarity);
            }
        }

        return maxFamiliarity;
    }

    /**
     * Calculates the overall familiarity with the home range based on visit coverage.
     * Returns the percentage of the home range area that has been visited.
     */
    public double getOverallFamiliarity() {
        if (visitedLocations.isEmpty()) {
            return 0.0;
        }

        // Estimate coverage by visited locations vs. theoretical maximum
        // This is a simplified heuristic - in practice would use spatial sampling
        int uniqueVisits = visitedLocations.size();
        double areaEstimate = Math.PI * radius * radius;

        // Assume each visit covers roughly a 3x3 area
        double coverageArea = uniqueVisits * 9.0;
        return Math.min(1.0, coverageArea / areaEstimate);
    }

    /**
     * Updates the center point of the home range
     */
    public void setCenter(BlockPos newCenter) {
        this.centerPoint = newCenter;
    }

    /**
     * Sets the radius of the home range
     */
    public void setRadius(double newRadius) {
        this.radius = Math.max(0, newRadius);
    }

    /**
     * Gets all visited locations
     */
    public Set<BlockPos> getVisitedLocations() {
        return new HashSet<>(visitedLocations);
    }

    /**
     * Helper method to calculate distance from center to a position
     */
    private double distanceToCenter(BlockPos position) {
        double dx = position.getX() - centerPoint.getX();
        double dy = position.getY() - centerPoint.getY();
        double dz = position.getZ() - centerPoint.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Helper method to get the maximum visit frequency
     */
    private int getMaxVisitFrequency() {
        return visitFrequency.values().stream()
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * Helper method to calculate distance between two positions
     */
    private double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
