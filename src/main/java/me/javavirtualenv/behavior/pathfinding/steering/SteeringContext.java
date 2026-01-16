package me.javavirtualenv.behavior.pathfinding.steering;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Context object containing environmental information for steering calculations.
 * Uses builder pattern for flexible construction.
 */
public class SteeringContext {
    private final Vec3 targetPosition;
    private final Entity targetEntity;
    private final List<Entity> nearbyEntities;
    private final float maxSpeed;
    private final float maxForce;
    private final Path currentPath;

    private SteeringContext(Builder builder) {
        this.targetPosition = builder.targetPosition;
        this.targetEntity = builder.targetEntity;
        this.nearbyEntities = builder.nearbyEntities;
        this.maxSpeed = builder.maxSpeed;
        this.maxForce = builder.maxForce;
        this.currentPath = builder.currentPath;
    }

    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public List<Entity> getNearbyEntities() {
        return nearbyEntities;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getMaxForce() {
        return maxForce;
    }

    public Path getCurrentPath() {
        return currentPath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Vec3 targetPosition;
        private Entity targetEntity;
        private List<Entity> nearbyEntities = new ArrayList<>();
        private float maxSpeed = 1.0f;
        private float maxForce = 0.5f;
        private Path currentPath;

        public Builder targetPosition(Vec3 targetPosition) {
            this.targetPosition = targetPosition;
            return this;
        }

        public Builder targetEntity(Entity targetEntity) {
            this.targetEntity = targetEntity;
            return this;
        }

        public Builder nearbyEntities(List<Entity> nearbyEntities) {
            this.nearbyEntities = nearbyEntities != null ? nearbyEntities : new ArrayList<>();
            return this;
        }

        public Builder maxSpeed(float maxSpeed) {
            this.maxSpeed = maxSpeed;
            return this;
        }

        public Builder maxForce(float maxForce) {
            this.maxForce = maxForce;
            return this;
        }

        public Builder currentPath(Path currentPath) {
            this.currentPath = currentPath;
            return this;
        }

        public SteeringContext build() {
            return new SteeringContext(this);
        }
    }
}
