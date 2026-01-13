package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class SheepBehaviorGameTests implements FabricGameTest {

    private <T extends Mob> T spawnWithAi(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T mob = HerbivoreTestUtils.spawnMobWithAi(helper, type, pos);
        mob.setNoAi(false);
        return mob;
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void sheepMaintainsHerdCohesion(GameTestHelper helper) {
        List<Sheep> flock = new ArrayList<>();
        BlockPos centerPos = helper.absolutePos(new BlockPos(5, 2, 5));

        for (int i = 0; i < 5; i++) {
            int offsetX = (i % 2 == 0) ? 2 : -2;
            int offsetZ = (i / 2) * 3 - 3;
            BlockPos spawnPos = centerPos.offset(offsetX, 0, offsetZ);
            Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, spawnPos);
            HerbivoreTestUtils.boostNavigation(sheep, 0.7);
            flock.add(sheep);
        }

        helper.runAfterDelay(80, () -> {
            BlockPos flockCenter = calculateFlockCenter(flock);
            int scatteredCount = 0;

            for (Sheep sheep : flock) {
                double distance = sheep.blockPosition().distSqr(flockCenter);
                if (distance > 36.0) {
                    scatteredCount++;
                }
            }

            helper.assertTrue(scatteredCount <= 1,
                "Flock is too scattered: " + scatteredCount + " sheep far from center");
        });

        helper.succeedWhen(() -> {
            BlockPos flockCenter = calculateFlockCenter(flock);
            int closeCount = 0;

            for (Sheep sheep : flock) {
                if (sheep.blockPosition().closerThan(flockCenter, 5.0)) {
                    closeCount++;
                }
            }

            helper.assertTrue(closeCount >= 4,
                "Sheep did not maintain herd cohesion: " + closeCount + "/5 sheep near center");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 340)
    public void sheepFlockMovesTogether(GameTestHelper helper) {
        List<Sheep> flock = new ArrayList<>();
        BlockPos startPos = helper.absolutePos(new BlockPos(3, 2, 3));

        for (int i = 0; i < 4; i++) {
            BlockPos spawnPos = startPos.offset(i % 2, 0, i / 2);
            Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, spawnPos);
            HerbivoreTestUtils.boostNavigation(sheep, 0.8);
            flock.add(sheep);
        }

        BlockPos[] flockPositions = new BlockPos[1];
        helper.runAtTickTime(60, () -> {
            flockPositions[0] = calculateFlockCenter(flock);
        });

        helper.succeedWhen(() -> {
            if (flockPositions[0] == null) {
                return;
            }

            BlockPos currentCenter = calculateFlockCenter(flock);
            double movementDistance = currentCenter.distSqr(flockPositions[0]);

            helper.assertTrue(movementDistance > 4.0,
                "Flock did not move together: distance " + movementDistance);

            int closeToCenter = 0;
            for (Sheep sheep : flock) {
                if (sheep.blockPosition().closerThan(currentCenter, 6.0)) {
                    closeToCenter++;
                }
            }

            helper.assertTrue(closeToCenter >= 3,
                "Flock scattered while moving: " + closeToCenter + "/4 sheep near center");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void sheepGrazesWhenHungry(GameTestHelper helper) {
        BlockPos grassGroundPos = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos grassPos = helper.absolutePos(new BlockPos(3, 2, 3));

        helper.setBlock(grassGroundPos, Blocks.STONE);
        helper.setBlock(grassPos, Blocks.SHORT_GRASS);

        Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, helper.absolutePos(new BlockPos(1, 2, 1)));

        helper.runAtTickTime(1, () -> {
            HerbivoreTestUtils.setHunger(sheep, 12);
            HerbivoreTestUtils.boostNavigation(sheep, 0.8);
            HerbivoreTestUtils.registerSheepGrazeGoal(sheep);
        });

        helper.succeedWhen(() -> {
            boolean grassEaten = !helper.getBlockState(grassPos).is(Blocks.SHORT_GRASS);
            helper.assertTrue(grassEaten, "Sheep did not eat the grass block");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void sheepSeeksGrassWhenMultipleSources(GameTestHelper helper) {
        BlockPos grass1GroundPos = helper.absolutePos(new BlockPos(1, 1, 5));
        BlockPos grass1Pos = helper.absolutePos(new BlockPos(1, 2, 5));
        BlockPos grass2GroundPos = helper.absolutePos(new BlockPos(7, 1, 5));
        BlockPos grass2Pos = helper.absolutePos(new BlockPos(7, 2, 5));

        helper.setBlock(grass1GroundPos, Blocks.STONE);
        helper.setBlock(grass1Pos, Blocks.SHORT_GRASS);
        helper.setBlock(grass2GroundPos, Blocks.STONE);
        helper.setBlock(grass2Pos, Blocks.SHORT_GRASS);

        Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, helper.absolutePos(new BlockPos(4, 2, 2)));

        helper.runAtTickTime(1, () -> {
            HerbivoreTestUtils.setHunger(sheep, 10);
            HerbivoreTestUtils.boostNavigation(sheep, 0.9);
            HerbivoreTestUtils.registerSheepGrazeGoal(sheep);
        });

        helper.succeedWhen(() -> {
            boolean grass1Eaten = !helper.getBlockState(grass1Pos).is(Blocks.SHORT_GRASS);
            boolean grass2Eaten = !helper.getBlockState(grass2Pos).is(Blocks.SHORT_GRASS);

            helper.assertTrue(
                grass1Eaten || grass2Eaten,
                "Sheep did not eat either grass source"
            );
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void sheepFleesWhenAttacked(GameTestHelper helper) {
        Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, helper.absolutePos(new BlockPos(1, 2, 1)));
        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, helper.absolutePos(new BlockPos(5, 2, 1)));
        HerbivoreTestUtils.boostNavigation(sheep, 0.9);

        HerbivoreTestUtils.setHealthPercent(sheep, 0.5f);

        wolf.setTarget(sheep);
        wolf.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));

        helper.runAtTickTime(20, () -> wolf.doHurtTarget(sheep));

        final int[] checkStartTick = {-1};
        helper.succeedWhen(() -> {
            long currentTick = helper.getTick();
            if (checkStartTick[0] == -1) {
                checkStartTick[0] = (int) currentTick;
            }
            long ticksSinceCheckStart = currentTick - checkStartTick[0];
            if (ticksSinceCheckStart < 40) {
                return;
            }

            helper.assertTrue(HerbivoreTestUtils.isRetreating(sheep), "Sheep state did not mark retreating after attack");
            helper.assertTrue(sheep.distanceToSqr(wolf) > 9.0, "Sheep did not flee far enough from wolf");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 360)
    public void sheepFlockFleesTogetherFromWolf(GameTestHelper helper) {
        List<Sheep> flock = new ArrayList<>();
        BlockPos flockCenter = helper.absolutePos(new BlockPos(4, 2, 4));

        for (int i = 0; i < 4; i++) {
            BlockPos spawnPos = flockCenter.offset((i % 2) * 2 - 1, 0, (i / 2) * 2 - 1);
            Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, spawnPos);
            HerbivoreTestUtils.boostNavigation(sheep, 0.9);
            flock.add(sheep);
        }

        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, flockCenter.offset(6, 0, 0));
        wolf.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));

        helper.runAtTickTime(30, () -> {
            if (flock.size() > 0) {
                wolf.setTarget(flock.get(0));
                HerbivoreTestUtils.setHealthPercent(flock.get(0), 0.4f);
                wolf.doHurtTarget(flock.get(0));
            }
        });

        helper.succeedWhen(() -> {
            int fleeingCount = 0;
            for (Sheep sheep : flock) {
                boolean movingAway = sheep.distanceToSqr(wolf) > 16.0 ||
                    HerbivoreTestUtils.isRetreating(sheep);
                if (movingAway) {
                    fleeingCount++;
                }
            }

            helper.assertTrue(fleeingCount >= 3,
                "Flock did not flee together: " + fleeingCount + "/4 sheep fleeing");

            BlockPos currentFlockCenter = calculateFlockCenter(flock);
            int stillCohesive = 0;
            for (Sheep sheep : flock) {
                if (sheep.blockPosition().closerThan(currentFlockCenter, 8.0)) {
                    stillCohesive++;
                }
            }

            helper.assertTrue(stillCohesive >= 3,
                "Flock scattered while fleeing: " + stillCohesive + "/4 sheep still cohesive");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void sheepRegroupsAfterFleeing(GameTestHelper helper) {
        List<Sheep> flock = new ArrayList<>();
        BlockPos startPos = helper.absolutePos(new BlockPos(4, 2, 4));

        for (int i = 0; i < 3; i++) {
            BlockPos spawnPos = startPos.offset(i, 0, 0);
            Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, spawnPos);
            HerbivoreTestUtils.boostNavigation(sheep, 0.85);
            flock.add(sheep);
        }

        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, startPos.offset(5, 0, 0));

        helper.runAtTickTime(25, () -> {
            HerbivoreTestUtils.setHealthPercent(flock.get(0), 0.5f);
            wolf.setTarget(flock.get(0));
            wolf.doHurtTarget(flock.get(0));
        });

        final double[] maxSpread = {0.0};
        helper.runAtTickTime(100, () -> {
            for (int i = 0; i < flock.size(); i++) {
                for (int j = i + 1; j < flock.size(); j++) {
                    double dist = flock.get(i).distanceToSqr(flock.get(j));
                    if (dist > maxSpread[0]) {
                        maxSpread[0] = dist;
                    }
                }
            }
        });

        helper.succeedWhen(() -> {
            if (maxSpread[0] == 0.0) {
                return;
            }

            double currentSpread = 0.0;
            for (int i = 0; i < flock.size(); i++) {
                for (int j = i + 1; j < flock.size(); j++) {
                    double dist = flock.get(i).distanceToSqr(flock.get(j));
                    if (dist > currentSpread) {
                        currentSpread = dist;
                    }
                }
            }

            boolean regrouped = currentSpread < maxSpread[0] * 0.8;
            helper.assertTrue(regrouped,
                "Flock did not regroup after fleeing: spread " + currentSpread + " vs max " + maxSpread[0]);
        });
    }

    /**
     * Test that a thirsty sheep seeks and moves toward water.
     * This test:
     * 1. Spawns a sheep with very low thirst
     * 2. Places a water block nearby
     * 3. Verifies the sheep moves toward the water
     */
    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void thirstySheepSeeksWater(GameTestHelper helper) {
        // Place water at (4,1,1) with a drinking position at (3,1,1) - closer for reliability
        BlockPos waterPos = helper.absolutePos(new BlockPos(4, 1, 1));
        BlockPos drinkPos = helper.absolutePos(new BlockPos(3, 1, 1));

        helper.setBlock(waterPos.below(), Blocks.STONE);
        helper.setBlock(drinkPos.below(), Blocks.STONE);
        helper.setBlock(waterPos, Blocks.WATER);
        helper.setBlock(drinkPos, Blocks.AIR);
        helper.setBlock(drinkPos.above(), Blocks.AIR);

        Sheep sheep = spawnWithAi(helper, EntityType.SHEEP, helper.absolutePos(new BlockPos(1, 2, 1)));

        helper.runAtTickTime(1, () -> {
            // Manually register SeekWaterGoal BEFORE setting thirst state
            HerbivoreTestUtils.registerSheepSeekWaterGoal(sheep);

            HerbivoreTestUtils.setThirst(sheep, 6);
            HerbivoreTestUtils.setThirstyState(sheep, true);
            HerbivoreTestUtils.boostNavigation(sheep, 1.2);
        });

        helper.succeedWhen(() -> {
            double distance = sheep.distanceToSqr(drinkPos.getX() + 0.5, drinkPos.getY(), drinkPos.getZ() + 0.5);
            helper.assertTrue(
                distance < 6.25,
                "Sheep failed to reach water to drink. Distance squared: " + distance
            );
        });
    }

    private BlockPos calculateFlockCenter(List<Sheep> flock) {
        if (flock.isEmpty()) {
            return BlockPos.ZERO;
        }

        int sumX = 0, sumY = 0, sumZ = 0;
        for (Sheep sheep : flock) {
            BlockPos pos = sheep.blockPosition();
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }

        return new BlockPos(
            sumX / flock.size(),
            sumY / flock.size(),
            sumZ / flock.size()
        );
    }
}
