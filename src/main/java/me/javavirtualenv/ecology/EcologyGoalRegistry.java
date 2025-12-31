package me.javavirtualenv.ecology;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public final class EcologyGoalRegistry {
	@FunctionalInterface
	public interface GoalFactory {
		Goal create(Mob mob, EcologyComponent component, EcologyProfile profile);
	}

	private static final Map<String, GoalFactory> FACTORIES = new HashMap<>();

	private EcologyGoalRegistry() {
	}

	public static void register(String id, GoalFactory factory) {
		FACTORIES.put(id, factory);
	}

	@Nullable
	public static GoalFactory get(String id) {
		return FACTORIES.get(id);
	}
}
