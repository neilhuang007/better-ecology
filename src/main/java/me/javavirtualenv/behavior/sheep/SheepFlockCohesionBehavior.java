package me.javavirtualenv.behavior.sheep;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;

import java.util.List;

/**
 * Strong flock cohesion behavior specific to sheep.
 * Sheep are highly gregarious and will actively seek to maintain close proximity to their flock.
 * This behavior is stronger than general herd cohesion and causes sheep to group tightly.
 */
public class SheepFlockCohesionBehavior extends SteeringBehavior {

    private final double flockRadius;
    private final double maxSpeed;
    private final double maxForce;
    private final double minSeparation;

    public SheepFlockCohesionBehavior(double flockRadius, double maxSpeed, double maxForce, double minSeparation) {
        this.flockRadius = flockRadius;
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
        this.minSeparation = minSeparation;
        this.weight = 1.5; // Higher priority for sheep
    }

    public SheepFlockCohesionBehavior() {
        this(16.0, 1.0, 0.2, 2.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        List<Entity> neighbors = context.getNeighbors();
        if (neighbors == null || neighbors.isEmpty()) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();

        // Filter for sheep entities
        Vec3d flockCenter = new Vec3d();
        int sheepCount = 0;

        for (Entity neighbor : neighbors) {
            if (neighbor.equals(context.getEntity())) {
                continue;
            }

            if (!(neighbor instanceof Sheep)) {
                continue;
            }

            Vec3d neighborPos = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            double distance = position.distanceTo(neighborPos);

            if (distance > 0 && distance < flockRadius) {
                flockCenter.add(neighborPos);
                sheepCount++;
            }
        }

        if (sheepCount == 0) {
            return new Vec3d();
        }

        // Calculate average position (flock center)
        flockCenter.div(sheepCount);

        // If too far from flock center, steer toward it
        double distanceToCenter = position.distanceTo(flockCenter);
        if (distanceToCenter < flockRadius * 0.3) {
            return new Vec3d(); // Already in good position
        }

        // Seek flock center
        Vec3d desired = Vec3d.sub(flockCenter, position);
        desired.normalize();
        desired.mult(maxSpeed);

        Vec3d steer = Vec3d.sub(desired, velocity);
        steer.limit(maxForce);

        return steer;
    }

    /**
     * Sheep experience separation distress when too far from flock.
     * This increases the urgency of cohesion.
     */
    public double calculateDistressFactor(BehaviorContext context) {
        List<Entity> neighbors = context.getNeighbors();
        if (neighbors == null || neighbors.isEmpty()) {
            return 1.0; // Maximum distress when alone
        }

        Vec3d position = context.getPosition();
        double nearestDistance = Double.MAX_VALUE;
        int sheepCount = 0;

        for (Entity neighbor : neighbors) {
            if (neighbor.equals(context.getEntity())) {
                continue;
            }

            if (!(neighbor instanceof Sheep)) {
                continue;
            }

            Vec3d neighborPos = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            double distance = position.distanceTo(neighborPos);

            if (distance < flockRadius) {
                sheepCount++;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                }
            }
        }

        if (sheepCount == 0) {
            return 1.0; // Maximum distress - no other sheep nearby
        }

        // Calculate distress based on distance to nearest flock member
        // Sheep are calm when within 6 blocks of another sheep
        if (nearestDistance < 6.0) {
            return 0.0; // No distress
        } else if (nearestDistance < flockRadius * 0.5) {
            return (nearestDistance - 6.0) / (flockRadius * 0.5 - 6.0) * 0.5;
        } else {
            return 0.5 + (nearestDistance - flockRadius * 0.5) / (flockRadius - flockRadius * 0.5) * 0.5;
        }
    }

    public void setFlockRadius(double flockRadius) {
        this.flockRadius = flockRadius;
    }

    public double getFlockRadius() {
        return flockRadius;
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

    public void setMinSeparation(double minSeparation) {
        this.minSeparation = minSeparation;
    }

    public double getMinSeparation() {
        return minSeparation;
    }
}
