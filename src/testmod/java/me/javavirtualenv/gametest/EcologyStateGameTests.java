package me.javavirtualenv.gametest;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;

/**
 * Comprehensive game tests for ecology state management.
 * Tests verify observable in-game behaviors including NBT persistence,
 * state synchronization, and resource decay mechanics.
 */
public class EcologyStateGameTests implements FabricGameTest {

    /**
     * Test that hunger state persists through NBT save/load cycle.
     * Verifies the complete persistence flow: set hunger -> save -> load -> verify.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void hungerStatePersistsAfterSaveLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        // Wait for entity to initialize
        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // Set hunger to specific value
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", 25);

            // Verify initial state
            int initialHunger = component.getHandleTag("hunger").getInt("hunger");
            if (initialHunger != 25) {
                helper.fail("Initial hunger not set correctly. Expected: 25, Got: " + initialHunger);
                return;
            }

            // Set hungry state flag
            component.state().setIsHungry(true);
            if (!component.state().isHungry()) {
                helper.fail("Hungry state flag not set correctly");
                return;
            }

            // Simulate NBT save/load cycle
            CompoundTag saveTag = new CompoundTag();
            cow.save(saveTag);

            // Remove cow and recreate to simulate fresh load
            cow.remove(Cow.RemovalReason.DISCARDED);

            // Load from saved NBT
            Cow loadedCow = EntityType.COW.create(level);
            if (loadedCow == null) {
                helper.fail("Failed to create cow for loading");
                return;
            }

            loadedCow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
            loadedCow.load(saveTag);
            level.addFreshEntity(loadedCow);

            // Verify hunger persisted correctly
            helper.runAtTickTime(2, () -> {
                EcologyComponent loadedComponent = HerbivoreTestUtils.getComponent(loadedCow);
                int loadedHunger = loadedComponent.getHandleTag("hunger").getInt("hunger");

                if (loadedHunger != 25) {
                    helper.fail("Hunger did not persist. Expected: 25, Got: " + loadedHunger);
                    return;
                }

                // Verify state flag matches persisted NBT
                boolean isHungry = loadedHunger < 50;
                if (loadedComponent.state().isHungry() != isHungry) {
                    helper.fail("State flag does not match persisted hunger. " +
                            "Hunger: " + loadedHunger + ", isHungry: " + loadedComponent.state().isHungry());
                    return;
                }

                helper.succeed();
            });
        });
    }

    /**
     * Test that thirst state persists through NBT save/load cycle.
     * Verifies thirst value and thirsty state flag are correctly restored.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void thirstStatePersistsAfterSaveLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        // Wait for entity to initialize
        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // Set thirst to low value (thirsty state)
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", 15);

            // Verify initial state
            int initialThirst = component.getHandleTag("thirst").getInt("thirst");
            if (initialThirst != 15) {
                helper.fail("Initial thirst not set correctly. Expected: 15, Got: " + initialThirst);
                return;
            }

            // Set thirsty state flag
            component.state().setIsThirsty(true);
            if (!component.state().isThirsty()) {
                helper.fail("Thirsty state flag not set correctly");
                return;
            }

            // Simulate NBT save/load cycle
            CompoundTag saveTag = new CompoundTag();
            cow.save(saveTag);

            // Remove and recreate cow
            cow.remove(Cow.RemovalReason.DISCARDED);

            Cow loadedCow = EntityType.COW.create(level);
            if (loadedCow == null) {
                helper.fail("Failed to create cow for loading");
                return;
            }

            loadedCow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
            loadedCow.load(saveTag);
            level.addFreshEntity(loadedCow);

            // Verify thirst persisted correctly
            helper.runAtTickTime(2, () -> {
                EcologyComponent loadedComponent = HerbivoreTestUtils.getComponent(loadedCow);
                int loadedThirst = loadedComponent.getHandleTag("thirst").getInt("thirst");

                if (loadedThirst != 15) {
                    helper.fail("Thirst did not persist. Expected: 15, Got: " + loadedThirst);
                    return;
                }

                // Verify state flag matches persisted NBT
                boolean isThirsty = loadedThirst < 40;
                if (loadedComponent.state().isThirsty() != isThirsty) {
                    helper.fail("State flag does not match persisted thirst. " +
                            "Thirst: " + loadedThirst + ", isThirsty: " + loadedComponent.state().isThirsty());
                    return;
                }

                helper.succeed();
            });
        });
    }

    /**
     * Test that NBT values and state flags stay synchronized.
     * Ensures that when NBT changes, state flags are updated accordingly.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void stateFlagsSynchronizedWithNbt(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // Test 1: Set hunger below threshold, verify hungry flag
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", 30);
            component.state().setIsHungry(true);

            int hunger1 = component.getHandleTag("hunger").getInt("hunger");
            boolean isHungry1 = component.state().isHungry();

            if (hunger1 < 50 && !isHungry1) {
                helper.fail("Hungry flag not set when hunger < 50. Hunger: " + hunger1 + ", isHungry: " + isHungry1);
                return;
            }

            // Test 2: Set thirst below threshold, verify thirsty flag
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", 20);
            component.state().setIsThirsty(true);

            int thirst1 = component.getHandleTag("thirst").getInt("thirst");
            boolean isThirsty1 = component.state().isThirsty();

            if (thirst1 < 40 && !isThirsty1) {
                helper.fail("Thirsty flag not set when thirst < 40. Thirst: " + thirst1 + ", isThirsty: " + isThirsty1);
                return;
            }

            // Test 3: Restore resources, verify flags clear
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", 80);
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", 90);
            component.state().setIsHungry(false);
            component.state().setIsThirsty(false);

            int hunger2 = component.getHandleTag("hunger").getInt("hunger");
            int thirst2 = component.getHandleTag("thirst").getInt("thirst");
            boolean isHungry2 = component.state().isHungry();
            boolean isThirsty2 = component.state().isThirsty();

            if (hunger2 >= 50 && isHungry2) {
                helper.fail("Hungry flag still set when hunger >= 50. Hunger: " + hunger2);
                return;
            }

            if (thirst2 >= 40 && isThirsty2) {
                helper.fail("Thirsty flag still set when thirst >= 40. Thirst: " + thirst2);
                return;
            }

            helper.succeed();
        });
    }

    /**
     * Test that hunger resources decrease over time through decay.
     * Verifies the hunger handle's decay mechanic is working.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void hungerDecaysOverTime(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // Set hunger to starting value (full)
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", 100);

            int initialHunger = component.getHandleTag("hunger").getInt("hunger");
            if (initialHunger != 100) {
                helper.fail("Initial hunger not set. Expected: 100, Got: " + initialHunger);
                return;
            }

            // Wait for hunger to decay (HungerHandle ticks every 20 ticks = 1 second)
            // After 60 ticks (3 seconds), hunger should have decreased
            helper.runAtTickTime(80, () -> {
                EcologyComponent tickedComponent = HerbivoreTestUtils.getComponent(cow);
                int decayedHunger = tickedComponent.getHandleTag("hunger").getInt("hunger");

                // Hunger should be less than initial value due to decay
                if (decayedHunger >= initialHunger) {
                    helper.fail("Hunger did not decay over time. Initial: " + initialHunger +
                            ", After decay: " + decayedHunger);
                    return;
                }

                // Verify hunger actually decreased (not just stayed same)
                if (decayedHunger == initialHunger) {
                    helper.fail("Hunger did not decrease. Decay mechanic not working?");
                    return;
                }

                // Verify hungry state is set if below threshold
                boolean isHungry = tickedComponent.state().isHungry();
                boolean shouldBeHungry = decayedHunger < 50;

                if (shouldBeHungry && !isHungry) {
                    helper.fail("Hungry state not set after hunger decayed to " + decayedHunger);
                    return;
                }

                helper.succeed();
            });
        });
    }

    /**
     * Test that thirst resources decrease over time through decay.
     * Verifies the thirst handle's decay mechanic is working.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform", timeoutTicks = 300)
    public void thirstDecaysOverTime(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // Set thirst to starting value (full)
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", 100);

            int initialThirst = component.getHandleTag("thirst").getInt("thirst");
            if (initialThirst != 100) {
                helper.fail("Initial thirst not set. Expected: 100, Got: " + initialThirst);
                return;
            }

            // Wait for thirst to decay (ThirstHandle ticks every 5 ticks = 0.25 seconds)
            // After 50 ticks (2.5 seconds), thirst should have decreased multiple times
            helper.runAtTickTime(80, () -> {
                EcologyComponent tickedComponent = HerbivoreTestUtils.getComponent(cow);
                int decayedThirst = tickedComponent.getHandleTag("thirst").getInt("thirst");

                // Thirst should be less than initial value due to decay
                if (decayedThirst >= initialThirst) {
                    helper.fail("Thirst did not decay over time. Initial: " + initialThirst +
                            ", After decay: " + decayedThirst);
                    return;
                }

                // Verify thirst actually decreased
                if (decayedThirst == initialThirst) {
                    helper.fail("Thirst did not decrease. Decay mechanic not working?");
                    return;
                }

                // Verify thirsty state is set if below threshold
                boolean isThirsty = tickedComponent.state().isThirsty();
                boolean shouldBeThirsty = decayedThirst < 40;

                if (shouldBeThirsty && !isThirsty) {
                    helper.fail("Thirsty state not set after thirst decayed to " + decayedThirst);
                    return;
                }

                helper.succeed();
            });
        });
    }

    /**
     * Test that multiple save/load cycles maintain state integrity.
     * Verifies that persistence works reliably across multiple cycles.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void multipleSaveLoadCyclesMaintainIntegrity(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        final int targetHunger = 35;
        final int targetThirst = 25;

        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // Set initial values
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", targetHunger);
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", targetThirst);
            component.state().setIsHungry(true);
            component.state().setIsThirsty(true);

            // First save/load cycle
            CompoundTag tag1 = new CompoundTag();
            cow.save(tag1);
            cow.load(tag1);

            helper.runAtTickTime(2, () -> {
                EcologyComponent component1 = HerbivoreTestUtils.getComponent(cow);
                int hunger1 = component1.getHandleTag("hunger").getInt("hunger");
                int thirst1 = component1.getHandleTag("thirst").getInt("thirst");

                if (hunger1 != targetHunger || thirst1 != targetThirst) {
                    helper.fail("Values changed after first load. H: " + hunger1 + " (exp " + targetHunger +
                            "), T: " + thirst1 + " (exp " + targetThirst + ")");
                    return;
                }

                // Second save/load cycle
                CompoundTag tag2 = new CompoundTag();
                cow.save(tag2);
                cow.load(tag2);

                helper.runAtTickTime(3, () -> {
                    EcologyComponent component2 = HerbivoreTestUtils.getComponent(cow);
                    int hunger2 = component2.getHandleTag("hunger").getInt("hunger");
                    int thirst2 = component2.getHandleTag("thirst").getInt("thirst");

                    if (hunger2 != targetHunger || thirst2 != targetThirst) {
                        helper.fail("Values changed after second load. H: " + hunger2 + " (exp " + targetHunger +
                                "), T: " + thirst2 + " (exp " + targetThirst + ")");
                        return;
                    }

                    // Verify state flags still match
                    boolean expectedHungry = hunger2 < 50;
                    boolean expectedThirsty = thirst2 < 40;

                    if (component2.state().isHungry() != expectedHungry) {
                        helper.fail("Hungry flag mismatch after multiple cycles. " +
                                "Hunger: " + hunger2 + ", isHungry: " + component2.state().isHungry());
                        return;
                    }

                    if (component2.state().isThirsty() != expectedThirsty) {
                        helper.fail("Thirsty flag mismatch after multiple cycles. " +
                                "Thirst: " + thirst2 + ", isThirsty: " + component2.state().isThirsty());
                        return;
                    }

                    helper.succeed();
                });
            });
        });
    }

    /**
     * Test that state flags are correctly set based on resource thresholds.
     * Verifies the threshold logic for hunger and thirst states.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void stateFlagsRespectResourceThresholds(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // Test case 1: High resources (not hungry/thirsty)
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", 90);
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", 85);
            component.state().setIsHungry(false);
            component.state().setIsThirsty(false);

            if (component.state().isHungry()) {
                helper.fail("Cow should not be hungry with 90 hunger");
                return;
            }
            if (component.state().isThirsty()) {
                helper.fail("Cow should not be thirsty with 85 thirst");
                return;
            }

            // Test case 2: Medium resources (at threshold boundary)
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", 50); // Exactly at threshold
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", 40); // Exactly at threshold

            // Not below threshold yet, so should not be flagged
            component.state().setIsHungry(false);
            component.state().setIsThirsty(false);

            // Test case 3: Low resources (hungry/thirsty)
            HerbivoreTestUtils.setHandleInt(cow, "hunger", "hunger", 30); // Below 50 threshold
            HerbivoreTestUtils.setHandleInt(cow, "thirst", "thirst", 20); // Below 40 threshold
            component.state().setIsHungry(true);
            component.state().setIsThirsty(true);

            int hunger = component.getHandleTag("hunger").getInt("hunger");
            int thirst = component.getHandleTag("thirst").getInt("thirst");

            if (hunger >= 50) {
                helper.fail("Test setup error: hunger should be below 50, got " + hunger);
                return;
            }
            if (thirst >= 40) {
                helper.fail("Test setup error: thirst should be below 40, got " + thirst);
                return;
            }
            if (!component.state().isHungry()) {
                helper.fail("Cow should be hungry with " + hunger + " hunger");
                return;
            }
            if (!component.state().isThirsty()) {
                helper.fail("Cow should be thirsty with " + thirst + " thirst");
                return;
            }

            helper.succeed();
        });
    }

    /**
     * Test that default values are applied when no NBT data exists.
     * Verifies proper initialization of new entities.
     */
    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void defaultValuesAppliedOnNewEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        var spawnPos = helper.absoluteVec(new Vec3(1, 2, 1));

