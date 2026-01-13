package me.javavirtualenv.gametest;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Pig;

final class PigGameTestHelpers {

    private PigGameTestHelpers() {
    }

    static void setHunger(Pig pig, int hunger) {
        if (!(pig instanceof EcologyAccess access)) {
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

    static boolean verifyHungerState(Pig pig, int expectedHunger) {
        if (!(pig instanceof EcologyAccess access)) {
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

    static void setThirst(Pig pig, int thirst) {
        if (!(pig instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        thirstTag.putInt("thirst", thirst);

        var state = component.state();
        state.setIsThirsty(thirst < 30);
    }

    static int getThirst(Pig pig) {
        if (!(pig instanceof EcologyAccess access)) {
            return -1;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return -1;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        return thirstTag.contains("thirst") ? thirstTag.getInt("thirst") : 100;
    }

    static boolean verifyThirstState(Pig pig, int expectedThirst) {
        if (!(pig instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        int actualThirst = thirstTag.getInt("thirst");

        var state = component.state();
        boolean expectedThirsty = expectedThirst < 30;
        boolean actualThirsty = state.isThirsty();

        return actualThirst == expectedThirst && actualThirsty == expectedThirsty;
    }

    static void setHealth(Pig pig, float health) {
        pig.setHealth(Math.max(1.0f, health));
    }

    static boolean isRetreating(Pig pig) {
        if (!(pig instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        return component.state().isRetreating();
    }
}
