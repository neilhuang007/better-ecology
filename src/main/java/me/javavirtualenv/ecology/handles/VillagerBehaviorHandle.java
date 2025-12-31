package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.villager.*;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.villager.VillagerMixin;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;

/**
 * Handle for registering villager-specific behavior goals.
 */
public class VillagerBehaviorHandle implements EcologyHandle {

    @Override
    public String id() {
        return "villager_behavior";
    }

    @Override
    public void initialize(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Villager villager)) {
            return;
        }

        // Get behavior systems from mixin
        TradingReputation tradingReputation = VillagerMixin.getTradingReputation(villager);
        GossipSystem gossipSystem = VillagerMixin.getGossipSystem(villager);
        WorkStationAI workStationAI = VillagerMixin.getWorkStationAI(villager);
        DailyRoutine dailyRoutine = VillagerMixin.getDailyRoutine(villager);
        EnhancedFarming enhancedFarming = VillagerMixin.getEnhancedFarming(villager);

        if (tradingReputation == null || gossipSystem == null || workStationAI == null ||
            dailyRoutine == null || enhancedFarming == null) {
            return;
        }

        // Register custom goals
        registerGoals(villager, tradingReputation, gossipSystem, dailyRoutine, enhancedFarming);
    }

    /**
     * Registers villager behavior goals.
     */
    private void registerGoals(
        Villager villager,
        TradingReputation tradingReputation,
        GossipSystem gossipSystem,
        DailyRoutine dailyRoutine,
        EnhancedFarming enhancedFarming
    ) {
        // Register enhanced trading goal
        var tradingGoalSelector = villager.goalSelector;
        tradingGoalSelector.addGoal(
            2, // High priority during trading
            new EnhancedTradingGoal(villager, tradingReputation, gossipSystem)
        );

        // Register socialize goal
        tradingGoalSelector.addGoal(
            5, // Medium priority
            new SocializeGoal(villager, gossipSystem, dailyRoutine)
        );

        // Note: Work and farming behaviors are handled through tick updates,
        // not goals, as they are continuous behaviors
    }

    @Override
    public void tick(Mob mob, EcologyComponent component) {
        // Villager behaviors are ticked through the mixin, not here
    }

    @Override
    public List<String> dependencies() {
        return List.of(); // No dependencies
    }
}
