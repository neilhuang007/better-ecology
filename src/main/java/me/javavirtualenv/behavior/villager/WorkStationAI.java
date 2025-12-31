package me.javavirtualenv.behavior.villager;

import me.javavirtualenv.behavior.core.BehaviorContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Manages villager work station behavior with visual animations and productivity tracking.
 */
public class WorkStationAI {
    private final Villager villager;
    private int workTicks = 0;
    private int productivity = 0;
    private int breakTimeRemaining = 0;
    private boolean isWorking = false;
    private BlockPos workStationPos;

    private static final int WORK_DURATION = 600; // 30 seconds of work
    private static final int BREAK_DURATION = 200; // 10 seconds break
    private static final int PRODUCTIVITY_PER_TICK = 1;
    private static final int MAX_PRODUCTIVITY = 1000;

    public WorkStationAI(Villager villager) {
        this.villager = villager;
    }

    /**
     * Called each tick to update work behavior.
     */
    public void tick() {
        if (breakTimeRemaining > 0) {
            breakTimeRemaining--;
            return;
        }

        BlockPos jobSite = getJobSite();
        if (jobSite == null) {
            stopWorking();
            return;
        }

        double distance = villager.position().distanceTo(
            jobSite.getX() + 0.5,
            jobSite.getY(),
            jobSite.getZ() + 0.5
        );

        if (distance > 3.0) {
            // Too far from work station
            stopWorking();
            return;
        }

        startWorking(jobSite);
        performWork(jobSite);

        workTicks++;
        if (workTicks >= WORK_DURATION) {
            takeBreak();
        }
    }

    /**
     * Performs work at the station.
     */
    private void performWork(BlockPos station) {
        if (!isWorking) {
            return;
        }

        productivity = Math.min(MAX_PRODUCTIVITY, productivity + PRODUCTIVITY_PER_TICK);

        // Visual effects based on work type
        BlockState block = villager.level().getBlockState(station);
        performWorkAnimation(block);

        // Play work sounds occasionally
        if (workTicks % 60 == 0) {
            playWorkSound(block);
        }
    }

    /**
     * Plays work animation based on workstation type.
     */
    private void performWorkAnimation(BlockState block) {
        // Every 20 ticks, show a particle effect
        if (villager.level().isClientSide || workTicks % 20 != 0) {
            return;
        }

        // Spawn particles to show working
        for (int i = 0; i < 3; i++) {
            double offsetX = villager.getRandom().nextGaussian() * 0.2;
            double offsetY = villager.getRandom().nextDouble() * 0.5;
            double offsetZ = villager.getRandom().nextGaussian() * 0.2;

            villager.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.HEART,
                villager.getX() + offsetX,
                villager.getY() + 1.0 + offsetY,
                villager.getZ() + offsetZ,
                0, 0.1, 0
            );
        }
    }

    /**
     * Plays appropriate work sound for the workstation type.
     */
    private void playWorkSound(BlockState block) {
        if (villager.level().isClientSide) {
            return;
        }

        // Play generic work sound
        villager.level().playSound(
            null,
            villager.blockPosition(),
            SoundEvents.VILLAGER_WORKING,
            SoundSource.NEUTRAL,
            0.5f,
            1.0f
        );
    }

    /**
     * Starts working at a station.
     */
    private void startWorking(BlockPos station) {
        if (!isWorking || workStationPos == null || !workStationPos.equals(station)) {
            isWorking = true;
            workStationPos = station;
            workTicks = 0;

            // Look at the workstation
            villager.getLookControl().setLookAt(
                station.getX() + 0.5,
                station.getY() + 0.5,
                station.getZ() + 0.5,
                10.0f,
                villager.getMaxHeadXRot()
            );
        }
    }

    /**
     * Stops working.
     */
    private void stopWorking() {
        isWorking = false;
        workStationPos = null;
        workTicks = 0;
    }

    /**
     * Takes a break from work.
     */
    private void takeBreak() {
        breakTimeRemaining = BREAK_DURATION;
        workTicks = 0;

        // Wander around a bit during break
        if (villager.getRandom().nextDouble() < 0.5) {
            villager.getNavigation().moveTo(
                villager.getX() + (villager.getRandom().nextDouble() - 0.5) * 4,
                villager.getY(),
                villager.getZ() + (villager.getRandom().nextDouble() - 0.5) * 4,
                0.5
            );
        }
    }

    /**
     * Gets the villager's job site position.
     */
    private BlockPos getJobSite() {
        return villager.getVillagerData().getJobSite().map(GlobalPos::pos).orElse(null);
    }

    /**
     * Gets current productivity level.
     */
    public int getProductivity() {
        return productivity;
    }

    /**
     * Resets productivity (called when inventory is used).
     */
    public void resetProductivity() {
        productivity = 0;
    }

    /**
     * Checks if the villager is currently working.
     */
    public boolean isWorking() {
        return isWorking;
    }

    /**
     * Gets the trade discount based on productivity.
     * Higher productivity = better prices.
     */
    public float getProductivityBonus() {
        return Math.min(0.2f, productivity / 5000.0f); // Max 20% discount
    }

    /**
     * Serializes work state to NBT.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("WorkTicks", workTicks);
        tag.putInt("Productivity", productivity);
        tag.putInt("BreakTimeRemaining", breakTimeRemaining);
        tag.putBoolean("IsWorking", isWorking);
        if (workStationPos != null) {
            tag.putInt("WorkStationX", workStationPos.getX());
            tag.putInt("WorkStationY", workStationPos.getY());
            tag.putInt("WorkStationZ", workStationPos.getZ());
        }
        return tag;
    }

    /**
     * Loads work state from NBT.
     */
    public void load(CompoundTag tag) {
        workTicks = tag.getInt("WorkTicks");
        productivity = tag.getInt("Productivity");
        breakTimeRemaining = tag.getInt("BreakTimeRemaining");
        isWorking = tag.getBoolean("IsWorking");

        if (tag.contains("WorkStationX")) {
            int x = tag.getInt("WorkStationX");
            int y = tag.getInt("WorkStationY");
            int z = tag.getInt("WorkStationZ");
            workStationPos = new BlockPos(x, y, z);
        } else {
            workStationPos = null;
        }
    }
}
