package me.javavirtualenv.behavior.core;

/**
 * Seek behavior steers the entity toward a target position.
 * Algorithm: desiredVelocity = normalize(target - position) * maxSpeed
 *            steeringForce = desiredVelocity - velocity
 */
public class SeekBehavior extends SteeringBehavior {
    private Vec3d target;

    public SeekBehavior() {
        this(new Vec3d());
    }

    public SeekBehavior(Vec3d target) {
        super();
        this.target = target;
    }

    public SeekBehavior(Vec3d target, double weight) {
        super();
        setWeight(weight);
        this.target = target;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (target == null) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();
        double maxSpeed = context.getMaxSpeed();
        double maxForce = context.getMaxForce();

        // Calculate desired velocity
        Vec3d desiredVelocity = Vec3d.sub(target, position);
        desiredVelocity.normalize();
        desiredVelocity.mult(maxSpeed);

        // Calculate steering force
        Vec3d steeringForce = Vec3d.sub(desiredVelocity, velocity);

        // Limit to max force
        return limitForce(steeringForce, maxForce);
    }

    public Vec3d getTarget() {
        return target;
    }

    public void setTarget(Vec3d target) {
        this.target = target;
    }
}
