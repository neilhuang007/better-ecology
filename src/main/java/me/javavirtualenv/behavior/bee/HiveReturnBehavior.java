package me.javavirtualenv.behavior.bee;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.mixin.animal.BeeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Behavior for bees returning to their hive.
 * <p>
 * This behavior:
 * - Guides bees back to their hive when they have nectar
 * - Triggers return to hive at night or during rain
 * - Ensures bees stay within their home range
 * - Deposits nectar in hive for honey production
 */
public class HiveReturnBehavior extends SteeringBehavior {

    private static final double HIVE_SEARCH_RADIUS = 64.0;
    private static final double ARRIVAL_THRESHOLD = 2.0;

    private BlockPos hivePos;
    private boolean isReturning;

    public HiveReturnBehavior() {
        this(1.0);
    }

    public HiveReturnBehavior(double weight) {
        this.hivePos = null;
        setWeight(weight);
        this.isReturning = false;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Bee bee)) {
            return new Vec3d();
        }

        Level level = context.getWorld();
        Vec3d position = context.getPosition();

        // Check if bee should return to hive
        boolean shouldReturn = shouldReturnToHive(bee, level);

        if (!shouldReturn) {
            isReturning = false;
            return new Vec3d();
        }

        isReturning = true;

        // Find hive if not known
        if (hivePos == null || !isValidHive(level, hivePos)) {
            hivePos = findNearestHive(level, position, bee);
        }

        if (hivePos == null) {
            return new Vec3d();
        }

        Vec3d hivePosVec = new Vec3d(
            hivePos.getX() + 0.5,
            hivePos.getY() + 0.5,
            hivePos.getZ() + 0.5
        );

        // Check if arrived at hive
        double distance = position.distanceTo(hivePosVec);
        if (distance <= ARRIVAL_THRESHOLD) {
            onArriveAtHive(bee, level);
            return new Vec3d();
        }

        // Calculate steering force toward hive
        Vec3d desired = Vec3d.sub(hivePosVec, position);
        desired.normalize();
        desired.mult(context.getMaxSpeed());

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        steer = limitForce(steer, context.getMaxForce());

        return steer;
    }

    /**
     * Determines if a bee should return to the hive.
     */
    private boolean shouldReturnToHive(Bee bee, Level level) {
        // Return if has nectar
        if (bee.hasNectar()) {
            return true;
        }

        // Return at night
        if (level.isNight()) {
            return true;
        }

        // Return during rain
        if (level.isRaining()) {
            return true;
        }

        // Return if getting too far from hive
        if (hivePos != null) {
            Vec3d position = new Vec3d(bee.position());
            Vec3d hivePosVec = new Vec3d(
                hivePos.getX() + 0.5,
                hivePos.getY() + 0.5,
                hivePos.getZ() + 0.5
            );
            double distance = position.distanceTo(hivePosVec);
            if (distance > HIVE_SEARCH_RADIUS * 0.8) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the nearest valid hive.
     */
    private BlockPos findNearestHive(Level level, Vec3d position, Bee bee) {
        BlockPos beePos = new BlockPos((int) position.x, (int) position.y, (int) position.z);

        BlockPos nearestHive = null;
        double nearestDistance = Double.MAX_VALUE;

        int searchRadius = (int) HIVE_SEARCH_RADIUS;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = beePos.offset(x, y, z);
                    if (isValidHive(level, testPos)) {
                        Vec3d testPosVec = new Vec3d(
                            testPos.getX() + 0.5,
                            testPos.getY() + 0.5,
                            testPos.getZ() + 0.5
                        );
                        double distance = position.distanceTo(testPosVec);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestHive = testPos;
                        }
                    }
                }
            }
        }

        return nearestHive;
    }

    /**
     * Checks if a position contains a valid hive.
     */
    private boolean isValidHive(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST);
    }

    /**
     * Called when the bee arrives at the hive.
     */
    private void onArriveAtHive(Bee bee, Level level) {
        if (bee.hasNectar()) {
            // Deposit nectar (vanilla behavior)
            ((BeeAccessor) bee).invokeSetHasNectar(false);

            // The vanilla bee entity will enter the hive
            // We don't need to manually handle honey production
        }
    }

    public boolean isReturning() {
        return isReturning;
    }

    public BlockPos getHivePos() {
        return hivePos;
    }

    public void setHivePos(BlockPos pos) {
        this.hivePos = pos;
    }
}
