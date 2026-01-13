package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.WolfBehaviorHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Simple goal that targets prey when the predator is hungry.
 * Used for wolves, foxes, and other predators to seek food when hungry.
 * For wolves, coordinates pack hunting by sharing targets.
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

        // Wake up fox if sleeping and hungry (hunger overrides sleep)
        if (predator instanceof net.minecraft.world.entity.animal.Fox fox && fox.isSleeping()) {
            wakeUpFox(fox);
        }

        // For wolves, check if pack members are already hunting
        if (predator instanceof Wolf wolf) {
            LivingEntity packTarget = getPackTarget(wolf);
            if (packTarget != null && packTarget.isAlive()) {
                // Join pack hunt
                predator.setTarget(packTarget);
                return true;
            }
        }

        // Find own target using parent's logic
        boolean canUse = super.canUse();

        // If super.canUse() succeeded, we found a target - set it now and share with pack
        if (canUse && predator instanceof Wolf wolf) {
            // The target field is set by super.canUse() but setTarget isn't called until start()
            // We need to set it explicitly for pack sharing to work
            if (this.target != null) {
                predator.setTarget(this.target);
                shareTargetWithPack(wolf, this.target);
            }
        }

        return canUse;
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
     * Get pack's shared target for coordinated hunting.
     */
    private LivingEntity getPackTarget(Wolf wolf) {
        // Find pack members
        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(
            Wolf.class,
            wolf.getBoundingBox().inflate(32.0)
        );

        for (Wolf packMember : nearbyWolves) {
            if (packMember.equals(wolf) || packMember.isTame()) {
                continue;
            }

            // Check if same pack
            if (!WolfBehaviorHandle.isSamePack(wolf, packMember)) {
                continue;
            }

            // Check if pack member has a target
            LivingEntity target = packMember.getTarget();
            if (target != null && target.isAlive()) {
                // Found pack member with target - join the hunt
                return target;
            }
        }

        return null;
    }

    /**
     * Share this wolf's target with pack members so they coordinate hunting.
     */
    private void shareTargetWithPack(Wolf wolf, LivingEntity target) {
        // Find hungry pack members
        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(
            Wolf.class,
            wolf.getBoundingBox().inflate(32.0)
        );

        for (Wolf packMember : nearbyWolves) {
            if (packMember.equals(wolf) || packMember.isTame()) {
                continue;
            }

            // Check if same pack
            if (!WolfBehaviorHandle.isSamePack(wolf, packMember)) {
                continue;
            }

            // Check if pack member is hungry and doesn't have a target
            if (WolfBehaviorHandle.isHungry(packMember) && packMember.getTarget() == null) {
                // Set same target for coordinated pack hunting
                packMember.setTarget(target);
            }
        }
    }

    /**
     * Wake up a fox if it's sleeping (hunger overrides sleep).
     */
    private void wakeUpFox(net.minecraft.world.entity.animal.Fox fox) {
        try {
            // Use accessor to wake up the fox
            if (fox instanceof me.javavirtualenv.mixin.animal.FoxAccessor foxAccessor) {
                foxAccessor.betterEcology$setSleeping(false);
            }
        } catch (Exception e) {
            // If accessor fails, just continue - not critical
        }
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
