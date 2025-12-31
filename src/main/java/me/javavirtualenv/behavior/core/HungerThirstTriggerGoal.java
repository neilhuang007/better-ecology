package me.javavirtualenv.behavior.core;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal that monitors hunger and thirst levels and triggers behavioral priority shifts.
 * <p>
 * Scientific research shows hunger and thirst are primary triggers for animal behavior:
 * - When below threshold, animals prioritize finding food/water over other activities
 * - More critical need (lower ratio) takes precedence
 * - Sets state flags that other goals can check for priority adjustments
 * <p>
 * This goal doesn't directly control movement but instead updates entity state
 * to signal to other goals that foraging/drinking should take priority.
 */
public class HungerThirstTriggerGoal extends Goal {
    private static final String NBT_HUNGER = "hunger";
    private static final String NBT_THIRST = "thirst";
    private static final int DEFAULT_HUNGER_THRESHOLD = 50;
    private static final int DEFAULT_THIRST_THRESHOLD = 40;
    private static final int CHECK_INTERVAL = 40;

    private final Mob mob;
    private final int hungerThreshold;
    private final int thirstThreshold;
    private final boolean hungerEnabled;
    private final boolean thirstEnabled;

    private int ticksSinceLastCheck;

    /**
     * Creates a new hunger/thirst trigger goal with configurable thresholds.
     *
     * @param mob The entity to monitor
     * @param hungerThreshold Hunger level below which foraging is prioritized (0-100)
     * @param thirstThreshold Thirst level below which drinking is prioritized (0-100)
     * @param hungerEnabled Whether hunger checking is enabled for this mob
     * @param thirstEnabled Whether thirst checking is enabled for this mob
     */
    public HungerThirstTriggerGoal(Mob mob, int hungerThreshold, int thirstThreshold,
                                   boolean hungerEnabled, boolean thirstEnabled) {
        this.mob = mob;
        this.hungerThreshold = hungerThreshold;
        this.thirstThreshold = thirstThreshold;
        this.hungerEnabled = hungerEnabled;
        this.thirstEnabled = thirstEnabled;
        this.ticksSinceLastCheck = 0;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    /**
     * Creates a new hunger/thirst trigger goal with default thresholds.
     *
     * @param mob The entity to monitor
     */
    public HungerThirstTriggerGoal(Mob mob) {
        this(mob, DEFAULT_HUNGER_THRESHOLD, DEFAULT_THIRST_THRESHOLD, true, true);
    }

    @Override
    public boolean canUse() {
        return mob.isAlive() && (hungerEnabled || thirstEnabled);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        ticksSinceLastCheck++;

        if (ticksSinceLastCheck < CHECK_INTERVAL) {
            return;
        }

        ticksSinceLastCheck = 0;
        updateState();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Updates entity state based on current hunger/thirst levels.
     * Sets flags that other behaviors can check to adjust their priorities.
     */
    private void updateState() {
        if (!(mob instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        EcologyProfile profile = component.profile();
        if (profile == null) {
            return;
        }

        EntityState state = component.state();
        CompoundTag handleData = component.getHandleTag("");

        int hungerMaxValue = profile.getIntFast("hunger", "max_value", 100);
        int thirstMaxValue = profile.getIntFast("thirst", "max_value", 100);

        int currentHunger = hungerEnabled ? getHunger(component, profile) : hungerMaxValue;
        int currentThirst = thirstEnabled ? getThirst(component, profile) : thirstMaxValue;

        double hungerRatio = (double) currentHunger / hungerMaxValue;
        double thirstRatio = (double) currentThirst / thirstMaxValue;

        boolean isHungry = hungerEnabled && currentHunger < hungerThreshold;
        boolean isThirsty = thirstEnabled && currentThirst < thirstThreshold;
        boolean isStarving = hungerEnabled && currentHunger < hungerThreshold / 2;
        boolean isDehydrated = thirstEnabled && currentThirst < thirstThreshold / 2;

        state.setIsHungry(isHungry);
        state.setIsThirsty(isThirsty);
        state.setIsStarving(isStarving);
        state.setIsDehydrated(isDehydrated);

        CompoundTag stateTag = component.getHandleTag("hunger_thirst_state");
        stateTag.putBoolean("needs_food", isHungry);
        stateTag.putBoolean("needs_water", isThirsty);
        stateTag.putBoolean("starving", isStarving);
        stateTag.putBoolean("dehydrated", isDehydrated);
        stateTag.putDouble("hunger_priority", calculateHungerPriority(hungerRatio));
        stateTag.putDouble("thirst_priority", calculateThirstPriority(thirstRatio));

        if (isHungry && isThirsty) {
            prioritizeMoreCriticalNeed(stateTag, hungerRatio, thirstRatio);
        }
    }

    private int getHunger(EcologyComponent component, EcologyProfile profile) {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (!hungerTag.contains(NBT_HUNGER)) {
            return profile.getIntFast("hunger", "starting_value", 100);
        }
        return hungerTag.getInt(NBT_HUNGER);
    }

    private int getThirst(EcologyComponent component, EcologyProfile profile) {
        CompoundTag thirstTag = component.getHandleTag("thirst");
        if (!thirstTag.contains(NBT_THIRST)) {
            return profile.getIntFast("thirst", "starting_value", 100);
        }
        return thirstTag.getInt(NBT_THIRST);
    }

    private double calculateHungerPriority(double hungerRatio) {
        if (hungerRatio >= 0.5) {
            return 0.0;
        }
        return (0.5 - hungerRatio) * 2.0;
    }

    private double calculateThirstPriority(double thirstRatio) {
        if (thirstRatio >= 0.5) {
            return 0.0;
        }
        return (0.5 - thirstRatio) * 2.0;
    }

    private void prioritizeMoreCriticalNeed(CompoundTag stateTag, double hungerRatio, double thirstRatio) {
        boolean thirstIsMoreCritical = thirstRatio < hungerRatio;
        stateTag.putBoolean("thirst_is_critical", thirstIsMoreCritical);
        stateTag.putBoolean("hunger_is_critical", !thirstIsMoreCritical);
    }

    /**
     * Gets whether the mob currently needs food.
     * Other goals can call this to check if they should yield priority.
     */
    public boolean needsFood() {
        if (!(mob instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag stateTag = component.getHandleTag("hunger_thirst_state");
        return stateTag.getBoolean("needs_food");
    }

    /**
     * Gets whether the mob currently needs water.
     * Other goals can call this to check if they should yield priority.
     */
    public boolean needsWater() {
        if (!(mob instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag stateTag = component.getHandleTag("hunger_thirst_state");
        return stateTag.getBoolean("needs_water");
    }

    /**
     * Gets the hunger priority (0.0 to 1.0, higher is more urgent).
     */
    public double getHungerPriority() {
        if (!(mob instanceof EcologyAccess access)) {
            return 0.0;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return 0.0;
        }

        CompoundTag stateTag = component.getHandleTag("hunger_thirst_state");
        return stateTag.getDouble("hunger_priority");
    }

    /**
     * Gets the thirst priority (0.0 to 1.0, higher is more urgent).
     */
    public double getThirstPriority() {
        if (!(mob instanceof EcologyAccess access)) {
            return 0.0;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return 0.0;
        }

        CompoundTag stateTag = component.getHandleTag("hunger_thirst_state");
        return stateTag.getDouble("thirst_priority");
    }

    /**
     * Gets whether thirst is more critical than hunger right now.
     */
    public boolean isThirstMoreCritical() {
        if (!(mob instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag stateTag = component.getHandleTag("hunger_thirst_state");
        return stateTag.getBoolean("thirst_is_critical");
    }

    /**
     * Gets whether hunger is more critical than thirst right now.
     */
    public boolean isHungerMoreCritical() {
        if (!(mob instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag stateTag = component.getHandleTag("hunger_thirst_state");
        return stateTag.getBoolean("hunger_is_critical");
    }
}
