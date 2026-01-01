package me.javavirtualenv.behavior.ai;

import me.javavirtualenv.behavior.BehaviorRegistry;
import me.javavirtualenv.behavior.BehaviorWeights;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * AI goal that applies steering behaviors to entity movement.
 * <p>
 * This goal uses the BehaviorHandle to calculate steering forces from
 * multiple weighted behaviors and applies them to the entity's movement
 * each tick. The steering forces are limited to a maximum force to prevent
 * unrealistic movement.
 */
public class SteeringBehaviorGoal extends Goal {

    private final Mob mob;
    private final Supplier<BehaviorRegistry> registrySupplier;
    private final Supplier<BehaviorWeights> weightsSupplier;
    private final double maxForce;
    private final double maxSpeed;

    private BehaviorRegistry cachedRegistry;
    private BehaviorWeights cachedWeights;

    /**
     * Creates a new steering behavior goal.
     *
     * @param mob The entity this goal controls
     * @param registrySupplier Supplier for the behavior registry
     * @param weightsSupplier Supplier for behavior weights
     * @param maxForce Maximum steering force to apply
     * @param maxSpeed Maximum movement speed
     */
    public SteeringBehaviorGoal(Mob mob,
                               Supplier<BehaviorRegistry> registrySupplier,
                               Supplier<BehaviorWeights> weightsSupplier,
                               double maxForce,
                               double maxSpeed) {
        this.mob = mob;
        this.registrySupplier = registrySupplier;
        this.weightsSupplier = weightsSupplier;
        this.maxForce = maxForce;
        this.maxSpeed = maxSpeed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        applySteeringForces();
    }

    /**
     * Calculates and applies steering forces to entity movement.
     */
    private void applySteeringForces() {
        if (cachedRegistry == null) {
            cachedRegistry = registrySupplier.get();
        }
        if (cachedWeights == null) {
            cachedWeights = weightsSupplier.get();
        }

        if (cachedRegistry == null || cachedWeights == null) {
            return;
        }

        // Don't apply steering when in water or when pathfinding is active
        if (shouldDeferToPathfinding()) {
            return;
        }

        BehaviorContext context = new BehaviorContext(mob);

        // Query nearby same-type entities for flocking behaviors
        List<Mob> nearbyMobs = SpatialIndex.getNearbySameType(mob, 16);
        context.setNeighbors(new ArrayList<>(nearbyMobs));

        // Calculate combined steering force
        Vec3d steeringForce = cachedRegistry.calculate(context, cachedWeights);

        // Limit force to max
        steeringForce.limit(maxForce);

        // Apply to entity movement
        if (steeringForce.magnitude() > 0.001) {
            Vec3 mcSteering = steeringForce.toMinecraftVec3();
            Vec3 currentMovement = mob.getDeltaMovement();
            Vec3 newMovement = currentMovement.add(mcSteering);

            // Clamp horizontal speed while preserving vertical (gravity) component
            double horizontalSpeed = Math.sqrt(newMovement.x * newMovement.x + newMovement.z * newMovement.z);
            if (horizontalSpeed > maxSpeed) {
                double scale = maxSpeed / horizontalSpeed;
                newMovement = new Vec3(
                    newMovement.x * scale,
                    newMovement.y,  // Preserve Y component (gravity)
                    newMovement.z * scale
                );
            }

            mob.setDeltaMovement(newMovement);
        }
    }

    /**
     * Checks if steering should defer to pathfinding/water avoidance.
     * Returns true when the mob is in water or has an active path.
     */
    private boolean shouldDeferToPathfinding() {
        // Don't interfere with water navigation
        if (mob.isInWater() || mob.isInWaterOrBubble()) {
            return true;
        }

        // Don't interfere with active pathfinding
        if (mob.getNavigation() != null && mob.getNavigation().isInProgress()) {
            return true;
        }

        return false;
    }

    /**
     * Sets the behavior registry to use.
     * Called by BehaviorHandle during initialization.
     */
    public void setRegistry(BehaviorRegistry registry) {
        this.cachedRegistry = registry;
    }

    /**
     * Sets the behavior weights to use.
     * Called by BehaviorHandle during initialization.
     */
    public void setWeights(BehaviorWeights weights) {
        this.cachedWeights = weights;
    }
}
