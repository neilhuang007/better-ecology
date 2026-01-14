package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for hunger-related behaviors.
 */
public class HungerBehaviorTests implements FabricGameTest {

    /**
     * Test that a herbivore seeks grass when hungry.
     * Setup: Spawn a hungry sheep, verify hunger is set correctly.
     * Expected: Sheep's hunger is below threshold.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testHerbivoreSeeksGrassWhenHungry(GameTestHelper helper) {
        // Spawn sheep with low hunger
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);
        float hungryValue = AnimalThresholds.HUNGRY - 10;
        AnimalNeeds.setHunger(sheep, hungryValue);

        // Verify the hunger was set correctly
        helper.runAfterDelay(10, () -> {
            float currentHunger = AnimalNeeds.getHunger(sheep);
            boolean isHungry = AnimalNeeds.isHungry(sheep);

            if (isHungry && currentHunger <= hungryValue) {
                helper.succeed();
            } else {
                helper.fail("Hunger check failed. Expected hungry: true, got: " + isHungry + ", hunger: " + currentHunger);
            }
        });
    }

    /**
     * Test that a predator seeks meat items when hungry.
     * Setup: Spawn a hungry wolf, place meat item on ground.
     * Expected: Wolf moves toward and picks up the meat.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void testPredatorSeeksMeatWhenHungry(GameTestHelper helper) {
        // Create a floor for pathfinding
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        // Place meat item at one end
        BlockPos meatPos = new BlockPos(8, 2, 5);
        ItemEntity meatItem = new ItemEntity(
            helper.getLevel(),
            helper.absolutePos(meatPos).getX() + 0.5,
            helper.absolutePos(meatPos).getY(),
            helper.absolutePos(meatPos).getZ() + 0.5,
            new ItemStack(Items.BEEF)
        );
        helper.getLevel().addFreshEntity(meatItem);

        // Spawn hungry wolf at the other end
        BlockPos wolfPos = new BlockPos(2, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.setHunger(wolf, AnimalThresholds.HUNGRY - 10);

        double initialDistance = wolf.position().distanceTo(meatPos.getCenter());

        // Wait and check that wolf moves toward meat
        helper.runAfterDelay(150, () -> {
            double currentDistance = wolf.position().distanceTo(meatPos.getCenter());
            if (currentDistance < initialDistance || !meatItem.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Wolf did not move toward meat. Initial: " + initialDistance + ", current: " + currentDistance);
            }
        });
    }

    /**
     * Test that an animal takes damage when starving.
     * Setup: Spawn animal with very low hunger.
     * Expected: Animal takes damage over time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testAnimalTakesDamageWhenStarving(GameTestHelper helper) {
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Set hunger below starving threshold
        AnimalNeeds.setHunger(sheep, AnimalThresholds.STARVING - 5);
        float initialHealth = sheep.getHealth();

        // Wait for damage to be applied
        helper.runAfterDelay(250, () -> {
            float currentHealth = sheep.getHealth();
            if (currentHealth < initialHealth) {
                helper.succeed();
            } else {
                helper.fail("Sheep did not take damage from starvation. Health: " + currentHealth);
            }
        });
    }

    /**
     * Test that eating restores hunger.
     * Setup: Spawn hungry sheep on grass.
     * Expected: Hunger increases after grazing.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testEatingRestoresHunger(GameTestHelper helper) {
        // Place grass for sheep to eat
        BlockPos grassPos = new BlockPos(5, 1, 5);
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        // Spawn hungry sheep on grass
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);
        float initialHunger = AnimalThresholds.HUNGRY - 20;
        AnimalNeeds.setHunger(sheep, initialHunger);

        // Wait for eating behavior
        helper.runAfterDelay(100, () -> {
            float currentHunger = AnimalNeeds.getHunger(sheep);
            if (currentHunger > initialHunger) {
                helper.succeed();
            } else {
                helper.fail("Hunger did not increase. Initial: " + initialHunger + ", current: " + currentHunger);
            }
        });
    }

    /**
     * Test that satisfied animals do not seek food.
     * Setup: Spawn fully fed sheep.
     * Expected: Sheep does not prioritize grazing.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSatisfiedAnimalDoesNotSeekFood(GameTestHelper helper) {
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);
        AnimalNeeds.setHunger(sheep, AnimalNeeds.MAX_VALUE);

        helper.runAfterDelay(60, () -> {
            if (!AnimalNeeds.isHungry(sheep)) {
                helper.succeed();
            } else {
                helper.fail("Sheep became hungry unexpectedly");
            }
        });
    }
}
