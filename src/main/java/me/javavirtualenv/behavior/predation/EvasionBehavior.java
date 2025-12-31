package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Evasion behavior for prey animals fleeing from predators.
 * Implements active evasion with zigzag patterns and obstacle seeking.
 * Based on research into escape strategies in ungulates and small mammals.
 */
public class EvasionBehavior extends SteeringBehavior {

    private final double evasionSpeed;
    private final double evasionForce;
    private final double detectionRange;
    private final double safetyDistance;

    private Entity currentThreat;
    private int zigzagTimer = 0;
    private int zigzagDirection = 1;
    private boolean isEvading = false;

    public EvasionBehavior(double evasionSpeed, double evasionForce,
                          double detectionRange, double safetyDistance) {
        this.evasionSpeed = evasionSpeed;
        this.evasionForce = evasionForce;
        this.detectionRange = detectionRange;
        this.safetyDistance = safetyDistance;
    }

    public EvasionBehavior() {
        this(1.5, 0.2, 24.0, 48.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof Mob prey)) {
            return new Vec3d();
        }

        // Find nearest threat
        Entity threat = findNearestThreat(prey);
        if (threat == null || !threat.isAlive()) {
            currentThreat = null;
            isEvading = false;
            return new Vec3d();
        }

        currentThreat = threat;
        Vec3d preyPos = context.getPosition();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        double distance = preyPos.distanceTo(threatPos);

        // Check if we've escaped to safety
        if (isEvading && distance > safetyDistance) {
            isEvading = false;
            return new Vec3d(); // Safe, can stop fleeing
        }

        // Start evading if threat is close
        if (distance < detectionRange) {
            isEvading = true;
            return calculateEvasion(preyPos, context.getVelocity(), threatPos);
        }

        return new Vec3d();
    }

    /**
     * Calculates evasion force with zigzag pattern.
     */
    private Vec3d calculateEvasion(Vec3d preyPos, Vec3d preyVelocity, Vec3d threatPos) {
        // Base direction: away from threat
        Vec3d awayFromThreat = Vec3d.sub(preyPos, threatPos);
        awayFromThreat.normalize();

        // Add zigzag pattern
        zigzagTimer++;
        if (zigzagTimer > 15) {
            zigzagTimer = 0;
            zigzagDirection *= -1;
        }

        // Calculate perpendicular vector
        Vec3d perpendicular = new Vec3d(-awayFromThreat.z, 0, awayFromThreat.x);
        Vec3d zigzagComponent = perpendicular.copy();
        zigzagComponent.mult(zigzagDirection * 0.3);

        // Combine evasion direction with zigzag
        Vec3d evasionDirection = awayFromThreat.copy();
        evasionDirection.add(zigzagComponent);
        evasionDirection.normalize();
        evasionDirection.mult(evasionSpeed);

        // Calculate steering force
        Vec3d steer = Vec3d.sub(evasionDirection, preyVelocity);
        steer.limit(evasionForce);
        return steer;
    }

    private Entity findNearestThreat(Mob prey) {
        if (currentThreat != null && currentThreat.isAlive()) {
            double distance = prey.position().distanceTo(currentThreat.position());
            if (distance < detectionRange * 1.5) {
                return currentThreat;
            }
        }

        Entity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        List<LivingEntity> nearbyEntities = prey.level().getEntitiesOfClass(
            LivingEntity.class,
            prey.getBoundingBox().inflate(detectionRange)
        );

        for (LivingEntity entity : nearbyEntities) {
            if (entity.equals(prey)) {
                continue;
            }

            if (!isThreat(prey, entity)) {
                continue;
            }

            double distance = prey.position().distanceTo(entity.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestThreat = entity;
            }
        }

        return nearestThreat;
    }

    private boolean isThreat(Mob prey, LivingEntity entity) {
        // Players are threats
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return !((net.minecraft.world.entity.player.Player) entity).isShiftKeyDown();
        }

        // Predators are threats
        String typeName = entity.getType().toString().toLowerCase();
        return typeName.contains("wolf") ||
               typeName.contains("fox") ||
               typeName.contains("cat") ||
               typeName.contains("ocelot") ||
               typeName.contains("spider") ||
               typeName.contains("phantom") ||
               (entity instanceof Mob && ((Mob) entity).isAggressive());
    }

    public Entity getCurrentThreat() {
        return currentThreat;
    }

    public boolean isEvading() {
        return isEvading;
    }

    public void setEvading(boolean evading) {
        this.isEvading = evading;
    }
}
