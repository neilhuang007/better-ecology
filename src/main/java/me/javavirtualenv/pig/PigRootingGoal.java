package me.javavirtualenv.pig;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Goal for pigs to root in the ground.
 * Pigs root in grass blocks, mycelium, and podzol to find food items.
 * Creates particle effects and converts grass to dirt.
 */
public class PigRootingGoal extends MoveToBlockGoal {
    private static final int SEARCH_RANGE = 8;
    private static final int SEARCH_VERTICAL_RANGE = 2;
    private static final int ROOTING_DURATION = 40;
    private static final double TRUFFLE_CHANCE_GRASS = 0.05;
    private static final double TRUFFLE_CHANCE_MYCELIUM = 0.15;
    private static final double TRUFFLE_CHANCE_PODZOL = 0.12;
    private static final double GRASS_TO_DIRT_CHANCE = 0.3;

    private final Pig pig;
    private int rootingTimer;
    private final Random random = new Random();

    public PigRootingGoal(Pig pig, double speedModifier) {
        super(pig, speedModifier, SEARCH_RANGE, SEARCH_VERTICAL_RANGE);
        this.pig = pig;
        this.rootingTimer = 0;
    }

    @Override
    public boolean canUse() {
        if (!canStartRooting()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.rootingTimer < ROOTING_DURATION && isTargetBlock(pig.level(), this.blockPos);
    }

    @Override
    public void start() {
        super.start();
        this.rootingTimer = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.rootingTimer = 0;
    }

    @Override
    public void tick() {
        if (!this.isReachedTarget()) {
            super.tick();
            return;
        }

        pig.getLookControl().setLookAt(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);

        if (this.rootingTimer % 10 == 0) {
            playRootingEffects();
        }

        this.rootingTimer++;
        if (this.rootingTimer >= ROOTING_DURATION) {
            performRooting();
            stop();
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isRootableBlock(state);
    }

    private boolean canStartRooting() {
        if (pig.isBaby()) {
            return random.nextFloat() < 0.3;
        }
        return random.nextFloat() < 0.15;
    }

    private boolean isRootableBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) ||
               state.is(Blocks.MYCELIUM) ||
               state.is(Blocks.PODZOL) ||
               state.is(Blocks.DIRT) ||
               state.is(Blocks.COARSE_DIRT);
    }

    private void playRootingEffects() {
        Level level = pig.level();
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            Vec3 pos = pig.position();
            double offsetX = random.nextDouble() * 0.5 - 0.25;
            double offsetZ = random.nextDouble() * 0.5 - 0.25;

            BlockState belowState = level.getBlockState(pig.blockPosition().below());
            serverLevel.sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, belowState),
                pos.x + offsetX,
                pos.y,
                pos.z + offsetZ,
                5,
                0.15,
                0.05,
                0.15,
                0.02
            );
        }

        if (random.nextFloat() < 0.4) {
            float pitchVariation = random.nextFloat() * 0.3f;
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.PIG_AMBIENT,
                SoundSource.NEUTRAL,
                0.6f,
                0.9f + pitchVariation
            );
        }

        if (random.nextFloat() < 0.2) {
            level.playSound(
                null,
                pig.blockPosition(),
                SoundEvents.SNIFFER_SNIFFING,
                SoundSource.NEUTRAL,
                0.5f,
                1.2f
            );
        }
    }

    private void performRooting() {
        Level level = pig.level();
        BlockState currentState = level.getBlockState(blockPos);

        if (isRootableBlock(currentState)) {
            boolean foundTruffle = tryFindTruffle(currentState);
            if (!foundTruffle) {
                tryConvertGrassToDirt(currentState);
            }
        }
    }

    private boolean tryFindTruffle(BlockState state) {
        double truffleChance = getTruffleChance(state);

        if (random.nextDouble() < truffleChance) {
            spawnTruffle();
            return true;
        }
        return false;
    }

    private double getTruffleChance(BlockState state) {
        if (state.is(Blocks.MYCELIUM)) {
            return TRUFFLE_CHANCE_MYCELIUM;
        } else if (state.is(Blocks.PODZOL)) {
            return TRUFFLE_CHANCE_PODZOL;
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            return TRUFFLE_CHANCE_GRASS;
        }
        return 0.0;
    }

    private void spawnTruffle() {
        Level level = pig.level();

        if (!level.isClientSide()) {
            net.minecraft.world.item.ItemStack truffleStack = getPigTruffle();
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                level,
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5,
                truffleStack
            );
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);

            level.playSound(
                null,
                blockPos,
                SoundEvents.ITEM_PICKUP,
                SoundSource.NEUTRAL,
                0.3f,
                1.2f
            );

            BetterEcology.LOGGER.debug("Pig found a truffle at {}", blockPos);
        }
    }

    private net.minecraft.world.item.ItemStack getPigTruffle() {
        try {
            return new net.minecraft.world.item.ItemStack(me.javavirtualenv.item.ModItems.TRUFFLE);
        } catch (Exception e) {
            BetterEcology.LOGGER.debug("Truffle item not available, spawning mushroom instead");
            return new net.minecraft.world.item.ItemStack(Items.BROWN_MUSHROOM);
        }
    }

    private void tryConvertGrassToDirt(BlockState currentState) {
        if (!currentState.is(Blocks.GRASS_BLOCK)) {
            return;
        }

        if (random.nextDouble() < GRASS_TO_DIRT_CHANCE) {
            Level level = pig.level();
            if (!level.isClientSide()) {
                level.setBlock(blockPos, Blocks.DIRT.defaultBlockState(), 3);
                level.playSound(
                    null,
                    blockPos,
                    SoundEvents.HOE_TILL,
                    SoundSource.BLOCKS,
                    0.3f,
                    0.8f
                );
            }
        }
    }

    private boolean isTargetBlock(Level level, BlockPos pos) {
        return isValidTarget(level, pos);
    }

    @Override
    protected boolean isReachedTarget() {
        if (this.blockPos == null) {
            return false;
        }
        double distance = pig.distanceToSqr(
            this.blockPos.getX() + 0.5,
            this.blockPos.getY(),
            this.blockPos.getZ() + 0.5
        );
        return distance < 2.5;
    }
}
