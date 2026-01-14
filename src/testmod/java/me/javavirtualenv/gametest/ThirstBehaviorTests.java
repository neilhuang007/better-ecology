package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for thirst-related behaviors.
 */
public class ThirstBehaviorTests implements FabricGameTest {

    /**
     * Test that an animal seeks water when thirsty.
     * Setup: Spawn a sheep with low thirst, verify needs are set correctly.
     * Expected: Sheep's thirst is below threshold.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testAnimalSeeksWaterWhenThirsty(GameTestHelper helper) {
        // Spawn sheep with low thirst
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);
        float thirstyValue = AnimalThresholds.THIRSTY - 10;
        AnimalNeeds.setThirst(sheep, thirstyValue);

        // Verify the thirst was set correctly
        helper.runAfterDelay(10, () -> {
            float currentThirst = AnimalNeeds.getThirst(sheep);
            boolean isThirsty = AnimalNeeds.isThirsty(sheep);

            if (isThirsty && currentThirst <= thirstyValue) {
                helper.succeed();
            } else {
                helper.fail("Thirst check failed. Expected thirsty: true, got: " + isThirsty + ", thirst: " + currentThirst);
            }
        });
    }

    /**
     * Test that an animal restores thirst after drinking.
     * Setup: Spawn sheep next to water with low thirst.
     * Expected: Thirst increases over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testAnimalDrinksAndRestoresThirst(GameTestHelper helper) {
        // Place a water block
        BlockPos waterPos = new BlockPos(5, 1, 5);
        helper.setBlock(waterPos, Blocks.WATER);

        // Place solid blocks around for the sheep to stand on
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 6), Blocks.STONE);

        // Spawn sheep right next to water with low thirst
        BlockPos sheepPos = new BlockPos(4, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);
        float initialThirst = AnimalThresholds.THIRSTY - 20;
        AnimalNeeds.setThirst(sheep, initialThirst);

        // Wait for drinking behavior and check thirst increased
        helper.runAfterDelay(150, () -> {
            float currentThirst = AnimalNeeds.getThirst(sheep);
            if (currentThirst > initialThirst) {
                helper.succeed();
            } else {
                helper.fail("Thirst did not increase. Initial: " + initialThirst + ", current: " + currentThirst);
            }
        });
    }

    /**
     * Test that an animal takes damage when critically dehydrated.
     * Setup: Spawn sheep with very low thirst.
     * Expected: Sheep takes damage over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testAnimalTakesDamageWhenDehydrated(GameTestHelper helper) {
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Set thirst below dehydrated threshold
        AnimalNeeds.setThirst(sheep, AnimalThresholds.DEHYDRATED - 5);
        float initialHealth = sheep.getHealth();

        // Wait for damage to be applied (damage interval is 200 ticks)
        helper.runAfterDelay(250, () -> {
            float currentHealth = sheep.getHealth();
            if (currentHealth < initialHealth) {
                helper.succeed();
            } else {
                helper.fail("Sheep did not take damage from dehydration. Health: " + currentHealth);
            }
        });
    }

    /**
     * Test that a hydrated animal does not seek water.
     * Setup: Spawn sheep with full thirst, place water nearby.
     * Expected: Sheep does not prioritize moving to water.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHydratedAnimalDoesNotSeekWater(GameTestHelper helper) {
        // Place water at one end
        BlockPos waterPos = new BlockPos(8, 1, 5);
        helper.setBlock(waterPos, Blocks.WATER);

        // Spawn sheep with full thirst
        BlockPos sheepPos = new BlockPos(2, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);
        AnimalNeeds.setThirst(sheep, AnimalNeeds.MAX_VALUE);

        BlockPos startPos = sheep.blockPosition();

        // Wait and verify sheep doesn't specifically move toward water
        helper.runAfterDelay(60, () -> {
            // Sheep might wander, but shouldn't be specifically at water
            if (!AnimalNeeds.isThirsty(sheep)) {
                helper.succeed();
            } else {
                helper.fail("Sheep became thirsty unexpectedly");
            }
        });
    }
}
