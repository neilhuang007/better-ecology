package me.javavirtualenv.gametest;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;

final class WolfGameTestHelpers {

    private WolfGameTestHelpers() {
    }

    /**
     * Manually trigger goal registration for game test environment.
     * This is needed because mixin injection may not work properly in game tests.
     */
    static void registerWolfGoals(Wolf wolf) {
        System.out.println("[WolfGameTestHelpers] Manually registering wolf goals for " + wolf.getId());
        EcologyHooks.onRegisterGoals(wolf);
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

        // Keep entity state in sync for behaviors that consult it
        // Set after NBT to ensure both data sources are aligned
        var state = component.state();
        state.setIsHungry(hunger < 50);
        state.setIsStarving(hunger <= 5);
    }

    /**
     * Verify that hunger state is properly set by checking both NBT and state flag.
     * This ensures the hunger system is working correctly for tests.
     */
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

    static boolean wolfHasStoredItem(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag storageTag = component.getHandleTag("wolf_item_storage");
        return storageTag.contains("carried_item");
    }

    static void enableAi(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    static boolean wolfPickedUpItem(Wolf wolf, ItemEntity itemEntity) {
        if (wolfHasStoredItem(wolf)) {
            return true;
        }
        return itemEntity == null || !itemEntity.isAlive();
    }
}
