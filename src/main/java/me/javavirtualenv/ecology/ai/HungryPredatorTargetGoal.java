package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Simple goal that targets prey when the predator is hungry.
 * Used for wolves, foxes, and other predators to seek food when hungry.
 */
public class HungryPredatorTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    private final Mob predator;
    private final int hungerThreshold;

    public HungryPredatorTargetGoal(Mob predator, Class<T> targetClass, int hungerThreshold) {
        super(predator, targetClass, true);
        this.predator = predator;
        this.hungerThreshold = hungerThreshold;
    }

    public HungryPredatorTargetGoal(Mob predator, Class<T> targetClass, int hungerThreshold, Predicate<LivingEntity> targetPredicate) {
        super(predator, targetClass, 10, true, false, targetPredicate);
        this.predator = predator;
        this.hungerThreshold = hungerThreshold;
    }

    @Override
    public boolean canUse() {
        // Only hunt when hungry
        if (!isHungry()) {
            return false;
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Continue if still hungry
        if (!isHungry()) {
            return false;
        }

        return super.canContinueToUse();
    }

    private boolean isHungry() {
        if (!(predator instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        // Check hunger level from NBT
        var hungerTag = component.getHandleTag("hunger");
        int hungerLevel = hungerTag.contains("hunger") ? hungerTag.getInt("hunger") : 100;

        // Also check state flag
        boolean stateHungry = component.state().isHungry();

        return stateHungry || hungerLevel < hungerThreshold;
    }

    /**
     * Creates a target goal for common prey animals (sheep, pigs, rabbits, chickens).
     */
    public static HungryPredatorTargetGoal<LivingEntity> forCommonPrey(Mob predator, int hungerThreshold) {
        return new HungryPredatorTargetGoal<>(predator, LivingEntity.class, hungerThreshold,
            target -> target instanceof Sheep || target instanceof Pig ||
                      target instanceof Rabbit || target instanceof Chicken);
    }
}
