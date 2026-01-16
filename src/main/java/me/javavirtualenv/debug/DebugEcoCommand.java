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
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug command for testing Better Ecology behaviors.
 * Usage: /debugeco [scenario]
 * Scenarios:
 * - predator: Creates wolf hunting scenario with prey animals
 * - food: Creates wolf with dropped meat items
 * - thirst: Creates animals near water to test drinking
 * - test: Creates test structures for manual observation
 * - all: Creates a complete test environment
 */
public class DebugEcoCommand {

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
            .then(Commands.literal("herd")
                .executes(ctx -> createHerdScenario(ctx)))
            .then(Commands.literal("dustbath")
                .executes(ctx -> createDustBathScenario(ctx)))
            .then(Commands.literal("roost")
                .executes(ctx -> createRoostScenario(ctx)))
            .then(Commands.literal("freeze")
                .executes(ctx -> createFreezeScenario(ctx)))
            .then(Commands.literal("zigzag")
                .executes(ctx -> createZigzagScenario(ctx)))
            .then(Commands.literal("pounce")
                .executes(ctx -> createPounceScenario(ctx)))
            .then(Commands.literal("ambush")
                .executes(ctx -> createAmbushScenario(ctx)))
            .then(Commands.literal("protect")
                .executes(ctx -> createProtectScenario(ctx)))
            .then(Commands.literal("school")
                .executes(ctx -> createSchoolScenario(ctx)))
            .then(Commands.literal("slope")
                .executes(ctx -> createSlopeScenario(ctx)))
            .then(Commands.literal("ridgeline")
                .executes(ctx -> createRidgelineScenario(ctx)))
            .then(Commands.literal("pathfind")
                .executes(ctx -> createPathfindScenario(ctx)))
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
            "§e/debugeco herd§7 - Herd cohesion test (Cows/Sheep)\n" +
            "§e/debugeco dustbath§7 - Chicken dust bathing test\n" +
            "§e/debugeco roost§7 - Chicken roosting test\n" +
            "§e/debugeco freeze§7 - Rabbit freeze behavior test\n" +
            "§e/debugeco zigzag§7 - Rabbit zigzag flee test\n" +
            "§e/debugeco pounce§7 - Fox pouncing test\n" +
            "§e/debugeco ambush§7 - Frog ambush test\n" +
            "§e/debugeco protect§7 - Parent protection test\n" +
            "§e/debugeco school§7 - Fish schooling test\n" +
            "§e/debugeco slope§7 - Slope pathfinding test\n" +
            "§e/debugeco ridgeline§7 - Ridgeline avoidance test\n" +
            "§e/debugeco pathfind§7 - Full pathfinding showcase\n" +
            "§e/debugeco test§7 - Run all behavior tests\n" +
            "§e/debugeco all§7 - Complete test environment\n" +
            "§e/debugeco status§7 - Show nearby animal stats\n" +
            "§e/debugeco detail§7 - Detailed info for nearest animal\n" +
            "§e/debugeco sethunger <0-100>§7 - Set nearby animal hunger\n" +
            "§e/debugeco setthirst <0-100>§7 - Set nearby animal thirst\n" +
            "§e/ecologyoverlay§7 - Toggle debug HUD overlay"
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

        source.sendSuccess(() -> Component.literal(
            "§6========== Running Better Ecology Tests ==========\n" +
            "§7Creating test structures...\n" +
            "§7Run §e/debugeco status§7 to check results manually."
        ), false);

