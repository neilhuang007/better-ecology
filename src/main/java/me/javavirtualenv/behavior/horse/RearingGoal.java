package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal for horse rearing behavior.
 * Horses rear up when frightened, angry, or excited.
 */
public class RearingGoal extends Goal {
    private static final TargetingConditions PREDATOR_TARGETING = TargetingConditions.forCombat().range(16.0);

    private final AbstractHorse horse;
    private final RearingConfig config;
    private int rearDurationTicks;
    private int rearCooldownTicks;

    public RearingGoal(AbstractHorse horse, RearingConfig config) {
        this.horse = horse;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (rearCooldownTicks > 0) {
            rearCooldownTicks--;
            return false;
        }

        if (!horse.isAlive()) {
            return false;
        }

        // Cannot rear while being ridden
        if (horse.isVehicle()) {
            return false;
        }

        // Check for rearing triggers
        return shouldRear();
    }

    @Override
    public boolean canContinueToUse() {
        if (rearDurationTicks <= 0) {
            return false;
        }

        if (horse.isVehicle()) {
            return false;
        }

        // Stop rearing if too panicked (should flee instead)
        if (horse.getPersistentData().getBoolean("better-ecology:is_fleeing")) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        rearDurationTicks = config.baseRearDuration;

        // Stop movement and make rear
        horse.getNavigation().stop();
        horse.makeRear();

        // Play rear sound
        Level level = horse.level();
        level.playSound(null, horse.blockPosition(),
            getRearSound(),
            net.minecraft.sounds.SoundSource.HOSTILE,
            1.0f, 1.0f
        );

        // Create dust particles
        if (!level.isClientSide) {
            for (int i = 0; i < 8; i++) {
                double x = horse.getX() + (level.random.nextDouble() - 0.5) * horse.getBbWidth();
                double y = horse.getY();
                double z = horse.getZ() + (level.random.nextDouble() - 0.5) * horse.getBbWidth();
                level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.DUST,
                    x, y, z,
                    1, 0, 0, 0, 0.02
                );
            }
        }
    }

    @Override
    public void stop() {
        rearDurationTicks = 0;
        rearCooldownTicks = config.rearCooldown;
    }

    @Override
    public void tick() {
        rearDurationTicks--;

        // Occasional extra rear animation during long rears
        if (rearDurationTicks % 20 == 0 && rearDurationTicks > 0) {
            horse.makeRear();
        }
    }

    private boolean shouldRear() {
        // Trigger 1: Predator nearby
        if (hasNearbyPredator()) {
            return horse.getRandom().nextFloat() < config.predatorRearChance;
        }

        // Trigger 2: Recent damage
        if (horse.getLastHurtByMob() != null &&
            horse.tickCount - horse.getLastHurtByMobTimestamp() < 40) {
            return horse.getRandom().nextFloat() < config.hurtRearChance;
        }

        // Trigger 3: Low health (fear response)
        double healthPercent = horse.getHealth() / horse.getMaxHealth();
        if (healthPercent < config.lowHealthThreshold) {
            return horse.getRandom().nextFloat() < config.lowHealthRearChance;
        }

        // Trigger 4: Random excitement (play behavior)
        if (horse.getRandom().nextFloat() < config.randomRearChance) {
            return true;
        }

        return false;
    }

    private boolean hasNearbyPredator() {
        Level level = horse.level();

        // Check for wolf packs
        List<net.minecraft.world.entity.animal.Wolf> wolves = level.getNearbyEntitiesOfClass(
            net.minecraft.world.entity.animal.Wolf.class,
            horse.getBoundingBox().inflate(16.0),
            wolf -> wolf.isAlive() && wolf.isAggressive()
        );

        if (!wolves.isEmpty()) {
            // More likely to rear if multiple wolves
            return wolves.size() >= 2;
        }

        // Check for zombies
        List<LivingEntity> zombies = level.getNearbyEntitiesOfClass(
            LivingEntity.class,
            horse.getBoundingBox().inflate(12.0),
            entity -> entity.getType() == EntityType.ZOMBIE ||
                     entity.getType() == EntityType.DROWNED ||
                     entity.getType() == EntityType.HUSK ||
                     entity.getType() == EntityType.ZOMBIFIED_PIGLIN
        );

        return !zombies.isEmpty();
    }

    private net.minecraft.sounds.SoundEvent getRearSound() {
        EntityType<?> type = horse.getType();

        if (type == EntityType.DONKEY) {
            return net.minecraft.sounds.SoundEvents.DONKEY_CHEST;
        } else if (type == EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_CHEST;
        } else if (type == EntityType.SKELETON_HORSE) {
            return net.minecraft.sounds.SoundEvents.SKELETON_HORSE_AMBIENT;
        } else if (type == EntityType.ZOMBIE_HORSE) {
            return net.minecraft.sounds.SoundEvents.ZOMBIE_HORSE_AMBIENT;
        } else {
            return net.minecraft.sounds.SoundEvents.HORSE_GALLOP;
        }
    }

    public static class RearingConfig {
        public int baseRearDuration = 30; // ticks
        public int rearCooldown = 200; // ticks

        // Trigger chances
        public double predatorRearChance = 0.7;
        public double hurtRearChance = 0.5;
        public double lowHealthThreshold = 0.3; // 30% health
        public double lowHealthRearChance = 0.4;
        public double randomRearChance = 0.005; // 0.5% per tick

        public static RearingConfig createDefault() {
            return new RearingConfig();
        }

        public static RearingConfig createSkittishConfig() {
            RearingConfig config = new RearingConfig();
            config.predatorRearChance = 0.9;
            config.hurtRearChance = 0.8;
            config.lowHealthRearChance = 0.7;
            config.baseRearDuration = 40;
            return config;
        }

        public static RearingConfig createCalmConfig() {
            RearingConfig config = new RearingConfig();
            config.predatorRearChance = 0.4;
            config.hurtRearChance = 0.3;
            config.lowHealthRearChance = 0.3;
            config.baseRearDuration = 20;
            return config;
        }
    }
}
