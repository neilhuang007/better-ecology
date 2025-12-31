package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Avoidance behavior for maintaining distance from threats.
 * Unlike evasion (reactive fleeing), avoidance is proactive - maintaining
 * a safe buffer zone from potential threats.
 */
public class AvoidanceBehavior extends SteeringBehavior {

    private final double avoidanceRadius;
    private final double maxAvoidanceForce;
    private final double detectionRange;

    public AvoidanceBehavior(double avoidanceRadius, double maxAvoidanceForce,
                            double detectionRange) {
        this.avoidanceRadius = avoidanceRadius;
        this.maxAvoidanceForce = maxAvoidanceForce;
        this.detectionRange = detectionRange;
    }

    public AvoidanceBehavior() {
        this(8.0, 0.3, 16.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof Mob mob)) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();
        Vec3d avoidanceForce = new Vec3d();

        int count = 0;

        // Find all nearby threats to avoid
        List<LivingEntity> nearbyEntities = mob.level().getEntitiesOfClass(
            LivingEntity.class,
            mob.getBoundingBox().inflate(detectionRange)
        );

        for (LivingEntity other : nearbyEntities) {
            if (other.equals(entity)) {
                continue;
            }

            if (!shouldAvoid(mob, other)) {
                continue;
            }

            Vec3d otherPos = new Vec3d(other.getX(), other.getY(), other.getZ());
            double distance = position.distanceTo(otherPos);

            if (distance < avoidanceRadius && distance > 0) {
                // Calculate avoidance force (stronger when closer)
                Vec3d diff = Vec3d.sub(position, otherPos);
                diff.normalize();

                // Weight by distance (closer = stronger avoidance)
                double weight = (avoidanceRadius - distance) / avoidanceRadius;
                diff.mult(weight);

                avoidanceForce.add(diff);
                count++;
            }
        }

        if (count > 0) {
            // Normalize and scale to max speed
            avoidanceForce.normalize();

            // Desired velocity is directly away from threats
            Vec3d desired = avoidanceForce.copy();
            desired.mult(context.getSpeed() > 0 ? context.getSpeed() : 0.5);

            // Reynolds steering formula: steer = desired - velocity
            Vec3d steer = Vec3d.sub(desired, velocity);
            steer.limit(maxAvoidanceForce);
            return steer;
        }

        return new Vec3d();
    }

    /**
     * Determines if the entity should avoid the other entity.
     */
    private boolean shouldAvoid(Mob entity, LivingEntity other) {
        // Avoid players
        if (other instanceof net.minecraft.world.entity.player.Player player) {
            return !player.isShiftKeyDown();
        }

        // Avoid predators
        String typeName = other.getType().toString().toLowerCase();
        if (typeName.contains("wolf") || typeName.contains("fox") ||
            typeName.contains("cat") || typeName.contains("ocelot") ||
            typeName.contains("spider") || typeName.contains("phantom")) {
            return true;
        }

        // Avoid aggressive mobs
        if (other instanceof Mob && ((Mob) other).isAggressive()) {
            return true;
        }

        return false;
    }

    public double getAvoidanceRadius() {
        return avoidanceRadius;
    }

    public double getMaxAvoidanceForce() {
        return maxAvoidanceForce;
    }

    public double getDetectionRange() {
        return detectionRange;
    }
}
