package me.javavirtualenv.behavior.production;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.production.WoolGrowthHandle;
import me.javavirtualenv.ecology.handles.production.WoolGrowthHandle.WoolQuality;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

import java.util.EnumSet;

/**
 * AI goal for sheep wool growth-related behaviors:
 * - Seeking grass to improve wool quality/growth
 * - Shelter seeking during cold for thick coat
 * - Social grooming with other sheep
 * - Response to players with shears
 * - Seeking shade if overheating (overgrown wool in summer)
 */
public class WoolGrowthGoal extends Goal {

    // NBT keys
    private static final String WOOL_COOLDOWN_KEY = "wool_behavior_cooldown";
    private static final String GROOMING_TIMER_KEY = "social_grooming_timer";

    // Configuration constants
    private static final double SEARCH_RADIUS = 20.0;
    private static final double SPEED_MODIFIER = 1.0;
    private static final int BEHAVIOR_COOLDOWN = 200;
    private static final int GRASS_SEARCH_RADIUS = 20;
    private static final int FLEE_DISTANCE = 16;
    private static final double STOP_DISTANCE = 2.0;

    // Instance fields
    private final PathfinderMob mob;
    private final Level level;

    private BlockPos targetGrassPos;
    private BlockPos targetShelterPos;
    private BlockPos targetShadePos;
    private Player targetShearsPlayer;
    private BehaviorState currentState;

    // Debug info
    private String lastDebugMessage = "";

    public WoolGrowthGoal(PathfinderMob mob) {
        this.mob = mob;
        this.level = mob.level();
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.currentState = BehaviorState.IDLE;
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (mob.level().isClientSide) {
            return false;
        }

        // Check cooldown
        CompoundTag woolTag = getWoolTag();
        if (woolTag != null && woolTag.contains(WOOL_COOLDOWN_KEY)) {
            int cooldown = woolTag.getInt(WOOL_COOLDOWN_KEY);
            if (cooldown > 0) {
                woolTag.putInt(WOOL_COOLDOWN_KEY, cooldown - 1);
                return false;
            }
        }

        // Determine what behavior is needed
        currentState = determineNeededBehavior();

        boolean canStart = currentState != BehaviorState.IDLE;
        if (canStart) {
            debug("STARTING: " + currentState.name().toLowerCase());
        }

        return canStart;
    }

    @Override
    public boolean canContinueToUse() {
        return switch (currentState) {
            case IDLE -> false;
            case SEEKING_GRASS -> targetGrassPos != null &&
                    mob.getNavigation().isInProgress() &&
                    !targetGrassPos.closerToCenterThan(mob.position(), STOP_DISTANCE);
            case SEEKING_SHELTER -> targetShelterPos != null &&
                    mob.getNavigation().isInProgress() &&
                    !targetShelterPos.closerToCenterThan(mob.position(), STOP_DISTANCE + 1);
            case FLEEING_SHEARS -> targetShearsPlayer != null &&
                    targetShearsPlayer.isAlive() &&
                    mob.distanceToSqr(targetShearsPlayer) < 64.0;
            case SEEKING_SHADE -> targetShadePos != null &&
                    mob.getNavigation().isInProgress() &&
                    !targetShadePos.closerToCenterThan(mob.position(), STOP_DISTANCE);
        };
    }

    @Override
    public void start() {
        double speed = switch (currentState) {
            case SEEKING_GRASS -> SPEED_MODIFIER * 1.2;
            case SEEKING_SHELTER -> SPEED_MODIFIER * 1.5;
            case FLEEING_SHEARS -> SPEED_MODIFIER * 1.8;
            case SEEKING_SHADE -> SPEED_MODIFIER * 1.3;
            default -> SPEED_MODIFIER;
        };

        BlockPos targetPos = switch (currentState) {
            case SEEKING_GRASS -> targetGrassPos;
            case SEEKING_SHELTER -> targetShelterPos;
            case SEEKING_SHADE -> targetShadePos;
            case FLEEING_SHEARS -> getFleePosition(targetShearsPlayer);
            default -> null;
        };

        if (targetPos != null) {
            mob.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
            debug("pathing to " + currentState.name().toLowerCase() + " target");
        }
    }

