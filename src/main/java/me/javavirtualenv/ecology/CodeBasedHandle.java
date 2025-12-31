package me.javavirtualenv.ecology;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Base class for code-based EcologyHandles that don't require EcologyProfile.
 * Extends the EcologyHandle interface with a profile-free implementation.
 */
public abstract class CodeBasedHandle implements EcologyHandle {

    @Override
    public final boolean supports(EcologyProfile profile) {
        // Code-based handles always return true since they're explicitly registered
        return true;
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Default implementation does nothing
        // Subclasses can override this
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Default implementation does nothing
        // Subclasses can override this
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Default implementation does nothing
        // NBT is automatically loaded via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Default implementation - save handle tag
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    @Override
    public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile, ItemStack stack, boolean original) {
        // Default implementation doesn't override food checks
        return original;
    }
}
