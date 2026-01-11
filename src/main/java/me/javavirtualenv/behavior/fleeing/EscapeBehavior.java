package me.javavirtualenv.behavior.fleeing;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Calculates escape trajectory vectors based on selected escape strategy.
 * Implements different escape patterns: straight, zigzag, refuge-seeking, and freezing.
 * <p>
 * Based on research by:
 * - Moore et al. (2017) - Unpredictability of escape trajectories (106+ citations)
 * - Kawabata et al. (2023) - Multiple preferred escape trajectories
 * - Humphries & Driver (1970) - Protean movement for unpredictable evasion
 */
public class EscapeBehavior extends SteeringBehavior {

    private final FleeingConfig config;
    private final Random random = new Random();

    // Zigzag state
    private int zigzagTimer = 0;
    private int zigzagDirection = 1;

    // Freezing state
    private int freezingTimer = 0;
    private boolean isFrozen = false;

    // Current strategy
    private EscapeStrategy currentStrategy;

    // Cached refuge position
    private BlockPos cachedRefuge;
    private long refugeCacheTime = 0;

    public EscapeBehavior(FleeingConfig config) {
        this.config = config;
        this.currentStrategy = config.getPrimaryStrategy();
    }

    public EscapeBehavior() {
        this(FleeingConfig.createDefault());
    }

    public EscapeBehavior(FleeingConfig config, EscapeStrategy strategy) {
        this.config = config;
        this.currentStrategy = strategy;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!enabled) {
            return new Vec3d();
        }

        // Check if currently freezing
        if (isFrozen) {
            updateFreezeState(context);
            if (isFrozen) {
                return new Vec3d(); // Continue freezing
            }
        }

        // Select appropriate strategy
        selectStrategy(context);

        // Calculate escape vector based on strategy
        Vec3d escapeVector = switch (currentStrategy) {
            case STRAIGHT -> calculateStraightEscape(context);
            case ZIGZAG -> calculateZigzagEscape(context);
            case REFUGE -> calculateRefugeEscape(context);
            case FREEZE -> initiateFreeze(context);
        };

