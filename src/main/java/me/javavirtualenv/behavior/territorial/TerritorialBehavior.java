package me.javavirtualenv.behavior.territorial;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;

/**
 * Steering behavior that implements territorial defense.
 * Animals with territories will:
 * 1. Stay within their defended territory boundaries
 * 2. Actively patrol and defend against intruders
 * 3. Be more aggressive near the territory core
 * <p>
 * Based on research from:
 * - Brown, J.L. (1964). The evolution of diversity in avian territorial systems
 * - Maher, C.R., & Lott, D.F. (2000). A review of ecological determinants of territoriality
 * - Adams, E.S. (2001). Approaches to the study of territoriality
 */
public class TerritorialBehavior extends SteeringBehavior {
    private final Territory territory;
    private final double coreRadiusRatio;
    private final double defenseStrength;
    private final double patrolSpeed;
    private final double boundaryRetentionForce;
    private final boolean markBoundaries;

    public TerritorialBehavior(Territory territory, double coreRadiusRatio, double defenseStrength,
                               double patrolSpeed, double boundaryRetentionForce, boolean markBoundaries) {
        this.territory = territory;
        this.coreRadiusRatio = Math.max(0.3, Math.min(1.0, coreRadiusRatio));
        this.defenseStrength = defenseStrength;
        this.patrolSpeed = patrolSpeed;
        this.boundaryRetentionForce = boundaryRetentionForce;
        this.markBoundaries = markBoundaries;
    }

    public TerritorialBehavior(Territory territory) {
        this(territory, 0.5, 0.3, 0.6, 0.4, true);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        BlockPos currentPos = context.getBlockPos();

        // Check if outside territory - strong force to return
        if (!territory.contains(currentPos)) {
            return calculateReturnForce(context);
        }

        // Check for intruders in territory
        Vec3d intruderForce = calculateIntruderResponse(context);
        if (intruderForce.magnitude() > 0.01) {
            return intruderForce;
        }

        // Patrol behavior within territory
        return calculatePatrolForce(context);
    }

    /**
     * Calculates force to return to territory when outside boundaries.
     * Force increases with distance from territory edge.
     */
    private Vec3d calculateReturnForce(BehaviorContext context) {
        Vec3d currentPos = context.getPosition();
        Vec3d territoryCenter = new Vec3d(
                territory.getCenter().getX() + 0.5,
                territory.getCenter().getY(),
                territory.getCenter().getZ() + 0.5
        );

        // Calculate distance to territory edge
        double distanceToEdge = territory.distanceToEdge(context.getBlockPos());

        // Force increases with distance beyond boundary
        double forceMagnitude = boundaryRetentionForce * (1.0 + distanceToEdge * 0.1);

        Vec3d toCenter = Vec3d.sub(territoryCenter, currentPos);
        toCenter.normalize();
        toCenter.mult(Math.min(forceMagnitude, context.getMaxForce()));

        return toCenter;
    }

    /**
     * Calculates response to intruders in territory.
     * Stronger response near territory core.
     */
    private Vec3d calculateIntruderResponse(BehaviorContext context) {
        Vec3d currentPos = context.getPosition();
        Vec3d totalForce = new Vec3d();
        int intruderCount = 0;

        List<Entity> nearbyEntities = context.getNearbyEntities();
        double coreRadius = territory.getRadius() * coreRadiusRatio;
        double distanceFromCenter = territory.distanceToCenter(context.getBlockPos());
        boolean isInCore = distanceFromCenter < coreRadius;

        for (Entity entity : nearbyEntities) {
            if (shouldDefendAgainst(entity, context)) {
                BlockPos entityPos = new BlockPos((int) entity.getX(), (int) entity.getY(), (int) entity.getZ());

                // Only respond if intruder is in our territory
                if (!territory.contains(entityPos)) {
                    continue;
                }

                Vec3d intruderPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                double distanceToIntruder = currentPos.distanceTo(intruderPos);

                // Detection range based on position in territory
                // When in core territory, use full territory radius for detection
                // When in outer territory, use core radius for focused defense
                double detectionRange = isInCore ? territory.getRadius() : coreRadius;

                if (distanceToIntruder <= detectionRange) {
                    // Urgency increases near core territory
                    double urgency = 1.0 - (distanceFromCenter / territory.getRadius());
                    urgency = Math.max(0.3, Math.min(1.0, urgency));

                    Vec3d toIntruder = Vec3d.sub(intruderPos, currentPos);
                    toIntruder.normalize();
                    toIntruder.mult(defenseStrength * urgency);

                    totalForce.add(toIntruder);
                    intruderCount++;
                }
            }
        }

        // Limit total defense force
        totalForce.limit(context.getMaxForce());
        return totalForce;
    }

