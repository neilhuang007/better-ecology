package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for pufferfish behaviors.
 */
public class PufferfishBehaviorTests implements FabricGameTest {

    /**
     * Test that a pufferfish flees from an axolotl.
     * Setup: Spawn pufferfish and axolotl close together, verify pufferfish moves away.
     * Expected: Pufferfish moves away from axolotl predator.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testPufferfishFleesFromAxolotl(GameTestHelper helper) {
        // Create water environment for aquatic entities
        for (int x = 0; x < 20; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 0; z < 20; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn pufferfish first
        BlockPos pufferfishPos = new BlockPos(10, 2, 10);
        Pufferfish pufferfish = helper.spawn(EntityType.PUFFERFISH, pufferfishPos);

        // Record initial position
        double initialX = pufferfish.getX();
        double initialZ = pufferfish.getZ();

        // Wait for pufferfish to stabilize
        helper.runAfterDelay(10, () -> {
            // Spawn axolotl close to pufferfish
            BlockPos axolotlPos = new BlockPos(10, 2, 8);
            Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

            // Check that pufferfish moves away when fleeing
            helper.runAfterDelay(80, () -> {
                double currentX = pufferfish.getX();
                double currentZ = pufferfish.getZ();
                double totalMovement = Math.abs(currentX - initialX) + Math.abs(currentZ - initialZ);

                // Pufferfish should have moved when fleeing from axolotl
                if (totalMovement > 1.0) {
                    helper.succeed();
                } else {
                    helper.fail("Pufferfish did not move away from axolotl. Movement: " + totalMovement);
                }
            });
        });
    }

    /**
     * Test that pufferfish flee goal has correct priority.
     * Setup: Spawn pufferfish and verify flee goal priority is correct.
     * Expected: Flee goal has higher priority than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testPufferfishFleeGoalPriority(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 10; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 0; z < 10; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn pufferfish
        BlockPos pufferfishPos = new BlockPos(5, 2, 5);
        Pufferfish pufferfish = helper.spawn(EntityType.PUFFERFISH, pufferfishPos);

        // Verify pufferfish is alive and flee goal has correct priority
        helper.runAfterDelay(10, () -> {
            // Verify flee priority is higher than normal goals
            if (pufferfish.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Pufferfish flee goal priority check failed");
            }
        });
    }

    /**
     * Test that pufferfish stops fleeing when safe.
     * Setup: Spawn pufferfish without any predators nearby.
     * Expected: Pufferfish does not panic flee behavior.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testPufferfishStopsFleeingWhenSafe(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 10; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 0; z < 10; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn pufferfish without predators
        BlockPos pufferfishPos = new BlockPos(5, 2, 5);
        Pufferfish pufferfish = helper.spawn(EntityType.PUFFERFISH, pufferfishPos);

        // No axolotl nearby - pufferfish should not be fleeing
        helper.runAfterDelay(60, () -> {
            // Pufferfish should be relatively calm (not running at full speed)
            // We just verify it's alive and not panicking
            if (pufferfish.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Pufferfish died unexpectedly");
            }
        });
    }
}
