package me.javavirtualenv.behavior.pathfinding.steering;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Cohesion behavior: steers toward the center of mass of nearby group members.
 * This keeps groups together and creates emergent flocking behavior.
 *
 * Algorithm:
 * 1. Calculate average position (center of mass) of same-type neighbors
 * 2. Apply seek behavior toward the center of mass
 * 3. This creates attraction to the group without direct following
 *
 * Only considers same entity type to maintain species-specific groups.
 */
public class CohesionBehavior implements SteeringBehavior {
    private final float weight;
    private final float perceptionRadius;
    private boolean active;

    /**
     * Creates a cohesion behavior with default parameters.
     * Uses perception radius of 10 blocks for group detection.
     */
    public CohesionBehavior() {
        this(1.0f, 10.0f);
    }

    /**
     * Creates a cohesion behavior with custom parameters.
     *
     * @param weight multiplier for blending with other behaviors
     * @param perceptionRadius distance threshold for group membership
     */
    public CohesionBehavior(float weight, float perceptionRadius) {
        this.weight = weight;
        this.perceptionRadius = perceptionRadius;
        this.active = true;
    }

    @Override
    public Vec3 calculate(Mob mob, SteeringContext context) {
        List<Entity> nearbyEntities = context.getNearbyEntities();
        if (nearbyEntities == null || nearbyEntities.isEmpty()) {
            return Vec3.ZERO;
        }

        Vec3 currentPosition = mob.position();
        Vec3 centerOfMass = Vec3.ZERO;
        int groupMemberCount = 0;

        // Calculate center of mass for same-type entities
        for (Entity neighbor : nearbyEntities) {
            if (neighbor == mob) {
                continue;
            }

            // Only consider same entity type (herd mates)
            if (!isSameType(mob, neighbor)) {
                continue;
            }

            Vec3 neighborPosition = neighbor.position();
            double distance = currentPosition.distanceTo(neighborPosition);

            // Only consider neighbors within perception radius
            if (distance > 0.01 && distance < perceptionRadius) {
                centerOfMass = centerOfMass.add(neighborPosition);
                groupMemberCount++;
            }
        }

        // No group members found
        if (groupMemberCount == 0) {
            return Vec3.ZERO;
        }

        // Calculate average position (center of mass)
        centerOfMass = centerOfMass.scale(1.0 / groupMemberCount);

        // Seek toward center of mass
        Vec3 desired = centerOfMass.subtract(currentPosition);
        double distance = desired.length();

        if (distance < 0.01) {
            return Vec3.ZERO;
        }

        // Normalize and scale to max speed
        desired = desired.normalize().scale(context.getMaxSpeed());

        // Steering force = desired velocity - current velocity
        Vec3 steering = desired.subtract(mob.getDeltaMovement());

        return steering;
    }

    /**
     * Checks if two entities are the same type for group cohesion.
     *
     * @param entity1 first entity
     * @param entity2 second entity
     * @return true if entities are the same type
     */
    private boolean isSameType(Entity entity1, Entity entity2) {
        return entity1.getType().equals(entity2.getType());
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

    public float getPerceptionRadius() {
        return perceptionRadius;
    }
}
