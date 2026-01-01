package me.javavirtualenv.ecology.handles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.RefugeAwareTargetGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public final class PredationHandle implements EcologyHandle {
	private static final String CACHE_KEY = "better-ecology:predation-cache";
	private static final int FLEE_PRIORITY = 2;
	private static final int HUNT_PRIORITY = 12;

	@Override
	public String id() {
		return "predation";
	}

	@Override
	public boolean supports(EcologyProfile profile) {
		PredationCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
		return cache.asPredator || cache.asPrey;
	}

	@Override
	public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
		PredationCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));

		if (cache.asPrey) {
			registerPreyGoals(mob, cache);
		}

		if (cache.asPredator) {
			registerPredatorGoals(mob, cache);
		}
	}

	private PredationCache buildCache(EcologyProfile profile) {
		boolean asPrey = profile.getBool("predation.as_prey.enabled", false);
		boolean asPredator = profile.getBool("predation.as_predator.enabled", false);

		List<EntityTarget> predatorTargets = buildEntityTargets(profile, "predation.predators");
		List<EntityTarget> preyTargets = buildEntityTargets(profile, "predation.prey");

		double fleeDistance = profile.getDouble("predation.flee_distance", 10.0);
		double detectionRange = profile.getDouble("predation.detection_range", 16.0);

		return new PredationCache(asPrey, asPredator, predatorTargets, preyTargets, fleeDistance, detectionRange);
	}

	private List<EntityTarget> buildEntityTargets(EcologyProfile profile, String path) {
		List<String> targetIds = profile.getStringList(path);
		List<EntityTarget> targets = new ArrayList<>();

		for (String targetId : targetIds) {
			if (targetId == null || targetId.isBlank()) {
				continue;
			}

			if (targetId.startsWith("#")) {
				ResourceLocation tagId = ResourceLocation.tryParse(targetId.substring(1));
				if (tagId != null) {
					TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
					targets.add(new EntityTarget(tag));
				}
			} else {
				ResourceLocation entityId = ResourceLocation.tryParse(targetId);
				if (entityId != null) {
					BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).ifPresent(entityType -> {
						Class<?> entityClass = entityType.getBaseClass();
						if (LivingEntity.class.isAssignableFrom(entityClass)) {
							@SuppressWarnings("unchecked")
							Class<? extends LivingEntity> livingClass = (Class<? extends LivingEntity>) entityClass;
							targets.add(new EntityTarget(livingClass));
						}
					});
				}
			}
		}

		return targets;
	}

	private void registerPreyGoals(Mob mob, PredationCache cache) {
		if (!(mob instanceof PathfinderMob pathfinderMob)) {
			return;
		}

		MobAccessor accessor = (MobAccessor) mob;

		for (EntityTarget target : cache.predatorTargets) {
			if (target.isTag()) {
				registerFleeFromTag(pathfinderMob, accessor, target.getTag(), cache);
			} else {
				registerFleeFromClass(pathfinderMob, accessor, target.getEntityClass(), cache);
			}
		}
	}

	private void registerFleeFromClass(PathfinderMob mob, MobAccessor accessor, Class<? extends LivingEntity> entityClass, PredationCache cache) {
		accessor.betterEcology$getGoalSelector().addGoal(FLEE_PRIORITY,
			new AvoidEntityGoal<>(mob, entityClass, (float) cache.fleeDistance, 1.0, 1.5));
	}

	private void registerFleeFromTag(PathfinderMob mob, MobAccessor accessor, TagKey<EntityType<?>> tagId, PredationCache cache) {
		Predicate<LivingEntity> tagPredicate = (livingEntity) -> {
			if (!(livingEntity instanceof Mob targetMob)) {
				return false;
			}
			return targetMob.getType().is(tagId);
		};

		Predicate<LivingEntity> combinedPredicate = tagPredicate.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR);

		accessor.betterEcology$getGoalSelector().addGoal(FLEE_PRIORITY,
			new AvoidEntityGoal<>(mob, LivingEntity.class, tagPredicate, (float) cache.fleeDistance, 1.0, 1.5,
				combinedPredicate));
	}

	private void registerPredatorGoals(Mob mob, PredationCache cache) {
		MobAccessor accessor = (MobAccessor) mob;

		for (EntityTarget target : cache.preyTargets) {
			if (target.isTag()) {
				registerHuntTag(mob, accessor, target.getTag(), cache);
			} else {
				registerHuntClass(mob, accessor, target.getEntityClass(), cache);
			}
		}
	}

	private void registerHuntClass(Mob mob, MobAccessor accessor, Class<? extends LivingEntity> entityClass, PredationCache cache) {
		accessor.betterEcology$getTargetSelector().addGoal(HUNT_PRIORITY,
			new RefugeAwareTargetGoal(mob, LivingEntity.class, 0, false, false,
				entity -> entityClass.isInstance(entity)));
	}

	private void registerHuntTag(Mob mob, MobAccessor accessor, TagKey<EntityType<?>> tagId, PredationCache cache) {
		Predicate<LivingEntity> predicate = (livingEntity) -> {
			if (!(livingEntity instanceof Mob targetMob)) {
				return false;
			}
			return targetMob.getType().is(tagId);
		};

		accessor.betterEcology$getTargetSelector().addGoal(HUNT_PRIORITY,
			new RefugeAwareTargetGoal(mob, LivingEntity.class, 0, false, false, predicate));
	}

	private static final class PredationCache {
		private final boolean asPrey;
		private final boolean asPredator;
		private final List<EntityTarget> predatorTargets;
		private final List<EntityTarget> preyTargets;
		private final double fleeDistance;
		private final double detectionRange;

		private PredationCache(boolean asPrey, boolean asPredator, List<EntityTarget> predatorTargets,
			List<EntityTarget> preyTargets, double fleeDistance, double detectionRange) {
			this.asPrey = asPrey;
			this.asPredator = asPredator;
			this.predatorTargets = predatorTargets;
			this.preyTargets = preyTargets;
			this.fleeDistance = fleeDistance;
			this.detectionRange = detectionRange;
		}
	}

	private static final class EntityTarget {
		private final Class<? extends LivingEntity> entityClass;
		private final TagKey<EntityType<?>> tag;
		private final boolean isTag;

		private EntityTarget(Class<? extends LivingEntity> entityClass) {
			this.entityClass = entityClass;
			this.tag = null;
			this.isTag = false;
		}

		private EntityTarget(TagKey<EntityType<?>> tag) {
			this.entityClass = null;
			this.tag = tag;
			this.isTag = true;
		}

		private boolean isTag() {
			return isTag;
		}

		private Class<? extends LivingEntity> getEntityClass() {
			return entityClass;
		}

		private TagKey<EntityType<?>> getTag() {
			return tag;
		}
	}
}
