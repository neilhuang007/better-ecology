package me.javavirtualenv.behavior.core;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes baby animals call out when separated from nearby adults.
 *
 * <p>When a baby is too far from any adult of its species, it will:
 * <ul>
 *   <li>Emit distress sounds at regular intervals</li>
 *   <li>Display visual distress (particles)</li>
 *   <li>Move erratically while searching</li>
 * </ul>
 *
 * <p>Based on research showing universal mammalian distress vocalizations
 * during maternal separation, with immediate cessation upon reunion.
 */
public class SeparationDistressGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeparationDistressGoal.class);

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int DISTRESS_CALL_INTERVAL = 60;
    private static final int MAX_DISTRESS_DURATION = 600;

    private final Mob mob;
    private final Class<? extends Mob> parentType;
    private final double safeDistance;
    private final double searchRadius;
    private final double speedModifier;

    private int checkCooldown;
    private int distressTicks;
    private int callCooldown;
    @Nullable
    private Mob nearestAdult;

    /**
     * Creates a new SeparationDistressGoal.
     *
     * @param mob the baby mob that experiences distress
     * @param parentType the type of adult to look for
     * @param safeDistance distance within which baby feels safe
     * @param searchRadius radius to search for adults
     * @param speedModifier movement speed when seeking adults
     */
    public SeparationDistressGoal(
            Mob mob,
            Class<? extends Mob> parentType,
            double safeDistance,
            double searchRadius,
            double speedModifier) {
        this.mob = mob;
        this.parentType = parentType;
        this.safeDistance = safeDistance;
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
        this.checkCooldown = 0;
        this.callCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Creates a new SeparationDistressGoal with default parameters.
     *
     * @param mob the baby mob that experiences distress
     * @param parentType the type of adult to look for
     */
    public SeparationDistressGoal(Mob mob, Class<? extends Mob> parentType) {
        this(mob, parentType, 8.0, 32.0, 1.2);
    }

    @Override
    public boolean canUse() {
        // Only baby animals experience separation distress
        if (!this.mob.isBaby()) {
            return false;
        }

        if (this.checkCooldown > 0) {
            this.checkCooldown--;
            return false;
        }

        this.checkCooldown = reducedTickDelay(CHECK_INTERVAL_TICKS);

        // Check if there's any adult nearby
        Mob adult = findNearestAdult();
        if (adult != null) {
            double distance = this.mob.distanceTo(adult);
            if (distance <= this.safeDistance) {
                // Close enough to feel safe - no distress
                return false;
            }
            // Found adult but too far - store for later seeking
            this.nearestAdult = adult;
        } else {
            // No adults nearby at all - distress
            this.nearestAdult = null;
        }

        LOGGER.debug("{} (baby) is separated from adults, entering distress",
            this.mob.getName().getString());

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.mob.isBaby()) {
            return false;
        }

        if (this.distressTicks > MAX_DISTRESS_DURATION) {
            LOGGER.debug("{} gave up seeking adults after {} ticks",
                this.mob.getName().getString(), this.distressTicks);
            return false;
        }

        // Check if we've reunited with an adult
        Mob adult = findNearestAdult();
        if (adult != null) {
            double distance = this.mob.distanceTo(adult);
            if (distance <= this.safeDistance) {
                LOGGER.debug("{} reunited with adult {} at distance {}",
                    this.mob.getName().getString(),
                    adult.getName().getString(),
                    String.format("%.1f", distance));
                return false;
            }
            this.nearestAdult = adult;
        }

        return true;
    }

    @Override
    public void start() {
        this.distressTicks = 0;
        this.callCooldown = 0;
        LOGGER.debug("{} starting separation distress behavior", this.mob.getName().getString());
    }

    @Override
    public void stop() {
        LOGGER.debug("{} ended separation distress after {} ticks",
            this.mob.getName().getString(), this.distressTicks);
        this.distressTicks = 0;
        this.callCooldown = 0;
        this.nearestAdult = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.distressTicks++;

        // Make distress call periodically
        if (this.callCooldown <= 0) {
            makeDistressCall();
            this.callCooldown = DISTRESS_CALL_INTERVAL;
        }
        this.callCooldown--;

        // Move toward nearest adult if found
        if (this.nearestAdult != null && this.nearestAdult.isAlive()) {
            this.mob.getLookControl().setLookAt(this.nearestAdult, 10.0F, this.mob.getMaxHeadXRot());

            if (this.distressTicks % 10 == 0 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(this.nearestAdult, this.speedModifier);
            }
        } else {
            // Wander erratically looking for adults
            if (this.distressTicks % 40 == 0) {
                double offsetX = (this.mob.getRandom().nextDouble() - 0.5) * 10;
                double offsetZ = (this.mob.getRandom().nextDouble() - 0.5) * 10;
                this.mob.getNavigation().moveTo(
                    this.mob.getX() + offsetX,
                    this.mob.getY(),
                    this.mob.getZ() + offsetZ,
                    this.speedModifier
                );
            }
        }
    }

    /**
     * Makes a distress call sound and spawns particles.
     */
    private void makeDistressCall() {
        // Play distress sound (generic baby sound)
        this.mob.playSound(SoundEvents.GENERIC_HURT, 0.8F, 1.5F);

        // Spawn distress particles
        if (this.mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.HEART,
                this.mob.getX(),
                this.mob.getY() + this.mob.getBbHeight() + 0.5,
                this.mob.getZ(),
                1,
                0.2, 0.2, 0.2,
                0.0
            );
        }

        LOGGER.debug("{} made distress call (tick {})",
            this.mob.getName().getString(), this.distressTicks);
    }

    /**
     * Finds the nearest adult entity of the parent type.
     *
     * @return the nearest adult, or null if none found
     */
    @Nullable
    private Mob findNearestAdult() {
        AABB searchBox = this.mob.getBoundingBox().inflate(this.searchRadius);
        List<? extends Mob> nearbyAdults = this.mob.level()
            .getEntitiesOfClass(this.parentType, searchBox, this::isValidAdult);

        return nearbyAdults.stream()
            .min((a, b) -> Double.compare(this.mob.distanceToSqr(a), this.mob.distanceToSqr(b)))
            .orElse(null);
    }

    /**
     * Validates if an entity is a valid adult.
     *
     * @param entity the entity to check
     * @return true if this is a valid adult
     */
    private boolean isValidAdult(Mob entity) {
        if (entity == this.mob) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        return !entity.isBaby();
    }
}
