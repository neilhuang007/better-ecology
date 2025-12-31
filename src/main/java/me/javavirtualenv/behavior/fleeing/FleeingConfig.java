package me.javavirtualenv.behavior.fleeing;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for fleeing behaviors based on species type.
 * Implements species-specific parameters derived from flight initiation distance (FID) research.
 * <p>
 * Based on research by:
 * - Ydenberg & Dill (1986) - Economics of fleeing from predators
 * - Blumstein et al. (2023) - Flight initiation distance in birds
 * - Stankowich (2008) - Ungulate flight responses (789 citations)
 */
public class FleeingConfig {

    // Flight initiation distance (FID) in blocks
    private final double flightInitiationDistance;

    // FID modifier for different threat types
    private final double ambushPredatorMultiplier;
    private final double cursorialPredatorMultiplier;
    private final double aerialPredatorMultiplier;
    private final double humanThreatMultiplier;

    // Escape strategy preferences
    private final EscapeStrategy primaryStrategy;
    private final EscapeStrategy secondaryStrategy;

    // Strategy-specific parameters
    private final double zigzagIntensity;
    private final int zigzagChangeInterval;
    private final double refugeDetectionRange;
    private final int freezingDuration;

    // Panic behavior parameters
    private final int panicThreshold;
    private final double stampedeSpeedMultiplier;
    private final double panicPropagationRange;

    // Alarm signal parameters
    private final double alarmCallRange;
    private final int alarmCooldown;
    private final boolean crossSpeciesWarning;

    // Recovery parameters
    private final int recoveryTime;
    private final double habituationRate;

    private FleeingConfig(Builder builder) {
        this.flightInitiationDistance = builder.flightInitiationDistance;
        this.ambushPredatorMultiplier = builder.ambushPredatorMultiplier;
        this.cursorialPredatorMultiplier = builder.cursorialPredatorMultiplier;
        this.aerialPredatorMultiplier = builder.aerialPredatorMultiplier;
        this.humanThreatMultiplier = builder.humanThreatMultiplier;
        this.primaryStrategy = builder.primaryStrategy;
        this.secondaryStrategy = builder.secondaryStrategy;
        this.zigzagIntensity = builder.zigzagIntensity;
        this.zigzagChangeInterval = builder.zigzagChangeInterval;
        this.refugeDetectionRange = builder.refugeDetectionRange;
        this.freezingDuration = builder.freezingDuration;
        this.panicThreshold = builder.panicThreshold;
        this.stampedeSpeedMultiplier = builder.stampedeSpeedMultiplier;
        this.panicPropagationRange = builder.panicPropagationRange;
        this.alarmCallRange = builder.alarmCallRange;
        this.alarmCooldown = builder.alarmCooldown;
        this.crossSpeciesWarning = builder.crossSpeciesWarning;
        this.recoveryTime = builder.recoveryTime;
        this.habituationRate = builder.habituationRate;
    }

    /**
     * Creates config for rabbits - high FID, zigzag escape, freeze first.
     * Rabbits use protean movement (unpredictable zigzagging) to evade predators.
     */
    public static FleeingConfig createRabbit() {
        return new Builder()
            .flightInitiationDistance(12.0)
            .primaryStrategy(EscapeStrategy.ZIGZAG)
            .secondaryStrategy(EscapeStrategy.FREEZE)
            .zigzagIntensity(0.8)
            .zigzagChangeInterval(15)
            .refugeDetectionRange(20.0)
            .freezingDuration(40)
            .ambushPredatorMultiplier(0.8)
            .cursorialPredatorMultiplier(1.2)
            .aerialPredatorMultiplier(1.5)
            .humanThreatMultiplier(1.3)
            .panicThreshold(2)
            .stampedeSpeedMultiplier(1.6)
            .panicPropagationRange(16.0)
            .alarmCallRange(0.0)
            .alarmCooldown(0)
            .crossSpeciesWarning(false)
            .recoveryTime(300)
            .habituationRate(0.05)
            .build();
    }

