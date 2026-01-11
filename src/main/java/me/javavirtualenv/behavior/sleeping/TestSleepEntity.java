package me.javavirtualenv.behavior.sleeping;

/**
 * Simple POJO implementation of SleepEntity for testing.
 * This class does not depend on any Minecraft classes.
 */
public class TestSleepEntity implements SleepEntity {

    private final double x;
    private final double y;
    private final double z;
    private final float health;
    private final float maxHealth;
    private final boolean isBaby;
    private final boolean isAlive;
    private final String entityType;

    private TestSleepEntity(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.health = builder.health;
        this.maxHealth = builder.maxHealth;
        this.isBaby = builder.isBaby;
        this.isAlive = builder.isAlive;
        this.entityType = builder.entityType;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public int getBlockX() {
        return (int) Math.floor(x);
    }

    @Override
    public int getBlockY() {
        return (int) Math.floor(y);
    }

    @Override
    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    @Override
    public float getHealth() {
        return health;
    }

    @Override
    public float getMaxHealth() {
        return maxHealth;
    }

    @Override
    public boolean isBaby() {
        return isBaby;
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    @Override
    public String getEntityType() {
        return entityType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double x = 0.0;
        private double y = 64.0;
        private double z = 0.0;
        private float health = 20.0f;
        private float maxHealth = 20.0f;
        private boolean isBaby = false;
        private boolean isAlive = true;
        private String entityType = "entity.minecraft.cow";

        public Builder atPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public Builder withHealth(float health, float maxHealth) {
            this.health = health;
            this.maxHealth = maxHealth;
            return this;
        }

        public Builder asBaby() {
            this.isBaby = true;
            return this;
        }

        public Builder isAlive(boolean isAlive) {
            this.isAlive = isAlive;
            return this;
        }

        public Builder withEntityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public TestSleepEntity build() {
            return new TestSleepEntity(this);
        }
    }
}
