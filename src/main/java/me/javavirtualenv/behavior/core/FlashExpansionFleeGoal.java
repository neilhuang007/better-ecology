package me.javavirtualenv.behavior.core;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal implementing flash expansion fleeing behavior for schooling fish.
 * <p>
 * Scientific Basis: When one fish in a school is attacked, the entire school
 * rapidly scatters in a coordinated burst to disorient predators. This is known
 * as the "flash expansion" response, where the school suddenly explodes outward
 * from the threat point, creating visual confusion for the predator.
 * <p>
 * Implementation details:
 * <ul>
 *   <li>Detects when any school member takes damage within detection radius</li>
 *   <li>All fish burst swim at 2.0x normal speed radially away from threat</li>
 *   <li>Burst lasts 2-3 seconds (40-60 ticks)</li>
 *   <li>Fish gradually regroup after 10-15 seconds</li>
 *   <li>Bubble particles visualize the panic response</li>
 * </ul>
 */
public class FlashExpansionFleeGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlashExpansionFleeGoal.class);

    private static final int DETECTION_RADIUS = 8;
    private static final double BURST_SPEED_MULTIPLIER = 2.0;
    private static final int BURST_DURATION_MIN = 40;  // 2 seconds
    private static final int BURST_DURATION_MAX = 60;  // 3 seconds
    private static final int REGROUP_DELAY = 200;  // 10 seconds
    private static final int PARTICLE_INTERVAL = 5;  // Spawn particles every 5 ticks

    private final PathfinderMob fish;
    private final Class<? extends PathfinderMob> schoolType;

    @Nullable
    private Vec3 escapeDirection;
    private int burstTicksRemaining;
    private int regroupTicksRemaining;
    private int particleTimer;
    private boolean isInBurst;

    /**
     * Creates a new flash expansion flee goal.
     *
     * @param fish the fish that will participate in flash expansion
     * @param schoolType the class of fish that form this school
     */
    public FlashExpansionFleeGoal(PathfinderMob fish, Class<? extends PathfinderMob> schoolType) {
        this.fish = fish;
        this.schoolType = schoolType;
        this.burstTicksRemaining = 0;
        this.regroupTicksRemaining = 0;
        this.particleTimer = 0;
        this.isInBurst = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.regroupTicksRemaining > 0) {
            this.regroupTicksRemaining--;
            return false;
        }

        if (!detectThreatInSchool()) {
            return false;
        }

        Vec3 escapeDir = calculateEscapeDirection();
        if (escapeDir == null) {
            return false;
        }

        this.escapeDirection = escapeDir;
        this.burstTicksRemaining = calculateBurstDuration();
        this.isInBurst = true;

        LOGGER.debug("{} detected school threat, initiating flash expansion burst for {} ticks",
                fish.getName().getString(), burstTicksRemaining);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.isInBurst) {
            return false;
        }

        if (this.burstTicksRemaining <= 0) {
            return false;
        }

        return this.escapeDirection != null;
    }

    @Override
    public void start() {
        if (this.escapeDirection != null) {
            navigateInEscapeDirection();
            spawnBurstParticles();
        }
    }

    @Override
    public void tick() {
        if (!this.isInBurst || this.escapeDirection == null) {
            return;
        }

        this.burstTicksRemaining--;
        this.particleTimer++;

        if (this.particleTimer >= PARTICLE_INTERVAL) {
            spawnBurstParticles();
            this.particleTimer = 0;
        }

        navigateInEscapeDirection();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} flash expansion burst ended, entering regroup delay",
                fish.getName().getString());

        this.isInBurst = false;
        this.escapeDirection = null;
        this.regroupTicksRemaining = REGROUP_DELAY;
        this.particleTimer = 0;
        this.fish.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Detects if any fish in the school has recently taken damage.
     * Checks nearby fish of the same type for hurt state.
     *
     * @return true if a threat is detected in the school
     */
    private boolean detectThreatInSchool() {
        AABB searchBox = this.fish.getBoundingBox().inflate(DETECTION_RADIUS);
        List<? extends PathfinderMob> nearbySchoolmates = this.fish.level()
                .getEntitiesOfClass(this.schoolType, searchBox, this::isValidSchoolmate);

        for (PathfinderMob schoolmate : nearbySchoolmates) {
            if (schoolmate.hurtTime > 0) {
                LOGGER.debug("{} detected schoolmate {} was hurt (hurtTime: {})",
                        fish.getName().getString(),
                        schoolmate.getName().getString(),
                        schoolmate.hurtTime);
                return true;
            }
        }

        if (this.fish.hurtTime > 0) {
            LOGGER.debug("{} was hurt itself, triggering flash expansion",
                    fish.getName().getString());
            return true;
        }

        return false;
    }

    /**
     * Validates if an entity is a valid school mate.
     *
     * @param entity the entity to check
     * @return true if this is a valid school mate
     */
    private boolean isValidSchoolmate(PathfinderMob entity) {
        if (entity == this.fish) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        return true;
    }

    /**
     * Calculates the escape direction radially away from the threat point.
     * If this fish was hurt, uses the damage source position.
     * Otherwise, calculates average position of hurt school members and flees away.
     *
     * @return escape direction vector, or null if cannot determine
     */
    @Nullable
    private Vec3 calculateEscapeDirection() {
        Vec3 threatCenter = determineThreatCenter();
        if (threatCenter == null) {
            Vec3 randomDirection = generateRandomRadialDirection();
            LOGGER.debug("{} using random escape direction (no clear threat center)",
                    fish.getName().getString());
            return randomDirection;
        }

        Vec3 awayFromThreat = this.fish.position().subtract(threatCenter).normalize();

        if (awayFromThreat.length() < 0.1) {
            Vec3 randomDirection = generateRandomRadialDirection();
            LOGGER.debug("{} threat center too close, using random direction",
                    fish.getName().getString());
            return randomDirection;
        }

        LOGGER.debug("{} escaping away from threat at {}, direction: {}",
                fish.getName().getString(),
                threatCenter,
                awayFromThreat);

        return awayFromThreat;
    }

    /**
     * Determines the center point of the threat based on hurt school members.
     *
     * @return threat center position, or null if cannot determine
     */
    @Nullable
    private Vec3 determineThreatCenter() {
        AABB searchBox = this.fish.getBoundingBox().inflate(DETECTION_RADIUS);
        List<? extends PathfinderMob> nearbySchoolmates = this.fish.level()
                .getEntitiesOfClass(this.schoolType, searchBox, this::isValidSchoolmate);

        double sumX = 0, sumY = 0, sumZ = 0;
        int hurtCount = 0;

        for (PathfinderMob schoolmate : nearbySchoolmates) {
            if (schoolmate.hurtTime > 0) {
                sumX += schoolmate.getX();
                sumY += schoolmate.getY();
                sumZ += schoolmate.getZ();
                hurtCount++;
            }
        }

        if (this.fish.hurtTime > 0) {
            sumX += this.fish.getX();
            sumY += this.fish.getY();
            sumZ += this.fish.getZ();
            hurtCount++;
        }

        if (hurtCount == 0) {
            return null;
        }

        return new Vec3(sumX / hurtCount, sumY / hurtCount, sumZ / hurtCount);
    }

    /**
     * Generates a random radial direction for escape.
     *
     * @return random direction vector
     */
    private Vec3 generateRandomRadialDirection() {
        double angle = this.fish.getRandom().nextDouble() * Math.PI * 2;
        double verticalAngle = (this.fish.getRandom().nextDouble() - 0.5) * 0.5;

        double horizontalComponent = Math.cos(verticalAngle);
        return new Vec3(
                Math.cos(angle) * horizontalComponent,
                Math.sin(verticalAngle),
                Math.sin(angle) * horizontalComponent
        ).normalize();
    }

    /**
     * Calculates a random burst duration within the specified range.
     *
     * @return burst duration in ticks
     */
    private int calculateBurstDuration() {
        return BURST_DURATION_MIN +
               this.fish.getRandom().nextInt(BURST_DURATION_MAX - BURST_DURATION_MIN + 1);
    }

    /**
     * Navigates the fish in the escape direction at burst speed.
     */
    private void navigateInEscapeDirection() {
        if (this.escapeDirection == null) {
            return;
        }

        Vec3 currentPos = this.fish.position();
        Vec3 targetPos = currentPos.add(this.escapeDirection.scale(5.0));

        this.fish.getNavigation().moveTo(
                targetPos.x,
                targetPos.y,
                targetPos.z,
                BURST_SPEED_MULTIPLIER
        );
    }

    /**
     * Spawns bubble particles around the fish during burst swimming.
     * Creates visual effect of panicked, rapid escape.
     */
    private void spawnBurstParticles() {
        if (!(this.fish.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.BUBBLE,
                this.fish.getX(),
                this.fish.getY() + 0.3,
                this.fish.getZ(),
                3,
                0.2, 0.2, 0.2,
                0.1
        );

        if (this.fish.getRandom().nextFloat() < 0.3f) {
            serverLevel.sendParticles(
                    ParticleTypes.BUBBLE_POP,
                    this.fish.getX(),
                    this.fish.getY() + 0.2,
                    this.fish.getZ(),
                    1,
                    0.1, 0.1, 0.1,
                    0.05
            );
        }
    }
}
