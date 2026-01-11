package me.javavirtualenv.behavior.fleeing;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * Implements group panic and stampede behavior triggered by multiple threats.
 * Coordinates collective anti-predator response based on research findings.
 * <p>
 * Based on research by:
 * - Yang et al. (2021) - Crowded-selfish-herd model with confusion effect
 * - Various studies on stampedes in dolphins, ungulates, and fish
 * <p>
 * Key behaviors:
 * - Panic triggered when multiple threats or very close threats detected
 * - All nearby animals flee together in coordinated stampede
 * - Speed multiplier during stampede (1.2-2.0x normal speed)
 * - Contagious panic spreading through herd
 * - Selfish herd positioning (individuals minimize personal risk)
 */
public class PanicBehavior extends SteeringBehavior {

    private final FleeingConfig config;

    // Panic state
    private boolean isInPanic = false;
    private int panicTimer = 0;
    private UUID panicSourceId = null;

    // Stampede direction
    private Vec3d stampedeDirection = new Vec3d();

    // Panic statistics
    private int threatCount = 0;
    private double panicIntensity = 0.0;

    // Cooldowns
    private int panicCooldown = 0;
    private static final int PANIC_DURATION = 200; // ticks (10 seconds)
    private static final int PANIC_COOLDOWN_TIME = 600; // ticks (30 seconds)
    private static final int MINIMUM_FLEE_DURATION = 80; // ticks (4 seconds) - minimum time to continue fleeing after threat clears

    public PanicBehavior(FleeingConfig config) {
        this.config = config;
    }

    public PanicBehavior() {
        this(FleeingConfig.createDefault());
    }

    public PanicBehavior(FleeingConfig config, double weight) {
        this.config = config;
        this.weight = weight;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!enabled) {
            return new Vec3d();
        }

        // Update cooldown
        if (panicCooldown > 0) {
            panicCooldown--;
            return new Vec3d();
        }

        // Update panic state
        updatePanicState(context);

        // If not in panic, return zero vector
        if (!isInPanic) {
            return new Vec3d();
        }

        // Calculate stampede force
        Vec3d stampedeForce = calculateStampedeForce(context);

        // Apply speed multiplier
        stampedeForce.mult(config.getStampedeSpeedMultiplier());

        // Apply weight
        stampedeForce.mult(weight);

