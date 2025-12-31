package me.javavirtualenv.behavior.steering;

import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Context object containing all information needed for steering behavior calculations.
 * Passed to each behavior's calculate method to provide entity state and environmental data.
 */
public class BehaviorContext {
    private final Entity self;
    private final Vec3d position;
    private final Vec3d velocity;
    private final double maxSpeed;
    private final double maxForce;
    private final List<Entity> nearbyEntities;
    private final Level world;

    private BehaviorContext(Builder builder) {
        this.self = builder.self;
        this.position = builder.position;
        this.velocity = builder.velocity;
        this.maxSpeed = builder.maxSpeed;
        this.maxForce = builder.maxForce;
        this.nearbyEntities = builder.nearbyEntities;
        this.world = builder.world;
    }

    public Entity getSelf() {
        return self;
    }

    public Vec3d getPosition() {
        return position;
    }

    public Vec3d getVelocity() {
        return velocity;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getMaxForce() {
        return maxForce;
    }

    public List<Entity> getNearbyEntities() {
        return nearbyEntities;
    }

    public Level getWorld() {
        return world;
    }

    /**
     * Query nearby entities by type.
     */
    public <T extends Entity> List<T> getEntitiesByType(Class<T> entityClass) {
        return nearbyEntities.stream()
            .filter(entityClass::isInstance)
            .map(entityClass::cast)
            .collect(Collectors.toList());
    }

    /**
     * Query nearby entities within a specific distance.
     */
    public List<Entity> getEntitiesWithinDistance(double distance) {
        double distanceSquared = distance * distance;
        return nearbyEntities.stream()
            .filter(entity -> position.distanceTo(Vec3d.fromMinecraftVec3(entity.position())) <= distance)
            .collect(Collectors.toList());
    }

    /**
     * Query nearby entities matching a predicate.
     */
    public List<Entity> getEntitiesMatching(Predicate<Entity> predicate) {
        return nearbyEntities.stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * Get the closest entity from the nearby entities list.
     */
    public Entity getClosestEntity() {
        if (nearbyEntities.isEmpty()) {
            return null;
        }

        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : nearbyEntities) {
            double distance = position.distanceTo(Vec3d.fromMinecraftVec3(entity.position()));
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }

        return closest;
    }

    /**
     * Get the closest entity of a specific type.
     */
    public <T extends Entity> T getClosestEntityByType(Class<T> entityClass) {
        List<T> typed = getEntitiesByType(entityClass);
        if (typed.isEmpty()) {
            return null;
        }

        T closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (T entity : typed) {
            double distance = position.distanceTo(Vec3d.fromMinecraftVec3(entity.position()));
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }

        return closest;
    }

    /**
     * Builder for BehaviorContext to allow flexible construction.
     */
    public static class Builder {
        private Entity self;
        private Vec3d position;
        private Vec3d velocity;
        private double maxSpeed = 1.0;
        private double maxForce = 0.5;
        private List<Entity> nearbyEntities = new ArrayList<>();
        private Level world;

        public Builder self(Entity self) {
            this.self = self;
            return this;
        }

        public Builder position(Vec3d position) {
            this.position = position;
            return this;
        }

        public Builder velocity(Vec3d velocity) {
            this.velocity = velocity;
            return this;
        }

        public Builder maxSpeed(double maxSpeed) {
            this.maxSpeed = maxSpeed;
            return this;
        }

        public Builder maxForce(double maxForce) {
            this.maxForce = maxForce;
            return this;
        }

        public Builder nearbyEntities(List<Entity> nearbyEntities) {
            this.nearbyEntities = nearbyEntities;
            return this;
        }

        public Builder world(Level world) {
            this.world = world;
            return this;
        }

        public BehaviorContext build() {
            if (self == null) {
                throw new IllegalStateException("Entity self cannot be null");
            }
            if (position == null) {
                position = Vec3d.fromMinecraftVec3(self.position());
            }
            if (velocity == null) {
                velocity = new Vec3d();
            }
            if (world == null) {
                world = self.level();
            }
            return new BehaviorContext(this);
        }
    }
}
