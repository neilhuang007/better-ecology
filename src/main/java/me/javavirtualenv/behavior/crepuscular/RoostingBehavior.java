package me.javavirtualenv.behavior.crepuscular;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Handles roosting behavior for crepuscular creatures.
 * Implements attraction to ceilings, clustering with group members,
 * and return-to-roost navigation.
 */
public class RoostingBehavior extends SteeringBehavior {
    private final CrepuscularConfig config;
    private BlockPos roostPosition;
    private boolean hasEstablishedRoost;

    public RoostingBehavior(CrepuscularConfig config) {
        this.config = config != null ? config : new CrepuscularConfig();
        this.hasEstablishedRoost = false;
    }

    public RoostingBehavior() {
        this(new CrepuscularConfig());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (context == null) {
            return new Vec3d();
        }

        Mob entity = context.getEntity();
        Level level = context.getLevel();

        // Handle null entity/level for testing
        if (entity == null || level == null) {
            return seekCeiling(context);
        }

        // If no roost established, find one
        if (!hasEstablishedRoost || roostPosition == null) {
            findRoostPosition(entity);
        }

        // If still no roost, just seek ceiling
        if (roostPosition == null) {
            return seekCeiling(context);
        }

        // Check if current roost is still valid
        if (!isValidRoost(level, roostPosition)) {
            findRoostPosition(entity);
            if (roostPosition == null) {
                return seekCeiling(context);
            }
        }

        // Return to roost
        return returnToRoost(context);
    }

    /**
     * Finds a suitable roost position.
     * Looks for solid blocks above (caves ceilings).
     */
    public void findRoostPosition(Mob entity) {
        Level level = entity.level();
        BlockPos entityPos = entity.blockPosition();

        // Search upward for ceiling
        for (int y = 1; y <= config.getCeilingAttractionRange(); y++) {
            BlockPos checkPos = entityPos.above(y);
            BlockState blockState = level.getBlockState(checkPos);

            // Found a solid block (ceiling)
            // Use blocksMotion() as replacement for deprecated isRedstoneConductor()
            if (blockState.blocksMotion()) {
                // Roost below the ceiling
                roostPosition = checkPos.below();
                hasEstablishedRoost = true;
                return;
            }
        }

        // If no ceiling found, try to find a dark corner
        BlockPos darkCorner = findDarkCorner(entity);
        if (darkCorner != null) {
            roostPosition = darkCorner;
            hasEstablishedRoost = true;
        } else {
            hasEstablishedRoost = false;
        }
    }

