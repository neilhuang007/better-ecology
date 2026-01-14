package me.javavirtualenv.behavior.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import com.mojang.serialization.Codec;

/**
 * Centralized utility for reading and writing animal hunger and thirst values.
 * Uses Fabric's Attachment API for persistent data storage on entities.
 */
public final class AnimalNeeds {

    private AnimalNeeds() {
        // Utility class
    }

    // ========== DEFAULT VALUES ==========
    // Must be declared before attachment types to avoid forward reference errors

    public static final float DEFAULT_HUNGER = 80f;
    public static final float DEFAULT_THIRST = 100f;
    public static final float MAX_VALUE = 100f;
    public static final float MIN_VALUE = 0f;

    // ========== ATTACHMENT TYPES ==========

    public static final AttachmentType<Float> HUNGER_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "hunger"),
        builder -> builder
            .initializer(() -> DEFAULT_HUNGER)
            .persistent(Codec.FLOAT)
            .copyOnDeath()
    );

    public static final AttachmentType<Float> THIRST_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "thirst"),
        builder -> builder
            .initializer(() -> DEFAULT_THIRST)
            .persistent(Codec.FLOAT)
            .copyOnDeath()
    );

    public static final AttachmentType<Long> LAST_DAMAGE_TICK_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "last_damage_tick"),
        builder -> builder
            .initializer(() -> 0L)
            .persistent(Codec.LONG)
    );

    public static final AttachmentType<Boolean> INITIALIZED_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "initialized"),
        builder -> builder
            .initializer(() -> false)
            .persistent(Codec.BOOL)
    );

    // ========== HUNGER METHODS ==========

    /**
     * Gets the current hunger value for a mob (0-100).
     * Returns the default value if not set.
     */
    public static float getHunger(Mob mob) {
        return mob.getAttachedOrCreate(HUNGER_ATTACHMENT);
    }

    /**
     * Sets the hunger value for a mob, clamped to 0-100.
     */
    public static void setHunger(Mob mob, float value) {
        float clamped = clamp(value);
        mob.setAttached(HUNGER_ATTACHMENT, clamped);
    }

    /**
     * Modifies hunger by delta (positive = more full, negative = more hungry).
     */
    public static void modifyHunger(Mob mob, float delta) {
        setHunger(mob, getHunger(mob) + delta);
    }

    /**
     * Decreases hunger by the decay rate (call each tick).
     */
    public static void decayHunger(Mob mob, float decayRate) {
        modifyHunger(mob, -decayRate);
    }

    // ========== THIRST METHODS ==========

    /**
     * Gets the current thirst value for a mob (0-100).
     * Returns the default value if not set.
     */
    public static float getThirst(Mob mob) {
        return mob.getAttachedOrCreate(THIRST_ATTACHMENT);
    }

    /**
     * Sets the thirst value for a mob, clamped to 0-100.
     */
    public static void setThirst(Mob mob, float value) {
        float clamped = clamp(value);
        mob.setAttached(THIRST_ATTACHMENT, clamped);
    }

    /**
     * Modifies thirst by delta (positive = more hydrated, negative = more thirsty).
     */
    public static void modifyThirst(Mob mob, float delta) {
        setThirst(mob, getThirst(mob) + delta);
    }

    /**
     * Decreases thirst by the decay rate (call each tick).
     */
    public static void decayThirst(Mob mob, float decayRate) {
        modifyThirst(mob, -decayRate);
    }

    // ========== THRESHOLD CHECKS ==========

    /**
     * Returns true if the mob is hungry (below HUNGRY threshold).
     */
    public static boolean isHungry(Mob mob) {
        return getHunger(mob) < AnimalThresholds.HUNGRY;
    }

    /**
     * Returns true if the mob is starving (below STARVING threshold).
     */
    public static boolean isStarving(Mob mob) {
        return getHunger(mob) < AnimalThresholds.STARVING;
    }

    /**
     * Returns true if the mob is satisfied (above SATISFIED threshold).
     */
    public static boolean isSatisfied(Mob mob) {
        return getHunger(mob) >= AnimalThresholds.SATISFIED;
    }

    /**
     * Returns true if the mob is thirsty (below THIRSTY threshold).
     */
    public static boolean isThirsty(Mob mob) {
        return getThirst(mob) < AnimalThresholds.THIRSTY;
    }

    /**
     * Returns true if the mob is dehydrated (below DEHYDRATED threshold).
     */
    public static boolean isDehydrated(Mob mob) {
        return getThirst(mob) < AnimalThresholds.DEHYDRATED;
    }

    /**
     * Returns true if the mob is hydrated (above HYDRATED threshold).
     */
    public static boolean isHydrated(Mob mob) {
        return getThirst(mob) >= AnimalThresholds.HYDRATED;
    }

    // ========== DAMAGE TRACKING ==========

    /**
     * Gets the last tick when damage was applied.
     */
    public static long getLastDamageTick(Mob mob) {
        return mob.getAttachedOrCreate(LAST_DAMAGE_TICK_ATTACHMENT);
    }

    /**
     * Sets the last tick when damage was applied.
     */
    public static void setLastDamageTick(Mob mob, long tick) {
        mob.setAttached(LAST_DAMAGE_TICK_ATTACHMENT, tick);
    }

    /**
     * Returns true if enough time has passed since last damage.
     */
    public static boolean canTakeDamage(Mob mob, int damageInterval) {
        long currentTick = mob.level().getGameTime();
        long lastDamage = getLastDamageTick(mob);
        return (currentTick - lastDamage) >= damageInterval;
    }

    // ========== INITIALIZATION ==========

    /**
     * Returns true if the mob has been initialized with ecology data.
     */
    public static boolean isInitialized(Mob mob) {
        return mob.getAttachedOrCreate(INITIALIZED_ATTACHMENT);
    }

    /**
     * Marks the mob as initialized.
     */
    public static void markInitialized(Mob mob) {
        mob.setAttached(INITIALIZED_ATTACHMENT, true);
    }

    /**
     * Initializes hunger and thirst for a newly spawned mob if not already set.
     */
    public static void initializeIfNeeded(Mob mob) {
        if (!isInitialized(mob)) {
            // Trigger default initialization by accessing values
            getHunger(mob);
            getThirst(mob);
            markInitialized(mob);
        }
    }

    // ========== UTILITY ==========

    private static float clamp(float value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }
}
