package me.javavirtualenv.behavior.parent;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Separation distress behavior - offspring vocalize when separated from mother.
 * Based on research into separation distress calls in mammalian young.
 * Mother responds by calling back and moving toward the distressed offspring.
 */
public class SeparationDistressBehavior extends SteeringBehavior {

    private double distressThreshold;
    private double callCooldown;
    private final double responseSpeed;
    private final boolean isMother;

    private UUID bondedEntityUuid;
    private long lastCallTime;
    private long lastResponseTime;
    private Vec3d lastHeardCallPosition;
    private boolean isDistressed;

    // Per-instance cache for distress state (no longer static/shared)
    private final Map<UUID, Boolean> distressCache = new ConcurrentHashMap<>();

    public SeparationDistressBehavior(double distressThreshold, double callCooldown,
                                     double responseSpeed, boolean isMother) {
        this.distressThreshold = distressThreshold;
        this.callCooldown = callCooldown;
        this.responseSpeed = responseSpeed;
        this.isMother = isMother;
        this.lastHeardCallPosition = new Vec3d();
    }

    public SeparationDistressBehavior(boolean isMother) {
        this(16.0, 60.0, 1.2, isMother);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof AgeableMob ageable)) {
            return new Vec3d();
        }

        if (!ageable.isAlive()) {
            return new Vec3d();
        }

        if (isMother) {
            return calculateMotherResponse(context, ageable);
        } else {
            return calculateOffspringDistress(context, ageable);
        }
    }

    private Vec3d calculateMotherResponse(BehaviorContext context, AgeableMob mother) {
        if (!mother.isAlive()) {
            return new Vec3d();
        }

        Entity nearestDistressedBaby = findNearestDistressedBaby(mother);
        if (nearestDistressedBaby == null) {
            return new Vec3d();
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResponseTime < callCooldown * 500) {
            return new Vec3d();
        }

        lastResponseTime = currentTime;

        motherPlayResponseSound(mother);

        Vec3d motherPos = context.getPosition();
        Vec3d babyPos = new Vec3d(nearestDistressedBaby.getX(),
                                  nearestDistressedBaby.getY(),
                                  nearestDistressedBaby.getZ());

        return seek(motherPos, context.getVelocity(), babyPos, responseSpeed);
    }

    private Vec3d calculateOffspringDistress(BehaviorContext context, AgeableMob offspring) {
        UUID offspringUuid = offspring.getUUID();

        if (!offspring.isBaby()) {
            isDistressed = false;
            distressCache.put(offspringUuid, false);
            return new Vec3d();
        }

        Entity mother = findMother(offspring);
        if (mother == null || !mother.isAlive()) {
            isDistressed = false;
            distressCache.put(offspringUuid, false);
            return new Vec3d();
        }

        bondedEntityUuid = mother.getUUID();

        Vec3d offspringPos = context.getPosition();
        Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());
        double distanceToMother = offspringPos.distanceTo(motherPos);

        if (distanceToMother > distressThreshold) {
            isDistressed = true;
            distressCache.put(offspringUuid, true);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastCallTime >= callCooldown * 1000) {
                lastCallTime = currentTime;
                offspringPlayDistressSound(offspring);
            }

            return seek(offspringPos, context.getVelocity(), motherPos, responseSpeed);
        } else {
            // Clear distress flag when back with mother
            if (isDistressed) {
                isDistressed = false;
                distressCache.put(offspringUuid, false);
            }
            return new Vec3d();
        }
    }

    private Entity findMother(AgeableMob offspring) {
        Level level = offspring.level();
        if (bondedEntityUuid != null) {
            // Find entity by UUID by searching nearby entities
            // Note: Level.getAllEntities() doesn't exist, search nearby instead
            for (Entity entity : level.getEntitiesOfClass(
                    Entity.class,
                    offspring.getBoundingBox().inflate(64.0))) {
                if (entity.getUUID().equals(bondedEntityUuid) && entity.isAlive()) {
                    return entity;
                }
            }
        }

        // getParent() doesn't exist on AgeableMob, skip this check
        return findNearestAdultOfSameSpecies(offspring);
    }

    private Entity findNearestDistressedBaby(AgeableMob mother) {
        Level level = mother.level();
        Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());

        for (Entity entity : level.getEntitiesOfClass(mother.getClass(),
                mother.getBoundingBox().inflate(distressThreshold * 1.5))) {
            if (entity instanceof AgeableMob baby && baby.isBaby() && baby.isAlive()) {
                if (isBabyDistressed(baby)) {
                    Vec3d babyPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                    if (motherPos.distanceTo(babyPos) <= distressThreshold * 1.5) {
                        return entity;
                    }
                }
            }
        }

        return null;
    }

    private Entity findNearestAdultOfSameSpecies(AgeableMob offspring) {
        Level level = offspring.level();
        Vec3d offspringPos = new Vec3d(offspring.getX(), offspring.getY(), offspring.getZ());
        Entity nearestAdult = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : level.getEntitiesOfClass(offspring.getClass(),
                offspring.getBoundingBox().inflate(32.0))) {
            if (entity instanceof AgeableMob adult && !adult.isBaby() && adult.isAlive()) {
                Vec3d adultPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                double distance = offspringPos.distanceTo(adultPos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestAdult = entity;
                }
            }
        }

        return nearestAdult;
    }

    private boolean isBabyDistressed(AgeableMob baby) {
        // Use cache instead of NBT data
        return distressCache.getOrDefault(baby.getUUID(), false);
    }

    private void offspringPlayDistressSound(AgeableMob offspring) {
        // Use cache instead of NBT data
        distressCache.put(offspring.getUUID(), true);

        String entityType = offspring.getType().toString().toLowerCase();
        SoundEvent distressSound = getDistressSoundForType(entityType);

        if (distressSound != null) {
            offspring.playSound(distressSound, 1.0F, 1.5F);
        }
    }

    private void motherPlayResponseSound(AgeableMob mother) {
        String entityType = mother.getType().toString().toLowerCase();
        SoundEvent responseSound = getResponseSoundForType(entityType);

        if (responseSound != null) {
            mother.playSound(responseSound, 1.0F, 0.8F);
        }
    }

    private SoundEvent getDistressSoundForType(String entityType) {
        if (entityType.contains("cow")) {
            return net.minecraft.sounds.SoundEvents.COW_AMBIENT;
        } else if (entityType.contains("sheep")) {
            return net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT;
        } else if (entityType.contains("pig")) {
            return net.minecraft.sounds.SoundEvents.PIG_AMBIENT;
        } else if (entityType.contains("chicken")) {
            return net.minecraft.sounds.SoundEvents.CHICKEN_AMBIENT;
        } else if (entityType.contains("wolf")) {
            return net.minecraft.sounds.SoundEvents.WOLF_WHINE;
        } else if (entityType.contains("cat")) {
            return net.minecraft.sounds.SoundEvents.CAT_BEG_FOR_FOOD;
        } else if (entityType.contains("rabbit")) {
            return net.minecraft.sounds.SoundEvents.RABBIT_AMBIENT;
        }
        return null;
    }

    private SoundEvent getResponseSoundForType(String entityType) {
        if (entityType.contains("cow")) {
            return net.minecraft.sounds.SoundEvents.COW_AMBIENT;
        } else if (entityType.contains("sheep")) {
            return net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT;
        } else if (entityType.contains("pig")) {
            return net.minecraft.sounds.SoundEvents.PIG_AMBIENT;
        } else if (entityType.contains("chicken")) {
            return net.minecraft.sounds.SoundEvents.CHICKEN_AMBIENT;
        } else if (entityType.contains("wolf")) {
            return net.minecraft.sounds.SoundEvents.WOLF_AMBIENT;
        } else if (entityType.contains("cat")) {
            return net.minecraft.sounds.SoundEvents.CAT_AMBIENT;
        } else if (entityType.contains("rabbit")) {
            return net.minecraft.sounds.SoundEvents.RABBIT_AMBIENT;
        }
        return null;
    }

    public void setBondedEntityUuid(UUID bondedEntityUuid) {
        this.bondedEntityUuid = bondedEntityUuid;
    }

    public UUID getBondedEntityUuid() {
        return bondedEntityUuid;
    }

    public boolean isDistressed() {
        return isDistressed;
    }

    public void setDistressed(boolean distressed) {
        this.isDistressed = distressed;
    }

    public void setDistressThreshold(double distressThreshold) {
        this.distressThreshold = distressThreshold;
    }

    public double getDistressThreshold() {
        return distressThreshold;
    }

    public void setCallCooldown(double callCooldown) {
        this.callCooldown = callCooldown;
    }

    public double getCallCooldown() {
        return callCooldown;
    }
}
