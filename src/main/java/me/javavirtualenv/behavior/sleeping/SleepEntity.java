package me.javavirtualenv.behavior.sleeping;

/**
 * Interface representing an entity for sleeping behavior calculations.
 * This abstraction allows testing without Minecraft entity dependencies.
 */
public interface SleepEntity {

    /**
     * Gets the X position of the entity.
     */
    double getX();

    /**
     * Gets the Y position of the entity.
     */
    double getY();

    /**
     * Gets the Z position of the entity.
     */
    double getZ();

    /**
     * Gets the block position X coordinate.
     */
    int getBlockX();

    /**
     * Gets the block position Y coordinate.
     */
    int getBlockY();

    /**
     * Gets the block position Z coordinate.
     */
    int getBlockZ();

    /**
     * Gets the current health of the entity.
     */
    float getHealth();

    /**
     * Gets the maximum health of the entity.
     */
    float getMaxHealth();

    /**
     * Returns true if the entity is a baby.
     */
    boolean isBaby();

    /**
     * Returns true if the entity is alive.
     */
    boolean isAlive();

    /**
     * Gets the entity type identifier (e.g., "entity.minecraft.cow").
     */
    String getEntityType();

    /**
     * Calculates squared distance to a position.
     */
    default double distanceToSqr(double x, double y, double z) {
        double dx = this.getX() - x;
        double dy = this.getY() - y;
        double dz = this.getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Gets health percentage (0.0 to 1.0).
     */
    default double getHealthPercent() {
        float maxHealth = getMaxHealth();
        return maxHealth > 0 ? getHealth() / maxHealth : 1.0;
    }
}
