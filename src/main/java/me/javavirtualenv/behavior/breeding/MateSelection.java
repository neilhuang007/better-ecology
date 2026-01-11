package me.javavirtualenv.behavior.breeding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Handles mate selection based on quality assessment.
 * Implements scientifically-based mate choice criteria including health, age,
 * display traits, genetic compatibility, honest signals, and territory quality.
 *
 * This redesigned version works with the BreedingEntity interface,
 * making it testable without mocking Minecraft classes.
 *
 * Research sources:
 * - Andersson, M. (1994). Sexual Selection
 * - Jennions, M.D., & Petrie, M. (2000). Why do females mate multiply?
 * - Johnstone, R.A. (1995). Sexual selection, honest advertisement and the handicap principle
 */
public class MateSelection {

    private final BreedingConfig config;
    private TerritorialDefense territorialDefense;

    public MateSelection(BreedingConfig config) {
        this.config = config;
    }

    /**
     * Sets the territorial defense system for territory quality assessment.
     *
     * @param territorialDefense The territorial defense instance
     */
    public void setTerritorialDefense(TerritorialDefense territorialDefense) {
        this.territorialDefense = territorialDefense;
    }

    /**
     * Selects the best mate from a list of potential partners.
     * Uses weighted scoring based on health, age, and display traits.
     *
     * @param chooser The entity seeking a mate
     * @param potentialMates List of potential mates to evaluate
     * @return The best mate, or null if no suitable mates found
     */
    public BreedingEntity selectBestMate(BreedingEntity chooser, List<BreedingEntity> potentialMates) {
        if (potentialMates == null || potentialMates.isEmpty()) {
            return null;
        }

        List<BreedingEntity> suitableMates = filterSuitableMates(chooser, potentialMates);

        if (suitableMates.isEmpty()) {
            return null;
        }

        return suitableMates.stream()
            .max(Comparator.comparingDouble(mate -> assessMateQuality(chooser, mate)))
            .orElse(null);
    }

    /**
     * Filters potential mates to only those suitable for breeding.
     * Removes mates that are dead, already breeding, too young, or unhealthy.
     *
     * @param chooser The entity seeking a mate
     * @param potentialMates List of potential mates to filter
     * @return Filtered list of suitable mates
     */
    public List<BreedingEntity> filterSuitableMates(BreedingEntity chooser, List<BreedingEntity> potentialMates) {
        List<BreedingEntity> suitable = new ArrayList<>();

        if (potentialMates == null) {
            return suitable;
        }

        for (BreedingEntity mate : potentialMates) {
            if (isMateSuitable(chooser, mate)) {
                suitable.add(mate);
            }
        }

        return suitable;
    }

    /**
     * Checks if a potential mate meets all breeding criteria.
     */
    private boolean isMateSuitable(BreedingEntity chooser, BreedingEntity potential) {
        if (potential == null || !potential.isAlive()) {
            return false;
        }

        if (potential.isBaby()) {
            return false;
        }

        if (potential.isInLove()) {
            return false;
        }

        double healthRatio = potential.getHealth() / potential.getMaxHealth();
        if (healthRatio < config.getMinHealthForBreeding()) {
            return false;
        }

        if (potential.getAge() < config.getMinAgeForBreeding()) {
            return false;
        }

        return !isSameSex(chooser, potential);
    }

    /**
     * Assesses the quality of a potential mate as a weighted score.
     * Considers health, age/experience, display traits, honest signals,
     * genetic compatibility, and territory quality (for males).
     * Based on research: Andersson (1994), Johnstone (1995) on honest signals and handicap principle.
     */
    private double assessMateQuality(BreedingEntity chooser, BreedingEntity potential) {
        double quality = 0.0;

        double healthRatio = potential.getHealth() / potential.getMaxHealth();
        quality += healthRatio;

        double ageScore = Math.min(1.0, potential.getAge() / 200.0);
        quality += ageScore * config.getAgePreference();

        double displayScore = getDisplayTraitScore(potential);
        quality += displayScore * config.getDisplayTraitWeight();

        double honestSignalScore = getHonestSignalScore(potential);
        quality += honestSignalScore * 0.3;

        double geneticCompatibility = calculateGeneticCompatibility(chooser, potential);
        quality += geneticCompatibility * 0.2;

        if (potential.isMale() && territorialDefense != null) {
            double territoryBoost = territorialDefense.calculateBreedingSuccessBoost(potential);
            quality += territoryBoost;
        }

        return quality;
    }

    /**
     * Retrieves the display trait score for an entity.
     * In full implementation, this would be stored in component data.
     * Returns a value based on entity size as a proxy for display quality.
     */
    private double getDisplayTraitScore(BreedingEntity entity) {
        double sizeScore = entity.getSize() * 0.1;
        return Math.min(1.0, Math.max(0.0, sizeScore));
    }

    /**
     * Calculates honest signal score based on phenotypic quality.
     * Research shows antler size, plumage, and other ornaments provide honest signals
     * of male quality (testicle size, sperm quality, overall health).
     * Reference: "Antler Size Provides an Honest Signal of Male Phenotypic Quality"
     */
    private double getHonestSignalScore(BreedingEntity entity) {
        double healthRatio = entity.getHealth() / entity.getMaxHealth();
        double sizeIndicator = entity.getSize() * 0.15;

        double honestSignal = (healthRatio * 0.6) + (sizeIndicator * 0.4);
        return Math.min(1.0, Math.max(0.0, honestSignal));
    }

    /**
     * Calculates genetic compatibility to prevent inbreeding.
     * Returns lower scores for closely related individuals.
     * In a full implementation, this would use actual genetic data.
     */
    private double calculateGeneticCompatibility(BreedingEntity chooser, BreedingEntity potential) {
        UUID chooserId = chooser.getUuid();
        UUID potentialId = potential.getUuid();

        if (chooserId.equals(potentialId)) {
            return 0.0;
        }

        int chooserHash = Math.abs(chooserId.hashCode());
        int potentialHash = Math.abs(potentialId.hashCode());
        int geneticDistance = Math.abs(chooserHash - potentialHash) % 100;
        double compatibility = geneticDistance / 100.0;

        return Math.max(0.3, Math.min(1.0, compatibility));
    }

    /**
     * Determines if two entities are the same sex.
     */
    private boolean isSameSex(BreedingEntity chooser, BreedingEntity potential) {
        return chooser.isMale() == potential.isMale();
    }
}
