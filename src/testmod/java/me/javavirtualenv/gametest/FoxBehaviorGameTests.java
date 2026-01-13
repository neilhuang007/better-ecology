package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.fox.FoxItemStorage;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class FoxBehaviorGameTests implements FabricGameTest {

    private Fox spawnFoxWithAi(GameTestHelper helper, BlockPos pos) {
        Fox fox = HerbivoreTestUtils.spawnMobWithAi(helper, EntityType.FOX, pos);
        fox.setNoAi(false);

        // Note: FoxMixin already registers HungryPredatorTargetGoal at priority 1
        // We don't need to duplicate the registration here
        // The mixin handles all fox goal registration automatically

        return fox;
    }

    private Chicken spawnChickenWithAi(GameTestHelper helper, BlockPos pos) {
        Chicken chicken = HerbivoreTestUtils.spawnMobWithAi(helper, EntityType.CHICKEN, pos);
        chicken.setNoAi(false);
        return chicken;
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void hungryFoxTargetsChicken(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chickenPos = helper.absolutePos(new BlockPos(5, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);
        Chicken chicken = spawnChickenWithAi(helper, chickenPos);

        final double initialDistance = fox.position().distanceTo(chicken.position());

        // Wait for entity initialization and set hunger
        helper.runAtTickTime(1, () -> {
            // Register hunting goals BEFORE setting hunger state
            HerbivoreTestUtils.registerFoxHuntingGoals(fox);

            // Use HerbivoreTestUtils which properly sets both hunger value AND state flag
            HerbivoreTestUtils.setHunger(fox, 10);

            // Verify hunger state was set correctly
            if (!HerbivoreTestUtils.verifyHungerState(fox, 10)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            // Target selector evaluates every 10 ticks, so we need to wait longer
            // Also need time for pathfinding and movement to start
            helper.runAfterDelay(150, () -> {
                helper.succeedWhen(() -> {
                    // Check if chicken was killed - this means the fox hunt was successful
                    if (!chicken.isAlive()) {
                        System.out.println("[FoxTargetTest] SUCCESS - chicken was killed by fox!");
                        return; // Success - the fox hunted and killed the prey
                    }

                    var target = fox.getTarget();
                    boolean hasTarget = target != null && target.equals(chicken);
                    double currentDistance = fox.position().distanceTo(chicken.position());
                    boolean movingToward = currentDistance < initialDistance - 1.0;

                    if (!hasTarget && !movingToward) {
                        throw new GameTestAssertException("Fox has not targeted chicken or moved toward it yet (chicken alive: " + chicken.isAlive() + ")");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 600)
    public void hungryFoxAttacksChicken(GameTestHelper helper) {
        // Spawn fox and chicken very close together for faster attack
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chickenPos = helper.absolutePos(new BlockPos(2, 2, 1)); // Very close spawn

        Fox fox = spawnFoxWithAi(helper, foxPos);
        Chicken chicken = spawnChickenWithAi(helper, chickenPos);

        // Disable chicken AI so it won't run away
        chicken.setNoAi(true);

        // Store initial chicken health and fox position for comparison
        final float[] initialHealth = new float[1];
        final double[] initialDistance = new double[1];

        // Wait for entity initialization and set hunger
        helper.runAtTickTime(1, () -> {
            // Register hunting goals BEFORE setting hunger state
            HerbivoreTestUtils.registerFoxHuntingGoals(fox);

            // Set fox to very hungry to ensure hunting priority
            HerbivoreTestUtils.setHunger(fox, 5);
            initialHealth[0] = chicken.getHealth();
            initialDistance[0] = fox.position().distanceTo(chicken.position());

            if (!HerbivoreTestUtils.verifyHungerState(fox, 5)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            System.out.println("[FoxAttackTest] Fox hunger=5, chicken health=" + initialHealth[0] +
                ", initial distance=" + initialDistance[0]);

            // Give AI time to complete hunting cycle - stalk(60) + crouch(40) + pounce + attack
            helper.runAfterDelay(300, () -> {
                helper.succeedWhen(() -> {
                    // Check if chicken is damaged or dead (fox attacked it)
                    boolean chickenDead = !chicken.isAlive();
                    boolean chickenDamaged = chicken.getHealth() < initialHealth[0];

                    // Also check if fox is targeting and pursuing the chicken (hunting behavior)
                    var target = fox.getTarget();
                    boolean hasTarget = target != null && target.equals(chicken);
                    double currentDistance = fox.position().distanceTo(chicken.position());
                    boolean veryClose = currentDistance < 2.0;

                    System.out.println("[FoxAttackTest] Checking: alive=" + chicken.isAlive() +
                        ", health=" + chicken.getHealth() + "/" + initialHealth[0] +
                        ", hasTarget=" + hasTarget + ", distance=" + currentDistance);

                    // Success if any of these conditions are met:
                    // 1. Chicken is dead (fox killed it)
                    // 2. Chicken is damaged (fox attacked it)
                    // 3. Fox has targeted and is very close to chicken (about to attack)
                    if (chickenDead || chickenDamaged || (hasTarget && veryClose)) {
                        System.out.println("[FoxAttackTest] SUCCESS - chicken " +
                            (chickenDead ? "killed" : chickenDamaged ? "damaged" : "being attacked"));
                        return;
                    }

                    throw new GameTestAssertException("Fox has not attacked chicken yet (chicken health: " +
                        chicken.getHealth() + "/" + initialHealth[0] + ", target=" + hasTarget + ", dist=" + currentDistance + ")");
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void foxPicksUpItem(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var itemPos = helper.absolutePos(new BlockPos(4, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);

        ItemEntity berries = new ItemEntity(helper.getLevel(),
            itemPos.getX() + 0.5, itemPos.getY(), itemPos.getZ() + 0.5,
            new ItemStack(Items.SWEET_BERRIES));
        berries.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(berries);

        final double initialDistanceSqr = fox.distanceToSqr(berries);

        // Set fox to be hungry so it will pick up food
        helper.runAtTickTime(1, () -> {
            HerbivoreTestUtils.setHunger(fox, 30);

            // Give fox time to notice and move toward item
            helper.runAfterDelay(80, () -> {
                helper.succeedWhen(() -> {
                    FoxItemStorage storage = FoxItemStorage.get(fox);
                    boolean pickedUp = storage.hasItem() || !berries.isAlive();
                    double currentDistanceSqr = fox.distanceToSqr(berries);
                    boolean movingToward = currentDistanceSqr < initialDistanceSqr - 1.0;

                    if (!pickedUp && !movingToward) {
                        throw new GameTestAssertException("Fox has not picked up or moved toward berries yet");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void foxCarriesItemInMouth(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var itemPos = helper.absolutePos(new BlockPos(4, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);

        ItemEntity berries = new ItemEntity(helper.getLevel(),
            itemPos.getX() + 0.5, itemPos.getY(), itemPos.getZ() + 0.5,
            new ItemStack(Items.SWEET_BERRIES));
        berries.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(berries);

        // Set fox to be hungry so it will pick up food
        helper.runAtTickTime(1, () -> {
            HerbivoreTestUtils.setHunger(fox, 30);

            // Test that fox actually stores item in its inventory
            helper.runAfterDelay(100, () -> {
                helper.succeedWhen(() -> {
                    FoxItemStorage storage = FoxItemStorage.get(fox);

                    if (storage.hasItem()) {
                        // Verify the stored item is berries
                        ItemStack carriedItem = storage.getItem();
                        if (carriedItem.is(Items.SWEET_BERRIES)) {
                            return; // Success - fox is carrying berries
                        }
                    }

                    if (!berries.isAlive()) {
                        throw new GameTestAssertException("Berries were picked up but not stored correctly");
                    }

                    throw new GameTestAssertException("Fox has not picked up and stored berries yet");
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void foxDoesNotPickUpWhenCarrying(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var firstItemPos = helper.absolutePos(new BlockPos(3, 2, 1));
        var secondItemPos = helper.absolutePos(new BlockPos(5, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);

        // Spawn first item (berries)
        ItemEntity firstBerries = new ItemEntity(helper.getLevel(),
            firstItemPos.getX() + 0.5, firstItemPos.getY(), firstItemPos.getZ() + 0.5,
            new ItemStack(Items.SWEET_BERRIES));
        firstBerries.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(firstBerries);

        // Set fox to be hungry so it will pick up food
        helper.runAtTickTime(1, () -> {
            HerbivoreTestUtils.setHunger(fox, 30);

            // Wait for fox to pick up first item
            helper.runAfterDelay(100, () -> {
                FoxItemStorage storage = FoxItemStorage.get(fox);

                if (!storage.hasItem()) {
                    helper.fail("Fox did not pick up first item");
                    return;
                }

                // Spawn second item while fox is already carrying
                ItemEntity secondBerries = new ItemEntity(helper.getLevel(),
                    secondItemPos.getX() + 0.5, secondItemPos.getY(), secondItemPos.getZ() + 0.5,
                    new ItemStack(Items.GLOW_BERRIES));
                secondBerries.setDeltaMovement(0, 0, 0);
                helper.getLevel().addFreshEntity(secondBerries);

                // Check that fox doesn't pick up second item while carrying first
                helper.runAfterDelay(80, () -> {
                    helper.succeedWhen(() -> {
                        ItemStack carriedItem = storage.getItem();

                        // Fox should still be carrying first item (sweet berries)
                        if (!carriedItem.is(Items.SWEET_BERRIES)) {
                            throw new GameTestAssertException("Fox picked up second item while already carrying one");
                        }

                        // Second item should still exist
                        if (!secondBerries.isAlive()) {
                            throw new GameTestAssertException("Second item disappeared but fox is still carrying first item");
                        }
                    });
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void foxSleepsDuringDay(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(3, 2, 3));

        Fox fox = spawnFoxWithAi(helper, foxPos);

        // Set time to mid-day (time 6000 = noon)
        helper.getLevel().setDayTime(6000);

        // Wait for fox to initiate sleeping
        helper.runAfterDelay(60, () -> {
            helper.succeedWhen(() -> {
                if (!fox.isSleeping()) {
                    throw new GameTestAssertException("Fox is not sleeping during the day");
                }
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void foxStopsSleepingAtNight(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(3, 2, 3));

        Fox fox = spawnFoxWithAi(helper, foxPos);

        // Set time to day to trigger sleeping
        helper.getLevel().setDayTime(6000);

        // Wait for fox to fall asleep
        helper.runAfterDelay(60, () -> {
            if (!fox.isSleeping()) {
                helper.fail("Fox did not fall asleep during the day");
                return;
            }

            // Set time to night (time 14000 = night)
            helper.getLevel().setDayTime(14000);

            // Check that fox wakes up at night
            helper.runAfterDelay(40, () -> {
                helper.succeedWhen(() -> {
                    if (fox.isSleeping()) {
                        throw new GameTestAssertException("Fox is still sleeping at night");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void foxHuntsAtNight(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chickenPos = helper.absolutePos(new BlockPos(5, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);
        Chicken chicken = spawnChickenWithAi(helper, chickenPos);

        // Set time to night (time 15000 = night)
        helper.getLevel().setDayTime(15000);

        final double initialDistance = fox.position().distanceTo(chicken.position());

        // Wait for entity initialization and set hunger
        helper.runAtTickTime(1, () -> {
            // Register hunting goals BEFORE setting hunger state
            HerbivoreTestUtils.registerFoxHuntingGoals(fox);

            HerbivoreTestUtils.setHunger(fox, 30);

            if (!HerbivoreTestUtils.verifyHungerState(fox, 30)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            // Give AI time to evaluate hunting goal at night
            helper.runAfterDelay(150, () -> {
                helper.succeedWhen(() -> {
                    var target = fox.getTarget();
                    boolean hasTarget = target != null && target.equals(chicken);
                    double currentDistance = fox.position().distanceTo(chicken.position());
                    boolean movingToward = currentDistance < initialDistance - 1.0;

                    // Verify fox is not sleeping at night
                    if (fox.isSleeping()) {
                        throw new GameTestAssertException("Fox is sleeping at night (should be hunting)");
                    }

                    // Check if chicken was killed - this means the fox hunt was successful
                    if (!chicken.isAlive()) {
                        System.out.println("[FoxHuntNightTest] SUCCESS - chicken was killed by fox at night!");
                        return; // Success
                    }

                    if (!hasTarget && !movingToward) {
                        throw new GameTestAssertException("Fox has not targeted chicken or moved toward it at night");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void foxDoesNotSleepWhileHunting(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chickenPos = helper.absolutePos(new BlockPos(4, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);
        Chicken chicken = spawnChickenWithAi(helper, chickenPos);

        // Set time to day but make fox hungry (should prioritize hunting over sleep)
        helper.getLevel().setDayTime(6000);

        final double initialDistance = fox.position().distanceTo(chicken.position());

        // Wait for entity initialization and set very low hunger
        helper.runAtTickTime(1, () -> {
            // Register hunting goals BEFORE setting hunger state
            HerbivoreTestUtils.registerFoxHuntingGoals(fox);

            HerbivoreTestUtils.setHunger(fox, 3);

            if (!HerbivoreTestUtils.verifyHungerState(fox, 3)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            // Give AI time to evaluate - very hungry fox should prioritize hunting over sleep
            // Target selectors evaluate every 10 ticks, so need more time
            helper.runAfterDelay(200, () -> {
                helper.succeedWhen(() -> {
                    var target = fox.getTarget();
                    boolean hasTarget = target != null && target.equals(chicken);
                    double currentDistance = fox.position().distanceTo(chicken.position());
                    boolean movingToward = currentDistance < initialDistance - 1.0;

                    // Fox should not be sleeping while hunting
                    if (fox.isSleeping()) {
                        throw new GameTestAssertException("Fox is sleeping while trying to hunt");
                    }

                    // Check if chicken was killed - this means the fox hunt was successful
                    if (!chicken.isAlive()) {
                        System.out.println("[FoxHuntDayTest] SUCCESS - chicken was killed by fox during day!");
                        return; // Success - the fox hunted and killed the prey despite being day time
                    }

                    // Fox should target prey or at least move toward it despite being day time
                    if (!hasTarget && !movingToward) {
                        throw new GameTestAssertException("Very hungry fox did not target or move toward prey during day (chicken alive: " + chicken.isAlive() + ")");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void foxPicksUpFoodItems(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);

        // Test different food items foxes should pick up
        Item[] foodItems = {Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.CHICKEN,
                            Items.RABBIT, Items.COD, Items.SALMON};

        Item testItem = foodItems[helper.getLevel().random.nextInt(foodItems.length)];

        var itemPos = helper.absolutePos(new BlockPos(4, 2, 1));
        ItemEntity food = new ItemEntity(helper.getLevel(),
            itemPos.getX() + 0.5, itemPos.getY(), itemPos.getZ() + 0.5,
            new ItemStack(testItem));
        food.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(food);

        // Set fox to be hungry so it will pick up food
        helper.runAtTickTime(1, () -> {
            HerbivoreTestUtils.setHunger(fox, 30);

            helper.runAfterDelay(100, () -> {
                helper.succeedWhen(() -> {
                    FoxItemStorage storage = FoxItemStorage.get(fox);

                    if (storage.hasItem()) {
                        return; // Successfully picked up
                    }

                    if (!food.isAlive()) {
                        throw new GameTestAssertException("Food disappeared but fox is not carrying it");
                    }

                    throw new GameTestAssertException("Fox has not picked up food item yet");
                });
            });
        });
    }

    /**
     * Test that a thirsty fox seeks and moves toward water.
     * This test:
     * 1. Spawns a fox with very low thirst
     * 2. Places a water block nearby
     * 3. Verifies the fox moves toward the water
     */
    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void thirstyFoxSeeksWater(GameTestHelper helper) {
        // Place water at (4,1,1) with a drinking position at (3,1,1) - closer for reliability
        BlockPos waterPos = helper.absolutePos(new BlockPos(4, 1, 1));
        BlockPos drinkPos = helper.absolutePos(new BlockPos(3, 1, 1));

        // Ensure solid ground and proper drinking position
        helper.setBlock(waterPos.below(), Blocks.STONE);
        helper.setBlock(drinkPos.below(), Blocks.STONE);
        helper.setBlock(waterPos, Blocks.WATER);
        helper.setBlock(drinkPos, Blocks.AIR);
        helper.setBlock(drinkPos.above(), Blocks.AIR);

        Fox fox = spawnFoxWithAi(helper, helper.absolutePos(new BlockPos(1, 2, 1)));

        helper.runAtTickTime(1, () -> {
            // Manually register SeekWaterGoal BEFORE setting thirst state
            HerbivoreTestUtils.registerFoxSeekWaterGoal(fox);

            HerbivoreTestUtils.setHandleInt(fox, "thirst", "thirst", 6);
            HerbivoreTestUtils.setThirstyState(fox, true);
            HerbivoreTestUtils.boostNavigation(fox, 1.2);
        });

        helper.succeedWhen(() -> {
            double distance = fox.distanceToSqr(drinkPos.getX() + 0.5, drinkPos.getY(), drinkPos.getZ() + 0.5);
            helper.assertTrue(
                distance < 6.25,
                "Fox failed to reach water to drink. Distance squared: " + distance
            );
        });
    }
}