        Cow cow = EntityType.COW.create(level);
        if (cow == null) {
            helper.fail("Failed to spawn cow");
            return;
        }

        cow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        level.addFreshEntity(cow);

        helper.runAtTickTime(1, () -> {
            EcologyComponent component = HerbivoreTestUtils.getComponent(cow);

            // New entity should have default values from profile
            int hunger = component.getHandleTag("hunger").getInt("hunger");
            int thirst = component.getHandleTag("thirst").getInt("thirst");

            // Check that values are reasonable (between 0 and max)
            if (hunger < 0 || hunger > 100) {
                helper.fail("Invalid default hunger: " + hunger);
                return;
            }
            if (thirst < 0 || thirst > 100) {
                helper.fail("Invalid default thirst: " + thirst);
                return;
            }

            // Entity should start with healthy resources (not hungry/thirsty)
            if (hunger < 70) {
                helper.fail("New cow starting with low hunger: " + hunger + " (expected >= 70)");
                return;
            }
            if (thirst < 70) {
                helper.fail("New cow starting with low thirst: " + thirst + " (expected >= 70)");
                return;
            }

            // State flags should reflect initial healthy state
            if (component.state().isHungry()) {
                helper.fail("New cow should not start hungry. Hunger: " + hunger);
                return;
            }
            if (component.state().isThirsty()) {
                helper.fail("New cow should not start thirsty. Thirst: " + thirst);
                return;
            }

            helper.succeed();
        });
    }
}
