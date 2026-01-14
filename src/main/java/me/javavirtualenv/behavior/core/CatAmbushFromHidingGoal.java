package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Goal that makes cats and ocelots hide and ambush prey from concealment.
 *
 * <p>Scientific Basis:
 * <ul>
 *   <li>Cats use concealment to improve hunting success</li>
 *   <li>Can wait motionless for up to 60 seconds</li>
 *   <li>Rapid pounce when prey within 4-6 blocks</li>
 *   <li>Success rate from ambush: 45% (higher than stalking)</li>
 * </ul>
 *
 * <p>Behavior:
 * <ul>
 *   <li>Cat finds concealed position (tall grass, leaves, ferns)</li>
 *   <li>Sits motionless waiting for prey</li>
 *   <li>Rapid pounce when prey within 4-6 blocks</li>
 *   <li>Can wait up to 60 seconds before giving up</li>
 * </ul>
 *
 * <p>Priority: {@link AnimalThresholds#PRIORITY_HUNT} (4)
 */
public class CatAmbushFromHidingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatAmbushFromHidingGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 40;
    private static final int MAX_WAIT_TICKS = 1200;
    private static final double HIDE_SEARCH_RANGE = 16.0;
    private static final double POUNCE_RANGE_MIN = 4.0;
    private static final double POUNCE_RANGE_MAX = 6.0;
    private static final double POUNCE_SPEED_MODIFIER = 2.0;
    private static final double ATTACK_DISTANCE_SQUARED = 4.0;
    private static final float SUCCESS_RATE = 0.45f;

    private static final Set<Block> HIDING_BLOCKS = Set.of(
        Blocks.TALL_GRASS,
        Blocks.SHORT_GRASS,
        Blocks.FERN,
        Blocks.LARGE_FERN,
        Blocks.OAK_LEAVES,
        Blocks.BIRCH_LEAVES,
        Blocks.SPRUCE_LEAVES,
        Blocks.JUNGLE_LEAVES,
        Blocks.ACACIA_LEAVES,
        Blocks.DARK_OAK_LEAVES,
        Blocks.AZALEA_LEAVES,
        Blocks.FLOWERING_AZALEA_LEAVES,
        Blocks.MANGROVE_LEAVES,
        Blocks.CHERRY_LEAVES
    );

    private final PathfinderMob cat;
    private final List<Class<? extends LivingEntity>> preyTypes;

    private BlockPos hidingSpot;
    private LivingEntity targetPrey;
    private int searchCooldown;
    private int waitTicks;
    private AmbushPhase currentPhase;
    private boolean pounceInitiated;

    private enum AmbushPhase {
        SEARCHING_HIDING_SPOT,
        MOVING_TO_HIDING_SPOT,
        WAITING_IN_AMBUSH,
        POUNCING
    }

    /**
     * Creates a new CatAmbushFromHidingGoal.
     *
     * @param cat the cat that will ambush from hiding
     * @param preyTypes valid prey entity types to hunt
     */
    @SafeVarargs
    public CatAmbushFromHidingGoal(PathfinderMob cat, Class<? extends LivingEntity>... preyTypes) {
        this.cat = cat;
        this.preyTypes = Arrays.asList(preyTypes);
        this.searchCooldown = 0;
        this.waitTicks = 0;
        this.currentPhase = AmbushPhase.SEARCHING_HIDING_SPOT;
        this.pounceInitiated = false;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AnimalNeeds.isHungry(this.cat)) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        this.hidingSpot = findNearbyHidingSpot();

        if (this.hidingSpot != null) {
            LOGGER.debug("{} found hiding spot at {}", this.cat.getName().getString(), this.hidingSpot);
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (AnimalNeeds.isSatisfied(this.cat)) {
            return false;
        }

        if (this.currentPhase == AmbushPhase.WAITING_IN_AMBUSH && this.waitTicks >= MAX_WAIT_TICKS) {
            LOGGER.debug("{} gave up waiting after {} ticks", this.cat.getName().getString(), this.waitTicks);
            return false;
        }

        if (this.currentPhase == AmbushPhase.POUNCING) {
            if (this.targetPrey == null || !this.targetPrey.isAlive()) {
                if (this.pounceInitiated && this.targetPrey != null && !this.targetPrey.isAlive()) {
                    checkPounceSuccess();
                }
                return false;
            }

            if (this.cat.distanceTo(this.targetPrey) > POUNCE_RANGE_MAX * 2) {
                LOGGER.debug("{} prey escaped during pounce", this.cat.getName().getString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting ambush hunt", this.cat.getName().getString());
        this.waitTicks = 0;
        this.currentPhase = AmbushPhase.MOVING_TO_HIDING_SPOT;
        this.pounceInitiated = false;
        this.targetPrey = null;
        moveToHidingSpot();
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped ambush hunt. Phase: {}, Hunger: {}",
            this.cat.getName().getString(), this.currentPhase, AnimalNeeds.getHunger(this.cat));
        this.hidingSpot = null;
        this.targetPrey = null;
        this.cat.setTarget(null);
        this.cat.getNavigation().stop();
        this.waitTicks = 0;
        this.currentPhase = AmbushPhase.SEARCHING_HIDING_SPOT;
        setSitting(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.currentPhase == AmbushPhase.MOVING_TO_HIDING_SPOT) {
            if (this.cat.getNavigation().isDone() || isAtHidingSpot()) {
                arriveAtHidingSpot();
            }
        } else if (this.currentPhase == AmbushPhase.WAITING_IN_AMBUSH) {
            this.waitTicks++;
            // Cat is crouched and waiting - subtle stalking animation
            if (this.waitTicks % 20 == 0) {
                HuntingAnimations.playStalkingAnimation(this.cat, this.waitTicks);
            }
            lookForPrey();
        } else if (this.currentPhase == AmbushPhase.POUNCING) {
            pounceAtPrey();

            if (this.targetPrey != null && this.cat.distanceToSqr(this.targetPrey) <= ATTACK_DISTANCE_SQUARED) {
                this.cat.doHurtTarget(this.targetPrey);
                HuntingAnimations.playAttackSwingAnimation(this.cat, this.targetPrey);
                LOGGER.debug("{} ambushed and attacked {}",
                    this.cat.getName().getString(),
                    this.targetPrey.getName().getString());
            }
        }
    }

    /**
     * Finds a nearby hiding spot with concealing vegetation.
     *
     * @return the hiding spot position, or null if none found
     */
    private BlockPos findNearbyHidingSpot() {
        BlockPos catPos = this.cat.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();

        int searchRadius = (int) HIDE_SEARCH_RANGE;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = catPos.offset(x, y, z);

                    if (isValidHidingSpot(checkPos)) {
                        candidates.add(checkPos);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(this.cat.getRandom().nextInt(candidates.size()));
    }

    /**
     * Checks if a position is a valid hiding spot.
     *
     * @param pos the position to check
     * @return true if this is a valid hiding spot
     */
    private boolean isValidHidingSpot(BlockPos pos) {
        BlockState blockState = this.cat.level().getBlockState(pos);
        BlockState belowState = this.cat.level().getBlockState(pos.below());

        boolean hasHidingBlock = HIDING_BLOCKS.contains(blockState.getBlock());
        boolean hasSolidGround = belowState.isSolid();

        BlockState aboveState = this.cat.level().getBlockState(pos.above());
        boolean hasHeadroom = aboveState.isAir() || HIDING_BLOCKS.contains(aboveState.getBlock());

        return hasHidingBlock && hasSolidGround && hasHeadroom;
    }

    /**
     * Checks if the cat is at the hiding spot.
     *
     * @return true if cat is at hiding spot
     */
    private boolean isAtHidingSpot() {
        if (this.hidingSpot == null) {
            return false;
        }

        return this.cat.blockPosition().distManhattan(this.hidingSpot) <= 2;
    }

    /**
     * Moves the cat to the hiding spot.
     */
    private void moveToHidingSpot() {
        if (this.hidingSpot == null) {
            return;
        }

        this.cat.getNavigation().moveTo(
            this.hidingSpot.getX() + 0.5,
            this.hidingSpot.getY(),
            this.hidingSpot.getZ() + 0.5,
            1.0
        );
    }

    /**
     * Called when the cat arrives at the hiding spot.
     */
    private void arriveAtHidingSpot() {
        this.currentPhase = AmbushPhase.WAITING_IN_AMBUSH;
        this.cat.getNavigation().stop();
        setSitting(true);
        LOGGER.debug("{} arrived at hiding spot and waiting", this.cat.getName().getString());
    }

    /**
     * Looks for prey while waiting in ambush.
     */
    private void lookForPrey() {
        List<LivingEntity> nearbyPrey = this.cat.level().getEntitiesOfClass(
            LivingEntity.class,
            this.cat.getBoundingBox().inflate(POUNCE_RANGE_MAX),
            this::isValidPrey
        );

        if (!nearbyPrey.isEmpty()) {
            LivingEntity closestPrey = nearbyPrey.stream()
                .min(Comparator.comparingDouble(this.cat::distanceTo))
                .orElse(null);

            if (closestPrey != null) {
                double distance = this.cat.distanceTo(closestPrey);

                if (distance >= POUNCE_RANGE_MIN && distance <= POUNCE_RANGE_MAX) {
                    initiatePounce(closestPrey);
                }
            }
        }
    }

    /**
     * Validates if an entity is valid prey.
     *
     * @param entity the entity to validate
     * @return true if the entity can be ambushed
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
     * Initiates the pounce from hiding.
     *
     * @param prey the prey to pounce at
     */
    private void initiatePounce(LivingEntity prey) {
        this.targetPrey = prey;
        this.currentPhase = AmbushPhase.POUNCING;
        this.pounceInitiated = true;
        this.cat.setTarget(prey);
        setSitting(false);

        // Play dramatic lunge animation when bursting from ambush
        HuntingAnimations.playLungeAnimation(this.cat, prey);

        LOGGER.debug("{} pouncing from hiding at {}",
            this.cat.getName().getString(),
            prey.getName().getString());
    }

    /**
     * Executes the pounce at prey.
     */
    private void pounceAtPrey() {
        if (this.targetPrey == null) {
            return;
        }

        this.cat.getLookControl().setLookAt(this.targetPrey, 30.0F, 30.0F);
        this.cat.getNavigation().moveTo(this.targetPrey, POUNCE_SPEED_MODIFIER);
    }

    /**
     * Checks if the pounce was successful and restores hunger if so.
     */
    private void checkPounceSuccess() {
        float roll = this.cat.getRandom().nextFloat();

        if (roll < SUCCESS_RATE) {
            float hungerRestore = 40f;
            AnimalNeeds.modifyHunger(this.cat, hungerRestore);
            LOGGER.debug("{} successful ambush! Restored {} hunger (success roll: {})",
                this.cat.getName().getString(), hungerRestore, String.format("%.2f", roll));
        } else {
            LOGGER.debug("{} failed ambush (success roll: {} > {})",
                this.cat.getName().getString(), String.format("%.2f", roll), SUCCESS_RATE);
        }
    }

    /**
     * Sets the cat's sitting state.
     *
     * @param sitting true to enable sitting, false to disable
     */
    private void setSitting(boolean sitting) {
        if (this.cat instanceof net.minecraft.world.entity.TamableAnimal tamable) {
            if (sitting) {
                tamable.setInSittingPose(true);
            } else {
                tamable.setInSittingPose(false);
            }
        }
    }
}
