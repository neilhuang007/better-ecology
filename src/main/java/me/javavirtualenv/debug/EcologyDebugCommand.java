package me.javavirtualenv.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command handler for Better Ecology debug mode.
 * Usage: /ecology debug [true|false|global]
 */
public final class EcologyDebugCommand {

    private EcologyDebugCommand() {
    }

    /**
     * Registers the ecology debug command.
     *
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("ecology")
                .then(Commands.literal("debug")
                    // Toggle personal debug mode
                    .executes(EcologyDebugCommand::togglePersonal)
                    // Set personal debug mode explicitly
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(EcologyDebugCommand::setPersonal))
                    // Global debug mode sub-command
                    .then(Commands.literal("global")
                        .requires(source -> source.hasPermission(2)) // Op level 2
                        .executes(EcologyDebugCommand::toggleGlobal)
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(EcologyDebugCommand::setGlobal))))
        );
    }

    private static int togglePersonal(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        boolean nowEnabled = DebugModeManager.toggleForPlayer(player.getUUID());
        String message = nowEnabled
            ? "Debug mode enabled. Entity states will be shown above entities."
            : "Debug mode disabled.";

        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int setPersonal(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        if (enabled) {
            DebugModeManager.enableForPlayer(player.getUUID());
        } else {
            DebugModeManager.disableForPlayer(player.getUUID());
        }

        String message = enabled
            ? "Debug mode enabled. Entity states will be shown above entities."
            : "Debug mode disabled.";

        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int toggleGlobal(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean nowEnabled = DebugModeManager.toggleGlobal();

        String message = nowEnabled
            ? "Global debug mode enabled for all players."
            : "Global debug mode disabled.";

        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int setGlobal(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        if (enabled) {
            DebugModeManager.enableGlobal();
        } else {
            DebugModeManager.disableGlobal();
        }

        String message = enabled
            ? "Global debug mode enabled for all players."
            : "Global debug mode disabled.";

        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }
}
