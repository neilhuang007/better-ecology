package me.javavirtualenv.behavior.core;

import me.javavirtualenv.behavior.BehaviorRule;

public abstract class SteeringBehavior implements BehaviorRule {
    protected double weight = 1.0;
    protected boolean enabled = true;

    public abstract Vec3d calculate(BehaviorContext context);

    public Vec3d calculateWeighted(BehaviorContext context) {
        if (!enabled) {
            return new Vec3d();
        }
        Vec3d result = calculate(context);
        Vec3d weighted = result.copy();
        weighted.mult(weight);
        return weighted;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected Vec3d seek(Vec3d currentPosition, Vec3d currentVelocity, Vec3d targetPosition, double maxSpeed) {
        Vec3d desired = Vec3d.sub(targetPosition, currentPosition);
        desired.normalize();
        desired.mult(maxSpeed);
        desired.sub(currentVelocity);
        return desired;
    }

    protected Vec3d flee(Vec3d currentPosition, Vec3d currentVelocity, Vec3d threatPosition, double maxSpeed) {
        Vec3d desired = Vec3d.sub(currentPosition, threatPosition);
        desired.normalize();
        desired.mult(maxSpeed);
        desired.sub(currentVelocity);
        return desired;
    }

    protected Vec3d arrive(Vec3d currentPosition, Vec3d currentVelocity, Vec3d targetPosition,
                          double maxSpeed, double slowingRadius) {
        Vec3d toTarget = Vec3d.sub(targetPosition, currentPosition);
        double distance = toTarget.magnitude();

        if (distance < 0.1) {
            return new Vec3d();
        }

        double desiredSpeed = maxSpeed;
        if (distance < slowingRadius) {
            desiredSpeed = maxSpeed * (distance / slowingRadius);
        }

        Vec3d desired = toTarget.copy();
        desired.normalize();
        desired.mult(desiredSpeed);
        desired.sub(currentVelocity);
        return desired;
    }

    protected Vec3d limitForce(Vec3d force, double maxForce) {
        if (force.magnitude() > maxForce) {
            force.normalize();
            force.mult(maxForce);
        }
        return force;
    }
}