    @Override
    public void stop() {
        debug("goal stopped (state was " + currentState.name().toLowerCase() + ")");

        // Set cooldown in NBT
        CompoundTag woolTag = getWoolTag();
        if (woolTag != null) {
            woolTag.putInt(WOOL_COOLDOWN_KEY, BEHAVIOR_COOLDOWN + mob.getRandom().nextInt(200));
        }

        targetGrassPos = null;
        targetShelterPos = null;
        targetShadePos = null;
        targetShearsPlayer = null;
        mob.getNavigation().stop();
        currentState = BehaviorState.IDLE;
    }

    @Override
    public void tick() {
        switch (currentState) {
            case SEEKING_GRASS -> tickGrassSeeking();
            case SEEKING_SHELTER -> tickShelterSeeking();
            case FLEEING_SHEARS -> tickFleeingShears();
            case SEEKING_SHADE -> tickShadeSeeking();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Handle grass seeking behavior.
     */
    private void tickGrassSeeking() {
        if (targetGrassPos == null) {
            return;
        }

        double distance = mob.position().distanceTo(
            net.minecraft.world.phys.Vec3.atCenterOf(targetGrassPos)
        );

        mob.getLookControl().setLookAt(
            targetGrassPos.getX() + 0.5,
            targetGrassPos.getY(),
            targetGrassPos.getZ() + 0.5,
            30.0f, 30.0f
        );

        if (mob.tickCount % 20 == 0) {
            debug("moving to grass, distance=" + String.format("%.1f", distance));
        }

        if (distance < 1.5) {
            eatGrass(targetGrassPos);
            targetGrassPos = null;
        }
    }

    /**
     * Handle shelter seeking behavior.
     */
    private void tickShelterSeeking() {
        if (targetShelterPos == null) {
            return;
        }

        double distance = mob.position().distanceTo(
            net.minecraft.world.phys.Vec3.atCenterOf(targetShelterPos)
        );

        if (distance < 3.0) {
            // Reached shelter, initiate social grooming
            initiateSocialGrooming();
            targetShelterPos = null;
        }
    }

    /**
     * Handle fleeing from shears behavior.
     */
    private void tickFleeingShears() {
        if (targetShearsPlayer == null) {
            return;
        }

        // Keep moving away
        if (!mob.getNavigation().isInProgress()) {
            BlockPos fleePos = getFleePosition(targetShearsPlayer);
            mob.getNavigation().moveTo(fleePos.getX(), fleePos.getY(), fleePos.getZ(),
                SPEED_MODIFIER * 1.8);
        }

        // Occasional bleat while fleeing
        if (mob.getRandom().nextFloat() < 0.05) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.SHEEP_AMBIENT,
                    SoundSource.NEUTRAL, 1.0F, 1.2F);
        }
    }

    /**
     * Handle shade seeking behavior.
     */
    private void tickShadeSeeking() {
        if (targetShadePos == null) {
            return;
        }

        double distance = mob.position().distanceTo(
            net.minecraft.world.phys.Vec3.atCenterOf(targetShadePos)
        );

        if (distance < 2.0) {
            // Reached shade, done
            targetShadePos = null;
        }
    }

