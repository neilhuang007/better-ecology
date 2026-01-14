package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes salmon swim upstream against water currents toward river sources.
 *
 * <p>Scientific Basis:
 * Salmon are famous for their upstream migration behavior, swimming against currents
 * to reach spawning grounds. This behavior is triggered by environmental cues and
 * reproductive readiness.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Triggers randomly (5% chance) when in river biomes or near gravel</li>
 *   <li>Detects water current direction from flowing water blocks</li>
 *   <li>Swims directly against current at 1.5x normal speed</li>
 *   <li>Persists for 5-10 minutes of game time</li>
 *   <li>Higher priority than schooling behavior</li>
 * </ul>
 */
public class SalmonUpstreamMigrationGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalmonUpstreamMigrationGoal.class);

    private static final double TRIGGER_CHANCE = 0.05;
    private static final double SPEED_MULTIPLIER = 1.5;
    private static final int MIN_DURATION = 6000; // 5 minutes
    private static final int MAX_DURATION = 12000; // 10 minutes
    private static final int CHECK_INTERVAL = 20; // Check flow direction every second
    private static final int GRAVEL_SEARCH_RADIUS = 8;

    private final Salmon salmon;
    private int migrationTicks;
    private int maxMigrationTicks;
    private int flowCheckCooldown;
    private Vec3 currentFlowDirection;
    private Vec3 targetDirection;

    public SalmonUpstreamMigrationGoal(Salmon salmon) {
        this.salmon = salmon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!salmon.isInWater()) {
            return false;
        }

        if (!shouldTriggerMigration()) {
            return false;
        }

        if (!detectWaterFlow()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!salmon.isInWater()) {
            return false;
        }

        if (migrationTicks >= maxMigrationTicks) {
            LOGGER.debug("Salmon {} completed migration after {} ticks", salmon.getName().getString(), migrationTicks);
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        maxMigrationTicks = MIN_DURATION + salmon.getRandom().nextInt(MAX_DURATION - MIN_DURATION);
        migrationTicks = 0;
        flowCheckCooldown = 0;
        LOGGER.debug("Salmon {} starting upstream migration for {} ticks", salmon.getName().getString(), maxMigrationTicks);
    }

    @Override
    public void stop() {
        LOGGER.debug("Salmon {} stopped upstream migration after {} ticks", salmon.getName().getString(), migrationTicks);
        currentFlowDirection = null;
        targetDirection = null;
        salmon.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        migrationTicks++;
        flowCheckCooldown--;

        if (flowCheckCooldown <= 0) {
            flowCheckCooldown = CHECK_INTERVAL;
            updateFlowDirection();
        }

        if (targetDirection != null) {
            swimAgainstCurrent();
        }
    }

    /**
     * Determines if migration should be triggered based on environmental conditions.
     */
    private boolean shouldTriggerMigration() {
        if (salmon.getRandom().nextDouble() > TRIGGER_CHANCE) {
            return false;
        }

        Level level = salmon.level();
        BlockPos pos = salmon.blockPosition();

        boolean inRiverBiome = level.getBiome(pos).is(BiomeTags.IS_RIVER);
        boolean nearGravel = isNearGravel();

        return inRiverBiome || nearGravel;
    }

    /**
     * Checks if salmon is near gravel blocks (potential spawning site).
     */
    private boolean isNearGravel() {
        BlockPos salmonPos = salmon.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = -GRAVEL_SEARCH_RADIUS; x <= GRAVEL_SEARCH_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -GRAVEL_SEARCH_RADIUS; z <= GRAVEL_SEARCH_RADIUS; z++) {
                    mutablePos.set(salmonPos.getX() + x, salmonPos.getY() + y, salmonPos.getZ() + z);
                    BlockState state = salmon.level().getBlockState(mutablePos);
                    if (state.is(net.minecraft.world.level.block.Blocks.GRAVEL)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Detects water flow direction from surrounding flowing water blocks.
     */
    private boolean detectWaterFlow() {
        BlockPos pos = salmon.blockPosition();
        Vec3 flowSum = Vec3.ZERO;
        int flowingBlocks = 0;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState fluidState = salmon.level().getFluidState(neighborPos);

            if (fluidState.is(FluidTags.WATER)) {
                Vec3 flow = fluidState.getFlow(salmon.level(), neighborPos);
                if (flow.lengthSqr() > 0.001) {
                    flowSum = flowSum.add(flow);
                    flowingBlocks++;
                }
            }
        }

        if (flowingBlocks == 0) {
            return false;
        }

        currentFlowDirection = flowSum.normalize();
        targetDirection = currentFlowDirection.scale(-1.0);

        return true;
    }

    /**
     * Updates the flow direction periodically.
     */
    private void updateFlowDirection() {
        detectWaterFlow();
    }

    /**
     * Swims the salmon against the current.
     */
    private void swimAgainstCurrent() {
        Vec3 currentPos = salmon.position();
        Vec3 targetPos = currentPos.add(targetDirection.scale(2.0));

        salmon.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z);

        salmon.getNavigation().moveTo(
            targetPos.x,
            targetPos.y,
            targetPos.z,
            SPEED_MULTIPLIER
        );

        Vec3 movement = targetDirection.scale(0.1 * SPEED_MULTIPLIER);
        salmon.setDeltaMovement(salmon.getDeltaMovement().add(movement));
    }
}
