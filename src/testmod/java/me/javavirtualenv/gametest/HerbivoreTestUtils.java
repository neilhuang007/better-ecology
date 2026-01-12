package me.javavirtualenv.gametest;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.EntityType;

/** Utility helpers shared across Better Ecology game tests. */
public final class HerbivoreTestUtils {
    private HerbivoreTestUtils() {
    }

    public static <T extends Mob> T spawnMob(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        return helper.spawnWithNoFreeWill(type, pos);
    }

    public static <T extends Mob> T spawnMobWithAi(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T mob = type.create(helper.getLevel());
        if (mob == null) {
            throw new IllegalStateException("Failed to create mob instance");
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    public static Cow spawnCowWithAi(GameTestHelper helper, BlockPos pos) {
        return spawnMobWithAi(helper, EntityType.COW, pos);
    }

    public static EcologyComponent getComponent(Mob mob) {
        if (!(mob instanceof EcologyAccess access)) {
            throw new IllegalStateException("Mob does not expose ecology component");
        }
        return access.betterEcology$getEcologyComponent();
    }

    public static void setHandleInt(Mob mob, String handleId, String key, int value) {
        EcologyComponent component = getComponent(mob);
        component.getHandleTag(handleId).putInt(key, value);
    }

    public static boolean isThirsty(Mob mob) {
        return getComponent(mob).state().isThirsty();
    }

    public static boolean isRetreating(Mob mob) {
        return getComponent(mob).state().isRetreating();
    }

    public static void boostNavigation(Mob mob, double speed) {
        PathNavigation navigation = mob.getNavigation();
        if (navigation instanceof GroundPathNavigation groundNav) {
            groundNav.setSpeedModifier(speed);
        }
        var movement = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && movement.getAttribute() instanceof RangedAttribute ranged) {
            double clamped = Math.min(ranged.getMaxValue(), speed);
            movement.setBaseValue(clamped);
        }
    }

    /**
     * Lower mob's health to a percentage of max health.
     * Useful for triggering flee behaviors that require low health.
     *
     * @param mob The mob to damage
     * @param healthPercent Target health percentage (0.0 to 1.0)
     */
    public static void setHealthPercent(Mob mob, float healthPercent) {
        float targetHealth = mob.getMaxHealth() * healthPercent;
        mob.setHealth(Math.max(1.0f, targetHealth));
    }

    /**
     * Set the hungry state on the entity state directly.
     * This ensures behaviors that check isHungry() will trigger.
     */
    public static void setHungryState(Mob mob, boolean hungry) {
        getComponent(mob).state().setIsHungry(hungry);
    }

    /**
     * Set the thirsty state on the entity state directly.
     * This ensures behaviors that check isThirsty() will trigger.
     */
    public static void setThirstyState(Mob mob, boolean thirsty) {
        getComponent(mob).state().setIsThirsty(thirsty);
    }

    /**
     * Set the retreating state on the entity state directly.
     * Useful for testing flee verification without waiting for goal to trigger.
     */
    public static void setRetreatingState(Mob mob, boolean retreating) {
        getComponent(mob).state().setIsRetreating(retreating);
    }

    /**
     * Set hunger value and update entity state.
     * Uses threshold of 50 for sheep to match SheepHungerHandle.
     * For other animals, threshold may vary (wolves use 40, foxes use 60).
     */
    public static void setHunger(Mob mob, int hunger) {
        EcologyComponent component = getComponent(mob);
        component.getHandleTag("hunger").putInt("hunger", hunger);

        // Determine threshold based on entity type
        int hungerThreshold = 50; // Default for sheep
        if (mob instanceof net.minecraft.world.entity.animal.Wolf) {
            hungerThreshold = 40; // Wolves use 40
        } else if (mob instanceof net.minecraft.world.entity.animal.Fox) {
            hungerThreshold = 60; // Foxes use 60
        }

        component.state().setIsHungry(hunger < hungerThreshold);
        component.state().setIsStarving(hunger <= 5);
    }

    /**
     * Get current hunger value.
     */
    public static int getHunger(Mob mob) {
        EcologyComponent component = getComponent(mob);
        return component.getHandleTag("hunger").getInt("hunger");
    }

    /**
     * Verify hunger state matches expected value.
     * Uses appropriate threshold for each animal type (wolves: 40, sheep: 50, foxes: 60).
     */
    public static boolean verifyHungerState(Mob mob, int expectedHunger) {
        EcologyComponent component = getComponent(mob);
        int actualHunger = component.getHandleTag("hunger").getInt("hunger");

        // Determine threshold based on entity type
        int hungerThreshold = 50; // Default for sheep
        if (mob instanceof net.minecraft.world.entity.animal.Wolf) {
            hungerThreshold = 40; // Wolves use 40
        } else if (mob instanceof net.minecraft.world.entity.animal.Fox) {
            hungerThreshold = 60; // Foxes use 60
        }

        boolean expectedHungry = expectedHunger < hungerThreshold;
        boolean actualHungry = component.state().isHungry();
        return actualHunger == expectedHunger && actualHungry == expectedHungry;
    }

    /**
     * Set thirst value and update entity state.
     */
    public static void setThirst(Mob mob, int thirst) {
        EcologyComponent component = getComponent(mob);
        component.getHandleTag("thirst").putInt("thirst", thirst);
        component.state().setIsThirsty(thirst < 30);
    }

    /**
     * Get current thirst value.
     */
    public static int getThirst(Mob mob) {
        EcologyComponent component = getComponent(mob);
        var thirstTag = component.getHandleTag("thirst");
        return thirstTag.contains("thirst") ? thirstTag.getInt("thirst") : 100;
    }

    /**
     * Verify thirst state matches expected value.
     */
    public static boolean verifyThirstState(Mob mob, int expectedThirst) {
        EcologyComponent component = getComponent(mob);
        int actualThirst = component.getHandleTag("thirst").getInt("thirst");
        boolean expectedThirsty = expectedThirst < 30;
        boolean actualThirsty = component.state().isThirsty();
        return actualThirst == expectedThirst && actualThirsty == expectedThirsty;
    }

    /**
     * Check if mob is hungry.
     */
    public static boolean isHungry(Mob mob) {
        return getComponent(mob).state().isHungry();
    }

    /**
     * Check if mob is starving.
     */
    public static boolean isStarving(Mob mob) {
        return getComponent(mob).state().isStarving();
    }

    /**
     * Reset the egg laying cooldown for chickens.
     * Sets the cooldown to 0 so the chicken is ready to lay an egg.
     * Also sets eggLayTime to 1 to trigger immediate egg laying on next tick.
     */
    public static void resetEggLayCooldown(Mob mob) {
        EcologyComponent component = getComponent(mob);
        component.getHandleTag("breeding").putInt("egg_lay_cooldown", 0);
    }

    /**
     * Set the egg laying cooldown for chickens.
     * @param mob The mob (should be a chicken)
     * @param cooldown The cooldown in ticks
     */
    public static void setEggLayCooldown(Mob mob, int cooldown) {
        EcologyComponent component = getComponent(mob);
        component.getHandleTag("breeding").putInt("egg_lay_cooldown", cooldown);
    }

    /**
     * Force an immediate egg lay on the next tick by directly spawning an egg.
     * This bypasses the normal nesting behavior for testing purposes.
     * @param chicken The chicken to lay an egg
     */
    public static void forceEggLay(net.minecraft.world.entity.animal.Chicken chicken) {
        net.minecraft.world.entity.Entity egg = chicken.spawnAtLocation(net.minecraft.world.item.Items.EGG);
        if (egg != null) {
            egg.setDeltaMovement(
                (chicken.level().getRandom().nextFloat() - 0.5) * 0.1,
                chicken.level().getRandom().nextFloat() * 0.1,
                (chicken.level().getRandom().nextFloat() - 0.5) * 0.1
            );
        }
    }

    /**
     * Manually register SeekWaterGoal for a pig in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerPigSeekWaterGoal(net.minecraft.world.entity.animal.Pig pig) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) pig;
        accessor.betterEcology$getGoalSelector().addGoal(2,
            new me.javavirtualenv.ecology.ai.SeekWaterGoal(pig, 1.0, 16));
    }

    /**
     * Manually register SeekWaterGoal for a chicken in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerChickenSeekWaterGoal(net.minecraft.world.entity.animal.Chicken chicken) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) chicken;
        accessor.betterEcology$getGoalSelector().addGoal(2,
            new me.javavirtualenv.ecology.ai.SeekWaterGoal(chicken, 1.0, 16));
    }

    /**
     * Manually register SeekWaterGoal for a sheep in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerSheepSeekWaterGoal(net.minecraft.world.entity.animal.Sheep sheep) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) sheep;
        accessor.betterEcology$getGoalSelector().addGoal(3,
            new me.javavirtualenv.ecology.ai.SeekWaterGoal(sheep, 1.0, 16));
    }

    /**
     * Manually register SeekWaterGoal for a rabbit in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerRabbitSeekWaterGoal(net.minecraft.world.entity.animal.Rabbit rabbit) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) rabbit;
        accessor.betterEcology$getGoalSelector().addGoal(3,
            new me.javavirtualenv.ecology.ai.SeekWaterGoal(rabbit, 1.2, 16));
    }

    /**
     * Manually register SeekWaterGoal for a fox in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerFoxSeekWaterGoal(net.minecraft.world.entity.animal.Fox fox) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) fox;
        accessor.betterEcology$getGoalSelector().addGoal(2,
            new me.javavirtualenv.ecology.ai.SeekWaterGoal(fox, 1.0, 16));
    }

    /**
     * Manually register SeekWaterGoal for a cow in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerCowSeekWaterGoal(net.minecraft.world.entity.animal.Cow cow) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) cow;
        accessor.betterEcology$getGoalSelector().addGoal(4,
            new me.javavirtualenv.ecology.ai.SeekWaterGoal(cow, 1.0, 16));
    }

    /**
     * Manually register CowGrazeGoal for a cow in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerCowGrazeGoal(net.minecraft.world.entity.animal.Cow cow) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) cow;
        accessor.betterEcology$getGoalSelector().addGoal(6,
            new me.javavirtualenv.ecology.ai.CowGrazeGoal(cow, 16.0, 0.8));
    }

    /**
     * Manually register HerdCohesionGoal for a cow in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerCowHerdCohesionGoal(net.minecraft.world.entity.animal.Cow cow) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) cow;
        accessor.betterEcology$getGoalSelector().addGoal(7,
            new me.javavirtualenv.ecology.ai.HerdCohesionGoal(cow, 24.0, 0.8));
    }

    /**
     * Manually register all cow goals in game test environment.
     * Delegates to CowGameTestHelpers for comprehensive goal registration.
     */
    public static void registerAllCowGoals(net.minecraft.world.entity.animal.Cow cow) {
        CowGameTestHelpers.registerCowGoals(cow);
    }

    /**
     * Manually register SheepGrazeGoal for a sheep in game test environment.
     * This ensures the goal is present even if mixins don't fire properly during tests.
     */
    public static void registerSheepGrazeGoal(net.minecraft.world.entity.animal.Sheep sheep) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) sheep;
        accessor.betterEcology$getGoalSelector().addGoal(4,
            new me.javavirtualenv.ecology.ai.SheepGrazeGoal(sheep, 16.0, 0.8));
    }

    /**
     * Manually register RabbitBehaviorGoal for a rabbit in game test environment.
     * This ensures the rabbit has evasion, thumping, and foraging behaviors.
     */
    public static void registerRabbitBehaviorGoal(net.minecraft.world.entity.animal.Rabbit rabbit) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) rabbit;
        EcologyComponent component = getComponent(rabbit);

        // Register the primary behavior goal with all rabbit behaviors
        me.javavirtualenv.behavior.rabbit.RabbitBehaviorGoal rabbitGoal =
            new me.javavirtualenv.behavior.rabbit.RabbitBehaviorGoal(rabbit, component);
        accessor.betterEcology$getGoalSelector().addGoal(2, rabbitGoal);
    }

    /**
     * Manually register LowHealthFleeGoal for a rabbit in game test environment.
     * This ensures the rabbit flees when health is low.
     */
    public static void registerRabbitFleeGoal(net.minecraft.world.entity.animal.Rabbit rabbit) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) rabbit;
        accessor.betterEcology$getGoalSelector().addGoal(1,
            new me.javavirtualenv.ecology.ai.LowHealthFleeGoal(rabbit, 0.85, 1.6));
    }

}
