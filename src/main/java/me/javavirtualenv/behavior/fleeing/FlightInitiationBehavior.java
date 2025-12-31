package me.javavirtualenv.behavior.fleeing;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Spider;

import java.util.List;

/**
 * Determines when an animal should initiate flight based on threat assessment.
 * Implements Flight Initiation Distance (FID) calculations based on economic escape theory.
 * <p>
 * Based on research by:
 * - Ydenberg & Dill (1986) - Economic escape theory: flee when costs of remaining equal costs of escaping
 * - Katsis et al. (2024) - FID is repeatable within individuals but flexible across contexts
 * - Stankowich (2008) - Factors affecting ungulate flight responses
 * <p>
 * FID is influenced by:
 * - Distance to threat (primary factor)
 * - Threat type (ambush vs cursorial vs aerial vs human)
 * - Animal's internal state (health, age, experience)
 * - Environmental context (cover, refuge distance, group size)
 */
public class FlightInitiationBehavior {

    private final FleeingConfig config;
    private double habituationLevel = 0.0;
    private long lastThreatTime = 0;
    private LivingEntity lastThreat = null;

    // Threat type classification
    public enum ThreatType {
        AMBUSH,     // Creeping predators: cats, spiders
        CURSORIAL,  // Chasing predators: wolves, foxes
        AERIAL,     // Flying predators: phantoms, bees
        HUMAN,      // Players
        UNKNOWN     // Unidentified threats
    }

    public FlightInitiationBehavior(FleeingConfig config) {
        this.config = config;
    }

    public FlightInitiationBehavior() {
        this(FleeingConfig.createDefault());
    }

    /**
     * Determines if the animal should flee based on current threat assessment.
     * Implements economic escape theory - flee when cost/benefit ratio threshold is met.
     *
     * @param context Behavior context containing entity state
     * @return true if flight should be initiated
     */
    public boolean shouldFlee(BehaviorContext context) {
        Mob entity = context.getEntity();
        LivingEntity threat = detectNearestThreat(context);

        if (threat == null) {
            return false;
        }

        double distance = calculateDistance(context, threat);
        double fid = calculateFlightInitiationDistance(context, threat);

        boolean shouldFlee = distance <= fid;

        if (shouldFlee) {
            lastThreat = threat;
            lastThreatTime = entity.level().getGameTime();
        }

        return shouldFlee;
    }

    /**
     * Calculates the Flight Initiation Distance (FID) for a specific threat.
     * Adjusts base FID based on threat type, environmental factors, and internal state.
     *
     * @param context Behavior context
     * @param threat  Potential threat entity
     * @return Adjusted FID in blocks
     */
    public double calculateFlightInitiationDistance(BehaviorContext context, LivingEntity threat) {
        double baseFid = config.getFlightInitiationDistance();

        // Get threat type and apply multiplier
        ThreatType threatType = classifyThreat(threat);
        double threatMultiplier = getThreatMultiplier(threatType);

        // Adjust for animal's internal state
        double stateModifier = calculateStateModifier(context);

        // Adjust for environmental factors
        double environmentModifier = calculateEnvironmentModifier(context, threat);

        // Apply habituation (reduced FID with repeated non-lethal encounters)
        double habituationModifier = 1.0 - (habituationLevel * 0.5);

        // Calculate final FID
        double adjustedFid = baseFid * threatMultiplier * stateModifier * environmentModifier * habituationModifier;

        return Math.max(4.0, adjustedFid); // Minimum FID of 4 blocks
    }

    /**
     * Detects the nearest threat to the entity.
     * Considers predators, players, and recently damaged sources.
     *
     * @param context Behavior context
     * @return Nearest threat entity, or null if no threats detected
     */
    public LivingEntity detectNearestThreat(BehaviorContext context) {
        Mob entity = context.getEntity();
        Vec3d position = context.getPosition();
        double detectionRange = config.getFlightInitiationDistance() * 1.5;

        List<LivingEntity> nearbyEntities = entity.level().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(detectionRange)
        );

