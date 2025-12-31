package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.AgeHandle;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;

/**
 * Extended breeding goal that enforces ecological requirements:
 * - Minimum age (maturity)
 * - Minimum health percentage
 * - Minimum body condition
 * - Breeding cooldown period
 */
public class EcologyBreedGoal extends BreedGoal {
    private final int minAge;
    private final double minHealth;
    private final int minCondition;
    private final int cooldownTicks;

    public EcologyBreedGoal(Animal animal, double speedModifier,
                           int minAge, double minHealth, int minCondition, int cooldownTicks) {
        super(animal, speedModifier);
        this.minAge = minAge;
        this.minHealth = minHealth;
        this.minCondition = minCondition;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }
        return meetsBreedingRequirements(this.animal);
    }

    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse()) {
            return false;
        }
        return meetsBreedingRequirements(this.animal);
    }

    private boolean meetsBreedingRequirements(Animal animal) {
        if (!checkHealthRequirement(animal)) {
            return false;
        }

        EcologyComponent component = getEcologyComponent(animal);
        if (component == null || !component.hasProfile()) {
            return true;
        }

        if (!checkAgeRequirement(component)) {
            return false;
        }

        if (!checkConditionRequirement(component)) {
            return false;
        }

        return true;
    }

    private boolean checkHealthRequirement(Animal animal) {
        double healthPercent = animal.getHealth() / animal.getMaxHealth();
        return healthPercent >= minHealth;
    }

    private boolean checkAgeRequirement(EcologyComponent component) {
        int ageTicks = AgeHandle.getAgeTicks(component);
        return ageTicks >= minAge;
    }

    private boolean checkConditionRequirement(EcologyComponent component) {
        int condition = ConditionHandle.getConditionLevel(component);
        return condition >= minCondition;
    }

    private EcologyComponent getEcologyComponent(Animal animal) {
        if (!(animal instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }
}