        // Create all test scenarios
        List<String> tests = new ArrayList<>();

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
            tests.add("Wolf Hunts Prey - Wolf should target or kill sheep");
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
            tests.add("Baby Follows Parent - Baby cow should move towards adult");
        }

        // Test 3: Prey flees from predator
        BlockPos fleePos = playerPos.offset(5, 0, 20);
        createPlatform(level, fleePos, 15, 15);
        createFence(level, fleePos, 15, 15);
        Wolf fleeWolf = EntityType.WOLF.create(level);
        Chicken fleeChicken = EntityType.CHICKEN.create(level);
        if (fleeWolf != null && fleeChicken != null) {
            fleeWolf.setPos(fleePos.getX() + 5, fleePos.getY() + 1, fleePos.getZ() + 7);
            AnimalNeeds.setHunger(fleeWolf, 10); // Make hungry so it's threatening
            level.addFreshEntity(fleeWolf);

            fleeChicken.setPos(fleePos.getX() + 7, fleePos.getY() + 1, fleePos.getZ() + 7);
            level.addFreshEntity(fleeChicken);
            tests.add("Prey Flees Predator - Chicken should flee from wolf");
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
            tests.add("Animal Seeks Water - Thirsty cow should move toward water");
        }

        // Test 5: Wolf pack data initialization
        Wolf packWolf = EntityType.WOLF.create(level);
        if (packWolf != null) {
            packWolf.setPos(playerPos.getX() + 45, playerPos.getY() + 1, playerPos.getZ() + 10);
            level.addFreshEntity(packWolf);
            tests.add("Wolf Pack Data Init - Wolf should have pack data initialized");
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

            closeChicken.setPos(retargetPos.getX() + 12, retargetPos.getY() + 1, retargetPos.getZ() + 10);
            level.addFreshEntity(closeChicken);
            tests.add("Dynamic Prey Retargeting - Wolf should prefer closer prey");
        }

        source.sendSuccess(() -> Component.literal(
            String.format("§aCreated %d test scenarios.\n" +
                "§7Observe the areas for a minute or two, then use §e/debugeco status§7 to inspect animals.", tests.size())
        ), false);

        return tests.size();
    }

    private static int createHerdScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 25, 25);
        createFence(level, basePos, 25, 25);

        // Spawn multiple cows to test herd cohesion
        for (int i = 0; i < 8; i++) {
            Cow cow = EntityType.COW.create(level);
            if (cow != null) {
                double x = basePos.getX() + 5 + (i % 4) * 4;
                double z = basePos.getZ() + 5 + (i / 4) * 4;
                cow.setPos(x, basePos.getY() + 1, z);
                level.addFreshEntity(cow);
            }
        }

        // Spawn multiple sheep to test herd cohesion
        for (int i = 0; i < 8; i++) {
            Sheep sheep = EntityType.SHEEP.create(level);
            if (sheep != null) {
                double x = basePos.getX() + 12 + (i % 4) * 4;
                double z = basePos.getZ() + 12 + (i / 4) * 4;
                sheep.setPos(x, basePos.getY() + 1, z);
                level.addFreshEntity(sheep);
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated herd scenario at " + basePos.toShortString() +
            "\n§78 Cows + 8 Sheep for herd cohesion testing"
        ), true);
        return 1;
    }

    private static int createDustBathScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Replace some grass with dirt for dust bathing
        for (int x = 5; x < 10; x++) {
            for (int z = 5; z < 10; z++) {
                level.setBlock(basePos.offset(x, 0, z), Blocks.DIRT.defaultBlockState(), 3);
            }
        }

        // Spawn chickens
        for (int i = 0; i < 5; i++) {
            Chicken chicken = EntityType.CHICKEN.create(level);
            if (chicken != null) {
                double x = basePos.getX() + 2 + i * 2;
                double z = basePos.getZ() + 7;
                chicken.setPos(x, basePos.getY() + 1, z);
                level.addFreshEntity(chicken);
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated dust bath scenario at " + basePos.toShortString() +
            "\n§75 Chickens + dirt patch for dust bathing"
        ), true);
        return 1;
    }

    private static int createRoostScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Create fence posts at different heights for roosting
        for (int x = 4; x < 12; x += 3) {
            for (int z = 4; z < 12; z += 3) {
                int height = 2 + ((x + z) % 3);
                for (int y = 1; y <= height; y++) {
                    level.setBlock(basePos.offset(x, y, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                }
            }
        }

        // Spawn chickens
        for (int i = 0; i < 6; i++) {
            Chicken chicken = EntityType.CHICKEN.create(level);
            if (chicken != null) {
                double x = basePos.getX() + 2 + i * 2;
                double z = basePos.getZ() + 2;
                chicken.setPos(x, basePos.getY() + 1, z);
                level.addFreshEntity(chicken);
            }
        }

        // Set time to night for roosting
        long currentTime = level.getDayTime();
        long timeOfDay = currentTime % 24000;
        if (timeOfDay < 13000 || timeOfDay > 23000) {
            level.setDayTime(currentTime + (13000 - timeOfDay));
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated roost scenario at " + basePos.toShortString() +
            "\n§76 Chickens + fence posts at varying heights\n§7Time set to night for roosting behavior"
        ), true);
        return 1;
    }

    private static int createFreezeScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 30, 10);
        createFence(level, basePos, 30, 10);

        // Spawn rabbit at one end
        Rabbit rabbit = EntityType.RABBIT.create(level);
        if (rabbit != null) {
            rabbit.setPos(basePos.getX() + 3, basePos.getY() + 1, basePos.getZ() + 5);
            level.addFreshEntity(rabbit);
        }

        // Spawn wolf at the other end (far enough to trigger freeze but not immediate flee)
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 20, basePos.getY() + 1, basePos.getZ() + 5);
            AnimalNeeds.setHunger(wolf, 15);
            level.addFreshEntity(wolf);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated freeze scenario at " + basePos.toShortString() +
            "\n§7Rabbit + distant Wolf to trigger freeze behavior"
        ), true);
        return 1;
    }

    private static int createZigzagScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 20, 20);
        createFence(level, basePos, 20, 20);

        // Spawn rabbit
        Rabbit rabbit = EntityType.RABBIT.create(level);
        if (rabbit != null) {
            rabbit.setPos(basePos.getX() + 10, basePos.getY() + 1, basePos.getZ() + 10);
            level.addFreshEntity(rabbit);
        }

        // Spawn wolf closer to trigger flee with zigzag
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 6, basePos.getY() + 1, basePos.getZ() + 10);
            AnimalNeeds.setHunger(wolf, 10);
            level.addFreshEntity(wolf);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated zigzag scenario at " + basePos.toShortString() +
            "\n§7Rabbit + close Wolf to trigger zigzag flee behavior"
        ), true);
        return 1;
    }

    private static int createPounceScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Spawn fox
        Fox fox = EntityType.FOX.create(level);
        if (fox != null) {
            fox.setPos(basePos.getX() + 3, basePos.getY() + 1, basePos.getZ() + 7);
            AnimalNeeds.setHunger(fox, 15);
            level.addFreshEntity(fox);
        }

        // Spawn chicken as prey
        Chicken chicken = EntityType.CHICKEN.create(level);
        if (chicken != null) {
            chicken.setPos(basePos.getX() + 10, basePos.getY() + 1, basePos.getZ() + 7);
            level.addFreshEntity(chicken);
        }

        // Spawn rabbit as alternative prey
        Rabbit rabbit = EntityType.RABBIT.create(level);
        if (rabbit != null) {
            rabbit.setPos(basePos.getX() + 7, basePos.getY() + 1, basePos.getZ() + 10);
            level.addFreshEntity(rabbit);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated pounce scenario at " + basePos.toShortString() +
            "\n§7Hungry Fox + Chicken + Rabbit for pouncing behavior"
        ), true);
        return 1;
    }

    private static int createAmbushScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Add some water and lily pads for frog
        for (int x = 5; x < 10; x++) {
            for (int z = 5; z < 10; z++) {
                level.setBlock(basePos.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3);
            }
        }

        // Spawn frog
        Frog frog = EntityType.FROG.create(level);
        if (frog != null) {
            frog.setPos(basePos.getX() + 7, basePos.getY() + 1, basePos.getZ() + 7);
            level.addFreshEntity(frog);
        }

        // Spawn small slime as prey
        Slime slime = EntityType.SLIME.create(level);
        if (slime != null) {
            slime.setPos(basePos.getX() + 10, basePos.getY() + 1, basePos.getZ() + 10);
            slime.setSize(1, true);
            level.addFreshEntity(slime);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated ambush scenario at " + basePos.toShortString() +
            "\n§7Frog + small Slime + water patch for ambush behavior"
        ), true);
        return 1;
    }

    private static int createProtectScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);
        createPlatform(level, basePos, 15, 15);
        createFence(level, basePos, 15, 15);

        // Spawn adult cow
        Cow adultCow = EntityType.COW.create(level);
        if (adultCow != null) {
            adultCow.setPos(basePos.getX() + 7, basePos.getY() + 1, basePos.getZ() + 7);
            level.addFreshEntity(adultCow);
        }

        // Spawn baby cow
        Cow babyCow = EntityType.COW.create(level);
        if (babyCow != null) {
            babyCow.setPos(basePos.getX() + 8, basePos.getY() + 1, basePos.getZ() + 7);
            babyCow.setBaby(true);
            level.addFreshEntity(babyCow);
        }

        // Spawn wolf as threat
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 12, basePos.getY() + 1, basePos.getZ() + 7);
            AnimalNeeds.setHunger(wolf, 15);
            level.addFreshEntity(wolf);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated protect scenario at " + basePos.toShortString() +
            "\n§7Adult Cow + Baby Cow + Wolf to test protection behavior"
        ), true);
        return 1;
    }

    private static int createSchoolScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);

        // Create glass water tank
        createWaterTank(level, basePos, 12, 12, 5);

        // Spawn cod for schooling
        for (int i = 0; i < 10; i++) {
            Cod cod = EntityType.COD.create(level);
            if (cod != null) {
                double x = basePos.getX() + 3 + (i % 3) * 2;
                double y = basePos.getY() + 2;
                double z = basePos.getZ() + 3 + (i / 3) * 2;
                cod.setPos(x, y, z);
                level.addFreshEntity(cod);
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated school scenario at " + basePos.toShortString() +
            "\n§710 Cod in glass water tank for schooling behavior"
        ), true);
        return 1;
    }

    private static void createWaterTank(ServerLevel level, BlockPos pos, int width, int depth, int height) {
        // Create glass walls
        for (int y = 0; y <= height; y++) {
            for (int x = 0; x < width; x++) {
                level.setBlock(pos.offset(x, y, 0), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(pos.offset(x, y, depth - 1), Blocks.GLASS.defaultBlockState(), 3);
            }
            for (int z = 0; z < depth; z++) {
                level.setBlock(pos.offset(0, y, z), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(pos.offset(width - 1, y, z), Blocks.GLASS.defaultBlockState(), 3);
            }
        }

        // Fill with water
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < depth - 1; z++) {
                for (int y = 1; y < height; y++) {
                    level.setBlock(pos.offset(x, y, z), Blocks.WATER.defaultBlockState(), 3);
                }
            }
        }

        // Glass floor
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                level.setBlock(pos.offset(x, 0, z), Blocks.GLASS.defaultBlockState(), 3);
            }
        }
    }

    // ============ Pathfinding Test Scenarios ============

    /**
     * Creates a slope pathfinding test scenario with various slope angles.
     * Animals should prefer gentler slopes and navigate smoothly up/down.
     */
    private static int createSlopeScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);

        // Create flat base area
        createPlatform(level, basePos, 40, 20);

        // Create gentle slope (15 degrees) - 1 block up per 4 horizontal
        for (int x = 5; x < 15; x++) {
            int height = (x - 5) / 4;
            for (int z = 2; z < 8; z++) {
                level.setBlock(basePos.offset(x, height, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(basePos.offset(x, height - 1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        // Create steep slope (45 degrees) - 1 block up per 1 horizontal
        for (int x = 5; x < 12; x++) {
            int height = x - 5;
            for (int z = 12; z < 18; z++) {
                level.setBlock(basePos.offset(x, height, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(basePos.offset(x, height - 1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        // Create switchback slope (realistic mountain path)
        // First leg going right
        for (int x = 20; x < 30; x++) {
            int height = (x - 20) / 2;
            for (int z = 2; z < 5; z++) {
                level.setBlock(basePos.offset(x, height, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(basePos.offset(x, height - 1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        // Turn and go back left
        for (int x = 30; x > 20; x--) {
            int height = 5 + (30 - x) / 2;
            for (int z = 6; z < 9; z++) {
                level.setBlock(basePos.offset(x, height, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(basePos.offset(x, height - 1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        // Spawn cows to test pathfinding
        for (int i = 0; i < 3; i++) {
            Cow cow = EntityType.COW.create(level);
            if (cow != null) {
                cow.setPos(basePos.getX() + 2 + i * 15, basePos.getY() + 1, basePos.getZ() + 5 + (i * 5));
                level.addFreshEntity(cow);
            }
        }

        // Place hay bales at top of each slope to attract
        level.setBlock(basePos.offset(14, 3, 5), Blocks.HAY_BLOCK.defaultBlockState(), 3);
        level.setBlock(basePos.offset(11, 6, 15), Blocks.HAY_BLOCK.defaultBlockState(), 3);
        level.setBlock(basePos.offset(21, 10, 7), Blocks.HAY_BLOCK.defaultBlockState(), 3);

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated slope pathfinding scenario at " + basePos.toShortString() +
            "\n§7Gentle slope (15°), Steep slope (45°), Switchback path" +
            "\n§7Watch how cows navigate different slope types!"
        ), true);
        return 1;
    }

    /**
     * Creates a ridgeline avoidance test scenario.
     * Prey animals should avoid exposed ridgelines and prefer sheltered paths.
     */
    private static int createRidgelineScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);

        // Create large base area
        createPlatform(level, basePos, 30, 30);

        // Create ridgeline (exposed high ground) down the center
        for (int x = 5; x < 25; x++) {
            for (int z = 13; z < 17; z++) {
                // Build up to ridgeline height (y=6)
                for (int y = 0; y <= 5; y++) {
                    level.setBlock(basePos.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
                level.setBlock(basePos.offset(x, 6, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }

        // Create sheltered path (lower, with tree cover) on the side
        for (int x = 5; x < 25; x++) {
            for (int z = 24; z < 28; z++) {
                level.setBlock(basePos.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                // Add tree canopy every few blocks
                if (x % 4 == 0) {
                    level.setBlock(basePos.offset(x, 1, z), Blocks.OAK_LOG.defaultBlockState(), 3);
                    level.setBlock(basePos.offset(x, 2, z), Blocks.OAK_LOG.defaultBlockState(), 3);
                    level.setBlock(basePos.offset(x, 3, z), Blocks.OAK_LOG.defaultBlockState(), 3);
                    // Leaves
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            level.setBlock(basePos.offset(x + dx, 4, z + dz), Blocks.OAK_LEAVES.defaultBlockState(), 3);
                            level.setBlock(basePos.offset(x + dx, 5, z + dz), Blocks.OAK_LEAVES.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        // Spawn prey animals (sheep) - they should avoid the ridgeline
        for (int i = 0; i < 4; i++) {
            Sheep sheep = EntityType.SHEEP.create(level);
            if (sheep != null) {
                sheep.setPos(basePos.getX() + 3, basePos.getY() + 1, basePos.getZ() + 15 + i * 3);
                level.addFreshEntity(sheep);
            }
        }

        // Spawn a wolf to create pressure
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 2, basePos.getY() + 1, basePos.getZ() + 15);
            AnimalNeeds.setHunger(wolf, 15);
            level.addFreshEntity(wolf);
        }

        // Place hay at end of sheltered path
        level.setBlock(basePos.offset(26, 1, 26), Blocks.HAY_BLOCK.defaultBlockState(), 3);

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated ridgeline avoidance scenario at " + basePos.toShortString() +
            "\n§7Center: Exposed ridgeline (y=6)" +
            "\n§7Side: Sheltered path with tree cover" +
            "\n§7Sheep should prefer the sheltered path when fleeing!"
        ), true);
        return 1;
    }

    /**
     * Creates a comprehensive pathfinding showcase with all terrain types.
     * Demonstrates slope costs, momentum, smooth turning, and terrain preferences.
     */
    private static int createPathfindScenario(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());

        BlockPos basePos = playerPos.offset(5, 0, 0);

        // Create large arena
        createPlatform(level, basePos, 50, 50);
        createFence(level, basePos, 50, 50);

        // === Section 1: Rolling hills (gentle slopes in all directions) ===
        for (int x = 5; x < 20; x++) {
            for (int z = 5; z < 20; z++) {
                // Create sine-wave hills
                double hillHeight = 2 * Math.sin(x * 0.5) * Math.sin(z * 0.5) + 2;
                int height = (int) hillHeight;
                for (int y = 0; y <= height; y++) {
                    level.setBlock(basePos.offset(x, y, z),
                        y == height ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }

        // === Section 2: Cliff face with ledges ===
        for (int x = 25; x < 35; x++) {
            for (int z = 5; z < 15; z++) {
                // Main cliff
                for (int y = 0; y <= 8; y++) {
                    level.setBlock(basePos.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
                // Ledges every 2 blocks
                if (z % 4 < 2) {
                    level.setBlock(basePos.offset(x, 9, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                }
            }
        }

        // === Section 3: Water crossing with stepping stones ===
        for (int x = 5; x < 20; x++) {
            for (int z = 25; z < 35; z++) {
                level.setBlock(basePos.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3);
            }
        }
        // Stepping stones
        for (int i = 0; i < 7; i++) {
            int stoneX = 6 + i * 2;
            int stoneZ = 27 + (i % 2) * 3;
            level.setBlock(basePos.offset(stoneX, 1, stoneZ), Blocks.COBBLESTONE.defaultBlockState(), 3);
        }

        // === Section 4: Forest with winding path ===
        for (int x = 25; x < 45; x++) {
            for (int z = 25; z < 45; z++) {
                // Dense forest floor
                level.setBlock(basePos.offset(x, 0, z), Blocks.PODZOL.defaultBlockState(), 3);
                // Trees
                if ((x + z) % 5 == 0 && x > 26 && x < 44 && z > 26 && z < 44) {
                    level.setBlock(basePos.offset(x, 1, z), Blocks.OAK_LOG.defaultBlockState(), 3);
                    level.setBlock(basePos.offset(x, 2, z), Blocks.OAK_LOG.defaultBlockState(), 3);
                    level.setBlock(basePos.offset(x, 3, z), Blocks.OAK_LEAVES.defaultBlockState(), 3);
                }
            }
        }
        // Clear winding path through forest
        int pathZ = 30;
        for (int x = 25; x < 45; x++) {
            pathZ = 30 + (int)(3 * Math.sin(x * 0.3));
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(basePos.offset(x, 0, pathZ + dz), Blocks.DIRT_PATH.defaultBlockState(), 3);
                // Clear any trees above path
                level.setBlock(basePos.offset(x, 1, pathZ + dz), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(basePos.offset(x, 2, pathZ + dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // Spawn animals in different sections
        // Cows in rolling hills
        for (int i = 0; i < 3; i++) {
            Cow cow = EntityType.COW.create(level);
            if (cow != null) {
                cow.setPos(basePos.getX() + 8 + i * 4, basePos.getY() + 5, basePos.getZ() + 10);
                level.addFreshEntity(cow);
            }
        }

        // Sheep near water crossing
        for (int i = 0; i < 3; i++) {
            Sheep sheep = EntityType.SHEEP.create(level);
            if (sheep != null) {
                sheep.setPos(basePos.getX() + 3, basePos.getY() + 1, basePos.getZ() + 28 + i * 2);
                level.addFreshEntity(sheep);
            }
        }

        // Wolf in forest
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.setPos(basePos.getX() + 35, basePos.getY() + 1, basePos.getZ() + 35);
            level.addFreshEntity(wolf);
        }

        // Place hay bales as goals
        level.setBlock(basePos.offset(17, 4, 12), Blocks.HAY_BLOCK.defaultBlockState(), 3);
        level.setBlock(basePos.offset(18, 1, 38), Blocks.HAY_BLOCK.defaultBlockState(), 3);
        level.setBlock(basePos.offset(43, 1, 30), Blocks.HAY_BLOCK.defaultBlockState(), 3);

        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aCreated pathfinding showcase at " + basePos.toShortString() +
            "\n§b=== Terrain Sections ===" +
            "\n§7• NW: Rolling hills (momentum test)" +
            "\n§7• NE: Cliff with ledges (slope cost test)" +
            "\n§7• SW: Water with stepping stones" +
            "\n§7• SE: Forest with winding path" +
            "\n§eAnimals will use smooth navigation and terrain awareness!"
        ), true);
        return 1;
    }

}
