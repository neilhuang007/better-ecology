package me.javavirtualenv.behavior.sleeping;

import java.util.List;

/**
 * Behavior for managing social sleeping patterns.
 * Handles group sleeping, sentinel duty rotation, and social sleep bonuses.
 * Refactored to be testable without Minecraft dependencies.
 */
public class SocialSleepBehavior {

    private final SleepingConfig config;
    private SleepEntity currentSentinel = null;
    private int sentinelRotationTimer = 0;
    private boolean hasSentinel = false;
    private int sentinelIndex = 0; // Track current sentinel index for round-robin

    public SocialSleepBehavior(SleepingConfig config) {
        this.config = config;
    }

    /**
     * Returns true if group sleeping is beneficial for the animal.
     */
    public boolean isGroupSleepingBeneficial(List<SleepEntity> nearbyGroup) {
        return nearbyGroup.size() >= 2;
    }

    /**
     * Calculates the safety bonus from sleeping in a group.
     */
    public double calculateGroupSafetyBonus(List<SleepEntity> nearbyGroup) {
        if (!config.isSentinelDuty() || nearbyGroup.isEmpty()) {
            return 0.0;
        }

        // Safety increases with group size, but with diminishing returns
        double bonus = Math.min(config.getSocialSleepBonus(),
                Math.log1p(nearbyGroup.size()) * 0.15);
        return bonus;
    }

    /**
     * Calculates social sleep bonus based on context.
     * Returns a safety bonus between 0.0 and 1.0 based on group size.
     */
    public double calculateSocialSleepBonus(SleepContext context) {
        if (!config.isSentinelDuty()) {
            return 0.0;
        }

        List<SleepEntity> nearbyGroup = getSleepingGroup(context);

        if (nearbyGroup.isEmpty()) {
            return 0.0;
        }

        // Safety increases with group size, but with diminishing returns
        // Using log function: bonus = log(1 + groupSize) * maxBonus / log(maxGroupSize)
        int groupSize = nearbyGroup.size();
        double maxBonus = config.getSocialSleepBonus();
        double bonus = Math.min(maxBonus, Math.log1p(groupSize) * 0.2);

        return Math.max(0.0, Math.min(1.0, bonus));
    }

    /**
     * Selects a sentinel from the group (if enabled).
     * Uses round-robin selection with index tracking.
     */
    public SleepEntity selectSentinel(List<SleepEntity> nearbyGroup) {
        if (!config.isSentinelDuty() || nearbyGroup.isEmpty()) {
            return null;
        }

        // Validate current sentinel is still in group
        if (currentSentinel != null && nearbyGroup.contains(currentSentinel)) {
            return currentSentinel;
        }

        // Reset index if group changed significantly or sentinel is invalid
        if (currentSentinel == null || !nearbyGroup.contains(currentSentinel)) {
            // Find current index if sentinel exists, otherwise reset to 0
            if (currentSentinel != null) {
                int oldIndex = nearbyGroup.indexOf(currentSentinel);
                if (oldIndex >= 0) {
                    sentinelIndex = oldIndex;
                }
            } else {
                sentinelIndex = 0;
            }
        }

        // Select sentinel using round-robin with wrapped index
        sentinelIndex = sentinelIndex % nearbyGroup.size();
        currentSentinel = nearbyGroup.get(sentinelIndex);
        sentinelRotationTimer = 0;

        return currentSentinel;
    }

    /**
     * Assigns sentinel duty to a mob in the context.
     */
    public void assignSentinelDuty(SleepContext context) {
        if (config.isSentinelDuty()) {
            hasSentinel = true;
        }
    }

    /**
     * Returns true if there is currently a sentinel.
     */
    public boolean hasSentinel(SleepContext context) {
        return hasSentinel && config.isSentinelDuty();
    }

    /**
     * Updates sentinel rotation timer and returns true if rotation is needed.
     */
    public boolean shouldRotateSentinel() {
        if (!config.isSentinelDuty()) {
            return false;
        }

        sentinelRotationTimer++;
        if (sentinelRotationTimer >= config.getSentinelRotationInterval()) {
            sentinelRotationTimer = 0;
            return true;
        }
        return false;
    }

    /**
     * Gets the current sentinel mob.
     */
    public SleepEntity getCurrentSentinel() {
        return currentSentinel;
    }

    /**
     * Sets the current sentinel mob.
     */
    public void setCurrentSentinel(SleepEntity sentinel) {
        this.currentSentinel = sentinel;
        this.sentinelRotationTimer = 0;
    }

