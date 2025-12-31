package me.javavirtualenv.behavior.crepuscular;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Complete day/night cycle manager for bats and crepuscular creatures.
 * Combines emergence triggers, foraging behavior, and roosting behavior
 * into a unified activity cycle.
 */
public class BatActivityCycle {
    private final EmergenceTrigger emergenceTrigger;
    private final RoostingBehavior roostingBehavior;
    private final CrepuscularActivity activityCalculator;

    private ActivityState currentState = ActivityState.ROOSTING;
    private int foragingTime = 0;
    private final int maxForagingTime;

    /**
     * Activity states for crepuscular creatures.
     */
    public enum ActivityState {
        ROOSTING,      // Sleeping/resting at roost during day
        EMERGING,      // Leaving roost at dusk
        FORAGING,      // Active hunting/feeding at night
        RETURNING,     // Heading back to roost at dawn
        ROOST_SEARCHING // Looking for a new roost
    }

    public BatActivityCycle(CrepuscularConfig config) {
        this.emergenceTrigger = new EmergenceTrigger(config);
        this.roostingBehavior = new RoostingBehavior(config);
        this.activityCalculator = new CrepuscularActivity(config);
        this.maxForagingTime = 6000; // Default 5 minutes
    }

    public BatActivityCycle() {
        this(new CrepuscularConfig());
    }

    /**
     * Updates the activity cycle for a creature.
     * Should be called each tick to manage state transitions.
     */
    public void updateCycle(Mob entity) {
        ActivityState newState = determineState(entity);

        // State transition logic
        if (newState != currentState) {
            onStateTransition(entity, currentState, newState);
            currentState = newState;
        }

        // Update state-specific behaviors
        switch (currentState) {
            case ROOSTING -> updateRoosting(entity);
            case EMERGING -> updateEmerging(entity);
            case FORAGING -> updateForaging(entity);
            case RETURNING -> updateReturning(entity);
            case ROOST_SEARCHING -> updateRoostSearching(entity);
        }
    }

    /**
     * Determines the current activity state based on conditions.
     */
    private ActivityState determineState(Mob entity) {
        Level level = entity.level();
        long dayTime = level.getDayTime() % 24000;

        // Check if should return to roost
        if (emergenceTrigger.shouldReturn(entity)) {
            if (roostingBehavior.hasEstablishedRoost()) {
                return ActivityState.RETURNING;
            } else {
                return ActivityState.ROOST_SEARCHING;
            }
        }

        // Check if should emerge
        if (emergenceTrigger.shouldEmerge(entity)) {
            if (currentState == ActivityState.ROOSTING) {
                return ActivityState.EMERGING;
            }
        }

        // If emerged and active, forage
        if (currentState == ActivityState.EMERGING || currentState == ActivityState.FORAGING) {
            // Check if exceeded max foraging time
            if (foragingTime >= maxForagingTime) {
                return ActivityState.RETURNING;
            }
            return ActivityState.FORAGING;
        }

        // Default to roosting
        return ActivityState.ROOSTING;
    }

    /**
     * Handles state transitions.
     */
    private void onStateTransition(Mob entity, ActivityState oldState, ActivityState newState) {
        switch (newState) {
            case ROOSTING -> {
                foragingTime = 0;
                if (oldState != ActivityState.ROOSTING) {
                    onReturnToRoost(entity);
                }
            }
            case EMERGING -> {
                onEmerge(entity);
            }
            case FORAGING -> {
                if (oldState == ActivityState.EMERGING) {
                    foragingTime = 0;
                }
            }
            case RETURNING -> {
                onBeginReturn(entity);
            }
            case ROOST_SEARCHING -> {
                roostingBehavior.clearRoost();
            }
        }
    }

    /**
     * Updates behavior while roosting.
     */
    private void updateRoosting(Mob entity) {
        // Slow down and settle at roost
        Vec3 zeroVel = new Vec3(0, 0, 0);
        entity.setDeltaMovement(zeroVel);

        // Periodically check roost validity
        if (entity.getRandom().nextInt(200) == 0) {
            if (!roostingBehavior.hasEstablishedRoost()) {
                roostingBehavior.findRoostPosition(entity);
            }
        }
    }

    /**
     * Updates behavior while emerging.
     */
    private void updateEmerging(Mob entity) {
        // Move away from roost
        Vec3 emergenceDir = emergenceTrigger.getEmergenceDirection(entity);
        entity.setDeltaMovement(emergenceDir.scale(0.3));

        // Transition to foraging after moving away
        if (entity.getRandom().nextInt(40) == 0) {
            currentState = ActivityState.FORAGING;
        }
    }

    /**
     * Updates behavior while foraging.
     */
    private void updateForaging(Mob entity) {
        foragingTime++;

        // Random flight movements
        if (entity.getRandom().nextInt(20) == 0) {
            double randomX = (entity.getRandom().nextDouble() - 0.5) * 0.5;
            double randomY = (entity.getRandom().nextDouble() - 0.3) * 0.3;
            double randomZ = (entity.getRandom().nextDouble() - 0.5) * 0.5;

            Vec3 newVel = new Vec3(randomX, randomY, randomZ);
            entity.setDeltaMovement(newVel);
        }

        // Stay within foraging range of roost
        if (roostingBehavior.hasEstablishedRoost()) {
            BlockPos roostPos = roostingBehavior.getRoostPosition();
            double distance = entity.blockPosition().distSqr(roostPos);
            int maxRange = activityCalculator.getConfig().getForagingRange();

            if (distance > maxRange * maxRange) {
                // Turn back toward roost
                Vec3 toRoost = Vec3.atCenterOf(roostPos)
                    .subtract(entity.position())
                    .normalize()
                    .scale(0.2);
                entity.setDeltaMovement(toRoost);
            }
        }
    }

