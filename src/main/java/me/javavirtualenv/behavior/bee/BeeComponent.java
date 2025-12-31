package me.javavirtualenv.behavior.bee;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for storing bee-specific state and data.
 * <p>
 * This component manages:
 * - Flower memory for efficient foraging
 * - Hive location
 * - Pollination state
 * - Defense status
 * - Waggle dance information
 * <p>
 * This data persists across server restarts via NBT serialization.
 */
public class BeeComponent {

    private static final String TAG_HAS_NECTAR = "HasNectar";
    private static final String TAG_POLLINATION_COOLDOWN = "PollinationCooldown";
    private static final String TAG_HIVE_POS = "HivePos";
    private static final String TAG_HIVE_POS_X = "X";
    private static final String TAG_HIVE_POS_Y = "Y";
    private static final String TAG_HIVE_POS_Z = "Z";
    private static final String TAG_FLOWER_MEMORY = "FlowerMemory";
    private static final String TAG_FLOWER_ENTRY = "Flower";
    private static final String TAG_IS_DANCING = "IsDancing";
    private static final String TAG_DANCE_TIMER = "DanceTimer";
    private static final String TAG_IS_DEFENDING = "IsDefending";
    private static final String TAG_ANGER_TIMER = "AngerTimer";

    private boolean hasNectar;
    private int pollinationCooldown;
    private BlockPos hivePos;
    private final List<BlockPos> flowerMemory;
    private boolean isDancing;
    private int danceTimer;
    private boolean isDefending;
    private int angerTimer;

    public BeeComponent() {
        this.hasNectar = false;
        this.pollinationCooldown = 0;
        this.hivePos = null;
        this.flowerMemory = new ArrayList<>();
        this.isDancing = false;
        this.danceTimer = 0;
        this.isDefending = false;
        this.angerTimer = 0;
    }

    /**
     * Serializes this component to NBT.
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean(TAG_HAS_NECTAR, hasNectar);
        tag.putInt(TAG_POLLINATION_COOLDOWN, pollinationCooldown);
        tag.putBoolean(TAG_IS_DANCING, isDancing);
        tag.putInt(TAG_DANCE_TIMER, danceTimer);
        tag.putBoolean(TAG_IS_DEFENDING, isDefending);
        tag.putInt(TAG_ANGER_TIMER, angerTimer);

        if (hivePos != null) {
            CompoundTag hivePosTag = new CompoundTag();
            hivePosTag.putInt(TAG_HIVE_POS_X, hivePos.getX());
            hivePosTag.putInt(TAG_HIVE_POS_Y, hivePos.getY());
            hivePosTag.putInt(TAG_HIVE_POS_Z, hivePos.getZ());
            tag.put(TAG_HIVE_POS, hivePosTag);
        }

        CompoundTag flowerMemoryTag = new CompoundTag();
        for (int i = 0; i < flowerMemory.size() && i < 10; i++) {
            BlockPos pos = flowerMemory.get(i);
            CompoundTag flowerTag = new CompoundTag();
            flowerTag.putInt(TAG_HIVE_POS_X, pos.getX());
            flowerTag.putInt(TAG_HIVE_POS_Y, pos.getY());
            flowerTag.putInt(TAG_HIVE_POS_Z, pos.getZ());
            flowerMemoryTag.putIntArray(TAG_FLOWER_ENTRY + i,
                new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
        tag.put(TAG_FLOWER_MEMORY, flowerMemoryTag);

        return tag;
    }

    /**
     * Deserializes this component from NBT.
     */
    public static BeeComponent fromNbt(CompoundTag tag) {
        BeeComponent component = new BeeComponent();

        component.hasNectar = tag.getBoolean(TAG_HAS_NECTAR);
        component.pollinationCooldown = tag.getInt(TAG_POLLINATION_COOLDOWN);
        component.isDancing = tag.getBoolean(TAG_IS_DANCING);
        component.danceTimer = tag.getInt(TAG_DANCE_TIMER);
        component.isDefending = tag.getBoolean(TAG_IS_DEFENDING);
        component.angerTimer = tag.getInt(TAG_ANGER_TIMER);

        if (tag.contains(TAG_HIVE_POS)) {
            CompoundTag hivePosTag = tag.getCompound(TAG_HIVE_POS);
            int x = hivePosTag.getInt(TAG_HIVE_POS_X);
            int y = hivePosTag.getInt(TAG_HIVE_POS_Y);
            int z = hivePosTag.getInt(TAG_HIVE_POS_Z);
            component.hivePos = new BlockPos(x, y, z);
        }

        if (tag.contains(TAG_FLOWER_MEMORY)) {
            CompoundTag flowerMemoryTag = tag.getCompound(TAG_FLOWER_MEMORY);
            component.flowerMemory.clear();
            for (int i = 0; i < 10; i++) {
                String key = TAG_FLOWER_ENTRY + i;
                if (flowerMemoryTag.contains(key)) {
                    int[] posArray = flowerMemoryTag.getIntArray(key);
                    if (posArray.length == 3) {
                        component.flowerMemory.add(new BlockPos(posArray[0], posArray[1], posArray[2]));
                    }
                }
            }
        }

        return component;
    }

    // Getters and setters

    public boolean hasNectar() {
        return hasNectar;
    }

    public void setHasNectar(boolean hasNectar) {
        this.hasNectar = hasNectar;
    }

    public int getPollinationCooldown() {
        return pollinationCooldown;
    }

    public void setPollinationCooldown(int pollinationCooldown) {
        this.pollinationCooldown = pollinationCooldown;
    }

    public BlockPos getHivePos() {
        return hivePos;
    }

    public void setHivePos(BlockPos hivePos) {
        this.hivePos = hivePos;
    }

    public List<BlockPos> getFlowerMemory() {
        return new ArrayList<>(flowerMemory);
    }

    public void addFlowerToMemory(BlockPos pos) {
        if (flowerMemory.size() >= 10) {
            flowerMemory.remove(0);
        }
        flowerMemory.add(pos.immutable());
    }

    public boolean isDancing() {
        return isDancing;
    }

    public void setDancing(boolean dancing) {
        isDancing = dancing;
    }

    public int getDanceTimer() {
        return danceTimer;
    }

    public void setDanceTimer(int danceTimer) {
        this.danceTimer = danceTimer;
    }

    public boolean isDefending() {
        return isDefending;
    }

    public void setDefending(boolean defending) {
        isDefending = defending;
    }

    public int getAngerTimer() {
        return angerTimer;
    }

    public void setAngerTimer(int angerTimer) {
        this.angerTimer = angerTimer;
    }
}
