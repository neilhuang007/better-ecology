package me.javavirtualenv.behavior.parent;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Mother protection behavior - defends offspring from nearby threats.
 * Based on research into maternal aggression in mammals during lactation period.
 * Mothers will prioritize protecting the most threatened baby.
 */
public class MotherProtectionBehavior extends SteeringBehavior {

    private double protectionRange;
    private final double threatDetectionRange;
    private final double attackSpeed;
    private double aggressionLevel;

    private Entity currentTarget;
    private UUID lastProtectedBaby;
    private int protectionCooldown;
    private static final int PROTECTION_COOLDOWN_TICKS = 100; // 5 seconds

    public MotherProtectionBehavior(double protectionRange, double threatDetectionRange,
                                    double attackSpeed, double aggressionLevel) {
        this.protectionRange = protectionRange;
        this.threatDetectionRange = threatDetectionRange;
        this.attackSpeed = attackSpeed;
        this.aggressionLevel = aggressionLevel;
        this.protectionCooldown = 0;
    }

    public MotherProtectionBehavior() {
        this(16.0, 24.0, 1.5, 1.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (context == null) {
            return new Vec3d();
        }

        // Update cooldown
        if (protectionCooldown > 0) {
            protectionCooldown--;
            if (protectionCooldown > 0) {
                return new Vec3d(); // In cooldown, don't protect
            }
        }

        Entity entity = context.getEntity();
        if (!(entity instanceof AgeableMob mother)) {
            return new Vec3d();
        }

        if (!mother.isAlive()) {
            currentTarget = null;
            return new Vec3d();
        }

        Entity nearestThreat = findNearestThreatToOffspring(mother);
        if (nearestThreat == null) {
            currentTarget = null;
            return new Vec3d();
        }

        if (!nearestThreat.isAlive()) {
            currentTarget = null;
            return new Vec3d();
        }

        currentTarget = nearestThreat;

        Vec3d motherPos = context.getPosition();
        Vec3d threatPos = new Vec3d(nearestThreat.getX(), nearestThreat.getY(), nearestThreat.getZ());
        double distanceToThreat = motherPos.distanceTo(threatPos);

        if (distanceToThreat <= 3.0) {
            // Reached threat, set cooldown before protecting again
            protectionCooldown = PROTECTION_COOLDOWN_TICKS;
            return new Vec3d();
        }

        Vec3d steer = seek(motherPos, context.getVelocity(), threatPos, attackSpeed);
        steer.mult(aggressionLevel);
        steer.limit(getMaxForce(mother));
        return steer;
    }

    private Entity findNearestThreatToOffspring(AgeableMob mother) {
        Level level = mother.level();
        Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());

        List<AgeableMob> nearbyBabies = level.getEntitiesOfClass(AgeableMob.class,
                mother.getBoundingBox().inflate(protectionRange));

        Entity mostThreatenedBabyThreat = null;
        double highestThreatLevel = 0.0;

        for (AgeableMob baby : nearbyBabies) {
            if (!baby.isBaby() || !baby.isAlive()) {
                continue;
            }

            if (!isSameSpecies(mother, baby)) {
                continue;
            }

            Entity threat = findThreatToBaby(level, baby);
            if (threat == null) {
                continue;
            }

            Vec3d babyPos = new Vec3d(baby.getX(), baby.getY(), baby.getZ());
            Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
            double threatDistance = babyPos.distanceTo(threatPos);

            double threatLevel = calculateThreatLevel(threatDistance, threat, baby);
            if (threatLevel > highestThreatLevel) {
                highestThreatLevel = threatLevel;
                mostThreatenedBabyThreat = threat;
                lastProtectedBaby = baby.getUUID();
            }
        }

        return mostThreatenedBabyThreat;
    }

    private Entity findThreatToBaby(Level level, AgeableMob baby) {
        Vec3d babyPos = new Vec3d(baby.getX(), baby.getY(), baby.getZ());

        List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class,
                baby.getBoundingBox().inflate(threatDetectionRange));

        Entity nearestThreat = null;
        double nearestThreatDistance = Double.MAX_VALUE;

        for (Entity entity : nearbyEntities) {
            if (!entity.isAlive()) {
                continue;
            }

            if (entity.equals(baby)) {
                continue;
            }

            if (!isPredator(entity)) {
                continue;
            }

            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            double distance = babyPos.distanceTo(entityPos);

            if (distance < nearestThreatDistance) {
                nearestThreatDistance = distance;
                nearestThreat = entity;
            }
        }

        return nearestThreat;
    }

    private boolean isPredator(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return isPredatoryMob(living);
        }
        return false;
    }

    private boolean isPredatoryMob(LivingEntity entity) {
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

    private double calculateThreatLevel(double distanceToThreat, Entity threat, AgeableMob baby) {
        double baseThreat = 1.0 / (1.0 + distanceToThreat);

        if (threat instanceof LivingEntity living) {
            double attackDamage = living.getAttribute(Attributes.ATTACK_DAMAGE) != null
                    ? living.getAttribute(Attributes.ATTACK_DAMAGE).getValue()
                    : 1.0;
            baseThreat *= attackDamage;
        }

        if (baby.getHealth() < baby.getMaxHealth() * 0.5) {
            baseThreat *= 1.5;
        }

        return baseThreat;
    }

    private double getMaxForce(AgeableMob mother) {
        return mother.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.0;
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }

    public UUID getLastProtectedBaby() {
        return lastProtectedBaby;
    }

    public void setProtectionRange(double protectionRange) {
        this.protectionRange = protectionRange;
    }

    public double getProtectionRange() {
        return protectionRange;
    }

    public void setAggressionLevel(double aggressionLevel) {
        this.aggressionLevel = aggressionLevel;
    }

    public double getAggressionLevel() {
        return aggressionLevel;
    }
}
