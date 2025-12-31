package me.javavirtualenv.behavior.production;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Base AI goal for resource gathering behaviors.
 * <p>
 * Entities with this goal will search for and gather resources from the environment,
 * then return to their "home" (hive, nest, etc.) to process or store the resources.
 * <p>
 * This is a generalized framework that can be extended for specific behaviors:
 * - Bees: Pollination and nectar collection
 * - Sniffers: Ancient seed digging
 * - Axolotls: Food hunting
 */
public class ResourceGatheringGoal extends Goal {

    protected final Mob mob;
    protected final double searchRadius;
    protected final int searchInterval;
    protected final int gatheringDuration;

    protected BlockPos targetResourcePos;
    protected int ticksSinceLastSearch;
    protected int gatheringTicks;
    protected boolean hasResource;
    protected boolean returningHome;

    public ResourceGatheringGoal(Mob mob, double searchRadius, int searchInterval, int gatheringDuration) {
        this.mob = mob;
        this.searchRadius = searchRadius;
        this.searchInterval = searchInterval;
        this.gatheringDuration = gatheringDuration;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive()) {
            return false;
        }

        if (hasResource && returningHome) {
            return true;
        }

        if (hasResource) {
            returningHome = true;
            return true;
        }

        ticksSinceLastSearch++;
        if (ticksSinceLastSearch < searchInterval) {
            return false;
        }

        ticksSinceLastSearch = 0;
        targetResourcePos = findNearestResource();

        return targetResourcePos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive()) {
            return false;
        }

        if (returningHome) {
            return !isNearHome();
        }

        if (targetResourcePos == null) {
            return false;
        }

        if (hasResource) {
            return true;
        }

        return gatheringTicks < gatheringDuration && mob.level().isLoaded(targetResourcePos);
    }

    @Override
    public void tick() {
        if (returningHome) {
            returnToHome();
            return;
        }

        if (targetResourcePos != null && mob.level().isLoaded(targetResourcePos)) {
            double distance = mob.position().distanceTo(
                targetResourcePos.getX() + 0.5,
                targetResourcePos.getY(),
                targetResourcePos.getZ() + 0.5
            );

            if (distance > 2.0) {
                mob.getNavigation().moveTo(targetResourcePos.getX(), targetResourcePos.getY(), targetResourcePos.getZ(), 1.0);
            } else {
                gatherResource();
                gatheringTicks++;

                if (gatheringTicks >= gatheringDuration) {
                    hasResource = true;
                }
            }
        }
    }

    @Override
    public void stop() {
        targetResourcePos = null;
        gatheringTicks = 0;

        if (returningHome && isNearHome()) {
            returningHome = false;
            hasResource = false;
            onResourceDelivered();
        }
    }

    /**
     * Finds the nearest resource block.
     * Override this for specific resource types.
     */
    protected BlockPos findNearestResource() {
        BlockPos mobPos = mob.blockPosition();

        BlockPos nearest = null;
        double nearestDistance = searchRadius;

        for (BlockPos pos : BlockPos.betweenClosed(
            mobPos.getX() - (int) searchRadius,
            mobPos.getY() - 8,
            mobPos.getZ() - (int) searchRadius,
            mobPos.getX() + (int) searchRadius,
            mobPos.getY() + 8,
            mobPos.getZ() + (int) searchRadius
        )) {
            if (isValidResource(pos)) {
                double distance = mob.position().distanceTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5
                );

                if (distance < nearestDistance) {
                    nearest = pos;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    /**
     * Checks if a position contains a valid resource.
     * Override this for specific resource types.
     */
    protected boolean isValidResource(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        return state.is(BlockTags.FLOWERS) || state.is(Blocks.SPORE_BLOSSOM);
    }

    /**
     * Gathers the resource at the target position.
     * Override this for specific gathering behaviors.
     */
    protected void gatherResource() {
        // Base implementation - override in subclasses
        gatheringTicks++;
    }

    /**
     * Returns the entity to its home.
     */
    protected void returnToHome() {
        BlockPos homePos = getHomePosition();
        if (homePos != null) {
            double distance = mob.position().distanceTo(
                homePos.getX() + 0.5,
                homePos.getY(),
                homePos.getZ() + 0.5
            );

            if (distance > 2.0) {
                mob.getNavigation().moveTo(homePos.getX(), homePos.getY(), homePos.getZ(), 1.0);
            }
        }
    }

    /**
     * Checks if the entity is near its home.
     */
    protected boolean isNearHome() {
        BlockPos homePos = getHomePosition();
        if (homePos == null) {
            return true;
        }

        double distance = mob.position().distanceTo(
            homePos.getX() + 0.5,
            homePos.getY(),
            homePos.getZ() + 0.5
        );

        return distance < 3.0;
    }

    /**
     * Gets the home position for this entity.
     * Override in subclasses for specific home types.
     */
    protected BlockPos getHomePosition() {
        if (mob instanceof net.minecraft.world.entity.animal.Bee) {
            net.minecraft.world.entity.animal.Bee bee = (net.minecraft.world.entity.animal.Bee) mob;
            return bee.getHivePos();
        }
        return mob.blockPosition();
    }

    /**
     * Called when resource is delivered to home.
     * Override in subclasses for specific behavior.
     */
    protected void onResourceDelivered() {
        // Base implementation - override in subclasses
    }
}
