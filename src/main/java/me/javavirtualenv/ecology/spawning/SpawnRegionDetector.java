package me.javavirtualenv.ecology.spawning;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.javavirtualenv.ecology.handles.SpawnHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.*;

/**
 * Efficient spawning region detection using flood-fill and spatial hashing.
 * <p>
 * Instead of checking individual positions, this identifies connected regions
 * of valid spawn areas and treats them as single spawning nodes.
 * <p>
 * Algorithm:
 * 1. Scan chunk to find valid spawn positions (memoized)
 * 2. Group adjacent positions into connected regions (flood-fill)
 * 3. Each region becomes a spawning "node" with capacity
 * 4. Regions are cached and only recalculated on chunk modification
 * <p>
 * Time Complexity: O(n) where n = number of blocks in chunk
 * Space Complexity: O(n/region_size) for region storage
 */
public final class SpawnRegionDetector {

    // Chunk dimension
    private static final int CHUNK_SIZE = 16;
    // Maximum region size to prevent memory bloat
    private static final int MAX_REGION_SIZE = 256;

    // Direction vectors for 4-way connectivity
    private static final int[][] DIRECTIONS = {
        {0, 1}, {1, 0}, {0, -1}, {-1, 0}
    };

    private SpawnRegionDetector() {
    }

