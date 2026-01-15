package me.javavirtualenv;

import me.javavirtualenv.client.command.EcologyOverlayCommand;
import me.javavirtualenv.client.hud.EcologyHudOverlay;
import me.javavirtualenv.client.network.ClientEcologyPacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * Client-side initialization for Better Ecology mod.
 */
public class BetterEcologyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register client-side packet handlers
		ClientEcologyPacketHandler.register();

		// Register HUD overlay
		EcologyHudOverlay overlay = new EcologyHudOverlay();
		HudRenderCallback.EVENT.register(overlay);

		// Register client commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			EcologyOverlayCommand.register(dispatcher, registryAccess);
		});

		BetterEcology.LOGGER.info("Better Ecology client initialized");
	}
}
