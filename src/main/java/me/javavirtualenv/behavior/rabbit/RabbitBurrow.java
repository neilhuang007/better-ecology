package me.javavirtualenv.behavior.rabbit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a rabbit burrow with stored state.
 * <p>
 * Burrows provide:
 * - Shelter from predators and weather
 * - Storage for cached food
 * - Breeding and nesting location
 * - Temperature regulation
 */
public class RabbitBurrow {

    private final BlockPos position;
    private final BurrowType type;
    private final long createdTime;
    private int occupantCount;
    private boolean isActive;
    private int capacity;

    // Food storage system
    private final Map<String, Integer> foodCache;
    private final int maxFoodStorage;

    public RabbitBurrow(BlockPos position, BurrowType type) {
        this.position = position;
        this.type = type;
        this.createdTime = System.currentTimeMillis();
        this.occupantCount = 0;
        this.isActive = true;
        this.capacity = type.getCapacity();
        this.foodCache = new HashMap<>();
        this.maxFoodStorage = 16; // Maximum food items stored
    }

    public static RabbitBurrow fromNbt(CompoundTag tag) {
        BlockPos pos = new BlockPos(
            tag.getInt("X"),
            tag.getInt("Y"),
            tag.getInt("Z")
        );
        BurrowType type = BurrowType.valueOf(tag.getString("Type"));

        RabbitBurrow burrow = new RabbitBurrow(pos, type);
        burrow.occupantCount = tag.getInt("Occupants");
        burrow.isActive = tag.getBoolean("Active");
        burrow.capacity = tag.getInt("Capacity");

        // Load food cache
        if (tag.contains("FoodCache")) {
            ListTag foodList = tag.getList("FoodCache", 10);
            for (int i = 0; i < foodList.size(); i++) {
                CompoundTag foodTag = foodList.getCompound(i);
                String item = foodTag.getString("Item");
                int count = foodTag.getInt("Count");
                burrow.foodCache.put(item, count);
            }
        }

        return burrow;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", position.getX());
        tag.putInt("Y", position.getY());
        tag.putInt("Z", position.getZ());
        tag.putString("Type", type.name());
        tag.putInt("Occupants", occupantCount);
        tag.putBoolean("Active", isActive);
        tag.putInt("Capacity", capacity);
        tag.putLong("Created", createdTime);

        // Save food cache
        ListTag foodList = new ListTag();
        for (Map.Entry<String, Integer> entry : foodCache.entrySet()) {
            CompoundTag foodTag = new CompoundTag();
            foodTag.putString("Item", entry.getKey());
            foodTag.putInt("Count", entry.getValue());
            foodList.add(foodTag);
        }
        tag.put("FoodCache", foodList);

        return tag;
    }

    public BlockPos getPosition() {
        return position;
    }

    public BurrowType getType() {
        return type;
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

    public double distanceTo(BlockPos other) {
        double dx = position.getX() - other.getX();
        double dy = position.getY() - other.getY();
        double dz = position.getZ() - other.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Food storage methods

    /**
     * Gets the current total food count in storage.
     */
    public int getTotalFoodCount() {
        return foodCache.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Checks if burrow has food storage capacity.
     */
    public boolean hasFoodStorageSpace() {
        return getTotalFoodCount() < maxFoodStorage;
    }

    /**
     * Stores food in the burrow.
     *
     * @param itemId The item ID to store
     * @param count  The quantity to store
     * @return The actual amount stored (may be less if capacity is reached)
     */
    public int storeFood(String itemId, int count) {
        int currentCount = foodCache.getOrDefault(itemId, 0);
        int totalFood = getTotalFoodCount();
        int spaceRemaining = maxFoodStorage - totalFood;
        int toStore = Math.min(count, spaceRemaining);

        if (toStore > 0) {
            foodCache.put(itemId, currentCount + toStore);
        }

        return toStore;
    }

    /**
     * Retrieves food from the burrow.
     *
     * @param itemId The item ID to retrieve
     * @param count  The quantity to retrieve
     * @return The actual amount retrieved (may be less if insufficient food)
     */
    public int retrieveFood(String itemId, int count) {
        int currentCount = foodCache.getOrDefault(itemId, 0);
        int toRetrieve = Math.min(count, currentCount);

        if (toRetrieve > 0) {
            if (toRetrieve == currentCount) {
                foodCache.remove(itemId);
            } else {
                foodCache.put(itemId, currentCount - toRetrieve);
            }
        }

        return toRetrieve;
    }

    /**
     * Gets the count of a specific food item.
     */
    public int getFoodCount(String itemId) {
        return foodCache.getOrDefault(itemId, 0);
    }

    /**
     * Gets all stored food items.
     */
    public Map<String, Integer> getFoodCache() {
        return new HashMap<>(foodCache);
    }

    /**
     * Checks if the burrow has a specific food item.
     */
    public boolean hasFood(String itemId) {
        return foodCache.containsKey(itemId) && foodCache.get(itemId) > 0;
    }

    /**
     * Gets the maximum food storage capacity.
     */
    public int getMaxFoodStorage() {
        return maxFoodStorage;
    }

    /**
     * Clears all food from storage.
     */
    public void clearFoodCache() {
        foodCache.clear();
    }
}