    /**
     * Returns true if a given mob is currently the sentinel.
     */
    public boolean isSentinel(SleepEntity mob) {
        return mob != null && mob.equals(currentSentinel);
    }

    /**
     * Rotates sentinel duty to the next mob in the group.
     * Uses tracked index for proper round-robin behavior.
     */
    public void rotateSentinel(List<SleepEntity> group) {
        if (group.isEmpty()) {
            currentSentinel = null;
            hasSentinel = false;
            sentinelIndex = 0;
            return;
        }

        // Increment index for round-robin, wrapping with modulo
        sentinelIndex = (sentinelIndex + 1) % group.size();
        currentSentinel = group.get(sentinelIndex);
        sentinelRotationTimer = 0;
    }

    /**
     * Calculates ideal group size for optimal safety.
     */
    public int calculateIdealGroupSize() {
        // Returns minimum group size for full sentinel benefit
        return 3;
    }

    /**
     * Returns true if the group is large enough for sentinel rotation.
     */
    public boolean canRotateSentinels(List<SleepEntity> group) {
        return group.size() >= 2;
    }

    /**
     * Resets sentinel state.
     * Also resets the round-robin index.
     */
    public void resetSentinel() {
        currentSentinel = null;
        hasSentinel = false;
        sentinelRotationTimer = 0;
        sentinelIndex = 0; // Reset round-robin index
    }

    /**
     * Simulates a tick passing for social sleep behavior.
     */
    public void tick(SleepContext context) {
        sentinelRotationTimer++;
        if (shouldRotateSentinel()) {
            sentinelRotationTimer = 0;
        }
    }

    /**
     * Rotates sentinel duty based on context.
     */
    public void rotateSentinelDuty(SleepContext context) {
        if (hasSentinel) {
            hasSentinel = false; // Toggle sentinel status
        }
    }

    /**
     * Returns true if the sentinel for this context is awake.
     */
    public boolean isSentinelAwake(SleepContext context) {
        return hasSentinel && config.isSentinelDuty();
    }

    /**
     * Gets the current sentinel for a given context.
     */
    public SleepEntity getCurrentSentinel(SleepContext context) {
        return currentSentinel;
    }

    /**
     * Gets the sleeping group for the given context.
     * Returns a list of nearby mobs of the same species within group radius.
     */
    public List<SleepEntity> getSleepingGroup(SleepContext context) {
        SleepBlockPos pos = context.getBlockPos();
        double radius = config.getGroupSleepRadius();

        List<SleepEntity> nearbyEntities = context.getWorld().getNearbyEntities(
                pos.getX(), pos.getY(), pos.getZ(), radius
        );

        List<SleepEntity> sleepingGroup = new java.util.ArrayList<>();
        String ownSpecies = context.getEntity().getEntityType().toLowerCase();

        for (SleepEntity entity : nearbyEntities) {
            // Skip self
            if (entity.equals(context.getEntity())) {
                continue;
            }

            // Check if same species
            String entitySpecies = entity.getEntityType().toLowerCase();
            if (!entitySpecies.contains(ownSpecies.substring(0, Math.min(ownSpecies.length(), 10)))) {
                continue;
            }

            // Must be alive
            if (!entity.isAlive()) {
                continue;
            }

            sleepingGroup.add(entity);
        }

        return sleepingGroup;
    }

    /**
     * Selects a sleep position for the dominant animal.
     * Returns a preferred position within the group sleeping area.
     */
    public SleepBlockPos selectSleepPosition(SleepContext context, SleepEntity dominantAnimal) {
        if (context == null || context.getBlockPos() == null) {
            return new SleepBlockPos(0, 64, 0);
        }

        // Start with current position
        SleepBlockPos basePos = context.getBlockPos();

        if (dominantAnimal == null || !dominantAnimal.isAlive()) {
            return basePos;
        }

        // Dominant animals get priority positions (center of group)
        List<SleepEntity> group = getSleepingGroup(context);

        if (group.isEmpty()) {
            return basePos;
        }

        // Calculate center of group
        double centerX = 0;
        double centerY = 0;
        double centerZ = 0;

        for (SleepEntity entity : group) {
            centerX += entity.getX();
            centerY += entity.getY();
            centerZ += entity.getZ();
        }

        centerX /= (group.size() + 1); // +1 for self
        centerY /= (group.size() + 1);
        centerZ /= (group.size() + 1);

        return new SleepBlockPos(
                (int) Math.floor(centerX),
                (int) Math.floor(centerY),
                (int) Math.floor(centerZ)
        );
    }
}
