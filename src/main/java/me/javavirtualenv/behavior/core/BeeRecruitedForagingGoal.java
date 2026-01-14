package me.javavirtualenv.behavior.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;

import java.util.EnumSet;

/**
 * Goal that makes recruited bees follow waggle dance directions to forage.
 *
 * When a bee is recruited through a waggle dance, it receives coordinates
 * to a high-quality flower patch. This goal makes the bee navigate to that
 * location and forage there. The recruitment expires after 60 seconds or
 * when flowers are found.
 */
public class BeeRecruitedForagingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeeRecruitedForagingGoal.class);

    private static final int RECRUITMENT_DURATION_TICKS = 1200;
    private static final double TARGET_ARRIVAL_DISTANCE = 3.0;
    private static final int FLOWER_SEARCH_RADIUS = 5;

    public static final AttachmentType<BlockPos> RECRUITED_FLOWER_POS_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "recruited_flower_pos"),
        builder -> builder
            .initializer(() -> BlockPos.ZERO)
            .persistent(BlockPos.CODEC)
    );

    public static final AttachmentType<Long> RECRUITED_TIME_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "recruited_time"),
        builder -> builder
            .initializer(() -> 0L)
            .persistent(Codec.LONG)
    );

    private final Bee bee;
    private final Level level;

    @Nullable
    private BlockPos targetFlowerPos;
    private boolean foundFlowers;

    public BeeRecruitedForagingGoal(Bee bee) {
        this.bee = bee;
        this.level = bee.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BlockPos recruitedPos = getRecruitedPosition();
        if (recruitedPos == null || recruitedPos.equals(BlockPos.ZERO)) {
            return false;
        }

        if (isRecruitmentExpired()) {
            clearRecruitment();
            return false;
        }

        this.targetFlowerPos = recruitedPos;
        this.foundFlowers = false;

        LOGGER.debug("{} following waggle dance directions to {}",
            this.bee.getName().getString(), this.targetFlowerPos);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetFlowerPos == null) {
            return false;
        }

        if (isRecruitmentExpired()) {
            return false;
        }

        if (this.foundFlowers) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        if (this.targetFlowerPos != null) {
            navigateToTarget();
        }
    }

    @Override
    public void stop() {
        if (this.foundFlowers || isRecruitmentExpired()) {
            clearRecruitment();
            LOGGER.debug("{} finished recruited foraging (found flowers: {})",
                this.bee.getName().getString(), this.foundFlowers);
        }

        this.targetFlowerPos = null;
        this.foundFlowers = false;
        this.bee.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetFlowerPos == null) {
            return;
        }

        this.bee.getLookControl().setLookAt(
            this.targetFlowerPos.getX() + 0.5,
            this.targetFlowerPos.getY() + 0.5,
            this.targetFlowerPos.getZ() + 0.5
        );

        if (hasReachedTarget()) {
            if (checkForFlowersNearby()) {
                this.foundFlowers = true;
                LOGGER.debug("{} found flowers at recruited location {}",
                    this.bee.getName().getString(), this.targetFlowerPos);
            }
        } else {
            if (this.bee.getNavigation().isDone()) {
                navigateToTarget();
            }
        }
    }

    @Nullable
    private BlockPos getRecruitedPosition() {
        BlockPos pos = this.bee.getAttachedOrCreate(RECRUITED_FLOWER_POS_ATTACHMENT);
        if (pos.equals(BlockPos.ZERO)) {
            return null;
        }
        return pos;
    }

    private boolean isRecruitmentExpired() {
        long recruitedTime = this.bee.getAttachedOrCreate(RECRUITED_TIME_ATTACHMENT);
        if (recruitedTime == 0L) {
            return true;
        }

        long currentTime = this.level.getGameTime();
        return (currentTime - recruitedTime) >= RECRUITMENT_DURATION_TICKS;
    }

    private void clearRecruitment() {
        this.bee.setAttached(RECRUITED_FLOWER_POS_ATTACHMENT, BlockPos.ZERO);
        this.bee.setAttached(RECRUITED_TIME_ATTACHMENT, 0L);
    }

    private boolean hasReachedTarget() {
        if (this.targetFlowerPos == null) {
            return false;
        }

        double distanceSq = this.bee.position().distanceToSqr(this.targetFlowerPos.getCenter());
        return distanceSq <= TARGET_ARRIVAL_DISTANCE * TARGET_ARRIVAL_DISTANCE;
    }

    private void navigateToTarget() {
        if (this.targetFlowerPos == null) {
            return;
        }

        this.bee.getNavigation().moveTo(
            this.targetFlowerPos.getX() + 0.5,
            this.targetFlowerPos.getY() + 0.5,
            this.targetFlowerPos.getZ() + 0.5,
            1.0
        );
    }

    private boolean checkForFlowersNearby() {
        if (this.targetFlowerPos == null) {
            return false;
        }

        for (BlockPos pos : BlockPos.betweenClosed(
            this.targetFlowerPos.getX() - FLOWER_SEARCH_RADIUS,
            this.targetFlowerPos.getY() - 1,
            this.targetFlowerPos.getZ() - FLOWER_SEARCH_RADIUS,
            this.targetFlowerPos.getX() + FLOWER_SEARCH_RADIUS,
            this.targetFlowerPos.getY() + 1,
            this.targetFlowerPos.getZ() + FLOWER_SEARCH_RADIUS)) {

            BlockState state = this.level.getBlockState(pos);
            if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.TALL_FLOWERS)) {
                return true;
            }
        }

        return false;
    }
}
