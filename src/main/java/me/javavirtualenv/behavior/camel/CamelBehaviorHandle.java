package me.javavirtualenv.behavior.camel;

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
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Camel-specific behavior handle.
 * <p>
 * Registers all camel behaviors including:
 * - Spitting defense goal
 * - Desert endurance handle
 * - Caravan behavior steering
 * - Sand movement modifiers
 * <p>
 * This handle integrates camel-specific behaviors with the existing
 * Better Ecology behavior system.
 */
public class CamelBehaviorHandle extends CodeBasedHandle {

    private static final CamelConfig CONFIG = CamelConfig.createDefault();

    @Override
    public String id() {
        return "behavior";
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Camel)) {
            return;
        }

        // Create per-entity registry to avoid shared state
        BehaviorRegistry registry = createCamelRegistry(mob, component);

        // Create goal with camel-specific weights
        BehaviorWeights weights = createCamelWeights();

        int priority = 3; // Run before vanilla goals
        SteeringBehaviorGoal steeringGoal = new SteeringBehaviorGoal(
            mob,
            () -> registry,
            () -> weights,
            0.15, // maxForce
            0.45  // maxSpeed (camel speed)
        );

        MobAccessor accessor = (MobAccessor) mob;
        accessor.betterEcology$getGoalSelector().addGoal(priority, steeringGoal);

        // Register spitting defense goal
        SpittingDefenseGoal spittingGoal = new SpittingDefenseGoal((Camel) mob, CONFIG);
        accessor.betterEcology$getGoalSelector().addGoal(2, spittingGoal);
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Camel)) {
            return;
        }

        // Camel-specific tick behavior
        // Apply sand movement bonuses
        applySandMovementBonuses(mob);
    }

    /**
     * Creates camel-specific behavior registry.
     */
    private BehaviorRegistry createCamelRegistry(Mob mob, EcologyComponent component) {
        BehaviorRegistry registry = new BehaviorRegistry();

        // Camel-specific caravan behavior
        CaravanBehavior caravan = new CaravanBehavior(CONFIG);
        registry.register("caravan", caravan, "camel");

        // Sand movement behavior (modifies movement, doesn't steer)
        SandMovementBehavior sandMovement = new SandMovementBehavior(CONFIG);
        registry.register("sand_movement", sandMovement, "camel");

        // Basic flocking behaviors (for when not in caravan)
        registry.register("separation", new SeparationBehavior(2.5, 0.45, 0.12), "flocking");
        registry.register("alignment", new AlignmentBehavior(0.8, 0.45, 0.08), "flocking");
        registry.register("cohesion", new CohesionBehavior(2.0, 0.45, 0.08), "flocking");

        // Evasion behavior (camels avoid predators)
        registry.register("evasion", new EvasionBehavior(1.3, 0.15, 20.0, 40.0), "predation");
        registry.register("avoidance", new AvoidanceBehavior(6.0, 0.12, 12.0), "predation");
        registry.register("attraction", new AttractionBehavior(0.8, 0.08, 16.0), "predation");

        return registry;
    }

    /**
     * Creates camel-specific behavior weights.
     */
    private BehaviorWeights createCamelWeights() {
        BehaviorWeights weights = new BehaviorWeights();

        // Caravan behavior is highest priority for camels
        weights.setWander(0.5);

        // Moderate flocking (camels are somewhat social)
        weights.setSeparation(0.25);
        weights.setAlignment(0.15);
        weights.setCohesion(0.3);

        // Evasion is important (camels avoid predators)
        weights.setEvasion(0.6);
        weights.setTerritorialDefense(0.0); // No territorial behavior
        weights.setFoodSeek(0.35);

        return weights;
    }

    /**
     * Applies sand movement bonuses.
     */
    private void applySandMovementBonuses(Mob mob) {
        // Check if on sand
        boolean onSand = SandMovementBehavior.isOnSand(mob);
        boolean onSoulSand = SandMovementBehavior.isOnSoulSand(mob);

        if (onSand || onSoulSand) {
            // Speed bonus is handled by the movement system
            // This is where we'd apply attribute modifiers if needed
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (!(mob instanceof Camel)) {
            return;
        }

        // Save caravan data
        CompoundTag caravanTag = component.getHandleTag("caravan");
        if (!caravanTag.isEmpty()) {
            tag.put("caravan", caravanTag.copy());
        }

        // Save desert endurance data
        CompoundTag desertTag = component.getHandleTag("desert_endurance");
        if (!desertTag.isEmpty()) {
            tag.put("desert_endurance", desertTag.copy());
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (!(mob instanceof Camel)) {
            return;
        }

        // Restore caravan data
        if (tag.contains("caravan")) {
            component.setHandleTag("caravan", tag.getCompound("caravan"));
        }

        // Restore desert endurance data
        if (tag.contains("desert_endurance")) {
            component.setHandleTag("desert_endurance", tag.getCompound("desert_endurance"));
        }
    }
}
