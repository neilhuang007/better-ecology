package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes animals seek water when thirsty and drink to restore thirst.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when {@link AnimalNeeds#isThirsty(Mob)} returns true</li>
 *   <li>Searches for nearby water blocks within a configurable radius</li>
 *   <li>Pathfinds to a position adjacent to water (not in it)</li>
 *   <li>Drinks to restore thirst when adjacent to water</li>
 *   <li>Stops when {@link AnimalNeeds#isHydrated(Mob)} returns true</li>
 * </ul>
 */
public class SeekWaterGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeekWaterGoal.class);
    private static final int SEARCH_INTERVAL_TICKS = 40;
    private static final int GIVE_UP_TICKS = 1200;
    private static final double ACCEPTED_DISTANCE = 1.5;

    private final Mob mob;
    private final double speedModifier;
    private final int searchRadius;
    private final int verticalSearchRange;

    private BlockPos targetWaterPos;
    private int searchCooldown;
    private int tryTicks;
    private boolean reachedWater;
    private int drinkingTicks;

    /**
     * Creates a new SeekWaterGoal.
     *
     * @param mob the mob that will seek water
     * @param speedModifier movement speed multiplier when pathfinding to water
     * @param searchRadius horizontal search radius for water blocks
     */
    public SeekWaterGoal(Mob mob, double speedModifier, int searchRadius) {
        this(mob, speedModifier, searchRadius, 3);
    }

    /**
     * Creates a new SeekWaterGoal with vertical search range.
     *
     * @param mob the mob that will seek water
     * @param speedModifier movement speed multiplier when pathfinding to water
     * @param searchRadius horizontal search radius for water blocks
     * @param verticalSearchRange vertical search range (blocks above and below)
     */
    public SeekWaterGoal(Mob mob, double speedModifier, int searchRadius, int verticalSearchRange) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.verticalSearchRange = verticalSearchRange;
        this.searchCooldown = 0;
        this.targetWaterPos = null;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AnimalNeeds.isThirsty(this.mob)) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        return findNearestWater();
    }

    @Override
    public boolean canContinueToUse() {
        if (AnimalNeeds.isHydrated(this.mob)) {
            LOGGER.debug("Mob {} is now hydrated, stopping water seeking", this.mob.getName().getString());
            return false;
        }

        if (this.tryTicks >= GIVE_UP_TICKS) {
            LOGGER.debug("Mob {} gave up seeking water after {} ticks", this.mob.getName().getString(), this.tryTicks);
            return false;
        }

        if (this.targetWaterPos == null || !isWaterBlock(this.targetWaterPos)) {
            LOGGER.debug("Water at {} is no longer valid for mob {}", this.targetWaterPos, this.mob.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("Mob {} starting to seek water at {}", this.mob.getName().getString(), this.targetWaterPos);
        this.tryTicks = 0;
        this.drinkingTicks = 0;
        this.reachedWater = false;
        navigateToWater();
    }

    @Override
    public void stop() {
        LOGGER.debug("Mob {} stopped seeking water. Thirst: {}", this.mob.getName().getString(), AnimalNeeds.getThirst(this.mob));
        this.targetWaterPos = null;
        this.reachedWater = false;
        this.drinkingTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.tryTicks++;

        if (isNearWater()) {
            this.reachedWater = true;
            this.mob.getNavigation().stop();

            drink();
        } else {
            this.reachedWater = false;
            this.drinkingTicks = 0;

            if (shouldRecalculatePath()) {
                navigateToWater();
            }
        }
    }

    /**
     * Finds the nearest water block within search radius.
     *
     * @return true if water was found, false otherwise
     */
    private boolean findNearestWater() {
        BlockPos mobPos = this.mob.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        BlockPos closestWater = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int y = -this.verticalSearchRange; y <= this.verticalSearchRange; y++) {
            for (int x = -this.searchRadius; x <= this.searchRadius; x++) {
                for (int z = -this.searchRadius; z <= this.searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);

                    if (isWaterBlock(mutablePos)) {
                        double distSq = mobPos.distSqr(mutablePos);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closestWater = mutablePos.immutable();
                        }
                    }
                }
            }
        }

        if (closestWater != null) {
            this.targetWaterPos = closestWater;
            LOGGER.debug("Mob {} found water at {} (distance: {})",
                this.mob.getName().getString(), closestWater, Math.sqrt(closestDistSq));
            return true;
        }

        LOGGER.debug("Mob {} could not find water within {} blocks",
            this.mob.getName().getString(), this.searchRadius);
        return false;
    }

    /**
     * Checks if a block position contains water.
     *
     * @param pos the position to check
     * @return true if the position contains water
     */
    private boolean isWaterBlock(BlockPos pos) {
        Level level = this.mob.level();
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    /**
     * Checks if the mob is near enough to the water to drink.
     *
     * @return true if the mob is within drinking distance of the water
     */
    private boolean isNearWater() {
        if (this.targetWaterPos == null) {
            return false;
        }

        return this.mob.position().closerThan(this.targetWaterPos.getCenter(), ACCEPTED_DISTANCE);
    }

    /**
     * Navigates the mob to the water position.
     */
    private void navigateToWater() {
        if (this.targetWaterPos == null) {
            return;
        }

        this.mob.getNavigation().moveTo(
            this.targetWaterPos.getX() + 0.5,
            this.targetWaterPos.getY(),
            this.targetWaterPos.getZ() + 0.5,
            this.speedModifier
        );
    }

    /**
     * Handles drinking behavior to restore thirst.
     */
    private void drink() {
        this.drinkingTicks++;

        if (this.drinkingTicks >= AnimalThresholds.DRINKING_DURATION) {
            return;
        }

        this.mob.getLookControl().setLookAt(
            this.targetWaterPos.getX() + 0.5,
            this.targetWaterPos.getY() + 0.5,
            this.targetWaterPos.getZ() + 0.5
        );

        if (this.drinkingTicks % 10 == 0) {
            float restoreAmount = AnimalThresholds.DRINKING_THIRST_RESTORE;
            AnimalNeeds.modifyThirst(this.mob, restoreAmount);

            LOGGER.debug("Mob {} drinking water. Thirst: {} (+{})",
                this.mob.getName().getString(), AnimalNeeds.getThirst(this.mob), restoreAmount);
        }
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
