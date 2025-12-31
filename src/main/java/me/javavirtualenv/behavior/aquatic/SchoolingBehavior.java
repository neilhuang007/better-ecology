package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Schooling behavior implementing 3D boids algorithm for aquatic animals.
 * Fish in schools exhibit coordinated movement with separation, alignment, and cohesion.
 * <p>
 * Based on research showing that fish track topological neighbors (typically 5-7 nearest)
 * rather than all within a fixed radius. This creates natural, fluid schooling motion.
 */
public class SchoolingBehavior extends SteeringBehavior {
    private final AquaticConfig config;
    private final SeparationBehavior separation;
    private final AlignmentBehavior alignment;
    private final CohesionBehavior cohesion;
    private final CenterAttraction centerAttraction;

    public SchoolingBehavior(AquaticConfig config) {
        super(config.getSchoolSeparationWeight(), true);
        this.config = config;
        this.separation = new SeparationBehavior(
            config.getSeparationDistance(),
            config.getMaxSpeed(),
            config.getMaxForce()
        );
        this.alignment = new AlignmentBehavior(
            config.getPerceptionRadius(),
            config.getMaxSpeed()
        );
        this.cohesion = new CohesionBehavior(
            config.getPerceptionRadius(),
            config.getMaxSpeed(),
            config.getMaxForce()
        );
        this.centerAttraction = new CenterAttraction(config.getMaxSpeed());
    }

    public SchoolingBehavior() {
        this(AquaticConfig.createForFish());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        List<Entity> school = findSchool(context);

        if (school.isEmpty()) {
            return new Vec3d();
        }

        Vec3d separationForce = separation.calculate(context, school);
        Vec3d alignmentForce = alignment.calculate(context, school);
        Vec3d cohesionForce = cohesion.calculate(context, school);
        Vec3d centerForce = centerAttraction.calculate(context, school);

        Vec3d totalForce = new Vec3d();
        totalForce.add(separationForce.mult(config.getSchoolSeparationWeight()));
        totalForce.add(alignmentForce.mult(config.getSchoolAlignmentWeight()));
        totalForce.add(cohesionForce.mult(config.getSchoolCohesionWeight()));
        totalForce.add(centerForce.mult(0.3));

        return limitForce(totalForce, config.getMaxForce());
    }

    private List<Entity> findSchool(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Level level = context.getLevel();
        double perceptionRadius = config.getPerceptionRadius();

        AABB searchBox = new AABB(
            position.x - perceptionRadius, position.y - perceptionRadius, position.z - perceptionRadius,
            position.x + perceptionRadius, position.y + perceptionRadius, position.z + perceptionRadius
        );

        List<Entity> nearbyFish = level.getEntities(self, searchBox, entity -> {
            if (entity == self) return false;
            if (!entity.isAlive()) return false;

            // Check if same species type
            if (!entity.getType().equals(self.getType())) return false;

            // Check distance
            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            double distance = position.distanceTo(entityPos);
            return distance <= perceptionRadius;
        });

        // Sort by distance and keep 6 nearest neighbors (topological)
        return nearbyFish.stream()
            .sorted(Comparator.comparingDouble(entity -> {
                Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                return position.distanceTo(entityPos);
            }))
            .limit(6)
            .collect(Collectors.toList());
    }

    /**
     * Separation behavior - avoid crowding neighbors
     */
    private static class SeparationBehavior {
        private final double separationDistance;
        private final double maxSpeed;
        private final double maxForce;

        SeparationBehavior(double separationDistance, double maxSpeed, double maxForce) {
            this.separationDistance = separationDistance;
            this.maxSpeed = maxSpeed;
            this.maxForce = maxForce;
        }

