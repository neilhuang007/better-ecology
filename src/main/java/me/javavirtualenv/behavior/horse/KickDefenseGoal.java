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
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal for horse kick defense behavior.
 * <p>
 * Horses kick behind them when threatened from the rear.
 * This is a natural defensive behavior that protects them from predators.
 * <p>
 * Kick strength varies by horse type:
 * <ul>
 *   <li>Regular horses: standard kick</li>
 *   <li>Donkeys: 1.3x damage and knockback</li>
 *   <li>Mules: 1.5x damage and knockback (strongest)</li>
 * </ul>
 */
public class KickDefenseGoal extends Goal {

    // Configuration constants
    private static final String FLEEING_KEY = "fleeing";

    private static final double KICK_RANGE = 3.0; // Range to kick
    private static final double DETECTION_RANGE = 4.0; // Detection range
    private static final double BEHIND_DOT_THRESHOLD = -0.3; // Dot product threshold for "behind"
    private static final int WARMUP_DURATION_TICKS = 15; // Ticks to wind up kick
    private static final int COOLDOWN_TICKS = 60; // Cooldown after kick
    private static final double BASE_DAMAGE = 4.0; // Base kick damage
    private static final double KNOCKBACK_STRENGTH = 0.6; // Knockback multiplier

    // Horse type modifiers
    private static final double DONKEY_DAMAGE_MULTIPLIER = 1.3;
    private static final double DONKEY_KNOCKBACK_MULTIPLIER = 0.7;
    private static final double MULE_DAMAGE_MULTIPLIER = 1.5;
    private static final double MULE_KNOCKBACK_MULTIPLIER = 0.8;

    // Instance fields
    private final AbstractHorse horse;
    private final boolean isDonkey;
    private final boolean isMule;
    private final double damageMultiplier;
    private final double knockbackMultiplier;

    private LivingEntity kickTarget;
    private int warmupTicks;
    private int cooldownTicks;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasKickingLastCheck = false;

    public KickDefenseGoal(AbstractHorse horse) {
        this.horse = horse;

        // Determine horse type for kick strength
        EntityType<?> type = horse.getType();
        this.isDonkey = type == EntityType.DONKEY;
        this.isMule = type == EntityType.MULE;

        if (isMule) {
            this.damageMultiplier = MULE_DAMAGE_MULTIPLIER;
            this.knockbackMultiplier = MULE_KNOCKBACK_MULTIPLIER;
        } else if (isDonkey) {
            this.damageMultiplier = DONKEY_DAMAGE_MULTIPLIER;
            this.knockbackMultiplier = DONKEY_KNOCKBACK_MULTIPLIER;
        } else {
            this.damageMultiplier = 1.0;
            this.knockbackMultiplier = KNOCKBACK_STRENGTH;
        }

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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

        // Cannot kick while being ridden
        if (horse.isVehicle()) {
            return false;
        }

        // Cannot kick while fleeing
        if (isFleeing()) {
            return false;
        }

        // Find threats from behind
        kickTarget = findRearThreat();

        if (kickTarget == null) {
            // Log state change
            if (wasKickingLastCheck) {
                debug("no rear threats detected");
                wasKickingLastCheck = false;
            }
            return false;
        }

        wasKickingLastCheck = true;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (kickTarget == null || !kickTarget.isAlive()) {
            debug("target no longer valid");
            return false;
        }

        if (horse.isVehicle()) {
            debug("horse being ridden, canceling kick");
            return false;
        }

        if (isFleeing()) {
            debug("fleeing, canceling kick");
            return false;
        }

        // Check if target is still behind and in range
        double distance = horse.distanceToSqr(kickTarget);
        if (!isTargetBehind(kickTarget) || distance > KICK_RANGE * KICK_RANGE) {
            debug("target moved out of kick position");
            return false;
        }

        return warmupTicks < WARMUP_DURATION_TICKS;
    }

    @Override
    public void start() {
        warmupTicks = 0;
        horse.getNavigation().stop();
        String targetName = kickTarget.getType().toShortString();
        debug("STARTING: kick windup at " + targetName);
    }

    @Override
    public void stop() {
        kickTarget = null;
        warmupTicks = 0;
        cooldownTicks = COOLDOWN_TICKS;
        debug("kick stopped, cooldown started");
    }

