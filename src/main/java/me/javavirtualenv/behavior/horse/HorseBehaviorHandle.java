package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.Nullable;

/**
 * Handle for horse-specific behaviors.
 * Registers kick defense, rearing, bonding, and herd dynamics goals.
 */
public class HorseBehaviorHandle extends CodeBasedHandle {
    private static final String NBT_BOND_DATA = "horse_bonds";

    @Override
    public String id() {
        return "horse_behavior";
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof AbstractHorse horse)) {
            return;
        }

        // Register kick defense (high priority for self-defense)
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
        accessor.betterEcology$getGoalSelector().addGoal(1, new KickDefenseGoal(horse));

        // Register rearing behavior
        accessor.betterEcology$getGoalSelector().addGoal(2, new RearingGoal(horse));

        // Register bonding behavior
        accessor.betterEcology$getGoalSelector().addGoal(3, new BondingGoal(horse));

        // Register herd dynamics (only for wild horses)
        accessor.betterEcology$getGoalSelector().addGoal(4, new HerdDynamicsGoal(horse));

        // Register social grooming
        accessor.betterEcology$getGoalSelector().addGoal(5, new SocialGroomingGoal(horse));
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        super.writeNbt(mob, component, profile, tag);

        // Save bond data
        HorseBondData bondData = getOrCreateBondData(component);
        CompoundTag bondTag = new CompoundTag();
        bondData.saveToNbt(bondTag);
        tag.put(NBT_BOND_DATA, bondTag);
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        super.readNbt(mob, component, profile, tag);

        // Load bond data
        if (tag.contains(NBT_BOND_DATA)) {
            HorseBondData bondData = getOrCreateBondData(component);
            CompoundTag bondTag = tag.getCompound(NBT_BOND_DATA);
            bondData.loadFromNbt(bondTag);
        }
    }

    private HorseBondData getOrCreateBondData(EcologyComponent component) {
        // Create new bond data
        HorseBondData bondData = new HorseBondData();

        // Load from NBT if available
        CompoundTag handleTag = component.getHandleTag(id());
        if (handleTag.contains(NBT_BOND_DATA)) {
            CompoundTag bondTag = handleTag.getCompound(NBT_BOND_DATA);
            bondData.loadFromNbt(bondTag);
        }

        return bondData;
    }

    /**
     * Called when a player rides the horse.
     * Records the ride for bonding purposes.
     */
    public static void onPlayerRide(AbstractHorse horse, EcologyComponent component) {
        if (component == null) {
            return;
        }

        HorseBehaviorHandle handle = getHandle(component);
        if (handle == null) {
            return;
        }

        HorseBondData bondData = handle.getOrCreateBondData(component);
        if (horse.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player player) {
            bondData.recordRide(player);
        }
    }

    /**
     * Called when a player interacts with the horse.
     * Records the interaction for bonding purposes.
     */
    public static void onPlayerInteract(AbstractHorse horse, EcologyComponent component) {
        if (component == null) {
            return;
        }

        HorseBehaviorHandle handle = getHandle(component);
        if (handle == null) {
            return;
        }

        HorseBondData bondData = handle.getOrCreateBondData(component);
        // Find the player who last interacted
        if (horse.getLastHurtByMob() instanceof net.minecraft.world.entity.player.Player) {
            // This is hit, not interaction
            return;
        }

        // Record interaction (would need proper hook in actual implementation)
    }

    @Nullable
    private static HorseBehaviorHandle getHandle(EcologyComponent component) {
        for (EcologyHandle handle : component.handles()) {
            if (handle instanceof HorseBehaviorHandle horseHandle) {
                return horseHandle;
            }
        }
        return null;
    }

    /**
     * Get bond data for a horse component.
     */
    public static HorseBondData getBondData(EcologyComponent component) {
        HorseBehaviorHandle handle = getHandle(component);
        if (handle == null) {
            return new HorseBondData();
        }
        return handle.getOrCreateBondData(component);
    }

    /**
     * Get speed bonus from bond with a player.
     */
    public static float getSpeedBonus(EcologyComponent component, net.minecraft.world.entity.player.Player player) {
        HorseBondData bondData = getBondData(component);
        return bondData.getSpeedBonus(player);
    }

    /**
     * Get jump bonus from bond with a player.
     */
    public static float getJumpBonus(EcologyComponent component, net.minecraft.world.entity.player.Player player) {
        HorseBondData bondData = getBondData(component);
        return bondData.getJumpBonus(player);
    }

    /**
     * Check if horse is bonded to a player.
     */
    public static boolean isBonded(EcologyComponent component, net.minecraft.world.entity.player.Player player) {
        HorseBondData bondData = getBondData(component);
        return bondData.isBonded(player);
    }

    /**
     * Get obedience chance for a player.
     */
    public static float getObedienceChance(EcologyComponent component, net.minecraft.world.entity.player.Player player) {
        HorseBondData bondData = getBondData(component);
        return bondData.getObedienceChance(player);
    }
}
