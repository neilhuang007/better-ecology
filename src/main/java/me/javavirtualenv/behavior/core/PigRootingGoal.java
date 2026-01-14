package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes pigs root in dirt and grass to find food.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when {@link AnimalNeeds#isHungry(Mob)} returns true</li>
 *   <li>Searches for suitable rooting blocks (grass, mycelium, podzol) within range</li>
 *   <li>Pathfinds to the block and performs rooting animation</li>
 *   <li>Has a chance to convert grass to dirt and restore hunger</li>
 *   <li>Shows particle effects and plays sounds during rooting</li>
 * </ul>
 *
 * <p>Based on real pig rooting behavior where they use their snouts to dig for food.
 */
public class PigRootingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(PigRootingGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 10;
    private static final int ROOTING_DURATION = 30;
    private static final int GIVE_UP_TICKS = 600;
    private static final double ACCEPTED_DISTANCE = 2.0;
    private static final float GRASS_CONVERSION_CHANCE = 0.6f;
    private static final float ROOTING_HUNGER_RESTORE = 8f;

    private final Mob mob;
    private final double speedModifier;
    private final int searchRadius;

    private BlockPos targetRootPos;
    private int searchCooldown;
    private int tryTicks;
    private int rootingTicks;
    private boolean isRooting;

    /**
     * Creates a new PigRootingGoal.
     *
     * @param mob the pig that will root
     * @param speedModifier movement speed multiplier when moving to rooting spot
     * @param searchRadius horizontal search radius for suitable blocks
     */
    public PigRootingGoal(Mob mob, double speedModifier, int searchRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.searchCooldown = 0;
        this.targetRootPos = null;
        this.rootingTicks = 0;
        this.isRooting = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AnimalNeeds.isHungry(this.mob)) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        return findNearestRootableBlock();
    }

    @Override
    public boolean canContinueToUse() {
        if (AnimalNeeds.isSatisfied(this.mob)) {
            LOGGER.debug("Mob {} is now satisfied, stopping rooting", this.mob.getName().getString());
            return false;
        }

        if (this.tryTicks >= GIVE_UP_TICKS) {
            LOGGER.debug("Mob {} gave up rooting after {} ticks", this.mob.getName().getString(), this.tryTicks);
            return false;
        }

        if (this.targetRootPos == null || !isRootableBlock(this.targetRootPos)) {
            LOGGER.debug("Block at {} is no longer rootable for mob {}", this.targetRootPos, this.mob.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("Mob {} starting to root at {}", this.mob.getName().getString(), this.targetRootPos);
        this.tryTicks = 0;
        this.rootingTicks = 0;
        this.isRooting = false;
        navigateToRootBlock();
    }

    @Override
    public void stop() {
        LOGGER.debug("Mob {} stopped rooting. Hunger: {}", this.mob.getName().getString(), AnimalNeeds.getHunger(this.mob));
        this.targetRootPos = null;
        this.isRooting = false;
        this.rootingTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.tryTicks++;

        if (isNearRootBlock()) {
            this.isRooting = true;
            this.mob.getNavigation().stop();

            performRooting();
        } else {
            this.isRooting = false;
            this.rootingTicks = 0;

            if (shouldRecalculatePath()) {
                navigateToRootBlock();
            }
        }
    }

    /**
     * Finds the nearest rootable block within search radius.
     *
     * @return true if a rootable block was found, false otherwise
     */
    private boolean findNearestRootableBlock() {
        BlockPos mobPos = this.mob.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        BlockPos closestBlock = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int y = -1; y <= 1; y++) {
            for (int x = -this.searchRadius; x <= this.searchRadius; x++) {
                for (int z = -this.searchRadius; z <= this.searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);

                    if (isRootableBlock(mutablePos)) {
                        double distSq = mobPos.distSqr(mutablePos);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closestBlock = mutablePos.immutable();
                        }
                    }
                }
            }
        }

        if (closestBlock != null) {
            this.targetRootPos = closestBlock;
            LOGGER.debug("Mob {} found rootable block at {} (distance: {})",
                this.mob.getName().getString(), closestBlock, Math.sqrt(closestDistSq));
            return true;
        }

        LOGGER.debug("Mob {} could not find rootable blocks within {} blocks",
            this.mob.getName().getString(), this.searchRadius);
        return false;
    }

    /**
     * Checks if a block position contains a rootable block.
     *
     * @param pos the position to check
     * @return true if the position contains grass, mycelium, or podzol
     */
    private boolean isRootableBlock(BlockPos pos) {
        Level level = this.mob.level();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        return block == Blocks.GRASS_BLOCK ||
               block == Blocks.MYCELIUM ||
               block == Blocks.PODZOL ||
               block == Blocks.DIRT;
    }

    /**
     * Checks if the mob is near enough to the rooting block.
     *
     * @return true if the mob is within rooting distance
     */
    private boolean isNearRootBlock() {
        if (this.targetRootPos == null) {
            return false;
        }

        return this.mob.position().closerThan(this.targetRootPos.getCenter(), ACCEPTED_DISTANCE);
    }

    /**
     * Navigates the mob to the rooting block.
     */
    private void navigateToRootBlock() {
        if (this.targetRootPos == null) {
            return;
        }

        this.mob.getNavigation().moveTo(
            this.targetRootPos.getX() + 0.5,
            this.targetRootPos.getY(),
            this.targetRootPos.getZ() + 0.5,
            this.speedModifier
        );
    }

    /**
     * Handles the rooting behavior and effects.
     */
    private void performRooting() {
        this.rootingTicks++;

        this.mob.getLookControl().setLookAt(
            this.targetRootPos.getX() + 0.5,
            this.targetRootPos.getY() + 0.5,
            this.targetRootPos.getZ() + 0.5
        );

        if (this.rootingTicks % 10 == 0) {
            spawnRootingParticles();
            playRootingSound();
        }

        if (this.rootingTicks >= ROOTING_DURATION) {
            finishRooting();
        }
    }

    /**
     * Spawns dirt particles to simulate rooting.
     */
    private void spawnRootingParticles() {
        if (this.mob.level() instanceof ServerLevel serverLevel) {
            BlockState state = serverLevel.getBlockState(this.targetRootPos);

            serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                this.targetRootPos.getX() + 0.5,
                this.targetRootPos.getY() + 1.0,
                this.targetRootPos.getZ() + 0.5,
                5,
                0.3, 0.1, 0.3,
                0.1
            );
        }
    }

    /**
     * Plays ambient pig sound during rooting.
     */
    private void playRootingSound() {
        this.mob.playSound(SoundEvents.PIG_AMBIENT, 0.6f, 1.0f);
    }

    /**
     * Completes the rooting action, potentially converting grass to dirt and restoring hunger.
     */
    private void finishRooting() {
        Level level = this.mob.level();
        BlockState state = level.getBlockState(this.targetRootPos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK && this.mob.getRandom().nextFloat() < GRASS_CONVERSION_CHANCE) {
            level.setBlock(this.targetRootPos, Blocks.DIRT.defaultBlockState(), 3);
            LOGGER.debug("Pig converted grass to dirt at {}", this.targetRootPos);
        }

        float previousHunger = AnimalNeeds.getHunger(this.mob);
        AnimalNeeds.modifyHunger(this.mob, ROOTING_HUNGER_RESTORE);
        float newHunger = AnimalNeeds.getHunger(this.mob);

        LOGGER.debug("{} finished rooting and restored hunger: {} -> {} (+{})",
            this.mob.getName().getString(),
            String.format("%.1f", previousHunger),
            String.format("%.1f", newHunger),
            String.format("%.1f", ROOTING_HUNGER_RESTORE));

        this.rootingTicks = 0;
        this.targetRootPos = null;
    }

    /**
     * Determines if the path should be recalculated.
     *
     * @return true if path recalculation is needed
     */
    private boolean shouldRecalculatePath() {
        return this.tryTicks % 40 == 0;
    }
}
