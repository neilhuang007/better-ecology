package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes baby animals follow adult animals of the same type.
 *
 * <p>Baby animals will:
 * <ul>
 *   <li>Search for nearby adults of the same species</li>
 *   <li>Follow the nearest adult as a "parent"</li>
 *   <li>Maintain a comfortable following distance</li>
 *   <li>Speed up when too far from the parent</li>
 * </ul>
 */
public class FollowParentGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowParentGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final int FOLLOW_START_DISTANCE = 6;
    private static final int FOLLOW_STOP_DISTANCE = 2;
    private static final int LOST_PARENT_DISTANCE = 24;

    private final Mob mob;
    private final Class<? extends Mob> parentType;
    private final double speedModifier;
    private final int searchRadius;

    @Nullable
    private Mob parent;
    private int searchCooldown;
    private int followTicks;

    /**
     * Creates a new FollowParentGoal.
     *
     * @param mob the baby mob that will follow a parent
     * @param parentType the class of adult entities to follow
     * @param speedModifier movement speed multiplier
     * @param searchRadius radius to search for parents
     */
    public FollowParentGoal(Mob mob, Class<? extends Mob> parentType, double speedModifier, int searchRadius) {
        this.mob = mob;
        this.parentType = parentType;
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.searchCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Creates a new FollowParentGoal with default parameters.
     *
     * @param mob the baby mob that will follow a parent
     * @param parentType the class of adult entities to follow
     */
    public FollowParentGoal(Mob mob, Class<? extends Mob> parentType) {
        this(mob, parentType, 1.1, 16);
    }

    @Override
    public boolean canUse() {
        // Only baby animals should follow parents
        if (!this.mob.isBaby()) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        Mob nearestParent = findNearestParent();
        if (nearestParent == null) {
            return false;
        }

        double distance = this.mob.distanceTo(nearestParent);
        if (distance < FOLLOW_START_DISTANCE) {
            return false;
        }

        this.parent = nearestParent;
        LOGGER.debug("{} (baby) found parent {} to follow at distance {}",
            this.mob.getName().getString(),
            nearestParent.getName().getString(),
            String.format("%.1f", distance));

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.mob.isBaby()) {
            return false;
        }

        if (this.parent == null || !this.parent.isAlive()) {
            return false;
        }

        double distance = this.mob.distanceTo(this.parent);

        if (distance > LOST_PARENT_DISTANCE) {
            LOGGER.debug("{} lost its parent - too far away", this.mob.getName().getString());
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
        navigateToParent();
    }

    @Override
    public void stop() {
        if (this.parent != null) {
            LOGGER.debug("{} stopped following parent {}",
                this.mob.getName().getString(),
                this.parent.getName().getString());
        }
        this.parent = null;
        this.followTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.followTicks++;

        if (this.parent == null) {
            return;
        }

        // Look at the parent
        this.mob.getLookControl().setLookAt(this.parent, 10.0F, this.mob.getMaxHeadXRot());

        // Recalculate path periodically or when navigation is done
        if (this.followTicks % 10 == 0 || this.mob.getNavigation().isDone()) {
            double distance = this.mob.distanceTo(this.parent);

            // Adjust speed based on distance - move faster when far from parent
            double adjustedSpeed = this.speedModifier;
            if (distance > 10) {
                adjustedSpeed = this.speedModifier * 1.3;
            } else if (distance > 6) {
                adjustedSpeed = this.speedModifier * 1.1;
            }

            this.mob.getNavigation().moveTo(this.parent, adjustedSpeed);
        }
    }

    /**
     * Finds the nearest adult entity of the parent type.
     *
     * @return the nearest parent, or null if none found
     */
    @Nullable
    private Mob findNearestParent() {
        AABB searchBox = this.mob.getBoundingBox().inflate(this.searchRadius);
        List<? extends Mob> nearbyAdults = this.mob.level()
            .getEntitiesOfClass(this.parentType, searchBox, this::isValidParent);

        return nearbyAdults.stream()
            .min(Comparator.comparingDouble(adult -> this.mob.distanceToSqr(adult)))
            .orElse(null);
    }

    /**
     * Validates if an entity is a valid parent.
     *
     * @param entity the entity to check
     * @return true if this is a valid parent
     */
    private boolean isValidParent(Mob entity) {
        if (entity == this.mob) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        // Only adults can be parents
        if (entity.isBaby()) {
            return false;
        }
        return true;
    }

    /**
     * Navigates the mob toward the parent.
     */
    private void navigateToParent() {
        if (this.parent == null) {
            return;
        }

        this.mob.getNavigation().moveTo(this.parent, this.speedModifier);
    }
}
