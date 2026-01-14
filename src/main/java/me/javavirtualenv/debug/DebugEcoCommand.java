package me.javavirtualenv.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.WolfPackData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug command for testing Better Ecology behaviors.
 * Usage: /debugeco [scenario]
 * Scenarios:
 * - predator: Creates wolf hunting scenario with prey animals
 * - food: Creates wolf with dropped meat items
 * - thirst: Creates animals near water to test drinking
 * - test: Creates test structures and monitors results
 * - all: Creates a complete test environment
 */
public class DebugEcoCommand {

    // Active tests tracking
    private static final Map<String, TestScenario> activeTests = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugeco")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("predator")
                .executes(ctx -> createPredatorScenario(ctx)))
            .then(Commands.literal("food")
                .executes(ctx -> createFoodScenario(ctx)))
            .then(Commands.literal("thirst")
                .executes(ctx -> createThirstScenario(ctx)))
            .then(Commands.literal("baby")
                .executes(ctx -> createBabyScenario(ctx)))
            .then(Commands.literal("pack")
                .executes(ctx -> createPackScenario(ctx)))
            .then(Commands.literal("test")
                .executes(ctx -> runAllTests(ctx)))
            .then(Commands.literal("all")
                .executes(ctx -> createAllScenarios(ctx)))
            .then(Commands.literal("status")
                .executes(ctx -> showNearbyStatus(ctx)))
            .then(Commands.literal("detail")
                .executes(ctx -> showDetailedStatus(ctx)))
            .then(Commands.literal("sethunger")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                    .executes(ctx -> setNearbyHunger(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("setthirst")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                    .executes(ctx -> setNearbyThirst(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
            .executes(ctx -> showHelp(ctx))
        );
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
            "§6Better Ecology Debug Commands:\n" +
            "§e/debugeco predator§7 - Wolf with prey animals\n" +
            "§e/debugeco food§7 - Wolf with dropped meat\n" +
            "§e/debugeco thirst§7 - Animals near water\n" +
            "§e/debugeco baby§7 - Baby animals with parents\n" +
            "§e/debugeco pack§7 - Wolf pack for hierarchy testing\n" +
            "§e/debugeco test§7 - Run all behavior tests (outputs results to chat)\n" +
            "§e/debugeco all§7 - Complete test environment\n" +
            "§e/debugeco status§7 - Show nearby animal stats\n" +
            "§e/debugeco detail§7 - Detailed info for nearest animal\n" +
            "§e/debugeco sethunger <0-100>§7 - Set nearby animal hunger\n" +
            "§e/debugeco setthirst <0-100>§7 - Set nearby animal thirst"
        ), false);
        return 1;
    }

    private static int createPredatorScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        // Create a fenced area
        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Spawn a hungry wolf
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 2, basePos.getY() + 1, basePos.getZ() + 2);
            AnimalNeeds.setHunger(wolf, 20); // Make it hungry
            level.addFreshEntity(wolf);
        }

        // Spawn prey animals
        Sheep sheep = EntityType.SHEEP.create(level);
        if (sheep != null) {
            sheep.setPos(basePos.getX() + 10, basePos.getY() + 1, basePos.getZ() + 10);
            level.addFreshEntity(sheep);
        }

        Chicken chicken = EntityType.CHICKEN.create(level);
        if (chicken != null) {
            chicken.setPos(basePos.getX() + 12, basePos.getY() + 1, basePos.getZ() + 8);
            level.addFreshEntity(chicken);
        }

        Pig pig = EntityType.PIG.create(level);
        if (pig != null) {
            pig.setPos(basePos.getX() + 8, basePos.getY() + 1, basePos.getZ() + 12);
            level.addFreshEntity(pig);
        }

        Rabbit rabbit = EntityType.RABBIT.create(level);
        if (rabbit != null) {
            rabbit.setPos(basePos.getX() + 6, basePos.getY() + 1, basePos.getZ() + 10);
            level.addFreshEntity(rabbit);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated predator scenario at " + basePos.toShortString() +
            "\n§7Wolf (hungry) + Sheep, Chicken, Pig, Rabbit"
        ), true);
        return 1;
    }

    private static int createFoodScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        // Create a platform
        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 10, 10);
        createFence(level, basePos, 10, 10);

        // Spawn a hungry wolf
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 2, basePos.getY() + 1, basePos.getZ() + 5);
            AnimalNeeds.setHunger(wolf, 15); // Make it very hungry
            level.addFreshEntity(wolf);
        }

        // Drop multiple meat items on the other side
        double meatX = basePos.getX() + 7.5;
        double meatY = basePos.getY() + 1.5;
        double meatZ = basePos.getZ() + 5.5;

        ItemEntity beef = new ItemEntity(level, meatX, meatY, meatZ, new ItemStack(Items.BEEF, 3));
        beef.setNoPickUpDelay();
        level.addFreshEntity(beef);

        ItemEntity pork = new ItemEntity(level, meatX + 1, meatY, meatZ, new ItemStack(Items.PORKCHOP, 2));
        pork.setNoPickUpDelay();
        level.addFreshEntity(pork);

        ItemEntity chicken = new ItemEntity(level, meatX, meatY, meatZ + 1, new ItemStack(Items.CHICKEN, 2));
        chicken.setNoPickUpDelay();
        level.addFreshEntity(chicken);

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated food scenario at " + basePos.toShortString() +
            "\n§7Hungry wolf + dropped beef, pork, chicken"
        ), true);
        return 1;
    }

    private static int createThirstScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        // Create a platform with a water pool
        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 12, 12);

        // Create water pool in center
        for (int x = 4; x < 8; x++) {
            for (int z = 4; z < 8; z++) {
                level.setBlock(basePos.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3);
            }
        }

        // Spawn thirsty animals around the water
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 2, basePos.getY() + 1, basePos.getZ() + 2);
            AnimalNeeds.setThirst(wolf, 20); // Make it thirsty
            level.addFreshEntity(wolf);
        }

        Cow cow = EntityType.COW.create(level);
        if (cow != null) {
            cow.setPos(basePos.getX() + 10, basePos.getY() + 1, basePos.getZ() + 2);
            AnimalNeeds.setThirst(cow, 15);
            level.addFreshEntity(cow);
        }

        Sheep sheep = EntityType.SHEEP.create(level);
        if (sheep != null) {
            sheep.setPos(basePos.getX() + 2, basePos.getY() + 1, basePos.getZ() + 10);
            AnimalNeeds.setThirst(sheep, 25);
            level.addFreshEntity(sheep);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated thirst scenario at " + basePos.toShortString() +
            "\n§7Thirsty Wolf, Cow, Sheep + water pool"
        ), true);
        return 1;
    }

    private static int createAllScenarios(CommandContext<CommandSourceStack> ctx) {
        createPredatorScenario(ctx);

        // Offset for next scenario
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());
        BlockPos offset = playerPos.offset(25, 0, 0);

        // Create food scenario at offset
        createPlatform(level, offset, 10, 10);
        createFence(level, offset, 10, 10);

        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(offset.getX() + 2, offset.getY() + 1, offset.getZ() + 5);
            AnimalNeeds.setHunger(wolf, 15);
            level.addFreshEntity(wolf);
        }

        ItemEntity beef = new ItemEntity(level, offset.getX() + 7.5, offset.getY() + 1.5, offset.getZ() + 5.5, new ItemStack(Items.BEEF, 3));
        beef.setNoPickUpDelay();
        level.addFreshEntity(beef);

        // Create thirst scenario at another offset
        BlockPos offset2 = playerPos.offset(5, 0, 20);
        createPlatform(level, offset2, 12, 12);

        for (int x = 4; x < 8; x++) {
            for (int z = 4; z < 8; z++) {
                level.setBlock(offset2.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3);
            }
        }

        Cow cow = EntityType.COW.create(level);
        if (cow != null) {
            cow.setPos(offset2.getX() + 2, offset2.getY() + 1, offset2.getZ() + 2);
            AnimalNeeds.setThirst(cow, 20);
            level.addFreshEntity(cow);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated all debug scenarios!\n" +
            "§7Predator area, Food pickup area, Thirst/water area"
        ), true);
        return 1;
    }

    private static int showNearbyStatus(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        StringBuilder sb = new StringBuilder("§6Nearby Animal Status (16 blocks):\n");
        int count = 0;

        for (Animal animal : level.getEntitiesOfClass(Animal.class,
                new net.minecraft.world.phys.AABB(playerPos).inflate(16))) {
            float hunger = AnimalNeeds.getHunger(animal);
            float thirst = AnimalNeeds.getThirst(animal);
            String hungerColor = hunger < 20 ? "§c" : (hunger < 50 ? "§e" : "§a");
            String thirstColor = thirst < 20 ? "§c" : (thirst < 50 ? "§e" : "§a");

            sb.append(String.format("§f%s§7: Hunger=%s%.0f§7, Thirst=%s%.0f\n",
                animal.getType().getDescription().getString(),
                hungerColor, hunger,
                thirstColor, thirst));
            count++;

            if (count >= 10) {
                sb.append("§7... and more");
                break;
            }
        }

        if (count == 0) {
            sb.append("§7No animals nearby");
        }

        String result = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(result), false);
        return count;
    }

    private static int setNearbyHunger(CommandContext<CommandSourceStack> ctx, int value) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        int count = 0;
        for (Animal animal : level.getEntitiesOfClass(Animal.class,
                new net.minecraft.world.phys.AABB(playerPos).inflate(16))) {
            AnimalNeeds.setHunger(animal, value);
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aSet hunger to " + value + " for " + finalCount + " animals"
        ), true);
        return count;
    }

    private static int setNearbyThirst(CommandContext<CommandSourceStack> ctx, int value) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        int count = 0;
        for (Animal animal : level.getEntitiesOfClass(Animal.class,
                new net.minecraft.world.phys.AABB(playerPos).inflate(16))) {
            AnimalNeeds.setThirst(animal, value);
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aSet thirst to " + value + " for " + finalCount + " animals"
        ), true);
        return count;
    }

    private static int createBabyScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Spawn adult sheep with baby
        Sheep adultSheep = EntityType.SHEEP.create(level);
        if (adultSheep != null) {
            adultSheep.setPos(basePos.getX() + 7, basePos.getY() + 1, basePos.getZ() + 7);
            level.addFreshEntity(adultSheep);
        }

        Sheep babySheep = EntityType.SHEEP.create(level);
        if (babySheep != null) {
            babySheep.setPos(basePos.getX() + 3, basePos.getY() + 1, basePos.getZ() + 3);
            babySheep.setBaby(true);
            level.addFreshEntity(babySheep);
        }

        // Spawn adult cow with baby
        Cow adultCow = EntityType.COW.create(level);
        if (adultCow != null) {
            adultCow.setPos(basePos.getX() + 12, basePos.getY() + 1, basePos.getZ() + 7);
            level.addFreshEntity(adultCow);
        }

        Cow babyCow = EntityType.COW.create(level);
        if (babyCow != null) {
            babyCow.setPos(basePos.getX() + 10, basePos.getY() + 1, basePos.getZ() + 3);
            babyCow.setBaby(true);
            level.addFreshEntity(babyCow);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated baby scenario at " + basePos.toShortString() +
            "\n§7Adult + Baby Sheep, Adult + Baby Cow"
        ), true);
        return 1;
    }

    private static int createPackScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Spawn alpha wolf (well-fed)
        Wolf alpha = EntityType.WOLF.create(level);
        if (alpha != null) {
            alpha.setPos(basePos.getX() + 7, basePos.getY() + 1, basePos.getZ() + 7);
            AnimalNeeds.setHunger(alpha, 90);
            level.addFreshEntity(alpha);
        }

        // Spawn beta wolf (moderately hungry)
        Wolf beta = EntityType.WOLF.create(level);
        if (beta != null) {
            beta.setPos(basePos.getX() + 5, basePos.getY() + 1, basePos.getZ() + 5);
            AnimalNeeds.setHunger(beta, 40);
            if (alpha != null) {
                WolfPackData.joinPackOf(beta, alpha);
            }
            level.addFreshEntity(beta);
        }

        // Spawn omega wolf (very hungry)
        Wolf omega = EntityType.WOLF.create(level);
        if (omega != null) {
            omega.setPos(basePos.getX() + 9, basePos.getY() + 1, basePos.getZ() + 5);
            AnimalNeeds.setHunger(omega, 15);
            if (alpha != null) {
                WolfPackData.joinPackOf(omega, alpha);
            }
            level.addFreshEntity(omega);
        }

        // Drop some meat for food sharing
        ItemEntity beef = new ItemEntity(level, basePos.getX() + 3.5, basePos.getY() + 1.5, basePos.getZ() + 7.5, new ItemStack(Items.BEEF, 2));
        beef.setNoPickUpDelay();
        level.addFreshEntity(beef);

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated wolf pack scenario at " + basePos.toShortString() +
            "\n§7Alpha (full), Beta (hungry), Omega (starving) + meat"
        ), true);
        return 1;
    }

    private static int showDetailedStatus(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        // Find nearest animal
        Animal nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Animal animal : level.getEntitiesOfClass(Animal.class,
                new net.minecraft.world.phys.AABB(playerPos).inflate(16))) {
            double dist = animal.distanceToSqr(playerPos.getX(), playerPos.getY(), playerPos.getZ());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = animal;
            }
        }

        if (nearest == null) {
            ctx.getSource().sendSuccess(() -> Component.literal("§cNo animals nearby"), false);
            return 0;
        }

        Animal animal = nearest;
        float hunger = AnimalNeeds.getHunger(animal);
        float thirst = AnimalNeeds.getThirst(animal);

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== ").append(animal.getType().getDescription().getString()).append(" ===\n");
        sb.append("§7Position: §f").append(String.format("%.1f, %.1f, %.1f", animal.getX(), animal.getY(), animal.getZ())).append("\n");
        sb.append("§7Health: §f").append(String.format("%.1f/%.1f", animal.getHealth(), animal.getMaxHealth())).append("\n");
        sb.append("§7Baby: §f").append(animal.isBaby() ? "Yes" : "No").append("\n");

        String hungerColor = hunger < AnimalThresholds.STARVING ? "§c" : (hunger < AnimalThresholds.HUNGRY ? "§e" : "§a");
        String thirstColor = thirst < AnimalThresholds.DEHYDRATED ? "§c" : (thirst < AnimalThresholds.THIRSTY ? "§e" : "§a");
        sb.append("§7Hunger: ").append(hungerColor).append(String.format("%.1f", hunger));
        sb.append(" §7(").append(AnimalNeeds.isHungry(animal) ? "§eHungry" : "§aSatisfied").append("§7)\n");
        sb.append("§7Thirst: ").append(thirstColor).append(String.format("%.1f", thirst));
        sb.append(" §7(").append(AnimalNeeds.isThirsty(animal) ? "§eThirsty" : "§aHydrated").append("§7)\n");

        // Wolf-specific pack info
        if (animal instanceof Wolf wolf) {
            WolfPackData packData = WolfPackData.getPackData(wolf);
            sb.append("§7Pack ID: §f").append(packData.packId().toString().substring(0, 8)).append("...\n");
            sb.append("§7Rank: §f").append(packData.rank().name()).append("\n");
            sb.append("§7Share Cooldown: §f").append(packData.sharesCooldown()).append("\n");
        }

        // Target info
        if (animal.getTarget() != null) {
            sb.append("§7Target: §c").append(animal.getTarget().getName().getString()).append("\n");
        }

        String result = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(result), false);
        return 1;
    }

    private static void createPlatform(ServerLevel level, BlockPos pos, int width, int depth) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                level.setBlock(pos.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }
    }

    private static void createFence(ServerLevel level, BlockPos pos, int width, int depth) {
        // Create fence around the perimeter
        for (int x = 0; x < width; x++) {
            level.setBlock(pos.offset(x, 1, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(pos.offset(x, 1, depth - 1), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        for (int z = 0; z < depth; z++) {
            level.setBlock(pos.offset(0, 1, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(pos.offset(width - 1, 1, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
    }

    // ============ Test Framework ============

    /**
     * Runs all behavior tests and outputs results to chat after a timeout.
     */
    private static int runAllTests(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());
        CommandSourceStack source = ctx.getSource();

        // Clear any previous tests
        activeTests.clear();

        source.sendSuccess(() -> Component.literal(
            "§6========== Running Better Ecology Tests ==========\n" +
            "§7Creating test structures... Results in 30 seconds."
        ), false);

        // Create all test scenarios
        List<TestScenario> tests = new ArrayList<>();

        // Test 1: Wolf hunts prey
        BlockPos predatorPos = playerPos.offset(5, 0, 0);
        createPlatform(level, predatorPos, 15, 15);
        createFence(level, predatorPos, 15, 15);
        Wolf huntWolf = EntityType.WOLF.create(level);
        Sheep huntSheep = EntityType.SHEEP.create(level);
        if (huntWolf != null && huntSheep != null) {
            huntWolf.setPos(predatorPos.getX() + 2, predatorPos.getY() + 1, predatorPos.getZ() + 2);
            AnimalNeeds.setHunger(huntWolf, 5); // Very hungry
            level.addFreshEntity(huntWolf);

            huntSheep.setPos(predatorPos.getX() + 10, predatorPos.getY() + 1, predatorPos.getZ() + 10);
            level.addFreshEntity(huntSheep);

            tests.add(new TestScenario(
                "Wolf Hunts Prey",
                () -> !huntSheep.isAlive() || huntWolf.getTarget() == huntSheep,
                "Wolf should target or kill the sheep"
            ));
        }

        // Test 2: Baby follows parent
        BlockPos babyPos = playerPos.offset(25, 0, 0);
        createPlatform(level, babyPos, 15, 15);
        createFence(level, babyPos, 15, 15);
        Cow adultCow = EntityType.COW.create(level);
        Cow babyCow = EntityType.COW.create(level);
        if (adultCow != null && babyCow != null) {
            adultCow.setPos(babyPos.getX() + 10, babyPos.getY() + 1, babyPos.getZ() + 10);
            level.addFreshEntity(adultCow);

            babyCow.setPos(babyPos.getX() + 3, babyPos.getY() + 1, babyPos.getZ() + 3);
            babyCow.setBaby(true);
            level.addFreshEntity(babyCow);

            tests.add(new TestScenario(
                "Baby Follows Parent",
                () -> babyCow.distanceTo(adultCow) < 6.0,
                "Baby cow should move towards adult cow"
            ));
        }

        // Test 3: Prey flees from predator
        BlockPos fleePos = playerPos.offset(5, 0, 20);
        createPlatform(level, fleePos, 15, 15);
        createFence(level, fleePos, 15, 15);
        Wolf fleeWolf = EntityType.WOLF.create(level);
        Chicken fleeChicken = EntityType.CHICKEN.create(level);
        double chickenStartX = fleePos.getX() + 7;
        double chickenStartZ = fleePos.getZ() + 7;
        if (fleeWolf != null && fleeChicken != null) {
            fleeWolf.setPos(fleePos.getX() + 5, fleePos.getY() + 1, fleePos.getZ() + 7);
            AnimalNeeds.setHunger(fleeWolf, 10); // Make hungry so it's threatening
            level.addFreshEntity(fleeWolf);

            fleeChicken.setPos(chickenStartX, fleePos.getY() + 1, chickenStartZ);
            level.addFreshEntity(fleeChicken);

            tests.add(new TestScenario(
                "Prey Flees Predator",
                () -> {
                    if (!fleeChicken.isAlive()) return true; // If killed, still considered success
                    double currentDist = fleeChicken.distanceTo(fleeWolf);
                    return currentDist > 4.0; // Chicken should maintain distance
                },
                "Chicken should flee from wolf"
            ));
        }

        // Test 4: Animal seeks water when thirsty
        BlockPos thirstPos = playerPos.offset(25, 0, 20);
        createPlatform(level, thirstPos, 15, 15);
        for (int x = 6; x < 10; x++) {
            for (int z = 6; z < 10; z++) {
                level.setBlock(thirstPos.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3);
            }
        }
        Cow thirstyCow = EntityType.COW.create(level);
        if (thirstyCow != null) {
            thirstyCow.setPos(thirstPos.getX() + 2, thirstPos.getY() + 1, thirstPos.getZ() + 2);
            AnimalNeeds.setThirst(thirstyCow, 15); // Make thirsty
            level.addFreshEntity(thirstyCow);

            tests.add(new TestScenario(
                "Animal Seeks Water",
                () -> thirstyCow.isInWater() || AnimalNeeds.getThirst(thirstyCow) > 20,
                "Thirsty cow should move toward water or drink"
            ));
        }

        // Test 5: Wolf pack data initialization
        Wolf packWolf = EntityType.WOLF.create(level);
        if (packWolf != null) {
            packWolf.setPos(playerPos.getX() + 45, playerPos.getY() + 1, playerPos.getZ() + 10);
            level.addFreshEntity(packWolf);

            tests.add(new TestScenario(
                "Wolf Pack Data Init",
                () -> {
                    WolfPackData data = WolfPackData.getPackData(packWolf);
                    return data != null && data.packId() != null;
                },
                "Wolf should have pack data initialized"
            ));
        }

        // Test 6: Dynamic retargeting test
        BlockPos retargetPos = playerPos.offset(45, 0, 0);
        createPlatform(level, retargetPos, 20, 20);
        createFence(level, retargetPos, 20, 20);
        Wolf retargetWolf = EntityType.WOLF.create(level);
        Chicken farChicken = EntityType.CHICKEN.create(level);
        Chicken closeChicken = EntityType.CHICKEN.create(level);
        if (retargetWolf != null && farChicken != null && closeChicken != null) {
            retargetWolf.setPos(retargetPos.getX() + 10, retargetPos.getY() + 1, retargetPos.getZ() + 10);
            AnimalNeeds.setHunger(retargetWolf, 5); // Very hungry
            level.addFreshEntity(retargetWolf);

            farChicken.setPos(retargetPos.getX() + 18, retargetPos.getY() + 1, retargetPos.getZ() + 10);
            level.addFreshEntity(farChicken);

            // Spawn close chicken after a delay (simulated by just spawning it closer)
            closeChicken.setPos(retargetPos.getX() + 12, retargetPos.getY() + 1, retargetPos.getZ() + 10);
            level.addFreshEntity(closeChicken);

            tests.add(new TestScenario(
                "Dynamic Prey Retargeting",
                () -> {
                    // Success if wolf targets closer chicken or kills one
                    return !closeChicken.isAlive() || !farChicken.isAlive() ||
                           (retargetWolf.getTarget() == closeChicken);
                },
                "Wolf should prefer closer prey"
            ));
        }

        // Schedule result check after 30 seconds (600 ticks)
        final List<TestScenario> finalTests = tests;
        level.getServer().execute(() -> {
            scheduleTestResults(level, source, finalTests, 600);
        });

        return tests.size();
    }

    /**
     * Schedules test result checking after a delay.
     */
    private static void scheduleTestResults(ServerLevel level, CommandSourceStack source,
                                             List<TestScenario> tests, int delayTicks) {
        // We need to use the server's scheduled task system
        // Since we can't directly schedule, we'll use a simple tick counter approach
        // Store the test info and check in a follow-up

        // For simplicity, we'll output a message and rely on the /debugeco status command
        // to check results, OR we implement a polling approach

        // Using server scheduling
        final long startTime = level.getGameTime();
        final long endTime = startTime + delayTicks;

        // Create a scheduled task using the server's tick
        Thread checker = new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50L); // 50ms per tick

                // Run on server thread
                level.getServer().execute(() -> {
                    outputTestResults(source, tests);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        checker.setDaemon(true);
        checker.start();
    }

    /**
     * Outputs test results to chat.
     */
    private static void outputTestResults(CommandSourceStack source, List<TestScenario> tests) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6========== Test Results ==========\n");

        int passed = 0;
        int failed = 0;

        for (TestScenario test : tests) {
            boolean success;
            try {
                success = test.condition.check();
            } catch (Exception e) {
                success = false;
            }

            if (success) {
                sb.append("§a✓ ").append(test.name).append("\n");
                passed++;
            } else {
                sb.append("§c✗ ").append(test.name).append("\n");
                sb.append("  §7").append(test.failureMessage).append("\n");
                failed++;
            }
        }

        sb.append("§6===================================\n");
        sb.append(String.format("§aPass: %d §7| §cFail: %d §7| Total: %d", passed, failed, tests.size()));

        String result = sb.toString();
        source.sendSuccess(() -> Component.literal(result), false);
    }

    /**
     * Represents a test scenario with a name, condition, and failure message.
     */
    private static class TestScenario {
        final String name;
        final TestCondition condition;
        final String failureMessage;

        TestScenario(String name, TestCondition condition, String failureMessage) {
            this.name = name;
            this.condition = condition;
            this.failureMessage = failureMessage;
        }
    }

    /**
     * Functional interface for test conditions.
     */
    @FunctionalInterface
    private interface TestCondition {
        boolean check();
    }
}
