package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.MilkProductionHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Goat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Goat behavior registration.
 * Goats are mountain-dwelling herd animals with the following key characteristics:
 * - Herbivores that eat grass, wheat, and various plants
 * - Social animals that prefer groups (herds)
 * - Diurnal activity pattern
 * - Excellent jumpers and climbers
 * - Can produce milk when fed wheat
 * - Ramming behavior when provoked
 *
 * Goat-specific behaviors:
 * - MilkProductionHandle: Goats produce milk with faster regeneration than cows
 * - High jumping ability and cliff navigation
 * - Ramming attack when provoked
 */
@Mixin(Goat.class)
public abstract class GoatMixin extends AnimalMixin {

    private static final ResourceLocation GOAT_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "goat");
    private static boolean behaviorsRegistered = false;

    /**
     * Register goat behaviors using the YAML profile configuration.
     * The goat YAML defines: hunger, condition, energy, age, social systems,
     * movement capabilities, predation responses, breeding, temporal behaviors,
     * and milk production.
     */
    @Override
    protected void registerBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(GOAT_ID)
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
                .addHandle(new PredationHandle())
                .addHandle(new BreedingHandle())
                .addHandle(new TemporalHandle())

                // Production systems
                .addHandle(new MilkProductionHandle())
                .build();

        AnimalBehaviorRegistry.register(GOAT_ID.toString(), config);
        behaviorsRegistered = true;
    }

    /**
     * Injection point after Goat constructor.
     * Registers goat behaviors once when the first Goat entity is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Animal> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();
    }
}
