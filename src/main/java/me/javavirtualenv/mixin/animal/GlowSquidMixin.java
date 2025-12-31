package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.production.SquidInkProductionGoal;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import me.javavirtualenv.behavior.aquatic.*;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.GlowSquid;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Glow Squid behavior registration.
 * Glow squids exhibit:
 * - Ink cloud release when threatened
 * - Vertical migration (surface to deep)
 * - Glowing particles attract prey
 * - Glow ink sac production
 */
@Mixin(GlowSquid.class)
public abstract class GlowSquidMixin {

    private static final ResourceLocation GLOW_SQUID_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "glow_squid");
    private static boolean behaviorsRegistered = false;
    private SquidInkProductionGoal productionGoal;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends GlowSquid> entityType, Level level, CallbackInfo ci) {
        registerGlowSquidBehaviors();
        registerProductionGoal();
    }

    private void registerGlowSquidBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(GLOW_SQUID_ID)
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new MovementHandle())
                .addHandle(new SizeHandle())
                .addHandle(new ResourceProductionHandle())
                .addHandle(new BehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(GLOW_SQUID_ID.toString(), config);
        behaviorsRegistered = true;
    }

    /**
     * Registers the glow ink production goal.
     */
    private void registerProductionGoal() {
        GlowSquid glowSquid = (GlowSquid) (Object) this;

        if (productionGoal == null) {
            productionGoal = new SquidInkProductionGoal(glowSquid);

            int goalPriority = 4;
            glowSquid.goalSelector.addGoal(goalPriority, productionGoal);
        }
    }
}
