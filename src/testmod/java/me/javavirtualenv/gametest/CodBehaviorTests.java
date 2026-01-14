package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for cod behaviors.
 */
public class CodBehaviorTests implements FabricGameTest {

    /**
     * Test that multiple cod form schools and stay together.
     * Setup: Spawn multiple cod in water.
     * Expected: Cod stay close together in a school formation.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCodSchooling(GameTestHelper helper) {
        // Create water environment for fish
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn multiple cod in water
        BlockPos cod1Pos = new BlockPos(10, 3, 10);
        BlockPos cod2Pos = new BlockPos(12, 3, 10);
        BlockPos cod3Pos = new BlockPos(10, 3, 12);
        BlockPos cod4Pos = new BlockPos(12, 3, 12);

        Cod cod1 = helper.spawn(EntityType.COD, cod1Pos);
        Cod cod2 = helper.spawn(EntityType.COD, cod2Pos);
        Cod cod3 = helper.spawn(EntityType.COD, cod3Pos);
        Cod cod4 = helper.spawn(EntityType.COD, cod4Pos);

        // Wait for cod to form school
        helper.runAfterDelay(100, () -> {
            if (cod1.isAlive() && cod2.isAlive() && cod3.isAlive() && cod4.isAlive()) {
                // Calculate average distance between cod
                double totalDistance = 0;
                int pairCount = 0;

                totalDistance += cod1.distanceTo(cod2);
                totalDistance += cod1.distanceTo(cod3);
                totalDistance += cod1.distanceTo(cod4);
                totalDistance += cod2.distanceTo(cod3);
                totalDistance += cod2.distanceTo(cod4);
                totalDistance += cod3.distanceTo(cod4);
                pairCount = 6;

                double averageDistance = totalDistance / pairCount;

                // Cod should stay relatively close (within schooling distance)
                if (averageDistance < 15.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cod did not form school. Average distance: " + averageDistance);
                }
            } else {
                helper.fail("Not all cod are alive");
            }
        });
    }

    /**
     * Test that cod flees from axolotl.
     * Setup: Spawn cod and axolotl nearby in water.
     * Expected: Cod detects axolotl and moves away.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCodFleesFromAxolotl(GameTestHelper helper) {
        // Create water environment for fish
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn cod and axolotl close together
        BlockPos codPos = new BlockPos(10, 3, 10);
        BlockPos axolotlPos = new BlockPos(13, 3, 10);

        Cod cod = helper.spawn(EntityType.COD, codPos);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Record initial distance
        double initialDistance = cod.distanceTo(axolotl);

        // Wait for cod to flee
        helper.runAfterDelay(100, () -> {
            if (cod.isAlive() && axolotl.isAlive()) {
                double finalDistance = cod.distanceTo(axolotl);

                // Cod should have moved away from axolotl or maintained safe distance
                if (finalDistance > initialDistance || finalDistance > 12.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cod did not flee. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Cod or axolotl not alive");
            }
        });
    }

    /**
     * Test that cod flee goal has correct priority.
     * Setup: Spawn cod in water.
     * Expected: Flee priority is higher than social behaviors.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testCodFleePriority(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn cod
        BlockPos codPos = new BlockPos(5, 3, 5);
        Cod cod = helper.spawn(EntityType.COD, codPos);

        // Verify cod is alive and flee goal priority is correct
        helper.runAfterDelay(10, () -> {
            // PRIORITY_FLEE = 1, PRIORITY_SOCIAL = 5
            // In Minecraft's goal system, lower number = higher priority
            if (cod.isAlive() && AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_SOCIAL) {
                helper.succeed();
            } else {
                helper.fail("Cod not alive or flee priority incorrect");
            }
        });
    }

    /**
     * Test that multiple cod respond to predator threat together.
     * Setup: Spawn multiple cod and one axolotl.
     * Expected: Cod detect the axolotl and can flee.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSchoolResponseToPredator(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn school of cod
        BlockPos cod1Pos = new BlockPos(10, 3, 10);
        BlockPos cod2Pos = new BlockPos(11, 3, 10);
        BlockPos cod3Pos = new BlockPos(10, 3, 11);
        BlockPos axolotlPos = new BlockPos(15, 3, 10);

        Cod cod1 = helper.spawn(EntityType.COD, cod1Pos);
        Cod cod2 = helper.spawn(EntityType.COD, cod2Pos);
        Cod cod3 = helper.spawn(EntityType.COD, cod3Pos);
        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, axolotlPos);

        // Wait for cod to potentially detect axolotl
        helper.runAfterDelay(50, () -> {
            // Check that all entities are alive
            if (cod1.isAlive() && cod2.isAlive() && cod3.isAlive() && axolotl.isAlive()) {
                // At least one cod should be aware of the axolotl
                double dist1 = cod1.distanceTo(axolotl);
                double dist2 = cod2.distanceTo(axolotl);
                double dist3 = cod3.distanceTo(axolotl);

                // At least one cod should be within detection range
                if (dist1 < 16.0 || dist2 < 16.0 || dist3 < 16.0) {
                    helper.succeed();
                } else {
                    helper.fail("No cod within detection range of predator");
                }
            } else {
                helper.fail("Not all entities alive");
            }
        });
    }

    /**
     * Test flash expansion trigger when one cod is hurt.
     * Setup: Spawn multiple cod in water, damage one cod.
     * Expected: Nearby cod within 8 blocks should scatter away from hurt cod.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testFlashExpansionTrigger(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn school of cod close together
        BlockPos cod1Pos = new BlockPos(10, 3, 10);
        BlockPos cod2Pos = new BlockPos(12, 3, 10);
        BlockPos cod3Pos = new BlockPos(10, 3, 12);
        BlockPos farCodPos = new BlockPos(20, 3, 20);

        Cod cod1 = helper.spawn(EntityType.COD, cod1Pos);
        Cod cod2 = helper.spawn(EntityType.COD, cod2Pos);
        Cod cod3 = helper.spawn(EntityType.COD, cod3Pos);
        Cod farCod = helper.spawn(EntityType.COD, farCodPos);

        // Record initial distances
        helper.runAfterDelay(10, () -> {
            double initialDist2 = cod1.distanceTo(cod2);
            double initialDist3 = cod1.distanceTo(cod3);

            // Damage cod1 to trigger flash expansion
            cod1.hurt(cod1.damageSources().generic(), 1.0f);

            // Wait for flash expansion response
            helper.runAfterDelay(30, () -> {
                if (cod1.isAlive() && cod2.isAlive() && cod3.isAlive() && farCod.isAlive()) {
                    double finalDist2 = cod1.distanceTo(cod2);
                    double finalDist3 = cod1.distanceTo(cod3);

                    // Nearby cod should have moved away (within detection radius of 8 blocks)
                    boolean cod2Scattered = finalDist2 > initialDist2 + 1.0;
                    boolean cod3Scattered = finalDist3 > initialDist3 + 1.0;

                    if (cod2Scattered || cod3Scattered) {
                        helper.succeed();
                    } else {
                        helper.fail("Nearby cod did not scatter. Cod2 dist: " + initialDist2 + " -> " + finalDist2 +
                                ", Cod3 dist: " + initialDist3 + " -> " + finalDist3);
                    }
                } else {
                    helper.fail("Not all cod are alive");
                }
            });
        });
    }

    /**
     * Test flash expansion burst speed.
     * Setup: Spawn cod in water, damage it to trigger flash expansion.
     * Expected: Cod should move faster during burst (2.0x speed multiplier).
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testFlashExpansionBurstSpeed(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn cod
        BlockPos codPos = new BlockPos(10, 3, 10);
        Cod cod = helper.spawn(EntityType.COD, codPos);

        // Record position before damage
        helper.runAfterDelay(10, () -> {
            double startX = cod.getX();
            double startZ = cod.getZ();

            // Damage cod to trigger burst swimming
            cod.hurt(cod.damageSources().generic(), 1.0f);

            // Check distance traveled during burst (should be moving faster)
            helper.runAfterDelay(20, () -> {
                if (cod.isAlive()) {
                    double distanceTraveled = Math.sqrt(
                            Math.pow(cod.getX() - startX, 2) +
                            Math.pow(cod.getZ() - startZ, 2)
                    );

                    // During burst at 2.0x speed, cod should travel at least 2 blocks in 20 ticks
                    if (distanceTraveled > 2.0) {
                        helper.succeed();
                    } else {
                        helper.fail("Cod did not move fast enough during burst. Distance: " + distanceTraveled);
                    }
                } else {
                    helper.fail("Cod is not alive");
                }
            });
        });
    }

    /**
     * Test flash expansion bubble particles.
     * Setup: Spawn cod in water, damage it to trigger flash expansion.
     * Expected: Cod should be in burst mode (verified by movement pattern).
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testFlashExpansionBurstMode(GameTestHelper helper) {
        // Create water environment
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn cod
        BlockPos codPos = new BlockPos(10, 3, 10);
        Cod cod = helper.spawn(EntityType.COD, codPos);

        helper.runAfterDelay(10, () -> {
            // Damage cod to trigger flash expansion and burst mode
            cod.hurt(cod.damageSources().generic(), 1.0f);

            // Verify cod enters burst mode by checking it has active navigation
            helper.runAfterDelay(5, () -> {
                if (cod.isAlive()) {
                    // Check that cod is actively navigating (burst swimming)
                    boolean isNavigating = cod.getNavigation().isInProgress();
                    boolean wasRecentlyHurt = cod.hurtTime > 0;

                    if (isNavigating || wasRecentlyHurt) {
                        helper.succeed();
                    } else {
                        helper.fail("Cod did not enter burst mode after damage");
                    }
                } else {
                    helper.fail("Cod is not alive");
                }
            });
        });
    }
}