    /**
     * Determine what behavior is currently needed.
     */
    private BehaviorState determineNeededBehavior() {
        CompoundTag woolTag = getWoolTag();
        if (woolTag == null) {
            return BehaviorState.IDLE;
        }

        // Check if fleeing from player with shears
        Player nearbyShearsPlayer = findPlayerWithShears();
        if (nearbyShearsPlayer != null && shouldFearShears(woolTag)) {
            targetShearsPlayer = nearbyShearsPlayer;
            return BehaviorState.FLEEING_SHEARS;
        }

        // Check if overheating (overgrown wool in hot biome)
        if (isOverheating(woolTag)) {
            targetShadePos = findShade();
            if (targetShadePos != null) {
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

    /**
     * Find a nearby player with shears.
     */
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

    /**
     * Check if sheep should fear shears.
     */
    private boolean shouldFearShears(CompoundTag woolTag) {
        WoolQuality quality = WoolGrowthHandle.getWoolQuality(woolTag);
        return quality == WoolQuality.POOR && mob.getRandom().nextFloat() < 0.3;
    }

    /**
     * Check if sheep is overheating.
     */
    private boolean isOverheating(CompoundTag woolTag) {
        float woolLength = WoolGrowthHandle.getWoolLength(woolTag);
        float coatThickness = WoolGrowthHandle.getCoatThickness(woolTag);

        float temperature = level.getBiome(mob.blockPosition()).value().getBaseTemperature();
        return woolLength > 80.0f && coatThickness > 1.0f && temperature > 0.5;
    }

    /**
     * Check if sheep is cold.
     */
    private boolean isCold(CompoundTag woolTag) {
        float woolLength = WoolGrowthHandle.getWoolLength(woolTag);

        float temperature = level.getBiome(mob.blockPosition()).value().getBaseTemperature();
        return woolLength < 30.0f && temperature < 0.0;
    }

    /**
     * Check if sheep needs grass.
     */
    private boolean needsGrass(CompoundTag woolTag) {
        float woolLength = WoolGrowthHandle.getWoolLength(woolTag);

        CompoundTag hungerTag = getHungerTag();
        int hunger = hungerTag != null ? hungerTag.getInt("hunger") : 100;

        return woolLength < 100.0f && hunger < 70;
    }

    /**
     * Find nearby grass to eat.
     */
    private BlockPos findNearbyGrass() {
        BlockPos mobPos = mob.blockPosition();
        BlockPos nearestGrass = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -GRASS_SEARCH_RADIUS; x <= GRASS_SEARCH_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -GRASS_SEARCH_RADIUS; z <= GRASS_SEARCH_RADIUS; z++) {
                    BlockPos pos = mobPos.offset(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                        double distance = mob.blockPosition().distSqr(pos);
                        if (distance < nearestDist) {
                            nearestDist = distance;
                            nearestGrass = pos;
                        }
                    }
                }
            }
        }

        return nearestGrass;
    }

    /**
     * Find shelter (other sheep to huddle with).
     */
    private BlockPos findShelter() {
        var nearbySheep = SpatialIndex.getNearbySameType(mob, 8);
        if (!nearbySheep.isEmpty()) {
            return nearbySheep.get(0).blockPosition();
        }
        return null;
    }

