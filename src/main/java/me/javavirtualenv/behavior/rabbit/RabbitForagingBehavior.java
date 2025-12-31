package me.javavirtualenv.behavior.rabbit;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Random;

/**
 * Rabbit foraging behavior with special abilities.
 * <p>
 * Rabbits are specialist foragers that:
 * - Eat crops (carrots, cabbage, wheat)
 * - Stand on hind legs to scout for food/danger
 * - Dig through snow in winter to reach vegetation
 * - Prefer young, tender plant shoots
 * <p>
 * Features:
 * - Crop eating with destruction
 * - Snow digging for winter foraging
 * - Standing on hind legs animation behavior
 * - Efficient foraging patterns
 */
public class RabbitForagingBehavior {

    private final RabbitForagingConfig config;
    private final Random random = new Random();

    // Foraging state
    private boolean isStanding = false;
    private int standTimer = 0;
    private BlockPos targetFood;
    private int forageCooldown = 0;

    // Statistics
    private int cropsEaten = 0;
    private int snowDug = 0;

    public RabbitForagingBehavior(RabbitForagingConfig config) {
        this.config = config;
    }

    public RabbitForagingBehavior() {
        this(RabbitForagingConfig.createDefault());
    }

    /**
     * Updates foraging behavior.
     *
     * @param context Behavior context
     */
    public void tick(BehaviorContext context) {
        Mob entity = context.getEntity();

        // Update cooldowns
        if (forageCooldown > 0) {
            forageCooldown--;
        }

        // Update standing state
        updateStandingState(context);

        // Check for nearby food
        if (forageCooldown == 0) {
            boolean ate = checkAndEatFood(context);

            // If didn't find food outside, try eating from burrow storage
            if (!ate) {
                tryEatFromBurrow(context);
            }
        }

        // Snow digging in winter
        if (isInSnow(context)) {
            tryDigSnow(context);
        }
    }

    /**
     * Attempts to eat food from burrow storage.
     */
    private void tryEatFromBurrow(BehaviorContext context) {
        Mob entity = context.getEntity();
        Level level = context.getLevel();
        BlockPos pos = entity.blockPosition();

        // Find nearest burrow
        BurrowSystem burrowSystem = BurrowSystem.get(level);
        RabbitBurrow burrow = burrowSystem.findNearestBurrow(pos, 16.0);

        if (burrow == null || burrow.getTotalFoodCount() == 0) {
            return; // No burrow or no food
        }

        // Get available food items
        Map<String, Integer> foodCache = burrow.getFoodCache();

        // Try to eat cached food (prefer carrots)
        String[] preferredFoods = {"minecraft:carrot", "minecraft:dandelion",
                                   "minecraft:wheat", "minecraft:potato",
                                   "minecraft:beetroot"};

        for (String food : preferredFoods) {
            if (burrow.hasFood(food)) {
                // Eat the food
                int eaten = burrow.retrieveFood(food, 1);

                if (eaten > 0) {
                    // Play eating sound
                    if (!level.isClientSide()) {
                        ServerLevel serverLevel = (ServerLevel) level;
                        serverLevel.playSound(
                            null,
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            SoundEvents.RABBIT_EAT,
                            SoundSource.NEUTRAL,
                            0.5F,
                            1.0F
                        );
                    }

                    forageCooldown = config.getEatCooldown();
                    return;
                }
            }
        }
    }

