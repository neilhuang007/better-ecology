package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.production.SnifferDiggingGoal;
import me.javavirtualenv.behavior.sniffer.*;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sniffer-specific behavior and production registration.
 * <p>
 * Sniffers:
 * - Are ancient creatures that dig for seeds
 * - Can find torchflower seeds and pitcher pods
 * - Produce ancient moss and rare ancient seeds
 * - Have limited daily digging capacity
 * - Use enhanced smell detection to locate seeds
 * - Parents teach babies to dig
 * - Share discoveries with other sniffers
 * - Remember good digging spots
 */
@Mixin(Sniffer.class)
public abstract class SnifferMixin {

    @Unique
    private static boolean behaviorsRegistered = false;

    @Unique
    private SnifferDiggingGoal diggingGoal;
    @Unique
    private SniffingGoal sniffingGoal;
    @Unique
    private SnifferSocialGoal socialGoal;
    @Unique
    private LowHealthFleeGoal fleeGoal;

    @Unique
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation snifferId = ResourceLocation.withDefaultNamespace("sniffer");

        AnimalConfig config = AnimalConfig.builder(snifferId)
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

            // Predation - disabled for sniffers
            .addHandle(new PredationHandle())

            // Diet
            .addHandle(new DietHandle())

            // Production - ancient seed/moss production
            .addHandle(new ResourceProductionHandle())

            // Behaviors - Note: BehaviorHandle comes from profile via mergeHandles

            .build();

        AnimalBehaviorRegistry.register(snifferId, config);
        markBehaviorsRegistered();
    }

    /**
     * Check if behaviors have been registered for this animal type.
     * This prevents duplicate registrations.
     */
    @Unique
    protected boolean areBehaviorsRegistered() {
        return behaviorsRegistered;
    }

    /**
     * Mark behaviors as registered for this animal type.
     */
    @Unique
    protected void markBehaviorsRegistered() {
        behaviorsRegistered = true;
    }

    /**
     * Inject after Sniffer constructor to register behaviors and goals.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        registerBehaviors();
        registerDiggingGoal();
        registerSniffingGoal();
        registerSocialGoal();
        registerFleeGoal();
    }

    /**
     * Registers the digging goal for ancient seeds.
     */
    private void registerDiggingGoal() {
        Sniffer sniffer = (Sniffer) (Object) this;

        if (diggingGoal == null) {
            diggingGoal = new SnifferDiggingGoal(sniffer);

            int goalPriority = 5;
            ((MobAccessor) sniffer).betterEcology$getGoalSelector().addGoal(goalPriority, diggingGoal);
        }
    }

    /**
     * Registers the sniffing goal for enhanced smell detection.
     */
    private void registerSniffingGoal() {
        Sniffer sniffer = (Sniffer) (Object) this;

        if (sniffingGoal == null) {
            sniffingGoal = new SniffingGoal(sniffer);

            int goalPriority = 4;
            ((MobAccessor) sniffer).betterEcology$getGoalSelector().addGoal(goalPriority, sniffingGoal);
        }
    }

    /**
     * Registers the social goal for teaching and communication.
     */
    private void registerSocialGoal() {
        Sniffer sniffer = (Sniffer) (Object) this;

        if (socialGoal == null) {
            socialGoal = new SnifferSocialGoal(sniffer);

            int goalPriority = 6;
            ((MobAccessor) sniffer).betterEcology$getGoalSelector().addGoal(goalPriority, socialGoal);
        }
    }

    /**
     * Registers the flee goal for low health situations.
     */
    private void registerFleeGoal() {
        Sniffer sniffer = (Sniffer) (Object) this;

        if (fleeGoal == null) {
            fleeGoal = new LowHealthFleeGoal(sniffer, 0.50, 1.0);

            int goalPriority = 1;
            ((MobAccessor) sniffer).betterEcology$getGoalSelector().addGoal(goalPriority, fleeGoal);
        }
    }
}
