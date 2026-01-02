package me.javavirtualenv;

import me.javavirtualenv.debug.DebugModeManager;
import me.javavirtualenv.debug.DebugNametagUpdater;
import me.javavirtualenv.debug.EcologyDebugCommand;
import me.javavirtualenv.ecology.EcologyBootstrap;
import me.javavirtualenv.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterEcology implements ModInitializer {
	public static final String MOD_ID = "better-ecology";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		EcologyBootstrap.init();
		ModItems.register();

		// Register debug command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			EcologyDebugCommand.register(dispatcher)
		);

		// Initialize debug nametag updater
		DebugNametagUpdater.init();

		// Reset debug state on server shutdown
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> DebugModeManager.reset());

		LOGGER.info("Better Ecology initialized");
	}
}
