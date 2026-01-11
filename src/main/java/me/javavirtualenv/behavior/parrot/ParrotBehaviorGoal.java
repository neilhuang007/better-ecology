package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Combined parrot behavior goal.
 * Orchestrates all parrot behaviors: mimicking, dancing, perching.
 * This goal manages the priority and switching between different behaviors.
 */
public class ParrotBehaviorGoal extends Goal {

    private static final int STATE_CHECK_INTERVAL = 20;
    private static final double IDLE_WANDER_CHANCE = 0.02;

    private final PathfinderMob parrot;
    private final MimicBehavior mimicBehavior;
    private final MusicDetectionBehavior musicBehavior;
    private final DanceBehavior danceBehavior;
    private final PerchBehavior perchBehavior;
    private final ParrotBehaviorConfig config;
    private final EcologyComponent component;

    private BehaviorState currentState;
    private int stateCheckTimer;
    private int stateChangeCount;

    private String lastDebugMessage = "";
    private BehaviorState lastState = BehaviorState.IDLE;

    public ParrotBehaviorGoal(PathfinderMob parrot,
                             MimicBehavior mimicBehavior,
                             MusicDetectionBehavior musicBehavior,
                             DanceBehavior danceBehavior,
                             PerchBehavior perchBehavior,
                             ParrotBehaviorConfig config) {
        this.parrot = parrot;
        this.mimicBehavior = mimicBehavior;
        this.musicBehavior = musicBehavior;
        this.danceBehavior = danceBehavior;
        this.perchBehavior = perchBehavior;
        this.config = config;
        this.component = getComponent();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!parrot.isAlive()) {
            return false;
        }

