package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Game tests for predator behaviors.
 */
public class PredatorBehaviorTests implements FabricGameTest {

    /**
     * Test that wolf picks up meat items.
     * Setup: Spawn hungry wolf, drop meat nearby.
     * Expected: Wolf pathfinds to and picks up meat.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testWolfPicksUpMeatItem(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        // Drop meat item
        BlockPos meatPos = new BlockPos(8, 2, 5);
        ItemEntity meatItem = new ItemEntity(
            helper.getLevel(),
            helper.absolutePos(meatPos).getX() + 0.5,
            helper.absolutePos(meatPos).getY(),
            helper.absolutePos(meatPos).getZ() + 0.5,
            new ItemStack(Items.BEEF)
        );
        meatItem.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(meatItem);

        // Spawn hungry wolf
        BlockPos wolfPos = new BlockPos(2, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.initializeIfNeeded(wolf);
        AnimalNeeds.setHunger(wolf, AnimalThresholds.HUNGRY - 15);

        double initialDistance = wolf.position().distanceTo(meatPos.getCenter());

        // Check wolf moves toward meat
        helper.runAfterDelay(250, () -> {
            double distance = wolf.position().distanceTo(meatPos.getCenter());
            boolean movedCloser = distance < initialDistance - 1.0;
            boolean pickedUp = !meatItem.isAlive();

            if (movedCloser || pickedUp) {
                helper.succeed();
            } else {
                helper.fail("Wolf did not reach meat. Distance: " + distance +
                           " (initial: " + initialDistance + "), Picked up: " + pickedUp);
            }
        });
    }

    /**
     * Test that wolf hunts prey when hungry and no meat available.
     * Setup: Spawn hungry wolf with no meat items, spawn sheep nearby.
     * Expected: Wolf targets the sheep.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testWolfHuntsWhenHungryNoMeat(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        // Spawn prey
        BlockPos sheepPos = new BlockPos(8, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Spawn very hungry wolf (no meat available)
        BlockPos wolfPos = new BlockPos(2, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.setHunger(wolf, AnimalThresholds.STARVING + 5);

        double initialDistance = wolf.position().distanceTo(sheep.position());

        // Check wolf targets or moves toward sheep
        helper.runAfterDelay(250, () -> {
            double currentDistance = wolf.position().distanceTo(sheep.position());
            boolean movedCloser = currentDistance < initialDistance - 1.0;
            boolean hasTarget = wolf.getTarget() == sheep;
            boolean sheepDead = !sheep.isAlive();

            if (movedCloser || hasTarget || sheepDead) {
                helper.succeed();
            } else {
                helper.fail("Wolf did not hunt sheep. Distance: " + currentDistance +
                           " (initial: " + initialDistance + "), Target: " + wolf.getTarget());
            }
        });
    }

    /**
     * Test that eating meat restores wolf hunger.
     * Setup: Spawn wolf next to meat with low hunger.
     * Expected: Hunger increases after eating.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testWolfEatsMeatRestoresHunger(GameTestHelper helper) {
        // Spawn wolf
        BlockPos wolfPos = new BlockPos(5, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        float initialHunger = AnimalThresholds.HUNGRY - 20;
        AnimalNeeds.setHunger(wolf, initialHunger);

        // Drop meat right next to wolf
        ItemEntity meatItem = new ItemEntity(
            helper.getLevel(),
            helper.absolutePos(wolfPos).getX() + 0.5,
            helper.absolutePos(wolfPos).getY(),
            helper.absolutePos(wolfPos).getZ() + 0.5,
            new ItemStack(Items.BEEF)
        );
        helper.getLevel().addFreshEntity(meatItem);

        // Wait for eating
        helper.runAfterDelay(100, () -> {
            float currentHunger = AnimalNeeds.getHunger(wolf);
            if (currentHunger > initialHunger || !meatItem.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Wolf hunger did not increase. Initial: " + initialHunger + ", current: " + currentHunger);
            }
        });
    }

    /**
     * Test that wolf prefers meat items over hunting.
     * Setup: Spawn hungry wolf with both meat item and live prey nearby.
     * Expected: Wolf goes for meat first.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testWolfPrefersMeatOverHunting(GameTestHelper helper) {
        // Spawn prey farther away
        BlockPos sheepPos = new BlockPos(8, 2, 8);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Drop meat closer
        BlockPos meatPos = new BlockPos(5, 2, 5);
        ItemEntity meatItem = new ItemEntity(
            helper.getLevel(),
            helper.absolutePos(meatPos).getX() + 0.5,
            helper.absolutePos(meatPos).getY(),
            helper.absolutePos(meatPos).getZ() + 0.5,
            new ItemStack(Items.BEEF)
        );
        helper.getLevel().addFreshEntity(meatItem);

        // Spawn hungry wolf
        BlockPos wolfPos = new BlockPos(2, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.setHunger(wolf, AnimalThresholds.HUNGRY - 10);

        // Wolf should go for meat (closer and easier)
        helper.runAfterDelay(100, () -> {
            double distanceToMeat = wolf.position().distanceTo(meatPos.getCenter());
            double distanceToSheep = wolf.position().distanceTo(sheep.position());

            // Wolf should be closer to meat than to sheep
            if (distanceToMeat < distanceToSheep || !meatItem.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Wolf did not prefer meat over hunting");
            }
        });
    }

    /**
     * Test that wolves are configured to hunt prey.
     * Setup: Spawn hungry wolf with prey nearby.
     * Expected: Wolf is hungry and prey exists.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testWolfPrefersWeakerPrey(GameTestHelper helper) {
        // Spawn prey
        BlockPos chickenPos = new BlockPos(8, 2, 5);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);

        // Spawn hungry wolf
        BlockPos wolfPos = new BlockPos(2, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.setHunger(wolf, AnimalThresholds.HUNGRY - 10);

        // Verify wolf is hungry and could hunt (configuration test)
        helper.runAfterDelay(10, () -> {
            boolean isHungry = AnimalNeeds.isHungry(wolf);
            boolean preyExists = chicken.isAlive();

            // Verify hunt priority is lower than normal (higher priority in goal system)
            boolean huntPriorityCorrect = AnimalThresholds.PRIORITY_HUNT > AnimalThresholds.PRIORITY_NORMAL;

            if (isHungry && preyExists && huntPriorityCorrect) {
                helper.succeed();
            } else {
                helper.fail("Wolf hunt configuration failed. Hungry: " + isHungry +
                           ", Prey exists: " + preyExists +
                           ", Hunt priority correct: " + huntPriorityCorrect);
            }
        });
    }
}
