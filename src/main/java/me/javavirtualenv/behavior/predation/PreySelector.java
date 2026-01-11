package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.conservation.PreyPopulationManager;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Selects optimal prey from available targets based on multiple factors.
 * Implements optimal foraging theory - predators select prey that maximizes
 * energy gain while minimizing risk and energy expenditure.
 * <p>
 * Based on research by:
 * - Emlen (1966) - Optimal choice in foraging behavior
 * - MacArthur & Pianka (1966) - On optimal use of a patchy environment
 * - Curio (1976) - The ethology of predation
 */
public class PreySelector {

    private final double maxPreyDistance;
    private final double sizePreference;
    private final double injuryBonus;
    private final double babyBonus;

    public PreySelector(double maxPreyDistance, double sizePreference,
                       double injuryBonus, double babyBonus) {
        this.maxPreyDistance = maxPreyDistance;
        this.sizePreference = sizePreference;
        this.injuryBonus = injuryBonus;
        this.babyBonus = babyBonus;
    }

    public PreySelector() {
        this(32.0, 1.0, 1.5, 2.0);
    }

    /**
     * Selects the best prey from available entities.
     * Uses SpatialIndex for efficient O(1) + O(k) queries instead of O(n) iteration.
     * Implements population checking to prevent prey extinction.
     *
     * @param predator The predator doing the hunting
     * @return The selected prey, or null if no valid prey found
     */
    public Entity selectPrey(Mob predator) {
        // Use SpatialIndex for efficient entity queries
        List<Mob> nearbyMobs = SpatialIndex.getNearbyMobs(predator, (int) maxPreyDistance);

        List<PreyCandidate> candidates = new ArrayList<>();

        for (Mob mob : nearbyMobs) {
            if (!(mob instanceof LivingEntity entity)) {
                continue;
            }

            if (entity.equals(predator)) {
                continue;
            }

            if (!isValidPrey(predator, entity)) {
                continue;
            }

            // Check prey population health - don't hunt scarce populations
            if (!isPreyPopulationHealthy(predator, entity)) {
                continue;
            }

            double score = scorePrey(predator, entity);
            candidates.add(new PreyCandidate(entity, score));
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Sort by score (lower is better - it's a "cost" score)
        candidates.sort(Comparator.comparingDouble(c -> c.score));

        return candidates.get(0).entity;
    }

    /**
     * Scores a potential prey item based on optimal foraging theory.
     * Based on Emlen (1966) and MacArthur & Pianka (1966): maximize E/h (energy gain per handling time).
     * Lower score = better prey (higher energy/time ratio).
     * Includes prey switching logic - alternative prey scored higher when primary is scarce.
     */
    private double scorePrey(Mob predator, LivingEntity prey) {
        // Calculate energy gain from prey
        double energyGain = calculateEnergyGain(prey);

        // Calculate handling time (time to catch and consume)
        double handlingTime = calculateHandlingTime(predator, prey);

        // Calculate pursuit time (time to reach prey)
        double pursuitTime = calculatePursuitTime(predator, prey);

        // Total time = handling + pursuit
        double totalTime = handlingTime + pursuitTime;

        // Optimal foraging: maximize E/t (energy per time)
        // Score = 1 / (E/t) = t/E, so lower is better
        double score;
        if (energyGain > 0.001) {
            score = totalTime / energyGain;
        } else {
            score = Double.MAX_VALUE;
        }

        // Apply population-based adjustment (prey switching)
        // When primary prey is scarce, predators should switch to alternative prey
        double populationRatio = getPreyPopulationRatio(predator, prey.getClass());
        if (populationRatio < 0.4) {
            // Prey is scarce - discourage hunting to prevent extinction
            score *= (2.0 - populationRatio);
        }

        return score;
    }

    /**
     * Calculates the energy gain from a prey item.
     * Based on prey size, nutritional value, and accessibility.
     */
    private double calculateEnergyGain(LivingEntity prey) {
        double baseEnergy = 0.0;

        // Base energy from prey size (larger prey = more energy)
        double preySize = prey.getBbWidth() * prey.getBbHeight() * prey.getType().getWidth();
        baseEnergy = preySize * 100.0;

        // Adjust for prey type (some animals more nutritious)
        String typeName = prey.getType().toString().toLowerCase();
        if (typeName.contains("cow") || typeName.contains("mooshroom")) {
            baseEnergy *= 1.5; // High energy
        } else if (typeName.contains("sheep") || typeName.contains("pig")) {
            baseEnergy *= 1.2;
        } else if (typeName.contains("chicken") || typeName.contains("rabbit")) {
            baseEnergy *= 0.7; // Lower energy
        }

        // Adjust for prey condition
        if (prey instanceof Animal animal) {
            double healthPercent = animal.getHealth() / animal.getMaxHealth();

            // Injured prey: easier to catch but less meat
            if (healthPercent < 0.5) {
                baseEnergy *= 0.7;
            }

            // Baby prey: less energy but much easier to catch
            if (animal.isBaby()) {
                baseEnergy *= 0.4;
            }
        }

        return baseEnergy;
    }

    /**
     * Calculates the handling time for a prey item.
     * Based on prey size, speed, and group protection.
     */
    private double calculateHandlingTime(Mob predator, LivingEntity prey) {
        double baseHandlingTime = 50.0; // Base ticks to handle prey

        // Larger prey takes longer to handle
        double preySize = prey.getBbWidth() * prey.getBbHeight() * prey.getType().getWidth();
        baseHandlingTime += preySize * 20.0;

        // Faster prey are harder to catch (longer handling time)
        double preySpeed = prey.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double predatorSpeed = predator.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);

        if (preySpeed > predatorSpeed) {
            baseHandlingTime *= (1.0 + (preySpeed - predatorSpeed) * 2.0);
        }

        // Group protection increases handling time (confusion effect, defense)
        int nearbyConspecifics = countNearbyConspecifics(prey);
        if (nearbyConspecifics > 0) {
            // Each group member adds difficulty
            baseHandlingTime *= (1.0 + nearbyConspecifics * 0.15);
        }

        // Injured prey easier to handle
        if (prey instanceof Animal animal) {
            double healthPercent = animal.getHealth() / animal.getMaxHealth();
            if (healthPercent < 0.5) {
                baseHandlingTime *= 0.6;
            }
        }

        // Baby prey much easier to handle
        if (prey instanceof Animal animal && animal.isBaby()) {
            baseHandlingTime *= 0.3;
        }

        return baseHandlingTime;
    }

    /**
     * Calculates the pursuit time to reach prey.
     * Based on distance and relative speeds.
     */
    private double calculatePursuitTime(Mob predator, LivingEntity prey) {
        double distance = predator.position().distanceTo(prey.position());

        // Get speeds
        double predatorSpeed = predator.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double preySpeed = prey.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);

        // If predator is faster, use closing speed
        if (predatorSpeed > preySpeed) {
            double closingSpeed = predatorSpeed - preySpeed;
            return distance / closingSpeed;
        } else {
            // Prey is faster - pursuit will be long or unsuccessful
            return distance * 2.0; // Penalize faster prey
        }
    }

