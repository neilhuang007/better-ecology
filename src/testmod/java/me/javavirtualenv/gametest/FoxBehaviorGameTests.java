package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.fox.FoxItemStorage;
import me.javavirtualenv.ecology.ai.HungryPredatorTargetGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class FoxBehaviorGameTests implements FabricGameTest {

    private Fox spawnFoxWithAi(GameTestHelper helper, BlockPos pos) {
        Fox fox = HerbivoreTestUtils.spawnMobWithAi(helper, EntityType.FOX, pos);
        fox.setNoAi(false);

        // Manually register fox hunting goals for gametest environment
        // This ensures HungryPredatorTargetGoal is registered properly
        if (fox instanceof Mob mob) {
            MobAccessor accessor = (MobAccessor) mob;

            // Register hungry predator targeting goal (targets chickens and rabbits when hungry)
            // Threshold 60 means fox hunts when moderately hungry
            HungryPredatorTargetGoal<LivingEntity> targetGoal = new HungryPredatorTargetGoal<>(
                fox,
                LivingEntity.class,
                60,
                target -> target instanceof Chicken || target instanceof Rabbit
            );
            accessor.betterEcology$getTargetSelector().addGoal(1, targetGoal);

            System.out.println("[FoxBehaviorGameTests] Registered HungryPredatorTargetGoal for fox " + fox.getId());
        }

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
            WolfGameTestHelpers.setHunger(fox, 10);

            if (!WolfGameTestHelpers.verifyHungerState(fox, 10)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            // Give AI time to evaluate targeting goal
            helper.runAfterDelay(100, () -> {
                helper.succeedWhen(() -> {
                    var target = fox.getTarget();
                    boolean hasTarget = target != null && target.equals(chicken);
                    double currentDistance = fox.position().distanceTo(chicken.position());
                    boolean movingToward = currentDistance < initialDistance - 1.0;

                    if (!hasTarget && !movingToward) {
                        throw new GameTestAssertException("Fox has not targeted chicken or moved toward it yet");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void hungryFoxAttacksChicken(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chickenPos = helper.absolutePos(new BlockPos(4, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);
        Chicken chicken = spawnChickenWithAi(helper, chickenPos);

        // Wait for entity initialization and set hunger
        helper.runAtTickTime(1, () -> {
            WolfGameTestHelpers.setHunger(fox, 10);

            if (!WolfGameTestHelpers.verifyHungerState(fox, 10)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            // Give AI time to hunt and attack
            helper.runAfterDelay(150, () -> {
                helper.succeedWhen(() -> {
                    // Check if chicken is damaged or dead (fox attacked it)
                    boolean chickenAttacked = !chicken.isAlive() ||
                                           chicken.getHealth() < chicken.getMaxHealth();

                    if (!chickenAttacked) {
                        throw new GameTestAssertException("Fox has not attacked chicken yet");
                    }
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
            WolfGameTestHelpers.setHunger(fox, 30);

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
            WolfGameTestHelpers.setHunger(fox, 30);

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
            WolfGameTestHelpers.setHunger(fox, 30);

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
            WolfGameTestHelpers.setHunger(fox, 30);

            if (!WolfGameTestHelpers.verifyHungerState(fox, 30)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            // Give AI time to evaluate hunting goal at night
            helper.runAfterDelay(100, () -> {
                helper.succeedWhen(() -> {
                    var target = fox.getTarget();
                    boolean hasTarget = target != null && target.equals(chicken);
                    double currentDistance = fox.position().distanceTo(chicken.position());
                    boolean movingToward = currentDistance < initialDistance - 1.0;

                    // Verify fox is not sleeping at night
                    if (fox.isSleeping()) {
                        throw new GameTestAssertException("Fox is sleeping at night (should be hunting)");
                    }

                    if (!hasTarget && !movingToward) {
                        throw new GameTestAssertException("Fox has not targeted chicken or moved toward it at night");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void foxDoesNotSleepWhileHunting(GameTestHelper helper) {
        var foxPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var chickenPos = helper.absolutePos(new BlockPos(4, 2, 1));

        Fox fox = spawnFoxWithAi(helper, foxPos);
        Chicken chicken = spawnChickenWithAi(helper, chickenPos);

        // Set time to day but make fox hungry (should prioritize hunting over sleep)
        helper.getLevel().setDayTime(6000);

        // Wait for entity initialization and set very low hunger
        helper.runAtTickTime(1, () -> {
            WolfGameTestHelpers.setHunger(fox, 5);

            if (!WolfGameTestHelpers.verifyHungerState(fox, 5)) {
                helper.fail("Fox hunger state not properly initialized");
                return;
            }

            // Give AI time to evaluate - very hungry fox should prioritize hunting over sleep
            helper.runAfterDelay(80, () -> {
                helper.succeedWhen(() -> {
                    var target = fox.getTarget();

                    // Fox should target prey despite being day time
                    if (target == null || !target.equals(chicken)) {
                        throw new GameTestAssertException("Very hungry fox did not target prey during day");
                    }

                    // Fox should not be sleeping while hunting
                    if (fox.isSleeping()) {
                        throw new GameTestAssertException("Fox is sleeping while trying to hunt");
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
            WolfGameTestHelpers.setHunger(fox, 30);

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
        // Place water at (5,1,1) with a drinking position at (4,1,1)
        BlockPos waterPos = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos drinkPos = helper.absolutePos(new BlockPos(4, 1, 1));

        // Ensure solid ground and proper drinking position
        helper.setBlock(waterPos.below(), Blocks.STONE);
        helper.setBlock(drinkPos.below(), Blocks.STONE);
        helper.setBlock(waterPos, Blocks.WATER);
        helper.setBlock(drinkPos, Blocks.AIR);
        helper.setBlock(drinkPos.above(), Blocks.AIR);

        Fox fox = spawnFoxWithAi(helper, helper.absolutePos(new BlockPos(1, 2, 1)));

        helper.runAtTickTime(1, () -> {
            // Manually register SeekWaterGoal for game test environment
            HerbivoreTestUtils.registerFoxSeekWaterGoal(fox);

            HerbivoreTestUtils.setHandleInt(fox, "thirst", "thirst", 6);
            HerbivoreTestUtils.setThirstyState(fox, true);
            HerbivoreTestUtils.boostNavigation(fox, 1.0);
        });

        helper.runAtTickTime(40, () -> {
            if (fox.getNavigation().isDone() && !fox.blockPosition().closerThan(drinkPos, 2.5)) {
                helper.fail("Fox did not start moving toward water when thirsty");
            }
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
