package me.javavirtualenv.behavior.breeding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages mate fidelity and pair bonding across seasons.
 * Tracks previous mates and applies fidelity preferences in mate selection.
 *
 * This redesigned version works with the BreedingEntity interface,
 * making it testable without mocking Minecraft classes.
 *
 * Research sources:
 * - Black, J.M. (1996). Partnerships in birds: The influence of sex and kinship
 * - Choudhury, S., & Black, J.M. (1993). Mate-selection and pair formation
 * - Ens, B.J., et al. (1993). The territorial defence and mate fidelity
 */
public class MateFidelity {

    private final BreedingConfig config;
    private final Map<UUID, UUID> previousMates;
    private final Map<UUID, Long> lastBreedingTime;

    public MateFidelity(BreedingConfig config) {
        this.config = config;
        this.previousMates = new HashMap<>();
        this.lastBreedingTime = new HashMap<>();
    }

    /**
     * Records a previous mate for an entity.
     *
     * @param entity The entity that bred
     * @param mate The UUID of the mate
     */
    public void recordPreviousMate(BreedingEntity entity, UUID mate) {
        if (entity == null || mate == null) {
            return;
        }
        previousMates.put(entity.getUuid(), mate);
        lastBreedingTime.put(entity.getUuid(), entity.getGameTime());
    }

    /**
     * Checks if an entity has a recorded previous mate.
     *
     * @param entity The entity to check
     * @return True if a previous mate exists
     */
    public boolean hasPreviousMate(BreedingEntity entity) {
        if (entity == null) {
            return false;
        }
        UUID previousMate = previousMates.get(entity.getUuid());
        return previousMate != null;
    }

    /**
     * Gets the UUID of the previous mate for an entity.
     *
     * @param entity The entity to query
     * @return The previous mate's UUID, or null if none
     */
    public UUID getPreviousMate(BreedingEntity entity) {
        if (entity == null) {
            return null;
        }
        return previousMates.get(entity.getUuid());
    }

    /**
     * Determines if an entity should prefer their previous mate.
     * Based on the configured mate fidelity threshold.
     *
     * @param entity The entity making the decision
     * @return True if previous mate should be preferred
     */
    public boolean shouldPreferPreviousMate(BreedingEntity entity) {
        if (!hasPreviousMate(entity)) {
            return false;
        }

        double fidelityScore = config.getMateFidelity();
        return Math.random() < fidelityScore;
    }

    /**
     * Applies mate fidelity to a list of potential mates.
     * If fidelity is high and previous mate is available, prefer them.
     *
     * @param chooser The entity seeking a mate
     * @param potentialMates List of potential mates
     * @return The selected mate based on fidelity preferences
     */
    public BreedingEntity applyMateFidelity(BreedingEntity chooser, List<BreedingEntity> potentialMates) {
        if (potentialMates == null || potentialMates.isEmpty()) {
            return null;
        }

        if (!hasPreviousMate(chooser) || !shouldPreferPreviousMate(chooser)) {
            return potentialMates.get(0);
        }

        UUID previousMateId = getPreviousMate(chooser);

        for (BreedingEntity mate : potentialMates) {
            if (mate != null && mate.getUuid().equals(previousMateId)) {
                return mate;
            }
        }

        return potentialMates.get(0);
    }

    /**
     * Filters potential mates to prioritize the previous mate.
     * Returns a sorted list with previous mate first if available.
     *
     * @param chooser The entity seeking a mate
     * @param potentialMates List of potential mates
     * @return Sorted list with previous mate prioritized
     */
    public List<BreedingEntity> filterByFidelity(BreedingEntity chooser, List<BreedingEntity> potentialMates) {
        if (potentialMates == null || potentialMates.isEmpty()) {
            return List.of();
        }

        if (!hasPreviousMate(chooser)) {
            return potentialMates;
        }

        UUID previousMateId = getPreviousMate(chooser);

        return potentialMates.stream()
            .sorted((a, b) -> {
                boolean aIsPrevious = a.getUuid().equals(previousMateId);
                boolean bIsPrevious = b.getUuid().equals(previousMateId);

                if (aIsPrevious && !bIsPrevious) {
                    return -1;
                } else if (!aIsPrevious && bIsPrevious) {
                    return 1;
                } else {
                    return 0;
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Calculates the "bond strength" with a previous mate.
     * Based on time since last breeding and fidelity level.
     *
     * @param entity The entity to check
     * @return Bond strength (0.0 to 1.0)
     */
    public double calculateBondStrength(BreedingEntity entity) {
        if (!hasPreviousMate(entity)) {
            return 0.0;
        }

        Long lastBred = lastBreedingTime.get(entity.getUuid());
        if (lastBred == null) {
            return 0.0;
        }

        long currentTime = entity.getGameTime();
        long timeSinceBreeding = currentTime - lastBred;

        double timeDecay = Math.exp(-timeSinceBreeding / 120000.0);
        return config.getMateFidelity() * timeDecay;
    }

    /**
     * Checks if a bond with a previous mate is still active.
     *
     * @param entity The entity to check
     * @return True if bond is still active
     */
    public boolean isBondActive(BreedingEntity entity) {
        return calculateBondStrength(entity) > 0.3;
    }

    /**
     * Clears the previous mate record for an entity.
     * Used when a mate dies or the bond is broken.
     *
     * @param entity The entity to clear
     */
    public void clearPreviousMate(BreedingEntity entity) {
        if (entity == null) {
            return;
        }
        UUID entityId = entity.getUuid();
        previousMates.remove(entityId);
        lastBreedingTime.remove(entityId);
    }

    /**
     * Gets the time since last breeding for an entity.
     *
     * @param entity The entity to check
     * @return Ticks since last breeding, or -1 if never bred
     */
    public long getTimeSinceLastBreeding(BreedingEntity entity) {
        if (entity == null) {
            return -1;
        }

        Long lastBred = lastBreedingTime.get(entity.getUuid());
        if (lastBred == null) {
            return -1;
        }

        return entity.getGameTime() - lastBred;
    }

    /**
     * Cleans up old records to prevent memory leaks.
     * Removes records for entities that haven't bred in over a year.
     */
    public void cleanup(BreedingEntity context) {
        long currentTime = context.getGameTime();
        long oneYearTicks = 360 * 24000L;

        lastBreedingTime.entrySet().removeIf(entry -> {
            long timeSinceBreeding = currentTime - entry.getValue();
            return timeSinceBreeding > oneYearTicks;
        });

        previousMates.keySet().removeIf(uuid -> !lastBreedingTime.containsKey(uuid));
    }
}
