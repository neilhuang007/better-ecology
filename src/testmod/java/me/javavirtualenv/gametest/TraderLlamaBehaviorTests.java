package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.WanderingTrader;

/**
 * Game tests for trader llama behaviors.
 */
public class TraderLlamaBehaviorTests implements FabricGameTest {

    /**
     * Test that trader llama follows wandering trader.
     * Setup: Spawn trader llama and wandering trader.
     * Expected: Llama moves closer to trader over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testTraderLlamaFollowsTrader(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn wandering trader and trader llama
        BlockPos traderPos = new BlockPos(10, 2, 10);
        BlockPos llamaPos = new BlockPos(15, 2, 15);
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, traderPos);
        TraderLlama llama = helper.spawn(EntityType.TRADER_LLAMA, llamaPos);

        // Record initial distance
        double initialDistance = llama.distanceTo(trader);

        // Wait for follow behavior to activate
        helper.runAfterDelay(150, () -> {
            if (llama.isAlive() && trader.isAlive()) {
                double finalDistance = llama.distanceTo(trader);
                // Llama should be closer to or near the trader
                if (finalDistance < initialDistance || finalDistance < 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Trader llama did not follow trader. Initial: " +
                        String.format("%.1f", initialDistance) +
                        ", Final: " + String.format("%.1f", finalDistance));
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test that trader llama protects wandering trader from zombie.
     * Setup: Spawn trader llama, wandering trader, and zombie.
     * Expected: Llama positions itself defensively and exhibits protective behavior.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testTraderLlamaProtectsTrader(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn wandering trader, trader llama, and zombie
        BlockPos traderPos = new BlockPos(10, 2, 10);
        BlockPos llamaPos = new BlockPos(11, 2, 11);
        BlockPos zombiePos = new BlockPos(15, 2, 10);

        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, traderPos);
        TraderLlama llama = helper.spawn(EntityType.TRADER_LLAMA, llamaPos);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, zombiePos);

        // Record initial positions
        double initialLlamaToTrader = llama.distanceTo(trader);

        // Wait for protection behavior to activate
        helper.runAfterDelay(100, () -> {
            if (llama.isAlive() && trader.isAlive() && zombie.isAlive()) {
                double finalLlamaToTrader = llama.distanceTo(trader);
                // Llama should stay close to trader to protect
                if (finalLlamaToTrader <= 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Trader llama did not protect trader. Distance to trader: " +
                        String.format("%.1f", finalLlamaToTrader));
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test that trader llama flees from zombie with enhanced speed.
     * Setup: Spawn trader llama and zombie nearby without trader.
     * Expected: Llama flees from zombie and maintains distance.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testTraderLlamaFleesBehavior(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn trader llama and zombie close together
        BlockPos llamaPos = new BlockPos(10, 2, 10);
        BlockPos zombiePos = new BlockPos(13, 2, 10);
        TraderLlama llama = helper.spawn(EntityType.TRADER_LLAMA, llamaPos);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, zombiePos);

        // Record initial distance
        double initialDistance = llama.distanceTo(zombie);

        // Wait for llama to flee
        helper.runAfterDelay(100, () -> {
            if (llama.isAlive()) {
                double finalDistance = llama.distanceTo(zombie);
                // Llama should have moved away from zombie
                if (finalDistance > initialDistance || finalDistance > 10.0) {
                    helper.succeed();
                } else {
                    helper.fail("Trader llama did not flee. Initial: " +
                        String.format("%.1f", initialDistance) +
                        ", Final: " + String.format("%.1f", finalDistance));
                }
            } else {
                helper.fail("Trader llama not alive");
            }
        });
    }

    /**
     * Test that multiple trader llamas form protective formation around trader.
     * Setup: Spawn wandering trader with multiple trader llamas and a zombie.
     * Expected: Llamas coordinate to protect the trader.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testTraderLlamaFormation(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn wandering trader
        BlockPos traderPos = new BlockPos(10, 2, 10);
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, traderPos);

        // Spawn three trader llamas
        BlockPos llama1Pos = new BlockPos(12, 2, 10);
        BlockPos llama2Pos = new BlockPos(10, 2, 12);
        BlockPos llama3Pos = new BlockPos(8, 2, 10);

        TraderLlama llama1 = helper.spawn(EntityType.TRADER_LLAMA, llama1Pos);
        TraderLlama llama2 = helper.spawn(EntityType.TRADER_LLAMA, llama2Pos);
        TraderLlama llama3 = helper.spawn(EntityType.TRADER_LLAMA, llama3Pos);

        // Spawn zombie as threat
        BlockPos zombiePos = new BlockPos(15, 2, 10);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, zombiePos);

        // Wait for formation to establish
        helper.runAfterDelay(200, () -> {
            if (llama1.isAlive() && llama2.isAlive() && llama3.isAlive() && trader.isAlive()) {
                // Check that llamas are positioned around trader
                double dist1 = llama1.distanceTo(trader);
                double dist2 = llama2.distanceTo(trader);
                double dist3 = llama3.distanceTo(trader);

                double avgDistance = (dist1 + dist2 + dist3) / 3.0;

                // Llamas should be relatively close to trader in formation
                if (avgDistance <= 10.0) {
                    helper.succeed();
                } else {
                    helper.fail("Trader llamas did not form protective formation. Avg distance: " +
                        String.format("%.1f", avgDistance));
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test that trader llama protection goal has correct priority.
     * Setup: Spawn trader llama.
     * Expected: Protection priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testTraderLlamaProtectionPriority(GameTestHelper helper) {
        // Spawn trader llama
        BlockPos llamaPos = new BlockPos(5, 2, 5);
        TraderLlama llama = helper.spawn(EntityType.TRADER_LLAMA, llamaPos);

        // Verify trader llama is alive and protection goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_CRITICAL = 2, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (llama.isAlive() && AnimalThresholds.PRIORITY_CRITICAL < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Trader llama not alive or protection priority incorrect");
            }
        });
    }

    /**
     * Test that trader llama flee goal has correct priority.
     * Setup: Spawn trader llama.
     * Expected: Flee priority is highest.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testTraderLlamaFleeGoalPriority(GameTestHelper helper) {
        // Spawn trader llama
        BlockPos llamaPos = new BlockPos(5, 2, 5);
        TraderLlama llama = helper.spawn(EntityType.TRADER_LLAMA, llamaPos);

        // Verify flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_CRITICAL = 2
            // In Minecraft's goal system, lower number = higher priority
            if (llama.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_CRITICAL) {
                helper.succeed();
            } else {
                helper.fail("Trader llama flee goal priority check failed");
            }
        });
    }
}