    /**
     * Checks for and eats nearby food.
     *
     * @return true if food was eaten
     */
    private boolean checkAndEatFood(BehaviorContext context) {
        Mob entity = context.getEntity();
        BlockPos pos = entity.blockPosition();
        Level level = context.getLevel();

        // Search for food in nearby blocks
        int searchRadius = config.getSearchRadius();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (isFoodBlock(state)) {
                        // Eat the food
                        eatFood(context, checkPos, state);
                        forageCooldown = config.getEatCooldown();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Determines if a block is food for rabbits.
     */
    private boolean isFoodBlock(BlockState state) {
        Block block = state.getBlock();

        // Crops
        if (block instanceof CarrotBlock) {
            return state.getValue(CarrotBlock.AGE) >= 2; // Eat partially grown carrots
        }
        if (block instanceof CropBlock) {
            return state.getValue(((CropBlock) block).getAgeProperty()) >= 1;
        }

        // Vegetables
        if (block == Blocks.CARROTS ||
            block == Blocks.POTATOES ||
            block == Blocks.WHEAT ||
            block == Blocks.BEETROOTS ||
            block == Blocks.MELON_STEM ||
            block == Blocks.PUMPKIN_STEM) {
            return true;
        }

        // Flowers and plants
        if (block instanceof FlowerBlock ||
            block instanceof DoublePlantBlock ||
            block == Blocks.DANDELION ||
            block == Blocks.POPPY ||
            block == Blocks.BLUE_ORCHID ||
            block == Blocks.ALLIUM ||
            block == Blocks.AZURE_BLUET ||
            block == Blocks.RED_TULIP ||
            block == Blocks.ORANGE_TULIP ||
            block == Blocks.WHITE_TULIP ||
            block == Blocks.PINK_TULIP ||
            block == Blocks.OXEYE_DAISY ||
            block == Blocks.CORNFLOWER ||
            block == Blocks.LILY_OF_THE_VALLEY ||
            block == Blocks.SWEET_BERRY_BUSH ||
            block == Blocks.LILY_PAD) {
            return true;
        }

        // Grass
        if (block == Blocks.GRASS ||
            block == Blocks.TALL_GRASS ||
            block == Blocks.FERN ||
            block == Blocks.LARGE_FERN) {
            return true;
        }

        return false;
    }

    /**
     * Eats food at the given position.
     */
    private void eatFood(BehaviorContext context, BlockPos pos, BlockState state) {
        Mob entity = context.getEntity();
        Level level = context.getLevel();

        // Play eating sound
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            serverLevel.playSound(
                null,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                SoundEvents.RABBIT_EAT,
                SoundSource.NEUTRAL,
                0.5F,
                1.0F
            );
        }

        // Determine if rabbit should cache food instead of eating
        if (shouldCacheFood(context)) {
            cacheFood(context, pos, state);
        } else {
            // Eat the food
            if (config.destroysCrops() && shouldDestroyCrop(state)) {
                level.destroyBlock(pos, false); // Don't drop items
            } else if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN) ||
                       state.is(Blocks.FERN)) {
                // Trim tall plants
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        cropsEaten++;
    }

    /**
     * Determines if rabbit should cache food for later.
     * Rabbits cache food in winter or when they find abundant food.
     */
    private boolean shouldCacheFood(BehaviorContext context) {
        Level level = context.getLevel();

        // Cache food in snowy biomes
        BlockPos pos = context.getBlockPos();
        BlockState biomeState = level.getBlockState(pos.below(2));

        // Check if in snow biome (snow blocks below)
        boolean isSnowy = biomeState.is(Blocks.SNOW_BLOCK) ||
                         level.getBlockState(pos).is(Blocks.SNOW);

        // 30% chance to cache food in winter
        if (isSnowy && random.nextDouble() < 0.3) {
            return true;
        }

        return false;
    }

    /**
     * Caches food in the nearest burrow for later consumption.
     */
    private void cacheFood(BehaviorContext context, BlockPos pos, BlockState state) {
        Mob entity = context.getEntity();
        Level level = context.getLevel();

        // Find nearest burrow
        BurrowSystem burrowSystem = BurrowSystem.get(level);
        RabbitBurrow burrow = burrowSystem.findNearestBurrow(pos, 32.0);

        if (burrow == null || !burrow.hasFoodStorageSpace()) {
            // No burrow nearby or full, just eat the food
            if (config.destroysCrops() && shouldDestroyCrop(state)) {
                level.destroyBlock(pos, false);
            }
            return;
        }

        // Determine what food item to cache
        String foodItem = getFoodItemId(state);
        if (foodItem != null) {
            // Store the food
            int cached = burrow.storeFood(foodItem, 1);

            if (cached > 0) {
                // Visual feedback for caching
                if (!level.isClientSide()) {
                    ServerLevel serverLevel = (ServerLevel) level;
                    serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HEART,
                        entity.getX(),
                        entity.getY() + 0.5,
                        entity.getZ(),
                        1,
                        0.0, 0.0, 0.0,
                        0.0
                    );
                }

                // Remove the food block
                level.destroyBlock(pos, false);
            }
        }
    }

