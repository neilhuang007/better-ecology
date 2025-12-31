package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

/**
 * Fox berry foraging behavior.
 * <p>
 * Foxes are highly attracted to sweet berries and will:
 * - Actively seek out sweet berry bushes
 * - Show excitement (particles, sounds) when finding berries
 * - Eat berries to restore hunger
 * - Prefer berry bushes with higher growth stages
 * <p>
 * Scientific basis: While real foxes are primarily carnivorous, they are opportunistic
 * omnivores and will eat fruits when available. Sweet berries are a seasonal favorite
 * that provide quick energy.
 */
public class FoxBerryForagingBehavior extends SteeringBehavior {

    private final double searchRange;
    private final double attractionStrength;
    private final double eatingSpeed;
    private final int maxEatDuration;

    private BlockPos targetBerryBush;
    private int eatTimer = 0;
    private boolean isEating = false;

    public FoxBerryForagingBehavior(double searchRange, double attractionStrength,
                                    double eatingSpeed, int maxEatDuration) {
        this.searchRange = searchRange;
        this.attractionStrength = attractionStrength;
        this.eatingSpeed = eatingSpeed;
        this.maxEatDuration = maxEatDuration;
    }

    public FoxBerryForagingBehavior() {
        this(24.0, 0.8, 0.5, 40);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob fox = (Mob) context.getEntity();
        Level level = fox.level();
        Vec3d foxPos = context.getPosition();

        // If currently eating, continue eating
        if (isEating) {
            return continueEating(fox);
        }

        // Check if target bush is still valid
        if (targetBerryBush != null) {
            BlockState blockState = level.getBlockState(targetBerryBush);
            if (!isSweetBerryBush(blockState) || !hasBerries(blockState)) {
                targetBerryBush = null;
            }
        }

        // Find berry bush if none targeted
        if (targetBerryBush == null) {
            targetBerryBush = findNearestBerryBush(fox, foxPos);
            if (targetBerryBush != null) {
                playExcitementSound(fox);
                spawnExcitementParticles(fox);
            }
        }

        // If no bush found, return empty
        if (targetBerryBush == null) {
            return new Vec3d();
        }

        // Calculate distance to bush
        Vec3 bushPos = Vec3.atCenterOf(targetBerryBush);
        double distance = foxPos.distanceTo(new Vec3d(bushPos.x, foxPos.y, bushPos.z));

        // If close enough, start eating
        if (distance < 1.5) {
            return startEating(fox);
        }

        // Move toward berry bush
        return moveTowardBerryBush(foxPos, bushPos, distance);
    }

    private Vec3d moveTowardBerryBush(Vec3d foxPos, Vec3 bushPos, double distance) {
        Vec3d toBush = new Vec3d(bushPos.x - foxPos.x, bushPos.y - foxPos.y, bushPos.z - foxPos.z);

        // Prioritize bushes with more berries
        toBush.normalize();
        toBush.mult(attractionStrength);

        // Slow down as approaching
        if (distance < 4.0) {
            toBush.mult(0.5);
        }

        return toBush;
    }

    private Vec3d startEating(Mob fox) {
        isEating = true;
        eatTimer = 0;

        // Eat berries from bush
        Level level = fox.level();
        BlockState blockState = level.getBlockState(targetBerryBush);

        if (hasBerries(blockState)) {
            IntegerProperty ageProperty = SweetBerryBushBlock.AGE;
            int currentAge = blockState.getValue(ageProperty);

            if (currentAge > 0) {
                // Reduce berry stage
                BlockState newState = blockState.setValue(ageProperty, currentAge - 1);
                level.setBlock(targetBerryBush, newState, 3);

                // Play eating sound
                playEatSound(fox);

                // Spawn particles
                spawnEatParticles(fox);
            }
        }

        return new Vec3d();
    }

    private Vec3d continueEating(Mob fox) {
        eatTimer++;

        // Check if done eating
        if (eatTimer >= maxEatDuration) {
            isEating = false;
            targetBerryBush = null;
            return new Vec3d();
        }

        // Stay still while eating
        return new Vec3d();
    }

    private BlockPos findNearestBerryBush(Mob fox, Vec3d foxPos) {
        Level level = fox.level();
        BlockPos nearestBush = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search in expanding radius
        BlockPos foxBlockPos = fox.blockPosition();
        int searchRadius = (int) Math.ceil(searchRange);

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = foxBlockPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance > searchRange) {
                        continue;
                    }

                    BlockState blockState = level.getBlockState(pos);
                    if (!isSweetBerryBush(blockState)) {
                        continue;
                    }

                    if (!hasBerries(blockState)) {
                        continue;
                    }

                    // Prioritize bushes with more berries
                    int berryStage = blockState.getValue(SweetBerryBushBlock.AGE);
                    double adjustedDistance = distance - (berryStage * 2.0);

                    if (adjustedDistance < nearestDistance) {
                        nearestDistance = adjustedDistance;
                        nearestBush = pos;
                    }
                }
            }
        }

        return nearestBush;
    }

    private boolean isSweetBerryBush(BlockState blockState) {
        return blockState.is(Blocks.SWEET_BERRY_BUSH);
    }

    private boolean hasBerries(BlockState blockState) {
        if (!isSweetBerryBush(blockState)) {
            return false;
        }
        int age = blockState.getValue(SweetBerryBushBlock.AGE);
        return age > 0;
    }

    private void playExcitementSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_AMBIENT,
            SoundSource.NEUTRAL, 1.0f, 1.5f);
    }

    private void playEatSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_EAT,
            SoundSource.NEUTRAL, 0.8f, 1.0f);
    }

    private void spawnExcitementParticles(Mob fox) {
        if (fox.level().isClientSide) {
            return;
        }

        Vec3 pos = fox.position();
        for (int i = 0; i < 5; i++) {
            fox.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.HEART,
                pos.x + (fox.getRandom().nextDouble() - 0.5) * 0.5,
                pos.y + 1.0 + fox.getRandom().nextDouble() * 0.5,
                pos.z + (fox.getRandom().nextDouble() - 0.5) * 0.5,
                0, 0.05, 0
            );
        }
    }

    private void spawnEatParticles(Mob fox) {
        if (fox.level().isClientSide) {
            return;
        }

        Vec3 pos = fox.position();
        fox.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.ITEM,
            pos.x, pos.y + 0.5, pos.z,
            (fox.getRandom().nextDouble() - 0.5) * 0.1,
            0.1,
            (fox.getRandom().nextDouble() - 0.5) * 0.1
        );
    }

    public BlockPos getTargetBerryBush() {
        return targetBerryBush;
    }

    public boolean isEating() {
        return isEating;
    }

    public void setTargetBerryBush(BlockPos pos) {
        this.targetBerryBush = pos;
    }

    public void setEating(boolean eating) {
        this.isEating = eating;
    }
}
