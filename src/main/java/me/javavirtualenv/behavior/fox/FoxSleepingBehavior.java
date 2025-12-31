package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Fox sleeping behavior for nocturnal patterns.
 * <p>
 * Foxes sleep during the day and are active at night:
 * - Seek out shaded areas during daytime
 * - Prefer sleeping under leaves or in dark areas
 * - Curl up while sleeping
 * - Wake up if threatened or at night
 * <p>
 * Scientific basis: Foxes are primarily nocturnal or crepuscular, hunting
 * at dawn/dusk and sleeping during the day. They prefer sheltered sleeping spots.
 */
public class FoxSleepingBehavior extends SteeringBehavior {

    private final double shelterSearchRange;
    private final double lightThreshold;
    private final int wakeTime;
    private final int sleepTime;

    private BlockPos sleepPosition;
    private boolean isSleeping = false;

    public FoxSleepingBehavior(double shelterSearchRange, double lightThreshold,
                              int wakeTime, int sleepTime) {
        this.shelterSearchRange = shelterSearchRange;
        this.lightThreshold = lightThreshold;
        this.wakeTime = wakeTime;
        this.sleepTime = sleepTime;
    }

    public FoxSleepingBehavior() {
        this(16.0, 0.5, 13500, 23000); // Wake at night, sleep during day
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob fox = (Mob) context.getEntity();
        Level level = fox.level();
        long dayTime = level.getDayTime() % 24000;

        // Check if should be sleeping based on time
        boolean shouldSleep = dayTime >= sleepTime || dayTime < wakeTime;

        if (shouldSleep && !isSleeping) {
            // Look for sleeping spot
            return findSleepingSpot(fox);
        } else if (!shouldSleep && isSleeping) {
            // Wake up
            wakeUp(fox);
        }

        if (isSleeping) {
            return new Vec3d();
        }

        return new Vec3d();
    }

    private Vec3d findSleepingSpot(Mob fox) {
        BlockPos currentPos = fox.blockPosition();
        BlockPos bestSpot = null;
        double lowestLight = Double.MAX_VALUE;

        // Search for sheltered spot
        int searchRadius = (int) Math.ceil(shelterSearchRange);

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = currentPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance > shelterSearchRange) {
                        continue;
                    }

                    // Check if position is valid for sleeping
                    if (!isValidSleepingSpot(fox, pos)) {
                        continue;
                    }

                    // Calculate light level (lower is better for sleeping)
                    double light = calculateDesirability(fox, pos);

                    // Prefer closer spots with low light
                    double score = light + (distance * 0.1);

                    if (score < lowestLight) {
                        lowestLight = score;
                        bestSpot = pos;
                    }
                }
            }
        }

        if (bestSpot != null) {
            sleepPosition = bestSpot;

            // If close enough, start sleeping
            if (fox.blockPosition().distSqr(bestSpot) < 4.0) {
                startSleeping(fox);
                return new Vec3d();
            }

            // Move toward sleeping spot
            Vec3d foxPos = new Vec3d(fox.getX(), fox.getY(), fox.getZ());
            Vec3d spotPos = new Vec3d(bestSpot.getX() + 0.5, bestSpot.getY(), bestSpot.getZ() + 0.5);
            Vec3d toSpot = Vec3d.sub(spotPos, foxPos);
            toSpot.normalize();
            toSpot.mult(0.3);
            return toSpot;
        }

        return new Vec3d();
    }

    private boolean isValidSleepingSpot(Mob fox, BlockPos pos) {
        Level level = fox.level();

        // Must have solid ground
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            return false;
        }

        // Must have space above
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.SNOW)) {
            return false;
        }

        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Check if fox can actually path there
        return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.SNOW);
    }

    private double calculateDesirability(Mob fox, BlockPos pos) {
        Level level = fox.level();

        // Check for overhead cover (leaves, solid blocks)
        boolean hasCover = false;
        for (int y = 1; y <= 3; y++) {
            BlockState aboveState = level.getBlockState(pos.above(y));
            if (aboveState.is(Blocks.ACACIA_LEAVES) ||
                aboveState.is(Blocks.BIRCH_LEAVES) ||
                aboveState.is(Blocks.DARK_OAK_LEAVES) ||
                aboveState.is(Blocks.JUNGLE_LEAVES) ||
                aboveState.is(Blocks.OAK_LEAVES) ||
                aboveState.is(Blocks.SPRUCE_LEAVES) ||
                aboveState.isSolidRender(level, pos.above(y))) {
                hasCover = true;
                break;
            }
        }

        // Base light level
        double light = level.getRawBrightness(pos, 0) / 15.0;

        // Prefer covered spots
        if (hasCover) {
            light -= 0.3;
        }

        return Math.max(0.0, light);
    }

    private void startSleeping(Mob fox) {
        isSleeping = true;

        // Set fox as sleeping if it's a vanilla fox
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            minecraftFox.setSleeping(true);
        }

        // Play sleep sound
        playSleepSound(fox);

        // Sleep particles
        spawnSleepParticles(fox);
    }

    private void wakeUp(Mob fox) {
        isSleeping = false;
        sleepPosition = null;

        // Wake up vanilla fox
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            minecraftFox.setSleeping(false);
        }

        // Play wake sound
        playWakeSound(fox);
    }

    private void playSleepSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(),
            net.minecraft.sounds.SoundEvents.FOX_SLEEP,
            net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.0f);
    }

    private void playWakeSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(),
            net.minecraft.sounds.SoundEvents.FOX_AMBIENT,
            net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.0f);
    }

    private void spawnSleepParticles(Mob fox) {
        if (fox.level().isClientSide) {
            return;
        }

        Vec3 pos = fox.position();
        for (int i = 0; i < 5; i++) {
            fox.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.HEART,
                pos.x + (fox.getRandom().nextDouble() - 0.5) * 0.5,
                pos.y + 0.5 + fox.getRandom().nextDouble() * 0.5,
                pos.z + (fox.getRandom().nextDouble() - 0.5) * 0.5,
                0, 0.02, 0
            );
        }
    }

    public boolean isSleeping() {
        return isSleeping;
    }

    public void setSleeping(boolean sleeping) {
        this.isSleeping = sleeping;
    }

    public BlockPos getSleepPosition() {
        return sleepPosition;
    }
}
