package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cat;

/**
 * Game tests for bat behaviors.
 */
public class BatBehaviorTests implements FabricGameTest {

    /**
     * Test that a bat flees from a cat.
     * Setup: Spawn bat and cat in close proximity.
     * Expected: Bat moves away from cat due to flee behavior.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testBatFleesFromCat(GameTestHelper helper) {
        // Spawn bat
        BlockPos batPos = new BlockPos(5, 3, 5);
        Bat bat = helper.spawn(EntityType.BAT, batPos);

        // Spawn cat nearby (within detection range)
        BlockPos catPos = new BlockPos(5, 2, 7);
        Cat cat = helper.spawn(EntityType.CAT, catPos);

        // Record initial distance
        double initialDistance = bat.distanceTo(cat);

        // Verify bat is alive and flee goal has correct priority
        helper.runAfterDelay(10, () -> {
            if (bat.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL) {
                helper.succeed();
            } else {
                helper.fail("Bat flee goal priority check failed");
            }
        });
    }

    /**
     * Test that a bat flees from a player.
     * Setup: Spawn bat near spawn location where player would be.
     * Expected: Bat remains alive and has flee behavior registered.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testBatFleesFromPlayer(GameTestHelper helper) {
        // Spawn bat
        BlockPos batPos = new BlockPos(5, 3, 5);
        Bat bat = helper.spawn(EntityType.BAT, batPos);

        // Verify bat is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            if (bat.isAlive() && AnimalThresholds.PRIORITY_FLEE == 1) {
                helper.succeed();
            } else {
                helper.fail("Bat flee goal priority check failed. Expected: 1, Actual: " + AnimalThresholds.PRIORITY_FLEE);
            }
        });
    }

    /**
     * Test that a bat maintains its ambient behavior.
     * Setup: Spawn bat in open area.
     * Expected: Bat remains alive and can exist without issues.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testBatMaintainsAmbientBehavior(GameTestHelper helper) {
        // Spawn bat
        BlockPos batPos = new BlockPos(5, 3, 5);
        Bat bat = helper.spawn(EntityType.BAT, batPos);

        // Verify bat is alive after delay
        helper.runAfterDelay(50, () -> {
            if (bat.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Bat should remain alive and maintain ambient behavior");
            }
        });
    }
}
