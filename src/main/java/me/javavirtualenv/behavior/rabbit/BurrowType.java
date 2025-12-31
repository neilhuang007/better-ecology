package me.javavirtualenv.behavior.rabbit;

/**
 * Types of rabbit burrows based on biome and purpose.
 */
public enum BurrowType {
    // Basic burrow in grassland/plains
    GRASSLAND(3, 0.8, "grassland"),

    // Forest burrow with root protection
    FOREST(2, 0.7, "forest"),

    // Desert burrow - deep and cool
    DESERT(4, 0.6, "desert"),

    // Snow burrow - insulated
    SNOW(3, 0.9, "snow"),

    // Warren - complex multi-entrance burrow
    WARREN(6, 0.5, "warren"),

    // Temporary hiding burrow
    HIDING_HOLE(1, 0.9, "hiding_hole");

    private final int capacity;
    private final double protectionFactor;
    private final String id;

    BurrowType(int capacity, double protectionFactor, String id) {
        this.capacity = capacity;
        this.protectionFactor = protectionFactor;
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getProtectionFactor() {
        return protectionFactor;
    }

    public String getId() {
        return id;
    }

    /**
     * Gets the appropriate burrow type for a given biome.
     */
    public static BurrowType forBiome(String biomeId) {
        String biomeLower = biomeId.toLowerCase();

        if (biomeLower.contains("desert") || biomeLower.contains("savanna")) {
            return DESERT;
        } else if (biomeLower.contains("snow") || biomeLower.contains("ice") ||
                   biomeLower.contains("frozen")) {
            return SNOW;
        } else if (biomeLower.contains("forest") || biomeLower.contains("taiga") ||
                   biomeLower.contains("dark")) {
            return FOREST;
        } else if (biomeLower.contains("plains") || biomeLower.contains("meadow") ||
                   biomeLower.contains("field")) {
            return GRASSLAND;
        }

        // Default to grassland
        return GRASSLAND;
    }
}
