package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

/**
 * Attraction behavior for predators seeking prey.
 * Drives predators toward potential prey within detection range.
 * Works with PursuitBehavior - this finds targets, pursuit catches them.
 */
public class AttractionBehavior extends SteeringBehavior {

    private final double attractionRadius;
    private final double maxAttractionForce;
    private final double detectionRange;
    private final PreySelector preySelector;

    private Entity currentTarget;

    public AttractionBehavior(double attractionRadius, double maxAttractionForce,
                             double detectionRange, PreySelector preySelector) {
        this.attractionRadius = attractionRadius;
        this.maxAttractionForce = maxAttractionForce;
        this.detectionRange = detectionRange;
        this.preySelector = preySelector != null ? preySelector : new PreySelector();
    }

    public AttractionBehavior(double attractionRadius, double maxAttractionForce,
                             double detectionRange) {
        this(attractionRadius, maxAttractionForce, detectionRange, new PreySelector());
    }

    public AttractionBehavior() {
        this(16.0, 0.15, 32.0, new PreySelector());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof Mob predator)) {
            return new Vec3d();
        }

        // Find or validate target
        Entity target = findTarget(predator);
        if (target == null) {
            currentTarget = null;
            return new Vec3d();
        }

        currentTarget = target;

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        double distance = position.distanceTo(targetPos);

        // Only attract if within attraction range but not too close
        if (distance < 2.0) {
            return new Vec3d(); // Close enough to attack
        }

        if (distance > attractionRadius) {
            return new Vec3d(); // Too far, let other behaviors handle
        }

        // Calculate attraction force
        Vec3d desired = Vec3d.sub(targetPos, position);
        desired.normalize();
        desired.mult(context.getSpeed() > 0 ? context.getSpeed() : 0.3);

        // Reynolds steering formula
        Vec3d steer = Vec3d.sub(desired, velocity);
        steer.limit(maxAttractionForce);
        return steer;
    }

    private Entity findTarget(Mob predator) {
        if (currentTarget != null && currentTarget.isAlive()) {
            double distance = predator.position().distanceTo(currentTarget.position());
            if (distance < detectionRange) {
                return currentTarget;
            }
        }

        return preySelector.selectPrey(predator);
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(Entity target) {
        this.currentTarget = target;
    }

    public double getAttractionRadius() {
        return attractionRadius;
    }

    public double getMaxAttractionForce() {
        return maxAttractionForce;
    }
}
