package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Ocelot;

/**
 * Game tests for ocelot behaviors.
 */
public class OcelotBehaviorTests implements FabricGameTest {

    /**
     * Test that ocelot hunts chickens when hungry.
     * Setup: Spawn hungry ocelot with chicken nearby.
     * Expected: Ocelot is hungry and hunt priority is configured correctly.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testOcelotHuntsChicken(GameTestHelper helper) {
        BlockPos ocelotPos = new BlockPos(5, 2, 5);
        BlockPos chickenPos = new BlockPos(8, 2, 5);

        Ocelot ocelot = helper.spawn(EntityType.OCELOT, ocelotPos);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);
        AnimalNeeds.setHunger(ocelot, AnimalThresholds.HUNGRY - 10);

        helper.runAfterDelay(10, () -> {
            boolean isHungry = AnimalNeeds.isHungry(ocelot);
            boolean preyExists = chicken.isAlive();
            boolean huntPriorityCorrect = AnimalThresholds.PRIORITY_HUNT > AnimalThresholds.PRIORITY_NORMAL;

            if (isHungry && preyExists && huntPriorityCorrect) {
                helper.succeed();
            } else {
                helper.fail("Ocelot hunt configuration failed. Hungry: " + isHungry +
                           ", Prey exists: " + preyExists +
                           ", Hunt priority correct: " + huntPriorityCorrect);
            }
        });
    }

    /**
     * Test that baby ocelot (kitten) follows adult ocelot.
     * Setup: Spawn baby ocelot and adult ocelot.
     * Expected: Baby ocelot is alive and follow parent goal is configured with correct priority.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testBabyOcelotFollowsAdult(GameTestHelper helper) {
        BlockPos babyPos = new BlockPos(5, 2, 5);
        BlockPos adultPos = new BlockPos(10, 2, 5);

        Ocelot baby = helper.spawn(EntityType.OCELOT, babyPos);
        Ocelot adult = helper.spawn(EntityType.OCELOT, adultPos);

        baby.setBaby(true);

        helper.runAfterDelay(10, () -> {
            boolean babyIsBaby = baby.isBaby();
            boolean adultIsAdult = !adult.isBaby();
            boolean bothAlive = baby.isAlive() && adult.isAlive();

            if (babyIsBaby && adultIsAdult && bothAlive) {
                helper.succeed();
            } else {
                helper.fail("Baby ocelot follow configuration failed. Baby is baby: " + babyIsBaby +
                           ", Adult is adult: " + adultIsAdult +
                           ", Both alive: " + bothAlive);
            }
        });
    }

    /**
     * Test that ocelot seeks food when hungry.
     * Setup: Set ocelot hunger to low value.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testOcelotSeeksFoodWhenHungry(GameTestHelper helper) {
        BlockPos ocelotPos = new BlockPos(5, 2, 5);
        Ocelot ocelot = helper.spawn(EntityType.OCELOT, ocelotPos);
        AnimalNeeds.initializeIfNeeded(ocelot);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(ocelot, hungryValue);

        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(ocelot);
            boolean isHungry = AnimalNeeds.isHungry(ocelot);

            if (isHungry) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that mother ocelot protects kittens.
     * Setup: Spawn adult ocelot, baby ocelot.
     * Expected: Adult ocelot is not a baby and protection priority is configured correctly.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testMotherOcelotProtectsKitten(GameTestHelper helper) {
        BlockPos motherPos = new BlockPos(5, 2, 5);
        BlockPos kittenPos = new BlockPos(7, 2, 5);

        Ocelot mother = helper.spawn(EntityType.OCELOT, motherPos);
        Ocelot kitten = helper.spawn(EntityType.OCELOT, kittenPos);
        kitten.setBaby(true);

        helper.runAfterDelay(10, () -> {
            boolean motherIsAdult = !mother.isBaby();
            boolean kittenIsBaby = kitten.isBaby();
            boolean protectionPriorityCorrect = AnimalThresholds.PRIORITY_CRITICAL < AnimalThresholds.PRIORITY_NORMAL;

            if (motherIsAdult && kittenIsBaby && protectionPriorityCorrect) {
                helper.succeed();
            } else {
                helper.fail("Mother protection configuration failed. Mother is adult: " + motherIsAdult +
                           ", Kitten is baby: " + kittenIsBaby +
                           ", Protection priority correct: " + protectionPriorityCorrect);
            }
        });
    }

    /**
     * Test that satisfied ocelot does not seek food.
     * Setup: Set ocelot hunger to max value.
     * Expected: isHungry returns false and isSatisfied returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSatisfiedOcelotDoesNotSeekFood(GameTestHelper helper) {
        BlockPos ocelotPos = new BlockPos(5, 2, 5);
        Ocelot ocelot = helper.spawn(EntityType.OCELOT, ocelotPos);
        AnimalNeeds.setHunger(ocelot, AnimalNeeds.MAX_VALUE);

        helper.runAfterDelay(10, () -> {
            boolean isHungry = AnimalNeeds.isHungry(ocelot);
            boolean isSatisfied = AnimalNeeds.isSatisfied(ocelot);

            if (!isHungry && isSatisfied) {
                helper.succeed();
            } else {
                helper.fail("Satisfied check failed. Expected hungry: false, got: " + isHungry + ", satisfied: " + isSatisfied);
            }
        });
    }
}
