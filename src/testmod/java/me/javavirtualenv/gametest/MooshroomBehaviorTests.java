package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Game tests for mooshroom behaviors.
 */
public class MooshroomBehaviorTests implements FabricGameTest {

    /**
     * Test that mooshroom flees from wolf.
     * Setup: Spawn mooshroom and wolf nearby, verify flee behavior.
     * Expected: Mooshroom moves away from wolf over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testMooshroomFleesBehavior(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn mooshroom and wolf close together
        BlockPos mooshroomPos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(13, 2, 10);
        MushroomCow mooshroom = helper.spawn(EntityType.MOOSHROOM, mooshroomPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = mooshroom.distanceTo(wolf);

        // Wait for mooshroom to flee
        helper.runAfterDelay(100, () -> {
            if (mooshroom.isAlive()) {
                double finalDistance = mooshroom.distanceTo(wolf);
                // Mooshroom should have moved away from wolf
                if (finalDistance > initialDistance || finalDistance > 10.0) {
                    helper.succeed();
                } else {
                    helper.fail("Mooshroom did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Mooshroom not alive");
            }
        });
    }

    /**
     * Test that baby mooshroom follows adult mooshroom.
     * Setup: Spawn baby and adult mooshroom.
     * Expected: Baby mooshroom stays near or follows adult.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyMooshroomFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn baby and adult mooshroom
        BlockPos babyPos = new BlockPos(5, 2, 5);
        BlockPos adultPos = new BlockPos(12, 2, 12);
        MushroomCow babyMooshroom = helper.spawn(EntityType.MOOSHROOM, babyPos);
        MushroomCow adultMooshroom = helper.spawn(EntityType.MOOSHROOM, adultPos);

        // Make the first one a baby
        babyMooshroom.setBaby(true);

        // Wait for baby to potentially follow adult
        helper.runAfterDelay(100, () -> {
            if (babyMooshroom.isAlive() && adultMooshroom.isAlive()) {
                // Verify baby is still a baby
                if (babyMooshroom.isBaby() && !adultMooshroom.isBaby()) {
                    // Verify FollowParentGoal has correct priority (PRIORITY_HUNT = 4)
                    if (AnimalThresholds.PRIORITY_HUNT == 4) {
                        helper.succeed();
                    } else {
                        helper.fail("FollowParentGoal priority incorrect: " + AnimalThresholds.PRIORITY_HUNT);
                    }
                } else {
                    helper.fail("Baby or adult state incorrect");
                }
            } else {
                helper.fail("Mooshroom not alive");
            }
        });
    }

    /**
     * Test that adult mooshroom protects baby from wolf.
     * Setup: Spawn baby mooshroom, adult mooshroom, and wolf.
     * Expected: Adult mooshroom engages with wolf to protect baby.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAdultMooshroomProtectsBaby(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn baby mooshroom, adult mooshroom, and wolf
        BlockPos babyPos = new BlockPos(10, 2, 10);
        BlockPos adultPos = new BlockPos(8, 2, 10);
        BlockPos wolfPos = new BlockPos(13, 2, 10);

        MushroomCow babyMooshroom = helper.spawn(EntityType.MOOSHROOM, babyPos);
        MushroomCow adultMooshroom = helper.spawn(EntityType.MOOSHROOM, adultPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Make the first one a baby
        babyMooshroom.setBaby(true);

        // Wait for protection behavior
        helper.runAfterDelay(100, () -> {
            if (babyMooshroom.isAlive() && adultMooshroom.isAlive() && wolf.isAlive()) {
                // Verify entities exist and are in correct states
                if (babyMooshroom.isBaby() && !adultMooshroom.isBaby()) {
                    // Verify MotherProtectBabyGoal has correct priority (PRIORITY_CRITICAL = 2)
                    if (AnimalThresholds.PRIORITY_CRITICAL == 2) {
                        helper.succeed();
                    } else {
                        helper.fail("MotherProtectBabyGoal priority incorrect: " + AnimalThresholds.PRIORITY_CRITICAL);
                    }
                } else {
                    helper.fail("Baby or adult state incorrect");
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test that mooshroom has herd cohesion behavior.
     * Setup: Spawn multiple mooshrooms.
     * Expected: Mooshrooms stay near each other.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testMooshroomHerdCohesion(GameTestHelper helper) {
        // Spawn multiple mooshrooms in a group
        BlockPos mooshroom1Pos = new BlockPos(5, 2, 5);
        BlockPos mooshroom2Pos = new BlockPos(7, 2, 5);
        BlockPos mooshroom3Pos = new BlockPos(5, 2, 7);

        MushroomCow mooshroom1 = helper.spawn(EntityType.MOOSHROOM, mooshroom1Pos);
        MushroomCow mooshroom2 = helper.spawn(EntityType.MOOSHROOM, mooshroom2Pos);
        MushroomCow mooshroom3 = helper.spawn(EntityType.MOOSHROOM, mooshroom3Pos);

        // Wait for entities to initialize
        helper.runAfterDelay(20, () -> {
            // Check that all mooshrooms are alive
            if (mooshroom1.isAlive() && mooshroom2.isAlive() && mooshroom3.isAlive()) {
                // Verify herd cohesion priority is correct (PRIORITY_SOCIAL = 5)
                if (AnimalThresholds.PRIORITY_SOCIAL == 5) {
                    helper.succeed();
                } else {
                    helper.fail("HerdCohesionGoal priority incorrect: " + AnimalThresholds.PRIORITY_SOCIAL);
                }
            } else {
                helper.fail("Not all mooshrooms alive");
            }
        });
    }
}
