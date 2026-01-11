package me.javavirtualenv.behavior.allay;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Behavior for allay sound following mechanics.
 * <p>
 * This behavior:
 * - Follows note block sounds when played
 * - Prefers and prioritizes jukebox music
 * - Dances to music when close to jukebox
 * - Emits heart particles when enjoying music
 * - Has stronger affinity for jukebox than note block
 */
public class SoundFollowingBehavior extends SteeringBehavior {

    private static final double NOTE_BLOCK_SEARCH_RADIUS = 48.0;
    private static final double JUKEBOX_SEARCH_RADIUS = 64.0;
    private static final double DANCE_DISTANCE = 8.0;
    private static final int SOUND_COOLDOWN_TICKS = 40;

    private BlockPos soundSource;
    private boolean isJukebox;
    private boolean isDancing;
    private int danceTicks;
    private int soundCooldown;
    private double affinityLevel;

    public SoundFollowingBehavior() {
        this(1.0);
    }

    public SoundFollowingBehavior(double weight) {
        this.soundSource = null;
        setWeight(weight);
        this.isJukebox = false;
        this.isDancing = false;
        this.danceTicks = 0;
        this.soundCooldown = 0;
        this.affinityLevel = 0.0;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Allay allay)) {
            return new Vec3d();
        }

        Level level = context.getWorld();
        Vec3d position = context.getPosition();

        // Update cooldown
        if (soundCooldown > 0) {
            soundCooldown--;
        }

        // Search for sound sources
        if (soundCooldown == 0) {
            searchForSoundSources(allay, position, level);
            soundCooldown = SOUND_COOLDOWN_TICKS;
        }

        if (soundSource == null) {
            stopDancing();
            return new Vec3d();
        }

        // Check if sound source is still valid
        if (!isValidSoundSource(level, soundSource)) {
            soundSource = null;
            stopDancing();
            return new Vec3d();
        }

        Vec3d sourcePos = new Vec3d(
            soundSource.getX() + 0.5,
            soundSource.getY(),
            soundSource.getZ() + 0.5
        );

        double distance = position.distanceTo(sourcePos);

        // Dance if close enough to music source
        if (isJukebox && distance <= DANCE_DISTANCE) {
            updateDancing(allay, level);
        } else {
            stopDancing();
        }

        // Calculate steering force toward sound source
        Vec3d desired = Vec3d.sub(sourcePos, position);
        desired.normalize();
        desired.mult(context.getMaxSpeed());

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        return limitForce(steer, context.getMaxForce());
    }

    /**
     * Searches for active sound sources (jukeboxes and note blocks).
     */
    private void searchForSoundSources(Allay allay, Vec3d position, Level level) {
        // Prioritize jukeboxes
        BlockPos nearestJukebox = findNearestPlayingJukebox(allay, position, level);

        if (nearestJukebox != null) {
            soundSource = nearestJukebox;
            isJukebox = true;
            affinityLevel = 1.0;
            return;
        }

        // Fall back to note blocks
        BlockPos nearestNoteBlock = findNearestNoteBlock(allay, position, level);

        if (nearestNoteBlock != null) {
            soundSource = nearestNoteBlock;
            isJukebox = false;
            affinityLevel = 0.5;
        } else {
            soundSource = null;
            affinityLevel = 0.0;
        }
    }

    /**
     * Finds the nearest jukebox that's playing music.
     */
    private BlockPos findNearestPlayingJukebox(Allay allay, Vec3d position, Level level) {
        BlockPos allayPos = allay.blockPosition();
        int searchRadius = (int) JUKEBOX_SEARCH_RADIUS;

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = allayPos.offset(x, y, z);

                    if (level.getBlockState(testPos).is(Blocks.JUKEBOX)) {
                        if (isJukeboxPlaying(level, testPos)) {
                            double dist = position.distanceTo(new Vec3d(
                                testPos.getX() + 0.5,
                                testPos.getY(),
                                testPos.getZ() + 0.5
                            ));

                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearest = testPos;
                            }
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Checks if a jukebox at the given position is playing music.
     */
    private boolean isJukeboxPlaying(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof JukeboxBlockEntity jukebox) {
            return !jukebox.getTheItem().isEmpty();
        }

        return false;
    }

    /**
     * Finds the nearest note block.
     */
    private BlockPos findNearestNoteBlock(Allay allay, Vec3d position, Level level) {
        BlockPos allayPos = allay.blockPosition();
        int searchRadius = (int) NOTE_BLOCK_SEARCH_RADIUS;

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = allayPos.offset(x, y, z);

                    if (level.getBlockState(testPos).is(Blocks.NOTE_BLOCK)) {
                        double dist = position.distanceTo(new Vec3d(
                            testPos.getX() + 0.5,
                            testPos.getY(),
                            testPos.getZ() + 0.5
                        ));

                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = testPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Checks if a sound source is still valid.
     */
    private boolean isValidSoundSource(Level level, BlockPos pos) {
        if (isJukebox) {
            return level.getBlockState(pos).is(Blocks.JUKEBOX) && isJukeboxPlaying(level, pos);
        } else {
            return level.getBlockState(pos).is(Blocks.NOTE_BLOCK);
        }
    }

    /**
     * Updates dancing state and effects.
     */
    private void updateDancing(Allay allay, Level level) {
        if (!isDancing) {
            startDancing();
        }

        danceTicks++;

        // Spawn heart particles periodically
        if (danceTicks % 20 == 0 && !level.isClientSide) {
            spawnHeartParticles(allay);
        }

        // Update affinity level based on dance duration
        affinityLevel = Math.min(2.0, affinityLevel + 0.001);
    }

    /**
     * Starts the dancing behavior.
     */
    private void startDancing() {
        isDancing = true;
        danceTicks = 0;
    }

    /**
     * Stops the dancing behavior.
     */
    private void stopDancing() {
        isDancing = false;
        danceTicks = 0;
        affinityLevel = 0.0;
    }

    /**
     * Spawns heart particles around the allay.
     */
    private void spawnHeartParticles(Allay allay) {
        if (allay.level().isClientSide) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            double offsetX = allay.level().random.nextDouble() * 0.5 - 0.25;
            double offsetY = allay.level().random.nextDouble() * 0.5;
            double offsetZ = allay.level().random.nextDouble() * 0.5 - 0.25;

            allay.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.HEART,
                allay.getX() + offsetX,
                allay.getY() + 1.0 + offsetY,
                allay.getZ() + offsetZ,
                0.0, 0.1, 0.0
            );
        }
    }

    /**
     * Gets the current sound source position.
     */
    public BlockPos getSoundSource() {
        return soundSource;
    }

    /**
     * Checks if the sound source is a jukebox.
     */
    public boolean isJukebox() {
        return isJukebox;
    }

    /**
     * Checks if the allay is currently dancing.
     */
    public boolean isDancing() {
        return isDancing;
    }

    /**
     * Gets the number of ticks the allay has been dancing.
     */
    public int getDanceTicks() {
        return danceTicks;
    }

    /**
     * Gets the current affinity level (0.0-2.0).
     */
    public double getAffinityLevel() {
        return affinityLevel;
    }

    /**
     * Sets the sound source manually.
     */
    public void setSoundSource(BlockPos pos, boolean isJukebox) {
        this.soundSource = pos;
        this.isJukebox = isJukebox;
    }
}
