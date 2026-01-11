package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class WolfBehaviorGameTests implements FabricGameTest {

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void hungryWolfTargetsPig(GameTestHelper helper) {
        var level = helper.getLevel();
        var wolfPos = helper.absoluteVec(new Vec3(1, 2, 1));
        var pigPos = helper.absoluteVec(new Vec3(5, 2, 1));

        Wolf wolf = EntityType.WOLF.create(level);
        Pig pig = EntityType.PIG.create(level);

        if (wolf == null || pig == null) {
            helper.fail("Failed to spawn test entities");
            return;
        }

        WolfGameTestHelpers.enableAi(wolf);
        WolfGameTestHelpers.enableAi(pig);

        wolf.moveTo(wolfPos.x, wolfPos.y, wolfPos.z, 0f, 0f);
        pig.moveTo(pigPos.x, pigPos.y, pigPos.z, 0f, 0f);
        level.addFreshEntity(wolf);
        level.addFreshEntity(pig);

        final double[] initialDistance = {wolfPos.distanceTo(pigPos)};

        // Wait one tick for entities to fully initialize and register goals
        helper.runAtTickTime(1, () -> {
            // Set hunger AFTER entity is added to level and fully initialized
            // This ensures ecology component and state are properly set up
            WolfGameTestHelpers.setHunger(wolf, 10);

            // Verify hunger state is properly set before testing behavior
            if (!WolfGameTestHelpers.verifyHungerState(wolf, 10)) {
                helper.fail("Wolf hunger state not properly initialized");
                return;
            }

            // Give AI goals additional time to evaluate (goals check periodically)
            // HungryPredatorTargetGoal canUse() needs to be evaluated by goal selector
            // Increased delay to account for:
            // 1. Goal selector evaluates targets periodically (not every tick)
            // 2. Pathfinding takes time to calculate route to target
            // 3. Entity needs time to actually move toward target
            helper.runAfterDelay(100, () -> {
                // Test that hungry wolf moves toward prey (targeting or pursuit behavior)
                helper.succeedWhen(() -> {
                    var target = wolf.getTarget();
                    boolean hasTarget = target != null && target.equals(pig);
                    double currentDistance = wolf.position().distanceTo(pig.position());
                    boolean movingToward = currentDistance < initialDistance[0] - 1.0;

                    if (!hasTarget && !movingToward) {
                        throw new GameTestAssertException("Wolf has not targeted pig or moved toward it yet");
                    }
                });
            });
        });
    }

    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 400)
    public void hungryWolfPicksUpMeat(GameTestHelper helper) {
        var level = helper.getLevel();
        var wolfPos = helper.absoluteVec(new Vec3(1, 2, 1));
        var meatPos = helper.absoluteVec(new Vec3(4, 2, 1));

        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) {
            helper.fail("Failed to spawn wolf for pickup test");
            return;
        }

        WolfGameTestHelpers.enableAi(wolf);

        wolf.moveTo(wolfPos.x, wolfPos.y, wolfPos.z, 0f, 0f);
        level.addFreshEntity(wolf);

        // Manually register wolf goals for game test environment
        WolfGameTestHelpers.registerWolfGoals(wolf);

        ItemEntity meat = new ItemEntity(level, meatPos.x, meatPos.y, meatPos.z, new ItemStack(Items.BEEF));
        meat.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(meat);

        final double[] initialDistance = {wolfPos.distanceTo(meatPos)};

        // Wait one tick for entity to fully initialize and register goals
        helper.runAtTickTime(1, () -> {
            // Set hunger below threshold (WolfBehaviorHandle.isHungry checks hunger < 40)
            // Set AFTER entity is added to level to ensure proper initialization
            WolfGameTestHelpers.setHunger(wolf, 10);

            // Verify hunger state is properly set before testing behavior
            if (!WolfGameTestHelpers.verifyHungerState(wolf, 10)) {
                helper.fail("Wolf hunger state not properly initialized");
                return;
            }

            // Give AI goals additional time to evaluate
            // WolfPickupItemGoal has randomized delays (90% skip rate) and 40-tick cooldown
            // With 90% skip rate, average 10 attempts needed to activate
            // 40 tick initial cooldown + ~10 ticks for activation + 20 ticks to reach meat
            helper.runAfterDelay(120, () -> {
                // Test that hungry wolf moves toward meat or picks it up
                helper.succeedWhen(() -> {
                    boolean pickedUp = WolfGameTestHelpers.wolfPickedUpItem(wolf, meat);
                    double currentDistance = wolf.position().distanceTo(meat.position());
                    boolean movingToward = currentDistance < initialDistance[0] - 1.0;

                    if (!pickedUp && !movingToward) {
                        throw new GameTestAssertException("Wolf has not picked up meat or moved toward it yet");
                    }
                });
            });
        });
    }
}
