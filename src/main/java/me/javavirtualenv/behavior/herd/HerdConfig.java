package me.javavirtualenv.behavior.herd;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for herd behavior parameters.
 * Contains research-based defaults from studies of wild ungulates and social mammals.
 */
public class HerdConfig {

    // Quorum-based movement defaults (from bison research: 47% quorum)
    private double quorumThreshold = 0.47;
    private double quorumCheckInterval = 1.0; // seconds between checks

    // Leadership parameters
    private double leadershipAgeBonus = 0.7;
    private double leadershipDominanceBonus = 0.5;
    private int maxLeaders = 3; // Shared leadership, not single leader
    private double leaderFollowRadius = 16.0; // blocks

    // Herd cohesion parameters
    private double cohesionRadius = 12.0; // blocks
    private double separationDistance = 2.5; // blocks - minimum spacing
    private double cohesionStrength = 1.0;
    private double selfishHerdStrength = 0.6; // Avoid edge positioning

    // Selfish herd positioning
    private boolean selfishHerdEnabled = true;
    private double edgeDetectionRadius = 8.0; // blocks to detect herd edge
    private double centerBias = 0.3; // Strength of pull toward center

    // Movement parameters
    private double maxSpeed = 0.4; // blocks per tick
    private double maxForce = 0.1; // steering force limit

    // Species-specific configurations
    private static final Map<String, HerdConfig> SPECIES_CONFIGS = new HashMap<>();

    static {
        // Bison - strong quorum, shared leadership
        HerdConfig bisonConfig = new HerdConfig();
        bisonConfig.quorumThreshold = 0.47;
        bisonConfig.leadershipAgeBonus = 0.8;
        bisonConfig.leadershipDominanceBonus = 0.6;
        bisonConfig.maxLeaders = 4;
        bisonConfig.cohesionRadius = 15.0;
        bisonConfig.selfishHerdEnabled = true;
        bisonConfig.selfishHerdStrength = 0.7;
        SPECIES_CONFIGS.put("entity.minecraft.bison", bisonConfig);

        // Sheep - strong cohesion, less quorum
        HerdConfig sheepConfig = new HerdConfig();
        sheepConfig.quorumThreshold = 0.35;
        sheepConfig.leadershipAgeBonus = 0.5;
        sheepConfig.leadershipDominanceBonus = 0.3;
        sheepConfig.maxLeaders = 2;
        sheepConfig.cohesionRadius = 10.0;
        sheepConfig.selfishHerdEnabled = true;
        sheepConfig.selfishHerdStrength = 0.5;
        SPECIES_CONFIGS.put("entity.minecraft.sheep", sheepConfig);

        // Cow - moderate everything
        HerdConfig cowConfig = new HerdConfig();
        cowConfig.quorumThreshold = 0.40;
        cowConfig.leadershipAgeBonus = 0.4;
        cowConfig.leadershipDominanceBonus = 0.2;
        cowConfig.maxLeaders = 2;
        cowConfig.cohesionRadius = 12.0;
        cowConfig.selfishHerdEnabled = true;
        cowConfig.selfishHerdStrength = 0.4;
        SPECIES_CONFIGS.put("entity.minecraft.cow", cowConfig);
        SPECIES_CONFIGS.put("entity.minecraft.mooshroom", cowConfig);

        // Pig - weaker herd instincts
        HerdConfig pigConfig = new HerdConfig();
        pigConfig.quorumThreshold = 0.30;
        pigConfig.leadershipAgeBonus = 0.3;
        pigConfig.leadershipDominanceBonus = 0.3;
        pigConfig.maxLeaders = 1;
        pigConfig.cohesionRadius = 8.0;
        pigConfig.selfishHerdEnabled = false;
        SPECIES_CONFIGS.put("entity.minecraft.pig", pigConfig);

        // Chicken - very weak herding
        HerdConfig chickenConfig = new HerdConfig();
        chickenConfig.quorumThreshold = 0.25;
        chickenConfig.leadershipAgeBonus = 0.2;
        chickenConfig.leadershipDominanceBonus = 0.1;
        chickenConfig.maxLeaders = 1;
        chickenConfig.cohesionRadius = 6.0;
        chickenConfig.selfishHerdEnabled = false;
        SPECIES_CONFIGS.put("entity.minecraft.chicken", chickenConfig);

        // Goat - independent, weak quorum
        HerdConfig goatConfig = new HerdConfig();
        goatConfig.quorumThreshold = 0.30;
        goatConfig.leadershipAgeBonus = 0.3;
        goatConfig.leadershipDominanceBonus = 0.2;
        goatConfig.maxLeaders = 2;
        goatConfig.cohesionRadius = 10.0;
        goatConfig.selfishHerdEnabled = true;
        goatConfig.selfishHerdStrength = 0.3;
        SPECIES_CONFIGS.put("entity.minecraft.goat", goatConfig);

        // Llama - strong leadership (used to packing behavior)
        HerdConfig llamaConfig = new HerdConfig();
        llamaConfig.quorumThreshold = 0.40;
        llamaConfig.leadershipAgeBonus = 0.7;
        llamaConfig.leadershipDominanceBonus = 0.5;
        llamaConfig.maxLeaders = 2;
        llamaConfig.cohesionRadius = 14.0;
        llamaConfig.selfishHerdEnabled = true;
        llamaConfig.selfishHerdStrength = 0.5;
        SPECIES_CONFIGS.put("entity.minecraft.llama", llamaConfig);

        // Wolf - strong pack hierarchy
        HerdConfig wolfConfig = new HerdConfig();
        wolfConfig.quorumThreshold = 0.50;
        wolfConfig.leadershipAgeBonus = 0.6;
        wolfConfig.leadershipDominanceBonus = 0.9;
        wolfConfig.maxLeaders = 2;
        wolfConfig.cohesionRadius = 20.0;
        wolfConfig.selfishHerdEnabled = true;
        wolfConfig.selfishHerdStrength = 0.3;
        SPECIES_CONFIGS.put("entity.minecraft.wolf", wolfConfig);
    }

