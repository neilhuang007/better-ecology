package me.javavirtualenv.behavior.rabbit;

/**
 * Configuration for rabbit thumping behavior.
 */
public class RabbitThumpConfig {
    private final double thumpDetectionRange;
    private final double thumpMinDistance;
    private final double thumpAlertRange;
    private final int thumpInterval;
    private final int thumpCooldown;
    private final int maxThumps;
    private final boolean chainReaction;

    private RabbitThumpConfig(Builder builder) {
        this.thumpDetectionRange = builder.thumpDetectionRange;
        this.thumpMinDistance = builder.thumpMinDistance;
        this.thumpAlertRange = builder.thumpAlertRange;
        this.thumpInterval = builder.thumpInterval;
        this.thumpCooldown = builder.thumpCooldown;
        this.maxThumps = builder.maxThumps;
        this.chainReaction = builder.chainReaction;
    }

    public static RabbitThumpConfig createDefault() {
        return new Builder()
            .thumpDetectionRange(16.0)
            .thumpMinDistance(4.0)
            .thumpAlertRange(12.0)
            .thumpInterval(10)
            .thumpCooldown(60)
            .maxThumps(3)
            .chainReaction(true)
            .build();
    }

    // Getters
    public double getThumpDetectionRange() {
        return thumpDetectionRange;
    }

    public double getThumpMinDistance() {
        return thumpMinDistance;
    }

    public double getThumpAlertRange() {
        return thumpAlertRange;
    }

    public int getThumpInterval() {
        return thumpInterval;
    }

    public int getThumpCooldown() {
        return thumpCooldown;
    }

    public int getMaxThumps() {
        return maxThumps;
    }

    public boolean isChainReaction() {
        return chainReaction;
    }

    public static class Builder {
        private double thumpDetectionRange = 16.0;
        private double thumpMinDistance = 4.0;
        private double thumpAlertRange = 12.0;
        private int thumpInterval = 10;
        private int thumpCooldown = 60;
        private int maxThumps = 3;
        private boolean chainReaction = true;

        public Builder thumpDetectionRange(double range) {
            this.thumpDetectionRange = range;
            return this;
        }

        public Builder thumpMinDistance(double distance) {
            this.thumpMinDistance = distance;
            return this;
        }

        public Builder thumpAlertRange(double range) {
            this.thumpAlertRange = range;
            return this;
        }

        public Builder thumpInterval(int ticks) {
            this.thumpInterval = ticks;
            return this;
        }

        public Builder thumpCooldown(int ticks) {
            this.thumpCooldown = ticks;
            return this;
        }

        public Builder maxThumps(int count) {
            this.maxThumps = count;
            return this;
        }

        public Builder chainReaction(boolean enabled) {
            this.chainReaction = enabled;
            return this;
        }

        public RabbitThumpConfig build() {
            return new RabbitThumpConfig(this);
        }
    }
}
