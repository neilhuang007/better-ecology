package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

/**
 * AI Goal for horse bonding behavior.
 * Manages the bonding system between horses and players.
 */
public class BondingGoal extends Goal {
    private final AbstractHorse horse;
    private final HorseBondData bondData;
    private final BondingConfig config;
    private Player bondingPlayer;
    private int bondingTicks;
    private int nickerCooldownTicks;

    public BondingGoal(AbstractHorse horse, HorseBondData bondData, BondingConfig config) {
        this.horse = horse;
        this.bondData = bondData;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!horse.isAlive()) {
            return false;
        }

        if (nickerCooldownTicks > 0) {
            nickerCooldownTicks--;
        }

        // Find nearby player to bond with
        bondingPlayer = findNearbyPlayer();
        return bondingPlayer != null && bondingPlayer.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (bondingPlayer == null || !bondingPlayer.isAlive()) {
            return false;
        }

        double distance = horse.distanceToSqr(bondingPlayer);
        return distance <= config.bondingRange * config.bondingRange;
    }

    @Override
    public void start() {
        bondingTicks = 0;
    }

    @Override
    public void stop() {
        bondingPlayer = null;
        bondingTicks = 0;
    }

    @Override
    public void tick() {
        if (bondingPlayer == null) {
            return;
        }

        // Look at the player
        horse.getLookControl().setLookAt(bondingPlayer, 30.0f, 30.0f);

        bondingTicks++;

        // Give bond experience for being near
        if (bondingTicks % config.bondInterval == 0) {
            double distance = horse.distanceTo(bondingPlayer);
            if (distance < 3.0) {
                bondData.addBondExperience(bondingPlayer, 1);
            }
        }

        // Play nicker sound for bonded players
        if (nickerCooldownTicks <= 0 && bondData.isBonded(bondingPlayer)) {
            // Check if player is approaching
            double distance = horse.distanceTo(bondingPlayer);
            if (distance > 6.0 && distance < 10.0) {
                if (horse.getRandom().nextFloat() < config.nickerChance) {
                    playNickerSound();
                    nickerCooldownTicks = config.nickerCooldown;
                }
            }
        }

        // Show heart particles for strong bonds
        if (bondData.isStronglyBonded(bondingPlayer)) {
            if (bondingTicks % 60 == 0 && horse.getRandom().nextFloat() < 0.3) {
                spawnHeartParticles();
            }
        }
    }

    private Player findNearbyPlayer() {
        Level level = horse.level();
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;

        for (Player player : level.players()) {
            if (!player.isAlive()) {
                continue;
            }

            double distance = horse.distanceToSqr(player);
            if (distance > config.bondingRange * config.bondingRange) {
                continue;
            }

            // Prefer bonded players
            if (bondData.isBonded(player)) {
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            } else if (closestPlayer == null) {
                // Also consider unbonded players at close range
                if (distance < 16.0 && distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
        }

        return closestPlayer;
    }

    private void playNickerSound() {
        Level level = horse.level();
        if (level.isClientSide) {
            return;
        }

        net.minecraft.sounds.SoundEvent sound = getNickerSound();
        level.playSound(null, horse.blockPosition(), sound,
            net.minecraft.sounds.SoundSource.NEUTRAL,
            0.8f, 1.0f
        );
    }

    private net.minecraft.sounds.SoundEvent getNickerSound() {
        net.minecraft.world.entity.EntityType<?> type = horse.getType();

        if (type == net.minecraft.world.entity.EntityType.DONKEY) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        } else if (type == net.minecraft.world.entity.EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        } else {
            return net.minecraft.sounds.SoundEvents.HORSE_AMBIENT;
        }
    }

    private void spawnHeartParticles() {
        Level level = horse.level();
        if (level.isClientSide) {
            return;
        }

        double x = horse.getX();
        double y = horse.getY() + horse.getBbHeight() + 0.5;
        double z = horse.getZ();

        for (int i = 0; i < 3; i++) {
            double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.5;
            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.HEART,
                x + offsetX, y, z + offsetZ,
                1, 0, 0.1, 0, 0
            );
        }
    }

    public void recordRide(Player player) {
        bondData.recordRide(player);
    }

    public void recordInteraction(Player player) {
        bondData.addBondExperience(player, config.interactionBondGain);
    }

    public static class BondingConfig {
        public double bondingRange = 12.0;
        public int bondInterval = 100; // ticks between bond gains
        public int interactionBondGain = 5;
        public double nickerChance = 0.4;
        public int nickerCooldown = 200; // ticks

        public static BondingConfig createDefault() {
            return new BondingConfig();
        }
    }
}
