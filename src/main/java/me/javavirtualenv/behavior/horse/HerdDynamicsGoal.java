package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * AI Goal for horse herd dynamics behavior.
 * Implements wild horse band behavior with lead stallion protection.
 */
public class HerdDynamicsGoal extends Goal {
    private final AbstractHorse horse;
    private final HerdConfig config;
    private AbstractHorse leadStallion;
    private List<AbstractHorse> herdMembers;
    private int groupCheckInterval;
    private boolean isLeadStallion;
    private boolean isWild;

    public HerdDynamicsGoal(AbstractHorse horse, HerdConfig config) {
        this.horse = horse;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!horse.isAlive()) {
            return false;
        }

        // Only wild horses form herds
        if (!isWildHorse()) {
            return false;
        }

        // Update herd information periodically
        if (groupCheckInterval <= 0) {
            updateHerdInformation();
            groupCheckInterval = config.herdCheckInterval;
        } else {
            groupCheckInterval--;
        }

        // Only herd behavior if we have herd members nearby
        return herdMembers != null && !herdMembers.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        if (!horse.isAlive()) {
            return false;
        }

        // Check if herd is still nearby
        if (herdMembers == null || herdMembers.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        updateHerdInformation();
    }

    @Override
    public void stop() {
        leadStallion = null;
        herdMembers = null;
    }

    @Override
    public void tick() {
        if (isLeadStallion) {
            behaveAsLeadStallion();
        } else {
            behaveAsHerdMember();
        }
    }

    private boolean isWildHorse() {
        // Horse is wild if not tamed
        if (horse.isTamed()) {
            isWild = false;
            return false;
        }

        EntityType<?> type = horse.getType();
        isWild = type == EntityType.HORSE ||
                 type == EntityType.DONKEY ||
                 type == EntityType.MULE;

        return isWild;
    }

    private void updateHerdInformation() {
        Level level = horse.level();

        // Find all nearby horses of same type
        List<AbstractHorse> nearbyHorses = SpatialIndex.getNearbyEntitiesOfClass(
            horse,
            AbstractHorse.class,
            config.herdRadius
        );

        // Filter for same type and wild
        EntityType<?> myType = horse.getType();
        herdMembers = nearbyHorses.stream()
            .filter(h -> h.getType() == myType)
            .filter(h -> !h.isTamed())
            .filter(h -> h.isAlive())
            .filter(h -> h != horse)
            .toList();

        // Find the lead stallion (adult male with highest health)
        if (herdMembers.isEmpty()) {
            // This horse is alone
            isLeadStallion = !horse.isBaby();
            leadStallion = isLeadStallion ? horse : null;
        } else {
            // Include self in potential leaders
            List<AbstractHorse> allHorses = new java.util.ArrayList<>(herdMembers);
            allHorses.add(horse);

            // Find adult with highest health (simplified stallion detection)
            leadStallion = allHorses.stream()
                .filter(h -> !h.isBaby())
                .max((h1, h2) -> Double.compare(h1.getHealth(), h2.getHealth()))
                .orElse(null);

            isLeadStallion = (leadStallion == horse);
        }
    }

    private void behaveAsLeadStallion() {
        // Lead stallion protects the herd
        Level level = horse.level();

        // Check for threats
        Entity threat = findThreat();
        if (threat != null) {
            // Warn the herd
            warnHerdOfThreat(threat);

            // Move towards threat to protect herd
            double distance = horse.distanceTo(threat);
            if (distance > config.protectionRange) {
                horse.getNavigation().moveTo(threat, 1.2);
            }
        } else {
            // Lead the herd to food/water
            if (horse.getRandom().nextFloat() < config.leadMovementChance) {
                Vec3 targetPos = find grazingTarget();
                if (targetPos != null) {
                    horse.getNavigation().moveTo(
                        targetPos.x,
                        targetPos.y,
                        targetPos.z,
                        0.8
                    );
                }
            }
        }

        // Check herd cohesion
        checkHerdCohesion();
    }

    private void behaveAsHerdMember() {
        if (leadStallion == null || !leadStallion.isAlive()) {
            // No leader, find new one
            updateHerdInformation();
            return;
        }

        double distanceToLeader = horse.distanceTo(leadStallion);

        // Stay close to leader
        if (distanceToLeader > config.cohesionDistance) {
            horse.getNavigation().moveTo(leadStallion, 1.0);
        } else if (distanceToLeader < config.minDistanceToLeader) {
            // Too close, move away slightly
            Vec3 awayFromLeader = horse.position().subtract(
                leadStallion.position()
            ).normalize().scale(3.0);

            Vec3 targetPos = horse.position().add(awayFromLeader);
            horse.getNavigation().moveTo(
                targetPos.x,
                targetPos.y,
                targetPos.z,
                0.6
            );
        }

        // Look at leader occasionally
        if (horse.getRandom().nextFloat() < 0.1) {
            horse.getLookControl().setLookAt(leadStallion);
        }
    }

