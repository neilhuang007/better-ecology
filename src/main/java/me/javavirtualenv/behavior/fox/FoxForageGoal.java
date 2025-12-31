package me.javavirtualenv.behavior.fox;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal for fox berry foraging behavior.
 * <p>
 * Foxes seek out sweet berry bushes and eat berries.
 * Higher priority when hungry.
 */
public class FoxForageGoal extends Goal {

    private final PathfinderMob fox;
    private final FoxBerryForagingBehavior foragingBehavior;
    private final int priority;

    public FoxForageGoal(PathfinderMob fox, FoxBerryForagingBehavior foragingBehavior, int priority) {
        this.fox = fox;
        this.foragingBehavior = foragingBehavior;
        this.priority = priority;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Don't forage if sleeping
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            if (minecraftFox.isSleeping()) {
                return false;
            }
        }

        // Check if there's a berry bush nearby
        return foragingBehavior.getTargetBerryBush() != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue if currently eating or have target
        return foragingBehavior.isEating() ||
               (foragingBehavior.getTargetBerryBush() != null);
    }

    @Override
    public void start() {
        // Movement is handled by the behavior
    }

    @Override
    public void stop() {
        if (!foragingBehavior.isEating()) {
            foragingBehavior.setTargetBerryBush(null);
        }
    }

    @Override
    public void tick() {
        // Look at berry bush if targeted
        if (foragingBehavior.getTargetBerryBush() != null) {
            double x = foragingBehavior.getTargetBerryBush().getX() + 0.5;
            double y = foragingBehavior.getTargetBerryBush().getY() + 0.5;
            double z = foragingBehavior.getTargetBerryBush().getZ() + 0.5;
            fox.getLookControl().setLookAt(x, y, z, 30.0f, 30.0f);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
