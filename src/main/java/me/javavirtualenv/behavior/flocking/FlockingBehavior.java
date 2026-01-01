package me.javavirtualenv.behavior.flocking;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.debug.BehaviorLogger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Combined flocking behavior implementing the classic boids algorithm.
 * Integrates separation, alignment, and cohesion with topological neighbor tracking.
 * <p>
 * Based on research by Ballerini et al. (2008) showing that birds track a fixed
 * number of nearest neighbors (typically 6-7) rather than all within a fixed radius.
 * This topological approach creates more robust, scale-invariant flocking behavior.
 */
public class FlockingBehavior extends SteeringBehavior {

    private final FlockingConfig config;
    private final SeparationBehavior separation;
    private final AlignmentBehavior alignment;
    private final CohesionBehavior cohesion;
    private final NoiseBehavior noise;

    /**
     * Creates flocking behavior with the specified configuration.
     *
     * @param config Flocking parameters (use FlockingConfig presets for species-specific behavior)
     */
    public FlockingBehavior(FlockingConfig config) {
        this.config = config;
        this.separation = new SeparationBehavior(
            config.getSeparationDistance(),
            config.getMaxSpeed(),
            config.getMaxForce(),
            config.getSeparationWeight()
        );
        this.alignment = new AlignmentBehavior(
            config.getPerceptionRadius(),
            config.getMaxSpeed(),
            config.getMaxForce(),
            config.getAlignmentWeight()
        );
        this.cohesion = new CohesionBehavior(
            config.getPerceptionRadius(),
            config.getMaxSpeed(),
            config.getMaxForce(),
            config.getCohesionWeight()
        );
        this.noise = new NoiseBehavior(config.getNoiseWeight());
    }

    /**
     * Creates flocking behavior with default parameters suitable for most bird species.
     */
    public FlockingBehavior() {
        this(new FlockingConfig());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        List<Entity> neighbors = findTopologicalNeighbors(context);

        if (neighbors.isEmpty()) {
            Vec3d noiseOnly = noise.calculateWeighted(context);
            logForceIfVerbose(context.getEntity(), "noise_only", noiseOnly);
            return noiseOnly;
        }

        // Store neighbors in context for sub-behaviors to use
        context.setNeighbors(neighbors);

        // Use calculateWeighted to apply weights once via the base class method
        Vec3d separationForce = separation.calculateWeighted(context);
        Vec3d alignmentForce = alignment.calculateWeighted(context);
        Vec3d cohesionForce = cohesion.calculateWeighted(context);
        Vec3d noiseForce = noise.calculateWeighted(context);

        // Log individual forces when VERBOSE logging is enabled
        logForceIfVerbose(context.getEntity(), "separation", separationForce);
        logForceIfVerbose(context.getEntity(), "alignment", alignmentForce);
        logForceIfVerbose(context.getEntity(), "cohesion", cohesionForce);
        logForceIfVerbose(context.getEntity(), "noise", noiseForce);

        Vec3d totalForce = new Vec3d();
        totalForce.add(separationForce);
        totalForce.add(alignmentForce);
        totalForce.add(cohesionForce);
        totalForce.add(noiseForce);

        logForceIfVerbose(context.getEntity(), "total", totalForce);

        return totalForce;
    }

    /**
     * Logs a flocking force vector if verbose logging is enabled.
     * Uses lazy evaluation to avoid string formatting overhead when disabled.
     */
    private void logForceIfVerbose(Entity entity, String forceType, Vec3d force) {
        if (BehaviorLogger.isVerbose()) {
            BehaviorLogger.logFlockingForce(entity, forceType, force);
        }
    }

    /**
     * Finds topological neighbors - the N nearest entities within perception radius.
     * This approach (tracking fixed count vs. fixed distance) creates more natural flocking.
     *
     * @param context Behavior context containing entity state
     * @return List of nearest neighbors, sorted by distance
     */
    private List<Entity> findTopologicalNeighbors(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        double perceptionRadius = config.getPerceptionRadius();
        double perceptionAngle = config.getPerceptionAngle();
        int neighborCount = config.getTopologicalNeighborCount();

        // Define search area using AABB for efficient spatial querying
        Vec3 mcPos = position.toMinecraftVec3();
        AABB searchBox = new AABB(
            mcPos.x - perceptionRadius, mcPos.y - perceptionRadius, mcPos.z - perceptionRadius,
            mcPos.x + perceptionRadius, mcPos.y + perceptionRadius, mcPos.z + perceptionRadius
        );

        // Get all entities in range
        List<Entity> nearbyEntities = self.level().getEntities(self, searchBox, entity -> {
            if (entity == self) {
                return false;
            }

            // Check actual distance (AABB is an approximation)
            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            double distance = position.distanceTo(entityPos);
            if (distance > perceptionRadius) {
                return false;
            }

            // Check if entity is within perception angle (forward field of view)
            if (!isInFieldOfView(position, context.getVelocity(), entityPos, perceptionAngle)) {
                return false;
            }

            return true;
        });

        // Sort by distance and take nearest N neighbors (topological approach)
        return nearbyEntities.stream()
            .sorted(Comparator.comparingDouble(entity -> {
                Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                return position.distanceTo(entityPos);
            }))
            .limit(neighborCount)
            .collect(Collectors.toList());
    }

    /**
     * Checks if a target entity is within the entity's field of view.
     * Uses dot product to determine if target is within the perception angle.
     *
     * @param position Current position
     * @param velocity Current velocity (indicates forward direction)
     * @param targetPos Target position to check
     * @param perceptionAngle Field of view angle in radians
     * @return true if target is within field of view
     */
    private boolean isInFieldOfView(Vec3d position, Vec3d velocity, Vec3d targetPos, double perceptionAngle) {
        // If velocity is near zero, assume all-around vision
        if (velocity.magnitude() < 0.01) {
            return true;
        }

        Vec3d toTarget = Vec3d.sub(targetPos, position);
        double distance = toTarget.magnitude();

        if (distance < 0.001) {
            return false;
        }

        toTarget.normalize();
        Vec3d forward = velocity.copy();
        forward.normalize();

        // Calculate angle using dot product
        double dotProduct = forward.dot(toTarget);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct)));

        return angle <= perceptionAngle / 2.0;
    }

    /**
     * Returns the current configuration for inspection or modification.
     */
    public FlockingConfig getConfig() {
        return config;
    }

    /**
     * Gets the list of current topological neighbors (useful for debugging/visualization).
     *
     * @param context Behavior context
     * @return List of nearest neighbors
     */
    public List<Entity> getNeighbors(BehaviorContext context) {
        return new ArrayList<>(findTopologicalNeighbors(context));
    }

    /**
     * Gets individual behavior components for fine-grained control.
     */
    public SeparationBehavior getSeparation() {
        return separation;
    }

    public AlignmentBehavior getAlignment() {
        return alignment;
    }

    public CohesionBehavior getCohesion() {
        return cohesion;
    }

    public NoiseBehavior getNoise() {
        return noise;
    }
}
