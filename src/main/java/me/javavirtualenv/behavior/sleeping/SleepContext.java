package me.javavirtualenv.behavior.sleeping;

import me.javavirtualenv.behavior.core.Vec3d;

import java.util.List;

/**
 * Testable context for sleeping behavior calculations.
 * This class does not depend on any Minecraft classes.
 */
public class SleepContext {

    private final SleepEntity entity;
    private final SleepWorldView world;
    private final Vec3d position;
    private final long dayTime;

    private SleepContext(Builder builder) {
        this.entity = builder.entity;
        this.world = builder.world;
        this.position = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        this.dayTime = builder.dayTime != null ? builder.dayTime : world.getDayTime();
    }

    public SleepEntity getEntity() {
        return entity;
    }

    public SleepWorldView getWorld() {
        return world;
    }

    public Vec3d getPosition() {
        return position;
    }

    public SleepBlockPos getBlockPos() {
        return new SleepBlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
    }

    public long getDayTime() {
        return dayTime % 24000;
    }

    public List<SleepEntity> getNearbyEntities(double radius) {
        return world.getNearbyEntities(entity.getX(), entity.getY(), entity.getZ(), radius);
    }

    public List<SleepEntity> getNearbyHostiles(double radius) {
        return world.getNearbyHostiles(entity.getX(), entity.getY(), entity.getZ(), radius);
    }

    public SleepBlockInfo getBlockInfo(int x, int y, int z) {
        return world.getBlockInfo(x, y, z);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SleepEntity entity;
        private SleepWorldView world;
        private Long dayTime;

        public Builder withEntity(SleepEntity entity) {
            this.entity = entity;
            return this;
        }

        public Builder inWorld(SleepWorldView world) {
            this.world = world;
            return this;
        }

        public Builder atDayTime(long dayTime) {
            this.dayTime = dayTime;
            return this;
        }

        public SleepContext build() {
            if (entity == null) {
                throw new IllegalStateException("Entity is required");
            }
            if (world == null) {
                throw new IllegalStateException("World is required");
            }
            return new SleepContext(this);
        }
    }
}
