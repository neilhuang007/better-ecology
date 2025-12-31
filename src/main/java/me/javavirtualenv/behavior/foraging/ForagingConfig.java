package me.javavirtualenv.behavior.foraging;

import net.minecraft.world.entity.animal.Animal;

/**
 * Configuration for foraging behaviors based on species type.
 * Implements species-specific parameters derived from ecological research.
 */
public class ForagingConfig {
    // Grazing time windows (in Minecraft day ticks, 0-24000)
    private final long grazingStartTime;
    private final long grazingEndTime;
    private final long middayRestStart;
    private final long middayRestEnd;

    // Patch selection parameters
    private final int patchSize;
    private final double givingUpDensity;
    private final int biteSize;
    private final double grazingSpeed;

    // Behavior modifiers
    private final double socialDominanceFactor;
    private final int memoryDuration;
    private final double searchRadius;
    private final int hungerRestore;
    private final int hydrationRestoreAmount;

    private ForagingConfig(Builder builder) {
        this.grazingStartTime = builder.grazingStartTime;
        this.grazingEndTime = builder.grazingEndTime;
        this.middayRestStart = builder.middayRestStart;
        this.middayRestEnd = builder.middayRestEnd;
        this.patchSize = builder.patchSize;
        this.givingUpDensity = builder.givingUpDensity;
        this.biteSize = builder.biteSize;
        this.grazingSpeed = builder.grazingSpeed;
        this.socialDominanceFactor = builder.socialDominanceFactor;
        this.memoryDuration = builder.memoryDuration;
        this.searchRadius = builder.searchRadius;
        this.hungerRestore = builder.hungerRestore;
        this.hydrationRestoreAmount = builder.hydrationRestoreAmount;
    }

    /**
     * Creates a bimodal grazing config (cattle, sheep pattern).
     * Morning and afternoon peaks with midday rest.
     */
    public static ForagingConfig createBimodal() {
        return new Builder()
            .grazingStartTime(1000)
            .grazingEndTime(11000)
            .middayRestStart(5000)
            .middayRestEnd(7000)
            .patchSize(5)
            .givingUpDensity(0.3)
            .biteSize(1)
            .grazingSpeed(0.3)
            .socialDominanceFactor(0.5)
            .memoryDuration(6000)
            .searchRadius(20.0)
            .hungerRestore(2)
            .hydrationRestoreAmount(5)
            .build();
    }

    /**
     * Creates a continuous grazing config (horses pattern).
     * Steady grazing throughout the day without pronounced rest periods.
     */
    public static ForagingConfig createContinuous() {
        return new Builder()
            .grazingStartTime(0)
            .grazingEndTime(12000)
            .middayRestStart(0)
            .middayRestEnd(0)
            .patchSize(7)
            .givingUpDensity(0.25)
            .biteSize(2)
            .grazingSpeed(0.25)
            .socialDominanceFactor(0.3)
            .memoryDuration(8000)
            .searchRadius(25.0)
            .hungerRestore(3)
            .hydrationRestoreAmount(7)
            .build();
    }

    /**
     * Creates a selective browsing config (deer, goats pattern).
     * More selective, smaller patches, higher selectivity.
     */
    public static ForagingConfig createSelective() {
        return new Builder()
            .grazingStartTime(500)
            .grazingEndTime(11500)
            .middayRestStart(5500)
            .middayRestEnd(6500)
            .patchSize(3)
            .givingUpDensity(0.5)
            .biteSize(1)
            .grazingSpeed(0.35)
            .socialDominanceFactor(0.2)
            .memoryDuration(4000)
            .searchRadius(15.0)
            .hungerRestore(2)
            .hydrationRestoreAmount(5)
            .build();
    }

