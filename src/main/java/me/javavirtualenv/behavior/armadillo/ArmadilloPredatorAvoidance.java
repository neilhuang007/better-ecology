package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.behavior.BehaviorRule;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;

import java.util.List;

/**
 * Predator avoidance behavior for armadillos.
 * <p>
 * Armadillos use a unique two-stage defense:
 * 1. Flee from predators if distance is safe
 * 2. Roll up into ball if predator is too close
 * <p>
 * This behavior calculates avoidance force based on:
 * - Distance to nearest predator
 * - Predator type (wolves, cats, ocelots, foxes)
 * - Current rolled state
 * - Visibility of armadillo to predator
 */
public class ArmadilloPredatorAvoidance implements BehaviorRule {

    private final double detectionRadius;
    private final double fleeWeight;
    private final double rollThreshold;

    public ArmadilloPredatorAvoidance() {
        this(24.0, 1.5, 8.0);
    }

    public ArmadilloPredatorAvoidance(double detectionRadius, double fleeWeight, double rollThreshold) {
        this.detectionRadius = detectionRadius;
        this.fleeWeight = fleeWeight;
        this.rollThreshold = rollThreshold;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob entity = context.getEntity();
        Vec3d position = context.getPosition();

        // Find nearest predator
        Mob nearestPredator = findNearestPredator(entity);
        if (nearestPredator == null) {
            return new Vec3d();
        }

        // Calculate distance to predator
        Vec3d predatorPos = new Vec3d(
            nearestPredator.getX(),
            nearestPredator.getY(),
            nearestPredator.getZ()
        );

        double distance = position.distanceTo(predatorPos);

        // If too close, signal to roll up instead of fleeing
        if (distance < rollThreshold) {
            // Return zero force - roll up behavior will handle this
            return new Vec3d();
        }

        // Calculate flee direction (away from predator)
        Vec3d fleeDirection = position.sub(predatorPos);
        fleeDirection.normalize();

        // Scale by distance (stronger when closer)
        double distanceFactor = 1.0 - (distance / detectionRadius);
        fleeDirection.mult(fleeWeight * distanceFactor);

        return fleeDirection;
    }

    /**
     * Finds the nearest predator to the armadillo.
     *
     * @param armadillo The armadillo entity
     * @return Nearest predator, or null if none found
     */
    private Mob findNearestPredator(Mob armadillo) {
        List<Mob> nearbyEntities = armadillo.level().getEntitiesOfClass(
            Mob.class,
            armadillo.getBoundingBox().inflate(detectionRadius)
        );

        Mob nearest = null;
        double nearestDistance = detectionRadius;

        for (Mob entity : nearbyEntities) {
            if (isPredator(entity)) {
                double distance = armadillo.distanceTo(entity);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = entity;
                }
            }
        }

        return nearest;
    }

    /**
     * Checks if an entity is a predator of armadillos.
     *
     * @param entity The entity to check
     * @return true if the entity is a predator
     */
    private boolean isPredator(Mob entity) {
        EntityType<?> type = entity.getType();

        // Natural predators
        if (type == EntityType.WOLF ||
            type == EntityType.CAT ||
            type == EntityType.OCELOT ||
            type == EntityType.FOX) {
            // Tamed animals are less likely to attack
            if (entity instanceof TamableAnimal tameable) {
                return !tameable.isTame();
            }
            return true;
        }

        return false;
    }

    /**
     * Gets the detection radius.
     */
    public double getDetectionRadius() {
        return detectionRadius;
    }

    /**
     * Gets the flee weight.
     */
    public double getFleeWeight() {
        return fleeWeight;
    }

    /**
     * Gets the roll threshold distance.
     */
    public double getRollThreshold() {
        return rollThreshold;
    }
}
