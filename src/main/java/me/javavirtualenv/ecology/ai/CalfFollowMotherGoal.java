package me.javavirtualenv.ecology.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * AI goal for calves to follow and seek their mothers.
 *
 * Calves will:
 * - Follow their mother closely
 * - Seek mother when hungry (nursing behavior)
 * - Stay near mother for protection
 * - Emit distress calls if separated from mother
 *
 * Scientific basis:
 * - Calves form strong bond with mother in first 3 months
 * - They nurse 5-10 times per day initially
 * - Calves stay within 5-10 meters of mother
 * - Separation distress vocalizations when lost
 */
public class CalfFollowMotherGoal extends Goal {
    private final AgeableMob calf;
    private final Level level;
    private final double followRange;
    private final double speedModifier;
    private AgeableMob mother;
    private UUID motherUuid;
    private int followTicks;
    private int distressTimer;
    private boolean isNursing;
    private int nursingTimer;

    public CalfFollowMotherGoal(AgeableMob calf, double followRange, double speedModifier) {
        this.calf = calf;
        this.level = calf.level();
        this.followRange = followRange;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.followTicks = 0;
        this.distressTimer = 0;
        this.isNursing = false;
        this.nursingTimer = 0;
    }

    @Override
    public boolean canUse() {
        if (!calf.isBaby()) {
            return false;
        }

        mother = findMother();
        return mother != null && mother.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (mother == null || !mother.isAlive()) {
            return false;
        }

        double distance = calf.distanceToSqr(mother);

        // Continue following if mother is within range or we're nursing
        return distance < followRange * followRange * 1.5 || isNursing;
    }

    @Override
    public void start() {
        followTicks = 0;
        distressTimer = 0;

        // Store mother's UUID for persistence
        if (mother != null) {
            motherUuid = mother.getUUID();
            calf.getPersistentData().putUUID("better-ecology:mother_uuid", motherUuid);
        }
    }

    @Override
    public void stop() {
        if (isNursing) {
            onNursingComplete();
        }
        mother = null;
        isNursing = false;
        nursingTimer = 0;
        calf.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (mother == null) {
            return;
        }

        followTicks++;

        Vec3 calfPos = calf.position();
        Vec3 motherPos = mother.position();
        double distance = calf.distanceToSqr(mother);

        // Look at mother
        calf.getLookControl().setLookAt(mother);

        // Check if should nurse
        if (shouldNurse() && distance < 9.0) {
            attemptNursing();
            return;
        }

        // Nursing behavior
        if (isNursing) {
            tickNursing();
            return;
        }

        // Following behavior
        if (distance > followRange * followRange) {
            // Too far, move towards mother
            if (distressTimer == 0) {
                distressTimer = 100; // Start distress timer
            }

            moveToMother();

            // Emit distress call if very far
            if (distance > 64.0 && followTicks % 100 == 0) {
                emitDistressCall();
            }
        } else if (distance < 4.0) {
            // Too close, maintain distance
            maintainDistance();
        } else {
            // Good distance, stop moving
            if (followTicks % 20 == 0) {
                calf.getNavigation().stop();
            }
        }

        // Clear distress timer if close enough
        if (distance < followRange * followRange * 0.5) {
            distressTimer = 0;
        }
    }

    /**
     * Attempt to nurse from mother.
     */
    private void attemptNursing() {
        Vec3 calfPos = calf.position();
        Vec3 motherPos = mother.position();
        double distance = calf.distanceToSqr(mother);

        if (distance < 4.0) {
            // Close enough to nurse
            isNursing = true;
            nursingTimer = 0;

            // Face mother
            calf.getLookControl().setLookAt(mother);

            // Stop moving
            calf.getNavigation().stop();

            // Play nursing start sound
            level.playSound(null, calf.blockPosition(),
                    SoundEvents.COW_MILK,
                    SoundSource.NEUTRAL,
                    0.6F, 1.2F);
        } else {
            // Move closer to mother
            calf.getNavigation().moveTo(mother, speedModifier * 0.8);
        }
    }

    /**
     * Handle nursing behavior.
     */
    private void tickNursing() {
        nursingTimer++;

        Vec3 calfPos = calf.position();
        Vec3 motherPos = mother.position();
        double distance = calf.distanceToSqr(mother);

        // Stay close while nursing
        if (distance > 6.0) {
            // Mother moved away, stop nursing
            isNursing = false;
            nursingTimer = 0;
            return;
        }

        // Face mother
        calf.getLookControl().setLookAt(mother);

        // Nursing lasts 200-400 ticks (10-20 seconds)
        if (nursingTimer >= 200 && calf.getRandom().nextFloat() < 0.05f) {
            // Chance to finish nursing
            isNursing = false;
            onNursingComplete();
        }

        // Play nursing sound periodically
        if (nursingTimer % 60 == 0) {
            level.playSound(null, calf.blockPosition(),
                    SoundEvents.COW_MILK,
                    SoundSource.NEUTRAL,
                    0.5F, 1.3F);
        }
    }

