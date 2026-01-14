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

/**
 * Debug command for testing Better Ecology behaviors.
 * Usage: /debugeco [scenario]
 * Scenarios:
 * - predator: Creates wolf hunting scenario with prey animals
 * - food: Creates wolf with dropped meat items
 * - thirst: Creates animals near water to test drinking
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
}