    /**
     * Finds a dark corner suitable for roosting.
     */
    private BlockPos findDarkCorner(Mob entity) {
        Level level = entity.level();
        BlockPos entityPos = entity.blockPosition();
        int searchRange = config.getRoostClusterDistance() * 2;

        for (int x = -searchRange; x <= searchRange; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRange; z <= searchRange; z++) {
                    BlockPos checkPos = entityPos.offset(x, y, z);

                    if (isValidRoost(level, checkPos)) {
                        return checkPos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if a position is valid for roosting.
     */
    private boolean isValidRoost(Level level, BlockPos pos) {
        // Check if position is not out of bounds
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        // Check if there's a solid block above (ceiling)
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        // Use blocksMotion() as replacement for deprecated isRedstoneConductor()
        if (!aboveState.blocksMotion()) {
            return false;
        }

        // Check if the position itself is not solid
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        // Check if it's dark enough
        int lightLevel = level.getMaxLocalRawBrightness(pos);
        return lightLevel <= 4;
    }

    /**
     * Creates steering behavior to seek the ceiling.
     */
    private Vec3d seekCeiling(BehaviorContext context) {
        Vec3d currentPosition = context.getPosition();
        Vec3d currentVelocity = context.getVelocity();

        // Simply seek upward
        Vec3d ceilingTarget = new Vec3d(
            currentPosition.x,
            currentPosition.y + config.getCeilingAttractionRange(),
            currentPosition.z
        );

        return arrive(currentPosition, currentVelocity, ceilingTarget, 0.2, 2.0);
    }

    /**
     * Creates steering behavior to return to established roost.
     */
    private Vec3d returnToRoost(BehaviorContext context) {
        if (roostPosition == null) {
            return new Vec3d();
        }

        Vec3d currentPosition = context.getPosition();
        Vec3d currentVelocity = context.getVelocity();

        Vec3d roostTarget = new Vec3d(
            roostPosition.getX() + 0.5,
            roostPosition.getY() + 0.1,
            roostPosition.getZ() + 0.5
        );

        double distance = currentPosition.distanceTo(roostTarget);

        // If very close to roost, stop moving
        if (distance < 0.5) {
            return new Vec3d();
        }

        return arrive(currentPosition, currentVelocity, roostTarget, 0.3, 3.0);
    }

    /**
     * Calculates attraction to clustered roost-mates.
     */
    public Vec3d calculateClusterAttraction(BehaviorContext context) {
        if (context == null) {
            return new Vec3d();
        }

        Mob entity = context.getEntity();
        Level level = context.getLevel();

        // Handle null entity/level for testing
        if (entity == null || level == null) {
            return new Vec3d();
        }

        Vec3d currentPosition = context.getPosition();
        Vec3d currentVelocity = context.getVelocity();

        // Find nearby roost-mates
        List<? extends Mob> nearbyCreatures = level.getEntitiesOfClass(
            entity.getClass(),
            entity.getBoundingBox().inflate(config.getRoostClusterDistance()),
            mob -> mob != entity
        );

        if (nearbyCreatures.isEmpty()) {
            return new Vec3d();
        }

        // Calculate center of cluster
        double centerX = 0.0;
        double centerY = 0.0;
        double centerZ = 0.0;
        for (Mob creature : nearbyCreatures) {
            centerX += creature.getX();
            centerY += creature.getY();
            centerZ += creature.getZ();
        }

        double count = nearbyCreatures.size();
        Vec3d clusterCenter = new Vec3d(centerX / count, centerY / count, centerZ / count);

        // Seek cluster center
        return seek(currentPosition, currentVelocity, clusterCenter, 0.1);
    }

    /**
     * Checks if the entity is at or near its roost.
     */
    public boolean isAtRoost(Mob entity) {
        if (roostPosition == null) {
            return false;
        }

        BlockPos currentPos = entity.blockPosition();
        return currentPos.closerThan(roostPosition, 1.5);
    }

    /**
     * Gets the current roost position.
     */
    public BlockPos getRoostPosition() {
        return roostPosition;
    }

    /**
     * Sets a specific roost position.
     */
    public void setRoostPosition(BlockPos pos) {
        this.roostPosition = pos;
        this.hasEstablishedRoost = (pos != null);
    }

    /**
     * Clears the established roost, forcing re-selection.
     */
    public void clearRoost() {
        this.roostPosition = null;
        this.hasEstablishedRoost = false;
    }

    /**
     * Checks if a roost has been established.
     */
    public boolean hasEstablishedRoost() {
        return hasEstablishedRoost && roostPosition != null;
    }

    /**
     * Finds a nearby roost-mate to cluster with.
     */
    public Mob findRoostMate(Mob entity) {
        Level level = entity.level();

        List<? extends Mob> nearbyCreatures = level.getEntitiesOfClass(
            entity.getClass(),
            entity.getBoundingBox().inflate(config.getRoostClusterDistance()),
            mob -> mob != entity
        );

        if (nearbyCreatures.isEmpty()) {
            return null;
        }

        // Return the closest one
        Mob closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Mob creature : nearbyCreatures) {
            double distance = entity.distanceTo(creature);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = creature;
            }
        }

        return closest;
    }

    /**
     * Calculates the ideal roost position near a group.
     */
    public BlockPos findGroupRoostPosition(Mob entity) {
        Mob roostMate = findRoostMate(entity);
        if (roostMate == null) {
            return null;
        }

        // Find a position near the roost-mate
        BlockPos matePos = roostMate.blockPosition();
        Level level = entity.level();
        int clusterDistance = config.getRoostClusterDistance();

        for (int offset = 1; offset <= clusterDistance; offset++) {
            for (int dx = -offset; dx <= offset; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -offset; dz <= offset; dz++) {
                        BlockPos checkPos = matePos.offset(dx, dy, dz);

                        if (isValidRoost(level, checkPos)) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Updates the configuration for this behavior.
     */
    public void setConfig(CrepuscularConfig config) {
        if (config != null) {
            this.config.setRoostClusterDistance(config.getRoostClusterDistance());
            this.config.setCeilingAttractionRange(config.getCeilingAttractionRange());
        }
    }
}
