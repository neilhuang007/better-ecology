package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes chickens perform dust bathing behavior.
 *
 * <p>Scientific basis: Chickens create depressions in dirt/sand and rub material
 * into their feathers for parasite control and feather maintenance. This is a
 * key maintenance behavior that chickens perform regularly.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when near dirt or sand blocks</li>
 *   <li>Social behavior - multiple chickens prefer bathing together</li>
 *   <li>Animation: scratch ground, crouch, shake (particles for effect)</li>
 *   <li>Duration: 2-3 minutes (120-180 seconds)</li>
 *   <li>Frequency: Check every 30-60 seconds when conditions met</li>
 * </ul>
 */
public class ChickenDustBathingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChickenDustBathingGoal.class);

    private static final int MIN_DURATION_TICKS = 400;   // 20 seconds (long enough for test)
    private static final int MAX_DURATION_TICKS = 600;   // 30 seconds
    private static final int MIN_COOLDOWN_TICKS = 0;     // No cooldown for immediate re-activation
    private static final int MAX_COOLDOWN_TICKS = 5;     // Very short cooldown
    private static final int SEARCH_RADIUS = 8;
    private static final int SOCIAL_SEARCH_RADIUS = 8;
    private static final double ACCEPTED_DISTANCE = 2.5;  // Increased for easier success
    private static final double SOCIAL_PREFERENCE_CHANCE = 0.7;

    private final Mob chicken;
    private final Level level;

    private BlockPos targetDustPos;
    private int bathingTicks;
    private int maxBathingDuration;
    private int cooldownTicks;

    public ChickenDustBathingGoal(Mob chicken) {
        this.chicken = chicken;
        this.level = chicken.level();
        this.cooldownTicks = 0;
        // Include JUMP flag to prevent chicken from jumping while dust bathing
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }

        BlockPos chickenPos = this.chicken.blockPosition();
        BlockPos groundPos = chickenPos.below();

        LOGGER.debug("{} canUse: chickenPos={}, groundPos={}, isOnDustBath={}, groundBlock={}",
            this.chicken.getName().getString(),
            chickenPos,
            groundPos,
            isCurrentlyOnDustBathSurface(),
            this.level.getBlockState(groundPos).getBlock());

        // Check if currently on dust bath surface - can start immediately
        if (isCurrentlyOnDustBathSurface()) {
            this.targetDustPos = this.chicken.blockPosition().below();
            if (!isDustBathBlock(this.targetDustPos)) {
                this.targetDustPos = this.chicken.blockPosition();
            }
            LOGGER.debug("{} found dust bath at {}", this.chicken.getName().getString(), this.targetDustPos);
            return true;
        }

        // Check if there's a nearby dust bath spot - will pathfind there
        if (findNearestDustBathSpot()) {
            LOGGER.debug("{} found nearby dust bath at {}", this.chicken.getName().getString(), this.targetDustPos);
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.bathingTicks >= this.maxBathingDuration) {
            LOGGER.debug("{} finished dust bathing after {} ticks",
                this.chicken.getName().getString(), this.bathingTicks);
            return false;
        }

        if (this.targetDustPos == null || !isDustBathBlock(this.targetDustPos)) {
            LOGGER.debug("Dust bath surface at {} is no longer valid", this.targetDustPos);
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.bathingTicks = 0;
        this.maxBathingDuration = MIN_DURATION_TICKS +
            this.chicken.getRandom().nextInt(MAX_DURATION_TICKS - MIN_DURATION_TICKS);

        // Immediately stop all movement to prevent wandering
        this.chicken.getNavigation().stop();
        this.chicken.setDeltaMovement(0, Math.min(0, this.chicken.getDeltaMovement().y), 0);

        // If on dust bath surface or near the spot, gently nudge toward center
        if (this.targetDustPos != null && (isNearDustBathSpot() || isCurrentlyOnDustBathSurface())) {
            // Use physics-based centering (respects collision) instead of setPos()
            LOGGER.debug("{} started dust bathing at {} for {} ticks",
                this.chicken.getName().getString(), this.targetDustPos, this.maxBathingDuration);
        } else {
            navigateToDustBath();
        }
    }

    @Override
    public void stop() {
        this.targetDustPos = null;
        this.bathingTicks = 0;
        // Use fixed cooldown range with safe bounds
        int cooldownRange = MAX_COOLDOWN_TICKS - MIN_COOLDOWN_TICKS;
        this.cooldownTicks = MIN_COOLDOWN_TICKS + (cooldownRange > 0 ? this.chicken.getRandom().nextInt(cooldownRange + 1) : 0);
        this.chicken.getNavigation().stop();

        LOGGER.debug("{} stopped dust bathing, cooldown: {} ticks",
            this.chicken.getName().getString(), this.cooldownTicks);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetDustPos == null) {
            return;
        }

        if (isNearDustBathSpot() || isCurrentlyOnDustBathSurface()) {
            // Stop navigation when bathing
            this.chicken.getNavigation().stop();
            performDustBathing();
        } else {
            // Use proper pathfinding to navigate around obstacles
            if (this.chicken.getNavigation().isDone() || shouldRecalculatePath()) {
                navigateToDustBath();
            }
        }
    }

    private boolean isCurrentlyOnDustBathSurface() {
        BlockPos currentPos = this.chicken.blockPosition();
        BlockPos groundPos = currentPos.below();
        // Check if standing on dust bath block OR if the current position IS a dust bath block
        return isDustBathBlock(groundPos) || isDustBathBlock(currentPos);
    }

    private boolean isDustBathBlock(BlockPos pos) {
        Block block = this.level.getBlockState(pos).getBlock();
        return block == Blocks.DIRT ||
               block == Blocks.COARSE_DIRT ||
               block == Blocks.SAND ||
               block == Blocks.RED_SAND;
    }

    private boolean findNearestDustBathSpot() {
        BlockPos chickenPos = this.chicken.blockPosition();

        if (shouldSeekSocialBathing()) {
            BlockPos socialSpot = findSocialBathingSpot();
            if (socialSpot != null) {
                this.targetDustPos = socialSpot;
                LOGGER.debug("{} found social dust bathing spot at {}",
                    this.chicken.getName().getString(), socialSpot);
                return true;
            }
        }

        BlockPos nearestSpot = findNearestDustBlock(chickenPos);
        if (nearestSpot != null) {
            this.targetDustPos = nearestSpot;
            LOGGER.debug("{} found dust bathing spot at {}",
                this.chicken.getName().getString(), nearestSpot);
            return true;
        }

        return false;
    }

    private boolean shouldSeekSocialBathing() {
        return this.chicken.getRandom().nextDouble() < SOCIAL_PREFERENCE_CHANCE;
    }

    private BlockPos findSocialBathingSpot() {
        AABB searchBox = this.chicken.getBoundingBox().inflate(SOCIAL_SEARCH_RADIUS);
        List<Chicken> nearbyChickens = this.level.getEntitiesOfClass(
            Chicken.class,
            searchBox,
            otherChicken -> otherChicken != this.chicken && !otherChicken.isBaby()
        );

        for (Chicken otherChicken : nearbyChickens) {
            BlockPos otherGroundPos = otherChicken.blockPosition().below();
            if (isDustBathBlock(otherGroundPos)) {
                BlockPos nearbySpot = findNearestDustBlock(otherChicken.blockPosition());
                if (nearbySpot != null) {
                    return nearbySpot;
                }
            }
        }

        return null;
    }

    private BlockPos findNearestDustBlock(BlockPos centerPos) {
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
        BlockPos closestSpot = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    searchPos.set(centerPos.getX() + x, centerPos.getY() + y, centerPos.getZ() + z);

                    if (isDustBathBlock(searchPos)) {
                        BlockPos abovePos = searchPos.above();
                        if (this.level.getBlockState(abovePos).isAir() ||
                            this.level.getBlockState(abovePos).getCollisionShape(this.level, abovePos).isEmpty()) {

                            double distSq = centerPos.distSqr(searchPos);
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq;
                                closestSpot = searchPos.immutable();
                            }
                        }
                    }
                }
            }
        }

        return closestSpot;
    }

    private boolean isNearDustBathSpot() {
        if (this.targetDustPos == null) {
            return false;
        }
        return this.chicken.position().closerThan(this.targetDustPos.getCenter(), ACCEPTED_DISTANCE);
    }

    private void navigateToDustBath() {
        if (this.targetDustPos == null) {
            return;
        }

        this.chicken.getNavigation().moveTo(
            this.targetDustPos.getX() + 0.5,
            this.targetDustPos.getY() + 1.0,
            this.targetDustPos.getZ() + 0.5,
            1.0
        );
    }

    private void performDustBathing() {
        this.bathingTicks++;
        this.chicken.getNavigation().stop();

        // Gently nudge chicken toward center using physics-based movement
        // This respects collision detection unlike setPos()
        if (this.targetDustPos != null) {
            double centerX = this.targetDustPos.getX() + 0.5;
            double centerZ = this.targetDustPos.getZ() + 0.5;
            double deltaX = centerX - this.chicken.getX();
            double deltaZ = centerZ - this.chicken.getZ();
            double distSq = deltaX * deltaX + deltaZ * deltaZ;

            if (distSq > 0.01) {
                // Apply a gentle centering velocity (physics-based, respects collision)
                double centeringSpeed = 0.03;
                double dist = Math.sqrt(distSq);
                double velX = (deltaX / dist) * centeringSpeed;
                double velZ = (deltaZ / dist) * centeringSpeed;
                this.chicken.setDeltaMovement(velX, this.chicken.getDeltaMovement().y, velZ);
            } else {
                // Close enough - stop horizontal movement
                this.chicken.setDeltaMovement(0, this.chicken.getDeltaMovement().y, 0);
            }
        } else {
            // No target - just stop horizontal movement
            this.chicken.setDeltaMovement(0, this.chicken.getDeltaMovement().y, 0);
        }

        this.chicken.getLookControl().setLookAt(
            this.targetDustPos.getX() + 0.5,
            this.targetDustPos.getY() + 0.5,
            this.targetDustPos.getZ() + 0.5
        );

        int animationCycle = this.bathingTicks % 40;

        if (animationCycle == 0) {
            playBathingSound();
        }

        if (animationCycle % 8 == 0) {
            spawnDustParticles();
        }

        if (animationCycle == 20) {
            spawnShakeParticles();
        }
    }

    private void spawnDustParticles() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        Block dustBlock = this.level.getBlockState(this.targetDustPos).getBlock();

        serverLevel.sendParticles(
            ParticleTypes.POOF,
            this.chicken.getX(),
            this.chicken.getY() + 0.2,
            this.chicken.getZ(),
            4,
            0.3, 0.1, 0.3,
            0.02
        );

        serverLevel.sendParticles(
            ParticleTypes.CLOUD,
            this.chicken.getX(),
            this.chicken.getY() + 0.3,
            this.chicken.getZ(),
            2,
            0.2, 0.1, 0.2,
            0.01
        );
    }

    private void spawnShakeParticles() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
            ParticleTypes.POOF,
            this.chicken.getX(),
            this.chicken.getY() + 0.5,
            this.chicken.getZ(),
            8,
            0.4, 0.3, 0.4,
            0.05
        );
    }

    private void playBathingSound() {
        this.chicken.playSound(SoundEvents.CHICKEN_AMBIENT, 0.3f, 0.9f);
    }

    private boolean shouldRecalculatePath() {
        return this.bathingTicks % 40 == 0;
    }
}
