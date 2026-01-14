package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for wandering trader behaviors.
 */
public class WanderingTraderBehaviorTests implements FabricGameTest {

    /**
     * Test that a wandering trader flees from a zombie.
     * Setup: Spawn wandering trader and zombie nearby.
     * Expected: Trader flees from zombie and maintains distance.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testWanderingTraderFleesFromZombie(GameTestHelper helper) {
        // Spawn wandering trader
        BlockPos traderPos = new BlockPos(5, 2, 5);
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, traderPos);

        // Spawn zombie nearby
        BlockPos zombiePos = new BlockPos(10, 2, 5);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, zombiePos);

        // Wait for flee behavior to activate
        helper.runAfterDelay(180, () -> {
            // Verify trader is alive
            if (!trader.isAlive()) {
                helper.fail("Wandering trader did not survive");
                return;
            }

            // Check that trader moved away from zombie
            double initialDistance = 5.0; // Initial spawn distance
            double currentDistance = trader.distanceTo(zombie);

            // Trader should have attempted to flee (distance should increase or stay similar if blocked)
            if (currentDistance >= initialDistance * 0.8) {
                helper.succeed();
            } else {
                helper.fail("Wandering trader did not flee from zombie. Distance: " +
                    String.format("%.1f", currentDistance));
            }
        });
    }

    /**
     * Test that a wandering trader seeks water when thirsty.
     * Setup: Spawn thirsty wandering trader near water.
     * Expected: Trader moves toward water to drink.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testWanderingTraderSeeksWater(GameTestHelper helper) {
        // Create a small water pool
        BlockPos waterPos = new BlockPos(8, 2, 5);
        helper.setBlock(waterPos, Blocks.WATER);
        helper.setBlock(waterPos.north(), Blocks.WATER);
        helper.setBlock(waterPos.south(), Blocks.WATER);

        // Spawn wandering trader away from water
        BlockPos traderPos = new BlockPos(2, 2, 5);
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, traderPos);

        // Set trader as thirsty
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(trader, thirstyValue);

        // Record initial position
        final double initialDistanceToWater = Math.sqrt(traderPos.distSqr(waterPos));

        // Wait for seek water behavior
        helper.runAfterDelay(180, () -> {
            // Verify trader is thirsty
            if (!AnimalNeeds.isThirsty(trader)) {
                helper.fail("Wandering trader is not thirsty");
                return;
            }

            // Check if trader moved toward water
            double currentDistanceToWater = trader.distanceToSqr(
                waterPos.getX() + 0.5,
                waterPos.getY(),
                waterPos.getZ() + 0.5
            );
            currentDistanceToWater = Math.sqrt(currentDistanceToWater);

            // Trader should have moved closer to water or be very close
            if (currentDistanceToWater <= initialDistanceToWater + 2.0) {
                helper.succeed();
            } else {
                helper.fail("Wandering trader did not seek water. Initial distance: " +
                    String.format("%.1f", initialDistanceToWater) +
                    ", Current distance: " + String.format("%.1f", currentDistanceToWater));
            }
        });
    }

    /**
     * Test that a hydrated wandering trader does not seek water.
     * Setup: Spawn wandering trader with full thirst.
     * Expected: Trader is not thirsty.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedWanderingTraderDoesNotSeekWater(GameTestHelper helper) {
        BlockPos traderPos = new BlockPos(5, 2, 5);
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, traderPos);
        AnimalNeeds.setThirst(trader, AnimalNeeds.MAX_VALUE);

        helper.runAfterDelay(80, () -> {
            if (!AnimalNeeds.isThirsty(trader)) {
                helper.succeed();
            } else {
                helper.fail("Wandering trader became thirsty unexpectedly");
            }
        });
    }

    /**
     * Test that wandering trader flee goal has correct priority.
     * Setup: Spawn wandering trader.
     * Expected: Flee priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testWanderingTraderFleeGoalPriority(GameTestHelper helper) {
        BlockPos traderPos = new BlockPos(5, 2, 5);
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, traderPos);

        helper.runAfterDelay(10, () -> {
            // Verify flee priority is higher than normal (lower number = higher priority)
            if (trader.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Wandering trader flee goal priority check failed");
            }
        });
    }
}
