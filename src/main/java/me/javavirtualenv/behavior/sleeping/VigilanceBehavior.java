package me.javavirtualenv.behavior.sleeping;

import java.util.List;

/**
 * Behavior for managing vigilance during sleep.
 * Adjusts sleep depth based on threat level and environmental conditions.
 * Refactored to be testable without Minecraft dependencies.
 */
public class VigilanceBehavior {

    private final SleepingConfig config;
    private double currentThreatLevel = 0.0;
    private double currentSleepDepth = 0.0;
    private boolean lightSleepMode = false;

    public VigilanceBehavior(SleepingConfig config) {
        this.config = config;
    }

    /**
     * Updates vigilance based on current threat level.
     * Higher threat leads to lighter sleep.
     */
    public void updateVigilance(double threatLevel) {
        this.currentThreatLevel = threatLevel;

        // Adjust sleep depth based on threat
        if (threatLevel > config.getVigilanceThreshold()) {
            // Enter light sleep mode
            currentSleepDepth = Math.max(0.0, config.getLightSleepThreshold() - threatLevel * 0.5);
            lightSleepMode = true;
        } else {
            // Can enter deep sleep
            currentSleepDepth = Math.min(1.0, threatLevel * 0.3 + 0.5);
            lightSleepMode = false;
        }
    }

    /**
     * Returns the current sleep depth (0.0 = fully alert, 1.0 = deep sleep).
     */
    public double getSleepDepth() {
        return currentSleepDepth;
    }

    /**
     * Returns true if the animal is in light sleep mode.
     */
    public boolean isLightSleep() {
        return currentSleepDepth < config.getDeepSleepThreshold();
    }

    /**
     * Returns true if the animal is in deep sleep mode.
     */
    public boolean isDeepSleep() {
        return currentSleepDepth >= config.getDeepSleepThreshold();
    }

    /**
     * Gets the current threat level.
     */
    public double getThreatLevel() {
        return currentThreatLevel;
    }

    /**
     * Applies social sleep bonus (reduced threat perception when in groups).
     */
    public void applySocialSleepBonus(double groupSize) {
        double bonus = Math.min(config.getSocialSleepBonus(), groupSize * 0.1);
        currentThreatLevel = Math.max(0.0, currentThreatLevel - bonus);
    }

    /**
     * Calculates threat level based on nearby entities.
     * Returns a normalized threat level between 0.0 and 1.0.
     */
    public double calculateThreatLevel(SleepContext context) {
        SleepBlockPos pos = context.getBlockPos();
        double searchRadius = 32.0;

        List<SleepEntity> nearbyEntities = context.getWorld().getNearbyEntities(
                pos.getX(), pos.getY(), pos.getZ(), searchRadius
        );

        if (nearbyEntities.isEmpty()) {
            return 0.0;
        }

        double totalThreat = 0.0;
        double maxThreat = 0.0;

        for (SleepEntity entity : nearbyEntities) {
            // Skip the entity itself
            if (entity.equals(context.getEntity())) {
                continue;
            }

            // Check if entity is a potential predator or threat
            String entityString = entity.getEntityType().toLowerCase();
            boolean isHostile = entityString.contains("zombie")
                    || entityString.contains("skeleton")
                    || entityString.contains("creeper")
                    || entityString.contains("spider")
                    || entityString.contains("phantom")
                    || entityString.contains("witch");

            boolean isPredator = entityString.contains("wolf")
                    || entityString.contains("fox")
                    || entityString.contains("cat")
                    || entityString.contains("ocelot");

            if (!isHostile && !isPredator) {
                continue;
            }

            // Calculate distance-based threat
            double distance = entity.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            double distNormalized = Math.sqrt(distance) / searchRadius;

            // Threat increases with proximity (inverse of distance)
            double proximityThreat = 1.0 - Math.max(0.0, distNormalized);

            // Weight by threat type
            double threatWeight = isHostile ? 1.0 : 0.7;
            double entityThreat = proximityThreat * threatWeight;

            totalThreat += entityThreat;
            maxThreat = Math.max(maxThreat, entityThreat);
        }

        // Use max threat for individual detection, but allow multiple threats to compound
        double combinedThreat = maxThreat + (totalThreat * 0.2);

        // Clamp to [0, 1]
        return Math.max(0.0, Math.min(1.0, combinedThreat));
    }

    /**
     * Determines if light sleep should be used based on threat level.
     */
    public boolean shouldUseLightSleep(SleepContext context, double threatLevel) {
        double clampedThreat = Math.max(0.0, Math.min(1.0, threatLevel));
        return clampedThreat > config.getLightSleepThreshold();
    }

    /**
     * Adjusts sleep duration based on threat level.
     */
    public int adjustSleepDuration(int baseDuration, double threatLevel) {
        double reduction = Math.min(0.5, threatLevel * 0.5);
        return (int) (baseDuration * (1.0 - reduction));
    }

    /**
     * Sets light sleep mode.
     */
    public void setLightSleep(boolean lightSleep) {
        this.lightSleepMode = lightSleep;
        if (lightSleep) {
            currentSleepDepth = Math.min(currentSleepDepth, config.getLightSleepThreshold());
        }
    }

    /**
     * Returns true if threats can be detected during sleep.
     */
    public boolean canDetectThreatsDuringSleep(SleepContext context) {
        return isLightSleep() || currentThreatLevel < config.getVigilanceThreshold();
    }

    /**
     * Increases threat level by a specified amount.
     */
    public void increaseThreatLevel(double amount) {
        currentThreatLevel = Math.min(1.0, currentThreatLevel + amount);
        updateVigilance(currentThreatLevel);
    }

    /**
     * Decreases threat level by a specified amount.
     */
    public void decreaseThreatLevel(double amount) {
        currentThreatLevel = Math.max(0.0, currentThreatLevel - amount);
        updateVigilance(currentThreatLevel);
    }

    /**
     * Gets the appropriate sleep posture based on threat level and context.
     * Considers health, threat level, and environmental conditions.
     */
    public SleepingConfig.SleepPosture getSleepPosture(SleepContext context, double threatLevel) {
        // Check if entity is injured (low health)
        double healthPercent = context.getEntity().getHealthPercent();

        // Injured animals use protective curled posture
        if (healthPercent < 0.5) {
            return SleepingConfig.SleepPosture.CURLED_UP;
        }

        // High threat - alert posture
        if (threatLevel > config.getVigilanceThreshold()) {
            return SleepingConfig.SleepPosture.SITTING_ALERT;
        }

        // Medium threat - can use sitting or curled
        if (threatLevel > config.getLightSleepThreshold()) {
            return lightSleepMode ? SleepingConfig.SleepPosture.SITTING_ALERT : SleepingConfig.SleepPosture.CURLED_UP;
        }

        // Low threat - deep sleep posture
        return SleepingConfig.SleepPosture.LYING_DOWN;
    }
}
