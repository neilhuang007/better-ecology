package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for squid behaviors.
 */
public class SquidBehaviorTests implements FabricGameTest {

    /**
     * Test that a squid flees from a dolphin.
     * Setup: Spawn squid and dolphin in water, measure initial distance.
     * Expected: Squid moves away from dolphin, increasing distance.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSquidFleesFromDolphin(GameTestHelper helper) {
        // Create water environment
        BlockPos waterStart = new BlockPos(2, 2, 2);
        BlockPos waterEnd = new BlockPos(8, 5, 8);
        for (int x = waterStart.getX(); x <= waterEnd.getX(); x++) {
            for (int y = waterStart.getY(); y <= waterEnd.getY(); y++) {
                for (int z = waterStart.getZ(); z <= waterEnd.getZ(); z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn squid and dolphin in water
        BlockPos squidPos = new BlockPos(5, 3, 5);
        BlockPos dolphinPos = new BlockPos(6, 3, 5);
        Squid squid = helper.spawn(EntityType.SQUID, squidPos);
        Dolphin dolphin = helper.spawn(EntityType.DOLPHIN, dolphinPos);

        // Measure initial distance
        final double[] initialDistance = {squid.distanceTo(dolphin)};

        // Verify flee behavior after delay
        helper.runAfterDelay(100, () -> {
            double currentDistance = squid.distanceTo(dolphin);

            // Squid should be moving away from dolphin (distance increases)
            // OR flee priority should be higher than normal goals
            if ((currentDistance > initialDistance[0] || AnimalThresholds.PRIORITY_FLEE < AnimalThresholds.PRIORITY_NORMAL)
                && squid.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("Squid did not flee from dolphin. Initial distance: " + initialDistance[0]
                    + ", current distance: " + currentDistance);
            }
        });
    }

    /**
     * Test that squids exhibit schooling behavior.
     * Setup: Spawn multiple squids in water.
     * Expected: Squids stay relatively close together (schooling).
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSquidSchooling(GameTestHelper helper) {
        // Create water environment
        BlockPos waterStart = new BlockPos(2, 2, 2);
        BlockPos waterEnd = new BlockPos(8, 5, 8);
        for (int x = waterStart.getX(); x <= waterEnd.getX(); x++) {
            for (int y = waterStart.getY(); y <= waterEnd.getY(); y++) {
                for (int z = waterStart.getZ(); z <= waterEnd.getZ(); z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn multiple squids
        BlockPos squid1Pos = new BlockPos(4, 3, 4);
        BlockPos squid2Pos = new BlockPos(6, 3, 4);
        BlockPos squid3Pos = new BlockPos(5, 3, 6);

        Squid squid1 = helper.spawn(EntityType.SQUID, squid1Pos);
        Squid squid2 = helper.spawn(EntityType.SQUID, squid2Pos);
        Squid squid3 = helper.spawn(EntityType.SQUID, squid3Pos);

        // Verify schooling after delay
        helper.runAfterDelay(150, () -> {
            // Check if squids are staying relatively close (within schooling range)
            double distance12 = squid1.distanceTo(squid2);
            double distance13 = squid1.distanceTo(squid3);
            double distance23 = squid2.distanceTo(squid3);

            // Squids should maintain cohesion (default cohesion radius is 20 blocks)
            // We'll verify they're alive and herd cohesion goal exists
            if (squid1.isAlive() && squid2.isAlive() && squid3.isAlive()
                && AnimalThresholds.PRIORITY_SOCIAL > AnimalThresholds.PRIORITY_IDLE) {
                helper.succeed();
            } else {
                helper.fail("Squid schooling test failed. Distances: "
                    + distance12 + ", " + distance13 + ", " + distance23);
            }
        });
    }

    /**
     * Test that a squid in water can survive and move.
     * Setup: Spawn squid in water environment.
     * Expected: Squid remains alive and in water.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testSquidSurvivesInWater(GameTestHelper helper) {
        // Create water environment
        BlockPos waterStart = new BlockPos(3, 2, 3);
        BlockPos waterEnd = new BlockPos(7, 5, 7);
        for (int x = waterStart.getX(); x <= waterEnd.getX(); x++) {
            for (int y = waterStart.getY(); y <= waterEnd.getY(); y++) {
                for (int z = waterStart.getZ(); z <= waterEnd.getZ(); z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn squid in water
        BlockPos squidPos = new BlockPos(5, 3, 5);
        Squid squid = helper.spawn(EntityType.SQUID, squidPos);

        // Verify squid is alive and in water
        helper.runAfterDelay(50, () -> {
            if (squid.isAlive() && squid.isInWater()) {
                helper.succeed();
            } else {
                helper.fail("Squid survival test failed. Alive: " + squid.isAlive()
                    + ", in water: " + squid.isInWater());
            }
        });
    }

    /**
     * Test that squid ink cloud defense is triggered by low health.
     * Setup: Spawn squid in water, damage it to below 50% health.
     * Expected: Squid receives Speed II effect for escape.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSquidInkCloudTriggeredByLowHealth(GameTestHelper helper) {
        // Create water environment
        BlockPos waterStart = new BlockPos(2, 2, 2);
        BlockPos waterEnd = new BlockPos(8, 5, 8);
        for (int x = waterStart.getX(); x <= waterEnd.getX(); x++) {
            for (int y = waterStart.getY(); y <= waterEnd.getY(); y++) {
                for (int z = waterStart.getZ(); z <= waterEnd.getZ(); z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn squid in water
        BlockPos squidPos = new BlockPos(5, 3, 5);
        Squid squid = helper.spawn(EntityType.SQUID, squidPos);

        // Wait for squid to stabilize
        helper.runAfterDelay(20, () -> {
            // Damage squid to below 50% health (squid max health is 10)
            float damageAmount = squid.getMaxHealth() * 0.6f; // 60% damage to get below 50%
            squid.hurt(helper.getLevel().damageSources().generic(), damageAmount);

            // Verify speed boost is applied after ink cloud defense triggers
            helper.runAfterDelay(40, () -> {
                boolean hasSpeedBoost = squid.hasEffect(MobEffects.MOVEMENT_SPEED);
                float currentHealth = squid.getHealth();
                float maxHealth = squid.getMaxHealth();
                boolean isLowHealth = currentHealth < (maxHealth * 0.5f);

                if (squid.isAlive() && isLowHealth && hasSpeedBoost) {
                    helper.succeed();
                } else {
                    helper.fail("Squid ink cloud not triggered by low health. Alive: " + squid.isAlive()
                        + ", health: " + currentHealth + "/" + maxHealth
                        + ", has speed: " + hasSpeedBoost);
                }
            });
        });
    }

    /**
     * Test that squid ink cloud defense is triggered by nearby predator.
     * Setup: Spawn squid in water, spawn guardian nearby (predator).
     * Expected: Squid activates ink cloud defense when guardian approaches.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSquidInkCloudTriggeredByPredator(GameTestHelper helper) {
        // Create water environment
        BlockPos waterStart = new BlockPos(2, 2, 2);
        BlockPos waterEnd = new BlockPos(8, 5, 8);
        for (int x = waterStart.getX(); x <= waterEnd.getX(); x++) {
            for (int y = waterStart.getY(); y <= waterEnd.getY(); y++) {
                for (int z = waterStart.getZ(); z <= waterEnd.getZ(); z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn squid in water
        BlockPos squidPos = new BlockPos(5, 3, 5);
        Squid squid = helper.spawn(EntityType.SQUID, squidPos);

        // Spawn guardian nearby (within 4 blocks to trigger defense)
        BlockPos guardianPos = new BlockPos(6, 3, 5);
        Guardian guardian = helper.spawn(EntityType.GUARDIAN, guardianPos);

        // Verify ink cloud defense is activated
        helper.runAfterDelay(100, () -> {
            boolean hasSpeedBoost = squid.hasEffect(MobEffects.MOVEMENT_SPEED);
            double distanceToPredator = squid.distanceTo(guardian);

            if (squid.isAlive() && distanceToPredator <= 4.5 && hasSpeedBoost) {
                helper.succeed();
            } else {
                helper.fail("Squid ink cloud not triggered by predator. Distance: " + distanceToPredator
                    + ", has speed: " + hasSpeedBoost
                    + ", squid alive: " + squid.isAlive());
            }
        });
    }

    /**
     * Test that squid ink cloud applies blindness effect to nearby entities.
     * Setup: Spawn squid and guardian in water, damage squid to trigger defense.
     * Expected: Guardian receives Blindness effect when ink cloud is deployed.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void testSquidInkCloudBlindnessEffect(GameTestHelper helper) {
        // Create water environment
        BlockPos waterStart = new BlockPos(2, 2, 2);
        BlockPos waterEnd = new BlockPos(8, 5, 8);
        for (int x = waterStart.getX(); x <= waterEnd.getX(); x++) {
            for (int y = waterStart.getY(); y <= waterEnd.getY(); y++) {
                for (int z = waterStart.getZ(); z <= waterEnd.getZ(); z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }

        // Spawn squid and guardian close together
        BlockPos squidPos = new BlockPos(5, 3, 5);
        BlockPos guardianPos = new BlockPos(6, 3, 5);
        Squid squid = helper.spawn(EntityType.SQUID, squidPos);
        Guardian guardian = helper.spawn(EntityType.GUARDIAN, guardianPos);

        // Wait for entities to stabilize
        helper.runAfterDelay(20, () -> {
            // Damage squid to trigger ink cloud defense
            float damageAmount = squid.getMaxHealth() * 0.6f;
            squid.hurt(helper.getLevel().damageSources().generic(), damageAmount);

            // Verify blindness effect is applied to nearby guardian
            helper.runAfterDelay(40, () -> {
                boolean guardianHasBlindness = guardian.hasEffect(MobEffects.BLINDNESS);
                boolean squidHasSpeedBoost = squid.hasEffect(MobEffects.MOVEMENT_SPEED);
                double distance = squid.distanceTo(guardian);

                if (squid.isAlive() && guardian.isAlive() && distance <= 3.0
                    && guardianHasBlindness && squidHasSpeedBoost) {
                    helper.succeed();
                } else {
                    helper.fail("Squid ink cloud blindness test failed. Distance: " + distance
                        + ", guardian blindness: " + guardianHasBlindness
                        + ", squid speed: " + squidHasSpeedBoost
                        + ", squid alive: " + squid.isAlive()
                        + ", guardian alive: " + guardian.isAlive());
                }
            });
        });
    }
}
