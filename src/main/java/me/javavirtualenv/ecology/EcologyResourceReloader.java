package me.javavirtualenv.ecology;

import java.util.List;
import me.javavirtualenv.BetterEcology;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

public final class EcologyResourceReloader implements ResourceManagerReloadListener {
	private final EcologyProfileLoader loader;
	private final Logger logger;

	public EcologyResourceReloader(Logger logger) {
		this.logger = logger;
		this.loader = new EcologyProfileLoader(logger);
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		List<EcologyProfile> profiles = loader.loadAll(manager);
		EcologyProfileRegistry.reload(profiles);
		logger.info("Better Ecology: loaded {} profiles", profiles.size());
	}
}
