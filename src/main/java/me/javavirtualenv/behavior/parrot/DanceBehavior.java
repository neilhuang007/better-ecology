package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Random;

/**
 * Dance behavior for parrots.
 * Parrots dance to music with different styles based on the music disc.
 */
public class DanceBehavior {
    private final Mob parrot;
    private final DanceConfig config;
    private final EcologyComponent component;
    private final Random random = new Random();

    private boolean isDancing = false;
    private DanceStyle currentStyle;
    private BlockPos dancePosition;
    private int danceTick = 0;
    private int partyTimer = 0;

    public DanceBehavior(Mob parrot, DanceConfig config, EcologyComponent component) {
        this.parrot = parrot;
        this.config = config;
        this.component = component;
    }

    /**
     * Starts dancing with the given style.
     */
    public void startDancing(DanceStyle style, BlockPos position) {
        this.isDancing = true;
        this.currentStyle = style;
        this.dancePosition = position;
        this.danceTick = 0;

        // Update component
        CompoundTag danceData = component.getHandleTag("dance");
        danceData.putBoolean("is_dancing", true);
        danceData.putString("dance_style", style.name());
        danceData.putInt("dance_pos_x", position.getX());
        danceData.putInt("dance_pos_y", position.getY());
        danceData.putInt("dance_pos_z", position.getZ());
        component.setHandleTag("dance", danceData);

        // Play start sound
        parrot.playSound(SoundEvents.PARROT_AMBIENT, 1.0f, 1.0f);
    }

    /**
     * Stops dancing.
     */
    public void stopDancing() {
        this.isDancing = false;
        this.currentStyle = null;
        this.danceTick = 0;
        this.partyTimer = 0;

        // Clear component
        CompoundTag danceData = component.getHandleTag("dance");
        danceData.putBoolean("is_dancing", false);
        danceData.remove("dance_style");
        component.setHandleTag("dance", danceData);
    }

    /**
     * Updates the dance behavior each tick.
     */
    public void tick() {
        if (!isDancing || currentStyle == null) {
            return;
        }

        danceTick++;

        // Perform dance moves
        performDanceMoves();

        // Spawn particles
        if (config.showParticles && danceTick % config.particleInterval == 0) {
            spawnDanceParticles();
        }

        // Check for party effect
        if (config.enablePartyEffect) {
            updatePartyEffect();
        }
    }

    /**
     * Performs the actual dance moves based on style.
     */
    private void performDanceMoves() {
        switch (currentStyle) {
            case BOUNCE -> performBounceDance();
            case SPIN -> performSpinDance();
            case WIGGLE -> performWiggleDance();
            case HEAD_BOB -> performHeadBobDance();
            case WING_FLAP -> performWingFlapDance();
            case PARTY -> performPartyDance();
            case RAVE -> performRaveDance();
            case DISCO -> performDiscoDance();
        }
    }

    private void performBounceDance() {
        int bounceInterval = 10;
        if (danceTick % bounceInterval == 0) {
            parrot.getJumpControl().jump();
        }

        // Slight rotation changes
        if (danceTick % 20 == 0) {
            parrot.setYRot(parrot.getYRot() + random.nextInt(90) - 45);
        }
    }

    private void performSpinDance() {
        int spinInterval = 5;
        if (danceTick % spinInterval == 0) {
            parrot.setYRot(parrot.getYRot() + 45);
        }

        // Occasional bounce
        if (danceTick % 40 == 0) {
            parrot.getJumpControl().jump();
        }
    }

    private void performWiggleDance() {
        // Rapid small rotations
        float wiggleAmount = 15;
        parrot.setYRot(parrot.getYRot() + (random.nextFloat() * wiggleAmount * 2) - wiggleAmount);

        // Move back and forth slightly
        if (danceTick % 10 == 0) {
            double wiggleSpeed = 0.1;
            double forwardOffset = Math.sin(danceTick * 0.5) * wiggleSpeed;
            parrot.setZza((float) forwardOffset);
        }
    }

    private void performHeadBobDance() {
        // Vertical movement simulation
        if (danceTick % 8 == 0) {
            parrot.getJumpControl().jump();
        }

        // Head movement (simulated by look angle)
        if (danceTick % 15 == 0) {
            parrot.setXRot(parrot.getXRot() + (random.nextFloat() * 20) - 10);
        }
    }

    private void performWingFlapDance() {
        // Frequent small jumps
        if (danceTick % 6 == 0) {
            parrot.getJumpControl().jump();
        }

        // Rotation changes
        if (danceTick % 12 == 0) {
            parrot.setYRot(parrot.getYRot() + 30);
        }
    }

    private void performPartyDance() {
        // Energetic bouncing
        if (danceTick % 5 == 0) {
            parrot.getJumpControl().jump();
        }

        // Random spins
        if (danceTick % 20 == 0) {
            parrot.setYRot(parrot.getYRot() + random.nextInt(180) - 90);
        }

        // Quick direction changes
        if (danceTick % 10 == 0) {
            parrot.setZza(random.nextFloat() * 0.2f - 0.1f);
        }
    }

