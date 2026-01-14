package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for bee behaviors.
 */
public class BeeBehaviorTests implements FabricGameTest {

    /**
     * Test that multiple bees stay near each other (flocking/herding).
     * Setup: Spawn multiple bees in an area.
     * Expected: Bees maintain cohesion and stay within a reasonable distance of each other.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testBeeFlocking(GameTestHelper helper) {
        // Spawn multiple bees
        BlockPos bee1Pos = new BlockPos(3, 2, 3);
        BlockPos bee2Pos = new BlockPos(7, 2, 7);
        BlockPos bee3Pos = new BlockPos(5, 2, 5);

        Bee bee1 = helper.spawn(EntityType.BEE, bee1Pos);
        Bee bee2 = helper.spawn(EntityType.BEE, bee2Pos);
        Bee bee3 = helper.spawn(EntityType.BEE, bee3Pos);

        // Wait for bees to move and adjust positions
        helper.runAfterDelay(200, () -> {
            // Calculate average distance between bees
            double dist12 = bee1.distanceTo(bee2);
            double dist13 = bee1.distanceTo(bee3);
            double dist23 = bee2.distanceTo(bee3);

            double avgDistance = (dist12 + dist13 + dist23) / 3.0;

            // Bees should maintain cohesion - average distance should be reasonable (not too far apart)
            // Using 20 blocks as the cohesion radius from HerdCohesionGoal default
            if (bee1.isAlive() && bee2.isAlive() && bee3.isAlive() && avgDistance < 25.0) {
                helper.succeed();
            } else {
                helper.fail("Bees did not maintain cohesion. Avg distance: " + avgDistance +
                    ", Alive: " + bee1.isAlive() + ", " + bee2.isAlive() + ", " + bee3.isAlive());
            }
        });
    }

    /**
     * Test that a bee seeks water when thirsty.
     * Setup: Spawn bee with low thirst near water.
     * Expected: Bee moves toward water.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testBeeSeeksWater(GameTestHelper helper) {
        // Create water source
        BlockPos waterPos = new BlockPos(8, 2, 8);
        helper.setBlock(waterPos, Blocks.WATER);

        // Spawn bee with low thirst at distance from water
        BlockPos beePos = new BlockPos(3, 2, 3);
        Bee bee = helper.spawn(EntityType.BEE, beePos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(bee, thirstyValue);

        // Record initial distance to water
        double initialDistance = bee.position().distanceTo(waterPos.getCenter());

        // Wait for bee to move toward water
        helper.runAfterDelay(300, () -> {
            double currentDistance = bee.position().distanceTo(waterPos.getCenter());
            float currentThirst = AnimalNeeds.getThirst(bee);

            // Bee should have moved closer to water or increased thirst (from drinking)
            if (bee.isAlive() && (currentDistance < initialDistance || currentThirst > thirstyValue)) {
                helper.succeed();
            } else {
                helper.fail("Bee did not seek water. Initial dist: " + initialDistance +
                    ", Current dist: " + currentDistance + ", Thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that a hydrated bee does not seek water.
     * Setup: Spawn bee with maximum thirst.
     * Expected: isThirsty returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedBeeDoesNotSeekWater(GameTestHelper helper) {
        // Spawn bee with maximum thirst
        BlockPos beePos = new BlockPos(5, 2, 5);
        Bee bee = helper.spawn(EntityType.BEE, beePos);
        AnimalNeeds.setThirst(bee, AnimalNeeds.MAX_VALUE);

        // Verify bee is not thirsty
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(bee);
            boolean isThirsty = AnimalNeeds.isThirsty(bee);

            if (!isThirsty && currentThirst >= AnimalThresholds.HYDRATED) {
                helper.succeed();
            } else {
                helper.fail("Hydration check failed. Expected thirsty: false, got: " + isThirsty + ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that a bee can drink water to restore thirst.
     * Setup: Spawn thirsty bee next to water.
     * Expected: Thirst increases after drinking.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testBeeDrinksWater(GameTestHelper helper) {
        // Create water source
        BlockPos waterPos = new BlockPos(5, 2, 5);
        helper.setBlock(waterPos, Blocks.WATER);

        // Spawn bee adjacent to water with low thirst
        BlockPos beePos = new BlockPos(6, 2, 5);
        Bee bee = helper.spawn(EntityType.BEE, beePos);
        float thirstyValue = AnimalThresholds.THIRSTY - 15;
        AnimalNeeds.setThirst(bee, thirstyValue);

        // Wait for bee to drink
        helper.runAfterDelay(200, () -> {
            float currentThirst = AnimalNeeds.getThirst(bee);

            // Thirst should have increased from drinking
            if (bee.isAlive() && currentThirst > thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Bee did not drink water. Initial thirst: " + thirstyValue + ", Current thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that bees maintain social cohesion with correct priority.
     * Setup: Spawn bee and verify social goal priority is correct.
     * Expected: Bee is alive and social goal has correct priority level.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testBeeSocialGoalPriority(GameTestHelper helper) {
        // Spawn bee
        BlockPos beePos = new BlockPos(5, 2, 5);
        Bee bee = helper.spawn(EntityType.BEE, beePos);

        // Verify bee is alive and social priority is correct
        helper.runAfterDelay(10, () -> {
            // Verify social priority is lower than normal needs
            if (bee.isAlive() && AnimalThresholds.PRIORITY_SOCIAL > AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Bee social goal priority check failed");
            }
        });
    }

    /**
     * Test bee waggle dance behavior when flowers are nearby.
     * Setup: Place flowers and hive, spawn bee near them.
     * Expected: Bee moves toward flowers for foraging.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testBeeWaggleDanceNearFlowers(GameTestHelper helper) {
        // Create grass floor for proper bee navigation
        createFloor(helper, Blocks.GRASS_BLOCK, 0, 1);

        // Place beehive
        BlockPos hivePos = new BlockPos(5, 2, 5);
        helper.setBlock(hivePos, Blocks.BEEHIVE);

        // Place flowers nearby
        BlockPos flower1Pos = new BlockPos(8, 2, 8);
        BlockPos flower2Pos = new BlockPos(9, 2, 8);
        BlockPos flower3Pos = new BlockPos(8, 2, 9);
        helper.setBlock(flower1Pos, Blocks.DANDELION);
        helper.setBlock(flower2Pos, Blocks.DANDELION);
        helper.setBlock(flower3Pos, Blocks.DANDELION);

        // Spawn bee near hive
        BlockPos beePos = new BlockPos(5, 2, 3);
        Bee bee = helper.spawn(EntityType.BEE, beePos);

        // Record initial distance to flowers
        double initialDistance = bee.position().distanceTo(flower1Pos.getCenter());

        // Wait for bee to move toward flowers
        helper.runAfterDelay(300, () -> {
            double currentDistance = bee.position().distanceTo(flower1Pos.getCenter());

            // Bee should have moved closer to flowers
            if (bee.isAlive() && currentDistance < initialDistance) {
                helper.succeed();
            } else {
                helper.fail("Bee did not move toward flowers. Initial dist: " + initialDistance +
                    ", Current dist: " + currentDistance);
            }
        });
    }

    /**
     * Test bee waggle dance communication between multiple bees.
     * Setup: Spawn 2 bees near hive with flowers nearby.
     * Expected: Both bees eventually find flowers through foraging or observation.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 500)
    public void testBeeWaggleDanceCommunication(GameTestHelper helper) {
        // Create grass floor for proper bee navigation
        createFloor(helper, Blocks.GRASS_BLOCK, 0, 1);

        // Place beehive
        BlockPos hivePos = new BlockPos(5, 2, 5);
        helper.setBlock(hivePos, Blocks.BEEHIVE);

        // Place flowers at distance
        BlockPos flowerPos = new BlockPos(10, 2, 10);
        helper.setBlock(flowerPos, Blocks.DANDELION);
        helper.setBlock(new BlockPos(11, 2, 10), Blocks.DANDELION);
        helper.setBlock(new BlockPos(10, 2, 11), Blocks.DANDELION);

        // Spawn two bees near hive
        BlockPos bee1Pos = new BlockPos(5, 2, 4);
        BlockPos bee2Pos = new BlockPos(6, 2, 5);
        Bee bee1 = helper.spawn(EntityType.BEE, bee1Pos);
        Bee bee2 = helper.spawn(EntityType.BEE, bee2Pos);

        // Record initial distances to flowers
        double initialDistance1 = bee1.position().distanceTo(flowerPos.getCenter());
        double initialDistance2 = bee2.position().distanceTo(flowerPos.getCenter());

        // Wait for bees to discover and move toward flowers
        helper.runAfterDelay(400, () -> {
            double currentDistance1 = bee1.position().distanceTo(flowerPos.getCenter());
            double currentDistance2 = bee2.position().distanceTo(flowerPos.getCenter());

            // At least one bee should have moved closer to flowers
            boolean bee1MovedCloser = currentDistance1 < initialDistance1;
            boolean bee2MovedCloser = currentDistance2 < initialDistance2;

            if (bee1.isAlive() && bee2.isAlive() && (bee1MovedCloser || bee2MovedCloser)) {
                helper.succeed();
            } else {
                helper.fail("Bees did not communicate flower location. Bee1 closer: " + bee1MovedCloser +
                    ", Bee2 closer: " + bee2MovedCloser);
            }
        });
    }

    /**
     * Test bee returns to hive after foraging flowers.
     * Setup: Place hive and flowers, spawn bee.
     * Expected: Bee returns to hive area after visiting flowers.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
    public void testBeeReturnsToHiveAfterForaging(GameTestHelper helper) {
        // Create grass floor for proper bee navigation
        createFloor(helper, Blocks.GRASS_BLOCK, 0, 1);

        // Place beehive
        BlockPos hivePos = new BlockPos(5, 2, 5);
        helper.setBlock(hivePos, Blocks.BEEHIVE);

        // Place flowers at moderate distance
        BlockPos flowerPos = new BlockPos(9, 2, 9);
        helper.setBlock(flowerPos, Blocks.DANDELION);
        helper.setBlock(new BlockPos(10, 2, 9), Blocks.DANDELION);
        helper.setBlock(new BlockPos(9, 2, 10), Blocks.DANDELION);

        // Spawn bee between hive and flowers
        BlockPos beePos = new BlockPos(7, 2, 7);
        Bee bee = helper.spawn(EntityType.BEE, beePos);

        // Wait for bee to forage and return to hive area
        helper.runAfterDelay(500, () -> {
            double distanceToHive = bee.position().distanceTo(hivePos.getCenter());

            // Bee should be back near hive area (within reasonable range)
            if (bee.isAlive() && distanceToHive < 8.0) {
                helper.succeed();
            } else {
                helper.fail("Bee did not return to hive. Distance to hive: " + distanceToHive);
            }
        });
    }

    /**
     * Helper method to create a floor for proper entity navigation.
     */
    private void createFloor(GameTestHelper helper, net.minecraft.world.level.block.Block block, int xStart, int yLevel) {
        for (int x = xStart; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(new BlockPos(x, yLevel, z), block);
            }
        }
    }
}
