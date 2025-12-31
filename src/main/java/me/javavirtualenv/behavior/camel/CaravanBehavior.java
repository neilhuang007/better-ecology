package me.javavirtualenv.behavior.camel;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caravan behavior for camels.
 * <p>
 * Camels naturally form caravans (lines) when traveling together:
 * - Follow a lead camel in single file
 * - Reduced exhaustion when traveling in groups
 * - Pack coordination for efficient movement
 * - Speed bonus when following other camels
 * <p>
 * Scientific basis: Camels are social animals that naturally form caravans
 * when traveling, following a leader in single file. This behavior reduces
 * energy expenditure and provides safety in numbers.
 */
public class CaravanBehavior extends SteeringBehavior {

    private final CamelConfig config;

    // Caravan state
    private UUID caravanLeaderId = null;
    private UUID followTargetId = null;
    private int caravanPosition = -1; // Position in caravan (0 = leader)
    private boolean isLeading = false;

    public CaravanBehavior(CamelConfig config) {
        this.config = config;
    }

    public CaravanBehavior() {
        this(CamelConfig.createDefault());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!enabled) {
            return new Vec3d();
        }

        Mob entity = context.getEntity();

        // Update caravan membership
        updateCaravanMembership(context);

        // If leading, no steering (followers will follow)
        if (isLeading) {
            return new Vec3d();
        }

        // Follow the camel ahead in caravan
        Camel target = findFollowTarget(context);
        if (target != null) {
            followTargetId = target.getUUID();
            return calculateFollowForce(context, target);
        }

