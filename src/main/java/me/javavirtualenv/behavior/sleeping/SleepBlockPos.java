package me.javavirtualenv.behavior.sleeping;

/**
 * Simple POJO for block position.
 * This class does not depend on any Minecraft classes.
 */
public class SleepBlockPos {

    private final int x;
    private final int y;
    private final int z;

    public SleepBlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    /**
     * Returns squared distance to another position.
     */
    public double distSqr(SleepBlockPos other) {
        if (other == null) {
            return Double.MAX_VALUE;
        }
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Returns offset position.
     */
    public SleepBlockPos offset(int dx, int dy, int dz) {
        return new SleepBlockPos(x + dx, y + dy, z + dz);
    }

    /**
     * Returns position above.
     */
    public SleepBlockPos above() {
        return new SleepBlockPos(x, y + 1, z);
    }

    /**
     * Returns position below.
     */
    public SleepBlockPos below() {
        return new SleepBlockPos(x, y - 1, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SleepBlockPos that = (SleepBlockPos) o;
        return x == that.x && y == that.y && z == that.z;
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
        return "SleepBlockPos{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