    /**
     * Called when nursing is complete.
     */
    private void onNursingComplete() {
        isNursing = false;
        nursingTimer = 0;

        // Nursing completion sound
        level.playSound(null, calf.blockPosition(),
                SoundEvents.COW_MILK,
                SoundSource.NEUTRAL,
                0.7F, 1.0F);

        // Spawn milk particles
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    calf.getX(),
                    calf.getY() + calf.getEyeHeight(),
                    calf.getZ(),
                    1, 0, 0, 0, 0
            );
        }

        // Mark mother as recently nursed (for cooldown)
        if (mother != null) {
            mother.getPersistentData().putLong("better-ecology:last_nursed_tick", calf.tickCount);
        }
    }

    /**
     * Check if calf should nurse.
     */
    private boolean shouldNurse() {
        // Calves nurse when hungry
        // Check cooldown from mother
        if (mother != null) {
            long lastNursed = mother.getPersistentData().getLong("better-ecology:last_nursed_tick");
            long cooldown = 600; // 30 seconds

            if (calf.tickCount - lastNursed < cooldown) {
                return false;
            }
        }

        // Random chance to nurse (more frequent when young)
        float nurseChance = 0.005f; // ~1% chance per tick when cooldown is over

        return calf.getRandom().nextFloat() < nurseChance;
    }

    /**
     * Move towards mother.
     */
    private void moveToMother() {
        double currentSpeed = speedModifier;

        // Move faster if very far
        double distance = calf.distanceToSqr(mother);
        if (distance > 100.0) {
            currentSpeed *= 1.5;
        }

        calf.getNavigation().moveTo(mother, currentSpeed);
    }

    /**
     * Maintain optimal distance from mother.
     */
    private void maintainDistance() {
        Vec3 calfPos = calf.position();
        Vec3 motherPos = mother.position();

        // Calculate direction away from mother
        Vec3 awayDir = calfPos.subtract(motherPos).normalize();

        // Target position is 4-6 blocks from mother
        double targetDistance = 5.0;
        Vec3 targetPos = motherPos.add(awayDir.scale(targetDistance));

        // Move to target position
        calf.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, speedModifier * 0.5);
    }

    /**
     * Emit distress call when separated from mother.
     */
    private void emitDistressCall() {
        level.playSound(null, calf.blockPosition(),
                SoundEvents.COW_MILK,
                SoundSource.NEUTRAL,
                2.0F, 1.5F);

        // Alert nearby cows
        calf.getPersistentData().putBoolean("better-ecology:calf_distress", true);
        calf.getPersistentData().putLong("better-ecology:distress_tick", calf.tickCount);
    }

    /**
     * Find the calf's mother.
     */
    private AgeableMob findMother() {
        // First check if we have a stored mother UUID
        if (motherUuid != null) {
            for (Entity entity : level.getEntitiesOfClass(
                    Entity.class,
                    calf.getBoundingBox().inflate(64.0))) {
                if (entity.getUUID().equals(motherUuid) &&
                    entity.isAlive() &&
                    entity instanceof AgeableMob adult &&
                    !adult.isBaby() &&
                    isSameSpecies(calf, adult)) {
                    return adult;
                }
            }
        }

        // Check persistent data for mother UUID
        UUID storedMotherUuid = calf.getPersistentData().getUUID("better-ecology:mother_uuid");
        if (storedMotherUuid != null) {
            motherUuid = storedMotherUuid;
            // Try again with stored UUID
            return findMother();
        }

        // Find nearest adult female of same species
        return findNearestAdult();
    }

    /**
     * Find nearest adult of same species.
     */
    private AgeableMob findNearestAdult() {
        List<AgeableMob> nearbyAdults = level.getEntitiesOfClass(
                AgeableMob.class,
                calf.getBoundingBox().inflate(followRange),
                adult -> !adult.isBaby() &&
                         adult.isAlive() &&
                         isSameSpecies(calf, adult)
        );

        if (nearbyAdults.isEmpty()) {
            return null;
        }

        AgeableMob nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (AgeableMob adult : nearbyAdults) {
            double dist = calf.distanceToSqr(adult);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = adult;
            }
        }

        return nearest;
    }

    /**
     * Check if two mobs are same species.
     */
    private boolean isSameSpecies(AgeableMob mob1, AgeableMob mob2) {
        return mob1.getType().equals(mob2.getType());
    }

    // Getters for external access

    public AgeableMob getMother() {
        return mother;
    }

    public boolean isNursing() {
        return isNursing;
    }

    public void setMotherUuid(UUID uuid) {
        this.motherUuid = uuid;
        calf.getPersistentData().putUUID("better-ecology:mother_uuid", uuid);
    }
}
