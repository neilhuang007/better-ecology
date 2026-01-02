package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.predation.PredatorFeedingGoal;
import me.javavirtualenv.behavior.production.AxolotlProductionGoal;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
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
public abstract class AxolotlMixin {

    private AxolotlProductionGoal productionGoal;
    private boolean behaviorsRegistered = false;

    protected void registerBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        ResourceLocation axolotlId = ResourceLocation.withDefaultNamespace("axolotl");

        AnimalConfig config = AnimalConfig.builder(axolotlId)
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

            // Temporal behaviors
            .addHandle(new TemporalHandle())

            // Predation
            .addHandle(new PredationHandle())

            // Diet
            .addHandle(new DietHandle())

            // Production - slime production
            .addHandle(new ResourceProductionHandle())

            // Behaviors - Note: BehaviorHandle comes from profile via mergeHandles

            .build();

        AnimalBehaviorRegistry.register(axolotlId, config);
        behaviorsRegistered = true;
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
     * Registers the slime production goal and predator feeding goal.
     */
    private void registerProductionGoal() {
        Axolotl axolotl = (Axolotl) (Object) this;

        if (productionGoal == null) {
            productionGoal = new AxolotlProductionGoal(axolotl);

            int goalPriority = 4;
            GoalSelector goalSelector = ((MobAccessor) axolotl).betterEcology$getGoalSelector();

            // Register slime production goal
            goalSelector.addGoal(goalPriority, productionGoal);

            // Register predator feeding goal for aquatic meat consumption
            // Axolotls can eat meat items that sink to the water floor
            if (axolotl instanceof PathfinderMob pathfinderMob) {
                goalSelector.addGoal(5, new PredatorFeedingGoal(pathfinderMob, 1.0));
            }
        }
    }
}
