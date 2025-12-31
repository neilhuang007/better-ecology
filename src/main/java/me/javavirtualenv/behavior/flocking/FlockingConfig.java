package me.javavirtualenv.behavior.flocking;

/**
 * Configuration class for flocking behavior parameters.
 * Provides research-based defaults from ballerini et al. (2008) on starling murmurations.
 */
public class FlockingConfig {

    // Behavior weights
    private double separationWeight = 2.5;
    private double alignmentWeight = 1.5;
    private double cohesionWeight = 1.3;

    // Topological neighbor tracking (research-based: 6-7 neighbors)
    private int topologicalNeighborCount = 7;

    // Distance thresholds (in blocks)
    private double separationDistance = 2.0;
    private double perceptionRadius = 16.0;

    // Movement constraints
    private double maxSpeed = 0.8;
    private double maxForce = 0.15;

    // Visual field constraints (270 degrees per research)
    private double perceptionAngle = Math.toRadians(270);

    // Noise for natural movement
    private double noiseWeight = 0.3;

    /**
     * Creates a config with default research-based parameters suitable for birds.
     */
    public FlockingConfig() {
    }

    /**
     * Creates a config optimized for specific species types.
     */
    public static FlockingConfig forMurmurations() {
        FlockingConfig config = new FlockingConfig();
        config.topologicalNeighborCount = 7;
        config.separationWeight = 2.0;
        config.alignmentWeight = 1.5;
        config.cohesionWeight = 1.2;
        config.perceptionRadius = 20.0;
        config.maxSpeed = 1.2;
        config.maxForce = 0.2;
        config.noiseWeight = 0.2;
        return config;
    }

    /**
     * Creates a config optimized for V-formation flight (geese, migratory birds).
     */
    public static FlockingConfig forVFormation() {
        FlockingConfig config = new FlockingConfig();
        config.topologicalNeighborCount = 6;
        config.separationWeight = 2.0;
        config.alignmentWeight = 2.0;
        config.cohesionWeight = 1.0;
        config.perceptionRadius = 12.0;
        config.maxSpeed = 1.0;
        config.maxForce = 0.1;
        config.noiseWeight = 0.1;
        return config;
    }

    /**
     * Creates a config for small, tight flocks (sparrows, pigeons).
     */
    public static FlockingConfig forSmallFlocks() {
        FlockingConfig config = new FlockingConfig();
        config.topologicalNeighborCount = 5;
        config.separationWeight = 3.0;
        config.alignmentWeight = 1.5;
        config.cohesionWeight = 1.5;
        config.separationDistance = 1.5;
        config.perceptionRadius = 8.0;
        config.maxSpeed = 0.6;
        config.maxForce = 0.12;
        return config;
    }

    public double getSeparationWeight() {
        return separationWeight;
    }

    public void setSeparationWeight(double separationWeight) {
        this.separationWeight = separationWeight;
    }

    public double getAlignmentWeight() {
        return alignmentWeight;
    }

    public void setAlignmentWeight(double alignmentWeight) {
        this.alignmentWeight = alignmentWeight;
    }

    public double getCohesionWeight() {
        return cohesionWeight;
    }

    public void setCohesionWeight(double cohesionWeight) {
        this.cohesionWeight = cohesionWeight;
    }

    public int getTopologicalNeighborCount() {
        return topologicalNeighborCount;
    }

    public void setTopologicalNeighborCount(int topologicalNeighborCount) {
        this.topologicalNeighborCount = Math.max(1, topologicalNeighborCount);
    }

    public double getSeparationDistance() {
        return separationDistance;
    }

    public void setSeparationDistance(double separationDistance) {
        this.separationDistance = Math.max(0.1, separationDistance);
    }

    public double getPerceptionRadius() {
        return perceptionRadius;
    }

    public void setPerceptionRadius(double perceptionRadius) {
        this.perceptionRadius = Math.max(1.0, perceptionRadius);
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = Math.max(0.01, maxSpeed);
    }

    public double getMaxForce() {
        return maxForce;
    }

    public void setMaxForce(double maxForce) {
        this.maxForce = Math.max(0.01, maxForce);
    }

    public double getPerceptionAngle() {
        return perceptionAngle;
    }

    public void setPerceptionAngle(double perceptionAngle) {
        this.perceptionAngle = Math.max(0.0, Math.min(Math.PI * 2, perceptionAngle));
    }

    public double getNoiseWeight() {
        return noiseWeight;
    }

    public void setNoiseWeight(double noiseWeight) {
        this.noiseWeight = Math.max(0.0, noiseWeight);
    }

    /**
     * Validates that all parameters are within acceptable ranges.
     */
    public void validate() {
        if (separationWeight < 0 || alignmentWeight < 0 || cohesionWeight < 0) {
            throw new IllegalStateException("Behavior weights must be non-negative");
        }
        if (topologicalNeighborCount < 1) {
            throw new IllegalStateException("Neighbor count must be at least 1");
        }
        if (maxSpeed <= 0 || maxForce <= 0) {
            throw new IllegalStateException("Max speed and force must be positive");
        }
    }

    @Override
    public String toString() {
        return String.format(
            "FlockingConfig{separation=%.2f, alignment=%.2f, cohesion=%.2f, neighbors=%d, radius=%.2f}",
            separationWeight, alignmentWeight, cohesionWeight, topologicalNeighborCount, perceptionRadius
        );
    }
}
