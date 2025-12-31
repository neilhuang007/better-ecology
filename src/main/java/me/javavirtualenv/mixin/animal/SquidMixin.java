package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.production.SquidInkProductionGoal;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import me.javavirtualenv.behavior.aquatic.*;
import me.javavirtualenv.behavior.BehaviorRule;
import me.javavirtualenv.behavior.BehaviorWeights;
import me.javavirtualenv.behavior.BehaviorRegistry;
import me.javavirtualenv.behavior.ai.SteeringBehaviorGoal;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.EcologyBootstrap;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Squid behavior registration.
 * Squids exhibit:
 * - Ink cloud release when threatened
 * - Vertical migration (surface to deep)
 * - Schooling behavior with other squid
 * - Ink sac production
 */
@Mixin(Squid.class)
public abstract class SquidMixin {

    private static final ResourceLocation SQUID_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "squid");
    private static boolean behaviorsRegistered = false;
    private SquidInkProductionGoal productionGoal;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Squid> entityType, Level level, CallbackInfo ci) {
        registerSquidBehaviors();
        registerProductionGoal();
    }

    private void registerSquidBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(SQUID_ID)
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new MovementHandle())
                .addHandle(new SizeHandle())
                .addHandle(new ResourceProductionHandle())
                .addHandle(new BehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(SQUID_ID.toString(), config);
        behaviorsRegistered = true;
    }

    /**
     * Registers the ink production goal.
     */
    private void registerProductionGoal() {
        Squid squid = (Squid) (Object) this;

        if (productionGoal == null) {
            productionGoal = new SquidInkProductionGoal(squid);

            int goalPriority = 4;
            squid.goalSelector.addGoal(goalPriority, productionGoal);
        }
    }
}
