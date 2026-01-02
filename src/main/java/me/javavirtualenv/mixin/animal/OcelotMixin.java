package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ocelot-specific behavior registration.
 * <p>
 * Ocelots are shy jungle cats that:
 * - Hunt creepers (deterrent behavior) and chickens
 * - Exhibit crepuscular activity patterns (most active at dawn/dusk)
 * - Serve as solitary predators in jungle ecosystems
 * - Have high agility and speed, especially when fleeing or hunting
 * - Require jungle canopy for hunting and shelter
 * - Are distrustful of players and flee from approach
 * - Display stalking and pouncing behaviors
 * <p>
 * Special feline behaviors:
 * - Stalking and pouncing on prey
 * - Creeping through undergrowth
 * - Creeper detection and deterrence
 * - Quiet stealth movement
 * - Squeezing through gaps in vegetation
 * - Climbing trees to escape threats or ambush prey
 * - Landing on feet (no fall damage)
 * - Play behavior with prey and environmental objects
 */
@Mixin(Ocelot.class)
public abstract class OcelotMixin {

    @Unique
    private static boolean ocelotBehaviorsRegistered = false;

    /**
     * Registers ocelot behaviors from JSON configuration.
     * Creates an AnimalConfig with handles for all ocelot-specific behaviors.
     */
    @Unique
    private void registerBehaviors() {
        if (ocelotBehaviorsRegistered) {
            return;
        }

        ResourceLocation ocelotId = ResourceLocation.withDefaultNamespace("ocelot");

        AnimalConfig config = AnimalConfig.builder(ocelotId)
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

            // Temporal behaviors - crepuscular pattern
            .addHandle(new TemporalHandle())

            // Predation - both predator and prey
            .addHandle(new PredationHandle())

            // Diet - carnivorous hunter
            .addHandle(new DietHandle())

            // Feline behaviors - stalking, pouncing, climbing, stealth
            .addHandle(new FelineBehaviorHandle())

            .build();

        AnimalBehaviorRegistry.register(ocelotId, config);
        ocelotBehaviorsRegistered = true;
    }

    /**
     * Inject after Ocelot constructor to register behaviors.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
        registerOcelotGoals();
    }

    /**
     * Register ocelot-specific goals.
     */
    @Unique
    private void registerOcelotGoals() {
        Ocelot ocelot = (Ocelot) (Object) this;
        if (!(ocelot instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        // Register goals via accessor
        MobAccessor accessor = (MobAccessor) ocelot;
        GoalSelector goalSelector = accessor.betterEcology$getGoalSelector();

        // Low health flee: highest priority - retreat when hurt
        goalSelector.addGoal(1, new LowHealthFleeGoal(ocelot, 0.55, 1.5));
    }
}
