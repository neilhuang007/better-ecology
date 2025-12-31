package me.javavirtualenv.ecology.handles;

import java.util.UUID;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import org.jetbrains.annotations.Nullable;

/**
 * Handle for tracking parent-child relationships between animals.
 * Stores the mother's UUID on baby entities to enable mother-offspring behaviors
 * like following, protection, and separation distress.
 */
public final class ParentChildHandle implements EcologyHandle {

    private static final String MOTHER_UUID_KEY = "mother_uuid";

    @Override
    public String id() {
        return "parent_child";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return true;
    }

    @Override
    public void initialize(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof AgeableMob ageableMob)) {
            return;
        }

        if (ageableMob.isBaby()) {
            CompoundTag data = component.getHandleTag(id());
            if (!data.contains(MOTHER_UUID_KEY)) {
                UUID motherUuid = getInheritedMotherUuid(ageableMob);
                if (motherUuid != null) {
                    setMotherUuid(component, motherUuid);
                }
            }
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag data = component.getHandleTag(id());
        if (tag.contains(id())) {
            CompoundTag savedData = tag.getCompound(id());
            if (savedData.contains(MOTHER_UUID_KEY)) {
                data.putUUID(MOTHER_UUID_KEY, savedData.getUUID(MOTHER_UUID_KEY));
            }
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag data = component.getHandleTag(id());
        if (data.contains(MOTHER_UUID_KEY)) {
            CompoundTag saveTag = new CompoundTag();
            saveTag.putUUID(MOTHER_UUID_KEY, data.getUUID(MOTHER_UUID_KEY));
            tag.put(id(), saveTag);
        }
    }

    /**
     * Sets the mother UUID for a baby entity.
     *
     * @param component The ecology component
     * @param motherUuid The UUID of the mother
     */
    public static void setMotherUuid(EcologyComponent component, UUID motherUuid) {
        if (motherUuid == null) {
            return;
        }
        CompoundTag data = component.getHandleTag("parent_child");
        data.putUUID(MOTHER_UUID_KEY, motherUuid);
    }

    /**
     * Gets the mother UUID for an entity.
     *
     * @param component The ecology component
     * @return The mother UUID, or null if not set
     */
    @Nullable
    public static UUID getMotherUuid(EcologyComponent component) {
        CompoundTag data = component.getHandleTag("parent_child");
        if (!data.contains(MOTHER_UUID_KEY)) {
            return null;
        }
        return data.getUUID(MOTHER_UUID_KEY);
    }

    /**
     * Clears the mother UUID from an entity.
     * Useful when the entity grows up or the mother dies.
     *
     * @param component The ecology component
     */
    public static void clearMotherUuid(EcologyComponent component) {
        CompoundTag data = component.getHandleTag("parent_child");
        data.remove(MOTHER_UUID_KEY);
    }

    /**
     * Attempts to get the mother UUID from the baby's temporary parent reference.
     * This is a fallback for when the handle data wasn't set during breeding.
     *
     * @param baby The baby entity
     * @return The mother UUID if available, null otherwise
     */
    @Nullable
    private static UUID getInheritedMotherUuid(AgeableMob baby) {
        return null;
    }
}
