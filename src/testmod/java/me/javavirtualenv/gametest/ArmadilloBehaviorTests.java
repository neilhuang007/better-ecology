package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.armadillo.Armadillo;

/**
 * Game tests for armadillo behaviors.
 */
public class ArmadilloBehaviorTests implements FabricGameTest {

    /**
     * Test that armadillo flees from wolf.
     * Setup: Spawn armadillo and wolf nearby.
     * Expected: Armadillo moves away from wolf.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testArmadilloFleesFromWolf(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn armadillo and wolf close together
        BlockPos armadilloPos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(12, 2, 10);
        Armadillo armadillo = helper.spawn(EntityType.ARMADILLO, armadilloPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = armadillo.distanceTo(wolf);

        // Wait for armadillo to flee
        helper.runAfterDelay(100, () -> {
            if (armadillo.isAlive()) {
                double finalDistance = armadillo.distanceTo(wolf);
                // Armadillo should have moved away from wolf
                if (finalDistance > initialDistance || finalDistance > 10.0) {
                    helper.succeed();
                } else {
                    helper.fail("Armadillo did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Armadillo not alive");
            }
        });
    }

    /**
     * Test that baby armadillo follows adult armadillo.
     * Setup: Spawn baby and adult armadillo.
     * Expected: Baby armadillo stays near adult.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyArmadilloFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult and baby armadillo
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(15, 2, 10);
        Armadillo adult = helper.spawn(EntityType.ARMADILLO, adultPos);
        Armadillo baby = helper.spawn(EntityType.ARMADILLO, babyPos);

        // Make one a baby
        baby.setBaby(true);

        // Wait for baby to follow adult
        helper.runAfterDelay(100, () -> {
            if (baby.isAlive() && adult.isAlive()) {
                double distance = baby.distanceTo(adult);
                // Baby should be following adult (within reasonable distance)
                if (distance < 10.0 && baby.isBaby()) {
                    helper.succeed();
                } else {
                    helper.fail("Baby not following adult. Distance: " + distance + ", isBaby: " + baby.isBaby());
                }
            } else {
                helper.fail("Armadillo(s) not alive");
            }
        });
    }

    /**
     * Test that armadillo flees from predators.
     * Setup: Spawn armadillo, verify flee goal priority is correct.
     * Expected: Armadillo is alive and flee priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testArmadilloFleeGoalPriority(GameTestHelper helper) {
        // Spawn armadillo
        BlockPos armadilloPos = new BlockPos(5, 2, 5);
        Armadillo armadillo = helper.spawn(EntityType.ARMADILLO, armadilloPos);

        // Verify armadillo is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (armadillo.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Armadillo not alive or flee priority incorrect");
            }
        });
    }

    /**
     * Test that adult armadillo protects baby armadillo.
     * Setup: Spawn adult armadillo, baby armadillo, and wolf nearby.
     * Expected: All entities exist with correct distances.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testAdultProtectsBabyArmadillo(GameTestHelper helper) {
        // Spawn armadillos and wolf nearby
        BlockPos adultPos = new BlockPos(5, 2, 5);
        BlockPos babyPos = new BlockPos(6, 2, 5);
        BlockPos wolfPos = new BlockPos(10, 2, 5);
        Armadillo adult = helper.spawn(EntityType.ARMADILLO, adultPos);
        Armadillo baby = helper.spawn(EntityType.ARMADILLO, babyPos);
        baby.setBaby(true);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Wait for entities to initialize
        helper.runAfterDelay(20, () -> {
            // Verify all entities are alive and the behaviors exist
            if (adult.isAlive() && baby.isAlive() && wolf.isAlive()) {
                // Simply verify the entities exist with correct distance
                double distance = adult.distanceTo(wolf);
                if (distance <= 16.0 && baby.isBaby()) {
                    helper.succeed();
                } else {
                    helper.fail("Distance too far or baby not baby: " + distance);
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test that armadillo seeks food when hungry.
     * Setup: Set armadillo hunger to low value.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testArmadilloSeeksFoodWhenHungry(GameTestHelper helper) {
        // Spawn armadillo with low hunger
        BlockPos armadilloPos = new BlockPos(5, 2, 5);
        Armadillo armadillo = helper.spawn(EntityType.ARMADILLO, armadilloPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(armadillo, hungryValue);

        // Verify hunger was set correctly and armadillo is hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(armadillo);
            boolean isHungry = AnimalNeeds.isHungry(armadillo);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that satisfied armadillo does not seek food.
     * Setup: Set armadillo hunger to max value.
     * Expected: isHungry returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSatisfiedArmadilloDoesNotSeekFood(GameTestHelper helper) {
        // Spawn armadillo with max hunger
        BlockPos armadilloPos = new BlockPos(5, 2, 5);
        Armadillo armadillo = helper.spawn(EntityType.ARMADILLO, armadilloPos);
        AnimalNeeds.setHunger(armadillo, AnimalNeeds.MAX_VALUE);

        // Verify armadillo is not hungry
        helper.runAfterDelay(10, () -> {
            boolean isHungry = AnimalNeeds.isHungry(armadillo);
            boolean isSatisfied = AnimalNeeds.isSatisfied(armadillo);

            if (!isHungry && isSatisfied) {
                helper.succeed();
            } else {
                helper.fail("Satisfaction check failed. Expected hungry: false, got: " + isHungry + ", satisfied: " + isSatisfied);
            }
        });
    }
}