    /**
     * Calculates patrol force within territory boundaries.
     * Animals should patrol between core area and boundaries.
     */
    private Vec3d calculatePatrolForce(BehaviorContext context) {
        Vec3d currentPos = context.getPosition();
        BlockPos territoryCenter = territory.getCenter();
        Vec3d centerVec = new Vec3d(
                territoryCenter.getX() + 0.5,
                territoryCenter.getY(),
                territoryCenter.getZ() + 0.5
        );

        double distanceFromCenter = currentPos.distanceTo(centerVec);
        double coreRadius = territory.getRadius() * coreRadiusRatio;
        double boundaryThreshold = territory.getRadius() * 0.85;

        // If near boundary, return toward center
        if (distanceFromCenter > boundaryThreshold) {
            Vec3d toCenter = Vec3d.sub(centerVec, currentPos);
            toCenter.normalize();
            toCenter.mult(patrolSpeed * 0.5);

            Vec3d steer = Vec3d.sub(toCenter, context.getVelocity());
            steer.limit(context.getMaxForce() * 0.5);
            return steer;
        }

        // If too close to center, move outward
        if (distanceFromCenter < coreRadius * 0.4) {
            Vec3d fromCenter = Vec3d.sub(currentPos, centerVec);
            fromCenter.normalize();
            fromCenter.mult(patrolSpeed * 0.3);

            Vec3d steer = Vec3d.sub(fromCenter, context.getVelocity());
            steer.limit(context.getMaxForce() * 0.3);
            return steer;
        }

        // Within patrol range - no force needed
        return new Vec3d();
    }

    /**
     * Determines if an entity should be defended against.
     * Default implementation checks for same-species rivals.
     * Subclasses can override for species-specific behavior.
     */
    protected boolean shouldDefendAgainst(Entity entity) {
        // Base implementation - defend against same-species entities
        // Subclasses can override for more specific behavior (e.g., predators, competitors)
        return false;
    }

    /**
     * Determines if an entity should be defended against with context.
     * Default implementation checks if entity is same species as territory owner.
     */
    protected boolean shouldDefendAgainst(Entity entity, BehaviorContext context) {
        // Don't defend against self
        if (entity.getUUID().equals(context.getSelf().getUUID())) {
            return false;
        }

        // Default behavior: defend against same-species entities
        // This handles territorial competition within the same species
        boolean isSameSpecies = entity.getType().equals(context.getEntity().getType());
        if (isSameSpecies) {
            return true;
        }

        // Allow subclass to override with additional logic
        return shouldDefendAgainst(entity);
    }

    /**
     * Checks if a position is within the territory core.
     * Core territory is defended more aggressively.
     */
    public boolean isInCoreTerritory(BlockPos position) {
        double distanceFromCenter = territory.distanceToCenter(position);
        double coreRadius = territory.getRadius() * coreRadiusRatio;
        return distanceFromCenter <= coreRadius;
    }

    /**
     * Checks if a position is near the territory boundary.
     */
    public boolean isNearBoundary(BlockPos position) {
        double distanceFromCenter = territory.distanceToCenter(position);
        double boundaryThreshold = territory.getRadius() * 0.85;
        return distanceFromCenter >= boundaryThreshold && distanceFromCenter <= territory.getRadius();
    }

    /**
     * Gets the territory being defended.
     */
    public Territory getTerritory() {
        return territory;
    }

    /**
     * Gets the core territory radius ratio.
     */
    public double getCoreRadiusRatio() {
        return coreRadiusRatio;
    }

    /**
     * Gets the defense strength parameter.
     */
    public double getDefenseStrength() {
        return defenseStrength;
    }

    /**
     * Checks if boundary marking is enabled.
     */
    public boolean shouldMarkBoundaries() {
        return markBoundaries;
    }

    /**
     * Gets the patrol speed parameter.
     */
    public double getPatrolSpeed() {
        return patrolSpeed;
    }
}
