package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * AI goal for parrot music detection and dancing.
 * Parrots detect music from jukeboxes/note blocks and fly toward them to dance.
 */
public class ParrotMusicGoal extends Goal {

    private static final int MUSIC_SCAN_INTERVAL = 60;
    private static final double ARRIVAL_DISTANCE = 6.0;
    private static final double FLIGHT_SPEED = 1.2;

    private final PathfinderMob parrot;
    private final MusicDetectionBehavior musicBehavior;
    private final DanceBehavior danceBehavior;
    private final MusicDetectionBehavior.MusicConfig config;
    private final EcologyComponent component;

    private BlockPos musicSource;
    private Path currentPath;
    private int scanTimer;
    private boolean isFlyingToMusic;
    private boolean isDancing;

    private String lastDebugMessage = "";
    private boolean wasFlyingLastCheck = false;

    public ParrotMusicGoal(PathfinderMob parrot,
                          MusicDetectionBehavior musicBehavior,
                          DanceBehavior danceBehavior,
                          MusicDetectionBehavior.MusicConfig config) {
        this.parrot = parrot;
        this.musicBehavior = musicBehavior;
        this.danceBehavior = danceBehavior;
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

        CompoundTag danceTag = component.getHandleTag("dance");
        boolean isDancing = danceTag.getBoolean("is_dancing");

        return isFlyingToMusic || isDancing;
    }

    @Override
    public void start() {
        debug("goal started");
        scanTimer = 0;
        isFlyingToMusic = false;
        isDancing = false;
        musicSource = null;
    }

    @Override
    public void stop() {
        debug("goal stopped");
        stopDancing();
        stopFlyingToMusic();
    }

