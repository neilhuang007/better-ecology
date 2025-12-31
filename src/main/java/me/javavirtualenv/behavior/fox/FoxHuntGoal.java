package me.javavirtualenv.behavior.fox;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal for fox hunting behavior.
 * <p>
 * Integrates the FoxPursuitBehavior with Minecraft's goal system.
 * Foxes will stalk, crouch, pounce on, and attack prey.
 */
public class FoxHuntGoal extends Goal {

    private final PathfinderMob fox;
    private final FoxPursuitBehavior pursuitBehavior;
    private final double speedModifier;

    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 20; // Update every second

    public FoxHuntGoal(PathfinderMob fox, FoxPursuitBehavior pursuitBehavior, double speedModifier) {
        this.fox = fox;
        this.pursuitBehavior = pursuitBehavior;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only hunt when not sleeping
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            if (minecraftFox.isSleeping()) {
                return false;
            }
        }

        // Check if has prey target
        return pursuitBehavior.getCurrentPrey() != null &&
               pursuitBehavior.getCurrentPrey().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        // Continue hunting while in pursuit or attacking state
        FoxPursuitBehavior.HuntingState state = pursuitBehavior.getCurrentState();

        if (state == FoxPursuitBehavior.HuntingState.IDLE) {
            return false;
        }

        // Check if prey is still valid
        if (pursuitBehavior.getCurrentPrey() == null ||
            !pursuitBehavior.getCurrentPrey().isAlive()) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        lastUpdateTime = 0;
    }

    @Override
    public void stop() {
        // Reset pursuit state when stopping
        if (pursuitBehavior.getCurrentState() == FoxPursuitBehavior.HuntingState.IDLE) {
            pursuitBehavior.setCurrentState(FoxPursuitBehavior.HuntingState.IDLE);
        }
    }

    @Override
    public void tick() {
        long currentTime = fox.level().getGameTime();

        // Limit updates to improve performance
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return;
        }

        lastUpdateTime = currentTime;

        // Look at prey
        if (pursuitBehavior.getCurrentPrey() != null) {
            fox.getLookControl().setLookAt(pursuitBehavior.getCurrentPrey(), 30.0f, 30.0f);
        }

        // Handle pouncing state
        if (pursuitBehavior.getCurrentState() == FoxPursuitBehavior.HuntingState.POUNCING) {
            fox.setSpeed(speedModifier * 1.5);
        } else if (pursuitBehavior.getCurrentState() == FoxPursuitBehavior.HuntingState.CROUCHING) {
            fox.setSpeed(speedModifier * 0.2);
        } else {
            fox.setSpeed(speedModifier);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
