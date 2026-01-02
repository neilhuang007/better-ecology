package me.javavirtualenv.ecology.seasonal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

/**
 * Command handler for forcing wolf siege mode.
 * Usage: /ecology siege start [siege_type] - Start siege for nearby wolves
 * /ecology siege stop - Stop siege for nearby wolves
 */
public final class WolfSiegeCommand {

    private WolfSiegeCommand() {
    }

    /**
     * Registers the wolf siege command.
     *
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ecology")
                        .then(Commands.literal("siege")
                                // Start siege command
                                .then(Commands.literal("start")
                                        .requires(source -> source.hasPermission(2)) // Op level 2
                                        .executes(WolfSiegeCommand::startSiegeDefault) // Default livestock raid
                                        .then(Commands.argument("siege_type", StringArgumentType.word())
                                                .suggests((context, builder) -> builder.suggest("livestock")
                                                        .suggest("full")
                                                        .buildFuture())
                                                .executes(WolfSiegeCommand::startSiege)))
                                // Stop siege command
                                .then(Commands.literal("stop")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(WolfSiegeCommand::stopSiege))));
    }

    /**
     * Starts siege with default livestock raid type.
     */
    private static int startSiegeDefault(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        return startSiegeInternal(source, player, WinterSiegeScheduler.SiegeType.LIVESTOCK_RAID);
    }

    /**
     * Starts siege with specified type.
     */
    private static int startSiege(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        String typeString = StringArgumentType.getString(context, "siege_type");
        WinterSiegeScheduler.SiegeType siegeType;

        switch (typeString.toLowerCase()) {
            case "full":
                siegeType = WinterSiegeScheduler.SiegeType.FULL_ASSAULT;
                break;
            case "livestock":
            default:
                siegeType = WinterSiegeScheduler.SiegeType.LIVESTOCK_RAID;
                break;
        }

        return startSiegeInternal(source, player, siegeType);
    }

    /**
     * Internal method to start siege for nearby wolves.
     */
    private static int startSiegeInternal(CommandSourceStack source, ServerPlayer player,
            WinterSiegeScheduler.SiegeType siegeType) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("Command must be run on server level"));
            return 0;
        }

        BlockPos playerPos = player.blockPosition();

        // Find all wolves in 100x100 area (inflate by 50 in each direction)
        AABB searchArea = new AABB(playerPos).inflate(50.0);
        var wolves = serverLevel.getEntitiesOfClass(Wolf.class, searchArea);

        int affectedCount = 0;
        long startTime = serverLevel.getGameTime();

        for (Wolf wolf : wolves) {
            // Skip tamed wolves
            if (wolf.isTame()) {
                continue;
            }

            // Apply siege effects
            applySiegeEffects(wolf, siegeType, startTime);

            // Set target based on siege type
            setSiegeTarget(wolf, serverLevel, siegeType);

            affectedCount++;
        }

        if (affectedCount == 0) {
            source.sendFailure(Component.literal("No wild wolves found in range"));
            return 0;
        }

        String typeName = siegeType == WinterSiegeScheduler.SiegeType.FULL_ASSAULT ? "full assault" : "livestock raid";
        String message = String.format("Started %s siege mode for %d wolf(es) within 100x100 blocks", typeName,
                affectedCount);
        source.sendSuccess(() -> Component.literal(message), true);

        return affectedCount;
    }

    /**
     * Stops siege for nearby wolves.
     */
    private static int stopSiege(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("Command must be run on server level"));
            return 0;
        }

        BlockPos playerPos = player.blockPosition();

        // Find all wolves in 100x100 area
        AABB searchArea = new AABB(playerPos).inflate(50.0);
        var wolves = serverLevel.getEntitiesOfClass(Wolf.class, searchArea);

        int affectedCount = 0;

        for (Wolf wolf : wolves) {
            // Remove siege effects
            if (wolf instanceof EcologyAccess access) {
                EcologyComponent component = access.betterEcology$getEcologyComponent();
                if (component != null) {
                    var tag = component.getHandleTag("siege");
                    tag.putBoolean("is_sieging", false);
                }
            }

            // Clear aggressive state
            wolf.setAggressive(false);

            // Clear target
            wolf.setTarget(null);

            affectedCount++;
        }

        if (affectedCount == 0) {
            source.sendFailure(Component.literal("No wolves found in range"));
            return 0;
        }

        String message = String.format("Stopped siege mode for %d wolf(es) within 100x100 blocks", affectedCount);
        source.sendSuccess(() -> Component.literal(message), true);

        return affectedCount;
    }

    /**
     * Applies siege effects to a wolf.
     */
    private static void applySiegeEffects(Wolf wolf, WinterSiegeScheduler.SiegeType siegeType, long startTime) {
        if (!(wolf instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        var tag = component.getHandleTag("siege");
        tag.putBoolean("is_sieging", true);
        tag.putLong("siege_start_time", startTime);
        tag.putString("siege_type", siegeType.name());
        tag.putBoolean("is_blizzard", false); // Manual siege, not weather-triggered
        tag.putBoolean("is_night", false); // Manual siege, not time-triggered
        tag.putDouble("speed_boost", 0.0);
    }

    /**
     * Sets an appropriate target for the siege.
     */
    private static void setSiegeTarget(Wolf wolf, ServerLevel serverLevel,
            WinterSiegeScheduler.SiegeType siegeType) {
        BlockPos wolfPos = wolf.blockPosition();

        // Search radius for targets
        AABB searchArea = new AABB(wolfPos).inflate(64.0);
        LivingEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        if (siegeType == WinterSiegeScheduler.SiegeType.LIVESTOCK_RAID) {
            // Target livestock: sheep, cows, pigs, chickens
            var livestock = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.animal.Sheep.class, searchArea);
            bestTarget = findClosestEntity(wolf, livestock);

            if (bestTarget == null) {
                var pigs = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, searchArea);
                bestTarget = findClosestEntity(wolf, pigs);
            }
            if (bestTarget == null) {
                var cows = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class, searchArea);
                bestTarget = findClosestEntity(wolf, cows);
            }
            if (bestTarget == null) {
                var chickens = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.animal.Chicken.class,
                        searchArea);
                bestTarget = findClosestEntity(wolf, chickens);
            }
        } else {
            // Full assault: target livestock, villagers, or golems
            var villagers = serverLevel.getEntitiesOfClass(Villager.class, searchArea);
            bestTarget = findClosestEntity(wolf, villagers);

            if (bestTarget == null) {
                var golems = serverLevel.getEntitiesOfClass(IronGolem.class, searchArea);
                bestTarget = findClosestEntity(wolf, golems);
            }

            if (bestTarget == null) {
                var livestock = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.animal.Sheep.class,
                        searchArea);
                bestTarget = findClosestEntity(wolf, livestock);
            }
        }

        // Set the target if found
        if (bestTarget != null) {
            wolf.setTarget(bestTarget);
        }
    }

    /**
     * Finds the closest entity to the wolf.
     */
    private static <T extends LivingEntity> T findClosestEntity(Wolf wolf, Iterable<T> entities) {
        T closest = null;
        double closestDist = Double.MAX_VALUE;

        for (T entity : entities) {
            if (!entity.isAlive()) {
                continue;
            }

            // Don't target tamed animals
            if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable && tameable.isTame()) {
                continue;
            }

            double dist = wolf.distanceToSqr(entity);
            if (dist < closestDist) {
                closest = entity;
                closestDist = dist;
            }
        }

        return closest;
    }
}
