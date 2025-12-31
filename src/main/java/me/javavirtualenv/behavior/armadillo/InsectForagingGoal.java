package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

    private final Mob mob;
    private final EcologyComponent component;
    private final EcologyProfile profile;
    private final ArmadilloComponent armadilloComponent;
    private final Random random;

    private BlockPos targetBlock;
    private int sniffTicks;
    private int digTicks;
    private static final int SNIFF_DURATION = 40; // 2 seconds to sniff
    private static final int DIG_DURATION = 30; // 1.5 seconds to dig
    private static final double SCENT_RANGE = 12.0; // Can smell insects from 12 blocks
    private static final int FORAGING_COOLDOWN = 600; // 30 seconds between foraging sessions

    public InsectForagingGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.profile = profile;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.random = mob.getRandom();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Can't forage while rolled
        if (armadilloComponent.isRolled()) {
            return false;
        }

        // Check if hungry
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (hungerTag.contains("hunger") && hungerTag.getInt("hunger") > 70) {
            return false; // Not hungry enough
        }

        // Check cooldown
        long lastForageTime = component.getHandleTag("armadillo").getLong("LastForageTime");
        long currentTime = mob.level().getGameTime();
        if (currentTime - lastForageTime < FORAGING_COOLDOWN) {
            return false;
        }

        // Find insect block
        targetBlock = findInsectBlock();
        return targetBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (armadilloComponent.isRolled()) {
            return false;
        }

        if (targetBlock == null) {
            return false;
        }

        // Continue if sniffing or digging
        return sniffTicks < SNIFF_DURATION || digTicks < DIG_DURATION;
    }

    @Override
    public void start() {
        sniffTicks = 0;
        digTicks = 0;

        // Look at target block
        mob.getLookControl().setLookAt(
            targetBlock.getX(),
            targetBlock.getY(),
            targetBlock.getZ()
        );
    }

    @Override
    public void tick() {
        // Look at target
        mob.getLookControl().setLookAt(
            targetBlock.getX(),
            targetBlock.getY(),
            targetBlock.getZ()
        );

        double distance = mob.blockPosition().distSqr(targetBlock);

        // Move towards target if far
        if (distance > 4.0) {
            mob.getNavigation().moveTo(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), 0.6);
        } else {
            // Close enough to start sniffing
            if (sniffTicks < SNIFF_DURATION) {
                performSniff();
            } else if (digTicks < DIG_DURATION) {
                performDig();
            } else {
                // Finished digging, eat insects
                eatInsects();
            }
        }
    }

    @Override
    public void stop() {
        targetBlock = null;
        sniffTicks = 0;
        digTicks = 0;

        // Update last forage time
        component.getHandleTag("armadillo").putLong("LastForageTime", mob.level().getGameTime());
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
                net.minecraft.core.particles.BlockParticleOption.BLOCK,
                targetBlock.getX() + 0.5,
                targetBlock.getY() + 0.5,
                targetBlock.getZ() + 0.5,
                3,
                0.2,
                0.2,
                0.2,
                0.1,
                blockState
            );
        }
    }

    /**
     * Eats insects from the block.
     */
    private void eatInsects() {
        // Restore hunger
        CompoundTag hungerTag = component.getHandleTag("hunger");
        int currentHunger = hungerTag.getInt("hunger");
        hungerTag.putInt("hunger", Math.min(100, currentHunger + 30));

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

        // Reset target to stop the goal
        targetBlock = null;
    }

    /**
     * Finds a block containing insects.
     */
    private BlockPos findInsectBlock() {
        BehaviorContext context = new BehaviorContext(mob);
        Vec3d position = context.getPosition();

        // Search in random directions for insect blocks
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 3.0 + random.nextDouble() * 5.0;

            double dx = Math.cos(angle) * distance;
            double dz = Math.sin(angle) * distance;

            BlockPos searchPos = new BlockPos(
                (int) (position.x + dx),
                (int) position.y,
                (int) (position.z + dz)
            );

            // Check if this block could contain insects
            if (couldContainInsects(searchPos)) {
                return searchPos;
            }
        }

        return null;
    }

    /**
     * Checks if a block could contain insects.
     */
    private boolean couldContainInsects(BlockPos pos) {
        BlockState blockState = mob.level().getBlockState(pos);
        Block block = blockState.getBlock();

        // Grass blocks, dirt, sand, and coarse dirt can contain insects
        return block == Blocks.GRASS_BLOCK ||
               block == Blocks.DIRT ||
               block == Blocks.SAND ||
               block == Blocks.COARSE_DIRT ||
               block == Blocks.PODZOL ||
               block == Blocks.MYCELIUM;
    }
}
