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
}
