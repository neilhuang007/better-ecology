package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.production.AllayCollectionGoal;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allay-specific behavior and production registration.
 * <p>
 * Allays:
 * - Are flying spirits that collect items
 * - Can duplicate items when dancing
 * - Have affinity with note blocks
 * - Follow players who give them items
 * - Can improve item quality
 * - Celebrate with dance animations
 * - Seek and collect specific item types with memory
 * - Follow note block sounds and jukebox music
 * - Dance to music with heart particles
 * - Bring items to player or note block
 */
@Mixin(Allay.class)
public abstract class AllayMixin extends AnimalMixin {

    private AllayCollectionGoal collectionGoal;

    @Override
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation allayId = ResourceLocation.withDefaultNamespace("allay");

        AnimalConfig config = AnimalConfig.builder(allayId)
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

            // Predation - disabled for allays
            .addHandle(new PredationHandle())

            // Diet
            .addHandle(new DietHandle())

            // Production - item duplication bonuses
            .addHandle(new ResourceProductionHandle())

            // Behaviors
            .addHandle(new BehaviorHandle())

            .build();

        AnimalBehaviorRegistry.register(allayId, config);
        markBehaviorsRegistered();
    }

    /**
     * Inject after Allay constructor to register behaviors and goals.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
        registerCollectionGoal();
    }

    /**
     * Registers the item collection goal.
     */
    private void registerCollectionGoal() {
        Allay allay = (Allay) (Object) this;

        if (collectionGoal == null) {
            collectionGoal = new AllayCollectionGoal(allay);

            int goalPriority = 3;
            allay.goalSelector.addGoal(goalPriority, collectionGoal);
        }
    }
}
