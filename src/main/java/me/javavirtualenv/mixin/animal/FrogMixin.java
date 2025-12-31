package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.frog.CroakingBehavior;
import me.javavirtualenv.behavior.frog.FrogHandle;
import me.javavirtualenv.behavior.frog.FrogJumpingBehavior;
import me.javavirtualenv.behavior.frog.FrogSwimmingBehavior;
import me.javavirtualenv.behavior.frog.TongueAttackBehavior;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.AgeHandle;
import me.javavirtualenv.ecology.handles.BehaviorHandle;
import me.javavirtualenv.ecology.handles.BreedingHandle;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import me.javavirtualenv.ecology.handles.DietHandle;
import me.javavirtualenv.ecology.handles.EggLayerHandle;
import me.javavirtualenv.ecology.handles.EnergyHandle;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.handles.MovementHandle;
import me.javavirtualenv.ecology.handles.SizeHandle;
import me.javavirtualenv.ecology.handles.SocialHandle;
import me.javavirtualenv.ecology.handles.TemporalHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Frog behavior registration.
 * <p>
 * Frogs are amphibians with unique behaviors:
 * - Tongue attack to catch small prey (slimes, magma cubes, insects)
 * - Croaking for communication and mating calls
 * - Efficient swimming in water
 * - Powerful jumping between lily pads
 * - Biome-specific variants (temperate, warm, cold)
 * - Lay frogspawn in water that hatches into tadpoles
 * <p>
 * Scientific basis:
 * - Frogs are ambush predators that use rapid tongue extension
 * - Vocalization patterns vary by species and environment
 * - Specialized for both aquatic and terrestrial locomotion
 * - Complex life cycle with metamorphosis
 * <p>
 * Frog-specific behaviors:
 * - TongueAttackBehavior: Long-range tongue attacks with visual feedback
 * - CroakingBehavior: Context-aware croaking (mating, territorial, communication)
 * - FrogSwimmingBehavior: Surface preference and vegetation attraction
 * - FrogJumpingBehavior: Targeted jumps to lily pads and water
 * - FrogHandle: Integrates behaviors with the ecology system
 */
@Mixin(Frog.class)
public abstract class FrogMixin extends AnimalMixin {

    private static final ResourceLocation FROG_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "frog");
    private static boolean behaviorsRegistered = false;

    /**
     * Register frog behaviors using code-based handles.
     */
    @Override
    protected void registerBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(FROG_ID)
                // Internal state systems
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new SocialHandle())

                // Physical capabilities
                .addHandle(new MovementHandle())
                .addHandle(new SizeHandle())

                // Behavioral systems
                .addHandle(new DietHandle())
                .addHandle(new BreedingHandle())
                .addHandle(new TemporalHandle())
                .addHandle(new EggLayerHandle())

                // Frog-specific behaviors
                .addHandle(new FrogHandle())
                .addHandle(new BehaviorHandle())

                // Build configuration
                .build();

        AnimalBehaviorRegistry.register(FROG_ID.toString(), config);
        behaviorsRegistered = true;
    }

    /**
     * Injection point after Frog constructor.
     * Registers frog behaviors once when the first Frog entity is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Animal> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();
    }
}
