package me.javavirtualenv.behavior.sleeping;

/**
 * Simple POJO representing block information for sleep site selection.
 * This abstraction allows testing without Minecraft BlockState dependencies.
 */
public class SleepBlockInfo {

    private final boolean isAir;
    private final boolean isSolid;
    private final boolean isLeaves;
    private final boolean isCovered;

    private SleepBlockInfo(Builder builder) {
        this.isAir = builder.isAir;
        this.isSolid = builder.isSolid;
        this.isLeaves = builder.isLeaves;
        this.isCovered = builder.isCovered;
    }

    public boolean isAir() {
        return isAir;
    }

    public boolean isSolid() {
        return isSolid;
    }

    public boolean isLeaves() {
        return isLeaves;
    }

    public boolean isCovered() {
        return isCovered;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean isAir = false;
        private boolean isSolid = false;
        private boolean isLeaves = false;
        private boolean isCovered = false;

        public Builder isAir(boolean isAir) {
            this.isAir = isAir;
            return this;
        }

        public Builder isSolid(boolean isSolid) {
            this.isSolid = isSolid;
            return this;
        }

        public Builder isLeaves(boolean isLeaves) {
            this.isLeaves = isLeaves;
            return this;
        }

        public Builder isCovered(boolean isCovered) {
            this.isCovered = isCovered;
            return this;
        }

        public SleepBlockInfo build() {
            return new SleepBlockInfo(this);
        }
    }

    /**
     * Creates a block info for air.
     */
    public static SleepBlockInfo air() {
        return builder().isAir(true).build();
    }

    /**
     * Creates a block info for solid ground.
     */
    public static SleepBlockInfo solid() {
        return builder().isSolid(true).build();
    }

    /**
     * Creates a block info for covered (safe) position.
     */
    public static SleepBlockInfo covered() {
        return builder().isSolid(true).isCovered(true).build();
    }

    /**
     * Creates a block info for leaves.
     */
    public static SleepBlockInfo leaves() {
        return builder().isLeaves(true).build();
    }
}
