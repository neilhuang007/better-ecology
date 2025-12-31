package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.*;
import me.javavirtualenv.behavior.ai.SteeringBehaviorGoal;
import me.javavirtualenv.behavior.armadillo.ArmadilloPredatorAvoidance;
import me.javavirtualenv.behavior.aquatic.*;
import me.javavirtualenv.behavior.bee.*;
import me.javavirtualenv.behavior.crepuscular.CrepuscularConfig;
import me.javavirtualenv.behavior.crepuscular.RoostingBehavior;
import me.javavirtualenv.behavior.flocking.AlignmentBehavior;
import me.javavirtualenv.behavior.flocking.CohesionBehavior;
import me.javavirtualenv.behavior.flocking.SeparationBehavior;
import me.javavirtualenv.behavior.fleeing.*;
import me.javavirtualenv.behavior.herd.HerdCohesion;
import me.javavirtualenv.behavior.herd.HerdConfig;
import me.javavirtualenv.behavior.herd.LeaderFollowing;
import me.javavirtualenv.behavior.parent.FollowMotherBehavior;
import me.javavirtualenv.behavior.parent.HidingBehavior;
import me.javavirtualenv.behavior.parent.MotherProtectionBehavior;
import me.javavirtualenv.behavior.parent.SeparationDistressBehavior;
import me.javavirtualenv.behavior.predation.AttractionBehavior;
import me.javavirtualenv.behavior.predation.AvoidanceBehavior;
import me.javavirtualenv.behavior.predation.EvasionBehavior;
import me.javavirtualenv.behavior.predation.PursuitBehavior;
import me.javavirtualenv.behavior.sniffer.DiggingBehavior;
import me.javavirtualenv.behavior.sniffer.SniffingBehavior;
import me.javavirtualenv.behavior.sniffer.SnifferSocialBehavior;
import me.javavirtualenv.behavior.territorial.HomeRange;
import me.javavirtualenv.behavior.territorial.HomeRangeBehavior;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Handle for managing steering behaviors in entities.
 * <p>
 * This handle registers and manages all steering behaviors from the behavior package,
 * applying steering forces to entity movement based on configuration.
 */
