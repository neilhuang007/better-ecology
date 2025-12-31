package me.javavirtualenv.behavior.villager;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Goal for enhanced trading behavior with dynamic pricing.
 */
public class EnhancedTradingGoal extends Goal {
    private final Villager villager;
    private final TradingReputation tradingReputation;
    private final GossipSystem gossipSystem;

    private Player currentCustomer;
    private int tradeDelay = 0;

    public EnhancedTradingGoal(Villager villager, TradingReputation tradingReputation, GossipSystem gossipSystem) {
        this.villager = villager;
        this.tradingReputation = tradingReputation;
        this.gossipSystem = gossipSystem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only active when trading
        return villager.isTrading() && tradeDelay == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return villager.isTrading() && currentCustomer != null && currentCustomer.isAlive();
    }

    @Override
    public void start() {
        currentCustomer = villager.getTradingPlayer();
    }

    @Override
    public void stop() {
        currentCustomer = null;
        tradeDelay = 40; // 2 second cooldown
    }

    @Override
    public void tick() {
        if (currentCustomer == null) {
            return;
        }

        // Update gossip based on trading reputation
        updateGossipFromTrading();

        // Check for special deal opportunities
        checkForSpecialDeals();
    }

    /**
     * Updates gossip based on trading relationship.
     */
    private void updateGossipFromTrading() {
        UUID playerId = currentCustomer.getUUID();
        TradingReputation.CustomerTier tier = tradingReputation.getCustomerTier(playerId);

        // Spread positive gossip about good customers
        if (tier == TradingReputation.CustomerTier.VIP) {
            if (villager.getRandom().nextDouble() < 0.01) {
                gossipSystem.addGossip(
                    GossipSystem.GossipType.MAJOR_POSITIVE,
                    playerId,
                    10
                );
            }
        } else if (tier == TradingReputation.CustomerTier.REGULAR) {
            if (villager.getRandom().nextDouble() < 0.005) {
                gossipSystem.addGossip(
                    GossipSystem.GossipType.MINOR_POSITIVE,
                    playerId,
                    5
                );
            }
        }
    }

    /**
     * Checks for special deal opportunities.
     */
    private void checkForSpecialDeals() {
        if (!villager.getRandom().nextBoolean()) {
            return;
        }

        // Occasional rare deals
        if (tradingReputation.hasSpecialDeals(currentCustomer.getUUID())) {
            // 1% chance for special deal
            if (villager.getRandom().nextDouble() < 0.01) {
                // Trigger special deal (would modify merchant recipes)
                triggerSpecialDeal();
            }
        }
    }

    /**
     * Triggers a special deal for the customer.
     */
    private void triggerSpecialDeal() {
        // This would add a temporary discounted trade
        // Implementation would modify the merchant's recipe list
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }
}
