package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.behavior.horse.HorseBehaviorHandle;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.animal.horse.Mule;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin for Mule-specific behavior registration.
 * Configures all behaviors defined in the mule YAML configuration.
 *
 * This mixin applies to net.minecraft.world.entity.animal.horse.Mule and registers
 * all ecology behaviors defined in the YAML at:
 * src/main/resources/data/better-ecology/mobs/passive/mule/mod_registry.yaml
 */
@Mixin(Mule.class)
public abstract class MuleMixin {

    @Unique
    private static final String MULE_ID = "minecraft:mule";

    @Unique
    private static boolean behaviorsRegistered = false;

    /**
     * Injection point after constructor to register mule behaviors.
     * This ensures behaviors are registered the first time a mule is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        registerMuleBehaviors();
    }

    /**
     * Register all mule behaviors defined in the YAML configuration.
     * Uses the builder pattern to create an AnimalConfig with all necessary handles.
     */
    @Unique
    private void registerMuleBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        // Create handles from YAML configuration values
        List<EcologyHandle> handles = createMuleHandles();

        // Build the animal configuration
        AnimalConfig config = AnimalConfig.builder(
                net.minecraft.resources.ResourceLocation.parse(MULE_ID))
            .addHandles(handles)
            .build();

        // Register with the behavior registry
        AnimalBehaviorRegistry.register(MULE_ID, config);
        behaviorsRegistered = true;
    }

    /**
     * Create all ecology handles for mules based on YAML configuration.
     * Values are extracted from src/main/resources/data/better-ecology/mobs/passive/mule/mod_registry.yaml
     *
     * System mappings:
     * - System 2: Physical Attributes (Health, Movement)
     * - System 3: Internal State (Hunger, Condition, Energy, Age, Social)
     * - System 4: Temporal Behavior
     * - System 5: Spatial Behavior
     * - System 6: Diet & Foraging
     * - System 7: Predation
     * - System 8: Reproduction (disabled - mules are sterile)
     * - System 9: Social Behavior
     * - System 10: Environmental Impact
     * - System 11: Population Dynamics
     * - System 12: Player Interaction
     * - System 13: Aesthetics & Personality
     */
    @Unique
    private List<EcologyHandle> createMuleHandles() {
        List<EcologyHandle> handles = new ArrayList<>();

        // System 2: Physical Attributes - Health
        // Base: 11.25 hearts (22.5 HP), Baby multiplier: 0.5
        handles.add(createHealthHandle());

        // System 2: Physical Attributes - Movement
        // Walk: 0.2, Run: 0.35, avoids_cliffs: true, cliff_threshold: 4
        handles.add(createMovementHandle());

        // System 3: Internal State - Hunger
        // Enabled: true, Max: 100, Starting: 80, Decay: 0.018
        handles.add(createHungerHandle());

        // System 3: Internal State - Condition
        // Enabled: true, Max: 100, Starting: 70
        handles.add(createConditionHandle());

        // System 3: Internal State - Energy
        // Enabled: true, Max: 100, Recovery: 0.5
        handles.add(createEnergyHandle());

        // System 3: Internal State - Age
        // Enabled: true, Baby duration: 24000, Maturity: 24000
        handles.add(createAgeHandle());

        // System 3: Internal State - Social
        // Enabled: true, Max: 100, Decay: 0.008, Recovery: 0.08
        handles.add(createSocialHandle());

        // System 4: Temporal Behavior
        // Daily cycle and weather effects
        handles.add(createTemporalHandle());

        // System 6: Diet & Foraging
        // Primary foods: grass_block, wheat, hay_block
        // Secondary foods: tall_grass, apple, carrot, sugar
        handles.add(createDietHandle());

        // System 7: Predation
        // As prey: flees from wolves and zombies
        handles.add(createPredationHandle());

        // System 8: Reproduction
        // Disabled - mules are sterile
        handles.add(createBreedingHandle());

        // System 13: Aesthetics & Personality
        // Idle behaviors and personality traits
        handles.add(createBehaviorHandle());

        // Horse-specific behaviors
        handles.add(new HorseBehaviorHandle());

        return handles;
    }

    /**
     * Handle for System 2: Physical Attributes - Health
     * YAML: physical.health.base = 11.25, baby_multiplier = 0.5
     */
    @Unique
    private EcologyHandle createHealthHandle() {
        return new CodeBasedHandle() {
            @Override
            public String id() {
                return "health";
            }

            @Override
            public void registerGoals(net.minecraft.world.entity.Mob mob, me.javavirtualenv.ecology.EcologyComponent component, me.javavirtualenv.ecology.EcologyProfile profile) {
                double baseHealth = 11.25;
                double babyMultiplier = 0.5;
                AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null) {
                    double health = baseHealth;
                    if (mob.isBaby()) {
                        health *= babyMultiplier;
                    }
                    healthAttr.setBaseValue(health);
                }
            }
        };
    }

    /**
     * Handle for System 2: Physical Attributes - Movement
     * YAML: physical.movement.speeds.walk = 0.2, run = 0.35
     *       physical.movement.capabilities.avoids_cliffs = true, cliff_threshold = 4
     */
    @Unique
    private EcologyHandle createMovementHandle() {
        return new CodeBasedHandle() {
            @Override
            public String id() {
                return "movement";
            }

            @Override
            public void registerGoals(net.minecraft.world.entity.Mob mob, me.javavirtualenv.ecology.EcologyComponent component, me.javavirtualenv.ecology.EcologyProfile profile) {
                double walkSpeed = 0.2;
                double runSpeed = 0.35;
                boolean avoidsCliffs = true;
                double cliffThreshold = 4.0;

                // Apply movement speed attribute
                AttributeInstance moveAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (moveAttr != null) {
                    moveAttr.setBaseValue(walkSpeed);
                }

                // Configure cliff avoidance
                if (avoidsCliffs) {
                    mob.setPathfindingMalus(PathType.DANGER_OTHER, (float) cliffThreshold);
                }

                // Register AI goals for pathfinder mobs
                if (!(mob instanceof PathfinderMob pathfinder)) {
                    return;
                }
                me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
                // Register water-avoiding stroll goal
                accessor.betterEcology$getGoalSelector().addGoal(5,
                    new WaterAvoidingRandomStrollGoal(pathfinder, runSpeed));
                // Register float goal
                accessor.betterEcology$getGoalSelector().addGoal(0, new FloatGoal(mob));
                // Register low health flee goal
                accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(pathfinder, 0.50, 1.5));
            }
        };
    }

    /**
     * Handle for System 3: Internal State - Hunger
     * Uses HungerHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createHungerHandle() {
        return new HungerHandle();
    }

    /**
     * Handle for System 3: Internal State - Condition
     * Uses ConditionHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createConditionHandle() {
        return new ConditionHandle();
    }

    /**
     * Handle for System 3: Internal State - Energy
     * Uses EnergyHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createEnergyHandle() {
        return new EnergyHandle();
    }

    /**
     * Handle for System 3: Internal State - Age
     * Uses AgeHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createAgeHandle() {
        return new AgeHandle();
    }

    /**
     * Handle for System 3: Internal State - Social
     * Uses SocialHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createSocialHandle() {
        return new SocialHandle();
    }

    /**
     * Handle for System 4: Temporal Behavior
     * Uses TemporalHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createTemporalHandle() {
        return new TemporalHandle();
    }

    /**
     * Handle for System 6: Diet & Foraging
     * Uses DietHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createDietHandle() {
        return new DietHandle();
    }

    /**
     * Handle for System 7: Predation
     * Uses PredationHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createPredationHandle() {
        return new PredationHandle();
    }

    /**
     * Handle for System 8: Reproduction
     * Uses BreedingHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createBreedingHandle() {
        return new BreedingHandle();
    }

    /**
     * Handle for System 13: Aesthetics & Personality
     * Uses BehaviorHandle which reads from YAML configuration
     */
    @Unique
    private EcologyHandle createBehaviorHandle() {
        return new BehaviorHandle();
    }
}
