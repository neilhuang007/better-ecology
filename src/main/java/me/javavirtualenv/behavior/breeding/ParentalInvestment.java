package me.javavirtualenv.behavior.breeding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages parental investment and care behaviors.
 * Tracks offspring, calculates care levels, and supports biparental care.
 *
 * This redesigned version works with the BreedingEntity interface,
 * making it testable without mocking Minecraft classes.
 *
 * Research sources:
 * - Trivers, R.L. (1972). Parental investment and sexual selection
 * - Clutton-Brock, T.H. (1991). The Evolution of Parental Care
 * - Royle, N.J., et al. (2012). Evolution of parental care strategies
 */
public class ParentalInvestment {

    private final BreedingConfig config;
    private final Map<UUID, List<UUID>> offspringMap;
    private final Map<UUID, Integer> careLevelMap;

    public ParentalInvestment(BreedingConfig config) {
        this.config = config;
        this.offspringMap = new HashMap<>();
        this.careLevelMap = new HashMap<>();
    }

    /**
     * Registers an offspring as the responsibility of a parent.
     *
     * @param parent The parent entity
     * @param offspring The UUID of the offspring
     */
    public void registerOffspring(BreedingEntity parent, UUID offspring) {
        if (parent == null || offspring == null) {
            return;
        }

        UUID parentId = parent.getUuid();
        offspringMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(offspring);

        int currentCare = careLevelMap.getOrDefault(parentId, 0);
        careLevelMap.put(parentId, currentCare + 1);
    }

    /**
     * Checks if a parent has any registered offspring.
     *
     * @param parent The parent to check
     * @return True if parent has offspring
     */
    public boolean hasOffspring(BreedingEntity parent) {
        if (parent == null) {
            return false;
        }

        UUID parentId = parent.getUuid();
        List<UUID> offspring = offspringMap.get(parentId);

        return offspring != null && !offspring.isEmpty();
    }

    /**
     * Checks if a specific parent is responsible for a specific offspring.
     *
     * @param parent The parent to check
     * @param offspring The offspring UUID
     * @return True if parent is responsible for this offspring
     */
    public boolean isResponsibleFor(BreedingEntity parent, UUID offspring) {
        if (parent == null || offspring == null) {
            return false;
        }

        UUID parentId = parent.getUuid();
        List<UUID> parentOffspring = offspringMap.get(parentId);

        return parentOffspring != null && parentOffspring.contains(offspring);
    }

    /**
     * Calculates the current care level for a parent.
     * Based on number of offspring and configured investment level.
     * Based on Trivers' (1972) theory of parental investment.
     *
     * @param parent The parent entity
     * @return Care level (0.0 to 1.0)
     */
    public double calculateCareLevel(BreedingEntity parent) {
        if (parent == null) {
            return 0.0;
        }

        double baseCareLevel = config.getParentalInvestmentLevel();

        UUID parentId = parent.getUuid();
        List<UUID> offspring = offspringMap.get(parentId);

        if (offspring == null || offspring.isEmpty()) {
            return 0.0;
        }

        double offspringCount = offspring.size();
        double dilutedCare = baseCareLevel / (1.0 + offspringCount * 0.2);

        return Math.max(0.0, Math.min(1.0, dilutedCare));
    }

    /**
     * Calculates mate choosiness based on parental investment level.
     * According to Trivers' (1972) theory: the sex with higher parental
     * investment becomes the limiting resource and is choosier in mate selection.
     *
     * Higher investment = higher choosiness (more selective)
     * Lower investment = lower choosiness (less selective, more competitive)
     *
     * @param parent The parent entity
     * @return Choosiness factor (0.0 to 1.0, where 1.0 = most choosy)
     */
    public double calculateMateChoosiness(BreedingEntity parent) {
        if (parent == null) {
            return 0.5;
        }

        double investmentLevel = config.getParentalInvestmentLevel();
        boolean isBiparental = config.isBiparentalCare();

        if (!isBiparental) {
            if (parent.isMale()) {
                return 0.3;
            } else {
                return 0.7 + (investmentLevel * 0.3);
            }
        }

        return 0.5 + (investmentLevel * 0.3);
    }

    /**
     * Calculates sexual selection pressure based on investment asymmetry.
     * When one sex invests more, the other sex experiences stronger
     * intrasexual competition.
     * Reference: Trivers (1972), Clutton-Brock (1991).
     *
     * @param entity The entity to check
     * @return Competition pressure (0.0 to 1.0)
     */
    public double calculateCompetitionPressure(BreedingEntity entity) {
        if (entity == null) {
            return 0.5;
        }

        boolean isBiparental = config.isBiparentalCare();

        if (isBiparental) {
            return 0.5;
        }

        if (entity.isMale()) {
            return 0.8;
        } else {
            return 0.3;
        }
    }

