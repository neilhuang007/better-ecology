package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal implementing sit-and-wait ambush hunting for frogs.
 *
 * <p>Scientific Basis: Frogs are sit-and-wait predators that remain motionless
 * near water edges or on lily pads, waiting for prey to come within tongue-strike
 * range (3-4 blocks). Tongue strikes occur in 50-70 milliseconds and have higher
 * success rates (70%) compared to active hunting (50%).
 *
 * <p>Behavior:
 * <ul>
 *   <li>Frog sits motionless at preferred ambush spots (lily pads, water edges, logs)</li>
 *   <li>Waits for prey (slimes, magma cubes) to come within 3.5 blocks</li>
 *   <li>Performs rapid tongue strike animation with particle effects</li>
 *   <li>Returns to ambush position after strike</li>
 *   <li>Higher success rate than active hunting (70% vs 50%)</li>
 * </ul>
 */
public class FrogAmbushHuntingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrogAmbushHuntingGoal.class);

    private static final int MIN_AMBUSH_WAIT_TICKS = 60;   // 3 seconds
    private static final int MAX_AMBUSH_WAIT_TICKS = 200;  // 10 seconds
    private static final double TONGUE_STRIKE_RANGE = 3.5;
    private static final double TONGUE_STRIKE_RANGE_SQ = TONGUE_STRIKE_RANGE * TONGUE_STRIKE_RANGE;
    private static final float STRIKE_SUCCESS_CHANCE = 0.7f;
    private static final int STRIKE_COOLDOWN_TICKS = 40;   // 2 seconds
    private static final int STRIKE_ANIMATION_TICKS = 4;   // 200ms (50-70ms tongue strike)
    private static final int AMBUSH_SEARCH_RADIUS = 16;
    private static final int MAX_AMBUSH_TIME = 600;        // 30 seconds before relocating

    private final PathfinderMob frog;
    private final List<Class<? extends LivingEntity>> preyTypes;

    private BlockPos ambushPosition;
    private int ambushWaitTicks;
    private int ambushTimer;
    private int strikeCooldown;
    private int strikeAnimationTimer;
    private LivingEntity targetPrey;
    private boolean isStriking;
    private boolean returningToAmbush;

    /**
     * Creates a new FrogAmbushHuntingGoal.
     *
     * @param frog the frog that will ambush hunt
     */
    @SafeVarargs
    public FrogAmbushHuntingGoal(PathfinderMob frog, Class<? extends LivingEntity>... preyTypes) {
        this.frog = frog;
        this.preyTypes = Arrays.asList(preyTypes);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AnimalNeeds.isHungry(this.frog)) {
            return false;
        }

        if (this.strikeCooldown > 0) {
            this.strikeCooldown--;
            return false;
        }

        return findAmbushPosition();
    }

    @Override
    public boolean canContinueToUse() {
        if (AnimalNeeds.isSatisfied(this.frog)) {
            LOGGER.debug("Frog {} is satisfied, stopping ambush", this.frog.getName().getString());
            return false;
        }

        if (this.ambushTimer >= MAX_AMBUSH_TIME) {
            LOGGER.debug("Frog {} ambush timeout, relocating", this.frog.getName().getString());
            return false;
        }

        if (this.ambushPosition == null || !isValidAmbushSpot(this.ambushPosition)) {
            LOGGER.debug("Frog {} ambush position no longer valid", this.frog.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("Frog {} starting ambush at {}", this.frog.getName().getString(), this.ambushPosition);
        this.ambushTimer = 0;
        this.ambushWaitTicks = MIN_AMBUSH_WAIT_TICKS +
            this.frog.getRandom().nextInt(MAX_AMBUSH_WAIT_TICKS - MIN_AMBUSH_WAIT_TICKS);
        this.isStriking = false;
        this.returningToAmbush = false;
        this.targetPrey = null;
        navigateToAmbushPosition();
    }

    @Override
    public void stop() {
        LOGGER.debug("Frog {} stopped ambush hunting", this.frog.getName().getString());
        this.ambushPosition = null;
        this.targetPrey = null;
        this.isStriking = false;
        this.returningToAmbush = false;
        this.frog.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ambushTimer++;

        if (this.isStriking) {
            performTongueStrike();
            return;
        }

        if (this.returningToAmbush) {
            if (isAtAmbushPosition()) {
                this.returningToAmbush = false;
                this.ambushWaitTicks = MIN_AMBUSH_WAIT_TICKS +
                    this.frog.getRandom().nextInt(MAX_AMBUSH_WAIT_TICKS - MIN_AMBUSH_WAIT_TICKS);
                LOGGER.debug("Frog {} returned to ambush position", this.frog.getName().getString());
            } else if (this.frog.getNavigation().isDone()) {
                navigateToAmbushPosition();
            }
            return;
        }

        if (!isAtAmbushPosition()) {
            if (this.frog.getNavigation().isDone()) {
                navigateToAmbushPosition();
            }
            return;
        }

        this.frog.getNavigation().stop();
        this.ambushWaitTicks--;

        LivingEntity nearbyPrey = scanForPrey();
        if (nearbyPrey != null && this.strikeCooldown <= 0) {
            initiateStrike(nearbyPrey);
        }
    }

    /**
     * Finds a suitable ambush position near water or on preferred surfaces.
     *
     * @return true if a valid ambush position was found
     */
    private boolean findAmbushPosition() {
        BlockPos frogPos = this.frog.blockPosition();
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        BlockPos bestPosition = null;
        int bestScore = -1;

        for (int x = -AMBUSH_SEARCH_RADIUS; x <= AMBUSH_SEARCH_RADIUS; x++) {
            for (int z = -AMBUSH_SEARCH_RADIUS; z <= AMBUSH_SEARCH_RADIUS; z++) {
                for (int y = -3; y <= 3; y++) {
                    searchPos.set(frogPos.getX() + x, frogPos.getY() + y, frogPos.getZ() + z);

                    if (isValidAmbushSpot(searchPos)) {
                        int score = scoreAmbushPosition(searchPos);
                        if (score > bestScore) {
                            bestScore = score;
                            bestPosition = searchPos.immutable();
                        }
                    }
                }
            }
        }

        if (bestPosition != null) {
            this.ambushPosition = bestPosition;
            LOGGER.debug("Frog {} found ambush position at {} (score: {})",
                this.frog.getName().getString(), bestPosition, bestScore);
            return true;
        }

        LOGGER.debug("Frog {} could not find ambush position", this.frog.getName().getString());
        return false;
    }

    /**
     * Checks if a position is a valid ambush spot.
     *
     * @param pos the position to check
     * @return true if valid for ambushing
     */
    private boolean isValidAmbushSpot(BlockPos pos) {
        Level level = this.frog.level();
        BlockState blockState = level.getBlockState(pos);
        BlockState belowState = level.getBlockState(pos.below());

        if (!blockState.isAir() && !level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }

        return belowState.is(Blocks.LILY_PAD) ||
               belowState.is(BlockTags.LOGS) ||
               isNearWaterEdge(pos);
    }

    /**
     * Checks if a position is near a water edge.
     *
     * @param pos the position to check
     * @return true if near water edge
     */
    private boolean isNearWaterEdge(BlockPos pos) {
        Level level = this.frog.level();
        BlockPos[] adjacentPositions = {
            pos.north(), pos.south(), pos.east(), pos.west()
        };

        for (BlockPos adjacent : adjacentPositions) {
            if (level.getFluidState(adjacent).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Scores an ambush position based on strategic value.
     * Higher scores indicate better positions.
     *
     * @param pos the position to score
     * @return ambush position score
     */
    private int scoreAmbushPosition(BlockPos pos) {
        Level level = this.frog.level();
        BlockState belowState = level.getBlockState(pos.below());
        int score = 0;

        if (belowState.is(Blocks.LILY_PAD)) {
            score += 50;
        }

        if (belowState.is(BlockTags.LOGS)) {
            score += 30;
        }

        if (isNearWaterEdge(pos)) {
            score += 40;
        }

        double distanceFromFrog = this.frog.blockPosition().distSqr(pos);
        score -= (int) (distanceFromFrog / 10.0);

        return score;
    }

    /**
     * Checks if the frog is at the ambush position.
     *
     * @return true if at ambush position
     */
    private boolean isAtAmbushPosition() {
        if (this.ambushPosition == null) {
            return false;
        }

        return this.frog.blockPosition().distSqr(this.ambushPosition) <= 4.0;
    }

    /**
     * Navigates the frog to the ambush position.
     */
    private void navigateToAmbushPosition() {
        if (this.ambushPosition == null) {
            return;
        }

        this.frog.getNavigation().moveTo(
            this.ambushPosition.getX() + 0.5,
            this.ambushPosition.getY(),
            this.ambushPosition.getZ() + 0.5,
            1.0
        );
    }

    /**
     * Scans for prey within tongue strike range.
     *
     * @return the nearest valid prey, or null if none found
     */
    private LivingEntity scanForPrey() {
        AABB searchBox = this.frog.getBoundingBox().inflate(TONGUE_STRIKE_RANGE);
        List<LivingEntity> nearbyEntities = this.frog.level().getEntitiesOfClass(
            LivingEntity.class, searchBox, this::isValidPrey);

        if (nearbyEntities.isEmpty()) {
            return null;
        }

        return nearbyEntities.stream()
            .min((a, b) -> Double.compare(
                this.frog.distanceToSqr(a),
                this.frog.distanceToSqr(b)))
            .orElse(null);
    }

    /**
     * Validates if an entity is valid prey for ambush hunting.
     *
     * @param entity the entity to validate
     * @return true if valid prey
     */
    private boolean isValidPrey(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == this.frog) {
            return false;
        }

        for (Class<? extends LivingEntity> preyType : this.preyTypes) {
            if (preyType.isInstance(entity)) {
                double distanceSq = this.frog.distanceToSqr(entity);
                return distanceSq <= TONGUE_STRIKE_RANGE_SQ;
            }
        }

        return false;
    }

    /**
     * Initiates a tongue strike at the target prey.
     *
     * @param prey the prey to strike
     */
    private void initiateStrike(LivingEntity prey) {
        this.targetPrey = prey;
        this.isStriking = true;
        this.strikeAnimationTimer = 0;

        LOGGER.debug("Frog {} initiating tongue strike at {}",
            this.frog.getName().getString(), prey.getName().getString());

        this.frog.getLookControl().setLookAt(prey, 30.0F, 30.0F);
    }

    /**
     * Performs the tongue strike animation and damage application.
     */
    private void performTongueStrike() {
        this.strikeAnimationTimer++;

        if (this.targetPrey == null || !this.targetPrey.isAlive()) {
            finishStrike(false);
            return;
        }

        this.frog.getLookControl().setLookAt(this.targetPrey, 30.0F, 30.0F);

        if (this.strikeAnimationTimer == 1) {
            playTongueStrikeAnimation();
        }

        if (this.strikeAnimationTimer >= STRIKE_ANIMATION_TICKS) {
            boolean success = attemptStrike();
            finishStrike(success);
        }
    }

    /**
     * Plays the tongue strike particle animation and sound.
     */
    private void playTongueStrikeAnimation() {
        Level level = this.frog.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 frogPos = this.frog.position().add(0, this.frog.getEyeHeight() * 0.5, 0);
        Vec3 preyPos = this.targetPrey.position().add(0, this.targetPrey.getBbHeight() * 0.5, 0);
        Vec3 direction = preyPos.subtract(frogPos).normalize();

        int particleCount = 15;
        for (int i = 0; i <= particleCount; i++) {
            double fraction = i / (double) particleCount;
            Vec3 particlePos = frogPos.add(direction.scale(frogPos.distanceTo(preyPos) * fraction));

            serverLevel.sendParticles(
                ParticleTypes.SPIT,
                particlePos.x, particlePos.y, particlePos.z,
                1,
                0, 0, 0,
                0
            );
        }

        level.playSound(
            null,
            this.frog.getX(), this.frog.getY(), this.frog.getZ(),
            SoundEvents.FROG_TONGUE,
            SoundSource.NEUTRAL,
            1.0F,
            0.9F + this.frog.getRandom().nextFloat() * 0.2F
        );
    }

    /**
     * Attempts to strike and damage the prey with success chance.
     *
     * @return true if strike was successful
     */
    private boolean attemptStrike() {
        if (this.targetPrey == null || !this.targetPrey.isAlive()) {
            return false;
        }

        double distanceSq = this.frog.distanceToSqr(this.targetPrey);
        if (distanceSq > TONGUE_STRIKE_RANGE_SQ) {
            LOGGER.debug("Frog {} strike failed - prey out of range", this.frog.getName().getString());
            return false;
        }

        if (this.frog.getRandom().nextFloat() > STRIKE_SUCCESS_CHANCE) {
            LOGGER.debug("Frog {} strike failed - missed", this.frog.getName().getString());
            return false;
        }

        boolean preyKilled = this.frog.doHurtTarget(this.targetPrey);

        if (!this.targetPrey.isAlive()) {
            float hungerRestore = 30f;
            AnimalNeeds.modifyHunger(this.frog, hungerRestore);
            LOGGER.debug("Frog {} successful strike - killed prey, restored {} hunger",
                this.frog.getName().getString(), hungerRestore);
            return true;
        }

        LOGGER.debug("Frog {} successful strike - damaged prey", this.frog.getName().getString());
        return true;
    }

    /**
     * Finishes the strike and sets cooldown.
     *
     * @param success whether the strike was successful
     */
    private void finishStrike(boolean success) {
        this.isStriking = false;
        this.targetPrey = null;
        this.strikeAnimationTimer = 0;
        this.strikeCooldown = STRIKE_COOLDOWN_TICKS;

        if (success) {
            playStrikeSuccessSound();
        }

        if (!AnimalNeeds.isSatisfied(this.frog)) {
            this.returningToAmbush = true;
        }
    }

    /**
     * Plays the sound effect for a successful strike.
     */
    private void playStrikeSuccessSound() {
        this.frog.level().playSound(
            null,
            this.frog.getX(), this.frog.getY(), this.frog.getZ(),
            SoundEvents.FROG_EAT,
            SoundSource.NEUTRAL,
            0.8F,
            0.9F + this.frog.getRandom().nextFloat() * 0.2F
        );
    }
}
