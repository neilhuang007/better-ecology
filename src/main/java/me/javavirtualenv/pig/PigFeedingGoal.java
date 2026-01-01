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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Goal for pigs to seek out and eat crops.
 * Pigs prefer dropped crops over growing crops, giving players time to collect harvests.
 *
 * This behavior follows vanilla design principles:
 * - Bad things happen but are preventable (fences protect crops within 5 blocks)
 * - Player presence stops the behavior (within 16 blocks)
 * - Takes time to complete (80 ticks = 4 seconds to eat, player can intervene)
 * - Pigs prioritize dropped crops over growing crops
 * - Search range reduced to 8 blocks for more manageable behavior
 */
public class PigFeedingGoal extends MoveToBlockGoal {
    private static final int SEARCH_RANGE = 8;
    private static final int SEARCH_VERTICAL_RANGE = 2;
    private static final int EATING_DURATION = 80;
    private static final int PLAYER_DETECTION_RANGE = 16;
    private static final int FENCE_PROTECTION_RANGE = 5;
    private static final double FEEDING_CHANCE = 0.15;
    private static final Predicate<BlockState> IS_FENCE = state ->
        state.is(Blocks.OAK_FENCE) || state.is(Blocks.SPRUCE_FENCE) ||
        state.is(Blocks.BIRCH_FENCE) || state.is(Blocks.JUNGLE_FENCE) ||
        state.is(Blocks.ACACIA_FENCE) || state.is(Blocks.DARK_OAK_FENCE) ||
        state.is(Blocks.CRIMSON_FENCE) || state.is(Blocks.WARPED_FENCE) ||
        state.is(Blocks.MANGROVE_FENCE) || state.is(Blocks.CHERRY_FENCE) ||
        state.is(Blocks.BAMBOO_FENCE) || state.is(Blocks.NETHER_BRICK_FENCE) ||
        state.getBlock() instanceof FenceGateBlock;

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

        if (isPlayerNearby()) {
            return false;
        }

        boolean hasDroppedFood = hasDroppedCropsNearby();

        if (hasDroppedFood) {
            return true;
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
        boolean hasDroppedFood = hasDroppedCropsNearby();

        if (hasDroppedFood) {
            moveToAndEatDroppedCrops();
            return;
        }

        if (!this.isReachedTarget()) {
            super.tick();
            return;
        }

        pig.getLookControl().setLookAt(pig.getX(), pig.getY() - 1.0, pig.getZ());

        this.eatingTimer++;

        if (this.eatingTimer >= EATING_DURATION) {
            eatCrop();
            stop();
        } else if (this.eatingTimer % 20 == 0) {
            playEatingEffects();
        }
    }

    private void moveToAndEatDroppedCrops() {
        List<ItemEntity> items = pig.level().getEntitiesOfClass(
            ItemEntity.class,
            pig.getBoundingBox().inflate(SEARCH_RANGE)
        );

        ItemEntity nearestCrop = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            var stack = item.getItem();
            if (stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)) {
                double distance = pig.distanceToSqr(item);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestCrop = item;
                }
            }
        }

        if (nearestCrop != null) {
            if (nearestDistance < 2.5) {
                eatDroppedCrop(nearestCrop);
                stop();
            } else {
                pig.getNavigation().moveTo(nearestCrop, 1.0);
            }
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isEdibleCrop(state) && !isNearFence(level, pos);
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

    private boolean isPlayerNearby() {
        var nearestPlayer = pig.level().getNearestPlayer(pig, PLAYER_DETECTION_RANGE);
        return nearestPlayer != null;
    }

    private boolean isNearFence(LevelReader level, BlockPos pos) {
        int range = FENCE_PROTECTION_RANGE;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);
                    if (IS_FENCE.test(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasDroppedCropsNearby() {
        List<ItemEntity> items = pig.level().getEntitiesOfClass(
            ItemEntity.class,
            pig.getBoundingBox().inflate(SEARCH_RANGE)
        );

        return items.stream().anyMatch(item -> {
            var stack = item.getItem();
            return stack.is(Items.CARROT) ||
                   stack.is(Items.POTATO) ||
                   stack.is(Items.BEETROOT);
        });
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

    private void eatDroppedCrop(ItemEntity itemEntity) {
        var stack = itemEntity.getItem();
        Block cropBlock = getCropBlockFromItem(stack);

        if (!pig.level().isClientSide()) {
            itemEntity.discard();
            feedPig(cropBlock);
            BetterEcology.LOGGER.debug("Pig ate dropped {} at {}", cropBlock, pig.blockPosition());
        }
    }

    private Block getCropBlockFromItem(net.minecraft.world.item.ItemStack stack) {
        if (stack.is(Items.CARROT)) {
            return Blocks.CARROTS;
        } else if (stack.is(Items.POTATO)) {
            return Blocks.POTATOES;
        } else if (stack.is(Items.BEETROOT)) {
            return Blocks.BEETROOTS;
        }
        return Blocks.AIR;
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

    private boolean isTargetBlock(Level level, BlockPos pos) {
        return isValidTarget(level, pos);
    }
}
