package me.javavirtualenv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.javavirtualenv.debug.DebugModeManager;
import me.javavirtualenv.debug.DebugNametagUpdater;
import me.javavirtualenv.debug.EcologyDebugCommand;
import me.javavirtualenv.ecology.EcologyBootstrap;
import me.javavirtualenv.ecology.seasonal.SeasonCommand;
import me.javavirtualenv.ecology.seasonal.SeasonManager;
import me.javavirtualenv.ecology.seasonal.SeasonSavedData;
import me.javavirtualenv.ecology.seasonal.WinterSiegeScheduler;
import me.javavirtualenv.ecology.seasonal.WolfSiegeCommand;
import me.javavirtualenv.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

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

		// Register debug and season commands
		CommandRegistrationCallback.EVENT
				.register((dispatcher, registryAccess, environment) -> {
					EcologyDebugCommand.register(dispatcher);
					SeasonCommand.register(dispatcher);
					WolfSiegeCommand.register(dispatcher);
				});

		// Initialize debug nametag updater
		DebugNametagUpdater.init();

		// Reset debug state on server shutdown
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> DebugModeManager.reset());

		// Register winter siege scheduler
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getLevel(
					net.minecraft.world.level.Level.OVERWORLD) instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				WinterSiegeScheduler.updateSieges(serverLevel);
			}
		});

		// Load season overrides on server start
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (server.getLevel(
					net.minecraft.world.level.Level.OVERWORLD) instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				SeasonSavedData data = SeasonSavedData.getOrCreate(serverLevel);
				for (var entry : data.getAllOverrides().entrySet()) {
					SeasonManager.setSeason(entry.getKey(), entry.getValue());
				}
			}
		});

		LOGGER.info("Better Ecology initialized");
	}
}
