package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.behavior.aquatic.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Cod behavior registration.
 * Cod exhibit:
 * - Schooling behavior (boids algorithm)
 * - Predator avoidance
 * - Feeding on small aquatic animals
 */
@Mixin(Cod.class)
public abstract class CodMixin {

    private static final ResourceLocation COD_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "cod");
    private static boolean behaviorsRegistered = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Cod> entityType, Level level, CallbackInfo ci) {
        registerCodBehaviors();
    }

    private void registerCodBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(COD_ID)
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new SocialHandle())
                .addHandle(new MovementHandle())
                .addHandle(new SizeHandle())
                .addHandle(new BehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(COD_ID.toString(), config);
        behaviorsRegistered = true;
    }
}
