package me.javavirtualenv.behavior.production;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import me.javavirtualenv.mixin.animal.BeeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Bee-specific pollination goal.
 * <p>
 * Bees search for flowers within range, pollinate them, and return to the hive.
 * Different flowers produce different honey types:
 * - Regular flowers: Regular honey
 * - Sunflower: Bright yellow honey, extra saturation
 * - Cornflower: Blue honey, speed boost
 * - Wither rose: Poison honey (dangerous)
 * <p>
 * Pollination also boosts crop growth in nearby farmland.
 */
public class BeePollinationGoal extends ResourceGatheringGoal {

    // NBT keys
    private static final String POLLINATION_TICKS_KEY = "pollination_ticks";
    private static final String FLOWER_TYPE_KEY = "pollination_flower";

    // Configuration constants
    private static final int POLLINATION_TICKS_NEEDED = 60;
    private static final int CROP_GROWTH_RADIUS = 8;
    private static final double MOVE_SPEED = 1.0;
    private static final double FLOWER_DISTANCE = 2.0;
    private static final double HIVE_DISTANCE = 2.0;
    private static final int SEARCH_INTERVAL = 100;

    // Instance fields
    private final Bee bee;
    private String currentFlowerType;
    private int pollinationTicks;

    // Debug info
    private String lastDebugMessage = "";

    public BeePollinationGoal(Bee bee) {
        super(bee, 22.0, SEARCH_INTERVAL, POLLINATION_TICKS_NEEDED);
        this.bee = bee;
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (bee.level().isClientSide) {
            return false;
        }

        if (!bee.isAlive()) {
            return false;
        }

        if (bee.isAngry()) {
            return false;
        }

        // Load state from NBT
        CompoundTag pollinationTag = getPollinationTag();
        if (pollinationTag != null) {
            pollinationTicks = pollinationTag.getInt(POLLINATION_TICKS_KEY);
            currentFlowerType = pollinationTag.getString(FLOWER_TYPE_KEY);
        }

        // If bee already has nectar, return to hive
        if (bee.hasNectar()) {
            if (!returningHome) {
                debug("has nectar, returning to hive");
            }
            returningHome = true;
            hasResource = true;
            return true;
        }

        // Don't pollinate at night or during bad weather
        long dayTime = bee.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;
        boolean isBadWeather = bee.level().isRaining() || bee.level().isThundering();

        if (isNight || isBadWeather) {
            return false;
        }

        // Search for flowers
        ticksSinceLastSearch++;
        if (ticksSinceLastSearch < searchInterval) {
            return false;
        }

        ticksSinceLastSearch = 0;
        targetResourcePos = findNearestResource();

        if (targetResourcePos != null) {
            BlockState flowerState = bee.level().getBlockState(targetResourcePos);
            currentFlowerType = getFlowerType(flowerState);
            debug("STARTING: pollinating " + currentFlowerType + " at " +
                  targetResourcePos.getX() + "," + targetResourcePos.getZ());
        }

        return targetResourcePos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!bee.isAlive()) {
            return false;
        }

        if (returningHome) {
            return !isNearHome();
        }

        if (targetResourcePos == null) {
            return false;
        }

        if (hasResource) {
            return true;
        }

