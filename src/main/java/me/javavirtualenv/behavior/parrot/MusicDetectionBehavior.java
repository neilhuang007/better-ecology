package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.behavior.parrot.DanceBehavior.DanceStyle;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * Music detection behavior for parrots.
 * Parrots can detect music from jukeboxes and note blocks,
 * and will fly toward and dance to the music.
 */
public class MusicDetectionBehavior {
    private final Mob parrot;
    private final MusicConfig config;
    private final EcologyComponent component;

    private BlockPos musicSource;
    private DanceStyle currentDance;
    private long musicStartTime;
    private boolean isFlyingToMusic = false;

    public MusicDetectionBehavior(Mob parrot, MusicConfig config, EcologyComponent component) {
        this.parrot = parrot;
        this.config = config;
        this.component = component;
    }

    /**
     * Scans for nearby music sources.
     * @return the nearest music source position, or null if none found
     */
    public BlockPos scanForMusic() {
        BlockPos nearestSource = null;
        double nearestDistance = Double.MAX_VALUE;

        int searchRadius = config.detectionRadius;
        BlockPos centerPos = parrot.blockPosition();

        // Search in expanding spiral pattern
        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) != radius && Math.abs(y) != radius && Math.abs(z) != radius) {
                            continue; // Only check outer shell
                        }

                        BlockPos checkPos = centerPos.offset(x, y, z);
                        if (isMusicSource(checkPos)) {
                            double distance = parrot.distanceToSqr(
                                checkPos.getX() + 0.5,
                                checkPos.getY() + 0.5,
                                checkPos.getZ() + 0.5
                            );

                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                nearestSource = checkPos;
                            }
                        }
                    }
                }
            }

            // Early exit if we found a source
            if (nearestSource != null) {
                break;
            }
        }

        return nearestSource;
    }

    /**
     * Checks if a position is a music source.
     */
    private boolean isMusicSource(BlockPos pos) {
        Block block = parrot.level().getBlockState(pos).getBlock();

        // Check for jukebox
        if (block == Blocks.JUKEBOX) {
            BlockEntity be = parrot.level().getBlockEntity(pos);
            if (be instanceof JukeboxBlockEntity jukebox) {
                return jukebox.getRecord().isPresent();
            }
        }

        // Check for recently played note block
        if (block == Blocks.NOTE_BLOCK) {
            return isNoteBlockActive(pos);
        }

        return false;
    }

    /**
     * Checks if a note block is actively being played.
     */
    private boolean isNoteBlockActive(BlockPos pos) {
        CompoundTag noteData = component.getHandleTag("note_blocks");
        long lastPlayed = noteData.getLong(pos.toString());

        // Consider active if played within last 5 seconds (100 ticks)
        return parrot.level().getGameTime() - lastPlayed < 100;
    }

    /**
     * Gets the dance style for a music source.
     */
    public DanceStyle getDanceStyle(BlockPos musicPos) {
        Block block = parrot.level().getBlockState(musicPos).getBlock();

        if (block == Blocks.JUKEBOX) {
            BlockEntity be = parrot.level().getBlockEntity(musicPos);
            if (be instanceof JukeboxBlockEntity jukebox) {
                return jukebox.getRecord()
                    .map(record -> DanceStyle.fromRecord(record.value()))
                    .orElse(DanceStyle.BOUNCE);
            }
        }

        return DanceStyle.BOUNCE; // Default for note blocks
    }

    /**
     * Starts flying toward a music source.
     */
    public void startFlyingToMusic(BlockPos musicPos, DanceStyle dance) {
        this.musicSource = musicPos;
        this.currentDance = dance;
        this.musicStartTime = parrot.level().getGameTime();
        this.isFlyingToMusic = true;

        // Update component data
        CompoundTag musicData = component.getHandleTag("music");
        musicData.putInt("source_x", musicPos.getX());
        musicData.putInt("source_y", musicPos.getY());
        musicData.putInt("source_z", musicPos.getZ());
        musicData.putString("dance_style", dance.name());
        musicData.putLong("music_start_time", musicStartTime);
        component.setHandleTag("music", musicData);
    }

    /**
     * Stops flying to music.
     */
    public void stopFlyingToMusic() {
        this.musicSource = null;
        this.currentDance = null;
        this.isFlyingToMusic = false;

        // Clear component data
        CompoundTag musicData = component.getHandleTag("music");
        musicData.remove("source_x");
        musicData.remove("source_y");
        musicData.remove("source_z");
        musicData.remove("dance_style");
        musicData.remove("music_start_time");
        component.setHandleTag("music", musicData);
    }

    /**
     * Updates flight toward music source.
     * @return true if still flying to music
     */
    public boolean updateFlightToMusic() {
        if (musicSource == null || !isFlyingToMusic) {
            return false;
        }

        // Check if music is still playing
        if (!isMusicSource(musicSource)) {
            stopFlyingToMusic();
            return false;
        }

        double distance = parrot.distanceTo(
            musicSource.getX() + 0.5,
            musicSource.getY() + 0.5,
            musicSource.getZ() + 0.5
        );

        // Check if we've arrived at the music
        if (distance <= config.arrivalDistance) {
            isFlyingToMusic = false;
            return false; // Arrived, ready to dance
        }

        // Fly toward music source
        if (parrot.getNavigation().isDone()) {
            parrot.getNavigation().moveTo(
                musicSource.getX() + 0.5,
                musicSource.getY() + 1.0,
                musicSource.getZ() + 0.5,
                config.flightSpeed
            );
        }

        return true;
    }

    /**
     * Checks if we're in range to hear the music.
     */
    public boolean isInRange(BlockPos musicPos) {
        if (musicPos == null) {
            return false;
        }

        double distance = parrot.distanceToSqr(
            musicPos.getX() + 0.5,
            musicPos.getY() + 0.5,
            musicPos.getZ() + 0.5
        );

        return distance <= config.detectionRadius * config.detectionRadius;
    }

    /**
     * Records that a note block was played.
     */
    public void recordNoteBlockPlayed(BlockPos pos) {
        CompoundTag noteData = component.getHandleTag("note_blocks");
        noteData.putLong(pos.toString(), parrot.level().getGameTime());
        component.setHandleTag("note_blocks", noteData);
    }

    public BlockPos getMusicSource() {
        return musicSource;
    }

    public DanceStyle getCurrentDance() {
        return currentDance;
    }

    public boolean isFlyingToMusic() {
        return isFlyingToMusic;
    }

    public long getMusicStartTime() {
        return musicStartTime;
    }

    /**
     * Configuration for music detection behavior.
     */
    public static class MusicConfig {
        public int detectionRadius = 64;
        public double arrivalDistance = 6.0;
        public double flightSpeed = 1.2;
        public boolean preferHighPerches = true;
        public double perchHeightBonus = 0.3;

        // How long to remember note block plays
        public int noteBlockMemoryTicks = 100;
    }
}
