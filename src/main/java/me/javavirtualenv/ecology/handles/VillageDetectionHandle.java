package me.javavirtualenv.ecology.handles;

import java.util.List;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Handle for detecting and tracking nearby villages.
 * Uses Minecraft 1.21 native APIs for village detection.
 * <p>
 * Village definition: ≥3 beds + ≥2 villagers + ≥1 bell/valid door in loaded
 * chunks
 */
public class VillageDetectionHandle implements EcologyHandle {

    private static final String HANDLE_ID = "village_detection";
    private static final long CACHE_TTL_TICKS = 100; // 5 seconds
    private static final double DETECTION_RANGE = 128.0; // Detection radius in blocks

    private VillageInfo lastDetectedVillage;
    private long lastDetectionTime = 0;

    @Override
    public String id() {
        return HANDLE_ID;
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return true;
    }

    @Override
    public void initialize(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Initialize detection data
        CompoundTag tag = component.getHandleTag(HANDLE_ID);
        if (!tag.contains("last_detection_time")) {
            tag.putLong("last_detection_time", 0);
        }
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Detection is performed on-demand, not every tick
    }

    /**
     * Detects the nearest village to the given mob.
     * Uses caching to avoid redundant checks.
     *
     * @param mob The mob to detect villages for
     * @return VillageInfo if a village is found, null otherwise
     */
    public VillageInfo detectNearestVillage(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }

        long currentTime = mob.tickCount;

        // Check cache first
        if (lastDetectedVillage != null && currentTime - lastDetectionTime < CACHE_TTL_TICKS) {
            // Verify village is still valid
            if (lastDetectedVillage.isValid(level)) {
                return lastDetectedVillage;
            }
        }

        // Perform new detection
        VillageInfo village = performVillageDetection(mob, level);

        // Update cache
        lastDetectedVillage = village;
        lastDetectionTime = currentTime;