        if (parrot.level().isClientSide) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        debug("goal started");
        currentState = BehaviorState.IDLE;
        stateCheckTimer = 0;
        stateChangeCount = 0;
        lastState = BehaviorState.IDLE;
    }

    @Override
    public void stop() {
        debug("goal stopped, state_changes=" + stateChangeCount);
        danceBehavior.stopDancing();
        musicBehavior.stopFlyingToMusic();
    }

    @Override
    public void tick() {
        mimicBehavior.tryMimic();

        stateCheckTimer++;
        if (stateCheckTimer >= STATE_CHECK_INTERVAL) {
            stateCheckTimer = 0;
            evaluateBehaviorState();
        }

        executeCurrentBehavior();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void evaluateBehaviorState() {
        BehaviorState newState = determineNextState();

        if (newState != currentState) {
            debug("state changed: " + currentState + " -> " + newState);
            currentState = newState;
            stateChangeCount++;
        }
    }

    private BehaviorState determineNextState() {
        if (parrot.isPassenger()) {
            return BehaviorState.SHOULDER_PERCHED;
        }

        CompoundTag danceTag = component.getHandleTag("dance");
        boolean isDancing = danceTag.getBoolean("is_dancing");

        CompoundTag musicTag = component.getHandleTag("music");
        boolean hasMusicSource = musicTag.contains("source_x");

        CompoundTag perchTag = component.getHandleTag("perch");
        boolean isPerched = perchTag.getBoolean("is_perched");

        if (isDancing) {
            return BehaviorState.DANCING;
        }

        if (hasMusicSource && isMusicInRange()) {
            return BehaviorState.FLYING_TO_MUSIC;
        }

        if (isPerched) {
            return BehaviorState.PERCHING;
        }

        if (config.enableMusicBehavior && shouldScanForMusic()) {
            BlockPos musicSource = musicBehavior.scanForMusic();
            if (musicSource != null) {
                DanceBehavior.DanceStyle dance = musicBehavior.getDanceStyle(musicSource);
                musicBehavior.startFlyingToMusic(musicSource, dance);
                return BehaviorState.FLYING_TO_MUSIC;
            }
        }

        if (config.enablePerchBehavior && currentState == BehaviorState.IDLE) {
            if (parrot.getRandom().nextDouble() < config.perchSeekChance) {
                BlockPos perch = perchBehavior.findPerch();
                if (perch != null) {
                    return BehaviorState.FLYING_TO_PERCH;
                }
            }
        }

        return BehaviorState.IDLE;
    }

    private boolean isMusicInRange() {
        CompoundTag musicTag = component.getHandleTag("music");
        if (!musicTag.contains("source_x")) {
            return false;
        }

        int musicX = musicTag.getInt("source_x");
        int musicY = musicTag.getInt("source_y");
        int musicZ = musicTag.getInt("source_z");
        BlockPos musicPos = new BlockPos(musicX, musicY, musicZ);

        double distance = parrot.distanceToSqr(
            musicPos.getX() + 0.5,
            musicPos.getY() + 0.5,
            musicPos.getZ() + 0.5
        );

        return distance <= config.musicDetectionRadius * config.musicDetectionRadius;
    }

    private boolean shouldScanForMusic() {
        return parrot.tickCount % config.musicDetectionInterval == 0;
    }

    private void executeCurrentBehavior() {
        switch (currentState) {
            case IDLE -> handleIdle();
            case DANCING -> handleDancing();
            case FLYING_TO_MUSIC -> handleFlyingToMusic();
            case PERCHING -> handlePerching();
            case FLYING_TO_PERCH -> handleFlyingToPerch();
            case SHOULDER_PERCHED -> { }
        }
    }

    private void handleIdle() {
        if (parrot.getRandom().nextDouble() < IDLE_WANDER_CHANCE) {
            if (parrot.getNavigation().isDone()) {
                wanderRandomly();
            }
        }
    }

    private void wanderRandomly() {
        double x = parrot.getX() + (parrot.getRandom().nextDouble() - 0.5) * 16;
        double y = parrot.getY() + (parrot.getRandom().nextDouble() - 0.5) * 8;
        double z = parrot.getZ() + (parrot.getRandom().nextDouble() - 0.5) * 16;
        parrot.getNavigation().moveTo(x, y, z, 0.6);
    }

    private void handleDancing() {
        CompoundTag danceTag = component.getHandleTag("dance");
        boolean isDancing = danceTag.getBoolean("is_dancing");

        if (!isDancing) {
            currentState = BehaviorState.IDLE;
            return;
        }

        danceBehavior.tick();
    }

    private void handleFlyingToMusic() {
        boolean stillFlying = musicBehavior.updateFlightToMusic();

        if (!stillFlying) {
            CompoundTag musicTag = component.getHandleTag("music");
            if (musicTag.contains("source_x")) {
                DanceBehavior.DanceStyle dance = musicBehavior.getCurrentDance();
                if (dance != null) {
                    int musicX = musicTag.getInt("source_x");
                    int musicY = musicTag.getInt("source_y");
                    int musicZ = musicTag.getInt("source_z");
                    BlockPos musicPos = new BlockPos(musicX, musicY, musicZ);

                    danceBehavior.startDancing(dance, musicPos);
                    currentState = BehaviorState.DANCING;
                }
            } else {
                currentState = BehaviorState.IDLE;
            }
        }
    }

    private void handlePerching() {
        perchBehavior.tick();

        CompoundTag perchTag = component.getHandleTag("perch");
        boolean isPerched = perchTag.getBoolean("is_perched");

        if (!isPerched) {
            currentState = BehaviorState.IDLE;
        }
    }

    private void handleFlyingToPerch() {
    }

    private EcologyComponent getComponent() {
        if (!(parrot instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ParrotBehavior] Parrot #" + parrot.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    public String getDebugState() {
        CompoundTag danceTag = component.getHandleTag("dance");
        CompoundTag musicTag = component.getHandleTag("music");
        CompoundTag perchTag = component.getHandleTag("perch");

        String danceStyle = danceTag.getString("dance_style");
        String musicPos = musicTag.contains("source_x")
            ? musicTag.getInt("source_x") + "," + musicTag.getInt("source_y") + "," + musicTag.getInt("source_z")
            : "none";
        boolean isPerched = perchTag.getBoolean("is_perched");

        return String.format(
            "state=%s, state_changes=%d, dancing=%s, music=%s, perched=%s, path=%s",
            currentState,
            stateChangeCount,
            danceStyle.isEmpty() ? "no" : "yes(" + danceStyle + ")",
            musicPos,
            isPerched,
            parrot.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }

    public BehaviorState getCurrentState() {
        return currentState;
    }

    public int getStateChangeCount() {
        return stateChangeCount;
    }

    public enum BehaviorState {
        IDLE,
        DANCING,
        FLYING_TO_MUSIC,
        PERCHING,
        FLYING_TO_PERCH,
        SHOULDER_PERCHED
    }

    public static class ParrotBehaviorConfig {
        public boolean enableMusicBehavior = true;
        public boolean enablePerchBehavior = true;
        public boolean enableMimicBehavior = true;
        public double perchSeekChance = 0.01;
        public int musicDetectionInterval = 60;
        public int musicDetectionRadius = 64;
    }
}
