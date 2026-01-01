package me.javavirtualenv.behavior.flocking;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Separation behavior - steers away from nearby flockmates to avoid crowding.
 * This is one of the three core flocking behaviors (separation, alignment, cohesion).
 */
public class SeparationBehavior extends SteeringBehavior {

    private double desiredSeparation;
    private double maxSpeed;
    private double maxForce;

    public SeparationBehavior(double desiredSeparation, double maxSpeed, double maxForce, double weight) {
        this.desiredSeparation = desiredSeparation;
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
        this.weight = weight;
    }

    public SeparationBehavior(double desiredSeparation, double maxSpeed, double maxForce) {
        this(desiredSeparation, maxSpeed, maxForce, 2.5);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        return calculateSeparation(context, context.getNeighbors());
    }

    /**
     * Calculates separation steering force from a list of flockmates.
     *
     * @param context The behavior context containing entity state
     * @param flockmates List of nearby flockmates to separate from (null = use context to find them)
     * @return Steering force vector to separate from nearby flockmates
     */
    public Vec3d calculateSeparation(BehaviorContext context, List<Entity> flockmates) {
        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();
        Vec3d steer = new Vec3d();
        int count = 0;

        if (flockmates == null) {
            return steer;
        }

        for (Entity flockmate : flockmates) {
            if (flockmate.equals(context.getEntity())) {
                continue;
            }

            Vec3d flockMatePos = new Vec3d(flockmate.getX(), flockmate.getY(), flockmate.getZ());
            double distance = position.distanceTo(flockMatePos);

            if (distance > 0 && distance < desiredSeparation) {
                Vec3d diff = Vec3d.sub(position, flockMatePos);
                diff.normalize();
                diff.div(distance);
                steer.add(diff);
                count++;
            }
        }

        if (count > 0) {
            steer.div(count);
        }

        if (steer.magnitude() > 0) {
            steer.normalize();
            steer.mult(maxSpeed);
            steer.sub(velocity);
            steer.limit(maxForce);
        }

        return steer;
    }

    public void setDesiredSeparation(double desiredSeparation) {
        this.desiredSeparation = desiredSeparation;
    }

    public double getDesiredSeparation() {
        return desiredSeparation;
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
