package me.javavirtualenv;

import me.javavirtualenv.client.debug.DebugKeyHandler;
import me.javavirtualenv.client.debug.EcologyDebugRenderer;
import me.javavirtualenv.client.pig.PigRenderFeature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;

public class BetterEcologyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		registerEntityRenderers();
		registerDebugSystem();
	}

	private void registerEntityRenderers() {
		EntityRendererRegistry.register(EntityType.PIG, (context) -> {
			PigRenderer renderer = new PigRenderer(context);
			// Note: Render layer registration may need to be done differently in future versions
			// For now, we keep the feature class for potential use
			return renderer;
		});
	}

	private void registerDebugSystem() {
		DebugKeyHandler.register();
		EcologyDebugRenderer.register();
	}
}
