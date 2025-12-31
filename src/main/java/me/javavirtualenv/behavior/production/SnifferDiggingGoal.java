package me.javavirtualenv.behavior.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sniffer-specific digging goal for ancient seeds.
 * <p>
 * Sniffers dig for ancient seeds and moss in dirt/grass blocks.
 * They can find:
 * - Torchflower seeds
 * - Pitcher pods
 * - Ancient moss
 * <p>
 * Production is limited daily and influenced by the sniffer's health and condition.
 */
public class SnifferDiggingGoal extends ResourceGatheringGoal {

    private static final int DIGGING_DURATION = 80;
    private static final int DAILY_DIG_LIMIT = 4;
    private static final int DAILY_RESET_TICKS = 24000;

    private final Sniffer sniffer;
    private int dailyDigs;
    private int lastDay;

    public SnifferDiggingGoal(Sniffer sniffer) {
        super(sniffer, 16.0, 200, DIGGING_DURATION);
        this.sniffer = sniffer;
        this.dailyDigs = 0;
        this.lastDay = (int) (sniffer.level().getDayTime() / DAILY_RESET_TICKS);
    }

    @Override
    public boolean canUse() {
        if (!sniffer.isAlive()) {
            return false;
        }

        if (sniffer.isBaby()) {
            return false;
        }

        long currentDay = sniffer.level().getDayTime() / DAILY_RESET_TICKS;
        if (currentDay != lastDay) {
            dailyDigs = 0;
            lastDay = (int) currentDay;
        }

        if (dailyDigs >= DAILY_DIG_LIMIT) {
            return false;
        }

        return super.canUse();
    }

    @Override
    protected BlockPos findNearestResource() {
        BlockPos snifferPos = sniffer.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = searchRadius;

        for (BlockPos pos : BlockPos.betweenClosed(
            snifferPos.getX() - (int) searchRadius,
            snifferPos.getY() - 4,
            snifferPos.getZ() - (int) searchRadius,
            snifferPos.getX() + (int) searchRadius,
            snifferPos.getY() + 2,
            snifferPos.getZ() + (int) searchRadius
        )) {
            if (isValidDirtBlock(pos)) {
                double distance = sniffer.position().distanceTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5
                );

                if (distance < nearestDistance) {
                    nearest = pos;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    @Override
    protected boolean isValidResource(BlockPos pos) {
        return isValidDirtBlock(pos);
    }

    private boolean isValidDirtBlock(BlockPos pos) {
        if (!sniffer.level().isLoaded(pos)) {
            return false;
        }

        BlockState state = sniffer.level().getBlockState(pos);

        return state.is(Blocks.DIRT) ||
               state.is(Blocks.GRASS_BLOCK) ||
               state.is(Blocks.PODZOL) ||
               state.is(Blocks.COARSE_DIRT) ||
               state.is(Blocks.MYCELIUM) ||
               state.is(Blocks.ROOTED_DIRT);
    }

    @Override
    protected void gatherResource() {
        if (targetResourcePos == null) {
            return;
        }

        gatheringTicks++;

        if (gatheringTicks >= 20 && gatheringTicks % 20 == 0) {
            sniffer.level().playSound(
                null,
                targetResourcePos,
                SoundEvents.SNIFFER_DIGGING,
                SoundSource.BLOCKS,
                0.5f,
                1.0f
            );
        }

        if (gatheringTicks >= gatheringDuration) {
            digUpResource();
            hasResource = true;
            dailyDigs++;
        }
    }

    @Override
    protected void onResourceDelivered() {
        gatheringTicks = 0;

        EcologyComponent component = EcologyComponent.getFromEntity(sniffer);
        if (component != null) {
            CompoundTag productionData = component.getHandleTag("production");

            double currentAmount = productionData.getDouble("amount");
            double quality = productionData.getDouble("quality");

            double addedAmount = 25.0 * quality;
            productionData.putDouble("amount", currentAmount + addedAmount);
        }
    }

    /**
     * Digs up the resource and spawns items.
     */
    private void digUpResource() {
        if (sniffer.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) sniffer.level();

        BlockState originalState = serverLevel.getBlockState(targetResourcePos);
        serverLevel.setBlock(
            targetResourcePos,
            Blocks.DIRT_PATH.defaultBlockState(),
            3
        );

        float random = serverLevel.random.nextFloat();

        if (random < 0.35) {
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                serverLevel,
                targetResourcePos.getX() + 0.5,
                targetResourcePos.getY() + 1.0,
                targetResourcePos.getZ() + 0.5,
                new net.minecraft.world.item.ItemStack(Blocks.TORCHFLOWER)
            );
            itemEntity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(itemEntity);
        } else if (random < 0.55) {
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                serverLevel,
                targetResourcePos.getX() + 0.5,
                targetResourcePos.getY() + 1.0,
                targetResourcePos.getZ() + 0.5,
                new net.minecraft.world.item.ItemStack(Blocks.PITCHER_PLANT)
            );
            itemEntity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(itemEntity);
        } else {
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                serverLevel,
                targetResourcePos.getX() + 0.5,
                targetResourcePos.getY() + 1.0,
                targetResourcePos.getZ() + 0.5,
                new net.minecraft.world.item.ItemStack(Blocks.MOSS_BLOCK)
            );
            itemEntity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(itemEntity);
        }

        serverLevel.levelEvent(2001, targetResourcePos, net.minecraft.world.level.block.Block.getId(originalState));
    }

    @Override
    protected BlockPos getHomePosition() {
        return sniffer.blockPosition();
    }
}