    private void performRaveDance() {
        // Fast spinning
        parrot.setYRot(parrot.getYRot() + 30);

        // Strobe-like jumping
        if (danceTick % 8 == 0) {
            parrot.getJumpControl().jump();
        }

        // Direction bursts
        if (danceTick % 15 == 0) {
            parrot.setZza(random.nextFloat() * 0.3f - 0.15f);
        }
    }

    private void performDiscoDance() {
        // Smooth, rhythmic movements
        double discoBeat = Math.sin(danceTick * 0.3);

        // Bobbing motion
        if ((int) (discoBeat * 10) % 5 == 0) {
            parrot.getJumpControl().jump();
        }

        // Smooth rotations
        parrot.setYRot(parrot.getYRot() + (float) (discoBeat * 10));
    }

    /**
     * Spawns dance particles.
     */
    private void spawnDanceParticles() {
        if (parrot.level().isClientSide) {
            return;
        }

        double particleX = parrot.getX() + (random.nextDouble() - 0.5) * config.particleSpread;
        double particleY = parrot.getY() + 1.0 + random.nextDouble() * config.particleHeight;
        double particleZ = parrot.getZ() + (random.nextDouble() - 0.5) * config.particleSpread;

        parrot.level().addParticle(
            ParticleTypes.NOTE,
            particleX,
            particleY,
            particleZ,
            0,
            0.1,
            0
        );
    }

    /**
     * Updates and checks for party effect (other parrots joining).
     */
    private void updatePartyEffect() {
        if (!config.enablePartyEffect) {
            return;
        }

        partyTimer++;

        if (partyTimer >= config.partyCheckInterval) {
            partyTimer = 0;
            checkForPartyJoiners();
        }
    }

    /**
     * Looks for nearby parrots to join the dance.
     */
    private void checkForPartyJoiners() {
        if (dancePosition == null) {
            return;
        }

        List<Mob> nearbyParrots = parrot.level().getEntitiesOfClass(
            Mob.class,
            parrot.getBoundingBox().inflate(config.partyRadius)
        );

        for (Mob other : nearbyParrots) {
            if (other == parrot || !other.isAlive()) {
                continue;
            }

            // Check if it's a parrot
            if (!(other instanceof net.minecraft.world.entity.animal.Parrot)) {
                continue;
            }

            // Check if this parrot is already dancing
            EcologyComponent otherComponent = EcologyComponent.getOrCreate(other);
            if (otherComponent == null) {
                continue;
            }

            CompoundTag otherDanceData = otherComponent.getHandleTag("dance");
            if (otherDanceData.getBoolean("is_dancing")) {
                continue;
            }

            // Chance to join the party
            if (random.nextDouble() < config.partyJoinChance) {
                // Make the other parrot start dancing
                notifyParrotToDance(other, currentStyle);
            }
        }
    }

    private void notifyParrotToDance(Mob other, DanceStyle style) {
        // Store in the other parrot's component that it should start dancing
        EcologyComponent otherComponent = EcologyComponent.getOrCreate(other);
        if (otherComponent != null) {
            CompoundTag danceData = otherComponent.getHandleTag("dance");
            danceData.putBoolean("should_start_dancing", true);
            danceData.putString("invited_style", style.name());
            otherComponent.setHandleTag("dance", danceData);
        }
    }

    public boolean isDancing() {
        return isDancing;
    }

    public DanceStyle getCurrentStyle() {
        return currentStyle;
    }

    public BlockPos getDancePosition() {
        return dancePosition;
    }

    public int getDanceTick() {
        return danceTick;
    }

    /**
     * Dance styles based on music discs.
     */
    public enum DanceStyle {
        BOUNCE("13", "cat"),
        SPIN("blocks", "chirp"),
        WIGGLE("far", "mall"),
        HEAD_BOB("mellohi", "stal"),
        WING_FLAP("strad", "ward"),
        PARTY("11", "wait"),
        RAVE("otherside", "5", "pigstep", "relic"),
        DISCO("pigstep");

        private final String[] discIds;

        DanceStyle(String... discIds) {
            this.discIds = discIds;
        }

        /**
         * Gets the dance style for a music record item.
         */
        public static DanceStyle fromRecord(net.minecraft.world.item.ItemStack record) {
            if (record.isEmpty()) {
                return BOUNCE;
            }

            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(record.getItem()).toString();

            for (DanceStyle style : values()) {
                for (String discId : style.discIds) {
                    if (itemId.equals("minecraft:" + discId + "_disc") ||
                        itemId.endsWith(discId)) {
                        return style;
                    }
                }
            }

            return BOUNCE;
        }
    }

    /**
     * Configuration for dance behavior.
     */
    public static class DanceConfig {
        public boolean showParticles = true;
        public int particleInterval = 5;
        public double particleSpread = 1.0;
        public double particleHeight = 0.5;

        public boolean enablePartyEffect = true;
        public int partyCheckInterval = 40;
        public double partyRadius = 16.0;
        public double partyJoinChance = 0.3;
    }
}
