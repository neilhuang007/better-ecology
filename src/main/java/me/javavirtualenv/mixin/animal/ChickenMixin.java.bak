package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chicken-specific behavior registration.
 * <p>
 * Chickens are unique in that they:
 * - Lay eggs periodically (special behavior)
 * - Can flap down slowly when falling (chicken jockey)
 * - Are ground-dwelling birds with flock behavior
 * - Are herbivores that primarily eat seeds
 * - Serve as prey for foxes, ocelots, and cats
 * <p>
 * This mixin registers all behaviors and configurations defined in the
 * chicken YAML config at data/better-ecology/mobs/passive/chicken/mod_registry.yaml
 */
@Mixin(Chicken.class)
public abstract class ChickenMixin extends AnimalMixin {

    /**
     * Registers chicken behaviors from YAML configuration.
     * Creates an AnimalConfig with handles for all chicken-specific behaviors.
     * <p>
     * Handles registered:
     * - Health: 2 HP base, 0.5x for babies
     * - Movement: 0.2 walk speed, cliff avoidance at 3.5 blocks
     * - Hunger: 100 max, 0.015 decay rate, faster metabolism
     * - Thirst: Disabled (chickens don't need water tracking)
     * - Condition: Body condition that affects breeding
     * - Energy: Energy for fleeing (1.4x speed when fleeing predators)
     * - Age: 24000 tick baby duration, no elderly/death from age
     * - Social: Flock behavior, gets lonely when alone
     * - Breeding: 6000 tick cooldown, min condition 55, sexual reproduction
     * - Temporal: Daily cycle activity, weather responses (thunder causes panic)
     * - Predation: Prey of fox, ocelot, cat - flees with zigzag pattern
     * - Diet: Seeds (wheat, beetroot, melon, pumpkin, torchflower, pitcher pod)
     * - Behavior: Flocking, fleeing, parental care (follow mother)
     * - EggLayer: Egg laying, seed dropping, grain eating behaviors
     * <p>
     * All configuration values are loaded from the YAML profile.
     */
    @Override
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation chickenId = ResourceLocation.withDefaultNamespace("chicken");

        AnimalConfig config = AnimalConfig.builder(chickenId)
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

            // Temporal behaviors
            .addHandle(new TemporalHandle())

            // Predation - chickens are prey
            .addHandle(new PredationHandle())

            // Diet - seed eaters
            .addHandle(new DietHandle())

            // Behaviors - flocking, fleeing, parental
            .addHandle(new BehaviorHandle())

            // Chicken-specific behaviors - egg laying, seed dropping, grain eating
            .addHandle(new EggLayerHandle())

            .build();

        AnimalBehaviorRegistry.register(chickenId, config);
        markBehaviorsRegistered();
    }

    /**
     * Inject after Chicken constructor to register behaviors.
     * Uses TAIL to ensure chicken is fully initialized.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
    }
}
