package me.javavirtualenv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.WolfPackData;
import me.javavirtualenv.debug.DebugEcoCommand;
import me.javavirtualenv.network.EcologyPackets;
import me.javavirtualenv.spawning.EcologySpawnPredicate;
import me.javavirtualenv.spawning.SpawnRequirements;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;

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

		// Register ecological spawn predicates
		registerSpawnPredicates();

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

	/**
	 * Registers ecological spawn predicates for animals.
	 *
	 * <p>These predicates enforce:
	 * <ul>
	 *   <li>Water proximity requirements</li>
	 *   <li>Territorial spacing</li>
	 *   <li>Population density limits</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	private void registerSpawnPredicates() {
		// Herbivores that need water nearby
		new EcologySpawnPredicate<>(Cow.class, SpawnRequirements.LAND_ANIMAL_NEEDS_WATER)
			.register((EntityType<Cow>) EntityType.COW);
		new EcologySpawnPredicate<>(Sheep.class, SpawnRequirements.LAND_ANIMAL_NEEDS_WATER)
			.register((EntityType<Sheep>) EntityType.SHEEP);
		new EcologySpawnPredicate<>(Pig.class, SpawnRequirements.LAND_ANIMAL_NEEDS_WATER)
			.register((EntityType<Pig>) EntityType.PIG);

		// Predators with larger territories
		new EcologySpawnPredicate<>(Wolf.class, SpawnRequirements.PREDATOR)
			.register((EntityType<Wolf>) EntityType.WOLF);

		LOGGER.debug("Registered ecology spawn predicates");
	}
}
