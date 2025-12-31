package me.javavirtualenv.behavior.fox;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal for fox sleeping behavior.
 * <p>
 * Foxes sleep during the day in sheltered areas.
 */
public class FoxSleepGoal extends Goal {

    private final PathfinderMob fox;
    private final FoxSleepingBehavior sleepingBehavior;

    public FoxSleepGoal(PathfinderMob fox, FoxSleepingBehavior sleepingBehavior) {
        this.fox = fox;
        this.sleepingBehavior = sleepingBehavior;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Check if it's daytime
        long dayTime = fox.level().getDayTime() % 24000;
        boolean isDay = dayTime >= 23000 || dayTime < 13500;

        if (!isDay) {
            return false;
        }

        // Check if already sleeping
        if (sleepingBehavior.isSleeping()) {
            return true;
        }

        // Check if has a sleep position
        return sleepingBehavior.getSleepPosition() != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue sleeping if it's still day
        long dayTime = fox.level().getDayTime() % 24000;
        boolean isDay = dayTime >= 23000 || dayTime < 13500;

        return isDay && sleepingBehavior.isSleeping();
    }

    @Override
    public void start() {
        // Set vanilla fox to sleeping
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            minecraftFox.setSleeping(true);
        }

        sleepingBehavior.setSleeping(true);
    }

    @Override
    public void stop() {
        // Wake up vanilla fox
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            minecraftFox.setSleeping(false);
        }

        sleepingBehavior.setSleeping(false);
    }

    @Override
    public void tick() {
        // Stop all movement while sleeping
        fox.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
