package me.javavirtualenv.behavior.core;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes rabbits perform "binky" jumps when safe and happy.
 * <p>
 * A binky is a characteristic rabbit behavior where the rabbit jumps into the air
 * and twists its body mid-flight. This is a sign of contentment and playfulness,
 * typically seen when rabbits feel safe, well-fed, and socially comfortable.
 * <p>
 * Conditions for binky:
 * <ul>
 *   <li>No predators within 24 blocks (safety)</li>
 *   <li>At least 2 rabbits nearby (social comfort)</li>
 *   <li>Not recently performed (2-5 minute intervals)</li>
 *   <li>On ground (needs solid surface to jump from)</li>
 * </ul>
 * <p>
 * Binky mechanics:
 * <ul>
 *   <li>Jump height: 1.5 blocks with mid-air rotation</li>
 *   <li>Body twist during jump for visual effect</li>
 *   <li>Happy particles and sound effects</li>
 * </ul>
 */
public class RabbitBinkyGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitBinkyGoal.class);

    private static final int SAFETY_RADIUS = 24;
    private static final int MIN_GROUP_SIZE = 2;
    private static final int MIN_COOLDOWN_TICKS = 2400;
    private static final int MAX_COOLDOWN_TICKS = 6000;
    private static final float JUMP_VELOCITY = 0.7F;
    private static final int BINKY_DURATION = 20;

    private final PathfinderMob mob;
    private final List<Class<? extends LivingEntity>> predatorTypes;

    private int cooldownTicks;
    private int binkyTicks;
    private boolean isJumping;
    private float initialYaw;

    /**
     * Creates a new rabbit binky goal.
     *
     * @param mob the rabbit that will binky
     * @param predatorTypes array of entity classes to check for safety
     */
    @SafeVarargs
    public RabbitBinkyGoal(
            PathfinderMob mob,
            Class<? extends LivingEntity>... predatorTypes) {
        this.mob = mob;
        this.predatorTypes = List.of(predatorTypes);
        this.cooldownTicks = MIN_COOLDOWN_TICKS;
        this.binkyTicks = 0;
        this.isJumping = false;
        this.initialYaw = 0.0F;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }

        if (!this.mob.onGround()) {
            return false;
        }

        if (!isSafeFromPredators()) {
            return false;
        }

        if (!hasEnoughNearbyRabbits()) {
            return false;
        }

        LOGGER.debug("{} conditions met for binky - safe, social, and happy",
                mob.getName().getString());

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.binkyTicks < BINKY_DURATION;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting binky jump!", mob.getName().getString());

        this.binkyTicks = 0;
        this.isJumping = false;
        this.initialYaw = this.mob.getYRot();

        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} completed binky", mob.getName().getString());

        this.binkyTicks = 0;
        this.isJumping = false;

        this.cooldownTicks = MIN_COOLDOWN_TICKS +
                this.mob.getRandom().nextInt(MAX_COOLDOWN_TICKS - MIN_COOLDOWN_TICKS + 1);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.binkyTicks++;

        if (this.binkyTicks == 5 && this.mob.onGround() && !this.isJumping) {
            performBinkyJump();
        }

        if (this.isJumping && this.mob.onGround() && this.binkyTicks > 10) {
            this.isJumping = false;
            playLandingEffects();
        }

        if (this.isJumping) {
            updateMidAirTwist();
        }
    }

    /**
     * Performs the binky jump with upward velocity and rotation.
     */
    private void performBinkyJump() {
        Vec3 currentMotion = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(currentMotion.x, JUMP_VELOCITY, currentMotion.z);

        this.mob.hasImpulse = true;
        this.isJumping = true;

        this.mob.playSound(SoundEvents.RABBIT_JUMP, 0.6F, 1.0F + this.mob.getRandom().nextFloat() * 0.2F);

        spawnBinkyParticles();

        LOGGER.debug("{} performed binky jump with velocity {}",
                mob.getName().getString(),
                JUMP_VELOCITY);
    }

    /**
     * Updates the rabbit's rotation during the mid-air twist.
     */
    private void updateMidAirTwist() {
        float twistAngle = 90.0F;
        float twistProgress = (this.binkyTicks - 5) / 10.0F;

        if (twistProgress > 0.0F && twistProgress < 1.0F) {
            float currentTwist = twistAngle * (float) Math.sin(twistProgress * Math.PI);
            this.mob.setYRot(this.initialYaw + currentTwist);
            this.mob.yRotO = this.mob.getYRot();
        }
    }

    /**
     * Spawns happy particles during the binky jump.
     */
    private void spawnBinkyParticles() {
        if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            double offsetX = (this.mob.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetY = this.mob.getRandom().nextDouble() * 0.5;
            double offsetZ = (this.mob.getRandom().nextDouble() - 0.5) * 0.5;

            serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    this.mob.getX() + offsetX,
                    this.mob.getY() + offsetY,
                    this.mob.getZ() + offsetZ,
                    1,
                    0, 0.1, 0,
                    0.1
            );
        }
    }

    /**
     * Plays landing effects after the binky jump completes.
     */
    private void playLandingEffects() {
        if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.POOF,
                this.mob.getX(),
                this.mob.getY(),
                this.mob.getZ(),
                3,
                0.2, 0.0, 0.2,
                0.02
        );

        this.mob.playSound(SoundEvents.RABBIT_AMBIENT, 0.5F, 1.2F);

        LOGGER.debug("{} landed binky jump", mob.getName().getString());
    }

    /**
     * Checks if the rabbit is safe from predators.
     *
     * @return true if no predators within safety radius
     */
    private boolean isSafeFromPredators() {
        for (Class<? extends LivingEntity> predatorType : this.predatorTypes) {
            List<? extends LivingEntity> nearbyPredators = this.mob.level()
                    .getEntitiesOfClass(
                            predatorType,
                            this.mob.getBoundingBox().inflate(SAFETY_RADIUS, 3.0, SAFETY_RADIUS)
                    );

            if (!nearbyPredators.isEmpty()) {
                LOGGER.debug("{} not safe for binky - {} {} within {} blocks",
                        mob.getName().getString(),
                        nearbyPredators.size(),
                        predatorType.getSimpleName(),
                        SAFETY_RADIUS);
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if there are enough rabbits nearby for social comfort.
     *
     * @return true if at least MIN_GROUP_SIZE rabbits nearby (including self)
     */
    private boolean hasEnoughNearbyRabbits() {
        List<Rabbit> nearbyRabbits = this.mob.level().getEntitiesOfClass(
                Rabbit.class,
                this.mob.getBoundingBox().inflate(SAFETY_RADIUS / 2.0, 3.0, SAFETY_RADIUS / 2.0),
                rabbit -> rabbit.isAlive()
        );

        boolean hasEnough = nearbyRabbits.size() >= MIN_GROUP_SIZE;

        if (!hasEnough) {
            LOGGER.debug("{} not enough rabbits for binky - found {}, need {}",
                    mob.getName().getString(),
                    nearbyRabbits.size(),
                    MIN_GROUP_SIZE);
        }

        return hasEnough;
    }
}
