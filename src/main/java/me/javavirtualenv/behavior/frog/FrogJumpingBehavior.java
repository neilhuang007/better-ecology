package me.javavirtualenv.behavior.frog;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.BehaviorContext;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Jumping behavior for frogs.
 * <p>
 * Implements realistic frog jumping patterns:
 * - Powerful jumps between lily pads
 * - Calculate jump trajectory to land on targets
 * - Prefer jumping into water when falling
 * - Climb vegetation with small hops
 * - Escape jumps when threatened
 * <p>
 * Scientific basis:
 * - Frogs are specialized for explosive jumping
 * - Can jump 10-20x their body length
 * - Use jumps for both hunting and escape
 * - Excellent depth perception for landing accuracy
 */
public class FrogJumpingBehavior extends SteeringBehavior {

    private static final double JUMP_FORCE = 0.8;
    private static final double JUMP_HORIZONTAL_SPEED = 0.5;
    private static final double TARGET_DETECTION_RANGE = 6.0;
    private static final double MIN_JUMP_DISTANCE = 1.5;
    private static final double MAX_JUMP_DISTANCE = 5.0;

    private final Random random = new Random();
    private BlockPos jumpTarget;
    private int jumpCooldown = 0;

    public FrogJumpingBehavior() {
        super(1.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof Frog frog)) {
            return new Vec3d();
        }