    public HerdConfig() {
    }

    /**
     * Get species-specific configuration, or default if none exists.
     */
    public static HerdConfig forSpecies(String speciesId) {
        return SPECIES_CONFIGS.getOrDefault(speciesId, new HerdConfig());
    }

    public double getQuorumThreshold() {
        return quorumThreshold;
    }

    public void setQuorumThreshold(double quorumThreshold) {
        this.quorumThreshold = Math.max(0.0, Math.min(1.0, quorumThreshold));
    }

    public double getQuorumCheckInterval() {
        return quorumCheckInterval;
    }

    public void setQuorumCheckInterval(double quorumCheckInterval) {
        this.quorumCheckInterval = Math.max(0.1, quorumCheckInterval);
    }

    public double getLeadershipAgeBonus() {
        return leadershipAgeBonus;
    }

    public void setLeadershipAgeBonus(double leadershipAgeBonus) {
        this.leadershipAgeBonus = Math.max(0.0, Math.min(1.0, leadershipAgeBonus));
    }

    public double getLeadershipDominanceBonus() {
        return leadershipDominanceBonus;
    }

    public void setLeadershipDominanceBonus(double leadershipDominanceBonus) {
        this.leadershipDominanceBonus = Math.max(0.0, Math.min(1.0, leadershipDominanceBonus));
    }

    public int getMaxLeaders() {
        return maxLeaders;
    }

    public void setMaxLeaders(int maxLeaders) {
        this.maxLeaders = Math.max(1, maxLeaders);
    }

    public double getLeaderFollowRadius() {
        return leaderFollowRadius;
    }

    public void setLeaderFollowRadius(double leaderFollowRadius) {
        this.leaderFollowRadius = Math.max(1.0, leaderFollowRadius);
    }

    public double getCohesionRadius() {
        return cohesionRadius;
    }

    public void setCohesionRadius(double cohesionRadius) {
        this.cohesionRadius = Math.max(1.0, cohesionRadius);
    }

    public double getSeparationDistance() {
        return separationDistance;
    }

    public void setSeparationDistance(double separationDistance) {
        this.separationDistance = Math.max(0.5, separationDistance);
    }

    public double getCohesionStrength() {
        return cohesionStrength;
    }

    public void setCohesionStrength(double cohesionStrength) {
        this.cohesionStrength = Math.max(0.0, cohesionStrength);
    }

    public double getSelfishHerdStrength() {
        return selfishHerdStrength;
    }

    public void setSelfishHerdStrength(double selfishHerdStrength) {
        this.selfishHerdStrength = Math.max(0.0, Math.min(1.0, selfishHerdStrength));
    }

    public boolean isSelfishHerdEnabled() {
        return selfishHerdEnabled;
    }

    public void setSelfishHerdEnabled(boolean selfishHerdEnabled) {
        this.selfishHerdEnabled = selfishHerdEnabled;
    }

    public double getEdgeDetectionRadius() {
        return edgeDetectionRadius;
    }

    public void setEdgeDetectionRadius(double edgeDetectionRadius) {
        this.edgeDetectionRadius = Math.max(1.0, edgeDetectionRadius);
    }

    public double getCenterBias() {
        return centerBias;
    }

    public void setCenterBias(double centerBias) {
        this.centerBias = Math.max(0.0, Math.min(1.0, centerBias));
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

    /**
     * Creates a copy of this configuration.
     */
    public HerdConfig copy() {
        HerdConfig copy = new HerdConfig();
        copy.quorumThreshold = this.quorumThreshold;
        copy.quorumCheckInterval = this.quorumCheckInterval;
        copy.leadershipAgeBonus = this.leadershipAgeBonus;
        copy.leadershipDominanceBonus = this.leadershipDominanceBonus;
        copy.maxLeaders = this.maxLeaders;
        copy.leaderFollowRadius = this.leaderFollowRadius;
        copy.cohesionRadius = this.cohesionRadius;
        copy.separationDistance = this.separationDistance;
        copy.cohesionStrength = this.cohesionStrength;
        copy.selfishHerdStrength = this.selfishHerdStrength;
        copy.selfishHerdEnabled = this.selfishHerdEnabled;
        copy.edgeDetectionRadius = this.edgeDetectionRadius;
        copy.centerBias = this.centerBias;
        copy.maxSpeed = this.maxSpeed;
        copy.maxForce = this.maxForce;
        return copy;
    }
}
