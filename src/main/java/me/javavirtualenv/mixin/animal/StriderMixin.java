package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.strider.LavaWalkingBehavior;
import me.javavirtualenv.behavior.strider.RidingBehavior;
import me.javavirtualenv.behavior.strider.TemperatureSeekingBehavior;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Strider behavior registration.
 * Striders are Nether-dwelling creatures with the following key characteristics:
 * - Walk on lava surface
 * - Seek warm areas (lava)
 * - Freeze in cold/overworld environments
 * - Can be ridden with warped fungus on a stick
 * - Shiver when cold
 * - Special movement mechanics on lava
 *
 * Strider-specific behaviors:
 * - LavaWalkingBehavior: Enables lava walking and heat resistance
 * - TemperatureSeekingBehavior: Seeks warmth, freezes when cold
 * - RidingBehavior: Handles player riding and steering mechanics
 *
 * Note: Strider extends Monster, not Animal, so this is a standalone mixin.
 */
@Mixin(Strider.class)
public abstract class StriderMixin {

    private static final ResourceLocation STRIDER_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "strider");
    private static boolean behaviorsRegistered = false;

    /**
     * Register strider behaviors using code-based handles.
     */
    private void registerBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(STRIDER_ID)
                // Internal state systems
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new SocialHandle())

                // Physical capabilities
                .addHandle(new MovementHandle())

                // Behavioral systems
                .addHandle(new DietHandle())
                .addHandle(new PredationHandle())
                .addHandle(new BreedingHandle())
                .addHandle(new TemporalHandle())

                // Strider-specific behaviors
                // Note: BehaviorHandle comes from profile via mergeHandles
                .build();

        AnimalBehaviorRegistry.register(STRIDER_ID.toString(), config);
        behaviorsRegistered = true;
    }

    /**
     * Register strider-specific AI goals.
     */
    private void registerStriderGoals(Strider strider) {
        MobAccessor accessor = (MobAccessor) strider;

        // Priority 1: Low health flee - striders flee when health drops below 55%
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(strider, 0.55, 1.2));
    }

    /**
     * Injection point after Strider constructor.
     * Registers strider behaviors once when the first Strider entity is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Strider> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();
        registerStriderGoals((Strider) (Object) this);
    }
}
