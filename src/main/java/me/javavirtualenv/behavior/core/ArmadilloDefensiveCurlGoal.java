package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes armadillos curl into a defensive ball when threatened.
 *
 * <p>Scientific basis: Three-banded armadillos (Tolypeutes) are unique among armadillos
 * in their ability to roll into a complete defensive ball when threatened. This provides
 * significant protection from predators but renders them immobile.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Triggers when health below threshold AND predator nearby AND cannot escape</li>
 *   <li>Armadillo curls into ball (becomes immobile)</li>
 *   <li>Gains damage reduction while curled</li>
 *   <li>Uncurls when safe (no threats nearby for duration)</li>
 * </ul>
 */
public class ArmadilloDefensiveCurlGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArmadilloDefensiveCurlGoal.class);

    private static final float TRIGGER_HEALTH_PERCENT = 0.5f;
    private static final int PREDATOR_DETECTION_RANGE = 4;
    private static final int SAFETY_CHECK_RANGE = 8;
    private static final int MIN_CURL_DURATION = 100; // 5 seconds
    private static final int MAX_CURL_DURATION = 200; // 10 seconds
    private static final int SAFETY_CHECK_DURATION = 60; // 3 seconds
    private static final float DAMAGE_REDUCTION = 0.6f;

    private final PathfinderMob armadillo;
    private final List<Class<? extends LivingEntity>> predatorTypes;
    private final PathNavigation pathNav;
    private final TargetingConditions targetingConditions;

    private int curledTicks;
    private int maxCurlDuration;
    private int safetyCheckTicks;
    private boolean isCurled;

    /**
     * Creates a new defensive curl goal for armadillos.
     *
     * @param armadillo the armadillo that will curl
     * @param predatorTypes classes of predators to curl from
     */
    @SafeVarargs
    public ArmadilloDefensiveCurlGoal(
            PathfinderMob armadillo,
            Class<? extends LivingEntity>... predatorTypes) {
        this.armadillo = armadillo;
        this.predatorTypes = List.of(predatorTypes);
        this.pathNav = armadillo.getNavigation();
        this.targetingConditions = TargetingConditions.forCombat()
                .range(PREDATOR_DETECTION_RANGE);
        this.isCurled = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!isHealthBelowThreshold()) {
            return false;
        }

        LivingEntity nearbyPredator = findNearestPredator(PREDATOR_DETECTION_RANGE);
        if (nearbyPredator == null) {
            return false;
        }

        if (canEscapeToPredator(nearbyPredator)) {
            LOGGER.debug("{} can escape, not curling", armadillo.getName().getString());
            return false;
        }

        LOGGER.debug("{} will curl defensively - health low and predator {} nearby",
                armadillo.getName().getString(),
                nearbyPredator.getName().getString());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.isCurled) {
            return false;
        }

        if (this.curledTicks >= this.maxCurlDuration) {
            LOGGER.debug("{} uncurling - max duration reached", armadillo.getName().getString());
            return false;
        }

        LivingEntity nearbyThreat = findNearestPredator(SAFETY_CHECK_RANGE);
        if (nearbyThreat == null) {
            this.safetyCheckTicks++;
            if (this.safetyCheckTicks >= SAFETY_CHECK_DURATION) {
                LOGGER.debug("{} uncurling - safe for {} ticks",
                        armadillo.getName().getString(), SAFETY_CHECK_DURATION);
                return false;
            }
        } else {
            this.safetyCheckTicks = 0;
        }

        return true;
    }

    @Override
    public void start() {
        this.curledTicks = 0;
        this.safetyCheckTicks = 0;
        this.maxCurlDuration = MIN_CURL_DURATION +
                this.armadillo.getRandom().nextInt(MAX_CURL_DURATION - MIN_CURL_DURATION);
        this.isCurled = true;

        this.pathNav.stop();
        this.armadillo.setJumping(false);

        LOGGER.debug("{} curled into defensive ball for max {} ticks",
                armadillo.getName().getString(), this.maxCurlDuration);
    }

    @Override
    public void stop() {
        this.isCurled = false;
        this.curledTicks = 0;
        this.safetyCheckTicks = 0;

        LOGGER.debug("{} uncurled from defensive position", armadillo.getName().getString());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.curledTicks++;

        this.pathNav.stop();
        this.armadillo.setJumping(false);

        Vec3 pos = this.armadillo.position();
        this.armadillo.getLookControl().setLookAt(pos.x, pos.y, pos.z);
    }

    /**
     * Checks if armadillo health is below the curl trigger threshold.
     *
     * @return true if health is low enough to trigger curl
     */
    private boolean isHealthBelowThreshold() {
        float healthPercent = this.armadillo.getHealth() / this.armadillo.getMaxHealth();
        return healthPercent < TRIGGER_HEALTH_PERCENT;
    }

    /**
     * Finds the nearest predator within detection range.
     *
     * @param range detection range in blocks
     * @return nearest predator or null if none found
     */
    @Nullable
    private LivingEntity findNearestPredator(int range) {
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Class<? extends LivingEntity> predatorType : this.predatorTypes) {
            List<? extends LivingEntity> nearbyPredators = this.armadillo.level()
                    .getEntitiesOfClass(
                            predatorType,
                            this.armadillo.getBoundingBox().inflate(range, 3.0, range)
                    );

            for (LivingEntity predator : nearbyPredators) {
                if (!this.targetingConditions.test(this.armadillo, predator)) {
                    continue;
                }

                double distSq = this.armadillo.distanceToSqr(predator);
                if (distSq < nearestDistSq) {
                    nearest = predator;
                    nearestDistSq = distSq;
                }
            }
        }

        return nearest;
    }

    /**
     * Checks if armadillo can find a path to escape from the predator.
     *
     * @param predator the predator to escape from
     * @return true if escape path exists
     */
    private boolean canEscapeToPredator(LivingEntity predator) {
        Vec3 escapePos = DefaultRandomPos.getPosAway(
                this.armadillo,
                16,
                7,
                predator.position()
        );

        if (escapePos == null) {
            return false;
        }

        return this.pathNav.createPath(escapePos.x, escapePos.y, escapePos.z, 0) != null;
    }

    /**
     * Checks if the armadillo is currently curled.
     *
     * @return true if curled in defensive position
     */
    public boolean isCurled() {
        return this.isCurled;
    }

    /**
     * Gets the damage reduction multiplier while curled.
     *
     * @return damage reduction factor (0.0 to 1.0)
     */
    public float getDamageReduction() {
        return this.isCurled ? DAMAGE_REDUCTION : 0.0f;
    }
}