    /**
     * Determines if an entity is valid prey for this predator.
     */
    private boolean isValidPrey(Mob predator, LivingEntity entity) {
        // Must be alive
        if (!entity.isAlive()) {
            return false;
        }

        // Don't hunt players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        // Prey should be appropriately sized
        double predatorSize = predator.getBbHeight();
        double preySize = entity.getBbHeight();

        // Too big to hunt
        if (preySize > predatorSize * 1.5) {
            return false;
        }

        // Too small to be worth it
        if (preySize < predatorSize * 0.1) {
            return false;
        }

        return true;
    }

    /**
     * Counts nearby entities of the same species as the prey.
     * Uses SpatialIndex for efficient O(1) + O(k) queries.
     * Used to assess group protection benefits.
     */
    private int countNearbyConspecifics(LivingEntity prey) {
        double detectionRange = 16.0;

        if (!(prey instanceof Mob preyMob)) {
            return 0;
        }

        // Use SpatialIndex for efficient same-type queries
        List<Mob> nearby = SpatialIndex.getNearbySameType(preyMob, (int) detectionRange);

        int count = 0;
        for (Mob entity : nearby) {
            if (!entity.equals(prey) && entity.getType().equals(prey.getType())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Gets all valid prey in range, sorted by preference.
     * Uses SpatialIndex for efficient O(1) + O(k) queries.
     */
    public List<LivingEntity> getRankedPrey(Mob predator) {
        // Use SpatialIndex for efficient entity queries
        List<Mob> nearbyMobs = SpatialIndex.getNearbyMobs(predator, (int) maxPreyDistance);

        List<PreyCandidate> candidates = new ArrayList<>();

        for (Mob mob : nearbyMobs) {
            if (!(mob instanceof LivingEntity entity)) {
                continue;
            }

            if (entity.equals(predator)) {
                continue;
            }

            if (!isValidPrey(predator, entity)) {
                continue;
            }

            double score = scorePrey(predator, entity);
            candidates.add(new PreyCandidate(entity, score));
        }

        candidates.sort(Comparator.comparingDouble(c -> c.score));

        List<LivingEntity> result = new ArrayList<>();
        for (PreyCandidate candidate : candidates) {
            result.add((LivingEntity) candidate.entity);
        }

        return result;
    }

    /**
     * Checks if specific prey is in range.
     */
    public boolean isPreyInRange(Mob predator, Entity prey) {
        if (!prey.isAlive()) {
            return false;
        }

        double distance = predator.position().distanceTo(prey.position());
        if (distance > maxPreyDistance) {
            return false;
        }

        return isValidPrey(predator, (LivingEntity) prey);
    }

    /**
     * Internal class for tracking prey candidates.
     */
    private static class PreyCandidate {
        final Entity entity;
        final double score;

        PreyCandidate(Entity entity, double score) {
            this.entity = entity;
            this.score = score;
        }
    }

    /**
     * Gets the local prey population count for a specific prey type.
     * Uses PreyPopulationManager for efficient querying.
     *
     * @param predator The predator entity
     * @param preyType The class of prey to count
     * @return Count of prey in the local area
     */
    public int getLocalPreyPopulation(Mob predator, Class<?> preyType) {
        int searchRadius = (int) maxPreyDistance;
        return PreyPopulationManager.getPreyCount(predator, preyType, searchRadius);
    }

    /**
     * Checks if the prey population is healthy enough to support hunting.
     * Implements Allee threshold - prevents hunting when population is too low.
     *
     * @param predator The predator entity
     * @param prey The potential prey entity
     * @return true if prey population is healthy enough to hunt
     */
    public boolean isPreyPopulationHealthy(Mob predator, LivingEntity prey) {
        Class<?> preyType = prey.getClass();
        int searchRadius = (int) maxPreyDistance;

        // Use PreyPopulationManager to check population health
        return PreyPopulationManager.isPreyPopulationHealthy(predator, preyType, searchRadius);
    }

    /**
     * Gets the population ratio for a prey type (current/expected).
     * Used for prey switching decisions.
     *
     * @param predator The predator entity
     * @param preyType The class of prey to check
     * @return Population ratio (0.0 to >1.0)
     */
    public double getPreyPopulationRatio(Mob predator, Class<?> preyType) {
        int searchRadius = (int) maxPreyDistance;
        return PreyPopulationManager.getPopulationRatio(predator, preyType, searchRadius);
    }

    public double getMaxPreyDistance() {
        return maxPreyDistance;
    }

    public double getSizePreference() {
        return sizePreference;
    }

    public double getInjuryBonus() {
        return injuryBonus;
    }

    public double getBabyBonus() {
        return babyBonus;
    }
}
