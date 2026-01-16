package me.javavirtualenv.behavior.pathfinding.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Custom pathfinding node evaluator that adds slope-aware and terrain-preference costs
 * to vanilla pathfinding behavior.
 *
 * <p>Key features:
 * <ul>
 *   <li>Slope cost calculation based on research: COT = 1.0 + (0.15 × slope_degrees) for uphill</li>
 *   <li>Ridgeline/exposure penalties for prey animals</li>
 *   <li>Gentle downhill bonus for energy efficiency</li>
 *   <li>Steep downhill penalty for braking costs</li>
 * </ul>
 *
 * <p>Research basis: Cost of Transport (COT) increases linearly with slope angle.
 * Prey animals avoid exposed high ground (ridgelines) to reduce predation risk.
 */
public class EcologyNodeEvaluator extends WalkNodeEvaluator {

    // Terrain preference constants
    private static final float SLOPE_COST_MULTIPLIER = 0.15f;
    private static final float GENTLE_DOWNHILL_BONUS = 0.05f;
    private static final float STEEP_DOWNHILL_PENALTY = 0.10f;
    private static final float STEEP_SLOPE_THRESHOLD = 20.0f;
    private static final float RIDGELINE_PENALTY = 8.0f;

    /**
     * Calculates slope-based traversal cost between two points.
     *
     * <p>Formula:
     * <ul>
     *   <li>Uphill: cost = 0.15 × slope_degrees (based on COT research)</li>
     *   <li>Gentle downhill (< 20°): bonus = -0.05 × slope_degrees</li>
     *   <li>Steep downhill (≥ 20°): cost = 0.10 × slope_degrees (braking)</li>
     * </ul>
     *
     * @param fromX starting X coordinate
     * @param fromY starting Y coordinate
     * @param fromZ starting Z coordinate
     * @param toX target X coordinate
     * @param toY target Y coordinate
     * @param toZ target Z coordinate
     * @return slope cost modifier (positive = cost, negative = bonus)
     */
    private float calculateSlopeCost(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        int heightDiff = toY - fromY;
        if (heightDiff == 0) {
            return 0.0f;
        }

        // Calculate horizontal distance
        double horizontalDist = Math.sqrt((toX - fromX) * (toX - fromX) + (toZ - fromZ) * (toZ - fromZ));
        if (horizontalDist < 0.1) {
            horizontalDist = 0.1; // Prevent division by zero
        }

        // Calculate slope angle
        double slopeRad = Math.atan2(Math.abs(heightDiff), horizontalDist);
        float slopeDeg = (float) Math.toDegrees(slopeRad);

        // Apply cost formula
        if (heightDiff > 0) {
            // Uphill: COT = 1.0 + (0.15 * slope)
            return SLOPE_COST_MULTIPLIER * slopeDeg;
        } else {
            // Downhill: gentle = slight bonus, steep = cost
            if (slopeDeg < STEEP_SLOPE_THRESHOLD) {
                return -GENTLE_DOWNHILL_BONUS * slopeDeg; // slight benefit
            } else {
                return STEEP_DOWNHILL_PENALTY * slopeDeg; // braking cost
            }
        }
    }

    /**
     * Calculates exposure/ridgeline cost for prey animals.
     *
     * <p>Prey animals receive a significant penalty for traversing exposed high ground
     * (ridgelines) to reduce predation risk. Predators do not receive this penalty.
     *
     * @param x X coordinate of the position
     * @param y Y coordinate of the position
     * @param z Z coordinate of the position
     * @param mob the mob being pathfound for
     * @return exposure cost (8.0 for ridgelines, 0.0 otherwise)
     */
    private float calculateExposureCost(int x, int y, int z, Mob mob) {
        // Only apply to prey animals
        if (!isPrey(mob)) {
            return 0.0f;
        }

        // Check if this is exposed high ground
        if (isRidgeline(x, y, z)) {
            return RIDGELINE_PENALTY;
        }

        return 0.0f;
    }

    /**
     * Determines if a mob is a prey animal.
     *
     * <p>Prey animals are passive animals that are not predators (wolves, foxes).
     *
     * @param mob the mob to check
     * @return true if the mob is a prey animal
     */
    private boolean isPrey(Mob mob) {
        // Check for common prey animal types
        return mob instanceof Animal && !(mob instanceof Wolf) && !(mob instanceof Fox);
    }

    /**
     * Detects if a position is a ridgeline (exposed high ground).
     *
     * <p>A position is considered a ridgeline if 6 or more of the 8 surrounding
     * horizontal blocks are at a lower elevation.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if the position is a ridgeline
     */
    private boolean isRidgeline(int x, int y, int z) {
        if (this.currentContext == null) {
            return false;
        }

        int lowerNeighbors = 0;

        // Check 8 surrounding horizontal positions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue; // Skip center
                }

                int neighborX = x + dx;
                int neighborZ = z + dz;

                // Find ground level at neighbor position
                int neighborY = findGroundLevel(neighborX, y, neighborZ);

                if (neighborY < y) {
                    lowerNeighbors++;
                }
            }
        }

        // Ridgeline if 6+ of 8 neighbors are lower
        return lowerNeighbors >= 6;
    }

    /**
     * Finds the ground level at a given position by searching downward.
     *
     * @param x X coordinate
     * @param startY starting Y coordinate to search from
     * @param z Z coordinate
     * @return ground level Y coordinate
     */
    private int findGroundLevel(int x, int startY, int z) {
        if (this.currentContext == null) {
            return startY;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);

        // Search downward for solid ground
        for (int y = startY; y >= startY - 5; y--) {
            pos.setY(y);
            if (!this.currentContext.getBlockState(pos).isAir() &&
                this.currentContext.getBlockState(pos).isSolid()) {
                return y;
            }
        }

        // Search upward if no ground found below
        for (int y = startY + 1; y <= startY + 5; y++) {
            pos.setY(y);
            if (!this.currentContext.getBlockState(pos).isAir() &&
                this.currentContext.getBlockState(pos).isSolid()) {
                return y;
            }
        }

        return startY;
    }

    /**
     * Evaluates neighboring nodes and adds slope and exposure costs.
     *
     * <p>This override adds custom cost modifiers to each neighbor node based on:
     * <ul>
     *   <li>Slope cost (based on elevation change)</li>
     *   <li>Exposure cost (for prey animals on ridgelines)</li>
     * </ul>
     *
     * @param nodes array to fill with neighbor nodes
     * @param node current node being evaluated
     * @return number of valid neighbors found
     */
    @Override
    public int getNeighbors(Node[] nodes, Node node) {
        int count = super.getNeighbors(nodes, node);

        // Add slope costs to each neighbor
        for (int i = 0; i < count; i++) {
            Node neighbor = nodes[i];

            float slopeCost = calculateSlopeCost(
                node.x, node.y, node.z,
                neighbor.x, neighbor.y, neighbor.z
            );

            float exposureCost = calculateExposureCost(
                neighbor.x, neighbor.y, neighbor.z,
                this.mob
            );

            // Add costs to existing malus
            neighbor.costMalus = Math.max(
                neighbor.costMalus,
                neighbor.costMalus + slopeCost + exposureCost
            );
        }

        return count;
    }
}
