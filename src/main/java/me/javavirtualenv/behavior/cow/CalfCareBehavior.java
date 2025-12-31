package me.javavirtualenv.behavior.cow;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Calf care behavior for mother cows.
 * <p>
 * Implements maternal behaviors:
 * - Protection from predators (aggressive defense)
 * - Nursing behavior (milk let-down for calves)
 * - Keeping calves close to herd
 * - Alarm signaling when threats detected
 * <p>
 * Based on research into maternal behavior in cattle:
 * - Mothers show strong protective aggression in first months
 * - Calves nurse 5-10 times per day initially
 * - Mother-calf bond is strongest in first 3 months
 * - Cows will form protective circles around calves when threatened
 */
public class CalfCareBehavior extends SteeringBehavior {
    private double protectionRange;
    private double nursingRange;
    private double followDistance;
    private int nursingCooldown;
    private double maternalAggression;

    private UUID lastProtectedCalf;
    private UUID currentCalfUuid;
    private int lastNursingTick;
    private boolean isAlerted;

    public CalfCareBehavior(double protectionRange, double nursingRange,
                           double followDistance, int nursingCooldown,
                           double maternalAggression) {
        this.protectionRange = protectionRange;
        this.nursingRange = nursingRange;
        this.followDistance = followDistance;
        this.nursingCooldown = nursingCooldown;
        this.maternalAggression = maternalAggression;
        this.lastNursingTick = -nursingCooldown;
        this.isAlerted = false;
    }

