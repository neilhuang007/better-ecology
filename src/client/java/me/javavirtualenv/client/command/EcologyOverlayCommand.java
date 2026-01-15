package me.javavirtualenv.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.javavirtualenv.client.hud.EcologyHudOverlay;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

/**
 * Client-side command for toggling the debug overlay.
 * Usage: /ecologyoverlay
 */
public class EcologyOverlayCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                 CommandBuildContext registryAccess) {
        dispatcher.register(ClientCommandManager.literal("ecologyoverlay")
            .executes(EcologyOverlayCommand::toggleOverlay)
        );
    }

    private static int toggleOverlay(CommandContext<FabricClientCommandSource> ctx) {
        EcologyHudOverlay.toggle();
        boolean enabled = EcologyHudOverlay.isEnabled();

        ctx.getSource().sendFeedback(Component.literal(
            enabled ? "§aEcology debug overlay enabled" : "§cEcology debug overlay disabled"
        ));
        return enabled ? 1 : 0;
    }
}
