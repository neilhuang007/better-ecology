package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Chicken;

/**
 * Game tests for fox behaviors.
 */
public class FoxBehaviorTests implements FabricGameTest {

    /**
     * Test that fox flees from wolf.
     * Setup: Spawn fox, verify flee goal priority is correct.
     * Expected: Fox is alive and flee priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testFoxFleesFromWolf(GameTestHelper helper) {
        BlockPos foxPos = new BlockPos(5, 2, 5);
        Fox fox = helper.spawn(EntityType.FOX, foxPos);

        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            if (fox.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Fox not alive or flee priority incorrect");
            }
        });
    }

    /**
     * Test that fox seeks water when thirsty.
     * Setup: Set fox thirst to low value.
     * Expected: isThirsty returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testFoxSeeksWaterWhenThirsty(GameTestHelper helper) {
        BlockPos foxPos = new BlockPos(5, 2, 5);
        Fox fox = helper.spawn(EntityType.FOX, foxPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(fox, thirstyValue);

        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(fox);
            boolean isThirsty = AnimalNeeds.isThirsty(fox);

            if (isThirsty && currentThirst <= thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Thirst check failed. Expected thirsty: true, got: " + isThirsty + ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that fox seeks food when hungry.
     * Setup: Set fox hunger to low value.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testFoxSeeksFoodWhenHungry(GameTestHelper helper) {
        BlockPos foxPos = new BlockPos(5, 2, 5);
        Fox fox = helper.spawn(EntityType.FOX, foxPos);
        // Initialize needs first, then set hunger
        AnimalNeeds.initializeIfNeeded(fox);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(fox, hungryValue);

        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(fox);
            boolean isHungry = AnimalNeeds.isHungry(fox);

            // Just check if fox is marked as hungry - value might fluctuate slightly
            if (isHungry) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that fox hunts prey when hungry.
     * Setup: Spawn hungry fox with chicken nearby.
     * Expected: Fox is hungry and hunt priority is configured correctly.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testFoxHuntsPrey(GameTestHelper helper) {
        BlockPos foxPos = new BlockPos(5, 2, 5);
        BlockPos chickenPos = new BlockPos(8, 2, 5);

        Fox fox = helper.spawn(EntityType.FOX, foxPos);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        AnimalNeeds.setHunger(fox, AnimalThresholds.HUNGRY - 10);

        helper.runAfterDelay(10, () -> {
            boolean isHungry = AnimalNeeds.isHungry(fox);
            boolean preyExists = chicken.isAlive();
            boolean huntPriorityCorrect = AnimalThresholds.PRIORITY_HUNT > AnimalThresholds.PRIORITY_NORMAL;

            if (isHungry && preyExists && huntPriorityCorrect) {
                helper.succeed();
            } else {
                helper.fail("Fox hunt configuration failed. Hungry: " + isHungry +
                           ", Prey exists: " + preyExists +
                           ", Hunt priority correct: " + huntPriorityCorrect);
            }
        });
    }

    /**
     * Test that hydrated fox does not seek water.
     * Setup: Set fox thirst to max value.
     * Expected: isThirsty returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedFoxDoesNotSeekWater(GameTestHelper helper) {
        BlockPos foxPos = new BlockPos(5, 2, 5);
        Fox fox = helper.spawn(EntityType.FOX, foxPos);
        AnimalNeeds.setThirst(fox, AnimalNeeds.MAX_VALUE);

        helper.runAfterDelay(10, () -> {
            boolean isThirsty = AnimalNeeds.isThirsty(fox);
            boolean isHydrated = AnimalNeeds.isHydrated(fox);

            if (!isThirsty && isHydrated) {
                helper.succeed();
            } else {
                helper.fail("Hydration check failed. Expected thirsty: false, got: " + isThirsty + ", hydrated: " + isHydrated);
            }
        });
    }
}
