package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.mixin.animal.FoxAccessor;
import net.minecraft.nbt.CompoundTag;
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
    private static final int VERY_HUNGRY_THRESHOLD = 60; // Match FoxHuntGoal and FoxMixin threshold

    public FoxSleepGoal(PathfinderMob fox, FoxSleepingBehavior sleepingBehavior) {
        this.fox = fox;
        this.sleepingBehavior = sleepingBehavior;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Check if it's daytime (0-13000 is day, 13000-23000 is night)
        long dayTime = fox.level().getDayTime() % 24000;
        boolean isDay = dayTime >= 0 && dayTime < 13000;

        if (!isDay) {
            return false;
        }

        // Don't sleep if very hungry - prioritize hunting
        if (isVeryHungry()) {
            return false;
        }

        // Can start sleeping during the day
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue sleeping if it's still day
        long dayTime = fox.level().getDayTime() % 24000;
        boolean isDay = dayTime >= 0 && dayTime < 13000;

        if (!isDay) {
            return false;
        }

        // Wake up if very hungry
        if (isVeryHungry()) {
            return false;
        }

        return sleepingBehavior.isSleeping();
    }

    private boolean isVeryHungry() {
        int hunger = getHungerLevel();
        return hunger < VERY_HUNGRY_THRESHOLD;
    }

    private int getHungerLevel() {
        if (!(fox instanceof EcologyAccess access)) {
            return 100;
        }
        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return 100;
        }
        CompoundTag tag = component.getHandleTag("hunger");
        return tag.contains("hunger") ? tag.getInt("hunger") : 100;
    }

    @Override
    public void start() {
        // Set vanilla fox to sleeping
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            ((FoxAccessor) minecraftFox).betterEcology$setSleeping(true);
        }

        sleepingBehavior.setSleeping(true);
    }

    @Override
    public void stop() {
        // Wake up vanilla fox
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            ((FoxAccessor) minecraftFox).betterEcology$setSleeping(false);
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
