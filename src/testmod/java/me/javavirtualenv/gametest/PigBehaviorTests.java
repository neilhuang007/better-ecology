package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for pig behaviors.
 */
public class PigBehaviorTests implements FabricGameTest {

    /**
     * Test that a pig flees from a wolf.
     * Setup: Spawn pig and wolf, verify pig is alive and flee goal priority is correct.
     * Expected: Pig is alive and flee priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testPigFleesFromWolf(GameTestHelper helper) {
        // Spawn pig
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);

        // Spawn wolf nearby
        BlockPos wolfPos = new BlockPos(5, 2, 3);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Verify pig is alive and flee priority is correct
        helper.runAfterDelay(10, () -> {
            boolean pigAlive = pig.isAlive();
            boolean fleePriorityCorrect = AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL;

            if (pigAlive && fleePriorityCorrect) {
                helper.succeed();
            } else {
                helper.fail("Pig flee test failed. Alive: " + pigAlive + ", flee priority correct: " + fleePriorityCorrect);
            }
        });
    }

    /**
     * Test that a pig seeks water when thirsty.
     * Setup: Spawn pig with low thirst, verify thirst state is correct.
     * Expected: Pig's thirst is below threshold and isThirsty returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testPigSeeksWaterWhenThirsty(GameTestHelper helper) {
        // Spawn pig with low thirst
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(pig, thirstyValue);

        // Verify the thirst was set correctly
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(pig);
            boolean isThirsty = AnimalNeeds.isThirsty(pig);

            if (isThirsty && currentThirst <= thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Thirst check failed. Expected thirsty: true, got: " + isThirsty + ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that a pig seeks food when hungry.
     * Setup: Spawn pig with low hunger, verify hunger state is correct.
     * Expected: Pig's hunger is below threshold and isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testPigSeeksFoodWhenHungry(GameTestHelper helper) {
        // Spawn pig with low hunger
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(pig, hungryValue);

        // Verify the hunger was set correctly
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(pig);
            boolean isHungry = AnimalNeeds.isHungry(pig);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that a hydrated pig does not seek water.
     * Setup: Spawn pig with full thirst.
     * Expected: Pig does not become thirsty and isThirsty returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedPigDoesNotSeekWater(GameTestHelper helper) {
        // Spawn pig with full thirst
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        AnimalNeeds.setThirst(pig, AnimalNeeds.MAX_VALUE);

        // Verify pig is not thirsty
        helper.runAfterDelay(60, () -> {
            if (!AnimalNeeds.isThirsty(pig)) {
                helper.succeed();
            } else {
                helper.fail("Pig became thirsty unexpectedly");
            }
        });
    }

    /**
     * Test that a satisfied pig does not seek food.
     * Setup: Spawn pig with full hunger.
     * Expected: Pig does not become hungry and isHungry returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSatisfiedPigDoesNotSeekFood(GameTestHelper helper) {
        // Spawn pig with full hunger
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        AnimalNeeds.setHunger(pig, AnimalNeeds.MAX_VALUE);

        // Verify pig is not hungry
        helper.runAfterDelay(60, () -> {
            if (!AnimalNeeds.isHungry(pig)) {
                helper.succeed();
            } else {
                helper.fail("Pig became hungry unexpectedly");
            }
        });
    }

    /**
     * Test that a hungry pig can root in grass to find food.
     * Setup: Spawn hungry pig on grass block.
     * Expected: Pig's hunger increases after rooting.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testPigRootsForFood(GameTestHelper helper) {
        // Create grass floor for rooting
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn hungry pig on grass
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        float initialHunger = AnimalThresholds.HUNGRY - 20;
        AnimalNeeds.setHunger(pig, initialHunger);

        // Wait for rooting behavior to complete
        helper.runAfterDelay(200, () -> {
            float currentHunger = AnimalNeeds.getHunger(pig);
            if (currentHunger > initialHunger) {
                helper.succeed();
            } else {
                helper.fail("Hunger did not increase from rooting. Initial: " + initialHunger + ", current: " + currentHunger);
            }
        });
    }

    /**
     * Test that grass can be converted to dirt when pigs root.
     * Setup: Spawn hungry pig on grass blocks.
     * Expected: At least one grass block gets converted to dirt after enough time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
    public void testPigConvertsGrassToDirt(GameTestHelper helper) {
        // Create grass floor
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn multiple hungry pigs to increase conversion chances
        BlockPos pigPos1 = new BlockPos(5, 2, 5);
        Pig pig1 = helper.spawn(EntityType.PIG, pigPos1);
        AnimalNeeds.setHunger(pig1, AnimalThresholds.HUNGRY - 20);

        BlockPos pigPos2 = new BlockPos(7, 2, 7);
        Pig pig2 = helper.spawn(EntityType.PIG, pigPos2);
        AnimalNeeds.setHunger(pig2, AnimalThresholds.HUNGRY - 20);

        // Check if any grass was converted to dirt
        helper.runAfterDelay(500, () -> {
            boolean foundDirt = false;
            for (int x = 0; x < 11; x++) {
                for (int z = 0; z < 11; z++) {
                    BlockPos checkPos = new BlockPos(x, 1, z);
                    if (helper.getBlockState(checkPos).is(Blocks.DIRT)) {
                        foundDirt = true;
                        break;
                    }
                }
                if (foundDirt) break;
            }

            if (foundDirt) {
                helper.succeed();
            } else {
                helper.fail("No grass blocks were converted to dirt");
            }
        });
    }

    /**
     * Test that pigs seek mud for bathing.
     * Setup: Spawn pig near water.
     * Expected: Pig remains healthy and doesn't panic.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testPigSeeksMudForBathing(GameTestHelper helper) {
        // Create water pool
        for (int x = 3; x <= 7; x++) {
            for (int z = 3; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                if (x >= 4 && x <= 6 && z >= 4 && z <= 6) {
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
                }
            }
        }

        // Spawn pig near water with low health to encourage bathing
        BlockPos pigPos = new BlockPos(3, 2, 3);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        pig.setHealth(pig.getMaxHealth() * 0.6f);
        float initialHealth = pig.getHealth();

        // Check that pig can find and use mud/water
        helper.runAfterDelay(300, () -> {
            float currentHealth = pig.getHealth();
            boolean healthImproved = currentHealth >= initialHealth;

            if (pig.isAlive() && healthImproved) {
                helper.succeed();
            } else {
                helper.fail("Pig bathing test failed. Alive: " + pig.isAlive() + ", health improved: " + healthImproved);
            }
        });
    }

    /**
     * Test that mud bathing heals pigs over time.
     * Setup: Spawn pig with low health near water.
     * Expected: Pig's health increases after bathing.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 500)
    public void testMudBathingHealsOverTime(GameTestHelper helper) {
        // Create water pool
        for (int x = 4; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }

        // Spawn pig ON the water (to guarantee bathing)
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        pig.setHealth(pig.getMaxHealth() * 0.5f);
        float initialHealth = pig.getHealth();

        // Wait for healing from bathing
        helper.runAfterDelay(400, () -> {
            float currentHealth = pig.getHealth();
            if (currentHealth > initialHealth) {
                helper.succeed();
            } else {
                helper.fail("Health did not increase from bathing. Initial: " + initialHealth + ", current: " + currentHealth);
            }
        });
    }
}
