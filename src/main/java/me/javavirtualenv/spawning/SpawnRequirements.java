package me.javavirtualenv.spawning;

/**
 * Configuration record for spawn requirements.
 *
 * <p>Based on ecological research for biome-appropriate spawning:
 * <ul>
 *   <li>Water requirements for drinking animals</li>
 *   <li>Height restrictions for surface spawning</li>
 *   <li>Territorial spacing to prevent clustering</li>
 *   <li>Population limits based on carrying capacity</li>
 * </ul>
 *
 * @param minWaterDistance minimum distance to water (0 = no requirement)
 * @param maxWaterDistance maximum distance to water (-1 = unlimited)
 * @param minY minimum spawn height
 * @param maxY maximum spawn height
 * @param territoryRadius minimum distance from existing groups of same species
 * @param maxPopulationInArea carrying capacity for the check area
 * @param populationCheckRadius radius to check for existing population
 */
public record SpawnRequirements(
    int minWaterDistance,
    int maxWaterDistance,
    int minY,
    int maxY,
    int territoryRadius,
    int maxPopulationInArea,
    int populationCheckRadius
) {
    /**
     * Default requirements for land animals that need water nearby.
     */
    public static final SpawnRequirements LAND_ANIMAL_NEEDS_WATER = new SpawnRequirements(
        0,    // minWaterDistance
        32,   // maxWaterDistance - must be within 32 blocks of water
        60,   // minY - above sea level
        256,  // maxY
        48,   // territoryRadius
        20,   // maxPopulationInArea
        96    // populationCheckRadius
    );

    /**
     * Default requirements for land animals that don't need water.
     */
    public static final SpawnRequirements LAND_ANIMAL_NO_WATER = new SpawnRequirements(
        0,    // minWaterDistance
        -1,   // maxWaterDistance - no water requirement
        60,   // minY
        256,  // maxY
        48,   // territoryRadius
        20,   // maxPopulationInArea
        96    // populationCheckRadius
    );

    /**
     * Default requirements for predators (larger territory).
     */
    public static final SpawnRequirements PREDATOR = new SpawnRequirements(
        0,    // minWaterDistance
        -1,   // maxWaterDistance
        60,   // minY
        256,  // maxY
        128,  // territoryRadius - larger for predators
        8,    // maxPopulationInArea - fewer predators
        192   // populationCheckRadius
    );

    /**
     * Default requirements for aquatic animals.
     */
    public static final SpawnRequirements AQUATIC = new SpawnRequirements(
        0,    // minWaterDistance
        0,    // maxWaterDistance - must be in water
        0,    // minY - can spawn at any depth
        256,  // maxY
        32,   // territoryRadius
        30,   // maxPopulationInArea - fish can be more dense
        64    // populationCheckRadius
    );

    /**
     * Checks if water is required for spawning.
     *
     * @return true if water must be nearby
     */
    public boolean requiresWater() {
        return maxWaterDistance >= 0;
    }

    /**
     * Creates a builder for custom spawn requirements.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating custom SpawnRequirements.
     */
    public static class Builder {
        private int minWaterDistance = 0;
        private int maxWaterDistance = -1;
        private int minY = 60;
        private int maxY = 256;
        private int territoryRadius = 48;
        private int maxPopulationInArea = 20;
        private int populationCheckRadius = 96;

        public Builder waterDistance(int min, int max) {
            this.minWaterDistance = min;
            this.maxWaterDistance = max;
            return this;
        }

        public Builder requiresWater(int maxDistance) {
            this.minWaterDistance = 0;
            this.maxWaterDistance = maxDistance;
            return this;
        }

        public Builder noWaterRequired() {
            this.maxWaterDistance = -1;
            return this;
        }

        public Builder heightRange(int min, int max) {
            this.minY = min;
            this.maxY = max;
            return this;
        }

        public Builder territoryRadius(int radius) {
            this.territoryRadius = radius;
            return this;
        }

        public Builder populationLimits(int maxPopulation, int checkRadius) {
            this.maxPopulationInArea = maxPopulation;
            this.populationCheckRadius = checkRadius;
            return this;
        }

        public SpawnRequirements build() {
            return new SpawnRequirements(
                minWaterDistance,
                maxWaterDistance,
                minY,
                maxY,
                territoryRadius,
                maxPopulationInArea,
                populationCheckRadius
            );
        }
    }
}
