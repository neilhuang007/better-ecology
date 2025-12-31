package me.javavirtualenv.ecology.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * AI goal for ewe (mother sheep) to protect and care for lambs.
 * Ewes will stay close to their lambs and respond to their distress calls.
 */
public class EweProtectLambGoal extends Goal {
    private final PathfinderMob ewe;
    private final Level level;
    private final double followRange;
    private final double protectionRange;
    private final double speedModifier;
    private LivingEntity nearestLamb;
    private UUID lastLambUuid;
    private int protectionCooldown;

    // Targeting conditions for finding nearby lambs
    private static final TargetingConditions LAMB_TARGETING = TargetingConditions.forNonCombat()
            .range(32.0)
            .ignoreInvisibilityTesting()
            .selector(entity -> {
                if (!(entity instanceof net.minecraft.world.entity.animal.Sheep sheep)) {
                    return false;
                }
                return sheep.isBaby();
            });

    public EweProtectLambGoal(PathfinderMob ewe, double followRange, double protectionRange, double speedModifier) {
        this.ewe = ewe;
        this.level = ewe.level();
        this.followRange = followRange;
        this.protectionRange = protectionRange;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.protectionCooldown = 0;
    }

    @Override
    public boolean canUse() {
        if (protectionCooldown > 0) {
            protectionCooldown--;
            return false;
        }

        // Only adult sheep protect lambs
        if (ewe.isBaby()) {
            return false;
        }

        // Find nearest lamb
        nearestLamb = findNearestLamb();
        return nearestLamb != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (nearestLamb == null || !nearestLamb.isAlive()) {
            return false;
        }

        // Check if lamb is still in range
        double distance = ewe.distanceToSqr(nearestLamb);
        return distance < followRange * followRange;
    }

    @Override
    public void start() {
        if (nearestLamb != null) {
            lastLambUuid = nearestLamb.getUUID();
            // Move toward lamb if far away
            double distance = ewe.distanceTo(nearestLamb);
            if (distance > 8.0) {
                ewe.getNavigation().moveTo(nearestLamb, speedModifier);
            }
        }
    }

    @Override
    public void stop() {
        nearestLamb = null;
        ewe.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (nearestLamb == null || !nearestLamb.isAlive()) {
            return;
        }

        double distance = ewe.distanceTo(nearestLamb);

        // Look at the lamb
        ewe.getLookControl().setLookAt(nearestLamb, 30.0F, 30.0F);

        // Move toward lamb if it's too far
        if (distance > 8.0) {
            ewe.getNavigation().moveTo(nearestLamb, speedModifier);
        }

        // Check if lamb is being attacked or is in danger
        if (isLambInDanger(nearestLamb)) {
            protectLamb();
        }

        // Occasionally bleat to communicate with lamb
        if (ewe.getRandom().nextFloat() < 0.02) {
            bleat();
        }
    }

    private LivingEntity findNearestLamb() {
        // First check if we're tracking a specific lamb
        if (lastLambUuid != null) {
            Entity entity = ((ServerLevel) level).getEntity(lastLambUuid);
            if (entity instanceof LivingEntity lamb && lamb.isAlive() && lamb instanceof net.minecraft.world.entity.animal.Sheep) {
                double distance = ewe.distanceToSqr(lamb);
                if (distance < followRange * followRange) {
                    return lamb;
                }
            }
        }

        // Find nearest lamb
        List<net.minecraft.world.entity.animal.Sheep> sheep = level.getNearbyEntities(
                EntityType.SHEEP,
                ewe,
                ewe.getBoundingBox().inflate(followRange)
        );

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (net.minecraft.world.entity.animal.Sheep sheepEntity : sheep) {
            if (!sheepEntity.isBaby()) {
                continue; // Only look for lambs
            }

            double distance = ewe.distanceToSqr(sheepEntity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = sheepEntity;
            }
        }

        return nearest;
    }

    private boolean isLambInDanger(LivingEntity lamb) {
        // Check if lamb is fleeing
        if (lamb instanceof net.minecraft.world.entity.animal.Sheep sheep) {
            // Lamb is in danger if it's moving quickly (fleeing) or has low health
            double speed = lamb.getDeltaMovement().length();
            boolean isRunning = speed > 0.3;
            boolean isHurt = lamb.getHealth() < lamb.getMaxHealth() * 0.7;

            return isRunning || isHurt;
        }

        return false;
    }

    private void protectLamb() {
        // Move between threat and lamb
        // For now, just move toward the lamb quickly
        if (nearestLamb != null) {
            ewe.getNavigation().moveTo(nearestLamb, speedModifier * 1.5);

            // Play protective sound
            level.playSound(null, ewe.getX(), ewe.getY(), ewe.getZ(),
                          SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.2F, 0.8F);

            protectionCooldown = 100;
        }
    }

    private void bleat() {
        // Play communication bleat
        level.playSound(null, ewe.getX(), ewe.getY(), ewe.getZ(),
                      SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
