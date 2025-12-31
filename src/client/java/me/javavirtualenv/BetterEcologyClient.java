package me.javavirtualenv;

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
	}

	private void registerEntityRenderers() {
		EntityRendererRegistry.register(EntityType.PIG, (context) -> {
			PigRenderer renderer = new PigRenderer(context);
			renderer.addLayer(new PigRenderFeature(renderer));
			return renderer;
		});
	}
}
