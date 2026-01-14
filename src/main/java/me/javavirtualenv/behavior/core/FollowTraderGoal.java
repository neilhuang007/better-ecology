package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes trader llamas follow and stay near their wandering trader.
 *
 * <p>Trader llamas will:
 * <ul>
 *   <li>Follow their assigned wandering trader</li>
 *   <li>Maintain a comfortable distance from the trader</li>
 *   <li>Speed up when too far from the trader</li>
 * </ul>
 */
public class FollowTraderGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowTraderGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final int FOLLOW_START_DISTANCE = 8;
    private static final int FOLLOW_STOP_DISTANCE = 3;
    private static final int LOST_TRADER_DISTANCE = 32;

    private final TraderLlama llama;
    private final double speedModifier;

    @Nullable
    private WanderingTrader trader;
    private int searchCooldown;
    private int followTicks;

    /**
     * Creates a new FollowTraderGoal.
     *
     * @param llama the trader llama that will follow the trader
     * @param speedModifier movement speed multiplier
     */
    public FollowTraderGoal(TraderLlama llama, double speedModifier) {
        this.llama = llama;
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        WanderingTrader foundTrader = findWanderingTrader();
        if (foundTrader == null) {
            return false;
        }

        double distance = this.llama.distanceTo(foundTrader);
        if (distance < FOLLOW_START_DISTANCE) {
            return false;
        }

        this.trader = foundTrader;
        LOGGER.debug("{} found trader {} to follow at distance {}",
            this.llama.getName().getString(),
            foundTrader.getName().getString(),
            String.format("%.1f", distance));

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.trader == null || !this.trader.isAlive()) {
            return false;
        }

        double distance = this.llama.distanceTo(this.trader);

        if (distance > LOST_TRADER_DISTANCE) {
            LOGGER.debug("{} lost its trader - too far away", this.llama.getName().getString());
            return false;
        }

        if (distance < FOLLOW_STOP_DISTANCE) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.followTicks = 0;
        navigateToTrader();
    }

    @Override
    public void stop() {
        if (this.trader != null) {
            LOGGER.debug("{} stopped following trader {}",
                this.llama.getName().getString(),
                this.trader.getName().getString());
        }
        this.trader = null;
        this.followTicks = 0;
        this.llama.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.followTicks++;

        if (this.trader == null) {
            return;
        }

        // Look at the trader
        this.llama.getLookControl().setLookAt(this.trader, 10.0F, this.llama.getMaxHeadXRot());

        // Recalculate path periodically or when navigation is done
        if (this.followTicks % 10 == 0 || this.llama.getNavigation().isDone()) {
            double distance = this.llama.distanceTo(this.trader);

            // Adjust speed based on distance - move faster when far from trader
            double adjustedSpeed = this.speedModifier;
            if (distance > 15) {
                adjustedSpeed = this.speedModifier * 1.5;
            } else if (distance > 10) {
                adjustedSpeed = this.speedModifier * 1.2;
            }

            this.llama.getNavigation().moveTo(this.trader, adjustedSpeed);
        }
    }

    /**
     * Finds the wandering trader that owns this llama.
     *
     * @return the wandering trader, or null if none found
     */
    @Nullable
    private WanderingTrader findWanderingTrader() {
        return this.llama.level()
            .getEntitiesOfClass(WanderingTrader.class, this.llama.getBoundingBox().inflate(32))
            .stream()
            .filter(t -> t.isAlive())
            .min((t1, t2) -> Double.compare(this.llama.distanceToSqr(t1), this.llama.distanceToSqr(t2)))
            .orElse(null);
    }

    /**
     * Navigates the llama toward the trader.
     */
    private void navigateToTrader() {
        if (this.trader == null) {
            return;
        }

        this.llama.getNavigation().moveTo(this.trader, this.speedModifier);
    }
}