    @Override
    public void tick() {
        CompoundTag danceTag = component.getHandleTag("dance");
        isDancing = danceTag.getBoolean("is_dancing");

        boolean isFlyingNow = isFlyingToMusic;

        if (isFlyingNow != wasFlyingLastCheck) {
            debug("flight state changed: " + wasFlyingLastCheck + " -> " + isFlyingNow);
            wasFlyingLastCheck = isFlyingNow;
        }

        scanTimer++;
        if (scanTimer >= MUSIC_SCAN_INTERVAL) {
            scanTimer = 0;
            scanForMusic();
        }

        if (isFlyingToMusic) {
            updateFlight();
        }

        if (isDancing) {
            updateDancing();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void scanForMusic() {
        if (musicSource != null && isInRange(musicSource)) {
            return;
        }

        BlockPos foundMusic = musicBehavior.scanForMusic();
        if (foundMusic == null) {
            return;
        }

        PathNavigation navigation = parrot.getNavigation();
        currentPath = navigation.createPath(foundMusic, 0);

        if (currentPath != null && currentPath.canReach()) {
            startFlyingToMusic(foundMusic);
        } else {
            debug("music found but unreachable at " + foundMusic.getX() + "," + foundMusic.getY() + "," + foundMusic.getZ());
        }
    }

    private void startFlyingToMusic(BlockPos musicPos) {
        this.musicSource = musicPos;
        this.isFlyingToMusic = true;

        DanceBehavior.DanceStyle dance = musicBehavior.getDanceStyle(musicPos);

        CompoundTag musicTag = component.getHandleTag("music");
        musicTag.putInt("source_x", musicPos.getX());
        musicTag.putInt("source_y", musicPos.getY());
        musicTag.putInt("source_z", musicPos.getZ());
        musicTag.putString("dance_style", dance.name());
        musicTag.putLong("music_start_time", parrot.level().getGameTime());
        component.setHandleTag("music", musicTag);

        debug("flying to music at " + musicPos.getX() + "," + musicPos.getY() + "," + musicPos.getZ() + " (" + dance + ")");
    }

    private void stopFlyingToMusic() {
        isFlyingToMusic = false;

        CompoundTag musicTag = component.getHandleTag("music");
        musicTag.remove("source_x");
        musicTag.remove("source_y");
        musicTag.remove("source_z");
        musicTag.remove("dance_style");
        musicTag.remove("music_start_time");
        component.setHandleTag("music", musicTag);

        musicSource = null;
        currentPath = null;
    }

    private void updateFlight() {
        if (musicSource == null) {
            isFlyingToMusic = false;
            return;
        }

        if (!isMusicSource(musicSource)) {
            debug("music stopped, cancelling flight");
            stopFlyingToMusic();
            return;
        }

        double distance = parrot.position().distanceTo(
            net.minecraft.world.phys.Vec3.atCenterOf(musicSource)
        );

        if (distance <= ARRIVAL_DISTANCE) {
            arrivedAtMusic();
            return;
        }

        PathNavigation navigation = parrot.getNavigation();
        if (!navigation.isInProgress() ||
            currentPath == null ||
            !currentPath.canReach()) {

            currentPath = navigation.createPath(musicSource, 0);
            if (currentPath != null && currentPath.canReach()) {
                navigation.moveTo(musicSource.getX() + 0.5, musicSource.getY() + 1.0, musicSource.getZ() + 0.5, FLIGHT_SPEED);
                if (parrot.tickCount % 20 == 0) {
                    debug("flying to music, distance=" + String.format("%.1f", distance));
                }
            } else {
                debug("no path to music, giving up");
                stopFlyingToMusic();
            }
        }

        parrot.getLookControl().setLookAt(
            musicSource.getX() + 0.5,
            musicSource.getY() + 0.5,
            musicSource.getZ() + 0.5,
            30.0f,
            30.0f
        );
    }

    private void arrivedAtMusic() {
        isFlyingToMusic = false;

        if (!isInRange(musicSource)) {
            stopFlyingToMusic();
            return;
        }

        DanceBehavior.DanceStyle dance = getDanceStyle();
        if (dance != null) {
            startDancing(dance);
        }
    }

    private DanceBehavior.DanceStyle getDanceStyle() {
        CompoundTag musicTag = component.getHandleTag("music");
        String styleName = musicTag.getString("dance_style");

        if (styleName.isEmpty()) {
            return DanceBehavior.DanceStyle.BOUNCE;
        }

        try {
            return DanceBehavior.DanceStyle.valueOf(styleName);
        } catch (IllegalArgumentException e) {
            return DanceBehavior.DanceStyle.BOUNCE;
        }
    }

    private void startDancing(DanceBehavior.DanceStyle style) {
        danceBehavior.startDancing(style, musicSource);
        isDancing = true;
        debug("started dancing (" + style + ")");
    }

    private void updateDancing() {
        CompoundTag danceTag = component.getHandleTag("dance");
        boolean isStillDancing = danceTag.getBoolean("is_dancing");

        if (!isStillDancing) {
            isDancing = false;
            return;
        }

        if (musicSource != null && !isInRange(musicSource)) {
            debug("music out of range, stopping dance");
            stopDancing();
            return;
        }

        danceBehavior.tick();
    }

    private void stopDancing() {
        isDancing = false;
        danceBehavior.stopDancing();
    }

    private boolean isMusicSource(BlockPos pos) {
        if (pos == null) {
            return false;
        }

        net.minecraft.world.level.block.Block block = parrot.level().getBlockState(pos).getBlock();

        if (block == net.minecraft.world.level.block.Blocks.JUKEBOX) {
            net.minecraft.world.level.block.entity.BlockEntity be = parrot.level().getBlockEntity(pos);
            if (be instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) {
                return !jukebox.getTheItem().isEmpty();
            }
        }

        if (block == net.minecraft.world.level.block.Blocks.NOTE_BLOCK) {
            CompoundTag noteTag = component.getHandleTag("note_blocks");
            long lastPlayed = noteTag.getLong(pos.toString());
            return parrot.level().getGameTime() - lastPlayed < 100;
        }

        return false;
    }

    private boolean isInRange(BlockPos musicPos) {
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

    private EcologyComponent getComponent() {
        if (!(parrot instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ParrotMusic] Parrot #" + parrot.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    public String getDebugState() {
        CompoundTag danceTag = component.getHandleTag("dance");
        String danceStyle = danceTag.getString("dance_style");

        return String.format(
            "flying=%s, dancing=%s, music_source=%s, style=%s, path=%s",
            isFlyingToMusic,
            isDancing,
            musicSource != null ? musicSource.getX() + "," + musicSource.getY() + "," + musicSource.getZ() : "none",
            danceStyle.isEmpty() ? "none" : danceStyle,
            parrot.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }

    public boolean isDancing() {
        return isDancing;
    }

    public boolean isFlyingToMusic() {
        return isFlyingToMusic;
    }

    public BlockPos getMusicSource() {
        return musicSource;
    }
}
