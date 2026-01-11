package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

/**
 * Avoidance behavior implementing Flight Initiation Distance (FID) based on economic escape theory.
 * Based on Ydenberg & Dill (1986): prey flee when cost of staying equals cost of fleeing.
 * Unlike evasion (reactive fleeing), avoidance is proactive - maintaining assessment of threat level.
 */
public class AvoidanceBehavior extends SteeringBehavior {

    private final double avoidanceRadius;
    private final double maxAvoidanceForce;
    private final double detectionRange;
    private final double flightInitiationDistance;
    private final double refugeDistance;

    public AvoidanceBehavior(double avoidanceRadius, double maxAvoidanceForce,
                            double detectionRange, double flightInitiationDistance,
                            double refugeDistance) {
        this.avoidanceRadius = avoidanceRadius;
        this.maxAvoidanceForce = maxAvoidanceForce;
        this.detectionRange = detectionRange;
        this.flightInitiationDistance = flightInitiationDistance;
        this.refugeDistance = refugeDistance;
    }

    public AvoidanceBehavior(double avoidanceRadius, double maxAvoidanceForce,
                            double detectionRange) {
        this(avoidanceRadius, maxAvoidanceForce, detectionRange, 16.0, 32.0);
    }

    public AvoidanceBehavior() {
        this(8.0, 0.3, 16.0, 16.0, 32.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof Mob mob)) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();
        Vec3d avoidanceForce = new Vec3d();

        int count = 0;
        double maxThreatLevel = 0.0;

        // Find all nearby threats to avoid
        List<LivingEntity> nearbyEntities = mob.level().getEntitiesOfClass(
            LivingEntity.class,
            mob.getBoundingBox().inflate(detectionRange)
        );

        for (LivingEntity other : nearbyEntities) {
            if (other.equals(entity)) {
                continue;
            }

            if (!shouldAvoid(mob, other)) {
                continue;
            }

            Vec3d otherPos = new Vec3d(other.getX(), other.getY(), other.getZ());
            double distance = position.distanceTo(otherPos);

            // Calculate FID based on economic escape theory
            // Flee when: cost of staying >= cost of fleeing
            double fid = calculateFlightInitiationDistance(mob, other, distance);

            if (distance < fid && distance > 0) {
                // Calculate avoidance force (stronger when closer)
                Vec3d diff = Vec3d.sub(position, otherPos);
                diff.normalize();

                // Weight by distance and threat level (closer = stronger avoidance)
                double weight = (fid - distance) / fid;
                double threatLevel = calculateThreatLevel(mob, other, distance);
                diff.mult(weight * threatLevel);

                avoidanceForce.add(diff);
                count++;
                maxThreatLevel = Math.max(maxThreatLevel, threatLevel);
            }
        }

        if (count > 0) {
            // Normalize and scale to max speed
            avoidanceForce.normalize();

            // Desired velocity is directly away from threats
            // Speed increases with threat level
            double desiredSpeed = (context.getSpeed() > 0 ? context.getSpeed() : 0.5) * (1.0 + maxThreatLevel);
            Vec3d desired = avoidanceForce.copy();
            desired.mult(Math.min(desiredSpeed, 1.5));

            // Reynolds steering formula: steer = desired - velocity
            Vec3d steer = Vec3d.sub(desired, velocity);
            steer.limit(maxAvoidanceForce);
            return steer;
        }

