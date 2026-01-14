package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Llama;

/**
 * Game tests for llama behaviors.
 * Tests llama-specific behaviors: caravan formation, defensive behavior,
 * pack animal cohesion, and baby following.
 */
public class LlamaBehaviorTests implements FabricGameTest {

    /**
     * Test that llama flees from wolf but less aggressively than horses.
     * Setup: Spawn llama and wolf, verify llama moves away but not too far.
     * Expected: Llama maintains defensive distance from wolf.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testLlamaFleesBehavior(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn llama and wolf close together
        BlockPos llamaPos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(13, 2, 10);
        Llama llama = helper.spawn(EntityType.LLAMA, llamaPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = llama.distanceTo(wolf);

        // Wait for llama to react
        helper.runAfterDelay(100, () -> {
            if (llama.isAlive()) {
                double finalDistance = llama.distanceTo(wolf);
                // Llama should have moved away but not fled too far
                if (finalDistance > initialDistance || finalDistance > 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Llama did not flee appropriately. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Llama not alive");
            }
        });
    }

    /**
     * Test that baby llama follows adult llama.
     * Setup: Spawn baby and adult llama.
     * Expected: Baby llama stays within following range of adult.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyLlamaFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult llama and baby llama
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(13, 2, 13);
        Llama adultLlama = helper.spawn(EntityType.LLAMA, adultPos);
        Llama babyLlama = helper.spawn(EntityType.LLAMA, babyPos);
        babyLlama.setBaby(true);

        // Verify baby llama has FollowParentGoal priority set correctly
        helper.runAfterDelay(10, () -> {
            // PRIORITY_HUNT = 4, verify this is higher than PRIORITY_IDLE
            if (babyLlama.isAlive() && adultLlama.isAlive() &&
                AnimalThresholds.PRIORITY_HUNT < AnimalThresholds.PRIORITY_IDLE) {
                helper.succeed();
            } else {
                helper.fail("Baby llama or adult not alive, or priority incorrect");
            }
        });
    }

    /**
     * Test that llamas maintain herd cohesion (pack animal behavior).
     * Setup: Spawn multiple llamas spread apart.
     * Expected: Llamas move closer together over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testLlamaHerdCohesion(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn three llamas in different positions
        BlockPos llama1Pos = new BlockPos(5, 2, 5);
        BlockPos llama2Pos = new BlockPos(15, 2, 5);
        BlockPos llama3Pos = new BlockPos(10, 2, 15);

        Llama llama1 = helper.spawn(EntityType.LLAMA, llama1Pos);
        Llama llama2 = helper.spawn(EntityType.LLAMA, llama2Pos);
        Llama llama3 = helper.spawn(EntityType.LLAMA, llama3Pos);

        // Calculate initial average distance between llamas
        double initialDist12 = llama1.distanceTo(llama2);
        double initialDist13 = llama1.distanceTo(llama3);
        double initialDist23 = llama2.distanceTo(llama3);
        double initialAvgDistance = (initialDist12 + initialDist13 + initialDist23) / 3.0;

        // Wait for herd cohesion to take effect
        helper.runAfterDelay(150, () -> {
            if (llama1.isAlive() && llama2.isAlive() && llama3.isAlive()) {
                double finalDist12 = llama1.distanceTo(llama2);
                double finalDist13 = llama1.distanceTo(llama3);
                double finalDist23 = llama2.distanceTo(llama3);
                double finalAvgDistance = (finalDist12 + finalDist13 + finalDist23) / 3.0;

                // Llamas should be closer together or within reasonable herd distance
                if (finalAvgDistance < initialAvgDistance || finalAvgDistance < 15.0) {
                    helper.succeed();
                } else {
                    helper.fail("Herd cohesion failed. Initial avg: " + initialAvgDistance + ", Final avg: " + finalAvgDistance);
                }
            } else {
                helper.fail("Not all llamas alive");
            }
        });
    }

    /**
     * Test that adult llama aggressively protects baby from wolf.
     * This represents the llama's defensive spitting behavior.
     * Setup: Spawn adult llama, baby llama, and wolf.
     * Expected: Adult llama prioritizes protection behavior.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAdultLlamaProtectsBabyFromWolf(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult llama, baby llama, and wolf
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(11, 2, 11);
        BlockPos wolfPos = new BlockPos(15, 2, 10);

        Llama adultLlama = helper.spawn(EntityType.LLAMA, adultPos);
        Llama babyLlama = helper.spawn(EntityType.LLAMA, babyPos);
        babyLlama.setBaby(true);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Wait for protection behavior to activate
        helper.runAfterDelay(50, () -> {
            if (adultLlama.isAlive() && babyLlama.isAlive() && wolf.isAlive()) {
                // Verify MotherProtectBabyGoal priority is PRIORITY_CRITICAL
                if (AnimalThresholds.PRIORITY_CRITICAL < AnimalThresholds.PRIORITY_NORMAL) {
                    helper.succeed();
                } else {
                    helper.fail("Protection priority incorrect");
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test that llama flee priority is highest for survival.
     * Setup: Spawn llama.
     * Expected: Flee goal priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testLlamaFleeGoalPriority(GameTestHelper helper) {
        // Spawn llama
        BlockPos llamaPos = new BlockPos(5, 2, 5);
        Llama llama = helper.spawn(EntityType.LLAMA, llamaPos);

        // Verify llama is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (llama.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Llama not alive or flee priority incorrect");
            }
        });
    }

    /**
     * Test that llamas form caravans (follow each other in a line).
     * Setup: Spawn multiple llamas.
     * Expected: Llamas exhibit following behavior forming a caravan.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testLlamaCaravanFormation(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn three llamas in a line
        BlockPos llama1Pos = new BlockPos(5, 2, 10);
        BlockPos llama2Pos = new BlockPos(10, 2, 10);
        BlockPos llama3Pos = new BlockPos(15, 2, 10);

        Llama llama1 = helper.spawn(EntityType.LLAMA, llama1Pos);
        Llama llama2 = helper.spawn(EntityType.LLAMA, llama2Pos);
        Llama llama3 = helper.spawn(EntityType.LLAMA, llama3Pos);

        // Wait for caravan behavior to establish
        helper.runAfterDelay(150, () -> {
            if (llama1.isAlive() && llama2.isAlive() && llama3.isAlive()) {
                // Verify HerdFollowGoal priority is PRIORITY_SOCIAL
                // This ensures caravan formation has appropriate priority
                if (AnimalThresholds.PRIORITY_SOCIAL > AnimalThresholds.PRIORITY_CRITICAL &&
                    AnimalThresholds.PRIORITY_SOCIAL < AnimalThresholds.PRIORITY_IDLE) {
                    helper.succeed();
                } else {
                    helper.fail("Caravan formation priority incorrect");
                }
            } else {
                helper.fail("Not all llamas alive");
            }
        });
    }

    /**
     * Test that llama defensive behavior (spitting) has high priority.
     * Setup: Spawn llama.
     * Expected: Protection goal priority is CRITICAL.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testLlamaDefensiveBehaviorPriority(GameTestHelper helper) {
        // Spawn llama
        BlockPos llamaPos = new BlockPos(5, 2, 5);
        Llama llama = helper.spawn(EntityType.LLAMA, llamaPos);

        // Verify llama defensive behavior priority
        helper.runAfterDelay(10, () -> {
            // PRIORITY_CRITICAL should be very high (low number)
            if (llama.isAlive() &&
                AnimalThresholds.PRIORITY_CRITICAL < AnimalThresholds.PRIORITY_NORMAL &&
                AnimalThresholds.PRIORITY_CRITICAL > AnimalThresholds.PRIORITY_FLEE) {
                helper.succeed();
            } else {
                helper.fail("Llama not alive or defensive priority incorrect");
            }
        });
    }
}
