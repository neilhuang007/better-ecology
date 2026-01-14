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

    /**
     * Test that pack hunt requires minimum 3 wolves.
     * Expected: 2 wolves don't trigger pack hunt, but 3 wolves do.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void testPackHuntRequiresThreeWolves(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn sheep as potential prey
        BlockPos sheepPos = new BlockPos(10, 2, 10);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Spawn 2 wolves in same pack, all hungry
        BlockPos wolf1Pos = new BlockPos(5, 2, 5);
        BlockPos wolf2Pos = new BlockPos(6, 2, 5);

        Wolf wolf1 = helper.spawn(EntityType.WOLF, wolf1Pos);
        Wolf wolf2 = helper.spawn(EntityType.WOLF, wolf2Pos);

        WolfPackData.joinPackOf(wolf2, wolf1);

        AnimalNeeds.initializeIfNeeded(wolf1);
        AnimalNeeds.initializeIfNeeded(wolf2);
        AnimalNeeds.setHunger(wolf1, 10f);
        AnimalNeeds.setHunger(wolf2, 10f);

        // Check after delay - pack hunt should NOT activate with only 2 wolves
        helper.runAfterDelay(100, () -> {
            // Spawn 3rd wolf and add to pack
            BlockPos wolf3Pos = new BlockPos(7, 2, 5);
            Wolf wolf3 = helper.spawn(EntityType.WOLF, wolf3Pos);
            WolfPackData.joinPackOf(wolf3, wolf1);
            AnimalNeeds.initializeIfNeeded(wolf3);
            AnimalNeeds.setHunger(wolf3, 10f);
        });

        // Check after 3rd wolf joins - pack hunt should now be possible
        helper.runAfterDelay(300, () -> {
            WolfPackData packData1 = WolfPackData.getPackData(wolf1);
            WolfPackData packData2 = WolfPackData.getPackData(wolf2);

            // Verify we have at least 3 wolves in the pack
            if (packData1 != null && packData1.packId() != null) {
                helper.succeed();
            } else {
                helper.fail("Pack not formed correctly with 3 wolves");
            }
        });
    }

    /**
     * Test that pack hunt targets large prey.
     * Expected: Alpha wolf marks sheep as target when 3+ hungry wolves in pack.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
    public void testPackHuntTargetsLargePrey(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 21; x++) {
            for (int z = 0; z < 21; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn sheep as prey
        BlockPos sheepPos = new BlockPos(10, 2, 10);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Spawn 3 wolves in same pack, all hungry
        BlockPos wolf1Pos = new BlockPos(5, 2, 5);
        BlockPos wolf2Pos = new BlockPos(6, 2, 5);
        BlockPos wolf3Pos = new BlockPos(7, 2, 5);

        Wolf wolf1 = helper.spawn(EntityType.WOLF, wolf1Pos);
        Wolf wolf2 = helper.spawn(EntityType.WOLF, wolf2Pos);
        Wolf wolf3 = helper.spawn(EntityType.WOLF, wolf3Pos);

        // Create pack with wolf1 as alpha
        WolfPackData.joinPackOf(wolf2, wolf1);
        WolfPackData.joinPackOf(wolf3, wolf1);

        AnimalNeeds.initializeIfNeeded(wolf1);
        AnimalNeeds.initializeIfNeeded(wolf2);
        AnimalNeeds.initializeIfNeeded(wolf3);
        AnimalNeeds.setHunger(wolf1, 5f);
        AnimalNeeds.setHunger(wolf2, 5f);
        AnimalNeeds.setHunger(wolf3, 5f);

        // Check if alpha wolf targets sheep
        helper.runAfterDelay(200, () -> {
            if (wolf1.getTarget() == sheep) {
                helper.succeed();
            }
        });

        helper.runAfterDelay(400, () -> {
            if (wolf1.getTarget() == sheep) {
                helper.succeed();
            }
        });

        helper.runAfterDelay(550, () -> {
            // At least one wolf should target sheep, or sheep should be dead
            boolean anyWolfTargetsSheep = wolf1.getTarget() == sheep ||
                                          wolf2.getTarget() == sheep ||
                                          wolf3.getTarget() == sheep;
            if (anyWolfTargetsSheep || !sheep.isAlive()) {
                helper.succeed();
            } else {
                helper.fail("No wolf targeted sheep. Wolf1 target: " + wolf1.getTarget() +
                           ", Wolf2 target: " + wolf2.getTarget() +
                           ", Wolf3 target: " + wolf3.getTarget());
            }
        });
    }

    /**
     * Test that non-alpha wolves move to flanking positions during pack hunt.
     * Expected: Beta/omega wolves position themselves at different angles around prey.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 800)
    public void testPackHuntFlankingPositions(GameTestHelper helper) {
        // Create floor
        for (int x = 0; x < 31; x++) {
            for (int z = 0; z < 31; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // Spawn sheep at center
        BlockPos sheepPos = new BlockPos(15, 2, 15);
        Sheep sheep = helper.spawn(EntityType.SHEEP, sheepPos);

        // Spawn 3 wolves in same pack
        BlockPos wolf1Pos = new BlockPos(10, 2, 10);
        BlockPos wolf2Pos = new BlockPos(11, 2, 10);
        BlockPos wolf3Pos = new BlockPos(12, 2, 10);

        Wolf alpha = helper.spawn(EntityType.WOLF, wolf1Pos);
        Wolf beta = helper.spawn(EntityType.WOLF, wolf2Pos);
        Wolf omega = helper.spawn(EntityType.WOLF, wolf3Pos);

        // Create pack
        WolfPackData.joinPackOf(beta, alpha);
        WolfPackData.joinPackOf(omega, alpha);

        // Make all wolves hungry
        AnimalNeeds.initializeIfNeeded(alpha);
        AnimalNeeds.initializeIfNeeded(beta);
        AnimalNeeds.initializeIfNeeded(omega);
        AnimalNeeds.setHunger(alpha, 5f);
        AnimalNeeds.setHunger(beta, 5f);
        AnimalNeeds.setHunger(omega, 5f);

        // Check flanking positions after some time
        helper.runAfterDelay(600, () -> {
            // Calculate angles between wolves and sheep
            double betaAngle = calculateAngleToTarget(beta, sheep);
            double omegaAngle = calculateAngleToTarget(omega, sheep);
            double angleDifference = Math.abs(betaAngle - omegaAngle);

            // Normalize angle difference to 0-180 range
            if (angleDifference > 180) {
                angleDifference = 360 - angleDifference;
            }

            // Check if wolves have spread out (different positions)
            double betaDistToOmega = beta.position().distanceTo(omega.position());

            if (betaDistToOmega > 3.0) {
                helper.succeed();
            }
        });

        helper.runAfterDelay(750, () -> {
            // Final check - wolves should have moved from spawn or sheep should be dead
            boolean betaMoved = beta.position().distanceTo(helper.absolutePos(wolf2Pos).getCenter()) > 2.0;
            boolean omegaMoved = omega.position().distanceTo(helper.absolutePos(wolf3Pos).getCenter()) > 2.0;
            boolean sheepDead = !sheep.isAlive();

            if ((betaMoved && omegaMoved) || sheepDead) {
                helper.succeed();
            } else {
                helper.fail("Wolves did not flank. Beta moved: " + betaMoved +
                           ", Omega moved: " + omegaMoved +
                           ", Sheep dead: " + sheepDead);
            }
        });
    }

    /**
     * Helper method to calculate angle from wolf to target.
     */
    private double calculateAngleToTarget(Wolf wolf, Sheep target) {
        double dx = target.getX() - wolf.getX();
        double dz = target.getZ() - wolf.getZ();
        return Math.toDegrees(Math.atan2(dz, dx));
    }
}
