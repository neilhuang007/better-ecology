package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.behavior.aquatic.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Tadpole;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Tadpole behavior registration.
 * Tadpoles exhibit:
 * - Swim to water surface
 * - Metamorphosis to frog over time
 * - Hide in seagrass
 */
@Mixin(Tadpole.class)
public abstract class TadpoleMixin {

    private static final ResourceLocation TADPOLE_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "tadpole");
    private static boolean behaviorsRegistered = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Tadpole> entityType, Level level, CallbackInfo ci) {
        registerTadpoleBehaviors();
    }

    private void registerTadpoleBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(TADPOLE_ID)
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new MovementHandle())
                .addHandle(new SizeHandle())
                .addHandle(new BehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(TADPOLE_ID.toString(), config);
        behaviorsRegistered = true;
    }
}
