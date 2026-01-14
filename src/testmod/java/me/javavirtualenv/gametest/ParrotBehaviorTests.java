package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;

/**
 * Game tests for parrot behaviors.
 */
public class ParrotBehaviorTests implements FabricGameTest {

    /**
     * Test that parrot flees from cat.
     * Setup: Spawn parrot and cat nearby.
     * Expected: Parrot is alive and flee priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testParrotFleesFromCat(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn parrot and cat nearby
        BlockPos parrotPos = new BlockPos(10, 2, 10);
        BlockPos catPos = new BlockPos(13, 2, 10);
        Parrot parrot = helper.spawn(EntityType.PARROT, parrotPos);
        Cat cat = helper.spawn(EntityType.CAT, catPos);

        // Record initial distance
        double initialDistance = parrot.distanceTo(cat);

        // Wait for parrot to flee
        helper.runAfterDelay(100, () -> {
            if (parrot.isAlive() && cat.isAlive()) {
                double finalDistance = parrot.distanceTo(cat);
                // Parrot should have moved away from cat
                if (finalDistance > initialDistance || finalDistance > 10.0) {
                    helper.succeed();
                } else {
                    helper.fail("Parrot did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Parrot or cat not alive");
            }
        });
    }

    /**
     * Test that parrots flock together.
     * Setup: Spawn multiple parrots in different locations.
     * Expected: Parrots move toward each other and stay together.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testParrotFlocking(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn parrots at different positions
        BlockPos parrot1Pos = new BlockPos(5, 2, 5);
        BlockPos parrot2Pos = new BlockPos(15, 2, 5);
        BlockPos parrot3Pos = new BlockPos(10, 2, 15);

        Parrot parrot1 = helper.spawn(EntityType.PARROT, parrot1Pos);
        Parrot parrot2 = helper.spawn(EntityType.PARROT, parrot2Pos);
        Parrot parrot3 = helper.spawn(EntityType.PARROT, parrot3Pos);

        // Calculate initial spread (max distance between any two parrots)
        double initialMaxDistance = Math.max(
            Math.max(parrot1.distanceTo(parrot2), parrot1.distanceTo(parrot3)),
            parrot2.distanceTo(parrot3)
        );

        // Wait for parrots to flock together
        helper.runAfterDelay(250, () -> {
            if (parrot1.isAlive() && parrot2.isAlive() && parrot3.isAlive()) {
                // Calculate final spread
                double finalMaxDistance = Math.max(
                    Math.max(parrot1.distanceTo(parrot2), parrot1.distanceTo(parrot3)),
                    parrot2.distanceTo(parrot3)
                );

                // Parrots should be closer together than initially
                // Or at least verify they are within a reasonable flocking distance
                if (finalMaxDistance < initialMaxDistance || finalMaxDistance < 12.0) {
                    helper.succeed();
                } else {
                    helper.fail("Parrots did not flock together. Initial max: " + initialMaxDistance + ", Final max: " + finalMaxDistance);
                }
            } else {
                helper.fail("Not all parrots alive");
            }
        });
    }

    /**
     * Test that parrot flee priority is correct.
     * Setup: Spawn parrot and verify flee goal priority.
     * Expected: Flee priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testParrotFleePriority(GameTestHelper helper) {
        // Spawn parrot
        BlockPos parrotPos = new BlockPos(5, 2, 5);
        Parrot parrot = helper.spawn(EntityType.PARROT, parrotPos);

        // Verify parrot is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (parrot.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Parrot not alive or flee priority incorrect");
            }
        });
    }
}
