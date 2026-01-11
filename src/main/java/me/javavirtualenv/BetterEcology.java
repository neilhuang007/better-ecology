package me.javavirtualenv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.debug.BehaviorLogger.LogLevel;
import me.javavirtualenv.debug.DebugModeManager;
import me.javavirtualenv.debug.DebugNametagUpdater;
import me.javavirtualenv.debug.EcologyDebugCommand;
import me.javavirtualenv.debug.WolfDebugCommand;
import me.javavirtualenv.ecology.EcologyBootstrap;
import me.javavirtualenv.ecology.conservation.PreyPopulationManager;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

public class BetterEcology implements ModInitializer {
	public static final String MOD_ID = "better-ecology";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Debug mode flag - set to true for additional logging
	public static boolean DEBUG_MODE = false;

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
					WolfDebugCommand.register(dispatcher);
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

		DevDebugHarness.register();

		LOGGER.info("Better Ecology initialized");
	}

	private static final class DevDebugHarness {
		private static final int SPAWN_DELAY_TICKS = 80;
		private static final int LOG_INTERVAL_TICKS = 40;
		private static final int MAX_CHECKS = 10;
		private static final int CHICKEN_COUNT = 32;
		private static final int PIG_COUNT = 16;
		private static final int WOLF_COUNT = 3;
		private static final int SPAWN_SPACING = 2;
		private static final int POPULATION_RADIUS = 32;

		private static int ticksSinceStart;
		private static int logChecks;
		private static boolean active;
		private static boolean spawned;
		private static BlockPos spawnOrigin;
		private static final List<Wolf> spawnedWolves = new ArrayList<>();

		private DevDebugHarness() {
		}

		private static void register() {
			if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
				return;
			}

			ServerLifecycleEvents.SERVER_STARTED.register(server -> {
				DebugModeManager.enableGlobal();
				BehaviorLogger.setLogLevel(LogLevel.MINIMAL);
				resetState(server);
			});

			ServerTickEvents.END_SERVER_TICK.register(server -> {
				if (!active) {
					return;
				}
				handleTick(server);
			});
		}

		private static void resetState(net.minecraft.server.MinecraftServer server) {
			active = true;
			spawned = false;
			ticksSinceStart = 0;
			logChecks = 0;
			spawnedWolves.clear();
			spawnOrigin = getSpawnPos(server);
			BetterEcology.LOGGER.info("Dev harness armed at spawn {}", spawnOrigin);
		}

		private static BlockPos getSpawnPos(net.minecraft.server.MinecraftServer server) {
			ServerLevel level = server.getLevel(Level.OVERWORLD);
			if (level == null) {
				return BlockPos.ZERO;
			}
			return level.getSharedSpawnPos();
		}

		private static void handleTick(net.minecraft.server.MinecraftServer server) {
			ServerLevel level = server.getLevel(Level.OVERWORLD);
			if (level == null) {
				active = false;
				return;
			}

			ticksSinceStart++;

			if (!spawned && ticksSinceStart >= SPAWN_DELAY_TICKS) {
				performSpawn(level);
				spawned = true;
				return;
			}

			if (!spawned) {
				return;
			}

			if (ticksSinceStart % LOG_INTERVAL_TICKS != 0) {
				return;
			}

			if (logChecks >= MAX_CHECKS) {
				active = false;
				return;
			}

			logChecks++;
			logTargets();
			logPopulationRatios();
		}

		private static void performSpawn(ServerLevel level) {
			BlockPos origin = spawnOrigin == null ? level.getSharedSpawnPos() : spawnOrigin;
			int wolves = spawnEntities(EntityType.WOLF, level, origin, WOLF_COUNT);
			int chickens = spawnEntities(EntityType.CHICKEN, level, origin.offset(8, 0, 0), CHICKEN_COUNT);
			int pigs = spawnEntities(EntityType.PIG, level, origin.offset(-8, 0, 0), PIG_COUNT);
			BetterEcology.LOGGER.info("Dev harness spawned wolves={}, chickens={}, pigs={} near {}", wolves, chickens, pigs, origin);
		}

		private static int spawnEntities(EntityType<? extends Mob> type, ServerLevel level, BlockPos origin, int count) {
			int spawnedCount = 0;
			int gridSize = (int) Math.ceil(Math.sqrt(count));

			for (int i = 0; i < count; i++) {
				int row = i / gridSize;
				int col = i % gridSize;
				BlockPos pos = origin.offset(col * SPAWN_SPACING, 0, row * SPAWN_SPACING);
				Mob mob = type.create(level);
				if (mob == null) {
					continue;
				}
				mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
				DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
				mob.finalizeSpawn(level, difficulty, MobSpawnType.EVENT, (SpawnGroupData) null);
				if (level.addFreshEntity(mob)) {
					spawnedCount++;
					if (mob instanceof Wolf wolf) {
						spawnedWolves.add(wolf);
					}
				}
			}

			return spawnedCount;
		}

		private static void logTargets() {
			for (Wolf wolf : spawnedWolves) {
				if (!wolf.isAlive()) {
					continue;
				}
				String targetType = wolf.getTarget() == null ? "none" : wolf.getTarget().getType().toShortString();
				BetterEcology.LOGGER.info("Dev harness wolf@{} target={} pos=({}, {}, {})",
					wolf.getId(), targetType, format(wolf.getX()), format(wolf.getY()), format(wolf.getZ()));
			}
		}

		private static void logPopulationRatios() {
			Wolf sampleWolf = spawnedWolves.stream().filter(Wolf::isAlive).findFirst().orElse(null);
			if (sampleWolf == null) {
				BetterEcology.LOGGER.info("Dev harness no live wolves for population check");
				return;
			}
			double chickenRatio = PreyPopulationManager.getPopulationRatio(sampleWolf, Chicken.class, POPULATION_RADIUS);
			double pigRatio = PreyPopulationManager.getPopulationRatio(sampleWolf, Pig.class, POPULATION_RADIUS);
			BetterEcology.LOGGER.info("Dev harness prey ratios: chicken={}, pig={}", format(chickenRatio), format(pigRatio));
		}

		private static String format(double value) {
			return String.format("%.2f", value);
		}
	}
}
