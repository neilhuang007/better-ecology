package me.javavirtualenv.behavior.rabbit;

/**
 * Configuration for rabbit evasion behavior.
 */
public class RabbitEvasionConfig {
    private final double evasionSpeed;
    private final double evasionForce;
    private final double detectionRange;
    private final double flightInitiationDistance;
    private final double safetyDistance;
    private final int zigzagChangeInterval;
    private final boolean canFreeze;
    private final int freezeDuration;
    private final double jumpChance;

    private RabbitEvasionConfig(Builder builder) {
        this.evasionSpeed = builder.evasionSpeed;
        this.evasionForce = builder.evasionForce;
        this.detectionRange = builder.detectionRange;
        this.flightInitiationDistance = builder.flightInitiationDistance;
        this.safetyDistance = builder.safetyDistance;
        this.zigzagChangeInterval = builder.zigzagChangeInterval;
        this.canFreeze = builder.canFreeze;
        this.freezeDuration = builder.freezeDuration;
        this.jumpChance = builder.jumpChance;
    }

    public static RabbitEvasionConfig createDefault() {
        return new Builder()
            .evasionSpeed(1.8)
            .evasionForce(0.25)
            .detectionRange(32.0)
            .flightInitiationDistance(8.0)
            .safetyDistance(48.0)
            .zigzagChangeInterval(8)
            .canFreeze(true)
            .freezeDuration(40)
            .jumpChance(0.3)
            .build();
    }

    // Getters
    public double getEvasionSpeed() {
        return evasionSpeed;
    }

    public double getEvasionForce() {
        return evasionForce;
    }

    public double getDetectionRange() {
        return detectionRange;
    }

    public double getFlightInitiationDistance() {
        return flightInitiationDistance;
    }

    public double getSafetyDistance() {
        return safetyDistance;
    }

    public int getZigzagChangeInterval() {
        return zigzagChangeInterval;
    }

    public boolean canFreeze() {
        return canFreeze;
    }

    public int getFreezeDuration() {
        return freezeDuration;
    }

    public double getJumpChance() {
        return jumpChance;
    }

    public static class Builder {
        private double evasionSpeed = 1.8;
        private double evasionForce = 0.25;
        private double detectionRange = 32.0;
        private double flightInitiationDistance = 8.0;
        private double safetyDistance = 48.0;
        private int zigzagChangeInterval = 8;
        private boolean canFreeze = true;
        private int freezeDuration = 40;
        private double jumpChance = 0.3;

        public Builder evasionSpeed(double speed) {
            this.evasionSpeed = speed;
            return this;
        }

        public Builder evasionForce(double force) {
            this.evasionForce = force;
            return this;
        }

        public Builder detectionRange(double range) {
            this.detectionRange = range;
            return this;
        }

        public Builder flightInitiationDistance(double distance) {
            this.flightInitiationDistance = distance;
            return this;
        }

        public Builder safetyDistance(double distance) {
            this.safetyDistance = distance;
            return this;
        }

        public Builder zigzagChangeInterval(int ticks) {
            this.zigzagChangeInterval = ticks;
            return this;
        }

        public Builder canFreeze(boolean canFreeze) {
            this.canFreeze = canFreeze;
            return this;
        }

        public Builder freezeDuration(int ticks) {
            this.freezeDuration = ticks;
            return this;
        }

        public Builder jumpChance(double chance) {
            this.jumpChance = chance;
            return this;
        }

        public RabbitEvasionConfig build() {
            return new RabbitEvasionConfig(this);
        }
    }
}
