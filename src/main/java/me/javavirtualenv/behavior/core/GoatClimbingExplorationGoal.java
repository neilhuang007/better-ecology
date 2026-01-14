package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes goats seek and climb to elevated positions for predator detection and exploration.
 *
 * <p>Scientific Basis: Mountain goats and domestic goats are exceptional climbers that seek
 * elevated positions for:
 * <ul>
 *   <li>Enhanced predator detection and vigilance</li>
 *   <li>Access to browse vegetation on slopes and cliffs</li>
 *   <li>Social dominance displays</li>
 *   <li>Thermoregulation (cooler/warmer positions)</li>
 * </ul>
 *
 * <p>Behavior:
 * <ul>
 *   <li>Searches for highest block within 16 blocks</li>
 *   <li>Targets climbable blocks: fences, walls, hay bales, logs, stones</li>
 *   <li>Stands on elevated position for 30-60 seconds</li>
 *   <li>Enhanced jump height when approaching elevated positions</li>
 *   <li>Prefers positions with clear view (exposed blocks)</li>
 *   <li>2-minute cooldown between climbing sessions</li>
 * </ul>
 */
public class GoatClimbingExplorationGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoatClimbingExplorationGoal.class);

    private static final int SEARCH_RADIUS = 16;
    private static final int VERTICAL_SEARCH_RANGE = 8;
    private static final int STAND_DURATION_MIN = 600;  // 30 seconds
    private static final int STAND_DURATION_MAX = 1200; // 60 seconds
    private static final int COOLDOWN_DURATION = 2400;  // 2 minutes
    private static final int SEARCH_INTERVAL = 40;      // 2 seconds between searches
    private static final double ACCEPTED_DISTANCE = 1.5;
    private static final double JUMP_BOOST_MULTIPLIER = 1.3;
    private static final int MIN_HEIGHT_ADVANTAGE = 2; // Minimum blocks higher than current position

    private final Mob goat;
    private final Level level;
    private final double speedModifier;

    private BlockPos targetClimbPos;
    private int standingTicks;
    private int standDuration;
    private int searchCooldown;
    private int sessionCooldown;
    private boolean isStanding;
    private double originalJumpBoost;

    /**
     * Creates a new GoatClimbingExplorationGoal.
     *
     * @param goat the goat that will climb
     * @param speedModifier movement speed multiplier when pathfinding to elevated position
     */
    public GoatClimbingExplorationGoal(Mob goat, double speedModifier) {
        this.goat = goat;
        this.level = goat.level();
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.sessionCooldown = 0;
        this.isStanding = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.sessionCooldown > 0) {
            this.sessionCooldown--;
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        if (this.goat.isInWater() || this.goat.isInLava()) {
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL);
        return findHighestClimbablePosition();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetClimbPos == null) {
            return false;
        }

        if (this.isStanding) {
            if (this.standingTicks >= this.standDuration) {
                return false;
            }

            double distanceSq = this.goat.position().distanceToSqr(
                this.targetClimbPos.getX() + 0.5,
                this.targetClimbPos.getY() + 1.0,
                this.targetClimbPos.getZ() + 0.5
            );

            return distanceSq <= ACCEPTED_DISTANCE * ACCEPTED_DISTANCE;
        }

        return isValidClimbTarget(this.targetClimbPos);
    }

    @Override
    public void start() {
        LOGGER.debug("Goat {} starting to climb to {}", this.goat.getName().getString(), this.targetClimbPos);
        this.standingTicks = 0;
        this.standDuration = STAND_DURATION_MIN + this.goat.getRandom().nextInt(STAND_DURATION_MAX - STAND_DURATION_MIN);
        this.isStanding = false;

        applyJumpBoost();
        navigateToClimbPosition();
    }

    @Override
    public void stop() {
        LOGGER.debug("Goat {} stopped climbing session", this.goat.getName().getString());
        this.targetClimbPos = null;
        this.isStanding = false;
        this.standingTicks = 0;
        this.sessionCooldown = COOLDOWN_DURATION;
        this.goat.getNavigation().stop();

        removeJumpBoost();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetClimbPos == null) {
            return;
        }

        if (isAtTargetPosition()) {
            if (!this.isStanding) {
                this.isStanding = true;
                this.goat.getNavigation().stop();
                LOGGER.debug("Goat {} reached elevated position, standing for {} ticks",
                    this.goat.getName().getString(), this.standDuration);
            }

            performLookoutBehavior();
            this.standingTicks++;
        } else {
            this.isStanding = false;

            this.goat.getLookControl().setLookAt(
                this.targetClimbPos.getX() + 0.5,
                this.targetClimbPos.getY() + 1.0,
                this.targetClimbPos.getZ() + 0.5
            );

            if (shouldRecalculatePath()) {
                navigateToClimbPosition();
            }
        }
    }

    /**
     * Finds the highest climbable position within search radius.
     *
     * @return true if a suitable climbing position was found
     */
    private boolean findHighestClimbablePosition() {
        BlockPos goatPos = this.goat.blockPosition();
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        BlockPos bestPosition = null;
        int bestHeight = goatPos.getY();
        double bestViewScore = 0;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = -2; y <= VERTICAL_SEARCH_RANGE; y++) {
                    searchPos.set(goatPos.getX() + x, goatPos.getY() + y, goatPos.getZ() + z);

                    if (!isValidClimbTarget(searchPos)) {
                        continue;
                    }

                    int height = searchPos.getY();
                    int heightAdvantage = height - goatPos.getY();

                    if (heightAdvantage < MIN_HEIGHT_ADVANTAGE) {
                        continue;
                    }

                    double viewScore = calculateViewScore(searchPos);

                    if (height > bestHeight || (height == bestHeight && viewScore > bestViewScore)) {
                        bestHeight = height;
                        bestViewScore = viewScore;
                        bestPosition = searchPos.immutable();
                    }
                }
            }
        }

        if (bestPosition != null) {
            this.targetClimbPos = bestPosition;
            LOGGER.debug("Goat {} found climbing position at {} (height advantage: {}, view score: {})",
                this.goat.getName().getString(), bestPosition, bestHeight - goatPos.getY(), bestViewScore);
            return true;
        }

        return false;
    }

    /**
     * Checks if a block position is a valid climbing target.
     *
     * @param pos the position to check
     * @return true if the position is a valid climbing target
     */
    private boolean isValidClimbTarget(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        Block block = state.getBlock();

        if (!isClimbableBlock(state)) {
            return false;
        }

        BlockPos above = pos.above();
        BlockState aboveState = this.level.getBlockState(above);

        if (!aboveState.isAir() && !aboveState.is(BlockTags.LEAVES) && !aboveState.is(BlockTags.FLOWERS)) {
            return false;
        }

        return canPathToPosition(pos);
    }

    /**
     * Checks if a block is climbable by goats.
     *
     * @param state the block state to check
     * @return true if the block is climbable
     */
    private boolean isClimbableBlock(BlockState state) {
        Block block = state.getBlock();

        if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
            return true;
        }

        return block == Blocks.HAY_BLOCK ||
               block == Blocks.STONE ||
               block == Blocks.COBBLESTONE ||
               block == Blocks.MOSSY_COBBLESTONE ||
               block == Blocks.STONE_BRICKS ||
               block == Blocks.MOSSY_STONE_BRICKS ||
               block == Blocks.ANDESITE ||
               block == Blocks.DIORITE ||
               block == Blocks.GRANITE ||
               state.is(BlockTags.LOGS) ||
               state.is(BlockTags.PLANKS);
    }

    /**
     * Calculates view score for a position (higher = better lookout position).
     *
     * @param pos the position to evaluate
     * @return view score (0-1 scale)
     */
    private double calculateViewScore(BlockPos pos) {
        int exposedSides = 0;

        BlockPos[] directions = {
            pos.north(),
            pos.south(),
            pos.east(),
            pos.west()
        };

        for (BlockPos adjacent : directions) {
            BlockState adjacentState = this.level.getBlockState(adjacent);
            if (adjacentState.isAir() || !adjacentState.isSolid()) {
                exposedSides++;
            }
        }

        BlockPos above = pos.above();
        if (this.level.getBlockState(above).isAir()) {
            exposedSides++;
        }

        return exposedSides / 5.0;
    }

    /**
     * Checks if the goat can path to a position.
     *
     * @param pos the position to check
     * @return true if pathfinding is possible
     */
    private boolean canPathToPosition(BlockPos pos) {
        PathNavigation navigation = this.goat.getNavigation();
        return navigation.createPath(pos, 0) != null;
    }

    /**
     * Checks if the goat is at the target position.
     *
     * @return true if at target
     */
    private boolean isAtTargetPosition() {
        if (this.targetClimbPos == null) {
            return false;
        }

        double distanceSq = this.goat.position().distanceToSqr(
            this.targetClimbPos.getX() + 0.5,
            this.targetClimbPos.getY() + 1.0,
            this.targetClimbPos.getZ() + 0.5
        );

        return distanceSq <= ACCEPTED_DISTANCE * ACCEPTED_DISTANCE;
    }

    /**
     * Navigates the goat to the climbing position.
     */
    private void navigateToClimbPosition() {
        if (this.targetClimbPos == null) {
            return;
        }

        this.goat.getNavigation().moveTo(
            this.targetClimbPos.getX() + 0.5,
            this.targetClimbPos.getY() + 1.0,
            this.targetClimbPos.getZ() + 0.5,
            this.speedModifier
        );
    }

    /**
     * Performs lookout behavior while standing on elevated position.
     */
    private void performLookoutBehavior() {
        int lookCycle = this.standingTicks % 80;

        if (lookCycle < 20) {
            lookInDirection(0);
        } else if (lookCycle < 40) {
            lookInDirection(90);
        } else if (lookCycle < 60) {
            lookInDirection(180);
        } else {
            lookInDirection(270);
        }
    }

    /**
     * Makes the goat look in a specific direction.
     *
     * @param yawOffset yaw offset in degrees
     */
    private void lookInDirection(float yawOffset) {
        float targetYaw = this.goat.getYRot() + yawOffset;
        double lookDistance = 8.0;

        double lookX = this.goat.getX() + Math.sin(Math.toRadians(targetYaw)) * lookDistance;
        double lookZ = this.goat.getZ() + Math.cos(Math.toRadians(targetYaw)) * lookDistance;

        this.goat.getLookControl().setLookAt(
            lookX,
            this.goat.getY() + this.goat.getEyeHeight(),
            lookZ
        );
    }

    /**
     * Applies temporary jump boost for climbing.
     */
    private void applyJumpBoost() {
        if (this.goat.getAttributes().hasAttribute(Attributes.JUMP_STRENGTH)) {
            this.originalJumpBoost = this.goat.getAttributeValue(Attributes.JUMP_STRENGTH);
            this.goat.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(
                this.originalJumpBoost * JUMP_BOOST_MULTIPLIER
            );
        }
    }

    /**
     * Removes temporary jump boost.
     */
    private void removeJumpBoost() {
        if (this.goat.getAttributes().hasAttribute(Attributes.JUMP_STRENGTH)) {
            this.goat.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(this.originalJumpBoost);
        }
    }

    /**
     * Determines if the path should be recalculated.
     *
     * @return true if path recalculation is needed
     */
    private boolean shouldRecalculatePath() {
        return this.goat.getNavigation().isDone();
    }
}
