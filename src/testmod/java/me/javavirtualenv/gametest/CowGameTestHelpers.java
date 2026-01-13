package me.javavirtualenv.gametest;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.ai.BullCompetitionGoal;
import me.javavirtualenv.ecology.ai.CalfFollowMotherGoal;
import me.javavirtualenv.ecology.ai.CowCudChewGoal;
import me.javavirtualenv.ecology.ai.CowGrazeGoal;
import me.javavirtualenv.ecology.ai.CowProtectCalfGoal;
import me.javavirtualenv.ecology.ai.HerdCohesionGoal;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.ai.SeekWaterGoal;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Cow;

final class CowGameTestHelpers {

    private CowGameTestHelpers() {
    }

    /**
     * Manually register cow goals for game test environment.
     * This is needed because mixin injection may not work properly in game tests.
     */
    static void registerCowGoals(Cow cow) {
        System.out.println("[CowGameTestHelpers] Manually registering cow goals for " + cow.getId());

        // First, call the standard ecology hooks registration
        EcologyHooks.onRegisterGoals(cow);

        // Then manually register the goals that CowMixin would register
        // This is necessary because the mixin may not be applied in test environment
        if (cow instanceof net.minecraft.world.entity.Mob mob) {
            var accessor = (me.javavirtualenv.mixin.MobAccessor) mob;

            System.out.println("[CowGameTestHelpers] Registering cow behavior goals...");

            // Priority 0: Float
            accessor.betterEcology$getGoalSelector().addGoal(0, new FloatGoal(cow));

            // Priority 1: Low health flee (critical survival)
            accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(cow, 0.60, 1.2));

            // Priority 2: Protect calf (highest after floating)
            accessor.betterEcology$getGoalSelector().addGoal(2, new CowProtectCalfGoal(cow, 16.0, 24.0));

            // Priority 3: Bull competition (for adults)
            if (!cow.isBaby()) {
                accessor.betterEcology$getGoalSelector().addGoal(3, new BullCompetitionGoal(cow, 20.0));
            }

            // Priority 4: Seek water when thirsty
            accessor.betterEcology$getGoalSelector().addGoal(4, new SeekWaterGoal(cow, 1.0, 16));

            // Priority 5: Breed (using vanilla breeding goal with our constraints)
            accessor.betterEcology$getGoalSelector().addGoal(5, new BreedGoal(cow, 1.0));

            // Priority 6: Grazing
            accessor.betterEcology$getGoalSelector().addGoal(6, new CowGrazeGoal(cow, 16.0, 0.8));

            // Priority 7: Herd cohesion (adults) or follow mother (calves)
            if (cow.isBaby()) {
                accessor.betterEcology$getGoalSelector().addGoal(7, new CalfFollowMotherGoal(cow, 24.0, 1.0));
            } else {
                accessor.betterEcology$getGoalSelector().addGoal(7, new HerdCohesionGoal(cow, 24.0, 0.8));
            }

            // Priority 8: Cud chewing (idle behavior)
            accessor.betterEcology$getGoalSelector().addGoal(8, new CowCudChewGoal(cow));

            // Priority 9: Random stroll (fallback)
            accessor.betterEcology$getGoalSelector().addGoal(9, new WaterAvoidingRandomStrollGoal(cow, 0.6));

            System.out.println("[CowGameTestHelpers] Successfully registered all cow goals");
            System.out.println("[CowGameTestHelpers] Goal selector goal count: " +
                accessor.betterEcology$getGoalSelector().getAvailableGoals().size());
        }
    }

    static void setHunger(Cow cow, int hunger) {
        if (!(cow instanceof EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        hungerTag.putInt("hunger", hunger);

        var state = component.state();
        // Use threshold of 60 to match CowMixin.CowHungerHandle
        state.setIsHungry(hunger < 60);
        state.setIsStarving(hunger <= 5);

        System.out.println("[CowGameTestHelpers] Set cow " + cow.getId() + " hunger to " + hunger +
            ", isHungry=" + state.isHungry() + ", isStarving=" + state.isStarving());
    }

    static boolean verifyHungerState(Cow cow, int expectedHunger) {
        if (!(cow instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag hungerTag = component.getHandleTag("hunger");
        int actualHunger = hungerTag.getInt("hunger");

        var state = component.state();
        // Use threshold of 60 to match CowMixin.CowHungerHandle
        boolean expectedHungry = expectedHunger < 60;
        boolean actualHungry = state.isHungry();

        return actualHunger == expectedHunger && actualHungry == expectedHungry;
    }

    static void setThirst(Cow cow, int thirst) {
        if (!(cow instanceof EcologyAccess access)) {
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

        System.out.println("[CowGameTestHelpers] Set cow " + cow.getId() + " thirst to " + thirst +
            ", isThirsty=" + state.isThirsty());
    }

    static int getThirst(Cow cow) {
        if (!(cow instanceof EcologyAccess access)) {
            return -1;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return -1;
        }

        CompoundTag thirstTag = component.getHandleTag("thirst");
        return thirstTag.contains("thirst") ? thirstTag.getInt("thirst") : 100;
    }

    static boolean verifyThirstState(Cow cow, int expectedThirst) {
        if (!(cow instanceof EcologyAccess access)) {
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

    static void setHealth(Cow cow, float health) {
        cow.setHealth(Math.max(1.0f, health));
    }

    static boolean isRetreating(Cow cow) {
        if (!(cow instanceof EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        return component.state().isRetreating();
    }
}
