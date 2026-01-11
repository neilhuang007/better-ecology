package me.javavirtualenv.behavior.sleeping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Simple implementation of SleepWorldView for testing.
 * This class does not depend on any Minecraft classes.
 */
public class TestSleepWorld implements SleepWorldView {

    private final long dayTime;
    private final long gameTime;
    private final List<SleepEntity> entities;
    private final BlockProvider blockProvider;

    private TestSleepWorld(Builder builder) {
        this.dayTime = builder.dayTime;
        this.gameTime = builder.gameTime;
        this.entities = new ArrayList<>(builder.entities);
        this.blockProvider = builder.blockProvider != null ? builder.blockProvider : (x, y, z) -> SleepBlockInfo.air();
    }

    @Override
    public long getDayTime() {
        return dayTime;
    }

    @Override
    public long getGameTime() {
        return gameTime;
    }

    @Override
    public SleepBlockInfo getBlockInfo(int x, int y, int z) {
        return blockProvider.getBlockInfo(x, y, z);
    }

    @Override
    public List<SleepEntity> getNearbyEntities(double x, double y, double z, double radius) {
        List<SleepEntity> nearby = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (SleepEntity entity : entities) {
            if (!entity.isAlive()) {
                continue;
            }
            double dist = entity.distanceToSqr(x, y, z);
            if (dist <= radiusSquared) {
                nearby.add(entity);
            }
        }

        return nearby;
    }

    @Override
    public List<SleepEntity> getNearbyEntitiesOfType(double x, double y, double z, double radius, String entityType) {
        return filterEntities(getNearbyEntities(x, y, z, radius), entityType);
    }

    @Override
    public List<SleepEntity> getNearbyHostiles(double x, double y, double z, double radius) {
        List<SleepEntity> nearby = getNearbyEntities(x, y, z, radius);
        List<SleepEntity> hostiles = new ArrayList<>();

        for (SleepEntity entity : nearby) {
            String type = entity.getEntityType().toLowerCase();
            if (isHostileType(type)) {
                hostiles.add(entity);
            }
        }

        return hostiles;
    }

    private List<SleepEntity> filterEntities(List<SleepEntity> entities, String entityType) {
        List<SleepEntity> filtered = new ArrayList<>();
        String targetSpecies = entityType.toLowerCase();

        for (SleepEntity entity : entities) {
            String entitySpecies = entity.getEntityType().toLowerCase();
            if (entitySpecies.contains(targetSpecies)) {
                filtered.add(entity);
            }
        }

        return filtered;
    }

    private boolean isHostileType(String entityType) {
        return entityType.contains("zombie")
                || entityType.contains("skeleton")
                || entityType.contains("creeper")
                || entityType.contains("spider")
                || entityType.contains("phantom")
                || entityType.contains("witch");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long dayTime = 6000;
        private long gameTime = 0;
        private List<SleepEntity> entities = new ArrayList<>();
        private BlockProvider blockProvider;

        public Builder atDayTime(long dayTime) {
            this.dayTime = dayTime % 24000;
            return this;
        }

        public Builder atGameTime(long gameTime) {
            this.gameTime = gameTime;
            return this;
        }

        public Builder withEntity(SleepEntity entity) {
            this.entities.add(entity);
            return this;
        }

        public Builder withEntities(List<SleepEntity> entities) {
            this.entities = new ArrayList<>(entities);
            return this;
        }

        public Builder withBlockProvider(BlockProvider provider) {
            this.blockProvider = provider;
            return this;
        }

        public TestSleepWorld build() {
            return new TestSleepWorld(this);
        }
    }

    @FunctionalInterface
    public interface BlockProvider {
        SleepBlockInfo getBlockInfo(int x, int y, int z);
    }
}
