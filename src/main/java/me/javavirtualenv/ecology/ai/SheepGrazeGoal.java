package me.javavirtualenv.ecology.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 */
public class SheepGrazeGoal extends Goal {
    private final PathfinderMob mob;
    private final Level level;
    private final double searchRadius;
    private final double speedModifier;
    private BlockPos targetGrassPos;
    private int grazeCooldown;

    public SheepGrazeGoal(PathfinderMob mob, double searchRadius, double speedModifier) {
        this.mob = mob;
        this.level = mob.level();
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.grazeCooldown = 0;
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
        if (targetGrassPos == null) {
            return false;
        }

        BlockState blockState = level.getBlockState(targetGrassPos);
        if (!isGrass(blockState)) {
            targetGrassPos = null;
            return false;
        }

        return !mob.getNavigation().isDone() && targetGrassPos.closerToCenterThan(mob.position(), 2.0);
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
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetGrassPos != null && targetGrassPos.closerToCenterThan(mob.position(), 1.5)) {
            eatGrass();
            targetGrassPos = null;
            grazeCooldown = 200 + mob.getRandom().nextInt(200);
        }
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
                        double distance = mob.distSqr(pos);
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
        return block == Blocks.GRASS || block == Blocks.TALL_GRASS;
    }

    private void eatGrass() {
        if (targetGrassPos == null) {
            return;
        }

        BlockState blockState = level.getBlockState(targetGrassPos);

        if (isGrass(blockState)) {
            // Eat the grass
            level.destroyBlock(targetGrassPos, false);

            // Play eating sound
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                          SoundEvents.SHEEP_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // Trigger game event
            level.gameEvent(mob, GameEvent.EAT, targetGrassPos);

            // Spawn particles
            if (level instanceof ServerLevel serverLevel) {
                spawnEatParticles(serverLevel, targetGrassPos);
            }
        }
    }

    private void spawnEatParticles(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 8; i++) {
            double offsetX = level.getRandom().nextDouble() * 0.5 - 0.25;
            double offsetY = level.getRandom().nextDouble() * 0.5;
            double offsetZ = level.getRandom().nextDouble() * 0.5 - 0.25;

            level.sendParticles(
                    net.minecraft.core.particles.BlockParticle.BLOCK,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.1
            );
        }
    }
}
