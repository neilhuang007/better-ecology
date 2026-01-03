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
import me.javavirtualenv.behavior.predation.PredatorFeedingGoal;
import me.javavirtualenv.behavior.predation.PursuitBehavior;
import me.javavirtualenv.behavior.wolf.PackHierarchyBehavior;
import me.javavirtualenv.behavior.wolf.PackHuntingBehavior;
import me.javavirtualenv.behavior.wolf.PackTerritoryBehavior;
import me.javavirtualenv.behavior.wolf.WolfPickupItemGoal;
import me.javavirtualenv.behavior.wolf.WolfShareFoodGoal;
import me.javavirtualenv.behavior.wolf.WolfPackAttackGoal;
import me.javavirtualenv.behavior.wolf.WolfSiegeAttackGoal;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;

import java.util.UUID;

/**
 * Wolf-specific behavior handle.
 * Registers all wolf behaviors including pack hunting, territorial behavior,
 * and pack hierarchy management, and feeding behaviors.
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
        if (!(mob instanceof Wolf wolf)) {
            return;
        }

        // Create per-entity registry to avoid shared state
        BehaviorRegistry registry = createWolfRegistry(mob, component);

        // Create goal with wolf-specific weights
        BehaviorWeights weights = createWolfWeights();

        int steeringPriority = 6; // Run after water avoidance (priority 5)
        SteeringBehaviorGoal steeringGoal = new SteeringBehaviorGoal(
                mob,
                () -> registry,
                () -> weights,
                0.18, // maxForce
                1.3 // maxSpeed
        );

        MobAccessor accessor = (MobAccessor) mob;

        // Register siege attack goal (higher priority than steering)
        accessor.betterEcology$getGoalSelector().addGoal(2,
                new WolfSiegeAttackGoal(wolf, 1.2, false));

        // Register pack hunting attack goal
        accessor.betterEcology$getGoalSelector().addGoal(3,
                new WolfPackAttackGoal(wolf, 1.0, false));
        // Register predator feeding goal (priority 4 - below attacks but above steering)
        accessor.betterEcology$getGoalSelector().addGoal(4,
                new PredatorFeedingGoal(wolf, 1.2));

        // Register wolf pickup item goal (priority 5 - for gathering food)
        accessor.betterEcology$getGoalSelector().addGoal(5,
                new WolfPickupItemGoal(wolf));

        // Register wolf share food goal (priority 5 - can run alongside pickup)
        accessor.betterEcology$getGoalSelector().addGoal(5,
                new WolfShareFoodGoal(wolf));

        // Register steering behavior goal (lowest priority)
        accessor.betterEcology$getGoalSelector().addGoal(steeringPriority, steeringGoal);
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

        // Wolf-specific pack behaviors - wrap SteeringBehavior with adapter
        PackHuntingBehavior packHunting = new PackHuntingBehavior();
        registry.register("pack_hunting", new SteeringBehaviorAdapter(packHunting, 1.3, 0.18), "wolf");

        PackTerritoryBehavior packTerritory = new PackTerritoryBehavior();
        registry.register("pack_territory", new SteeringBehaviorAdapter(packTerritory, 1.3, 0.18), "wolf");

        PackHierarchyBehavior packHierarchy = new PackHierarchyBehavior();
        registry.register("pack_hierarchy", new SteeringBehaviorAdapter(packHierarchy, 1.3, 0.18), "wolf");

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
                wolf.getBoundingBox().inflate(32.0));

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
        EcologyComponent component = ((EcologyAccess) wolf).betterEcology$getEcologyComponent();
        CompoundTag tag = component.getHandleTag(id());

        if (tag.hasUUID(NBT_PACK_ID)) {
            return tag.copy();
        }

        return new CompoundTag();
    }

    /**
     * Check if a wolf is hungry.
     *
     * @param wolf The wolf to check
     * @return true if hunger is below threshold
     */
    public static boolean isHungry(Wolf wolf) {
        EcologyComponent component = ((EcologyAccess) wolf).betterEcology$getEcologyComponent();
        CompoundTag hungerTag = component.getHandleTag("hunger");
        int hunger = hungerTag.getInt("hunger");
        return hunger < 40;
    }

    /**
     * Check if another wolf is in the same pack.
     *
     * @param wolf First wolf
     * @param other Second wolf
     * @return true if both wolves are in the same pack
     */
    public static boolean isSamePack(Wolf wolf, Wolf other) {
        EcologyComponent myComponent = ((EcologyAccess) wolf).betterEcology$getEcologyComponent();
        EcologyComponent otherComponent = ((EcologyAccess) other).betterEcology$getEcologyComponent();

        CompoundTag myTag = myComponent.getHandleTag("behavior");
        CompoundTag otherTag = otherComponent.getHandleTag("behavior");

        if (!myTag.hasUUID(NBT_PACK_ID) || !otherTag.hasUUID(NBT_PACK_ID)) {
            return false;
        }

        UUID myPackId = myTag.getUUID(NBT_PACK_ID);
        UUID otherPackId = otherTag.getUUID(NBT_PACK_ID);

        return myPackId.equals(otherPackId);
    }

    /**
     * Get hierarchy rank of a wolf.
     *
     * @param wolf The wolf to check
     * @return Hierarchy rank (1=alpha, 2=beta, 3=omega)
     */
    public static int getHierarchyRank(Wolf wolf) {
        EcologyComponent component = ((EcologyAccess) wolf).betterEcology$getEcologyComponent();
        CompoundTag tag = component.getHandleTag("behavior");

        if (!tag.contains(NBT_HIERARCHY_RANK)) {
            return 3;
        }

        return tag.getInt(NBT_HIERARCHY_RANK);
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (!tag.contains(id())) {
            return;
        }

        CompoundTag handleTag = tag.getCompound(id());
        component.setHandleTag(id(), handleTag);
    }
}
