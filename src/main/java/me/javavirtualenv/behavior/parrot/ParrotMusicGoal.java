package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI goal for parrot music detection and dancing.
 * Parrots detect music from jukeboxes/note blocks and fly toward them to dance.
 */
public class ParrotMusicGoal extends Goal {
    private final PathfinderMob parrot;
    private final MusicDetectionBehavior musicBehavior;
    private final DanceBehavior danceBehavior;
    private final MusicDetectionBehavior.MusicConfig musicConfig;

    private int scanInterval = 60;
    private int scanTimer = 0;
    private boolean isDancing = false;

    public ParrotMusicGoal(PathfinderMob parrot,
                          MusicDetectionBehavior musicBehavior,
                          DanceBehavior danceBehavior,
                          MusicDetectionBehavior.MusicConfig musicConfig) {
        this.parrot = parrot;
        this.musicBehavior = musicBehavior;
        this.danceBehavior = danceBehavior;
        this.musicConfig = musicConfig;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!parrot.isAlive()) {
            return false;
        }

        // Can't dance if perched on shoulder
        if (parrot.isPassenger()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canUse()) {
            return false;
        }

        // Continue if we're flying to music or dancing
        if (musicBehavior.isFlyingToMusic() || isDancing) {
            return true;
        }

        return false;
    }

    @Override
    public void start() {
        scanTimer = 0;
        isDancing = false;
    }

    @Override
    public void stop() {
        danceBehavior.stopDancing();
        musicBehavior.stopFlyingToMusic();
        isDancing = false;
    }

    @Override
    public void tick() {
        // Scan for music periodically
        scanTimer++;
        if (scanTimer >= scanInterval) {
            scanTimer = 0;
            scanForMusic();
        }

        // Update flight to music
        if (musicBehavior.isFlyingToMusic()) {
            updateFlight();
        }

        // Update dancing
        if (isDancing) {
            updateDancing();
        }
    }

    /**
     * Scans for nearby music sources.
     */
    private void scanForMusic() {
        // Don't scan if we already have a music source
        if (musicBehavior.getMusicSource() != null && musicBehavior.isInRange(musicBehavior.getMusicSource())) {
            return;
        }

        BlockPos musicSource = musicBehavior.scanForMusic();
        if (musicSource != null) {
            DanceBehavior.DanceStyle dance = musicBehavior.getDanceStyle(musicSource);
            musicBehavior.startFlyingToMusic(musicSource, dance);
        }
    }

    /**
     * Updates flight toward music source.
     */
    private void updateFlight() {
        boolean stillFlying = musicBehavior.updateFlightToMusic();

        if (!stillFlying && musicBehavior.getMusicSource() != null) {
            // We've arrived, start dancing
            BlockPos musicPos = musicBehavior.getMusicSource();
            DanceBehavior.DanceStyle dance = musicBehavior.getCurrentDance();

            // Check if music is still playing
            if (musicBehavior.isInRange(musicPos)) {
                startDancing(dance, musicPos);
            } else {
                musicBehavior.stopFlyingToMusic();
            }
        }
    }

    /**
     * Starts dancing to music.
     */
    private void startDancing(DanceBehavior.DanceStyle style, BlockPos pos) {
        danceBehavior.startDancing(style, pos);
        isDancing = true;
    }

    /**
     * Updates the dance behavior.
     */
    private void updateDancing() {
        // Check if music is still playing
        BlockPos musicPos = musicBehavior.getMusicSource();
        if (musicPos == null || !musicBehavior.isInRange(musicPos)) {
            danceBehavior.stopDancing();
            isDancing = false;
            return;
        }

        // Update dance
        danceBehavior.tick();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
