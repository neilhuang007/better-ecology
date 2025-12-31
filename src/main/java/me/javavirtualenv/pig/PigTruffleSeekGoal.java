package me.javavirtualenv.pig;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Goal for pigs to detect and seek out truffles.
 * Pigs can smell truffles from a distance and will excitedly pursue them.
 */
public class PigTruffleSeekGoal extends Goal {
    private static final int DETECTION_RANGE = 16;
    private static final int COOLDOWN_TICKS = 600;
    private static final double SPEED_MODIFIER = 1.2;

    private final Pig pig;
    private final Random random = new Random();
    private ItemEntity targetTruffle;
    private int cooldown;
    private int sniffTimer;

    public PigTruffleSeekGoal(Pig pig) {
        this.pig = pig;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (pig.isBaby()) {
            return random.nextFloat() < 0.05;
        }

        targetTruffle = findNearestTruffle();
        return targetTruffle != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetTruffle == null || !targetTruffle.isAlive()) {
            return false;
        }

        double distance = pig.distanceToSqr(targetTruffle);
        return distance < 256.0;
    }

    @Override
    public void start() {
        sniffTimer = 0;
    }

    @Override
    public void stop() {
        targetTruffle = null;
        cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        if (targetTruffle == null || !targetTruffle.isAlive()) {
            return;
        }

        double distance = pig.distanceToSqr(targetTruffle);

        if (distance > 1.5) {
            pig.getNavigation().moveTo(targetTruffle, SPEED_MODIFIER);
        } else {
            pig.getNavigation().stop();
        }

        pig.getLookControl().setLookAt(targetTruffle);

        if (sniffTimer % 20 == 0) {
            playSniffingEffects();
        }

        if (distance < 3.0 && random.nextFloat() < 0.1) {
            showExcitement();
        }

        sniffTimer++;
    }

    private ItemEntity findNearestTruffle() {
        Level level = pig.level();
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(
            ItemEntity.class,
            pig.getBoundingBox().inflate(DETECTION_RANGE)
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
            BetterEcology.LOGGER.debug("Error checking if item is truffle", e);
            return false;
        }
    }

    private void playSniffingEffects() {
        Level level = pig.level();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vec3 pos = pig.position();

            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.3;
                double offsetY = random.nextDouble() * 0.3;
                double offsetZ = (random.nextDouble() - 0.5) * 0.3;

                serverLevel.sendParticles(
                    ParticleTypes.SNEEZE,
                    pos.x + offsetX,
                    pos.y + 0.5 + offsetY,
                    pos.z + offsetZ,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.01
                );
            }
        }

        if (random.nextFloat() < 0.3) {
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.SNIFFER_SNIFFING,
                SoundSource.NEUTRAL,
                0.6f,
                1.3f + (random.nextFloat() * 0.2f)
            );
        }
    }

    private void showExcitement() {
        Level level = pig.level();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vec3 pos = pig.position();

            serverLevel.sendParticles(
                ParticleTypes.HEART,
                pos.x,
                pos.y + 0.5,
                pos.z,
                2,
                0.1,
                0.1,
                0.1,
                0.02
            );
        }

        level.playSound(
            null,
            pig.blockPosition(),
            SoundEvents.PIG_AMBIENT,
            SoundSource.NEUTRAL,
            0.7f,
            1.4f
        );
    }
}
