package me.javavirtualenv.behavior.frog;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.AgeHandle;
import me.javavirtualenv.ecology.handles.BehaviorHandle;
import me.javavirtualenv.ecology.handles.BreedingHandle;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import me.javavirtualenv.ecology.handles.DietHandle;
import me.javavirtualenv.ecology.handles.EnergyHandle;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.handles.MovementHandle;
import me.javavirtualenv.ecology.handles.SizeHandle;
import me.javavirtualenv.ecology.handles.SocialHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Frog;

/**
 * Handle for managing frog-specific behaviors.
 * <p>
 * Integrates frog behaviors with the Better Ecology system:
 * - Tongue attack system for catching prey
 * - Croaking for communication and mating
 * - Swimming and jumping behaviors
 * - Biome-specific temperaments
 * - Frogspawn laying behavior
 */
public class FrogHandle implements EcologyHandle {

    private static final String CACHE_KEY = "better-ecology:frog-cache";

    @Override
    public String id() {
        return "frog";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        FrogCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled();
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Frog frog)) {
            return;
        }

        FrogCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        // Frog behaviors are primarily controlled through the vanilla brain system
        // We enhance it with our custom behaviors via tick()
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Frog frog)) {
            return;
        }

        FrogCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        CompoundTag handleTag = component.getHandleTag(id());

        // Update croaking
        if (cache.enableCroaking()) {
            tickCroaking(frog, handleTag);
        }

        // Update tongue attack
        if (cache.enableTongueAttack()) {
            tickTongueAttack(frog, handleTag);
        }

        // Update swimming
        if (cache.enableSwimming()) {
            tickSwimming(frog, handleTag);
        }

        // Update jumping
        if (cache.enableJumping()) {
            tickJumping(frog, handleTag);
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // NBT data is automatically loaded via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    @Override
    public void initialize(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Frog frog)) {
            return;
        }

        // Initialize frog-specific data
        CompoundTag handleTag = component.getHandleTag(id());

        if (!handleTag.contains("variant")) {
            // Store frog variant for biome-specific behavior
            String variantId = frog.getVariant().unwrap().location().toString();
            handleTag.putString("variant", variantId);
        }
    }

    private void tickCroaking(Frog frog, CompoundTag handleTag) {
        // Croaking is handled by CroakingBehavior in the behavior system
        // This is just for additional state tracking
        if (!frog.level().isClientSide() && frog.getRandom().nextDouble() < 0.001) {
            handleTag.putLong("last_croak_time", frog.level().getGameTime());
        }
    }

    private void tickTongueAttack(Frog frog, CompoundTag handleTag) {
        // Update tongue attack animation state
        int tongueState = handleTag.getInt("tongue_state");
        if (tongueState > 0) {
            // Advance animation state
            int newState = tongueState - 1;
            handleTag.putInt("tongue_state", newState);

            // Clear target when done
            if (newState == 0) {
                handleTag.remove("tongue_target_id");
            }
        }
    }

    private void tickSwimming(Frog frog, CompoundTag handleTag) {
        // Swimming is primarily handled by vanilla movement controls
        // Track whether frog is actively swimming
        handleTag.putBoolean("is_swimming", frog.isInWater());
    }

    private void tickJumping(Frog frog, CompoundTag handleTag) {
        // Track jump state for animation purposes
        handleTag.putBoolean("is_jumping", !frog.isOnGround());
    }

    private FrogCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBool("frog.enabled", true);

        if (!enabled) {
            return null;
        }

        boolean enableCroaking = profile.getBool("frog.croaking_enabled", true);
        boolean enableTongueAttack = profile.getBool("frog.tongue_attack_enabled", true);
        boolean enableSwimming = profile.getBool("frog.swimming_enabled", true);
        boolean enableJumping = profile.getBool("frog.jumping_enabled", true);
        double tongueRange = profile.getDouble("frog.tongue_range", 3.0);
        int tongueCooldown = profile.getInt("frog.tongue_cooldown", 60);
        int croakInterval = profile.getInt("frog.croak_interval", 200);

        return new FrogCache(
                enabled,
                enableCroaking,
                enableTongueAttack,
                enableSwimming,
                enableJumping,
                tongueRange,
                tongueCooldown,
                croakInterval
        );
    }

    /**
     * Gets the temperament modifier for a frog based on its variant.
     * Cold frogs are more passive, temperate are balanced, warm are more aggressive.
     */
    public static float getTemperamentModifier(Frog frog) {
        var variant = frog.getVariant();
        String variantId = variant.unwrap().location().toString();

        return switch (variantId) {
            case "minecraft:temperate" -> 1.0F;
            case "minecraft:warm" -> 1.2F;
            case "minecraft:cold" -> 0.8F;
            default -> 1.0F;
        };
    }

    /**
     * Checks if a frog can lay frogspawn.
     */
    public static boolean canLayFrogspawn(Frog frog) {
        // Must be in water
        if (!frog.isInWater()) {
            return false;
        }

        // Must have breeding target
        if (frog.getBrain() == null
                || !frog.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.BREED_TARGET)) {
            return false;
        }

        return true;
    }

    private static final class FrogCache {
        private final boolean enabled;
        private final boolean enableCroaking;
        private final boolean enableTongueAttack;
        private final boolean enableSwimming;
        private final boolean enableJumping;
        private final double tongueRange;
        private final int tongueCooldown;
        private final int croakInterval;

        private FrogCache(boolean enabled, boolean enableCroaking, boolean enableTongueAttack,
                         boolean enableSwimming, boolean enableJumping, double tongueRange,
                         int tongueCooldown, int croakInterval) {
            this.enabled = enabled;
            this.enableCroaking = enableCroaking;
            this.enableTongueAttack = enableTongueAttack;
            this.enableSwimming = enableSwimming;
            this.enableJumping = enableJumping;
            this.tongueRange = tongueRange;
            this.tongueCooldown = tongueCooldown;
            this.croakInterval = croakInterval;
        }

        private boolean enabled() {
            return enabled;
        }

        private boolean enableCroaking() {
            return enableCroaking;
        }

        private boolean enableTongueAttack() {
            return enableTongueAttack;
        }

        private boolean enableSwimming() {
            return enableSwimming;
        }

        private boolean enableJumping() {
            return enableJumping;
        }

        private double tongueRange() {
            return tongueRange;
        }

        private int tongueCooldown() {
            return tongueCooldown;
        }

        private int croakInterval() {
            return croakInterval;
        }
    }
}
