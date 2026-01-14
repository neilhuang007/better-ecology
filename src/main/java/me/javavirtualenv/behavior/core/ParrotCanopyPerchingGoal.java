package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes parrots seek and perch on high tree canopy positions.
 * <p>
 * Based on real parrot behavior where parrots prefer high tree canopy positions
 * during the day for safety and visibility. They cluster socially in preferred trees.
 * <p>
 * Behavior:
 * <ul>
 *   <li>Seeks highest available tree leaves/logs</li>
 *   <li>Prefers jungle trees, then oak/birch, then other trees</li>
 *   <li>Minimum height preference of 10+ blocks</li>
 *   <li>Social clustering - prefers trees with other parrots</li>
 *   <li>Stays perched for 2-5 minutes while vocalizing</li>
 * </ul>
 */
public class ParrotCanopyPerchingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParrotCanopyPerchingGoal.class);

    private static final int MIN_HEIGHT = 10;
    private static final int SEARCH_RADIUS = 32;
    private static final int PERCH_DURATION_MIN = 2400;
    private static final int PERCH_DURATION_MAX = 6000;
    private static final int SEARCH_INTERVAL_TICKS = 200;
    private static final double SOCIAL_CLUSTERING_RADIUS = 8.0;
    private static final int VOCALIZATION_INTERVAL_MIN = 300;
    private static final int VOCALIZATION_INTERVAL_MAX = 800;

    private final Parrot parrot;
    private final double speedModifier;

    private BlockPos targetPerchPos;
    private int searchCooldown;
    private int perchTicks;
    private int perchDuration;
    private int ticksUntilNextVocalization;
    private boolean isPerched;

    /**
     * Creates a new ParrotCanopyPerchingGoal.
     *
     * @param parrot the parrot that will seek canopy perches
     * @param speedModifier movement speed when flying to perch
     */
    public ParrotCanopyPerchingGoal(Parrot parrot, double speedModifier) {
        this.parrot = parrot;
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.targetPerchPos = null;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Creates a new ParrotCanopyPerchingGoal with default speed.
     *
     * @param parrot the parrot that will seek canopy perches
     */
    public ParrotCanopyPerchingGoal(Parrot parrot) {
        this(parrot, 1.0);
    }

    @Override
    public boolean canUse() {
        if (this.parrot.isOrderedToSit() || this.parrot.isInWaterOrBubble()) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        BlockPos currentPos = this.parrot.blockPosition();
        if (isValidPerchPosition(currentPos) && currentPos.getY() >= MIN_HEIGHT) {
            this.targetPerchPos = currentPos;
            return true;
        }

        return findCanopyPerch();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPerchPos == null) {
            return false;
        }

        if (this.parrot.isOrderedToSit() || this.parrot.isInWaterOrBubble()) {
            return false;
        }

        if (this.isPerched && this.perchTicks >= this.perchDuration) {
            return false;
        }

        if (!isValidPerchPosition(this.targetPerchPos)) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("{} flying to canopy perch at {}",
                parrot.getName().getString(),
                targetPerchPos);

        this.perchTicks = 0;
        this.perchDuration = PERCH_DURATION_MIN +
                this.parrot.getRandom().nextInt(PERCH_DURATION_MAX - PERCH_DURATION_MIN + 1);
        this.isPerched = false;
        this.ticksUntilNextVocalization = calculateNextVocalizationInterval();

        navigateToPerch();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped perching (perched for {} ticks)",
                parrot.getName().getString(),
                perchTicks);

        this.targetPerchPos = null;
        this.isPerched = false;
        this.perchTicks = 0;
        this.parrot.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetPerchPos == null) {
            return;
        }

        if (!this.isPerched) {
            if (this.parrot.position().closerThan(this.targetPerchPos.getCenter(), 2.0)) {
                this.isPerched = true;
                this.parrot.getNavigation().stop();

                LOGGER.debug("{} reached perch at {} (height: {})",
                        parrot.getName().getString(),
                        targetPerchPos,
                        targetPerchPos.getY());
            }
        } else {
            this.perchTicks++;

            this.parrot.getLookControl().setLookAt(
                    this.targetPerchPos.getX() + 0.5 + (this.parrot.getRandom().nextDouble() - 0.5) * 5,
                    this.targetPerchPos.getY(),
                    this.targetPerchPos.getZ() + 0.5 + (this.parrot.getRandom().nextDouble() - 0.5) * 5
            );

            this.ticksUntilNextVocalization--;
            if (this.ticksUntilNextVocalization <= 0) {
                vocalize();
                this.ticksUntilNextVocalization = calculateNextVocalizationInterval();
            }
        }
    }

    /**
     * Finds the best canopy perch position within search radius.
     *
     * @return true if a suitable perch was found
     */
    private boolean findCanopyPerch() {
        BlockPos parrotPos = this.parrot.blockPosition();
        Level level = this.parrot.level();

        List<ScoredPerch> candidates = new ArrayList<>();

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = MIN_HEIGHT; y <= SEARCH_RADIUS; y++) {
                    BlockPos pos = parrotPos.offset(x, y, z);

                    if (isValidPerchPosition(pos)) {
                        int score = scorePerchPosition(pos);
                        if (score > 0) {
                            candidates.add(new ScoredPerch(pos, score));
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            LOGGER.debug("{} could not find suitable canopy perch within {} blocks",
                    parrot.getName().getString(),
                    SEARCH_RADIUS);
            return false;
        }

        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        this.targetPerchPos = candidates.get(0).pos;

        LOGGER.debug("{} found canopy perch at {} (score: {}, height: {})",
                parrot.getName().getString(),
                targetPerchPos,
                candidates.get(0).score,
                targetPerchPos.getY());

        return true;
    }

    /**
     * Validates if a position is suitable for perching.
     *
     * @param pos the position to check
     * @return true if valid perch position
     */
    private boolean isValidPerchPosition(BlockPos pos) {
        Level level = this.parrot.level();
        BlockState state = level.getBlockState(pos);

        if (state.is(BlockTags.LEAVES)) {
            return true;
        }

        if (state.is(BlockTags.LOGS)) {
            return true;
        }

        return false;
    }

    /**
     * Scores a perch position based on height, tree type, and social factors.
     *
     * @param pos the position to score
     * @return score (higher is better)
     */
    private int scorePerchPosition(BlockPos pos) {
        Level level = this.parrot.level();
        BlockState state = level.getBlockState(pos);
        int score = 0;

        score += pos.getY() * 2;

        if (state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.JUNGLE_LOG)) {
            score += 50;
        } else if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.OAK_LOG)) {
            score += 30;
        } else if (state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.BIRCH_LOG)) {
            score += 20;
        } else if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
            score += 10;
        }

        int nearbyParrots = countNearbyParrotsAtPerch(pos);
        if (nearbyParrots > 0) {
            score += nearbyParrots * 40;
        }

        if (!hasOpenSkyAbove(pos)) {
            score -= 20;
        }

        return score;
    }

    /**
     * Counts how many other parrots are near this perch position.
     *
     * @param pos the perch position
     * @return number of nearby parrots
     */
    private int countNearbyParrotsAtPerch(BlockPos pos) {
        List<Parrot> nearbyParrots = this.parrot.level().getEntitiesOfClass(
                Parrot.class,
                this.parrot.getBoundingBox().inflate(SOCIAL_CLUSTERING_RADIUS).move(
                        pos.getX() - this.parrot.getX(),
                        pos.getY() - this.parrot.getY(),
                        pos.getZ() - this.parrot.getZ()
                ),
                otherParrot -> otherParrot != this.parrot && otherParrot.isAlive()
        );

        return nearbyParrots.size();
    }

    /**
     * Checks if there is open sky above the perch position.
     *
     * @param pos the perch position
     * @return true if has open sky
     */
    private boolean hasOpenSkyAbove(BlockPos pos) {
        Level level = this.parrot.level();
        return level.canSeeSky(pos.above());
    }

    /**
     * Navigates the parrot to the perch position.
     */
    private void navigateToPerch() {
        if (this.targetPerchPos == null) {
            return;
        }

        this.parrot.getNavigation().moveTo(
                this.targetPerchPos.getX() + 0.5,
                this.targetPerchPos.getY() + 0.5,
                this.targetPerchPos.getZ() + 0.5,
                this.speedModifier
        );
    }

    /**
     * Makes the parrot vocalize while perched.
     */
    private void vocalize() {
        if (this.parrot.isSilent()) {
            return;
        }

        float pitch = 0.8F + this.parrot.getRandom().nextFloat() * 0.5F;
        this.parrot.playSound(SoundEvents.PARROT_AMBIENT, 0.5F, pitch);

        LOGGER.debug("{} vocalized while perched at {}",
                parrot.getName().getString(),
                targetPerchPos);
    }

    /**
     * Calculates the interval until the next vocalization.
     *
     * @return ticks until next vocalization
     */
    private int calculateNextVocalizationInterval() {
        return VOCALIZATION_INTERVAL_MIN +
                this.parrot.getRandom().nextInt(VOCALIZATION_INTERVAL_MAX - VOCALIZATION_INTERVAL_MIN + 1);
    }

    /**
     * Helper class to store scored perch positions.
     */
    private static class ScoredPerch {
        final BlockPos pos;
        final int score;

        ScoredPerch(BlockPos pos, int score) {
            this.pos = pos;
            this.score = score;
        }
    }
}
