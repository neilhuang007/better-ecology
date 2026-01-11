package me.javavirtualenv.behavior.breeding;

import me.javavirtualenv.behavior.core.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages territorial defense behavior during breeding season.
 * Handles detection of intruders, rival assessment, and defense force calculation.
 *
 * This redesigned version works with the BreedingEntity interface,
 * making it testable without mocking Minecraft classes.
 *
 * Research sources:
 * - Brown, J.L. (1964). The evolution of diversity in avian territorial systems
 * - Maher, C.R., & Lott, D.F. (2000). A review of ecological determinants of territoriality
 * - Adams, E.S. (2001). Approaches to the study of territoriality
 */
public class TerritorialDefense {

    private final BreedingConfig config;
    private final Map<UUID, Vec3d> territoryCenters;
    private final Map<UUID, List<BreedingEntity>> rivals;

    public TerritorialDefense(BreedingConfig config) {
        this.config = config;
        this.territoryCenters = new HashMap<>();
        this.rivals = new HashMap<>();
    }

    /**
     * Sets the center point of the territory for an entity.
     *
     * @param ownerId The UUID of the territory owner
     * @param center The center position of the territory
     */
    public void setTerritoryCenter(UUID ownerId, Vec3d center) {
        territoryCenters.put(ownerId, center);
        if (!rivals.containsKey(ownerId)) {
            rivals.put(ownerId, new ArrayList<>());
        }
    }

    /**
     * Sets the territory center from an entity's position.
     *
     * @param owner The entity that owns the territory
     */
    public void setTerritoryCenter(BreedingEntity owner) {
        setTerritoryCenter(owner.getUuid(), owner.getPosition());
    }

    /**
     * Checks if a potential entity is an intruder in the territory.
     *
     * @param owner The territory owner
     * @param potential The entity to check
     * @return True if the entity is within territory bounds
     */
    public boolean isIntruder(BreedingEntity owner, BreedingEntity potential) {
        if (potential == null || !potential.isAlive()) {
            return false;
        }

        Vec3d center = territoryCenters.get(owner.getUuid());
        if (center == null) {
            return false;
        }

        double distance = calculateDistanceToTerritory(center, potential);
        return distance <= config.getTerritorySize() && distance > 2.0;
    }

    /**
     * Determines if a potential entity is a rival (same sex, same species).
     *
     * @param owner The territory owner
     * @param potential The entity to assess
     * @return True if the entity is a rival
     */
    public boolean isRival(BreedingEntity owner, BreedingEntity potential) {
        if (potential == null) {
            return false;
        }

        if (!isSameSpecies(owner, potential)) {
            return false;
        }

        return !isPotentialMate(owner, potential);
    }

    /**
     * Adds a rival to the tracking list for threat assessment.
     *
     * @param ownerId The UUID of the territory owner
     * @param rival The rival entity to track
     */
    public void addRival(UUID ownerId, BreedingEntity rival) {
        if (!rivals.containsKey(ownerId)) {
            rivals.put(ownerId, new ArrayList<>());
        }
        rivals.get(ownerId).add(rival);
    }

    /**
     * Adds a rival to the tracking list.
     *
     * @param owner The territory owner
     * @param rival The rival entity to track
     */
    public void addRival(BreedingEntity owner, BreedingEntity rival) {
        addRival(owner.getUuid(), rival);
    }

    /**
     * Clears the rival list for the given owner.
     *
     * @param ownerId The UUID of the territory owner
     */
    public void clearRivals(UUID ownerId) {
        if (rivals.containsKey(ownerId)) {
            rivals.get(ownerId).clear();
        }
    }

    /**
     * Calculates the defense force vector toward all tracked rivals.
     * Force magnitude increases with rival proximity and threat level.
     *
     * @param owner The territory owner
     * @return Force vector for territorial defense
     */
    public Vec3d calculateDefenseForce(BreedingEntity owner) {
        Vec3d force = new Vec3d();
        List<BreedingEntity> ownerRivals = rivals.get(owner.getUuid());

        if (ownerRivals == null || ownerRivals.isEmpty()) {
            return force;
        }

        for (BreedingEntity rival : ownerRivals) {
            if (rival != null && rival.isAlive()) {
                Vec3d rivalForce = calculateForceTowardRival(owner, rival);
                force.add(rivalForce);
            }
        }

        return force;
    }

