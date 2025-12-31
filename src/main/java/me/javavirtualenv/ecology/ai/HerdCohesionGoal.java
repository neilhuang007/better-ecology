package me.javavirtualenv.ecology.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * AI goal for herd cohesion and leader following behavior.
 *
 * Cows will:
 * - Stay close to herd members
 * - Follow dominant/herd leaders
 * - Move as a cohesive group
 * - Become stressed when separated from herd
 *
 * Scientific basis:
 * - Cattle are highly social herd animals
 * - They follow dominance hierarchies
 * - Herds typically move together with leaders in front
 * - Separation from herd causes stress and anxiety
 * - Quorum sensing: ~30% of herd needed to initiate movement
 */
public class HerdCohesionGoal extends Goal {
    private final PathfinderMob mob;
    private final Level level;
    private final double cohesionRange;
    private final double speedModifier;
    private final double minHerdSize;
    private Mob herdLeader;
    private UUID leaderUuid;
    private int followTicks;
    private Vec3 herdCenter;
    private int lastCenterUpdate;

    public HerdCohesionGoal(PathfinderMob mob, double cohesionRange, double speedModifier) {
        this(mob, cohesionRange, speedModifier, 3.0);
    }

    public HerdCohesionGoal(PathfinderMob mob, double cohesionRange, double speedModifier, double minHerdSize) {
        this.mob = mob;
        this.level = mob.level();
        this.cohesionRange = cohesionRange;
        this.speedModifier = speedModifier;
        this.minHerdSize = minHerdSize;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.followTicks = 0;
        this.herdCenter = Vec3.ZERO;
        this.lastCenterUpdate = 0;
    }

    @Override
    public boolean canUse() {
        // Find herd members
        List<Mob> nearbyHerd = getNearbyHerdMembers();

        if (nearbyHerd.size() < minHerdSize) {
            return false;
        }

        // Find or update herd leader
        updateHerdLeader(nearbyHerd);

        return herdLeader != null && herdLeader.isAlive() && !herdLeader.equals(mob);
    }

    @Override
    public boolean canContinueToUse() {
        if (herdLeader == null || !herdLeader.isAlive()) {
            return false;
        }

        double distanceToLeader = mob.distanceToSqr(herdLeader);

        // Continue following leader within range
        return distanceToLeader < cohesionRange * cohesionRange * 2.0;
    }

    @Override
    public void start() {
        followTicks = 0;
        updateHerdCenter();

        // Store leader UUID for persistence
        if (herdLeader != null) {
            leaderUuid = herdLeader.getUUID();
        }
    }

    @Override
    public void stop() {
        herdLeader = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        followTicks++;

        // Update herd center periodically
        if (mob.tickCount - lastCenterUpdate > 100) {
            updateHerdCenter();
        }

        // Check if leader exists and is valid
        if (herdLeader == null || !herdLeader.isAlive()) {
            // Try to find new leader
            List<Mob> nearbyHerd = getNearbyHerdMembers();
            if (!nearbyHerd.isEmpty()) {
                updateHerdLeader(nearbyHerd);
            }
            return;
        }

        Vec3 mobPos = mob.position();
        Vec3 leaderPos = herdLeader.position();
        double distanceToLeader = mob.distanceToSqr(herdLeader);

        // Calculate target position based on herd role
        Vec3 targetPos = calculateTargetPosition(mobPos, leaderPos);

        // Move towards target position
        double currentSpeed = speedModifier;

        // Adjust speed based on distance
        if (distanceToLeader > 64.0) {
            currentSpeed *= 1.3; // Speed up if far from leader
        } else if (distanceToLeader < 9.0) {
            currentSpeed *= 0.6; // Slow down if close
        }

        // Look towards leader/target
        mob.getLookControl().setLookAt(herdLeader);

        // Move if not at target
        double distanceToTarget = mobPos.distanceToSqr(targetPos);
        if (distanceToTarget > 4.0) {
            mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, currentSpeed);
        } else {
            // Close enough, stop moving
            if (followTicks % 20 == 0) {
                mob.getNavigation().stop();
            }
        }

