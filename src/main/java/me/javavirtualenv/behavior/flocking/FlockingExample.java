package me.javavirtualenv.behavior.flocking;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Mob;

/**
 * Example usage of flocking behavior in Minecraft entities.
 * <p>
 * This example demonstrates how to integrate flocking behavior into entity AI.
 * In practice, you would call calculateFlockingForce() from your entity's
 * custom movement goal or AI behavior.
 * <p>
 * Example integration:
 * <pre>{@code
 * public class BirdEntity extends PathfinderMob {
 *     private final FlockingBehavior flocking = new FlockingBehavior(FlockingConfig.forMurmurations());
 *
 *     public void tick() {
 *         super.tick();
 *         BehaviorContext context = new BehaviorContext(this);
 *         Vec3d flockingForce = flocking.calculate(context);
 *         // Apply force to movement...
 *     }
 * }
 * }</pre>
 */
public class FlockingExample {

    /**
     * Basic flocking integration example.
     */
    public Vec3d calculateFlockingForce(Mob entity) {
        // Create behavior with default config
        FlockingBehavior flocking = new FlockingBehavior();

        // Build context from entity
        BehaviorContext context = new BehaviorContext(entity);

        // Calculate combined flocking force
        Vec3d force = flocking.calculate(context);

        return force;
    }

    /**
     * Species-specific configuration example.
     */
    public Vec3d calculateMurmurationForce(Mob entity) {
        // Use murmuration preset (starling-like behavior)
        FlockingConfig config = FlockingConfig.forMurmurations();
        FlockingBehavior flocking = new FlockingBehavior(config);

        BehaviorContext context = new BehaviorContext(entity);
        return flocking.calculate(context);
    }

    /**
     * V-formation flight example (geese, migratory birds).
     */
    public Vec3d calculateVFormationForce(Mob entity) {
        // Use V-formation preset
        FlockingConfig config = FlockingConfig.forVFormation();
        FlockingBehavior flocking = new FlockingBehavior(config);

        BehaviorContext context = new BehaviorContext(entity);
        return flocking.calculate(context);
    }

    /**
     * Custom configuration example.
     */
    public Vec3d calculateCustomFlockingForce(Mob entity) {
        // Create custom config
        FlockingConfig config = new FlockingConfig();
        config.setSeparationWeight(3.0);
        config.setAlignmentWeight(1.0);
        config.setCohesionWeight(1.0);
        config.setTopologicalNeighborCount(5);
        config.setPerceptionRadius(10.0);
        config.setMaxSpeed(0.7);

        FlockingBehavior flocking = new FlockingBehavior(config);

        BehaviorContext context = new BehaviorContext(entity);
        return flocking.calculate(context);
    }

    /**
     * Dynamic adjustment example based on flock size.
     */
    public Vec3d calculateAdaptiveFlockingForce(Mob entity, int nearbyFlockSize) {
        FlockingConfig config = new FlockingConfig();

        // Adjust behavior based on flock size
        if (nearbyFlockSize > 50) {
            // Large murmuration - stronger cohesion, wider perception
            config.setCohesionWeight(1.5);
            config.setPerceptionRadius(25.0);
            config.setTopologicalNeighborCount(7);
        } else if (nearbyFlockSize > 10) {
            // Medium flock - balanced behavior
            config.setCohesionWeight(1.3);
            config.setPerceptionRadius(16.0);
            config.setTopologicalNeighborCount(6);
        } else {
            // Small group - tighter cohesion, less separation needed
            config.setCohesionWeight(1.8);
            config.setSeparationWeight(2.0);
            config.setPerceptionRadius(12.0);
            config.setTopologicalNeighborCount(4);
        }

        FlockingBehavior flocking = new FlockingBehavior(config);
        BehaviorContext context = new BehaviorContext(entity);
        return flocking.calculate(context);
    }

    /**
     * Applying flocking force to entity movement.
     */
    public void applyFlockingForce(Mob entity, Vec3d flockingForce, double weight) {
        // Scale the force
        flockingForce.mult(weight);

        // Limit to maximum force
        double maxForce = 0.15; // Typical max force
        flockingForce.limit(maxForce);

        // Apply to entity's delta movement (velocity)
        Vec3d currentDelta = new Vec3d(
            entity.getDeltaMovement().x,
            entity.getDeltaMovement().y,
            entity.getDeltaMovement().z
        );

        currentDelta.add(flockingForce);

        // Limit speed
        double maxSpeed = 0.8; // Typical max speed
        currentDelta.limit(maxSpeed);

        // Apply to entity
        entity.setDeltaMovement(
            currentDelta.toMinecraftVec3()
        );
    }
}
