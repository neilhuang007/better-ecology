package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.production.AxolotlProductionGoal;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Axolotl-specific behavior and production registration.
 * <p>
 * Axolotls:
 * - Are aquatic amphibians that live in water
 * - Can produce slime balls through grooming
 * - Playful behavior increases production
 * - Can be "milked" with a bottle
 * - Have play dead behavior for defense
 * - Hunt aquatic fish
 */
@Mixin(Axolotl.class)
public abstract class AxolotlMixin extends AnimalMixin {

    private AxolotlProductionGoal productionGoal;

    @Override
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation axolotlId = ResourceLocation.withDefaultNamespace("axolotl");

        AnimalConfig config = AnimalConfig.builder(axolotlId)
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

            // Predation
            .addHandle(new PredationHandle())

            // Diet
            .addHandle(new DietHandle())

            // Production - slime production
            .addHandle(new ResourceProductionHandle())

            // Behaviors
            .addHandle(new BehaviorHandle())

            .build();

        AnimalBehaviorRegistry.register(axolotlId, config);
        markBehaviorsRegistered();
    }

    /**
     * Inject after Axolotl constructor to register behaviors and goals.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
        registerProductionGoal();
    }

    /**
     * Registers the slime production goal.
     */
    private void registerProductionGoal() {
        Axolotl axolotl = (Axolotl) (Object) this;

        if (productionGoal == null) {
            productionGoal = new AxolotlProductionGoal(axolotl);

            int goalPriority = 4;
            axolotl.goalSelector.addGoal(goalPriority, productionGoal);
        }
    }
}
