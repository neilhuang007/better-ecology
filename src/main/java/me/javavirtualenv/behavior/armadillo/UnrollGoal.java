package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal for armadillo to unroll from defensive ball.
 * <p>
 * Triggers when:
 * - Threat has passed (no predators nearby)
 * - Safe environment detected
 * - After minimum roll duration
 * <p>
 * Unroll process:
 * - Takes time to fully unroll
 * - Vulnerable during unroll
 * - Movement gradually restored
 */
public class UnrollGoal extends Goal {

    private final Mob mob;
    private final EcologyComponent component;
    private final EcologyProfile profile;
    private final ArmadilloComponent armadilloComponent;

    private static final int UNROLL_DURATION = 40; // 2 seconds to unroll
    private static final int MIN_ROLL_TIME = 60; // Must stay rolled for at least 3 seconds
    private static final double SAFE_DISTANCE = 16.0; // No predators within 16 blocks

    public UnrollGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.profile = profile;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Must be rolled
        if (!armadilloComponent.isRolled()) {
            return false;
        }

        // Check if safe to unroll
        return isSafeToUnroll();
    }

    @Override
    public boolean canContinueToUse() {
        return armadilloComponent.isRolled() &&
               armadilloComponent.getUnrollTicks() < UNROLL_DURATION &&
               isSafeToUnroll();
    }

    @Override
    public void start() {
        armadilloComponent.setUnrollTicks(0);

        // Play start unrolling sound
        if (!mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ARMADILLO_UNROLL,
                SoundSource.NEUTRAL,
                0.4F,
                1.0F
            );
        }
    }

    @Override
    public void tick() {
        int currentTicks = armadilloComponent.getUnrollTicks();
        armadilloComponent.setUnrollTicks(currentTicks + 1);

        // Gradually restore movement
        double progress = (double) currentTicks / UNROLL_DURATION;
        double currentSpeed = profile.getDouble("movement.base_speed", 0.15) * progress;

        if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(currentSpeed);
        }

        // Spawn unroll particles
        if (currentTicks % 5 == 0 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                mob.getX(),
                mob.getY() + 0.3,
                mob.getZ(),
                2,
                0.1,
                0.1,
                0.1,
                0.0
            );
        }

        // Fully unrolled
        if (currentTicks >= UNROLL_DURATION) {
            armadilloComponent.setRolled(false);
            armadilloComponent.setUnrollTicks(0);

            // Restore full movement speed
            double baseSpeed = profile.getDouble("movement.base_speed", 0.15);
            if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
                mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);
            }

            // Play fully unrolled sound
            if (!mob.level().isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) mob.level();
                serverLevel.playSound(
                    null,
                    mob.getX(),
                    mob.getY(),
                    mob.getZ(),
                    SoundEvents.ARMADILLO_AMBIENT,
                    SoundSource.NEUTRAL,
                    0.3F,
                    1.0F
                );
            }
        }
    }

    @Override
    public void stop() {
        // If stopped early (threat detected), immediately roll back up
        if (armadilloComponent.getUnrollTicks() > 0 && armadilloComponent.getUnrollTicks() < UNROLL_DURATION) {
            armadilloComponent.setRolled(true);
            armadilloComponent.setUnrollTicks(0);
        }
    }

    /**
     * Checks if it's safe to unroll.
     */
    private boolean isSafeToUnroll() {
        // Check if no predators nearby
        if (isPredatorNearby()) {
            return false;
        }

        // Check if not panicking
        if (armadilloComponent.isPanicking()) {
            return false;
        }

        // Check if recently attacked
        if (mob.getLastHurtByMob() != null || mob.getLastHurtDamageMob() != null) {
            long lastHurtTime = mob.getLastHurtByMobTimestamp();
            long currentTime = mob.level().getGameTime();
            if (currentTime - lastHurtTime < MIN_ROLL_TIME) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a predator is nearby.
     */
    private boolean isPredatorNearby() {
        return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(SAFE_DISTANCE)).stream()
            .anyMatch(entity -> {
                net.minecraft.world.entity.EntityType<?> type = entity.getType();
                return type == net.minecraft.world.entity.EntityType.WOLF ||
                       type == net.minecraft.world.entity.EntityType.CAT ||
                       type == net.minecraft.world.entity.EntityType.OCELOT ||
                       type == net.minecraft.world.entity.EntityType.FOX;
            });
    }
}
