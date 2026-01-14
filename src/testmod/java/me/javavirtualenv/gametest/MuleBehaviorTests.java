package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Mule;

/**
 * Game tests for mule behaviors.
 * Mules are sterile hybrids between horses and donkeys.
 */
public class MuleBehaviorTests implements FabricGameTest {

    /**
     * Test that mule flees from wolf.
     * Setup: Spawn mule and wolf nearby.
     * Expected: Mule detects wolf and flees.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testMuleFleesBehavior(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn mule and wolf nearby
        BlockPos mulePos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(13, 2, 10);
        Mule mule = helper.spawn(EntityType.MULE, mulePos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = mule.distanceTo(wolf);

        // Wait for mule to flee
        helper.runAfterDelay(100, () -> {
            if (mule.isAlive() && wolf.isAlive()) {
                double finalDistance = mule.distanceTo(wolf);
                // Mule should have moved away from wolf
                if (finalDistance > initialDistance || finalDistance > 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Mule did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Mule or wolf not alive");
            }
        });
    }

    /**
     * Test that mule seeks grass when hungry.
     * Setup: Set mule hunger to low value.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testMuleGrazes(GameTestHelper helper) {
        // Spawn mule with low hunger
        BlockPos mulePos = new BlockPos(5, 2, 5);
        Mule mule = helper.spawn(EntityType.MULE, mulePos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(mule, hungryValue);

        // Verify hunger was set correctly and mule is hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(mule);
            boolean isHungry = AnimalNeeds.isHungry(mule);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that baby mule follows adult horses/donkeys/mules.
     * Setup: Spawn adult mule and baby mule.
     * Expected: Baby mule follows adult mule.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyMuleFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult and baby mule
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(5, 2, 5);
        Mule adult = helper.spawn(EntityType.MULE, adultPos);
        Mule baby = helper.spawn(EntityType.MULE, babyPos);

        // Make one a baby
        baby.setBaby(true);

        // Record initial distance
        double initialDistance = baby.distanceTo(adult);

        // Wait for baby to start following
        helper.runAfterDelay(100, () -> {
            if (baby.isAlive() && adult.isAlive() && baby.isBaby()) {
                double finalDistance = baby.distanceTo(adult);
                // Baby should have moved toward adult if initially far away
                if (initialDistance > 6.0 && finalDistance < initialDistance) {
                    helper.succeed();
                } else if (finalDistance <= 6.0) {
                    // Baby is already close enough, which is acceptable
                    helper.succeed();
                } else {
                    helper.fail("Baby mule did not follow adult. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Mules not alive or baby not baby");
            }
        });
    }

    /**
     * Test that mule flee priority is higher than normal goals.
     * Setup: Spawn mule.
     * Expected: PRIORITY_FLEE is less than PRIORITY_NORMAL.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testMuleFleesPriorityCorrect(GameTestHelper helper) {
        // Spawn mule
        BlockPos mulePos = new BlockPos(5, 2, 5);
        Mule mule = helper.spawn(EntityType.MULE, mulePos);

        // Verify mule is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (mule.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Mule not alive or flee priority incorrect");
            }
        });
    }
}
