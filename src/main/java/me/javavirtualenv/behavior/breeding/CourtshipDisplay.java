package me.javavirtualenv.behavior.breeding;

import me.javavirtualenv.behavior.core.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages courtship display behaviors.
 * Handles the initiation, progression, and termination of courtship rituals,
 * including intensity tracking and display force calculation.
 *
 * This redesigned version works with the BreedingEntity interface,
 * making it testable without mocking Minecraft classes.
 *
 * Research sources:
 * - Darwin, C. (1871). The Descent of Man, and Selection in Relation to Sex
 * - Bradbury, J.W., & Vehrencamp, S.L. (2011). Principles of Animal Communication
 * - Rosenthal, G.G. (2017). Mate Choice: The Evolution of Sexual Decision Making
 */
public class CourtshipDisplay {

    private final BreedingConfig config;
    private final Map<UUID, CourtshipState> activeCourtships;

    public CourtshipDisplay(BreedingConfig config) {
        this.config = config;
        this.activeCourtships = new HashMap<>();
    }

    /**
     * Determines if courtship should be initiated with a potential mate.
     * Checks distance, breeding status, and display range.
     *
     * @param performer The entity performing the display
     * @param potentialMate The potential mate to display to
     * @return True if courtship should be initiated
     */
    public boolean shouldInitiateCourtship(BreedingEntity performer, BreedingEntity potentialMate) {
        if (potentialMate == null || !potentialMate.isAlive()) {
            return false;
        }

        if (potentialMate.isInLove()) {
            return false;
        }

        double distance = calculateDistance(performer, potentialMate);
        return distance <= config.getDisplayRange();
    }

    /**
     * Starts a new courtship display for the performer.
     *
     * @param performer The entity beginning the courtship display
     */
    public void startCourtship(BreedingEntity performer) {
        UUID performerId = performer.getUuid();
        CourtshipState state = new CourtshipState();
        state.isActive = true;
        state.ticksElapsed = 0;
        state.intensity = 1.0;
        activeCourtships.put(performerId, state);
    }

    /**
     * Updates the courtship state, called each tick.
     * Advances the display and reduces intensity over time.
     */
    public void tick() {
        for (CourtshipState state : activeCourtships.values()) {
            if (state.isActive) {
                state.ticksElapsed++;
                double progress = (double) state.ticksElapsed / config.getCourtshipDuration();
                state.intensity = Math.max(0.0, 1.0 - progress);
            }
        }
    }

    /**
     * Checks if courtship is complete for the performer.
     *
     * @param performer The entity performing the display
     * @return True if courtship duration has elapsed
     */
    public boolean isCourtshipComplete(BreedingEntity performer) {
        CourtshipState state = activeCourtships.get(performer.getUuid());
        return state != null && state.ticksElapsed >= config.getCourtshipDuration();
    }

    /**
     * Checks if courtship is currently active for the performer.
     *
     * @param performer The entity to check
     * @return True if courtship is active
     */
    public boolean isCourtshipActive(BreedingEntity performer) {
        CourtshipState state = activeCourtships.get(performer.getUuid());
        return state != null && state.isActive;
    }

    /**
     * Gets the current intensity of the courtship display.
     *
     * @return Current intensity (0.0 to 1.0)
     */
    public double getCurrentIntensity() {
        for (CourtshipState state : activeCourtships.values()) {
            if (state.isActive) {
                return Math.max(0.0, state.intensity);
            }
        }
        return 0.0;
    }

    /**
     * Gets the intensity for a specific performer.
     *
     * @param performer The entity to check
     * @return Current intensity (0.0 to 1.0)
     */
    public double getCurrentIntensity(BreedingEntity performer) {
        CourtshipState state = activeCourtships.get(performer.getUuid());
        return state != null ? Math.max(0.0, state.intensity) : 0.0;
    }

    /**
     * Calculates the display force for movement during courtship.
     * Different display types generate different movement patterns.
     *
     * @param performer The entity performing the display
     * @return Force vector for display movement
     */
    public Vec3d calculateDisplayForce(BreedingEntity performer) {
        double intensity = getCurrentIntensity(performer);
        if (intensity <= 0.0) {
            return new Vec3d();
        }

        Vec3d force = new Vec3d();
        long gameTime = performer.getGameTime();

        switch (config.getDisplayType()) {
            case DANCING -> {
                force.x = Math.sin(gameTime * 0.1) * 0.15 * intensity;
                force.z = Math.cos(gameTime * 0.1) * 0.15 * intensity;
                force.y = Math.abs(Math.sin(gameTime * 0.05)) * 0.1 * intensity;
            }
            case POSTURING -> {
                force.y = 0.05 * intensity;
            }
            case VOCALIZATION -> {
                double vibration = (Math.random() - 0.5) * 0.05 * intensity;
                force.x = vibration;
                force.z = vibration;
            }
            case COLORATION, GIFT_GIVING, SCENT_MARKING -> {
                force.x = (Math.random() - 0.5) * 0.1 * intensity;
                force.z = (Math.random() - 0.5) * 0.1 * intensity;
            }
        }

        return force;
    }

    /**
     * Gets the configured display type.
     *
     * @return The display type for this courtship behavior
     */
    public DisplayType getDisplayType() {
        return config.getDisplayType();
    }

    /**
     * Handles rejection of courtship, terminating the display.
     *
     * @param performer The entity whose courtship was rejected
     */
    public void onRejected(BreedingEntity performer) {
        UUID performerId = performer.getUuid();
        CourtshipState state = activeCourtships.get(performerId);
        if (state != null) {
            state.isActive = false;
            state.ticksElapsed = 0;
            state.intensity = 0.0;
        }
    }

    /**
     * Clears completed courtships to prevent memory leaks.
     */
    public void cleanup() {
        activeCourtships.entrySet().removeIf(entry -> {
            CourtshipState state = entry.getValue();
            return !state.isActive || state.ticksElapsed >= config.getCourtshipDuration();
        });
    }

    /**
     * Internal state tracking for courtship displays.
     */
    private static class CourtshipState {
        boolean isActive;
        int ticksElapsed;
        double intensity;
    }

    /**
     * Calculates horizontal distance between performer and potential mate.
     */
    private double calculateDistance(BreedingEntity performer, BreedingEntity potentialMate) {
        Vec3d performerPos = performer.getPosition();
        Vec3d matePos = potentialMate.getPosition();
        double dx = performerPos.x - matePos.x;
        double dz = performerPos.z - matePos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
