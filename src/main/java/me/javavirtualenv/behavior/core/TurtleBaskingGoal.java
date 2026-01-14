package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes turtles seek sunny beach areas for basking (thermoregulation).
 *
 * <p>Behavior:
 * <ul>
 *   <li>Turtles seek sunny beach/rock areas during day</li>
 *   <li>Become lethargic in cold water</li>
 *   <li>Basking duration: 2-5 minutes</li>
 *   <li>Requires sunlight level > 12</li>
 * </ul>
 *
 * <p>Scientific basis: Sea turtles are ectothermic and require basking to regulate body temperature.
 * Basking helps with digestion, immune function, and vitamin D synthesis.
 */
public class TurtleBaskingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurtleBaskingGoal.class);

    private static final int MIN_BASKING_DURATION = 2400; // 2 minutes
    private static final int MAX_BASKING_DURATION = 6000; // 5 minutes
    private static final int MIN_SUNLIGHT_LEVEL = 12;
    private static final int DAY_START = 0;
    private static final int DAY_END = 12000;
    private static final int SEARCH_INTERVAL_TICKS = 80;
    private static final int SEARCH_RADIUS = 16;
    private static final int VERTICAL_SEARCH_RANGE = 4;
    private static final int GIVE_UP_TICKS = 1600;
    private static final double ACCEPTED_DISTANCE = 1.5;

    private final Turtle turtle;
    private final double speedModifier;

    private BlockPos baskingSpot;
    private int searchCooldown;
    private int tryTicks;
    private int baskingTicks;
    private int baskingDuration;
    private boolean isBasking;

    /**
     * Creates a new TurtleBaskingGoal.
     *
     * @param turtle the turtle entity
     * @param speedModifier movement speed when moving to basking spot
     */
    public TurtleBaskingGoal(Turtle turtle, double speedModifier) {
        this.turtle = turtle;
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.turtle.isBaby()) {
            return false;
        }

        if (!isDayTime()) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        if (isAlreadyInGoodBaskingSpot()) {
            return false;
        }

        return findBaskingSpot();
    }

    @Override
    public boolean canContinueToUse() {
        if (!isDayTime() && !this.isBasking) {
            return false;
        }

        if (this.isBasking && this.baskingTicks >= this.baskingDuration) {
            LOGGER.debug("Turtle {} finished basking after {} ticks", this.turtle.getName().getString(), this.baskingTicks);
            return false;
        }

        if (this.tryTicks >= GIVE_UP_TICKS && !this.isBasking) {
            LOGGER.debug("Turtle {} gave up finding basking spot after {} ticks", this.turtle.getName().getString(), this.tryTicks);
            return false;
        }

        if (this.baskingSpot == null) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("Turtle {} starting basking behavior at {}", this.turtle.getName().getString(), this.baskingSpot);
        this.tryTicks = 0;
        this.baskingTicks = 0;
        this.isBasking = false;
        this.baskingDuration = MIN_BASKING_DURATION + this.turtle.getRandom().nextInt(MAX_BASKING_DURATION - MIN_BASKING_DURATION);
        navigateToBaskingSpot();
    }

    @Override
    public void stop() {
        LOGGER.debug("Turtle {} stopped basking behavior (basked for {} ticks)",
            this.turtle.getName().getString(), this.baskingTicks);
        this.baskingSpot = null;
        this.isBasking = false;
        this.baskingTicks = 0;
        this.turtle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.tryTicks++;

        if (this.isBasking) {
            performBasking();
        } else if (isNearBaskingSpot()) {
            startBasking();
        } else if (shouldRecalculatePath()) {
            navigateToBaskingSpot();
        }
    }

    /**
     * Checks if it is currently daytime.
     *
     * @return true if it is day
     */
    private boolean isDayTime() {
        long dayTime = this.turtle.level().getDayTime() % 24000;
        return dayTime >= DAY_START && dayTime < DAY_END;
    }

    /**
     * Checks if the turtle is already in a good basking spot.
     *
     * @return true if current position is suitable
     */
    private boolean isAlreadyInGoodBaskingSpot() {
        BlockPos currentPos = this.turtle.blockPosition();
        return isValidBaskingSpot(currentPos);
    }

    /**
     * Finds a suitable basking spot within search radius.
     *
     * @return true if a basking spot was found
     */
    private boolean findBaskingSpot() {
        BlockPos turtlePos = this.turtle.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Level level = this.turtle.level();

        BlockPos bestSpot = null;
        double bestScore = -1;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = -VERTICAL_SEARCH_RANGE; y <= VERTICAL_SEARCH_RANGE; y++) {
                    mutablePos.set(turtlePos.getX() + x, turtlePos.getY() + y, turtlePos.getZ() + z);

                    if (isValidBaskingSpot(mutablePos)) {
                        double score = calculateBaskingSpotScore(mutablePos);
                        if (score > bestScore) {
                            bestScore = score;
                            bestSpot = mutablePos.immutable();
                        }
                    }
                }
            }
        }

        if (bestSpot != null) {
            this.baskingSpot = bestSpot;
            LOGGER.debug("Turtle {} found basking spot at {} (score: {})",
                this.turtle.getName().getString(), bestSpot, bestScore);
            return true;
        }

        LOGGER.debug("Turtle {} could not find basking spot within {} blocks",
            this.turtle.getName().getString(), SEARCH_RADIUS);
        return false;
    }

    /**
     * Checks if a position is valid for basking.
     *
     * @param pos the position to check
     * @return true if valid for basking
     */
    private boolean isValidBaskingSpot(BlockPos pos) {
        Level level = this.turtle.level();
        BlockState groundState = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());

        if (!isValidBaskingSurface(groundState)) {
            return false;
        }

        if (!aboveState.isAir()) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }

        int lightLevel = level.getBrightness(LightLayer.SKY, pos.above());
        if (lightLevel < MIN_SUNLIGHT_LEVEL) {
            return false;
        }

        return true;
    }

    /**
     * Checks if a block state is a valid basking surface.
     *
     * @param state the block state to check
     * @return true if valid for basking
     */
    private boolean isValidBaskingSurface(BlockState state) {
        if (state.is(BlockTags.SAND)) {
            return true;
        }

        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) ||
            state.is(Blocks.STONE_SLAB) || state.is(Blocks.SMOOTH_STONE_SLAB)) {
            return true;
        }

        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) {
            return true;
        }

        return false;
    }

    /**
     * Calculates a score for a basking spot based on sunlight and accessibility.
     *
     * @param pos the position to score
     * @return score (higher is better)
     */
    private double calculateBaskingSpotScore(BlockPos pos) {
        Level level = this.turtle.level();
        double distToTurtle = this.turtle.blockPosition().distSqr(pos);

        int lightLevel = level.getBrightness(LightLayer.SKY, pos.above());
        double sunlightScore = lightLevel * 5.0;

        double accessibilityScore = 100.0 / (1.0 + distToTurtle / 20.0);

        BlockState groundState = level.getBlockState(pos);
        double surfaceScore = 0;
        if (groundState.is(BlockTags.SAND)) {
            surfaceScore = 20.0;
        } else if (groundState.is(Blocks.STONE) || groundState.is(Blocks.COBBLESTONE)) {
            surfaceScore = 15.0;
        } else {
            surfaceScore = 10.0;
        }

        return sunlightScore + accessibilityScore + surfaceScore;
    }

    /**
     * Checks if the turtle is near the basking spot.
     *
     * @return true if within basking distance
     */
    private boolean isNearBaskingSpot() {
        if (this.baskingSpot == null) {
            return false;
        }

        return this.turtle.position().closerThan(this.baskingSpot.getCenter(), ACCEPTED_DISTANCE);
    }

    /**
     * Navigates the turtle to the basking spot.
     */
    private void navigateToBaskingSpot() {
        if (this.baskingSpot == null) {
            return;
        }

        this.turtle.getNavigation().moveTo(
            this.baskingSpot.getX() + 0.5,
            this.baskingSpot.getY(),
            this.baskingSpot.getZ() + 0.5,
            this.speedModifier
        );
    }

    /**
     * Starts the basking behavior.
     */
    private void startBasking() {
        this.isBasking = true;
        this.baskingTicks = 0;
        this.turtle.getNavigation().stop();
        LOGGER.debug("Turtle {} started basking for {} ticks",
            this.turtle.getName().getString(), this.baskingDuration);
    }

    /**
     * Performs the basking behavior (mostly staying still).
     */
    private void performBasking() {
        this.baskingTicks++;

        if (this.baskingSpot != null) {
            this.turtle.getLookControl().setLookAt(
                this.baskingSpot.getX() + 0.5 + (this.turtle.getRandom().nextDouble() - 0.5) * 2.0,
                this.baskingSpot.getY() + 1.0,
                this.baskingSpot.getZ() + 0.5 + (this.turtle.getRandom().nextDouble() - 0.5) * 2.0
            );
        }

        this.turtle.getNavigation().stop();

        if (this.baskingTicks % 100 == 0) {
            LOGGER.debug("Turtle {} is basking ({}/{})",
                this.turtle.getName().getString(), this.baskingTicks, this.baskingDuration);
        }
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
