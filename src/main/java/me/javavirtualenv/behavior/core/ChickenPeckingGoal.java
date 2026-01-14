package me.javavirtualenv.behavior.core;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal for chickens to peck at the ground searching for seeds and grubs.
 * Chickens will occasionally stop and peck at grass blocks, with a chance to find food.
 * This behavior is more frequent when the chicken is hungry.
 */
public class ChickenPeckingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChickenPeckingGoal.class);

    private static final int PECKING_ANIMATION_TICKS = 30;
    private static final int PECKING_COOLDOWN_MIN = 100;
    private static final int PECKING_COOLDOWN_MAX = 400;
    private static final int HUNGRY_COOLDOWN_MIN = 60;
    private static final int HUNGRY_COOLDOWN_MAX = 200;
    private static final double FIND_FOOD_CHANCE = 0.3;
    private static final double HUNGRY_FIND_FOOD_CHANCE = 0.5;

    private final Mob chicken;
    private final Level level;

    private int peckingTick;
    private int peckingCooldown;
    private BlockPos peckingPos;

    public ChickenPeckingGoal(Mob chicken) {
        this.chicken = chicken;
        this.level = chicken.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.peckingCooldown > 0) {
            this.peckingCooldown--;
            return false;
        }

        BlockPos groundPos = findPeckableGround();
        if (groundPos == null) {
            return false;
        }

        this.peckingPos = groundPos;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.peckingTick > 0;
    }

    @Override
    public void start() {
        this.peckingTick = this.adjustedTickDelay(PECKING_ANIMATION_TICKS);
        this.chicken.getNavigation().stop();

        LOGGER.debug("{} started pecking at {}", this.chicken.getName().getString(), this.peckingPos);
    }

    @Override
    public void stop() {
        this.peckingTick = 0;
        this.peckingPos = null;

        boolean isHungry = AnimalNeeds.isHungry(this.chicken);
        if (isHungry) {
            this.peckingCooldown = this.chicken.getRandom().nextInt(
                HUNGRY_COOLDOWN_MAX - HUNGRY_COOLDOWN_MIN
            ) + HUNGRY_COOLDOWN_MIN;
        } else {
            this.peckingCooldown = this.chicken.getRandom().nextInt(
                PECKING_COOLDOWN_MAX - PECKING_COOLDOWN_MIN
            ) + PECKING_COOLDOWN_MIN;
        }

        LOGGER.debug("{} stopped pecking, cooldown: {}", this.chicken.getName().getString(), this.peckingCooldown);
    }

    @Override
    public void tick() {
        if (this.peckingPos == null) {
            return;
        }

        this.chicken.getLookControl().setLookAt(
            this.peckingPos.getX() + 0.5,
            this.peckingPos.getY(),
            this.peckingPos.getZ() + 0.5
        );

        this.peckingTick--;

        if (this.peckingTick == this.adjustedTickDelay(15)) {
            tryFindFood();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private BlockPos findPeckableGround() {
        BlockPos chickenPos = this.chicken.blockPosition();
        BlockPos groundPos = chickenPos.below();

        BlockState groundState = this.level.getBlockState(groundPos);

        if (groundState.is(Blocks.GRASS_BLOCK) ||
            groundState.is(Blocks.DIRT) ||
            groundState.is(Blocks.COARSE_DIRT) ||
            groundState.is(Blocks.FARMLAND)) {

            BlockState aboveGround = this.level.getBlockState(chickenPos);
            if (aboveGround.isAir() || aboveGround.is(Blocks.SHORT_GRASS)) {
                return groundPos;
            }
        }

        return null;
    }

    private void tryFindFood() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }

        boolean isHungry = AnimalNeeds.isHungry(this.chicken);
        double findChance = isHungry ? HUNGRY_FIND_FOOD_CHANCE : FIND_FOOD_CHANCE;

        if (this.chicken.getRandom().nextDouble() < findChance) {
            ItemStack foundFood = determineFoodFound();

            if (!foundFood.isEmpty()) {
                this.level.broadcastEntityEvent(this.chicken, (byte) 10);

                float hungerRestore = calculateHungerRestore(foundFood);
                AnimalNeeds.modifyHunger(this.chicken, hungerRestore);

                LOGGER.debug("{} found {} while pecking, restored {} hunger (now: {})",
                    this.chicken.getName().getString(),
                    foundFood.getItem(),
                    hungerRestore,
                    AnimalNeeds.getHunger(this.chicken)
                );
            }
        }
    }

    private ItemStack determineFoodFound() {
        double roll = this.chicken.getRandom().nextDouble();

        if (roll < 0.4) {
            return new ItemStack(Items.WHEAT_SEEDS);
        } else if (roll < 0.7) {
            return new ItemStack(Items.BEETROOT_SEEDS);
        } else if (roll < 0.85) {
            return new ItemStack(Items.MELON_SEEDS);
        } else {
            return new ItemStack(Items.PUMPKIN_SEEDS);
        }
    }

    private float calculateHungerRestore(ItemStack foodItem) {
        return AnimalThresholds.FOOD_HUNGER_RESTORE_PER_NUTRITION * 0.5f;
    }
}