    /**
     * Creates config for deer - moderate FID, straight escape to cover.
     * Deer use tail-flagging visual signals and straight-line escape to refuge.
     */
    public static FleeingConfig createDeer() {
        return new Builder()
            .flightInitiationDistance(18.0)
            .primaryStrategy(EscapeStrategy.REFUGE)
            .secondaryStrategy(EscapeStrategy.STRAIGHT)
            .zigzagIntensity(0.2)
            .zigzagChangeInterval(30)
            .refugeDetectionRange(32.0)
            .freezingDuration(60)
            .ambushPredatorMultiplier(0.9)
            .cursorialPredatorMultiplier(1.0)
            .aerialPredatorMultiplier(1.3)
            .humanThreatMultiplier(1.2)
            .panicThreshold(3)
            .stampedeSpeedMultiplier(1.8)
            .panicPropagationRange(24.0)
            .alarmCallRange(32.0)
            .alarmCooldown(200)
            .crossSpeciesWarning(true)
            .recoveryTime(600)
            .habituationRate(0.03)
            .build();
    }

    /**
     * Creates config for sheep - moderate FID, flock cohesion, group flee.
     * Sheep exhibit strong flocking behavior during escape with selfish positioning.
     */
    public static FleeingConfig createSheep() {
        return new Builder()
            .flightInitiationDistance(14.0)
            .primaryStrategy(EscapeStrategy.STRAIGHT)
            .secondaryStrategy(EscapeStrategy.REFUGE)
            .zigzagIntensity(0.1)
            .zigzagChangeInterval(40)
            .refugeDetectionRange(24.0)
            .freezingDuration(30)
            .ambushPredatorMultiplier(0.9)
            .cursorialPredatorMultiplier(1.1)
            .aerialPredatorMultiplier(1.4)
            .humanThreatMultiplier(1.0)
            .panicThreshold(2)
            .stampedeSpeedMultiplier(1.5)
            .panicPropagationRange(20.0)
            .alarmCallRange(24.0)
            .alarmCooldown(150)
            .crossSpeciesWarning(true)
            .recoveryTime(400)
            .habituationRate(0.08)
            .build();
    }

    /**
     * Creates config for cattle - lower FID, herd stampede, slow recovery.
     * Cattle prioritize herd cohesion and will stampede when threshold is met.
     */
    public static FleeingConfig createCattle() {
        return new Builder()
            .flightInitiationDistance(10.0)
            .primaryStrategy(EscapeStrategy.STRAIGHT)
            .secondaryStrategy(EscapeStrategy.REFUGE)
            .zigzagIntensity(0.0)
            .zigzagChangeInterval(0)
            .refugeDetectionRange(32.0)
            .freezingDuration(20)
            .ambushPredatorMultiplier(1.0)
            .cursorialPredatorMultiplier(1.2)
            .aerialPredatorMultiplier(1.5)
            .humanThreatMultiplier(0.9)
            .panicThreshold(3)
            .stampedeSpeedMultiplier(2.0)
            .panicPropagationRange(32.0)
            .alarmCallRange(28.0)
            .alarmCooldown(180)
            .crossSpeciesWarning(true)
            .recoveryTime(800)
            .habituationRate(0.1)
            .build();
    }

    /**
     * Creates config for pigs - moderate FID, seek refuge aggressively.
     * Pigs will actively seek shelter and are more territorial in defense.
     */
    public static FleeingConfig createPig() {
        return new Builder()
            .flightInitiationDistance(12.0)
            .primaryStrategy(EscapeStrategy.REFUGE)
            .secondaryStrategy(EscapeStrategy.STRAIGHT)
            .zigzagIntensity(0.1)
            .zigzagChangeInterval(35)
            .refugeDetectionRange(28.0)
            .freezingDuration(25)
            .ambushPredatorMultiplier(1.1)
            .cursorialPredatorMultiplier(1.1)
            .aerialPredatorMultiplier(1.3)
            .humanThreatMultiplier(1.0)
            .panicThreshold(2)
            .stampedeSpeedMultiplier(1.4)
            .panicPropagationRange(18.0)
            .alarmCallRange(20.0)
            .alarmCooldown(120)
            .crossSpeciesWarning(true)
            .recoveryTime(350)
            .habituationRate(0.07)
            .build();
    }

