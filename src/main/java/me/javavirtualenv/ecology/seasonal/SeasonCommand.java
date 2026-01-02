package me.javavirtualenv.ecology.seasonal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Command handler for Better Ecology season control.
 * 
 * Usage:
 * - /ecology season - Display current season
 * - /ecology season set <spring|summer|autumn|winter> - Set season override
 * - /ecology season auto - Enable auto mode
 * - /ecology season skip [days] - Skip game days
 */
public final class SeasonCommand {

    private SeasonCommand() {
    }

    /**
     * Registers the ecology season command.
     *
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ecology")
                        .then(Commands.literal("season")
                                // Display current season
                                .executes(SeasonCommand::displaySeason)
                                // Set season subcommand
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2)) // Op level 2
                                        .then(Commands.literal("spring")
                                                .executes(context -> setSeason(context, SeasonalContext.Season.SPRING)))
                                        .then(Commands.literal("summer")
                                                .executes(context -> setSeason(context, SeasonalContext.Season.SUMMER)))
                                        .then(Commands.literal("autumn")
                                                .executes(context -> setSeason(context, SeasonalContext.Season.AUTUMN)))
                                        .then(Commands.literal("winter")
                                                .executes(
                                                        context -> setSeason(context, SeasonalContext.Season.WINTER))))
                                // Enable auto mode
                                .then(Commands.literal("auto")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(SeasonCommand::setAutoMode))
                                // Skip days
                                .then(Commands.literal("skip")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(SeasonCommand::skipDays)
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 100))
                                                .executes(context -> skipDays(context,
                                                        IntegerArgumentType.getInteger(context, "days")))))));
    }

    /**
     * Displays the current season.
     */
    private static int displaySeason(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        SeasonalContext.Season currentSeason = SeasonManager.getCurrentSeason(level);
        SeasonalContext.TimePeriod timePeriod = SeasonalContext.getTimePeriod(level);
        boolean isOverride = SeasonManager.hasOverride(level);

        String modeText = isOverride ? "§6[Override]" : "§a[Auto]";

        Component message = Component.literal("§e=== Season Info ===\n")
                .append(Component.literal("§fSeason: §b" + currentSeason + " " + modeText + "\n"))
                .append(Component.literal("§fTime: §b" + timePeriod + "\n"))
                .append(Component.literal("§fActivity Multiplier: §b"
                        + SeasonalContext.getSeasonalActivityMultiplier(currentSeason) + "\n"))
                .append(Component.literal("§fBreeding Multiplier: §b"
                        + SeasonalContext.getSeasonalBreedingMultiplier(currentSeason) + "\n"))
                .append(Component.literal("§fMovement Multiplier: §b"
                        + SeasonalContext.getSeasonalMovementMultiplier(currentSeason) + "\n"))
                .append(Component.literal(
                        "§fHunger Multiplier: §b" + SeasonalContext.getSeasonalHungerMultiplier(currentSeason)));

        source.sendSuccess(() -> message, false);
        return 1;
    }

    /**
     * Sets a season override.
     */
    private static int setSeason(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            SeasonalContext.Season season) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        SeasonManager.setSeason(level, season);

        // Save to persistent storage
        SeasonSavedData.getOrCreate(level).setSeasonOverride(level.dimension().location().toString(), season);

        String message = String.format("§aSeason set to §e%s§a. Auto mode disabled.", season);
        source.sendSuccess(() -> Component.literal(message), true);

        return 1;
    }

    /**
     * Enables auto mode.
     */
    private static int setAutoMode(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        SeasonManager.setAutoMode(level);

        // Clear override from persistent storage
        SeasonSavedData.getOrCreate(level).setSeasonOverride(level.dimension().location().toString(), null);

        source.sendSuccess(() -> Component.literal("§aAuto mode enabled. Season will change based on game time."),
                true);

        return 1;
    }

    /**
     * Skips a default number of days (1 day).
     */
    private static int skipDays(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        return skipDays(context, 1);
    }

    /**
     * Skips a specified number of days.
     */
    private static int skipDays(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int days) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        long ticksToSkip = days * 24000L;
        level.setDayTime(level.getDayTime() + ticksToSkip);

        SeasonalContext.Season newSeason = SeasonalContext.getSeason(level);

        String message = String.format("§aSkipped §e%d§a day(s). New season: §e%s", days, newSeason);
        source.sendSuccess(() -> Component.literal(message), true);

        return 1;
    }
}
