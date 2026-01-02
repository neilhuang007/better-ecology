package me.javavirtualenv.behavior.shared;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;

/**
 * Generic component for storing items carried by animals.
 * <p>
 * Provides a shared storage system for animals that can carry items
 * (e.g., foxes, wolves, cats). The item persists across saves and can
 * be retrieved, dropped, or transferred.
 * <p>
 * This component attaches to mob entities and uses the ecology system
 * for persistence through NBT data.
 */
public class AnimalItemStorage {

    private static final String ITEM_KEY = "carried_item";

    private final Mob mob;
    private final String storageKey;
    private ItemStack carriedItem = ItemStack.EMPTY;

    private AnimalItemStorage(Mob mob, String storageKey) {
        this.mob = mob;
        this.storageKey = storageKey;
    }

    /**
     * Get or create the item storage component for a mob.
     *
     * @param mob The mob entity
     * @param storageKey Unique key for this storage type (e.g., "animal_item_storage")
     * @return The storage component
     */
    public static AnimalItemStorage get(Mob mob, String storageKey) {
        EcologyComponent component = ((EcologyAccess) mob).betterEcology$getEcologyComponent();
        CompoundTag tag = component.getHandleTag(storageKey);

        if (tag != null && tag.contains(ITEM_KEY)) {
            return loadFromTag(mob, storageKey, tag);
        }

        AnimalItemStorage storage = new AnimalItemStorage(mob, storageKey);
        component.setHandleTag(storageKey, storage.saveToTag());
        return storage;
    }

    /**
     * Check if the animal has an item stored.
     *
     * @return true if carrying an item
     */
    public boolean hasItem() {
        return !carriedItem.isEmpty();
    }

    /**
     * Get the carried item.
     *
     * @return A copy of the carried item stack
     */
    public ItemStack getItem() {
        return carriedItem.copy();
    }

    /**
     * Set the carried item.
     *
     * @param itemStack The item to carry
     */
    public void setItem(ItemStack itemStack) {
        this.carriedItem = itemStack.copy();
        saveToMob();
    }

    /**
     * Remove the carried item.
     */
    public void clearItem() {
        this.carriedItem = ItemStack.EMPTY;
        saveToMob();
    }

    /**
     * Save storage data to NBT.
     *
     * @return NBT tag containing storage data
     */
    public CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        if (!carriedItem.isEmpty()) {
            HolderLookup.Provider registries = mob.registryAccess();
            CompoundTag itemTag = (CompoundTag) carriedItem.save(registries);
            tag.put(ITEM_KEY, itemTag);
        }
        return tag;
    }

    /**
     * Save storage data to mob's persistent data.
     */
    private void saveToMob() {
        EcologyComponent component = ((EcologyAccess) mob).betterEcology$getEcologyComponent();
        component.setHandleTag(storageKey, saveToTag());
    }

    /**
     * Load storage data from NBT.
     *
     * @param mob The mob entity
     * @param storageKey The storage key
     * @param tag The NBT tag to load from
     * @return A new storage instance with loaded data
     */
    private static AnimalItemStorage loadFromTag(Mob mob, String storageKey, CompoundTag tag) {
        AnimalItemStorage storage = new AnimalItemStorage(mob, storageKey);

        if (tag.contains(ITEM_KEY)) {
            CompoundTag itemTag = tag.getCompound(ITEM_KEY);
            HolderLookup.Provider registries = mob.registryAccess();
            storage.carriedItem = ItemStack.parseOptional(registries, itemTag);
        }

        return storage;
    }

    /**
     * Remove storage from mob (for cleanup).
     *
     * @param mob The mob entity
     * @param storageKey The storage key
     */
    public static void remove(Mob mob, String storageKey) {
        EcologyComponent component = ((EcologyAccess) mob).betterEcology$getEcologyComponent();
        component.setHandleTag(storageKey, new CompoundTag());
    }
}
