package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

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
    }

    @Override
    public boolean canUse() {
        LivingEntity attacker = mob.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive()) {
            return false;
        }

        long currentTime = mob.level().getGameTime();
        long timeSinceHurt = currentTime - mob.getLastHurtByMobTimestamp();

        if (timeSinceHurt >= recentCombatWindowTicks) {
            return false;
        }

        float currentHealth = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        double healthPercent = maxHealth > 0 ? currentHealth / maxHealth : 0;

        return healthPercent < healthThresholdPercent;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity attacker = mob.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive()) {
            return false;
        }

        float currentHealth = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        double healthPercent = maxHealth > 0 ? currentHealth / maxHealth : 0;

        if (healthPercent >= healthThresholdPercent) {
            return false;
        }

        double distanceToAttacker = mob.distanceToSqr(attacker);
        double detectionRangeSquared = 24.0 * 24.0;

        return distanceToAttacker < detectionRangeSquared;
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
        LivingEntity attacker = mob.getLastHurtByMob();
        if (attacker == null) {
            return;
        }

        Vec3 fleePos = findFleePositionAwayFrom(attacker);
        if (fleePos != null) {
            mob.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, fleeSpeed);
        }

        mob.getLookControl().setLookAt(attacker);
    }

    /**
     * Find a position to flee to.
     */
    private void findFleePosition() {
        LivingEntity attacker = mob.getLastHurtByMob();
        if (attacker == null) {
            return;
        }

        Vec3 fleePos = findFleePositionAwayFrom(attacker);
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
}
