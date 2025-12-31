package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.production.SnifferDiggingGoal;
import me.javavirtualenv.behavior.sniffer.*;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Mixin;
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
public abstract class SnifferMixin extends AnimalMixin {

    private SnifferDiggingGoal diggingGoal;
    private SniffingGoal sniffingGoal;
    private SnifferSocialGoal socialGoal;

    @Override
    protected void registerBehaviors() {
        if (areBehaviorsRegistered()) {
            return;
        }

        ResourceLocation snifferId = ResourceLocation.withDefaultNamespace("sniffer");

        AnimalConfig config = AnimalConfig.builder(snifferId)
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

            // Predation - disabled for sniffers
            .addHandle(new PredationHandle())

            // Diet
            .addHandle(new DietHandle())

            // Production - ancient seed/moss production
            .addHandle(new ResourceProductionHandle())

            // Behaviors
            .addHandle(new BehaviorHandle())

            .build();

        AnimalBehaviorRegistry.register(snifferId, config);
        markBehaviorsRegistered();
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
    }

    /**
     * Registers the digging goal for ancient seeds.
     */
    private void registerDiggingGoal() {
        Sniffer sniffer = (Sniffer) (Object) this;

        if (diggingGoal == null) {
            diggingGoal = new SnifferDiggingGoal(sniffer);

            int goalPriority = 5;
            sniffer.goalSelector.addGoal(goalPriority, diggingGoal);
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
            sniffer.goalSelector.addGoal(goalPriority, sniffingGoal);
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
            sniffer.goalSelector.addGoal(goalPriority, socialGoal);
        }
    }
}
