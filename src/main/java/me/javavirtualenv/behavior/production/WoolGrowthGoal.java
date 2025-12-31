package me.javavirtualenv.behavior.production;

import java.util.EnumSet;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.production.WoolGrowthHandle;
import me.javavirtualenv.ecology.handles.production.WoolGrowthHandle.WoolQuality;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * AI goal for wool growth-related behaviors:
 * - Seeking grass to improve wool quality/growth
 * - Shelter seeking during cold for thick coat
 * - Social grooming with other sheep
 * - Response to players with shears
 * - Seeking shade if overheating (overgrown wool in summer)
 */
public class WoolGrowthGoal extends Goal {
    private final PathfinderMob mob;
    private final Level level;
    private final EcologyComponent component;
    private final double searchRadius;
    private final double speedModifier;

    private BlockPos targetGrassPos;
    private BlockPos targetShelterPos;
    private Player targetShearsPlayer;
    private BehaviorState currentState;
    private int behaviorCooldown;
    private int socialGroomingTimer;

    public WoolGrowthGoal(PathfinderMob mob, EcologyComponent component, double searchRadius, double speedModifier) {
        this.mob = mob;
        this.level = mob.level();
        this.component = component;
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.currentState = BehaviorState.IDLE;
        this.behaviorCooldown = 0;
        this.socialGroomingTimer = 0;
    }

    @Override
    public boolean canUse() {
        if (behaviorCooldown > 0) {
            behaviorCooldown--;
            return false;
        }

        // Determine what behavior is needed
        currentState = determineNeededBehavior();

        return currentState != BehaviorState.IDLE;
    }

    @Override
    public boolean canContinueToUse() {
        if (currentState == BehaviorState.IDLE) {
            return false;
        }

        if (currentState == BehaviorState.SEEKING_GRASS) {
            return targetGrassPos != null &&
                   !mob.getNavigation().isDone() &&
                   targetGrassPos.closerToCenterThan(mob.position(), 2.0);
        }

        if (currentState == BehaviorState.SEEKING_SHELTER) {
            return targetShelterPos != null &&
                   !mob.getNavigation().isDone() &&
                   targetShelterPos.closerToCenterThan(mob.position(), 3.0);
        }

        if (currentState == BehaviorState.FLEEING_SHEARS) {
            return targetShearsPlayer != null &&
                   targetShearsPlayer.isAlive() &&
                   mob.distanceToSqr(targetShearsPlayer) < 64.0;
        }

        if (currentState == BehaviorState.SEEKING_SHADE) {
            return targetShelterPos != null &&
                   !mob.getNavigation().isDone() &&
                   targetShelterPos.closerToCenterThan(mob.position(), 2.0);
        }

        return false;
    }

    @Override
    public void start() {
        switch (currentState) {
            case SEEKING_GRASS -> {
                if (targetGrassPos != null) {
                    mob.getNavigation().moveTo(
                        targetGrassPos.getX(),
                        targetGrassPos.getY(),
                        targetGrassPos.getZ(),
                        speedModifier * 1.2
                    );
                }
            }
            case SEEKING_SHELTER -> {
                if (targetShelterPos != null) {
                    mob.getNavigation().moveTo(
                        targetShelterPos.getX(),
                        targetShelterPos.getY(),
                        targetShelterPos.getZ(),
                        speedModifier * 1.5
                    );
                }
            }
            case FLEEING_SHEARS -> {
                if (targetShearsPlayer != null) {
                    // Flee in opposite direction
                    BlockPos fleePos = getFleePosition(targetShearsPlayer);
                    mob.getNavigation().moveTo(
                        fleePos.getX(),
                        fleePos.getY(),
                        fleePos.getZ(),
                        speedModifier * 1.8
                    );
                }
            }
            case SEEKING_SHADE -> {
                if (targetShelterPos != null) {
                    mob.getNavigation().moveTo(
                        targetShelterPos.getX(),
                        targetShelterPos.getY(),
                        targetShelterPos.getZ(),
                        speedModifier * 1.3
                    );
                }
            }
        }
    }

    @Override
    public void stop() {
        targetGrassPos = null;
        targetShelterPos = null;
        targetShearsPlayer = null;
        mob.getNavigation().stop();
        currentState = BehaviorState.IDLE;
        behaviorCooldown = 200 + mob.getRandom().nextInt(200);
    }

