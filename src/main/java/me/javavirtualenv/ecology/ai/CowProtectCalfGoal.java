package me.javavirtualenv.ecology.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * AI goal for mother cows to protect their calves from predators.
 *
 * Mother cows will:
 * - Stay close to calves
 * - Position themselves between calves and threats
 * - Charge at predators that get too close
 * - Alert the herd with alarm calls when threats are detected
 *
 * Scientific basis:
 * - Cattle show strong maternal protective behavior
 * - Mothers will aggressively defend calves from predators
 * - Protective aggression is strongest in first 3 months
 * - Cows form defensive circles around calves when threatened
 */
public class CowProtectCalfGoal extends Goal {
    private final Mob mother;
    private final Level level;
    private final double protectionRange;
    private final double detectRange;
    private final double chargeSpeed;
    private LivingEntity currentThreat;
    private AgeableMob currentCalf;
    private int alarmCooldown;
    private int chargeCooldown;

    public CowProtectCalfGoal(Mob mother, double protectionRange, double detectRange) {
        this.mother = mother;
        this.level = mother.level();
        this.protectionRange = protectionRange;
        this.detectRange = detectRange;
        this.chargeSpeed = 1.3;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.alarmCooldown = 0;
        this.chargeCooldown = 0;
    }

    @Override
    public boolean canUse() {
        // Only adult females protect calves
        if (mother.isBaby()) {
            return false;
        }

        // Find nearest calf
        currentCalf = findNearestCalf();
        if (currentCalf == null) {
            return false;
        }

        // Check for threats to calf
        currentThreat = findThreatToCalf(currentCalf);
        return currentThreat != null && currentThreat.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (currentCalf == null || !currentCalf.isAlive()) {
            return false;
        }

        if (currentThreat == null || !currentThreat.isAlive()) {
            return false;
        }

        double distanceToCalf = mother.distanceToSqr(currentCalf);
        double distanceToThreat = mother.distanceToSqr(currentThreat);

        // Continue while threat is close to calf or we're charging
        return distanceToCalf < protectionRange * protectionRange ||
               distanceToThreat < detectRange * detectRange;
    }

    @Override
    public void start() {
        // Emit alarm call
        if (alarmCooldown <= 0) {
            emitAlarmCall();
            alarmCooldown = 200; // 10 seconds
        }
    }

    @Override
    public void stop() {
        currentThreat = null;
        currentCalf = null;
        mother.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (currentCalf == null || currentThreat == null) {
            return;
        }

        Vec3 calfPos = currentCalf.position();
        Vec3 threatPos = currentThreat.position();
        Vec3 motherPos = mother.position();

        // Calculate ideal protective position (between calf and threat)
        Vec3 protectivePos = calculateProtectivePosition(motherPos, calfPos, threatPos);

        double distanceToThreat = mother.distanceToSqr(currentThreat);
        double distanceToCalf = mother.distanceToSqr(currentCalf);

        // Face the threat
        mother.getLookControl().setLookAt(currentThreat);

        // If threat is very close and we're ready, charge
        if (distanceToThreat < 16.0 && chargeCooldown <= 0) {
            chargeThreat();
            chargeCooldown = 100; // 5 seconds between charges
            return;
        }

        // Otherwise, move to protective position
        if (distanceToCalf > 9.0 || distanceToThreat < 36.0) {
            moveToProtectivePosition(protectivePos);
        }
    }

    /**
     * Find the nearest calf within protection range.
     */
    private AgeableMob findNearestCalf() {
        List<AgeableMob> nearbyCalves = level.getEntitiesOfClass(
                AgeableMob.class,
                mother.getBoundingBox().inflate(protectionRange),
                calf -> calf.isBaby() && calf.isAlive() && isSameSpecies(mother, calf)
        );

        if (nearbyCalves.isEmpty()) {
            return null;
        }

        AgeableMob nearestCalf = null;
        double nearestDistance = Double.MAX_VALUE;

        for (AgeableMob calf : nearbyCalves) {
            double distance = mother.distanceToSqr(calf);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestCalf = calf;
            }
        }

