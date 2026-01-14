package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes cats and ocelots stalk prey before pouncing.
 *
 * <p>Scientific Basis:
 * <ul>
 *   <li>Cats stalk within 3-5 meters before pouncing</li>
 *   <li>Crouch-walk reduces visibility and vibration detection</li>
 *   <li>70% of hunt time is stalking</li>
 *   <li>Success rate is 25-35%</li>
 * </ul>
 *
 * <p>Behavior:
 * <ul>
 *   <li>When prey detected and not looking at cat, enter stalk mode</li>
 *   <li>Crouch animation (sneaking pose)</li>
 *   <li>Move at 0.3x speed toward prey</li>
 *   <li>Approach to 2-3 blocks, then rapid dash and pounce</li>
 *   <li>If prey notices (turns toward cat), stalk fails - must wait and retry</li>
 *   <li>Hunt success rate: 30% (most attempts fail)</li>
 * </ul>
 *
 * <p>Priority: {@link AnimalThresholds#PRIORITY_HUNT} (4)
 */
public class CatStalkingAmbushGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatStalkingAmbushGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final int GIVE_UP_TICKS = 600;
    private static final double STALK_SPEED_MODIFIER = 0.3;
    private static final double POUNCE_SPEED_MODIFIER = 2.0;
    private static final double STALK_RANGE = 20.0;
    private static final double POUNCE_DISTANCE_MIN = 2.0;
    private static final double POUNCE_DISTANCE_MAX = 3.0;
    private static final double ATTACK_DISTANCE_SQUARED = 4.0;
    private static final float SUCCESS_RATE = 0.30f;
    private static final int FAILED_RETRY_COOLDOWN = 100;
    private static final double PREY_NOTICE_ANGLE = 60.0;

    private final PathfinderMob cat;
    private final List<Class<? extends LivingEntity>> preyTypes;

    private LivingEntity targetPrey;
    private int searchCooldown;
    private int stalkTicks;
    private int failedCooldown;
    private StalkPhase currentPhase;
    private boolean pounceInitiated;

    private enum StalkPhase {
        SEARCHING,
        STALKING,
        POUNCING,
        FAILED
    }

    /**
     * Creates a new CatStalkingAmbushGoal.
     *
     * @param cat the cat that will stalk and pounce
     * @param preyTypes valid prey entity types to hunt
     */
    @SafeVarargs
    public CatStalkingAmbushGoal(PathfinderMob cat, Class<? extends LivingEntity>... preyTypes) {
        this.cat = cat;
        this.preyTypes = Arrays.asList(preyTypes);
        this.searchCooldown = 0;
        this.stalkTicks = 0;
        this.failedCooldown = 0;
        this.currentPhase = StalkPhase.SEARCHING;
        this.pounceInitiated = false;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AnimalNeeds.isHungry(this.cat)) {
            return false;
        }

        if (this.failedCooldown > 0) {
            this.failedCooldown--;
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        return findAndTargetPrey();
    }

    @Override
    public boolean canContinueToUse() {
        if (AnimalNeeds.isSatisfied(this.cat)) {
            return false;
        }

        if (this.stalkTicks >= GIVE_UP_TICKS) {
            LOGGER.debug("{} gave up stalking after {} ticks", this.cat.getName().getString(), this.stalkTicks);
            return false;
        }

        if (this.targetPrey == null || !this.targetPrey.isAlive()) {
            if (this.pounceInitiated && this.targetPrey != null && !this.targetPrey.isAlive()) {
                checkPounceSuccess();
            }
            return false;
        }

        if (isPreyTooFar()) {
            LOGGER.debug("{} prey escaped beyond range", this.cat.getName().getString());
            return false;
        }

        if (this.currentPhase == StalkPhase.FAILED) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting stalk for {}", this.cat.getName().getString(),
            this.targetPrey != null ? this.targetPrey.getName().getString() : "null");
        this.stalkTicks = 0;
        this.currentPhase = StalkPhase.STALKING;
        this.pounceInitiated = false;
        this.cat.setTarget(this.targetPrey);
        setSneaking(true);
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped stalking. Phase: {}, Hunger: {}",
            this.cat.getName().getString(), this.currentPhase, AnimalNeeds.getHunger(this.cat));
        this.targetPrey = null;
        this.cat.setTarget(null);
        this.cat.getNavigation().stop();
        this.stalkTicks = 0;
        this.currentPhase = StalkPhase.SEARCHING;
        setSneaking(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.stalkTicks++;

        if (this.targetPrey == null) {
            return;
        }

        this.cat.getLookControl().setLookAt(this.targetPrey, 30.0F, 30.0F);

        double distanceToPrey = this.cat.distanceTo(this.targetPrey);

        if (this.currentPhase == StalkPhase.STALKING) {
            // Play stalking animation while crouched
            HuntingAnimations.playStalkingAnimation(this.cat, this.stalkTicks);

            if (isPreyLookingAtCat()) {
                failStalk();
                return;
            }

            if (distanceToPrey <= POUNCE_DISTANCE_MAX && distanceToPrey >= POUNCE_DISTANCE_MIN) {
                initiatePounce();
            } else {
                stalkTowardPrey();
            }
        } else if (this.currentPhase == StalkPhase.POUNCING) {
            pounceAtPrey();

            if (this.cat.distanceToSqr(this.targetPrey) <= ATTACK_DISTANCE_SQUARED) {
                this.cat.doHurtTarget(this.targetPrey);
                HuntingAnimations.playAttackSwingAnimation(this.cat, this.targetPrey);
                LOGGER.debug("{} pounced and attacked {}",
                    this.cat.getName().getString(),
                    this.targetPrey.getName().getString());
            }
        }
    }

    /**
     * Finds and targets the best prey within stalk range.
     *
     * @return true if valid prey was found
     */
    private boolean findAndTargetPrey() {
        List<LivingEntity> potentialPrey = this.cat.level().getEntitiesOfClass(
            LivingEntity.class,
            this.cat.getBoundingBox().inflate(STALK_RANGE),
            this::isValidPrey
        );

        if (potentialPrey.isEmpty()) {
            return false;
        }

        LivingEntity selectedPrey = selectBestPrey(potentialPrey);

        if (selectedPrey != null && !isPreyLookingAtTarget(selectedPrey, this.cat)) {
            this.targetPrey = selectedPrey;
            LOGGER.debug("{} selected prey: {} at distance {}",
                this.cat.getName().getString(),
                selectedPrey.getName().getString(),
                this.cat.distanceTo(selectedPrey));
            return true;
        }

        return false;
    }

    /**
     * Validates if an entity is valid prey for stalking.
     *
     * @param entity the entity to validate
     * @return true if the entity can be stalked
     */
    private boolean isValidPrey(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == this.cat || entity.isInvulnerable()) {
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
     * Selects the best prey from a list of potential targets.
     * Prefers nearest prey that is not looking at the cat.
     *
     * @param potentialPrey list of valid prey entities
     * @return the selected prey, or null if none suitable
     */
    private LivingEntity selectBestPrey(List<LivingEntity> potentialPrey) {
        return potentialPrey.stream()
            .filter(prey -> !isPreyLookingAtTarget(prey, this.cat))
            .min(Comparator.comparingDouble(this.cat::distanceTo))
            .orElse(null);
    }

    /**
     * Checks if the prey is currently looking at the cat.
     *
     * @return true if prey is facing the cat
     */
    private boolean isPreyLookingAtCat() {
        if (this.targetPrey == null) {
            return false;
        }
        return isPreyLookingAtTarget(this.targetPrey, this.cat);
    }

    /**
     * Checks if a prey entity is looking at a target entity.
     *
     * @param prey the prey entity
     * @param target the target entity to check
     * @return true if prey is facing the target
     */
    private boolean isPreyLookingAtTarget(LivingEntity prey, LivingEntity target) {
        Vec3 preyLookVec = prey.getViewVector(1.0F);
        Vec3 toTarget = new Vec3(
            target.getX() - prey.getX(),
            target.getEyeY() - prey.getEyeY(),
            target.getZ() - prey.getZ()
        ).normalize();

        double dotProduct = preyLookVec.dot(toTarget);
        double angleThreshold = Math.cos(Math.toRadians(PREY_NOTICE_ANGLE));

        return dotProduct > angleThreshold;
    }

    /**
     * Checks if the prey has escaped beyond stalking range.
     *
     * @return true if prey is too far
     */
    private boolean isPreyTooFar() {
        if (this.targetPrey == null) {
            return true;
        }
        return this.cat.distanceTo(this.targetPrey) > STALK_RANGE * 1.5;
    }

    /**
     * Moves the cat toward prey in stalking mode (slow, crouched).
     */
    private void stalkTowardPrey() {
        if (this.targetPrey == null) {
            return;
        }

        this.cat.getNavigation().moveTo(this.targetPrey, STALK_SPEED_MODIFIER);
    }

    /**
     * Initiates the pounce phase.
     */
    private void initiatePounce() {
        this.currentPhase = StalkPhase.POUNCING;
        this.pounceInitiated = true;
        setSneaking(false);

        // Play lunge animation for dramatic pounce
        HuntingAnimations.playLungeAnimation(this.cat, this.targetPrey);

        LOGGER.debug("{} initiating pounce at {}",
            this.cat.getName().getString(),
            this.targetPrey.getName().getString());
    }

    /**
     * Executes the pounce at prey (fast dash).
     */
    private void pounceAtPrey() {
        if (this.targetPrey == null) {
            return;
        }

        this.cat.getNavigation().moveTo(this.targetPrey, POUNCE_SPEED_MODIFIER);
    }

    /**
     * Marks the stalk as failed and sets cooldown.
     */
    private void failStalk() {
        this.currentPhase = StalkPhase.FAILED;
        this.failedCooldown = FAILED_RETRY_COOLDOWN;
        LOGGER.debug("{} stalk failed - prey noticed", this.cat.getName().getString());
    }

    /**
     * Checks if the pounce was successful and restores hunger if so.
     */
    private void checkPounceSuccess() {
        float roll = this.cat.getRandom().nextFloat();

        if (roll < SUCCESS_RATE) {
            float hungerRestore = 40f;
            AnimalNeeds.modifyHunger(this.cat, hungerRestore);
            LOGGER.debug("{} successful hunt! Restored {} hunger (success roll: {})",
                this.cat.getName().getString(), hungerRestore, String.format("%.2f", roll));
        } else {
            LOGGER.debug("{} failed hunt (success roll: {} > {})",
                this.cat.getName().getString(), String.format("%.2f", roll), SUCCESS_RATE);
        }
    }

    /**
     * Sets the cat's sneaking state.
     *
     * @param sneaking true to enable sneaking, false to disable
     */
    private void setSneaking(boolean sneaking) {
        this.cat.setShiftKeyDown(sneaking);
    }
}
