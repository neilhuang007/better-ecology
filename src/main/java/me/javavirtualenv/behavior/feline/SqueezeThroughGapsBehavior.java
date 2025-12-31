package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Squeeze through gaps behavior for felines.
 * <p>
 * Cats can squeeze through small gaps that other mobs cannot:
 * - Half-door gaps (open fence gates, trapdoors)
 * - Spaces between blocks
 * - Small openings in fences
 * - Under slabs and stairs
 */
public class SqueezeThroughGapsBehavior extends SteeringBehavior {

    private final double squeezeRange;
    private final double squeezeSpeed;
    private BlockPos targetGap;

    public SqueezeThroughGapsBehavior(double squeezeRange, double squeezeSpeed) {
        super(0.9);
        this.squeezeRange = squeezeRange;
        this.squeezeSpeed = squeezeSpeed;
    }

    public SqueezeThroughGapsBehavior() {
        this(3.0, 0.4);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();
        Level level = context.getLevel();
        Vec3d position = context.getPosition();

        // Check if current path is blocked
        if (!isPathBlocked(mob, level)) {
            targetGap = null;
            return new Vec3d();
        }

        // Find a nearby gap to squeeze through
        BlockPos gap = findNearbyGap(mob, level);
        if (gap == null) {
            return new Vec3d();
        }

        targetGap = gap;

        // Move toward the gap
        Vec3d gapPos = new Vec3d(gap.getX() + 0.5, gap.getY(), gap.getZ() + 0.5);
        Vec3d toGap = Vec3d.sub(gapPos, position);
        toGap.normalize();
        toGap.mult(squeezeSpeed);

        return toGap;
    }

    private boolean isPathBlocked(Mob mob, Level level) {
        Vec3 movement = mob.getDeltaMovement();
        if (movement.length() < 0.01) {
            return false;
        }

        // Look ahead for blocks
        BlockPos ahead = mob.blockPosition().offset(
            (int) Math.signum(movement.x) * 2,
            0,
            (int) Math.signum(movement.z) * 2
        );

        BlockState state = level.getBlockState(ahead);
        return !state.isAir() && state.blocksMotion();
    }

    private BlockPos findNearbyGap(Mob mob, Level level) {
        BlockPos mobPos = mob.blockPosition();

        // Search in a 3x3x2 area around the mob
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 1; y++) {
                    BlockPos pos = mobPos.offset(x, y, z);
                    if (isSqueezableGap(level, pos)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private boolean isSqueezableGap(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Open fence gates
        if (state.getBlock() instanceof FenceGateBlock) {
            return state.getValue(FenceGateBlock.OPEN);
        }

        // Air blocks that are adjacent to solid blocks (gaps in fences)
        if (state.isAir()) {
            boolean hasSolidSide = false;
            for (BlockPos side : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
                if (!level.getBlockState(side).isAir()) {
                    hasSolidSide = true;
                    break;
                }
            }
            return hasSolidSide;
        }

        return false;
    }

    public BlockPos getTargetGap() {
        return targetGap;
    }

    public boolean canSqueeze() {
        return targetGap != null;
    }
}
