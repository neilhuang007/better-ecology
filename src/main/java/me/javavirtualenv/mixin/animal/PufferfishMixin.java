package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.behavior.aquatic.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Pufferfish behavior registration.
 * Pufferfish exhibit:
 * - Inflate when threatened
 * - Poisonous when inflated
 * - Slow movement normally, quick bursts when fleeing
 */
@Mixin(Pufferfish.class)
public abstract class PufferfishMixin {

    private static final ResourceLocation PUFFERFISH_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "pufferfish");
    private static boolean behaviorsRegistered = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Pufferfish> entityType, Level level, CallbackInfo ci) {
        registerPufferfishBehaviors();
    }

    private void registerPufferfishBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(PUFFERFISH_ID)
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new MovementHandle())
                .addHandle(new SizeHandle())
                .addHandle(new BehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(PUFFERFISH_ID.toString(), config);
        behaviorsRegistered = true;
    }
}
