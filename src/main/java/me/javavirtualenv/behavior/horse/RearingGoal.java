package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal for horse rearing behavior.
 * <p>
 * Horses rear up when frightened, angry, or excited.
 * This is a natural defensive and social behavior.
 * <p>
 * Rearing triggers:
 * <ul>
 *   <li>Predators nearby (high chance)</li>
 *   <li>Recent damage (moderate chance)</li>
 *   <li>Low health (moderate chance)</li>
 *   <li>Random excitement/play (low chance)</li>
 * </ul>
 */
public class RearingGoal extends Goal {

    // Configuration constants
    private static final String FLEEING_KEY = "fleeing";

    private static final int BASE_REAR_DURATION = 30; // Base ticks to rear
    private static final int REAR_COOLDOWN_TICKS = 200; // Cooldown after rearing
    private static final double PREDATOR_REAR_CHANCE = 0.7; // Chance to rear at predator
    private static final double HURT_REAR_CHANCE = 0.5; // Chance to rear when hurt
    private static final double LOW_HEALTH_THRESHOLD = 0.3; // 30% health triggers fear rear
    private static final double LOW_HEALTH_REAR_CHANCE = 0.4; // Chance to rear at low health
    private static final double RANDOM_REAR_CHANCE = 0.005; // 0.5% per tick for random rear
    private static final double PREDATOR_DETECTION_RANGE = 16.0; // Range to detect predators
    private static final double ZOMBIE_DETECTION_RANGE = 12.0; // Range to detect zombies
    private static final int HURT_REAR_WINDOW_TICKS = 40; // Ticks after hurt to trigger rear
    private static final int MIN_WOLVES_FOR_REAR = 2; // Minimum wolves to trigger rear

    // Instance fields
    private final AbstractHorse horse;
    private final EntityType<?> horseType;

    private int rearDurationTicks;
    private int cooldownTicks;

    // Debug info
    private String lastDebugMessage = "";

    public RearingGoal(AbstractHorse horse) {
        this.horse = horse;
        this.horseType = horse.getType();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (horse.level().isClientSide) {
            return false;
        }

        // Update cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Must be alive
        if (!horse.isAlive()) {
            return false;
        }

        // Cannot rear while being ridden
        if (horse.isVehicle()) {
            return false;
        }

        // Cannot rear if fleeing (should run instead)
        if (isFleeing()) {
            return false;
        }

        // Check for rearing triggers
        return shouldRear();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if rear duration is over
        if (rearDurationTicks <= 0) {
            return false;
        }

        // Stop if being ridden
        if (horse.isVehicle()) {
            return false;
        }

        // Stop if too panicked (should flee instead)
        if (isFleeing()) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        rearDurationTicks = BASE_REAR_DURATION;
        horse.getNavigation().stop();

        // Play rear sound
        horse.level().playSound(null, horse.blockPosition(),
            getRearSound(),
            net.minecraft.sounds.SoundSource.HOSTILE,
            1.0f, 1.0f
        );

        // Create dust particles
        if (!horse.level().isClientSide) {
            for (int i = 0; i < 8; i++) {
                double x = horse.getX() + (horse.getRandom().nextDouble() - 0.5) * horse.getBbWidth();
                double y = horse.getY();
                double z = horse.getZ() + (horse.getRandom().nextDouble() - 0.5) * horse.getBbWidth();
                ((net.minecraft.server.level.ServerLevel) horse.level()).sendParticles(
                    net.minecraft.core.particles.ParticleTypes.POOF,
                    x, y, z,
                    1, 0, 0, 0, 0.02
                );
            }
        }

        debug("STARTING: rear for " + rearDurationTicks + " ticks");
    }

    @Override
    public void stop() {
        rearDurationTicks = 0;
        cooldownTicks = REAR_COOLDOWN_TICKS;
        debug("rear stopped, cooldown=" + cooldownTicks);
    }

