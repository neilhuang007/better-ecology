package me.javavirtualenv.behavior.rabbit;

/**
 * Configuration for rabbit foraging behavior.
 */
public class RabbitForagingConfig {
    private final int searchRadius;
    private final int eatCooldown;
    private final boolean destroysCrops;
    private final double cropDestructionChance;
    private final double snowDigChance;
    private final int snowDigCooldown;
    private final double standChance;
    private final int standDuration;

    private RabbitForagingConfig(Builder builder) {
        this.searchRadius = builder.searchRadius;
        this.eatCooldown = builder.eatCooldown;
        this.destroysCrops = builder.destroysCrops;
        this.cropDestructionChance = builder.cropDestructionChance;
        this.snowDigChance = builder.snowDigChance;
        this.snowDigCooldown = builder.snowDigCooldown;
        this.standChance = builder.standChance;
        this.standDuration = builder.standDuration;
    }

    public static RabbitForagingConfig createDefault() {
        return new Builder()
            .searchRadius(4)
            .eatCooldown(40)
            .destroysCrops(true)
            .cropDestructionChance(0.5)
            .snowDigChance(0.3)
            .snowDigCooldown(60)
            .standChance(0.1)
            .standDuration(60)
            .build();
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public int getEatCooldown() {
        return eatCooldown;
    }

    public boolean destroysCrops() {
        return destroysCrops;
    }

    public double getCropDestructionChance() {
        return cropDestructionChance;
    }

    public double getSnowDigChance() {
        return snowDigChance;
    }

    public int getSnowDigCooldown() {
        return snowDigCooldown;
    }

    public double getStandChance() {
        return standChance;
    }

    public int getStandDuration() {
        return standDuration;
    }

    public static class Builder {
        private int searchRadius = 4;
        private int eatCooldown = 40;
        private boolean destroysCrops = true;
        private double cropDestructionChance = 0.5;
        private double snowDigChance = 0.3;
        private int snowDigCooldown = 60;
        private double standChance = 0.1;
        private int standDuration = 60;

        public Builder searchRadius(int radius) {
            this.searchRadius = radius;
            return this;
        }

        public Builder eatCooldown(int ticks) {
            this.eatCooldown = ticks;
            return this;
        }

        public Builder destroysCrops(boolean destroys) {
            this.destroysCrops = destroys;
            return this;
        }

        public Builder cropDestructionChance(double chance) {
            this.cropDestructionChance = chance;
            return this;
        }

        public Builder snowDigChance(double chance) {
            this.snowDigChance = chance;
            return this;
        }

        public Builder snowDigCooldown(int ticks) {
            this.snowDigCooldown = ticks;
            return this;
        }

        public Builder standChance(double chance) {
            this.standChance = chance;
            return this;
        }

        public Builder standDuration(int ticks) {
            this.standDuration = ticks;
            return this;
        }

        public RabbitForagingConfig build() {
            return new RabbitForagingConfig(this);
        }
    }
}
