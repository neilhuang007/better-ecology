package me.javavirtualenv.ecology.state;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Tracks dynamic state of an entity.
 * States are computed fresh each tick from entity properties and cached.
 * Caching is reset only when needed to avoid redundant computations.
 */
public final class EntityState {

    private final Mob mob;
    private final int tickCount;
    private int lastComputedTick = -1;

    // Cached computed states - only computed once per tick
    private Boolean isBaby;
    private Boolean isElderly;
    private Boolean isInjured;
    private Boolean isHungry;
    private Boolean isThirsty;
    private Boolean isStarving;
    private Boolean isDehydrated;
    private Boolean isFleeing;
    private Boolean isHunting;
    private Boolean isCarryingOffspring;
    private Boolean isInWater;
    private Boolean isTamed;
    private Boolean isLonely;
    private Boolean isPanicking;
    private Boolean isInCombat;
    private Boolean isRetreating;
    private Float healthPercent;
    private Double cachedSpeed;

    // Dirty flags to track which values changed
    private boolean stateChanged = false;

    public EntityState(Mob mob) {
        this.mob = mob;
        this.tickCount = mob.tickCount;
    }

    public Mob mob() {
        return mob;
    }

    public int tickCount() {
        return tickCount;
    }

    /**
     * Check if state changed since last tick.
     * Only true if at least one state value actually changed.
     */
    public boolean stateChanged() {
        return stateChanged;
    }

    /**
     * Prepare state for a new tick.
     * Clears cached values to force recomputation on first access.
     * This lazy evaluation avoids computing unused states.
     */
    public void prepareForTick() {
        if (mob.tickCount == lastComputedTick) {
            return; // Already prepared for this tick
        }

        // Store old values for change detection (only for externally set states)
        boolean oldIsHungry = isHungry != null && isHungry;
        boolean oldIsThirsty = isThirsty != null && isThirsty;
        boolean oldIsInjured = isInjured != null && isInjured;
        boolean oldIsLonely = isLonely != null && isLonely;

        // Reset all cached computed states
        isBaby = null;
        isElderly = null;
        isInjured = null;
        isHungry = null;
        isThirsty = null;
        isStarving = false;
        isDehydrated = false;
        isFleeing = null;
        isHunting = null;
        isCarryingOffspring = null;
        isInWater = null;
        isTamed = null;
        isLonely = null;
        isPanicking = null;
        isInCombat = null;
        isRetreating = null;
        healthPercent = null;
        cachedSpeed = null;

        lastComputedTick = mob.tickCount;
        stateChanged = false;

        // Restore externally set states and detect changes
        setIsHungry(oldIsHungry);
        setIsThirsty(oldIsThirsty);
        setIsLonely(oldIsLonely);
        if (oldIsInjured) {
            isInjured = true;
        }
    }

    /** Young animal (before maturity) */
    public boolean isBaby() {
        if (isBaby == null) {
            if (mob instanceof AgeableMob ageable) {
                isBaby = ageable.isBaby();
            } else {
                isBaby = false;
            }
        }
        return isBaby;
    }

    /** Old animal (past elderly_age if defined) */
    public boolean isElderly() {
        if (isElderly == null) {
            // Will be set by AgeHandle if elderly age is defined
            isElderly = false;
        }
        return isElderly;
    }

    /** Below 30% health */
    public boolean isInjured() {
        if (isInjured == null) {
            isInjured = healthPercent() < 0.3f;
        }
        return isInjured;
    }

    /** Health as percentage (0.0 to 1.0) */
    public float healthPercent() {
        if (healthPercent == null) {
            float maxHealth = mob.getMaxHealth();
            healthPercent = maxHealth > 0 ? mob.getHealth() / maxHealth : 0;
        }
        return healthPercent;
    }

    public void setIsElderly(boolean elderly) {
        if (isElderly == null || isElderly != elderly) {
            isElderly = elderly;
            stateChanged = true;
        }
    }

    public void setIsHungry(boolean hungry) {
        if (isHungry == null || isHungry != hungry) {
            isHungry = hungry;
            stateChanged = true;
        }
    }

