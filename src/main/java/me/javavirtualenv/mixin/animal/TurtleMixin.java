package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingHandle;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turtle-specific behavior registration with nest building support.
 * <p>
 * Turtles are unique aquatic reptiles that:
 * - Dig egg chambers on sand beaches (not above-ground nests)
 * - Return to the same beach for nesting (homing behavior)
 * - Cover eggs with sand after laying
 * - Hatchlings emerge and move to water
 * - Are herbivores that primarily eat seagrass
 * - Move slowly on land but swim well in water
 * <p>
 * Nest building behaviors:
 * - Migrate to sandy beaches for nesting
 * - Dig egg chambers in sand blocks
 * - Lay eggs in the chamber
 * - Cover eggs with sand using back flippers
 * - Return to the same beach location (homing)
 * - No nest construction materials needed
 * <p>
 * This mixin registers all behaviors and configurations defined in the
 * turtle YAML config.
 */
@Mixin(Turtle.class)
public abstract class TurtleMixin {

    @Unique
    private static boolean behaviorsRegistered = false;

    /**
     * Registers turtle behaviors from YAML configuration.
     * Creates an AnimalConfig with handles for all turtle-specific behaviors.
     * <p>
     * Handles registered:
     * - Health: 15 HP base
     * - Movement: 0.15 walk speed, 0.3 swim speed
     * - Hunger: 100 max, 0.01 decay rate (slow metabolism)
     * - Condition: Body condition that affects breeding
     * - Energy: Energy for swimming and beach migration
     * - Age: 24000 tick baby duration, long lifespan
     * - Social: Solitary but congregate for nesting
     * - Breeding: 12000 tick cooldown, min condition 70
     * - NestBuilding: Sand nest digging on beaches
     * - Temporal: Daily cycle activity, migrates for nesting
     * - Predation: Vulnerable when on land, protected in water
     * - Diet: Seagrass and other aquatic plants
     * - Behavior: Swimming, beach nesting, homing
     */
    @Unique
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation turtleId = ResourceLocation.withDefaultNamespace("turtle");

        AnimalConfig config = AnimalConfig.builder(turtleId)
            // Physical attributes
            .addHandle(new HealthHandle())
            .addHandle(new MovementHandle())

            // Internal state tracking
            .addHandle(new HungerHandle())
            .addHandle(new ThirstHandle())
            .addHandle(new ConditionHandle())
            .addHandle(new EnergyHandle())
            .addHandle(new AgeHandle())
            .addHandle(new SocialHandle())

            // Reproduction
            .addHandle(new BreedingHandle())
            .addHandle(new NestBuildingHandle())

            // Temporal behaviors
            .addHandle(new TemporalHandle())

            // Predation - turtles are prey on land
            .addHandle(new PredationHandle())

            // Diet - aquatic herbivores
            .addHandle(new DietHandle())

            // Note: BehaviorHandle comes from profile via mergeHandles

            .build();

        AnimalBehaviorRegistry.register(turtleId, config);
        markBehaviorsRegistered();
    }

    @Unique
    protected boolean areBehaviorsRegistered() {
        return behaviorsRegistered;
    }

    @Unique
    protected void markBehaviorsRegistered() {
        behaviorsRegistered = true;
    }

    /**
     * Inject after Turtle constructor to register behaviors.
     * Uses TAIL to ensure turtle is fully initialized.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();

        // Add turtle-specific goals if needed
        Turtle turtle = (Turtle) (Object) this;
        if (!(turtle instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        EcologyComponent component = ((EcologyAccess) turtle).betterEcology$getEcologyComponent();
        // Nest building goals are registered by NestBuildingHandle
        // Turtle-specific homing and beach migration can be added here

        // Add low health flee goal - turtles flee when health drops below 40%
        // Turtles have shells so they fight longer (40% threshold vs 30% default)
        // Turtles are slow, so flee speed is 0.8
        MobAccessor accessor = (MobAccessor) pathfinderMob;
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(turtle, 0.40, 0.8));
    }
}
