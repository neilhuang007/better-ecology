package me.javavirtualenv.behavior.parent;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Hiding behavior for "hider" species offspring (e.g., deer fawns).
 * Instead of following mother, offspring stay hidden and immobile when mother is away.
 * Based on research into hider vs. follower strategies in ungulates.
 */
public class HidingBehavior extends SteeringBehavior {

    private double motherReturnThreshold;
    private final double hidingDetectionRange;
    private final double emergeSpeed;
    private int maxHideDuration;

    private UUID motherUuid;
    private long hidingStartTime;
    private BlockPos hidingPosition;
    private boolean isHiding;
    private boolean hasFoundHidingSpot;

    public HidingBehavior(double motherReturnThreshold, double hidingDetectionRange,
                         double emergeSpeed, int maxHideDuration) {
        this.motherReturnThreshold = motherReturnThreshold;
        this.hidingDetectionRange = hidingDetectionRange;
        this.emergeSpeed = emergeSpeed;
        this.maxHideDuration = maxHideDuration;
        this.isHiding = false;
        this.hasFoundHidingSpot = false;
    }

    public HidingBehavior() {
        this(8.0, 32.0, 1.3, 6000);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof AgeableMob offspring)) {
            return new Vec3d();
        }

        if (!offspring.isBaby()) {
            isHiding = false;
            return new Vec3d();
        }

        Entity mother = findMother(offspring);
        if (mother == null || !mother.isAlive()) {
            return new Vec3d();
        }

        motherUuid = mother.getUUID();

        Vec3d offspringPos = context.getPosition();
        Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());
        double distanceToMother = offspringPos.distanceTo(motherPos);

        if (distanceToMother > motherReturnThreshold) {
            return enterHidingState(context, offspring);
        } else {
            return exitHidingState(context, offspring, motherPos);
        }
    }

    private Vec3d enterHidingState(BehaviorContext context, AgeableMob offspring) {
        if (!hasFoundHidingSpot) {
            BlockPos hidingSpot = findHidingSpot(offspring);
            if (hidingSpot != null) {
                hidingPosition = hidingSpot;
                hasFoundHidingSpot = true;
                hidingStartTime = System.currentTimeMillis();
            }
        }

        if (hidingPosition != null) {
            isHiding = true;
            // NBT data access removed - using local state instead

            long hidingDuration = (System.currentTimeMillis() - hidingStartTime) / 50;
            if (hidingDuration > maxHideDuration) {
                hasFoundHidingSpot = false;
                return new Vec3d();
            }

            Vec3d targetPos = new Vec3d(hidingPosition.getX() + 0.5,
                                        hidingPosition.getY(),
                                        hidingPosition.getZ() + 0.5);
            Vec3d currentPos = context.getPosition();

            if (currentPos.distanceTo(targetPos) > 0.5) {
                Vec3d steer = seek(currentPos, context.getVelocity(), targetPos, 0.8);
                steer.mult(0.5);
                return steer;
            } else {
                return new Vec3d();
            }
        }

        return new Vec3d();
    }

    private Vec3d exitHidingState(BehaviorContext context, AgeableMob offspring, Vec3d motherPos) {
        if (isHiding) {
            isHiding = false;
            // NBT data access removed - using local state instead
            hasFoundHidingSpot = false;
        }

        Vec3d offspringPos = context.getPosition();
        double distanceToMother = offspringPos.distanceTo(motherPos);

        if (distanceToMother > 3.0) {
            return arrive(offspringPos, context.getVelocity(), motherPos, emergeSpeed, 2.0);
        }

        return new Vec3d();
    }

    private BlockPos findHidingSpot(AgeableMob offspring) {
        Level level = offspring.level();
        BlockPos entityPos = offspring.blockPosition();
        BlockPos bestHidingSpot = null;
        double bestHidingScore = 0.0;

        int searchRadius = (int) hidingDetectionRange / 2;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = entityPos.offset(x, y, z);
                    double score = evaluateHidingSpot(level, checkPos, entityPos);

                    if (score > bestHidingScore) {
                        bestHidingScore = score;
                        bestHidingSpot = checkPos;
                    }
                }
            }
        }

        return bestHidingSpot;
    }

    private double evaluateHidingSpot(Level level, BlockPos pos, BlockPos entityPos) {
        if (!isValidHidingSpot(level, pos)) {
            return 0.0;
        }

        double score = 0.0;
        BlockState blockState = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());

        if (isConcealingGrass(blockState)) {
            score += 10.0;
        }

        if (aboveState.is(Blocks.TALL_GRASS) ||
            aboveState.is(Blocks.LARGE_FERN) ||
            aboveState.is(Blocks.PEONY) ||
            aboveState.is(Blocks.SUNFLOWER) ||
            aboveState.is(Blocks.LILAC) ||
            aboveState.is(Blocks.ROSE_BUSH)) {
            score += 15.0;
        }

        if (aboveState.is(Blocks.OAK_LEAVES) ||
            aboveState.is(Blocks.BIRCH_LEAVES) ||
            aboveState.is(Blocks.SPRUCE_LEAVES) ||
            aboveState.is(Blocks.JUNGLE_LEAVES) ||
            aboveState.is(Blocks.ACACIA_LEAVES) ||
            aboveState.is(Blocks.DARK_OAK_LEAVES)) {
            score += 20.0;
        }

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.is(Blocks.GRASS_BLOCK) ||
            belowState.is(Blocks.DIRT) ||
            belowState.is(Blocks.PODZOL) ||
            belowState.is(Blocks.COARSE_DIRT)) {
            score += 5.0;
        }

        double distanceFromEntity = Math.sqrt(pos.distSqr(entityPos));
        if (distanceFromEntity > 8.0) {
            score *= 0.5;
        }

        int nearbyConcealment = countConcealingBlocks(level, pos, 3);
        score += nearbyConcealment * 2.0;

        return score;
    }

    private boolean isValidHidingSpot(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());
        BlockState belowState = level.getBlockState(pos.below());

        if (!blockState.isAir()) {
            return false;
        }

        if (!aboveState.isAir()) {
            return isConcealingBlock(aboveState);
        }

        return belowState.isSolidRender(level, pos);
    }

    private boolean isConcealingGrass(BlockState blockState) {
        return blockState.is(Blocks.SHORT_GRASS) ||
               blockState.is(Blocks.FERN) ||
               blockState.is(Blocks.DEAD_BUSH);
    }

    private boolean isConcealingBlock(BlockState blockState) {
        return isConcealingGrass(blockState) ||
               blockState.is(Blocks.TALL_GRASS) ||
               blockState.is(Blocks.LARGE_FERN) ||
               blockState.is(Blocks.PEONY) ||
               blockState.is(Blocks.SUNFLOWER) ||
               blockState.is(Blocks.LILAC) ||
               blockState.is(Blocks.ROSE_BUSH) ||
               blockState.is(Blocks.OAK_LEAVES) ||
               blockState.is(Blocks.BIRCH_LEAVES) ||
               blockState.is(Blocks.SPRUCE_LEAVES) ||
               blockState.is(Blocks.JUNGLE_LEAVES) ||
               blockState.is(Blocks.ACACIA_LEAVES) ||
               blockState.is(Blocks.DARK_OAK_LEAVES);
    }

    private int countConcealingBlocks(Level level, BlockPos center, int radius) {
        int count = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    mutablePos.set(center).move(x, y, z);
                    BlockState blockState = level.getBlockState(mutablePos);

                    if (isConcealingBlock(blockState)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private Entity findMother(AgeableMob offspring) {
        Level level = offspring.level();
        if (motherUuid != null) {
            // Find entity by UUID by searching nearby entities
            // Note: Level.getAllEntities() doesn't exist, search nearby instead
            for (Entity entity : level.getEntitiesOfClass(
                    Entity.class,
                    offspring.getBoundingBox().inflate(64.0))) {
                if (entity.getUUID().equals(motherUuid) && entity.isAlive()) {
                    return entity;
                }
            }
        }

        // getParent() doesn't exist on AgeableMob, skip this check
        return findNearestAdultOfSameSpecies(offspring);
    }

    private Entity findNearestAdultOfSameSpecies(AgeableMob offspring) {
        Level level = offspring.level();
        Vec3 offspringPos = offspring.position();
        Entity nearestAdult = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : level.getEntitiesOfClass(offspring.getClass(),
                offspring.getBoundingBox().inflate(32.0))) {
            if (entity instanceof AgeableMob adult && !adult.isBaby() && adult.isAlive()) {
                double distance = offspringPos.distanceTo(entity.position());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestAdult = entity;
                }
            }
        }

        return nearestAdult;
    }

    public boolean isHiding() {
        return isHiding;
    }

    public void setHiding(boolean hiding) {
        this.isHiding = hiding;
    }

    public BlockPos getHidingPosition() {
        return hidingPosition;
    }

    public void setMotherUuid(UUID motherUuid) {
        this.motherUuid = motherUuid;
    }

    public void setMotherReturnThreshold(double motherReturnThreshold) {
        this.motherReturnThreshold = motherReturnThreshold;
    }

    public double getMotherReturnThreshold() {
        return motherReturnThreshold;
    }

    public void setMaxHideDuration(int maxHideDuration) {
        this.maxHideDuration = maxHideDuration;
    }

    public int getMaxHideDuration() {
        return maxHideDuration;
    }
}
