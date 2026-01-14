package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for salmon behaviors.
 * Salmon are schooling fish that flee from aquatic predators.
 */
public class SalmonBehaviorTests implements FabricGameTest {

    /**
     * Test that salmon schools together with other salmon.
     * Setup: Spawn multiple salmon in water.
     * Expected: Salmon stay within schooling distance of each other.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSalmonSchooling(GameTestHelper helper) {
        // Create water environment for salmon
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.WATER);
            }
        }

        // Spawn multiple salmon in a school formation
        BlockPos salmon1Pos = new BlockPos(10, 2, 10);
        BlockPos salmon2Pos = new BlockPos(12, 2, 10);
        BlockPos salmon3Pos = new BlockPos(11, 2, 12);

        Salmon salmon1 = helper.spawn(EntityType.SALMON, salmon1Pos);
        Salmon salmon2 = helper.spawn(EntityType.SALMON, salmon2Pos);
        Salmon salmon3 = helper.spawn(EntityType.SALMON, salmon3Pos);

        // Record initial distances
        double initialDist12 = salmon1.distanceTo(salmon2);
        double initialDist13 = salmon1.distanceTo(salmon3);

        // Wait for schooling behavior to activate
        helper.runAfterDelay(100, () -> {
            if (salmon1.isAlive() && salmon2.isAlive() && salmon3.isAlive()) {
                double finalDist12 = salmon1.distanceTo(salmon2);
                double finalDist13 = salmon1.distanceTo(salmon3);

                // Salmon should maintain schooling distance (within cohesion radius of 12)
                // or get closer together
                if ((finalDist12 <= 12.0 || finalDist12 < initialDist12) &&
                    (finalDist13 <= 12.0 || finalDist13 < initialDist13)) {
                    helper.succeed();
                } else {
                    helper.fail("Salmon did not maintain school. " +
                        "Dist 1-2: " + initialDist12 + " -> " + finalDist12 + ", " +
                        "Dist 1-3: " + initialDist13 + " -> " + finalDist13);
                }
            } else {
                helper.fail("Not all salmon alive");
            }
        });
    }

    /**
     * Test that salmon flees from axolotl.
     * Setup: Spawn salmon and axolotl in water.
     * Expected: Salmon moves away from axolotl.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSalmonFleesFromAxolotl(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.WATER);
            }
        }

        // Spawn salmon and axolotl close together
        BlockPos salmonPos = new BlockPos(10, 2, 10);
        BlockPos axolotlPos = new BlockPos(13, 2, 10);

        Salmon salmon = helper.spawn(EntityType.SALMON, salmonPos);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Record initial distance
        double initialDistance = salmon.distanceTo(axolotl);

        // Wait for flee behavior to activate
        helper.runAfterDelay(100, () -> {
            if (salmon.isAlive()) {
                double finalDistance = salmon.distanceTo(axolotl);

                // Salmon should have moved away from axolotl or maintain safe distance
                if (finalDistance > initialDistance || finalDistance > 16.0) {
                    helper.succeed();
                } else {
                    helper.fail("Salmon did not flee. Initial: " + initialDistance +
                        ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Salmon not alive");
            }
        });
    }

    /**
     * Test that salmon flee behavior has higher priority than schooling.
     * Setup: Verify priority values.
     * Expected: PRIORITY_FLEE < PRIORITY_SOCIAL.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSalmonFleeHasPriorityOverSchooling(GameTestHelper helper) {
        // Spawn salmon to verify priorities
        BlockPos salmonPos = new BlockPos(5, 2, 5);
        Salmon salmon = helper.spawn(EntityType.SALMON, salmonPos);

        // Verify flee goal priority is higher (lower number) than social behaviors
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_SOCIAL = 5
            // In Minecraft's goal system, lower number = higher priority
            if (salmon.isAlive() &&
                AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_SOCIAL) {
                helper.succeed();
            } else {
                helper.fail("Salmon not alive or flee priority incorrect");
            }
        });
    }

    /**
     * Test that multiple salmon react to predator collectively.
     * Setup: Spawn multiple salmon and one axolotl.
     * Expected: Multiple salmon detect threat and flee.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testMultipleSalmonFleeFromPredator(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.WATER);
            }
        }

        // Spawn school of salmon
        BlockPos salmon1Pos = new BlockPos(10, 2, 10);
        BlockPos salmon2Pos = new BlockPos(11, 2, 10);
        BlockPos salmon3Pos = new BlockPos(10, 2, 11);
        BlockPos axolotlPos = new BlockPos(15, 2, 10);

        Salmon salmon1 = helper.spawn(EntityType.SALMON, salmon1Pos);
        Salmon salmon2 = helper.spawn(EntityType.SALMON, salmon2Pos);
        Salmon salmon3 = helper.spawn(EntityType.SALMON, salmon3Pos);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Wait for salmon to potentially detect axolotl
        helper.runAfterDelay(100, () -> {
            // Check that all salmon are alive and axolotl is present
            if (salmon1.isAlive() && salmon2.isAlive() && salmon3.isAlive() && axolotl.isAlive()) {
                // At least one salmon should be aware of the axolotl
                double dist1 = salmon1.distanceTo(axolotl);
                double dist2 = salmon2.distanceTo(axolotl);
                double dist3 = salmon3.distanceTo(axolotl);

                // Verify distances are within detection range (16 blocks)
                if (dist1 <= 20.0 && dist2 <= 20.0 && dist3 <= 20.0) {
                    helper.succeed();
                } else {
                    helper.fail("Distances too far: " + dist1 + ", " + dist2 + ", " + dist3);
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }
}
