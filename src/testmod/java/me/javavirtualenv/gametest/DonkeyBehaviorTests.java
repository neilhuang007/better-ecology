package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Donkey;

/**
 * Game tests for donkey behaviors.
 */
public class DonkeyBehaviorTests implements FabricGameTest {

    /**
     * Test that donkey flees from wolf.
     * Setup: Spawn donkey and wolf nearby.
     * Expected: Donkey detects wolf and flees.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testDonkeyFleesBehavior(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn donkey and wolf nearby
        BlockPos donkeyPos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(13, 2, 10);
        Donkey donkey = helper.spawn(EntityType.DONKEY, donkeyPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = donkey.distanceTo(wolf);

        // Wait for donkey to flee
        helper.runAfterDelay(100, () -> {
            if (donkey.isAlive() && wolf.isAlive()) {
                double finalDistance = donkey.distanceTo(wolf);
                // Donkey should have moved away from wolf
                if (finalDistance > initialDistance || finalDistance > 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Donkey did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Donkey or wolf not alive");
            }
        });
    }

    /**
     * Test that baby donkey follows adult donkey.
     * Setup: Spawn adult donkey and baby donkey.
     * Expected: Baby donkey follows adult donkey.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyDonkeyFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult and baby donkey
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(5, 2, 5);
        Donkey adult = helper.spawn(EntityType.DONKEY, adultPos);
        Donkey baby = helper.spawn(EntityType.DONKEY, babyPos);

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
                    helper.fail("Baby donkey did not follow adult. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Donkeys not alive or baby not baby");
            }
        });
    }

    /**
     * Test that donkey seeks grass when hungry.
     * Setup: Set donkey hunger to low value.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testDonkeySeeksGrassWhenHungry(GameTestHelper helper) {
        // Spawn donkey with low hunger
        BlockPos donkeyPos = new BlockPos(5, 2, 5);
        Donkey donkey = helper.spawn(EntityType.DONKEY, donkeyPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(donkey, hungryValue);

        // Verify hunger was set correctly and donkey is hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(donkey);
            boolean isHungry = AnimalNeeds.isHungry(donkey);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that adult donkey protects baby from wolf.
     * Setup: Spawn adult donkey, baby donkey, and wolf.
     * Expected: Adult donkey moves toward wolf when it threatens baby.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAdultDonkeyProtectsBaby(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult donkey, baby donkey, and wolf
        BlockPos adultPos = new BlockPos(5, 2, 5);
        BlockPos babyPos = new BlockPos(12, 2, 12);
        BlockPos wolfPos = new BlockPos(14, 2, 12);

        Donkey adult = helper.spawn(EntityType.DONKEY, adultPos);
        Donkey baby = helper.spawn(EntityType.DONKEY, babyPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        baby.setBaby(true);

        // Record initial distance between adult and wolf
        double initialDistance = adult.distanceTo(wolf);

        // Wait for adult to react
        helper.runAfterDelay(100, () -> {
            if (adult.isAlive() && baby.isAlive() && wolf.isAlive()) {
                double finalDistance = adult.distanceTo(wolf);
                // Adult should have moved toward wolf to protect baby
                if (finalDistance < initialDistance) {
                    helper.succeed();
                } else {
                    helper.fail("Adult donkey did not protect baby. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("One or more entities not alive");
            }
        });
    }

    /**
     * Test that donkey flee priority is higher than normal goals.
     * Setup: Spawn donkey.
     * Expected: PRIORITY_FLEE is less than PRIORITY_NORMAL.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testDonkeyFleesPriorityCorrect(GameTestHelper helper) {
        // Spawn donkey
        BlockPos donkeyPos = new BlockPos(5, 2, 5);
        Donkey donkey = helper.spawn(EntityType.DONKEY, donkeyPos);

        // Verify donkey is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (donkey.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Donkey not alive or flee priority incorrect");
            }
        });
    }
}