        return village;
    }

    /**
     * Performs village detection using Minecraft 1.21 APIs.
     * Searches for villages meeting the official definition criteria.
     */
    private VillageInfo performVillageDetection(Mob mob, ServerLevel level) {
        BlockPos mobPos = mob.blockPosition();

        // Search for villagers in range
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class,
                new AABB(mobPos).inflate(DETECTION_RANGE));

        if (nearbyVillagers.size() < 2) {
            return null; // Need at least 2 villagers
        }

        // Count beds in the area (valid village indicator)
        int bedCount = countBeds(level, mobPos);

        if (bedCount < 3) {
            return null; // Need at least 3 beds
        }

        // Check for bell (another valid village indicator)
        BlockPos bellPos = findNearestBell(level, mobPos);

        if (bellPos == null) {
            return null; // Need at least 1 bell
        }

        // Calculate village center (weighted by villager positions)
        BlockPos villageCenter = calculateVillageCenter(nearbyVillagers);

        // Count villagers
        int villagerCount = nearbyVillagers.size();

        // Assess threat level based on village defenses
        ThreatLevel threatLevel = assessThreatLevel(level, villageCenter, villagerCount);

        return new VillageInfo(villageCenter, villagerCount, bedCount, bellPos, threatLevel);
    }

    /**
     * Counts beds within detection range.
     * Beds are a key indicator of village presence.
     */
    private int countBeds(ServerLevel level, BlockPos center) {
        int bedCount = 0;
        int range = (int) (DETECTION_RANGE / 2);

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.WHITE_BED) || state.is(Blocks.ORANGE_BED) ||
                            state.is(Blocks.MAGENTA_BED) || state.is(Blocks.LIGHT_BLUE_BED) ||
                            state.is(Blocks.YELLOW_BED) || state.is(Blocks.LIME_BED) ||
                            state.is(Blocks.PINK_BED) || state.is(Blocks.GRAY_BED) ||
                            state.is(Blocks.LIGHT_GRAY_BED) || state.is(Blocks.CYAN_BED) ||
                            state.is(Blocks.PURPLE_BED) || state.is(Blocks.BLUE_BED) ||
                            state.is(Blocks.BROWN_BED) || state.is(Blocks.GREEN_BED) ||
                            state.is(Blocks.RED_BED) || state.is(Blocks.BLACK_BED)) {
                        bedCount++;
                    }
                }
            }
        }

        return bedCount;
    }

    /**
     * Finds the nearest bell within detection range.
     * Bells are used by villagers to summon iron golems.
     */
    private BlockPos findNearestBell(ServerLevel level, BlockPos center) {
        int range = (int) (DETECTION_RANGE / 2);
        BlockPos nearestBell = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.BELL)) {
                        double distance = pos.distSqr(center);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestBell = pos;
                        }
                    }
                }
            }
        }

        return nearestBell;
    }

    /**
     * Calculates the center of a village based on villager positions.
     * Uses weighted average to account for villager clustering.
     */
    private BlockPos calculateVillageCenter(List<Villager> villagers) {
        double centerX = 0;
        double centerY = 0;
        double centerZ = 0;

        for (Villager villager : villagers) {
            centerX += villager.getX();
            centerY += villager.getY();
            centerZ += villager.getZ();
        }

        centerX /= villagers.size();
        centerY /= villagers.size();
        centerZ /= villagers.size();

        return new BlockPos((int) centerX, (int) centerY, (int) centerZ);
    }

    /**
     * Assesses the threat level of a village.
     * Considers villager count, iron golems, and defenses.
     */
    private ThreatLevel assessThreatLevel(ServerLevel level, BlockPos center, int villagerCount) {
        // Check for iron golems (they defend villages)
        int golemCount = level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.IronGolem.class,
                new AABB(center).inflate(48.0)).size();

        // Base threat on villager count and golem count
        double threatScore = villagerCount * 1.0 + golemCount * 3.0;

        // More villagers = higher threat (they can fight back)
        // Iron golems = very high threat (they are powerful)

        if (threatScore < 5) {
            return ThreatLevel.LOW;
        } else if (threatScore < 15) {
            return ThreatLevel.MEDIUM;
        } else if (threatScore < 30) {
            return ThreatLevel.HIGH;
        } else {
            return ThreatLevel.EXTREME;
        }
    }

    /**
     * Information about a detected village.
     */
    public static class VillageInfo {
        private final BlockPos center;
        private final int villagerCount;
        private final int bedCount;
        private final BlockPos bellPosition;
        private final ThreatLevel threatLevel;

        public VillageInfo(BlockPos center, int villagerCount, int bedCount,
                BlockPos bellPosition, ThreatLevel threatLevel) {
            this.center = center;
            this.villagerCount = villagerCount;
            this.bedCount = bedCount;
            this.bellPosition = bellPosition;
            this.threatLevel = threatLevel;
        }

        public BlockPos getCenter() {
            return center;
        }

        public int getVillagerCount() {
            return villagerCount;
        }

        public int getBedCount() {
            return bedCount;
        }

        public BlockPos getBellPosition() {
            return bellPosition;
        }

        public ThreatLevel getThreatLevel() {
            return threatLevel;
        }

        /**
         * Checks if this village information is still valid.
         * Verifies that the bell still exists and villagers are present.
         */
        public boolean isValid(ServerLevel level) {
            // Check if bell still exists
            if (bellPosition != null) {
                BlockState state = level.getBlockState(bellPosition);
                if (!state.is(Blocks.BELL)) {
                    return false;
                }
            }

            // Check if villagers are still in range
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                    new AABB(center).inflate(DETECTION_RANGE));

            return villagers.size() >= 2;
        }

        /**
         * Gets the distance from the village center to a given position.
         */
        public double distanceTo(BlockPos pos) {
            return center.distSqr(pos);
        }

        /**
         * Gets the distance from the village center to a given position.
         */
        public double distanceTo(net.minecraft.world.phys.Vec3 pos) {
            return Math.sqrt(center.distSqr(net.minecraft.core.BlockPos.containing(pos.x, pos.y, pos.z)));
        }
    }

    /**
     * Threat levels for village assessment.
     */
    public enum ThreatLevel {
        LOW, // Small village, minimal defenses
        MEDIUM, // Medium village, some defenses
        HIGH, // Large village, iron golems present
        EXTREME // Major village, multiple iron golems
    }
}