        return nearestCalf;
    }

    /**
     * Find a threatening entity near the calf.
     */
    private LivingEntity findThreatToCalf(AgeableMob calf) {
        // Check for predators, hostile mobs, and players
        TargetingConditions threatConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight()
                .selector(this::isThreat);

        List<LivingEntity> nearbyEntities = level.getNearbyEntities(
                LivingEntity.class,
                threatConditions,
                calf,
                calf.getBoundingBox().inflate(detectRange)
        );

        if (nearbyEntities.isEmpty()) {
            return null;
        }

        LivingEntity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : nearbyEntities) {
            double distance = calf.distanceToSqr(entity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestThreat = entity;
            }
        }

        return nearestThreat;
    }

    /**
     * Calculate ideal position between calf and threat.
     */
    private Vec3 calculateProtectivePosition(Vec3 motherPos, Vec3 calfPos, Vec3 threatPos) {
        // Vector from calf to threat
        Vec3 calfToThreat = threatPos.subtract(calfPos).normalize();

        // Position at 30% of distance from calf to threat (closer to calf)
        Vec3 protectivePos = calfPos.add(calfToThreat.scale(3.0));

        // Ensure position is on ground
        protectivePos = new Vec3(protectivePos.x, motherPos.y, protectivePos.z);

        return protectivePos;
    }

    /**
     * Move to the protective position.
     */
    private void moveToProtectivePosition(Vec3 targetPos) {
        double speed = chargeSpeed * 0.7;
        mother.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, speed);
    }

    /**
     * Charge at the threat.
     */
    private void chargeThreat() {
        Vec3 threatPos = currentThreat.position();
        double chargeSpeed = this.chargeSpeed * 1.2;

        mother.getNavigation().moveTo(threatPos.x, threatPos.y, threatPos.z, chargeSpeed);

        // Play warning sound
        level.playSound(null, mother.blockPosition(),
                SoundEvents.COW_MILK, // Use as aggressive moo sound
                SoundSource.HOSTILE,
                1.5F, 0.8F);

        // Spawn aggressive particles
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                    mother.getX(),
                    mother.getY() + mother.getEyeHeight(),
                    mother.getZ(),
                    5, 0.2, 0.2, 0.2, 0.0
            );
        }
    }

    /**
     * Emit an alarm call to alert the herd.
     */
    private void emitAlarmCall() {
        level.playSound(null, mother.blockPosition(),
                SoundEvents.COW_MILK, // Use as alarm call
                SoundSource.NEUTRAL,
                2.0F, 1.2F);

        // Alert nearby herd members through persistent data
        mother.getPersistentData().putBoolean("better-ecology:alarm_active", true);
        mother.getPersistentData().putLong("better-ecology:alarm_tick", mother.tickCount);
    }

    /**
     * Check if an entity is a threat to calves.
     */
    private boolean isThreat(Entity entity) {
        if (entity.equals(mother) || entity.equals(currentCalf)) {
            return false;
        }

        EntityType<?> type = entity.getType();

        // Predators
        if (type == EntityType.WOLF ||
            type == EntityType.FOX ||
            type == EntityType.CAT ||
            type == EntityType.OCELOT) {
            return true;
        }

        // Hostile mobs
        if (entity.getType().getCategory() == net.minecraft.world.entity.EntityType.MONSTER) {
            return true;
        }

        // Players (unless sneaking)
        if (type == EntityType.PLAYER) {
            return !entity.isSteppingCarefully();
        }

        return false;
    }

    /**
     * Check if two mobs are the same species.
     */
    private boolean isSameSpecies(Mob mob1, Mob mob2) {
        return mob1.getType().equals(mob2.getType());
    }
}
