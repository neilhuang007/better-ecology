package me.javavirtualenv.behavior.crepuscular;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

/**
 * Handles emergence triggers for crepuscular creatures.
 * Determines when to emerge from roost based on environmental conditions.
 */
public class EmergenceTrigger {
    private final CrepuscularActivity activityCalculator;
    private final CrepuscularConfig config;

    // Targeting conditions for detecting nearby creatures
    private TargetingConditions groupDetectionConditions;

    public EmergenceTrigger(CrepuscularConfig config) {
        this.config = config;
        this.activityCalculator = new CrepuscularActivity(config);
        setupTargetingConditions();
    }

    public EmergenceTrigger() {
        this(new CrepuscularConfig());
    }

    private void setupTargetingConditions() {
        groupDetectionConditions = TargetingConditions.forNonCombat()
            .range(config.getGroupDetectionRange())
            .selector(getSameSpeciesSelector());
    }

    /**
     * Gets a predicate for filtering same-species entities.
     */
    private java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> getSameSpeciesSelector() {
        return entity -> true; // Filtered by class in getEntitiesOfClass
    }

    /**
     * Determines if a creature should emerge from its roost.
     * Checks light levels, time, temperature, and social triggers.
     */
    public boolean shouldEmerge(Mob entity) {
        Level level = entity.level();

        // Check if weather prevents emergence
        if (activityCalculator.isWeatherDelayingEmergence(level)) {
            return false;
        }

        // Check light level threshold
        if (!activityCalculator.isEmergenceLightLevel(entity)) {
            return false;
        }

        // Check time of day (dusk or dawn)
        long dayTime = level.getDayTime() % 24000;
        if (!activityCalculator.isDusk(dayTime) && !activityCalculator.isDawn(dayTime)) {
            return false;
        }

        // Calculate emergence chance with modifiers
        double emergenceChance = calculateEmergenceChance(entity);

        // Check for nearby predators
        if (hasNearbyPredator(entity)) {
            return false;
        }

        // Group emergence: follow others out
        boolean othersEmerging = areOthersEmerging(entity);

        if (othersEmerging) {
            // Higher chance to follow the group
            emergenceChance *= config.getGroupEmergenceChance();
        }

        // Random chance based on all conditions
        return entity.getRandom().nextDouble() < emergenceChance;
    }

    /**
     * Determines if a creature should return to its roost.
     */
    public boolean shouldReturn(Mob entity) {
        Level level = entity.level();
        long dayTime = level.getDayTime() % 24000;

        // Return before dawn
        if (activityCalculator.isDawn(dayTime)) {
            int lightLevel = activityCalculator.getSkyLightLevel(entity);
            return lightLevel > config.getReturnLightLevel();
        }

        // Return after dusk if too dark
        if (activityCalculator.isNighttime(dayTime)) {
            int lightLevel = activityCalculator.getSkyLightLevel(entity);
            return lightLevel < 2;
        }

        return false;
    }

    /**
     * Calculates the emergence chance based on environmental modifiers.
     */
    private double calculateEmergenceChance(Mob entity) {
        double baseChance = 0.1;
        double modifier = activityCalculator.calculateEmergenceModifier(entity);

        // Adjust by temperature
        if (config.isUseTemperatureModifier()) {
            double tempMod = activityCalculator.getTemperatureModifier(entity);
            baseChance *= tempMod;
        }

        // Adjust by moon phase
        double moonMod = activityCalculator.getMoonPhaseModifier(entity.level());
        baseChance *= moonMod;

        return Math.max(0.01, Math.min(1.0, baseChance * modifier));
    }

    /**
     * Checks if there are nearby predators that should prevent emergence.
     */
    private boolean hasNearbyPredator(Mob entity) {
        Level level = entity.level();
        double detectionRange = config.getGroupDetectionRange() * 2;

        // Check for hostile mobs near emergence point
        List<? extends Mob> nearbyHostiles = level.getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(detectionRange),
            this::isPredator
        );

        return !nearbyHostiles.isEmpty();
    }

    /**
     * Determines if a mob is a potential predator.
     */
    private boolean isPredator(Mob mob) {
        // Bats avoid flying creatures and owls (phantoms represent owls in Minecraft)
        String entityName = mob.getName().getString().toLowerCase();
        return entityName.contains("phantom") ||
               entityName.contains("owl") ||
               mob.getClass().getSimpleName().contains("Phantom");
    }

    /**
     * Checks if other members of the same species are emerging.
     * Implements social emergence behavior.
     */
    private boolean areOthersEmerging(Mob entity) {
        Level level = entity.level();
        Class<? extends Mob> entityClass = entity.getClass();

        // Find nearby creatures of the same type
        List<? extends Mob> nearbyCreatures = level.getEntitiesOfClass(
            entityClass,
            entity.getBoundingBox().inflate(config.getGroupDetectionRange()),
            getSameSpeciesSelector()
        );

        // Check if any are already active (not resting)
        for (Mob creature : nearbyCreatures) {
            if (creature != entity && isEmerging(creature)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a specific creature is currently emerging or active.
     */
    private boolean isEmerging(Mob creature) {
        // Check if creature is moving (not resting)
        return !creature.getDeltaMovement().equals(net.minecraft.world.phys.Vec3.ZERO);
    }

    /**
     * Gets a count of nearby group members.
     */
    public int getNearbyGroupCount(Mob entity) {
        Level level = entity.level();
        Class<? extends Mob> entityClass = entity.getClass();

        List<? extends Mob> nearbyCreatures = level.getEntitiesOfClass(
            entityClass,
            entity.getBoundingBox().inflate(config.getGroupDetectionRange()),
            mob -> mob != entity
        );

        return nearbyCreatures.size();
    }

    /**
     * Determines the optimal emergence direction (away from predators).
     */
    public net.minecraft.world.phys.Vec3 getEmergenceDirection(Mob entity) {
        Level level = entity.level();

        // Find nearest predator
        List<? extends Mob> nearbyPredators = level.getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(config.getGroupDetectionRange() * 2),
            this::isPredator
        );

        if (!nearbyPredators.isEmpty()) {
            // Flee from predator
            Mob predator = nearbyPredators.get(0);
            net.minecraft.world.phys.Vec3 awayFromPredator = entity.position()
                .subtract(predator.position())
                .normalize();
            return awayFromPredator;
        }

        // Default: fly toward open space
        // Create a random direction vector
        double yaw = entity.getRandom().nextFloat() * 2 * Math.PI;
        double pitch = -0.17; // Slightly upward (about -10 degrees)
        double x = Math.cos(pitch) * Math.cos(yaw);
        double y = Math.sin(pitch);
        double z = Math.cos(pitch) * Math.sin(yaw);
        return new net.minecraft.world.phys.Vec3(x, y, z).normalize();
    }

    /**
     * Updates the configuration for this trigger.
     */
    public void setConfig(CrepuscularConfig config) {
        activityCalculator.setConfig(config);
        setupTargetingConditions();
    }

    /**
     * Gets the activity calculator used by this trigger.
     */
    public CrepuscularActivity getActivityCalculator() {
        return activityCalculator;
    }

    /**
     * Gets the configuration for this trigger.
     */
    public CrepuscularConfig getConfig() {
        return config;
    }
}
