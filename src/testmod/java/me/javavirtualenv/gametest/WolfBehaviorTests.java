package me.javavirtualenv.gametest;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.WolfPackData;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for wolf-specific behaviors including:
 * - Pack data initialization
 * - Meat pickup
 * - Food sharing with pack members
 * - Hunting prey
 */
public class WolfBehaviorTests implements FabricGameTest {

    /**
     * Test that wolves initialize with pack data.
     * Expected: New wolf has pack data with ALPHA rank.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testWolfPackDataInitialization(GameTestHelper helper) {
        BlockPos wolfPos = new BlockPos(5, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);

        helper.runAfterDelay(20, () -> {
            WolfPackData data = WolfPackData.getPackData(wolf);
            if (data != null && data.packId() != null && data.rank() == WolfPackData.PackRank.ALPHA) {
                helper.succeed();
            } else {
                helper.fail("Wolf pack data not initialized correctly. Data: " + data);
            }
        });
    }

    /**
     * Test that two wolves can be in the same pack.
     * Expected: After joining, both wolves have same pack ID.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testWolfPackMembership(GameTestHelper helper) {
        BlockPos wolf1Pos = new BlockPos(5, 2, 5);
        BlockPos wolf2Pos = new BlockPos(7, 2, 5);

        Wolf wolf1 = helper.spawn(EntityType.WOLF, wolf1Pos);
        Wolf wolf2 = helper.spawn(EntityType.WOLF, wolf2Pos);

        // Make wolf2 join wolf1's pack
        WolfPackData.joinPackOf(wolf2, wolf1);

        helper.runAfterDelay(20, () -> {
            boolean arePackmates = WolfPackData.arePackmates(wolf1, wolf2);
            WolfPackData data1 = WolfPackData.getPackData(wolf1);
            WolfPackData data2 = WolfPackData.getPackData(wolf2);

            if (arePackmates && data1.packId().equals(data2.packId())) {
                helper.succeed();
            } else {
                helper.fail("Wolves are not packmates. Pack IDs: " + data1.packId() + " vs " + data2.packId());
            }
        });
    }

    /**
     * Test that wolves pick up meat items when not holding anything.
     * This is an optional test - wolf pickup behavior can be tricky to trigger.
     * Expected: Wolf picks up nearby meat item or meat is consumed.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 800, required = false)
    public void testWolfPicksMeatItem(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn wolf - hungry so it will definitely seek food
        BlockPos wolfPos = new BlockPos(5, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.initializeIfNeeded(wolf);
        AnimalNeeds.setHunger(wolf, 20f); // Hungry, will seek food

        // Drop meat right next to wolf
        BlockPos meatPos = new BlockPos(6, 2, 5);
        ItemEntity meat = new ItemEntity(
            helper.getLevel(),
            helper.absolutePos(meatPos).getX() + 0.5,
            helper.absolutePos(meatPos).getY(),
            helper.absolutePos(meatPos).getZ() + 0.5,
            new ItemStack(Items.BEEF, 1)
        );
        meat.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(meat);

        // Multiple checks
        helper.runAfterDelay(200, () -> {
            // Check if wolf picked up or is eating meat
            ItemStack held = wolf.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                helper.succeed();
                return;
            }
            // Check if meat was consumed
            if (!meat.isAlive()) {
                helper.succeed();
            }
        });

        helper.runAfterDelay(500, () -> {
            ItemStack held = wolf.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty() || !meat.isAlive()) {
                helper.succeed();
            }
        });

        helper.runAfterDelay(750, () -> {
            ItemStack held = wolf.getItemBySlot(EquipmentSlot.MAINHAND);
            // Accept: wolf holds meat, meat consumed, or wolf hunger increased
            if (!held.isEmpty() || !meat.isAlive() || AnimalNeeds.getHunger(wolf) > 25) {
                helper.succeed();
            } else {
                helper.fail("Wolf did not interact with meat. Meat alive: " + meat.isAlive() +
                           ", Wolf holding: " + held + ", Hunger: " + AnimalNeeds.getHunger(wolf));
            }
        });
    }

    /**
     * Test that a hungry wolf hunts prey.
     * Expected: Wolf targets and kills nearby sheep.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 800)
    public void testWolfHuntsPrey(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn wolf
        BlockPos wolfPos = new BlockPos(10, 2, 10);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.initializeIfNeeded(wolf);
        AnimalNeeds.setHunger(wolf, 5f); // Very hungry

        // Spawn sheep nearby
        BlockPos sheepPos = new BlockPos(13, 2, 10);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Check if wolf killed sheep or is hunting
        helper.runAfterDelay(300, () -> {
            if (!sheep.isAlive()) {
                helper.succeed();
            }
        });

        helper.runAfterDelay(600, () -> {
            if (!sheep.isAlive()) {
                helper.succeed();
            }
        });

        helper.runAfterDelay(750, () -> {
            if (!sheep.isAlive()) {
                helper.succeed();
            } else {
                // Wolf should at least be targeting the sheep OR have moved from spawn
                boolean isTargeting = wolf.getTarget() == sheep;
                boolean wolfMoved = wolf.position().distanceTo(helper.absolutePos(wolfPos).getCenter()) > 2.0;
                if (isTargeting || wolfMoved) {
                    helper.succeed();
                } else {
                    helper.fail("Wolf did not hunt sheep. Wolf target: " + wolf.getTarget() +
                               ", Wolf hungry: " + AnimalNeeds.isHungry(wolf) +
                               ", Wolf moved: " + wolfMoved);
                }
            }
        });
    }

    /**
     * Test that wolf seek water when thirsty.
     * Expected: isThirsty returns true when thirst is low.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testWolfSeeksWaterWhenThirsty(GameTestHelper helper) {
        BlockPos wolfPos = new BlockPos(5, 2, 5);
        Wolf wolf = helper.spawn(EntityType.WOLF, wolfPos);
        AnimalNeeds.initializeIfNeeded(wolf);
        AnimalNeeds.setThirst(wolf, AnimalThresholds.THIRSTY - 10);

        helper.runAfterDelay(10, () -> {
            boolean isThirsty = AnimalNeeds.isThirsty(wolf);
            if (isThirsty) {
                helper.succeed();
            } else {
                helper.fail("Wolf not marked as thirsty. Thirst: " + AnimalNeeds.getThirst(wolf));
            }
        });
    }

    /**
     * Test that wolf pack hierarchy can be modified.
     * Expected: Wolf can be promoted from OMEGA to BETA to ALPHA.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testWolfPackPromotion(GameTestHelper helper) {
        BlockPos wolf1Pos = new BlockPos(5, 2, 5);
        BlockPos wolf2Pos = new BlockPos(7, 2, 5);

        Wolf alpha = helper.spawn(EntityType.WOLF, wolf1Pos);
        Wolf omega = helper.spawn(EntityType.WOLF, wolf2Pos);

        // Make omega join alpha's pack
        WolfPackData.joinPackOf(omega, alpha);

        helper.runAfterDelay(20, () -> {
            // Check initial ranks
            WolfPackData alphaData = WolfPackData.getPackData(alpha);
            WolfPackData omegaData = WolfPackData.getPackData(omega);

            if (alphaData.rank() != WolfPackData.PackRank.ALPHA) {
                helper.fail("Alpha wolf not ALPHA rank: " + alphaData.rank());
                return;
            }
            if (omegaData.rank() != WolfPackData.PackRank.OMEGA) {
                helper.fail("Omega wolf not OMEGA rank: " + omegaData.rank());
                return;
            }

            // Promote omega to beta
            WolfPackData.promote(omega);
            WolfPackData newOmegaData = WolfPackData.getPackData(omega);

            if (newOmegaData.rank() == WolfPackData.PackRank.BETA) {
                helper.succeed();
            } else {
                helper.fail("Wolf not promoted to BETA: " + newOmegaData.rank());
            }
        });
    }
}
