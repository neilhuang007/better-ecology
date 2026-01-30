package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Utility class for counting animal populations in an area.
 *
 * <p>Used for density-dependent behaviors like:
 * <ul>
 *   <li>Breeding rate modulation based on carrying capacity</li>
 *   <li>Spawn condition checking for territorial spacing</li>
 *   <li>Emergency breeding triggers when population is low</li>
 * </ul>
 *
 * <p>Based on ecological principles from the logistic growth model:
 * dN/dt = rN(1 - N/K) where K is carrying capacity.
 */
public final class PopulationCounter {

    private PopulationCounter() {
        // Utility class
    }

    /**
     * Counts animals of a specific type within a radius.
     *
     * @param level the world level
     * @param center the center position
     * @param type the animal class to count
     * @param radius the search radius in blocks
     * @return the number of animals found
     */
    public static int countNearby(Level level, BlockPos center,
                                   Class<? extends Animal> type, int radius) {
        AABB searchBox = new AABB(center).inflate(radius);
        List<? extends Animal> animals = level.getEntitiesOfClass(type, searchBox);
        return animals.size();
    }

    /**
     * Counts all animals within a radius.
     *
     * @param level the world level
     * @param center the center position
     * @param radius the search radius in blocks
     * @return the number of animals found
     */
    public static int countAllAnimalsNearby(Level level, BlockPos center, int radius) {
        AABB searchBox = new AABB(center).inflate(radius);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, searchBox);
        return animals.size();
    }

    /**
     * Calculates the density ratio (N/K) for population-aware behaviors.
     *
     * @param level the world level
     * @param center the center position
     * @param type the animal class to count
     * @param radius the search radius in blocks
     * @param carryingCapacity the maximum sustainable population (K)
     * @return density ratio between 0.0 and 1.0+ (can exceed 1.0 if overpopulated)
     */
    public static float getDensityRatio(Level level, BlockPos center,
                                        Class<? extends Animal> type,
                                        int radius, int carryingCapacity) {
        if (carryingCapacity <= 0) {
            return 1.0f;
        }
        int population = countNearby(level, center, type, radius);
        return (float) population / carryingCapacity;
    }

    /**
     * Checks if the population is at or above carrying capacity.
     *
     * @param level the world level
     * @param center the center position
     * @param type the animal class to count
     * @param radius the search radius in blocks
     * @param carryingCapacity the maximum sustainable population
     * @return true if population >= carrying capacity
     */
    public static boolean isAtCarryingCapacity(Level level, BlockPos center,
                                                Class<? extends Animal> type,
                                                int radius, int carryingCapacity) {
        int population = countNearby(level, center, type, radius);
        return population >= carryingCapacity;
    }

    /**
     * Checks if the population is critically low (emergency breeding threshold).
     *
     * @param level the world level
     * @param center the center position
     * @param type the animal class to count
     * @param radius the search radius in blocks
     * @param emergencyThreshold the population below which emergency breeding triggers
     * @return true if population <= emergency threshold
     */
    public static boolean isCriticallyLow(Level level, BlockPos center,
                                          Class<? extends Animal> type,
                                          int radius, int emergencyThreshold) {
        int population = countNearby(level, center, type, radius);
        return population <= emergencyThreshold;
    }

    /**
     * Checks if there is space for a new group (territorial spacing).
     *
     * @param level the world level
     * @param center the proposed spawn center
     * @param type the animal class to check
     * @param territoryRadius the minimum distance from existing groups
     * @return true if no animals of this type exist within territory radius
     */
    public static boolean hasSpaceForGroup(Level level, BlockPos center,
                                           Class<? extends Animal> type,
                                           int territoryRadius) {
        int existing = countNearby(level, center, type, territoryRadius);
        return existing == 0;
    }
}
