package me.javavirtualenv.behavior.sniffer;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Base behavior class for sniffer-specific behaviors.
 * Provides common utilities for seed detection, scent tracking, and digging site evaluation.
 */
public abstract class SnifferBehavior extends SteeringBehavior {
    protected final double smellRadius;
    protected final int scentPersistenceTicks;
    protected final int seedMemorySize;

    protected List<ScentMarker> detectedScents;
    protected List<BlockPos> diggingMemory;
    protected BlockPos currentScentTarget;

    public SnifferBehavior(double smellRadius, int scentPersistenceTicks, int seedMemorySize) {
        this.smellRadius = smellRadius;
        this.scentPersistenceTicks = scentPersistenceTicks;
        this.seedMemorySize = seedMemorySize;
        this.detectedScents = new ArrayList<>();
        this.diggingMemory = new ArrayList<>();
    }

    public SnifferBehavior() {
        this(24.0, 1200, 10);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!(context.getEntity() instanceof Sniffer)) {
            return new Vec3d();
        }

        updateScentMarkers(context);
        updateDiggingMemory(context);

        return calculateSnifferBehavior(context);
    }

    protected abstract Vec3d calculateSnifferBehavior(BehaviorContext context);

    protected void updateScentMarkers(BehaviorContext context) {
        Level level = context.getLevel();
        Vec3d position = context.getPosition();

        detectedScents.removeIf(scent -> {
            if (!scent.isValid(level)) {
                return true;
            }
            scent.age++;
            return scent.age > scentPersistenceTicks;
        });

        detectNewScents(context, position);
    }

    protected void detectNewScents(BehaviorContext context, Vec3d position) {
        Level level = context.getLevel();
        BlockPos center = context.getBlockPos();

        for (BlockPos pos : BlockPos.betweenClosed(
            center.getX() - 8, center.getY() - 3, center.getZ() - 8,
            center.getX() + 8, center.getY() + 2, center.getZ() + 8
        )) {
            if (containsSeedScent(level, pos)) {
                Vec3d scentPos = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                double distance = position.distanceTo(scentPos);

                if (distance <= smellRadius) {
                    boolean alreadyDetected = detectedScents.stream()
                        .anyMatch(scent -> scent.position.equals(scentPos));

                    if (!alreadyDetected) {
                        detectedScents.add(new ScentMarker(scentPos, pos, 0));
                    }
                }
            }
        }
    }

    protected boolean containsSeedScent(Level level, BlockPos pos) {
        return isDiggableBlock(level, pos);
    }

    protected boolean isDiggableBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DIRT) ||
               level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
               level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.PODZOL) ||
               level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.COARSE_DIRT) ||
               level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.MYCELIUM) ||
               level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT);
    }

    protected void updateDiggingMemory(BehaviorContext context) {
        diggingMemory.removeIf(pos -> {
            if (!isDiggableBlock(context.getLevel(), pos)) {
                return true;
            }
            return context.getBlockPos().distSqr(pos) > 256.0;
        });

        if (diggingMemory.size() > seedMemorySize) {
            diggingMemory = new ArrayList<>(diggingMemory.subList(0, seedMemorySize));
        }
    }

    protected BlockPos findNearestScent(BehaviorContext context) {
        Vec3d position = context.getPosition();
        ScentMarker nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ScentMarker scent : detectedScents) {
            double distance = position.distanceTo(scent.position);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = scent;
            }
        }

        return nearest != null ? nearest.blockPosition : null;
    }

    protected BlockPos findBestDiggingSite(BehaviorContext context) {
        BlockPos center = context.getBlockPos();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.getX() - 12, center.getY() - 4, center.getZ() - 12,
            center.getX() + 12, center.getY() + 2, center.getZ() + 12
        )) {
            if (!isDiggableBlock(context.getLevel(), pos)) {
                continue;
            }

            double score = evaluateDiggingSite(context, pos);
            if (score < bestScore) {
                bestScore = score;
                best = pos;
            }
        }

        return best;
    }

    protected double evaluateDiggingSite(BehaviorContext context, BlockPos pos) {
        double distanceScore = context.getBlockPos().distSqr(pos);
        double memoryScore = diggingMemory.contains(pos) ? 100.0 : 0.0;
        return distanceScore + memoryScore;
    }

    protected void addToDiggingMemory(BlockPos pos) {
        if (!diggingMemory.contains(pos)) {
            diggingMemory.add(pos.immutable());
        }
    }

    public List<ScentMarker> getDetectedScents() {
        return new ArrayList<>(detectedScents);
    }

    public List<BlockPos> getDiggingMemory() {
        return new ArrayList<>(diggingMemory);
    }

    public BlockPos getCurrentScentTarget() {
        return currentScentTarget;
    }

    public void setCurrentScentTarget(BlockPos pos) {
        this.currentScentTarget = pos;
    }

    protected static class ScentMarker {
        final Vec3d position;
        final BlockPos blockPosition;
        int age;

        ScentMarker(Vec3d position, BlockPos blockPosition, int age) {
            this.position = position;
            this.blockPosition = blockPosition;
            this.age = age;
        }

        boolean isValid(Level level) {
            return level.isLoaded(blockPosition) &&
                   level.getBlockState(blockPosition).isAir();
        }

        double getStrength() {
            return 1.0 - (age / 1200.0);
        }
    }
}