        return stampedeForce;
    }

    /**
     * Updates panic state based on current conditions.
     * Triggers panic when threshold is met or when panic spreads from nearby animals.
     *
     * @param context Behavior context
     */
    private void updatePanicState(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null) {
            // Cannot update panic state without entity
            return;
        }

        long currentTime = entity.level().getGameTime();

        // Check if panic should end
        if (isInPanic && panicTimer > 0) {
            panicTimer--;

            if (panicTimer <= 0) {
                // Clear panic state from EcologyComponent
                EcologyComponent component = EcologyHooks.getEcologyComponent(entity);
                if (component != null) {
                    component.state().setIsPanicking(false);
                }
                endPanic();
                return;
            }
        }

        // Check if panic is spreading from nearby animals
        boolean nearbyPanic = checkNearbyPanic(context);

        // Assess current threat level
        threatCount = countNearbyThreats(context);
        boolean veryCloseThreat = hasVeryCloseThreat(context);

        // Determine if panic should be triggered
        boolean shouldPanic = nearbyPanic ||
                            threatCount >= config.getPanicThreshold() ||
                            (veryCloseThreat && threatCount >= 1);

        if (shouldPanic && !isInPanic) {
            triggerPanic(context);
        } else if (!shouldPanic && isInPanic) {
            // Gradual wind-down if threats have cleared
            // Only wind down after minimum flee duration has passed
            if (panicTimer > MINIMUM_FLEE_DURATION) {
                panicTimer = Math.max(panicTimer - 2, MINIMUM_FLEE_DURATION);
            }
        }

        // Update panic intensity based on threat density
        updatePanicIntensity(context);
    }

    /**
     * Calculates the force vector for stampede movement.
     * Combines threat avoidance with herd cohesion during panic.
     * Each entity calculates its own flee direction based on visible threats,
     * falling back to cached stampede direction only during blind panic.
     *
     * @param context Behavior context
     * @return Stampede force vector
     */
    private Vec3d calculateStampedeForce(BehaviorContext context) {
        Vec3d position = context.getPosition();

        // Priority 1: Always recalculate direction based on visible threat
        // This ensures each entity flees away from the threat relative to its own position
        LivingEntity primaryThreat = findPrimaryThreat(context);
        if (primaryThreat != null) {
            Vec3d threatPos = new Vec3d(
                primaryThreat.getX(),
                primaryThreat.getY(),
                primaryThreat.getZ()
            );

            Vec3d awayFromThreat = Vec3d.sub(position, threatPos);
            awayFromThreat.normalize();

            // Update stampede direction for this entity
            stampedeDirection = awayFromThreat;

            // Add some noise for natural variation
            Vec3d noise = generateStampedeNoise();
            Vec3d force = awayFromThreat.copy();
            force.add(noise);
            force.normalize();
            return force;
        }

        // Priority 2: No visible threat - use cached stampede direction (blind panic)
        if (stampedeDirection.magnitude() > 0.1) {
            // Add some noise for natural variation
            Vec3d noise = generateStampedeNoise();
            Vec3d force = stampedeDirection.copy();
            force.add(noise);
            force.normalize();
            return force;
        }

        // Priority 3: No cached direction - continue in current movement direction
        Vec3d currentDir = context.getVelocity().copy();
        if (currentDir.magnitude() > 0.1) {
            currentDir.normalize();
            stampedeDirection = currentDir;
            return currentDir;
        }

        // Fallback: move in random direction
        Vec3d randomDir = generateRandomDirection();
        stampedeDirection = randomDir;
        return randomDir;
    }

    /**
     * Generates noise vector for natural stampede variation.
     * Helps prevent unnatural alignment during panic.
     *
     * @return Small random vector
     */
    private Vec3d generateStampedeNoise() {
        double noiseAmount = 0.2 * panicIntensity;
        double angle = Math.random() * Math.PI * 2;

        Vec3d noise = new Vec3d(
            Math.cos(angle) * noiseAmount,
            0,
            Math.sin(angle) * noiseAmount
        );

        return noise;
    }

    /**
     * Generates a random horizontal direction vector.
     *
     * @return Random normalized direction
     */
    private Vec3d generateRandomDirection() {
        double angle = Math.random() * Math.PI * 2;
        Vec3d dir = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
        dir.normalize();
        return dir;
    }

    /**
     * Triggers panic state for this entity and nearby animals.
     *
     * @param context Behavior context
     */
    private void triggerPanic(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null) {
            // Cannot trigger panic without entity, just set panic state
            isInPanic = true;
            panicTimer = PANIC_DURATION;
            return;
        }

        isInPanic = true;
        panicTimer = PANIC_DURATION;
        panicSourceId = entity.getUUID();

        // Set panic state on entity's EcologyComponent
        EcologyComponent component = EcologyHooks.getEcologyComponent(entity);
        if (component != null) {
            component.state().setIsPanicking(true);
        }

        // Calculate initial stampede direction
        LivingEntity primaryThreat = findPrimaryThreat(context);
        if (primaryThreat != null) {
            Vec3d position = context.getPosition();
            Vec3d threatPos = new Vec3d(
                primaryThreat.getX(),
                primaryThreat.getY(),
                primaryThreat.getZ()
            );
            stampedeDirection = Vec3d.sub(position, threatPos);
            stampedeDirection.normalize();
        }

        // Spread panic to nearby herd members
        spreadPanicToHerd(context);
    }

    /**
     * Spreads panic to nearby animals of the same species.
     * Implements contagious panic behavior by setting panic state on neighbors.
     *
     * @param context Behavior context
     */
    private void spreadPanicToHerd(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null) {
            return;
        }

        double propagationRange = config.getPanicPropagationRange();

        List<Mob> nearbyAnimals = entity.level().getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(propagationRange)
        );

        for (Mob nearby : nearbyAnimals) {
            // Skip self
            if (nearby == entity) {
                continue;
            }

            // Only spread to same species
            if (nearby.getType() != entity.getType()) {
                continue;
            }

            // Don't spread to already panicked animals
            if (isEntityInPanic(nearby)) {
                continue;
            }

            // Set panic state on nearby animal's EcologyComponent
            // Their PanicBehavior will detect this and trigger panic on next tick
            EcologyComponent nearbyComponent = EcologyHooks.getEcologyComponent(nearby);
            if (nearbyComponent != null) {
                nearbyComponent.state().setIsPanicking(true);
            }
        }
    }

    /**
     * Checks if panic is spreading from nearby animals.
     * Looks for other animals that are already panicked.
     *
     * @param context Behavior context
     * @return true if nearby panic detected
     */
    private boolean checkNearbyPanic(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null) {
            return false;
        }

        double propagationRange = config.getPanicPropagationRange();

        // Check for entities with high speed (indicating panic)
        List<Mob> nearbyAnimals = entity.level().getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(propagationRange)
        );

        int panickedCount = 0;

        for (Mob nearby : nearbyAnimals) {
            if (nearby == entity) {
                continue;
            }

            if (nearby.getType() != entity.getType()) {
                continue;
            }

            // Check if nearby animal has panic state set
            EcologyComponent nearbyComponent = EcologyHooks.getEcologyComponent(nearby);
            boolean hasPanicState = nearbyComponent != null && nearbyComponent.state().isPanicking();

            // Check if nearby animal appears panicked (high speed, fleeing)
            if (hasPanicState || isEntityInPanic(nearby) || isEntityFleeing(nearby)) {
                panickedCount++;

                // Panic spreads if multiple animals are panicked
                if (panickedCount >= 1) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Counts the number of nearby threats.
     *
     * @param context Behavior context
     * @return Number of threats
     */
    private int countNearbyThreats(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null || context.getLevel() == null) {
            return 0;
        }

        Vec3d position = context.getPosition();
        double detectionRange = config.getFlightInitiationDistance() * 1.5;

        List<LivingEntity> nearbyEntities = context.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(detectionRange)
        );

        int count = 0;

        for (LivingEntity nearby : nearbyEntities) {
            if (isThreat(context, nearby)) {
                Vec3d threatPos = new Vec3d(nearby.getX(), nearby.getY(), nearby.getZ());
                double distance = position.distanceTo(threatPos);

                if (distance <= detectionRange) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Checks if there's a very close threat (critical distance).
     *
     * @param context Behavior context
     * @return true if very close threat detected
     */
    private boolean hasVeryCloseThreat(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null || context.getLevel() == null) {
            return false;
        }

        Vec3d position = context.getPosition();
        double criticalDistance = config.getFlightInitiationDistance() * 0.5;

        List<LivingEntity> nearbyEntities = context.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(criticalDistance)
        );

        for (LivingEntity nearby : nearbyEntities) {
            if (isThreat(context, nearby)) {
                Vec3d threatPos = new Vec3d(nearby.getX(), nearby.getY(), nearby.getZ());
                double distance = position.distanceTo(threatPos);

                if (distance <= criticalDistance) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Finds the primary threat (closest or most dangerous).
     *
     * @param context Behavior context
     * @return Primary threat entity, or null if none found
     */
    private LivingEntity findPrimaryThreat(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null || context.getLevel() == null) {
            return null;
        }

        Vec3d position = context.getPosition();
        double detectionRange = config.getFlightInitiationDistance() * 1.5;

        List<LivingEntity> nearbyEntities = context.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(detectionRange)
        );

        LivingEntity primaryThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity nearby : nearbyEntities) {
            if (!isThreat(context, nearby)) {
                continue;
            }

            Vec3d threatPos = new Vec3d(nearby.getX(), nearby.getY(), nearby.getZ());
            double distance = position.distanceTo(threatPos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                primaryThreat = nearby;
            }
        }

        return primaryThreat;
    }

    /**
     * Updates panic intensity based on threat density and proximity.
     * Higher intensity = faster, more chaotic stampede.
     *
     * @param context Behavior context
     */
    private void updatePanicIntensity(BehaviorContext context) {
        // Base intensity on threat count
        double baseIntensity = Math.min(1.0, threatCount / 5.0);

        // Increase if very close threat
        if (hasVeryCloseThreat(context)) {
            baseIntensity = Math.min(1.0, baseIntensity + 0.3);
        }

        // Increase if herd is large (contagious effect)
        int herdSize = countNearbyHerdMembers(context);
        if (herdSize > 5) {
            baseIntensity = Math.min(1.0, baseIntensity + 0.2);
        }

        panicIntensity = baseIntensity;
    }

    /**
     * Ends panic state and initiates cooldown.
     */
    private void endPanic() {
        isInPanic = false;
        panicTimer = 0;
        panicCooldown = PANIC_COOLDOWN_TIME;
        stampedeDirection = new Vec3d();
        panicIntensity = 0.0;

        // Clear panic state from entity's EcologyComponent
        // Note: We can't access the entity here directly without context,
        // so the state will be cleared naturally on next tick via prepareForTick()
        // The isInPanic flag will prevent panic from triggering again until cooldown expires
    }

    /**
     * Counts nearby herd members of the same species.
     */
    private int countNearbyHerdMembers(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (entity == null) {
            return 0;
        }

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
     * Determines if an entity is a threat.
     */
    private boolean isThreat(BehaviorContext context, LivingEntity entity) {
        // Players are threats
        if (entity instanceof Player) {
            return !entity.isShiftKeyDown();
        }

        // Known predator types
        String typeName = entity.getType().toString().toLowerCase();
        if (typeName.contains("wolf") || typeName.contains("fox") ||
            typeName.contains("spider") || typeName.contains("phantom") ||
            typeName.contains("cat") || typeName.contains("bee")) {
            return true;
        }

        // Aggressive mobs
        if (entity instanceof Mob mob && mob.isAggressive()) {
            return true;
        }

        return false;
    }

    /**
     * Checks if another entity appears to be in panic state.
     * This is a simplified check - in full implementation would use entity data.
     */
    private boolean isEntityInPanic(Mob entity) {
        if (entity == null) {
            return false;
        }
        // Check if entity is moving at high speed
        double speed = entity.getDeltaMovement().length();
        return speed > 0.3; // High movement speed suggests panic
    }

    /**
     * Checks if entity is actively fleeing.
     */
    private boolean isEntityFleeing(Mob entity) {
        if (entity == null) {
            return false;
        }
        // Check if entity is moving away from player or predator
        // Simplified: check if moving at above-normal speed
        double speed = entity.getDeltaMovement().length();
        return speed > 0.2;
    }

    // Getters and setters

    public boolean isInPanic() {
        return isInPanic;
    }

    public int getPanicTimer() {
        return panicTimer;
    }

    public double getPanicIntensity() {
        return panicIntensity;
    }

    public int getThreatCount() {
        return threatCount;
    }

    public Vec3d getStampedeDirection() {
        return stampedeDirection.copy();
    }

    public FleeingConfig getConfig() {
        return config;
    }

    /**
     * Manually triggers panic (useful for testing or scripted events).
     *
     * @param context Behavior context
     */
    public void forcePanic(BehaviorContext context) {
        triggerPanic(context);
    }

    /**
     * Manually ends panic.
     */
    public void forceEndPanic() {
        endPanic();
    }

    /**
     * Resets panic state.
     */
    public void reset() {
        isInPanic = false;
        panicTimer = 0;
        panicCooldown = 0;
        stampedeDirection = new Vec3d();
        panicIntensity = 0.0;
        threatCount = 0;
    }
}
