package me.javavirtualenv.ecology.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Efficient cave detection using scanline algorithm.
 * <p>
 * Caves are detected by scanning columns from top to bottom and finding
 * enclosed air spaces. Uses a state machine to track cave entrances vs interiors.
 * <p>
 * Time Complexity: O(chunk_width * chunk_depth * height)
 * Space Complexity: O(1) - only tracks state per column
 */
public final class CaveDetector {

    // Minimum cave ceiling height (to distinguish from overhangs)
    private static final int MIN_CAVE_HEIGHT = 3;
    // Minimum cave depth from surface
    private static final int MIN_CAVE_DEPTH = 5;

    private CaveDetector() {
    }

    /**
     * Check if a position is inside a cave.
     * A position is in a cave if:
     * - It's below ground level (not open to sky)
     * - It has solid blocks above and around it
     * - The space is large enough to be a cave
     */
    public static boolean isInCave(Level level, BlockPos pos) {
        // Quick check: is there solid block above?
        if (level.canSeeSky(pos.above())) {
            return false;
        }

        // Check if we're deep enough underground
        BlockPos surfacePos = findSurfaceAbove(level, pos);
        if (surfacePos == null) {
            return false;
        }
        int depth = surfacePos.getY() - pos.getY();
        if (depth < MIN_CAVE_DEPTH) {
            return false;
        }

        // Check cave ceiling height
        int ceilingHeight = measureCaveHeight(level, pos);
        if (ceilingHeight < MIN_CAVE_HEIGHT) {
            return false;
        }

        // Check horizontal enclosure (cave walls)
        return hasSolidWalls(level, pos);
    }

    /**
     * Find all cave positions in a chunk using scanline algorithm.
     * Much more efficient than checking every position individually.
     *
     * @param chunk The chunk to scan
     * @return BitSet where bit index = local_y + local_x * height + local_z * width * height
     */
    public static CaveScanResult scanChunkForCaves(ChunkAccess chunk) {
        CaveScanResult result = new CaveScanResult();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        // Scan each column in the chunk
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                scanColumn(chunk, minX + localX, minZ + localZ, minY, maxY, result);
            }
        }

        return result;
    }

    /**
     * Scan a single column for cave spaces using state machine.
     * States: SURFACE -> TRANSITION -> CAVE -> TRANSITION -> DEEP
     */
    private static void scanColumn(ChunkAccess chunk, int x, int z, int minY, int maxY, CaveScanResult result) {
        enum ColumnState { SURFACE, TRANSITION, CAVE, DEEP }

        ColumnState state = ColumnState.SURFACE;
        int caveStart = -1;
        int surfaceY = findSurfaceY(chunk, x, z, maxY, minY);

        for (int y = surfaceY; y >= minY; y--) {
            boolean isSolid = !chunk.getBlockState(new BlockPos(x, y, z)).isAir();

            switch (state) {
                case SURFACE:
                    if (!isSolid) {
                        state = ColumnState.TRANSITION;
                        caveStart = y;
                    }
                    break;

                case TRANSITION:
                    if (isSolid) {
                        state = ColumnState.SURFACE;
                        caveStart = -1;
                    } else if (surfaceY - y >= MIN_CAVE_DEPTH) {
                        state = ColumnState.CAVE;
                    }
                    break;

                case CAVE:
                    if (isSolid) {
                        // Mark the cave segment
                        for (int cy = caveStart; cy > y; cy--) {
                            if (surfaceY - cy >= MIN_CAVE_DEPTH) {
                                result.addCavePosition(toLocalX(x), toLocalZ(z), cy - minY);
                            }
                        }
                        state = ColumnState.SURFACE;
                        caveStart = -1;
                    }
                    break;
            }
        }
    }

    /**
     * Find the surface Y coordinate for a column.
     * Returns the first non-air block from top down.
     */
    public static int findSurfaceY(ChunkAccess chunk, int x, int z, int maxY, int minY) {
        for (int y = maxY - 1; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            if (!state.isAir() && state.getBlock() != Blocks.WATER) {
                return y + 1; // First air block above surface
            }
        }
        return minY;
    }

    /**
     * Find surface position above a given position.
     */
    private static BlockPos findSurfaceAbove(Level level, BlockPos pos) {
        for (int y = pos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.canSeeSky(checkPos)) {
                return checkPos;
            }
            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir()) {
                return new BlockPos(pos.getX(), y - 1, pos.getZ());
            }
        }
        return null;
    }

    /**
     * Measure cave height from position upward.
     */
    private static int measureCaveHeight(Level level, BlockPos pos) {
        int height = 0;
        for (int y = pos.getY(); y < level.getMaxBuildHeight(); y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState state = level.getBlockState(checkPos);
            if (state.isAir()) {
                height++;
            } else {
                break;
            }
        }
        return height;
    }

    /**
     * Check if position has solid walls (cave-like enclosure).
     */
    private static boolean hasSolidWalls(Level level, BlockPos pos) {
        int solidCount = 0;
        int totalChecks = 0;

        // Check 4 horizontal directions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockState state = level.getBlockState(checkPos);
                totalChecks++;
                if (!state.isAir()) {
                    solidCount++;
                }
            }
        }

        // At least 50% of surroundings should be solid
        return solidCount >= totalChecks / 2;
    }

    private static int toLocalX(int worldX) {
        return worldX & 15;
    }

    private static int toLocalZ(int worldZ) {
        return worldZ & 15;
    }

    /**
     * Result of a cave scan containing all cave positions in a chunk.
     * Uses a compact BitSet representation for memory efficiency.
     */
    public static final class CaveScanResult {
        // BitSet where each bit represents a block position
        // Index = y + x * height + z * width * height
        private final java.util.BitSet cavePositions;
        private final int minY;
        private final int maxY;
        private int count = 0;

        public CaveScanResult() {
            this.minY = -64; // Default, will be set properly
            this.maxY = 320;
            this.cavePositions = new java.util.BitSet(16 * 16 * (maxY - minY));
        }

        public CaveScanResult(int minY, int maxY) {
            this.minY = minY;
            this.maxY = maxY;
            int height = maxY - minY;
            this.cavePositions = new java.util.BitSet(16 * 16 * height);
        }

        void addCavePosition(int localX, int localZ, int localY) {
            int height = maxY - minY;
            int index = localY + localX * height + localZ * 16 * height;
            cavePositions.set(index);
            count++;
        }

        public boolean isCave(int localX, int localZ, int localY) {
            int height = maxY - minY;
            int index = localY + localX * height + localZ * 16 * height;
            return cavePositions.get(index);
        }

        public int getCount() {
            return count;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        /**
         * Get a random cave position in this chunk.
         */
        public BlockPos getRandomCavePosition(int chunkMinX, int chunkMinZ, RandomSource random) {
            if (isEmpty()) {
                return null;
            }

            int targetIndex = random.nextInt(count);
            int found = 0;
            int height = maxY - minY;

            for (int i = 0; i < cavePositions.length(); i++) {
                if (cavePositions.get(i)) {
                    if (found == targetIndex) {
                        // Decode index to x, y, z
                        int localY = i % height;
                        int remainder = i / height;
                        int localX = remainder % 16;
                        int localZ = remainder / 16;

                        return new BlockPos(
                            chunkMinX + localX,
                            minY + localY,
                            chunkMinZ + localZ
                        );
                    }
                    found++;
                }
            }

            return null;
        }
    }
}
