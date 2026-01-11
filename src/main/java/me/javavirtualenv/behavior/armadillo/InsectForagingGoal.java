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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.Random;

/**
 * Goal for armadillo insect foraging behavior.
 * <p>
 * Armadillos are insectivores that:
 * - Use keen sense of smell to find insects
 * - Dig into blocks to extract insects
 * - Sniff and scan for insect locations
 * - Eat insects to restore hunger
 * <p>
 * Behaviors:
 * - Sniffing behavior to detect insects
 * - Digging animation when extracting insects
 * - Eating insects from blocks
 * - Storing scent information
 */
public class InsectForagingGoal extends Goal {

    // Configuration constants
    private static final int SNIFF_DURATION = 40; // 2 seconds to sniff
    private static final int DIG_DURATION = 30; // 1.5 seconds to dig
    private static final double SCENT_RANGE = 12.0; // Can smell insects from 12 blocks
    private static final int FORAGING_COOLDOWN = 600; // 30 seconds between foraging sessions
    private static final double SEARCH_DISTANCE = 8.0; // Search 8 blocks for insect blocks
    private static final double WORK_DISTANCE = 2.5; // Distance to start working
    private static final double MOVE_SPEED = 0.6; // Movement speed to target
    private static final int HUNGRY_THRESHOLD = 70; // Forage when hunger < 70

    // Blocks that can contain insects
    private static final Block[] INSECT_BLOCKS = {
        Blocks.GRASS_BLOCK,
        Blocks.DIRT,
        Blocks.SAND,
        Blocks.COARSE_DIRT,
        Blocks.PODZOL,
        Blocks.MYCELIUM
    };

    // Instance fields
    private final Mob mob;
    private final EcologyComponent component;
    private final EcologyProfile profile;
    private final ArmadilloComponent armadilloComponent;
    private final Random random;

    private BlockPos targetBlock;
    private Path currentPath;
    private int sniffTicks;
    private int digTicks;
    private int ticksSinceCheck;
    private boolean wasHungryLastCheck;

    // Debug info
    private String lastDebugMessage = "";

    public InsectForagingGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.profile = profile;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.random = new Random();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (mob.level().isClientSide) {
            return false;
        }

        // Can't forage while rolled
        if (armadilloComponent.isRolled()) {
            return false;
        }

        // Check if hungry
        int hunger = getHungerLevel();
        boolean isHungry = hunger < HUNGRY_THRESHOLD;

        // Log state change
        if (isHungry != wasHungryLastCheck) {
            debug("hunger state changed: " + wasHungryLastCheck + " -> " + isHungry + " (hunger=" + hunger + ")");
            wasHungryLastCheck = isHungry;
        }

        if (!isHungry) {
            return false;
        }

        // Check cooldown
        long lastForageTime = getLastForageTime();
        long currentTime = mob.level().getGameTime();
        if (currentTime - lastForageTime < FORAGING_COOLDOWN) {
            // Log every 5 seconds that we're waiting
            ticksSinceCheck++;
            if (ticksSinceCheck % 100 == 0) {
                debug("waiting for cooldown (" + (FORAGING_COOLDOWN - (currentTime - lastForageTime)) + " ticks remaining)");
            }
            return false;
        }

        // Find insect block
        targetBlock = findInsectBlock();
        if (targetBlock == null) {
            ticksSinceCheck++;
            if (ticksSinceCheck % 100 == 0) {
                debug("hungry but no insect blocks found nearby");
            }
            return false;
        }

        ticksSinceCheck = 0;
        sniffTicks = 0;
        digTicks = 0;
        debug("STARTING: foraging at " + targetBlock.getX() + "," + targetBlock.getY() + "," + targetBlock.getZ());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if rolled
        if (armadilloComponent.isRolled()) {
            debug("cannot continue - rolled up");
            return false;
        }

        // Stop if target is gone
        if (targetBlock == null) {
            return false;
        }

