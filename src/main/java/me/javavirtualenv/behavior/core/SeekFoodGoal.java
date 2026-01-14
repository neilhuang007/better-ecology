package me.javavirtualenv.behavior.core;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal that makes mobs seek and consume food when hungry.
 * Supports two modes:
 * - GRAZER: Finds and eats grass blocks (for herbivores like sheep, cows)
 * - ITEM_SEEKER: Finds and picks up food items from the ground (for omnivores/predators)
 */
public class SeekFoodGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeekFoodGoal.class);

    private static final int EAT_ANIMATION_TICKS = 40;
    private static final int SEARCH_COOLDOWN = 10;  // Reduced for faster activation
    private static final double ITEM_PICKUP_DISTANCE = 1.5;
    private static final double GRASS_EAT_DISTANCE = 1.0;
    private static final int ITEM_EATING_TICKS = 60;  // Time to consume picked up item

    private final Mob mob;
    private final Level level;
    private final double speedModifier;
    private final int searchRadius;
    private final FoodMode mode;
    private final Predicate<ItemStack> foodPredicate;

    private BlockPos targetGrassPos;
    private ItemEntity targetItem;
    private int eatAnimationTick;
    private int searchCooldown;
    private int itemEatingTick;  // Timer for eating picked up item
    private ItemStack heldFoodItem;  // Item being eaten

    /**
     * Defines how the mob seeks food.
     */
    public enum FoodMode {
        /** Find and eat grass blocks */
        GRAZER,
        /** Find and pick up food items */
        ITEM_SEEKER
    }

    /**
     * Creates a new SeekFoodGoal.
     *
     * @param mob The mob that will seek food
     * @param speedModifier Movement speed multiplier when pathfinding to food
     * @param searchRadius Radius to search for food
     * @param mode GRAZER for grass blocks, ITEM_SEEKER for food items
     * @param foodPredicate Predicate to validate food items (only used in ITEM_SEEKER mode)
     */
    public SeekFoodGoal(Mob mob, double speedModifier, int searchRadius, FoodMode mode, Predicate<ItemStack> foodPredicate) {
        this.mob = mob;
        this.level = mob.level();
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.mode = mode;
        this.foodPredicate = foodPredicate;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        float hunger = AnimalNeeds.getHunger(this.mob);
        boolean isHungry = AnimalNeeds.isHungry(this.mob);

        if (!isHungry) {
            LOGGER.debug("{} not hungry (hunger: {}, threshold: {})",
                this.mob.getName().getString(), hunger, AnimalThresholds.HUNGRY);
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            LOGGER.debug("{} on cooldown ({})", this.mob.getName().getString(), this.searchCooldown);
            return false;
        }

        LOGGER.debug("{} is hungry (hunger: {}), seeking food in mode: {}",
            this.mob.getName().getString(), hunger, this.mode);

        if (this.mode == FoodMode.GRAZER) {
            boolean found = findNearestGrass();
            LOGGER.debug("{} findNearestGrass result: {}", this.mob.getName().getString(), found);
            return found;
        } else {
            return findNearestFoodItem();
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (AnimalNeeds.isSatisfied(this.mob)) {
            LOGGER.debug("{} is satisfied, stopping food seeking", this.mob);
            return false;
        }

        // Continue while eating (grass or item)
        if (this.eatAnimationTick > 0 || this.itemEatingTick > 0) {
            return true;
        }

        if (this.mode == FoodMode.GRAZER) {
            return this.targetGrassPos != null && isValidGrassBlock(this.targetGrassPos);
        } else {
            return this.targetItem != null && this.targetItem.isAlive() && isValidFoodItem(this.targetItem);
        }
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting to seek food (mode: {})", this.mob, this.mode);

        if (this.mode == FoodMode.GRAZER && this.targetGrassPos != null) {
            this.mob.getNavigation().moveTo(
                this.targetGrassPos.getX() + 0.5,
                this.targetGrassPos.getY() + 1,
                this.targetGrassPos.getZ() + 0.5,
                this.speedModifier
            );
        } else if (this.mode == FoodMode.ITEM_SEEKER && this.targetItem != null) {
            this.mob.getNavigation().moveTo(this.targetItem, this.speedModifier);
        }

        this.eatAnimationTick = 0;
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopping food seeking", this.mob);
        this.targetGrassPos = null;
        this.targetItem = null;
        this.eatAnimationTick = 0;
        this.itemEatingTick = 0;
        this.searchCooldown = SEARCH_COOLDOWN;
        this.mob.getNavigation().stop();

        // Clear held item if interrupted while eating
        if (this.heldFoodItem != null) {
            this.mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.heldFoodItem = null;
        }
    }

    @Override
    public void tick() {
        // Handle item eating animation (wolf holding meat)
        if (this.itemEatingTick > 0) {
            handleItemEating();
            return;
        }

        if (this.eatAnimationTick > 0) {
            handleEatingAnimation();
            return;
        }

        if (this.mode == FoodMode.GRAZER) {
            tickGrazerMode();
        } else {
            tickItemSeekerMode();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void tickGrazerMode() {
        if (this.targetGrassPos == null) {
            return;
        }

        this.mob.getLookControl().setLookAt(
            this.targetGrassPos.getX() + 0.5,
            this.targetGrassPos.getY() + 0.5,
            this.targetGrassPos.getZ() + 0.5
        );

        if (this.mob.position().distanceToSqr(
            this.targetGrassPos.getX() + 0.5,
            this.targetGrassPos.getY(),
            this.targetGrassPos.getZ() + 0.5
        ) <= GRASS_EAT_DISTANCE * GRASS_EAT_DISTANCE) {
            startEating();
        } else {
            if (this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(
                    this.targetGrassPos.getX() + 0.5,
                    this.targetGrassPos.getY() + 1,
                    this.targetGrassPos.getZ() + 0.5,
                    this.speedModifier
                );
            }
        }
    }

    private void tickItemSeekerMode() {
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return;
        }

        this.mob.getLookControl().setLookAt(this.targetItem, 30.0F, 30.0F);

        double distanceSq = this.mob.distanceToSqr(this.targetItem);

        if (distanceSq <= ITEM_PICKUP_DISTANCE * ITEM_PICKUP_DISTANCE) {
            pickupAndConsumeItem();
        } else {
            if (this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(this.targetItem, this.speedModifier);
            }
        }
    }

    private void startEating() {
        this.eatAnimationTick = this.adjustedTickDelay(EAT_ANIMATION_TICKS);
        this.level.broadcastEntityEvent(this.mob, (byte) 10);
        this.mob.getNavigation().stop();
        LOGGER.debug("{} started eating animation", this.mob);
    }

    private void handleEatingAnimation() {
        this.eatAnimationTick--;

        // Play grazing animation during eating
        if (this.mode == FoodMode.GRAZER && this.targetGrassPos != null) {
            AnimalAnimations.playGrazingAnimation(this.mob, this.targetGrassPos, EAT_ANIMATION_TICKS - this.eatAnimationTick);
        }

        if (this.eatAnimationTick == this.adjustedTickDelay(4)) {
            if (this.mode == FoodMode.GRAZER) {
                consumeGrass();
            }
        }
    }

    private void consumeGrass() {
        if (this.targetGrassPos == null) {
            return;
        }

        BlockState state = this.level.getBlockState(this.targetGrassPos);

        if (state.is(Blocks.SHORT_GRASS)) {
            if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                this.level.destroyBlock(this.targetGrassPos, false);
            }
            restoreHungerFromGrass();
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                this.level.levelEvent(2001, this.targetGrassPos, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                this.level.setBlock(this.targetGrassPos, Blocks.DIRT.defaultBlockState(), 2);
            }
            restoreHungerFromGrass();
        }

        this.targetGrassPos = null;
    }

    private void restoreHungerFromGrass() {
        float restoreAmount = AnimalThresholds.GRASS_HUNGER_RESTORE;
        AnimalNeeds.modifyHunger(this.mob, restoreAmount);
        LOGGER.debug("{} ate grass and restored {} hunger (now: {})",
            this.mob, restoreAmount, AnimalNeeds.getHunger(this.mob));
    }

    private void pickupAndConsumeItem() {
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return;
        }

        // Pick up the item and hold it in mouth
        this.heldFoodItem = this.targetItem.getItem().copy();
        this.mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, this.heldFoodItem);
        this.targetItem.discard();
        this.targetItem = null;

        // Start eating animation
        this.itemEatingTick = this.adjustedTickDelay(ITEM_EATING_TICKS);
        this.mob.getNavigation().stop();

        LOGGER.debug("{} picked up {} and started eating", this.mob, this.heldFoodItem.getItem());
    }

    /**
     * Handles the item eating animation and consumption.
     */
    private void handleItemEating() {
        this.itemEatingTick--;

        // Play eating animation with particles and sounds
        if (this.heldFoodItem != null && !this.heldFoodItem.isEmpty()) {
            AnimalAnimations.playEatingAnimation(this.mob, this.heldFoodItem, ITEM_EATING_TICKS - this.itemEatingTick);
        }

        // Finish eating
        if (this.itemEatingTick <= 0) {
            if (this.heldFoodItem != null) {
                FoodProperties foodProps = this.heldFoodItem.get(DataComponents.FOOD);
                float restoreAmount = 0;
                if (foodProps != null) {
                    restoreAmount = foodProps.nutrition() * AnimalThresholds.FOOD_HUNGER_RESTORE_PER_NUTRITION;
                } else {
                    restoreAmount = AnimalThresholds.FOOD_HUNGER_RESTORE_PER_NUTRITION;
                }

                AnimalNeeds.modifyHunger(this.mob, restoreAmount);

                // Play finish eating sound
                AnimalAnimations.playEatingFinishSound(this.mob);

                LOGGER.debug("{} finished eating {} and restored {} hunger (now: {})",
                    this.mob, this.heldFoodItem.getItem(), restoreAmount, AnimalNeeds.getHunger(this.mob));

                // Remove item from mouth
                this.mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.heldFoodItem = null;
            }
        }
    }

    private boolean findNearestGrass() {
        BlockPos mobPos = this.mob.blockPosition();
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        BlockPos closestGrass = null;
        double closestDistanceSq = Double.MAX_VALUE;
        int blocksChecked = 0;
        int grassFound = 0;

        for (int x = -this.searchRadius; x <= this.searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -this.searchRadius; z <= this.searchRadius; z++) {
                    searchPos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);
                    blocksChecked++;

                    if (isValidGrassBlock(searchPos)) {
                        grassFound++;
                        double distanceSq = mobPos.distSqr(searchPos);
                        if (distanceSq < closestDistanceSq) {
                            closestDistanceSq = distanceSq;
                            closestGrass = searchPos.immutable();
                        }
                    }
                }
            }
        }

        LOGGER.debug("{} searched {} blocks, found {} grass. Mob at {} (y={})",
            this.mob.getName().getString(), blocksChecked, grassFound, mobPos, mobPos.getY());

        if (closestGrass != null) {
            this.targetGrassPos = closestGrass;
            LOGGER.debug("{} found grass at {}", this.mob, closestGrass);
            return true;
        }

        return false;
    }

    private boolean isValidGrassBlock(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);

        if (state.is(Blocks.SHORT_GRASS)) {
            return true;
        }

        if (state.is(Blocks.GRASS_BLOCK)) {
            BlockPos above = pos.above();
            BlockState aboveState = this.level.getBlockState(above);
            return aboveState.isAir() || aboveState.is(Blocks.SHORT_GRASS);
        }

        return false;
    }

    private boolean findNearestFoodItem() {
        AABB searchBox = this.mob.getBoundingBox().inflate(this.searchRadius);
        List<ItemEntity> items = this.level.getEntitiesOfClass(ItemEntity.class, searchBox, this::isValidFoodItem);

        if (items.isEmpty()) {
            return false;
        }

        ItemEntity closest = items.stream()
            .min(Comparator.comparingDouble(item -> this.mob.distanceToSqr(item)))
            .orElse(null);

        if (closest != null) {
            this.targetItem = closest;
            LOGGER.debug("{} found food item: {}", this.mob, closest.getItem().getItem());
            return true;
        }

        return false;
    }

    private boolean isValidFoodItem(ItemEntity itemEntity) {
        if (itemEntity == null || !itemEntity.isAlive()) {
            return false;
        }

        ItemStack stack = itemEntity.getItem();
        return this.foodPredicate.test(stack);
    }
}
