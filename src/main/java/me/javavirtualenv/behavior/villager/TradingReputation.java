package me.javavirtualenv.behavior.villager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages trading reputation between a villager and players.
 * Reputation affects prices and unlocks special trades.
 */
public class TradingReputation {
    private final Villager villager;
    private final Map<UUID, PlayerReputation> playerReputations = new HashMap<>();

    public TradingReputation(Villager villager) {
        this.villager = villager;
    }

    /**
     * Gets or creates reputation data for a player.
     */
    public PlayerReputation getOrCreatePlayerReputation(UUID playerId) {
        return playerReputations.computeIfAbsent(playerId, id -> new PlayerReputation());
    }

    /**
     * Gets existing reputation for a player, or null if none exists.
     */
    public PlayerReputation getPlayerReputation(UUID playerId) {
        return playerReputations.get(playerId);
    }

    /**
     * Records a trade with a player.
     */
    public void recordTrade(UUID playerId, boolean wasSuccessful, int emeraldValue) {
        PlayerReputation reputation = getOrCreatePlayerReputation(playerId);
        reputation.totalTrades++;
        if (wasSuccessful) {
            reputation.successfulTrades++;
            reputation.reputation += Math.min(5, emeraldValue / 10); // +1 rep per 10 emeralds, max 5
            reputation.totalEmeraldsTraded += emeraldValue;
        }
        reputation.lastTradeTime = villager.level().getGameTime();
    }

    /**
     * Gets the price multiplier for a player based on reputation.
     * Higher reputation = lower prices (multiplier < 1.0)
     * Lower reputation = higher prices (multiplier > 1.0)
     */
    public float getPriceMultiplier(UUID playerId) {
        PlayerReputation reputation = getPlayerReputation(playerId);
        if (reputation == null) {
            return 1.0f;
        }

        // Base multiplier starts at 1.0
        float multiplier = 1.0f;

        // Reputation reduces price: 100 rep = 20% discount (0.8x)
        multiplier -= Math.min(0.3f, reputation.reputation / 500.0f);

        // Frequent customer bonus: 50+ trades = 10% additional discount
        if (reputation.totalTrades >= 50) {
            multiplier -= 0.1f;
        } else if (reputation.totalTrades >= 20) {
            multiplier -= 0.05f;
        }

        // High spender bonus: 1000+ emeralds = 5% additional discount
        if (reputation.totalEmeraldsTraded >= 1000) {
            multiplier -= 0.05f;
        }

        return Math.max(0.5f, multiplier); // Minimum 50% of base price
    }

    /**
     * Checks if a player qualifies for special deals.
     */
    public boolean hasSpecialDeals(UUID playerId) {
        PlayerReputation reputation = getPlayerReputation(playerId);
        if (reputation == null) {
            return false;
        }

        // Unlock special trades at 100 reputation and 20 trades
        return reputation.reputation >= 100 && reputation.totalTrades >= 20;
    }

    /**
     * Gets the customer tier for a player.
     */
    public CustomerTier getCustomerTier(UUID playerId) {
        PlayerReputation reputation = getPlayerReputation(playerId);
        if (reputation == null) {
            return CustomerTier.STRANGER;
        }

        if (reputation.reputation >= 300 && reputation.totalTrades >= 50) {
            return CustomerTier.VIP;
        } else if (reputation.reputation >= 100 && reputation.totalTrades >= 20) {
            return CustomerTier.REGULAR;
        } else if (reputation.reputation >= 20 && reputation.totalTrades >= 5) {
            return CustomerTier.CASUAL;
        }
        return CustomerTier.STRANGER;
    }

    /**
     * Serializes reputation data to NBT.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        playerReputations.forEach((uuid, rep) -> {
            CompoundTag playerTag = rep.save();
            tag.put(uuid.toString(), playerTag);
        });
        return tag;
    }

    /**
     * Loads reputation data from NBT.
     */
    public void load(CompoundTag tag) {
        playerReputations.clear();
        tag.getAllKeys().forEach(key -> {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerReputation rep = new PlayerReputation();
                rep.load(tag.getCompound(key));
                playerReputations.put(uuid, rep);
            } catch (IllegalArgumentException e) {
                // Invalid UUID, skip
            }
        });
    }

    /**
     * Reputation data for a single player.
     */
    public static class PlayerReputation {
        private int reputation = 0;
        private int totalTrades = 0;
        private int successfulTrades = 0;
        private int totalEmeraldsTraded = 0;
        private long lastTradeTime = 0;
        private long firstTradeTime = 0;

        public int getReputation() {
            return reputation;
        }

        public int getTotalTrades() {
            return totalTrades;
        }

        public int getSuccessfulTrades() {
            return successfulTrades;
        }

        public float getSuccessRate() {
            return totalTrades > 0 ? (float) successfulTrades / totalTrades : 0;
        }

        public int getTotalEmeraldsTraded() {
            return totalEmeraldsTraded;
        }

        public long getLastTradeTime() {
            return lastTradeTime;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Reputation", reputation);
            tag.putInt("TotalTrades", totalTrades);
            tag.putInt("SuccessfulTrades", successfulTrades);
            tag.putInt("TotalEmeraldsTraded", totalEmeraldsTraded);
            tag.putLong("LastTradeTime", lastTradeTime);
            tag.putLong("FirstTradeTime", firstTradeTime);
            return tag;
        }

        public void load(CompoundTag tag) {
            reputation = tag.getInt("Reputation");
            totalTrades = tag.getInt("TotalTrades");
            successfulTrades = tag.getInt("SuccessfulTrades");
            totalEmeraldsTraded = tag.getInt("TotalEmeraldsTraded");
            lastTradeTime = tag.getLong("LastTradeTime");
            firstTradeTime = tag.getLong("FirstTradeTime");
        }
    }

    /**
     * Customer tiers for trading benefits.
     */
    public enum CustomerTier {
        STRANGER("stranger", 1.0f),
        CASUAL("casual", 0.95f),
        REGULAR("regular", 0.85f),
        VIP("vip", 0.75f);

        private final String name;
        private final float baseDiscount;

        CustomerTier(String name, float baseDiscount) {
            this.name = name;
            this.baseDiscount = baseDiscount;
        }

        public String getName() {
            return name;
        }

        public float getBaseDiscount() {
            return baseDiscount;
        }
    }
}
