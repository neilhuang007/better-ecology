package me.javavirtualenv.ecology;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.MilkProductionHandle;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingHandle;
import me.javavirtualenv.ecology.spawning.BiomeSpawnModifier;
import me.javavirtualenv.ecology.spawning.SpawnBootstrap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class EcologyBootstrap {
	private static boolean initialized = false;

	private EcologyBootstrap() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		EcologyHandleRegistry.reset();

		// Core physical handlers
		EcologyHandleRegistry.register(new MovementHandle());
		EcologyHandleRegistry.register(new SizeHandle());
		EcologyHandleRegistry.register(new HealthHandle());

		// Internal state handlers
		EcologyHandleRegistry.register(new HungerHandle());
		EcologyHandleRegistry.register(new ThirstHandle());
		EcologyHandleRegistry.register(new ConditionHandle());
		EcologyHandleRegistry.register(new EnergyHandle());
		EcologyHandleRegistry.register(new AgeHandle());
		EcologyHandleRegistry.register(new SocialHandle());

		// Behavioral handlers
		EcologyHandleRegistry.register(new TemporalHandle());
		EcologyHandleRegistry.register(new DietHandle());
		EcologyHandleRegistry.register(new BreedingHandle());
		EcologyHandleRegistry.register(new PredationHandle());
		EcologyHandleRegistry.register(new BehaviorHandle());
		EcologyHandleRegistry.register(new me.javavirtualenv.ecology.handles.social.InteractionHandle());

		// Parent-child relationship tracking
		EcologyHandleRegistry.register(new ParentChildHandle());

		// Reproduction handlers
		EcologyHandleRegistry.register(new NestBuildingHandle());

		// Production handlers
		EcologyHandleRegistry.register(new MilkProductionHandle());
		EcologyHandleRegistry.register(new ResourceProductionHandle());

		// Villager-specific handlers
		EcologyHandleRegistry.register(new VillagerBehaviorHandle());

		EcologyResourceReloader reloader = new EcologyResourceReloader(BetterEcology.LOGGER);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			BiomeSpawnModifier.clearAll();
			reloader.onResourceManagerReload(resourceManager);
			BiomeSpawnModifier.registerAll();
		});
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			reloader.onResourceManagerReload(server.getResourceManager());
			BiomeSpawnModifier.registerAll();
		});

		// Initialize spawn density tracker
		SpawnBootstrap.init();
	}
}
