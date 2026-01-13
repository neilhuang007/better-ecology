package me.javavirtualenv.behavior.rabbit;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Rabbit-specific evasion behavior with rapid zigzag patterns.
 * <p>
 * Rabbits are small, fast prey animals that rely on:
 * - Explosive acceleration from a standstill
 * - Unpredictable zigzag evasion patterns
 * - Quick bursts of speed rather than sustained running
 * - Freezing behavior to avoid detection
 * <p>
 * Based on research into lagomorph escape strategies.
 */
public class RabbitEvasionBehavior {

    private final RabbitEvasionConfig config;
    private final Random random = new Random();

    // Evasion state
    private boolean isEvading = false;
    private Entity currentThreat = null;
    private int evasionTimer = 0;
    private int lastHurtTime = -1000;

    // Zigzag state
    private int zigzagTimer = 0;
    private int zigzagDirection = 1;
    private double zigzagIntensity = 0.0;

    // Freezing state
    private boolean isFrozen = false;
    private int freezeTimer = 0;

    public RabbitEvasionBehavior(RabbitEvasionConfig config) {
        this.config = config;
    }

    public RabbitEvasionBehavior() {
        this(RabbitEvasionConfig.createDefault());
    }

    /**
     * Calculates evasion steering force.
     *
     * @param context Behavior context
     * @return Evasion force vector
     */
    public Vec3d calculate(BehaviorContext context) {
        Mob entity = context.getEntity();

        // Check if recently hurt - this triggers immediate evasion
        boolean wasRecentlyHurt = checkRecentlyHurt(entity);

        // Check if currently freezing
        if (isFrozen) {
            // Break freeze if recently hurt
            if (wasRecentlyHurt) {
                isFrozen = false;
                freezeTimer = 0;
            } else {
                updateFreezeState(context);
                if (isFrozen) {
                    return new Vec3d(); // Continue freezing - no movement
                }
            }
        }

        // Find nearest threat
        LivingEntity threat = findNearestThreat(entity);

        if (threat == null || !threat.isAlive()) {
            // Stop evading only if not recently hurt
            if (!wasRecentlyHurt) {
                currentThreat = null;
                isEvading = false;
                setRetreatingState(entity, false);
                return new Vec3d();
            }
        }

        // Use found threat or keep current threat if recently hurt
        if (threat != null) {
            currentThreat = threat;
        } else if (wasRecentlyHurt && currentThreat == null) {
            // Recently hurt but no visible threat - still evade in random direction
            isEvading = true;
            evasionTimer++;
            setRetreatingState(entity, true);
            return calculatePanicEvasion(context);
        }

        Vec3d position = context.getPosition();
        Vec3d threatPos = new Vec3d(currentThreat.getX(), currentThreat.getY(), currentThreat.getZ());
        double distance = position.distanceTo(threatPos);

        // Decide whether to freeze or flee
        if (distance > config.getFlightInitiationDistance() * 0.7 && !wasRecentlyHurt) {
            // Far enough that we might freeze first (but not if recently hurt)
            if (config.canFreeze() && !isEvading && shouldFreeze(entity, (LivingEntity)currentThreat)) {
                initiateFreeze();
                return new Vec3d();
            }
        }

        // Start evading if threat is close enough OR recently hurt
        if (distance < config.getFlightInitiationDistance() || wasRecentlyHurt) {
            isEvading = true;
            evasionTimer++;
            setRetreatingState(entity, true);

            // Break freeze if threat gets too close
            if (isFrozen && distance < config.getFlightInitiationDistance() * 0.5) {
                isFrozen = false;
                freezeTimer = 0;
            }

            return calculateEvasion(context, (LivingEntity)currentThreat);
        } else if (distance > config.getSafetyDistance()) {
            // Escaped to safety
            isEvading = false;
            evasionTimer = 0;
            setRetreatingState(entity, false);
            return new Vec3d();
        }

        return new Vec3d();
    }

    /**
     * Checks if the entity was recently hurt.
     * This triggers immediate evasion response.
     */
    private boolean checkRecentlyHurt(Mob entity) {
        int currentHurtTime = entity.hurtTime;
        if (currentHurtTime > 0 && currentHurtTime != lastHurtTime) {
            lastHurtTime = currentHurtTime;
            return true;
        }
        return false;
    }

    /**
     * Calculates panic evasion when hurt but no visible threat.
     * Rabbits flee in a semi-random direction when attacked.
     */
    private Vec3d calculatePanicEvasion(BehaviorContext context) {
        Vec3d velocity = context.getVelocity();

        // Choose a random evasion direction
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3d evasionDirection = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
        evasionDirection.normalize();
        evasionDirection.mult(config.getEvasionSpeed());

        // Calculate steering force
        Vec3d steer = Vec3d.sub(evasionDirection, velocity);
        steer.limit(config.getEvasionForce());

        return steer;
    }

    /**
     * Calculates evasion force with rabbit-specific zigzag pattern.
     * Rabbits use more erratic, tighter zigzags than larger animals.
     */
    private Vec3d calculateEvasion(BehaviorContext context, LivingEntity threat) {
        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());

        // Base direction: away from threat
        Vec3d awayFromThreat = Vec3d.sub(position, threatPos);
        awayFromThreat.normalize();

        // Update zigzag pattern with rabbit-specific timing
        zigzagTimer++;

        // Rabbits change direction more frequently than larger animals
        int changeInterval = config.getZigzagChangeInterval();

        // Random changes for unpredictability
        if (zigzagTimer >= changeInterval) {
            zigzagTimer = 0;
            // Rabbits use more extreme direction changes
            zigzagDirection = random.nextBoolean() ? 1 : -1;
            // Vary intensity for unpredictability
            zigzagIntensity = 0.3 + (random.nextDouble() * 0.4);
        }

