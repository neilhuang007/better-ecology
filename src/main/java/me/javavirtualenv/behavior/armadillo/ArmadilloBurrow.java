package me.javavirtualenv.behavior.armadillo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Represents an armadillo burrow.
 * <p>
 * Armadillos dig burrows for:
 * - Sleeping during the day (nocturnal/crepuscular)
 * - Temperature regulation
 * - Safety from predators
 * - Raising young
 * <p>
 * Unlike rabbits, armadillo burrows are:
 * - Larger (can fit multiple adults)
 * - Deeper (more extensive tunnel systems)
 * - More permanent (used for extended periods)
 */
public class ArmadilloBurrow {

    private final BlockPos position;
    private final long createdTime;
    private int occupantCount;
    private boolean isActive;
    private final int capacity;
    private double temperature;
    private int lastUsedTick;

    public ArmadilloBurrow(BlockPos position) {
        this.position = position;
        this.createdTime = System.currentTimeMillis();
        this.occupantCount = 0;
        this.isActive = true;
        this.capacity = 4; // Armadillos share burrows more readily than rabbits
        this.temperature = 20.0; // Default comfortable temperature
        this.lastUsedTick = 0;
    }

    public static ArmadilloBurrow fromNbt(CompoundTag tag) {
        BlockPos pos = new BlockPos(
            tag.getInt("X"),
            tag.getInt("Y"),
            tag.getInt("Z")
        );

        ArmadilloBurrow burrow = new ArmadilloBurrow(pos);
        burrow.occupantCount = tag.getInt("Occupants");
        burrow.isActive = tag.getBoolean("Active");
        burrow.temperature = tag.getDouble("Temperature");
        burrow.lastUsedTick = tag.getInt("LastUsed");

        return burrow;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", position.getX());
        tag.putInt("Y", position.getY());
        tag.putInt("Z", position.getZ());
        tag.putInt("Occupants", occupantCount);
        tag.putBoolean("Active", isActive);
        tag.putInt("Capacity", capacity);
        tag.putLong("Created", createdTime);
        tag.putDouble("Temperature", temperature);
        tag.putInt("LastUsed", lastUsedTick);

        return tag;
    }

    public BlockPos getPosition() {
        return position;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public int getOccupantCount() {
        return occupantCount;
    }

    public void setOccupantCount(int count) {
        this.occupantCount = Math.max(0, Math.min(count, capacity));
    }

    public void addOccupant() {
        if (occupantCount < capacity) {
            occupantCount++;
        }
    }

    public void removeOccupant() {
        if (occupantCount > 0) {
            occupantCount--;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isFull() {
        return occupantCount >= capacity;
    }

    public boolean isEmpty() {
        return occupantCount == 0;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getLastUsedTick() {
        return lastUsedTick;
    }

    public void setLastUsedTick(int tick) {
        this.lastUsedTick = tick;
    }

    public double distanceTo(BlockPos other) {
        double dx = position.getX() - other.getX();
        double dy = position.getY() - other.getY();
        double dz = position.getZ() - other.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean isComfortableTemperature(double targetTemp) {
        double diff = Math.abs(temperature - targetTemp);
        return diff < 5.0; // Within 5 degrees is comfortable
    }
}
