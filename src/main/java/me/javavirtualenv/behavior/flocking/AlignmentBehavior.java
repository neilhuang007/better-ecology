package me.javavirtualenv.behavior.flocking;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Alignment behavior - steers to match average heading/velocity of nearby flockmates.
 * This is one of the three core flocking behaviors (separation, alignment, cohesion).
 */
public class AlignmentBehavior extends SteeringBehavior {

    private double alignmentRadius;
    private double maxSpeed;
    private double maxForce;

    public AlignmentBehavior(double alignmentRadius, double maxSpeed, double maxForce, double weight) {
        this.alignmentRadius = alignmentRadius;
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
        this.weight = weight;
    }

    public AlignmentBehavior(double alignmentRadius, double maxSpeed, double maxForce) {
        this(alignmentRadius, maxSpeed, maxForce, 1.5);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        return calculateAlignment(context, context.getNeighbors());
    }

    /**
     * Calculates alignment steering force to match average velocity of flockmates.
     *
     * @param context The behavior context containing entity state
     * @param flockmates List of nearby flockmates to align with (null = use context to find them)
     * @return Steering force vector to align with nearby flockmates
     */
    public Vec3d calculateAlignment(BehaviorContext context, List<Entity> flockmates) {
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

            if (distance > 0 && distance < alignmentRadius) {
                Vec3d flockMateVel = new Vec3d(
                    flockmate.getDeltaMovement().x,
                    flockmate.getDeltaMovement().y,
                    flockmate.getDeltaMovement().z
                );
                steer.add(flockMateVel);
                count++;
            }
        }

        if (count > 0) {
            steer.div(count);
            steer.normalize();
            steer.mult(maxSpeed);
            steer.sub(velocity);
            steer.limit(maxForce);
        }

        steer.mult(weight);
        return steer;
    }

    public void setAlignmentRadius(double alignmentRadius) {
        this.alignmentRadius = alignmentRadius;
    }

    public double getAlignmentRadius() {
        return alignmentRadius;
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
