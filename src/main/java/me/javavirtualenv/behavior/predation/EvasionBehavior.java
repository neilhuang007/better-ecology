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
    private final double zigzagIntensity;

    private Entity currentThreat;
    private int zigzagTimer = 0;
    private double currentZigzagDirection = 0.0;
    private double targetZigzagDirection = 0.0;
    private boolean isEvading = false;

    public EvasionBehavior(double evasionSpeed, double evasionForce,
                          double detectionRange, double safetyDistance, double zigzagIntensity) {
        this.evasionSpeed = evasionSpeed;
        this.evasionForce = evasionForce;
        this.detectionRange = detectionRange;
        this.safetyDistance = safetyDistance;
        this.zigzagIntensity = zigzagIntensity;
    }

    public EvasionBehavior(double evasionSpeed, double evasionForce,
                          double detectionRange, double safetyDistance) {
        this(evasionSpeed, evasionForce, detectionRange, safetyDistance, 0.5);
    }

    public EvasionBehavior() {
        this(1.5, 0.2, 24.0, 36.0, 0.5);
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
     * Calculates evasion force with protean (unpredictable) zigzag pattern.
     * Based on research: Moore et al. (2017) - unpredictability directly correlated with evasion success.
     * Uses random direction changes smoothed over time to prevent predators from anticipating path.
     */
    private Vec3d calculateEvasion(Vec3d preyPos, Vec3d preyVelocity, Vec3d threatPos) {
        // Base direction: away from threat
        Vec3d awayFromThreat = Vec3d.sub(preyPos, threatPos);
        awayFromThreat.normalize();

        // Implement protean movement - unpredictable zigzagging
        // Instead of regular pattern, use random direction changes smoothed over time
        zigzagTimer++;

        // Change target direction randomly every 5-15 ticks (unpredictable timing)
        if (zigzagTimer > 5 + (int)(Math.random() * 10)) {
            zigzagTimer = 0;
            // Random direction between -1 and 1
            targetZigzagDirection = (Math.random() * 2.0 - 1.0) * zigzagIntensity;
        }

        // Smoothly interpolate current direction toward target (prevents jerky movement)
        double lerpFactor = 0.2;
        currentZigzagDirection = currentZigzagDirection + (targetZigzagDirection - currentZigzagDirection) * lerpFactor;

        // Calculate perpendicular vector for lateral movement
        Vec3d perpendicular = new Vec3d(-awayFromThreat.z, 0, awayFromThreat.x);
        Vec3d zigzagComponent = perpendicular.copy();
        zigzagComponent.mult(currentZigzagDirection);

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
            if (distance < detectionRange) {
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
               typeName.contains("dolphin") ||
               typeName.contains("guardian") ||
               typeName.contains("drowned") ||
               typeName.contains("axolotl") ||
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
