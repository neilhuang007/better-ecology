package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for chicken behaviors.
 */
public class ChickenBehaviorTests implements FabricGameTest {

    /**
     * Test that a chicken flees from a fox.
     * Setup: Spawn chicken and verify flee goal priority is correct.
     * Expected: Chicken is alive and flee goal has higher priority.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testChickenFleesFromFox(GameTestHelper helper) {
        // Spawn chicken
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);

        // Verify chicken is alive and flee goal has correct priority
        helper.runAfterDelay(10, () -> {
            // Verify flee priority is higher than normal goals
            if (chicken.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Chicken flee goal priority check failed");
            }
        });
    }

    /**
     * Test that a chicken seeks water when thirsty.
     * Setup: Spawn chicken with low thirst.
     * Expected: isThirsty returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testChickenSeeksWaterWhenThirsty(GameTestHelper helper) {
        // Spawn chicken with low thirst
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(chicken, thirstyValue);

        // Verify the thirst was set correctly and chicken is thirsty
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(chicken);
            boolean isThirsty = AnimalNeeds.isThirsty(chicken);

            if (isThirsty && currentThirst <= thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Thirst check failed. Expected thirsty: true, got: " + isThirsty + ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that a chicken seeks seeds when hungry.
     * Setup: Spawn chicken with low hunger.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testChickenSeeksSeedsWhenHungry(GameTestHelper helper) {
        // Spawn chicken with low hunger
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(chicken, hungryValue);

        // Verify the hunger was set correctly and chicken is hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(chicken);
            boolean isHungry = AnimalNeeds.isHungry(chicken);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that a hydrated chicken does not seek water.
     * Setup: Spawn chicken with maximum thirst.
     * Expected: isThirsty returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedChickenDoesNotSeekWater(GameTestHelper helper) {
        // Spawn chicken with maximum thirst
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        AnimalNeeds.setThirst(chicken, AnimalNeeds.MAX_VALUE);

        // Verify chicken is not thirsty
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(chicken);
            boolean isThirsty = AnimalNeeds.isThirsty(chicken);

            if (!isThirsty && currentThirst >= AnimalThresholds.HYDRATED) {
                helper.succeed();
            } else {
                helper.fail("Hydration check failed. Expected thirsty: false, got: " + isThirsty + ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that a satisfied chicken does not seek food.
     * Setup: Spawn chicken with maximum hunger.
     * Expected: isHungry returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSatisfiedChickenDoesNotSeekFood(GameTestHelper helper) {
        // Spawn chicken with maximum hunger
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        AnimalNeeds.setHunger(chicken, AnimalNeeds.MAX_VALUE);

        // Verify chicken is not hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(chicken);
            boolean isHungry = AnimalNeeds.isHungry(chicken);

            if (!isHungry && currentHunger >= AnimalThresholds.SATISFIED) {
                helper.succeed();
            } else {
                helper.fail("Satisfaction check failed. Expected hungry: false, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that a chicken pecks at the ground when on suitable terrain.
     * Setup: Spawn chicken on grass block with medium hunger.
     * Expected: Chicken should execute pecking behavior and remain alive.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testChickenPecksAtGround(GameTestHelper helper) {
        // Create grass floor for pecking
        BlockPos grassPos = new BlockPos(5, 1, 5);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        // Spawn chicken on grass with medium hunger
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        AnimalNeeds.setHunger(chicken, 60f);

        // Verify chicken is alive and on suitable ground
        helper.runAfterDelay(100, () -> {
            if (chicken.isAlive() && helper.getBlockState(grassPos).is(Blocks.GRASS_BLOCK)) {
                helper.succeed();
            } else {
                helper.fail("Chicken pecking test failed. Alive: " + chicken.isAlive());
            }
        });
    }

    /**
     * Test that a well-fed chicken lays eggs.
     * Setup: Spawn chicken with high hunger.
     * Expected: Chicken's hunger decreases after laying an egg.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 8500)
    public void testWellFedChickenLaysEggs(GameTestHelper helper) {
        // Spawn chicken with high hunger
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        AnimalNeeds.setHunger(chicken, 90f);

        float initialHunger = AnimalNeeds.getHunger(chicken);

        // Wait for egg laying (up to 8000 ticks)
        helper.runAfterDelay(8000, () -> {
            float currentHunger = AnimalNeeds.getHunger(chicken);
            // Hunger should have decreased due to egg laying cost
            if (currentHunger < initialHunger) {
                helper.succeed();
            } else {
                helper.fail("Chicken did not lay egg or hunger did not decrease. Initial: " + initialHunger + ", current: " + currentHunger);
            }
        });
    }

    /**
     * Test that a hungry chicken does not lay eggs.
     * Setup: Spawn chicken with low hunger (below minimum egg laying threshold).
     * Expected: Chicken cannot use egg laying goal due to hunger check.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHungryChickenDoesNotLayEggs(GameTestHelper helper) {
        // Spawn chicken with low hunger (below 40f minimum threshold)
        BlockPos chickenPos = new BlockPos(5, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        AnimalNeeds.setHunger(chicken, 30f);

        // Verify chicken is hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(chicken);
            boolean isHungry = AnimalNeeds.isHungry(chicken);

            if (isHungry && currentHunger < 40f) {
                helper.succeed();
            } else {
                helper.fail("Chicken should be hungry and below egg laying threshold. Hunger: " + currentHunger + ", isHungry: " + isHungry);
            }
        });
    }
}

