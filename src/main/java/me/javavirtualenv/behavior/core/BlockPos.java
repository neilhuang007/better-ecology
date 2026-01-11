package me.javavirtualenv.behavior.core;

/**
 * Simple POJO representing a block position with x, y, z coordinates.
 * Used in behavior calculations for grid-based operations like patch selection.
 * This is a test-friendly version of net.minecraft.core.BlockPos.
 */
public class BlockPos {
    public final int x;
    public final int y;
    public final int z;

    /**
     * Creates a block position at the origin.
     */
    public BlockPos() {
        this(0, 0, 0);
    }

    /**
     * Creates a block position at the specified coordinates.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a block position from double coordinates (truncates to int).
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public BlockPos(double x, double y, double z) {
        this.x = (int) Math.floor(x);
        this.y = (int) Math.floor(y);
        this.z = (int) Math.floor(z);
    }

    /**
     * Creates a block position from a Vec3d (truncates to int).
     * @param vec Position vector
     */
    public BlockPos(Vec3d vec) {
        this.x = (int) Math.floor(vec.x);
        this.y = (int) Math.floor(vec.y);
        this.z = (int) Math.floor(vec.z);
    }

    /**
     * Gets the X coordinate.
     * @return X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the Y coordinate.
     * @return Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the Z coordinate.
     * @return Z coordinate
     */
    public int getZ() {
        return z;
    }

    /**
     * Calculates the squared distance from this position to another.
     * @param other The other position
     * @return Squared distance
     */
    public double distSqr(BlockPos other) {
        if (other == null) {
            return Double.MAX_VALUE;
        }
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the squared distance from this position to a vector.
     * @param vec The position vector
     * @return Squared distance
     */
    public double distSqr(Vec3d vec) {
        if (vec == null) {
            return Double.MAX_VALUE;
        }
        double dx = this.x - vec.x;
        double dy = this.y - vec.y;
        double dz = this.z - vec.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Gets a new BlockPos offset by the specified amounts.
     * @param dx X offset
     * @param dy Y offset
     * @param dz Z offset
     * @return New offset position
     */
    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
    }

    /**
     * Checks if this position is within the specified bounds.
     * @param minX Minimum X (inclusive)
     * @param minY Minimum Y (inclusive)
     * @param minZ Minimum Z (inclusive)
     * @param maxX Maximum X (inclusive)
     * @param maxY Maximum Y (inclusive)
     * @param maxZ Maximum Z (inclusive)
     * @return True if within bounds
     */
    public boolean isInBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockPos)) return false;
        BlockPos other = (BlockPos) obj;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "BlockPos{x=" + x + ", y=" + y + ", z=" + z + "}";
    }

    /**
     * Converts this BlockPos to a Minecraft BlockPos.
     * This is a convenience method for Minecraft-specific code.
     * Note: This returns Object to avoid hard dependency on Minecraft classes in behavior core.
     * @return Object that can be cast to net.minecraft.core.BlockPos, or this if conversion fails
     */
    public Object toMcBlockPos() {
        try {
            Class<?> mcBlockPosClass = Class.forName("net.minecraft.core.BlockPos");
            java.lang.reflect.Constructor<?> constructor = mcBlockPosClass.getConstructor(int.class, int.class, int.class);
            return constructor.newInstance(x, y, z);
        } catch (Throwable e) {
            // If any reflection fails, return our BlockPos as fallback
            return this;
        }
    }

    /**
     * Creates a behavior BlockPos from a Minecraft BlockPos.
     * This is a convenience method for adapting Minecraft entities.
     * @param mcBlockPos Object that should be net.minecraft.core.BlockPos
     * @return A new BlockPos, or null if conversion fails
     */
    public static BlockPos fromMcBlockPos(Object mcBlockPos) {
        if (mcBlockPos == null) {
            return null;
        }
        if (mcBlockPos instanceof BlockPos) {
            return (BlockPos) mcBlockPos;
        }
        try {
            // Use reflection to extract coordinates from Minecraft BlockPos
            Class<?> mcBlockPosClass = Class.forName("net.minecraft.core.BlockPos");
            int x = (int) mcBlockPosClass.getMethod("getX").invoke(mcBlockPos);
            int y = (int) mcBlockPosClass.getMethod("getY").invoke(mcBlockPos);
            int z = (int) mcBlockPosClass.getMethod("getZ").invoke(mcBlockPos);
            return new BlockPos(x, y, z);
        } catch (Exception e) {
            // If conversion fails, return null
            return null;
        }
    }
}
