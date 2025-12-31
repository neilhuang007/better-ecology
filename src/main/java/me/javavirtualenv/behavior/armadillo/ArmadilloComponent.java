package me.javavirtualenv.behavior.armadillo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Component for managing armadillo-specific state.
 * <p>
 * Tracks:
 * - Rolled up state (defense mode)
 * - Roll/unroll timers
 * - Current burrow location
 * - Panic state
 * - Insect scent detection
 */
public class ArmadilloComponent {

    private static final String NBT_IS_ROLLED = "IsRolled";
    private static final String NBT_ROLL_TICKS = "RollTicks";
    private static final String NBT_UNROLL_TICKS = "UnrollTicks";
    private static final String NBT_BURROW_POS = "BurrowPos";
    private static final String NBT_IS_PANICKING = "IsPanicking";
    private static final String NBT_PANIC_TICKS = "PanicTicks";
    private static final String NBT_SCENT_STRENGTH = "ScentStrength";

    private final CompoundTag tag;

    public ArmadilloComponent(CompoundTag tag) {
        this.tag = tag;
    }

    public boolean isRolled() {
        return tag.getBoolean(NBT_IS_ROLLED);
    }

    public void setRolled(boolean rolled) {
        tag.putBoolean(NBT_IS_ROLLED, rolled);
        if (rolled) {
            setRollTicks(0);
        } else {
            setUnrollTicks(0);
        }
    }

    public int getRollTicks() {
        return tag.getInt(NBT_ROLL_TICKS);
    }

    public void setRollTicks(int ticks) {
        tag.putInt(NBT_ROLL_TICKS, ticks);
    }

    public int getUnrollTicks() {
        return tag.getInt(NBT_UNROLL_TICKS);
    }

    public void setUnrollTicks(int ticks) {
        tag.putInt(NBT_UNROLL_TICKS, ticks);
    }

    public BlockPos getBurrowPos() {
        if (!tag.contains(NBT_BURROW_POS)) {
            return null;
        }
        int[] posArray = tag.getIntArray(NBT_BURROW_POS);
        if (posArray.length != 3) {
            return null;
        }
        return new BlockPos(posArray[0], posArray[1], posArray[2]);
    }

    public void setBurrowPos(BlockPos pos) {
        if (pos == null) {
            tag.remove(NBT_BURROW_POS);
        } else {
            tag.putIntArray(NBT_BURROW_POS, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    public boolean isPanicking() {
        return tag.getBoolean(NBT_IS_PANICKING);
    }

    public void setPanicking(boolean panicking) {
        tag.putBoolean(NBT_IS_PANICKING, panicking);
        if (panicking) {
            setPanicTicks(0);
        }
    }

    public int getPanicTicks() {
        return tag.getInt(NBT_PANIC_TICKS);
    }

    public void setPanicTicks(int ticks) {
        tag.putInt(NBT_PANIC_TICKS, ticks);
    }

    public double getScentStrength() {
        return tag.getDouble(NBT_SCENT_STRENGTH);
    }

    public void setScentStrength(double strength) {
        tag.putDouble(NBT_SCENT_STRENGTH, Math.max(0.0, Math.min(1.0, strength)));
    }

    /**
     * Checks if armadillo can unroll (safe environment).
     */
    public boolean canUnroll() {
        if (isPanicking()) {
            return false;
        }
        return getUnrollTicks() >= 40; // 2 second minimum unroll time
    }

    /**
     * Checks if armadillo can roll (not already rolled, not in burrow).
     */
    public boolean canRoll(Mob mob) {
        if (isRolled()) {
            return false;
        }
        // Can't roll if in a burrow
        BlockPos burrowPos = getBurrowPos();
        if (burrowPos != null) {
            BlockPos currentPos = mob.blockPosition();
            return currentPos.distSqr(burrowPos) > 9.0; // More than 3 blocks away
        }
        return true;
    }

    public CompoundTag getTag() {
        return tag;
    }
}
