package me.javavirtualenv.pig;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Goal for pigs to sniff out nearby truffles or food.
 * Pigs will pause and sniff the air when they detect interesting scents.
 */
public class PigSniffTrufflesGoal extends Goal {
    private static final int SNIFF_DETECTION_RANGE = 12;
    private static final int SNIFF_DURATION = 60;
    private static final int COOLDOWN_TICKS = 200;

    private final Pig pig;
    private final Random random = new Random();
    private BlockPos sniffTarget;
    private int sniffTimer;
    private int cooldown;

    public PigSniffTrufflesGoal(Pig pig) {
        this.pig = pig;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (pig.isBaby()) {
            return random.nextFloat() < 0.02;
        }

        sniffTarget = findInterestingScent();
        return sniffTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return sniffTimer < SNIFF_DURATION;
    }

    @Override
    public void start() {
        sniffTimer = 0;
    }

    @Override
    public void stop() {
        sniffTimer = 0;
        sniffTarget = null;
        cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        if (sniffTarget != null) {
            pig.getLookControl().setLookAt(
                sniffTarget.getX(),
                sniffTarget.getY(),
                sniffTarget.getZ(),
                30.0f,
                30.0f
            );
        }

        if (sniffTimer % 15 == 0) {
            playSniffEffects();
        }

        sniffTimer++;
    }

    private BlockPos findInterestingScent() {
        Level level = pig.level();

        ItemEntity nearestTruffle = findNearestTruffle();
        if (nearestTruffle != null) {
            return nearestTruffle.blockPosition();
        }

        BlockPos foodBlock = findNearbyFood();
        if (foodBlock != null) {
            return foodBlock;
        }

        BlockPos myceliumBlock = findNearbyMycelium();
        if (myceliumBlock != null) {
            return myceliumBlock;
        }

        return null;
    }

    private ItemEntity findNearestTruffle() {
        Level level = pig.level();
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(
            ItemEntity.class,
            pig.getBoundingBox().inflate(SNIFF_DETECTION_RANGE)
        );

        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity item : nearbyItems) {
            if (isTruffle(item)) {
                double distance = pig.distanceToSqr(item);
                if (distance < nearestDistance) {
                    nearest = item;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    private boolean isTruffle(ItemEntity item) {
        try {
            return item.getItem().is(ModItems.TRUFFLE);
        } catch (Exception e) {
            return false;
        }
    }

    private BlockPos findNearbyFood() {
        Level level = pig.level();
        BlockPos pigPos = pig.blockPosition();
        int searchRadius = 8;

        for (BlockPos pos : BlockPos.betweenClosed(
            pigPos.offset(-searchRadius, -2, -searchRadius),
            pigPos.offset(searchRadius, 2, searchRadius)
        )) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.CARROTS) ||
                state.is(Blocks.POTATOES) ||
                state.is(Blocks.BEETROOTS)) {
                return pos;
            }
        }

        return null;
    }

    private BlockPos findNearbyMycelium() {
        Level level = pig.level();
        BlockPos pigPos = pig.blockPosition();
        int searchRadius = 6;

        for (BlockPos pos : BlockPos.betweenClosed(
            pigPos.offset(-searchRadius, -1, -searchRadius),
            pigPos.offset(searchRadius, 1, searchRadius)
        )) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
                return pos;
            }
        }

        return null;
    }

    private void playSniffEffects() {
        Level level = pig.level();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vec3 pos = pig.position();
            Vec3 lookAngle = pig.getLookAngle();

            for (int i = 0; i < 2; i++) {
                double offsetX = lookAngle.x * 0.5 + (random.nextDouble() - 0.5) * 0.2;
                double offsetY = 0.5 + random.nextDouble() * 0.2;
                double offsetZ = lookAngle.z * 0.5 + (random.nextDouble() - 0.5) * 0.2;

                serverLevel.sendParticles(
                    ParticleTypes.SNEEZE,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    1,
                    lookAngle.x * 0.05,
                    0.02,
                    lookAngle.z * 0.05,
                    0.01
                );
            }
        }

        if (random.nextFloat() < 0.4) {
            float pitch = 1.2f + (random.nextFloat() * 0.2f);
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.SNIFFER_SNIFFING,
                SoundSource.NEUTRAL,
                0.5f,
                pitch
            );
        }

        if (random.nextFloat() < 0.15) {
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.PIG_AMBIENT,
                SoundSource.NEUTRAL,
                0.4f,
                1.1f
            );
        }
    }
}
