package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.behavior.aquatic.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Salmon behavior registration.
 * Salmon exhibit:
 * - Schooling behavior
 * - Upstream spawning migration
 * - Predator avoidance
 */
@Mixin(Salmon.class)
public abstract class SalmonMixin {

    private static final ResourceLocation SALMON_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "salmon");
    private static boolean behaviorsRegistered = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Salmon> entityType, Level level, CallbackInfo ci) {
        registerSalmonBehaviors();
    }

    private void registerSalmonBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(SALMON_ID)
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new SocialHandle())
                .addHandle(new MovementHandle())
                // Note: BehaviorHandle comes from profile via mergeHandles
                .build();

        AnimalBehaviorRegistry.register(SALMON_ID.toString(), config);
        behaviorsRegistered = true;
    }

}
