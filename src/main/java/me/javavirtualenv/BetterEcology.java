package me.javavirtualenv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.WolfPackData;
import me.javavirtualenv.debug.DebugEcoCommand;
import me.javavirtualenv.network.EcologyPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Better Ecology - A Minecraft Fabric mod that implements scientifically-based animal behaviors.
 */
public class BetterEcology implements ModInitializer {
	public static final String MOD_ID = "better-ecology";

	// Logger for the mod
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Debug mode flag - set to true for additional logging
	public static boolean DEBUG_MODE = false;

	@Override
	public void onInitialize() {
		// Force loading of AnimalNeeds class to register attachments early
		// This ensures attachments are available for deserialization
		registerAttachments();

		// Register network packets
		registerNetworking();

		// Register commands
		registerCommands();

		LOGGER.info("Better Ecology initialized");
	}

	/**
	 * Registers all data attachments used by the mod.
	 * This must be called during mod initialization.
	 */
	private void registerAttachments() {
		// Simply access the static fields to trigger class loading and attachment registration
		LOGGER.debug("Registering hunger attachment: {}", AnimalNeeds.HUNGER_ATTACHMENT);
		LOGGER.debug("Registering thirst attachment: {}", AnimalNeeds.THIRST_ATTACHMENT);
		LOGGER.debug("Registering last_damage_tick attachment: {}", AnimalNeeds.LAST_DAMAGE_TICK_ATTACHMENT);
		LOGGER.debug("Registering initialized attachment: {}", AnimalNeeds.INITIALIZED_ATTACHMENT);
		LOGGER.debug("Registering wolf pack attachment: {}", WolfPackData.PACK_DATA_ATTACHMENT);
	}

	/**
	 * Registers all network packets used by the mod.
	 */
	private void registerNetworking() {
		EcologyPackets.register();
		LOGGER.debug("Registered ecology networking");
	}

	/**
	 * Registers all commands used by the mod.
	 */
	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			DebugEcoCommand.register(dispatcher);
			LOGGER.debug("Registered /debugeco command");
		});
	}
}
