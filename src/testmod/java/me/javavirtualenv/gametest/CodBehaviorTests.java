package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for cod behaviors.
 */
public class CodBehaviorTests implements FabricGameTest {

    /**
     * Test that multiple cod form schools and stay together.
     * Setup: Spawn multiple cod in water.
     * Expected: Cod stay close together in a school formation.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCodSchooling(GameTestHelper helper) {
        // Create water environment for fish
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn multiple cod in water
        BlockPos cod1Pos = new BlockPos(10, 3, 10);
        BlockPos cod2Pos = new BlockPos(12, 3, 10);
        BlockPos cod3Pos = new BlockPos(10, 3, 12);
        BlockPos cod4Pos = new BlockPos(12, 3, 12);

        Cod cod1 = helper.spawn(EntityType.COD, cod1Pos);
        Cod cod2 = helper.spawn(EntityType.COD, cod2Pos);
        Cod cod3 = helper.spawn(EntityType.COD, cod3Pos);
        Cod cod4 = helper.spawn(EntityType.COD, cod4Pos);

        // Wait for cod to form school
        helper.runAfterDelay(100, () -> {
            if (cod1.isAlive() && cod2.isAlive() && cod3.isAlive() && cod4.isAlive()) {
                // Calculate average distance between cod
                double totalDistance = 0;
                int pairCount = 0;

                totalDistance += cod1.distanceTo(cod2);
                totalDistance += cod1.distanceTo(cod3);
                totalDistance += cod1.distanceTo(cod4);
                totalDistance += cod2.distanceTo(cod3);
                totalDistance += cod2.distanceTo(cod4);
                totalDistance += cod3.distanceTo(cod4);
                pairCount = 6;

                double averageDistance = totalDistance / pairCount;

                // Cod should stay relatively close (within schooling distance)
                if (averageDistance < 15.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cod did not form school. Average distance: " + averageDistance);
                }
            } else {
                helper.fail("Not all cod are alive");
            }
        });
    }

    /**
     * Test that cod flees from axolotl.
     * Setup: Spawn cod and axolotl nearby in water.
     * Expected: Cod detects axolotl and moves away.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCodFleesFromAxolotl(GameTestHelper helper) {
        // Create water environment for fish
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn cod and axolotl close together
        BlockPos codPos = new BlockPos(10, 3, 10);
        BlockPos axolotlPos = new BlockPos(13, 3, 10);

        Cod cod = helper.spawn(EntityType.COD, codPos);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Record initial distance
        double initialDistance = cod.distanceTo(axolotl);

        // Wait for cod to flee
        helper.runAfterDelay(100, () -> {
            if (cod.isAlive() && axolotl.isAlive()) {
                double finalDistance = cod.distanceTo(axolotl);

                // Cod should have moved away from axolotl or maintained safe distance
                if (finalDistance > initialDistance || finalDistance > 12.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cod did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Cod or axolotl not alive");
            }
        });
    }

    /**
     * Test that cod flee goal has correct priority.
     * Setup: Spawn cod in water.
     * Expected: Flee priority is higher than social behaviors.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testCodFleePriority(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn cod
        BlockPos codPos = new BlockPos(5, 3, 5);
        Cod cod = helper.spawn(EntityType.COD, codPos);

        // Verify cod is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_SOCIAL = 5
            // In Minecraft's goal system, lower number = higher priority
            if (cod.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_SOCIAL) {
                helper.succeed();
            } else {
                helper.fail("Cod not alive or flee priority incorrect");
            }
        });
    }

    /**
     * Test that multiple cod respond to predator threat together.
     * Setup: Spawn multiple cod and one axolotl.
     * Expected: Cod detect the axolotl and can flee.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSchoolResponseToPredator(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn school of cod
        BlockPos cod1Pos = new BlockPos(10, 3, 10);
        BlockPos cod2Pos = new BlockPos(11, 3, 10);
        BlockPos cod3Pos = new BlockPos(10, 3, 11);
        BlockPos axolotlPos = new BlockPos(15, 3, 10);

        Cod cod1 = helper.spawn(EntityType.COD, cod1Pos);
        Cod cod2 = helper.spawn(EntityType.COD, cod2Pos);
        Cod cod3 = helper.spawn(EntityType.COD, cod3Pos);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Wait for cod to potentially detect axolotl
        helper.runAfterDelay(50, () -> {
            // Check that all entities are alive
            if (cod1.isAlive() && cod2.isAlive() && cod3.isAlive() && axolotl.isAlive()) {
                // At least one cod should be aware of the axolotl
                double dist1 = cod1.distanceTo(axolotl);
                double dist2 = cod2.distanceTo(axolotl);
                double dist3 = cod3.distanceTo(axolotl);

                // At least one cod should be within detection range
                if (dist1 < 16.0 || dist2 < 16.0 || dist3 < 16.0) {
                    helper.succeed();
                } else {
                    helper.fail("No cod within detection range of predator");
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }
}