        LivingEntity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity nearby : nearbyEntities) {
            // Skip self
            if (nearby == entity) {
                continue;
            }

            // Skip same species (herd members aren't threats)
            if (nearby.getType() == entity.getType()) {
                continue;
            }

            // Check if entity is a threat
            if (!isThreat(entity, nearby)) {
                continue;
            }

            Vec3d threatPos = new Vec3d(nearby.getX(), nearby.getY(), nearby.getZ());
            double distance = position.distanceTo(threatPos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestThreat = nearby;
            }
        }

        return nearestThreat;
    }

    /**
     * Classifies a threat entity into threat types for appropriate FID adjustment.
     *
     * @param threat Potential threat entity
     * @return Classified threat type
     */
    public ThreatType classifyThreat(LivingEntity threat) {
        // Players
        if (threat instanceof Player) {
            return ThreatType.HUMAN;
        }

        // Ambush predators (creeping, stealthy)
        if (threat instanceof Spider || isAmbushPredator(threat)) {
            return ThreatType.AMBUSH;
        }

        // Cursorial predators (chasing, pursuit)
        if (threat instanceof Wolf || isCursorialPredator(threat)) {
            return ThreatType.CURSORIAL;
        }

        // Aerial predators (flying)
        if (isAerialPredator(threat)) {
            return ThreatType.AERIAL;
        }

        return ThreatType.UNKNOWN;
    }

    /**
     * Gets the FID multiplier for a specific threat type.
     * Based on research showing different FID for different predator types.
     *
     * @param threatType Type of threat
     * @return FID multiplier
     */
    private double getThreatMultiplier(ThreatType threatType) {
        return switch (threatType) {
            case AMBUSH -> config.getAmbushPredatorMultiplier();
            case CURSORIAL -> config.getCursorialPredatorMultiplier();
            case AERIAL -> config.getAerialPredatorMultiplier();
            case HUMAN -> config.getHumanThreatMultiplier();
            default -> 1.0;
        };
    }

    /**
     * Calculates FID modifier based on animal's internal state.
     * Factors: health, age (baby vs adult), recent damage, movement capability.
     *
     * @param context Behavior context
     * @return State modifier (0.5 - 1.5)
     */
    private double calculateStateModifier(BehaviorContext context) {
        Mob entity = context.getEntity();
        double modifier = 1.0;

        // Health factor - injured animals flee earlier
        double healthRatio = entity.getHealth() / entity.getMaxHealth();
        if (healthRatio < 0.5) {
            modifier *= 1.3; // Flee earlier when injured
        } else if (healthRatio < 0.8) {
            modifier *= 1.1;
        }

        // Age factor - babies flee earlier (more vulnerable)
        if (entity.isBaby()) {
            modifier *= 1.4;
        }

        // Recent damage - flee immediately if recently attacked
        if (entity.getLastHurtByMob() != null) {
            long timeSinceDamage = entity.level().getGameTime() - entity.getLastHurtByMobTimestamp();
            if (timeSinceDamage < 100) {
                modifier *= 1.5;
            }
        }

        // Speed factor - slower animals may flee earlier
        double currentSpeed = context.getSpeed();
        if (currentSpeed < 0.1) {
            modifier *= 1.2; // Can't move well, flee earlier
        }

        return modifier;
    }

    /**
     * Calculates FID modifier based on environmental factors.
     * Factors: distance to refuge, group size, terrain cover.
     *
     * @param context Behavior context
     * @param threat  Current threat
     * @return Environment modifier (0.7 - 1.3)
     */
    private double calculateEnvironmentModifier(BehaviorContext context, LivingEntity threat) {
        Mob entity = context.getEntity();
        double modifier = 1.0;

        // Distance to refuge - animals flee earlier when refuge is farther
        double refugeDistance = findNearestRefugeDistance(context);
        if (refugeDistance > config.getRefugeDetectionRange()) {
            modifier *= 1.2; // No refuge nearby, be more cautious
        } else if (refugeDistance < 8.0) {
            modifier *= 0.8; // Refuge close, can afford to wait longer
        }

        // Group size - larger groups may reduce individual FID (dilution effect)
        int nearbyHerdMembers = countNearbyHerdMembers(context);
        if (nearbyHerdMembers > 5) {
            modifier *= 0.9; // Safety in numbers
        } else if (nearbyHerdMembers == 0) {
            modifier *= 1.15; // Isolated, more cautious
        }

        // Light level - more cautious in darkness
        int lightLevel = entity.level().getLightEmission(entity.blockPosition());
        if (lightLevel < 4) {
            modifier *= 1.1; // Dark conditions, reduced visibility
        }

        return modifier;
    }

    /**
     * Determines if an entity is considered a threat to this animal.
     *
     * @param self   The animal assessing threat
     * @param other  Potential threat entity
     * @return true if entity is a threat
     */
    private boolean isThreat(Mob self, LivingEntity other) {
        // Players are threats unless sneaking
        if (other instanceof Player player) {
            return !player.isShiftKeyDown();
        }

        // Predators are threats
        EntityType<?> otherType = other.getType();
        String typeName = otherType.toString().toLowerCase();

        // Known predator types
        if (typeName.contains("wolf") || typeName.contains("fox") || typeName.contains("cat") ||
            typeName.contains("spider") || typeName.contains("phantom") || typeName.contains("bee")) {
            return true;
        }

        // Aggressive mobs are threats
        if (other instanceof Mob mob && mob.isAggressive()) {
            return true;
        }

        return false;
    }

    /**
     * Helper methods for threat classification.
     */
    private boolean isAmbushPredator(LivingEntity entity) {
        String typeName = entity.getType().toString().toLowerCase();
        return typeName.contains("cat") || typeName.contains("spider") ||
               typeName.contains("ocelot") || typeName.contains("frog");
    }

    private boolean isCursorialPredator(LivingEntity entity) {
        String typeName = entity.getType().toString().toLowerCase();
        return typeName.contains("wolf") || typeName.contains("fox") ||
               typeName.contains("dolphin") || typeName.contains("axolotl");
    }

    private boolean isAerialPredator(LivingEntity entity) {
        String typeName = entity.getType().toString().toLowerCase();
        return typeName.contains("phantom") || typeName.contains("bee") ||
               typeName.contains("parrot") || entity.isOnFire();
    }

    /**
     * Finds distance to nearest refuge (shelter, hiding spot).
     *
     * @param context Behavior context
     * @return Distance to nearest refuge, or large value if none found
     */
    private double findNearestRefugeDistance(BehaviorContext context) {
        // Simplified refuge detection - check for nearby blocks that provide cover
        // In full implementation, would check for water, caves, dense foliage, etc.
        return Double.MAX_VALUE;
    }

    /**
     * Counts nearby herd members of the same species.
     *
     * @param context Behavior context
     * @return Number of nearby herd members
     */
    private int countNearbyHerdMembers(BehaviorContext context) {
        Mob entity = context.getEntity();
        double detectionRange = 16.0;

        List<Mob> nearby = entity.level().getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(detectionRange)
        );

        int count = 0;
        for (Mob other : nearby) {
            if (other != entity && other.getType() == entity.getType()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Calculates Euclidean distance between entity and threat.
     */
    private double calculateDistance(BehaviorContext context, LivingEntity threat) {
        Vec3d entityPos = context.getPosition();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        return entityPos.distanceTo(threatPos);
    }

    /**
     * Increases habituation level after non-lethal threat encounters.
     * Reduces FID over time as animals learn to tolerate non-threatening presence.
     *
     * @param amount Amount to increase (0.0 - 1.0)
     */
    public void increaseHabituation(double amount) {
        habituationLevel = Math.min(1.0, habituationLevel + amount);
    }

    /**
     * Resets habituation after a threatening event (attack, injury).
     */
    public void resetHabituation() {
        habituationLevel = 0.0;
    }

    /**
     * Gets the last detected threat.
     */
    public LivingEntity getLastThreat() {
        return lastThreat;
    }

    /**
     * Gets the time of last threat detection.
     */
    public long getLastThreatTime() {
        return lastThreatTime;
    }

    /**
     * Gets the current config.
     */
    public FleeingConfig getConfig() {
        return config;
    }

    /**
     * Gets current habituation level.
     */
    public double getHabituationLevel() {
        return habituationLevel;
    }
}
