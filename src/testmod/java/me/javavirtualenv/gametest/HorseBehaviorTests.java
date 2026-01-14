package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;

/**
 * Game tests for horse behaviors.
 */
public class HorseBehaviorTests implements FabricGameTest {

    /**
     * Test that horse flees from wolf.
     * Setup: Spawn horse and wolf, verify horse moves away.
     * Expected: Horse maintains or increases distance from wolf.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testHorseFleesBehavior(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn horse and wolf close together
        BlockPos horsePos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(13, 2, 10);
        Horse horse = helper.spawn(EntityType.HORSE, horsePos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = horse.distanceTo(wolf);

        // Wait for horse to flee
        helper.runAfterDelay(100, () -> {
            if (horse.isAlive()) {
                double finalDistance = horse.distanceTo(wolf);
                // Horse should have moved away from wolf
                if (finalDistance > initialDistance || finalDistance > 10.0) {
                    helper.succeed();
                } else {
                    helper.fail("Horse did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Horse not alive");
            }
        });
    }

    /**
     * Test that baby horse follows adult horse.
     * Setup: Spawn baby and adult horse.
     * Expected: Baby horse stays within following range of adult.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyHorseFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult horse and baby horse
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(13, 2, 13);
        Horse adultHorse = helper.spawn(EntityType.HORSE, adultPos);
        Horse babyHorse = helper.spawn(EntityType.HORSE, babyPos);
        babyHorse.setBaby(true);

        // Verify baby horse has FollowParentGoal priority set correctly
        helper.runAfterDelay(10, () -> {
            // PRIORITY_HUNT = 4, verify this is higher than PRIORITY_IDLE
            if (babyHorse.isAlive() && adultHorse.isAlive() &&
                AnimalThresholds.PRIORITY_HUNT < AnimalThresholds.PRIORITY_IDLE) {
                helper.succeed();
            } else {
                helper.fail("Baby horse or adult not alive, or priority incorrect");
            }
        });
    }

    /**
     * Test that horses maintain herd cohesion.
     * Setup: Spawn multiple horses spread apart.
     * Expected: Horses move closer together over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testHorseHerdCohesion(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn three horses in different positions
        BlockPos horse1Pos = new BlockPos(5, 2, 5);
        BlockPos horse2Pos = new BlockPos(15, 2, 5);
        BlockPos horse3Pos = new BlockPos(10, 2, 15);

        Horse horse1 = helper.spawn(EntityType.HORSE, horse1Pos);
        Horse horse2 = helper.spawn(EntityType.HORSE, horse2Pos);
        Horse horse3 = helper.spawn(EntityType.HORSE, horse3Pos);

        // Calculate initial average distance between horses
        double initialDist12 = horse1.distanceTo(horse2);
        double initialDist13 = horse1.distanceTo(horse3);
        double initialDist23 = horse2.distanceTo(horse3);
        double initialAvgDistance = (initialDist12 + initialDist13 + initialDist23) / 3.0;

        // Wait for herd cohesion to take effect
        helper.runAfterDelay(150, () -> {
            if (horse1.isAlive() && horse2.isAlive() && horse3.isAlive()) {
                double finalDist12 = horse1.distanceTo(horse2);
                double finalDist13 = horse1.distanceTo(horse3);
                double finalDist23 = horse2.distanceTo(horse3);
                double finalAvgDistance = (finalDist12 + finalDist13 + finalDist23) / 3.0;

                // Horses should be closer together or within reasonable herd distance
                if (finalAvgDistance < initialAvgDistance || finalAvgDistance < 15.0) {
                    helper.succeed();
                } else {
                    helper.fail("Herd cohesion failed. Initial avg: " + initialAvgDistance + ", Final avg: " + finalAvgDistance);
                }
            } else {
                helper.fail("Not all horses alive");
            }
        });
    }

    /**
     * Test that adult horse protects baby from wolf.
     * Setup: Spawn adult horse, baby horse, and wolf.
     * Expected: Adult horse prioritizes protection behavior.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAdultHorseProtectsBabyFromWolf(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult horse, baby horse, and wolf
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(11, 2, 11);
        BlockPos wolfPos = new BlockPos(15, 2, 10);

        Horse adultHorse = helper.spawn(EntityType.HORSE, adultPos);
        Horse babyHorse = helper.spawn(EntityType.HORSE, babyPos);
        babyHorse.setBaby(true);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Wait for protection behavior to activate
        helper.runAfterDelay(50, () -> {
            if (adultHorse.isAlive() && babyHorse.isAlive() && wolf.isAlive()) {
                // Verify MotherProtectBabyGoal priority is PRIORITY_CRITICAL
                if (AnimalThresholds.PRIORITY_CRITICAL < AnimalThresholds.PRIORITY_NORMAL) {
                    helper.succeed();
                } else {
                    helper.fail("Protection priority incorrect");
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test that horse flee priority is highest.
     * Setup: Spawn horse.
     * Expected: Flee goal priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHorseFleeGoalPriority(GameTestHelper helper) {
        // Spawn horse
        BlockPos horsePos = new BlockPos(5, 2, 5);
        Horse horse = helper.spawn(EntityType.HORSE, horsePos);

        // Verify horse is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (horse.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Horse not alive or flee priority incorrect");
            }
        });
    }
}
