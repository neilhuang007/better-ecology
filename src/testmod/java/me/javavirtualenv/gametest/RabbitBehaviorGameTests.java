package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class RabbitBehaviorGameTests implements FabricGameTest {

    private <T extends Mob> T spawnWithAi(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T mob = HerbivoreTestUtils.spawnMobWithAi(helper, type, pos);
        mob.setNoAi(false);
        return mob;
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void rabbitFleesFromWolf(GameTestHelper helper) {
        BlockPos rabbitPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos wolfPos = helper.absolutePos(new BlockPos(5, 2, 1));

        Rabbit rabbit = spawnWithAi(helper, EntityType.RABBIT, rabbitPos);
        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, wolfPos);
        HerbivoreTestUtils.boostNavigation(rabbit, 0.9);
        HerbivoreTestUtils.setHealthPercent(rabbit, 0.5f);

        double[] initialDistance = new double[]{rabbit.distanceToSqr(wolf)};

        wolf.setTarget(rabbit);
        wolf.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));

        helper.runAtTickTime(25, () -> wolf.doHurtTarget(rabbit));
        helper.runAtTickTime(35, () -> initialDistance[0] = rabbit.distanceToSqr(wolf));

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

            helper.assertTrue(HerbivoreTestUtils.isRetreating(rabbit), "Rabbit did not enter retreating state when pursued by wolf");
            double currentDistance = rabbit.distanceToSqr(wolf);
            helper.assertTrue(currentDistance > initialDistance[0], "Rabbit distance from wolf did not increase while fleeing");
            helper.assertTrue(currentDistance > 12.0, "Rabbit did not create sufficient distance from wolf");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 260)
    public void hungryRabbitSeeksCarrots(GameTestHelper helper) {
        BlockPos rabbitPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos carrotPos = helper.absolutePos(new BlockPos(5, 2, 1));

        ItemEntity carrot = new ItemEntity(helper.getLevel(), carrotPos.getX() + 0.5, carrotPos.getY(), carrotPos.getZ() + 0.5, new ItemStack(Items.CARROT));
        carrot.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(carrot);

        Rabbit rabbit = spawnWithAi(helper, EntityType.RABBIT, rabbitPos);
        HerbivoreTestUtils.setHandleInt(rabbit, "hunger", "hunger", 10);
        HerbivoreTestUtils.setHungryState(rabbit, true);
        HerbivoreTestUtils.boostNavigation(rabbit, 0.9);

        helper.runAtTickTime(20, () -> {
            if (rabbit.getNavigation().isDone()) {
                helper.fail("Rabbit navigation did not start toward carrot");
            }
        });

        helper.succeedWhen(() -> helper.assertTrue(
            rabbit.distanceToSqr(carrot) < 3.0,
            "Hungry rabbit did not approach carrot item"
        ));
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void rabbitFleesFromPlayer(GameTestHelper helper) {
        BlockPos rabbitPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos playerPos = helper.absolutePos(new BlockPos(5, 2, 2));

        Rabbit rabbit = spawnWithAi(helper, EntityType.RABBIT, rabbitPos);
        HerbivoreTestUtils.boostNavigation(rabbit, 0.9);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);

        double[] initialDistance = new double[]{rabbit.distanceToSqr(player)};

        helper.succeedWhen(() -> {
            double currentDistance = rabbit.distanceToSqr(player);
            helper.assertTrue(currentDistance > initialDistance[0], "Rabbit did not increase distance from approaching player");
            helper.assertTrue(currentDistance > 9.0, "Rabbit did not create sufficient distance from player");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void rabbitUsesZigzagEscape(GameTestHelper helper) {
        BlockPos rabbitPos = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos wolfPos = helper.absolutePos(new BlockPos(1, 2, 3));

        Rabbit rabbit = spawnWithAi(helper, EntityType.RABBIT, rabbitPos);
        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, wolfPos);
        HerbivoreTestUtils.boostNavigation(rabbit, 0.95);

        HerbivoreTestUtils.setHealthPercent(rabbit, 0.5f);
        wolf.setTarget(rabbit);

        var initialPos = rabbit.blockPosition();
        double[] directionChanges = new double[]{0};
        BlockPos[] previousPos = new BlockPos[]{initialPos};

        helper.runAtTickTime(15, () -> wolf.doHurtTarget(rabbit));

        helper.runAfterDelay(5, () -> {
            var currentPos = rabbit.blockPosition();
            double dx = currentPos.getX() - previousPos[0].getX();
            double dz = currentPos.getZ() - previousPos[0].getZ();
            if (Math.abs(dx) > 0.5 || Math.abs(dz) > 0.5) {
                directionChanges[0]++;
            }
            previousPos[0] = currentPos;
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(HerbivoreTestUtils.isRetreating(rabbit), "Rabbit did not enter retreating state");
            double distanceMoved = rabbit.blockPosition().distSqr(initialPos);
            helper.assertTrue(distanceMoved > 16.0, "Rabbit did not move sufficiently far from initial position");
            helper.assertTrue(directionChanges[0] >= 2, "Rabbit did not show zigzag movement pattern");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 340)
    public void rabbitSeeksRefugeUnderThreat(GameTestHelper helper) {
        BlockPos rabbitPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos shelterPos = helper.absolutePos(new BlockPos(6, 2, 6));
        BlockPos wolfPos = helper.absolutePos(new BlockPos(3, 2, 3));

        Rabbit rabbit = spawnWithAi(helper, EntityType.RABBIT, rabbitPos);
        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, wolfPos);
        HerbivoreTestUtils.boostNavigation(rabbit, 0.9);

        HerbivoreTestUtils.setHealthPercent(rabbit, 0.4f);
        wolf.setTarget(rabbit);

        double initialDistToShelter = rabbit.blockPosition().distSqr(shelterPos);

        helper.runAtTickTime(10, () -> wolf.doHurtTarget(rabbit));

        helper.succeedWhen(() -> {
            double currentDistToShelter = rabbit.blockPosition().distSqr(shelterPos);
            helper.assertTrue(currentDistToShelter < initialDistToShelter, "Rabbit did not move toward shelter position when threatened");
            helper.assertTrue(HerbivoreTestUtils.isRetreating(rabbit), "Rabbit did not enter retreating state");
            helper.assertTrue(rabbit.distanceToSqr(wolf) > 8.0, "Rabbit did not maintain distance from wolf while seeking refuge");
        });
    }
}
