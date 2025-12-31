package me.javavirtualenv.behavior.herd;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

/**
 * Quorum-based movement initiation behavior.
 * Animals wait until a threshold percentage of the herd is ready before moving.
 * Based on research showing bison herds require ~47% quorum before collective movement.
 */
public class QuorumMovement extends SteeringBehavior {

    private final HerdConfig config;
    private double lastQuorumCheck = 0.0;
    private boolean movementAllowed = false;
    private int readyCount = 0;
    private int totalAdults = 0;

    public QuorumMovement(HerdConfig config) {
        this.config = config;
    }

    public QuorumMovement(HerdConfig config, double weight) {
        this.config = config;
        this.weight = weight;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!config.isSelfishHerdEnabled()) {
            movementAllowed = true;
            return new Vec3d();
        }

        double currentTime = context.getEntity().level().getGameTime() / 20.0;

        if (currentTime - lastQuorumCheck < config.getQuorumCheckInterval()) {
            if (!movementAllowed) {
                return new Vec3d();
            }
            return new Vec3d();
        }

        lastQuorumCheck = currentTime;
        updateQuorumStatus(context, null);

        if (!movementAllowed) {
            return new Vec3d();
        }

        return new Vec3d();
    }

    /**
     * Updates quorum status based on nearby herd members.
     * Returns true if quorum is met (threshold of adults ready to move).
     */
    public boolean updateQuorumStatus(BehaviorContext context, List<Entity> herdMembers) {
        if (herdMembers == null || herdMembers.isEmpty()) {
            movementAllowed = true;
            return true;
        }

        readyCount = 0;
        totalAdults = 0;

        for (Entity member : herdMembers) {
            if (member.equals(context.getEntity())) {
                continue;
            }

            if (isAdultHerdMember(member)) {
                totalAdults++;
                if (isMemberReadyToMove(member, context)) {
                    readyCount++;
                }
            }
        }

        // Include self in count if adult
        if (isAdultHerdMember(context.getEntity())) {
            totalAdults++;
            readyCount++;
        }

        double quorumRatio = totalAdults > 0 ? (double) readyCount / totalAdults : 1.0;
        movementAllowed = quorumRatio >= config.getQuorumThreshold();

        return movementAllowed;
    }

    /**
     * Checks if an entity is an adult herd member (not a baby).
     */
    private boolean isAdultHerdMember(Entity entity) {
        if (entity instanceof AgeableMob ageable) {
            return ageable.getAge() >= 0;
        }
        if (entity instanceof Animal animal) {
            return !animal.isBaby();
        }
        return true;
    }

    /**
     * Determines if a herd member is ready to move based on its state.
     * Adults that are standing and not occupied are considered ready.
     */
    private boolean isMemberReadyToMove(Entity member, BehaviorContext context) {
        if (!(member instanceof Animal animal)) {
            return true;
        }

        // Don't count if member is sleeping, sitting, or otherwise occupied
        if (animal.isInLove() || animal.isSleeping() || animal.isPassenger()) {
            return false;
        }

        // Check if member is moving or stationary
        Vec3d memberVelocity = new Vec3d(
            member.getDeltaMovement().x,
            member.getDeltaMovement().y,
            member.getDeltaMovement().z
        );

        double memberSpeed = memberVelocity.magnitude();

        // Consider ready if stationary or already moving
        return memberSpeed < 0.1;
    }

    /**
     * Calculates the quorum ratio (ready adults / total adults).
     */
    public double calculateQuorumRatio(BehaviorContext context, List<Entity> herdMembers) {
        if (herdMembers == null || herdMembers.isEmpty()) {
            return 1.0;
        }

        int ready = 0;
        int adults = 0;

        for (Entity member : herdMembers) {
            if (member.equals(context.getEntity())) {
                continue;
            }

            if (isAdultHerdMember(member)) {
                adults++;
                if (isMemberReadyToMove(member, context)) {
                    ready++;
                }
            }
        }

        if (isAdultHerdMember(context.getEntity())) {
            adults++;
            ready++;
        }

        return adults > 0 ? (double) ready / adults : 1.0;
    }

    /**
     * Checks if movement is currently allowed based on quorum.
     */
    public boolean isMovementAllowed() {
        return movementAllowed;
    }

    /**
     * Manually set movement allowed state (for testing or forced movement).
     */
    public void setMovementAllowed(boolean allowed) {
        this.movementAllowed = allowed;
    }

    /**
     * Gets the number of herd members ready to move.
     */
    public int getReadyCount() {
        return readyCount;
    }

    /**
     * Gets the total number of adult herd members.
     */
    public int getTotalAdults() {
        return totalAdults;
    }

    /**
     * Gets the current quorum threshold from config.
     */
    public double getQuorumThreshold() {
        return config.getQuorumThreshold();
    }

    /**
     * Resets quorum state, forcing re-evaluation on next check.
     */
    public void resetQuorum() {
        this.movementAllowed = false;
        this.readyCount = 0;
        this.totalAdults = 0;
        this.lastQuorumCheck = 0.0;
    }

    public HerdConfig getConfig() {
        return config;
    }
}