        // Calculate perpendicular vector for zigzag
        Vec3d perpendicular = new Vec3d(-awayFromThreat.z, 0, awayFromThreat.x);

        // Apply zigzag with sinusoidal variation for smoothness
        double zigzagOffset = Math.sin(zigzagTimer * 0.8) * zigzagIntensity;

        perpendicular.mult(zigzagDirection * zigzagOffset);

        // Combine evasion direction with zigzag
        Vec3d evasionDirection = awayFromThreat.copy();
        evasionDirection.add(perpendicular);
        evasionDirection.normalize();

        // Apply speed
        evasionDirection.mult(config.getEvasionSpeed());

        // Calculate steering force
        Vec3d steer = Vec3d.sub(evasionDirection, velocity);

        // Add random jitter for extra unpredictability
        double jitterAmount = 0.1;
        steer.x += (random.nextDouble() - 0.5) * jitterAmount;
        steer.z += (random.nextDouble() - 0.5) * jitterAmount;

        // Limit steering force
        steer.limit(config.getEvasionForce());

        // Add slight vertical component for jumping
        // Rabbits jump over obstacles while fleeing
        if (random.nextDouble() < config.getJumpChance()) {
            steer.y += 0.2;
        }

        return steer;
    }

    /**
     * Initiates freezing behavior.
     */
    private void initiateFreeze() {
        isFrozen = true;
        freezeTimer = config.getFreezeDuration();
    }

    /**
     * Updates freezing state.
     */
    private void updateFreezeState(BehaviorContext context) {
        freezeTimer--;

        // Check if threat is getting too close
        LivingEntity threat = findNearestThreat(context.getEntity());
        if (threat != null) {
            Vec3d position = context.getPosition();
            Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
            double distance = position.distanceTo(threatPos);

            // Break freeze if threat is too close
            if (distance < config.getFlightInitiationDistance() * 0.4) {
                freezeTimer = 0;
                isFrozen = false;
            }
        }

        if (freezeTimer <= 0) {
            isFrozen = false;
        }
    }

    /**
     * Determines if rabbit should freeze instead of fleeing.
     * Freezing is effective when not yet detected.
     */
    private boolean shouldFreeze(Mob entity, LivingEntity threat) {
        // Don't freeze if already moving
        if (entity.getDeltaMovement().length() > 0.1) {
            return false;
        }

        // Check if threat is looking at us
        Vec3d rabbitPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        Vec3d toRabbit = Vec3d.sub(rabbitPos, threatPos);

        Vec3 threatLookDir = threat.getLookAngle();
        Vec3d lookDir = new Vec3d(threatLookDir.x, threatLookDir.y, threatLookDir.z);

        // Normalize
        toRabbit.normalize();
        lookDir.normalize();

        // Dot product to check if threat is looking toward rabbit
        double dotProduct = toRabbit.x * lookDir.x + toRabbit.y * lookDir.y + toRabbit.z * lookDir.z;

        // Freeze if threat is not looking directly at us
        return dotProduct < 0.5;
    }

    /**
     * Finds the nearest threat entity.
     */
    private LivingEntity findNearestThreat(Mob prey) {
        // Check current threat first
        if (currentThreat != null && currentThreat.isAlive()) {
            double distance = prey.position().distanceTo(currentThreat.position());
            if (distance < config.getDetectionRange() * 1.5) {
                return (LivingEntity) currentThreat;
            }
        }

        LivingEntity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        List<LivingEntity> nearbyEntities = prey.level().getEntitiesOfClass(
            LivingEntity.class,
            prey.getBoundingBox().inflate(config.getDetectionRange())
        );

        for (LivingEntity entity : nearbyEntities) {
            if (entity.equals(prey)) {
                continue;
            }

            if (!isThreat(prey, entity)) {
                continue;
            }

            double distance = prey.position().distanceTo(entity.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestThreat = entity;
            }
        }

        return nearestThreat;
    }

    /**
     * Determines if an entity is a threat to the rabbit.
     */
    private boolean isThreat(Mob prey, LivingEntity entity) {
        // Players are threats (unless sneaking)
        if (entity instanceof Player player) {
            return !player.isShiftKeyDown();
        }

        // Predators are threats
        String typeName = entity.getType().toString().toLowerCase();
        return typeName.contains("wolf") ||
               typeName.contains("fox") ||
               typeName.contains("cat") ||
               typeName.contains("ocelot") ||
               typeName.contains("spider") ||
               typeName.contains("phantom") ||
               (entity instanceof Mob && ((Mob) entity).isAggressive());
    }

    /**
     * Sets the retreating state on the entity's ecology component.
     */
    private void setRetreatingState(Mob entity, boolean retreating) {
        if (entity instanceof EcologyAccess access) {
            EcologyComponent component = access.betterEcology$getEcologyComponent();
            if (component != null) {
                component.state().setIsRetreating(retreating);
            }
        }
    }

    // Getters and setters

    public boolean isEvading() {
        return isEvading;
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    public Entity getCurrentThreat() {
        return currentThreat;
    }

    public int getEvasionTimer() {
        return evasionTimer;
    }

    public RabbitEvasionConfig getConfig() {
        return config;
    }

    public void setEvading(boolean evading) {
        this.isEvading = evading;
    }

    public void reset() {
        isEvading = false;
        currentThreat = null;
        evasionTimer = 0;
        lastHurtTime = -1000;
        zigzagTimer = 0;
        zigzagDirection = 1;
        zigzagIntensity = 0.0;
        isFrozen = false;
        freezeTimer = 0;
    }
}
