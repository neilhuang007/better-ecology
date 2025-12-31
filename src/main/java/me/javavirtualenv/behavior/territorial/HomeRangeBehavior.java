package me.javavirtualenv.behavior.territorial;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;

/**
 * Steering behavior that keeps an animal within its home range.
 * When outside the range, applies a force pulling the animal back toward the center.
 */
public class HomeRangeBehavior extends SteeringBehavior {
    private final HomeRange homeRange;
    private final double returnStrength;
    private final boolean softBoundary;
    private final double maxSpeed;

    public HomeRangeBehavior(HomeRange homeRange, double returnStrength, boolean softBoundary, double maxSpeed) {
        this.homeRange = homeRange;
        this.returnStrength = returnStrength;
        this.softBoundary = softBoundary;
        this.maxSpeed = maxSpeed;
    }

    public HomeRangeBehavior(HomeRange homeRange, double returnStrength, boolean softBoundary) {
        this(homeRange, returnStrength, softBoundary, 0.5);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        BlockPos currentPos = context.getBlockPos();

        // Record this visit
        homeRange.recordVisit(currentPos);

        // If within home range, no force needed
        if (homeRange.isWithinRange(currentPos)) {
            return new Vec3d();
        }

        // Outside home range - calculate return force
        BlockPos center = homeRange.getCenter();
        Vec3d centerVec = new Vec3d(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        Vec3d currentVec = context.getPosition();

        if (softBoundary) {
            return calculateSoftBoundaryForce(currentVec, centerVec, context.getVelocity());
        } else {
            return calculateHardBoundaryForce(currentVec, centerVec, context.getVelocity());
        }
    }

    /**
     * Calculates a gradual force proportional to distance beyond the boundary
     */
    private Vec3d calculateSoftBoundaryForce(Vec3d currentPos, Vec3d centerPos, Vec3d velocity) {
        Vec3d toCenter = Vec3d.sub(centerPos, currentPos);
        double distance = toCenter.magnitude();
        double excessDistance = distance - homeRange.getRadius();

        if (excessDistance <= 0) {
            return new Vec3d();
        }

        // Force increases with distance beyond boundary
        double forceMagnitude = excessDistance * returnStrength;

        Vec3d force = toCenter.copy();
        force.normalize();
        force.mult(forceMagnitude);

        return force;
    }

    /**
     * Calculates a strong force immediately when outside boundary
     */
    private Vec3d calculateHardBoundaryForce(Vec3d currentPos, Vec3d centerPos, Vec3d velocity) {
        // Use seek behavior to return to center
        return seek(currentPos, velocity, centerPos, maxSpeed * returnStrength);
    }

    /**
     * Gets the associated home range
     */
    public HomeRange getHomeRange() {
        return homeRange;
    }

    /**
     * Gets the return strength parameter
     */
    public double getReturnStrength() {
        return returnStrength;
    }

    /**
     * Checks if using soft boundary
     */
    public boolean isSoftBoundary() {
        return softBoundary;
    }
}
