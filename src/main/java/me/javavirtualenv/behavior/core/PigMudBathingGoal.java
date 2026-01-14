package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes pigs seek mud (water on dirt/clay) for temperature regulation and comfort.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates randomly or when health is low (stress-related)</li>
 *   <li>Searches for water blocks or wet surfaces (water, mud) within range</li>
 *   <li>Pathfinds to the mud/water and bathes</li>
 *   <li>Restores a small amount of health while bathing</li>
 *   <li>Shows water particle effects during bathing</li>
 * </ul>
 *
 * <p>Based on real pig wallowing behavior for thermoregulation since pigs cannot sweat.
 */
public class PigMudBathingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(PigMudBathingGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 200;
    private static final int BATHING_DURATION = 200;
    private static final int GIVE_UP_TICKS = 800;
    private static final double ACCEPTED_DISTANCE = 1.5;
    private static final float BATHING_HEAL_AMOUNT = 0.5f;
    private static final int HEAL_INTERVAL_TICKS = 20;
    private static final float BASE_ACTIVATION_CHANCE = 0.05f;
    private static final float LOW_HEALTH_THRESHOLD = 0.8f;
    private static final float LOW_HEALTH_CHANCE_BOOST = 0.15f;

    private final Mob mob;
    private final double speedModifier;
    private final int searchRadius;

    private BlockPos targetMudPos;
    private int searchCooldown;
    private int tryTicks;
    private int bathingTicks;
    private boolean isBathing;

    /**
     * Creates a new PigMudBathingGoal.
     *
     * @param mob the pig that will seek mud
     * @param speedModifier movement speed multiplier when moving to mud
     * @param searchRadius horizontal search radius for mud blocks
     */
    public PigMudBathingGoal(Mob mob, double speedModifier, int searchRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.searchCooldown = 0;
        this.targetMudPos = null;
        this.bathingTicks = 0;
        this.isBathing = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Always check if pig is already in water/mud - if so, always bathe
        if (isCurrentlyInMud()) {
            this.targetMudPos = this.mob.blockPosition();
            return true;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        float activationChance = calculateActivationChance();
        if (this.mob.getRandom().nextFloat() >= activationChance) {
            this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        return findNearestMud();
    }

    /**
     * Checks if the mob is currently standing in mud or water.
     *
     * @return true if the mob is in water/mud
     */
    private boolean isCurrentlyInMud() {
        BlockPos mobPos = this.mob.blockPosition();
        return isMudBlock(mobPos) || isMudBlock(mobPos.below());
    }

    @Override
    public boolean canContinueToUse() {
        if (this.tryTicks >= GIVE_UP_TICKS) {
            LOGGER.debug("Mob {} gave up seeking mud after {} ticks", this.mob.getName().getString(), this.tryTicks);
            return false;
        }

        if (this.bathingTicks >= BATHING_DURATION) {
            LOGGER.debug("Mob {} finished bathing after {} ticks", this.mob.getName().getString(), this.bathingTicks);
            return false;
        }

        if (this.targetMudPos == null || !isMudBlock(this.targetMudPos)) {
            LOGGER.debug("Mud at {} is no longer valid for mob {}", this.targetMudPos, this.mob.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("Mob {} starting to seek mud at {}", this.mob.getName().getString(), this.targetMudPos);
        this.tryTicks = 0;
        this.bathingTicks = 0;
        this.isBathing = false;

        // If already in mud, start bathing immediately
        if (isNearMud()) {
            this.isBathing = true;
            LOGGER.debug("Mob {} is already in mud, starting to bathe immediately", this.mob.getName().getString());
        } else {
            navigateToMud();
        }
    }

    @Override
    public void stop() {
        LOGGER.debug("Mob {} stopped bathing. Bathed for {} ticks", this.mob.getName().getString(), this.bathingTicks);
        this.targetMudPos = null;
        this.isBathing = false;
        this.bathingTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.tryTicks++;

        if (isNearMud()) {
            this.isBathing = true;
            this.mob.getNavigation().stop();

            performBathing();
        } else {
            this.isBathing = false;

            if (shouldRecalculatePath()) {
                navigateToMud();
            }
        }
    }

    /**
     * Calculates the activation chance based on pig condition.
     *
     * @return probability of seeking mud (0.0 to 1.0)
     */
    private float calculateActivationChance() {
        float healthRatio = this.mob.getHealth() / this.mob.getMaxHealth();

        if (healthRatio < LOW_HEALTH_THRESHOLD) {
            return BASE_ACTIVATION_CHANCE + LOW_HEALTH_CHANCE_BOOST;
        }

        return BASE_ACTIVATION_CHANCE;
    }

    /**
     * Finds the nearest mud or water block within search radius.
     *
     * @return true if mud was found, false otherwise
     */
    private boolean findNearestMud() {
        BlockPos mobPos = this.mob.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        BlockPos closestMud = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int y = -2; y <= 2; y++) {
            for (int x = -this.searchRadius; x <= this.searchRadius; x++) {
                for (int z = -this.searchRadius; z <= this.searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);

                    if (isMudBlock(mutablePos)) {
                        double distSq = mobPos.distSqr(mutablePos);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closestMud = mutablePos.immutable();
                        }
                    }
                }
            }
        }

        if (closestMud != null) {
            this.targetMudPos = closestMud;
            LOGGER.debug("Mob {} found mud at {} (distance: {})",
                this.mob.getName().getString(), closestMud, Math.sqrt(closestDistSq));
            return true;
        }

        LOGGER.debug("Mob {} could not find mud within {} blocks",
            this.mob.getName().getString(), this.searchRadius);
        return false;
    }

    /**
     * Checks if a block position contains mud or water.
     *
     * @param pos the position to check
     * @return true if the position contains water, mud, or suitable bathing surface
     */
    private boolean isMudBlock(BlockPos pos) {
        Level level = this.mob.level();

        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return true;
        }

        Block block = level.getBlockState(pos).getBlock();
        return block == Blocks.MUD || block == Blocks.CLAY;
    }

    /**
     * Checks if the mob is near enough to the mud to bathe.
     *
     * @return true if the mob is within bathing distance
     */
    private boolean isNearMud() {
        if (this.targetMudPos == null) {
            return false;
        }

        return this.mob.position().closerThan(this.targetMudPos.getCenter(), ACCEPTED_DISTANCE);
    }

    /**
     * Navigates the mob to the mud.
     */
    private void navigateToMud() {
        if (this.targetMudPos == null) {
            return;
        }

        this.mob.getNavigation().moveTo(
            this.targetMudPos.getX() + 0.5,
            this.targetMudPos.getY(),
            this.targetMudPos.getZ() + 0.5,
            this.speedModifier
        );
    }

    /**
     * Handles the bathing behavior and effects.
     */
    private void performBathing() {
        this.bathingTicks++;

        this.mob.getLookControl().setLookAt(
            this.targetMudPos.getX() + 0.5,
            this.targetMudPos.getY() + 0.5,
            this.targetMudPos.getZ() + 0.5
        );

        if (this.bathingTicks % 20 == 0) {
            spawnBathingParticles();
            playBathingSound();
        }

        if (this.bathingTicks % HEAL_INTERVAL_TICKS == 0) {
            healFromBathing();
        }
    }

    /**
     * Spawns water particles to simulate bathing.
     */
    private void spawnBathingParticles() {
        if (this.mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.DRIPPING_WATER,
                this.mob.getX(),
                this.mob.getY() + 0.5,
                this.mob.getZ(),
                8,
                0.3, 0.2, 0.3,
                0.1
            );

            serverLevel.sendParticles(
                ParticleTypes.SPLASH,
                this.mob.getX(),
                this.mob.getY(),
                this.mob.getZ(),
                5,
                0.2, 0.1, 0.2,
                0.05
            );
        }
    }

    /**
     * Plays pig ambient sound at lower pitch during bathing.
     */
    private void playBathingSound() {
        this.mob.playSound(SoundEvents.PIG_AMBIENT, 0.4f, 0.8f);
    }

    /**
     * Heals the pig slightly while bathing.
     */
    private void healFromBathing() {
        if (this.mob.getHealth() < this.mob.getMaxHealth()) {
            this.mob.heal(BATHING_HEAL_AMOUNT);
            LOGGER.debug("{} healed from bathing: {} HP",
                this.mob.getName().getString(),
                String.format("%.1f", BATHING_HEAL_AMOUNT));
        }
    }

    /**
     * Determines if the path should be recalculated.
     *
     * @return true if path recalculation is needed
     */
    private boolean shouldRecalculatePath() {
        return this.tryTicks % 40 == 0;
    }
}
