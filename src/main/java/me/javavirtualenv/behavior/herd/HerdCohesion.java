package me.javavirtualenv.behavior.herd;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

/**
 * Herd cohesion behavior with selfish positioning.
 * Keeps the group together while younger/weaker animals seek center positions for safety.
 * Based on selfish herd theory - individuals minimize predation risk by positioning
 * other conspecifics between themselves and predators.
 */
public class HerdCohesion extends SteeringBehavior {

    private final HerdConfig config;

    public HerdCohesion(HerdConfig config) {
        this.config = config;
    }

    public HerdCohesion(HerdConfig config, double weight) {
        this.config = config;
        this.weight = weight;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        return calculateCohesion(context, null);
    }

    /**
     * Calculates cohesion steering force toward herd center.
     * Applies selfish positioning for vulnerable animals (babies, weak adults).
     */
    public Vec3d calculateCohesion(BehaviorContext context, List<Entity> herdMembers) {
        if (herdMembers == null || herdMembers.isEmpty()) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();

        // Calculate herd center of mass
        Vec3d herdCenter = calculateHerdCenter(context, herdMembers);

        if (herdCenter == null) {
            return new Vec3d();
        }

        // Calculate edge status - how close to the perimeter
        double edgeFactor = calculateEdgeFactor(context, herdMembers, herdCenter);

        // Vulnerable animals feel stronger pull toward center
        double vulnerabilityFactor = calculateVulnerability(context.getEntity());

        // Base cohesion force toward herd center
        Vec3d steer = seek(position, velocity, herdCenter, config.getMaxSpeed());

        // Apply selfish herd bias - avoid edges
        if (config.isSelfishHerdEnabled()) {
            double selfishBias = edgeFactor * vulnerabilityFactor * config.getSelfishHerdStrength();
            steer.mult(1.0 + selfishBias);
        }

        // Apply overall cohesion strength
        steer.mult(config.getCohesionStrength());

        steer.limit(config.getMaxForce());
        return steer;
    }

    /**
     * Calculates the center of mass of the herd.
     */
    public Vec3d calculateHerdCenter(BehaviorContext context, List<Entity> herdMembers) {
        Vec3d center = new Vec3d();
        int count = 0;

        for (Entity member : herdMembers) {
            if (member.equals(context.getEntity())) {
                continue;
            }

            Vec3d memberPos = new Vec3d(member.getX(), member.getY(), member.getZ());
            double distance = context.getPosition().distanceTo(memberPos);

            if (distance < config.getCohesionRadius()) {
                center.add(memberPos);
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        center.div(count);
        return center;
    }

    /**
     * Calculates how close the entity is to the herd edge.
     * Returns 0.0 (at center) to 1.0 (at edge).
     */
    public double calculateEdgeFactor(BehaviorContext context, List<Entity> herdMembers, Vec3d herdCenter) {
        if (herdCenter == null) {
            return 0.0;
        }

        Vec3d position = context.getPosition();
        double distanceToCenter = position.distanceTo(herdCenter);

        // Calculate average distance of all herd members from center
        double totalDistance = 0.0;
        int count = 0;

        for (Entity member : herdMembers) {
            if (member.equals(context.getEntity())) {
                continue;
            }

            Vec3d memberPos = new Vec3d(member.getX(), member.getY(), member.getZ());
            double distance = memberPos.distanceTo(herdCenter);
            totalDistance += distance;
            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        double averageDistance = totalDistance / count;

        // Edge factor increases as we get further from center than average
        if (distanceToCenter <= averageDistance * 0.5) {
            return 0.0; // Well within center
        }

        double edgeFactor = (distanceToCenter - averageDistance * 0.5) / (averageDistance * 0.5);
        return Math.min(1.0, Math.max(0.0, edgeFactor));
    }

    /**
     * Calculates vulnerability factor (0.0 to 1.0).
     * Vulnerable animals: babies, weak health, young adults.
     */
    public double calculateVulnerability(Entity entity) {
        double vulnerability = 0.0;

        if (entity instanceof Animal animal) {
            // Babies are most vulnerable
            if (animal.isBaby()) {
                vulnerability = 1.0;
            } else if (entity instanceof AgeableMob ageable) {
                // Young adults have some vulnerability
                int age = ageable.getAge();
                if (age < 100) {
                    vulnerability = 0.7 - (age / 100.0) * 0.7;
                }
            }

            // Low health increases vulnerability
            double healthPercent = animal.getHealth() / animal.getMaxHealth();
            if (healthPercent < 0.5) {
                vulnerability += (0.5 - healthPercent);
            }
        }

        return Math.min(1.0, Math.max(0.0, vulnerability));
    }

    /**
     * Calculates separation force to maintain minimum spacing.
     * Prevents herd members from getting too close.
     */
    public Vec3d calculateSeparation(BehaviorContext context, List<Entity> herdMembers) {
        Vec3d position = context.getPosition();
        Vec3d steer = new Vec3d();
        int count = 0;

        for (Entity member : herdMembers) {
            if (member.equals(context.getEntity())) {
                continue;
            }

            Vec3d memberPos = new Vec3d(member.getX(), member.getY(), member.getZ());
            double distance = position.distanceTo(memberPos);

            if (distance > 0 && distance < config.getSeparationDistance()) {
                Vec3d diff = Vec3d.sub(position, memberPos);
                diff.normalize();
                diff.div(distance); // Weight by distance (closer = stronger)
                steer.add(diff);
                count++;
            }
        }

        if (count > 0) {
            steer.div(count);
            steer.normalize();
            steer.mult(config.getMaxSpeed());
            steer.sub(context.getVelocity());
            steer.limit(config.getMaxForce());
        }

        return steer;
    }

    /**
     * Combined cohesion and separation for balanced herd behavior.
     */
    public Vec3d calculateBalancedCohesion(BehaviorContext context, List<Entity> herdMembers) {
        Vec3d cohesion = calculateCohesion(context, herdMembers);
        Vec3d separation = calculateSeparation(context, herdMembers);

        // Separation has higher priority to prevent crowding
        separation.mult(1.5);
        cohesion.mult(1.0);

        Vec3d result = cohesion.copy();
        result.add(separation);
        return result;
    }

    /**
     * Checks if the entity is at the edge of the herd.
     */
    public boolean isAtHerdEdge(BehaviorContext context, List<Entity> herdMembers) {
        Vec3d herdCenter = calculateHerdCenter(context, herdMembers);
        if (herdCenter == null) {
            return false;
        }

        double edgeFactor = calculateEdgeFactor(context, herdMembers, herdCenter);
        return edgeFactor > 0.7;
    }

    /**
     * Gets the cohesion radius from config.
     */
    public double getCohesionRadius() {
        return config.getCohesionRadius();
    }

    /**
     * Gets the separation distance from config.
     */
    public double getSeparationDistance() {
        return config.getSeparationDistance();
    }

    public HerdConfig getConfig() {
        return config;
    }
}