    /**
     * Creates config for chickens - low FID, erratic escape, short freezing.
     * Chickens may flap upward or scatter in different directions.
     */
    public static FleeingConfig createChicken() {
        return new Builder()
            .flightInitiationDistance(8.0)
            .primaryStrategy(EscapeStrategy.ZIGZAG)
            .secondaryStrategy(EscapeStrategy.FREEZE)
            .zigzagIntensity(0.7)
            .zigzagChangeInterval(10)
            .refugeDetectionRange(16.0)
            .freezingDuration(20)
            .ambushPredatorMultiplier(1.2)
            .cursorialPredatorMultiplier(1.3)
            .aerialPredatorMultiplier(0.8)
            .humanThreatMultiplier(1.1)
            .panicThreshold(1)
            .stampedeSpeedMultiplier(1.3)
            .panicPropagationRange(12.0)
            .alarmCallRange(16.0)
            .alarmCooldown(100)
            .crossSpeciesWarning(true)
            .recoveryTime(200)
            .habituationRate(0.12)
            .build();
    }

    /**
     * Creates config for generic prey animals with balanced parameters.
     */
    public static FleeingConfig createDefault() {
        return new Builder()
            .flightInitiationDistance(12.0)
            .primaryStrategy(EscapeStrategy.STRAIGHT)
            .secondaryStrategy(EscapeStrategy.REFUGE)
            .zigzagIntensity(0.3)
            .zigzagChangeInterval(20)
            .refugeDetectionRange(24.0)
            .freezingDuration(30)
            .ambushPredatorMultiplier(1.0)
            .cursorialPredatorMultiplier(1.0)
            .aerialPredatorMultiplier(1.2)
            .humanThreatMultiplier(1.0)
            .panicThreshold(2)
            .stampedeSpeedMultiplier(1.5)
            .panicPropagationRange(20.0)
            .alarmCallRange(24.0)
            .alarmCooldown(150)
            .crossSpeciesWarning(true)
            .recoveryTime(400)
            .habituationRate(0.05)
            .build();
    }

    /**
     * Gets species-specific config from entity type string.
     */
    public static FleeingConfig forSpecies(String speciesId) {
        String lowerId = speciesId.toLowerCase();

        if (lowerId.contains("rabbit")) {
            return createRabbit();
        } else if (lowerId.contains("deer") || lowerId.contains("fox") || lowerId.contains("antelope")) {
            return createDeer();
        } else if (lowerId.contains("sheep")) {
            return createSheep();
        } else if (lowerId.contains("cow") || lowerId.contains("cattle") || lowerId.contains("mooshroom")) {
            return createCattle();
        } else if (lowerId.contains("pig") || lowerId.contains("hog")) {
            return createPig();
        } else if (lowerId.contains("chicken") || lowerId.contains("parrot")) {
            return createChicken();
        }

        return createDefault();
    }

    // Getters

    public double getFlightInitiationDistance() {
        return flightInitiationDistance;
    }

    public double getAmbushPredatorMultiplier() {
        return ambushPredatorMultiplier;
    }

    public double getCursorialPredatorMultiplier() {
        return cursorialPredatorMultiplier;
    }

    public double getAerialPredatorMultiplier() {
        return aerialPredatorMultiplier;
    }

    public double getHumanThreatMultiplier() {
        return humanThreatMultiplier;
    }

    public EscapeStrategy getPrimaryStrategy() {
        return primaryStrategy;
    }

    public EscapeStrategy getSecondaryStrategy() {
        return secondaryStrategy;
    }

    public double getZigzagIntensity() {
        return zigzagIntensity;
    }

    public int getZigzagChangeInterval() {
        return zigzagChangeInterval;
    }

    public double getRefugeDetectionRange() {
        return refugeDetectionRange;
    }

    public int getFreezingDuration() {
        return freezingDuration;
    }

