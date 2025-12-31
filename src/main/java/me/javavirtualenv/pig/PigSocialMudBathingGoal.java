package me.javavirtualenv.pig;

import me.javavirtualenv.BetterEcology;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Goal for pigs to socialize while mud bathing.
 * Pigs in mud will attract nearby pigs to join them.
 */
public class PigSocialMudBathingGoal extends Goal {
    private static final int ATTRACTION_RANGE = 12;
    private static final int MAX_BATHING_PIGS = 4;
    private static final int SOCIAL_DURATION = 400;
    private static final double ATTRACTION_CHANCE = 0.3;

    private final Pig pig;
    private final Random random = new Random();
    private int socialTimer;
    private BlockPos mudPosition;
    private boolean isLeader;

    public PigSocialMudBathingGoal(Pig pig) {
        this.pig = pig;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!pig.isInWaterOrBubble() && !isInMud()) {
            return false;
        }

        List<Pig> nearbyPigs = getNearbyBathingPigs();
        if (!nearbyPigs.isEmpty()) {
            isLeader = false;
            mudPosition = findMudPosition(nearbyPigs);
            return true;
        }

        if (random.nextFloat() < 0.05) {
            isLeader = true;
            mudPosition = pig.blockPosition();
            List<Pig> currentBathers = getNearbyBathingPigs();
            return currentBathers.size() < MAX_BATHING_PIGS;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return socialTimer < SOCIAL_DURATION &&
               (pig.isInWaterOrBubble() || isInMud()) &&
               getNearbyBathingPigs().size() <= MAX_BATHING_PIGS;
    }

    @Override
    public void start() {
        socialTimer = 0;

        if (isLeader) {
            attractNearbyPigs();
        }
    }

    @Override
    public void stop() {
        socialTimer = 0;
        mudPosition = null;
        isLeader = false;
    }

    @Override
    public void tick() {
        socialTimer++;

        if (mudPosition != null) {
            pig.getLookControl().setLookAt(
                mudPosition.getX(),
                mudPosition.getY(),
                mudPosition.getZ()
            );
        }

        if (isLeader && socialTimer % 100 == 0) {
            callOtherPigs();
        }

        if (socialTimer % 40 == 0) {
            playSocialEffects();
        }

        if (socialTimer % 60 == 0) {
            showSocialParticles();
        }
    }

    private boolean isInMud() {
        Level level = pig.level();
        BlockPos pos = pig.blockPosition();
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.MUD) ||
               state.is(Blocks.CLAY) ||
               level.getBlockState(pos.below()).is(Blocks.MUD);
    }

    private List<Pig> getNearbyBathingPigs() {
        return pig.level().getEntitiesOfClass(
            Pig.class,
            pig.getBoundingBox().inflate(ATTRACTION_RANGE)
        );
    }

    private BlockPos findMudPosition(List<Pig> pigs) {
        for (Pig other : pigs) {
            if (other != pig && (other.isInWaterOrBubble() || isInMud())) {
                return other.blockPosition();
            }
        }
        return pig.blockPosition();
    }

    private void attractNearbyPigs() {
        List<Pig> nearbyPigs = getNearbyBathingPigs();

        for (Pig other : nearbyPigs) {
            if (other != pig && random.nextFloat() < ATTRACTION_CHANCE) {
                callPigToMud(other);
            }
        }
    }

    private void callOtherPigs() {
        List<Pig> nearbyPigs = getNearbyBathingPigs();

        for (Pig other : nearbyPigs) {
            if (other != pig && !other.isInWaterOrBubble()) {
                callPigToMud(other);
            }
        }
    }

    private void callPigToMud(Pig other) {
        Level level = pig.level();
        double distance = pig.distanceToSqr(other);

        if (distance < ATTRACTION_RANGE * ATTRACTION_RANGE) {
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.PIG_AMBIENT,
                SoundSource.NEUTRAL,
                0.8f,
                1.0f
            );

            BetterEcology.LOGGER.debug("Pig calling other pig to mud bath");
        }
    }

    private void playSocialEffects() {
        Level level = pig.level();

        if (random.nextFloat() < 0.3) {
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.PIG_AMBIENT,
                SoundSource.NEUTRAL,
                0.5f,
                0.8f + (random.nextFloat() * 0.3f)
            );
        }

        if (random.nextFloat() < 0.1) {
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.SLIME_SQUISH,
                SoundSource.NEUTRAL,
                0.4f,
                1.0f
            );
        }
    }

    private void showSocialParticles() {
        Level level = pig.level();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vec3 pos = pig.position();
            int nearbyCount = getNearbyBathingPigs().size();

            int particleCount = Math.min(5, nearbyCount + 1);
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 1.0;
                double offsetZ = (random.nextDouble() - 0.5) * 1.0;

                serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.x + offsetX,
                    pos.y + 0.3,
                    pos.z + offsetZ,
                    1,
                    0.05,
                    0.05,
                    0.05,
                    0.01
                );
            }
        }
    }
}