    /**
     * Checks if both parents are investing in the same offspring.
     *
     * @param mother The mother entity
     * @param father The father entity
     * @param offspring The offspring UUID
     * @return True if both parents care for this offspring
     */
    public boolean bothParentsInvest(BreedingEntity mother, BreedingEntity father, UUID offspring) {
        if (!config.isBiparentalCare()) {
            return false;
        }

        boolean motherCares = mother != null && isResponsibleFor(mother, offspring);
        boolean fatherCares = father != null && isResponsibleFor(father, offspring);

        return motherCares && fatherCares;
    }

    /**
     * Gets the list of offspring for a parent.
     *
     * @param parent The parent entity
     * @return List of offspring UUIDs
     */
    public List<UUID> getOffspring(BreedingEntity parent) {
        if (parent == null) {
            return List.of();
        }

        UUID parentId = parent.getUuid();
        List<UUID> offspring = offspringMap.get(parentId);

        return offspring != null ? new ArrayList<>(offspring) : List.of();
    }

    /**
     * Gets the number of offspring a parent is responsible for.
     *
     * @param parent The parent entity
     * @return Number of offspring
     */
    public int getOffspringCount(BreedingEntity parent) {
        if (parent == null) {
            return 0;
        }

        UUID parentId = parent.getUuid();
        List<UUID> offspring = offspringMap.get(parentId);

        return offspring != null ? offspring.size() : 0;
    }

    /**
     * Removes an offspring from a parent's responsibility list.
     * Called when offspring becomes independent or dies.
     *
     * @param parent The parent entity
     * @param offspring The offspring UUID to remove
     */
    public void removeOffspring(BreedingEntity parent, UUID offspring) {
        if (parent == null || offspring == null) {
            return;
        }

        UUID parentId = parent.getUuid();
        List<UUID> parentOffspring = offspringMap.get(parentId);

        if (parentOffspring != null) {
            parentOffspring.remove(offspring);

            Integer currentCare = careLevelMap.get(parentId);
            if (currentCare != null && currentCare > 0) {
                careLevelMap.put(parentId, currentCare - 1);
            }
        }
    }

    /**
     * Clears all offspring for a parent.
     * Used when offspring become independent simultaneously.
     *
     * @param parent The parent entity
     */
    public void clearOffspring(BreedingEntity parent) {
        if (parent == null) {
            return;
        }

        UUID parentId = parent.getUuid();
        offspringMap.remove(parentId);
        careLevelMap.remove(parentId);
    }

    /**
     * Calculates the resource allocation per offspring.
     * Higher values mean more care per individual offspring.
     *
     * @param parent The parent entity
     * @return Allocation level (0.0 to 1.0)
     */
    public double calculatePerOffspringAllocation(BreedingEntity parent) {
        int offspringCount = getOffspringCount(parent);
        if (offspringCount == 0) {
            return 0.0;
        }

        double totalInvestment = config.getParentalInvestmentLevel();
        return totalInvestment / offspringCount;
    }

    /**
     * Checks if a parent should accept another offspring.
     * Based on current offspring count and investment capacity.
     *
     * @param parent The parent entity
     * @return True if parent can accept more offspring
     */
    public boolean canAcceptMoreOffspring(BreedingEntity parent) {
        int currentCount = getOffspringCount(parent);

        // Can accept if under the max count
        if (currentCount >= 10) {
            return false;
        }

        // If no offspring, parent can always accept
        if (currentCount == 0) {
            return true;
        }

        // Otherwise check if per-offspring care is sufficient
        double perOffspringCare = calculatePerOffspringAllocation(parent);
        return perOffspringCare > 0.2;
    }

    /**
     * Calculates the "parental stress" level based on offspring load.
     *
     * @param parent The parent entity
     * @return Stress level (0.0 to 1.0, where 1.0 is maximum stress)
     */
    public double calculateParentalStress(BreedingEntity parent) {
        int offspringCount = getOffspringCount(parent);
        double idealBroodSize = 2.0 / (config.getParentalInvestmentLevel() + 0.01);

        if (offspringCount <= idealBroodSize) {
            return 0.0;
        }

        double overload = offspringCount - idealBroodSize;
        return Math.min(1.0, overload / idealBroodSize);
    }

    /**
     * Cleans up data for parents with no offspring.
     */
    public void cleanup() {
        offspringMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        careLevelMap.keySet().removeIf(uuid -> !offspringMap.containsKey(uuid));
    }
}
