package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.fox.*;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fox-specific behavior registration.
 * <p>
 * Foxes are nocturnal hunters with the following behaviors:
 * - Hunting: Stalk, crouch, pounce on prey (rabbits, chickens, fish)
 * - Berry Foraging: Seek out and eat sweet berries
 * - Item Carrying: Pick up and carry items, gift to trusted players
 * - Sleeping: Sleep during the day in sheltered areas
 * <p>
 * This mixin registers all behaviors and goals defined in the
 * fox configuration.
 */
@Mixin(Fox.class)
public abstract class FoxMixin extends AnimalMixin {

    // Behavior instances (created per fox in registerBehaviors)
    private FoxPursuitBehavior pursuitBehavior;
    private FoxBerryForaging berryForagingBehavior;
    private FoxItemCarry itemCarryBehavior;
    private FoxSleeping sleepingBehavior;

    /**
     * Registers fox behaviors from configuration.
     * Creates an AnimalConfig with handles for all fox-specific behaviors.
     */
    @Override
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation foxId = ResourceLocation.withDefaultNamespace("fox");

        AnimalConfig config = AnimalConfig.builder(foxId)
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

            // Temporal behaviors - nocturnal pattern
            .addHandle(new TemporalHandle())

            // Predation - both predator and prey
            .addHandle(new PredationHandle())

            // Diet - carnivorous hunter with berry foraging
            .addHandle(new DietHandle())

            .build();

        AnimalBehaviorRegistry.register(foxId, config);
        markBehaviorsRegistered();
    }

    /**
     * Inject after Fox constructor to register behaviors.
     * Uses TAIL to ensure fox is fully initialized.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
        registerFoxGoals();
    }

    /**
     * Register fox-specific goals.
     */
    private void registerFoxGoals() {
        Fox fox = (Fox) (Object) this;
        if (!(fox instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        // Create behavior instances for this fox
        pursuitBehavior = new FoxPursuitBehavior(0.3, 1.2, 0.8, 5.0, 64.0, 100, 40);
        berryForagingBehavior = new FoxBerryForagingBehavior(24.0, 0.8, 0.5, 40);
        itemCarryBehavior = new FoxItemCarryBehavior(2.0, 16.0, 0.6);
        sleepingBehavior = new FoxSleepingBehavior(16.0, 0.5, 13500, 23000);

        // Initialize item storage
        FoxItemStorage.get(fox);

        // Register goals via accessor
        MobAccessor accessor = (MobAccessor) fox;
        GoalSelector goalSelector = accessor.betterEcology$getGoalSelector();

        // Fox goal priorities (higher number = lower priority)
        // Sleep: highest priority during day
        goalSelector.addGoal(1, new FoxSleepGoal(pathfinderMob, sleepingBehavior));

        // Hunt: high priority when hungry or at night
        goalSelector.addGoal(3, new FoxHuntGoal(pathfinderMob, pursuitBehavior, 1.2));

        // Forage: medium priority
        goalSelector.addGoal(5, new FoxForageGoal(pathfinderMob, berryForagingBehavior, 5));

        // Pickup items: lower priority
        goalSelector.addGoal(7, new FoxPickupItemGoal(pathfinderMob, itemCarryBehavior, 0.8));
    }
}
