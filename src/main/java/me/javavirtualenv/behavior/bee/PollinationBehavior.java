package me.javavirtualenv.behavior.bee;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.ecology.spatial.BlockSpatialCache;
import me.javavirtualenv.mixin.animal.BeeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Behavior for bee pollination of flowers and crops.
 * <p>
 * This behavior:
 * - Guides bees to nearby flowers and crops
 * - Accelerates crop growth when pollinated
 * - Creates pollen particles on the bee
 * - Remembers good flower patches
 * - Interacts with ecosystem services (pollinating crops other animals helped grow)
 */
public class PollinationBehavior extends SteeringBehavior {

    private static final double FLOWER_SEARCH_RADIUS = 22.0;
    private static final double CROP_SEARCH_RADIUS = 16.0;
    private static final double POLLINATION_RANGE = 2.0;
    private static final int MAX_FLOWER_MEMORY = 10;
    private static final int FLOWER_MEMORY_DURATION = 6000;

    private final List<BlockPos> flowerMemory;
    private int pollinationCooldown;
    private boolean hasNectar;
    private BlockPos currentTarget;

    public PollinationBehavior() {
        this(1.0);
    }

    public PollinationBehavior(double weight) {
        this.flowerMemory = new ArrayList<>();
        setWeight(weight);
        this.pollinationCooldown = 0;
        this.hasNectar = false;
        this.currentTarget = null;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Bee bee)) {
            return new Vec3d();
        }

        Level level = context.getWorld();
        Vec3d position = context.getPosition();

        // Update cooldown
        if (pollinationCooldown > 0) {
            pollinationCooldown--;
        }

        // Check if bee has nectar
        hasNectar = bee.hasNectar();

        // If bee has nectar, return to hive instead of pollinating
        if (hasNectar) {
            currentTarget = null;
            return new Vec3d();
        }

        // Find flowers or crops to pollinate
        BlockPos target = findPollinationTarget(level, position, bee);

        if (target == null) {
            currentTarget = null;
            return new Vec3d();
        }

        // Check if close enough to pollinate
        Vec3d targetPos = new Vec3d(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        double distance = position.distanceTo(targetPos);

        if (distance <= POLLINATION_RANGE && pollinationCooldown == 0) {
            // Attempt pollination
            if (attemptPollination(level, target, bee)) {
                pollinationCooldown = 40; // 2 second cooldown
                addFlowerToMemory(target);
            }
        }

        currentTarget = target;

        // Calculate steering force toward target
        Vec3d desired = Vec3d.sub(targetPos, position);
        desired.normalize();
        desired.mult(context.getMaxSpeed());

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        steer = limitForce(steer, context.getMaxForce());

        return steer;
    }

    /**
     * Finds the best pollination target (flowers or crops) near the bee.
     * Uses optimized chunk-based search with caching to avoid O(n³) performance.
     */
    private BlockPos findPollinationTarget(Level level, Vec3d position, Bee bee) {
        BlockPos beePos = new BlockPos((int) position.x, (int) position.y, (int) position.z);

        // First check flower memory for known good locations
        for (BlockPos memoryPos : flowerMemory) {
            if (level.isLoaded(memoryPos) && isValidPollinationTarget(level, memoryPos)) {
                double distance = Vec3d.dist(position, new Vec3d(
                    memoryPos.getX() + 0.5,
                    memoryPos.getY(),
                    memoryPos.getZ() + 0.5
                ));
                if (distance <= FLOWER_SEARCH_RADIUS) {
                    return memoryPos;
                }
            }
        }

        // Use optimized cache-based search for flowers and crops
        // This reduces from O(22³) = 10,648 block checks to O(chunks) ~ 64 chunks
        List<Block> targetBlocks = getPollinationTargetBlocks(level);
        List<BlockPos> nearbyTargets = BlockSpatialCache.findBlocksOfType(
                level,
                beePos,
                (int) FLOWER_SEARCH_RADIUS,
                targetBlocks
        );

        // Find closest target
        BlockPos closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos testPos : nearbyTargets) {
            if (isValidPollinationTarget(level, testPos)) {
                Vec3d testPosVec = new Vec3d(
                    testPos.getX() + 0.5,
                    testPos.getY(),
                    testPos.getZ() + 0.5
                );
                double distance = position.distanceTo(testPosVec);
                if (distance < closestDistance && distance <= FLOWER_SEARCH_RADIUS) {
                    closestDistance = distance;
                    closestTarget = testPos;
                }
            }
        }

        return closestTarget;
    }

    /**
     * Get list of blocks that are valid pollination targets.
     * Used for cache-based block searching.
     */
    private List<Block> getPollinationTargetBlocks(Level level) {
        // Return common flowers and crops
        // The cache will filter to actual valid targets during search
        List<Block> targets = new ArrayList<>();

        // Add common flowers
        targets.add(Blocks.DANDELION);
        targets.add(Blocks.POPPY);
        targets.add(Blocks.BLUE_ORCHID);
        targets.add(Blocks.ALLIUM);
        targets.add(Blocks.AZURE_BLUET);
        targets.add(Blocks.RED_TULIP);
        targets.add(Blocks.ORANGE_TULIP);
        targets.add(Blocks.WHITE_TULIP);
        targets.add(Blocks.PINK_TULIP);
        targets.add(Blocks.OXEYE_DAISY);
        targets.add(Blocks.CORNFLOWER);
        targets.add(Blocks.LILY_OF_THE_VALLEY);
        targets.add(Blocks.SUNFLOWER);
        targets.add(Blocks.LILAC);
        targets.add(Blocks.PEONY);
        targets.add(Blocks.ROSE_BUSH);

        // Add crops
        targets.add(Blocks.WHEAT);
        targets.add(Blocks.CARROTS);
        targets.add(Blocks.POTATOES);
        targets.add(Blocks.BEETROOTS);
        targets.add(Blocks.PUMPKIN_STEM);
        targets.add(Blocks.MELON_STEM);

        return targets;
    }

    /**
     * Checks if a block position is a valid pollination target.
     */
    private boolean isValidPollinationTarget(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Check if it's a flower
        if (block instanceof FlowerBlock) {
            return true;
        }

        // Check if it's a crop that can be pollinated
        if (block instanceof CropBlock cropBlock) {
            // Only pollinate crops that aren't fully grown
            return !cropBlock.isMaxAge(state);
        }

        // Check for flower tags
        if (state.is(BlockTags.FLOWERS)) {
            return true;
        }

        return false;
    }

    /**
     * Attempts to pollinate a flower or crop at the given position.
     */
    private boolean attemptPollination(Level level, BlockPos pos, Bee bee) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Spawn pollen particles
        spawnPollenParticles(level, pos);

        if (block instanceof CropBlock cropBlock) {
            // Accelerate crop growth
            IntegerProperty ageProperty = CropBlock.AGE;
            int currentAge = state.getValue(ageProperty);
            int maxAge = cropBlock.getMaxAge();

            if (currentAge < maxAge) {
                // Apply growth boost (1-2 stages)
                int growthBoost = level.random.nextBoolean() ? 2 : 1;
                int newAge = Math.min(currentAge + growthBoost, maxAge);
                level.setBlock(pos, state.setValue(ageProperty, newAge), 3);
                return true;
            }
        } else if (block instanceof FlowerBlock || state.is(BlockTags.FLOWERS)) {
            // Pollinating flower gives nectar
            ((BeeAccessor) bee).invokeSetHasNectar(true);
            return true;
        }

        return false;
    }

    /**
     * Spawns pollen particles around a pollinated block.
     */
    private void spawnPollenParticles(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            double xOffset = level.random.nextDouble() * 0.5 - 0.25;
            double yOffset = level.random.nextDouble() * 0.5;
            double zOffset = level.random.nextDouble() * 0.5 - 0.25;

            double posX = pos.getX() + 0.5 + xOffset;
            double posY = pos.getY() + 0.5 + yOffset;
            double posZ = pos.getZ() + 0.5 + zOffset;

            // Pollen particle color (yellow from dandelion)
            BlockParticleOption particleOption = new BlockParticleOption(
                ParticleTypes.BLOCK,
                Blocks.DANDELION.defaultBlockState()
            );
            level.addParticle(
                particleOption,
                posX, posY, posZ,
                0.0, 0.05, 0.0
            );
        }
    }

    /**
     * Adds a flower position to memory.
     */
    private void addFlowerToMemory(BlockPos pos) {
        if (flowerMemory.size() >= MAX_FLOWER_MEMORY) {
            flowerMemory.remove(0); // Remove oldest
        }
        flowerMemory.add(pos.immutable());
    }

    /**
     * Removes expired flower memories.
     */
    public void cleanFlowerMemory() {
        // Memory cleanup is handled by periodic checks
        if (flowerMemory.size() > MAX_FLOWER_MEMORY) {
            flowerMemory.subList(0, flowerMemory.size() - MAX_FLOWER_MEMORY).clear();
        }
    }

    public boolean hasNectar() {
        return hasNectar;
    }

    public BlockPos getCurrentTarget() {
        return currentTarget;
    }

    public List<BlockPos> getFlowerMemory() {
        return new ArrayList<>(flowerMemory);
    }

    public void setHasNectar(boolean hasNectar) {
        this.hasNectar = hasNectar;
    }
}
