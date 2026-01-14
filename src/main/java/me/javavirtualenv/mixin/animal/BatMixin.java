package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalNeeds;
import me.javavirtualenv.behavior.core.AnimalThresholds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that adds ecology state tracking and nocturnal behavior for Bat.
 *
 * Bats are AmbientCreatures (extend Mob, not PathfinderMob) with very limited AI capabilities.
 * They cannot use pathfinding-based goals like FleeFromPredatorGoal, so this mixin only
 * initializes and tracks ecology state (hunger/thirst) and implements time-based activity.
 *
 * Nocturnal Behavior:
 * - Bats rest during the day (0-12000 ticks)
 * - Bats become active at dusk (12000-13500 ticks) and throughout the night (13500-23000)
 * - Bats seek shelter before dawn (23000-24000 ticks)
 */
@Mixin(Bat.class)
public abstract class BatMixin {

    @Shadow
    public abstract boolean isResting();

    @Shadow
    public abstract void setResting(boolean resting);

    @Unique
    private static final long DAY_START = 0;
    @Unique
    private static final long DUSK_START = 12000;
    @Unique
    private static final long NIGHT_START = 13500;
    @Unique
    private static final long NIGHT_END = 23000;
    @Unique
    private static final long DAY_CYCLE = 24000;

    @Unique
    private int betterEcology$activityCheckCooldown = 0;

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
     * Implements nocturnal behavior - bats rest during day, active at night.
     * Note: customServerAiStep is already server-side only.
     */
    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void betterEcology$tickNeeds(CallbackInfo ci) {
        Bat bat = (Bat) (Object) this;

        // Handle nocturnal activity patterns
        betterEcology$updateNocturnalBehavior(bat);

        // Decay hunger and thirst (bats have lower metabolism than most animals)
        // Bats consume less energy when resting
        float metabolismMultiplier = this.isResting() ? 0.3f : 0.5f;
        AnimalNeeds.decayHunger(bat, AnimalThresholds.DEFAULT_HUNGER_DECAY * metabolismMultiplier);
        AnimalNeeds.decayThirst(bat, AnimalThresholds.DEFAULT_THIRST_DECAY * metabolismMultiplier);

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

    /**
     * Update bat's resting state based on time of day.
     * Bats are nocturnal - they rest during day and are active at night.
     */
    @Unique
    private void betterEcology$updateNocturnalBehavior(Bat bat) {
        // Check activity state periodically to avoid excessive calculations
        if (this.betterEcology$activityCheckCooldown > 0) {
            this.betterEcology$activityCheckCooldown--;
            return;
        }

        // Reset cooldown (check every 20 ticks = 1 second)
        this.betterEcology$activityCheckCooldown = 20;

        long timeOfDay = bat.level().getDayTime() % DAY_CYCLE;
        boolean shouldBeResting = betterEcology$shouldBatRest(timeOfDay);
        boolean currentlyResting = this.isResting();

        // Update resting state if it differs from what it should be
        if (shouldBeResting && !currentlyResting) {
            betterEcology$startResting(bat);
        } else if (!shouldBeResting && currentlyResting) {
            betterEcology$stopResting(bat);
        }
    }

    /**
     * Determine if bat should be resting based on time of day.
     * Bats rest during day (0-12000) and seek shelter before dawn (23000-24000).
     * They are active during dusk/night (12000-23000).
     */
    @Unique
    private boolean betterEcology$shouldBatRest(long timeOfDay) {
        // Day time: 0-12000 ticks (bat should rest)
        if (timeOfDay >= DAY_START && timeOfDay < DUSK_START) {
            return true;
        }

        // Dusk and night: 12000-23000 ticks (bat should be active)
        if (timeOfDay >= DUSK_START && timeOfDay < NIGHT_END) {
            return false;
        }

        // Pre-dawn: 23000-24000 ticks (bat seeks shelter, resting)
        return true;
    }

    /**
     * Make the bat start resting.
     * In vanilla, bats rest when hanging from blocks (like in caves).
     */
    @Unique
    private void betterEcology$startResting(Bat bat) {
        // Only rest if the bat is near a suitable resting location
        // (e.g., has a solid block above or is in a dark area)
        if (betterEcology$canRestAtCurrentLocation(bat)) {
            this.setResting(true);
        }
    }

    /**
     * Make the bat stop resting and become active.
     */
    @Unique
    private void betterEcology$stopResting(Bat bat) {
        this.setResting(false);
    }

    /**
     * Check if the bat can rest at its current location.
     * Bats prefer dark areas with solid blocks above them (like cave ceilings).
     */
    @Unique
    private boolean betterEcology$canRestAtCurrentLocation(Bat bat) {
        BlockPos batPos = bat.blockPosition();
        BlockPos abovePos = batPos.above();

        // Check if there's a solid block above (like a cave ceiling)
        boolean hasCeilingAbove = bat.level().getBlockState(abovePos).isSolid();

        // Check if the area is dark enough (light level < 8)
        int lightLevel = bat.level().getMaxLocalRawBrightness(batPos);
        boolean isDarkEnough = lightLevel < 8;

        // Bat can rest if there's a ceiling above OR if it's dark enough
        // (being lenient to avoid forcing bats into impossible situations)
        return hasCeilingAbove || isDarkEnough;
    }
}
