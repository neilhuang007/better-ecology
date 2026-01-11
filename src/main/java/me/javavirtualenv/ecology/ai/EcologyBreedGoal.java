package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.conservation.HabitatQuality;
import me.javavirtualenv.ecology.conservation.LineageRegistry;
import me.javavirtualenv.ecology.conservation.RefugeSystem;
import me.javavirtualenv.ecology.handles.HabitatHandle;
import me.javavirtualenv.ecology.handles.PopulationDensityHandle;
import me.javavirtualenv.ecology.handles.SeasonalHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Enhanced breeding goal that enforces ecological requirements.
 * <p>
 * Animals will breed when:
 * <ul>
 *   <li>They are adult (not baby)</li>
 *   <li>They are healthy enough (min health percentage)</li>
 *   <li>They are not on breeding cooldown</li>
 *   <li>Density requirements are met (Allee threshold, carrying capacity)</li>
 *   <li>Habitat quality allows breeding</li>
 *   <li>Seasonal conditions allow breeding</li>
 *   <li>Partner is not too closely related (genetic diversity)</li>
 *   <li>Partner is reachable (pathfinding check)</li>
 * </ul>
 */
public class EcologyBreedGoal extends BreedGoal {

    // Configuration constants
    private static final double MIN_HEALTH_PERCENT = 0.7; // Must have 70% health to breed
    private static final int DEFAULT_COOLDOWN_TICKS = 6000; // 5 minutes default cooldown

    // Instance fields
    private final Animal animal;
    private final PopulationDensityHandle.DensityConfig densityConfig;
    private final double minHealthPercent;
    private final int cooldownTicks;

    // Debug info
    private String lastDebugMessage = "";
    private Animal currentPartner;
    private Path currentPath;

    /**
     * Create an ecology breed goal with default settings.
     *
     * @param animal The animal
     * @param speedModifier Movement speed modifier
     */
    public EcologyBreedGoal(Animal animal, double speedModifier) {
        this(animal, speedModifier, MIN_HEALTH_PERCENT, DEFAULT_COOLDOWN_TICKS, null);
    }

    /**
     * Create an ecology breed goal with custom settings (legacy constructor).
     * Accepts old BreedingConfig parameter format for backward compatibility.
     *
     * @param animal The animal
     * @param speedModifier Movement speed modifier
     * @param minAge Ignored (uses isBaby() check)
     * @param minHealthPercent Minimum health percentage (0.0-1.0)
     * @param minCondition Ignored (condition system not used)
     * @param cooldownTicks Breeding cooldown in ticks
     * @param densityConfig Population density configuration
     */
    public EcologyBreedGoal(Animal animal, double speedModifier,
                           int minAge, double minHealthPercent, int minCondition,
                           int cooldownTicks, PopulationDensityHandle.DensityConfig densityConfig) {
        this(animal, speedModifier, minHealthPercent, cooldownTicks, densityConfig);
    }

    /**
     * Create an ecology breed goal with custom settings.
     *
     * @param animal The animal
     * @param speedModifier Movement speed modifier
     * @param minHealthPercent Minimum health percentage (0.0-1.0)
     * @param cooldownTicks Breeding cooldown in ticks
     * @param densityConfig Population density configuration
     */
    public EcologyBreedGoal(Animal animal, double speedModifier,
                           double minHealthPercent, int cooldownTicks,
                           PopulationDensityHandle.DensityConfig densityConfig) {
        super(animal, speedModifier);
        this.animal = animal;
        this.minHealthPercent = minHealthPercent;
        this.cooldownTicks = cooldownTicks;
        this.densityConfig = densityConfig;
    }

    @Override
    public boolean canUse() {
        // Client-side validation
        if (animal.level().isClientSide) {
            return false;
        }

        // Must be adult
        if (animal.isBaby()) {
            return false;
        }

        // Check health requirement
        double healthPercent = animal.getHealth() / animal.getMaxHealth();
        if (healthPercent < minHealthPercent) {
            return false;
        }

        // Check breeding cooldown in NBT
        int breedingCooldown = getBreedingCooldown();
        if (breedingCooldown > 0) {
            return false;
        }

        // Check vanilla conditions (has partner, in love, etc.)
        if (!super.canUse()) {
            return false;
        }

        // Find and validate partner
        currentPartner = findPartner();
        if (currentPartner == null) {
            return false;
        }

        // Check if partner is reachable
        if (!canReachPartner(currentPartner)) {
            debug("partner unreachable");
            return false;
        }

        // Check ecological requirements
        if (!meetsEcologicalRequirements()) {
            return false;
        }

        debug("STARTING: breeding with " + currentPartner.getType().toShortString() + " #" + currentPartner.getId());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        debug("breeding stopped");
        currentPartner = null;
        currentPath = null;
        super.stop();
    }

    /**
     * Find a valid breeding partner.
     * Uses pathfinding to ensure reachability.
     */
    private Animal findPartner() {
        Object loveCause = animal.getLoveCause();
        if (!(loveCause instanceof Animal partner)) {
            return null;
        }

        if (partner == animal || !partner.isAlive()) {
            return null;
        }

        // Partner must be adult
        if (partner.isBaby()) {
            return null;
        }

        // Check genetic diversity
        if (LineageRegistry.areTooCloselyRelated(animal.getUUID(), partner.getUUID())) {
            debug("partner too closely related");
            return null;
        }

        return partner;
    }

