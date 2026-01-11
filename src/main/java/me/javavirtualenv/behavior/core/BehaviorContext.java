package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class BehaviorContext {
    private final Mob entity;
    private final Level level;
    private Vec3d position;
    private Vec3d velocity;
    private Double maxSpeed;
    private Double maxForce;
    private List<Entity> neighbors;

    public BehaviorContext(Mob entity) {
        this.entity = entity;
        this.level = entity.level();
        this.position = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        this.velocity = new Vec3d(
            entity.getDeltaMovement().x,
            entity.getDeltaMovement().y,
            entity.getDeltaMovement().z
        );
        this.neighbors = new ArrayList<>();
    }

    /**
     * Constructor for testing - allows creating a context with explicit values
     * without requiring a Minecraft entity.
     */
    public BehaviorContext(Mob entity, Vec3d position, Vec3d velocity, double maxSpeed, double maxForce) {
        this.entity = entity;
        this.level = entity != null ? entity.level() : null;
        this.position = position != null ? position : new Vec3d();
        this.velocity = velocity != null ? velocity : new Vec3d();
        this.overrideMaxSpeed = maxSpeed;
        this.overrideMaxForce = maxForce;
        this.neighbors = new ArrayList<>();
    }

    /**
     * Constructor for testing with null entity.
     */
    public BehaviorContext(Vec3d position, Vec3d velocity, double maxSpeed, double maxForce) {
        this.entity = null;
        this.level = null;
        this.position = position != null ? position : new Vec3d();
        this.velocity = velocity != null ? velocity : new Vec3d();
        this.overrideMaxSpeed = maxSpeed;
        this.overrideMaxForce = maxForce;
        this.neighbors = new ArrayList<>();
    }

    public Mob getEntity() {
        return entity;
    }

    public Mob getMob() {
        return entity;
    }

    public Level getLevel() {
        return level;
    }

    public Vec3d getPosition() {
        return position;
    }

    public Vec3d getVelocity() {
        return velocity;
    }

    public BlockPos getBlockPos() {
        if (entity != null) {
            return entity.blockPosition();
        }
        return new BlockPos((int) position.x, (int) position.y, (int) position.z);
    }

    public double getSpeed() {
        return velocity.magnitude();
    }

    public long getDayTime() {
        return level.getDayTime() % 24000;
    }

    public List<Entity> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<Entity> neighbors) {
        this.neighbors = neighbors != null ? neighbors : new ArrayList<>();
    }

    // Backwards compatibility methods
    public Entity getSelf() {
        return entity;
    }

    public Level getWorld() {
        return level;
    }

    public List<Entity> getNearbyEntities() {
        return neighbors;
    }

    /**
     * Builder for creating BehaviorContext instances with custom parameters.
     */
    public static class Builder {
        private Mob entity;
        private Level level;
        private Vec3d position;
        private Vec3d velocity;
        private List<Entity> nearbyEntities;
        private Double maxSpeed;
        private Double maxForce;

        public Builder self(Mob entity) {
            this.entity = entity;
            return this;
        }

        public Builder world(Level level) {
            this.level = level;
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

        public BehaviorContext build() {
            if (entity == null) {
                throw new IllegalStateException("Entity must be set");
            }
            if (level == null) {
                level = entity.level();
            }

            BehaviorContext context = new BehaviorContext(entity);
            if (position != null) {
                context.position = position;
            }
            if (velocity != null) {
                context.velocity = velocity;
            }
            if (nearbyEntities != null) {
                context.neighbors = nearbyEntities;
            }
            if (maxSpeed != null) {
                context.overrideMaxSpeed = maxSpeed;
            }
            if (maxForce != null) {
                context.overrideMaxForce = maxForce;
            }
            return context;
        }
    }

    // Fields for overridden values from builder
    private Double overrideMaxSpeed;
    private Double overrideMaxForce;

    public double getMaxSpeed() {
        if (overrideMaxSpeed != null) {
            return overrideMaxSpeed;
        }
        return entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
    }

    public double getMaxForce() {
        if (overrideMaxForce != null) {
            return overrideMaxForce;
        }
        return 0.5; // Default max force for steering
    }
}
