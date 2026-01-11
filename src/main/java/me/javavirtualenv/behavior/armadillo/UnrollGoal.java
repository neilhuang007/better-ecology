package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.EntityType;

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

    // Configuration constants
    private static final int UNROLL_DURATION = 40; // 2 seconds to unroll
    private static final int MIN_ROLL_TIME = 60; // Must stay rolled for at least 3 seconds
    private static final double SAFE_DISTANCE = 16.0; // No predators within 16 blocks

    // Predator types
    private static final EntityType<?>[] PREDATORS = {
        EntityType.WOLF,
        EntityType.CAT,
        EntityType.OCELOT,
        EntityType.FOX
    };

    // Instance fields
    private final Mob mob;
    private final EcologyComponent component;
    private final EcologyProfile profile;
    private final ArmadilloComponent armadilloComponent;
    private int unrollTicks;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasSafeLastCheck = false;

    public UnrollGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.profile = profile;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (mob.level().isClientSide) {
            return false;
        }

        // Must be rolled
        if (!armadilloComponent.isRolled()) {
            return false;
        }

        // Check if safe to unroll
        boolean isSafe = isSafeToUnroll();

        // Log state change
        if (isSafe != wasSafeLastCheck) {
            debug("safety state changed: " + wasSafeLastCheck + " -> " + isSafe);
            wasSafeLastCheck = isSafe;
        }

        if (!isSafe) {
            return false;
        }

        debug("STARTING: unrolling (safe environment detected)");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue if rolled and not finished unrolling
        if (!armadilloComponent.isRolled()) {
            return false;
        }

        // Stop if no longer safe
        if (!isSafeToUnroll()) {
            debug("no longer safe, aborting unroll");
            return false;
        }

        return unrollTicks < UNROLL_DURATION;
    }

    @Override
    public void start() {
        unrollTicks = 0;
        debug("goal started, unrolling");

        // Play start unrolling sound
        if (!mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ARMADILLO_UNROLL_START,
                SoundSource.NEUTRAL,
                0.4F,
                1.0F
            );
        }
    }

    @Override
    public void tick() {
        unrollTicks++;

        // Gradually restore movement
        double progress = (double) unrollTicks / UNROLL_DURATION;
        double currentSpeed = profile.getDouble("movement.base_speed", 0.15) * progress;

        if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(currentSpeed);
        }

        // Spawn unroll particles
        if (unrollTicks % 5 == 0 && !mob.level().isClientSide()) {
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

        // Log progress every half second
        if (unrollTicks % 10 == 0) {
            debug("unrolling, progress=" + String.format("%.0f", progress * 100) + "%");
        }

        // Fully unrolled
        if (unrollTicks >= UNROLL_DURATION) {
            armadilloComponent.setRolled(false);
            unrollTicks = 0;

            // Restore full movement speed
            double baseSpeed = profile.getDouble("movement.base_speed", 0.15);
            if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
                mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);
            }

            debug("fully unrolled");

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
        if (unrollTicks > 0 && unrollTicks < UNROLL_DURATION) {
            debug("unroll interrupted, rolling back up");
            armadilloComponent.setRolled(true);
            unrollTicks = 0;

            // Restore base speed while rolled
            double baseSpeed = profile.getDouble("movement.base_speed", 0.15);
            if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
                mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);
            }
        } else {
            debug("goal stopped");
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
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
        if (mob.getLastHurtByMob() != null || mob.getLastHurtMob() != null) {
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
                EntityType<?> type = entity.getType();
                for (EntityType<?> predator : PREDATORS) {
                    if (type == predator) {
                        return true;
                    }
                }
                return false;
            });
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ArmadilloUnroll] Armadillo #" + mob.getId() + " ";
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
        return String.format("rolled=%s, unrollTicks=%d/%d, predatorNearby=%s, panicking=%s, safe=%s",
            armadilloComponent.isRolled(),
            unrollTicks,
            UNROLL_DURATION,
            isPredatorNearby(),
            armadilloComponent.isPanicking(),
            isSafeToUnroll()
        );
    }
}