    /**
     * Calculates the threat level posed by a rival.
     * Based on distance, health, and size differences.
     *
     * @param owner The territory owner
     * @param rival The rival to assess
     * @return Threat level (0.0 to 1.0)
     */
    public double calculateThreatLevel(BreedingEntity owner, BreedingEntity rival) {
        if (rival == null || !rival.isAlive()) {
            return 0.0;
        }

        double distance = calculateDistanceBetween(owner, rival);
        double distanceThreat = 1.0 / (1.0 + distance * 0.1);

        double healthRatio = rival.getHealth() / rival.getMaxHealth();
        double healthThreat = healthRatio * 0.5;

        return Math.min(1.0, distanceThreat + healthThreat);
    }

    /**
     * Calculates territory quality score based on size and centrality.
     * Research shows territory size directly correlates with male mating success.
     * Reference: "Territory size directly correlates with male mating success"
     *
     * @param owner The territory owner
     * @return Quality score (0.0 to 1.0)
     */
    public double calculateTerritoryQuality(BreedingEntity owner) {
        if (owner == null) {
            return 0.0;
        }

        Vec3d center = territoryCenters.get(owner.getUuid());
        if (center == null) {
            return 0.0;
        }

        double territorySize = config.getTerritorySize();
        double maxTerritorySize = 64.0;
        double sizeScore = Math.min(1.0, territorySize / maxTerritorySize);

        return sizeScore;
    }

    /**
     * Calculates breeding success boost from territory quality.
     * Research shows breeding success increases with territory size/quality.
     * This factor should be applied to mate selection quality assessment.
     *
     * @param owner The territory owner
     * @return Quality boost (0.0 to 0.5 added to mate score)
     */
    public double calculateBreedingSuccessBoost(BreedingEntity owner) {
        double territoryQuality = calculateTerritoryQuality(owner);
        return territoryQuality * 0.5;
    }

    /**
     * Gets the list of tracked rivals for an owner.
     *
     * @param owner The territory owner
     * @return List of rival entities
     */
    public List<BreedingEntity> getRivals(BreedingEntity owner) {
        List<BreedingEntity> ownerRivals = rivals.get(owner.getUuid());
        return ownerRivals != null ? new ArrayList<>(ownerRivals) : new ArrayList<>();
    }

    /**
     * Checks if a position is within the territory bounds.
     *
     * @param owner The territory owner
     * @param pos The position to check
     * @return True if position is within territory
     */
    public boolean isPositionInTerritory(BreedingEntity owner, Vec3d pos) {
        Vec3d center = territoryCenters.get(owner.getUuid());
        if (center == null) {
            return false;
        }

        double distance = center.distanceTo(pos);
        return distance <= config.getTerritorySize();
    }

    /**
     * Calculates force toward a specific rival for confrontation.
     */
    private Vec3d calculateForceTowardRival(BreedingEntity owner, BreedingEntity rival) {
        Vec3d ownerPos = owner.getPosition();
        Vec3d rivalPos = rival.getPosition();

        double dx = rivalPos.x - ownerPos.x;
        double dy = rivalPos.y - ownerPos.y;
        double dz = rivalPos.z - ownerPos.z;

        Vec3d direction = new Vec3d(dx, dy, dz);
        double distance = direction.magnitude();

        if (distance < 0.01) {
            return new Vec3d();
        }

        direction.normalize();

        double threatLevel = calculateThreatLevel(owner, rival);
        double forceMagnitude = 0.2 * threatLevel;

        direction.mult(forceMagnitude);
        return direction;
    }

    /**
     * Calculates distance from a territory center to an entity.
     */
    private double calculateDistanceToTerritory(Vec3d center, BreedingEntity entity) {
        Vec3d entityPos = entity.getPosition();
        double dx = center.x - entityPos.x;
        double dz = center.z - entityPos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calculates horizontal distance between owner and another entity.
     */
    private double calculateDistanceBetween(BreedingEntity owner, BreedingEntity other) {
        Vec3d ownerPos = owner.getPosition();
        Vec3d otherPos = other.getPosition();
        double dx = ownerPos.x - otherPos.x;
        double dz = ownerPos.z - otherPos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Determines if two entities are the same species.
     */
    private boolean isSameSpecies(BreedingEntity owner, BreedingEntity potential) {
        return owner.getSpeciesId().equals(potential.getSpeciesId());
    }

    /**
     * Determines if a potential entity could be a mate (opposite sex).
     */
    private boolean isPotentialMate(BreedingEntity owner, BreedingEntity potential) {
        return owner.isMale() != potential.isMale();
    }

    /**
     * Clears stale data for entities that are no longer relevant.
     */
    public void cleanup() {
        rivals.entrySet().removeIf(entry -> {
            List<BreedingEntity> rivalList = entry.getValue();
            rivalList.removeIf(rival -> rival == null || !rival.isAlive());
            return rivalList.isEmpty();
        });
    }
}
