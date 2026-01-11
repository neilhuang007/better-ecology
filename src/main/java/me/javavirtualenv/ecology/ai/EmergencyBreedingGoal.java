package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.conservation.PopulationRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Emergency breeding goal for endangered species.
 * <p>
 * Activates when population drops below threshold and uses relaxed breeding requirements.
 * <p>
 * Features:
 * <ul>
 *   <li>Lower age/health/condition requirements (50% of normal)</li>
 *   <li>Shorter breeding cooldown</li>
 *   <li>Higher movement speed to find mates</li>
 *   <li>Only active when species is endangered</li>
 *   <li>Path validation to ensure mates are reachable</li>
 * </ul>
 */
public class EmergencyBreedingGoal extends BreedGoal {

    // Configuration constants
    private static final double RELAXED_HEALTH_RATIO = 0.5; // 50% of normal health requirement
    private static final double RELAXED_AGE_RATIO = 0.5; // 50% of normal age requirement
    private static final double RELAXED_CONDITION_RATIO = 0.5; // 50% of normal condition requirement
    private static final int DEFAULT_EMERGENCY_THRESHOLD = 10; // Emergency when < 10 individuals
    private static final int DEFAULT_MIN_COOLDOWN_TICKS = 3000; // 2.5 minutes minimum cooldown
    private static final double EMERGENCY_SPEED_MULTIPLIER = 1.5; // Move faster to find mates

    // Instance fields
    private final Animal animal;
    private final int emergencyThreshold;
    private final double normalMinHealth;
    private final int normalMinAge;
    private final int normalMinCondition;
    private final double relaxedMinHealth;
    private final int relaxedMinAge;
    private final int relaxedMinCondition;

    // Debug info
    private String lastDebugMessage = "";
    private Animal currentPartner;
    private Path currentPath;

    /**
     * Create an emergency breeding goal with default settings.
     *
     * @param animal The animal
     * @param speedModifier Movement speed modifier
     */
    public EmergencyBreedingGoal(Animal animal, double speedModifier) {
        this(animal, speedModifier, 0.7, 0, 0, 6000, DEFAULT_EMERGENCY_THRESHOLD);
    }

    /**
     * Create an emergency breeding goal with custom settings.
     *
     * @param animal The animal
     * @param speedModifier Movement speed modifier
     * @param normalMinHealth Normal minimum health percentage (0.0-1.0)
     * @param normalMinAge Normal minimum age in ticks
     * @param normalMinCondition Normal minimum condition level
     * @param normalCooldownTicks Normal breeding cooldown in ticks
     * @param emergencyThreshold Population count below which emergency breeding activates
     */
    public EmergencyBreedingGoal(Animal animal, double speedModifier,
                               double normalMinHealth, int normalMinAge,
                               int normalMinCondition, int normalCooldownTicks,
                               int emergencyThreshold) {
        super(animal, speedModifier * EMERGENCY_SPEED_MULTIPLIER);
        this.animal = animal;
        this.emergencyThreshold = emergencyThreshold;
        this.normalMinHealth = normalMinHealth;
        this.normalMinAge = normalMinAge;
        this.normalMinCondition = normalMinCondition;

        // Calculate relaxed requirements (50% of normal)
        this.relaxedMinHealth = normalMinHealth * RELAXED_HEALTH_RATIO;
        this.relaxedMinAge = (int) (normalMinAge * RELAXED_AGE_RATIO);
        this.relaxedMinCondition = (int) (normalMinCondition * RELAXED_CONDITION_RATIO);
    }

    @Override
    public boolean canUse() {
        // Client-side validation
        if (animal.level().isClientSide) {
            return false;
        }

        // Only activate if species is endangered
        if (!isSpeciesEndangered()) {
            return false;
        }

        // Must be adult (relaxed age check - can breed younger)
        int age = getAge();
        if (age < relaxedMinAge) {
            return false;
        }

        // Check relaxed health requirement
        double healthPercent = animal.getHealth() / animal.getMaxHealth();
        if (healthPercent < relaxedMinHealth) {
            return false;
        }

        // Check breeding cooldown (relaxed - allows breeding sooner)
        int breedingCooldown = getBreedingCooldown();
        if (breedingCooldown > DEFAULT_MIN_COOLDOWN_TICKS) {
            return false;
        }

        // Check vanilla conditions
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

        // Check condition requirement (relaxed)
        int condition = getConditionLevel();
        if (condition < relaxedMinCondition) {
            return false;
        }

        debug("EMERGENCY BREEDING: population=" + getPopulationCount() + " with " +
              currentPartner.getType().toShortString() + " #" + currentPartner.getId());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if species recovers
        if (!isSpeciesEndangered()) {
            return false;
        }

        return super.canContinueToUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        debug("emergency breeding stopped");
        currentPartner = null;
        currentPath = null;
        super.stop();
    }

    /**
     * Check if the species is currently endangered.
     */
    private boolean isSpeciesEndangered() {
        return PopulationRegistry.isEndangered(animal, emergencyThreshold);
    }

    /**
     * Find a valid breeding partner.
     */
    private Animal findPartner() {
        Object loveCause = animal.getLoveCause();
        if (!(loveCause instanceof Animal partner)) {
            return null;
        }

        if (partner == animal || !partner.isAlive()) {
            return null;
        }

        // Partner must be adult (relaxed - younger partners allowed)
        int partnerAge = getAge(partner);
        if (partnerAge < relaxedMinAge) {
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
     * Get age from NBT with default fallback.
     */
    private int getAge() {
        return getAge(animal);
    }

    /**
     * Get age from NBT for a specific animal.
     */
    private int getAge(Animal target) {
        EcologyComponent component = getComponent(target);
        if (component == null) {
            return target.isBaby() ? -1 : 0;
        }

        CompoundTag ageTag = component.getHandleTag("age");
        if (!ageTag.contains("age_ticks")) {
            return target.isBaby() ? -1 : 0;
        }

        return ageTag.getInt("age_ticks");
    }

    /**
     * Get breeding cooldown from NBT.
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
     * Get condition level from NBT.
     */
    private int getConditionLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100; // Default healthy condition
        }

        CompoundTag conditionTag = component.getHandleTag("condition");
        if (!conditionTag.contains("condition_level")) {
            return 100;
        }

        return conditionTag.getInt("condition_level");
    }

    /**
     * Get population count for debugging.
     */
    private int getPopulationCount() {
        return PopulationRegistry.getPopulation(animal);
    }

    /**
     * Get ecology component for this animal.
     */
    private EcologyComponent getComponent() {
        return getComponent(animal);
    }

    /**
     * Get ecology component for a specific animal.
     */
    private EcologyComponent getComponent(Animal target) {
        if (!(target instanceof EcologyAccess access)) {
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
            String prefix = "[EmergencyBreeding] " + animal.getType().toShortString() + " #" + animal.getId() + " ";
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
        int populationCount = getPopulationCount();
        int age = getAge();
        int condition = getConditionLevel();
        boolean isEndangered = isSpeciesEndangered();
        String partnerInfo = currentPartner != null ?
            currentPartner.getType().toShortString() + "#" + currentPartner.getId() : "none";

        return String.format("health=%.0f%%, age=%d, condition=%d, cooldown=%d, pop=%d, endangered=%b, partner=%s",
            healthPercent * 100,
            age,
            condition,
            breedingCooldown,
            populationCount,
            isEndangered,
            partnerInfo
        );
    }
}
