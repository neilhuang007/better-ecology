package me.javavirtualenv.pig;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Goal for pigs to seek out and bathe in mud or water.
 * Mud bathing provides temperature regulation and condition restoration.
 * Applies visual mud effect to the pig after bathing.
 */
public class PigMudBathingGoal extends MoveToBlockGoal {
    private static final int SEARCH_RANGE = 16;
    private static final int SEARCH_VERTICAL_RANGE = 4;
    private static final int BATHING_DURATION = 200;
    private static final double CONDITION_GAIN_PER_TICK = 0.1;
    private static final int MUD_EFFECT_DURATION = 6000;

    private final Pig pig;
    private int bathingTimer;
    private final Random random = new Random();

    public PigMudBathingGoal(Pig pig, double speedModifier) {
        super(pig, speedModifier, SEARCH_RANGE, SEARCH_VERTICAL_RANGE);
        this.pig = pig;
        this.bathingTimer = 0;
    }

    @Override
    public boolean canUse() {
        if (!shouldSeekMud()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.bathingTimer < BATHING_DURATION && isTargetBlock(pig.level(), this.blockPos);
    }

    @Override
    public void start() {
        super.start();
        this.bathingTimer = 0;
    }

    @Override
    public void stop() {
        super.stop();
        if (this.bathingTimer > 60) {
            applyMudEffect();
        }
        this.bathingTimer = 0;
    }

    @Override
    public void tick() {
        if (!this.isReachedTarget()) {
            super.tick();
            return;
        }

        if (this.bathingTimer % 15 == 0) {
            playBathingEffects();
        }

        this.bathingTimer++;

        if (this.bathingTimer % 20 == 0) {
            restoreCondition();
        }

        if (this.bathingTimer >= BATHING_DURATION) {
            stop();
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isMudOrWater(state);
    }

    private boolean shouldSeekMud() {
        if (pig.isInWaterOrBubble()) {
            return false;
        }

        if (pig.isBaby()) {
            return random.nextFloat() < 0.1;
        }

        boolean needsCondition = pig.getHealth() < pig.getMaxHealth() * 0.8;
        boolean isHot = isHotEnvironment();

        if (needsCondition || isHot) {
            return random.nextFloat() < 0.4;
        }

        return random.nextFloat() < 0.05;
    }

    private boolean isHotEnvironment() {
        Level level = pig.level();
        if (level.isDay()) {
            BlockPos pos = pig.blockPosition();
            return level.canSeeSky(pos);
        }
        return false;
    }

    private boolean isMudOrWater(BlockState state) {
        return state.is(Blocks.WATER) ||
               state.is(Blocks.MUD) ||
               state.is(Blocks.CLAY);
    }

    private boolean isReachedTarget() {
        if (this.blockPos == null) {
            return false;
        }

        BlockState targetState = pig.level().getBlockState(blockPos);
        if (!isMudOrWater(targetState)) {
            return false;
        }

        double distance = pig.distanceToSqr(
            this.blockPos.getX() + 0.5,
            this.blockPos.getY(),
            this.blockPos.getZ() + 0.5
        );
        return distance < 3.0;
    }

    private void playBathingEffects() {
        Level level = pig.level();

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            Vec3 pos = pig.position();

            for (int i = 0; i < 3; i++) {
                double offsetX = random.nextDouble() * 0.6 - 0.3;
                double offsetZ = random.nextDouble() * 0.6 - 0.3;

                serverLevel.sendParticles(
                    ParticleTypes.DRIPPING_WATER,
                    pos.x + offsetX,
                    pos.y + 0.3,
                    pos.z + offsetZ,
                    1,
                    0.05,
                    0.0,
                    0.05,
                    0.01
                );
            }
        }

        if (random.nextFloat() < 0.2) {
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.PIG_AMBIENT,
                SoundSource.NEUTRAL,
                0.4f,
                0.8f
            );
        }
    }

    private void restoreCondition() {
        if (!(pig instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        var tag = component.getHandleTag("condition");
        int currentCondition = tag.getInt("condition");
        int newCondition = (int) Math.min(100, currentCondition + 2);
        tag.putInt("condition", newCondition);

        BetterEcology.LOGGER.debug("Pig restored condition through mud bathing: {} -> {}",
            currentCondition, newCondition);
    }

    private void applyMudEffect() {
        if (!(pig instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        var pigData = component.getHandleTag("pig_behavior");
        pigData.putInt("mudEffectTimer", MUD_EFFECT_DURATION);

        Level level = pig.level();
        level.playSound(
            null,
            pig.blockPosition(),
            SoundEvents.SLIME_SQUISH,
            SoundSource.NEUTRAL,
            0.5f,
            1.0f
        );

        BetterEcology.LOGGER.debug("Pig applied mud effect for {} ticks", MUD_EFFECT_DURATION);
    }
}
