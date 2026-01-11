package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

    private static final ResourceLocation ROLL_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("better-ecology", "roll_slowdown");

    // Configuration constants
    private static final int ROLL_DURATION = 60; // 3 seconds to fully roll
    private static final double ROLL_TRIGGER_DISTANCE = 8.0; // Roll when predator within 8 blocks
    private static final double BURROW_SAFE_DISTANCE = 9.0; // Don't roll if within 3 blocks of burrow
    private static final int ATTACK_MEMORY_TICKS = 100; // Remember attacks for 5 seconds

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
    private final ArmadilloComponent armadilloComponent;
    private int rollTicks;

    // Debug info
    private String lastDebugMessage = "";
    private boolean wasPredatorNearby = false;

    public RollUpGoal(Mob mob, EcologyComponent component, EcologyProfile profile) {
        this.mob = mob;
        this.component = component;
        this.armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (mob.level().isClientSide) {
            return false;
        }

        // Can't roll if already rolled
        if (armadilloComponent.isRolled()) {
            return false;
        }

        // Can't roll if in burrow
        BlockPos burrowPos = getBurrowPos();
        if (burrowPos != null) {
            double distance = mob.blockPosition().distSqr(burrowPos);
            if (distance <= BURROW_SAFE_DISTANCE) {
                return false;
            }
        }

        // Check if predator nearby
        boolean predatorNearby = isPredatorNearby();

        // Log state change
        if (predatorNearby != wasPredatorNearby) {
            debug("predator proximity changed: " + wasPredatorNearby + " -> " + predatorNearby);
            wasPredatorNearby = predatorNearby;
        }

        if (predatorNearby) {
            debug("STARTING: predator detected nearby");
            return true;
        }

        // Check if recently attacked
        if (wasRecentlyAttacked()) {
            debug("STARTING: recently attacked");
            return true;
        }

        // Check if panicking
        if (isPanicking()) {
            debug("STARTING: panicking");
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue until fully rolled
        return rollTicks < ROLL_DURATION && !armadilloComponent.isRolled();
    }

    @Override
    public void start() {
        rollTicks = 0;
        debug("goal started, rolling up");

        // Play start rolling sound
        if (!mob.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) mob.level();
            serverLevel.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.ARMADILLO_ROLL,
                SoundSource.NEUTRAL,
                0.5F,
                1.0F
            );
        }
    }

    @Override
    public void tick() {
        rollTicks++;

        // Slow down movement as rolling progresses using attribute modifier
        double progress = (double) rollTicks / ROLL_DURATION;
        double slowdownFactor = -0.9 * progress; // Reduce speed by up to 90%

        if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
            // Remove old modifier if present
            if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .getModifier(ROLL_SPEED_MODIFIER_ID) != null) {
                mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                        .removeModifier(ROLL_SPEED_MODIFIER_ID);
            }

            // Add new modifier with updated slowdown
            AttributeModifier speedModifier = new AttributeModifier(
                    ROLL_SPEED_MODIFIER_ID,
                    slowdownFactor,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .addPermanentModifier(speedModifier);
        }

        // Spawn curl particles
        if (rollTicks % 5 == 0 && !mob.level().isClientSide()) {
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

        // Log progress every second
        if (rollTicks % 20 == 0) {
            debug("rolling up, progress=" + String.format("%.0f", progress * 100) + "%");
        }

        // Fully rolled
        if (rollTicks >= ROLL_DURATION) {
            armadilloComponent.setRolled(true);
            rollTicks = 0;
            debug("fully rolled up");

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
        // Remove the speed modifier to restore normal movement
        if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .removeModifier(ROLL_SPEED_MODIFIER_ID);
        }
        debug("goal stopped");
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Checks if a predator is nearby.
     */
    private boolean isPredatorNearby() {
        return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(ROLL_TRIGGER_DISTANCE)).stream()
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
     * Checks if the armadillo was recently attacked.
     */
    private boolean wasRecentlyAttacked() {
        // Check if was recently hurt
        if (mob.getLastHurtByMob() != null) {
            long lastHurtTime = mob.getLastHurtByMobTimestamp();
            long currentTime = mob.level().getGameTime();
            return currentTime - lastHurtTime < ATTACK_MEMORY_TICKS;
        }

        // Check fleeing state in NBT
        CompoundTag tag = component.getHandleTag("behavior");
        if (tag.contains("isFleeing") && tag.getBoolean("isFleeing")) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the armadillo is panicking.
     */
    private boolean isPanicking() {
        return armadilloComponent.isPanicking();
    }

    /**
     * Get the burrow position from NBT.
     */
    private BlockPos getBurrowPos() {
        CompoundTag armadilloTag = component.getHandleTag("armadillo");
        if (!armadilloTag.contains("BurrowPos")) {
            return null;
        }
        int[] posArray = armadilloTag.getIntArray("BurrowPos");
        if (posArray.length != 3) {
            return null;
        }
        return new BlockPos(posArray[0], posArray[1], posArray[2]);
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ArmadilloRoll] Armadillo #" + mob.getId() + " ";
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
        BlockPos burrowPos = getBurrowPos();
        return String.format("rolled=%s, rollTicks=%d/%d, predatorNearby=%s, burrow=%s, panicking=%s",
            armadilloComponent.isRolled(),
            rollTicks,
            ROLL_DURATION,
            isPredatorNearby(),
            burrowPos != null ? burrowPos.getX() + "," + burrowPos.getZ() : "none",
            isPanicking()
        );
    }
}
