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

}