        // No target to follow
        followTargetId = null;
        return new Vec3d();
    }

    /**
     * Updates caravan membership and position.
     */
    private void updateCaravanMembership(BehaviorContext context) {
        Mob entity = context.getEntity();

        // Find nearby camels
        List<Camel> nearbyCamels = findNearbyCamels(context);

        if (nearbyCamels.isEmpty()) {
            // No camels nearby, become leader of own caravan
            isLeading = true;
            caravanPosition = 0;
            caravanLeaderId = entity.getUUID();
            return;
        }

        // Check if already in a caravan
        Camel existingCaravan = findExistingCaravan(context, nearbyCamels);
        if (existingCaravan != null) {
            // Join existing caravan
            joinCaravan(context, existingCaravan);
            return;
        }

        // Start new caravan as leader
        isLeading = true;
        caravanPosition = 0;
        caravanLeaderId = entity.getUUID();
    }

    /**
     * Finds camels nearby.
     */
    private List<Camel> findNearbyCamels(BehaviorContext context) {
        Mob entity = context.getEntity();
        double range = config.getCaravanFollowRange();

        List<Camel> nearbyCamels = entity.level().getEntitiesOfClass(
            Camel.class,
            entity.getBoundingBox().inflate(range)
        );

        // Remove self
        nearbyCamels.removeIf(camel -> camel == entity);

        return nearbyCamels;
    }

    /**
     * Finds an existing caravan to join.
     */
    private Camel findExistingCaravan(BehaviorContext context, List<Camel> nearbyCamels) {
        for (Camel camel : nearbyCamels) {
            if (isInCaravan(camel)) {
                return camel;
            }
        }
        return null;
    }

    /**
     * Checks if a camel is in a caravan.
     */
    private boolean isInCaravan(Camel camel) {
        if (camel instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            var tag = component.getHandleTag("caravan");
            return tag.contains("caravanLeaderId");
        }
        return false;
    }

    /**
     * Joins an existing caravan.
     */
    private void joinCaravan(BehaviorContext context, Camel caravanMember) {
        Mob entity = context.getEntity();

        // Get caravan leader from the member
        UUID leaderId = getCaravanLeaderId(caravanMember);
        if (leaderId != null) {
            caravanLeaderId = leaderId;
            isLeading = false;

            // Calculate position in caravan (behind the member we're joining)
            caravanPosition = getCaravanPosition(caravanMember) + 1;

            // Limit caravan size
            if (caravanPosition >= config.getMaxCaravanSize()) {
                // Caravan is full, start a new one
                isLeading = true;
                caravanPosition = 0;
                caravanLeaderId = entity.getUUID();
            }

            // Store caravan info
            if (entity instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
                var component = access.betterEcology$getEcologyComponent();
                var tag = component.getHandleTag("caravan");
                tag.putUUID("caravanLeaderId", caravanLeaderId);
                tag.putInt("caravanPosition", caravanPosition);
                tag.putBoolean("isLeading", isLeading);
            }
        }
    }

    /**
     * Gets the caravan leader ID from a camel.
     */
    private UUID getCaravanLeaderId(Camel camel) {
        if (camel instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            var tag = component.getHandleTag("caravan");
            if (tag.hasUUID("caravanLeaderId")) {
                return tag.getUUID("caravanLeaderId");
            }
        }
        return null;
    }

    /**
     * Gets the caravan position from a camel.
     */
    private int getCaravanPosition(Camel camel) {
        if (camel instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            var tag = component.getHandleTag("caravan");
            return tag.getInt("caravanPosition");
        }
        return 0;
    }

    /**
     * Finds the camel to follow in the caravan.
     */
    private Camel findFollowTarget(BehaviorContext context) {
        Mob entity = context.getEntity();
        List<Camel> nearbyCamels = findNearbyCamels(context);

        // Find camels in the same caravan with lower position
        Camel bestTarget = null;
        int bestPosition = -1;

        for (Camel camel : nearbyCamels) {
            UUID leaderId = getCaravanLeaderId(camel);
            if (leaderId != null && leaderId.equals(caravanLeaderId)) {
                int position = getCaravanPosition(camel);
                // Follow the camel immediately ahead
                if (position == caravanPosition - 1) {
                    return camel;
                }
                // Or find the closest camel with a lower position
                if (position < caravanPosition && position > bestPosition) {
                    bestPosition = position;
                    bestTarget = camel;
                }
            }
        }

        return bestTarget;
    }

    /**
     * Calculates the steering force to follow the target camel.
     */
    private Vec3d calculateFollowForce(BehaviorContext context, Camel target) {
        Vec3d position = context.getPosition();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());

        // Calculate desired position behind the target
        Vec3d toTarget = Vec3d.sub(targetPos, position);
        double distance = toTarget.magnitude();

        // Desired following distance
        double followDistance = config.getCaravanCohesionDistance();

        if (distance < followDistance * 0.5) {
            // Too close, slow down
            return new Vec3d();
        }

        if (distance <= followDistance) {
            // At good following distance
            return new Vec3d();
        }

        // Move towards target
        Vec3d desired = toTarget.copy();
        desired.normalize();
        desired.mult(context.getSpeed() * (1.0 + config.getCaravanSpeedBonus()));

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());

        return steer;
    }

    /**
     * Applies the exhaustion reduction benefit from caravan travel.
     */
    public static double applyCaravanExhaustionMultiplier(Mob mob, double baseExhaustion) {
        if (!(mob instanceof Camel)) {
            return baseExhaustion;
        }

        if (mob instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            var tag = component.getHandleTag("caravan");

            boolean isInCaravan = tag.contains("caravanLeaderId") && !tag.getBoolean("isLeading");

            if (isInCaravan) {
                CamelConfig config = CamelConfig.createDefault();
                return baseExhaustion * config.getCaravanExhaustionMultiplier();
            }
        }

        return baseExhaustion;
    }

    // Getters for external query
    public boolean isLeading() {
        return isLeading;
    }

    public int getCaravanPosition() {
        return caravanPosition;
    }

    public UUID getCaravanLeaderId() {
        return caravanLeaderId;
    }

    public UUID getFollowTargetId() {
        return followTargetId;
    }

    /**
     * Forces this camel to become a caravan leader.
     */
    public void becomeLeader(BehaviorContext context) {
        Mob entity = context.getEntity();
        isLeading = true;
        caravanPosition = 0;
        caravanLeaderId = entity.getUUID();
        followTargetId = null;

        if (entity instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            var tag = component.getHandleTag("caravan");
            tag.putUUID("caravanLeaderId", caravanLeaderId);
            tag.putInt("caravanPosition", caravanPosition);
            tag.putBoolean("isLeading", isLeading);
        }
    }

    /**
     * Forces this camel to leave the caravan.
     */
    public void leaveCaravan(BehaviorContext context) {
        Mob entity = context.getEntity();
        isLeading = true;
        caravanPosition = 0;
        caravanLeaderId = entity.getUUID();
        followTargetId = null;

        if (entity instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            var tag = component.getHandleTag("caravan");
            tag.putUUID("caravanLeaderId", caravanLeaderId);
            tag.putInt("caravanPosition", caravanPosition);
            tag.putBoolean("isLeading", isLeading);
        }
    }
}
