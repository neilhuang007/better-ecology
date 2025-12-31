package me.javavirtualenv.behavior.parent;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Behavior for offspring to follow their mother.
 * Following distance increases as the offspring ages, promoting gradual independence.
 * Based on research into filial imprinting and following behaviors in precocial mammals.
 */
public class FollowMotherBehavior extends SteeringBehavior {

    private double baseFollowDistance;
    private double maxFollowDistance;
    private final double followSpeed;
    private final double slowingRadius;
    private final int adulthoodAge;

    private UUID motherUuid;
    private Vec3d lastMotherPosition;

    public FollowMotherBehavior(double baseFollowDistance, double maxFollowDistance,
                                double followSpeed, double slowingRadius, int adulthoodAge) {
        this.baseFollowDistance = baseFollowDistance;
        this.maxFollowDistance = maxFollowDistance;
        this.followSpeed = followSpeed;
        this.slowingRadius = slowingRadius;
        this.adulthoodAge = adulthoodAge;
        this.lastMotherPosition = new Vec3d();
    }

    public FollowMotherBehavior() {
        this(4.0, 12.0, 1.0, 2.0, -24000);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof AgeableMob offspring)) {
            return new Vec3d();
        }

        if (!offspring.isBaby()) {
            return new Vec3d();
        }

        Entity mother = findMother(offspring);
        if (mother == null || !mother.isAlive()) {
            return new Vec3d();
        }

        motherUuid = mother.getUUID();
        lastMotherPosition = new Vec3d(mother.getX(), mother.getY(), mother.getZ());

        Vec3d offspringPos = context.getPosition();
        double distanceToMother = offspringPos.distanceTo(lastMotherPosition);
        double currentFollowDistance = calculateFollowDistance(offspring);

        if (distanceToMother <= currentFollowDistance) {
            return new Vec3d();
        }

        return arrive(offspringPos, context.getVelocity(), lastMotherPosition,
                     followSpeed, slowingRadius);
    }

    private Entity findMother(AgeableMob offspring) {
        Level level = offspring.level();
        if (motherUuid != null) {
            // Find entity by UUID by searching nearby entities
            // Note: Level.getAllEntities() doesn't exist, search nearby instead
            for (Entity entity : level.getEntitiesOfClass(
                    Entity.class,
                    offspring.getBoundingBox().inflate(64.0))) {
                if (entity.getUUID().equals(motherUuid) && entity.isAlive() && entity instanceof AgeableMob) {
                    return entity;
                }
            }
        }

        // getParent() doesn't exist on AgeableMob, skip this check
        // Use findNearestAdultOfSameSpecies instead
        return findNearestAdultOfSameSpecies(offspring);
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

    private double calculateFollowDistance(AgeableMob offspring) {
        int babyAge = offspring.getAge();
        double ageProgress = Math.min(1.0, Math.abs(babyAge) / (double) Math.abs(adulthoodAge));
        return baseFollowDistance + (maxFollowDistance - baseFollowDistance) * ageProgress;
    }

    public void setMotherUuid(UUID motherUuid) {
        this.motherUuid = motherUuid;
    }

    public UUID getMotherUuid() {
        return motherUuid;
    }

    public Vec3d getLastMotherPosition() {
        return lastMotherPosition.copy();
    }

    public void setBaseFollowDistance(double baseFollowDistance) {
        this.baseFollowDistance = baseFollowDistance;
    }

    public double getBaseFollowDistance() {
        return baseFollowDistance;
    }

    public void setMaxFollowDistance(double maxFollowDistance) {
        this.maxFollowDistance = maxFollowDistance;
    }

    public double getMaxFollowDistance() {
        return maxFollowDistance;
    }
}
