package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.bee.BeeComponent;
import me.javavirtualenv.behavior.bee.HiveDefenseBehavior;
import me.javavirtualenv.behavior.bee.HiveReturnBehavior;
import me.javavirtualenv.behavior.bee.PollinationBehavior;
import me.javavirtualenv.behavior.bee.WaggleDanceBehavior;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bee-specific behavior registration.
 * <p>
 * Bees are unique in that they:
 * - Are flying insects that pollinate flowers and crops
 * - Live in colonies with hive-based social structure
 * - Have diurnal activity patterns (return to hive at night)
 * - Cannot swim and avoid water
 * - Produce honey in hives
 * - Have swarm defense behavior when provoked
 * - Serve as critical pollinators for ecosystem
 * <p>
 * This mixin registers all behaviors and configurations defined in the
 * bee YAML config at data/better-ecology/mobs/passive/bee/mod_registry.yaml
 */
@Mixin(Bee.class)
public abstract class BeeMixin extends AnimalMixin {

    private static final String BEE_COMPONENT_KEY = "better-ecology:bee-data";

    /**
     * Registers bee behaviors from YAML configuration.
     * Creates an AnimalConfig with handles for all bee-specific behaviors.
     * <p>
     * Handles registered:
     * - Health: 5 HP base (10 hearts), 0.5x for babies
     * - Size: 0.7 width, 0.6 height, 0.5x scale for babies
     * - Movement: 0.3 fly speed, no walk/swim, avoids water, 1.4x when angry
     * - Hunger: 100 max, 0.015 decay rate, 80 starting value
     * - Thirst: Disabled (bees don't need water tracking)
     * - Condition: Body condition for health and breeding
     * - Energy: Flying costs 0.4 energy/tick, fleeing costs 0.3
     * - Age: 24000 tick baby duration, no elderly/death from age
     * - Social: Colony-based, hive-centered, 0.02 decay away from hive
     * - Breeding: 24000 tick cooldown, min condition 65, sexual reproduction
     * - Temporal: Diurnal (active day, inactive night), rain/storm seeks hive
     * - Predation: Disabled (bees have defense, not prey behavior)
     * - Diet: Flowers (#minecraft:flowers) via pollination, 20 nutrition
     * - Behavior: Pollination, hive-returning, swarming, waggle dance
     * <p>
     * All configuration values are loaded from the YAML profile.
     */
    @Override
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation beeId = ResourceLocation.withDefaultNamespace("bee");

        AnimalConfig config = AnimalConfig.builder(beeId)
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

            // Predation - disabled for bees
            .addHandle(new PredationHandle())

            // Diet - pollination-based
            .addHandle(new DietHandle())

            // Production - honey production
            .addHandle(new ResourceProductionHandle())

            // Behaviors - pollination, hive behaviors, swarming
            .addHandle(new BehaviorHandle())

            .build();

        AnimalBehaviorRegistry.register(beeId, config);
        markBehaviorsRegistered();
    }

    /**
     * Inject after Bee constructor to register behaviors.
     * Uses TAIL to ensure bee is fully initialized.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
        initializeBeeComponent();
    }

    /**
     * Initialize the bee component for this bee.
     */
    private void initializeBeeComponent() {
        Bee bee = (Bee) (Object) this;
        EcologyComponent component = EcologyComponent.getOrCreate(bee);

        if (!component.hasHandleData(BEE_COMPONENT_KEY)) {
            BeeComponent beeComponent = new BeeComponent();
            CompoundTag tag = beeComponent.toNbt();
            component.setHandleData(BEE_COMPONENT_KEY, tag);
        }
    }

    /**
     * Inject into addAdditionalSaveData to save bee component data.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSaveData(CompoundTag compound, CallbackInfo ci) {
        Bee bee = (Bee) (Object) this;
        EcologyComponent component = EcologyComponent.getIfExists(bee);

        if (component != null && component.hasHandleData(BEE_COMPONENT_KEY)) {
            compound.put(BEE_COMPONENT_KEY, component.getHandleData(BEE_COMPONENT_KEY));
        }
    }

    /**
     * Inject into readAdditionalSaveData to load bee component data.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoadData(CompoundTag compound, CallbackInfo ci) {
        Bee bee = (Bee) (Object) this;
        EcologyComponent component = EcologyComponent.getOrCreate(bee);

        if (compound.contains(BEE_COMPONENT_KEY)) {
            CompoundTag beeTag = compound.getCompound(BEE_COMPONENT_KEY);
            component.setHandleData(BEE_COMPONENT_KEY, beeTag);
        }
    }

    /**
     * Inject into hasNectar getter to sync with bee component.
     */
    @Inject(method = "hasNectar", at = @At("HEAD"), cancellable = true)
    private void onHasNectar(CallbackInfoReturnable<Boolean> cir) {
        Bee bee = (Bee) (Object) this;
        EcologyComponent component = EcologyComponent.getIfExists(bee);

        if (component != null && component.hasHandleData(BEE_COMPONENT_KEY)) {
            CompoundTag tag = component.getHandleData(BEE_COMPONENT_KEY);
            BeeComponent beeComponent = BeeComponent.fromNbt(tag);
            cir.setReturnValue(beeComponent.hasNectar());
        }
    }

    /**
     * Inject into setHasNectar to sync with bee component.
     */
    @Inject(method = "setHasNectar", at = @At("TAIL"))
    private void onSetHasNectar(boolean hasNectar, CallbackInfo ci) {
        Bee bee = (Bee) (Object) this;
        EcologyComponent component = EcologyComponent.getOrCreate(bee);

        CompoundTag tag = component.hasHandleData(BEE_COMPONENT_KEY)
            ? component.getHandleData(BEE_COMPONENT_KEY)
            : new CompoundTag();
        BeeComponent beeComponent = BeeComponent.fromNbt(tag);
        beeComponent.setHasNectar(hasNectar);
        component.setHandleData(BEE_COMPONENT_KEY, beeComponent.toNbt());
    }

    /**
     * Gets the bee component for this bee.
     */
    public BeeComponent getBeeComponent() {
        Bee bee = (Bee) (Object) this;
        EcologyComponent component = EcologyComponent.getIfExists(bee);

        if (component != null && component.hasHandleData(BEE_COMPONENT_KEY)) {
            return BeeComponent.fromNbt(component.getHandleData(BEE_COMPONENT_KEY));
        }

        return new BeeComponent();
    }
}
