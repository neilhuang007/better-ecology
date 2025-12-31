package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
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
 * This mixin registers all behaviors and configurations defined in the
 * ocelot YAML config at data/better-ecology/mobs/passive/ocelot/mod_registry.yaml
 * <p>
 * Special feline behaviors:
 * - Stalking and pouncing on prey
 * - Creeping through undergrowth
 * - Creeper detection and deterrence
 * - Quiet stealth movement
 * - Squeezing through gaps in vegetation
 */
@Mixin(Ocelot.class)
public abstract class OcelotMixin extends AnimalMixin {

    /**
     * Registers ocelot behaviors from YAML configuration.
     * Creates an AnimalConfig with handles for all ocelot-specific behaviors.
     * <p>
     * Handles registered:
     * - Health: 10 HP base (5 hearts), 0.5x for babies
     * - Movement: 0.4 walk speed, 0.6 run speed, 1.5 jump height, avoids cliffs and water
     * - Size: 0.6 width, 0.7 height, 0.5x scale for babies
     * - Biomass: 50 adult, 15 baby, 0.1 transfer efficiency
     * - Hunger: 100 max, 80 starting, 0.025 decay rate
     * - Thirst: Disabled (ocelots don't need water tracking)
     * - Condition: Body condition starting at 70, affects behavior and breeding
     * - Energy: Costs for sprinting (0.3), hunting (0.5), fleeing (0.4), swimming (0.3)
     * - Age: 24000 tick baby duration, no elderly/death from age
     * - Social: Disabled (ocelots are solitary)
     * - Breeding: 24000 tick cooldown, min condition 65, sexual reproduction
     * - Temporal: Crepuscular pattern - active at dawn/dusk (1.0), less active midday (0.2)
     * - Spatial: Home range 48 blocks, needs jungle canopy, seeks shelter in bad weather
     * - Habitat: Jungle variants only (jungle, bamboo_jungle, sparse_jungle)
     * - Predation: Predator of chickens, prey response with fleeing behavior
     * - Diet: Carnivore - primary prey (chicken), secondary (cod, salmon)
     * - Foraging: Search radius 16-32 based on hunger, remembers food locations
     * - Behavior: Sitting, grooming, stalking practice, creeper deterrent
     * - Feline: Stalking, pouncing, creeping, stealth, creeper detection
     * - Population: Weight 8 spawning, soft cap per chunk
     * - Player: Fearful base attitude, no habituation, 6 block flee distance
     * - Aesthetics: Sit and watch, groom, stalk practice idle behaviors
     * <p>
     * All configuration values are loaded from the YAML profile.
     */
    @Override
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation ocelotId = ResourceLocation.withDefaultNamespace("ocelot");

        AnimalConfig config = AnimalConfig.builder(ocelotId)
            // Physical attributes
            .addHandle(new HealthHandle())
            .addHandle(new MovementHandle())
            .addHandle(new SizeHandle())

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

            // Behaviors - sitting, stalking, creeper deterrent
            .addHandle(new BehaviorHandle())

            // Feline behaviors - stalking, pouncing, stealth
            .addHandle(new FelineBehaviorHandle())

            .build();

        AnimalBehaviorRegistry.register(ocelotId, config);
        markBehaviorsRegistered();
    }

    /**
     * Inject after Ocelot constructor to register behaviors.
     * Uses TAIL to ensure ocelot is fully initialized.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
    }
}
