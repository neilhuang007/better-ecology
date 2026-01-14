package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;

/**
 * Game tests for tropical fish behaviors.
 */
public class TropicalFishBehaviorTests implements FabricGameTest {

    /**
     * Test that tropical fish exhibit schooling behavior and stay together.
     * Setup: Spawn multiple tropical fish in the same area.
     * Expected: Fish remain alive and within reasonable distance of each other.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testTropicalFishSchooling(GameTestHelper helper) {
        // Spawn three tropical fish in close proximity
        BlockPos fish1Pos = new BlockPos(5, 2, 5);
        BlockPos fish2Pos = new BlockPos(6, 2, 5);
        BlockPos fish3Pos = new BlockPos(5, 2, 6);

        TropicalFish fish1 = helper.spawn(EntityType.TROPICAL_FISH, fish1Pos);
        TropicalFish fish2 = helper.spawn(EntityType.TROPICAL_FISH, fish2Pos);
        TropicalFish fish3 = helper.spawn(EntityType.TROPICAL_FISH, fish3Pos);

        // Verify fish stay together after some time
        helper.runAfterDelay(100, () -> {
            // All fish should be alive
            boolean allAlive = fish1.isAlive() && fish2.isAlive() && fish3.isAlive();

            // Calculate average position (school center)
            double avgX = (fish1.getX() + fish2.getX() + fish3.getX()) / 3.0;
            double avgY = (fish1.getY() + fish2.getY() + fish3.getY()) / 3.0;
            double avgZ = (fish1.getZ() + fish2.getZ() + fish3.getZ()) / 3.0;

            // Check that all fish are within reasonable distance of center (school cohesion)
            double maxDistance = 16.0; // cohesion radius
            double dist1 = Math.sqrt(
                Math.pow(fish1.getX() - avgX, 2) +
                Math.pow(fish1.getY() - avgY, 2) +
                Math.pow(fish1.getZ() - avgZ, 2)
            );
            double dist2 = Math.sqrt(
                Math.pow(fish2.getX() - avgX, 2) +
                Math.pow(fish2.getY() - avgY, 2) +
                Math.pow(fish2.getZ() - avgZ, 2)
            );
            double dist3 = Math.sqrt(
                Math.pow(fish3.getX() - avgX, 2) +
                Math.pow(fish3.getY() - avgY, 2) +
                Math.pow(fish3.getZ() - avgZ, 2)
            );

            boolean schooling = dist1 <= maxDistance && dist2 <= maxDistance && dist3 <= maxDistance;

            if (allAlive && schooling) {
                helper.succeed();
            } else {
                helper.fail("Tropical fish schooling failed. All alive: " + allAlive +
                    ", distances from center: [" + String.format("%.2f", dist1) + ", " +
                    String.format("%.2f", dist2) + ", " + String.format("%.2f", dist3) + "]");
            }
        });
    }

    /**
     * Test that a tropical fish flees from an axolotl.
     * Setup: Spawn tropical fish and axolotl nearby.
     * Expected: Fish moves away from the axolotl.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testTropicalFishFleesFromAxolotl(GameTestHelper helper) {
        // Spawn tropical fish
        BlockPos fishPos = new BlockPos(5, 2, 5);
        TropicalFish fish = helper.spawn(EntityType.TROPICAL_FISH, fishPos);

        // Spawn axolotl nearby (predator)
        BlockPos axolotlPos = new BlockPos(8, 2, 5);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Record initial distance
        double initialDistance = fish.distanceTo(axolotl);

        // After some time, fish should have moved away from axolotl
        helper.runAfterDelay(60, () -> {
            double currentDistance = fish.distanceTo(axolotl);

            // Fish should either maintain or increase distance from predator
            // (fleeing increases distance or maintains safe distance)
            boolean fishAlive = fish.isAlive();
            boolean axolotlAlive = axolotl.isAlive();
            boolean fleeingOrSafe = currentDistance >= initialDistance - 2.0; // Allow small tolerance

            if (fishAlive && axolotlAlive && fleeingOrSafe) {
                helper.succeed();
            } else {
                helper.fail("Tropical fish flee test failed. Fish alive: " + fishAlive +
                    ", axolotl alive: " + axolotlAlive +
                    ", initial distance: " + String.format("%.2f", initialDistance) +
                    ", current distance: " + String.format("%.2f", currentDistance));
            }
        });
    }
}
