package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.behavior.shared.AnimalItemStorage;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Component for storing items carried by foxes.
 * <p>
 * Foxes can carry one item at a time in their mouth.
 * The item is visible to players and can be dropped or gifted.
 * <p>
 * This component attaches to fox entities and persists across saves.
 * Delegates to the shared AnimalItemStorage for functionality.
 */
public class FoxItemStorage {

    private static final String STORAGE_KEY = "fox_item_storage";

    private final AnimalItemStorage delegate;

    private FoxItemStorage(AnimalItemStorage delegate) {
        this.delegate = delegate;
    }

    /**
     * Get or create the item storage component for a fox.
     */
    public static FoxItemStorage get(Mob fox) {
        AnimalItemStorage delegate = AnimalItemStorage.get(fox, STORAGE_KEY);
        return new FoxItemStorage(delegate);
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
     */
    public void setItem(ItemStack itemStack) {
        delegate.setItem(itemStack);
    }

    /**
     * Remove the carried item.
     */
    public void clearItem() {
        delegate.clearItem();
    }

    /**
     * Remove storage from fox (for cleanup).
     */
    public static void remove(Mob fox) {
        AnimalItemStorage.remove(fox, STORAGE_KEY);
    }
}
