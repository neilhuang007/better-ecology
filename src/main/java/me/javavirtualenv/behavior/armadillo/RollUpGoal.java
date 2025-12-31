package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.EntityType;

import java.util.EnumSet;

/**
 * Goal for armadillo to roll up into a ball for defense.
 * <p>
 * Triggered when:
 * - Predator detected nearby
 * - Attacked by hostile mob
 * - Sudden loud noise (startle response)
 * <p>
 * While rolled:
 * - Movement disabled
 * - Damage reduced significantly
 * - Invulnerable to most attacks
 */
public class RollUpGoal extends Goal {

    private final Mob mob;
    private final EcologyComponent component;
    private final EcologyProfile profile;
    private final ArmadilloComponent armadilloComponent;

    private static final int ROLL_DURATION = 60; // 3 seconds to fully roll
    private static final double ROLL_TRIGGER_DISTANCE = 8.0; // Roll when predator within 8 blocks

    public RollUpGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.profile = profile;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Can't roll if already rolled
        if (armadilloComponent.isRolled()) {
            return false;
        }

        // Can't roll if in burrow
        if (armadilloComponent.getBurrowPos() != null) {
            BlockPos burrowPos = armadilloComponent.getBurrowPos();
            if (mob.blockPosition().distSqr(burrowPos) <= 9.0) {
                return false;
            }
        }

        // Check if predator nearby
        if (isPredatorNearby()) {
            return true;
        }

        // Check if recently attacked
        if (wasRecentlyAttacked()) {
            return true;
        }

        // Check if panicking
        if (armadilloComponent.isPanicking()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return armadilloComponent.getRollTicks() < ROLL_DURATION && armadilloComponent.canRoll(mob);
    }

    @Override
    public void start() {
        armadilloComponent.setRollTicks(0);

        // Play start rolling sound
        if (!mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ARMADILLO_ROLL, // Vanilla armadillo roll sound
                SoundSource.NEUTRAL,
                0.5F,
                1.0F
            );
        }
    }

    @Override
    public void tick() {
        int currentTicks = armadilloComponent.getRollTicks();
        armadilloComponent.setRollTicks(currentTicks + 1);

        // Slow down movement as rolling progresses
        double slowdown = 1.0 - ((double) currentTicks / ROLL_DURATION);
        mob.setAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue() * slowdown);

        // Spawn curl particles
        if (currentTicks % 5 == 0 && !mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.HEART,
                mob.getX(),
                mob.getY() + 0.5,
                mob.getZ(),
                1,
                0.1,
                0.1,
                0.1,
                0.0
            );
        }

        // Fully rolled
        if (currentTicks >= ROLL_DURATION) {
            armadilloComponent.setRolled(true);
            armadilloComponent.setRollTicks(0);

            // Play fully rolled sound
            if (!mob.level().isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) mob.level();
                serverLevel.playSound(
                    null,
                    mob.getX(),
                    mob.getY(),
                    mob.getZ(),
                    SoundEvents.ARMADILLO_LAND,
                    SoundSource.NEUTRAL,
                    0.4F,
                    0.8F
                );
            }
        }
    }

    @Override
    public void stop() {
        // Reset movement speed
        double baseSpeed = profile.getDouble("movement.base_speed", 0.15);
        mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);
    }

    /**
     * Checks if a predator is nearby.
     */
    private boolean isPredatorNearby() {
        // Check for wolves, cats, ocelots, foxes
        return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(ROLL_TRIGGER_DISTANCE)).stream()
            .anyMatch(entity -> {
                EntityType<?> type = entity.getType();
                return type == EntityType.WOLF ||
                       type == EntityType.CAT ||
                       type == EntityType.OCELOT ||
                       type == EntityType.FOX;
            });
    }

    /**
     * Checks if the armadillo was recently attacked.
     */
    private boolean wasRecentlyAttacked() {
        EntityState state = component.state();
        return state.isFleeing() || mob.getLastHurtByMob() != null ||
               mob.getLastHurtDamageMob() != null;
    }
}
