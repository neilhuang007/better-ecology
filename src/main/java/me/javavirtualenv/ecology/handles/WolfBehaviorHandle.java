package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.BehaviorRegistry;
import me.javavirtualenv.behavior.BehaviorWeights;
import me.javavirtualenv.behavior.ai.SteeringBehaviorGoal;
import me.javavirtualenv.behavior.flocking.AlignmentBehavior;
import me.javavirtualenv.behavior.flocking.CohesionBehavior;
import me.javavirtualenv.behavior.flocking.SeparationBehavior;
import me.javavirtualenv.behavior.predation.AttractionBehavior;
import me.javavirtualenv.behavior.predation.AvoidanceBehavior;
import me.javavirtualenv.behavior.predation.EvasionBehavior;
import me.javavirtualenv.behavior.predation.PursuitBehavior;
import me.javavirtualenv.behavior.wolf.*;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Wolf-specific behavior handle.
 * Registers all wolf behaviors including pack hunting, territorial behavior,
 * and pack hierarchy management.
 */
public final class WolfBehaviorHandle extends CodeBasedHandle {

    private static final String NBT_PACK_ID = "packId";
    private static final String NBT_TERRITORY_CENTER_X = "territoryX";
    private static final String NBT_TERRITORY_CENTER_Y = "territoryY";
    private static final String NBT_TERRITORY_CENTER_Z = "territoryZ";
    private static final String NBT_HIERARCHY_RANK = "hierarchyRank";