    public CalfCareBehavior() {
        this(16.0, 3.0, 8.0, 600, 1.5); // 30 second nursing cooldown
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof AgeableMob mother)) {
            return new Vec3d();
        }

        if (!mother.isAlive()) {
            return new Vec3d();
        }

        // Find nearest calf
        Entity calf = findNearestCalf(mother);
        if (calf == null) {
            return new Vec3d();
        }

        currentCalfUuid = calf.getUUID();

        // Check for threats to calf
        Entity threat = findThreatToCalf(mother, calf);
        if (threat != null && threat.isAlive()) {
            lastProtectedCalf = calf.getUUID();
            isAlerted = true;
            return defendCalf(context, mother, calf, threat);
        }

        // Reset alert status when no threat
        if (isAlerted) {
            isAlerted = false;
        }

        // Check if calf needs nursing
        if (calf instanceof AgeableMob ageableCalf && ageableCalf.isBaby()) {
            if (shouldNurseCalf(context, mother, calf)) {
                return approachCalfForNursing(context, mother, calf);
            }
        }

        // Keep calf close (follow behavior)
        return keepCalfClose(context, mother, calf);
    }

    private Entity findNearestCalf(AgeableMob mother) {
        Level level = mother.level();
        Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());

        List<AgeableMob> nearbyCalves = level.getEntitiesOfClass(AgeableMob.class,
                mother.getBoundingBox().inflate(protectionRange));

        Entity nearestCalf = null;
        double nearestDistance = Double.MAX_VALUE;

        for (AgeableMob baby : nearbyCalves) {
            if (!baby.isBaby() || !baby.isAlive()) {
                continue;
            }

            if (!isSameSpecies(mother, baby)) {
                continue;
            }

            Vec3d babyPos = new Vec3d(baby.getX(), baby.getY(), baby.getZ());
            double distance = motherPos.distanceTo(babyPos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestCalf = baby;
            }
        }

        return nearestCalf;
    }

    private Entity findThreatToCalf(AgeableMob mother, Entity calf) {
        Level level = mother.level();
        Vec3d calfPos = new Vec3d(calf.getX(), calf.getY(), calf.getZ());

        List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class,
                calf.getBoundingBox().inflate(16.0));

        Entity nearestThreat = null;
        double nearestThreatDistance = Double.MAX_VALUE;

        for (Entity entity : nearbyEntities) {
            if (!entity.isAlive()) {
                continue;
            }

            if (entity.equals(calf) || entity.equals(mother)) {
                continue;
            }

            if (!isPredator(entity)) {
                continue;
            }

            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            double distance = calfPos.distanceTo(entityPos);

            if (distance < nearestThreatDistance) {
                nearestThreatDistance = distance;
                nearestThreat = entity;
            }
        }

        return nearestThreat;
    }

    private Vec3d defendCalf(BehaviorContext context, AgeableMob mother, Entity calf, Entity threat) {
        Vec3d motherPos = context.getPosition();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        Vec3d calfPos = new Vec3d(calf.getX(), calf.getY(), calf.getZ());

        double distanceToThreat = motherPos.distanceTo(threatPos);
        double distanceToCalf = motherPos.distanceTo(calfPos);

        // Position between threat and calf
        Vec3d protectivePosition = calculateProtectivePosition(motherPos, calfPos, threatPos);

        // If already in position, face threat
        if (distanceToThreat <= 4.0 && distanceToCalf <= 6.0) {
            return new Vec3d(); // Hold ground
        }

        // Move to protective position
        double attackSpeed = mother.getAttributeValue(Attributes.MOVEMENT_SPEED) * maternalAggression;
        return seek(motherPos, context.getVelocity(), protectivePosition, attackSpeed);
    }

    private Vec3d calculateProtectivePosition(Vec3d motherPos, Vec3d calfPos, Vec3d threatPos) {
        // Position halfway between calf and threat, but slightly closer to calf
        Vec3d calfToThreat = threatPos.subtract(calfPos);
        Vec3d protectivePos = calfPos.add(calfToThreat.mult(0.3));

        // Ensure protective position is on ground
        protectivePos.y = Math.max(motherPos.y, protectivePos.y);

        return protectivePos;
    }

    private boolean shouldNurseCalf(BehaviorContext context, AgeableMob mother, Entity calf) {
        int currentTick = context.getEntity().tickCount();

        // Check cooldown
        if (currentTick - lastNursingTick < nursingCooldown) {
            return false;
        }

        // Check if calf is close enough
        double distance = context.getPosition().distanceTo(
                new Vec3d(calf.getX(), calf.getY(), calf.getZ()));

        return distance <= nursingRange;
    }

    private Vec3d approachCalfForNursing(BehaviorContext context, AgeableMob mother, Entity calf) {
        Vec3d motherPos = context.getPosition();
        Vec3d calfPos = new Vec3d(calf.getX(), calf.getY(), calf.getZ());

        double distance = motherPos.distanceTo(calfPos);

        if (distance <= 2.0) {
            // Close enough to nurse
            lastNursingTick = context.getEntity().tickCount();
            onNursedCalf(mother, calf);
            return new Vec3d();
        }

        // Approach calf slowly
        double approachSpeed = mother.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.5;
        return seek(motherPos, context.getVelocity(), calfPos, approachSpeed);
    }

    private void onNursedCalf(AgeableMob mother, Entity calf) {
        // Mark calf as nursed (handled by separate nursing system)
        // Play nursing sound
        mother.level().playSound(null, mother.blockPosition(),
                net.minecraft.sounds.SoundEvents.COW_MILK,
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1.0F);

        // Could spawn milk particles here
    }

    private Vec3d keepCalfClose(BehaviorContext context, AgeableMob mother, Entity calf) {
        Vec3d motherPos = context.getPosition();
        Vec3d calfPos = new Vec3d(calf.getX(), calf.getY(), calf.getZ());

        double distance = motherPos.distanceTo(calfPos);

        // If calf is within follow distance, no action needed
        if (distance <= followDistance) {
            return new Vec3d();
        }

        // Move towards calf but don't crowd it
        double targetDistance = followDistance * 0.7;
        Vec3d directionToCalf = calfPos.subtract(motherPos).normalize();
        Vec3d targetPos = motherPos.add(directionToCalf.mult(targetDistance));

        double approachSpeed = mother.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.6;
        return seek(motherPos, context.getVelocity(), targetPos, approachSpeed);
    }

    private boolean isPredator(Entity entity) {
        String entityType = entity.getType().toString().toLowerCase();
        return entityType.contains("wolf") ||
               entityType.contains("fox") ||
               entityType.contains("cat") ||
               entityType.contains("ocelot") ||
               entityType.contains("bear") ||
               entityType.contains("zombie") ||
               entityType.contains("skeleton") ||
               entityType.contains("creeper") ||
               entityType.contains("spider") ||
               entityType.contains("phantom") ||
               entityType.contains("pillager") ||
               entityType.contains("vindicator") ||
               entityType.contains("vex");
    }

    private boolean isSameSpecies(AgeableMob mother, AgeableMob baby) {
        return mother.getClass().equals(baby.getClass());
    }

    // Getters for external access

    public UUID getCurrentCalfUuid() {
        return currentCalfUuid;
    }

    public UUID getLastProtectedCalf() {
        return lastProtectedCalf;
    }

    public boolean isAlerted() {
        return isAlerted;
    }

    public void setProtectionRange(double protectionRange) {
        this.protectionRange = protectionRange;
    }

    public double getProtectionRange() {
        return protectionRange;
    }

    public void setMaternalAggression(double maternalAggression) {
        this.maternalAggression = maternalAggression;
    }

    public double getMaternalAggression() {
        return maternalAggression;
    }
}
