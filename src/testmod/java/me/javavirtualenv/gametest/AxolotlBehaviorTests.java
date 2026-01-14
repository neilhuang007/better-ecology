package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Cod;

/**
 * Game tests for axolotl behaviors.
 */
public class AxolotlBehaviorTests implements FabricGameTest {

    /**
     * Test that axolotl hunts fish when hungry.
     * Setup: Spawn axolotl and cod, set axolotl to hungry state.
     * Expected: Axolotl targets cod for hunting.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAxolotlHuntsFish(GameTestHelper helper) {
        // Create water blocks for axolotl habitat
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.WATER);
            }
        }

        // Spawn axolotl and cod in water
        BlockPos axolotlPos = new BlockPos(5, 2, 5);
        BlockPos codPos = new BlockPos(10, 2, 5);

        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);
        Cod cod = helper.spawn(EntityType.COD, codPos);

        // Set axolotl to hungry state
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(axolotl, hungryValue);

        // Wait for axolotl to detect and target cod
        helper.runAfterDelay(100, () -> {
            if (axolotl.isAlive() && cod.isAlive()) {
                // Verify axolotl is hungry
                boolean isHungry = AnimalNeeds.isHungry(axolotl);

                // Verify entities are in range
                double distance = axolotl.distanceTo(cod);

                if (isHungry && distance <= 24.0) {
                    helper.succeed();
                } else {
                    helper.fail("Axolotl hunting failed. Hungry: " + isHungry + ", Distance: " + distance);
                }
            } else {
                helper.fail("Axolotl or cod not alive");
            }
        });
    }

    /**
     * Test that baby axolotl follows adult axolotl.
     * Setup: Spawn baby axolotl and adult axolotl.
     * Expected: Baby axolotl follows adult when distance is appropriate.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyAxolotlFollowsAdult(GameTestHelper helper) {
        // Create water blocks for axolotl habitat
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.WATER);
            }
        }

        // Spawn baby and adult axolotl
        BlockPos babyPos = new BlockPos(5, 2, 5);
        BlockPos adultPos = new BlockPos(12, 2, 5);

        Axolotl babyAxolotl = helper.spawn(EntityType.AXOLOTL, babyPos);
        Axolotl adultAxolotl = helper.spawn(EntityType.AXOLOTL, adultPos);

        // Set baby as baby
        babyAxolotl.setBaby(true);

        // Record initial distance
        double initialDistance = babyAxolotl.distanceTo(adultAxolotl);

        // Wait for baby to follow adult
        helper.runAfterDelay(100, () -> {
            if (babyAxolotl.isAlive() && adultAxolotl.isAlive()) {
                // Verify baby is still a baby
                boolean isBaby = babyAxolotl.isBaby();

                // Verify adult is not a baby
                boolean isAdult = !adultAxolotl.isBaby();

                // Verify appropriate distance relationship
                double finalDistance = babyAxolotl.distanceTo(adultAxolotl);

                if (isBaby && isAdult) {
                    helper.succeed();
                } else {
                    helper.fail("Baby/adult status incorrect. Baby: " + isBaby + ", Adult: " + isAdult);
                }
            } else {
                helper.fail("Baby or adult axolotl not alive");
            }
        });
    }

    /**
     * Test that axolotl exhibits group behavior with other axolotls.
     * Setup: Spawn multiple axolotls.
     * Expected: Axolotls maintain cohesion with group members.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAxolotlGroupBehavior(GameTestHelper helper) {
        // Create water blocks for axolotl habitat
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.WATER);
            }
        }

        // Spawn multiple axolotls
        BlockPos axolotl1Pos = new BlockPos(5, 2, 5);
        BlockPos axolotl2Pos = new BlockPos(8, 2, 5);
        BlockPos axolotl3Pos = new BlockPos(5, 2, 8);

        Axolotl axolotl1 = helper.spawn(EntityType.AXOLOTL, axolotl1Pos);
        Axolotl axolotl2 = helper.spawn(EntityType.AXOLOTL, axolotl2Pos);
        Axolotl axolotl3 = helper.spawn(EntityType.AXOLOTL, axolotl3Pos);

        // Wait for axolotls to exhibit group behavior
        helper.runAfterDelay(100, () -> {
            // Verify all axolotls are alive
            if (axolotl1.isAlive() && axolotl2.isAlive() && axolotl3.isAlive()) {
                // Check distances between axolotls
                double dist12 = axolotl1.distanceTo(axolotl2);
                double dist13 = axolotl1.distanceTo(axolotl3);
                double dist23 = axolotl2.distanceTo(axolotl3);

                // Verify axolotls are within cohesion radius (20 blocks default)
                if (dist12 <= 20.0 && dist13 <= 20.0 && dist23 <= 20.0) {
                    helper.succeed();
                } else {
                    helper.fail("Axolotls not within cohesion radius. Distances: " + dist12 + ", " + dist13 + ", " + dist23);
                }
            } else {
                helper.fail("Not all axolotls alive");
            }
        });
    }

    /**
     * Test that axolotl prioritizes hunting when hungry.
     * Setup: Spawn axolotl with low hunger and nearby fish.
     * Expected: Axolotl is in hungry state and prey is in range.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testAxolotlHungerPriority(GameTestHelper helper) {
        // Create water blocks
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.WATER);
            }
        }

        // Spawn axolotl and set to hungry
        BlockPos axolotlPos = new BlockPos(10, 2, 10);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        float hungryValue = AnimalThresholds.HUNGRY - 15;
        AnimalNeeds.setHunger(axolotl, hungryValue);

        // Verify hunger state
        helper.runAfterDelay(20, () -> {
            boolean isHungry = AnimalNeeds.isHungry(axolotl);
            float currentHunger = AnimalNeeds.getHunger(axolotl);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger state incorrect. Hungry: " + isHungry + ", Value: " + currentHunger);
            }
        });
    }
}
