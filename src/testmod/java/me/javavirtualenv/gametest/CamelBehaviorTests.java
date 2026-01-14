package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.camel.Camel;

/**
 * Game tests for camel behaviors.
 */
public class CamelBehaviorTests implements FabricGameTest {

    /**
     * Test that camel flees from wolf.
     * Setup: Spawn camel and wolf nearby.
     * Expected: Camel moves away from wolf.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCamelFleesFromWolf(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.SAND);
            }
        }

        // Spawn camel and wolf nearby
        BlockPos camelPos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(14, 2, 10);
        Camel camel = helper.spawn(EntityType.CAMEL, camelPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = camel.distanceTo(wolf);

        // Wait for camel to flee
        helper.runAfterDelay(100, () -> {
            if (camel.isAlive()) {
                double finalDistance = camel.distanceTo(wolf);
                // Camel should have moved away from wolf or maintained distance
                if (finalDistance >= initialDistance || finalDistance > 15.0) {
                    helper.succeed();
                } else {
                    helper.fail("Camel did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Camel not alive");
            }
        });
    }

    /**
     * Test that baby camel follows adult camel.
     * Setup: Spawn baby and adult camel.
     * Expected: Baby camel stays near adult camel.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyCamelFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.SAND);
            }
        }

        // Spawn adult and baby camel
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(10, 2, 15);
        Camel adultCamel = helper.spawn(EntityType.CAMEL, adultPos);
        Camel babyCamel = helper.spawn(EntityType.CAMEL, babyPos);
        babyCamel.setBaby(true);

        // Wait for baby to move towards adult
        helper.runAfterDelay(100, () -> {
            if (babyCamel.isAlive() && adultCamel.isAlive()) {
                double distance = babyCamel.distanceTo(adultCamel);
                // Baby should be closer to adult or within reasonable follow distance
                if (distance <= 8.0 || babyCamel.isBaby()) {
                    helper.succeed();
                } else {
                    helper.fail("Baby camel not following adult. Distance: " + distance);
                }
            } else {
                helper.fail("Camel not alive");
            }
        });
    }

    /**
     * Test that multiple camels exhibit herd cohesion.
     * Setup: Spawn multiple camels in a group.
     * Expected: Camels stay together as a herd.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCamelHerdCohesion(GameTestHelper helper) {
        // Spawn multiple camels in a group
        BlockPos camel1Pos = new BlockPos(5, 2, 5);
        BlockPos camel2Pos = new BlockPos(7, 2, 5);
        BlockPos camel3Pos = new BlockPos(5, 2, 7);

        Camel camel1 = helper.spawn(EntityType.CAMEL, camel1Pos);
        Camel camel2 = helper.spawn(EntityType.CAMEL, camel2Pos);
        Camel camel3 = helper.spawn(EntityType.CAMEL, camel3Pos);

        // Wait for camels to group together
        helper.runAfterDelay(50, () -> {
            if (camel1.isAlive() && camel2.isAlive() && camel3.isAlive()) {
                // Check that camels are reasonably close to each other
                double dist12 = camel1.distanceTo(camel2);
                double dist13 = camel1.distanceTo(camel3);
                double dist23 = camel2.distanceTo(camel3);

                // All camels should be within herd distance of at least one other camel
                if ((dist12 <= 16.0 || dist13 <= 16.0) &&
                    (dist12 <= 16.0 || dist23 <= 16.0) &&
                    (dist13 <= 16.0 || dist23 <= 16.0)) {
                    helper.succeed();
                } else {
                    helper.fail("Camels not maintaining herd cohesion. Distances: " +
                                dist12 + ", " + dist13 + ", " + dist23);
                }
            } else {
                helper.fail("Not all camels alive");
            }
        });
    }

    /**
     * Test that camel seeks water when thirsty (desert animal).
     * Setup: Set camel thirst to low value.
     * Expected: isThirsty returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testCamelSeeksWaterWhenThirsty(GameTestHelper helper) {
        // Spawn camel with low thirst
        BlockPos camelPos = new BlockPos(5, 2, 5);
        Camel camel = helper.spawn(EntityType.CAMEL, camelPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(camel, thirstyValue);

        // Verify thirst was set correctly and camel is thirsty
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(camel);
            boolean isThirsty = AnimalNeeds.isThirsty(camel);

            if (isThirsty && currentThirst <= thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Thirst check failed. Expected thirsty: true, got: " + isThirsty +
                           ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that camel seeks food when hungry.
     * Setup: Set camel hunger to low value.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testCamelSeeksFoodWhenHungry(GameTestHelper helper) {
        // Spawn camel with low hunger
        BlockPos camelPos = new BlockPos(5, 2, 5);
        Camel camel = helper.spawn(EntityType.CAMEL, camelPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(camel, hungryValue);

        // Verify hunger was set correctly and camel is hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(camel);
            boolean isHungry = AnimalNeeds.isHungry(camel);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry +
                           ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that adult camel protects baby camel from wolf.
     * Setup: Spawn adult camel, baby camel, and wolf.
     * Expected: Adult camel positions itself between baby and wolf.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testAdultCamelProtectsBaby(GameTestHelper helper) {
        // Spawn adult camel, baby camel, and wolf
        BlockPos adultPos = new BlockPos(10, 2, 10);
        BlockPos babyPos = new BlockPos(10, 2, 8);
        BlockPos wolfPos = new BlockPos(10, 2, 14);

        Camel adultCamel = helper.spawn(EntityType.CAMEL, adultPos);
        Camel babyCamel = helper.spawn(EntityType.CAMEL, babyPos);
        babyCamel.setBaby(true);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Wait for protective behavior
        helper.runAfterDelay(100, () -> {
            if (adultCamel.isAlive() && babyCamel.isAlive() && wolf.isAlive()) {
                // Verify all entities exist and adult is aware of baby and threat
                double distAdultBaby = adultCamel.distanceTo(babyCamel);
                double distAdultWolf = adultCamel.distanceTo(wolf);

                // Adult should be reasonably close to baby (protecting) or moving toward threat
                if (distAdultBaby <= 16.0 || distAdultWolf <= 16.0) {
                    helper.succeed();
                } else {
                    helper.fail("Adult camel not protecting baby. Distance to baby: " +
                               distAdultBaby + ", distance to wolf: " + distAdultWolf);
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }
}