        // Update cohesion status in persistent data
        boolean isCohesive = distanceToLeader < cohesionRange * cohesionRange;
        mob.getPersistentData().putBoolean("better-ecology:herd_cohesive", isCohesive);
    }

    /**
     * Calculate target position based on herd dynamics.
     */
    private Vec3 calculateTargetPosition(Vec3 mobPos, Vec3 leaderPos) {
        // Check if we're the leader
        if (mob.equals(herdLeader)) {
            // Leaders move towards herd center to keep herd together
            return herdCenter;
        }

        double distanceToLeader = mob.distanceToSqr(herdLeader);

        // Calculate position relative to leader
        Vec3 toLeader = leaderPos.subtract(mobPos).normalize();
        double targetDistance = 6.0; // Maintain 6 blocks from leader

        // Add some randomness to prevent stacking
        double angleOffset = (mob.getRandom().nextFloat() - 0.5) * Math.PI * 0.5;
        double cos = Math.cos(angleOffset);
        double sin = Math.sin(angleOffset);

        // Rotate direction slightly
        Vec3 offsetDir = new Vec3(
                toLeader.x * cos - toLeader.z * sin,
                0,
                toLeader.x * sin + toLeader.z * cos
        ).normalize();

        Vec3 targetPos = leaderPos.subtract(offsetDir.scale(targetDistance));

        // Ensure target is on ground
        targetPos = new Vec3(targetPos.x, mobPos.y, targetPos.z);

        return targetPos;
    }

    /**
     * Get nearby herd members of same species.
     */
    private List<Mob> getNearbyHerdMembers() {
        return level.getEntitiesOfClass(
                Mob.class,
                mob.getBoundingBox().inflate(cohesionRange),
                member -> !member.equals(mob) &&
                          member.isAlive() &&
                          isSameSpecies(mob, member)
        );
    }

    /**
     * Update the herd leader based on dominance or random selection.
     */
    private void updateHerdLeader(List<Mob> herdMembers) {
        if (herdMembers.isEmpty()) {
            herdLeader = null;
            return;
        }

        // Check if current leader is still valid
        if (herdLeader != null && herdLeader.isAlive() && herdMembers.contains(herdLeader)) {
            return;
        }

        // Check if we have a stored leader UUID
        if (leaderUuid != null) {
            for (Mob member : herdMembers) {
                if (member.getUUID().equals(leaderUuid)) {
                    herdLeader = member;
                    return;
                }
            }
        }

        // Select new leader based on dominance score
        Mob bestLeader = null;
        int bestDominance = -1;

        for (Mob member : herdMembers) {
            int dominance = member.getPersistentData().getInt("better-ecology:dominance_score");

            if (dominance > bestDominance) {
                bestDominance = dominance;
                bestLeader = member;
            }
        }

        // If no dominance data, pick random member
        if (bestLeader == null) {
            bestLeader = herdMembers.get(mob.getRandom().nextInt(herdMembers.size()));
        }

        herdLeader = bestLeader;
        leaderUuid = herdLeader.getUUID();
    }

    /**
     * Update the center point of the herd.
     */
    private void updateHerdCenter() {
        List<Mob> herdMembers = getNearbyHerdMembers();

        if (herdMembers.isEmpty()) {
            herdCenter = mob.position();
            lastCenterUpdate = mob.tickCount;
            return;
        }

        double sumX = mob.getX();
        double sumY = mob.getY();
        double sumZ = mob.getZ();
        int count = 1;

        for (Mob member : herdMembers) {
            sumX += member.getX();
            sumY += member.getY();
            sumZ += member.getZ();
            count++;
        }

        herdCenter = new Vec3(sumX / count, sumY / count, sumZ / count);
        lastCenterUpdate = mob.tickCount;
    }

    /**
     * Check if two mobs are the same species.
     */
    private boolean isSameSpecies(Mob mob1, Mob mob2) {
        return mob1.getType().equals(mob2.getType());
    }

    // Getters for external access

    public Mob getHerdLeader() {
        return herdLeader;
    }

    public Vec3 getHerdCenter() {
        return herdCenter;
    }

    public int getHerdSize() {
        return getNearbyHerdMembers().size() + 1; // +1 for self
    }

    public boolean isHerdCohesive() {
        if (herdLeader == null) {
            return false;
        }
        return mob.distanceToSqr(herdLeader) < cohesionRange * cohesionRange;
    }
}
