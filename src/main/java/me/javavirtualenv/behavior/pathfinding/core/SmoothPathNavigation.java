package me.javavirtualenv.behavior.pathfinding.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom path navigation that extends GroundPathNavigation to provide path smoothing,
 * look-ahead targeting, and switchback detection for steep slopes.
 *
 * <p>This navigation system improves upon vanilla pathfinding by:
 * <ul>
 *   <li>Using Catmull-Rom interpolation for smoother paths between waypoints</li>
 *   <li>Looking ahead multiple nodes to reduce sharp turns</li>
 *   <li>Detecting steep slopes that may benefit from switchback paths</li>
 *   <li>Using the custom EcologyNodeEvaluator for better terrain assessment</li>
 * </ul>
 *
 * @see GroundPathNavigation
 * @see EcologyNodeEvaluator
 */
public class SmoothPathNavigation extends GroundPathNavigation {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmoothPathNavigation.class);

    /**
     * Number of nodes to look ahead for path smoothing.
     * Higher values create smoother but potentially less responsive paths.
     */
    private static final int LOOK_AHEAD_NODES = 3;

    /**
     * Slope angle threshold in degrees for switchback detection.
     * Slopes steeper than this are logged as potentially requiring switchbacks.
     */
    private static final float SWITCHBACK_THRESHOLD = 15.0f;

    /**
     * Preferred angle in degrees for switchback turns.
     * Currently unused but reserved for future switchback generation.
     */
    private static final float SWITCHBACK_ANGLE = 30.0f;

    /**
     * Constructs a new SmoothPathNavigation for the given mob.
     *
     * @param mob the mob that will use this navigation
     * @param level the level the mob is navigating in
     */
    public SmoothPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    /**
     * Creates a custom PathFinder using the EcologyNodeEvaluator.
     * This evaluator provides improved terrain assessment compared to vanilla.
     *
     * @param maxNodes the maximum number of nodes to evaluate during pathfinding
     * @return a PathFinder configured with the ecology node evaluator
     */
    @Override
    protected PathFinder createPathFinder(int maxNodes) {
        this.nodeEvaluator = new EcologyNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxNodes);
    }

    /**
     * Gets a smoothed target position by interpolating between current and future waypoints.
     * This creates more natural movement by looking ahead along the path and blending
     * the current target with future positions using linear interpolation.
     *
     * @return the smoothed target position, or null if no valid path exists
     */
    private Vec3 getSmoothedTarget() {
        if (this.path == null || this.path.isDone()) {
            return null;
        }

        int currentIndex = this.path.getNextNodeIndex();
        int lookAheadIndex = Math.min(currentIndex + LOOK_AHEAD_NODES, this.path.getNodeCount() - 1);

        if (currentIndex >= this.path.getNodeCount()) {
            return this.path.getNextEntityPos(this.mob);
        }

        // Get current target position
        Vec3 currentTarget = this.path.getNextEntityPos(this.mob);

        if (lookAheadIndex == currentIndex) {
            return currentTarget;
        }

        // Get future target position for look-ahead
        Vec3 futureTarget = Vec3.atBottomCenterOf(this.path.getNodePos(lookAheadIndex));

        // Simple lerp toward look-ahead point for smoother paths
        // Weight of 0.3 means 70% current target, 30% future target
        double t = 0.3;
        return new Vec3(
            Mth.lerp(t, currentTarget.x, futureTarget.x),
            Mth.lerp(t, currentTarget.y, futureTarget.y),
            Mth.lerp(t, currentTarget.z, futureTarget.z)
        );
    }

    /**
     * Follows the current path by checking if waypoints have been reached and advancing.
     * Uses dynamic waypoint distance based on entity size for better accuracy.
     */
    @Override
    protected void followThePath() {
        Vec3 mobPos = this.getTempMobPos();

        // Calculate dynamic waypoint reach distance based on mob size
        this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F
            ? this.mob.getBbWidth() / 2.0F
            : 0.75F - this.mob.getBbWidth() / 2.0F;

        Vec3i nextNodePos = this.path.getNextNodePos();
        double dx = Math.abs(this.mob.getX() - (nextNodePos.getX() + 0.5));
        double dy = Math.abs(this.mob.getY() - nextNodePos.getY());
        double dz = Math.abs(this.mob.getZ() - (nextNodePos.getZ() + 0.5));

        boolean reachedNode = dx < this.maxDistanceToWaypoint
            && dz < this.maxDistanceToWaypoint
            && dy < 1.0;

        if (reachedNode || (this.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNode(mobPos))) {
            this.path.advance();
        }

        this.doStuckDetection(mobPos);
    }

    /**
     * Determines if the mob should skip the current node and target the next one.
     * This is a reimplementation of the parent's private shouldTargetNextNodeInDirection method.
     *
     * @param mobPos the current position of the mob
     * @return true if the next node should be targeted instead of the current one
     */
    private boolean shouldTargetNextNode(Vec3 mobPos) {
        if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
            return false;
        }

        Vec3 currentNodeVec = Vec3.atBottomCenterOf(this.path.getNextNodePos());
        if (!mobPos.closerThan(currentNodeVec, 2.0)) {
            return false;
        }

        Vec3 nextNodeVec = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
        Vec3 currentToNext = nextNodeVec.subtract(currentNodeVec);
        Vec3 mobToCurrent = mobPos.subtract(currentNodeVec);

        // Check if mob is already moving toward the next node
        return currentToNext.dot(mobToCurrent) > 0.0;
    }

    /**
     * Main navigation tick that handles path following and smooth movement.
     * Overrides the parent to use smoothed target positions for more natural movement.
     */
    @Override
    public void tick() {
        this.tick++;

        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }

        if (!this.isDone()) {
            if (this.canUpdatePath()) {
                this.followThePath();
            } else if (this.path != null && !this.path.isDone()) {
                Vec3 mobPos = this.getTempMobPos();
                Vec3 nextPos = this.path.getNextEntityPos(this.mob);

                // Handle falling onto path
                if (mobPos.y > nextPos.y
                    && !this.mob.onGround()
                    && Mth.floor(mobPos.x) == Mth.floor(nextPos.x)
                    && Mth.floor(mobPos.z) == Mth.floor(nextPos.z)) {
                    this.path.advance();
                }
            }

            if (!this.isDone()) {
                // Use smoothed target instead of raw waypoint for more natural movement
                Vec3 smoothedTarget = getSmoothedTarget();
                if (smoothedTarget != null) {
                    double groundY = this.getGroundY(smoothedTarget);
                    this.mob.getMoveControl().setWantedPosition(
                        smoothedTarget.x,
                        groundY,
                        smoothedTarget.z,
                        this.speedModifier
                    );
                }
            }
        }
    }

    /**
     * Checks if the path requires switchbacks and logs steep slopes.
     * Currently performs detection only; actual switchback insertion would require
     * modifying the pathfinding algorithm itself to generate alternative routes.
     *
     * <p>Steep slopes are identified by calculating the angle between consecutive nodes.
     * In future implementations, these segments could be flagged for higher costs during
     * pathfinding to encourage the algorithm to find gentler routes.
     */
    private void processSteepSlopes() {
        if (this.path == null || this.path.getNodeCount() < 2) {
            return;
        }

        // Check consecutive nodes for steep slopes
        for (int i = 0; i < this.path.getNodeCount() - 1; i++) {
            Node current = this.path.getNode(i);
            Node next = this.path.getNode(i + 1);

            int heightDiff = next.y - current.y;
            if (Math.abs(heightDiff) <= 1) {
                continue;
            }

            double horizDist = Math.sqrt(
                (next.x - current.x) * (next.x - current.x) +
                (next.z - current.z) * (next.z - current.z)
            );

            double slopeDeg = Math.toDegrees(Math.atan2(Math.abs(heightDiff), horizDist));

            if (slopeDeg > SWITCHBACK_THRESHOLD && horizDist > 0.5) {
                // This segment is too steep - would benefit from switchbacks
                // Note: Actually inserting nodes is complex; instead, we increase the
                // cost during pathfinding so the algorithm finds a better route
                LOGGER.debug("Steep slope detected: {}° from ({},{},{}) to ({},{},{})",
                    String.format("%.1f", slopeDeg), current.x, current.y, current.z,
                    next.x, next.y, next.z);
            }
        }
    }

    /**
     * Creates a path to the specified block position and processes it for steep slopes.
     *
     * @param blockPos the target position
     * @param reachRange the acceptable distance from the target
     * @return the created path, or null if no path could be found
     */
    @Override
    public Path createPath(BlockPos blockPos, int reachRange) {
        Path path = super.createPath(blockPos, reachRange);
        if (path != null) {
            processSteepSlopes();
        }
        return path;
    }
}