    /**
     * Check if partner is reachable via pathfinding.
     */
    private boolean canReachPartner(Animal partner) {
        PathNavigation navigation = animal.getNavigation();
        currentPath = navigation.createPath(partner, 0);

        if (currentPath == null || !currentPath.canReach()) {
            currentPath = null;
            return false;
        }

        return true;
    }

    /**
     * Check all ecological breeding requirements.
     */
    private boolean meetsEcologicalRequirements() {
        if (!checkDensityRequirements()) {
            return false;
        }

        if (!checkHabitatRequirements()) {
            return false;
        }

        if (!checkSeasonalRequirements()) {
            return false;
        }

        return true;
    }

    /**
     * Check population density requirements.
     */
    private boolean checkDensityRequirements() {
        // Density effects disabled
        if (densityConfig == null || !densityConfig.enabled()) {
            return true;
        }

        // Check Allee threshold
        boolean meetsAllee = PopulationDensityHandle.meetsAlleeThreshold(
            animal.level(),
            animal.blockPosition(),
            animal.getType(),
            densityConfig.checkRadius(),
            densityConfig.alleeThreshold()
        );
        if (!meetsAllee) {
            debug("population below Allee threshold");
            return false;
        }

        // Check carrying capacity
        boolean belowCapacity = PopulationDensityHandle.belowCarryingCapacity(
            animal.level(),
            animal.blockPosition(),
            animal.getType(),
            densityConfig.checkRadius(),
            densityConfig.carryingCapacity()
        );
        if (!belowCapacity) {
            debug("population at carrying capacity");
            return false;
        }

        // Apply breeding probability multiplier
        double multiplier = PopulationDensityHandle.getBreedingMultiplier(
            animal.level(),
            animal.blockPosition(),
            animal.getType(),
            densityConfig.checkRadius(),
            densityConfig.densityCurve()
        );

        if (multiplier <= 0.0) {
            debug("density prevents breeding");
            return false;
        }

        if (multiplier < 1.0) {
            boolean passes = animal.getRandom().nextDouble() < multiplier;
            if (!passes) {
                debug("density probability check failed");
            }
            return passes;
        }

        return true;
    }

    /**
     * Check habitat quality requirements.
     */
    private boolean checkHabitatRequirements() {
        if (!(animal.level() instanceof ServerLevel level)) {
            return true;
        }

        double totalMultiplier = 1.0;

        // Check habitat quality
        HabitatQuality quality = HabitatQuality.evaluateHabitat(level, animal.blockPosition(), animal.getType());
        double habitatMultiplier = HabitatHandle.getBreedingMultiplier(quality);
        totalMultiplier *= habitatMultiplier;

        // Check refuge status
        if (RefugeSystem.isInRefuge(level, animal.blockPosition(), animal.getType())) {
            double refugeBonus = RefugeSystem.getBreedingBonus(level, animal.blockPosition());
            totalMultiplier *= refugeBonus;
        }

        if (totalMultiplier <= 0.0) {
            debug("habitat prevents breeding");
            return false;
        }

        if (totalMultiplier < 1.0) {
            boolean passes = animal.getRandom().nextDouble() < totalMultiplier;
            if (!passes) {
                debug("habitat probability check failed");
            }
            return passes;
        }

        return true;
    }

    /**
     * Check seasonal breeding requirements.
     */
    private boolean checkSeasonalRequirements() {
        // Check if current season is a breeding season
        if (!SeasonalHandle.isBreedingSeason(animal)) {
            debug("not in breeding season");
            return false;
        }

        // Apply seasonal breeding multiplier
        double seasonalMultiplier = SeasonalHandle.getBreedingMultiplier(animal);

        if (seasonalMultiplier <= 0.1) {
            debug("season prevents breeding");
            return false;
        }

        if (seasonalMultiplier < 1.0) {
            boolean passes = animal.getRandom().nextDouble() < seasonalMultiplier;
            if (!passes) {
                debug("season probability check failed");
            }
            return passes;
        }

        return true;
    }

    /**
     * Get breeding cooldown from NBT with default fallback.
     */
    private int getBreedingCooldown() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 0;
        }

        CompoundTag breedingTag = component.getHandleTag("breeding");
        return breedingTag.getInt("breeding_cooldown");
    }

    /**
     * Get ecology component for this animal.
     */
    private EcologyComponent getComponent() {
        if (!(animal instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[Breeding] " + animal.getType().toShortString() + " #" + animal.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        double healthPercent = animal.getHealth() / animal.getMaxHealth();
        int breedingCooldown = getBreedingCooldown();
        String partnerInfo = currentPartner != null ?
            currentPartner.getType().toShortString() + "#" + currentPartner.getId() : "none";

        return String.format("health=%.0f%%, cooldown=%d, partner=%s, isBaby=%b",
            healthPercent * 100,
            breedingCooldown,
            partnerInfo,
            animal.isBaby()
        );
    }
}
