package me.javavirtualenv.behavior.pathfinding.steering;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Separation behavior: steers away from nearby neighbors to maintain personal space.
 * This prevents crowding and collisions within groups.
 *
 * Algorithm:
 * 1. For each neighbor within separation distance:
 *    - Calculate repulsion vector: normalize(position - neighborPosition)
 *    - Weight by inverse distance (closer neighbors have stronger influence)
 * 2. Sum all repulsion vectors
 * 3. Normalize and scale by max speed
 *
 * Based on research: separation distance = 2.5 blocks for optimal spacing.
 */
public class SeparationBehavior implements SteeringBehavior {
    private final float weight;
    private final float separationDistance;
    private boolean active;

    /**
     * Creates a separation behavior with default parameters.
     * Uses research-based separation distance of 2.5 blocks.
     */
    public SeparationBehavior() {
        this(1.5f, 2.5f);
    }

    /**
     * Creates a separation behavior with custom parameters.
     *
     * @param weight multiplier for blending with other behaviors
     * @param separationDistance distance threshold for neighbor influence
     */
    public SeparationBehavior(float weight, float separationDistance) {
        this.weight = weight;
        this.separationDistance = separationDistance;
        this.active = true;
    }

    @Override
    public Vec3 calculate(Mob mob, SteeringContext context) {
        List<Entity> nearbyEntities = context.getNearbyEntities();
        if (nearbyEntities == null || nearbyEntities.isEmpty()) {
            return Vec3.ZERO;
        }

        Vec3 currentPosition = mob.position();
        Vec3 totalRepulsion = Vec3.ZERO;
        int neighborCount = 0;

        // Calculate repulsion from each nearby neighbor
        for (Entity neighbor : nearbyEntities) {
            if (neighbor == mob) {
                continue;
            }

            Vec3 neighborPosition = neighbor.position();
            Vec3 offset = currentPosition.subtract(neighborPosition);
            double distance = offset.length();

            // Only consider neighbors within separation distance
            if (distance > 0.01 && distance < separationDistance) {
                // Repulsion weighted by inverse distance (closer = stronger)
                Vec3 repulsion = offset.normalize().scale(1.0 / distance);
                totalRepulsion = totalRepulsion.add(repulsion);
                neighborCount++;
            }
        }

        // No neighbors to separate from
        if (neighborCount == 0) {
            return Vec3.ZERO;
        }

        // Average the repulsion vectors
        totalRepulsion = totalRepulsion.scale(1.0 / neighborCount);

        // Normalize and scale to max speed
        double length = totalRepulsion.length();
        if (length > 0.01) {
            totalRepulsion = totalRepulsion.normalize().scale(context.getMaxSpeed());
        }

        // Subtract current velocity to get steering force
        Vec3 steering = totalRepulsion.subtract(mob.getDeltaMovement());

        return steering;
    }

    @Override
    public float getWeight() {
        return weight;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public float getSeparationDistance() {
        return separationDistance;
    }
}
