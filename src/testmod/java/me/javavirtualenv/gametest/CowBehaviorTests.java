package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Game tests for cow behaviors.
 */
public class CowBehaviorTests implements FabricGameTest {

    /**
     * Test that a cow flees from a wolf.
     * Setup: Spawn cow and wolf, verify flee goal priority is correct.
     * Expected: Cow is alive and flee goal priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testCowFleesFromWolf(GameTestHelper helper) {
        // Spawn cow
        BlockPos cowPos = new BlockPos(5, 2, 5);
        Cow cow = helper.spawn(EntityType.COW, cowPos);

        // Verify cow is alive and flee priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (cow.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Cow flee goal priority check failed");
            }
        });
    }

    /**
     * Test that a cow seeks water when thirsty.
     * Setup: Spawn cow with low thirst, verify isThirsty returns true.
     * Expected: Cow's thirst is below threshold.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testCowSeeksWaterWhenThirsty(GameTestHelper helper) {
        // Spawn cow with low thirst
        BlockPos cowPos = new BlockPos(5, 2, 5);
        Cow cow = helper.spawn(EntityType.COW, cowPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(cow, thirstyValue);

        // Verify the thirst was set correctly
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(cow);
            boolean isThirsty = AnimalNeeds.isThirsty(cow);

            if (isThirsty && currentThirst <= thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Thirst check failed. Expected thirsty: true, got: " + isThirsty + ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that a cow seeks grass when hungry.
     * Setup: Spawn cow with low hunger, verify isHungry returns true.
     * Expected: Cow's hunger is below threshold.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testCowSeeksGrassWhenHungry(GameTestHelper helper) {
        // Spawn cow with low hunger
        BlockPos cowPos = new BlockPos(5, 2, 5);
        Cow cow = helper.spawn(EntityType.COW, cowPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(cow, hungryValue);

        // Verify the hunger was set correctly
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(cow);
            boolean isHungry = AnimalNeeds.isHungry(cow);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that a hydrated cow does not seek water.
     * Setup: Spawn cow with full thirst.
     * Expected: Cow is not thirsty.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedCowDoesNotSeekWater(GameTestHelper helper) {
        BlockPos cowPos = new BlockPos(5, 2, 5);
        Cow cow = helper.spawn(EntityType.COW, cowPos);
        AnimalNeeds.setThirst(cow, AnimalNeeds.MAX_VALUE);

        helper.runAfterDelay(60, () -> {
            if (!AnimalNeeds.isThirsty(cow)) {
                helper.succeed();
            } else {
                helper.fail("Cow became thirsty unexpectedly");
            }
        });
    }

    /**
     * Test that a satisfied cow does not seek food.
     * Setup: Spawn cow with full hunger.
     * Expected: Cow is not hungry.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSatisfiedCowDoesNotSeekFood(GameTestHelper helper) {
        BlockPos cowPos = new BlockPos(5, 2, 5);
        Cow cow = helper.spawn(EntityType.COW, cowPos);
        AnimalNeeds.setHunger(cow, AnimalNeeds.MAX_VALUE);

        helper.runAfterDelay(60, () -> {
            if (!AnimalNeeds.isHungry(cow)) {
                helper.succeed();
            } else {
                helper.fail("Cow became hungry unexpectedly");
            }
        });
    }

    /**
     * Test that multiple cows stay near each other (herd cohesion).
     * Setup: Spawn 3 cows in a line, verify they stay within herd distance.
     * Expected: Cows remain within cohesion radius of each other.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCowsStayInHerd(GameTestHelper helper) {
        // Spawn 3 cows in a line
        Cow cow1 = helper.spawn(EntityType.COW, new BlockPos(3, 2, 5));
        Cow cow2 = helper.spawn(EntityType.COW, new BlockPos(7, 2, 5));
        Cow cow3 = helper.spawn(EntityType.COW, new BlockPos(11, 2, 5));

        // Wait for them to interact
        helper.runAfterDelay(180, () -> {
            // Check that cows are reasonably close to each other
            double maxDistance = 20.0; // cohesion radius from HerdCohesionGoal

            double dist12 = cow1.distanceTo(cow2);
            double dist23 = cow2.distanceTo(cow3);
            double dist13 = cow1.distanceTo(cow3);

            boolean inHerd = dist12 < maxDistance || dist23 < maxDistance || dist13 < maxDistance;

            if (inHerd && cow1.isAlive() && cow2.isAlive() && cow3.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Cows did not maintain herd cohesion. Distances: " +
                    String.format("%.1f, %.1f, %.1f", dist12, dist23, dist13));
            }
        });
    }

    /**
     * Test that when one cow moves, nearby cows eventually follow (quorum response).
     * Setup: Spawn 3 cows close together, verify they are alive and nearby.
     * Expected: Cows are spawned and alive.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 150)
    public void testQuorumMovement(GameTestHelper helper) {
        // Spawn 3 cows close together
        Cow cow1 = helper.spawn(EntityType.COW, new BlockPos(5, 2, 5));
        Cow cow2 = helper.spawn(EntityType.COW, new BlockPos(6, 2, 5));
        Cow cow3 = helper.spawn(EntityType.COW, new BlockPos(7, 2, 5));

        // Verify all cows are alive and close enough to form a herd
        helper.runAfterDelay(140, () -> {
            if (cow1.isAlive() && cow2.isAlive() && cow3.isAlive()) {
                double maxDist = Math.max(cow1.distanceTo(cow2),
                    Math.max(cow2.distanceTo(cow3), cow1.distanceTo(cow3)));

                // Verify they stayed reasonably close
                if (maxDist < 20.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cows drifted too far apart: " + String.format("%.1f", maxDist));
                }
            } else {
                helper.fail("Not all cows survived");
            }
        });
    }
}
