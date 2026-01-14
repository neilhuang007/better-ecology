package me.javavirtualenv.mixin;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that handles hunger and thirst decay for all Animal entities.
 * This is the central point where needs are updated each tick.
 */
@Mixin(Animal.class)
public abstract class MobEcologyMixin {

    /**
     * Initialize hunger/thirst when the animal first spawns or loads.
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void betterEcology$initializeNeeds(CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;

        if (!AnimalNeeds.isInitialized(animal)) {
            AnimalNeeds.initializeIfNeeded(animal);
        }
    }

    /**
     * Decay hunger and thirst each tick, and apply damage if critical.
     * Also auto-hydrate when standing in water.
     */
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void betterEcology$tickNeeds(CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;

        // Only process on server side
        if (animal.level().isClientSide()) {
            return;
        }

        // Decay hunger and thirst
        AnimalNeeds.decayHunger(animal, AnimalThresholds.DEFAULT_HUNGER_DECAY);
        AnimalNeeds.decayThirst(animal, AnimalThresholds.DEFAULT_THIRST_DECAY);

        // Auto-hydrate when in water (touching water or submerged)
        if (animal.isInWater() || animal.isInWaterOrRain()) {
            betterEcology$autoHydrate(animal);
        }

        // Apply starvation damage
        if (AnimalNeeds.isStarving(animal)) {
            betterEcology$applyNeedsDamage(animal, "starvation");
        }

        // Apply dehydration damage
        if (AnimalNeeds.isDehydrated(animal)) {
            betterEcology$applyNeedsDamage(animal, "dehydration");
        }
    }

    /**
     * Auto-hydrate the animal when standing in water.
     */
    @Unique
    private void betterEcology$autoHydrate(Animal animal) {
        // Restore thirst gradually while in water
        if (AnimalNeeds.getThirst(animal) < AnimalNeeds.MAX_VALUE) {
            AnimalNeeds.modifyThirst(animal, AnimalThresholds.DRINKING_THIRST_RESTORE * 0.5f);
        }
    }

    /**
     * Apply damage from starvation or dehydration if enough time has passed.
     */
    @Unique
    private void betterEcology$applyNeedsDamage(Animal animal, String cause) {
        if (AnimalNeeds.canTakeDamage(animal, AnimalThresholds.DEFAULT_DAMAGE_INTERVAL)) {
            DamageSource damageSource = animal.damageSources().starve();
            animal.hurt(damageSource, AnimalThresholds.DEFAULT_DAMAGE);
            AnimalNeeds.setLastDamageTick(animal, animal.level().getGameTime());
        }
    }
}
