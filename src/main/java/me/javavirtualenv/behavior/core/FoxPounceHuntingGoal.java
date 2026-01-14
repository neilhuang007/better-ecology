package me.javavirtualenv.behavior.core;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal that makes foxes perform their signature high-arc pounce attack on small prey.
 *
 * <p>Scientific Basis:
 * Foxes are known for their distinctive hunting technique where they leap 1-2 meters high
 * in a parabolic arc to land on small prey like mice and rabbits. This technique has a
 * ~75-80% success rate and is the signature fox hunting behavior.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when fox is hungry and prey is within 3-8 blocks</li>
 *   <li>Fox performs a dramatic leap in a high arc toward prey</li>
 *   <li>Leap has height of 1.5 blocks and 2x normal speed</li>
 *   <li>On landing near prey, 80% chance of successful attack</li>
 *   <li>Visual effect: snow/cloud particles during jump</li>
 *   <li>Targets: Chickens, Rabbits (small prey)</li>
 * </ul>
 *
 * <p>Priority: {@link AnimalThresholds#PRIORITY_HUNT} (4) - Same as normal hunting
 */
public class FoxPounceHuntingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FoxPounceHuntingGoal.class);

    private static final double MIN_POUNCE_DISTANCE = 3.0;
    private static final double MAX_POUNCE_DISTANCE = 8.0;
    private static final double POUNCE_HEIGHT = 1.5;
    private static final double POUNCE_SPEED_MULTIPLIER = 2.0;
    private static final double SUCCESS_RATE = 0.80;
    private static final int POUNCE_DURATION_TICKS = 15;
    private static final int COOLDOWN_AFTER_POUNCE = 40;
    private static final int SEARCH_INTERVAL_TICKS = 10;
    private static final float HUNGER_RESTORE_ON_KILL = 35f;

    private final PathfinderMob mob;
    private final List<Class<? extends LivingEntity>> preyTypes;

    private LivingEntity targetPrey;
    private int searchCooldown;
    private int pounceTicks;
    private int cooldownTicks;
    private boolean isPouncing;
    private Vec3 pounceStartPos;
    private Vec3 pounceTargetPos;
    private boolean preyWasKilled;

    /**
     * Creates a new FoxPounceHuntingGoal.
     *
     * @param mob the fox that will pounce
     * @param preyTypes valid prey entity types to pounce on
     */
    @SafeVarargs
    public FoxPounceHuntingGoal(PathfinderMob mob, Class<? extends LivingEntity>... preyTypes) {
        this.mob = mob;
        this.preyTypes = Arrays.asList(preyTypes);
        this.searchCooldown = 0;
        this.pounceTicks = 0;
        this.cooldownTicks = 0;
        this.isPouncing = false;
        this.preyWasKilled = false;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }

        if (!AnimalNeeds.isHungry(this.mob)) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        boolean foundPrey = findPounceTargetInRange();

        LOGGER.debug("{} canUse pounce: foundPrey={}, hunger={}",
            this.mob.getName().getString(), foundPrey, AnimalNeeds.getHunger(this.mob));

        return foundPrey;
    }

    @Override
    public boolean canContinueToUse() {
        if (AnimalNeeds.isSatisfied(this.mob)) {
            LOGGER.debug("{} is satisfied, stopping pounce", this.mob.getName().getString());
            return false;
        }

        if (this.targetPrey == null || !this.targetPrey.isAlive()) {
            if (this.preyWasKilled) {
                restoreHungerFromKill();
                this.preyWasKilled = false;
            }
            return false;
        }

        if (this.isPouncing) {
            return this.pounceTicks < POUNCE_DURATION_TICKS;
        }

        double distance = this.mob.distanceTo(this.targetPrey);
        return distance <= MAX_POUNCE_DISTANCE;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting pounce at {}",
            this.mob.getName().getString(),
            this.targetPrey != null ? this.targetPrey.getName().getString() : "null");

        this.pounceTicks = 0;
        this.isPouncing = false;
        this.preyWasKilled = false;

        if (this.targetPrey != null) {
            initiatePounce();
        }
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped pouncing. Hunger: {}",
            this.mob.getName().getString(), AnimalNeeds.getHunger(this.mob));

        this.targetPrey = null;
        this.mob.setTarget(null);
        this.isPouncing = false;
        this.pounceTicks = 0;
        this.pounceStartPos = null;
        this.pounceTargetPos = null;
        this.cooldownTicks = COOLDOWN_AFTER_POUNCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetPrey == null) {
            return;
        }

        if (!this.targetPrey.isAlive() && this.mob.getTarget() == this.targetPrey) {
            this.preyWasKilled = true;
            return;
        }

        this.mob.getLookControl().setLookAt(this.targetPrey, 30.0F, 30.0F);

        if (this.isPouncing) {
            executePounce();
        } else {
            double distance = this.mob.distanceTo(this.targetPrey);
            if (isInPounceRange(distance)) {
                initiatePounce();
            } else if (distance < MIN_POUNCE_DISTANCE) {
                moveAwaySlightly();
            } else {
                // Fox is stalking toward prey - crouch animation
                approachPrey();
                // Play stalking animation while approaching (foxes stalk before pouncing)
                if (distance <= MAX_POUNCE_DISTANCE * 1.5) {
                    HuntingAnimations.playStalkingAnimation(this.mob, (int) (distance * 10));
                }
            }
        }
    }

    /**
     * Finds prey within pounce range.
     *
     * @return true if valid prey was found in pounce range
     */
    private boolean findPounceTargetInRange() {
        AABB searchBox = this.mob.getBoundingBox().inflate(MAX_POUNCE_DISTANCE);
        List<LivingEntity> potentialPrey = this.mob.level()
            .getEntitiesOfClass(LivingEntity.class, searchBox, this::isValidPrey);

        if (potentialPrey.isEmpty()) {
            return false;
        }

        LivingEntity closestPrey = null;
        double closestDistance = Double.MAX_VALUE;

        for (LivingEntity prey : potentialPrey) {
            double distance = this.mob.distanceTo(prey);
            if (isInPounceRange(distance) && distance < closestDistance) {
                closestDistance = distance;
                closestPrey = prey;
            }
        }

        if (closestPrey != null) {
            this.targetPrey = closestPrey;
            LOGGER.debug("{} found pounce target: {} at distance {}",
                this.mob.getName().getString(),
                closestPrey.getName().getString(),
                String.format("%.1f", closestDistance));
            return true;
        }

        return false;
    }

    /**
     * Validates if an entity is valid prey for pouncing.
     *
     * @param entity the entity to validate
     * @return true if the entity can be pounced on
     */
    private boolean isValidPrey(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        if (entity == this.mob || entity.isInvulnerable()) {
            return false;
        }

        for (Class<? extends LivingEntity> preyType : this.preyTypes) {
            if (preyType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if distance is within ideal pounce range.
     *
     * @param distance distance to prey
     * @return true if in pounce range
     */
    private boolean isInPounceRange(double distance) {
        return distance >= MIN_POUNCE_DISTANCE && distance <= MAX_POUNCE_DISTANCE;
    }

    /**
     * Initiates the pounce attack.
     */
    private void initiatePounce() {
        if (this.targetPrey == null) {
            return;
        }

        this.isPouncing = true;
        this.pounceTicks = 0;
        this.pounceStartPos = this.mob.position();
        this.pounceTargetPos = this.targetPrey.position();

        this.mob.setTarget(this.targetPrey);
        this.mob.getNavigation().stop();

        // Play lunge animation and sound
        HuntingAnimations.playLungeAnimation(this.mob, this.targetPrey);

        LOGGER.debug("{} initiated pounce on {} from distance {}",
            this.mob.getName().getString(),
            this.targetPrey.getName().getString(),
            String.format("%.1f", this.mob.distanceTo(this.targetPrey)));
    }

    /**
     * Executes the pounce motion and attack.
     */
    private void executePounce() {
        this.pounceTicks++;

        if (this.pounceStartPos == null || this.pounceTargetPos == null) {
            this.isPouncing = false;
            return;
        }

        double progress = (double) this.pounceTicks / POUNCE_DURATION_TICKS;
        Vec3 currentPos = calculatePouncePosition(progress);

        Vec3 velocity = currentPos.subtract(this.mob.position());
        this.mob.setDeltaMovement(velocity);

        spawnPounceParticles();

        if (this.pounceTicks >= POUNCE_DURATION_TICKS) {
            completePounce();
        }
    }

    /**
     * Calculates position along pounce arc.
     *
     * @param progress progress from 0.0 to 1.0
     * @return position along arc
     */
    private Vec3 calculatePouncePosition(double progress) {
        Vec3 horizontalPos = this.pounceStartPos.lerp(this.pounceTargetPos, progress);

        double parabolaHeight = POUNCE_HEIGHT * Math.sin(progress * Math.PI);

        return new Vec3(
            horizontalPos.x,
            horizontalPos.y + parabolaHeight,
            horizontalPos.z
        );
    }

    /**
     * Spawns particle effects during pounce.
     */
    private void spawnPounceParticles() {
        if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.pounceTicks % 2 == 0) {
            Vec3 pos = this.mob.position();
            serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                pos.x, pos.y + 0.5, pos.z,
                2,
                0.2, 0.1, 0.2,
                0.02
            );
        }
    }

    /**
     * Completes the pounce and attempts to attack prey.
     */
    private void completePounce() {
        this.isPouncing = false;

        if (this.targetPrey == null || !this.targetPrey.isAlive()) {
            return;
        }

        double distanceToPrey = this.mob.distanceTo(this.targetPrey);

        if (distanceToPrey <= 2.5) {
            boolean success = this.mob.getRandom().nextDouble() < SUCCESS_RATE;

            if (success) {
                this.mob.doHurtTarget(this.targetPrey);
                HuntingAnimations.playAttackSwingAnimation(this.mob, this.targetPrey);
                LOGGER.debug("{} successfully pounced on {} ({}% success rate)",
                    this.mob.getName().getString(),
                    this.targetPrey.getName().getString(),
                    (int)(SUCCESS_RATE * 100));
            } else {
                LOGGER.debug("{} pounce missed {} by {}",
                    this.mob.getName().getString(),
                    this.targetPrey.getName().getString(),
                    String.format("%.1f", distanceToPrey));
            }
        } else {
            LOGGER.debug("{} pounce landed too far from prey ({})",
                this.mob.getName().getString(),
                String.format("%.1f", distanceToPrey));
        }
    }

    /**
     * Moves fox away slightly if too close to prey.
     */
    private void moveAwaySlightly() {
        if (this.targetPrey == null) {
            return;
        }

        Vec3 mobPos = this.mob.position();
        Vec3 preyPos = this.targetPrey.position();
        Vec3 awayDirection = mobPos.subtract(preyPos).normalize();
        Vec3 targetPos = mobPos.add(awayDirection.scale(2.0));

        this.mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 0.8);
    }

    /**
     * Approaches prey to get into pounce range.
     */
    private void approachPrey() {
        if (this.targetPrey == null) {
            return;
        }

        this.mob.getNavigation().moveTo(this.targetPrey, 1.0);
    }

    /**
     * Restores hunger when prey is successfully killed.
     */
    private void restoreHungerFromKill() {
        float previousHunger = AnimalNeeds.getHunger(this.mob);
        AnimalNeeds.modifyHunger(this.mob, HUNGER_RESTORE_ON_KILL);
        float newHunger = AnimalNeeds.getHunger(this.mob);

        LOGGER.debug("{} killed prey with pounce and restored hunger: {} -> {} (+{})",
            this.mob.getName().getString(),
            String.format("%.1f", previousHunger),
            String.format("%.1f", newHunger),
            String.format("%.1f", HUNGER_RESTORE_ON_KILL));
    }
}
