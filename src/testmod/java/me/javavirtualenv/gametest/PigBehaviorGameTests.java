package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
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
        BlockPos waterPos = helper.absolutePos(BlockPos.ZERO);
        helper.setBlock(waterPos, Blocks.WATER);

        Pig pig = spawnWithAi(helper, EntityType.PIG, helper.absolutePos(new BlockPos(3, 2, 3)));

        // Wait a tick for ecology component to initialize
        helper.runAtTickTime(1, () -> {
            HerbivoreTestUtils.setHandleInt(pig, "thirst", "thirst", 6);
            // Also set the thirsty state on EntityState to trigger behaviors
            HerbivoreTestUtils.setThirstyState(pig, true);
            HerbivoreTestUtils.boostNavigation(pig, 1.2);
        });

        // Wait longer for AI to evaluate goals and start moving
        helper.runAtTickTime(40, () -> {
            boolean movingTowardWater = !pig.getNavigation().isDone();
            boolean alreadyAtWater = pig.blockPosition().closerThan(waterPos, 2.5);
            if (!movingTowardWater && !alreadyAtWater) {
                helper.fail("Pig did not start moving toward water when thirsty");
            }
        });

        helper.succeedWhen(() -> helper.assertTrue(
            pig.blockPosition().closerThan(waterPos, 2.5),
            "Pig failed to reach water to drink"
        ));
    }
}
