package me.javavirtualenv.ecology.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.EnumSet;

/**
 * AI goal for cows to actively seek and graze on grass blocks.
 * Cows will move toward grass when hungry and eat grass blocks,
 * converting them to dirt.
 *
 * Scientific basis:
 * - Cattle are grazers that feed on grass and other low vegetation
 * - They show bimodal feeding patterns (dawn and dusk peaks)
 * - Social facilitation: cows graze when herd members graze
 * - Giving-up density: abandon patch when grass is depleted
 */
public class CowGrazeGoal extends Goal {
    private final PathfinderMob mob;
    private final Level level;
    private final double searchRadius;
    private final double speedModifier;
    private final int hungerThreshold;
    private BlockPos targetGrassPos;
    private int grazeCooldown;
    private int ticksEating;
    private boolean isEating;
    private static final int EAT_ANIMATION_TICKS = 40;

    public CowGrazeGoal(PathfinderMob mob, double searchRadius, double speedModifier) {
        this(mob, searchRadius, speedModifier, 60);
    }

    public CowGrazeGoal(PathfinderMob mob, double searchRadius, double speedModifier, int hungerThreshold) {
        this.mob = mob;
        this.level = mob.level();
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
        this.hungerThreshold = hungerThreshold;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.grazeCooldown = 0;
        this.ticksEating = 0;
        this.isEating = false;
    }

    @Override
    public boolean canUse() {
        if (grazeCooldown > 0) {
            grazeCooldown--;
            return false;
        }

        // Randomly decide to look for grass (higher chance when hungry)
        float hungerBonus = isHungry() ? 0.05f : 0.01f;
        if (mob.getRandom().nextFloat() < hungerBonus) {
            targetGrassPos = findNearbyGrass();
            return targetGrassPos != null;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (isEating && ticksEating > 0) {
            return true;
        }

        if (targetGrassPos == null) {
            return false;
        }

        BlockState blockState = level.getBlockState(targetGrassPos);
        if (!isGrassBlock(blockState)) {
            targetGrassPos = null;
            return false;
        }

        double distance = mob.distanceToSqr(targetGrassPos.getX(), targetGrassPos.getY(), targetGrassPos.getZ());

        // Continue if close to grass or currently eating
        return (!mob.getNavigation().isDone() && distance < searchRadius * searchRadius) || ticksEating > 0;
    }

    @Override
    public void start() {
        if (targetGrassPos != null) {
            mob.getNavigation().moveTo(targetGrassPos.getX(), targetGrassPos.getY(), targetGrassPos.getZ(), speedModifier);
        }
    }

    @Override
    public void stop() {
        targetGrassPos = null;
        ticksEating = 0;
        isEating = false;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (isEating) {
            ticksEating--;

            if (ticksEating <= 0) {
                eatGrass();
                isEating = false;
                targetGrassPos = null;
                grazeCooldown = 400 + mob.getRandom().nextInt(600);
            }
            return;
        }

        if (targetGrassPos == null) {
            return;
        }

        double distance = mob.distanceToSqr(targetGrassPos.getX(), targetGrassPos.getY(), targetGrassPos.getZ());

        // If close enough to grass, start eating
        if (distance < 2.5) {
            startEating();
        } else if (!mob.getNavigation().isDone()) {
            // Not close yet, make sure we're still moving
            mob.getNavigation().moveTo(targetGrassPos.getX(), targetGrassPos.getY(), targetGrassPos.getZ(), speedModifier);
        }
    }

    private void startEating() {
        if (targetGrassPos == null || !isGrassBlock(level.getBlockState(targetGrassPos))) {
            return;
        }

        isEating = true;
        ticksEating = EAT_ANIMATION_TICKS;

        level.broadcastEntityEvent(mob, (byte) 10);

        mob.getNavigation().stop();
    }

    /**
     * Find the nearest grass block within search radius.
     */
    private BlockPos findNearbyGrass() {
        BlockPos mobPos = mob.blockPosition();
        BlockPos nearestGrass = null;
        double nearestDistance = Double.MAX_VALUE;
        double searchRadiusSq = searchRadius * searchRadius;

        // Search in expanding radius pattern
        int searchRadiusInt = (int) this.searchRadius;
        for (int x = -searchRadiusInt; x <= searchRadiusInt; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -searchRadiusInt; z <= searchRadiusInt; z++) {
                    BlockPos pos = mobPos.offset(x, y, z);
                    BlockState blockState = level.getBlockState(pos);

                    if (isGrassBlock(blockState)) {
                        double distance = mob.blockPosition().distSqr(pos);
                        if (distance < nearestDistance && distance < searchRadiusSq) {
                            nearestDistance = distance;
                            nearestGrass = pos;
                        }
                    }
                }
            }
        }

        return nearestGrass;
    }

    /**
     * Check if block is a grass block (cows eat grass blocks, not tall grass).
     */
    private boolean isGrassBlock(BlockState blockState) {
        return blockState.is(Blocks.GRASS_BLOCK);
    }

    /**
     * Eat the grass, converting it to dirt.
     */
    private void eatGrass() {
        if (targetGrassPos == null) {
            return;
        }

        BlockState blockState = level.getBlockState(targetGrassPos);

        if (isGrassBlock(blockState)) {
            // Convert grass block to dirt
            level.setBlock(targetGrassPos, Blocks.DIRT.defaultBlockState(), 3);

            // Play eating sound
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.COW_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // Trigger game event
            level.gameEvent(mob, GameEvent.EAT, targetGrassPos);

            // Spawn eat particles
            if (level instanceof ServerLevel serverLevel) {
                spawnEatParticles(serverLevel, targetGrassPos);
            }
        }
    }

    /**
     * Spawn block break particles when eating grass.
     */
    private void spawnEatParticles(ServerLevel serverLevel, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        for (int i = 0; i < 8; i++) {
            double offsetX = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;
            double offsetY = serverLevel.getRandom().nextDouble() * 0.5;
            double offsetZ = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;

            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 1.0 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.1
            );
        }
    }

    /**
     * Check if cow is hungry and should seek food more actively.
     */
    private boolean isHungry() {
        // This would integrate with the hunger system
        // For now, use a simple probability
        return mob.getRandom().nextFloat() < 0.3f;
    }
}
