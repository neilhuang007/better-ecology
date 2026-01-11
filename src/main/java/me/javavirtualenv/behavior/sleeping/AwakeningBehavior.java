package me.javavirtualenv.behavior.sleeping;

import me.javavirtualenv.behavior.core.Vec3d;

import java.util.List;

/**
 * Behavior for managing rapid awakening from sleep.
 * Handles quick responses to threats and post-awakening vigilance.
 * Refactored to be testable without Minecraft dependencies.
 */
public class AwakeningBehavior {

    private final SleepingConfig config;
    private int ticksSinceThreat = 0;
    private boolean isAwakening = false;
    private double postAwakeningVigilance = 1.0;
    private SleepContext currentContext = null;
    private double sleepDepth = 0.5;
    private long sleepStartTime = 0;

    public AwakeningBehavior(SleepingConfig config) {
        this.config = config;
    }

    /**
     * Triggers rapid awakening in response to a threat.
     * Calculates sleep depth based on time asleep before awakening.
     */
    public void awaken(SleepContext context) {
        this.currentContext = context;
        isAwakening = true;
        ticksSinceThreat = 0;
        postAwakeningVigilance = 1.0; // Max vigilance after awakening

        // Calculate sleep depth based on time asleep
        long currentTime = System.currentTimeMillis();
        if (sleepStartTime > 0) {
            long timeAsleepMs = currentTime - sleepStartTime;
            // Normalize to 0-1 range over 30 seconds (assuming ~30000ms for deep sleep)
            double calculatedDepth = Math.min(1.0, timeAsleepMs / 30000.0);
            this.sleepDepth = calculatedDepth;
        }
    }

    /**
     * Triggers awakening with a context.
     * Sets awakening state and initializes post-awakening vigilance.
     */
    public void triggerAwakening(SleepContext context) {
        awaken(context);
    }

    /**
     * Updates awakening state. Returns true if fully awakened.
     */
    public boolean update() {
        if (!isAwakening) {
            return false;
        }

        ticksSinceThreat++;

        // Gradually reduce post-awakening vigilance
        if (ticksSinceThreat > config.getRapidAwakeningResponseTicks()) {
            postAwakeningVigilance = Math.max(0.0,
                    1.0 - (ticksSinceThreat - config.getRapidAwakeningResponseTicks()) * 0.01);

            if (postAwakeningVigilance <= 0.0) {
                isAwakening = false;
                return true; // Fully awakened
            }
        }

        return false;
    }

    /**
     * Simulates a tick passing for the awakening behavior.
     */
    public void tick(SleepContext context) {
        this.currentContext = context;
        update();
    }

    /**
     * Returns true if currently in the awakening process.
     */
    public boolean isAwakening() {
        return isAwakening;
    }

    /**
     * Returns true if post-awakening vigilant for given context.
     */
    public boolean isPostAwakeningVigilant(SleepContext context) {
        return isAwakening || postAwakeningVigilance > 0.0;
    }

    /**
     * Gets the current post-awakening vigilance level (0.0 = normal, 1.0 = max).
     */
    public double getPostAwakeningVigilance() {
        return postAwakeningVigilance;
    }

    /**
     * Returns true if the animal can respond rapidly to a threat.
     */
    public boolean canRapidlyRespond() {
        return isAwakening || ticksSinceThreat < config.getRapidAwakeningResponseTicks() * 2;
    }

    /**
     * Gets the response time in ticks based on sleep depth.
     */
    public int getResponseTime(double sleepDepth) {
        // Deeper sleep = longer response time
        int baseResponse = config.getRapidAwakeningResponseTicks();
        return (int) (baseResponse * (1.0 + sleepDepth));
    }

    /**
     * Calculates awakening response ticks based on context and threat.
     */
    public long calculateAwakeningResponseTicks(SleepContext context, SleepEntity threat) {
        double distance = threat != null ? Math.sqrt(threat.distanceToSqr(
                context.getEntity().getX(),
                context.getEntity().getY(),
                context.getEntity().getZ()
        )) : 10.0;
        // Closer threats = faster response
        double urgency = Math.max(0.1, 1.0 - distance / 20.0);
        return (long) (getResponseTime(sleepDepth) * (1.0 - urgency * 0.5));
    }

