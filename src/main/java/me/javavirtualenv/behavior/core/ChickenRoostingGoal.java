package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that makes chickens seek elevated perches to roost at night.
 *
 * <p>Scientific basis: Chickens instinctively seek elevated positions at dusk
 * for antipredator defense while sleeping. In nature, they roost in trees; in
 * domestic settings, they use fences, logs, and other elevated structures.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates at dusk/night (13000-23000 ticks)</li>
 *   <li>Pathfinds to fences, logs, walls (elevated positions)</li>
 *   <li>Prefers height 2-3 blocks off ground</li>
 *   <li>Stays until dawn</li>
 *   <li>Multiple chickens roost together</li>
 * </ul>
 */
public class ChickenRoostingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChickenRoostingGoal.class);

    private static final long DUSK_TIME = 13000;
    private static final long DAWN_TIME = 23000;
    private static final int SEARCH_RADIUS = 16;
    private static final int MIN_ROOST_HEIGHT = 2;
    private static final int MAX_ROOST_HEIGHT = 3;
    private static final double ACCEPTED_DISTANCE = 1.0;
    private static final int SEARCH_COOLDOWN_TICKS = 100;

    private final Mob chicken;
    private final Level level;

    private BlockPos roostPos;
    private int searchCooldown;
    private boolean isRoosting;

    public ChickenRoostingGoal(Mob chicken) {
        this.chicken = chicken;
        this.level = chicken.level();
        this.searchCooldown = 0;
        this.isRoosting = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!isNightTime()) {
            return false;
        }

        if (isAlreadyRoosting()) {
            return true;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        boolean foundRoost = findNearestRoost();
        if (!foundRoost) {
            this.searchCooldown = SEARCH_COOLDOWN_TICKS;
        }

        return foundRoost;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isNightTime()) {
            LOGGER.debug("{} stopping roosting - dawn has arrived",
                this.chicken.getName().getString());
            return false;
        }

        if (this.isRoosting) {
            return true;
        }

        if (this.roostPos == null) {
            return false;
        }

        if (!isValidRoostBlock(this.roostPos)) {
            LOGGER.debug("Roost at {} is no longer valid", this.roostPos);
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.isRoosting = false;

        if (this.roostPos != null) {
            LOGGER.debug("{} starting to seek roost at {}",
                this.chicken.getName().getString(), this.roostPos);
            navigateToRoost();
        }
    }

    @Override
    public void stop() {
        this.roostPos = null;
        this.isRoosting = false;
        this.chicken.getNavigation().stop();

        LOGGER.debug("{} stopped roosting", this.chicken.getName().getString());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.roostPos == null) {
            return;
        }

        if (isNearRoost()) {
            performRoosting();
        } else {
            if (!this.isRoosting && shouldRecalculatePath()) {
                navigateToRoost();
            }
        }
    }

    private boolean isNightTime() {
        long timeOfDay = this.level.getDayTime() % 24000;
        return timeOfDay >= DUSK_TIME || timeOfDay < DAWN_TIME;
    }

    private boolean isAlreadyRoosting() {
        if (!this.isRoosting) {
            return false;
        }

        BlockPos currentPos = this.chicken.blockPosition();
        return isValidRoostBlock(currentPos.below()) || isValidRoostBlock(currentPos);
    }

    private boolean findNearestRoost() {
        BlockPos chickenPos = this.chicken.blockPosition();
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        BlockPos closestRoost = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = MIN_ROOST_HEIGHT; y <= MAX_ROOST_HEIGHT; y++) {
                    searchPos.set(chickenPos.getX() + x, chickenPos.getY() + y, chickenPos.getZ() + z);

                    if (isValidRoostPosition(searchPos)) {
                        double distSq = chickenPos.distSqr(searchPos);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closestRoost = searchPos.immutable();
                        }
                    }
                }
            }
        }

        if (closestRoost != null) {
            this.roostPos = closestRoost;
            LOGGER.debug("{} found roost at {} (distance: {})",
                this.chicken.getName().getString(), closestRoost, Math.sqrt(closestDistSq));
            return true;
        }

        LOGGER.debug("{} could not find roost within {} blocks",
            this.chicken.getName().getString(), SEARCH_RADIUS);
        return false;
    }

    private boolean isValidRoostPosition(BlockPos pos) {
        if (!isValidRoostBlock(pos)) {
            return false;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = this.level.getBlockState(abovePos);
        if (!aboveState.isAir() && !aboveState.getCollisionShape(this.level, abovePos).isEmpty()) {
            return false;
        }

        int heightAboveGround = getHeightAboveGround(pos);
        if (heightAboveGround < MIN_ROOST_HEIGHT || heightAboveGround > MAX_ROOST_HEIGHT) {
            return false;
        }

        return true;
    }

    private boolean isValidRoostBlock(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof FenceBlock) {
            return true;
        }

        if (block instanceof WallBlock) {
            return true;
        }

        if (block == Blocks.OAK_LOG || block == Blocks.STRIPPED_OAK_LOG ||
            block == Blocks.BIRCH_LOG || block == Blocks.STRIPPED_BIRCH_LOG ||
            block == Blocks.SPRUCE_LOG || block == Blocks.STRIPPED_SPRUCE_LOG ||
            block == Blocks.JUNGLE_LOG || block == Blocks.STRIPPED_JUNGLE_LOG ||
            block == Blocks.DARK_OAK_LOG || block == Blocks.STRIPPED_DARK_OAK_LOG ||
            block == Blocks.ACACIA_LOG || block == Blocks.STRIPPED_ACACIA_LOG ||
            block == Blocks.CHERRY_LOG || block == Blocks.STRIPPED_CHERRY_LOG ||
            block == Blocks.MANGROVE_LOG || block == Blocks.STRIPPED_MANGROVE_LOG) {
            return true;
        }

        if (block == Blocks.OAK_FENCE || block == Blocks.BIRCH_FENCE ||
            block == Blocks.SPRUCE_FENCE || block == Blocks.JUNGLE_FENCE ||
            block == Blocks.DARK_OAK_FENCE || block == Blocks.ACACIA_FENCE ||
            block == Blocks.CHERRY_FENCE || block == Blocks.MANGROVE_FENCE ||
            block == Blocks.BAMBOO_FENCE || block == Blocks.NETHER_BRICK_FENCE) {
            return true;
        }

        return false;
    }

    private int getHeightAboveGround(BlockPos roostPos) {
        BlockPos.MutableBlockPos checkPos = roostPos.mutable();

        for (int i = 0; i <= MAX_ROOST_HEIGHT + 2; i++) {
            checkPos.move(0, -1, 0);

            BlockState state = this.level.getBlockState(checkPos);
            if (!state.isAir() && !state.getCollisionShape(this.level, checkPos).isEmpty()) {
                if (!isValidRoostBlock(checkPos)) {
                    return roostPos.getY() - checkPos.getY();
                }
            }
        }

        return 0;
    }

    private boolean isNearRoost() {
        if (this.roostPos == null) {
            return false;
        }

        return this.chicken.position().closerThan(this.roostPos.getCenter(), ACCEPTED_DISTANCE);
    }

    private void navigateToRoost() {
        if (this.roostPos == null) {
            return;
        }

        this.chicken.getNavigation().moveTo(
            this.roostPos.getX() + 0.5,
            this.roostPos.getY() + 1.0,
            this.roostPos.getZ() + 0.5,
            1.0
        );
    }

    private void performRoosting() {
        if (!this.isRoosting) {
            this.isRoosting = true;
            this.chicken.getNavigation().stop();
            LOGGER.debug("{} is now roosting at {}",
                this.chicken.getName().getString(), this.roostPos);
        }

        this.chicken.getNavigation().stop();

        double targetX = this.roostPos.getX() + 0.5;
        double targetZ = this.roostPos.getZ() + 0.5;

        double deltaX = targetX - this.chicken.getX();
        double deltaZ = targetZ - this.chicken.getZ();
        double distSq = deltaX * deltaX + deltaZ * deltaZ;

        if (distSq > 0.01) {
            this.chicken.setPos(targetX, this.chicken.getY(), targetZ);
        }

        this.chicken.setYRot(this.chicken.getYRot() + (this.chicken.getRandom().nextFloat() - 0.5f) * 2.0f);
    }

    private boolean shouldRecalculatePath() {
        return this.chicken.tickCount % 40 == 0;
    }
}
