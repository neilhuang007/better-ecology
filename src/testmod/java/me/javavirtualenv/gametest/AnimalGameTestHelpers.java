package me.javavirtualenv.gametest;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

/**
 * Generic helper methods for animal behavior game tests.
 * Provides common functionality for setting hunger, thirst, and other ecology states.
 */
final class AnimalGameTestHelpers {

    private AnimalGameTestHelpers() {
    }

    /**
     * Manually trigger goal registration for game test environment.
     * This is needed because mixin injection may not work properly in game tests.
     */
    static void registerGoals(Mob mob) {
        System.out.println("[AnimalGameTestHelpers] Manually registering goals for " + mob.getId());
        EcologyHooks.onRegisterGoals(mob);
    }

    static void enableAi(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    static void setHunger(LivingEntity entity, int hunger) {
        if (!(entity instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        hungerTag.putInt("hunger", hunger);

        var state = component.state();
        state.setIsHungry(hunger < 50);
        state.setIsStarving(hunger <= 5);
    }

    static int getHunger(LivingEntity entity) {
        if (!(entity instanceof EcologyAccess access)) {
            return -1;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return -1;
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        return hungerTag.contains("hunger") ? hungerTag.getInt("hunger") : 100;
    }

    static boolean verifyHungerState(LivingEntity entity, int expectedHunger) {
        if (!(entity instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        int actualHunger = hungerTag.getInt("hunger");

        var state = component.state();
        boolean expectedHungry = expectedHunger < 50;
        boolean actualHungry = state.isHungry();

        return actualHunger == expectedHunger && actualHungry == expectedHungry;
    }

    static void setThirst(LivingEntity entity, int thirst) {
        if (!(entity instanceof EcologyAccess access)) {
            System.err.println("[AnimalGameTestHelpers] Cannot set thirst - entity does not implement EcologyAccess");
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            System.err.println("[AnimalGameTestHelpers] Cannot set thirst - EcologyComponent is null");
            return;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        thirstTag.putInt("thirst", thirst);

        var state = component.state();
        state.setIsThirsty(thirst < 30);

        System.out.println("[AnimalGameTestHelpers] Set entity " + entity.getId() + " thirst to " + thirst);
    }

    static int getThirst(LivingEntity entity) {
        if (!(entity instanceof EcologyAccess access)) {
            return -1;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return -1;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        return thirstTag.contains("thirst") ? thirstTag.getInt("thirst") : 100;
    }

    static boolean verifyThirstState(LivingEntity entity, int expectedThirst) {
        if (!(entity instanceof EcologyAccess access)) {
            System.err.println("[AnimalGameTestHelpers] Cannot verify thirst - entity does not implement EcologyAccess");
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            System.err.println("[AnimalGameTestHelpers] Cannot verify thirst - EcologyComponent is null");
            return false;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        int actualThirst = thirstTag.getInt("thirst");

        var state = component.state();
        boolean expectedThirsty = expectedThirst < 30;
        boolean actualThirsty = state.isThirsty();

        boolean nbtMatch = actualThirst == expectedThirst;
        boolean stateMatch = actualThirsty == expectedThirsty;

        if (!nbtMatch) {
            System.err.println("[AnimalGameTestHelpers] Thirst NBT mismatch: expected " + expectedThirst + ", got " + actualThirst);
        }
        if (!stateMatch) {
            System.err.println("[AnimalGameTestHelpers] Thirst state mismatch: expected thirsty=" + expectedThirsty + ", got " + actualThirsty);
        }

        return nbtMatch && stateMatch;
    }

    static void setHealth(LivingEntity entity, float health) {
        entity.setHealth(Math.max(1.0f, health));
        System.out.println("[AnimalGameTestHelpers] Set entity " + entity.getId() + " health to " + health);
    }

    static boolean hasEcologyComponent(LivingEntity entity) {
        if (!(entity instanceof EcologyAccess access)) {
            return false;
        }
        return access.betterEcology$getEcologyComponent() != null;
    }

    static boolean isRetreating(LivingEntity entity) {
        if (!(entity instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        return component.state().isRetreating();
    }

    static boolean isFleeing(LivingEntity entity) {
        if (!(entity instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        return component.state().isFleeing();
    }

    static void printStatus(LivingEntity entity, String context) {
        System.out.println("[AnimalGameTestHelpers] === Entity Status (" + context + ") ===");
        System.out.println("  Entity ID: " + entity.getId());
        System.out.println("  Implements EcologyAccess: " + (entity instanceof EcologyAccess));

        if (!(entity instanceof EcologyAccess access)) {
            System.out.println("  ERROR: Entity does not implement EcologyAccess - mixin not applied");
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        System.out.println("  Has EcologyComponent: " + (component != null));

        if (component == null) {
            System.out.println("  ERROR: EcologyComponent is null");
            return;
        }

        System.out.println("  Handle count: " + component.handles().size());
        System.out.println("  Hunger: " + getHunger(entity));
        System.out.println("  Is Hungry: " + component.state().isHungry());
        System.out.println("  Thirst: " + getThirst(entity));
        System.out.println("  Is Thirsty: " + component.state().isThirsty());
    }
}
