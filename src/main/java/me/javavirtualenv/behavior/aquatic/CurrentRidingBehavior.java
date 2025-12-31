package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Current riding behavior for aquatic animals.
 * Fish can ride water currents to conserve energy while traveling.
 * <p>
 * Scientific basis: Fish exhibit rheotaxis - orientation to water currents.
 * They use currents for energy-efficient transport, swimming with flow when migrating
 * or seeking new territories.
 */
public class CurrentRidingBehavior extends SteeringBehavior {
    private final AquaticConfig config;
    private Vec3d lastCurrentDirection = new Vec3d();
    private int currentRideTimer = 0;

    public CurrentRidingBehavior(AquaticConfig config) {
        super(0.8, true);
        this.config = config;
    }

    public CurrentRidingBehavior() {
        this(AquaticConfig.createForFish());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();
        Level level = context.getLevel();

        // Detect water current at current position
        Vec3d currentDirection = detectWaterCurrent(context);

        // No current or too weak
        if (currentDirection.magnitude() < 0.01) {
            currentRideTimer = 0;
            return new Vec3d();
        }

        // Store current direction
        lastCurrentDirection = currentDirection.copy();

        // Determine if should ride current or swim against it
        if (shouldRideCurrent(context)) {
            return rideCurrent(context, currentDirection);
        }

        return new Vec3d();
    }

    /**
     * Detect water flow direction at entity's position.
     * Checks water blocks to determine flow direction and strength.
     */
    private Vec3d detectWaterCurrent(BehaviorContext context) {
        Entity self = context.getEntity();
        BlockPos pos = self.blockPosition();
        Level level = context.getLevel();

        // Check current block
        BlockState currentBlock = level.getBlockState(pos);

        if (currentBlock.is(Blocks.WATER)) {
            Integer flow = currentBlock.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL);

            if (flow != null && flow > 0) {
                // Flowing water - find flow direction
                return findFlowDirection(level, pos, flow);
            }
        }

        // Check if in flowing water bubble column
        if (isInBubbleColumn(level, pos)) {
            // Bubble column pushes up or down
            return new Vec3d(0, 1.0, 0); // Upward flow
        }

        return new Vec3d();
    }

    /**
     * Find water flow direction by analyzing neighboring blocks.
     */
    private Vec3d findFlowDirection(Level level, BlockPos pos, int currentLevel) {
        Vec3d flowDir = new Vec3d();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        // Check adjacent blocks to find flow direction
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                checkPos.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                int neighborLevel = getWaterLevel(level, checkPos);

                // Water flows from higher levels to lower levels
                // Level 0 = source, higher values = further from source
                if (neighborLevel < currentLevel) {
                    // Flow is toward this neighbor (lower level value = closer to source)
                    flowDir.x += dx;
                    flowDir.z += dz;
                }
            }
        }

        // Add downward flow for falling water
        checkPos.set(pos.getX(), pos.getY() - 1, pos.getZ());
        if (getWaterLevel(level, checkPos) < currentLevel) {
            flowDir.y -= 0.5;
        }

        // Normalize
        if (flowDir.magnitude() > 0) {
            flowDir.normalize();
        }

        return flowDir;
    }

    private int getWaterLevel(Level level, BlockPos pos) {
        BlockState block = level.getBlockState(pos);

        if (!block.is(Blocks.WATER)) {
            return Integer.MAX_VALUE;
        }

        Integer levelProp = block.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL);
        return levelProp != null ? levelProp : 0;
    }

    private boolean isInBubbleColumn(Level level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        return above.is(Blocks.BUBBLE_COLUMN) || below.is(Blocks.BUBBLE_COLUMN);
    }

    /**
     * Determine if entity should ride the current.
     * Fish ride currents when:
     * - Not fleeing from predators
     * - Not hunting prey
     * - Looking to travel longer distances
     */
    private boolean shouldRideCurrent(BehaviorContext context) {
        // Random decision to occasionally ride currents for exploration
        if (Math.random() < 0.3) {
            return true;
        }

        // Ride current if timer is active (continue riding)
        if (currentRideTimer > 0) {
            currentRideTimer--;
            return true;
        }

        return false;
    }

    /**
     * Calculate movement force when riding current.
     * Fish align with current and add minimal effort to maintain position.
     */
    private Vec3d rideCurrent(BehaviorContext context, Vec3d currentDirection) {
        Vec3d currentVelocity = context.getVelocity();

        // Align with current direction
        Vec3d desired = currentDirection.copy();
        desired.mult(config.getMaxSpeed() * 0.5); // Ride at half speed

        // Calculate steering to align with current
        Vec3d steering = Vec3d.sub(desired, currentVelocity);

        // Use minimal force - riding current is energy efficient
        double rideForce = config.getMaxForce() * 0.3;
        if (steering.magnitude() > rideForce) {
            steering.normalize();
            steering.mult(rideForce);
        }

        // Add small random movements to simulate natural drift
        steering.x += (Math.random() - 0.5) * 0.02;
        steering.y += (Math.random() - 0.5) * 0.01;
        steering.z += (Math.random() - 0.5) * 0.02;

        // Extend ride timer
        currentRideTimer = 40 + (int) (Math.random() * 60);

        return steering;
    }

    /**
     * Check if entity is in flowing water.
     */
    public boolean isInFlowingWater(BehaviorContext context) {
        Entity self = context.getEntity();
        BlockPos pos = self.blockPosition();
        Level level = context.getLevel();

        BlockState block = level.getBlockState(pos);

        if (!block.is(Blocks.WATER)) {
            return false;
        }

        Integer levelProp = block.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL);
        return levelProp != null && levelProp > 0;
    }

    /**
     * Get the detected current direction.
     */
    public Vec3d getCurrentDirection() {
        return lastCurrentDirection.copy();
    }

    /**
     * Get remaining ride timer ticks.
     */
    public int getCurrentRideTimer() {
        return currentRideTimer;
    }
}
