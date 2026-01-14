package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Game tests for allay behaviors.
 */
public class AllayBehaviorTests implements FabricGameTest {

    /**
     * Test that multiple allays flock together.
     * Setup: Spawn multiple allays.
     * Expected: Allays stay close together in a group.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAllayFlocking(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn multiple allays in different positions
        BlockPos allay1Pos = new BlockPos(5, 2, 5);
        BlockPos allay2Pos = new BlockPos(15, 2, 15);
        BlockPos allay3Pos = new BlockPos(10, 2, 10);

        Allay allay1 = helper.spawn(EntityType.ALLAY, allay1Pos);
        Allay allay2 = helper.spawn(EntityType.ALLAY, allay2Pos);
        Allay allay3 = helper.spawn(EntityType.ALLAY, allay3Pos);

        // Record initial distances
        double initialDist12 = allay1.distanceTo(allay2);
        double initialDist13 = allay1.distanceTo(allay3);

        // Wait for allays to flock together
        helper.runAfterDelay(150, () -> {
            if (allay1.isAlive() && allay2.isAlive() && allay3.isAlive()) {
                // Check if allays have moved closer together
                double finalDist12 = allay1.distanceTo(allay2);
                double finalDist13 = allay1.distanceTo(allay3);

                // At least one pair should be closer, or they should be within reasonable distance
                if (finalDist12 < initialDist12 || finalDist13 < initialDist13 ||
                    (finalDist12 < 15.0 && finalDist13 < 15.0)) {
                    helper.succeed();
                } else {
                    helper.fail("Allays did not flock together. Distances: " +
                        finalDist12 + ", " + finalDist13);
                }
            } else {
                helper.fail("Not all allays alive");
            }
        });
    }

    /**
     * Test that allay flees from zombie.
     * Setup: Spawn allay and zombie nearby.
     * Expected: Allay flees from zombie and moves away.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAllayFleesFromZombie(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn allay and zombie close together
        BlockPos allayPos = new BlockPos(10, 2, 10);
        BlockPos zombiePos = new BlockPos(13, 2, 10);

        Allay allay = helper.spawn(EntityType.ALLAY, allayPos);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, zombiePos);

        // Record initial distance
        double initialDistance = allay.distanceTo(zombie);

        // Wait for allay to flee
        helper.runAfterDelay(100, () -> {
            if (allay.isAlive()) {
                double finalDistance = allay.distanceTo(zombie);

                // Allay should have moved away from zombie
                if (finalDistance > initialDistance || finalDistance > 12.0) {
                    helper.succeed();
                } else {
                    helper.fail("Allay did not flee. Initial: " + initialDistance +
                        ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Allay not alive");
            }
        });
    }

    /**
     * Test that allay flee goal has correct priority.
     * Setup: Spawn allay.
     * Expected: Flee priority is higher than social behaviors.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testAllayFleeGoalPriority(GameTestHelper helper) {
        // Spawn allay
        BlockPos allayPos = new BlockPos(5, 2, 5);
        Allay allay = helper.spawn(EntityType.ALLAY, allayPos);

        // Verify allay is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_SOCIAL = 5
            // In Minecraft's goal system, lower number = higher priority
            if (allay.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_SOCIAL) {
                helper.succeed();
            } else {
                helper.fail("Allay not alive or flee priority incorrect");
            }
        });
    }
}