        Vec3d calculate(BehaviorContext context, List<Entity> school) {
            Vec3d steering = new Vec3d();
            int count = 0;

            Vec3d position = context.getPosition();

            for (Entity other : school) {
                Vec3d otherPos = new Vec3d(other.getX(), other.getY(), other.getZ());
                double distance = position.distanceTo(otherPos);

                if (distance > 0 && distance < separationDistance) {
                    Vec3d diff = Vec3d.sub(position, otherPos);
                    diff.normalize();
                    diff.div(distance);
                    steering.add(diff);
                    count++;
                }
            }

            if (count > 0) {
                steering.div(count);
                steering.normalize();
                steering.mult(maxSpeed);
                steering.sub(context.getVelocity());
                limitForce(steering, maxForce);
            }

            return steering;
        }
    }

    /**
     * Alignment behavior - steer towards average heading of neighbors
     */
    private static class AlignmentBehavior {
        private final double perceptionRadius;
        private final double maxSpeed;

        AlignmentBehavior(double perceptionRadius, double maxSpeed) {
            this.perceptionRadius = perceptionRadius;
            this.maxSpeed = maxSpeed;
        }

        Vec3d calculate(BehaviorContext context, List<Entity> school) {
            Vec3d averageVelocity = new Vec3d();
            int count = 0;

            for (Entity other : school) {
                Vec3d otherVel = new Vec3d(
                    other.getDeltaMovement().x,
                    other.getDeltaMovement().y,
                    other.getDeltaMovement().z
                );
                averageVelocity.add(otherVel);
                count++;
            }

            if (count > 0) {
                averageVelocity.div(count);
                averageVelocity.normalize();
                averageVelocity.mult(maxSpeed);
                Vec3d steering = Vec3d.sub(averageVelocity, context.getVelocity());
                return steering;
            }

            return new Vec3d();
        }
    }

    /**
     * Cohesion behavior - steer towards average position of neighbors
     */
    private static class CohesionBehavior {
        private final double perceptionRadius;
        private final double maxSpeed;
        private final double maxForce;

        CohesionBehavior(double perceptionRadius, double maxSpeed, double maxForce) {
            this.perceptionRadius = perceptionRadius;
            this.maxSpeed = maxSpeed;
            this.maxForce = maxForce;
        }

        Vec3d calculate(BehaviorContext context, List<Entity> school) {
            Vec3d centerOfMass = new Vec3d();
            int count = 0;

            Vec3d position = context.getPosition();

            for (Entity other : school) {
                Vec3d otherPos = new Vec3d(other.getX(), other.getY(), other.getZ());
                centerOfMass.add(otherPos);
                count++;
            }

            if (count > 0) {
                centerOfMass.div(count);
                return seek(centerOfMass, context.getVelocity(), maxSpeed);
            }

            return new Vec3d();
        }

        private Vec3d seek(Vec3d target, Vec3d currentVelocity, double maxSpeed) {
            Vec3d desired = Vec3d.sub(target, currentVelocity);
            desired.normalize();
            desired.mult(maxSpeed);
            Vec3d steer = Vec3d.sub(desired, currentVelocity);
            return steer;
        }

        private void limitForce(Vec3d force, double max) {
            if (force.magnitude() > max) {
                force.normalize();
                force.mult(max);
            }
        }
    }

    /**
     * Center attraction - weak force keeping school loosely centered
     */
    private static class CenterAttraction {
        private final double maxSpeed;

        CenterAttraction(double maxSpeed) {
            this.maxSpeed = maxSpeed;
        }

        Vec3d calculate(BehaviorContext context, List<Entity> school) {
            if (school.isEmpty()) return new Vec3d();

            Vec3d center = new Vec3d();
            Vec3d position = context.getPosition();

            for (Entity other : school) {
                center.add(new Vec3d(other.getX(), other.getY(), other.getZ()));
            }

            center.div(school.size());

            Vec3d desired = Vec3d.sub(center, position);
            desired.normalize();
            desired.mult(maxSpeed * 0.3);

            return Vec3d.sub(desired, context.getVelocity());
        }
    }

    public AquaticConfig getConfig() {
        return config;
    }
}
