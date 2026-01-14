package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes trader llamas protect their wandering trader from hostile mobs.
 *
 * <p>When a hostile mob threatens the trader, the llama will:
 * <ul>
 *   <li>Position itself between the trader and threat</li>
 *   <li>Spit at the hostile mob</li>
 *   <li>Continue defending until the threat is eliminated or flees</li>
 * </ul>
 *
 * <p>Based on protective behaviors of pack animals defending their herd leaders.
 */
public class ProtectTraderGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectTraderGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 10;
    private static final int PROTECTION_DURATION = 300;

    private final TraderLlama llama;
    private final double protectionRange;
    private final double threatRange;
    private final double speedModifier;

    @Nullable
    private WanderingTrader trader;
    @Nullable
    private LivingEntity targetThreat;
    private int searchCooldown;
    private int protectTicks;

    /**
     * Creates a new ProtectTraderGoal.
     *
     * @param llama the trader llama that protects the trader
     * @param protectionRange radius to scan for the trader
     * @param threatRange radius to scan for threats around trader
     * @param speedModifier movement speed when defending
     */
    public ProtectTraderGoal(
            TraderLlama llama,
            double protectionRange,
            double threatRange,
            double speedModifier) {
        this.llama = llama;
        this.protectionRange = protectionRange;
        this.threatRange = threatRange;
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        WanderingTrader foundTrader = findNearbyTrader();
        if (foundTrader == null) {
            return false;
        }

        LivingEntity threat = findThreatNearTrader(foundTrader);
        if (threat == null) {
            return false;
        }

        this.trader = foundTrader;
        this.targetThreat = threat;

        LOGGER.debug("{} will protect trader {} from threat {}",
            this.llama.getName().getString(),
            foundTrader.getName().getString(),
            threat.getName().getString());

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.protectTicks > PROTECTION_DURATION) {
            return false;
        }

        if (this.targetThreat == null || !this.targetThreat.isAlive()) {
            return false;
        }

        if (this.trader == null || !this.trader.isAlive()) {
            return false;
        }

        double threatDistanceToTrader = this.trader.distanceTo(this.targetThreat);
        if (threatDistanceToTrader > this.threatRange * 1.5) {
            LOGGER.debug("{} stops protecting - threat {} has fled",
                this.llama.getName().getString(),
                this.targetThreat.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.protectTicks = 0;
        moveToDefensivePosition();
    }

    @Override
    public void stop() {
        if (this.targetThreat != null) {
            LOGGER.debug("{} finished protecting from {}",
                this.llama.getName().getString(),
                this.targetThreat.getName().getString());
        }
        this.trader = null;
        this.targetThreat = null;
        this.protectTicks = 0;
        this.llama.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.protectTicks++;

        if (this.targetThreat == null || this.trader == null) {
            return;
        }

        // Look at the threat
        this.llama.getLookControl().setLookAt(this.targetThreat, 30.0F, 30.0F);

        // Position between trader and threat
        double distanceToThreat = this.llama.distanceTo(this.targetThreat);

        if (distanceToThreat > 8.0) {
            if (this.protectTicks % 10 == 0 || this.llama.getNavigation().isDone()) {
                moveToDefensivePosition();
            }
        }

        // Spit at threat when in range - trigger attack behavior
        if (distanceToThreat <= 16.0 && this.protectTicks % 40 == 0) {
            // Set the target to trigger spit attack behavior
            this.llama.setTarget(this.targetThreat);
            LOGGER.debug("{} targets threat {} for spitting",
                this.llama.getName().getString(),
                this.targetThreat.getName().getString());
        }
    }

    /**
     * Finds the nearest wandering trader.
     *
     * @return the wandering trader, or null if none
     */
    @Nullable
    private WanderingTrader findNearbyTrader() {
        AABB searchBox = this.llama.getBoundingBox().inflate(this.protectionRange);
        List<WanderingTrader> nearbyTraders = this.llama.level()
            .getEntitiesOfClass(WanderingTrader.class, searchBox, t -> t.isAlive());

        return nearbyTraders.stream()
            .min((t1, t2) -> Double.compare(this.llama.distanceToSqr(t1), this.llama.distanceToSqr(t2)))
            .orElse(null);
    }

    /**
     * Finds the nearest hostile mob threatening the trader.
     *
     * @param trader the trader to check threats around
     * @return the nearest threat, or null if none
     */
    @Nullable
    private LivingEntity findThreatNearTrader(WanderingTrader trader) {
        AABB threatBox = trader.getBoundingBox().inflate(this.threatRange);

        List<LivingEntity> hostiles = this.llama.level()
            .getEntitiesOfClass(LivingEntity.class, threatBox,
                entity -> entity instanceof Enemy && entity.isAlive() && entity != this.llama);

        return hostiles.stream()
            .min((e1, e2) -> Double.compare(trader.distanceToSqr(e1), trader.distanceToSqr(e2)))
            .orElse(null);
    }

    /**
     * Moves the llama to a defensive position between trader and threat.
     */
    private void moveToDefensivePosition() {
        if (this.trader == null || this.targetThreat == null) {
            return;
        }

        // Position the llama between trader and threat
        double dx = this.targetThreat.getX() - this.trader.getX();
        double dz = this.targetThreat.getZ() - this.trader.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0) {
            // Normalize and position llama halfway between trader and threat
            double normalizedX = dx / distance;
            double normalizedZ = dz / distance;
            double targetX = this.trader.getX() + normalizedX * 3.0;
            double targetZ = this.trader.getZ() + normalizedZ * 3.0;

            this.llama.getNavigation().moveTo(targetX, this.trader.getY(), targetZ, this.speedModifier);
        }
    }
}
