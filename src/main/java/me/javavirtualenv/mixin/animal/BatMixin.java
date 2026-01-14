package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that adds minimal ecology state tracking for Bat.
 *
 * Bats are AmbientCreatures (extend Mob, not PathfinderMob) with very limited AI capabilities.
 * They cannot use pathfinding-based goals like FleeFromPredatorGoal, so this mixin only
 * initializes and tracks ecology state (hunger/thirst).
 *
 * Note: Bats are nocturnal and rest during the day (vanilla behavior preserved).
 */
@Mixin(Bat.class)
public abstract class BatMixin {

    /**
     * Initialize ecology state when bat is created.
     * Since Bat extends AmbientCreature (not Animal), it doesn't get ecology
     * initialization from MobEcologyMixin, so we handle it here.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$initEcology(EntityType<? extends Bat> entityType, Level level, CallbackInfo ci) {
        Bat bat = (Bat) (Object) this;
        AnimalNeeds.initializeIfNeeded(bat);
    }

    /**
     * Initialize needs if not already done.
     * This mirrors the behavior of MobEcologyMixin but for AmbientCreature (Bat).
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void betterEcology$initializeNeeds(CallbackInfo ci) {
        Bat bat = (Bat) (Object) this;

        if (!AnimalNeeds.isInitialized(bat)) {
            AnimalNeeds.initializeIfNeeded(bat);
        }
    }

    /**
     * Decay hunger and thirst each tick, and apply damage if critical.
     * Also auto-hydrate when in water/rain.
     * Note: customServerAiStep is already server-side only.
     */
    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void betterEcology$tickNeeds(CallbackInfo ci) {
        Bat bat = (Bat) (Object) this;

        // Decay hunger and thirst (bats have lower metabolism than most animals)
        AnimalNeeds.decayHunger(bat, AnimalThresholds.DEFAULT_HUNGER_DECAY * 0.5f);
        AnimalNeeds.decayThirst(bat, AnimalThresholds.DEFAULT_THIRST_DECAY * 0.5f);

        // Auto-hydrate when in water or rain
        if (bat.isInWater() || bat.isInWaterOrRain()) {
            betterEcology$autoHydrate(bat);
        }

        // Apply starvation damage
        if (AnimalNeeds.isStarving(bat)) {
            betterEcology$applyNeedsDamage(bat, "starvation");
        }

        // Apply dehydration damage
        if (AnimalNeeds.isDehydrated(bat)) {
            betterEcology$applyNeedsDamage(bat, "dehydration");
        }
    }

    /**
     * Auto-hydrate the bat when in water or rain.
     */
    @Unique
    private void betterEcology$autoHydrate(Bat bat) {
        // Restore thirst gradually while in water/rain
        if (AnimalNeeds.getThirst(bat) < AnimalNeeds.MAX_VALUE) {
            AnimalNeeds.modifyThirst(bat, AnimalThresholds.DRINKING_THIRST_RESTORE * 0.5f);
        }
    }

    /**
     * Apply damage from starvation or dehydration if enough time has passed.
     */
    @Unique
    private void betterEcology$applyNeedsDamage(Bat bat, String cause) {
        if (AnimalNeeds.canTakeDamage(bat, AnimalThresholds.DEFAULT_DAMAGE_INTERVAL)) {
            DamageSource damageSource = bat.damageSources().starve();
            bat.hurt(damageSource, AnimalThresholds.DEFAULT_DAMAGE);
            AnimalNeeds.setLastDamageTick(bat, bat.level().getGameTime());
        }
    }
}
