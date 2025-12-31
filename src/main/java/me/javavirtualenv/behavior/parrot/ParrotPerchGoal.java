package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI goal for parrot perching behavior.
 * Parrots seek high perches, player shoulders, and perform short flights.
 */
public class ParrotPerchGoal extends Goal {
    private final PathfinderMob parrot;
    private final PerchBehavior perchBehavior;
    private final PerchBehavior.PerchConfig config;

    private int perchSearchInterval = 100;
    private int perchSearchTimer = 0;
    private boolean isFlyingToPerch = false;
    private BlockPos targetPerch;

    public ParrotPerchGoal(PathfinderMob parrot,
                          PerchBehavior perchBehavior,
                          PerchBehavior.PerchConfig config) {
        this.parrot = parrot;
        this.perchBehavior = perchBehavior;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!parrot.isAlive()) {
            return false;
        }

        // Don't perch if already passenger (shoulder riding)
        if (parrot.isPassenger()) {
            return false;
        }

        // Don't perch if currently dancing to music
        EcologyComponent component = EcologyComponent.getOrCreate(parrot);
        if (component != null) {
            var danceData = component.getHandleTag("dance");
            if (danceData.getBoolean("is_dancing")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canUse()) {
            return false;
        }

        // Continue if flying to perch or perched
        return isFlyingToPerch || perchBehavior.isPerched();
    }

    @Override
    public void start() {
        perchSearchTimer = 0;
        isFlyingToPerch = false;
        targetPerch = null;
    }

    @Override
    public void stop() {
        isFlyingToPerch = false;
        targetPerch = null;
    }

    @Override
    public void tick() {
        // Try shoulder perching first
        if (perchBehavior.tryPerchOnShoulder()) {
            return;
        }

        // Update perching state
        if (perchBehavior.isPerched()) {
            perchBehavior.tick();
            return;
        }

        // Search for perch periodically
        perchSearchTimer++;
        if (perchSearchTimer >= perchSearchInterval) {
            perchSearchTimer = 0;
            searchForPerch();
        }

        // Fly to perch
        if (isFlyingToPerch && targetPerch != null) {
            updateFlightToPerch();
        }
    }

    /**
     * Searches for a suitable perch.
     */
    private void searchForPerch() {
        // Find a perch
        BlockPos perch = perchBehavior.findPerch();
        if (perch != null) {
            targetPerch = perch;
            isFlyingToPerch = true;
        }
    }

    /**
     * Updates flight toward perch.
     */
    private void updateFlightToPerch() {
        if (targetPerch == null) {
            isFlyingToPerch = false;
            return;
        }

        double distance = parrot.distanceTo(
            targetPerch.getX() + 0.5,
            targetPerch.getY() + 0.5,
            targetPerch.getZ() + 0.5
        );

        // Check if we've arrived
        if (distance <= 2.0) {
            perchBehavior.startPerching(targetPerch, determinePerchType(targetPerch));
            isFlyingToPerch = false;
            targetPerch = null;
            return;
        }

        // Move toward perch
        if (parrot.getNavigation().isDone()) {
            parrot.getNavigation().moveTo(
                targetPerch.getX() + 0.5,
                targetPerch.getY() + 1.0,
                targetPerch.getZ() + 0.5,
                1.0
            );
        }
    }

    /**
     * Determines the type of perch based on position.
     */
    private PerchBehavior.PerchType determinePerchType(BlockPos pos) {
        // Check height
        int height = pos.getY() - parrot.level().getMinBuildHeight();
        if (height >= config.minPerchHeight) {
            return PerchBehavior.PerchType.TREE_BRANCH;
        }

        return PerchBehavior.PerchType.GROUND;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