    /**
     * Sets the current sleep depth.
     */
    public void setSleepDepth(double depth) {
        this.sleepDepth = Math.max(0.0, Math.min(1.0, depth));
    }

    /**
     * Gets the current sleep depth.
     */
    public double getSleepDepth() {
        return sleepDepth;
    }

    /**
     * Gets the flee response vector for the awakening behavior.
     * Calculates a direction away from the nearest threat.
     */
    public Vec3d getFleeResponse(SleepContext context) {
        if (context == null || context.getPosition() == null) {
            return new Vec3d(1, 0, 0); // Default flee direction
        }

        Vec3d fleeDirection = new Vec3d(0, 0, 0);
        SleepBlockPos pos = context.getBlockPos();
        double detectionRadius = 16.0;

        List<SleepEntity> nearbyEntities = context.getWorld().getNearbyEntities(
                pos.getX(), pos.getY(), pos.getZ(), detectionRadius
        );

        SleepEntity closestThreat = null;
        double closestDistance = Double.MAX_VALUE;

        for (SleepEntity entity : nearbyEntities) {
            if (entity.equals(context.getEntity())) {
                continue;
            }

            String entityString = entity.getEntityType().toLowerCase();
            boolean isThreat = entityString.contains("zombie")
                    || entityString.contains("skeleton")
                    || entityString.contains("creeper")
                    || entityString.contains("spider")
                    || entityString.contains("wolf")
                    || entityString.contains("fox");

            if (isThreat && entity.isAlive()) {
                double distance = entity.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestThreat = entity;
                }
            }
        }

        if (closestThreat != null) {
            // Calculate vector away from threat
            Vec3d ownPos = context.getPosition();
            Vec3d threatPos = new Vec3d(
                    closestThreat.getX(),
                    closestThreat.getY(),
                    closestThreat.getZ()
            );

            // Vector from threat to self (flee direction)
            fleeDirection = Vec3d.sub(ownPos, threatPos);

            // Normalize to get direction
            fleeDirection.normalize();
        } else {
            // No threat found, return default direction
            fleeDirection = new Vec3d(1, 0, 0);
        }

        return fleeDirection;
    }

    /**
     * Resets the awakening state.
     * Initializes sleep start time for sleep depth calculation.
     */
    public void reset() {
        isAwakening = false;
        ticksSinceThreat = 0;
        postAwakeningVigilance = 0.0;
        currentContext = null;
        sleepDepth = 0.0; // Start with shallow sleep
        sleepStartTime = System.currentTimeMillis(); // Track when sleep started
    }

    /**
     * Gets the current context.
     */
    public SleepContext getContext() {
        return currentContext;
    }

    /**
     * Returns true if the animal should awaken based on context.
     * Checks for nearby threats and other awakening conditions.
     */
    public boolean shouldAwaken(SleepContext context) {
        if (context == null || context.getWorld() == null) {
            return false;
        }

        // Check for nearby threats
        SleepBlockPos pos = context.getBlockPos();
        double detectionRadius = 16.0;

        List<SleepEntity> nearbyEntities = context.getWorld().getNearbyEntities(
                pos.getX(), pos.getY(), pos.getZ(), detectionRadius
        );

        for (SleepEntity entity : nearbyEntities) {
            // Skip self
            if (entity.equals(context.getEntity())) {
                continue;
            }

            // Check if entity is a threat
            String entityString = entity.getEntityType().toLowerCase();
            boolean isThreat = entityString.contains("zombie")
                    || entityString.contains("skeleton")
                    || entityString.contains("creeper")
                    || entityString.contains("spider")
                    || entityString.contains("wolf")
                    || entityString.contains("fox");

            if (isThreat && entity.isAlive()) {
                // Calculate distance
                double distance = entity.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());

                // Awaken if threat is close enough (within 10 blocks)
                if (distance <= 100.0) { // 10 blocks squared
                    return true;
                }
            }
        }

        return false;
    }
}
