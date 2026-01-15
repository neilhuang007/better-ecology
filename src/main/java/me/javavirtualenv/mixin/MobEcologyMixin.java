package me.javavirtualenv.mixin;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.network.EcologyPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
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

        // Store old values for change detection
        float oldHunger = AnimalNeeds.getHunger(animal);
        float oldThirst = AnimalNeeds.getThirst(animal);

        // Decay hunger and thirst
        AnimalNeeds.decayHunger(animal, AnimalThresholds.DEFAULT_HUNGER_DECAY);
        AnimalNeeds.decayThirst(animal, AnimalThresholds.DEFAULT_THIRST_DECAY);

        // Auto-hydrate when in water (touching water, submerged, or standing in water)
        if (animal.isInWater() || animal.isInWaterOrRain() || betterEcology$isStandingInWater(animal)) {
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

        // Sync to clients if values changed significantly (more than 0.5 difference)
        // or periodically every 20 ticks for nearby players
        float newHunger = AnimalNeeds.getHunger(animal);
        float newThirst = AnimalNeeds.getThirst(animal);
        boolean hungerChanged = Math.abs(newHunger - oldHunger) > 0.5f;
        boolean thirstChanged = Math.abs(newThirst - oldThirst) > 0.5f;
        boolean shouldSync = hungerChanged || thirstChanged || (animal.level().getGameTime() % 20 == 0);

        if (shouldSync) {
            EcologyPackets.sendAnimalNeedsToTracking(animal, newHunger, newThirst);
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

    /**
     * Check if the animal is standing in water (feet or body touching water block).
     */
    @Unique
    private boolean betterEcology$isStandingInWater(Animal animal) {
        BlockPos feetPos = animal.blockPosition();
        // Check if the block at feet level or below contains water
        if (animal.level().getFluidState(feetPos).is(FluidTags.WATER)) {
            return true;
        }
        if (animal.level().getFluidState(feetPos.below()).is(FluidTags.WATER)) {
            return true;
        }
        return false;
    }
}
