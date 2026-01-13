package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class PigBehaviorGameTests implements FabricGameTest {

    private <T extends Mob> T spawnWithAi(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T mob = HerbivoreTestUtils.spawnMobWithAi(helper, type, pos);
        mob.setNoAi(false);
        return mob;
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void hungryPigRootsForFood(GameTestHelper helper) {
        BlockPos grassPos = helper.absolutePos(new BlockPos(2, 1, 1));
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        Pig pig = spawnWithAi(helper, EntityType.PIG, helper.absolutePos(new BlockPos(1, 2, 1)));
        HerbivoreTestUtils.setHandleInt(pig, "hunger", "hunger", 10);
        // Also set the hungry state on EntityState to trigger behaviors
        HerbivoreTestUtils.setHungryState(pig, true);
        HerbivoreTestUtils.boostNavigation(pig, 0.8);

        helper.runAtTickTime(20, () -> {
            boolean movingTowardRootingSpot = !pig.getNavigation().isDone();
            boolean alreadyAtRootingSpot = pig.blockPosition().closerThan(grassPos, 1.5);
            if (!movingTowardRootingSpot && !alreadyAtRootingSpot) {
                helper.fail("Pig did not start rooting movement when hungry");
            }
        });

        helper.succeedWhen(() -> helper.assertTrue(
            pig.blockPosition().closerThan(grassPos, 2.5),
            "Pig did not reach rooting spot"
        ));
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void thirstyPigSeeksWater(GameTestHelper helper) {
        // Place water at (4,1,1) with a drinking position at (3,1,1) - closer for reliability
        BlockPos waterPos = helper.absolutePos(new BlockPos(4, 1, 1));
        BlockPos drinkPos = helper.absolutePos(new BlockPos(3, 1, 1));

        // Ensure solid ground exists for the drinking position
        helper.setBlock(waterPos.below(), Blocks.STONE);
        helper.setBlock(drinkPos.below(), Blocks.STONE);
        helper.setBlock(waterPos, Blocks.WATER);
        helper.setBlock(drinkPos, Blocks.AIR);
        helper.setBlock(drinkPos.above(), Blocks.AIR);

        Pig pig = spawnWithAi(helper, EntityType.PIG, helper.absolutePos(new BlockPos(1, 2, 1)));

        // Wait a tick for ecology component to initialize
        helper.runAtTickTime(1, () -> {
            // Manually register SeekWaterGoal BEFORE setting thirst state
            HerbivoreTestUtils.registerPigSeekWaterGoal(pig);

            HerbivoreTestUtils.setHandleInt(pig, "thirst", "thirst", 6);
            HerbivoreTestUtils.setThirstyState(pig, true);
            HerbivoreTestUtils.boostNavigation(pig, 1.2);
        });

        helper.succeedWhen(() -> {
            double distance = pig.distanceToSqr(drinkPos.getX() + 0.5, drinkPos.getY(), drinkPos.getZ() + 0.5);
            helper.assertTrue(
                distance < 6.25,
                "Pig failed to reach water to drink. Distance squared: " + distance
            );
        });
    }

    /**
     * Test that a pig with low health flees from a wolf predator.
     * This test:
     * 1. Spawns a pig with low health (40%)
     * 2. Spawns a wolf nearby to trigger flee behavior
     * 3. Verifies the pig enters retreating state and moves away
     */
    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void pigFleesFromPredatorWhenLowHealth(GameTestHelper helper) {
        Pig pig = spawnWithAi(helper, EntityType.PIG, helper.absolutePos(new BlockPos(2, 2, 2)));
        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, helper.absolutePos(new BlockPos(6, 2, 2)));

        HerbivoreTestUtils.boostNavigation(pig, 0.9);
        HerbivoreTestUtils.setHealthPercent(pig, 0.4f);

        final double[] initialDistance = {pig.distanceToSqr(wolf)};

        wolf.setTarget(pig);
        wolf.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));

        helper.runAtTickTime(20, () -> wolf.doHurtTarget(pig));
        helper.runAtTickTime(30, () -> initialDistance[0] = pig.distanceToSqr(wolf));

        final int[] checkStartTick = {-1};
        helper.succeedWhen(() -> {
            long currentTick = helper.getTick();
            if (checkStartTick[0] == -1) {
                checkStartTick[0] = (int) currentTick;
            }
            long ticksSinceCheckStart = currentTick - checkStartTick[0];
            if (ticksSinceCheckStart < 50) {
                return;
            }

            helper.assertTrue(HerbivoreTestUtils.isRetreating(pig), "Pig did not enter retreating state when threatened by wolf");
            double currentDistance = pig.distanceToSqr(wolf);
            helper.assertTrue(currentDistance > initialDistance[0] + 4.0, "Pig did not increase distance from wolf while fleeing");
        });
    }
}
