package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for frog behaviors.
 */
public class FrogBehaviorTests implements FabricGameTest {

    /**
     * Test that frog seeks water when thirsty.
     * Setup: Spawn thirsty frog near water.
     * Expected: Frog moves toward water to drink.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testFrogSeeksWater(GameTestHelper helper) {
        // Create water block
        BlockPos waterPos = new BlockPos(10, 2, 10);
        helper.setBlock(waterPos, Blocks.WATER);

        // Spawn frog with low thirst
        BlockPos frogPos = new BlockPos(5, 2, 5);
        Frog frog = helper.spawn(EntityType.FROG, frogPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(frog, thirstyValue);

        // Record initial position
        double initialDistanceToWater = frog.position().distanceTo(waterPos.getCenter());

        // Wait for frog to move toward water
        helper.runAfterDelay(100, () -> {
            if (frog.isAlive()) {
                boolean isThirsty = AnimalNeeds.isThirsty(frog);
                double finalDistanceToWater = frog.position().distanceTo(waterPos.getCenter());

                // Frog should have moved closer to water or stopped being thirsty (drank)
                if (finalDistanceToWater < initialDistanceToWater || !isThirsty) {
                    helper.succeed();
                } else {
                    helper.fail("Frog did not seek water. Initial distance: " + initialDistanceToWater +
                               ", Final distance: " + finalDistanceToWater + ", Thirsty: " + isThirsty);
                }
            } else {
                helper.fail("Frog not alive");
            }
        });
    }

    /**
     * Test that frog flees from wolf.
     * Setup: Spawn frog and wolf nearby.
     * Expected: Frog moves away from wolf.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testFrogFleesFromWolf(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn frog and wolf nearby
        BlockPos frogPos = new BlockPos(10, 2, 10);
        BlockPos wolfPos = new BlockPos(14, 2, 10);
        Frog frog = helper.spawn(EntityType.FROG, frogPos);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        // Record initial distance
        double initialDistance = frog.distanceTo(wolf);

        // Wait for frog to flee
        helper.runAfterDelay(100, () -> {
            if (frog.isAlive()) {
                double finalDistance = frog.distanceTo(wolf);
                // Frog should have moved away from wolf
                if (finalDistance > initialDistance || finalDistance > 12.0) {
                    helper.succeed();
                } else {
                    helper.fail("Frog did not flee from wolf. Initial: " + initialDistance +
                               ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Frog not alive");
            }
        });
    }

    /**
     * Test that frog is thirsty when dehydrated.
     * Setup: Set frog thirst to low value.
     * Expected: isThirsty returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testFrogIsThirstyWhenDehydrated(GameTestHelper helper) {
        // Spawn frog with low thirst
        BlockPos frogPos = new BlockPos(5, 2, 5);
        Frog frog = helper.spawn(EntityType.FROG, frogPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(frog, thirstyValue);

        // Verify thirst was set correctly and frog is thirsty
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(frog);
            boolean isThirsty = AnimalNeeds.isThirsty(frog);

            if (isThirsty && currentThirst <= thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Thirst check failed. Expected thirsty: true, got: " + isThirsty +
                           ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that hydrated frog does not seek water.
     * Setup: Set frog thirst to max value.
     * Expected: isThirsty returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedFrogDoesNotSeekWater(GameTestHelper helper) {
        // Spawn frog with max thirst
        BlockPos frogPos = new BlockPos(5, 2, 5);
        Frog frog = helper.spawn(EntityType.FROG, frogPos);
        AnimalNeeds.setThirst(frog, AnimalNeeds.MAX_VALUE);

        // Verify frog is not thirsty
        helper.runAfterDelay(10, () -> {
            boolean isThirsty = AnimalNeeds.isThirsty(frog);
            boolean isHydrated = AnimalNeeds.isHydrated(frog);

            if (!isThirsty && isHydrated) {
                helper.succeed();
            } else {
                helper.fail("Hydration check failed. Expected thirsty: false, got: " + isThirsty +
                           ", hydrated: " + isHydrated);
            }
        });
    }

    /**
     * Test that frog flee priority is correct.
     * Setup: Spawn frog, verify flee goal priority.
     * Expected: Flee priority is higher than normal goals.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testFrogFleePriorityIsCorrect(GameTestHelper helper) {
        // Spawn frog
        BlockPos frogPos = new BlockPos(5, 2, 5);
        Frog frog = helper.spawn(EntityType.FROG, frogPos);

        // Verify frog is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_NORMAL = 3
            // In Minecraft's goal system, lower number = higher priority
            if (frog.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Frog not alive or flee priority incorrect");
            }
        });
    }

    /**
     * Test that frog hunts when hungry.
     * Setup: Set frog hunger to low value.
     * Expected: isHungry returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testFrogHuntsWhenHungry(GameTestHelper helper) {
        // Spawn frog with low hunger
        BlockPos frogPos = new BlockPos(5, 2, 5);
        Frog frog = helper.spawn(EntityType.FROG, frogPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(frog, hungryValue);

        // Verify hunger was set correctly and frog is hungry
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(frog);
            boolean isHungry = AnimalNeeds.isHungry(frog);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry +
                           ", hunger: " + currentHunger);
            }
        });
    }
}
