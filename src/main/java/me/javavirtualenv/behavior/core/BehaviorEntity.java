package me.javavirtualenv.behavior.core;

import java.util.List;

/**
 * Minimal interface defining what behavior algorithms need from an entity.
 * This allows behavior tests to use simple POJO implementations while
 * the actual mod can wrap Minecraft entities.
 */
public interface BehaviorEntity {
    /**
     * Gets the current position of this entity.
     * @return Position vector
     */
    Vec3d getPosition();

    /**
     * Gets the current velocity of this entity.
     * @return Velocity vector
     */
    Vec3d getVelocity();

    /**
     * Gets nearby entities within the given radius.
     * @param radius Search radius
     * @return List of nearby entities (may be empty)
     */
    List<BehaviorEntity> getNearbyEntities(double radius);

    /**
     * Gets a unique identifier for this entity.
     * Used to filter self from neighbor lists.
     * @return Unique ID
     */
    Object getId();

    /**
     * Gets the world/level this entity is in.
     * Returns Object to keep the interface generic - Minecraft implementations
     * will return net.minecraft.world.level.Level.
     * @return The world/level object
     */
    default Object getLevel() {
        return null;
    }

    /**
     * Gets the block position of this entity.
     * Returns Object to keep the interface generic - implementations can return:
     * - net.minecraft.core.BlockPos for Minecraft entities
     * - me.javavirtualenv.behavior.core.BlockPos for test entities
     * @return The block position object, or null
     */
    default Object getBlockPos() {
        return null;
    }

    /**
     * Gets the underlying entity object.
     * Returns Object to keep the interface generic - Minecraft implementations
     * will return net.minecraft.world.entity.Entity.
     * @return The underlying entity object
     */
    default Object getUnderlyingEntity() {
        return null;
    }
}
