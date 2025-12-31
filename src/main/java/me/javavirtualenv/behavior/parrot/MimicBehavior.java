package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Mimic behavior for parrots.
 * Parrots mimic hostile mob sounds as a warning system for players.
 * Each parrot has different mimic accuracy and preferences.
 */
public class MimicBehavior {
    private final Mob parrot;
    private final MimicConfig config;
    private final EcologyComponent component;
    private final Random random = new Random();

    private int mimicCooldown = 0;
    private MimicMemory lastMimic;
    private double mimicAccuracy;

    public MimicBehavior(Mob parrot, MimicConfig config, EcologyComponent component) {
        this.parrot = parrot;
        this.config = config;
        this.component = component;
        this.mimicAccuracy = config.baseMimicAccuracy + (random.nextDouble() * config.accuracyVariation);
    }

    /**
     * Attempts to mimic a hostile mob sound.
     * @return true if a mimic was performed
     */
    public boolean tryMimic() {
        if (mimicCooldown > 0) {
            mimicCooldown--;
            return false;
        }

        if (random.nextDouble() > config.mimicChance) {
            return false;
        }

        MimicType mimic = selectMimic();
        if (mimic == null) {
            return false;
        }

        performMimic(mimic);
        mimicCooldown = config.mimicCooldownTicks;
        return true;
    }

    /**
     * Mimics as a warning when a hostile mob is nearby.
     * @param hostileDistance distance to the hostile mob
     * @return true if warning mimic was performed
     */
    public boolean tryWarningMimic(double hostileDistance) {
        if (hostileDistance > config.warningRange) {
            return false;
        }

        // Higher accuracy for warnings
        double warningBonus = (1.0 - hostileDistance / config.warningRange) * config.warningAccuracyBonus;

        if (random.nextDouble() > config.warningChance) {
            return false;
        }

        MimicType mimic = selectDangerousMimic();
        if (mimic == null) {
            return false;
        }

        // Apply warning bonus to accuracy
        double originalAccuracy = mimicAccuracy;
        mimicAccuracy = Math.min(1.0, mimicAccuracy + warningBonus);

        performMimic(mimic);

        // Restore original accuracy
        mimicAccuracy = originalAccuracy;

        mimicCooldown = config.warningCooldownTicks;
        return true;
    }

    private MimicType selectMimic() {
        List<MimicType> availableMimics = new ArrayList<>();

        // Weight selection based on mob frequency in area
        for (MimicType mimic : MimicType.values()) {
            if (config.mimicWeights.containsKey(mimic)) {
                double weight = config.mimicWeights.get(mimic);
                if (random.nextDouble() < weight) {
                    availableMimics.add(mimic);
                }
            }
        }

        if (availableMimics.isEmpty()) {
            return MimicType.values()[random.nextInt(MimicType.values().length)];
        }

        return availableMimics.get(random.nextInt(availableMimics.size()));
    }

    private MimicType selectDangerousMimic() {
        // Prefer dangerous mobs for warnings
        List<MimicType> dangerousMimics = List.of(
            MimicType.CREEPER,
            MimicType.ENDERMAN,
            MimicType.WITCH,
            MimicType.RAVAGER
        );

        return dangerousMimics.get(random.nextInt(dangerousMimics.size()));
    }

    private void performMimic(MimicType mimic) {
        // Check if mimic should be accurate or imprecise
        boolean isAccurate = random.nextDouble() < mimicAccuracy;

        SoundEvent sound = isAccurate ? mimic.accurateSound : mimic.impreciseSound;

        // Play the mimic sound
        parrot.level().playSound(
            null,
            parrot.getX(),
            parrot.getY(),
            parrot.getZ(),
            sound,
            parrot.getSoundSource(),
            config.mimicVolume,
            config.mimicPitch
        );

        // Store in memory
        lastMimic = new MimicMemory(mimic, isAccurate, parrot.level().getGameTime());

        // Notify nearby players
        notifyNearbyPlayers(mimic, isAccurate);

        // Update component data
        updateMimicData(mimic, isAccurate);
    }