    /**
     * Updates behavior while returning to roost.
     */
    private void updateReturning(Mob entity) {
        if (!roostingBehavior.hasEstablishedRoost()) {
            currentState = ActivityState.ROOST_SEARCHING;
            return;
        }

        BlockPos roostPos = roostingBehavior.getRoostPosition();
        Vec3 toRoost = Vec3.atCenterOf(roostPos)
            .subtract(entity.position())
            .normalize()
            .scale(0.25);

        entity.setDeltaMovement(toRoost);

        // Check if arrived at roost
        if (roostingBehavior.isAtRoost(entity)) {
            currentState = ActivityState.ROOSTING;
        }
    }

    /**
     * Updates behavior while searching for a roost.
     */
    private void updateRoostSearching(Mob entity) {
        // Find a new roost
        roostingBehavior.findRoostPosition(entity);

        if (roostingBehavior.hasEstablishedRoost()) {
            currentState = ActivityState.RETURNING;
        } else {
            // Wander slowly
            Vec3 wander = new Vec3(
                (entity.getRandom().nextDouble() - 0.5) * 0.1,
                (entity.getRandom().nextDouble() - 0.5) * 0.1,
                (entity.getRandom().nextDouble() - 0.5) * 0.1
            );
            entity.setDeltaMovement(wander);

            // Seek ceiling
            Vec3 upward = new Vec3(0, 0.1, 0);
            entity.setDeltaMovement(entity.getDeltaMovement().add(upward));
        }
    }

    /**
     * Called when creature emerges from roost.
     */
    private void onEmerge(Mob entity) {
        foragingTime = 0;
        // Wake up creature
        if (entity instanceof net.minecraft.world.entity.ambient.Bat) {
            net.minecraft.world.entity.ambient.Bat bat = (net.minecraft.world.entity.ambient.Bat) entity;
            bat.setResting(false);
        }
    }

    /**
     * Called when creature begins returning to roost.
     */
    private void onBeginReturn(Mob entity) {
        // Prepare for rest
    }

    /**
     * Called when creature returns to roost.
     */
    private void onReturnToRoost(Mob entity) {
        // Settle at roost
        if (entity instanceof net.minecraft.world.entity.ambient.Bat) {
            net.minecraft.world.entity.ambient.Bat bat = (net.minecraft.world.entity.ambient.Bat) entity;
            bat.setResting(true);
        }
    }

    /**
     * Gets the current activity state.
     */
    public ActivityState getCurrentState() {
        return currentState;
    }

    /**
     * Sets the current activity state.
     */
    public void setCurrentState(ActivityState state) {
        this.currentState = state;
    }

    /**
     * Gets the emergence trigger.
     */
    public EmergenceTrigger getEmergenceTrigger() {
        return emergenceTrigger;
    }

    /**
     * Gets the roosting behavior.
     */
    public RoostingBehavior getRoostingBehavior() {
        return roostingBehavior;
    }

    /**
     * Gets the activity calculator.
     */
    public CrepuscularActivity getActivityCalculator() {
        return activityCalculator;
    }

    /**
     * Calculates steering behavior based on current state.
     */
    public Vec3d calculateSteering(BehaviorContext context) {
        switch (currentState) {
            case ROOSTING -> {
                return new Vec3d();
            }
            case EMERGING -> {
                Vec3d emergeDir = Vec3d.fromMinecraftVec3(
                    emergenceTrigger.getEmergenceDirection(context.getEntity())
                );
                emergeDir.mult(0.3);
                return emergeDir;
            }
            case FORAGING -> {
                // Random foraging movement
                double randomX = (Math.random() - 0.5) * 0.5;
                double randomY = (Math.random() - 0.3) * 0.3;
                double randomZ = (Math.random() - 0.5) * 0.5;
                return new Vec3d(randomX, randomY, randomZ);
            }
            case RETURNING, ROOST_SEARCHING -> {
                return roostingBehavior.calculate(context);
            }
            default -> {
                return new Vec3d();
            }
        }
    }

    /**
     * Gets seasonal adjustments to behavior.
     */
    public double getSeasonalModifier(Level level) {
        long dayTime = level.getDayTime() % 24000;
        long dayOfYear = level.getDayTime() / 24000;

        // Simple seasonal model based on day of year
        // Assume 72 days per season (Minecraft year is much shorter)
        int season = (int) ((dayOfYear / 72) % 4);

        return switch (season) {
            case 0 -> 1.0; // Spring
            case 1 -> 1.2; // Summer - more active
            case 2 -> 1.0; // Fall
            case 3 -> 0.8; // Winter - less active
            default -> 1.0;
        };
    }

    /**
     * Updates the configuration for the entire cycle.
     */
    public void setConfig(CrepuscularConfig config) {
        emergenceTrigger.setConfig(config);
        roostingBehavior.setConfig(config);
        activityCalculator.setConfig(config);
    }

    /**
     * Gets the configuration.
     */
    public CrepuscularConfig getConfig() {
        return activityCalculator.getConfig();
    }

    /**
     * Resets the cycle to initial state.
     */
    public void reset() {
        currentState = ActivityState.ROOSTING;
        foragingTime = 0;
        roostingBehavior.clearRoost();
    }
}
