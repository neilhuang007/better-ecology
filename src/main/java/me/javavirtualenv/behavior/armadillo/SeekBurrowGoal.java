package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal for armadillo to seek and enter burrow.
 * <p>
 * Armadillos seek burrows:
 * - During the day (they're crepuscular/nocturnal)
 * - When threatened
 * - To rest and sleep
 * - During extreme temperatures
 * <p>
 * Behavior:
 * - Navigate to nearest burrow
 * - Enter burrow with animation
 * - Sleep while in burrow
 * - Exit when safe/time to forage
 */
public class SeekBurrowGoal extends Goal {

    private final Mob mob;
    private final EcologyComponent component;
    private final EcologyProfile profile;
    private final ArmadilloComponent armadilloComponent;
    private final ArmadilloBurrowSystem burrowSystem;

    private BlockPos targetBurrowPos;
    private int enterTicks;
    private static final int ENTER_DURATION = 30; // 1.5 seconds to enter
    private static final double BURROW_SEARCH_RANGE = 64.0; // Search for burrows within 64 blocks

    public SeekBurrowGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.profile = profile;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.burrowSystem = ArmadilloBurrowSystem.get(mob.level());
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Can't seek burrow while rolled
        if (armadilloComponent.isRolled()) {
            return false;
        }

        // Check if it's daytime (armadillos are crepuscular/nocturnal)
        long dayTime = mob.level().getDayTime() % 24000;
        boolean isDay = dayTime > 2000 && dayTime < 13000;

        // Seek burrow during day, when threatened, or when tired
        boolean shouldSeekBurrow = isDay ||
                                  armadilloComponent.isPanicking() ||
                                  isTired();

        if (!shouldSeekBurrow) {
            return false;
        }

        // Check if already at/near burrow
        BlockPos currentBurrow = armadilloComponent.getBurrowPos();
        if (currentBurrow != null) {
            double distance = mob.blockPosition().distSqr(currentBurrow);
            if (distance <= 9.0) {
                return false; // Already at burrow
            }
        }

        // Find nearest burrow
        ArmadilloBurrow burrow = burrowSystem.findAvailableBurrow(mob.blockPosition(), BURROW_SEARCH_RANGE);
        if (burrow == null) {
            return false;
        }

        targetBurrowPos = burrow.getPosition();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (armadilloComponent.isRolled()) {
            return false;
        }

        if (targetBurrowPos == null) {
            return false;
        }

        // Continue if not yet entered
        return enterTicks < ENTER_DURATION;
    }

    @Override
    public void start() {
        enterTicks = 0;
    }

    @Override
    public void tick() {
        double distance = mob.blockPosition().distSqr(targetBurrowPos);

        // Move towards burrow
        if (distance > 4.0) {
            mob.getNavigation().moveTo(
                targetBurrowPos.getX(),
                targetBurrowPos.getY(),
                targetBurrowPos.getZ(),
                0.7
            );
        } else {
            // Close enough to enter
            performEnter();
        }

        // Look at burrow entrance
        mob.getLookControl().setLookAt(
            targetBurrowPos.getX(),
            targetBurrowPos.getY(),
            targetBurrowPos.getZ()
        );
    }

    @Override
    public void stop() {
        // Update current burrow position
        if (enterTicks >= ENTER_DURATION) {
            armadilloComponent.setBurrowPos(targetBurrowPos);
        }
        targetBurrowPos = null;
        enterTicks = 0;
    }

    /**
     * Performs entering animation and logic.
     */
    private void performEnter() {
        enterTicks++;

        // Play enter sound
        if (enterTicks == 1 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ARMADILLO_STEP,
                SoundSource.NEUTRAL,
                0.3F,
                0.7F
            );
        }

        // Shrink into burrow
        if (mob.level() instanceof ServerLevel serverLevel) {
            double scale = 1.0 - ((double) enterTicks / ENTER_DURATION);
            // Note: Actual size change would require mixin to entity dimensions
            // For now, use particle effect
            if (enterTicks % 5 == 0) {
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.POOF,
                    mob.getX(),
                    mob.getY() + 0.2,
                    mob.getZ(),
                    2,
                    0.1,
                    0.1,
                    0.1,
                    0.0
                );
            }
        }

        // Fully entered
        if (enterTicks >= ENTER_DURATION) {
            ArmadilloBurrow burrow = burrowSystem.findNearestBurrow(targetBurrowPos, 1.0);
            if (burrow != null) {
                burrowSystem.enterBurrow(mob, burrow);
                armadilloComponent.setBurrowPos(targetBurrowPos);

                // Play entered sound
                if (!mob.level().isClientSide()) {
                    ServerLevel serverLevel = (ServerLevel) mob.level();
                    serverLevel.playSound(
                        null,
                        targetBurrowPos.getX(),
                        targetBurrowPos.getY(),
                        targetBurrowPos.getZ(),
                        SoundEvents.ARMADILLO_IDLE,
                        SoundSource.NEUTRAL,
                        0.2F,
                        0.8F
                    );
                }
            }
        }
    }

    /**
     * Checks if armadillo is tired and needs rest.
     */
    private boolean isTired() {
        CompoundTag energyTag = component.getHandleTag("energy");
        if (energyTag.contains("energy")) {
            int energy = energyTag.getInt("energy");
            return energy < 30;
        }
        return false;
    }
}