    @Override
    public void tick() {
        switch (currentState) {
            case SEEKING_GRASS -> {
                if (targetGrassPos != null && targetGrassPos.closerToCenterThan(mob.position(), 1.5)) {
                    eatGrass();
                    stop();
                }
            }
            case SEEKING_SHELTER -> {
                if (targetShelterPos != null && targetShelterPos.closerToCenterThan(mob.position(), 2.5)) {
                    // Reached shelter, huddle with other sheep
                    initiateSocialGrooming();
                    stop();
                }
            }
            case FLEEING_SHEARS -> {
                if (mob.getRandom().nextFloat() < 0.05) {
                    // Occasional bleat while fleeing
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                                   SoundEvents.SHEEP_AMBIENT,
                                   SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            }
            case SEEKING_SHADE -> {
                if (targetShelterPos != null && targetShelterPos.closerToCenterThan(mob.position(), 2.0)) {
                    // Reached shade, rest
                    stop();
                }
            }
        }
    }

    private BehaviorState determineNeededBehavior() {
        var woolTag = component.getHandleTag("wool_growth");

        // Check if fleeing from player with shears
        Player nearbyShearsPlayer = findPlayerWithShears();
        if (nearbyShearsPlayer != null && shouldFearShears(woolTag)) {
            targetShearsPlayer = nearbyShearsPlayer;
            return BehaviorState.FLEEING_SHEARS;
        }

        // Check if overheating (overgrown wool in hot biome)
        if (isOverheating(woolTag)) {
            targetShelterPos = findShade();
            if (targetShelterPos != null) {
                return BehaviorState.SEEKING_SHADE;
            }
        }

        // Check if cold (sheared sheep in cold biome)
        if (isCold(woolTag)) {
            targetShelterPos = findShelter();
            if (targetShelterPos != null) {
                return BehaviorState.SEEKING_SHELTER;
            }
        }

        // Check if hungry and needs grass for wool growth
        if (needsGrass(woolTag)) {
            targetGrassPos = findNearbyGrass();
            if (targetGrassPos != null) {
                return BehaviorState.SEEKING_GRASS;
            }
        }

        // Social grooming if near other sheep
        if (mob.getRandom().nextFloat() < 0.01) {
            initiateSocialGrooming();
        }

        return BehaviorState.IDLE;
    }

    private Player findPlayerWithShears() {
        for (Player player : level.players()) {
            if (!player.isSpectator() && mob.distanceToSqr(player) < 64.0) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                if (mainHand.is(Items.SHEARS) || offHand.is(Items.SHEARS)) {
                    return player;
                }
            }
        }
        return null;
    }

    private boolean shouldFearShears(net.minecraft.nbt.CompoundTag woolTag) {
        // Only low-quality sheep fear shears (defensive behavior)
        WoolQuality quality = WoolGrowthHandle.getWoolQuality(woolTag);
        return quality == WoolQuality.POOR && mob.getRandom().nextFloat() < 0.3;
    }

    private boolean isOverheating(net.minecraft.nbt.CompoundTag woolTag) {
        float woolLength = WoolGrowthHandle.getWoolLength(woolTag);
        float coatThickness = WoolGrowthHandle.getCoatThickness(woolTag);

        // Overgrown wool in hot biome
        float temperature = level.getBiome(mob.blockPosition()).value().getBaseTemperature();
        return woolLength > 80.0f && coatThickness > 1.0f && temperature > 0.5;
    }

    private boolean isCold(net.minecraft.nbt.CompoundTag woolTag) {
        float woolLength = WoolGrowthHandle.getWoolLength(woolTag);

        // Sheared sheep in cold biome
        float temperature = level.getBiome(mob.blockPosition()).value().getBaseTemperature();
        return woolLength < 30.0f && temperature < 0.0;
    }

    private boolean needsGrass(net.minecraft.nbt.CompoundTag woolTag) {
        float woolLength = WoolGrowthHandle.getWoolLength(woolTag);

        // Needs grass if wool is growing and sheep is hungry
        var hungerTag = component.getHandleTag("hunger");
        int hunger = hungerTag.getInt("hunger");

        return woolLength < 100.0f && hunger < 70;
    }

