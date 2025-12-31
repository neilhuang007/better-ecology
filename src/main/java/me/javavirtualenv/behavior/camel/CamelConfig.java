package me.javavirtualenv.behavior.camel;

import java.util.UUID;

/**
 * Configuration class for camel-specific behaviors.
 * <p>
 * This class holds all configurable parameters for camel behaviors including
 * spitting defense, desert endurance, caravan behavior, and sand movement.
 */
public class CamelConfig {

    // Spitting Defense Configuration
    private final double spittingRange;
    private final int spittingDamage;
    private final int spittingSlowDuration;
    private final int spittingCooldown;
    private final int spittingWindupTicks;
    private final double spittingAccuracy;

    // Desert Endurance Configuration
    private final double desertHungerMultiplier;
    private final double desertThirstMultiplier;
    private final int waterStorageMax;
    private final int waterStorageDecayRate;
    private final double heatResistance;
    private final boolean heatDamageImmunity;

    // Caravan Configuration
    private final double caravanFollowRange;
    private final double caravanCohesionDistance;
    private final double caravanExhaustionMultiplier;
    private final int maxCaravanSize;
    private final double caravanSpeedBonus;

    // Sand Movement Configuration
    private final double sandSpeedBonus;
    private final boolean noSandSink;
    private final double soulSandSpeedBonus;
    private final boolean noSoulSandSink;

    private CamelConfig(Builder builder) {
        // Spitting Defense
        this.spittingRange = builder.spittingRange;
        this.spittingDamage = builder.spittingDamage;
        this.spittingSlowDuration = builder.spittingSlowDuration;
        this.spittingCooldown = builder.spittingCooldown;
        this.spittingWindupTicks = builder.spittingWindupTicks;
        this.spittingAccuracy = builder.spittingAccuracy;

        // Desert Endurance
        this.desertHungerMultiplier = builder.desertHungerMultiplier;
        this.desertThirstMultiplier = builder.desertThirstMultiplier;
        this.waterStorageMax = builder.waterStorageMax;
        this.waterStorageDecayRate = builder.waterStorageDecayRate;
        this.heatResistance = builder.heatResistance;
        this.heatDamageImmunity = builder.heatDamageImmunity;

        // Caravan
        this.caravanFollowRange = builder.caravanFollowRange;
        this.caravanCohesionDistance = builder.caravanCohesionDistance;
        this.caravanExhaustionMultiplier = builder.caravanExhaustionMultiplier;
        this.maxCaravanSize = builder.maxCaravanSize;
        this.caravanSpeedBonus = builder.caravanSpeedBonus;

        // Sand Movement
        this.sandSpeedBonus = builder.sandSpeedBonus;
        this.noSandSink = builder.noSandSink;
        this.soulSandSpeedBonus = builder.soulSandSpeedBonus;
        this.noSoulSandSink = builder.noSoulSandSink;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CamelConfig createDefault() {
        return builder().build();
    }

    // Getters
    public double getSpittingRange() { return spittingRange; }
    public int getSpittingDamage() { return spittingDamage; }
    public int getSpittingSlowDuration() { return spittingSlowDuration; }
    public int getSpittingCooldown() { return spittingCooldown; }
    public int getSpittingWindupTicks() { return spittingWindupTicks; }
    public double getSpittingAccuracy() { return spittingAccuracy; }

    public double getDesertHungerMultiplier() { return desertHungerMultiplier; }
    public double getDesertThirstMultiplier() { return desertThirstMultiplier; }
    public int getWaterStorageMax() { return waterStorageMax; }
    public int getWaterStorageDecayRate() { return waterStorageDecayRate; }
    public double getHeatResistance() { return heatResistance; }
    public boolean isHeatDamageImmunity() { return heatDamageImmunity; }

    public double getCaravanFollowRange() { return caravanFollowRange; }
    public double getCaravanCohesionDistance() { return caravanCohesionDistance; }
    public double getCaravanExhaustionMultiplier() { return caravanExhaustionMultiplier; }
    public int getMaxCaravanSize() { return maxCaravanSize; }
    public double getCaravanSpeedBonus() { return caravanSpeedBonus; }

    public double getSandSpeedBonus() { return sandSpeedBonus; }
    public boolean isNoSandSink() { return noSandSink; }
    public double getSoulSandSpeedBonus() { return soulSandSpeedBonus; }
    public boolean isNoSoulSandSink() { return noSoulSandSink; }

    public static class Builder {
        private double spittingRange = 10.0;
        private int spittingDamage = 1;
        private int spittingSlowDuration = 100;
        private int spittingCooldown = 200;
        private int spittingWindupTicks = 20;
        private double spittingAccuracy = 0.85;

        private double desertHungerMultiplier = 0.4;
        private double desertThirstMultiplier = 0.2;
        private int waterStorageMax = 200;
        private int waterStorageDecayRate = 1;
        private double heatResistance = 0.8;
        private boolean heatDamageImmunity = true;

        private double caravanFollowRange = 16.0;
        private double caravanCohesionDistance = 6.0;
        private double caravanExhaustionMultiplier = 0.5;
        private int maxCaravanSize = 6;
        private double caravanSpeedBonus = 0.15;

        private double sandSpeedBonus = 0.3;
        private boolean noSandSink = true;
        private double soulSandSpeedBonus = 0.4;
        private boolean noSoulSandSink = true;

        public Builder spittingRange(double val) { spittingRange = val; return this; }
        public Builder spittingDamage(int val) { spittingDamage = val; return this; }
        public Builder spittingSlowDuration(int val) { spittingSlowDuration = val; return this; }
        public Builder spittingCooldown(int val) { spittingCooldown = val; return this; }
        public Builder spittingWindupTicks(int val) { spittingWindupTicks = val; return this; }
        public Builder spittingAccuracy(double val) { spittingAccuracy = val; return this; }

        public Builder desertHungerMultiplier(double val) { desertHungerMultiplier = val; return this; }
        public Builder desertThirstMultiplier(double val) { desertThirstMultiplier = val; return this; }
        public Builder waterStorageMax(int val) { waterStorageMax = val; return this; }
        public Builder waterStorageDecayRate(int val) { waterStorageDecayRate = val; return this; }
        public Builder heatResistance(double val) { heatResistance = val; return this; }
        public Builder heatDamageImmunity(boolean val) { heatDamageImmunity = val; return this; }

        public Builder caravanFollowRange(double val) { caravanFollowRange = val; return this; }
        public Builder caravanCohesionDistance(double val) { caravanCohesionDistance = val; return this; }
        public Builder caravanExhaustionMultiplier(double val) { caravanExhaustionMultiplier = val; return this; }
        public Builder maxCaravanSize(int val) { maxCaravanSize = val; return this; }
        public Builder caravanSpeedBonus(double val) { caravanSpeedBonus = val; return this; }

        public Builder sandSpeedBonus(double val) { sandSpeedBonus = val; return this; }
        public Builder noSandSink(boolean val) { noSandSink = val; return this; }
        public Builder soulSandSpeedBonus(double val) { soulSandSpeedBonus = val; return this; }
        public Builder noSoulSandSink(boolean val) { noSoulSandSink = val; return this; }

        public CamelConfig build() {
            return new CamelConfig(this);
        }
    }
}
