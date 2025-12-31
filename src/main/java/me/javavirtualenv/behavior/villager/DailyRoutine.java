package me.javavirtualenv.behavior.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Manages villager daily routines - work, socialize, sleep, gather.
 */
public class DailyRoutine {
    private final Villager villager;
    private RoutinePhase currentPhase = RoutinePhase.IDLE;
    private int phaseTicks = 0;
    private BlockPos targetPos;

    private static final int DAY_TIME = 12000; // 0-12000 is day
    private static final int WORK_START = 2000;
    private static final int WORK_END = 8000;
    private static final int SOCIALIZE_START = 8000;
    private static final int SOCIALIZE_END = 11000;
    private static final int SLEEP_START = 12500;
    private static final int SLEEP_END = 1000;

    public DailyRoutine(Villager villager) {
        this.villager = villager;
    }

    /**
     * Updates the daily routine based on time of day.
     */
    public void tick() {
        long dayTime = villager.level().getDayTime() % 24000;
        RoutinePhase desiredPhase = getPhaseForTime(dayTime);

        if (desiredPhase != currentPhase) {
            transitionToPhase(desiredPhase);
        }

        phaseTicks++;
        executeCurrentPhase();
    }

    /**
     * Determines the appropriate phase for a given time.
     */
    private RoutinePhase getPhaseForTime(long dayTime) {
        // Night time - sleep
        if (dayTime >= SLEEP_START || dayTime < SLEEP_END) {
            return RoutinePhase.SLEEPING;
        }

        // Early morning - prepare for work
        if (dayTime >= SLEEP_END && dayTime < WORK_START) {
            return RoutinePhase.MORNING_PREP;
        }

        // Day - work
        if (dayTime >= WORK_START && dayTime < WORK_END) {
            return RoutinePhase.WORKING;
        }

        // Late afternoon - socialize
        if (dayTime >= SOCIALIZE_START && dayTime < SOCIALIZE_END) {
            return RoutinePhase.SOCIALIZING;
        }

        // Evening - prepare for sleep
        return RoutinePhase.EVENING_PREP;
    }

    /**
     * Transitions to a new routine phase.
     */
    private void transitionToPhase(RoutinePhase newPhase) {
        currentPhase = newPhase;
        phaseTicks = 0;

        // Clear previous navigation
        villager.getNavigation().stop();

        // Execute phase-specific initialization
        switch (newPhase) {
            case WORKING -> goToWorkStation();
            case SOCIALIZING -> goToMeetingPoint();
            case SLEEPING -> goToBed();
            case IDLE -> wanderRandomly();
        }
    }

    /**
     * Executes the current phase behavior.
     */
    private void executeCurrentPhase() {
        if (targetPos != null && villager.position().distanceTo(
            targetPos.getX() + 0.5,
            targetPos.getY(),
            targetPos.getZ() + 0.5
        ) < 2.0) {
            // Arrived at target
            onArrivedAtTarget();
        }
    }

    /**
     * Goes to work station.
     */
    private void goToWorkStation() {
        villager.getVillagerData().getJobSite().ifPresent(jobSite -> {
            targetPos = jobSite.pos();
            moveToPos(targetPos, 0.6);
        });
    }

    /**
     * Goes to village meeting point (bell).
     */
    private void goToMeetingPoint() {
        // Find nearby bell
        BlockPos bellPos = findNearestBell();
        if (bellPos != null) {
            targetPos = bellPos;
            moveToPos(bellPos, 0.5);
        } else {
            wanderRandomly();
        }
    }

    /**
     * Goes to bed for sleep.
     */
    private void goToBed() {
        // Villager's home bed
        villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME)
            .ifPresent(homePos -> {
                targetPos = homePos.pos();
                moveToPos(targetPos, 0.5);
            });
    }

    /**
     * Wanders randomly when idle.
     */
    private void wanderRandomly() {
        if (villager.getRandom().nextDouble() < 0.02) {
            villager.getNavigation().moveTo(
                villager.getX() + (villager.getRandom().nextDouble() - 0.5) * 8,
                villager.getY(),
                villager.getZ() + (villager.getRandom().nextDouble() - 0.5) * 8,
                0.4
            );
        }
    }

    /**
     * Moves to a specific position.
     */
    private void moveToPos(BlockPos pos, double speed) {
        villager.getNavigation().moveTo(
            pos.getX() + 0.5,
            pos.getY(),
            pos.getZ() + 0.5,
            speed
        );
    }

    /**
     * Called when arrived at target position.
     */
    private void onArrivedAtTarget() {
        if (currentPhase == RoutinePhase.SOCIALIZING) {
            // Share gossip with nearby villagers
            shareGossipWithNeighbors();
        }
    }

    /**
     * Shares gossip with nearby villagers.
     */
    private void shareGossipWithNeighbors() {
        // Find nearby villagers
        villager.level().getEntitiesOfClass(
            Villager.class,
            villager.getBoundingBox().inflate(8.0)
        ).forEach(other -> {
            if (other != villager) {
                // Spread gossip through the GossipSystem
                GossipSystem myGossip = VillagerMixin.getGossipSystem(villager);
                GossipSystem otherGossip = VillagerMixin.getGossipSystem(other);
                if (myGossip != null && otherGossip != null) {
                    myGossip.spreadGossip(other);
                }
            }
        });
    }

    /**
     * Finds the nearest village bell.
     */
    private BlockPos findNearestBell() {
        BlockPos searchCenter = villager.blockPosition();
        int searchRadius = 32;

        for (BlockPos pos : BlockPos.betweenClosed(
            searchCenter.offset(-searchRadius, -4, -searchRadius),
            searchCenter.offset(searchRadius, 4, searchRadius)
        )) {
            if (villager.level().getBlockState(pos).is(Blocks.BELL)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Rings the bell when threatened.
     */
    public void ringBell() {
        BlockPos bellPos = findNearestBell();
        if (bellPos != null) {
            // Ring the bell
            villager.level().blockUpdated(bellPos, Blocks.BELL);
        }
    }

    /**
     * Gets the current routine phase.
     */
    public RoutinePhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Checks if it's time to sleep.
     */
    public boolean shouldSleep() {
        return currentPhase == RoutinePhase.SLEEPING;
    }

    /**
     * Checks if it's time to work.
     */
    public boolean shouldWork() {
        return currentPhase == RoutinePhase.WORKING;
    }

    /**
     * Serializes routine state to NBT.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentPhase", currentPhase.name());
        tag.putInt("PhaseTicks", phaseTicks);
        if (targetPos != null) {
            tag.putInt("TargetX", targetPos.getX());
            tag.putInt("TargetY", targetPos.getY());
            tag.putInt("TargetZ", targetPos.getZ());
        }
        return tag;
    }

    /**
     * Loads routine state from NBT.
     */
    public void load(CompoundTag tag) {
        String phaseName = tag.getString("CurrentPhase");
        try {
            currentPhase = RoutinePhase.valueOf(phaseName);
        } catch (IllegalArgumentException e) {
            currentPhase = RoutinePhase.IDLE;
        }
        phaseTicks = tag.getInt("PhaseTicks");

        if (tag.contains("TargetX")) {
            int x = tag.getInt("TargetX");
            int y = tag.getInt("TargetY");
            int z = tag.getInt("TargetZ");
            targetPos = new BlockPos(x, y, z);
        }
    }

    /**
     * Phases of the daily routine.
     */
    public enum RoutinePhase {
        IDLE,
        MORNING_PREP,
        WORKING,
        SOCIALIZING,
        EVENING_PREP,
        SLEEPING
    }
}
