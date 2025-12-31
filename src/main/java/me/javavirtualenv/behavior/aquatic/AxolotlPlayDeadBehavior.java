package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Axolotl;

import java.util.UUID;

/**
 * Axolotl play dead behavior for regeneration and defense.
 * When damaged below certain health threshold, axolotl plays dead to regenerate.
 * <p>
 * Scientific basis: Axolotls can regenerate lost limbs and tissues.
 * In game, playing dead provides a brief period of damage avoidance and regeneration.
 */
public class AxolotlPlayDeadBehavior extends SteeringBehavior {
    private static final UUID REGEN_BOOST_UUID = UUID.fromString("7f101913-8c23-11ee-b9d1-0242ac120004");

    private final AquaticConfig config;
    private boolean isPlayingDead = false;
    private int playDeadTimer = 0;
    private long lastPlayDeadTime = 0;

    public AxolotlPlayDeadBehavior(AquaticConfig config) {
        super(1.0, true);
        this.config = config;
    }

    public AxolotlPlayDeadBehavior() {
        this(AquaticConfig.createDefault());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();

        if (!(self instanceof Axolotl axolotl)) {
            return new Vec3d();
        }

        // Check if should play dead
        if (!isPlayingDead && shouldTriggerPlayDead(axolotl)) {
            startPlayingDead(axolotl);
        }

        // Handle play dead state
        if (isPlayingDead) {
            handlePlayDeadState(axolotl);
            return new Vec3d(); // No movement when playing dead
        }

        return new Vec3d();
    }

    private boolean shouldTriggerPlayDead(Axolotl axolotl) {
        long currentTime = axolotl.level().getGameTime();

        // Check cooldown
        if (currentTime - lastPlayDeadTime < getPlayDeadCooldown()) {
            return false;
        }

        // Trigger if health is low
        float healthPercent = axolotl.getHealth() / axolotl.getMaxHealth();
        return healthPercent < getPlayDeadThreshold();
    }

    private void startPlayingDead(Axolotl axolotl) {
        isPlayingDead = true;
        playDeadTimer = getPlayDeadDuration();
        lastPlayDeadTime = axolotl.level().getGameTime();

        // Apply regeneration boost
        if (axolotl.getAttribute(Attributes.REGENERATION) != null) {
            AttributeModifier regenBoost = new AttributeModifier(
                REGEN_BOOST_UUID,
                "Play dead regeneration",
                2.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            axolotl.getAttribute(Attributes.REGENERATION).addPermanentModifier(regenBoost);
        }

        // Play dead sound
        if (!axolotl.level().isClientSide) {
            axolotl.playSound(net.minecraft.sounds.SoundEvents.AXOLOTL_HURT, 1.0F, 0.8F);
        }
    }

    private void handlePlayDeadState(Axolotl axolotl) {
        playDeadTimer--;

        // Regenerate health while playing dead
        if (axolotl.getHealth() < axolotl.getMaxHealth()) {
            axolotl.heal(0.5F);
        }

        // End play dead when timer expires or health is high enough
        float healthPercent = axolotl.getHealth() / axolotl.getMaxHealth();

        if (playDeadTimer <= 0 || healthPercent >= 0.7f) {
            stopPlayingDead(axolotl);
        }
    }

    private void stopPlayingDead(Axolotl axolotl) {
        isPlayingDead = false;
        playDeadTimer = 0;

        // Remove regeneration boost
        if (axolotl.getAttribute(Attributes.REGENERATION) != null) {
            axolotl.getAttribute(Attributes.REGENERATION).removeModifier(REGEN_BOOST_UUID);
        }
    }

    private int getPlayDeadDuration() {
        return 200; // 10 seconds
    }

    private long getPlayDeadCooldown() {
        return 1200; // 60 seconds cooldown
    }

    private float getPlayDeadThreshold() {
        return 0.3f; // Play dead when below 30% health
    }

    /**
     * Check if axolotl is currently playing dead.
     */
    public boolean isPlayingDead() {
        return isPlayingDead;
    }

    /**
     * Force axolotl to play dead (for external triggers).
     */
    public void triggerPlayDead(Axolotl axolotl) {
        if (!isPlayingDead) {
            startPlayingDead(axolotl);
        }
    }

    /**
     * Force axolotl to stop playing dead.
     */
    public void stopPlayDead(Axolotl axolotl) {
        if (isPlayingDead) {
            stopPlayingDead(axolotl);
        }
    }

    /**
     * Get remaining play dead time in ticks.
     */
    public int getPlayDeadTimer() {
        return playDeadTimer;
    }
}