    private Entity findThreat() {
        Level level = horse.level();

        // Check for predators
        List<net.minecraft.world.entity.LivingEntity> threats = level.getNearbyEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            horse.getBoundingBox().inflate(config.threatDetectionRange),
            entity -> {
                if (entity == horse) {
                    return false;
                }

                EntityType<?> type = entity.getType();

                // Hostile mobs
                if (type == EntityType.WOLF ||
                    type == EntityType.POLAR_BEAR ||
                    type == EntityType.ZOMBIE ||
                    type == EntityType.DROWNED ||
                    type == EntityType.HUSK ||
                    type == EntityType.ZOMBIFIED_PIGLIN ||
                    type == EntityType.VINDICATOR ||
                    type == EntityType.EVOKER ||
                    type == EntityType.RAVAGER) {
                    return true;
                }

                // Players that are attacking
                if (type == EntityType.PLAYER) {
                    return horse.getLastHurtByMob() == entity;
                }

                return false;
            }
        );

        return threats.isEmpty() ? null : threats.get(0);
    }

    private void warnHerdOfThreat(Entity threat) {
        Level level = horse.level();

        // Play alarm call sound
        level.playSound(null, horse.blockPosition(),
            getAlarmCallSound(),
            net.minecraft.sounds.SoundSource.HOSTILE,
            1.2f, 1.1f
        );

        // Alert nearby herd members
        if (herdMembers != null) {
            for (AbstractHorse herdMember : herdMembers) {
                if (herdMember.isAlive() && herdMember.distanceTo(horse) < config.alarmCallRange) {
                    // Mark herd member as aware of threat
                    CompoundTag data = herdMember.getPersistentData();
                    data.putBoolean("better-ecology:is_fleeing", true);
                    data.putUUID("better-ecology:flee_from", threat.getUUID());

                    // Set flee cooldown
                    data.putLong("better-ecology:flee_until", level.getGameTime() + 200);
                }
            }
        }
    }

    private Vec3 findGrazingTarget() {
        // Simple random target for grazing
        // In a more sophisticated version, this could find actual grass blocks
        double angle = horse.getRandom().nextDouble() * Math.PI * 2;
        double distance = 8.0 + horse.getRandom().nextDouble() * 8.0;

        double x = horse.getX() + Math.cos(angle) * distance;
        double z = horse.getZ() + Math.sin(angle) * distance;
        double y = horse.getY();

        return new Vec3(x, y, z);
    }

    private void checkHerdCohesion() {
        if (herdMembers == null || herdMembers.isEmpty()) {
            return;
        }

        // Count how many herd members are nearby
        long nearbyCount = herdMembers.stream()
            .filter(h -> h.isAlive())
            .filter(h -> horse.distanceTo(h) < config.cohesionDistance)
            .count();

        // If herd is scattered, call them
        if (nearbyCount < herdMembers.size() * 0.5) {
            // Play cohesion call
            Level level = horse.level();
            level.playSound(null, horse.blockPosition(),
                getCohesionCallSound(),
                net.minecraft.sounds.SoundSource.NEUTRAL,
                1.0f, 1.0f
            );
        }
    }

    private net.minecraft.sounds.SoundEvent getAlarmCallSound() {
        EntityType<?> type = horse.getType();

        if (type == EntityType.DONKEY) {
            return net.minecraft.sounds.SoundEvents.DONKEY_CHEST;
        } else if (type == EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_CHEST;
        } else {
            return net.minecraft.sounds.SoundEvents.HORSE_GALLOP;
        }
    }

    private net.minecraft.sounds.SoundEvent getCohesionCallSound() {
        EntityType<?> type = horse.getType();

        if (type == EntityType.DONKEY) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        } else if (type == EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        } else {
            return net.minecraft.sounds.SoundEvents.HORSE_AMBIENT;
        }
    }

    public static class HerdConfig {
        public double herdRadius = 32.0;
        public double cohesionDistance = 12.0;
        public double minDistanceToLeader = 3.0;
        public int herdCheckInterval = 100; // ticks
        public double threatDetectionRange = 24.0;
        public double protectionRange = 16.0;
        public double alarmCallRange = 32.0;
        public double leadMovementChance = 0.02;

        public static HerdConfig createDefault() {
            return new HerdConfig();
        }
    }
}
