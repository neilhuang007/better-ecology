package me.javavirtualenv.behavior.flocking;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Cohesion behavior - steers toward average position (center of mass) of nearby flockmates.
 * This is one of the three core flocking behaviors (separation, alignment, cohesion).
 */
public class CohesionBehavior extends SteeringBehavior {

    private double cohesionRadius;
    private double maxSpeed;
    private double maxForce;

    public CohesionBehavior(double cohesionRadius, double maxSpeed, double maxForce, double weight) {
        this.cohesionRadius = cohesionRadius;
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
        this.weight = weight;
    }

    public CohesionBehavior(double cohesionRadius, double maxSpeed, double maxForce) {
        this(cohesionRadius, maxSpeed, maxForce, 1.3);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        return calculateCohesion(context, context.getNeighbors());
    }

    /**
     * Calculates cohesion steering force toward center of mass of flockmates.
     *
     * @param context The behavior context containing entity state
     * @param flockmates List of nearby flockmates to cohere with (null = use context to find them)
     * @return Steering force vector toward center of mass
     */
    public Vec3d calculateCohesion(BehaviorContext context, List<Entity> flockmates) {
        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();
        Vec3d target = new Vec3d();
        int count = 0;

        if (flockmates == null) {
            return new Vec3d();
        }

        for (Entity flockmate : flockmates) {
            if (flockmate.equals(context.getEntity())) {
                continue;
            }

            Vec3d flockMatePos = new Vec3d(flockmate.getX(), flockmate.getY(), flockmate.getZ());
            double distance = position.distanceTo(flockMatePos);

            if (distance > 0 && distance < cohesionRadius) {
                target.add(flockMatePos);
                count++;
            }
        }

        if (count == 0) {
            return new Vec3d();
        }

        target.div(count);

        Vec3d steer = Vec3d.sub(target, position);
        steer.normalize();
        steer.mult(maxSpeed);
        steer.sub(velocity);
        steer.limit(maxForce);

        return steer;
    }

    public void setCohesionRadius(double cohesionRadius) {
        this.cohesionRadius = cohesionRadius;
    }

    public double getCohesionRadius() {
        return cohesionRadius;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxForce(double maxForce) {
        this.maxForce = maxForce;
    }

    public double getMaxForce() {
        return maxForce;
    }
}