        return gatheringTicks < gatheringDuration && bee.level().isLoaded(targetResourcePos);
    }

    @Override
    public void start() {
        if (targetResourcePos != null) {
            Vec3 targetVec = new Vec3(
                targetResourcePos.getX() + 0.5,
                targetResourcePos.getY(),
                targetResourcePos.getZ() + 0.5
            );
            double distance = bee.position().distanceTo(targetVec);

            if (distance > FLOWER_DISTANCE) {
                bee.getNavigation().moveTo(targetResourcePos.getX(), targetResourcePos.getY(),
                    targetResourcePos.getZ(), MOVE_SPEED);
            }
        }
    }

    @Override
    public void stop() {
        if (returningHome && isNearHome()) {
            onResourceDelivered();
        }

        // Save state to NBT
        CompoundTag pollinationTag = getPollinationTag();
        if (pollinationTag != null) {
            pollinationTag.putInt(POLLINATION_TICKS_KEY, pollinationTicks);
            if (currentFlowerType != null) {
                pollinationTag.putString(FLOWER_TYPE_KEY, currentFlowerType);
            }
        }

        targetResourcePos = null;
        gatheringTicks = 0;
        pollinationTicks = 0;
        currentFlowerType = null;

        debug("goal stopped");
    }

    @Override
    public void tick() {
        if (returningHome) {
            returnToHome();
            return;
        }

        if (targetResourcePos == null || !bee.level().isLoaded(targetResourcePos)) {
            return;
        }

        Vec3 targetVec = new Vec3(
            targetResourcePos.getX() + 0.5,
            targetResourcePos.getY(),
            targetResourcePos.getZ() + 0.5
        );
        double distance = bee.position().distanceTo(targetVec);

        if (distance > FLOWER_DISTANCE) {
            bee.getNavigation().moveTo(targetResourcePos.getX(), targetResourcePos.getY(),
                targetResourcePos.getZ(), MOVE_SPEED);
            return;
        }

        // Close enough to pollinate
        gatherResource();
        pollinationTicks++;

        // Pollinate nearby crops periodically
        if (pollinationTicks >= 10 && pollinationTicks % 10 == 0) {
            pollinateNearbyCrops();
        }

        // Log progress every second
        if (bee.tickCount % 20 == 0) {
            debug("pollinating, progress=" + pollinationTicks + "/" + gatheringDuration +
                  ", flower=" + currentFlowerType);
        }

        if (pollinationTicks >= gatheringDuration) {
            ((BeeAccessor) bee).invokeSetHasNectar(true);
            hasResource = true;

            // Store pollination target
            EcologyComponent component = getComponent();
            if (component != null) {
                ResourceProductionHandle productionHandle = new ResourceProductionHandle();
                productionHandle.setPollinationTarget(bee, component, currentFlowerType);
            }

            debug("pollination complete, returning to hive");
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    protected boolean isValidResource(BlockPos pos) {
        if (!bee.level().isLoaded(pos)) {
            return false;
        }

        BlockState state = bee.level().getBlockState(pos);

        if (state.is(BlockTags.FLOWERS)) {
            return true;
        }

        return state.getBlock() == Blocks.SPORE_BLOSSOM ||
               state.getBlock() == Blocks.MANGROVE_PROPAGULE ||
               state.getBlock() == Blocks.AZALEA ||
               state.getBlock() == Blocks.FLOWERING_AZALEA_LEAVES ||
               state.getBlock() == Blocks.PINK_PETALS;
    }

    @Override
    protected void returnToHome() {
        BlockPos hivePos = bee.getHivePos();
        if (hivePos == null) {
            return;
        }

        Vec3 hiveCenter = new Vec3(hivePos.getX() + 0.5, hivePos.getY(), hivePos.getZ() + 0.5);
        double distance = bee.position().distanceTo(hiveCenter);

        if (distance > HIVE_DISTANCE) {
            bee.getNavigation().moveTo(hivePos.getX(), hivePos.getY(), hivePos.getZ(), MOVE_SPEED);
        }

        // Log progress every second
        if (bee.tickCount % 20 == 0) {
            debug("returning to hive, distance=" + String.format("%.1f", distance));
        }
    }

    @Override
    protected boolean isNearHome() {
        BlockPos hivePos = bee.getHivePos();
        if (hivePos == null) {
            return true;
        }

        Vec3 hiveCenter = new Vec3(hivePos.getX() + 0.5, hivePos.getY(), hivePos.getZ() + 0.5);
        double distance = bee.position().distanceTo(hiveCenter);

        return distance < HIVE_DISTANCE;
    }

    @Override
    protected void onResourceDelivered() {
        if (bee.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) bee.level();
        BlockPos hivePos = bee.getHivePos();

        if (hivePos != null) {
            BlockState hiveState = bee.level().getBlockState(hivePos);

            int honeyLevel = 0;
            if (hiveState.hasProperty(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL)) {
                honeyLevel = hiveState.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL);
            }

            if (honeyLevel < 5) {
                serverLevel.setBlock(
                    hivePos,
                    hiveState.setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, honeyLevel + 1),
                    3
                );
                debug("delivered nectar to hive (honey level now " + (honeyLevel + 1) + "/5)");
            }
        }

        ((BeeAccessor) bee).invokeSetHasNectar(false);
        returningHome = false;
        hasResource = false;

        // Clear NBT state
        CompoundTag pollinationTag = getPollinationTag();
        if (pollinationTag != null) {
            pollinationTag.putInt(POLLINATION_TICKS_KEY, 0);
            pollinationTag.putString(FLOWER_TYPE_KEY, "");
        }
    }

    /**
     * Gets the flower type for honey production.
     */
    private String getFlowerType(BlockState flowerState) {
        if (flowerState.is(Blocks.SUNFLOWER)) {
            return "sunflower";
        }
        if (flowerState.is(Blocks.CORNFLOWER)) {
            return "cornflower";
        }
        if (flowerState.is(Blocks.WITHER_ROSE)) {
            return "wither_rose";
        }
        if (flowerState.is(Blocks.LILAC)) {
            return "lilac";
        }
        if (flowerState.is(Blocks.PEONY)) {
            return "peony";
        }
        if (flowerState.is(Blocks.ROSE_BUSH)) {
            return "rose";
        }
        if (flowerState.is(Blocks.POPPY)) {
            return "poppy";
        }
        if (flowerState.is(Blocks.DANDELION)) {
            return "dandelion";
        }
        if (flowerState.is(Blocks.BLUE_ORCHID)) {
            return "orchid";
        }
        if (flowerState.is(Blocks.ALLIUM)) {
            return "allium";
        }
        if (flowerState.is(Blocks.AZURE_BLUET)) {
            return "azure_bluet";
        }
        if (flowerState.is(Blocks.RED_TULIP) ||
            flowerState.is(Blocks.ORANGE_TULIP) ||
            flowerState.is(Blocks.WHITE_TULIP) ||
            flowerState.is(Blocks.PINK_TULIP)) {
            return "tulip";
        }
        if (flowerState.is(Blocks.OXEYE_DAISY)) {
            return "daisy";
        }
        if (flowerState.is(Blocks.LILY_OF_THE_VALLEY)) {
            return "lily_of_the_valley";
        }
        if (flowerState.is(BlockTags.TALL_FLOWERS)) {
            return "tall_flower";
        }

        return "regular";
    }

    /**
     * Pollinates nearby crops, boosting their growth.
     */
    private void pollinateNearbyCrops() {
        if (bee.level().isClientSide || targetResourcePos == null) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) bee.level();

        for (BlockPos pos : BlockPos.betweenClosed(
            targetResourcePos.getX() - CROP_GROWTH_RADIUS,
            targetResourcePos.getY() - 4,
            targetResourcePos.getZ() - CROP_GROWTH_RADIUS,
            targetResourcePos.getX() + CROP_GROWTH_RADIUS,
            targetResourcePos.getY() + 4,
            targetResourcePos.getZ() + CROP_GROWTH_RADIUS
        )) {
            BlockState state = serverLevel.getBlockState(pos);

            if (isGrowableCrop(state)) {
                if (serverLevel.random.nextFloat() < 0.15f) {
                    net.minecraft.world.level.block.Block block = state.getBlock();
                    if (state.isRandomlyTicking()) {
                        state.randomTick(serverLevel, pos, serverLevel.random);
                    }
                }
            }
        }
    }

    /**
     * Checks if a block is a growable crop.
     */
    private boolean isGrowableCrop(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.CROPS) ||
               state.is(Blocks.MANGROVE_PROPAGULE) ||
               state.is(Blocks.SWEET_BERRY_BUSH) ||
               state.is(Blocks.CACTUS) ||
               state.is(Blocks.SUGAR_CANE) ||
               state.is(Blocks.BAMBOO) ||
               state.is(Blocks.PUMPKIN_STEM) ||
               state.is(Blocks.MELON_STEM);
    }

    /**
     * Get pollination tag from NBT.
     */
    private CompoundTag getPollinationTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }

        CompoundTag tag = component.getHandleTag("resource_production");
        if (!tag.contains(POLLINATION_TICKS_KEY)) {
            tag.putInt(POLLINATION_TICKS_KEY, 0);
        }
        if (!tag.contains(FLOWER_TYPE_KEY)) {
            tag.putString(FLOWER_TYPE_KEY, "");
        }
        return tag;
    }

    /**
     * Get the ecology component for this bee.
     */
    private EcologyComponent getComponent() {
        if (!(bee instanceof EcologyAccess access)) {
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
            String prefix = "[BeePollination] Bee #" + bee.getId() + " ";
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
        BlockPos hivePos = bee.getHivePos();
        String hiveInfo = hivePos != null ?
            (hivePos.getX() + "," + hivePos.getZ()) : "none";

        return String.format(
            "has_nectar=%s, flower=%s, progress=%d/%d, hive=%s, returning=%s",
            bee.hasNectar(),
            currentFlowerType != null ? currentFlowerType : "none",
            pollinationTicks,
            gatheringDuration,
            hiveInfo,
            returningHome ? "yes" : "no"
        );
    }
}
