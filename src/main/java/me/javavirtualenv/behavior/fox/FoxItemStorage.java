package me.javavirtualenv.behavior.fox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Component for storing items carried by foxes.
 * <p>
 * Foxes can carry one item at a time in their mouth.
 * The item is visible to players and can be dropped or gifted.
 * <p>
 * This component attaches to fox entities and persists across saves.
 */
public class FoxItemStorage {

    private static final String STORAGE_KEY = "better-ecology:fox-item-storage";
    private static final String ITEM_KEY = "carried_item";

    private final Mob fox;
    private ItemStack carriedItem = ItemStack.EMPTY;

    private FoxItemStorage(Mob fox) {
        this.fox = fox;
    }

    /**
     * Get or create the item storage component for a fox.
     */
    public static FoxItemStorage get(Mob fox) {
        if (fox.getPersistentData().contains(STORAGE_KEY)) {
            CompoundTag tag = fox.getPersistentData().getCompound(STORAGE_KEY);
            return loadFromTag(fox, tag);
        }

        FoxItemStorage storage = new FoxItemStorage(fox);
        fox.getPersistentData().put(STORAGE_KEY, storage.saveToTag());
        return storage;
    }

    /**
     * Check if fox has an item stored.
     */
    public boolean hasItem() {
        return !carriedItem.isEmpty();
    }

    /**
     * Get the carried item.
     */
    public ItemStack getItem() {
        return carriedItem.copy();
    }

    /**
     * Set the carried item.
     */
    public void setItem(ItemStack itemStack) {
        this.carriedItem = itemStack.copy();
        saveToFox();
    }

    /**
     * Remove the carried item.
     */
    public void clearItem() {
        this.carriedItem = ItemStack.EMPTY;
        saveToFox();
    }

    /**
     * Save storage data to NBT.
     */
    public CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        if (!carriedItem.isEmpty()) {
            tag.put(ITEM_KEY, carriedItem.save(new CompoundTag()));
        }
        return tag;
    }

    /**
     * Save storage data to fox's persistent data.
     */
    private void saveToFox() {
        fox.getPersistentData().put(STORAGE_KEY, saveToTag());
    }

    /**
     * Load storage data from NBT.
     */
    private static FoxItemStorage loadFromTag(Mob fox, CompoundTag tag) {
        FoxItemStorage storage = new FoxItemStorage(fox);

        if (tag.contains(ITEM_KEY)) {
            CompoundTag itemTag = tag.getCompound(ITEM_KEY);
            storage.carriedItem = ItemStack.of(itemTag);
        }

        return storage;
    }

    /**
     * Remove storage from fox (for cleanup).
     */
    public static void remove(Mob fox) {
        fox.getPersistentData().remove(STORAGE_KEY);
    }
}