        // Apply weight and limit
        escapeVector.mult(weight);
        return escapeVector;
    }

    /**
     * Calculates straight-line escape directly away from threat.
     * Most efficient when speed is primary advantage and terrain is open.
     *
     * @param context Behavior context
     * @return Escape vector pointing away from threat
     */
    private Vec3d calculateStraightEscape(BehaviorContext context) {
        LivingEntity threat = findNearestThreat(context);
        Vec3d position = context.getPosition();

        if (threat == null) {
            // No specific threat, move in current direction
            Vec3d result = context.getVelocity().copy();
            result.normalize();
            result.mult(0.5);
            return result;
        }

        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());

        // Calculate vector away from threat
        Vec3d awayFromThreat = Vec3d.sub(position, threatPos);
        awayFromThreat.normalize();

        // Add slight vertical component for jumping over obstacles
        awayFromThreat.y += 0.1;

        return awayFromThreat;
    }

    /**
     * Calculates zigzag escape with unpredictable directional changes.
     * Implements protean movement - adaptively unpredictable evasion.
     * <p>
     * Based on research showing unpredictability directly correlates with evasion success.
     *
     * @param context Behavior context
     * @return Escape vector with zigzag pattern
     */
    private Vec3d calculateZigzagEscape(BehaviorContext context) {
        LivingEntity threat = findNearestThreat(context);
        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();

        if (threat == null) {
            Vec3d result = velocity.copy();
            result.normalize();
            result.mult(0.5);
            return result;
        }

        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());

        // Base direction: away from threat
        Vec3d awayFromThreat = Vec3d.sub(position, threatPos);
        awayFromThreat.normalize();

        // Update zigzag state
        zigzagTimer++;
        int changeInterval = config.getZigzagChangeInterval();

        // Randomly change direction periodically
        if (zigzagTimer >= changeInterval) {
            zigzagTimer = 0;
            zigzagDirection = random.nextBoolean() ? 1 : -1;
        }

        // Calculate perpendicular vector for zigzag
        // Cross product with up vector gives horizontal perpendicular
        Vec3d perpendicular = new Vec3d(-awayFromThreat.z, 0, awayFromThreat.x);

        // Apply zigzag intensity with sinusoidal variation for smoothness
        double zigzagAmount = config.getZigzagIntensity();
        double zigzagOffset = Math.sin(zigzagTimer * 0.5) * zigzagAmount;

        perpendicular.mult(zigzagDirection * zigzagOffset);

        // Combine away vector with perpendicular component
        Vec3d escapeVector = awayFromThreat.copy();
        escapeVector.add(perpendicular);
        escapeVector.normalize();

        // Add slight randomization for additional unpredictability
        double randomAngle = (random.nextDouble() - 0.5) * 0.2;
        escapeVector.x += Math.cos(randomAngle) * 0.1;
        escapeVector.z += Math.sin(randomAngle) * 0.1;
        escapeVector.normalize();

        return escapeVector;
    }

    /**
     * Calculates escape toward nearest refuge or shelter.
     * Prioritizes reaching safety over maximizing distance from threat.
     *
     * @param context Behavior context
     * @return Escape vector pointing toward refuge
     */
    private Vec3d calculateRefugeEscape(BehaviorContext context) {
        Vec3d position = context.getPosition();
        Level level = context.getLevel();

        // Find or update cached refuge
        BlockPos refuge = findNearestRefuge(context);

        if (refuge == null) {
            // No refuge found, fall back to straight escape
            return calculateStraightEscape(context);
        }

        Vec3d refugePos = new Vec3d(
            refuge.getX() + 0.5,
            refuge.getY() + 0.5,
            refuge.getZ() + 0.5
        );

        // Calculate vector toward refuge
        Vec3d toRefuge = Vec3d.sub(refugePos, position);
        double distanceToRefuge = toRefuge.magnitude();

        if (distanceToRefuge < 2.0) {
            // Very close to refuge, slow down
            return new Vec3d();
        }

        toRefuge.normalize();

        // Check if path to refuge is clear
        if (!isPathClear(position, refugePos, level)) {
            // Path blocked, try alternative route or fall back
            return calculateAlternativeRefugePath(context, position, refugePos);
        }

        return toRefuge;
    }

    /**
     * Initiates freezing behavior - staying immobile to avoid detection.
     * Controlled by amygdala-PAG neural circuits in real animals.
     *
     * @param context Behavior context
     * @return Zero vector (no movement)
     */
    private Vec3d initiateFreeze(BehaviorContext context) {
        isFrozen = true;
        freezingTimer = config.getFreezingDuration();
        return new Vec3d();
    }

    /**
     * Updates freezing state and determines when to resume movement.
     *
     * @param context Behavior context
     */
    private void updateFreezeState(BehaviorContext context) {
        freezingTimer--;

        // Check if a threat is getting too close - break freeze and flee
        LivingEntity threat = findNearestThreat(context);
        if (threat != null) {
            Vec3d position = context.getPosition();
            Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
            double distance = position.distanceTo(threatPos);

            // Break freeze if threat is very close (critical distance)
            if (distance < config.getFlightInitiationDistance() * 0.3) {
                freezingTimer = 0;
                isFrozen = false;
                // Switch to primary escape strategy
                currentStrategy = config.getPrimaryStrategy();
            }
        }

        if (freezingTimer <= 0) {
            isFrozen = false;
            // After freezing, switch to primary escape strategy
            currentStrategy = config.getPrimaryStrategy();
        }
    }

    /**
     * Selects the appropriate escape strategy based on context.
     * May switch between primary and secondary strategies.
     */
    private void selectStrategy(BehaviorContext context) {
        // Don't change strategy if currently freezing
        if (isFrozen) {
            return;
        }

        // Check if we should use secondary strategy
        LivingEntity threat = findNearestThreat(context);

        if (threat == null) {
            // No immediate threat, reset to primary
            currentStrategy = config.getPrimaryStrategy();
            return;
        }

        Vec3d position = context.getPosition();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        double distance = position.distanceTo(threatPos);

        // Use secondary strategy if:
        // - Threat is very close and secondary is REFUGE (emergency shelter)
        // - Threat is ambush predator and secondary is FREEZE (freeze first)
        EscapeStrategy secondary = config.getSecondaryStrategy();

        if (distance < config.getFlightInitiationDistance() * 0.6) {
            if (secondary == EscapeStrategy.REFUGE) {
                currentStrategy = EscapeStrategy.REFUGE;
            }
        } else if (distance > config.getFlightInitiationDistance() * 0.8) {
            currentStrategy = config.getPrimaryStrategy();
        }
    }

    /**
     * Finds the nearest refuge position (shelter, hiding spot).
     * Looks for: water, caves, dense foliage, under trees, etc.
     *
     * @param context Behavior context
     * @return Position of nearest refuge, or null if none found
     */
    private BlockPos findNearestRefuge(BehaviorContext context) {
        if (context.getEntity() == null || context.getLevel() == null) {
            return null;
        }

        long currentTime = context.getEntity().level().getGameTime();

        // Use cached refuge if recent
        if (cachedRefuge != null && currentTime - refugeCacheTime < 100) {
            return cachedRefuge;
        }

        BlockPos center = context.getBlockPos();
        Level level = context.getLevel();
        double searchRange = config.getRefugeDetectionRange();

        BlockPos nearestRefuge = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search in expanding spiral pattern
        int searchRadius = (int) Math.ceil(searchRange);

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance > searchRange) {
                        continue;
                    }

                    if (isRefuge(level, pos)) {
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestRefuge = pos;
                        }
                    }
                }
            }
        }

        cachedRefuge = nearestRefuge;
        refugeCacheTime = currentTime;

        return nearestRefuge;
    }

    /**
     * Determines if a block position constitutes valid refuge.
     * Refuge includes: water, caves (darkness), leaves, bushes.
     *
     * @param level Level instance
     * @param pos   Position to check
     * @return true if position is refuge
     */
    private boolean isRefuge(Level level, BlockPos pos) {
        // Water is refuge for many animals
        if (level.getBlockState(pos).is(Blocks.WATER)) {
            return true;
        }

        // Leaves provide cover
        if (level.getBlockState(pos).is(Blocks.ACACIA_LEAVES) ||
            level.getBlockState(pos).is(Blocks.BIRCH_LEAVES) ||
            level.getBlockState(pos).is(Blocks.DARK_OAK_LEAVES) ||
            level.getBlockState(pos).is(Blocks.JUNGLE_LEAVES) ||
            level.getBlockState(pos).is(Blocks.OAK_LEAVES) ||
            level.getBlockState(pos).is(Blocks.SPRUCE_LEAVES) ||
            level.getBlockState(pos).is(Blocks.AZALEA_LEAVES) ||
            level.getBlockState(pos).is(Blocks.FLOWERING_AZALEA_LEAVES)) {
            return true;
        }

        // Dark areas (caves, under trees)
        if (level.getLightEmission(pos) < 4) {
            return true;
        }

        return false;
    }

    /**
     * Checks if path between two points is clear of obstacles.
     *
     * @param start  Start position
     * @param end    End position
     * @param level  Level instance
     * @return true if path is clear
     */
    private boolean isPathClear(Vec3d start, Vec3d end, Level level) {
        Vec3d direction = Vec3d.sub(end, start);
        double distance = direction.magnitude();
        direction.normalize();

        int steps = (int) (distance / 1.0); // Check every block

        for (int i = 1; i < steps; i++) {
            Vec3d checkPos = start.copy();
            Vec3d stepOffset = direction.copy();
            stepOffset.mult(i);
            checkPos.add(stepOffset);

            BlockPos blockPos = new BlockPos(
                (int) Math.floor(checkPos.x),
                (int) Math.floor(checkPos.y),
                (int) Math.floor(checkPos.z)
            );

            // Check for solid blocks blocking path
            if (level.getBlockState(blockPos).isSuffocating(level, blockPos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates alternative path when direct path to refuge is blocked.
     *
     * @param context   Behavior context
     * @param position  Current position
     * @param refugePos Refuge position
     * @return Alternative escape vector
     */
    private Vec3d calculateAlternativeRefugePath(BehaviorContext context, Vec3d position, Vec3d refugePos) {
        // Try moving perpendicular to direct path
        Vec3d toRefuge = Vec3d.sub(refugePos, position);
        toRefuge.normalize();

        Vec3d perpendicular = new Vec3d(-toRefuge.z, 0, toRefuge.x);

        // Try both perpendicular directions
        Vec3d alt1 = toRefuge.copy();
        alt1.add(perpendicular);
        alt1.normalize();

        Vec3d alt2 = toRefuge.copy();
        Vec3d perpCopy = perpendicular.copy();
        perpCopy.mult(-1);
        alt2.add(perpCopy);
        alt2.normalize();

        // Check if path is clear for both options
        Level level = context.getLevel();
        Vec3d alt1End = position.copy();
        alt1End.add(alt1);
        Vec3d alt2End = position.copy();
        alt2End.add(alt2);

        boolean alt1Clear = isPathClear(position, alt1End, level);
        boolean alt2Clear = isPathClear(position, alt2End, level);

        // Return whichever direction has clear path, or alt1 as default
        if (alt1Clear && !alt2Clear) {
            return alt1;
        } else if (!alt1Clear && alt2Clear) {
            return alt2;
        } else if (alt1Clear && alt2Clear) {
            // Both clear, prefer direction that reduces distance to refuge
            Vec3d alt1Pos = position.copy();
            alt1Pos.add(alt1);
            Vec3d alt2Pos = position.copy();
            alt2Pos.add(alt2);
            double dist1 = alt1Pos.distanceTo(refugePos);
            double dist2 = alt2Pos.distanceTo(refugePos);
            return dist1 < dist2 ? alt1 : alt2;
        }
        return alt1;
    }

    /**
     * Finds the nearest threat entity.
     */
    private LivingEntity findNearestThreat(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (entity == null || context.getLevel() == null) {
            return null;
        }

        Vec3d position = context.getPosition();
        double detectionRange = config.getFlightInitiationDistance() * 1.5;

        List<LivingEntity> nearbyEntities = context.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(detectionRange)
        );

        LivingEntity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity nearby : nearbyEntities) {
            if (nearby == entity) {
                continue;
            }

            // Check if entity is a threat
            if (!isPotentialThreat(nearby)) {
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
     * Simple threat check for escape calculations.
     */
    private boolean isPotentialThreat(LivingEntity entity) {
        String typeName = entity.getType().toString().toLowerCase();

        // Players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return true;
        }

        // Predators
        if (typeName.contains("wolf") || typeName.contains("fox") ||
            typeName.contains("spider") || typeName.contains("phantom") ||
            typeName.contains("cat")) {
            return true;
        }

        return false;
    }

    // Getters and setters

    public EscapeStrategy getCurrentStrategy() {
        return currentStrategy;
    }

    public void setCurrentStrategy(EscapeStrategy strategy) {
        this.currentStrategy = strategy;
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    public int getFreezingTimer() {
        return freezingTimer;
    }

    public FleeingConfig getConfig() {
        return config;
    }

    public void reset() {
        zigzagTimer = 0;
        zigzagDirection = 1;
        freezingTimer = 0;
        isFrozen = false;
        currentStrategy = config.getPrimaryStrategy();
        cachedRefuge = null;
        refugeCacheTime = 0;
    }
}
