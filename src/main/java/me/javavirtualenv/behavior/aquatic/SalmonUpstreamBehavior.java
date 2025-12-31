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
 * Salmon upstream spawning migration behavior.
 * Salmon swim upstream (against water current) to reach spawning grounds.
 * <p>
 * Scientific basis: Salmon are anadromous fish that migrate from oceans
 * upstream to freshwater spawning grounds. They can detect water flow direction
 * and swim against currents using burst swimming patterns.
 */
public class SalmonUpstreamBehavior extends SteeringBehavior {
    private final AquaticConfig config;

    public SalmonUpstreamBehavior(AquaticConfig config) {
        super(1.3, true);
        this.config = config;
    }

    public SalmonUpstreamBehavior() {
        this(AquaticConfig.createForSalmon());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();
        Level level = context.getLevel();

        // Detect water current direction at current position
        Vec3d currentDirection = detectWaterCurrent(context);

        // If no current or very weak current, no upstream swimming
        if (currentDirection.magnitude() < 0.01) {
            return new Vec3d();
        }

        // Swim against the current (upstream)
        return swimAgainstCurrent(context, currentDirection);
    }

    /**
     * Detect water flow direction at entity's position.
     * Checks water blocks in all directions to determine flow.
     */
    private Vec3d detectWaterCurrent(BehaviorContext context) {
        Entity self = context.getEntity();
        BlockPos pos = self.blockPosition();
        Level level = context.getLevel();

        // Check current block
        BlockState currentBlock = level.getBlockState(pos);

        if (currentBlock.is(Blocks.WATER)) {
            // Flowing water has level property
            Integer flow = currentBlock.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL);

            if (flow != null && flow > 0) {
                // Find direction to nearest water source block
                return findFlowDirection(level, pos);
            }
        }

        return new Vec3d();
    }

    /**
     * Find water flow direction by searching for source blocks.
     * Water flows toward lower level values, away from source (level=0).
     */
    private Vec3d findFlowDirection(Level level, BlockPos pos) {
        Vec3d flowDir = new Vec3d();
        int currentLevel = getWaterLevel(level, pos);

        // Check adjacent blocks
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        // Check in all horizontal directions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                checkPos.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                int neighborLevel = getWaterLevel(level, checkPos);

                if (neighborLevel < currentLevel) {
                    // Water flows toward this direction (to lower level)
                    flowDir.x += dx;
                    flowDir.z += dz;
                }
            }
        }

        // Normalize
        flowDir.normalize();
        return flowDir;
    }

    /**
     * Get water level at position.
     * Returns 0 for source water, higher values for flowing water.
     */
    private int getWaterLevel(Level level, BlockPos pos) {
        BlockState block = level.getBlockState(pos);

        if (!block.is(Blocks.WATER)) {
            return Integer.MAX_VALUE; // Not water
        }

        Integer levelProp = block.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL);
        return levelProp != null ? levelProp : 0;
    }

    /**
     * Calculate swimming force against current.
     * Uses burst swimming pattern with high force against flow.
     */
    private Vec3d swimAgainstCurrent(BehaviorContext context, Vec3d currentDirection) {
        // Desired direction is opposite to current (upstream)
        Vec3d upstreamDir = new Vec3d(-currentDirection.x, -currentDirection.y, -currentDirection.z);
        upstreamDir.normalize();

        // Check if entity is already swimming upstream
        Vec3d currentVelocity = context.getVelocity();
        double upstreamAlignment = currentVelocity.dot(upstreamDir);

        // Add some randomness to create burst swimming pattern
        double burstIntensity = 0.8 + Math.random() * 0.4;

        Vec3d desired = upstreamDir.copy();
        desired.mult(config.getUpstreamSpeed() * burstIntensity);

        // Add slight vertical variation for leaping behavior
        if (Math.random() < 0.1) {
            desired.y += 0.3; // Small leap
        }

        // Calculate steering force
        Vec3d steering = Vec3d.sub(desired, currentVelocity);

        // Scale by current resistance (strength against flow)
        steering.mult(config.getCurrentResistance());

        // Limit force
        if (steering.magnitude() > config.getMaxForce()) {
            steering.normalize();
            steering.mult(config.getMaxForce());
        }

        return steering;
    }

    /**
     * Check if position is in flowing water (not still water).
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
     * Check if salmon should migrate upstream.
     * This could be seasonal or based on time.
     */
    public boolean shouldMigrate(Level level) {
        long timeOfDay = level.getDayTime() % 24000;

        // Salmon are more active during dawn and dusk
        return (timeOfDay >= 23000 || timeOfDay <= 1000) || // Dawn
               (timeOfDay >= 12000 && timeOfDay <= 14000);   // Dusk
    }
}
