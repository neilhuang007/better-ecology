package me.javavirtualenv.behavior.cow;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Nursing behavior for calves seeking their mothers.
 * <p>
 * Calves will:
 * - Follow their mother closely
 * - Seek mother when hungry
 * - Nurse from mother when close enough
 * - Stay near mother for protection
 * <p>
 * Based on research into calf behavior:
 * - Calves nurse 5-10 times per day
 * - Each nursing session lasts 5-15 minutes
 * - Calves stay within 5-10 meters of mother
 * - Strong mother-calf bond in first 3 months
 */
public class NursingBehavior extends SteeringBehavior {
    private double seekMotherRange;
    private double nursingDistance;
    private double hungerThreshold;
    private int nursingDuration;

    private UUID motherUuid;
    private Vec3d lastMotherPosition;
    private int ticksNursing;
    private boolean isNursing;

    public NursingBehavior(double seekMotherRange, double nursingDistance,
                          double hungerThreshold, int nursingDuration) {
        this.seekMotherRange = seekMotherRange;
        this.nursingDistance = nursingDistance;
        this.hungerThreshold = hungerThreshold;
        this.nursingDuration = nursingDuration;
        this.lastMotherPosition = new Vec3d();
        this.ticksNursing = 0;
        this.isNursing = false;
    }

    public NursingBehavior() {
        this(32.0, 2.0, 60.0, 200); // ~10 seconds nursing
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof AgeableMob calf)) {
            return new Vec3d();
        }

        if (!calf.isBaby() || !calf.isAlive()) {
            return new Vec3d();
        }

        // Find mother
        Entity mother = findMother(calf);
        if (mother == null || !mother.isAlive()) {
            return new Vec3d();
        }

        motherUuid = mother.getUUID();
        lastMotherPosition = new Vec3d(mother.getX(), mother.getY(), mother.getZ());

        Vec3d calfPos = context.getPosition();
        double distanceToMother = calfPos.distanceTo(lastMotherPosition);

        // Check if should nurse
        if (shouldNurse(calf) && distanceToMother <= nursingDistance) {
            return nurseBehavior(context, calf, mother);
        }

        // If nursing, continue nursing
        if (isNursing) {
            return continueNursing(context, calf, mother);
        }

        // Stay close to mother
        if (distanceToMother > seekMotherRange * 0.5) {
            return seekMother(context, calfPos, distanceToMother);
        }

        return new Vec3d();
    }

    private Entity findMother(AgeableMob calf) {
        Level level = calf.level();

        // If we have a mother UUID, try to find her
        if (motherUuid != null) {
            for (Entity entity : level.getEntitiesOfClass(Entity.class,
                    calf.getBoundingBox().inflate(64.0))) {
                if (entity.getUUID().equals(motherUuid) && entity.isAlive() &&
                    entity instanceof AgeableMob adult && !adult.isBaby()) {
                    return entity;
                }
            }
        }

        // Find nearest adult of same species
        return findNearestAdult(calf);
    }

    private Entity findNearestAdult(AgeableMob calf) {
        Level level = calf.level();
        Vec3d calfPos = new Vec3d(calf.getX(), calf.getY(), calf.getZ());
        Entity nearestAdult = null;
        double nearestDistance = Double.MAX_VALUE;

        List<AgeableMob> nearbyAdults = level.getEntitiesOfClass(AgeableMob.class,
                calf.getBoundingBox().inflate(seekMotherRange));

        for (AgeableMob adult : nearbyAdults) {
            if (adult.isBaby() || !adult.isAlive()) {
                continue;
            }

            if (!isSameSpecies(calf, adult)) {
                continue;
            }

            Vec3d adultPos = new Vec3d(adult.getX(), adult.getY(), adult.getZ());
            double distance = calfPos.distanceTo(adultPos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestAdult = adult;
            }
        }

        return nearestAdult;
    }

    private boolean shouldNurse(AgeableMob calf) {
        // Calves nurse when hungry
        // This would integrate with hunger system
        return calf.tickCount % 1200 == 0; // Approx every minute
    }

    private Vec3d nurseBehavior(BehaviorContext context, AgeableMob calf, Entity mother) {
        // Start nursing
        isNursing = true;
        ticksNursing = 0;

        // Face mother
        Vec3d calfPos = context.getPosition();
        Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());

        return arrive(calfPos, context.getVelocity(), motherPos, 0.3, 1.5);
    }

    private Vec3d continueNursing(BehaviorContext context, AgeableMob calf, Entity mother) {
        ticksNursing++;

        if (ticksNursing >= nursingDuration) {
            // Done nursing
            isNursing = false;
            ticksNursing = 0;

            // Notify that nursing occurred
            onNursingComplete(calf);
            return new Vec3d();
        }

        // Stay close to mother while nursing
        Vec3d calfPos = context.getPosition();
        Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());

        if (calfPos.distanceTo(motherPos) > nursingDistance * 1.5) {
            return arrive(calfPos, context.getVelocity(), motherPos, 0.4, 2.0);
        }

        return new Vec3d();
    }

    private void onNursingComplete(AgeableMob calf) {
        // Restore hunger (handled by hunger system)
        // Could play sound here
        calf.level().playSound(null, calf.blockPosition(),
                net.minecraft.sounds.SoundEvents.COW_MILK,
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 1.2F);
    }

    private Vec3d seekMother(BehaviorContext context, Vec3d calfPos, double distanceToMother) {
        // Calculate target position (stay close but don't crowd)
        double targetDistance = 4.0;
        Vec3d direction = lastMotherPosition.subtract(calfPos).normalize();
        Vec3d targetPos = lastMotherPosition.subtract(direction.mult(targetDistance));

        double seekSpeed = 0.35;
        return seek(calfPos, context.getVelocity(), targetPos, seekSpeed);
    }

    private boolean isSameSpecies(AgeableMob calf, AgeableMob adult) {
        return calf.getClass().equals(adult.getClass());
    }

    // Getters for external access

    public UUID getMotherUuid() {
        return motherUuid;
    }

    public void setMotherUuid(UUID motherUuid) {
        this.motherUuid = motherUuid;
    }

    public Vec3d getLastMotherPosition() {
        return lastMotherPosition.copy();
    }

    public boolean isNursing() {
        return isNursing;
    }

    public int getTicksNursing() {
        return ticksNursing;
    }
}
