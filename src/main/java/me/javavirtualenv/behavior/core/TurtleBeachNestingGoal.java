package me.javavirtualenv.behavior.core;

import me.javavirtualenv.mixin.animal.TurtleAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that implements natal philopatry for turtles - returning to their birth beach to lay eggs.
 *
 * <p>Behavior:
 * <ul>
 *   <li>When ready to breed, pregnant turtles return to home beach</li>
 *   <li>Digs nest above high tide line (on sand, not in water)</li>
 *   <li>Nesting happens at night for predator avoidance</li>
 *   <li>Digging animation with particle effects</li>
 * </ul>
 *
 * <p>Scientific basis: Sea turtles exhibit natal philopatry - they return to their birth beach to lay eggs.
 * This complements vanilla's TurtleGoToHomeGoal by adding ecological constraints.
 */
public class TurtleBeachNestingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurtleBeachNestingGoal.class);

    private static final int HOME_BEACH_RADIUS = 32;
    private static final int MIN_DIGGING_DURATION = 60; // 3 seconds
    private static final int MAX_DIGGING_DURATION = 120; // 6 seconds
    private static final int NIGHT_START = 12000;
    private static final int NIGHT_END = 23000;
    private static final int SEARCH_INTERVAL_TICKS = 100;
    private static final int GIVE_UP_TICKS = 2400;
    private static final double ACCEPTED_DISTANCE = 2.0;

    private final Turtle turtle;
    private final double speedModifier;

    private BlockPos nestingSpot;
    private int searchCooldown;
    private int tryTicks;
    private int diggingTicks;
    private int diggingDuration;
    private boolean isDigging;

    /**
     * Creates a new TurtleBeachNestingGoal.
     *
     * @param turtle the turtle entity
     * @param speedModifier movement speed when traveling to beach
     */
    public TurtleBeachNestingGoal(Turtle turtle, double speedModifier) {
        this.turtle = turtle;
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.turtle.isBaby()) {
            return false;
        }

        if (!this.turtle.hasEgg()) {
            return false;
        }

        if (!isNightTime()) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        return findNestingSpot();
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.turtle.hasEgg() && !this.isDigging) {
            return false;
        }

        if (!isNightTime() && !this.isDigging) {
            return false;
        }

        if (this.tryTicks >= GIVE_UP_TICKS) {
            LOGGER.debug("Turtle {} gave up nesting after {} ticks", this.turtle.getName().getString(), this.tryTicks);
            return false;
        }

        if (this.nestingSpot == null) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("Turtle {} starting nesting behavior at {}", this.turtle.getName().getString(), this.nestingSpot);
        this.tryTicks = 0;
        this.diggingTicks = 0;
        this.isDigging = false;
        this.diggingDuration = MIN_DIGGING_DURATION + this.turtle.getRandom().nextInt(MAX_DIGGING_DURATION - MIN_DIGGING_DURATION);
        navigateToNestingSpot();
    }

    @Override
    public void stop() {
        LOGGER.debug("Turtle {} stopped nesting behavior", this.turtle.getName().getString());
        this.nestingSpot = null;
        this.isDigging = false;
        this.diggingTicks = 0;
        this.turtle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.tryTicks++;

        if (this.isDigging) {
            performDigging();
        } else if (isNearNestingSpot()) {
            startDigging();
        } else if (shouldRecalculatePath()) {
            navigateToNestingSpot();
        }
    }

    /**
     * Checks if it is currently night time.
     *
     * @return true if it is night
     */
    private boolean isNightTime() {
        long dayTime = this.turtle.level().getDayTime() % 24000;
        return dayTime >= NIGHT_START && dayTime <= NIGHT_END;
    }

    /**
     * Finds a suitable nesting spot near the home beach.
     *
     * @return true if a nesting spot was found
     */
    private boolean findNestingSpot() {
        BlockPos homeBeach = ((TurtleAccessor) this.turtle).invokeGetHomePos();
        if (homeBeach == null || homeBeach.equals(BlockPos.ZERO)) {
            homeBeach = this.turtle.blockPosition();
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Level level = this.turtle.level();

        BlockPos bestSpot = null;
        double bestScore = -1;

        for (int x = -HOME_BEACH_RADIUS; x <= HOME_BEACH_RADIUS; x++) {
            for (int z = -HOME_BEACH_RADIUS; z <= HOME_BEACH_RADIUS; z++) {
                for (int y = -5; y <= 5; y++) {
                    mutablePos.set(homeBeach.getX() + x, homeBeach.getY() + y, homeBeach.getZ() + z);

                    if (isValidNestingSpot(mutablePos)) {
                        double score = calculateNestingSpotScore(mutablePos, homeBeach);
                        if (score > bestScore) {
                            bestScore = score;
                            bestSpot = mutablePos.immutable();
                        }
                    }
                }
            }
        }

        if (bestSpot != null) {
            this.nestingSpot = bestSpot;
            LOGGER.debug("Turtle {} found nesting spot at {} (score: {})",
                this.turtle.getName().getString(), bestSpot, bestScore);
            return true;
        }

        LOGGER.debug("Turtle {} could not find valid nesting spot near home beach",
            this.turtle.getName().getString());
        return false;
    }

    /**
     * Checks if a position is valid for nesting.
     *
     * @param pos the position to check
     * @return true if valid for nesting
     */
    private boolean isValidNestingSpot(BlockPos pos) {
        Level level = this.turtle.level();
        BlockState groundState = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());

        if (!groundState.is(BlockTags.SAND)) {
            return false;
        }

        if (!aboveState.isAir()) {
            return false;
        }

        if (level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Calculates a score for a nesting spot based on distance and suitability.
     *
     * @param pos the position to score
     * @param homeBeach the home beach position
     * @return score (higher is better)
     */
    private double calculateNestingSpotScore(BlockPos pos, BlockPos homeBeach) {
        double distToHome = homeBeach.distSqr(pos);
        double distToTurtle = this.turtle.blockPosition().distSqr(pos);

        double proximityScore = 100.0 / (1.0 + distToHome / 100.0);
        double accessibilityScore = 100.0 / (1.0 + distToTurtle / 50.0);

        return proximityScore + accessibilityScore;
    }

    /**
     * Checks if the turtle is near the nesting spot.
     *
     * @return true if within nesting distance
     */
    private boolean isNearNestingSpot() {
        if (this.nestingSpot == null) {
            return false;
        }

        return this.turtle.position().closerThan(this.nestingSpot.getCenter(), ACCEPTED_DISTANCE);
    }

    /**
     * Navigates the turtle to the nesting spot.
     */
    private void navigateToNestingSpot() {
        if (this.nestingSpot == null) {
            return;
        }

        this.turtle.getNavigation().moveTo(
            this.nestingSpot.getX() + 0.5,
            this.nestingSpot.getY(),
            this.nestingSpot.getZ() + 0.5,
            this.speedModifier
        );
    }

    /**
     * Starts the digging behavior.
     */
    private void startDigging() {
        this.isDigging = true;
        this.diggingTicks = 0;
        this.turtle.getNavigation().stop();
        LOGGER.debug("Turtle {} started digging nest", this.turtle.getName().getString());
    }

    /**
     * Performs the digging behavior with animations.
     */
    private void performDigging() {
        this.diggingTicks++;

        if (this.nestingSpot == null) {
            return;
        }

        this.turtle.getLookControl().setLookAt(
            this.nestingSpot.getX() + 0.5,
            this.nestingSpot.getY(),
            this.nestingSpot.getZ() + 0.5
        );

        playDiggingAnimation();

        if (this.diggingTicks >= this.diggingDuration) {
            finishNesting();
        }
    }

    /**
     * Plays the digging animation with particles and sounds.
     */
    private void playDiggingAnimation() {
        if (this.nestingSpot == null) {
            return;
        }

        Level level = this.turtle.level();

        if (this.diggingTicks % 8 == 0) {
            level.playSound(
                null,
                this.nestingSpot,
                SoundEvents.SAND_BREAK,
                SoundSource.BLOCKS,
                0.5F,
                0.8F + this.turtle.getRandom().nextFloat() * 0.4F
            );
        }

        if (this.diggingTicks % 4 == 0 && level instanceof ServerLevel serverLevel) {
            BlockState sandState = level.getBlockState(this.nestingSpot);
            BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, sandState);

            for (int i = 0; i < 5; i++) {
                double offsetX = (this.turtle.getRandom().nextDouble() - 0.5) * 0.5;
                double offsetZ = (this.turtle.getRandom().nextDouble() - 0.5) * 0.5;

                serverLevel.sendParticles(
                    particleOption,
                    this.nestingSpot.getX() + 0.5 + offsetX,
                    this.nestingSpot.getY() + 1.0,
                    this.nestingSpot.getZ() + 0.5 + offsetZ,
                    1,
                    0, 0.1, 0,
                    0.1
                );
            }
        }
    }

    /**
     * Finishes the nesting behavior.
     * Note: Actual egg laying is handled by vanilla Turtle.TurtleLayEggGoal.
     * This goal provides the ecological constraints (night time, beach location).
     */
    private void finishNesting() {
        LOGGER.debug("Turtle {} finished digging nest", this.turtle.getName().getString());
        this.isDigging = false;
    }

    /**
     * Determines if the path should be recalculated.
     *
     * @return true if recalculation is needed
     */
    private boolean shouldRecalculatePath() {
        return this.tryTicks % 40 == 0;
    }
}
