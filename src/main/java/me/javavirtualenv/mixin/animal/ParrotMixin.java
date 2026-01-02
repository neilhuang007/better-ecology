package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingHandle;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Parrot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Parrot-specific behavior registration with comprehensive special behaviors.
 * <p>
 * Parrots are unique tropical birds that:
 * - Mimic hostile mob sounds as a warning system for players
 * - Dance to music from jukeboxes and note blocks with different styles
 * - Detect music from 64 blocks away and fly toward it
 * - Perch on high locations and player shoulders
 * - Perform short flights and gliding movements
 * - Build nests in tree canopies using leaves, sticks, feathers, and wool
 * - Prefer tall trees for nesting (high nesting behavior)
 * - Use colorful materials to attract mates
 * - Are ground-dwelling birds that can fly up to trees
 * - Are herbivores that primarily eat seeds
 * - Can be tamed with seeds
 * <p>
 * Special behaviors implemented:
 * <b>Mimicking System:</b>
 * - Mimic hostile mob sounds (creeper, zombie, skeleton, spider, etc.)
 * - Mimic as warning system when hostiles are nearby
 * - Random mimicking when idle
 * - Different mimic accuracy based on individual parrot
 * - Adjustable accuracy through configuration
 * <p>
 * <b>Dancing Behavior:</b>
 * - Dance to music from jukeboxes with different styles per disc
 * - Dance styles: BOUNCE, SPIN, WIGGLE, HEAD_BOB, WING_FLAP, PARTY, RAVE, DISCO
 * - Party effect - other parrots join in the dancing
 * - Note particle effects while dancing
 * - Perches near music players to dance
 * <p>
 * <b>Music Detection:</b>
 * - Detect music from 64 block radius
 * - Fly toward music source (jukebox or note block)
 * - Different dance styles for different music discs
 * - Perch near music players
 * <p>
 * <b>Flight and Perching:</b>
 * - Perch on player shoulders (tamed parrots)
 * - Fly in short bursts between perches
 * - Prefer high perches (tree branches, buildings)
 * - Glide to ground safely
 * - Seek isolated perches away from threats
 * <p>
 * <b>Nest Building:</b>
 * - Search for tall trees with dense canopies
 * - Gather sticks, feathers, and wool for nest construction
 * - Build nests high in tree branches (leaves/wood blocks)
 * - Return to nest periodically for maintenance
 * - Defend nest from other parrots and predators
 * <p>
 * This mixin registers all behaviors and configurations defined in the
 * parrot JSON config.
 */
@Mixin(Parrot.class)
public abstract class ParrotMixin {

    @Unique
    private static boolean behaviorsRegistered = false;

    /**
     * Registers parrot behaviors with comprehensive special behavior support.
     * Creates an AnimalConfig with handles for all parrot-specific behaviors.
     * <p>
     * Handles registered:
     * <b>Core Attributes:</b>
     * - Health: 3 HP base
     * - Movement: 0.4 fly speed, can perch on blocks
     * - Size: Small bird size
     * <p>
     * <b>Internal State:</b>
     * - Hunger: 80 max, 0.02 decay rate
     * - Thirst: Water needs tracking
     * - Condition: Body condition that affects breeding
     * - Energy: Energy for flying and nest building
     * - Age: 24000 tick baby duration
     * - Social: Flock behavior, gets lonely when alone
     * <p>
     * <b>Reproduction:</b>
     * - Breeding: 6000 tick cooldown, min condition 60
     * - NestBuilding: Tree nest building with colorful materials
     * <p>
     * <b>Behaviors:</b>
     * - Temporal: Daily cycle activity, active during day
     * - Predation: Prey of many predators - flees to trees
     * - Diet: Seeds (cookie poisoning behavior handled separately)
     * - ParrotBehavior: Special parrot behaviors (mimic, dance, perch, music)
     */
    @Unique
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation parrotId = ResourceLocation.withDefaultNamespace("parrot");

        AnimalConfig config = AnimalConfig.builder(parrotId)
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

            // Temporal behaviors
            .addHandle(new TemporalHandle())

            // Predation - parrots are prey
            .addHandle(new PredationHandle())

            // Diet - seed eaters
            .addHandle(new DietHandle())

            // Parrot-specific special behaviors (mimic, dance, perch, music)
            .addHandle(new ParrotBehaviorHandle())

            // Note: BehaviorHandle comes from profile via mergeHandles

            .build();

        AnimalBehaviorRegistry.register(parrotId, config);
        setBehaviorsRegistered();
    }

    /**
     * Check if behaviors have been registered for this animal type.
     * This prevents duplicate registrations.
     */
    @Unique
    private boolean areBehaviorsRegistered() {
        return behaviorsRegistered;
    }

    /**
     * Mark behaviors as registered for this animal type.
     */
    @Unique
    private void setBehaviorsRegistered() {
        behaviorsRegistered = true;
    }

    /**
     * Inject after Parrot constructor to register behaviors.
     * Uses TAIL to ensure parrot is fully initialized.
     * ParrotBehaviorHandle registers all special behavior goals.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();

        Parrot parrot = (Parrot) (Object) this;
        MobAccessor accessor = (MobAccessor) parrot;

        // Priority 1: Low health flee (parrots are fragile and flee early)
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(parrot, 0.75, 1.6));

        // All parrot-specific goals are registered by ParrotBehaviorHandle
        // - MimicGoal: Handles mimicking sounds and warning mimics
        // - MusicGoal: Handles music detection and dancing
        // - PerchGoal: Handles perching and flight behavior
        // - ParrotBehaviorGoal: Orchestrates all behaviors together
    }
}
