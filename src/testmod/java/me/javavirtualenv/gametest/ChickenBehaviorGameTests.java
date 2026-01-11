package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ChickenBehaviorGameTests implements FabricGameTest {

    private <T extends Mob> T spawnWithAi(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T mob = HerbivoreTestUtils.spawnMobWithAi(helper, type, pos);
        mob.setNoAi(false);
        return mob;
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 260)
    public void chickenFleesFromFox(GameTestHelper helper) {
        Chicken chicken = spawnWithAi(helper, EntityType.CHICKEN, helper.absolutePos(new BlockPos(1, 2, 1)));
        Fox fox = spawnWithAi(helper, EntityType.FOX, helper.absolutePos(new BlockPos(5, 2, 1)));

        HerbivoreTestUtils.boostNavigation(chicken, 0.9);
        // Lower chicken health to trigger LowHealthFleeGoal (requires < 80% health for chickens)
        HerbivoreTestUtils.setHealthPercent(chicken, 0.5f);

        fox.setTarget(chicken);
        double[] initialDistance = new double[]{chicken.distanceToSqr(fox)};

        helper.runAtTickTime(20, () -> fox.doHurtTarget(chicken));
        helper.runAtTickTime(30, () -> initialDistance[0] = chicken.distanceToSqr(fox));

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

            helper.assertTrue(HerbivoreTestUtils.isRetreating(chicken), "Chicken did not enter retreating state when threatened by fox");
            helper.assertTrue(chicken.distanceToSqr(fox) > initialDistance[0] + 4.0, "Chicken did not increase distance from fox while fleeing");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 280)
    public void hungryChickenSeeksSeeds(GameTestHelper helper) {
        var level = helper.getLevel();
        var seedPos = helper.absoluteVec(new Vec3(4, 2, 1));

        ItemEntity seeds = new ItemEntity(level, seedPos.x, seedPos.y, seedPos.z, new ItemStack(Items.WHEAT_SEEDS));
        seeds.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(seeds);

        Chicken chicken = spawnWithAi(helper, EntityType.CHICKEN, helper.absolutePos(new BlockPos(1, 2, 1)));
        HerbivoreTestUtils.setHandleInt(chicken, "hunger", "hunger", 10);
        // Also set the hungry state on EntityState to trigger behaviors
        HerbivoreTestUtils.setHungryState(chicken, true);
        HerbivoreTestUtils.boostNavigation(chicken, 0.85);

        helper.runAtTickTime(20, () -> {
            if (chicken.getNavigation().isDone()) {
                helper.fail("Chicken navigation did not start toward seeds");
            }
        });

        helper.succeedWhen(() -> helper.assertTrue(
            chicken.position().distanceToSqr(seedPos) < 4.0,
            "Hungry chicken did not move toward seeds"
        ));
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 260)
    public void chickenFleesFromOcelot(GameTestHelper helper) {
        Chicken chicken = spawnWithAi(helper, EntityType.CHICKEN, helper.absolutePos(new BlockPos(1, 2, 1)));
        Ocelot ocelot = spawnWithAi(helper, EntityType.OCELOT, helper.absolutePos(new BlockPos(5, 2, 1)));

        HerbivoreTestUtils.boostNavigation(chicken, 0.9);
        HerbivoreTestUtils.setHealthPercent(chicken, 0.6f);

        ocelot.setTarget(chicken);
        final double[] initialDistance = {chicken.distanceToSqr(ocelot)};

        helper.runAtTickTime(20, () -> ocelot.doHurtTarget(chicken));
        helper.runAtTickTime(30, () -> initialDistance[0] = chicken.distanceToSqr(ocelot));

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

            helper.assertTrue(HerbivoreTestUtils.isRetreating(chicken), "Chicken did not enter retreating state when threatened by ocelot");
            helper.assertTrue(chicken.distanceToSqr(ocelot) > initialDistance[0] + 4.0, "Chicken did not increase distance from ocelot while fleeing");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 260)
    public void chickenFleesFromCat(GameTestHelper helper) {
        Chicken chicken = spawnWithAi(helper, EntityType.CHICKEN, helper.absolutePos(new BlockPos(1, 2, 1)));
        Cat cat = spawnWithAi(helper, EntityType.CAT, helper.absolutePos(new BlockPos(5, 2, 1)));

        HerbivoreTestUtils.boostNavigation(chicken, 0.9);
        HerbivoreTestUtils.setHealthPercent(chicken, 0.6f);

        cat.setTarget(chicken);
        final double[] initialDistance = {chicken.distanceToSqr(cat)};

        helper.runAtTickTime(20, () -> cat.doHurtTarget(chicken));
        helper.runAtTickTime(30, () -> initialDistance[0] = chicken.distanceToSqr(cat));

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

            helper.assertTrue(HerbivoreTestUtils.isRetreating(chicken), "Chicken did not enter retreating state when threatened by cat");
            helper.assertTrue(chicken.distanceToSqr(cat) > initialDistance[0] + 4.0, "Chicken did not increase distance from cat while fleeing");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void chickensFlockTogether(GameTestHelper helper) {
        var level = helper.getLevel();
        List<Chicken> flock = new ArrayList<>();

        // Spawn a small flock of chickens
        for (int i = 0; i < 4; i++) {
            int x = 2 + (i % 2) * 2;
            int z = 2 + (i / 2) * 2;
            Chicken chicken = spawnWithAi(helper, EntityType.CHICKEN, helper.absolutePos(new BlockPos(x, 2, z)));
            flock.add(chicken);
            // Boost navigation to encourage movement
            HerbivoreTestUtils.boostNavigation(chicken, 0.75);
        }

        Chicken testChicken = flock.get(0);
        final double[] flockCohesion = {0.0};
        final int[] neighborCount = {0};

        helper.runAfterDelay(80, () -> {
            // Calculate flock cohesion - average distance to other chickens
            double totalDistance = 0.0;
            int neighbors = 0;

            for (Chicken other : flock) {
                if (other != testChicken) {
                    double distance = testChicken.distanceToSqr(other);
                    totalDistance += distance;
                    if (distance <= 64.0) { // 8 blocks cohesion radius squared
                        neighbors++;
                    }
                }
            }

            flockCohesion[0] = totalDistance / (flock.size() - 1);
            neighborCount[0] = neighbors;
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(neighborCount[0] >= 2, "Chicken should stay near at least 2 flock members");
            helper.assertTrue(flockCohesion[0] < 36.0, "Flock should maintain cohesion (average distance < 6 blocks)");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void chickenLaysEggWhenSafe(GameTestHelper helper) {
        var level = helper.getLevel();
        Chicken chicken = spawnWithAi(helper, EntityType.CHICKEN, helper.absolutePos(new BlockPos(3, 2, 3)));

        // Ensure chicken is healthy and safe to lay eggs
        chicken.setHealth(chicken.getMaxHealth());
        HerbivoreTestUtils.setHandleInt(chicken, "hunger", "hunger", 80);
        HerbivoreTestUtils.setHungryState(chicken, false);
        HerbivoreTestUtils.setRetreatingState(chicken, false);

        // Count eggs in the area
        final int[] eggCount = {0};

        helper.runAtTickTime(100, () -> {
            // Count eggs that have been dropped
            level.getEntitiesOfClass(ItemEntity.class, chicken.getBoundingBox().inflate(8.0))
                .stream()
                .filter(item -> item.getItem().is(Items.EGG))
                .forEach(item -> eggCount[0]++);
        });

        helper.succeedWhen(() -> {
            // Re-count eggs at final check
            int currentEggs = (int) level.getEntitiesOfClass(ItemEntity.class, chicken.getBoundingBox().inflate(8.0))
                .stream()
                .filter(item -> item.getItem().is(Items.EGG))
                .count();

            helper.assertTrue(currentEggs > 0 || eggCount[0] > 0, "Chicken should lay at least one egg when safe and healthy");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 380)
    public void chickenGroupFleeFromPredator(GameTestHelper helper) {
        var level = helper.getLevel();
        List<Chicken> flock = new ArrayList<>();

        // Spawn a flock of chickens
        for (int i = 0; i < 5; i++) {
            int x = 1 + i;
            Chicken chicken = spawnWithAi(helper, EntityType.CHICKEN, helper.absolutePos(new BlockPos(x, 2, 2)));
            flock.add(chicken);
            HerbivoreTestUtils.boostNavigation(chicken, 0.9);
            HerbivoreTestUtils.setHealthPercent(chicken, 0.6f);
        }

        Fox fox = spawnWithAi(helper, EntityType.FOX, helper.absolutePos(new BlockPos(1, 2, 6)));
        fox.setTarget(flock.get(0));

        helper.runAtTickTime(20, () -> {
            // Attack one chicken to trigger alarm
            fox.doHurtTarget(flock.get(0));
        });

        final int[] fleeingCount = {0};
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

            // Count chickens in retreating state or far from fox
            fleeingCount[0] = 0;
            for (Chicken chicken : flock) {
                if (HerbivoreTestUtils.isRetreating(chicken) || chicken.distanceToSqr(fox) > 25.0) {
                    fleeingCount[0]++;
                }
            }

            helper.assertTrue(fleeingCount[0] >= 3, "At least 3 chickens should flee from fox predator (group flee behavior)");
        });
    }
}
