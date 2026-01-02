package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.fox.*;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingHandle;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fox-specific behavior registration with den creation support.
 * <p>
 * Foxes are nocturnal hunters with the following behaviors:
 * - Hunting: Stalk, crouch, pounce on prey (rabbits, chickens, fish)
 * - Berry Foraging: Seek out and eat sweet berries
 * - Item Carrying: Pick up and carry items, gift to trusted players
 * - Sleeping: Sleep during the day in sheltered areas
 * - Den Creation: Create dens in natural caves or dug burrows
 * - Multiple Entrances: Dens have multiple entrances for escape
 * - Soft Lining: Line dens with soft materials for comfort
 * <p>
 * Nest building (den creation) behaviors:
 * - Find natural caves or dig burrows in hillsides
 * - Create multiple entrances for escape routes
 * - Line den with wool, feathers, and fur for comfort
 * - Return to den during day for sleeping
 * - Raise young in den for protection
 * - Defend den territory from intruders
 * <p>
 * This mixin registers all behaviors and goals defined in the
 * fox configuration.
 */
@Mixin(Fox.class)
public abstract class FoxMixin {

    // Behavior instances (created per fox in registerFoxGoals)
    private FoxPursuitBehavior pursuitBehavior;
    private FoxBerryForagingBehavior berryForagingBehavior;
    private FoxItemCarryBehavior itemCarryBehavior;
    private FoxSleepingBehavior sleepingBehavior;

    @Unique
    private static boolean behaviorsRegistered = false;

    /**
     * Registers fox behaviors from configuration.
     * Creates an AnimalConfig with handles for all fox-specific behaviors.
     */
    @Unique
    private void registerBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        ResourceLocation foxId = ResourceLocation.withDefaultNamespace("fox");

        AnimalConfig config = AnimalConfig.builder(foxId)
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

            // Temporal behaviors - nocturnal pattern
            .addHandle(new TemporalHandle())

            // Predation - both predator and prey
            .addHandle(new PredationHandle())

            // Diet - carnivorous hunter with berry foraging
            .addHandle(new DietHandle())

            .build();

        AnimalBehaviorRegistry.register(foxId, config);
        behaviorsRegistered = true;
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
        // Low health flee: highest priority - retreat when hurt
        goalSelector.addGoal(1, new LowHealthFleeGoal(fox, 0.50, 1.5));

        // Sleep: high priority during day
        goalSelector.addGoal(2, new FoxSleepGoal(pathfinderMob, sleepingBehavior));

        // Hunt: high priority when hungry or at night
        goalSelector.addGoal(3, new FoxHuntGoal(pathfinderMob, pursuitBehavior, 1.2));

        // Forage: medium priority
        goalSelector.addGoal(5, new FoxForageGoal(pathfinderMob, berryForagingBehavior, 5));

        // Pickup items: lower priority
        goalSelector.addGoal(7, new FoxPickupItemGoal(pathfinderMob, itemCarryBehavior, 0.8));
    }
}
