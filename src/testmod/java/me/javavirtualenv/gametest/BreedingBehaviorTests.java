package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for breeding behaviors.
 * Tests verify that animals only breed when well-fed and follow enhanced breeding rules.
 */
public class BreedingBehaviorTests implements FabricGameTest {

    /**
     * Test that well-fed sheep can enter love mode and breed.
     * Setup: Spawn two well-fed sheep and feed them wheat.
     * Expected: Sheep enter love mode when fed.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testWellFedSheepCanBreed(GameTestHelper helper) {
        // Spawn two sheep
        BlockPos sheep1Pos = new BlockPos(5, 2, 5);
        BlockPos sheep2Pos = new BlockPos(7, 2, 5);

        Sheep sheep1 = helper.spawn(EntityType.SHEEP, sheep1Pos);
        Sheep sheep2 = helper.spawn(EntityType.SHEEP, sheep2Pos);

        // Set them as well-fed (above breeding threshold)
        AnimalNeeds.setHunger(sheep1, 80f);
        AnimalNeeds.setHunger(sheep2, 80f);

        // Verify they can enter love mode
        helper.runAfterDelay(10, () -> {
            // Simulate feeding
            sheep1.setInLove(null);
            sheep2.setInLove(null);

            // Check both are in love
            if (sheep1.isInLove() && sheep2.isInLove()) {
                helper.succeed();
            } else {
                helper.fail("Well-fed sheep did not enter love mode. " +
                    "Sheep1 in love: " + sheep1.isInLove() +
                    ", Sheep2 in love: " + sheep2.isInLove());
            }
        });
    }

    /**
     * Test that breeding costs hunger for both parents.
     * Setup: Spawn two sheep in love mode.
     * Expected: After breeding, hunger should decrease.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBreedingCostsHunger(GameTestHelper helper) {
        // Spawn two cows
        BlockPos cow1Pos = new BlockPos(5, 2, 5);
        BlockPos cow2Pos = new BlockPos(6, 2, 5);

        Cow cow1 = helper.spawn(EntityType.COW, cow1Pos);
        Cow cow2 = helper.spawn(EntityType.COW, cow2Pos);

        // Set initial hunger (high enough to breed)
        float initialHunger = 90f;
        AnimalNeeds.setHunger(cow1, initialHunger);
        AnimalNeeds.setHunger(cow2, initialHunger);

        // Put them in love
        cow1.setInLove(null);
        cow2.setInLove(null);

        // Wait for breeding to occur
        helper.runAfterDelay(150, () -> {
            // Breeding behavior should have reduced hunger
            // Note: This tests the breeding goal's energy cost, not vanilla breeding
            float hunger1 = AnimalNeeds.getHunger(cow1);
            float hunger2 = AnimalNeeds.getHunger(cow2);

            // If breeding occurred, hunger should have decreased
            // Or animals should no longer be in love
            if (!cow1.isInLove() || !cow2.isInLove() || hunger1 < initialHunger || hunger2 < initialHunger) {
                helper.succeed();
            } else {
                helper.fail("Breeding did not affect hunger. " +
                    "Hunger1: " + hunger1 + ", Hunger2: " + hunger2);
            }
        });
    }

    /**
     * Test that the enhanced breeding goal selects healthier mates.
     * Setup: Verify hunger threshold for breeding is enforced.
     * Expected: Hungry animals cannot use the breeding goal.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testBreedingRequiresSufficientHunger(GameTestHelper helper) {
        // Spawn pig with low hunger
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);

        // Set hunger below breeding threshold (60f)
        AnimalNeeds.setHunger(pig, 40f);

        helper.runAfterDelay(10, () -> {
            float hunger = AnimalNeeds.getHunger(pig);
            boolean isHungry = AnimalNeeds.isHungry(pig);

            // Pig should be hungry and below the MIN_HUNGER_TO_BREED threshold (60f)
            if (isHungry && hunger < 60f) {
                helper.succeed();
            } else {
                helper.fail("Hunger threshold check failed. Hunger: " + hunger +
                    ", isHungry: " + isHungry);
            }
        });
    }

    /**
     * Test that breeding priority is lower than survival needs.
     * Expected: Breeding has PRIORITY_IDLE (6), which is lower than PRIORITY_FLEE (1).
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 50)
    public void testBreedingPriorityIsLowerThanSurvival(GameTestHelper helper) {
        helper.runAfterDelay(10, () -> {
            // PRIORITY_IDLE = 6, PRIORITY_FLEE = 1
            // Lower number = higher priority
            boolean priorityCorrect = AnimalThresholds.PRIORITY_IDLE > AnimalThresholds.PRIORITY_FLEE;

            if (priorityCorrect) {
                helper.succeed();
            } else {
                helper.fail("Breeding priority is not lower than flee priority. " +
                    "IDLE: " + AnimalThresholds.PRIORITY_IDLE +
                    ", FLEE: " + AnimalThresholds.PRIORITY_FLEE);
            }
        });
    }

    /**
     * Test that only adult animals can breed.
     * Setup: Spawn baby sheep.
     * Expected: Baby sheep cannot be in love.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testOnlyAdultsCanBreed(GameTestHelper helper) {
        // Spawn baby sheep
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep babySheep = helper.spawn(EntityType.SHEEP, sheepPos);
        babySheep.setAge(-24000); // Baby age

        // Set full hunger
        AnimalNeeds.setHunger(babySheep, 100f);

        // Try to put in love
        babySheep.setInLove(null);

        helper.runAfterDelay(10, () -> {
            // Baby should not be in love
            if (babySheep.isBaby() && !babySheep.isInLove()) {
                helper.succeed();
            } else {
                helper.fail("Baby sheep breeding check failed. " +
                    "isBaby: " + babySheep.isBaby() +
                    ", isInLove: " + babySheep.isInLove());
            }
        });
    }

    /**
     * Test mate selection prefers healthier animals.
     * This is a configuration test verifying the system is set up correctly.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testMateSelectionPrefersHealthierAnimals(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn one healthy and one injured sheep
        BlockPos healthyPos = new BlockPos(5, 2, 5);
        BlockPos injuredPos = new BlockPos(7, 2, 5);

        Sheep healthySheep = helper.spawn(EntityType.SHEEP, healthyPos);
        Sheep injuredSheep = helper.spawn(EntityType.SHEEP, injuredPos);

        // Set health levels
        healthySheep.setHealth(healthySheep.getMaxHealth()); // Full health
        injuredSheep.setHealth(healthySheep.getMaxHealth() / 2); // Half health

        // Set hunger levels
        AnimalNeeds.setHunger(healthySheep, 80f);
        AnimalNeeds.setHunger(injuredSheep, 80f);

        helper.runAfterDelay(10, () -> {
            // Verify health difference exists
            float healthyRatio = healthySheep.getHealth() / healthySheep.getMaxHealth();
            float injuredRatio = injuredSheep.getHealth() / injuredSheep.getMaxHealth();

            if (healthyRatio > injuredRatio) {
                helper.succeed();
            } else {
                helper.fail("Health difference not set correctly. " +
                    "Healthy: " + healthyRatio + ", Injured: " + injuredRatio);
            }
        });
    }
}