public final class BehaviorHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:behavior-cache";

    @Override
    public String id() {
        return "behavior";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        BehaviorCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled();
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        BehaviorCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        // Create a per-entity registry to avoid shared state issues
        ResourceLocation mobId = profile.id();
        BehaviorRegistry perEntityRegistry = createRegistry(mob, mobId);

        // Register the steering behavior goal with isolated registry
        int priority = cache.priority();
        SteeringBehaviorGoal goal = new SteeringBehaviorGoal(
            mob,
            () -> perEntityRegistry,
            () -> cache.weights(),
            cache.maxForce(),
            cache.maxSpeed()
        );
        MobAccessor accessor = (MobAccessor) mob;
        accessor.betterEcology$getGoalSelector().addGoal(priority, goal);
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Steering is applied via the goal, not here
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // NBT data is automatically loaded via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private BehaviorCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBool("behaviors.enabled", false);

        if (!enabled) {
            return null;
        }

        int priority = profile.getInt("behaviors.priority", 0);
        double maxForce = profile.getDouble("behaviors.max_force", 0.15);
        double maxSpeed = profile.getDouble("behaviors.max_speed", 1.0);

        // Build weights from config
        BehaviorWeights weights = buildWeights(profile);

        return new BehaviorCache(enabled, priority, maxForce, maxSpeed, weights);
    }

    private BehaviorWeights buildWeights(EcologyProfile profile) {
        BehaviorWeights weights = new BehaviorWeights();

        // Flocking
        weights.setSeparation(profile.getDouble("behaviors.separation", weights.getSeparation()));
        weights.setAlignment(profile.getDouble("behaviors.alignment", weights.getAlignment()));
        weights.setCohesion(profile.getDouble("behaviors.cohesion", weights.getCohesion()));

        // Predation
        weights.setPursuit(profile.getDouble("behaviors.pursuit", weights.getPursuit()));
        weights.setEvasion(profile.getDouble("behaviors.evasion", weights.getEvasion()));
        weights.setTerritorialDefense(profile.getDouble("behaviors.avoidance", weights.getTerritorialDefense()));
        weights.setFoodSeek(profile.getDouble("behaviors.attraction", weights.getFoodSeek()));

        // Herd
        weights.setWander(profile.getDouble("behaviors.leader_following", weights.getWander()));
        weights.setCohesion(profile.getDouble("behaviors.herd_cohesion", weights.getCohesion()));

        // Fleeing
        weights.setObstacleAvoidance(profile.getDouble("behaviors.escape", weights.getObstacleAvoidance()));
        weights.setLightPreference(profile.getDouble("behaviors.flight_initiation", weights.getLightPreference()));
        weights.setPursuit(profile.getDouble("behaviors.panic", weights.getPursuit()));
        weights.setEvasion(profile.getDouble("behaviors.alarm_signal", weights.getEvasion()));

        // Parent-offspring
        weights.setWander(profile.getDouble("behaviors.follow_mother", weights.getWander()));
        weights.setShelterSeek(profile.getDouble("behaviors.hiding", weights.getShelterSeek()));
        weights.setTerritorialDefense(profile.getDouble("behaviors.mother_protection", weights.getTerritorialDefense()));
        weights.setSeparation(profile.getDouble("behaviors.separation_distress", weights.getSeparation()));

        // Sniffer behaviors
        weights.setSniffing(profile.getDouble("behaviors.sniffing", 0.0));
        weights.setDigging(profile.getDouble("behaviors.digging", 0.0));
        weights.setSnifferSocial(profile.getDouble("behaviors.sniffer_social", 0.0));

        // Allay behaviors
        weights.setItem_collecting(profile.getDouble("behaviors.item_collecting", weights.getItem_collecting()));
        weights.setSound_following(profile.getDouble("behaviors.sound_following", weights.getSound_following()));

        // Strider behaviors
        weights.setLava_walking(profile.getDouble("behaviors.lava_walking", weights.getLava_walking()));
        weights.setTemperature_seeking(profile.getDouble("behaviors.temperature_seeking", weights.getTemperature_seeking()));
        weights.setRiding(profile.getDouble("behaviors.riding", weights.getRiding()));

        return weights;
    }

    /**
     * Creates a new registry for a single entity.
     * Each entity gets its own registry with isolated behavior instances.
     */
    private static BehaviorRegistry createRegistry() {
        return createRegistry(null, null);
    }

    /**
     * Creates a new registry for a single entity with optional mob type.
     * Each entity gets its own registry with isolated behavior instances.
     */
    private static BehaviorRegistry createRegistry(Mob mob, ResourceLocation mobId) {
        BehaviorRegistry registry = new BehaviorRegistry();

        // Create per-entity config objects
        HerdConfig herdConfig = new HerdConfig();
        FleeingConfig fleeingConfig = FleeingConfig.createDefault();

        // Flocking behaviors - new instances per entity
        registry.register("separation", new SeparationBehavior(2.0, 1.0, 0.15), "flocking");
        registry.register("alignment", new AlignmentBehavior(1.0, 1.0, 0.1), "flocking");
        registry.register("cohesion", new CohesionBehavior(2.0, 1.0, 0.1), "flocking");

        // Predation behaviors - new instances per entity
        registry.register("pursuit", new PursuitBehavior(1.2, 0.15, 1.0, 64.0), "predation");
        registry.register("evasion", new EvasionBehavior(1.5, 0.2, 24.0, 48.0), "predation");
        registry.register("avoidance", new AvoidanceBehavior(8.0, 0.15, 16.0), "predation");
        registry.register("attraction", new AttractionBehavior(1.0, 0.1, 16.0), "predation");

        // Herd behaviors - new instances per entity
        registry.register("leader_following", new LeaderFollowing(herdConfig), "herd");
        registry.register("herd_cohesion", new HerdCohesion(herdConfig), "herd");

        // Fleeing behaviors - new instances per entity
        registry.register("escape", new EscapeBehavior(fleeingConfig), "fleeing");
        registry.register("panic", new PanicBehavior(fleeingConfig), "fleeing");
        // Note: flight_initiation and alarm_signal don't implement BehaviorRule
        // They are handled separately as behavioral triggers, not steering behaviors

        // Parent-offspring behaviors - new instances per entity
        registry.register("follow_mother", new FollowMotherBehavior(), "parent");
        registry.register("hiding", new HidingBehavior(), "parent");
        registry.register("mother_protection", new MotherProtectionBehavior(), "parent");
        registry.register("separation_distress", new SeparationDistressBehavior(false), "parent");

        // Crepuscular behaviors - only register for bats
        if (mobId != null && mobId.equals(ResourceLocation.withDefaultNamespace("bat"))) {
            CrepuscularConfig batConfig = new CrepuscularConfig();
            registry.register("roosting", new RoostingBehavior(batConfig), "crepuscular");
            // Note: EmergenceTrigger is not a SteeringBehavior, it's a utility class
            // and cannot be registered directly in the behavior registry
        }

        // Territorial behaviors - home range for wolves, foxes, and ocelots
        if (mobId != null && mob != null) {
            String mobIdStr = mobId.toString();
            if (mobIdStr.equals("minecraft:wolf") ||
                mobIdStr.equals("minecraft:fox") ||
                mobIdStr.equals("minecraft:ocelot") ||
                mobIdStr.equals("minecraft:cat")) {
                // Use entity's current position as home range center
                BlockPos homeCenter = mob.blockPosition();
                HomeRange homeRange = new HomeRange(homeCenter, 64.0);
                registry.register("home_range", new HomeRangeBehavior(homeRange, 0.3, true, 0.5), "territorial");
            }
        }

        // Bee-specific behaviors - only register for bees
        if (mobId != null && mobId.equals(ResourceLocation.withDefaultNamespace("bee"))) {
            registry.register("pollination", new PollinationBehavior(), "bee");
            registry.register("hive_return", new HiveReturnBehavior(), "bee");
            registry.register("waggle_dance", new WaggleDanceBehavior(), "bee");
            registry.register("hive_defense", new HiveDefenseBehavior(), "bee");
        }

        // Allay-specific behaviors
        if (mobId != null && mobId.equals(ResourceLocation.withDefaultNamespace("allay"))) {
            registry.register("item_collecting", new me.javavirtualenv.behavior.allay.ItemCollectingBehavior(), "allay");
            registry.register("sound_following", new me.javavirtualenv.behavior.allay.SoundFollowingBehavior(), "allay");
        }

        // Strider-specific behaviors
        if (mobId != null && mobId.equals(ResourceLocation.withDefaultNamespace("strider"))) {
            registry.register("lava_walking", new me.javavirtualenv.behavior.strider.LavaWalkingBehavior(), "strider");
            registry.register("temperature_seeking", new me.javavirtualenv.behavior.strider.TemperatureSeekingBehavior(), "strider");
            registry.register("riding", new me.javavirtualenv.behavior.strider.RidingBehavior(), "strider");
        }

        // Armadillo-specific behaviors
        if (mobId != null && mobId.equals(ResourceLocation.withDefaultNamespace("armadillo"))) {
            registry.register("predator_avoidance", new ArmadilloPredatorAvoidance(24.0, 1.5, 8.0), "armadillo");
        }

        // Aquatic behaviors - register for fish and squid
        if (mobId != null) {
            String mobIdStr = mobId.toString();

            // Schooling behavior for fish
            if (mobIdStr.equals("minecraft:cod") ||
                mobIdStr.equals("minecraft:salmon") ||
                mobIdStr.equals("minecraft:tropical_fish")) {
                registry.register("schooling", new SchoolingBehavior(), "aquatic");
                registry.register("current_riding", new CurrentRidingBehavior(), "aquatic");
            }

            // Salmon-specific: upstream swimming
            if (mobIdStr.equals("minecraft:salmon")) {
                registry.register("upstream", new SalmonUpstreamBehavior(), "aquatic");
            }

            // Squid and glow squid behaviors
            if (mobIdStr.equals("minecraft:squid") ||
                mobIdStr.equals("minecraft:glow_squid")) {
                registry.register("ink_cloud", new InkCloudBehavior(), "aquatic");
                registry.register("vertical_migration", new VerticalMigrationBehavior(), "aquatic");

                // Glow squid also attracts prey
                if (mobIdStr.equals("minecraft:glow_squid")) {
                    registry.register("prey_attraction", new GlowSquidPreyAttractionBehavior(), "aquatic");
                }
            }

            // Pufferfish inflate behavior
            if (mobIdStr.equals("minecraft:pufferfish")) {
                registry.register("inflate", new PufferfishInflateBehavior(), "aquatic");
            }

            // Axolotl behaviors
            if (mobIdStr.equals("minecraft:axolotl")) {
                registry.register("hunting", new AxolotlHuntingBehavior(), "aquatic");
                registry.register("play_dead", new AxolotlPlayDeadBehavior(), "aquatic");
            }

            // Dolphin behaviors
            if (mobIdStr.equals("minecraft:dolphin")) {
                registry.register("wave_riding", new DolphinWaveRidingBehavior(), "aquatic");
                registry.register("treasure_hunt", new DolphinTreasureHuntBehavior(), "aquatic");
            }

            // Tadpole metamorphosis and surface-seeking
            if (mobIdStr.equals("minecraft:tadpole")) {
                registry.register("metamorphosis", new TadpoleMetamorphosisBehavior(), "aquatic");
            }

            // Sniffer-specific behaviors
            if (mobIdStr.equals("minecraft:sniffer")) {
                registry.register("sniffing", new SniffingBehavior(), "sniffer");
                registry.register("digging", new DiggingBehavior(), "sniffer");
                registry.register("sniffer_social", new SnifferSocialBehavior(), "sniffer");
            }
        }

        return registry;
    }

    private static final class BehaviorCache {
        private final boolean enabled;
        private final int priority;
        private final double maxForce;
        private final double maxSpeed;
        private final BehaviorWeights weights;

        private BehaviorCache(boolean enabled, int priority, double maxForce,
                             double maxSpeed, BehaviorWeights weights) {
            this.enabled = enabled;
            this.priority = priority;
            this.maxForce = maxForce;
            this.maxSpeed = maxSpeed;
            this.weights = weights;
        }

        private boolean enabled() {
            return enabled;
        }

        private int priority() {
            return priority;
        }

        private double maxForce() {
            return maxForce;
        }

        private double maxSpeed() {
            return maxSpeed;
        }

        private BehaviorWeights weights() {
            return weights;
        }
    }
}
