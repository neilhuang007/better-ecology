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

    private static final int MIN_DURATION_TICKS = 2400;  // 120 seconds
    private static final int MAX_DURATION_TICKS = 3600;  // 180 seconds
    private static final int MIN_COOLDOWN_TICKS = 600;   // 30 seconds
    private static final int MAX_COOLDOWN_TICKS = 1200;  // 60 seconds
    private static final int SEARCH_RADIUS = 8;
    private static final int SOCIAL_SEARCH_RADIUS = 8;
    private static final double ACCEPTED_DISTANCE = 1.5;
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
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }

        if (isCurrentlyOnDustBathSurface()) {
            this.targetDustPos = this.chicken.blockPosition().below();
            return true;
        }

        return findNearestDustBathSpot();
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

        if (isNearDustBathSpot()) {
            this.chicken.getNavigation().stop();
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
        this.cooldownTicks = MIN_COOLDOWN_TICKS +
            this.chicken.getRandom().nextInt(MAX_COOLDOWN_TICKS - MIN_COOLDOWN_TICKS);
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

        if (isNearDustBathSpot()) {
            performDustBathing();
        } else {
            if (shouldRecalculatePath()) {
                navigateToDustBath();
            }
        }
    }

    private boolean isCurrentlyOnDustBathSurface() {
        BlockPos groundPos = this.chicken.blockPosition().below();
        return isDustBathBlock(groundPos);
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
