package me.javavirtualenv.behavior.sniffer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component for storing sniffer-specific state and data.
 * <p>
 * This component manages:
 * - Scent memory for detected ancient seeds
 * - Sniffing state and cooldowns
 * - Digging state and limits
 * - Excitement level when seeds detected
 * - Known scent locations
 * <p>
 * This data persists across server restarts via NBT serialization.
 */
public class SnifferComponent {

    private static final String TAG_IS_SNIFFING = "IsSniffing";
    private static final String TAG_SNIFF_COOLDOWN = "SniffCooldown";
    private static final String TAG_EXCITEMENT_LEVEL = "ExcitementLevel";
    private static final String TAG_KNOWN_SCENTS = "KnownScents";
    private static final String TAG_SCENT_ENTRY = "Scent";
    private static final String TAG_POS_X = "X";
    private static final String TAG_POS_Y = "Y";
    private static final String TAG_POS_Z = "Z";
    private static final String TAG_SCENT_TYPE = "ScentType";
    private static final String TAG_SCENT_STRENGTH = "ScentStrength";
    private static final String TAG_CURRENT_SCENT_POS = "CurrentScentPos";
    private static final String TAG_DAILY_DIGS = "DailyDigs";
    private static final String TAG_LAST_DAY = "LastDay";
    private static final String TAG_IS_SHARING_SCENT = "IsSharingScent";
    private static final String TAG_SHARE_COOLDOWN = "ShareCooldown";

    private boolean isSniffing;
    private int sniffCooldown;
    private double excitementLevel;
    private final Map<BlockPos, ScentData> knownScents;
    private BlockPos currentScentPos;
    private int dailyDigs;
    private int lastDay;
    private boolean isSharingScent;
    private int shareCooldown;

    public SnifferComponent() {
        this.isSniffing = false;
        this.sniffCooldown = 0;
        this.excitementLevel = 0.0;
        this.knownScents = new HashMap<>();
        this.currentScentPos = null;
        this.dailyDigs = 0;
        this.lastDay = 0;
        this.isSharingScent = false;
        this.shareCooldown = 0;
    }

    /**
     * Serializes this component to NBT.
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean(TAG_IS_SNIFFING, isSniffing);
        tag.putInt(TAG_SNIFF_COOLDOWN, sniffCooldown);
        tag.putDouble(TAG_EXCITEMENT_LEVEL, excitementLevel);
        tag.putInt(TAG_DAILY_DIGS, dailyDigs);
        tag.putInt(TAG_LAST_DAY, lastDay);
        tag.putBoolean(TAG_IS_SHARING_SCENT, isSharingScent);
        tag.putInt(TAG_SHARE_COOLDOWN, shareCooldown);

        if (currentScentPos != null) {
            CompoundTag scentPosTag = new CompoundTag();
            scentPosTag.putInt(TAG_POS_X, currentScentPos.getX());
            scentPosTag.putInt(TAG_POS_Y, currentScentPos.getY());
            scentPosTag.putInt(TAG_POS_Z, currentScentPos.getZ());
            tag.put(TAG_CURRENT_SCENT_POS, scentPosTag);
        }

        CompoundTag scentsTag = new CompoundTag();
        int index = 0;
        for (Map.Entry<BlockPos, ScentData> entry : knownScents.entrySet()) {
            if (index >= 20) {
                break;
            }
            BlockPos pos = entry.getKey();
            ScentData scentData = entry.getValue();

            CompoundTag scentEntry = new CompoundTag();
            scentEntry.putInt(TAG_POS_X, pos.getX());
            scentEntry.putInt(TAG_POS_Y, pos.getY());
            scentEntry.putInt(TAG_POS_Z, pos.getZ());
            scentEntry.putString(TAG_SCENT_TYPE, scentData.scentType());
            scentEntry.putDouble(TAG_SCENT_STRENGTH, scentData.strength());

            scentsTag.put(TAG_SCENT_ENTRY + index, scentEntry);
            index++;
        }
        tag.put(TAG_KNOWN_SCENTS, scentsTag);

        return tag;
    }

    /**
     * Deserializes this component from NBT.
     */
    public static SnifferComponent fromNbt(CompoundTag tag) {
        SnifferComponent component = new SnifferComponent();

        component.isSniffing = tag.getBoolean(TAG_IS_SNIFFING);
        component.sniffCooldown = tag.getInt(TAG_SNIFF_COOLDOWN);
        component.excitementLevel = tag.getDouble(TAG_EXCITEMENT_LEVEL);
        component.dailyDigs = tag.getInt(TAG_DAILY_DIGS);
        component.lastDay = tag.getInt(TAG_LAST_DAY);
        component.isSharingScent = tag.getBoolean(TAG_IS_SHARING_SCENT);
        component.shareCooldown = tag.getInt(TAG_SHARE_COOLDOWN);

        if (tag.contains(TAG_CURRENT_SCENT_POS)) {
            CompoundTag scentPosTag = tag.getCompound(TAG_CURRENT_SCENT_POS);
            int x = scentPosTag.getInt(TAG_POS_X);
            int y = scentPosTag.getInt(TAG_POS_Y);
            int z = scentPosTag.getInt(TAG_POS_Z);
            component.currentScentPos = new BlockPos(x, y, z);
        }

        if (tag.contains(TAG_KNOWN_SCENTS)) {
            CompoundTag scentsTag = tag.getCompound(TAG_KNOWN_SCENTS);
            component.knownScents.clear();

            for (int i = 0; i < 20; i++) {
                String key = TAG_SCENT_ENTRY + i;
                if (scentsTag.contains(key)) {
                    CompoundTag scentEntry = scentsTag.getCompound(key);
                    int x = scentEntry.getInt(TAG_POS_X);
                    int y = scentEntry.getInt(TAG_POS_Y);
                    int z = scentEntry.getInt(TAG_POS_Z);
                    BlockPos pos = new BlockPos(x, y, z);

                    String scentType = scentEntry.getString(TAG_SCENT_TYPE);
                    double strength = scentEntry.getDouble(TAG_SCENT_STRENGTH);

                    component.knownScents.put(pos, new ScentData(scentType, strength));
                }
            }
        }

        return component;
    }

