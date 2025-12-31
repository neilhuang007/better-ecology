package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Combined parrot behavior goal.
 * Orchestrates all parrot behaviors: mimicking, dancing, perching.
 * This goal manages the priority and switching between different behaviors.
 */
public class ParrotBehaviorGoal extends Goal {
    private final PathfinderMob parrot;
    private final MimicBehavior mimicBehavior;
    private final MusicDetectionBehavior musicBehavior;
    private final DanceBehavior danceBehavior;
    private final PerchBehavior perchBehavior;

    private final ParrotBehaviorConfig config;

    private BehaviorState currentState = BehaviorState.IDLE;
    private int stateCheckInterval = 20;
    private int stateCheckTimer = 0;

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
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return parrot.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        currentState = BehaviorState.IDLE;
        stateCheckTimer = 0;
    }

    @Override
    public void stop() {
        danceBehavior.stopDancing();
        musicBehavior.stopFlyingToMusic();
    }

    @Override
    public void tick() {
        // Always check for mimic opportunities
        mimicBehavior.tryMimic();

        // Periodically evaluate what behavior to perform
        stateCheckTimer++;
        if (stateCheckTimer >= stateCheckInterval) {
            stateCheckTimer = 0;
            evaluateBehaviorState();
        }

        // Execute current behavior
        switch (currentState) {
            case IDLE -> handleIdle();
            case DANCING -> handleDancing();
            case FLYING_TO_MUSIC -> handleFlyingToMusic();
            case PERCHING -> handlePerching();
            case FLYING_TO_PERCH -> handleFlyingToPerch();
        }
    }

    /**
     * Evaluates which behavior state we should be in.
     */
    private void evaluateBehaviorState() {
        // Priority: Dancing (highest) > Flying to Music > Perching > Flying to Perch > Idle

        // Check if we're shoulder-perched (passenger)
        if (parrot.isPassenger()) {
            currentState = BehaviorState.SHOULDER_PERCHED;
            return;
        }

        // Check for music first (highest priority)
        if (musicBehavior.getMusicSource() != null && musicBehavior.isInRange(musicBehavior.getMusicSource())) {
            if (danceBehavior.isDancing()) {
                currentState = BehaviorState.DANCING;
            } else {
                currentState = BehaviorState.FLYING_TO_MUSIC;
            }
            return;
        }

        // Scan for new music sources
        java.util.function.Supplier<BlockPos> musicScanner = () -> {
            int scanInterval = 60;
            if (parrot.tickCount % scanInterval == 0) {
                return musicBehavior.scanForMusic();
            }
            return null;
        };

        BlockPos musicSource = musicScanner.get();
        if (musicSource != null && config.enableMusicBehavior) {
            DanceBehavior.DanceStyle dance = musicBehavior.getDanceStyle(musicSource);
            musicBehavior.startFlyingToMusic(musicSource, dance);
            currentState = BehaviorState.FLYING_TO_MUSIC;
            return;
        }

        // Check if we should perch
        if (perchBehavior.isPerched()) {
            currentState = BehaviorState.PERCHING;
            return;
        }

        // Look for a perch if idle
        if (currentState == BehaviorState.IDLE && config.enablePerchBehavior) {
            // Random chance to seek a perch
            if (parrot.getRandom().nextDouble() < config.perchSeekChance) {
                BlockPos perch = perchBehavior.findPerch();
                if (perch != null) {
                    // Can't directly set target here, handled by PerchGoal
                    currentState = BehaviorState.FLYING_TO_PERCH;
                    return;
                }
            }
        }

        // Default to idle
        currentState = BehaviorState.IDLE;
    }

    private void handleIdle() {
        // Just wander around
        if (parrot.getRandom().nextDouble() < 0.02) {
            if (parrot.getNavigation().isDone()) {
                // Random nearby position
                double x = parrot.getX() + (parrot.getRandom().nextDouble() - 0.5) * 16;
                double y = parrot.getY() + (parrot.getRandom().nextDouble() - 0.5) * 8;
                double z = parrot.getZ() + (parrot.getRandom().nextDouble() - 0.5) * 16;
                parrot.getNavigation().moveTo(x, y, z, 0.6);
            }
        }
    }

    private void handleDancing() {
        if (!danceBehavior.isDancing()) {
            currentState = BehaviorState.IDLE;
            return;
        }

        danceBehavior.tick();
    }

    private void handleFlyingToMusic() {
        if (!musicBehavior.isFlyingToMusic() && musicBehavior.getMusicSource() != null) {
            // We've arrived, start dancing
            DanceBehavior.DanceStyle dance = musicBehavior.getCurrentDance();
            danceBehavior.startDancing(dance, musicBehavior.getMusicSource());
            currentState = BehaviorState.DANCING;
            return;
        }

        musicBehavior.updateFlightToMusic();

        // If music stopped, go to idle
        if (musicBehavior.getMusicSource() == null || !musicBehavior.isInRange(musicBehavior.getMusicSource())) {
            musicBehavior.stopFlyingToMusic();
            currentState = BehaviorState.IDLE;
        }
    }

    private void handlePerching() {
        perchBehavior.tick();

        if (!perchBehavior.isPerched()) {
            currentState = BehaviorState.IDLE;
        }
    }

    private void handleFlyingToPerch() {
        // Handled by PerchGoal
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Behavior states for parrots.
     */
    public enum BehaviorState {
        IDLE,
        DANCING,
        FLYING_TO_MUSIC,
        PERCHING,
        FLYING_TO_PERCH,
        SHOULDER_PERCHED
    }

    /**
     * Overall configuration for parrot behaviors.
     */
    public static class ParrotBehaviorConfig {
        public boolean enableMusicBehavior = true;
        public boolean enablePerchBehavior = true;
        public boolean enableMimicBehavior = true;
        public double perchSeekChance = 0.01;
        public int musicDetectionInterval = 60;
    }
}