    public void setIsThirsty(boolean thirsty) {
        if (isThirsty == null || isThirsty != thirsty) {
            isThirsty = thirsty;
            stateChanged = true;
        }
    }

    public void setIsFleeing(boolean fleeing) {
        if (isFleeing == null || isFleeing != fleeing) {
            isFleeing = fleeing;
            stateChanged = true;
        }
    }

    public void setIsHunting(boolean hunting) {
        if (isHunting == null || isHunting != hunting) {
            isHunting = hunting;
            stateChanged = true;
        }
    }

    public void setIsCarryingOffspring(boolean carrying) {
        if (isCarryingOffspring == null || isCarryingOffspring != carrying) {
            isCarryingOffspring = carrying;
            stateChanged = true;
        }
    }

    /** Below hunger threshold */
    public boolean isHungry() {
        return isHungry != null && isHungry;
    }

    /** Below thirst threshold */
    public boolean isThirsty() {
        return isThirsty != null && isThirsty;
    }

    /** Critical hunger - near starvation */
    public boolean isStarving() {
        return isStarving != null && isStarving;
    }

    public void setIsStarving(boolean starving) {
        if (isStarving == null || isStarving != starving) {
            isStarving = starving;
            stateChanged = true;
        }
    }

    /** Critical thirst - near dehydration */
    public boolean isDehydrated() {
        return isDehydrated != null && isDehydrated;
    }

    public void setIsDehydrated(boolean dehydrated) {
        if (isDehydrated == null || isDehydrated != dehydrated) {
            isDehydrated = dehydrated;
            stateChanged = true;
        }
    }

    /** Currently fleeing from predator */
    public boolean isFleeing() {
        return isFleeing != null && isFleeing;
    }

    /** Currently hunting prey */
    public boolean isHunting() {
        return isHunting != null && isHunting;
    }

    /** Carrying baby (e.g., fox, ocelot) */
    public boolean isCarryingOffspring() {
        return isCarryingOffspring != null && isCarryingOffspring;
    }

    /** Standing in water */
    public boolean isInWater() {
        if (isInWater == null) {
            isInWater = mob.isInWater();
        }
        return isInWater;
    }

    /** Is tamed by player */
    public boolean isTamed() {
        if (isTamed == null) {
            isTamed = mob instanceof TamableAnimal tamable && tamable.isTame();
        }
        return isTamed;
    }

    /** Is lonely (no nearby group members) */
    public boolean isLonely() {
        return isLonely != null && isLonely;
    }

    public void setIsLonely(boolean lonely) {
        if (isLonely == null || isLonely != lonely) {
            isLonely = lonely;
            stateChanged = true;
        }
    }

    /** Is currently panicking (stampede behavior) */
    public boolean isPanicking() {
        return isPanicking != null && isPanicking;
    }

    public void setIsPanicking(boolean panicking) {
        if (isPanicking == null || isPanicking != panicking) {
            isPanicking = panicking;
            stateChanged = true;
        }
    }

    /** Is in combat (recently hurt by another mob) */
    public boolean isInCombat() {
        if (isInCombat == null) {
            isInCombat = mob.getLastHurtByMob() != null && mob.tickCount - mob.getLastHurtByMobTimestamp() < 200;
        }
        return isInCombat;
    }

    /** Is retreating (fleeing due to low health) */
    public boolean isRetreating() {
        return isRetreating != null && isRetreating;
    }

    public void setIsRetreating(boolean retreating) {
        if (isRetreating == null || isRetreating != retreating) {
            isRetreating = retreating;
            stateChanged = true;
        }
    }

    /** Current movement speed attribute value */
    public double getCurrentSpeed() {
        if (cachedSpeed == null) {
            AttributeInstance attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            cachedSpeed = attr != null ? attr.getValue() : 0;
        }
        return cachedSpeed;
    }

    /**
     * Reset cached states (call at start of tick) - DEPRECATED: Use
     * prepareForTick()
     */
    public void reset() {
        prepareForTick();
    }
}
