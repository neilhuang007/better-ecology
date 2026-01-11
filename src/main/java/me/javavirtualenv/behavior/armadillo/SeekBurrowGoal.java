package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal for armadillo to seek and enter burrow.
 * <p>
 * Armadillos seek burrows:
 * - During the day (they're crepuscular/nocturnal)
 * - When threatened
 * - To rest and sleep
 * - During extreme temperatures
 * <p>
 * Behavior:
 * - Navigate to nearest burrow
 * - Enter burrow with animation
 * - Sleep while in burrow
 * - Exit when safe/time to forage
 */
public class SeekBurrowGoal extends Goal {

    // Configuration constants
    private static final int ENTER_DURATION = 30; // 1.5 seconds to enter
    private static final double BURROW_SEARCH_RANGE = 64.0; // Search for burrows within 64 blocks
    private static final double MOVE_DISTANCE = 4.0; // Distance to start entering
    private static final double ARRIVAL_DISTANCE = 3.0; // Distance to consider "at" burrow
    private static final double MOVE_SPEED = 0.7; // Movement speed to burrow
    private static final int DAY_START = 2000; // Day time start
    private static final int DAY_END = 13000; // Day time end
    private static final int TIRED_THRESHOLD = 30; // Seek burrow when energy < 30

    // Instance fields
    private final Mob mob;
    private final EcologyComponent component;
    private final ArmadilloComponent armadilloComponent;
    private final ArmadilloBurrowSystem burrowSystem;

    private BlockPos targetBurrowPos;
    private Path currentPath;
    private int enterTicks;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasSeekingLastCheck = false;

    public SeekBurrowGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.burrowSystem = ArmadilloBurrowSystem.get(mob.level());
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (mob.level().isClientSide) {
            return false;
        }

        // Can't seek burrow while rolled
        if (armadilloComponent.isRolled()) {
            return false;
        }

        // Determine if should seek burrow
        boolean shouldSeekBurrow = shouldSeekBurrow();
        boolean isDay = isDayTime();

        // Log state change
        if (shouldSeekBurrow != wasSeekingLastCheck) {
            String reason = isDay ? "daytime" : (isTired() ? "tired" : "threatened");
            debug("seek state changed: " + wasSeekingLastCheck + " -> " + shouldSeekBurrow + " (reason: " + reason + ")");
            wasSeekingLastCheck = shouldSeekBurrow;
        }

        if (!shouldSeekBurrow) {
            return false;
        }

        // Check if already at/near burrow
        BlockPos currentBurrow = getBurrowPos();
        if (currentBurrow != null) {
            double distance = mob.blockPosition().distSqr(currentBurrow);
            if (distance <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                return false; // Already at burrow
            }
        }

        // Find nearest burrow
        ArmadilloBurrow burrow = burrowSystem.findAvailableBurrow(mob.blockPosition(), BURROW_SEARCH_RANGE);
        if (burrow == null) {
            debug("no available burrow found within " + (int) BURROW_SEARCH_RANGE + " blocks");
            return false;
        }

        targetBurrowPos = burrow.getPosition();

        // Check if we can reach this burrow
        PathNavigation navigation = mob.getNavigation();
        Path path = navigation.createPath(targetBurrowPos, 0);
        if (path == null || !path.canReach()) {
            debug("burrow found but unreachable at " + targetBurrowPos.getX() + "," + targetBurrowPos.getZ());
            targetBurrowPos = null;
            return false;
        }

        debug("STARTING: seeking burrow at " + targetBurrowPos.getX() + "," + targetBurrowPos.getY() + "," + targetBurrowPos.getZ());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if rolled
        if (armadilloComponent.isRolled()) {
            return false;
        }

        // Stop if target is gone
        if (targetBurrowPos == null) {
            return false;
        }

        // Continue if not yet entered
        return enterTicks < ENTER_DURATION;
    }

    @Override
    public void start() {
        enterTicks = 0;
        debug("goal started, moving to burrow");
        moveToBurrow();
    }

    @Override
    public void stop() {
        // Update current burrow position
        if (enterTicks >= ENTER_DURATION && targetBurrowPos != null) {
            setBurrowPos(targetBurrowPos);
            debug("entered burrow at " + targetBurrowPos.getX() + "," + targetBurrowPos.getZ());
        }

        targetBurrowPos = null;
        currentPath = null;
        mob.getNavigation().stop();
        debug("goal stopped");
    }

    @Override
    public void tick() {
        if (targetBurrowPos == null) {
            return;
        }

        double distance = mob.blockPosition().distSqr(targetBurrowPos);

        // Move towards burrow
        if (distance > MOVE_DISTANCE * MOVE_DISTANCE) {
            // Re-path if we're not moving or lost our path
            if (!mob.getNavigation().isInProgress() ||
                currentPath == null ||
                !currentPath.canReach()) {
                moveToBurrow();
            }

            // Look at burrow entrance
            mob.getLookControl().setLookAt(
                targetBurrowPos.getX(),
                targetBurrowPos.getY(),
                targetBurrowPos.getZ()
            );

            // Log progress every second
            if (mob.tickCount % 20 == 0) {
                debug("moving to burrow, distance=" + String.format("%.1f", Math.sqrt(distance)) + " blocks");
            }
            return;
        }

        // Close enough to enter
        performEnter();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Move towards the target burrow.
     */
    private void moveToBurrow() {
        if (targetBurrowPos == null) {
            return;
        }

        PathNavigation navigation = mob.getNavigation();
        currentPath = navigation.createPath(targetBurrowPos, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(targetBurrowPos.getX() + 0.5, targetBurrowPos.getY(), targetBurrowPos.getZ() + 0.5, MOVE_SPEED);
            debug("path found to burrow, distance=" + currentPath.getNodeCount() + " nodes");
        } else {
            debug("NO PATH to burrow, giving up");
            targetBurrowPos = null;
        }
    }

    /**
     * Performs entering animation and logic.
     */
    private void performEnter() {
        enterTicks++;

        // Play enter sound
        if (enterTicks == 1 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ARMADILLO_STEP,
                SoundSource.NEUTRAL,
                0.3F,
                0.7F
            );
        }

        // Shrink into burrow with particles
        if (enterTicks % 5 == 0 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.POOF,
                mob.getX(),
                mob.getY() + 0.2,
                mob.getZ(),
                2,
                0.1,
                0.1,
                0.1,
                0.0
            );
        }

        // Log progress
        if (enterTicks % 10 == 0) {
            debug("entering burrow, progress=" + String.format("%.0f", (enterTicks * 100.0 / ENTER_DURATION)) + "%");
        }

        // Fully entered
        if (enterTicks >= ENTER_DURATION) {
            ArmadilloBurrow burrow = burrowSystem.findNearestBurrow(targetBurrowPos, 1.0);
            if (burrow != null) {
                burrowSystem.enterBurrow(mob, burrow);
                setBurrowPos(targetBurrowPos);

                // Play entered sound
                if (!mob.level().isClientSide()) {
                    ServerLevel serverLevel = (ServerLevel) mob.level();
                    serverLevel.playSound(
                        null,
                        targetBurrowPos.getX(),
                        targetBurrowPos.getY(),
                        targetBurrowPos.getZ(),
                        SoundEvents.ARMADILLO_AMBIENT,
                        SoundSource.NEUTRAL,
                        0.2F,
                        0.8F
                    );
                }

                debug("fully entered burrow");
            }
        }
    }

    /**
     * Determines if armadillo should seek burrow.
     */
    private boolean shouldSeekBurrow() {
        // Seek during day
        if (isDayTime()) {
            return true;
        }

        // Seek when threatened
        if (isPanicking()) {
            return true;
        }

        // Seek when tired
        if (isTired()) {
            return true;
        }

        return false;
    }

    /**
     * Checks if it's currently daytime.
     */
    private boolean isDayTime() {
        long dayTime = mob.level().getDayTime() % 24000;
        return dayTime > DAY_START && dayTime < DAY_END;
    }

    /**
     * Checks if armadillo is tired and needs rest.
     */
    private boolean isTired() {
        CompoundTag energyTag = component.getHandleTag("energy");
        if (!energyTag.contains("energy")) {
            return false;
        }
        int energy = energyTag.getInt("energy");
        return energy < TIRED_THRESHOLD;
    }

    /**
     * Checks if armadillo is panicking.
     */
    private boolean isPanicking() {
        return armadilloComponent.isPanicking();
    }

    /**
     * Get the burrow position from NBT.
     */
    private BlockPos getBurrowPos() {
        CompoundTag armadilloTag = component.getHandleTag("armadillo");
        if (!armadilloTag.contains("BurrowPos")) {
            return null;
        }
        int[] posArray = armadilloTag.getIntArray("BurrowPos");
        if (posArray.length != 3) {
            return null;
        }
        return new BlockPos(posArray[0], posArray[1], posArray[2]);
    }

    /**
     * Set the burrow position in NBT.
     */
    private void setBurrowPos(BlockPos pos) {
        CompoundTag armadilloTag = component.getHandleTag("armadillo");
        if (pos == null) {
            armadilloTag.remove("BurrowPos");
        } else {
            armadilloTag.putIntArray("BurrowPos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ArmadilloBurrow] Armadillo #" + mob.getId() + " ";
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
        BlockPos burrowPos = getBurrowPos();
        CompoundTag energyTag = component.getHandleTag("energy");
        int energy = energyTag.contains("energy") ? energyTag.getInt("energy") : 100;

        return String.format("energy=%d, isDay=%s, rolled=%s, target=%s, enterProgress=%d/%d, path=%s",
            energy,
            isDayTime(),
            armadilloComponent.isRolled(),
            targetBurrowPos != null ? targetBurrowPos.getX() + "," + targetBurrowPos.getZ() : "none",
            enterTicks,
            ENTER_DURATION,
            mob.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