    public int getPanicThreshold() {
        return panicThreshold;
    }

    public double getStampedeSpeedMultiplier() {
        return stampedeSpeedMultiplier;
    }

    public double getPanicPropagationRange() {
        return panicPropagationRange;
    }

    public double getAlarmCallRange() {
        return alarmCallRange;
    }

    public int getAlarmCooldown() {
        return alarmCooldown;
    }

    public boolean isCrossSpeciesWarning() {
        return crossSpeciesWarning;
    }

    public int getRecoveryTime() {
        return recoveryTime;
    }

    public double getHabituationRate() {
        return habituationRate;
    }

    /**
     * Builder pattern for creating custom configurations.
     */
    public static class Builder {
        private double flightInitiationDistance = 12.0;
        private double ambushPredatorMultiplier = 1.0;
        private double cursorialPredatorMultiplier = 1.0;
        private double aerialPredatorMultiplier = 1.2;
        private double humanThreatMultiplier = 1.0;
        private EscapeStrategy primaryStrategy = EscapeStrategy.STRAIGHT;
        private EscapeStrategy secondaryStrategy = EscapeStrategy.REFUGE;
        private double zigzagIntensity = 0.3;
        private int zigzagChangeInterval = 20;
        private double refugeDetectionRange = 24.0;
        private int freezingDuration = 30;
        private int panicThreshold = 2;
        private double stampedeSpeedMultiplier = 1.5;
        private double panicPropagationRange = 20.0;
        private double alarmCallRange = 24.0;
        private int alarmCooldown = 150;
        private boolean crossSpeciesWarning = true;
        private int recoveryTime = 400;
        private double habituationRate = 0.05;

        public Builder flightInitiationDistance(double distance) {
            this.flightInitiationDistance = distance;
            return this;
        }

        public Builder ambushPredatorMultiplier(double multiplier) {
            this.ambushPredatorMultiplier = multiplier;
            return this;
        }

        public Builder cursorialPredatorMultiplier(double multiplier) {
            this.cursorialPredatorMultiplier = multiplier;
            return this;
        }

        public Builder aerialPredatorMultiplier(double multiplier) {
            this.aerialPredatorMultiplier = multiplier;
            return this;
        }

        public Builder humanThreatMultiplier(double multiplier) {
            this.humanThreatMultiplier = multiplier;
            return this;
        }

        public Builder primaryStrategy(EscapeStrategy strategy) {
            this.primaryStrategy = strategy;
            return this;
        }

        public Builder secondaryStrategy(EscapeStrategy strategy) {
            this.secondaryStrategy = strategy;
            return this;
        }

        public Builder zigzagIntensity(double intensity) {
            this.zigzagIntensity = intensity;
            return this;
        }

        public Builder zigzagChangeInterval(int interval) {
            this.zigzagChangeInterval = interval;
            return this;
        }

        public Builder refugeDetectionRange(double range) {
            this.refugeDetectionRange = range;
            return this;
        }

        public Builder freezingDuration(int duration) {
            this.freezingDuration = duration;
            return this;
        }

        public Builder panicThreshold(int threshold) {
            this.panicThreshold = threshold;
            return this;
        }

        public Builder stampedeSpeedMultiplier(double multiplier) {
            this.stampedeSpeedMultiplier = multiplier;
            return this;
        }

        public Builder panicPropagationRange(double range) {
            this.panicPropagationRange = range;
            return this;
        }

        public Builder alarmCallRange(double range) {
            this.alarmCallRange = range;
            return this;
        }

        public Builder alarmCooldown(int cooldown) {
            this.alarmCooldown = cooldown;
            return this;
        }

        public Builder crossSpeciesWarning(boolean enabled) {
            this.crossSpeciesWarning = enabled;
            return this;
        }

        public Builder recoveryTime(int time) {
            this.recoveryTime = time;
            return this;
        }

        public Builder habituationRate(double rate) {
            this.habituationRate = rate;
            return this;
        }

        public FleeingConfig build() {
            return new FleeingConfig(this);
        }
    }
}
