package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.EcologyProfileRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Base mixin for animal-specific behavior registration.
 * Subclasses should override {@link #registerBehaviors()} to register custom behaviors.
 *
 * This mixin provides a template method pattern for code-based animal behavior registration.
 * Subclasses can override the registration methods to customize specific animal types.
 */
@Mixin(Animal.class)
public abstract class AnimalMixin {

    @Unique
    private static boolean behaviorsRegistered = false;

    /**
     * Static initializer for registering animal behaviors.
     * This is called once when the class is loaded.
     *
     * To register behaviors for a specific animal:
     * 1. Create a mixin class that extends this pattern
     * 2. Override the registerBehaviors method
     * 3. Call AnimalBehaviorRegistry.register() with your configuration
     *
     * Example:
     * <pre>
     * {@code
     * @Mixin(Pig.class)
     * public abstract class PigMixin extends AnimalMixin {
     *     @Inject(method = "<init>", at = @At("RETURN"))
     *     private void onInit(CallbackInfo ci) {
     *         registerBehaviors();
     *     }
     *
     *     @Override
     *     protected void registerBehaviors() {
     *         AnimalConfig config = AnimalConfig.builder(EntityType.PIG)
     *             .addHandle(new CustomHungerHandle(20, 1, 100))
     *             .addHandle(new CustomMovementHandle(0.25, true))
     *             .build();
     *         AnimalBehaviorRegistry.register("minecraft:pig", config);
     *     }
     * }
     * }
     * </pre>
     */
    protected void registerBehaviors() {
        // Default implementation does nothing
        // Subclasses should override this to register behaviors
    }

    /**
     * Check if behaviors have been registered for this animal type.
     * This prevents duplicate registrations.
     */
    @Unique
    protected final boolean areBehaviorsRegistered() {
        return behaviorsRegistered;
    }

    /**
     * Mark behaviors as registered for this animal type.
     */
    @Unique
    protected final void markBehaviorsRegistered() {
        behaviorsRegistered = true;
    }

    /**
     * Get the EcologyComponent for this mob instance.
     */
    @Unique
    protected final EcologyComponent getEcologyComponent() {
        Animal animal = (Animal) (Object) this;
        if (!(animal instanceof Mob mob)) {
            return null;
        }
        if (mob instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            return access.betterEcology$getEcologyComponent();
        }
        return null;
    }

    /**
     * Get the EcologyProfile for this mob instance.
     * Returns null if no profile exists.
     */
    @Unique
    @Nullable
    protected final EcologyProfile getEcologyProfile() {
        EcologyComponent component = getEcologyComponent();
        return component != null ? component.profile() : null;
    }

    /**
     * Get the AnimalConfig for this mob instance.
     * Returns null if no code-based configuration exists.
     */
    @Unique
    @Nullable
    protected final AnimalConfig getAnimalConfig() {
        Animal animal = (Animal) (Object) this;
        return animal instanceof Mob mob ? AnimalBehaviorRegistry.getForMob(mob) : null;
    }

    /**
     * Injection point after constructor.
     * Subclasses can inject here to register behaviors.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(EntityType<? extends Animal> entityType, Level level, CallbackInfo ci) {
        // Default implementation does nothing
        // Subclasses can inject here with their own registration logic
    }
}
