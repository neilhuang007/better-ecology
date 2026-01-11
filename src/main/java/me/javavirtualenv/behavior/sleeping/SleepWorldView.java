package me.javavirtualenv.behavior.sleeping;

import java.util.List;

/**
 * Interface for world access in sleeping behavior calculations.
 * This abstraction allows testing without Minecraft Level dependencies.
 */
public interface SleepWorldView {

    /**
     * Gets the current day time (0-24000).
     */
    long getDayTime();

    /**
     * Gets the current game time.
     */
    long getGameTime();

    /**
     * Gets block information at the given position.
     */
    SleepBlockInfo getBlockInfo(int x, int y, int z);

    /**
     * Gets all nearby entities within the given radius.
     */
    List<SleepEntity> getNearbyEntities(double x, double y, double z, double radius);

    /**
     * Gets entities of a specific type nearby.
     */
    List<SleepEntity> getNearbyEntitiesOfType(double x, double y, double z, double radius, String entityType);

    /**
     * Gets hostile entities nearby.
     */
    List<SleepEntity> getNearbyHostiles(double x, double y, double z, double radius);
}