    /**
     * Gets the item ID for a food block state.
     */
    private String getFoodItemId(BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.CARROTS) {
            return "minecraft:carrot";
        } else if (block == Blocks.POTATOES) {
            return "minecraft:potato";
        } else if (block == Blocks.WHEAT) {
            return "minecraft:wheat";
        } else if (block == Blocks.BEETROOTS) {
            return "minecraft:beetroot";
        } else if (block == Blocks.DANDELION) {
            return "minecraft:dandelion";
        } else if (block instanceof FlowerBlock) {
            return "minecraft:" + block.builtInRegistryHolder().key().location().getPath();
        }

        return null;
    }

    /**
     * Determines if crop should be fully destroyed.
     */
    private boolean shouldDestroyCrop(BlockState state) {
        Block block = state.getBlock();

        // Always destroy young crops
        if (block instanceof CarrotBlock) {
            int age = state.getValue(CarrotBlock.AGE);
            return age < 7; // Destroy before fully mature
        }

        // Random chance for other crops
        return random.nextDouble() < config.getCropDestructionChance();
    }

    /**
     * Checks if rabbit is in snow and should dig.
     */
    private boolean isInSnow(BehaviorContext context) {
        Mob entity = context.getEntity();
        BlockPos pos = entity.blockPosition();
        BlockState feetState = context.getLevel().getBlockState(pos);

        return feetState.is(Blocks.SNOW);
    }

    /**
     * Attempts to dig through snow to reach vegetation.
     */
    private void tryDigSnow(BehaviorContext context) {
        Mob entity = context.getEntity();
        BlockPos pos = entity.blockPosition();
        Level level = context.getLevel();

        // Check if standing on snow
        BlockState feetState = level.getBlockState(pos);
        if (!feetState.is(Blocks.SNOW)) {
            return;
        }

        // Check cooldown
        if (forageCooldown > 0) {
            return;
        }

        // Chance to dig based on hunger (would use hunger component)
        if (random.nextDouble() > config.getSnowDigChance()) {
            return;
        }

        // Remove snow block
        level.destroyBlock(pos, false);

        // Play dig sound
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            serverLevel.playSound(
                null,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                SoundBlocks.SNOW_PLACE,
                SoundSource.NEUTRAL,
                0.3F,
                1.2F
            );
        }

        // Spawn particles
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            for (int i = 0; i < 5; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                    entity.getX() + offsetX,
                    entity.getY(),
                    entity.getZ() + offsetZ,
                    3,
                    0.0, 0.1, 0.0,
                    0.02
                );
            }
        }

        snowDug++;
        forageCooldown = config.getSnowDigCooldown();
    }

    /**
     * Makes the rabbit stand on hind legs to scout.
     */
    public void tryStand(BehaviorContext context) {
        if (isStanding) {
            return; // Already standing
        }

        // Random chance to stand
        if (random.nextDouble() > config.getStandChance()) {
            return;
        }

        initiateStand();
    }

    /**
     * Initiates standing behavior.
     */
    private void initiateStand() {
        isStanding = true;
        standTimer = config.getStandDuration();
    }

    /**
     * Updates standing state.
     */
    private void updateStandingState(BehaviorContext context) {
        if (!isStanding) {
            return;
        }

        standTimer--;

        if (standTimer <= 0) {
            isStanding = false;
        }
    }

    /**
     * Checks if rabbit is currently standing.
     */
    public boolean isStanding() {
        return isStanding;
    }

    /**
     * Gets the target food position.
     */
    public BlockPos getTargetFood() {
        return targetFood;
    }

    /**
     * Sets target food position.
     */
    public void setTargetFood(BlockPos pos) {
        this.targetFood = pos;
    }

    /**
     * Gets foraging statistics.
     */
    public int getCropsEaten() {
        return cropsEaten;
    }

    public int getSnowDug() {
        return snowDug;
    }

    public RabbitForagingConfig getConfig() {
        return config;
    }

    /**
     * Resets foraging state.
     */
    public void reset() {
        isStanding = false;
        standTimer = 0;
        targetFood = null;
        forageCooldown = 0;
        cropsEaten = 0;
        snowDug = 0;
    }

    // SoundBlocks placeholder
    private static class SoundBlocks {
        private static final net.minecraft.sounds.SoundEvent SNOW_PLACE =
            net.minecraft.sounds.SoundEvents.SNOW_PLACE;
    }
}