    @Override
    public void tick() {
        if (kickTarget == null) {
            return;
        }

        // Look at the target
        horse.getLookControl().setLookAt(kickTarget, 30.0f, 30.0f);

        warmupTicks++;

        // Check if target is still behind and in range
        double distance = horse.distanceToSqr(kickTarget);
        if (!isTargetBehind(kickTarget) || distance > KICK_RANGE * KICK_RANGE) {
            stop();
            return;
        }

        // Execute kick when warmup completes
        if (warmupTicks >= WARMUP_DURATION_TICKS) {
            executeKick();
            stop();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Find a threat behind the horse.
     */
    private LivingEntity findRearThreat() {
        Vec3 horsePos = horse.position();
        Vec3 horseLook = horse.getLookAngle();

        // Search area behind horse
        AABB searchArea = new AABB(
            horsePos.x - DETECTION_RANGE,
            horsePos.y - 1,
            horsePos.z - DETECTION_RANGE,
            horsePos.x + DETECTION_RANGE,
            horsePos.y + 2,
            horsePos.z + DETECTION_RANGE
        );

        List<LivingEntity> nearbyEntities = horse.level().getEntitiesOfClass(
            LivingEntity.class,
            searchArea,
            entity -> entity != horse && entity.isAlive() && isThreat(entity)
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity entity : nearbyEntities) {
            if (isTargetBehind(entity)) {
                double distance = horse.distanceToSqr(entity);
                if (distance < bestScore) {
                    bestScore = distance;
                    bestTarget = entity;
                }
            }
        }

        return bestTarget;
    }

    /**
     * Check if an entity is behind the horse.
     */
    private boolean isTargetBehind(Entity target) {
        Vec3 toTarget = target.position().subtract(horse.position()).normalize();
        Vec3 horseLook = horse.getLookAngle();

        // Target is behind if dot product is negative
        double dot = toTarget.dot(horseLook);
        return dot < BEHIND_DOT_THRESHOLD;
    }

    /**
     * Check if an entity is a threat.
     */
    private boolean isThreat(Entity entity) {
        EntityType<?> type = entity.getType();

        // Players are threats if attacking
        if (type == EntityType.PLAYER) {
            return horse.getLastHurtByMob() == entity || horse.getTarget() == entity;
        }

        // Predators are always threats
        if (type == EntityType.WOLF ||
            type == EntityType.POLAR_BEAR ||
            type == EntityType.ZOMBIE ||
            type == EntityType.DROWNED ||
            type == EntityType.VINDICATOR ||
            type == EntityType.EVOKER ||
            type == EntityType.PILLAGER ||
            type == EntityType.RAVAGER) {
            return true;
        }

        // Hostile to this horse
        return horse.getLastHurtByMob() == entity;
    }

    /**
     * Execute the kick attack.
     */
    private void executeKick() {
        if (kickTarget == null) {
            return;
        }

        // Play kick sound
        var sound = getKickSound();
        horse.level().playSound(null, horse.blockPosition(), sound,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);

        // Calculate kick damage
        double damage = BASE_DAMAGE * damageMultiplier;

        // Apply knockback and damage
        Vec3 kickDirection = horse.position().subtract(kickTarget.position()).normalize();
        Vec3 kickVelocity = kickDirection.scale(knockbackMultiplier);

        kickTarget.hurt(horse.level().damageSources().mobAttack(horse), (float) damage);
        kickTarget.setDeltaMovement(kickTarget.getDeltaMovement().add(kickVelocity));
        kickTarget.hurtMarked = true;

        // Additional knockback
        kickTarget.knockback(0.5f, kickDirection.x, kickDirection.z);

        // Create particle effect
        if (!horse.level().isClientSide) {
            for (int i = 0; i < 10; i++) {
                double x = horse.getX() + (horse.getRandom().nextDouble() - 0.5) * 0.5;
                double y = horse.getY() + horse.getBbHeight() * 0.5;
                double z = horse.getZ() + (horse.getRandom().nextDouble() - 0.5) * 0.5;
                ((net.minecraft.server.level.ServerLevel) horse.level()).sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CLOUD,
                    x, y, z,
                    1, 0, 0, 0, 0.02
                );
            }
        }

        String targetName = kickTarget.getType().toShortString();
        debug("kicked " + targetName + " for " + String.format("%.1f", damage) + " damage");
    }

    /**
     * Get the kick sound for this horse type.
     */
    private net.minecraft.sounds.SoundEvent getKickSound() {
        if (isDonkey || isMule) {
            return net.minecraft.sounds.SoundEvents.DONKEY_CHEST;
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
            String prefix = "[KickDefense] Horse #" + horse.getId() + " ";
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
        String targetName = kickTarget != null ? kickTarget.getType().toShortString() : "none";
        String typeName = isDonkey ? "donkey" : isMule ? "mule" : "horse";
        return String.format("type=%s, target=%s, warmup=%d/%d, cooldown=%d, fleeing=%b",
            typeName, targetName, warmupTicks, WARMUP_DURATION_TICKS, cooldownTicks, isFleeing());
    }
}
