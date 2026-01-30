package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.ProtectedAreaDetection;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for protected area detection and fence-respecting behaviors.
 *
 * <p>These tests verify that animals respect player-protected areas
 * (marked by fences, walls, or farmland) and don't destroy grass/crops there.
 */
public class ProtectedAreaTests implements FabricGameTest {

    /**
     * Test that fence blocks are detected as protection markers.
     * Setup: Place oak fence.
     * Expected: isProtectedArea returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void testFenceIsProtectionMarker(GameTestHelper helper) {
        BlockPos fencePos = new BlockPos(5, 2, 5);
        helper.setBlock(fencePos, Blocks.OAK_FENCE);

        helper.runAfterDelay(10, () -> {
            BlockPos checkPos = new BlockPos(5, 2, 6);
            boolean isProtected = ProtectedAreaDetection.isProtectedArea(
                helper.getLevel(), helper.absolutePos(checkPos));

            if (isProtected) {
                helper.succeed();
            } else {
                helper.fail("Fence was not detected as protection marker");
            }
        });
    }

    /**
     * Test that walls are detected as protection markers.
     * Setup: Place cobblestone wall.
     * Expected: isProtectedArea returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void testWallIsProtectionMarker(GameTestHelper helper) {
        BlockPos wallPos = new BlockPos(5, 2, 5);
        helper.setBlock(wallPos, Blocks.COBBLESTONE_WALL);

        helper.runAfterDelay(10, () -> {
            BlockPos checkPos = new BlockPos(5, 2, 6);
            boolean isProtected = ProtectedAreaDetection.isProtectedArea(
                helper.getLevel(), helper.absolutePos(checkPos));

            if (isProtected) {
                helper.succeed();
            } else {
                helper.fail("Wall was not detected as protection marker");
            }
        });
    }

    /**
     * Test that areas without fences are not protected.
     * Setup: Plain grass area with no fences.
     * Expected: isProtectedArea returns false.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void testOpenAreaIsNotProtected(GameTestHelper helper) {
        // Create grass area with no fences
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        helper.runAfterDelay(10, () -> {
            BlockPos checkPos = new BlockPos(5, 2, 5);
            boolean isProtected = ProtectedAreaDetection.isProtectedArea(
                helper.getLevel(), helper.absolutePos(checkPos));

            if (!isProtected) {
                helper.succeed();
            } else {
                helper.fail("Open area was incorrectly detected as protected");
            }
        });
    }

    /**
     * Test that pigs don't root in fenced areas.
     * Setup: Spawn hungry pig in fenced area with grass.
     * Expected: Grass blocks remain unchanged after time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testPigDoesNotRootInFencedArea(GameTestHelper helper) {
        // Create grass floor
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Create fence around the area
        for (int x = 1; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 2, 1), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(x, 2, 9), Blocks.OAK_FENCE);
        }
        for (int z = 1; z <= 9; z++) {
            helper.setBlock(new BlockPos(1, 2, z), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(9, 2, z), Blocks.OAK_FENCE);
        }

        // Spawn hungry pig inside fenced area
        BlockPos pigPos = new BlockPos(5, 2, 5);
        Pig pig = helper.spawn(EntityType.PIG, pigPos);
        AnimalNeeds.setHunger(pig, AnimalThresholds.HUNGRY - 20);

        // Count initial grass blocks
        int initialGrassCount = 0;
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK)) {
                    initialGrassCount++;
                }
            }
        }
        final int expectedGrass = initialGrassCount;

        // Verify grass blocks are preserved
        helper.runAfterDelay(300, () -> {
            int currentGrassCount = 0;
            for (int x = 2; x <= 8; x++) {
                for (int z = 2; z <= 8; z++) {
                    if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK)) {
                        currentGrassCount++;
                    }
                }
            }

            if (currentGrassCount == expectedGrass) {
                helper.succeed();
            } else {
                helper.fail("Pig rooted in fenced area. Grass before: " + expectedGrass + ", after: " + currentGrassCount);
            }
        });
    }

    /**
     * Test that sheep don't graze in fenced areas.
     * Setup: Spawn hungry sheep in fenced area with grass.
     * Expected: Grass blocks remain unchanged after time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testSheepDoesNotGrazeInFencedArea(GameTestHelper helper) {
        // Create grass floor
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Create fence around the area
        for (int x = 1; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 2, 1), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(x, 2, 9), Blocks.OAK_FENCE);
        }
        for (int z = 1; z <= 9; z++) {
            helper.setBlock(new BlockPos(1, 2, z), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(9, 2, z), Blocks.OAK_FENCE);
        }

        // Spawn hungry sheep inside fenced area
        BlockPos sheepPos = new BlockPos(5, 2, 5);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);
        AnimalNeeds.setHunger(sheep, AnimalThresholds.HUNGRY - 20);

        // Count initial grass blocks
        int initialGrassCount = 0;
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK)) {
                    initialGrassCount++;
                }
            }
        }
        final int expectedGrass = initialGrassCount;

        // Verify grass blocks are preserved
        helper.runAfterDelay(300, () -> {
            int currentGrassCount = 0;
            for (int x = 2; x <= 8; x++) {
                for (int z = 2; z <= 8; z++) {
                    if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK)) {
                        currentGrassCount++;
                    }
                }
            }

            if (currentGrassCount == expectedGrass) {
                helper.succeed();
            } else {
                helper.fail("Sheep grazed in fenced area. Grass before: " + expectedGrass + ", after: " + currentGrassCount);
            }
        });
    }

    /**
     * Test that cows don't graze in fenced areas.
     * Setup: Spawn hungry cow in fenced area with grass.
     * Expected: Grass blocks remain unchanged after time.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testCowDoesNotGrazeInFencedArea(GameTestHelper helper) {
        // Create grass floor
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Create fence around the area
        for (int x = 1; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 2, 1), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(x, 2, 9), Blocks.OAK_FENCE);
        }
        for (int z = 1; z <= 9; z++) {
            helper.setBlock(new BlockPos(1, 2, z), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(9, 2, z), Blocks.OAK_FENCE);
        }

        // Spawn hungry cow inside fenced area
        BlockPos cowPos = new BlockPos(5, 2, 5);
        Cow cow = helper.spawn(EntityType.COW, cowPos);
        AnimalNeeds.setHunger(cow, AnimalThresholds.HUNGRY - 20);

        // Count initial grass blocks
        int initialGrassCount = 0;
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK)) {
                    initialGrassCount++;
                }
            }
        }
        final int expectedGrass = initialGrassCount;

        // Verify grass blocks are preserved
        helper.runAfterDelay(300, () -> {
            int currentGrassCount = 0;
            for (int x = 2; x <= 8; x++) {
                for (int z = 2; z <= 8; z++) {
                    if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK)) {
                        currentGrassCount++;
                    }
                }
            }

            if (currentGrassCount == expectedGrass) {
                helper.succeed();
            } else {
                helper.fail("Cow grazed in fenced area. Grass before: " + expectedGrass + ", after: " + currentGrassCount);
            }
        });
    }

    /**
     * Test that animals can graze in open (unfenced) areas.
     * Setup: Spawn hungry pig on grass without fences.
     * Expected: At least some grass gets converted to dirt.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
    public void testPigCanRootInOpenArea(GameTestHelper helper) {
        // Create grass floor with NO fences
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn hungry pigs
        BlockPos pigPos1 = new BlockPos(5, 2, 5);
        Pig pig1 = helper.spawn(EntityType.PIG, pigPos1);
        AnimalNeeds.setHunger(pig1, AnimalThresholds.HUNGRY - 30);

        BlockPos pigPos2 = new BlockPos(7, 2, 7);
        Pig pig2 = helper.spawn(EntityType.PIG, pigPos2);
        AnimalNeeds.setHunger(pig2, AnimalThresholds.HUNGRY - 30);

        // Verify grass blocks are converted
        helper.runAfterDelay(500, () -> {
            boolean foundDirt = false;
            for (int x = 0; x < 11; x++) {
                for (int z = 0; z < 11; z++) {
                    if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.DIRT)) {
                        foundDirt = true;
                        break;
                    }
                }
                if (foundDirt) break;
            }

            if (foundDirt) {
                helper.succeed();
            } else {
                helper.fail("Pig did not root in open area (no grass converted to dirt)");
            }
        });
    }

    /**
     * Test that fence gates are also detected as protection.
     * Setup: Place oak fence gate.
     * Expected: isProtectedArea returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void testFenceGateIsProtectionMarker(GameTestHelper helper) {
        BlockPos gatePos = new BlockPos(5, 2, 5);
        helper.setBlock(gatePos, Blocks.OAK_FENCE_GATE);

        helper.runAfterDelay(10, () -> {
            BlockPos checkPos = new BlockPos(5, 2, 6);
            boolean isProtected = ProtectedAreaDetection.isProtectedArea(
                helper.getLevel(), helper.absolutePos(checkPos));

            if (isProtected) {
                helper.succeed();
            } else {
                helper.fail("Fence gate was not detected as protection marker");
            }
        });
    }

    /**
     * Test that nether brick fence is detected as protection.
     * Setup: Place nether brick fence.
     * Expected: isProtectedArea returns true.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void testNetherBrickFenceIsProtectionMarker(GameTestHelper helper) {
        BlockPos fencePos = new BlockPos(5, 2, 5);
        helper.setBlock(fencePos, Blocks.NETHER_BRICK_FENCE);

        helper.runAfterDelay(10, () -> {
            BlockPos checkPos = new BlockPos(5, 2, 6);
            boolean isProtected = ProtectedAreaDetection.isProtectedArea(
                helper.getLevel(), helper.absolutePos(checkPos));

            if (isProtected) {
                helper.succeed();
            } else {
                helper.fail("Nether brick fence was not detected as protection marker");
            }
        });
    }
}