    /**
     * Creates config for sheep (selective grazers, prefer small patches).
     */
    public static ForagingConfig createSheep() {
        return new Builder()
            .grazingStartTime(1000)
            .grazingEndTime(11000)
            .middayRestStart(5000)
            .middayRestEnd(7000)
            .patchSize(3)
            .givingUpDensity(0.4)
            .biteSize(1)
            .grazingSpeed(0.35)
            .socialDominanceFactor(0.3)
            .memoryDuration(7000)
            .searchRadius(25.0)
            .hungerRestore(2)
            .hydrationRestoreAmount(5)
            .build();
    }

    /**
     * Creates config for cattle (less selective, larger patches).
     */
    public static ForagingConfig createCattle() {
        return new Builder()
            .grazingStartTime(1000)
            .grazingEndTime(11000)
            .middayRestStart(5000)
            .middayRestEnd(7000)
            .patchSize(7)
            .givingUpDensity(0.25)
            .biteSize(2)
            .grazingSpeed(0.25)
            .socialDominanceFactor(0.6)
            .memoryDuration(5000)
            .searchRadius(20.0)
            .hungerRestore(3)
            .hydrationRestoreAmount(7)
            .build();
    }

    public long getGrazingStartTime() {
        return grazingStartTime;
    }

    public long getGrazingEndTime() {
        return grazingEndTime;
    }

    public long getMiddayRestStart() {
        return middayRestStart;
    }

    public long getMiddayRestEnd() {
        return middayRestEnd;
    }

    public int getPatchSize() {
        return patchSize;
    }

    public double getGivingUpDensity() {
        return givingUpDensity;
    }

    public int getBiteSize() {
        return biteSize;
    }

    public double getGrazingSpeed() {
        return grazingSpeed;
    }

    public double getSocialDominanceFactor() {
        return socialDominanceFactor;
    }

    public int getMemoryDuration() {
        return memoryDuration;
    }

    public double getSearchRadius() {
        return searchRadius;
    }

    public int getHungerRestore() {
        return hungerRestore;
    }

    public int getHydrationRestoreAmount() {
        return hydrationRestoreAmount;
    }

    /**
     * Builder pattern for creating custom configurations.
     */
    public static class Builder {
        private long grazingStartTime = 1000;
        private long grazingEndTime = 11000;
        private long middayRestStart = 5000;
        private long middayRestEnd = 7000;
        private int patchSize = 5;
        private double givingUpDensity = 0.3;
        private int biteSize = 1;
        private double grazingSpeed = 0.3;
        private double socialDominanceFactor = 0.5;
        private int memoryDuration = 6000;
        private double searchRadius = 20.0;
        private int hungerRestore = 2;
        private int hydrationRestoreAmount = 5;

        public Builder grazingStartTime(long time) {
            this.grazingStartTime = time;
            return this;
        }

        public Builder grazingEndTime(long time) {
            this.grazingEndTime = time;
            return this;
        }

        public Builder middayRestStart(long time) {
            this.middayRestStart = time;
            return this;
        }

        public Builder middayRestEnd(long time) {
            this.middayRestEnd = time;
            return this;
        }

        public Builder patchSize(int size) {
            this.patchSize = size;
            return this;
        }

        public Builder givingUpDensity(double density) {
            this.givingUpDensity = density;
            return this;
        }

        public Builder biteSize(int size) {
            this.biteSize = size;
            return this;
        }

        public Builder grazingSpeed(double speed) {
            this.grazingSpeed = speed;
            return this;
        }

        public Builder socialDominanceFactor(double factor) {
            this.socialDominanceFactor = factor;
            return this;
        }

        public Builder memoryDuration(int duration) {
            this.memoryDuration = duration;
            return this;
        }

        public Builder searchRadius(double radius) {
            this.searchRadius = radius;
            return this;
        }

        public Builder hungerRestore(int amount) {
            this.hungerRestore = amount;
            return this;
        }

        public Builder hydrationRestoreAmount(int amount) {
            this.hydrationRestoreAmount = amount;
            return this;
        }

        public ForagingConfig build() {
            return new ForagingConfig(this);
        }
    }
}
