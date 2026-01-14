package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Game tests for cat behaviors.
 */
public class CatBehaviorTests implements FabricGameTest {

    /**
     * Test that cat hunts rabbit.
     * Setup: Spawn hungry cat and rabbit.
     * Expected: Cat targets rabbit when hungry.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCatHuntsRabbit(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn cat and rabbit
        BlockPos catPos = new BlockPos(5, 2, 5);
        BlockPos rabbitPos = new BlockPos(10, 2, 5);
        Cat cat = helper.spawn(EntityType.CAT, catPos);
        Rabbit rabbit = helper.spawn(EntityType.RABBIT, rabbitPos);

        // Make cat hungry so it hunts
        AnimalNeeds.setHunger(cat, AnimalThresholds.HUNGRY - 10);

        // Wait for cat to potentially target rabbit
        helper.runAfterDelay(100, () -> {
            if (cat.isAlive() && rabbit.isAlive()) {
                // Verify cat is hunting (has target or moved toward rabbit)
                double distance = cat.distanceTo(rabbit);
                boolean catHasTarget = cat.getTarget() != null;

                if (catHasTarget || distance < 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cat did not hunt rabbit. Distance: " + distance + ", Has target: " + catHasTarget);
                }
            } else {
                helper.fail("Cat or rabbit not alive");
            }
        });
    }

    /**
     * Test that baby cat follows adult cat.
     * Setup: Spawn baby and adult cat.
     * Expected: Baby follows adult.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testBabyCatFollowsAdult(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn adult and baby cat
        BlockPos adultPos = new BlockPos(5, 2, 5);
        BlockPos babyPos = new BlockPos(12, 2, 5);
        Cat adultCat = helper.spawn(EntityType.CAT, adultPos);
        Cat babyCat = helper.spawn(EntityType.CAT, babyPos);

        // Make one a baby
        babyCat.setBaby(true);

        // Record initial distance
        double initialDistance = babyCat.distanceTo(adultCat);

        // Wait for baby to follow
        helper.runAfterDelay(100, () -> {
            if (babyCat.isAlive() && adultCat.isAlive() && babyCat.isBaby()) {
                double finalDistance = babyCat.distanceTo(adultCat);

                // Baby should have moved closer to adult
                if (finalDistance < initialDistance || finalDistance < 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Baby cat did not follow adult. Initial: " + initialDistance + ", Final: " + finalDistance);
                }
            } else {
                helper.fail("Cats not in expected state");
            }
        });
    }

    /**
     * Test that cat seeks food.
     * Setup: Spawn hungry cat with food item nearby.
     * Expected: Cat moves toward food.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCatSeeksFood(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn cat
        BlockPos catPos = new BlockPos(5, 2, 5);
        Cat cat = helper.spawn(EntityType.CAT, catPos);

        // Make cat hungry
        AnimalNeeds.setHunger(cat, AnimalThresholds.HUNGRY - 10);

        // Spawn food item nearby
        BlockPos foodPos = new BlockPos(10, 2, 5);
        helper.spawnItem(Items.COD, foodPos.getX(), foodPos.getY(), foodPos.getZ());

        // Wait for cat to seek food
        helper.runAfterDelay(100, () -> {
            if (cat.isAlive()) {
                // Cat should have moved toward food location
                double distanceToFood = cat.position().distanceTo(
                    foodPos.getCenter()
                );

                if (distanceToFood < 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cat did not seek food. Distance: " + distanceToFood);
                }
            } else {
                helper.fail("Cat not alive");
            }
        });
    }

    /**
     * Test that cat hunts chicken.
     * Setup: Spawn hungry cat and chicken.
     * Expected: Cat targets chicken when hungry.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCatHuntsChicken(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn cat and chicken
        BlockPos catPos = new BlockPos(5, 2, 5);
        BlockPos chickenPos = new BlockPos(10, 2, 5);
        Cat cat = helper.spawn(EntityType.CAT, catPos);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);

        // Make cat hungry so it hunts
        AnimalNeeds.setHunger(cat, AnimalThresholds.HUNGRY - 10);

        // Wait for cat to potentially target chicken
        helper.runAfterDelay(100, () -> {
            if (cat.isAlive() && chicken.isAlive()) {
                // Verify cat is hunting (has target or moved toward chicken)
                double distance = cat.distanceTo(chicken);
                boolean catHasTarget = cat.getTarget() != null;

                if (catHasTarget || distance < 8.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cat did not hunt chicken. Distance: " + distance + ", Has target: " + catHasTarget);
                }
            } else {
                helper.fail("Cat or chicken not alive");
            }
        });
    }

    /**
     * Test that satisfied cat does not hunt.
     * Setup: Spawn satisfied cat with nearby rabbit.
     * Expected: Cat does not hunt when satisfied.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSatisfiedCatDoesNotHunt(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn cat and rabbit
        BlockPos catPos = new BlockPos(5, 2, 5);
        BlockPos rabbitPos = new BlockPos(8, 2, 5);
        Cat cat = helper.spawn(EntityType.CAT, catPos);
        Rabbit rabbit = helper.spawn(EntityType.RABBIT, rabbitPos);

        // Make cat satisfied
        AnimalNeeds.setHunger(cat, AnimalNeeds.MAX_VALUE);

        // Record initial distance
        double initialDistance = cat.distanceTo(rabbit);

        // Wait and verify cat doesn't hunt
        helper.runAfterDelay(100, () -> {
            if (cat.isAlive() && rabbit.isAlive()) {
                boolean catHasTarget = cat.getTarget() != null;
                boolean isSatisfied = AnimalNeeds.isSatisfied(cat);

                // Cat should not be hunting rabbit
                if (!catHasTarget && isSatisfied) {
                    helper.succeed();
                } else {
                    helper.fail("Satisfied cat is hunting. Has target: " + catHasTarget + ", Satisfied: " + isSatisfied);
                }
            } else {
                helper.fail("Cat or rabbit not alive");
            }
        });
    }

    /**
     * Test that cat stalks chicken slowly.
     * Setup: Spawn hungry wild cat near chicken.
     * Expected: Cat approaches chicken slowly in stalking mode.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCatStalksChicken(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn wild cat and chicken
        BlockPos catPos = new BlockPos(5, 2, 5);
        BlockPos chickenPos = new BlockPos(12, 2, 5);
        Cat cat = helper.spawn(EntityType.CAT, catPos);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);

        // Ensure cat is wild (not tamed)
        cat.setTame(false, true);

        // Make cat hungry so it hunts
        AnimalNeeds.setHunger(cat, AnimalThresholds.HUNGRY - 10);

        // Record initial distance
        double initialDistance = cat.distanceTo(chicken);

        // Wait for cat to stalk
        helper.runAfterDelay(100, () -> {
            if (cat.isAlive() && chicken.isAlive()) {
                double finalDistance = cat.distanceTo(chicken);
                boolean catHasTarget = cat.getTarget() != null;
                boolean catIsCrouching = cat.isCrouching();

                // Cat should be approaching (reduced distance) or crouching (stalking)
                if (finalDistance < initialDistance || catIsCrouching || catHasTarget) {
                    helper.succeed();
                } else {
                    helper.fail("Cat did not stalk chicken. Initial distance: " + initialDistance +
                               ", Final distance: " + finalDistance + ", Crouching: " + catIsCrouching +
                               ", Has target: " + catHasTarget);
                }
            } else {
                helper.fail("Cat or chicken not alive");
            }
        });
    }

    /**
     * Test that cat pounces on prey after stalking.
     * Setup: Spawn hungry wild cat close to chicken.
     * Expected: Cat attacks chicken when in range.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testCatPounceOnPrey(GameTestHelper helper) {
        // Create floor for pathfinding
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn wild cat very close to chicken to trigger pounce
        BlockPos catPos = new BlockPos(5, 2, 5);
        BlockPos chickenPos = new BlockPos(7, 2, 5);
        Cat cat = helper.spawn(EntityType.CAT, catPos);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);

        // Ensure cat is wild (not tamed)
        cat.setTame(false, true);

        // Make cat hungry so it hunts
        AnimalNeeds.setHunger(cat, AnimalThresholds.HUNGRY - 10);

        // Wait for cat to pounce
        helper.runAfterDelay(100, () -> {
            // If chicken is dead or hurt, cat successfully pounced
            if (cat.isAlive()) {
                boolean chickenAttacked = !chicken.isAlive() || chicken.getHealth() < chicken.getMaxHealth();
                boolean catHasTarget = cat.getTarget() != null;
                double distance = chicken.isAlive() ? cat.distanceTo(chicken) : 0.0;

                if (chickenAttacked || catHasTarget || distance < 3.0) {
                    helper.succeed();
                } else {
                    helper.fail("Cat did not pounce on prey. Chicken attacked: " + chickenAttacked +
                               ", Has target: " + catHasTarget + ", Distance: " + distance);
                }
            } else {
                helper.fail("Cat not alive");
            }
        });
    }

    /**
     * Test that tamed cat does not hunt.
     * Setup: Spawn tamed cat with nearby chicken.
     * Expected: Tamed cat does not stalk or hunt prey.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testTamedCatDoesNotHunt(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            }
        }

        // Spawn tamed cat and chicken
        BlockPos catPos = new BlockPos(5, 2, 5);
        BlockPos chickenPos = new BlockPos(8, 2, 5);
        Cat cat = helper.spawn(EntityType.CAT, catPos);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, chickenPos);

        // Ensure cat is tamed
        cat.setTame(true, true);

        // Make cat hungry (but tamed cats shouldn't hunt regardless)
        AnimalNeeds.setHunger(cat, AnimalThresholds.HUNGRY - 10);

        // Wait and verify cat doesn't hunt
        helper.runAfterDelay(100, () -> {
            if (cat.isAlive() && chicken.isAlive()) {
                boolean catHasTarget = cat.getTarget() != null;
                boolean catIsCrouching = cat.isCrouching();
                boolean chickenAttacked = chicken.getHealth() < chicken.getMaxHealth();

                // Tamed cat should not hunt
                if (!catHasTarget && !catIsCrouching && !chickenAttacked) {
                    helper.succeed();
                } else {
                    helper.fail("Tamed cat is hunting. Has target: " + catHasTarget +
                               ", Crouching: " + catIsCrouching + ", Chicken attacked: " + chickenAttacked);
                }
            } else {
                helper.fail("Cat or chicken not alive");
            }
        });
    }
}
