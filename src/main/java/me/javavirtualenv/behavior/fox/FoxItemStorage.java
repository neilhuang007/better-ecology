package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.behavior.shared.AnimalItemStorage;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;

/**
 * Component for storing items carried by foxes.
 * <p>
 * Foxes can carry one item at a time in their mouth.
 * The item is visible to players and can be dropped or gifted.
 * <p>
 * This component attaches to fox entities and persists across saves.
 * Synchronizes with vanilla Fox equipment slot for visual display.
 */
public class FoxItemStorage {

    private static final String STORAGE_KEY = "fox_item_storage";

    private final AnimalItemStorage delegate;
    private final Fox fox;

    private FoxItemStorage(Fox fox, AnimalItemStorage delegate) {
        this.fox = fox;
        this.delegate = delegate;
    }

    /**
     * Get or create the item storage component for a fox.
     */
    public static FoxItemStorage get(Mob mob) {
        if (!(mob instanceof Fox fox)) {
            throw new IllegalArgumentException("FoxItemStorage can only be used with Fox entities");
        }
        AnimalItemStorage delegate = AnimalItemStorage.get(mob, STORAGE_KEY);
        FoxItemStorage storage = new FoxItemStorage(fox, delegate);
        storage.syncFromVanilla();
        return storage;
    }

    /**
     * Check if fox has an item stored.
     */
    public boolean hasItem() {
        return delegate.hasItem();
    }

    /**
     * Get the carried item.
     */
    public ItemStack getItem() {
        return delegate.getItem();
    }

    /**
     * Set the carried item.
     * Synchronizes with vanilla fox mouth slot for visual display.
     */
    public void setItem(ItemStack itemStack) {
        delegate.setItem(itemStack);
        syncToVanilla();
    }

    /**
     * Remove the carried item.
     * Synchronizes with vanilla fox mouth slot.
     */
    public void clearItem() {
        delegate.clearItem();
        syncToVanilla();
    }

    /**
     * Synchronize storage to vanilla fox mouth slot.
     * Updates the visual representation of the carried item.
     */
    private void syncToVanilla() {
        ItemStack currentItem = delegate.getItem();
        // Foxes use MAINHAND slot for carrying items in their mouth
        fox.setItemSlot(EquipmentSlot.MAINHAND, currentItem);

        // Mark equipment changed for network sync
        if (!fox.level().isClientSide) {
            fox.setDropChance(EquipmentSlot.MAINHAND, 2.0f); // Don't drop on death
        }
    }

    /**
     * Synchronize from vanilla fox mouth slot to storage.
     * Loads the item from vanilla slot if storage is empty.
     */
    private void syncFromVanilla() {
        if (!delegate.hasItem()) {
            ItemStack vanillaItem = fox.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!vanillaItem.isEmpty()) {
                delegate.setItem(vanillaItem);
            }
        }
    }

    /**
     * Remove storage from fox (for cleanup).
     */
    public static void remove(Mob fox) {
        AnimalItemStorage.remove(fox, STORAGE_KEY);
    }
}