    public boolean isSniffing() {
        return isSniffing;
    }

    public void setSniffing(boolean sniffing) {
        isSniffing = sniffing;
    }

    public int getSniffCooldown() {
        return sniffCooldown;
    }

    public void setSniffCooldown(int cooldown) {
        this.sniffCooldown = cooldown;
    }

    public void decrementSniffCooldown() {
        if (sniffCooldown > 0) {
            sniffCooldown--;
        }
    }

    public double getExcitementLevel() {
        return excitementLevel;
    }

    public void setExcitementLevel(double excitementLevel) {
        this.excitementLevel = Math.max(0.0, Math.min(1.0, excitementLevel));
    }

    public void addExcitement(double amount) {
        this.excitementLevel = Math.max(0.0, Math.min(1.0, excitementLevel + amount));
    }

    public void decreaseExcitement(double amount) {
        this.excitementLevel = Math.max(0.0, excitementLevel - amount);
    }

    public Map<BlockPos, ScentData> getKnownScents() {
        return new HashMap<>(knownScents);
    }

    public void addScent(BlockPos pos, String scentType, double strength) {
        knownScents.put(pos.immutable(), new ScentData(scentType, strength));

        if (knownScents.size() > 20) {
            removeOldestScent();
        }
    }

    public void removeScent(BlockPos pos) {
        knownScents.remove(pos);
    }

    public void clearScents() {
        knownScents.clear();
    }

    private void removeOldestScent() {
        if (knownScents.isEmpty()) {
            return;
        }

        BlockPos oldest = null;
        double oldestStrength = Double.MAX_VALUE;

        for (Map.Entry<BlockPos, ScentData> entry : knownScents.entrySet()) {
            if (entry.getValue().strength() < oldestStrength) {
                oldest = entry.getKey();
                oldestStrength = entry.getValue().strength();
            }
        }

        if (oldest != null) {
            knownScents.remove(oldest);
        }
    }

    public BlockPos getCurrentScentPos() {
        return currentScentPos;
    }

    public void setCurrentScentPos(BlockPos pos) {
        this.currentScentPos = pos != null ? pos.immutable() : null;
    }

    public int getDailyDigs() {
        return dailyDigs;
    }

    public void setDailyDigs(int dailyDigs) {
        this.dailyDigs = Math.max(0, dailyDigs);
    }

    public void incrementDailyDigs() {
        dailyDigs++;
    }

    public int getLastDay() {
        return lastDay;
    }

    public void setLastDay(int lastDay) {
        this.lastDay = lastDay;
    }

    public boolean isSharingScent() {
        return isSharingScent;
    }

    public void setSharingScent(boolean sharingScent) {
        isSharingScent = sharingScent;
    }

    public int getShareCooldown() {
        return shareCooldown;
    }

    public void setShareCooldown(int cooldown) {
        this.shareCooldown = cooldown;
    }

    public void decrementShareCooldown() {
        if (shareCooldown > 0) {
            shareCooldown--;
        }
    }

    /**
     * Data class for scent information.
     */
    public record ScentData(String scentType, double strength) {
        public static final String SCENT_TORCHFLOWER = "torchflower";
        public static final String SCENT_PITCHER_POD = "pitcher_pod";
        public static final String SCENT_MOSS = "moss";
    }
}
