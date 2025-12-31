package me.javavirtualenv.behavior.aquatic;

/**
 * Configuration for aquatic behaviors.
 * Provides scientifically-based parameters for underwater movement and behaviors.
 */
public class AquaticConfig {
    private double perceptionRadius = 12.0;
    private double separationDistance = 1.5;
    private double maxSpeed = 0.25;
    private double maxForce = 0.12;
    private double schoolCohesionWeight = 1.2;
    private double schoolAlignmentWeight = 1.0;
    private double schoolSeparationWeight = 1.8;

    // Pufferfish specific
    private double inflateThreshold = 4.0;
    private int inflateDuration = 200;
    private int deflateCooldown = 600;

    // Squid specific
    private double inkCloudRadius = 6.0;
    private int inkCloudDuration = 300;
    private double inkReleaseThreshold = 8.0;
    private double verticalMigrationDepth = 20.0;
    private int verticalMigrationInterval = 12000;

    // Salmon specific
    private double upstreamSpeed = 0.35;
    private double currentResistance = 0.7;

    // Tadpole specific
    private int metamorphosisTime = 24000; // 20 minutes to become frog
    private double surfaceSeekingStrength = 0.8;

    public static AquaticConfig createDefault() {
        return new AquaticConfig();
    }

    public static AquaticConfig createForFish() {
        AquaticConfig config = new AquaticConfig();
        config.perceptionRadius = 10.0;
        config.separationDistance = 1.2;
        config.maxSpeed = 0.3;
        config.maxForce = 0.15;
        config.schoolCohesionWeight = 1.5;
        config.schoolAlignmentWeight = 1.2;
        config.schoolSeparationWeight = 2.0;
        return config;
    }

    public static AquaticConfig createForSquid() {
        AquaticConfig config = new AquaticConfig();
        config.perceptionRadius = 16.0;
        config.maxSpeed = 0.25;
        config.maxForce = 0.12;
        config.inkCloudRadius = 8.0;
        config.inkCloudDuration = 400;
        config.inkReleaseThreshold = 6.0;
        config.verticalMigrationDepth = 25.0;
        return config;
    }

    public static AquaticConfig createForPufferfish() {
        AquaticConfig config = new AquaticConfig();
        config.perceptionRadius = 8.0;
        config.maxSpeed = 0.15;
        config.maxForce = 0.08;
        config.inflateThreshold = 5.0;
        config.inflateDuration = 300;
        config.deflateCooldown = 800;
        return config;
    }

    public static AquaticConfig createForTadpole() {
        AquaticConfig config = new AquaticConfig();
        config.perceptionRadius = 6.0;
        config.maxSpeed = 0.12;
        config.maxForce = 0.1;
        config.metamorphosisTime = 24000;
        config.surfaceSeekingStrength = 0.9;
        return config;
    }

    public static AquaticConfig createForSalmon() {
        AquaticConfig config = new AquaticConfig();
        config.perceptionRadius = 14.0;
        config.maxSpeed = 0.35;
        config.maxForce = 0.18;
        config.upstreamSpeed = 0.4;
        config.currentResistance = 0.8;
        return config;
    }

    // Getters
    public double getPerceptionRadius() { return perceptionRadius; }
    public double getSeparationDistance() { return separationDistance; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getMaxForce() { return maxForce; }
    public double getSchoolCohesionWeight() { return schoolCohesionWeight; }
    public double getSchoolAlignmentWeight() { return schoolAlignmentWeight; }
    public double getSchoolSeparationWeight() { return schoolSeparationWeight; }
    public double getInflateThreshold() { return inflateThreshold; }
    public int getInflateDuration() { return inflateDuration; }
    public int getDeflateCooldown() { return deflateCooldown; }
    public double getInkCloudRadius() { return inkCloudRadius; }
    public int getInkCloudDuration() { return inkCloudDuration; }
    public double getInkReleaseThreshold() { return inkReleaseThreshold; }
    public double getVerticalMigrationDepth() { return verticalMigrationDepth; }
    public int getVerticalMigrationInterval() { return verticalMigrationInterval; }
    public double getUpstreamSpeed() { return upstreamSpeed; }
    public double getCurrentResistance() { return currentResistance; }
    public int getMetamorphosisTime() { return metamorphosisTime; }
    public double getSurfaceSeekingStrength() { return surfaceSeekingStrength; }

    // Setters for configuration
    public void setPerceptionRadius(double value) { this.perceptionRadius = value; }
    public void setSeparationDistance(double value) { this.separationDistance = value; }
    public void setMaxSpeed(double value) { this.maxSpeed = value; }
    public void setMaxForce(double value) { this.maxForce = value; }
    public void setSchoolCohesionWeight(double value) { this.schoolCohesionWeight = value; }
    public void setSchoolAlignmentWeight(double value) { this.schoolAlignmentWeight = value; }
    public void setSchoolSeparationWeight(double value) { this.schoolSeparationWeight = value; }
    public void setInflateThreshold(double value) { this.inflateThreshold = value; }
    public void setInflateDuration(int value) { this.inflateDuration = value; }
    public void setDeflateCooldown(int value) { this.deflateCooldown = value; }
    public void setInkCloudRadius(double value) { this.inkCloudRadius = value; }
    public void setInkCloudDuration(int value) { this.inkCloudDuration = value; }
    public void setInkReleaseThreshold(double value) { this.inkReleaseThreshold = value; }
    public void setVerticalMigrationDepth(double value) { this.verticalMigrationDepth = value; }
    public void setVerticalMigrationInterval(int value) { this.verticalMigrationInterval = value; }
    public void setUpstreamSpeed(double value) { this.upstreamSpeed = value; }
    public void setCurrentResistance(double value) { this.currentResistance = value; }
    public void setMetamorphosisTime(int value) { this.metamorphosisTime = value; }
    public void setSurfaceSeekingStrength(double value) { this.surfaceSeekingStrength = value; }
}