    private void notifyNearbyPlayers(MimicType mimic, boolean isAccurate) {
        List<Player> nearbyPlayers = parrot.level().getEntitiesOfClass(
            Player.class,
            parrot.getBoundingBox().inflate(config.notificationRadius)
        );

        for (Player player : nearbyPlayers) {
            // Could add particle effects or other notifications here
            if (config.showMimicParticles) {
                spawnMimicParticles(isAccurate);
            }
        }
    }

    private void spawnMimicParticles(boolean isAccurate) {
        // Spawn note particles for successful mimics
        if (!parrot.level().isClientSide) {
            return;
        }

        // Particle logic handled on client side
    }

    private void updateMimicData(MimicType mimic, boolean isAccurate) {
        CompoundTag mimicData = component.getHandleTag("mimic");
        mimicData.putString("last_mimic", mimic.name());
        mimicData.putBoolean("was_accurate", isAccurate);
        mimicData.putLong("mimic_time", parrot.level().getGameTime());
        mimicData.putDouble("accuracy", mimicAccuracy);
        component.setHandleTag("mimic", mimicData);
    }

    public MimicMemory getLastMimic() {
        return lastMimic;
    }

    public double getMimicAccuracy() {
        return mimicAccuracy;
    }

    public void setMimicAccuracy(double accuracy) {
        this.mimicAccuracy = Math.max(0.0, Math.min(1.0, accuracy));
    }

    /**
     * Memory of a performed mimic.
     */
    public record MimicMemory(MimicType type, boolean wasAccurate, long timestamp) {
        public boolean isRecent(long currentTime, long maxAge) {
            return currentTime - timestamp < maxAge;
        }
    }

    /**
     * Types of mimics parrots can perform.
     */
    public enum MimicType {
        CREEPER(SoundEvents.PARROT_IMITATE_CREEPER, null),
        ZOMBIE(SoundEvents.PARROT_IMITATE_ZOMBIE, null),
        SKELETON(SoundEvents.PARROT_IMITATE_SKELETON, null),
        SPIDER(SoundEvents.PARROT_IMITATE_SPIDER, null),
        ENDERMAN(SoundEvents.PARROT_IMITATE_ENDERMAN, null),
        WITCH(SoundEvents.PARROT_IMITATE_WITCH, null),
        WITHER_SKELETON(SoundEvents.PARROT_IMITATE_WITHER_SKELETON, null),
        RAVAGER(SoundEvents.PARROT_IMITATE_RAVAGER, null),
        PHANTOM(SoundEvents.PARROT_IMITATE_PHANTOM, null),
        HOGLIN(SoundEvents.PARROT_IMITATE_HOGLIN, null),
        PIGLIN(SoundEvents.PARROT_IMITATE_PIGLIN, null),
        VINDICATOR(SoundEvents.PARROT_IMITATE_VINDICATOR, null);

        private final SoundEvent accurateSound;
        private final SoundEvent impreciseSound;

        MimicType(SoundEvent accurateSound, SoundEvent impreciseSound) {
            this.accurateSound = accurateSound;
            this.impreciseSound = impreciseSound != null ? impreciseSound : accurateSound;
        }

        public SoundEvent getAccurateSound() {
            return accurateSound;
        }

        public SoundEvent getImpreciseSound() {
            return impreciseSound;
        }
    }

    /**
     * Configuration for mimic behavior.
     */
    public static class MimicConfig {
        public double baseMimicAccuracy = 0.75;
        public double accuracyVariation = 0.2;
        public double mimicChance = 0.05;
        public int mimicCooldownTicks = 200;

        public double warningChance = 0.8;
        public double warningRange = 32.0;
        public double warningAccuracyBonus = 0.15;
        public int warningCooldownTicks = 100;

        public double mimicVolume = 0.7f;
        public float mimicPitch = 1.0f;
        public double notificationRadius = 16.0;
        public boolean showMimicParticles = true;

        public java.util.Map<MimicType, Double> mimicWeights = new java.util.HashMap<>();

        public MimicConfig() {
            // Default mimic weights
            mimicWeights.put(MimicType.ZOMBIE, 0.3);
            mimicWeights.put(MimicType.SKELETON, 0.25);
            mimicWeights.put(MimicType.SPIDER, 0.2);
            mimicWeights.put(MimicType.CREEPER, 0.15);
            mimicWeights.put(MimicType.ENDERMAN, 0.1);
        }
    }
}
