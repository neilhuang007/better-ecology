package me.javavirtualenv.pig;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.behavior.core.HungerThirstPriority;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Random;

/**
 * Goal for pigs to seek out and eat crops.
 * Pigs will root through crop farms to find food.
 */
public class PigFeedingGoal extends MoveToBlockGoal {
    private static final int SEARCH_RANGE = 12;
    private static final int SEARCH_VERTICAL_RANGE = 2;
    private static final int EATING_DURATION = 30;
    private static final double FEEDING_CHANCE = 0.15;

    private final Pig pig;
    private int eatingTimer;
    private final Random random = new Random();

    public PigFeedingGoal(Pig pig, double speedModifier) {
        super(pig, speedModifier, SEARCH_RANGE, SEARCH_VERTICAL_RANGE);
        this.pig = pig;
        this.eatingTimer = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!shouldSeekFood()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.eatingTimer < EATING_DURATION && isTargetBlock(pig.level(), this.blockPos);
    }

    @Override
    public void start() {
        super.start();
        this.eatingTimer = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.eatingTimer = 0;
    }

    @Override
    public void tick() {
        if (!this.isReachedTarget()) {
            super.tick();
            return;
        }

        this.eatingTimer++;

        if (this.eatingTimer >= EATING_DURATION) {
            eatCrop();
            stop();
        } else if (this.eatingTimer % 10 == 0) {
            playEatingEffects();
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isEdibleCrop(state);
    }

    private boolean shouldSeekFood() {
        if (pig.isBaby()) {
            return random.nextFloat() < 0.2;
        }

        boolean isHungry = HungerThirstPriority.needsFood(pig);
        if (isHungry) {
            double hungerPriority = HungerThirstPriority.getHungerPriority(pig);
            if (hungerPriority > 0.7) {
                return true;
            }
            return random.nextFloat() < 0.4;
        }

        return random.nextFloat() < FEEDING_CHANCE;
    }

    private boolean isHungry() {
        return HungerThirstPriority.needsFood(pig);
    }

    private boolean isEdibleCrop(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CarrotBlock carrotBlock) {
            return state.getValue(CarrotBlock.AGE) == 7;
        }

        if (block == Blocks.POTATOES) {
            return state.getValue(CropBlock.AGE) == 7;
        }

        if (block == Blocks.BEETROOTS) {
            return state.getValue(CropBlock.AGE) == 3;
        }

        return false;
    }

    private boolean isReachedTarget() {
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

    private void playEatingEffects() {
        Level level = pig.level();

        level.playSound(
            null,
            pig.blockPosition(),
            SoundEvents.PIG_AMBIENT,
            SoundSource.NEUTRAL,
            0.4f,
            1.1f
        );
    }

    private void eatCrop() {
        Level level = pig.level();
        BlockState cropState = level.getBlockState(blockPos);
        Block cropBlock = cropState.getBlock();

        if (!isEdibleCrop(cropState)) {
            return;
        }

        if (!level.isClientSide()) {
            level.destroyBlock(blockPos, false);

            feedPig(cropBlock);

            BetterEcology.LOGGER.debug("Pig ate {} crop at {}", cropBlock, blockPos);
        }
    }

    private void feedPig(Block cropBlock) {
        if (!(pig instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        var hungerTag = component.getHandleTag("hunger");
        int currentHunger = hungerTag.getInt("hunger");
        int hungerRestore = getHungerRestoreAmount(cropBlock);
        int newHunger = Math.min(100, currentHunger + hungerRestore);
        hungerTag.putInt("hunger", newHunger);

        pig.heal(1.0f);

        BetterEcology.LOGGER.debug("Pig fed with {}, hunger: {} -> {}",
            cropBlock, currentHunger, newHunger);
    }

    private int getHungerRestoreAmount(Block cropBlock) {
        if (cropBlock == Blocks.CARROTS) {
            return 20;
        } else if (cropBlock == Blocks.POTATOES) {
            return 25;
        } else if (cropBlock == Blocks.BEETROOTS) {
            return 15;
        }
        return 10;
    }
}