    private BlockPos findNearbyGrass() {
        BlockPos mobPos = mob.blockPosition();
        BlockPos nearestGrass = null;
        double nearestDistance = Double.MAX_VALUE;

        int searchRadius = (int) this.searchRadius;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = mobPos.offset(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                        double distance = mob.distSqr(pos);
                        if (distance < nearestDistance &&
                            distance < this.searchRadius * this.searchRadius) {
                            nearestDistance = distance;
                            nearestGrass = pos;
                        }
                    }
                }
            }
        }

        return nearestGrass;
    }

    private BlockPos findShelter() {
        // Look for nearby sheep (huddle together for warmth)
        return SpatialIndex.getNearestSameTypePosition(mob, 8);
    }

    private BlockPos findShade() {
        // Look for shaded areas (under leaves or dark blocks)
        BlockPos mobPos = mob.blockPosition();
        BlockPos nearestShade = null;
        double nearestDistance = Double.MAX_VALUE;

        int searchRadius = (int) this.searchRadius;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                BlockPos pos = mobPos.offset(x, 0, z);
                BlockPos abovePos = pos.above();

                // Check if there's a block above providing shade
                if (level.getBlockState(abovePos).isAir() &&
                    (level.getBlockState(abovePos.above()).is(Blocks.OAK_LOG) ||
                     level.getBlockState(abovePos.above()).is(Blocks.OAK_LEAVES) ||
                     level.getBrightness(abovePos) < 10)) {
                    double distance = mob.distSqr(pos);
                    if (distance < nearestDistance &&
                        distance < this.searchRadius * this.searchRadius) {
                        nearestDistance = distance;
                        nearestShade = pos;
                    }
                }
            }
        }

        return nearestShade;
    }

    private BlockPos getFleePosition(Player player) {
        // Flee in opposite direction from player
        BlockPos mobPos = mob.blockPosition();
        BlockPos playerPos = player.blockPosition();

        int dx = mobPos.getX() - playerPos.getX();
        int dz = mobPos.getZ() - playerPos.getZ();

        // Normalize and extend
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 0) {
            int fleeDistance = 16;
            return mobPos.offset(
                (int) (dx / length * fleeDistance),
                0,
                (int) (dz / length * fleeDistance)
            );
        }

        return mobPos;
    }

    private void eatGrass() {
        if (targetGrassPos == null) {
            return;
        }

        // Eat grass and trigger wool growth boost
        level.destroyBlock(targetGrassPos, false);

        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                       SoundEvents.SHEEP_EAT,
                       SoundSource.NEUTRAL, 1.0F, 1.0F);

        // Spawn particles
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                double offsetX = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;
                double offsetY = serverLevel.getRandom().nextDouble() * 0.5;
                double offsetZ = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;

                serverLevel.sendParticles(
                    net.minecraft.core.particles.BlockParticle.BLOCK,
                    targetGrassPos.getX() + 0.5 + offsetX,
                    targetGrassPos.getY() + offsetY,
                    targetGrassPos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.1
                );
            }
        }

        // Boost hunger
        var hungerTag = component.getHandleTag("hunger");
        int currentHunger = hungerTag.getInt("hunger");
        hungerTag.putInt("hunger", Math.min(100, currentHunger + 15));
    }

    private void initiateSocialGrooming() {
        // Social grooming improves wool quality
        socialGroomingTimer = 100 + mob.getRandom().nextInt(200);

        // Find nearby sheep
        boolean hasNearbySheep = SpatialIndex.hasNearbySameType(mob, 4);

        if (hasNearbySheep) {
            // Boost social and slightly improve wool quality
            var socialTag = component.getHandleTag("social");
            int currentSocial = socialTag.getInt("social");
            socialTag.putInt("social", Math.min(100, currentSocial + 5));

            var woolTag = component.getHandleTag("wool_growth");
            float currentQuality = woolTag.getFloat("woolQuality");
            woolTag.putFloat("woolQuality", Math.min(100, currentQuality + 2));
        }
    }

    private enum BehaviorState {
        IDLE,
        SEEKING_GRASS,
        SEEKING_SHELTER,
        FLEEING_SHEARS,
        SEEKING_SHADE
    }
}
