package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal that makes animals flee when their health drops below a threshold while in combat.
 *
 * Animals will:
 * - Trigger when health falls below the threshold during combat
 * - Flee away from the attacker
 * - Continue fleeing until health recovers or attacker is far away
 * - Mark themselves as retreating in the entity state
 *
 * Scientific basis:
 * - Prey animals disengage from combat when injured to survive
 * - Low health triggers flight response to escape predators
 * - Retreating behavior conserves energy and reduces injury risk
 */
public class LowHealthFleeGoal extends Goal {
    private final PathfinderMob mob;
    private final double healthThresholdPercent;
    private final double fleeSpeed;
    private final int recentCombatWindowTicks;

    public LowHealthFleeGoal(PathfinderMob mob, double healthThresholdPercent, double fleeSpeed) {
        this.mob = mob;
        this.healthThresholdPercent = healthThresholdPercent;
        this.fleeSpeed = fleeSpeed;
        this.recentCombatWindowTicks = 200;
        // Set goal flags to allow proper goal scheduling
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Check health threshold first
        float currentHealth = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        double healthPercent = maxHealth > 0 ? currentHealth / maxHealth : 0;

        if (healthPercent >= healthThresholdPercent) {
            return false;
        }

        // Check for recent attacker
        LivingEntity attacker = mob.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            long currentTime = mob.level().getGameTime();
            long timeSinceHurt = currentTime - mob.getLastHurtByMobTimestamp();

            if (timeSinceHurt < recentCombatWindowTicks) {
                return true;
            }
        }

        // Also check for nearby hostile targets even without being hit
        // This helps when health is manually lowered or after combat window expires
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive() && mob.distanceToSqr(target) < 24.0 * 24.0) {
            return true;
        }

        // Finally, check for nearby hostile entities or players that might be threatening us
        LivingEntity nearbyThreat = findNearbyThreat();
        return nearbyThreat != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Check health first
        float currentHealth = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        double healthPercent = maxHealth > 0 ? currentHealth / maxHealth : 0;

        if (healthPercent >= healthThresholdPercent) {
            return false;
        }

        // Determine threat entity - prioritize attacker, fall back to target, then nearby threats
        LivingEntity threatEntity = mob.getLastHurtByMob();
        if (threatEntity == null || !threatEntity.isAlive()) {
            threatEntity = mob.getTarget();
        }
        if (threatEntity == null || !threatEntity.isAlive()) {
            threatEntity = findNearbyThreat();
        }

        if (threatEntity == null) {
            return false;
        }

        double distanceToThreat = mob.distanceToSqr(threatEntity);
        double detectionRangeSquared = 24.0 * 24.0;

        return distanceToThreat < detectionRangeSquared;
    }

    @Override
    public void start() {
        if (mob instanceof EcologyAccess access) {
            EntityState entityState = access.betterEcology$getEcologyComponent().state();
            entityState.setIsRetreating(true);
        }

        findFleePosition();
    }

    @Override
    public void stop() {
        if (mob instanceof EcologyAccess access) {
            EntityState entityState = access.betterEcology$getEcologyComponent().state();
            entityState.setIsRetreating(false);
        }

        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Determine who to flee from - prioritize attacker, fall back to target, then nearby threats
        LivingEntity threatEntity = mob.getLastHurtByMob();
        if (threatEntity == null || !threatEntity.isAlive()) {
            threatEntity = mob.getTarget();
        }
        if (threatEntity == null || !threatEntity.isAlive()) {
            threatEntity = findNearbyThreat();
        }

        if (threatEntity == null) {
            return;
        }

        Vec3 fleePos = findFleePositionAwayFrom(threatEntity);
        if (fleePos != null) {
            mob.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, fleeSpeed);
        }

        mob.getLookControl().setLookAt(threatEntity);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Find a position to flee to.
     */
    private void findFleePosition() {
        // Determine who to flee from - prioritize attacker, fall back to target, then nearby threats
        LivingEntity threatEntity = mob.getLastHurtByMob();
        if (threatEntity == null || !threatEntity.isAlive()) {
            threatEntity = mob.getTarget();
        }
        if (threatEntity == null || !threatEntity.isAlive()) {
            threatEntity = findNearbyThreat();
        }

        if (threatEntity == null) {
            return;
        }

        Vec3 fleePos = findFleePositionAwayFrom(threatEntity);
        if (fleePos != null) {
            mob.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, fleeSpeed);
        }
    }

    /**
     * Find a flee position away from the given entity.
     *
     * @param awayFromEntity The entity to flee from
     * @return A position away from the entity, or null if none found
     */
    private Vec3 findFleePositionAwayFrom(LivingEntity awayFromEntity) {
        Vec3 awayFromPos = awayFromEntity.position();
        return DefaultRandomPos.getPosAway(mob, 16, 7, awayFromPos);
    }

    /**
     * Finds nearby threatening entities.
     * Checks for hostile mobs, predators, players, or any entity targeting this mob.
     *
     * @return The nearest threat, or null if none found
     */
    private LivingEntity findNearbyThreat() {
        java.util.List<LivingEntity> nearbyEntities = mob.level().getEntitiesOfClass(
            LivingEntity.class,
            mob.getBoundingBox().inflate(16.0),
            entity -> entity != mob && entity.isAlive()
        );

        for (LivingEntity entity : nearbyEntities) {
            // Check if this entity is targeting us
            if (entity instanceof net.minecraft.world.entity.Mob mobEntity) {
                if (mobEntity.getTarget() == mob) {
                    return entity;
                }
            }

            // Check if this is a known predator type
            if (isPredator(entity)) {
                return entity;
            }

            // Check for nearby players (not sneaking)
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                if (!player.isShiftKeyDown()) {
                    return entity;
                }
            }
        }

        return null;
    }

    /**
     * Checks if an entity is a predator that should trigger fleeing.
     *
     * @param entity The entity to check
     * @return True if the entity is a predator
     */
    private boolean isPredator(LivingEntity entity) {
        String typeName = entity.getType().toString().toLowerCase();
        return typeName.contains("wolf") ||
               typeName.contains("fox") ||
               typeName.contains("cat") ||
               typeName.contains("ocelot") ||
               typeName.contains("spider") ||
               typeName.contains("zombie") ||
               typeName.contains("skeleton") ||
               typeName.contains("creeper");
    }
}
