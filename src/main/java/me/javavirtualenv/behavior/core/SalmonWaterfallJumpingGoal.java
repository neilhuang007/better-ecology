package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes salmon leap up waterfalls during upstream migration.
 *
 * <p>Scientific Basis:
 * Salmon can leap up to 3.7 meters (12 feet) to clear waterfalls and obstacles
 * during their upstream migration. They use powerful tail thrusts to propel
 * themselves vertically through rushing water.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Detects 2+ block vertical water drops ahead during migration</li>
 *   <li>Applies upward velocity (0.6-0.8) to leap</li>
 *   <li>Success rate: 60-70% (some attempts fail, requiring retry)</li>
 *   <li>Max height: 3 blocks</li>
 *   <li>Creates water splash particles on jump</li>
 * </ul>
 */
public class SalmonWaterfallJumpingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalmonWaterfallJumpingGoal.class);

    private static final int MIN_WATERFALL_HEIGHT = 2;
    private static final int MAX_WATERFALL_HEIGHT = 3;
    private static final double MIN_JUMP_VELOCITY = 0.6;
    private static final double MAX_JUMP_VELOCITY = 0.8;
    private static final double SUCCESS_RATE = 0.65;
    private static final int DETECTION_DISTANCE = 3;
    private static final int JUMP_COOLDOWN = 40; // 2 seconds between attempts
    private static final int RETRY_COOLDOWN = 100; // 5 seconds after failed jump

    private final Salmon salmon;
    private BlockPos waterfallPos;
    private int jumpCooldown;
    private int waterfallHeight;
    private boolean isJumping;
    private int jumpTicks;

    public SalmonWaterfallJumpingGoal(Salmon salmon) {
        this.salmon = salmon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!salmon.isInWater()) {
            return false;
        }

        if (jumpCooldown > 0) {
            jumpCooldown--;
            return false;
        }

        waterfallPos = detectWaterfall();
        return waterfallPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (isJumping) {
            return jumpTicks < 20;
        }

        return waterfallPos != null && salmon.blockPosition().distSqr(waterfallPos) < 16;
    }

    @Override
    public void start() {
        isJumping = false;
        jumpTicks = 0;
        LOGGER.debug("Salmon {} detected waterfall at {} (height: {})",
            salmon.getName().getString(), waterfallPos, waterfallHeight);

        approachWaterfall();
    }

    @Override
    public void stop() {
        if (waterfallPos != null) {
            LOGGER.debug("Salmon {} stopped waterfall jumping", salmon.getName().getString());
        }
        waterfallPos = null;
        isJumping = false;
        jumpTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (isJumping) {
            jumpTicks++;
            return;
        }

        if (isAtWaterfallBase()) {
            attemptJump();
        } else {
            approachWaterfall();
        }
    }

    /**
     * Detects a waterfall ahead of the salmon.
     */
    private BlockPos detectWaterfall() {
        Vec3 lookVec = salmon.getLookAngle();
        BlockPos currentPos = salmon.blockPosition();

        for (int distance = 1; distance <= DETECTION_DISTANCE; distance++) {
            int checkX = currentPos.getX() + (int)(lookVec.x * distance);
            int checkZ = currentPos.getZ() + (int)(lookVec.z * distance);

            int height = findWaterfallHeight(checkX, currentPos.getY(), checkZ);
            if (height >= MIN_WATERFALL_HEIGHT && height <= MAX_WATERFALL_HEIGHT) {
                waterfallHeight = height;
                return new BlockPos(checkX, currentPos.getY(), checkZ);
            }
        }

        return null;
    }

    /**
     * Finds the height of a waterfall at the given x,z coordinates.
     */
    private int findWaterfallHeight(int x, int startY, int z) {
        Level level = salmon.level();
        int height = 0;

        for (int y = startY; y <= startY + MAX_WATERFALL_HEIGHT; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            FluidState fluidState = level.getFluidState(pos);

            if (!fluidState.is(FluidTags.WATER)) {
                break;
            }

            if (hasWaterAbove(pos)) {
                height++;
            } else {
                break;
            }
        }

        return height;
    }

    /**
     * Checks if there is water above the given position.
     */
    private boolean hasWaterAbove(BlockPos pos) {
        BlockPos above = pos.above();
        return salmon.level().getFluidState(above).is(FluidTags.WATER);
    }

    /**
     * Checks if salmon is at the base of the waterfall.
     */
    private boolean isAtWaterfallBase() {
        if (waterfallPos == null) {
            return false;
        }

        BlockPos salmonPos = salmon.blockPosition();
        return salmonPos.distSqr(waterfallPos) <= 2;
    }

    /**
     * Navigates salmon to the waterfall base.
     */
    private void approachWaterfall() {
        if (waterfallPos == null) {
            return;
        }

        salmon.getNavigation().moveTo(
            waterfallPos.getX() + 0.5,
            waterfallPos.getY(),
            waterfallPos.getZ() + 0.5,
            1.2
        );
    }

    /**
     * Attempts to jump up the waterfall.
     */
    private void attemptJump() {
        boolean success = salmon.getRandom().nextDouble() < SUCCESS_RATE;

        if (success) {
            performSuccessfulJump();
        } else {
            performFailedJump();
        }

        isJumping = true;
        jumpTicks = 0;
    }

    /**
     * Performs a successful waterfall jump.
     */
    private void performSuccessfulJump() {
        double jumpVelocity = MIN_JUMP_VELOCITY +
            (salmon.getRandom().nextDouble() * (MAX_JUMP_VELOCITY - MIN_JUMP_VELOCITY));

        Vec3 motion = salmon.getDeltaMovement();
        salmon.setDeltaMovement(motion.x, jumpVelocity, motion.z);

        spawnJumpParticles();
        playJumpSound();

        jumpCooldown = JUMP_COOLDOWN;
        LOGGER.debug("Salmon {} successfully jumped waterfall with velocity {}",
            salmon.getName().getString(), jumpVelocity);
    }

    /**
     * Performs a failed waterfall jump.
     */
    private void performFailedJump() {
        double failedJumpVelocity = MIN_JUMP_VELOCITY * 0.5;

        Vec3 motion = salmon.getDeltaMovement();
        salmon.setDeltaMovement(motion.x, failedJumpVelocity, motion.z);

        spawnJumpParticles();
        playJumpSound();

        jumpCooldown = RETRY_COOLDOWN;
        LOGGER.debug("Salmon {} failed waterfall jump, will retry after cooldown",
            salmon.getName().getString());
    }

    /**
     * Spawns water splash particles when salmon jumps.
     */
    private void spawnJumpParticles() {
        Level level = salmon.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 pos = salmon.position();

        for (int i = 0; i < 8; i++) {
            double offsetX = (salmon.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetY = salmon.getRandom().nextDouble() * 0.3;
            double offsetZ = (salmon.getRandom().nextDouble() - 0.5) * 0.5;

            serverLevel.sendParticles(
                ParticleTypes.SPLASH,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                2,
                0, 0.2, 0,
                0.2
            );
        }

        for (int i = 0; i < 3; i++) {
            double offsetX = (salmon.getRandom().nextDouble() - 0.5) * 0.3;
            double offsetZ = (salmon.getRandom().nextDouble() - 0.5) * 0.3;

            serverLevel.sendParticles(
                ParticleTypes.BUBBLE,
                pos.x + offsetX,
                pos.y,
                pos.z + offsetZ,
                1,
                0, 0.1, 0,
                0.1
            );
        }
    }

    /**
     * Plays splash sound when salmon jumps.
     */
    private void playJumpSound() {
        salmon.level().playSound(
            null,
            salmon.getX(),
            salmon.getY(),
            salmon.getZ(),
            SoundEvents.PLAYER_SPLASH,
            SoundSource.NEUTRAL,
            0.8F + salmon.getRandom().nextFloat() * 0.2F,
            0.9F + salmon.getRandom().nextFloat() * 0.3F
        );
    }
}
