package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.behavior.aquatic.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Tropical Fish behavior registration.
 * Tropical fish exhibit:
 * - Schooling behavior
 * - Vibrant colors for attraction
 * - Hide in coral/kelp
 */
@Mixin(TropicalFish.class)
public abstract class TropicalFishMixin {

    private static final ResourceLocation TROPICAL_FISH_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "tropical_fish");
    private static boolean behaviorsRegistered = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends TropicalFish> entityType, Level level, CallbackInfo ci) {
        registerTropicalFishBehaviors();
    }

    private void registerTropicalFishBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(TROPICAL_FISH_ID)
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new SocialHandle())
                .addHandle(new MovementHandle())
                .addHandle(new SizeHandle())
                .addHandle(new BehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(TROPICAL_FISH_ID.toString(), config);
        behaviorsRegistered = true;
    }
}