        // Update cooldown
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }

        // Only jump when on ground
        if (!frog.isOnGround()) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();

        // Check if we should jump
        if (shouldJump(frog, position)) {
            return calculateJumpForce(frog, position);
        }

        return new Vec3d();
    }

    /**
     * Determines if the frog should jump.
     */
    private boolean shouldJump(Frog frog, Vec3d position) {
        if (jumpCooldown > 0) {
            return false;
        }

        // Don't jump if panicking (that's handled separately)
        if (isPanicking(frog)) {
            return false;
        }

        // Random idle jumps
        if (random.nextDouble() < 0.02) {
            return true;
        }

        // Jump toward lily pads
        BlockPos targetLily = findBestLilyPadTarget(frog);
        if (targetLily != null) {
            jumpTarget = targetLily;
            return true;
        }

        // Jump toward water if on land
        if (!frog.isInWater() && !isNearWater(frog)) {
            BlockPos waterTarget = findNearestWater(frog);
            if (waterTarget != null) {
                jumpTarget = waterTarget;
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates the jump force vector.
     */
    private Vec3d calculateJumpForce(Frog frog, Vec3d position) {
        Vec3d target;

        if (jumpTarget != null) {
            target = new Vec3d(
                    jumpTarget.getX() + 0.5,
                    jumpTarget.getY(),
                    jumpTarget.getZ() + 0.5
            );
        } else {
            // Random jump target
            target = generateRandomJumpTarget(frog, position);
        }

        Vec3d direction = Vec3d.sub(target, position);
        double distance = direction.magnitude();

        // Clamp distance to reasonable jump range
        if (distance > MAX_JUMP_DISTANCE) {
            distance = MAX_JUMP_DISTANCE;
        }

        direction.normalize();

        // Calculate vertical component for arc
        double verticalForce = calculateVerticalForce(distance);

        Vec3d jumpForce = new Vec3d(
                direction.x * JUMP_HORIZONTAL_SPEED,
                verticalForce,
                direction.z * JUMP_HORIZONTAL_SPEED
        );

        // Set cooldown
        jumpCooldown = 40 + random.nextInt(20); // 2-3 seconds
        jumpTarget = null;

        return jumpForce;
    }

    /**
     * Calculates the vertical force needed for a jump of given distance.
     */
    private double calculateVerticalForce(double distance) {
        // Approximate ballistic trajectory
        // Higher vertical force for longer jumps
        double normalizedDistance = distance / MAX_JUMP_DISTANCE;
        return 0.3 + normalizedDistance * 0.3;
    }

    /**
     * Finds the best lily pad to jump to.
     */
    private BlockPos findBestLilyPadTarget(Frog frog) {
        BlockPos bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        BlockPos frogPos = frog.blockPosition();
        int searchRadius = 6;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = frogPos.offset(x, y, z);
                    BlockState state = frog.level().getBlockState(checkPos);

                    if (state.is(Blocks.LILY_PAD)) {
                        double score = scoreLilyPadTarget(frog, checkPos);
                        if (score > bestScore) {
                            bestScore = score;
                            bestTarget = checkPos;
                        }
                    }
                }
            }
        }

        return bestTarget;
    }

    /**
     * Scores a lily pad as a jump target.
     */
    private double scoreLilyPadTarget(Frog frog, BlockPos pos) {
        double distance = frog.position().distanceTo(Vec3.atCenterOf(pos));

        // Too close or too far
        if (distance < MIN_JUMP_DISTANCE || distance > MAX_JUMP_DISTANCE) {
            return Double.NEGATIVE_INFINITY;
        }

        double score = 10.0 - distance; // Prefer closer pads

        // Bonus for pads near other frogs (social)
        if (hasNearbyFrogs(frog, pos, 4.0)) {
            score += 3.0;
        }

        // Bonus for pads near vegetation
        if (hasNearbyVegetation(frog, pos)) {
            score += 2.0;
        }

        return score;
    }

    /**
     * Finds the nearest water block.
     */
    private BlockPos findNearestWater(Frog frog) {
        BlockPos frogPos = frog.blockPosition();
        int searchRadius = 10;

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -3; y <= 1; y++) {
                    BlockPos checkPos = frogPos.offset(x, y, z);
                    BlockState state = frog.level().getBlockState(checkPos);

                    if (state.is(Blocks.WATER)) {
                        double distance = frog.position().distanceTo(Vec3.atCenterOf(checkPos));
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Generates a random jump target.
     */
    private Vec3d generateRandomJumpTarget(Frog frog, Vec3d position) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = MIN_JUMP_DISTANCE + random.nextDouble() * (MAX_JUMP_DISTANCE - MIN_JUMP_DISTANCE);

        Vec3d target = new Vec3d(
                position.x + Math.cos(angle) * distance,
                position.y,
                position.z + Math.sin(angle) * distance
        );

        return target;
    }

    /**
     * Checks if the frog is near water.
     */
    private boolean isNearWater(Frog frog) {
        BlockPos pos = frog.blockPosition();
        int radius = 2;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = frog.level().getBlockState(checkPos);
                    if (state.is(Blocks.WATER)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if there are other frogs near a position.
     */
    private boolean hasNearbyFrogs(Frog frog, BlockPos pos, double radius) {
        return !frog.level().getEntitiesOfClass(Frog.class,
                new net.minecraft.world.phys.AABB(
                        pos.getX() - radius, pos.getY() - 1, pos.getZ() - radius,
                        pos.getX() + radius, pos.getY() + 1, pos.getZ() + radius
                ),
                e -> !e.equals(frog)).isEmpty();
    }

    /**
     * Checks if there's vegetation near a position.
     */
    private boolean hasNearbyVegetation(Frog frog, BlockPos pos) {
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                BlockState above = frog.level().getBlockState(checkPos.above());
                if (above.is(Blocks.TALL_GRASS) || above.is(Blocks.SUGAR_CANE)
                        || above.is(Blocks.VINE) || above.is(Blocks.LILY_PAD)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the frog is panicking.
     */
    private boolean isPanicking(Frog frog) {
        return frog.getBrain() != null
                && frog.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.IS_PANICKING);
    }

    /**
     * Forces a jump in the given direction (for emergency escapes).
     */
    public Vec3d forceJump(Frog frog, Vec3d direction) {
        Vec3d jumpForce = new Vec3d(
                direction.x * JUMP_HORIZONTAL_SPEED * 1.5,
                0.5, // Higher jump for escapes
                direction.z * JUMP_HORIZONTAL_SPEED * 1.5
        );

        jumpCooldown = 60; // Longer cooldown for forced jumps
        return jumpForce;
    }

    /**
     * Gets the current jump target.
     */
    public BlockPos getJumpTarget() {
        return jumpTarget;
    }

    /**
     * Gets the remaining jump cooldown.
     */
    public int getJumpCooldown() {
        return jumpCooldown;
    }

    /**
     * Checks if the frog can jump.
     */
    public boolean canJump() {
        return jumpCooldown == 0;
    }
}
