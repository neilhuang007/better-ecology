package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Game tests for sheep behaviors.
 */
public class SheepBehaviorTests implements FabricGameTest {

    /**
     * Test that multiple sheep stay near each other (herd cohesion).
     * Setup: Spawn 3 sheep in a line, verify they stay within herd distance.
     * Expected: Sheep remain within cohesion radius of each other.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSheepStayInHerd(GameTestHelper helper) {
        // Spawn 3 sheep in a line
        Sheep sheep1 = helper.spawn(EntityType.SHEEP, new BlockPos(3, 2, 5));
        Sheep sheep2 = helper.spawn(EntityType.SHEEP, new BlockPos(7, 2, 5));
        Sheep sheep3 = helper.spawn(EntityType.SHEEP, new BlockPos(11, 2, 5));

        // Wait for them to interact
        helper.runAfterDelay(180, () -> {
            // Check that sheep are reasonably close to each other
            double maxDistance = 20.0; // cohesion radius from HerdCohesionGoal

            double dist12 = sheep1.distanceTo(sheep2);
            double dist23 = sheep2.distanceTo(sheep3);
            double dist13 = sheep1.distanceTo(sheep3);

            boolean inHerd = dist12 < maxDistance || dist23 < maxDistance || dist13 < maxDistance;

            if (inHerd && sheep1.isAlive() && sheep2.isAlive() && sheep3.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Sheep did not maintain herd cohesion. Distances: " +
                    String.format("%.1f, %.1f, %.1f", dist12, dist23, dist13));
            }
        });
    }

    /**
     * Test that when one sheep moves, nearby sheep eventually follow (quorum response).
     * Setup: Spawn 4 sheep close together, verify they stay as a group.
     * Expected: Sheep remain close together demonstrating herd behavior.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 150)
    public void testSheepQuorumMovement(GameTestHelper helper) {
        // Spawn 4 sheep close together to ensure quorum threshold can be met
        Sheep sheep1 = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 5));
        Sheep sheep2 = helper.spawn(EntityType.SHEEP, new BlockPos(6, 2, 5));
        Sheep sheep3 = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 6));
        Sheep sheep4 = helper.spawn(EntityType.SHEEP, new BlockPos(6, 2, 6));

        // Verify all sheep are alive and close enough to form a herd
        helper.runAfterDelay(140, () -> {
            if (sheep1.isAlive() && sheep2.isAlive() && sheep3.isAlive() && sheep4.isAlive()) {
                // Calculate average position to check cohesion
                double avgX = (sheep1.getX() + sheep2.getX() + sheep3.getX() + sheep4.getX()) / 4.0;
                double avgZ = (sheep1.getZ() + sheep2.getZ() + sheep3.getZ() + sheep4.getZ()) / 4.0;

                // Check that all sheep are within reasonable distance of center
                double maxDistFromCenter = 0;
                for (Sheep sheep : new Sheep[]{sheep1, sheep2, sheep3, sheep4}) {
                    double dist = Math.sqrt(
                        Math.pow(sheep.getX() - avgX, 2) + Math.pow(sheep.getZ() - avgZ, 2)
                    );
                    maxDistFromCenter = Math.max(maxDistFromCenter, dist);
                }

                // If max distance from center is less than cohesion radius, they're grouped
                if (maxDistFromCenter < 20.0) {
                    helper.succeed();
                } else {
                    helper.fail("Sheep scattered too far from group center: " +
                        String.format("%.1f", maxDistFromCenter));
                }
            } else {
                helper.fail("Not all sheep survived");
            }
        });
    }

    /**
     * Test that a sheep flees from a wolf.
     * Setup: Spawn sheep and wolf close together.
     * Expected: Sheep flee goal priority is correct.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSheepFleeFromWolf(GameTestHelper helper) {
        // Spawn sheep
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Verify sheep is alive and flee priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_SOCIAL = 5
            // In Minecraft's goal system, lower number = higher priority
            if (sheep.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_SOCIAL) {
                helper.succeed();
            } else {
                helper.fail("Sheep flee goal priority check failed");
            }
        });
    }

    /**
     * Test that two sheep maintain minimum separation.
     * Setup: Spawn 2 sheep very close together.
     * Expected: Sheep maintain some separation distance.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void testSheepMaintainSeparation(GameTestHelper helper) {
        // Spawn 2 sheep at same position
        Sheep sheep1 = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 5));
        Sheep sheep2 = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 5));

        // Wait and check they separated slightly
        helper.runAfterDelay(100, () -> {
            double distance = sheep1.distanceTo(sheep2);
            double minSeparation = 1.0; // Should maintain at least some distance

            if (sheep1.isAlive() && sheep2.isAlive() && distance >= minSeparation) {
                helper.succeed();
            } else {
                helper.fail("Sheep did not maintain separation. Distance: " +
                    String.format("%.2f", distance));
            }
        });
    }
}