        return new Vec3d();
    }

    /**
     * Calculates Flight Initiation Distance based on economic escape theory.
     * Based on Ydenberg & Dill (1986): prey flee when cost of staying equals cost of fleeing.
     *
     * Factors affecting FID:
     * - Distance to refuge (closer refuge = later flee)
     * - Predator approach speed (faster approach = earlier flee)
     * - Prey condition (injured/young = earlier flee)
     * - Group size (dilution effect = later flee when in groups)
     */
    private double calculateFlightInitiationDistance(Mob prey, LivingEntity threat, double currentDistance) {
        double baseFid = flightInitiationDistance;

        // Adjust for refuge availability (closer refuge = feel safer, flee later)
        double distanceToRefuge = findDistanceToRefuge(prey);
        if (distanceToRefuge < refugeDistance) {
            // Have nearby refuge, can afford to wait longer
            baseFid *= 0.7;
        } else {
            // No refuge nearby, need to flee earlier
            baseFid *= 1.3;
        }

        // Adjust for predator speed (faster predators require earlier flight)
        double predatorSpeed = getEntitySpeed(threat);
        if (predatorSpeed > 0.3) {
            baseFid *= 1.2;
        }

        // Adjust for prey condition (young/injured flee earlier)
        if (prey instanceof Animal animal) {
            if (animal.isBaby()) {
                baseFid *= 1.5; // Babies are more cautious
            }
            double healthPercent = animal.getHealth() / animal.getMaxHealth();
            if (healthPercent < 0.5) {
                baseFid *= 1.3; // Injured animals more cautious
            }
        }

        // Adjust for group size (dilution effect - safety in numbers)
        int nearbyConspecifics = countNearbyConspecifics(prey);
        if (nearbyConspecifics > 3) {
            baseFid *= 0.8; // Feel safer in groups
        }

        return baseFid;
    }

    /**
     * Calculates threat level based on predator type, distance, and approach behavior.
     * Returns 0.0 to 1.0 where 1.0 is maximum threat.
     */
    private double calculateThreatLevel(Mob prey, LivingEntity threat, double distance) {
        double threatLevel = 0.5;

        // Ambush predators (creeping) are more dangerous when close
        String typeName = threat.getType().toString().toLowerCase();
        boolean isAmbushPredator = typeName.contains("cat") || typeName.contains("spider");

        // Cursorial predators (chasing) maintain high threat at distance
        boolean isCursorialPredator = typeName.contains("wolf") || typeName.contains("fox");

        if (isAmbushPredator) {
            // Ambush predators: threat increases sharply as distance decreases
            threatLevel = 1.0 - (distance / (flightInitiationDistance * 1.5));
            threatLevel = Math.max(0.2, Math.min(1.0, threatLevel * 1.5));
        } else if (isCursorialPredator) {
            // Cursorial predators: sustained high threat
            threatLevel = 0.6 + 0.4 * (1.0 - distance / (flightInitiationDistance * 2.0));
        } else {
            // Default threat calculation
            threatLevel = 1.0 - (distance / (flightInitiationDistance * 1.2));
        }

        return Math.max(0.1, Math.min(1.0, threatLevel));
    }

    /**
     * Finds distance to nearest refuge (cover, water, etc.)
     * Checks solid blocks overhead and water nearby.
     */
    private double findDistanceToRefuge(Mob mob) {
        // Check if under cover (trees, overhangs)
        if (mob.level().getBlockState(mob.blockPosition().above(2)).isSolidRender(mob.level(), mob.blockPosition().above(2))) {
            return 0.0;
        }

        // Check if currently in water (many animals can escape predators in water)
        if (!mob.level().getFluidState(mob.blockPosition()).isEmpty()) {
            return 0.0;
        }

        // Check for nearby water as potential refuge
        BlockPos mobPos = mob.blockPosition();
        int searchRadius = 5;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = mobPos.offset(x, y, z);
                    if (!mob.level().getFluidState(checkPos).isEmpty()) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        return distance;
                    }
                }
            }
        }

        return Double.MAX_VALUE;
    }

    /**
     * Gets the movement speed attribute of an entity.
     */
    private double getEntitySpeed(LivingEntity entity) {
        return entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
    }

    /**
     * Counts nearby entities of the same species.
     * Used for dilution effect calculation.
     */
    private int countNearbyConspecifics(Mob mob) {
        int count = 0;
        double range = 16.0;

        List<LivingEntity> nearby = mob.level().getEntitiesOfClass(
            LivingEntity.class,
            mob.getBoundingBox().inflate(range)
        );

        for (LivingEntity other : nearby) {
            if (!other.equals(mob) && other.getType().equals(mob.getType())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Determines if the entity should avoid the other entity.
     */
    private boolean shouldAvoid(Mob entity, LivingEntity other) {
        // Avoid players
        if (other instanceof net.minecraft.world.entity.player.Player player) {
            return !player.isShiftKeyDown();
        }

        // Avoid predators
        String typeName = other.getType().toString().toLowerCase();
        if (typeName.contains("wolf") || typeName.contains("fox") ||
            typeName.contains("cat") || typeName.contains("ocelot") ||
            typeName.contains("spider") || typeName.contains("phantom")) {
            return true;
        }

        // Avoid aggressive mobs
        if (other instanceof Mob && ((Mob) other).isAggressive()) {
            return true;
        }

        return false;
    }

    public double getAvoidanceRadius() {
        return avoidanceRadius;
    }

    public double getMaxAvoidanceForce() {
        return maxAvoidanceForce;
    }

    public double getDetectionRange() {
        return detectionRange;
    }

    public double getFlightInitiationDistance() {
        return flightInitiationDistance;
    }

    public double getRefugeDistance() {
        return refugeDistance;
    }
}
