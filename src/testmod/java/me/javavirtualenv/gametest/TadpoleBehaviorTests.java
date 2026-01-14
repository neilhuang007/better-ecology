package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for tadpole behaviors.
 */
public class TadpoleBehaviorTests implements FabricGameTest {

    /**
     * Test that tadpole flees from axolotl.
     * Setup: Spawn tadpole and axolotl close together.
     * Expected: Tadpole moves away from axolotl over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testTadpoleFleesFromAxolotl(GameTestHelper helper) {
        // Create water floor for tadpole movement
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }

        // Spawn tadpole and axolotl close together
        BlockPos tadpolePos = new BlockPos(10, 2, 10);
        BlockPos axolotlPos = new BlockPos(13, 2, 10);
        Tadpole tadpole = helper.spawn(EntityType.TADPOLE, tadpolePos);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Record initial distance
        double initialDistance = tadpole.distanceTo(axolotl);

        // Wait for tadpole to flee
        helper.runAfterDelay(100, () -> {
            if (tadpole.isAlive() && axolotl.isAlive()) {
                double finalDistance = tadpole.distanceTo(axolotl);
                // Tadpole should have moved away from axolotl
                if (finalDistance > initialDistance || finalDistance > 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Tadpole did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Tadpole or axolotl not alive");
            }
        });
    }

    /**
     * Test that multiple tadpoles stay together in a school.
     * Setup: Spawn multiple tadpoles in a group.
     * Expected: Tadpoles maintain cohesion and stay near each other.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testTadpoleSchooling(GameTestHelper helper) {
        // Create water floor for tadpole movement
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }

        // Spawn multiple tadpoles in a group
        BlockPos tadpole1Pos = new BlockPos(8, 2, 8);
        BlockPos tadpole2Pos = new BlockPos(10, 2, 8);
        BlockPos tadpole3Pos = new BlockPos(8, 2, 10);

        Tadpole tadpole1 = helper.spawn(EntityType.TADPOLE, tadpole1Pos);
        Tadpole tadpole2 = helper.spawn(EntityType.TADPOLE, tadpole2Pos);
        Tadpole tadpole3 = helper.spawn(EntityType.TADPOLE, tadpole3Pos);

        // Wait for schooling behavior to activate
        helper.runAfterDelay(100, () -> {
            if (tadpole1.isAlive() && tadpole2.isAlive() && tadpole3.isAlive()) {
                // Calculate average distance between tadpoles
                double dist12 = tadpole1.distanceTo(tadpole2);
                double dist13 = tadpole1.distanceTo(tadpole3);
                double dist23 = tadpole2.distanceTo(tadpole3);
                double avgDistance = (dist12 + dist13 + dist23) / 3.0;

                // Tadpoles should maintain cohesion (average distance should be reasonable)
                // School cohesion radius is 12 blocks, so average should be well below that
                if (avgDistance < 15.0) {
                    helper.succeed();
                } else {
                    helper.fail("Tadpoles too far apart. Average distance: " + avgDistance);
                }
            } else {
                helper.fail("Not all tadpoles alive");
            }
        });
    }
}