    /**
     * Find shade.
     */
    private BlockPos findShade() {
        BlockPos mobPos = mob.blockPosition();
        BlockPos nearestShade = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -GRASS_SEARCH_RADIUS; x <= GRASS_SEARCH_RADIUS; x++) {
            for (int z = -GRASS_SEARCH_RADIUS; z <= GRASS_SEARCH_RADIUS; z++) {
                BlockPos pos = mobPos.offset(x, 0, z);
                BlockPos abovePos = pos.above();

                if (level.getBlockState(abovePos).isAir()) {
                    boolean hasShade = level.getBlockState(abovePos.above()).is(Blocks.OAK_LOG) ||
                            level.getBlockState(abovePos.above()).is(Blocks.OAK_LEAVES) ||
                            level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, abovePos) < 10;

                    if (hasShade) {
                        double distance = mob.blockPosition().distSqr(pos);
                        if (distance < nearestDist) {
                            nearestDist = distance;
                            nearestShade = pos;
                        }
                    }
                }
            }
        }

        return nearestShade;
    }

    /**
     * Get flee position from player.
     */
    private BlockPos getFleePosition(Player player) {
        BlockPos mobPos = mob.blockPosition();
        BlockPos playerPos = player.blockPosition();

        int dx = mobPos.getX() - playerPos.getX();
        int dz = mobPos.getZ() - playerPos.getZ();

        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 0) {
            return mobPos.offset(
                (int) (dx / length * FLEE_DISTANCE),
                0,
                (int) (dz / length * FLEE_DISTANCE)
            );
        }

        return mobPos;
    }

    /**
     * Eat grass at position.
     */
    private void eatGrass(BlockPos pos) {
        level.destroyBlock(pos, false);

        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.SHEEP_AMBIENT,
                SoundSource.NEUTRAL, 1.0F, 0.8F);

        // Spawn particles
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                double offsetX = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;
                double offsetY = serverLevel.getRandom().nextDouble() * 0.5;
                double offsetZ = serverLevel.getRandom().nextDouble() * 0.5 - 0.25;

                serverLevel.sendParticles(
                    new net.minecraft.core.particles.BlockParticleOption(
                        net.minecraft.core.particles.ParticleTypes.BLOCK,
                        level.getBlockState(pos)
                    ),
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0.1
                );
            }
        }

        // Boost hunger
        CompoundTag hungerTag = getHungerTag();
        if (hungerTag != null) {
            int currentHunger = hungerTag.getInt("hunger");
            hungerTag.putInt("hunger", Math.min(100, currentHunger + 15));
        }

        debug("ate grass at " + pos.getX() + "," + pos.getZ());
    }

    /**
     * Initiate social grooming with nearby sheep.
     */
    private void initiateSocialGrooming() {
        CompoundTag woolTag = getWoolTag();
        CompoundTag socialTag = getSocialTag();

        if (woolTag == null || socialTag == null) {
            return;
        }

        boolean hasNearbySheep = SpatialIndex.hasNearbySameType(mob, 4);

        if (hasNearbySheep) {
            // Boost social and slightly improve wool quality
            int currentSocial = socialTag.getInt("social");
            socialTag.putInt("social", Math.min(100, currentSocial + 5));

            float currentQuality = woolTag.getFloat("woolQuality");
            woolTag.putFloat("woolQuality", Math.min(100, currentQuality + 2));

            debug("social grooming (quality=" + currentQuality + " -> " + woolTag.getFloat("woolQuality") + ")");
        }
    }

    /**
     * Get wool tag from NBT.
     */
    private CompoundTag getWoolTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }
        return component.getHandleTag("wool_growth");
    }

    /**
     * Get hunger tag from NBT.
     */
    private CompoundTag getHungerTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }
        return component.getHandleTag("hunger");
    }

    /**
     * Get social tag from NBT.
     */
    private CompoundTag getSocialTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }
        return component.getHandleTag("social");
    }

    /**
     * Get the ecology component for this mob.
     */
    private EcologyComponent getComponent() {
        if (!(mob instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[WoolGrowth] " + mob.getType().getDescription().getString() + " #" +
                           mob.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        CompoundTag woolTag = getWoolTag();
        float woolLength = woolTag != null ? WoolGrowthHandle.getWoolLength(woolTag) : 100f;

        return String.format(
            "state=%s, wool=%.1f, target=%s, moving=%s",
            currentState.name().toLowerCase(),
            woolLength,
            targetGrassPos != null ? "grass" :
                (targetShelterPos != null ? "shelter" :
                    (targetShadePos != null ? "shade" :
                        (targetShearsPlayer != null ? "fleeing" : "none"))),
            mob.getNavigation().isInProgress() ? "yes" : "no"
        );
    }

    private enum BehaviorState {
        IDLE,
        SEEKING_GRASS,
        SEEKING_SHELTER,
        FLEEING_SHEARS,
        SEEKING_SHADE
    }
}
