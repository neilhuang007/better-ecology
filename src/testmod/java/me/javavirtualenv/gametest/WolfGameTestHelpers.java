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
        // Use species-specific threshold: wolves=40, foxes=60
        int hungerThreshold = entity instanceof net.minecraft.world.entity.animal.Fox ? 60 : 40;
        setHunger(entity, hunger, hungerThreshold);
    }

    static void setHunger(LivingEntity entity, int hunger, int hungerThreshold) {
        if (!(entity instanceof EcologyAccess access)) {
            System.err.println("[WolfGameTestHelpers] Cannot set hunger - entity does not implement EcologyAccess");
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            System.err.println("[WolfGameTestHelpers] Cannot set hunger - EcologyComponent is null");
            return;
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        hungerTag.putInt("hunger", hunger);

        // Keep entity state in sync for behaviors that consult it
        // Set after NBT to ensure both data sources are aligned
        var state = component.state();
        state.setIsHungry(hunger < hungerThreshold);
        state.setIsStarving(hunger <= 5);

        System.out.println("[WolfGameTestHelpers] Set entity " + entity.getId() + " hunger to " + hunger +
            " (threshold=" + hungerThreshold + "), isHungry=" + state.isHungry() + ", isStarving=" + state.isStarving());
    }

    /**
     * Verify that hunger state is properly set by checking both NBT and state flag.
     * This ensures the hunger system is working correctly for tests.
     */
    static boolean verifyHungerState(LivingEntity entity, int expectedHunger) {
        // Use species-specific threshold: wolves=40, foxes=60
        int hungerThreshold = entity instanceof net.minecraft.world.entity.animal.Fox ? 60 : 40;

        if (!(entity instanceof EcologyAccess access)) {
            System.err.println("[WolfGameTestHelpers] Cannot verify hunger - entity does not implement EcologyAccess");
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            System.err.println("[WolfGameTestHelpers] Cannot verify hunger - EcologyComponent is null");
            return false;
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        int actualHunger = hungerTag.getInt("hunger");

        var state = component.state();
        boolean expectedHungry = expectedHunger < hungerThreshold;
        boolean actualHungry = state.isHungry();

        boolean nbtMatch = actualHunger == expectedHunger;
        boolean stateMatch = actualHungry == expectedHungry;

        if (!nbtMatch) {
            System.err.println("[WolfGameTestHelpers] Hunger NBT mismatch: expected " + expectedHunger + ", got " + actualHunger);
        }
        if (!stateMatch) {
            System.err.println("[WolfGameTestHelpers] Hunger state mismatch: expected hungry=" + expectedHungry + " (threshold=" + hungerThreshold + "), got " + actualHungry);
        }

        return nbtMatch && stateMatch;
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

    static void setThirst(Wolf wolf, int thirst) {
        if (!(wolf instanceof EcologyAccess access)) {
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

    static int getThirst(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
            return -1;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return -1;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        return thirstTag.contains("thirst") ? thirstTag.getInt("thirst") : 100;
    }

    static boolean verifyThirstState(Wolf wolf, int expectedThirst) {
        if (!(wolf instanceof EcologyAccess access)) {
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

    static boolean hasEcologyComponent(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
            return false;
        }
        return access.betterEcology$getEcologyComponent() != null;
    }

    static void printWolfStatus(Wolf wolf, String context) {
        System.out.println("[WolfGameTestHelpers] === Wolf Status (" + context + ") ===");
        System.out.println("  Wolf ID: " + wolf.getId());
        System.out.println("  Implements EcologyAccess: " + (wolf instanceof EcologyAccess));

        if (!(wolf instanceof EcologyAccess access)) {
            System.out.println("  ERROR: Wolf does not implement EcologyAccess");
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        System.out.println("  Has EcologyComponent: " + (component != null));

        if (component == null) {
            System.out.println("  ERROR: EcologyComponent is null");
            return;
        }

        System.out.println("  Thirst: " + getThirst(wolf));
        System.out.println("  Is Thirsty: " + component.state().isThirsty());
    }

    static void setHealth(Wolf wolf, float health) {
        wolf.setHealth(Math.max(1.0f, health));
    }

    static boolean hasStoredItem(Wolf wolf) {
        return wolfHasStoredItem(wolf);
    }

    static void setPackId(Wolf wolf, java.util.UUID packId) {
        if (!(wolf instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        CompoundTag packTag = component.getHandleTag("behavior");
        packTag.putUUID("packId", packId);
    }

    static void giveStoredFood(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        CompoundTag storageTag = component.getHandleTag("wolf_item_storage");
        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("id", "minecraft:beef");
        itemTag.putByte("Count", (byte) 1);
        storageTag.put("carried_item", itemTag);
    }

    static boolean isRetreating(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        return component.state().isRetreating();
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
}
