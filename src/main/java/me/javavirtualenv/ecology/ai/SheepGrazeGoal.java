package me.javavirtualenv.ecology.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.EnumSet;

/**
 * AI goal for sheep to actively seek and graze on grass.
 * Sheep will move toward grass blocks when hungry and eat them.
 * Triggers the vanilla eating animation when eating grass.
 */
public class SheepGrazeGoal extends Goal {
    private static final int EAT_ANIMATION_TICKS = 40;
    private final PathfinderMob mob;
    private final Level level;
    private final double searchRadius;
    private final double speedModifier;
    private BlockPos targetGrassPos;
    private int grazeCooldown;
    private int eatAnimationTick;
    private boolean isEating;

    public SheepGrazeGoal(PathfinderMob mob, double searchRadius, double speedModifier) {
        this.mob = mob;
        this.level = mob.level();
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.grazeCooldown = 0;
        this.eatAnimationTick = 0;
        this.isEating = false;
    }

    @Override
    public boolean canUse() {
        if (grazeCooldown > 0) {
            grazeCooldown--;
            return false;
        }

        if (mob.getRandom().nextFloat() < 0.02) {
            targetGrassPos = findNearbyGrass();
            return targetGrassPos != null;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue if we're in the middle of eating animation
        if (isEating && eatAnimationTick > 0) {
            return true;
        }

        if (targetGrassPos == null) {
            return false;
        }

        BlockState blockState = level.getBlockState(targetGrassPos);
        if (!isGrass(blockState)) {
            targetGrassPos = null;
            return false;
        }

        return !mob.getNavigation().isDone() || targetGrassPos.closerToCenterThan(mob.position(), 2.0);
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
        eatAnimationTick = 0;
        isEating = false;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        // If we're in the middle of eating animation, count down
        if (isEating) {
            eatAnimationTick = Math.max(0, eatAnimationTick - 1);

            // Near the end of animation (at tick 4), actually eat the grass
            if (eatAnimationTick == 4) {
                finishEating();
            }

            // Animation complete
            if (eatAnimationTick == 0) {
                isEating = false;
                targetGrassPos = null;
                grazeCooldown = 200 + mob.getRandom().nextInt(200);
            }
            return;
        }

        // Check if we've reached the target grass
        if (targetGrassPos != null && targetGrassPos.closerToCenterThan(mob.position(), 1.5)) {
            startEating();
        }
    }

    /**
     * Start the eating animation - broadcasts entity event to trigger client-side animation.
     */
    private void startEating() {
        if (targetGrassPos == null || !isGrass(level.getBlockState(targetGrassPos))) {
            return;
        }

        isEating = true;
        eatAnimationTick = EAT_ANIMATION_TICKS;

        // Broadcast entity event 10 to trigger eating animation on clients
        level.broadcastEntityEvent(mob, (byte) 10);

        // Stop movement during eating
        mob.getNavigation().stop();
    }

    private BlockPos findNearbyGrass() {
        BlockPos mobPos = mob.blockPosition();
        BlockPos nearestGrass = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search in a spiral pattern around the mob
        int searchRadius = (int) this.searchRadius;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = mobPos.offset(x, y, z);
                    BlockState blockState = level.getBlockState(pos);

                    if (isGrass(blockState)) {
                        double distance = mob.blockPosition().distSqr(pos);
                        if (distance < nearestDistance && distance < this.searchRadius * this.searchRadius) {
                            nearestDistance = distance;
                            nearestGrass = pos;
                        }
                    }
                }
            }
        }

        return nearestGrass;
    }

    private boolean isGrass(BlockState blockState) {
        Block block = blockState.getBlock();
        return block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS;
    }

    /**
     * Complete the eating action - destroys grass and triggers mob.ate() for wool regrowth.
     */
    private void finishEating() {
        if (targetGrassPos == null) {
            return;
        }

        BlockState blockState = level.getBlockState(targetGrassPos);

        if (isGrass(blockState)) {
            // Spawn particles before destroying block (need the blockState)
            if (level instanceof ServerLevel serverLevel) {
                spawnEatParticles(serverLevel, targetGrassPos, blockState);
            }

            // Destroy the grass block
            level.destroyBlock(targetGrassPos, false);

            // Trigger game event
            level.gameEvent(mob, GameEvent.EAT, targetGrassPos);

            // Call ate() method which triggers wool regrowth in sheep
            mob.ate();
        }
    }

    private void spawnEatParticles(ServerLevel serverLevel, BlockPos pos, BlockState blockState) {
        for (int i = 0; i < 8; i++) {
            double offsetX = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;
            double offsetY = serverLevel.getRandom().nextDouble() * 0.5;
            double offsetZ = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;

            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.1
            );
        }
    }
}