        // Continue if sniffing or digging
        return sniffTicks < SNIFF_DURATION || digTicks < DIG_DURATION;
    }

    @Override
    public void start() {
        debug("goal started, moving to target");
        moveToTarget();
    }

    @Override
    public void stop() {
        debug("goal stopped");
        targetBlock = null;
        currentPath = null;
        mob.getNavigation().stop();

        // Update last forage time
        setLastForageTime(mob.level().getGameTime());
    }

    @Override
    public void tick() {
        if (targetBlock == null) {
            return;
        }

        // Look at target
        mob.getLookControl().setLookAt(
            targetBlock.getX(),
            targetBlock.getY(),
            targetBlock.getZ()
        );

        double distance = mob.blockPosition().distSqr(targetBlock);

        // Move towards target if far
        if (distance > WORK_DISTANCE * WORK_DISTANCE) {
            // Re-path if we're not moving or lost our path
            if (!mob.getNavigation().isInProgress() ||
                currentPath == null ||
                !currentPath.canReach()) {
                moveToTarget();
            }

            // Log progress every second
            if (mob.tickCount % 20 == 0) {
                debug("moving to target, distance=" + String.format("%.1f", Math.sqrt(distance)) + " blocks");
            }
            return;
        }

        // Close enough to work
        if (sniffTicks < SNIFF_DURATION) {
            performSniff();
        } else if (digTicks < DIG_DURATION) {
            performDig();
        } else {
            // Finished digging, eat insects
            eatInsects();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Move towards the target block.
     */
    private void moveToTarget() {
        if (targetBlock == null) {
            return;
        }

        PathNavigation navigation = mob.getNavigation();
        currentPath = navigation.createPath(targetBlock, 0);

        if (currentPath != null && currentPath.canReach()) {
            navigation.moveTo(targetBlock.getX() + 0.5, targetBlock.getY(), targetBlock.getZ() + 0.5, MOVE_SPEED);
            debug("path found to target, distance=" + currentPath.getNodeCount() + " nodes");
        } else {
            debug("NO PATH to target, giving up");
            targetBlock = null;
        }
    }

    /**
     * Performs sniffing behavior.
     */
    private void performSniff() {
        sniffTicks++;

        // Update scent strength
        double scentStrength = (double) sniffTicks / SNIFF_DURATION;
        armadilloComponent.setScentStrength(scentStrength);

        // Play sniff sound periodically
        if (sniffTicks % 10 == 0 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.FOX_SNIFF,
                SoundSource.NEUTRAL,
                0.3F,
                0.8F + (random.nextFloat() * 0.2F)
            );
        }

        // Spawn sniff particles
        if (sniffTicks % 5 == 0 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SNEEZE,
                mob.getX(),
                mob.getY() + 0.5,
                mob.getZ(),
                1,
                0.2,
                0.1,
                0.2,
                0.02
            );
        }
    }

    /**
     * Performs digging behavior.
     */
    private void performDig() {
        digTicks++;

        // Play dig sound periodically
        if (digTicks % 10 == 0 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                targetBlock.getX(),
                targetBlock.getY(),
                targetBlock.getZ(),
                SoundEvents.HOE_TILL,
                SoundSource.BLOCKS,
                0.3F,
                0.8F
            );
        }

        // Spawn dig particles
        if (digTicks % 3 == 0 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            BlockState blockState = mob.level().getBlockState(targetBlock);
            serverLevel.sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, blockState),
                targetBlock.getX() + 0.5,
                targetBlock.getY() + 0.5,
                targetBlock.getZ() + 0.5,
                3,
                0.2,
                0.2,
                0.2,
                0.1
            );
        }
    }

    /**
     * Eats insects from the block.
     */
    private void eatInsects() {
        // Restore hunger
        int currentHunger = getHungerLevel();
        setHungerLevel(Math.min(100, currentHunger + 30));

        // Play eat sound
        if (!mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ARMADILLO_EAT,
                SoundSource.NEUTRAL,
                0.4F,
                1.0F
            );
        }

        debug("ate insects, hunger restored to " + getHungerLevel());

        // Reset target to stop the goal
        targetBlock = null;
    }

    /**
     * Finds a block containing insects.
     */
    private BlockPos findInsectBlock() {
        BlockPos nearest = null;
        double nearestDist = SEARCH_DISTANCE;

        // Search in random directions for insect blocks
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 2.0 + random.nextDouble() * (SEARCH_DISTANCE - 2.0);

            double dx = Math.cos(angle) * distance;
            double dz = Math.sin(angle) * distance;

            BlockPos searchPos = new BlockPos(
                (int) (mob.getX() + dx),
                (int) mob.getY(),
                (int) (mob.getZ() + dz)
            );

            // Check if this block could contain insects
            if (!couldContainInsects(searchPos)) {
                continue;
            }

            // Check if we can reach this block
            Path path = mob.getNavigation().createPath(searchPos, 0);
            if (path == null || !path.canReach()) {
                continue;
            }

            double dist = mob.blockPosition().distSqr(searchPos);
            if (dist < nearestDist * nearestDist) {
                nearestDist = Math.sqrt(dist);
                nearest = searchPos;
            }
        }

        return nearest;
    }

    /**
     * Checks if a block could contain insects.
     */
    private boolean couldContainInsects(BlockPos pos) {
        BlockState blockState = mob.level().getBlockState(pos);
        Block block = blockState.getBlock();

        for (Block insectBlock : INSECT_BLOCKS) {
            if (block == insectBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the current hunger level.
     */
    private int getHungerLevel() {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (!hungerTag.contains("hunger")) {
            return 100; // Not hungry if doesn't exist
        }
        return hungerTag.getInt("hunger");
    }

    /**
     * Set the hunger level.
     */
    private void setHungerLevel(int value) {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        hungerTag.putInt("hunger", value);
    }

    /**
     * Get the last forage time.
     */
    private long getLastForageTime() {
        CompoundTag armadilloTag = component.getHandleTag("armadillo");
        if (!armadilloTag.contains("LastForageTime")) {
            return 0;
        }
        return armadilloTag.getLong("LastForageTime");
    }

    /**
     * Set the last forage time.
     */
    private void setLastForageTime(long value) {
        CompoundTag armadilloTag = component.getHandleTag("armadillo");
        armadilloTag.putLong("LastForageTime", value);
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ArmadilloForage] Armadillo #" + mob.getId() + " ";
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
        return String.format("hunger=%d, rolled=%s, target=%s, sniff=%d/%d, dig=%d/%d, path=%s",
            getHungerLevel(),
            armadilloComponent.isRolled(),
            targetBlock != null ? targetBlock.getX() + "," + targetBlock.getZ() : "none",
            sniffTicks,
            SNIFF_DURATION,
            digTicks,
            DIG_DURATION,
            mob.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }
}