    /**
     * Detect all spawn regions in a chunk for a given entity type.
     * Returns a list of regions, each with spawn position samples.
     */
    public static List<SpawnRegion> detectRegions(
        ServerLevel level,
        ChunkAccess chunk,
        EntityType<?> entityType,
        SpawnHandle.SpawnConditions conditions,
        int maxSamplesPerRegion
    ) {
        // Step 1: Build validity grid using memoization
        ValidityGrid grid = buildValidityGrid(level, chunk, entityType, conditions);

        // Step 2: Find connected components (regions) using flood-fill
        List<SpawnRegion> regions = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        for (int y = grid.getMinY(); y < grid.getMaxY(); y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    long key = encodePosition(x, y - grid.getMinY(), z);
                    if (visited.contains(key) || !grid.isValid(x, y - grid.getMinY(), z)) {
                        continue;
                    }

                    // Found new region - flood-fill to find all connected positions
                    SpawnRegion region = floodFillRegion(
                        grid, x, y - grid.getMinY(), z, visited,
                        chunk.getPos().getMinBlockX(),
                        chunk.getPos().getMinBlockZ(),
                        grid.getMinY(),
                        maxSamplesPerRegion
                    );

                    if (!region.isEmpty()) {
                        regions.add(region);
                    }
                }
            }
        }

        return regions;
    }

    /**
     * Build a memoized grid of valid spawn positions.
     * This avoids re-checking conditions during flood-fill.
     */
    private static ValidityGrid buildValidityGrid(
        ServerLevel level,
        ChunkAccess chunk,
        EntityType<?> entityType,
        SpawnHandle.SpawnConditions conditions
    ) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        int height = maxY - minY;
        ValidityGrid grid = new ValidityGrid(minY, maxY);

        // Early exit for cave-only entities (use cached cave data)
        if (conditions.inCaves()) {
            CaveDetector.CaveScanResult caves = CaveDetector.scanChunkForCaves(chunk);
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    for (int y = 0; y < height; y++) {
                        if (caves.isCave(x, z, y)) {
                            grid.setValid(x, y, z, true);
                        }
                    }
                }
            }
            return grid;
        }

        // Scan surfaces for non-cave entities
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int surfaceY = CaveDetector.findSurfaceY(chunk,
                    chunk.getPos().getMinBlockX() + x,
                    chunk.getPos().getMinBlockZ() + z,
                    maxY, minY);

                // Check around surface for valid spawn heights
                int checkRange = Math.min(5, surfaceY - minY);
                for (int dy = 0; dy <= checkRange; dy++) {
                    int worldY = surfaceY - dy;
                    if (worldY < minY) break;

                    BlockPos pos = new BlockPos(
                        chunk.getPos().getMinBlockX() + x,
                        worldY,
                        chunk.getPos().getMinBlockZ() + z
                    );

                    if (SpawnConditionChecker.checkSpawnConditions(level, pos, entityType, conditions)) {
                        grid.setValid(x, worldY - minY, z, true);
                    }
                }
            }
        }

        return grid;
    }

    /**
     * Flood-fill algorithm to find all connected valid positions.
     * Uses iterative approach with stack to avoid recursion depth issues.
     */
    private static SpawnRegion floodFillRegion(
        ValidityGrid grid,
        int startX, int startY, int startZ,
        Set<Long> visited,
        int chunkMinX, int chunkMinZ, int chunkMinY,
        int maxSamples
    ) {
        Deque<long[]> stack = new ArrayDeque<>();
        List<BlockPos> positions = new ArrayList<>();

        stack.push(new long[]{startX, startY, startZ});
        visited.add(encodePosition(startX, startY, startZ));

        int regionSize = 0;
        Random random = new Random(chunkMinX * 31L + chunkMinZ + startY);

        while (!stack.isEmpty() && regionSize < MAX_REGION_SIZE) {
            long[] current = stack.pop();
            int x = (int) current[0];
            int y = (int) current[1];
            int z = (int) current[2];

            // Add position to region (sample randomly)
            if (positions.size() < maxSamples || random.nextInt(maxSamples + 1) == 0) {
                if (positions.size() < maxSamples) {
                    positions.add(new BlockPos(
                        chunkMinX + x,
                        chunkMinY + y,
                        chunkMinZ + z
                    ));
                } else {
                    // Replace random position
                    int idx = random.nextInt(positions.size());
                    positions.set(idx, new BlockPos(
                        chunkMinX + x,
                        chunkMinY + y,
                        chunkMinZ + z
                    ));
                }
            }
            regionSize++;

            // Check neighbors
            for (int[] dir : DIRECTIONS) {
                int nx = x + dir[0];
                int ny = y; // No vertical connectivity in regions (same Y level)
                int nz = z + dir[1];

                if (nx < 0 || nx >= CHUNK_SIZE || nz < 0 || nz >= CHUNK_SIZE) {
                    continue;
                }

                long key = encodePosition(nx, ny, nz);
                if (visited.contains(key) || !grid.isValid(nx, ny, nz)) {
                    continue;
                }

                visited.add(key);
                stack.push(new long[]{nx, ny, nz});
            }
        }

        return new SpawnRegion(positions, regionSize);
    }

    /**
     * Encode position to a single long key for hashing.
     * Format: [x:4][z:4][y:12] = 20 bits (fits in long)
     */
    private static long encodePosition(int x, int y, int z) {
        return ((long) x & 0xF) << 16 | ((long) z & 0xF) << 12 | (y & 0xFF);
    }

    /**
     * Memoized grid of valid spawn positions.
     * Uses BitSet for memory efficiency.
     */
    private static final class ValidityGrid {
        private final java.util.BitSet validPositions;
        private final int minY;
        private final int maxY;
        private final int height;

        ValidityGrid(int minY, int maxY) {
            this.minY = minY;
            this.maxY = maxY;
            this.height = maxY - minY;
            this.validPositions = new java.util.BitSet(CHUNK_SIZE * height * CHUNK_SIZE);
        }

        void setValid(int x, int y, int z, boolean valid) {
            int index = y * CHUNK_SIZE * CHUNK_SIZE + z * CHUNK_SIZE + x;
            validPositions.set(index, valid);
        }

        boolean isValid(int x, int y, int z) {
            return validPositions.get(y * CHUNK_SIZE * CHUNK_SIZE + z * CHUNK_SIZE + x);
        }

        int getMinY() {
            return minY;
        }

        int getMaxY() {
            return maxY;
        }
    }

    /**
     * Represents a connected region of valid spawn positions.
     * Acts as a single spawning node with calculated capacity.
     */
    public static final class SpawnRegion {
        private final List<BlockPos> samplePositions;
        private final int totalSize;
        private int spawnCount = 0;

        SpawnRegion(List<BlockPos> samplePositions, int totalSize) {
            this.samplePositions = samplePositions;
            this.totalSize = totalSize;
        }

        /**
         * Get a sample position from this region for spawning.
         * Returns null if region is exhausted.
         */
        public BlockPos getPositionForSpawn() {
            if (samplePositions.isEmpty()) {
                return null;
            }
            int idx = spawnCount % samplePositions.size();
            spawnCount++;
            return samplePositions.get(idx);
        }

        /**
         * Get the total number of positions in this region.
         */
        public int getTotalSize() {
            return totalSize;
        }

        /**
         * Get the number of sample positions.
         */
        public int getSampleCount() {
            return samplePositions.size();
        }

        /**
         * Check if this region is empty.
         */
        public boolean isEmpty() {
            return samplePositions.isEmpty();
        }

        /**
         * Calculate spawn capacity based on region size.
         * Larger regions support more spawns.
         */
        public int calculateCapacity(double density) {
            return Math.max(1, (int) (totalSize * density));
        }

        /**
         * Get all sample positions.
         */
        public List<BlockPos> getSamplePositions() {
            return Collections.unmodifiableList(samplePositions);
        }
    }
}
