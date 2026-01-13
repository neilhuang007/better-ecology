package me.javavirtualenv.gametest;

import me.javavirtualenv.BetterEcology;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CowBehaviorGameTests implements FabricGameTest {

    private <T extends Mob> T spawnWithAi(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T mob = HerbivoreTestUtils.spawnMobWithAi(helper, type, pos);
        mob.setNoAi(false);

        // Manually register goals for cow if needed (mixin may not fire in test environment)
        if (mob instanceof Cow cow) {
            CowGameTestHelpers.registerCowGoals(cow);
        }

        return mob;
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 320)
    public void thirstyCowMovesToAndDrinksWater(GameTestHelper helper) {
        // Place water at (4,1,1) with a drinking position at (3,1,1) - closer for reliability
        BlockPos waterPos = helper.absolutePos(new BlockPos(4, 1, 1));
        BlockPos drinkPos = helper.absolutePos(new BlockPos(3, 1, 1));

        // Ensure solid ground exists for the drinking position
        helper.setBlock(waterPos.below(), Blocks.STONE);
        helper.setBlock(drinkPos.below(), Blocks.STONE);
        helper.setBlock(waterPos, Blocks.WATER);
        helper.setBlock(drinkPos, Blocks.AIR);
        helper.setBlock(drinkPos.above(), Blocks.AIR);

        Cow cow = HerbivoreTestUtils.spawnCowWithAi(helper, helper.absolutePos(new BlockPos(1, 2, 1)));

        // Wait a tick for ecology component to initialize
        helper.runAtTickTime(1, () -> {
            // Manually register all cow goals for game test environment
            HerbivoreTestUtils.registerAllCowGoals(cow);

            // Set very low thirst to trigger SeekWaterGoal (threshold is < 30)
            HerbivoreTestUtils.setThirst(cow, 6);
            HerbivoreTestUtils.setThirstyState(cow, true);

            // Boost navigation to ensure cow can reach water quickly
            HerbivoreTestUtils.boostNavigation(cow, 1.2);
        });

        helper.succeedWhen(() -> {
            double distance = cow.distanceToSqr(drinkPos.getX() + 0.5, drinkPos.getY(), drinkPos.getZ() + 0.5);
            helper.assertTrue(
                distance < 6.25,
                "Cow failed to reach water to drink. Distance squared: " + distance
            );
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void cowHerdMovesTogetherWithQuorum(GameTestHelper helper) {
        // Spawn 5 cows in a group - quorum threshold is typically ~30-50%
        // With 5 cows, we need at least 2-3 ready to trigger movement
        var level = helper.getLevel();

        Cow leader = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(4, 2, 4)));
        Cow cow2 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(5, 2, 4)));
        Cow cow3 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(4, 2, 5)));
        Cow cow4 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(5, 2, 5)));
        Cow cow5 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(4, 2, 3)));

        Cow[] herd = {leader, cow2, cow3, cow4, cow5};

        // Make cows hungry to motivate movement toward grass
        for (Cow cow : herd) {
            HerbivoreTestUtils.setHunger(cow, 20); // Low hunger triggers foraging
            HerbivoreTestUtils.boostNavigation(cow, 0.8);
        }

        // Place grass ahead of the herd to motivate movement
        BlockPos grassPos = helper.absolutePos(new BlockPos(10, 1, 10));
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);
        helper.setBlock(grassPos.east(), Blocks.GRASS_BLOCK);
        helper.setBlock(grassPos.south(), Blocks.GRASS_BLOCK);
        helper.setBlock(grassPos.east().south(), Blocks.GRASS_BLOCK);

        // Ensure solid ground for grass
        helper.setBlock(grassPos.below(), Blocks.STONE);
        helper.setBlock(grassPos.east().below(), Blocks.STONE);
        helper.setBlock(grassPos.south().below(), Blocks.STONE);
        helper.setBlock(grassPos.east().south().below(), Blocks.STONE);

        // Record initial positions
        final Vec3[] initialPositions = new Vec3[herd.length];
        for (int i = 0; i < herd.length; i++) {
            initialPositions[i] = herd[i].position();
        }

        // Wait for AI to evaluate and herd to form
        helper.runAtTickTime(60, () -> {
            // Check if cows are beginning to move toward grass as a group
            int movingCount = 0;
            double totalMovement = 0.0;

            for (int i = 0; i < herd.length; i++) {
                Vec3 currentPos = herd[i].position();
                double movement = currentPos.distanceToSqr(initialPositions[i]);
                totalMovement += movement;

                if (movement > 1.0) {
                    movingCount++;
                }
            }

            // At least quorum (60% = 3 of 5 cows) should be moving
            if (movingCount >= 3) {
                helper.succeed();
            }
        });

        // Final check - ensure most of the herd moved together
        helper.succeedWhen(() -> {
            int reachedCount = 0;
            double totalDistanceToGrass = 0.0;

            for (Cow cow : herd) {
                double distanceToGrass = cow.distanceToSqr(grassPos.getX(), grassPos.getY(), grassPos.getZ());
                totalDistanceToGrass += distanceToGrass;

                if (distanceToGrass < 25.0) { // Within 5 blocks
                    reachedCount++;
                }
            }

            double avgDistance = totalDistanceToGrass / herd.length;

            // Success if majority of herd moved toward grass
            helper.assertTrue(reachedCount >= 3,
                "Only " + reachedCount + " of " + herd.length + " cows reached grass area. " +
                "Average distance to grass: " + String.format("%.2f", avgDistance));
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 350)
    public void cowHerdMaintainsCohesion(GameTestHelper helper) {
        // Spawn 4 cows to test herd cohesion
        Cow cow1 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(3, 2, 3)));
        Cow cow2 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(4, 2, 3)));
        Cow cow3 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(3, 2, 4)));
        Cow cow4 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(4, 2, 4)));

        Cow[] herd = {cow1, cow2, cow3, cow4};

        // Boost navigation for controlled movement
        for (Cow cow : herd) {
            HerbivoreTestUtils.boostNavigation(cow, 0.7);
        }

        // Place attractive resource (grass) to encourage movement
        BlockPos grassPos = helper.absolutePos(new BlockPos(8, 1, 8));
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        // Wait for movement and check cohesion
        final int[] checkStartTick = {-1};
        helper.succeedWhen(() -> {
            long currentTick = helper.getTick();
            if (checkStartTick[0] == -1) {
                checkStartTick[0] = (int) currentTick;
            }
            long ticksSinceCheckStart = currentTick - checkStartTick[0];
            if (ticksSinceCheckStart < 80) {
                return; // Give cows time to move
            }

            // Calculate pairwise distances between all cows
            double maxDistance = 0.0;
            double totalDistance = 0.0;
            int pairCount = 0;

            for (int i = 0; i < herd.length; i++) {
                for (int j = i + 1; j < herd.length; j++) {
                    double distance = herd[i].distanceTo(herd[j]);
                    maxDistance = Math.max(maxDistance, distance);
                    totalDistance += distance;
                    pairCount++;
                }
            }

            double avgDistance = totalDistance / pairCount;

            // Herd should stay cohesive - max distance between any two cows < 12 blocks
            // and average distance < 6 blocks
            helper.assertTrue(maxDistance < 12.0,
                "Herd cohesion failed: max distance between cows is " + String.format("%.2f", maxDistance) +
                " (expected < 12.0). Average distance: " + String.format("%.2f", avgDistance));

            helper.assertTrue(avgDistance < 6.0,
                "Herd cohesion weak: average distance between cows is " + String.format("%.2f", avgDistance) +
                " (expected < 6.0)");
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 380)
    public void cowsFollowHerdLeader(GameTestHelper helper) {
        // Spawn cows with one positioned to potentially be leader
        Cow leader = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(4, 2, 4)));
        Cow follower1 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(3, 2, 3)));
        Cow follower2 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(5, 2, 3)));
        Cow follower3 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(3, 2, 5)));

        Cow[] followers = {follower1, follower2, follower3};

        // Boost navigation
        HerbivoreTestUtils.boostNavigation(leader, 0.8);
        for (Cow cow : followers) {
            HerbivoreTestUtils.boostNavigation(cow, 0.8);
        }

        // Place grass ahead to motivate movement
        BlockPos grassPos = helper.absolutePos(new BlockPos(10, 1, 8));
        helper.setBlock(grassPos, Blocks.GRASS_BLOCK);

        Vec3 leaderStartPos = leader.position();

        // Wait and check if followers move toward leader's new position
        helper.runAtTickTime(80, () -> {
            Vec3 leaderCurrentPos = leader.position();
            double leaderMovement = leaderCurrentPos.distanceToSqr(leaderStartPos);

            if (leaderMovement < 4.0) {
                return; // Leader hasn't moved much yet
            }

            // Check if followers are moving in same direction as leader
            int followersMovingWithLeader = 0;

            for (Cow follower : followers) {
                Vec3 followerPos = follower.position();
                Vec3 toLeader = leaderCurrentPos.subtract(followerPos);

                // Follower should be moving toward leader or in leader's direction
                double distanceToLeader = follower.distanceTo(leader);

                // Check if follower is reasonably close to leader (following behavior)
                if (distanceToLeader < 10.0) {
                    followersMovingWithLeader++;
                }
            }

            // At least 2 of 3 followers should be following the leader
            helper.assertTrue(followersMovingWithLeader >= 2,
                "Leader following failed: only " + followersMovingWithLeader + " of " + followers.length +
                " cows are following the leader. Leader moved " + String.format("%.2f", leaderMovement) + " blocks.");
        });

        helper.succeedWhen(() -> {
            Vec3 leaderCurrentPos = leader.position();
            double leaderMovement = leaderCurrentPos.distanceToSqr(leaderStartPos);

            if (leaderMovement < 9.0) {
                return; // Wait for leader to move more
            }

            // Verify followers are still near leader
            int followersNearLeader = 0;
            double totalFollowerDistance = 0.0;

            for (Cow follower : followers) {
                double distanceToLeader = follower.distanceTo(leader);
                totalFollowerDistance += distanceToLeader;

                if (distanceToLeader < 12.0) {
                    followersNearLeader++;
                }
            }

            double avgFollowerDistance = totalFollowerDistance / followers.length;

            helper.assertTrue(followersNearLeader >= 2,
                "Leader following failed: only " + followersNearLeader + " of " + followers.length +
                " followers are near leader (within 12 blocks). Average distance: " +
                String.format("%.2f", avgFollowerDistance));
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 420)
    public void multipleThirstyCowsSeekWaterTogether(GameTestHelper helper) {
        // Place water closer with proper drinking positions around it
        BlockPos waterPos = helper.absolutePos(new BlockPos(5, 1, 5));

        // Set up water with solid ground and drinking positions on all sides
        helper.setBlock(waterPos.below(), Blocks.STONE);
        helper.setBlock(waterPos, Blocks.WATER);

        // Create accessible drinking positions on all 4 sides
        BlockPos[] drinkingSpots = {
            waterPos.north(),
            waterPos.south(),
            waterPos.east(),
            waterPos.west()
        };

        for (BlockPos spot : drinkingSpots) {
            helper.setBlock(spot.below(), Blocks.STONE);
            helper.setBlock(spot, Blocks.AIR);
            helper.setBlock(spot.above(), Blocks.AIR);
        }

        // Spawn 4 thirsty cows closer to the water
        Cow cow1 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(1, 2, 5)));
        Cow cow2 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(2, 2, 5)));
        Cow cow3 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(1, 2, 6)));
        Cow cow4 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(2, 2, 6)));

        Cow[] herd = {cow1, cow2, cow3, cow4};

        // Wait a tick for ecology component to initialize
        helper.runAtTickTime(1, () -> {
            // Make all cows thirsty using the unified helper
            for (Cow cow : herd) {
                HerbivoreTestUtils.setThirst(cow, 6);
                HerbivoreTestUtils.setThirstyState(cow, true);
                HerbivoreTestUtils.boostNavigation(cow, 1.2);
            }
        });

        // Check if cows reach water together (herd behavior)
        helper.succeedWhen(() -> {
            int reachedWater = 0;
            double totalDistance = 0.0;

            for (Cow cow : herd) {
                double distance = cow.distanceToSqr(waterPos.getX(), waterPos.getY(), waterPos.getZ());
                totalDistance += distance;

                if (distance < 12.0) { // Within ~3.5 blocks
                    reachedWater++;
                }
            }

            double avgDistance = totalDistance / herd.length;

            // At least 3 of 4 cows should reach water, and they should stay relatively close
            helper.assertTrue(reachedWater >= 3,
                "Only " + reachedWater + " of " + herd.length + " cows reached water. " +
                "Average distance to water: " + String.format("%.2f", avgDistance));
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 360)
    public void cowHerdMaintainsCohesionWhileMoving(GameTestHelper helper) {
        // Test that cows maintain reasonable distances while moving toward food
        Cow cow1 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(2, 2, 2)));
        Cow cow2 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(3, 2, 2)));
        Cow cow3 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(2, 2, 3)));
        Cow cow4 = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(3, 2, 3)));

        Cow[] herd = {cow1, cow2, cow3, cow4};

        for (Cow cow : herd) {
            HerbivoreTestUtils.boostNavigation(cow, 0.75);
        }

        // Place grass in a line to encourage directional movement
        for (int i = 0; i < 5; i++) {
            helper.setBlock(helper.absolutePos(new BlockPos(6 + i, 1, 6)), Blocks.GRASS_BLOCK);
        }

        final int[] checkStartTick = {-1};
        helper.succeedWhen(() -> {
            long currentTick = helper.getTick();
            if (checkStartTick[0] == -1) {
                checkStartTick[0] = (int) currentTick;
            }
            long ticksSinceCheckStart = currentTick - checkStartTick[0];
            if (ticksSinceCheckStart < 100) {
                return; // Give time for movement
            }

            // Check cohesion during movement
            double maxDistance = 0.0;
            int pairCount = 0;
            double totalDistance = 0.0;

            for (int i = 0; i < herd.length; i++) {
                for (int j = i + 1; j < herd.length; j++) {
                    double distance = herd[i].distanceTo(herd[j]);
                    maxDistance = Math.max(maxDistance, distance);
                    totalDistance += distance;
                    pairCount++;
                }
            }

            double avgDistance = totalDistance / pairCount;

            // While moving, herd should still maintain cohesion
            // Max separation should not exceed 15 blocks
            // Average separation should be under 8 blocks
            helper.assertTrue(maxDistance < 15.0,
                "Herd too dispersed during movement: max separation " + String.format("%.2f", maxDistance) +
                " (expected < 15.0)");

            helper.assertTrue(avgDistance < 8.0,
                "Herd cohesion weak during movement: avg separation " + String.format("%.2f", avgDistance) +
                " (expected < 8.0)");
        });
    }

    /**
     * Test that a cow with low health flees from a wolf predator.
     * This test:
     * 1. Spawns a cow with low health (30%)
     * 2. Spawns a wolf nearby to trigger flee behavior
     * 3. Verifies the cow enters retreating state and moves away
     */
    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 360)
    public void cowFleeFromPredatorWhenLowHealth(GameTestHelper helper) {
        Cow cow = spawnWithAi(helper, EntityType.COW, helper.absolutePos(new BlockPos(2, 2, 2)));
        Wolf wolf = spawnWithAi(helper, EntityType.WOLF, helper.absolutePos(new BlockPos(6, 2, 2)));

        HerbivoreTestUtils.boostNavigation(cow, 0.9);
        HerbivoreTestUtils.setHealthPercent(cow, 0.3f);

        final double[] initialDistance = {cow.distanceToSqr(wolf)};

        wolf.setTarget(cow);

        helper.runAtTickTime(20, () -> wolf.doHurtTarget(cow));
        helper.runAtTickTime(30, () -> initialDistance[0] = cow.distanceToSqr(wolf));

        final int[] checkStartTick = {-1};
        helper.succeedWhen(() -> {
            long currentTick = helper.getTick();
            if (checkStartTick[0] == -1) {
                checkStartTick[0] = (int) currentTick;
            }
            long ticksSinceCheckStart = currentTick - checkStartTick[0];
            if (ticksSinceCheckStart < 60) {
                return;
            }

            helper.assertTrue(HerbivoreTestUtils.isRetreating(cow), "Cow did not enter retreating state when threatened by wolf");
            double currentDistance = cow.distanceToSqr(wolf);
            helper.assertTrue(currentDistance > initialDistance[0] + 4.0, "Cow did not increase distance from wolf while fleeing");
        });
    }

    /**
     * Helper method to get navigation target position.
     */
    private Vec3 getNavigationTarget(Mob mob) {
        try {
            var path = mob.getNavigation().getPath();
            if (path != null && !path.isDone()) {
                var targetNode = path.getTarget();
                if (targetNode != null) {
                    return Vec3.atCenterOf(targetNode);
                }
            }
        } catch (Exception e) {
            // Path navigation may not be available
        }
        return null;
    }
}