    @Override
    public String id() {
        return "behavior";
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Wolf)) {
            return;
        }

        // Create per-entity registry to avoid shared state
        BehaviorRegistry registry = createWolfRegistry(mob, component);

        // Create goal with wolf-specific weights
        BehaviorWeights weights = createWolfWeights();

        int priority = 3; // Run before vanilla goals
        SteeringBehaviorGoal goal = new SteeringBehaviorGoal(
            mob,
            () -> registry,
            () -> weights,
            0.18, // maxForce
            1.3   // maxSpeed
        );

        MobAccessor accessor = (MobAccessor) mob;
        accessor.betterEcology$getGoalSelector().addGoal(priority, goal);
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Wolf wolf)) {
            return;
        }

        // Skip if tamed - wolves become more dog-like
        if (wolf.isTame()) {
            return;
        }

        // Periodic pack behaviors
        int currentTick = wolf.tickCount;

        // Howling behavior (social bonding and territory communication)
        if (currentTick % 1200 == 0 && wolf.getRandom().nextDouble() < 0.3) {
            // 30 second interval, 30% chance
            // Howling is handled by the territorial behavior
        }

        // Pack cohesion check
        if (currentTick % 200 == 0) {
            ensurePackCohesion(wolf, component);
        }
    }

    /**
     * Creates wolf-specific behavior registry.
     */
    private BehaviorRegistry createWolfRegistry(Mob mob, EcologyComponent component) {
        BehaviorRegistry registry = new BehaviorRegistry();

        // Wolf-specific pack behaviors
        PackHuntingBehavior packHunting = new PackHuntingBehavior();
        registry.register("pack_hunting", packHunting, "wolf");

        PackTerritoryBehavior packTerritory = new PackTerritoryBehavior();
        registry.register("pack_territory", packTerritory, "wolf");

        PackHierarchyBehavior packHierarchy = new PackHierarchyBehavior();
        registry.register("pack_hierarchy", packHierarchy, "wolf");

        // Basic steering behaviors (for when not in pack mode)
        registry.register("separation", new SeparationBehavior(3.0, 1.3, 0.18), "flocking");
        registry.register("alignment", new AlignmentBehavior(1.0, 1.3, 0.1), "flocking");
        registry.register("cohesion", new CohesionBehavior(3.0, 1.3, 0.1), "flocking");

        // Predation behaviors
        registry.register("pursuit", new PursuitBehavior(1.3, 0.18, 1.0, 64.0), "predation");
        registry.register("evasion", new EvasionBehavior(1.5, 0.2, 24.0, 48.0), "predation");
        registry.register("avoidance", new AvoidanceBehavior(8.0, 0.15, 16.0), "predation");
        registry.register("attraction", new AttractionBehavior(1.0, 0.1, 16.0), "predation");

        return registry;
    }

    /**
     * Creates wolf-specific behavior weights.
     */
    private BehaviorWeights createWolfWeights() {
        BehaviorWeights weights = new BehaviorWeights();
        weights.setPursuit(0.4);
        weights.setEvasion(0.6);
        weights.setSeparation(0.3);
        weights.setAlignment(0.2);
        weights.setCohesion(0.25);
        weights.setTerritorialDefense(0.8);
        weights.setWander(0.15);
        return weights;
    }

    /**
     * Ensures wolf is part of a cohesive pack.
     */
    private void ensurePackCohesion(Wolf wolf, EcologyComponent component) {
        CompoundTag tag = component.getHandleTag(id());

        // Check if wolf has a pack
        if (!tag.hasUUID(NBT_PACK_ID)) {
            // Try to join nearby pack
            Wolf nearbyPack = findNearbyPack(wolf);
            if (nearbyPack != null) {
                // Join their pack
                CompoundTag otherTag = getOtherWolfTag(nearbyPack);
                if (otherTag.hasUUID(NBT_PACK_ID)) {
                    tag.putUUID(NBT_PACK_ID, otherTag.getUUID(NBT_PACK_ID));
                }
            } else {
                // Start new pack
                tag.putUUID(NBT_PACK_ID, java.util.UUID.randomUUID());
            }
        }
    }

    /**
     * Finds a nearby wolf pack to join.
     */
    private Wolf findNearbyPack(Wolf wolf) {
        var nearbyWolves = wolf.level().getEntitiesOfClass(
            Wolf.class,
            wolf.getBoundingBox().inflate(32.0)
        );

        for (Wolf other : nearbyWolves) {
            if (other.isTame() || other.equals(wolf)) {
                continue;
            }
            return other;
        }

        return null;
    }

    /**
     * Gets another wolf's behavior tag (for pack sharing).
     */
    private CompoundTag getOtherWolfTag(Wolf wolf) {
        // This would need to access the other wolf's component
        // For now, use persistent data
        var persistentData = wolf.getPersistentData();
        CompoundTag tag = new CompoundTag();

        if (persistentData.hasUUID("WolfPackId")) {
            tag.putUUID(NBT_PACK_ID, persistentData.getUUID("WolfPackId"));
        }

        return tag;
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());

        // Save pack data
        if (mob instanceof Wolf wolf && !wolf.isTame()) {
            var persistentData = wolf.getPersistentData();

            if (persistentData.hasUUID("WolfPackId")) {
                handleTag.putUUID(NBT_PACK_ID, persistentData.getUUID("WolfPackId"));
            }

            if (persistentData.contains("WolfTerritoryX")) {
                handleTag.putDouble(NBT_TERRITORY_CENTER_X, persistentData.getDouble("WolfTerritoryX"));
                handleTag.putDouble(NBT_TERRITORY_CENTER_Y, persistentData.getDouble("WolfTerritoryY"));
                handleTag.putDouble(NBT_TERRITORY_CENTER_Z, persistentData.getDouble("WolfTerritoryZ"));
            }
        }

        tag.put(id(), handleTag.copy());
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (!tag.contains(id())) {
            return;
        }

        CompoundTag handleTag = tag.getCompound(id());
        component.setHandleTag(id(), handleTag);

        // Restore pack data to persistent data
        if (mob instanceof Wolf wolf && !wolf.isTame()) {
            var persistentData = wolf.getPersistentData();

            if (handleTag.hasUUID(NBT_PACK_ID)) {
                persistentData.putUUID("WolfPackId", handleTag.getUUID(NBT_PACK_ID));
            }

            if (handleTag.contains(NBT_TERRITORY_CENTER_X)) {
                persistentData.putDouble("WolfTerritoryX", handleTag.getDouble(NBT_TERRITORY_CENTER_X));
                persistentData.putDouble("WolfTerritoryY", handleTag.getDouble(NBT_TERRITORY_CENTER_Y));
                persistentData.putDouble("WolfTerritoryZ", handleTag.getDouble(NBT_TERRITORY_CENTER_Z));
            }
        }
    }
}
