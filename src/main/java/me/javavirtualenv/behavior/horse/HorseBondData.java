package me.javavirtualenv.behavior.horse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores bonding data between a horse and players.
 * Bond levels improve horse obedience and speed.
 */
public class HorseBondData {
    private static final String NBT_BONDS = "bonds";
    private static final String NBT_PLAYER_ID = "id";
    private static final String NBT_BOND_LEVEL = "level";
    private static final String NBT_RIDE_COUNT = "rides";
    private static final String NBT_LAST_RIDE_TIME = "last_ride";
    private static final String NBT_INTERACTION_COUNT = "interactions";

    private final Map<UUID, BondInfo> playerBonds = new HashMap<>();
    private UUID primaryBondedPlayer;
    private int maxBondLevel = 100;
    private int bondThresholds = 25;

    public int getBondLevel(Player player) {
        if (player == null) {
            return 0;
        }
        BondInfo info = playerBonds.get(player.getUUID());
        return info != null ? info.level : 0;
    }

    public void addBondExperience(Player player, int amount) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        BondInfo info = playerBonds.computeIfAbsent(playerId, k -> new BondInfo());
        info.level = Math.min(maxBondLevel, info.level + amount);
        info.interactionCount++;

        // Update primary bonded player if this is the highest bond
        if (primaryBondedPlayer == null || info.level > getBondLevel(primaryBondedPlayer)) {
            primaryBondedPlayer = playerId;
        }
    }

    public void recordRide(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        BondInfo info = playerBonds.computeIfAbsent(playerId, k -> new BondInfo());
        info.rideCount++;
        info.lastRideTime = System.currentTimeMillis();

        // Riding gives bonus bond experience
        addBondExperience(player, 2);
    }

    public int getRideCount(Player player) {
        if (player == null) {
            return 0;
        }
        BondInfo info = playerBonds.get(player.getUUID());
        return info != null ? info.rideCount : 0;
    }

    public UUID getPrimaryBondedPlayer() {
        return primaryBondedPlayer;
    }

    public boolean isBonded(Player player) {
        if (player == null) {
            return false;
        }
        return getBondLevel(player) >= bondThresholds;
    }

    public boolean isStronglyBonded(Player player) {
        if (player == null) {
            return false;
        }
        return getBondLevel(player) >= maxBondLevel * 0.7;
    }

    public float getSpeedBonus(Player player) {
        int bondLevel = getBondLevel(player);
        // Max 20% speed bonus at full bond
        return (bondLevel / (float) maxBondLevel) * 0.2f;
    }

    public float getJumpBonus(Player player) {
        int bondLevel = getBondLevel(player);
        // Max 15% jump height bonus at full bond
        return (bondLevel / (float) maxBondLevel) * 0.15f;
    }

    public float getObedienceChance(Player player) {
        int bondLevel = getBondLevel(player);
        // Higher bond = better obedience
        // Base 50% obedience, up to 95% at max bond
        return 0.5f + (bondLevel / (float) maxBondLevel) * 0.45f;
    }

    public void saveToNbt(CompoundTag tag) {
        CompoundTag bondsTag = new CompoundTag();
        int index = 0;

        for (Map.Entry<UUID, BondInfo> entry : playerBonds.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(NBT_PLAYER_ID, entry.getKey());
            playerTag.putInt(NBT_BOND_LEVEL, entry.getValue().level);
            playerTag.putInt(NBT_RIDE_COUNT, entry.getValue().rideCount);
            playerTag.putLong(NBT_LAST_RIDE_TIME, entry.getValue().lastRideTime);
            playerTag.putInt(NBT_INTERACTION_COUNT, entry.getValue().interactionCount);
            bondsTag.put("bond_" + index, playerTag);
            index++;
        }

        tag.put(NBT_BONDS, bondsTag);
        if (primaryBondedPlayer != null) {
            tag.putUUID("primary_bond", primaryBondedPlayer);
        }
    }

    public void loadFromNbt(CompoundTag tag) {
        playerBonds.clear();

        if (tag.contains(NBT_BONDS)) {
            CompoundTag bondsTag = tag.getCompound(NBT_BONDS);
            int index = 0;

            while (bondsTag.contains("bond_" + index)) {
                CompoundTag playerTag = bondsTag.getCompound("bond_" + index);
                UUID playerId = playerTag.getUUID(NBT_PLAYER_ID);
                BondInfo info = new BondInfo();
                info.level = playerTag.getInt(NBT_BOND_LEVEL);
                info.rideCount = playerTag.getInt(NBT_RIDE_COUNT);
                info.lastRideTime = playerTag.getLong(NBT_LAST_RIDE_TIME);
                info.interactionCount = playerTag.getInt(NBT_INTERACTION_COUNT);
                playerBonds.put(playerId, info);
                index++;
            }
        }

        if (tag.hasUUID("primary_bond")) {
            primaryBondedPlayer = tag.getUUID("primary_bond");
        }
    }

    private int getBondLevel(UUID playerId) {
        BondInfo info = playerBonds.get(playerId);
        return info != null ? info.level : 0;
    }

    private static class BondInfo {
        int level = 0;
        int rideCount = 0;
        long lastRideTime = 0;
        int interactionCount = 0;
    }
}
