package me.javavirtualenv.behavior.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal implementing bee waggle dance behavior based on Karl von Frisch's research.
 *
 * When a bee discovers a high-quality flower patch (5+ flowers in 10-block radius),
 * it returns to its hive and performs a waggle dance near the entrance. This dance
 * communicates the location of the flowers to other bees within a 12-block radius,
 * recruiting them to forage at that location.
 *
 * The dance duration and intensity scale with flower patch quality (number of flowers).
 */
public class BeeWaggleDanceGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeeWaggleDanceGoal.class);

    private static final int MIN_FLOWERS_FOR_DANCE = 5;
    private static final int FLOWER_SEARCH_RADIUS = 10;
    private static final int MIN_DANCE_DURATION_TICKS = 100;
    private static final int MAX_DANCE_DURATION_TICKS = 200;
    private static final double HIVE_DANCE_DISTANCE = 2.0;
    private static final double RECRUITMENT_RADIUS = 12.0;
    private static final int SEARCH_COOLDOWN_TICKS = 100;
    private static final int DANCE_COOLDOWN_TICKS = 600;

    public static final AttachmentType<Long> LAST_DANCE_TIME_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "bee_last_dance_time"),
        builder -> builder
            .initializer(() -> 0L)
            .persistent(Codec.LONG)
    );

    private final Bee bee;
    private final Level level;

    @Nullable
    private BlockPos flowerPatchCenter;
    private int flowerCount;
    private int danceTicks;
    private int danceMaxTicks;
    private int searchCooldown;
    @Nullable
    private BlockPos dancePosition;

    public BeeWaggleDanceGoal(Bee bee) {
        this.bee = bee;
        this.level = bee.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        if (!canDanceAgain()) {
            return false;
        }

        if (!hasHive()) {
            return false;
        }

        BlockPos flowerPatch = findHighQualityFlowerPatch();
        if (flowerPatch == null) {
            this.searchCooldown = reducedTickDelay(SEARCH_COOLDOWN_TICKS);
            return false;
        }

        this.flowerPatchCenter = flowerPatch;
        LOGGER.debug("{} found high-quality flower patch at {} with {} flowers",
            this.bee.getName().getString(), flowerPatch, this.flowerCount);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.danceTicks > 0 && this.danceTicks < this.danceMaxTicks) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.danceMaxTicks = calculateDanceDuration(this.flowerCount);
        this.danceTicks = 0;

        BlockPos hivePos = getHivePosition();
        if (hivePos != null) {
            this.dancePosition = findDancePosition(hivePos);
            navigateToDancePosition();
        }

        LOGGER.debug("{} starting waggle dance for {} ticks to recruit foragers",
            this.bee.getName().getString(), this.danceMaxTicks);
    }

    @Override
    public void stop() {
        if (this.flowerPatchCenter != null) {
            long currentTime = this.level.getGameTime();
            this.bee.setAttached(LAST_DANCE_TIME_ATTACHMENT, currentTime);

            LOGGER.debug("{} finished waggle dance at tick {}",
                this.bee.getName().getString(), currentTime);
        }

        this.flowerPatchCenter = null;
        this.flowerCount = 0;
        this.danceTicks = 0;
        this.dancePosition = null;
        this.bee.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.dancePosition == null || this.flowerPatchCenter == null) {
            return;
        }

        if (!isAtDancePosition()) {
            if (this.bee.getNavigation().isDone()) {
                navigateToDancePosition();
            }
            return;
        }

        this.danceTicks++;
        performDanceAnimation();

        if (this.danceTicks % 20 == 0) {
            recruitNearbyBees();
        }
    }

    private boolean hasHive() {
        BlockPos hivePos = getHivePosition();
        return hivePos != null;
    }

    @Nullable
    private BlockPos getHivePosition() {
        return this.bee.getHivePos();
    }

    private boolean canDanceAgain() {
        long lastDanceTime = this.bee.getAttachedOrCreate(LAST_DANCE_TIME_ATTACHMENT);
        long currentTime = this.level.getGameTime();
        return (currentTime - lastDanceTime) >= DANCE_COOLDOWN_TICKS;
    }

    @Nullable
    private BlockPos findHighQualityFlowerPatch() {
        BlockPos beePos = this.bee.blockPosition();
        AABB searchBox = new AABB(beePos).inflate(FLOWER_SEARCH_RADIUS);

        int maxFlowers = 0;
        BlockPos bestPatch = null;

        for (BlockPos pos : BlockPos.betweenClosed(
            (int) searchBox.minX, (int) searchBox.minY, (int) searchBox.minZ,
            (int) searchBox.maxX, (int) searchBox.maxY, (int) searchBox.maxZ)) {

            int localFlowerCount = countFlowersNear(pos.immutable());
            if (localFlowerCount > maxFlowers) {
                maxFlowers = localFlowerCount;
                bestPatch = pos.immutable();
            }
        }

        if (maxFlowers >= MIN_FLOWERS_FOR_DANCE) {
            this.flowerCount = maxFlowers;
            return bestPatch;
        }

        return null;
    }

    private int countFlowersNear(BlockPos center) {
        int count = 0;
        int radius = 3;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.getX() - radius, center.getY() - 1, center.getZ() - radius,
            center.getX() + radius, center.getY() + 1, center.getZ() + radius)) {

            BlockState state = this.level.getBlockState(pos);
            if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.TALL_FLOWERS)) {
                count++;
            }
        }

        return count;
    }

    private int calculateDanceDuration(int flowers) {
        float quality = (float) flowers / (MIN_FLOWERS_FOR_DANCE * 3);
        quality = Math.min(1.0f, quality);

        int duration = (int) (MIN_DANCE_DURATION_TICKS +
            (MAX_DANCE_DURATION_TICKS - MIN_DANCE_DURATION_TICKS) * quality);

        return duration;
    }

    @Nullable
    private BlockPos findDancePosition(BlockPos hivePos) {
        Vec3 hiveCenter = hivePos.getCenter();
        double offsetX = (this.bee.getRandom().nextDouble() - 0.5) * HIVE_DANCE_DISTANCE * 2;
        double offsetZ = (this.bee.getRandom().nextDouble() - 0.5) * HIVE_DANCE_DISTANCE * 2;

        return BlockPos.containing(
            hiveCenter.x + offsetX,
            hiveCenter.y,
            hiveCenter.z + offsetZ
        );
    }

    private boolean isAtDancePosition() {
        if (this.dancePosition == null) {
            return false;
        }

        double distanceSq = this.bee.position().distanceToSqr(this.dancePosition.getCenter());
        return distanceSq <= HIVE_DANCE_DISTANCE * HIVE_DANCE_DISTANCE;
    }

    private void navigateToDancePosition() {
        if (this.dancePosition == null) {
            return;
        }

        this.bee.getNavigation().moveTo(
            this.dancePosition.getX() + 0.5,
            this.dancePosition.getY() + 0.5,
            this.dancePosition.getZ() + 0.5,
            1.0
        );
    }

    private void performDanceAnimation() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        double angle = (this.danceTicks * 0.3) % (Math.PI * 2);
        double radius = 0.3 + Math.sin(this.danceTicks * 0.15) * 0.1;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;

        Vec3 beePos = this.bee.position();
        double particleX = beePos.x + offsetX;
        double particleY = beePos.y + 0.3;
        double particleZ = beePos.z + offsetZ;

        if (this.danceTicks % 5 == 0) {
            serverLevel.sendParticles(
                ParticleTypes.NOTE,
                particleX,
                particleY,
                particleZ,
                1,
                0, 0, 0,
                0
            );
        }

        if (this.danceTicks % 8 == 0) {
            serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                particleX,
                particleY,
                particleZ,
                2,
                0.2, 0.2, 0.2,
                0.02
            );
        }

        if (this.danceTicks % 15 == 0) {
            this.level.playSound(
                null,
                this.bee.getX(),
                this.bee.getY(),
                this.bee.getZ(),
                SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.NEUTRAL,
                0.3F,
                1.5F + this.bee.getRandom().nextFloat() * 0.3F
            );
        }

        double waggleX = beePos.x + Math.sin(this.danceTicks * 0.5) * 0.15;
        double waggleZ = beePos.z + Math.cos(this.danceTicks * 0.5) * 0.15;
        this.bee.getLookControl().setLookAt(waggleX, beePos.y, waggleZ);
    }

    private void recruitNearbyBees() {
        if (this.flowerPatchCenter == null) {
            return;
        }

        AABB recruitmentBox = this.bee.getBoundingBox().inflate(RECRUITMENT_RADIUS);
        List<Bee> nearbyBees = this.level.getEntitiesOfClass(
            Bee.class,
            recruitmentBox,
            this::isRecruitableBee
        );

        for (Bee recruitedBee : nearbyBees) {
            recruitBee(recruitedBee);
        }

        if (!nearbyBees.isEmpty()) {
            LOGGER.debug("{} recruited {} bees to forage at {}",
                this.bee.getName().getString(), nearbyBees.size(), this.flowerPatchCenter);
        }
    }

    private boolean isRecruitableBee(Bee otherBee) {
        if (otherBee == this.bee) {
            return false;
        }

        if (!otherBee.isAlive()) {
            return false;
        }

        BlockPos existingTarget = otherBee.getAttachedOrCreate(
            BeeRecruitedForagingGoal.RECRUITED_FLOWER_POS_ATTACHMENT
        );

        if (existingTarget != null && !existingTarget.equals(BlockPos.ZERO)) {
            return false;
        }

        return true;
    }

    private void recruitBee(Bee recruitedBee) {
        if (this.flowerPatchCenter == null) {
            return;
        }

        recruitedBee.setAttached(
            BeeRecruitedForagingGoal.RECRUITED_FLOWER_POS_ATTACHMENT,
            this.flowerPatchCenter
        );

        long currentTime = this.level.getGameTime();
        recruitedBee.setAttached(
            BeeRecruitedForagingGoal.RECRUITED_TIME_ATTACHMENT,
            currentTime
        );
    }
}
