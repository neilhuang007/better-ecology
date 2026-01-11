package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.EmergencyBreedingGoal;
import me.javavirtualenv.ecology.conservation.ConservationStatus;
import me.javavirtualenv.ecology.conservation.PopulationRegistry;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

/**
 * Handle for conservation system integration.
 * Tracks population status and provides emergency breeding for endangered species.
 */
public final class ConservationHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:conservation-cache";

    @Override
    public String id() {
        return "conservation";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return profile.getBool("conservation.enabled", false);
    }

    @Override
    public void initialize(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Track this entity in the population registry
        PopulationRegistry.onEntitySpawned(mob);

        // Store MVP threshold for quick access
        ConservationConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
        component.getHandleTag("conservation").putInt("mvp_threshold", config.mvpThreshold());
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Animal animal)) {
            return;
        }

        ConservationConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));

        // Only register emergency breeding if enabled
        if (!config.emergencyBreedingEnabled()) {
            return;
        }

        int priority = profile.getInt("ai_priority_framework.conservation.emergency_breeding", 7);
        MobAccessor accessor = (MobAccessor) mob;

        // Emergency breeding goal with relaxed requirements
        accessor.betterEcology$getGoalSelector().addGoal(priority,
            new EmergencyBreedingGoal(animal, config.moveSpeed(),
                config.reducedMinHealth(), config.reducedMinAge(),
                (int) config.reducedMinCondition(), config.cooldown(),
                component.getHandleTag("conservation").getInt("mvp_threshold")));
    }

    /**
     * Get current conservation status for a mob.
     *
     * @param mob The mob entity
     * @param component The ecology component
     * @return Conservation status, or null if not tracked
     */
    public static ConservationStatus getStatus(Mob mob, EcologyComponent component) {
        if (!component.hasProfile()) {
            return null;
        }

        CompoundTag tag = component.getHandleTag("conservation");
        int mvpThreshold = tag.contains("mvp_threshold") ? tag.getInt("mvp_threshold") : 20;
        return PopulationRegistry.getStatus(mob, mvpThreshold);
    }

    /**
     * Check if a mob's species is endangered.
     *
     * @param mob The mob entity
     * @param component The ecology component
     * @return true if species is endangered, false otherwise
     */
    public static boolean isEndangered(Mob mob, EcologyComponent component) {
        ConservationStatus status = getStatus(mob, component);
        return status != null && status.isEmergencyState();
    }

    /**
     * Get breeding success multiplier based on conservation status.
     *
     * @param mob The mob entity
     * @param component The ecology component
     * @return Breeding multiplier (1.0-2.0)
     */
    public static double getBreedingMultiplier(Mob mob, EcologyComponent component) {
        ConservationStatus status = getStatus(mob, component);
        return status != null ? status.getBreedingBonus() : 1.0;
    }

    private ConservationConfig buildConfig(EcologyProfile profile) {
        int mvpThreshold = profile.getInt("conservation.mvp_threshold", 20);
        boolean emergencyBreeding = profile.getBool("conservation.emergency_breeding.enabled", true);
        int cooldown = profile.getInt("conservation.emergency_breeding.cooldown", 3000);
        double moveSpeed = profile.getDouble("conservation.emergency_breeding.move_speed", 1.2);

        // Relaxed requirements for emergency breeding (50% of normal)
        int minAge = profile.getInt("reproduction.requirements.min_age", 0) / 2;
        double minHealth = profile.getDouble("reproduction.requirements.min_health", 0.0) * 0.5;
        double minCondition = profile.getDouble("reproduction.requirements.min_condition", 0.0) * 0.5;

        return new ConservationConfig(mvpThreshold, emergencyBreeding, minAge, minHealth,
            minCondition, cooldown, moveSpeed);
    }

    private record ConservationConfig(
        int mvpThreshold,
        boolean emergencyBreedingEnabled,
        int reducedMinAge,
        double reducedMinHealth,
        double reducedMinCondition,
        int cooldown,
        double moveSpeed
    ) {}
}
