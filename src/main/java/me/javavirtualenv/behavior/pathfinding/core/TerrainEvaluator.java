package me.javavirtualenv.behavior.pathfinding.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates terrain characteristics for pathfinding decisions.
 * Analyzes slope, ridgelines, cover, and terrain types to inform realistic movement.
 */
public class TerrainEvaluator {

    public static final float PREFERRED_SLOPE_THRESHOLD = 15.0f;
    public static final float COSTLY_SLOPE_THRESHOLD = 20.0f;
    public static final float PROHIBITIVE_SLOPE_THRESHOLD = 30.0f;

    /**
     * Calculates the slope angle between two positions.
     *
     * @param from Starting position
     * @param to Target position
     * @return Slope angle in degrees (0-90)
     */
    public static float calculateSlope(BlockPos from, BlockPos to) {
        int horizontalDistance = (int) Math.sqrt(
            Math.pow(to.getX() - from.getX(), 2) +
            Math.pow(to.getZ() - from.getZ(), 2)
        );

        if (horizontalDistance == 0) {
            return 0.0f;
        }

        int verticalDistance = to.getY() - from.getY();
        double angleRadians = Math.atan2(Math.abs(verticalDistance), horizontalDistance);
        return (float) Math.toDegrees(angleRadians);
    }

    /**
     * Determines if a position is on a ridgeline (exposed high ground).
     *
     * @param level The world level
     * @param pos Position to check
     * @return True if the position is a ridgeline
     */
    public static boolean isRidgeline(Level level, BlockPos pos) {
        int currentHeight = pos.getY();
        int lowerNeighborCount = 0;

        Direction[] directions = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.NORTH.getClockWise(), Direction.SOUTH.getClockWise(),
            Direction.NORTH.getCounterClockWise(), Direction.SOUTH.getCounterClockWise()
        };

        for (Direction direction : directions) {
            BlockPos neighborPos = pos.offset(direction.getNormal());
            int neighborHeight = getHighestSolidBlock(level, neighborPos.getX(), neighborPos.getZ());

            if (neighborHeight < currentHeight) {
                lowerNeighborCount++;
            }
        }

        return lowerNeighborCount >= 6;
    }

    /**
     * Calculates the cover value at a position based on surrounding solid blocks.
     *
     * @param level The world level
     * @param pos Position to evaluate
     * @return Cover value from 0.0 (no cover) to 1.0 (full cover)
     */
    public static float getCoverValue(Level level, BlockPos pos) {
        int solidBlockCount = 0;
        int totalChecks = 0;

        for (int y = 1; y <= 3; y++) {
            BlockPos abovePos = pos.above(y);
            totalChecks++;
            if (level.getBlockState(abovePos).isSolid()) {
                solidBlockCount++;
            }
        }

        Direction[] cardinalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction direction : cardinalDirections) {
            BlockPos eyeLevelPos = pos.relative(direction).above();
            totalChecks++;
            if (level.getBlockState(eyeLevelPos).isSolid()) {
                solidBlockCount++;
            }
        }

        if (totalChecks == 0) {
            return 0.0f;
        }

        return (float) solidBlockCount / totalChecks;
    }

    /**
     * Gets the heights of surrounding blocks in an 8-direction pattern.
     *
     * @param level The world level
     * @param pos Center position
     * @return Map of directions to heights
     */
    public static Map<Direction, Integer> getSurroundingHeights(Level level, BlockPos pos) {
        Map<Direction, Integer> heightMap = new HashMap<>();

        Direction[] directions = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.NORTH.getClockWise(), Direction.SOUTH.getClockWise(),
            Direction.NORTH.getCounterClockWise(), Direction.SOUTH.getCounterClockWise()
        };

        for (Direction direction : directions) {
            BlockPos neighborPos = pos.offset(direction.getNormal());
            int height = getHighestSolidBlock(level, neighborPos.getX(), neighborPos.getZ());
            heightMap.put(direction, height);
        }

        return heightMap;
    }

    /**
     * Finds the highest solid block at the given x, z coordinates.
     *
     * @param level The world level
     * @param x X coordinate
     * @param z Z coordinate
     * @return Y coordinate of the highest solid block
     */
    public static int getHighestSolidBlock(Level level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /**
     * Classifies the terrain type between two positions.
     *
     * @param level The world level
     * @param from Starting position
     * @param to Target position
     * @return TerrainType classification
     */
    public static TerrainType getTerrainType(Level level, BlockPos from, BlockPos to) {
        float slope = calculateSlope(from, to);

        if (level.getBlockState(to).getFluidState().isSource()) {
            return TerrainType.WATER;
        }

        if (!level.getBlockState(to.below()).isSolid() && to.getY() - from.getY() < -3) {
            return TerrainType.HAZARD;
        }

        if (slope >= PROHIBITIVE_SLOPE_THRESHOLD) {
            return TerrainType.CLIFF;
        }

        if (slope >= COSTLY_SLOPE_THRESHOLD) {
            return TerrainType.STEEP_SLOPE;
        }

        if (slope >= PREFERRED_SLOPE_THRESHOLD) {
            return TerrainType.GENTLE_SLOPE;
        }

        return TerrainType.FLAT;
    }

    /**
     * Enum representing different terrain classifications.
     */
    public enum TerrainType {
        FLAT,
        GENTLE_SLOPE,
        STEEP_SLOPE,
        CLIFF,
        WATER,
        HAZARD
    }
}
