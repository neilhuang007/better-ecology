package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;

/**
 * Climbing behavior for agile felines.
 * <p>
 * Ocelots can climb trees and other vertical surfaces:
 * - Climb trees to escape threats
 * - Climb to get better vantage points
 * - Climb while stalking prey from above
 * - Descend carefully (head first)
 */
public class ClimingBehavior extends SteeringBehavior {

    private final double climbSpeed;
    private final double maxClimbHeight;
    private final int searchRadius;

    private boolean isClimbing = false;
    private BlockPos climbTarget;
    private ClimbDirection climbDirection;

    public ClimingBehavior(double climbSpeed, double maxClimbHeight, int searchRadius) {
        super(0.8);
        this.climbSpeed = climbSpeed;
        this.maxClimbHeight = maxClimbHeight;
        this.searchRadius = searchRadius;
    }

    public ClimingBehavior() {
        this(0.15, 8.0, 6);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();
        Level level = context.getLevel();

        // Check if should climb
        if (!shouldClimb(mob, level)) {
            if (isClimbing) {
                stopClimbing();
            }
            return new Vec3d();
        }

        // Find climbable target if not climbing
        if (!isClimbing || climbTarget == null) {
            climbTarget = findClimbTarget(mob, level);
            if (climbTarget == null) {
                return new Vec3d();
            }
            isClimbing = true;
            climbDirection = ClimbDirection.UP;
        }

        // Calculate climbing movement
        return calculateClimbMovement(context, climbTarget);
    }

    private boolean shouldClimb(Mob mob, Level level) {
        // Only ocelots climb (cats can but prefer not to)
        if (!(mob instanceof net.minecraft.world.entity.animal.Ocelot)) {
            return false;
        }

        // Climb to escape threats
        if (isThreatened(mob, level)) {
            return true;
        }

        // Climb for hunting vantage
        if (isHunting(mob) && RANDOM.nextDouble() < 0.1) {
            return true;
        }

        return false;
    }

    private boolean isThreatened(Mob mob, Level level) {
        // Check for nearby predators
        for (net.minecraft.world.entity.Entity entity : level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                mob.getBoundingBox().inflate(12.0))) {
            if (entity.equals(mob)) {
                continue;
            }

            // Larger mobs are threats
            if (entity.getBbWidth() > mob.getBbWidth() * 1.3) {
                return true;
            }
        }

        return false;
    }

    private boolean isHunting(Mob mob) {
        // Check if mob has a prey target
        return mob.getTarget() != null && mob.getTarget().isAlive();
    }

    private BlockPos findClimbTarget(Mob mob, Level level) {
        BlockPos mobPos = mob.blockPosition();

        // Look for climbable trees nearby
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = 0; y <= maxClimbHeight; y++) {
                    BlockPos pos = mobPos.offset(x, y, z);

                    if (isClimbable(level, pos)) {
                        // Found a climbable block
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private boolean isClimbable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Can climb logs (trees)
        if (state.is(BlockTags.LOGS)) {
            return true;
        }

        // Can climb leaves (though slower)
        if (state.getBlock() instanceof LeavesBlock) {
            return true;
        }

        // Can climb some other blocks
        if (state.is(Blocks.VINE) || state.is(Blocks.LADDER)) {
            return true;
        }

        return false;
    }

    private Vec3d calculateClimbMovement(BehaviorContext context, BlockPos target) {
        Mob mob = context.getEntity();
        Vec3d mobPos = context.getPosition();
        Vec3d targetPos = new Vec3d(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        Vec3d toTarget = Vec3d.sub(targetPos, mobPos);
        double distance = toTarget.magnitude();

        // If we're at the target height, stop climbing
        if (Math.abs(toTarget.y) < 0.5) {
            stopClimbing();
            return new Vec3d();
        }

        // Normalize and apply climb speed
        toTarget.normalize();
        toTarget.mult(climbSpeed);

        return toTarget;
    }

    private void stopClimbing() {
        isClimbing = false;
        climbTarget = null;
        climbDirection = null;
    }

    public boolean isClimbing() {
        return isClimbing;
    }

    public BlockPos getClimbTarget() {
        return climbTarget;
    }

    public void startClimbing(BlockPos target) {
        this.climbTarget = target;
        this.isClimbing = true;
        this.climbDirection = ClimbDirection.UP;
    }

    private static final java.util.Random RANDOM = new java.util.Random();

    public enum ClimbDirection {
        UP,
        DOWN
    }
}
