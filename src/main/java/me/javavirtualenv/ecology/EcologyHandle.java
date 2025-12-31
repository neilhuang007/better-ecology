package me.javavirtualenv.ecology;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

public interface EcologyHandle {
	String id();

	boolean supports(EcologyProfile profile);

	default void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
	}

	default void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
	}

	default int tickInterval() {
		return 1;
	}

	default void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
	}

	default void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
	}

	default boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile, ItemStack stack, boolean original) {
		return original;
	}

	/**
	 * Initialize this handle for the given entity and component.
	 * Called lazily on first access to the handle.
	 *
	 * @param mob The entity
	 * @param component The ecology component
	 * @param profile The ecology profile
	 */
	default void initialize(Mob mob, EcologyComponent component, EcologyProfile profile) {
	}
}