    @Override
    public void tick() {
        rearDurationTicks--;

        // Occasional extra rear animation during long rears
        if (rearDurationTicks % 20 == 0 && rearDurationTicks > 0) {
            // Play sound to indicate continued rearing
            horse.level().playSound(null, horse.blockPosition(),
                getRearSound(),
                net.minecraft.sounds.SoundSource.HOSTILE,
                0.5f, 1.2f
            );
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Check if the horse should rear.
     */
    private boolean shouldRear() {
        // Check each trigger in order

        // Trigger 1: Predator nearby
        if (hasNearbyPredator()) {
            if (horse.getRandom().nextFloat() < PREDATOR_REAR_CHANCE) {
                debug("triggered by predator");
                return true;
            }
        }

        // Trigger 2: Recent damage
        if (wasRecentlyHurt()) {
            if (horse.getRandom().nextFloat() < HURT_REAR_CHANCE) {
                debug("triggered by recent damage");
                return true;
            }
        }

        // Trigger 3: Low health (fear response)
        if (isLowHealth()) {
            if (horse.getRandom().nextFloat() < LOW_HEALTH_REAR_CHANCE) {
                debug("triggered by low health");
                return true;
            }
        }

        // Trigger 4: Random excitement (play behavior)
        if (horse.getRandom().nextFloat() < RANDOM_REAR_CHANCE) {
            debug("triggered by random excitement");
            return true;
        }

        return false;
    }

    /**
     * Check if there are nearby predators.
     */
    private boolean hasNearbyPredator() {
        // Check for aggressive wolf packs
        List<net.minecraft.world.entity.animal.Wolf> wolves = horse.level().getEntitiesOfClass(
            net.minecraft.world.entity.animal.Wolf.class,
            horse.getBoundingBox().inflate(PREDATOR_DETECTION_RANGE),
            wolf -> wolf.isAlive() && wolf.isAggressive()
        );

        if (wolves.size() >= MIN_WOLVES_FOR_REAR) {
            return true;
        }

        // Check for zombies
        List<LivingEntity> zombies = horse.level().getEntitiesOfClass(
            LivingEntity.class,
            horse.getBoundingBox().inflate(ZOMBIE_DETECTION_RANGE),
            entity -> entity.getType() == EntityType.ZOMBIE ||
                     entity.getType() == EntityType.DROWNED ||
                     entity.getType() == EntityType.HUSK ||
                     entity.getType() == EntityType.ZOMBIFIED_PIGLIN
        );

        return !zombies.isEmpty();
    }

    /**
     * Check if the horse was recently hurt.
     */
    private boolean wasRecentlyHurt() {
        Entity lastHurtBy = horse.getLastHurtByMob();
        if (lastHurtBy == null) {
            return false;
        }

        long ticksSinceHurt = horse.tickCount - horse.getLastHurtByMobTimestamp();
        return ticksSinceHurt < HURT_REAR_WINDOW_TICKS;
    }

    /**
     * Check if the horse is at low health.
     */
    private boolean isLowHealth() {
        double healthPercent = horse.getHealth() / horse.getMaxHealth();
        return healthPercent < LOW_HEALTH_THRESHOLD;
    }

    /**
     * Get the rear sound for this horse type.
     */
    private net.minecraft.sounds.SoundEvent getRearSound() {
        if (horseType == EntityType.DONKEY || horseType == EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_CHEST;
        } else if (horseType == EntityType.SKELETON_HORSE) {
            return net.minecraft.sounds.SoundEvents.SKELETON_HORSE_AMBIENT;
        } else if (horseType == EntityType.ZOMBIE_HORSE) {
            return net.minecraft.sounds.SoundEvents.ZOMBIE_HORSE_AMBIENT;
        }
        return net.minecraft.sounds.SoundEvents.HORSE_GALLOP;
    }

    /**
     * Check if the horse is currently fleeing.
     */
    private boolean isFleeing() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return false;
        }
        return component.getHandleTag(FLEEING_KEY).getBoolean("is_fleeing");
    }

    /**
     * Get the ecology component for this horse.
     */
    private EcologyComponent getComponent() {
        if (!(horse instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[Rearing] Horse #" + horse.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        double healthPercent = (horse.getHealth() / horse.getMaxHealth()) * 100;
        String typeName = horseType.toShortString();
        return String.format("type=%s, rear=%d/%d, cooldown=%d, health=%.0f%%, fleeing=%b",
            typeName, rearDurationTicks, BASE_REAR_DURATION, cooldownTicks, healthPercent, isFleeing());
    }
}
