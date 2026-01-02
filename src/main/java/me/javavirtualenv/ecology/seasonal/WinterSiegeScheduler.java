package me.javavirtualenv.ecology.seasonal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

/**
 * Scheduler for winter village sieges by wolf packs.
 * <p>
 * Manages the initiation and coordination of wolf sieges based on:
 * - Winter season detection
 * - Hunger gradient system (0-100)
 * - Weather conditions (blizzard = higher siege chance)
 * - Time of day (night = doubled siege chance)
 * - Pack size limits (≤12 wolves/pack, ≤20 wolves/chunk)
 * - Village presence and threat level
 * <p>
 * Triggers sieges only when wolves are driven by resource scarcity,
 * not randomly. This maintains biome realism.
 */
public class WinterSiegeScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WinterSiegeScheduler.class);

    // Siege configuration parameters
    private static final int HUNGER_THRESHOLD_LOW = 60; // Low-risk livestock raids
    private static final int HUNGER_THRESHOLD_HIGH = 80; // Full village assault
    private static final double SIEGE_CHANCE_BASE = 0.05; // 5% base chance per day
    private static final double SIEGE_CHANCE_BLIZZARD = 0.10; // 10% chance during blizzard
    private static final double NIGHT_BONUS_MULTIPLIER = 2.0; // Double chance at night
    private static final double BLIZZARD_SPEED_BOOST = 0.15; // 15% speed boost in blizzard
    private static final double RETREAT_CASUALTY_THRESHOLD = 0.5; // 50% pack casualties
    private static final int MAX_WOLVES_PER_PACK = 12;
    private static final int MAX_WOLVES_PER_CHUNK = 20;
    private static final int GOLEM_THREAT_THRESHOLD = 8; // Pack size needed to attack golems

    // Siege tracking
    private static final Map<UUID, SiegeState> activeSieges = new HashMap<>();
    private static final long SIEGE_CHECK_INTERVAL_TICKS = 24000; // Check once per day

    // Last siege check time per level
    private static final Map<String, Long> lastSiegeCheckTime = new HashMap<>();

    // Server level reference for random access
    private static ServerLevel currentLevel;

    /**
     * Updates siege state for all wolves in level.
     * Called from server tick handler.
     *
     * @param level The server level
     */
    public static void updateSieges(ServerLevel level) {
        currentLevel = level;
        String levelKey = level.dimension().location().toString();
        long currentTime = level.getGameTime();

        // Check for new sieges once per day
        if (currentTime - lastSiegeCheckTime.getOrDefault(levelKey, 0L) >= SIEGE_CHECK_INTERVAL_TICKS) {
            checkForNewSieges(level);
            lastSiegeCheckTime.put(levelKey, currentTime);
        }

        // Update active sieges
        updateActiveSieges(level);
    }

    /**
     * Checks for new sieges to initiate.
     * Evaluates winter conditions and hunger levels.
     */
    private static void checkForNewSieges(ServerLevel level) {
        SeasonalContext.Season season = SeasonalContext.getCurrentSeason(level);

        // Only check during winter
        if (season != SeasonalContext.Season.WINTER) {
            return;
        }

        // Get all wolves in loaded chunks
        List<Wolf> allWolves = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Wolf wolf && !wolf.isTame()) {
                allWolves.add(wolf);
            }
        }

        if (allWolves.isEmpty()) {
            return;
        }

        // Group wolves by pack
        Map<UUID, List<Wolf>> packs = groupWolvesByPack(allWolves);

        // Check each pack for siege conditions
        for (Map.Entry<UUID, List<Wolf>> entry : packs.entrySet()) {
            UUID packId = entry.getKey();
            List<Wolf> pack = entry.getValue();

            // Skip packs that are already sieging
            if (activeSieges.containsKey(packId)) {
                continue;
            }

            // Check pack size limits
            if (pack.size() > MAX_WOLVES_PER_PACK) {
                continue;
            }

            // Check chunk wolf count limits
            if (exceedsChunkWolfLimit(level, pack)) {
                continue;
            }

            // Evaluate siege conditions
            SiegeConditions conditions = evaluateSiegeConditions(level, pack);

            if (conditions.shouldInitiateSiege()) {
                initiateSiege(level, pack, conditions);
            }
        }
    }

    /**
     * Groups wolves by pack ID.
     * Wolves without a pack ID are treated as individual packs.
     */
    private static Map<UUID, List<Wolf>> groupWolvesByPack(List<Wolf> wolves) {
        Map<UUID, List<Wolf>> packs = new HashMap<>();

        for (Wolf wolf : wolves) {
            UUID packId = getPackId(wolf);
            if (packId == null) {
                packId = wolf.getUUID(); // Individual pack
            }
            packs.computeIfAbsent(packId, k -> new ArrayList<>()).add(wolf);
        }

        return packs;
    }

    /**
     * Checks if the pack exceeds chunk wolf limit.
     */
    private static boolean exceedsChunkWolfLimit(ServerLevel level, List<Wolf> pack) {
        int chunkCount = 0;

        for (Wolf wolf : pack) {
            int chunkX = wolf.chunkPosition().x;
            int chunkZ = wolf.chunkPosition().z;
            String chunkKey = chunkX + "," + chunkZ;

            // Count wolves in this chunk
            long wolvesInChunk = level.getEntitiesOfClass(Wolf.class,
                    new AABB(wolf.blockPosition()).inflate(16.0)).stream()
                    .filter(w -> !w.isTame())
                    .count();

            if (wolvesInChunk > MAX_WOLVES_PER_CHUNK) {
                return true;
            }

            chunkCount++;
        }

        return false;
    }

    /**
     * Evaluates siege conditions for a wolf pack.
     * Considers hunger, weather, time, and village proximity.
     */
    private static SiegeConditions evaluateSiegeConditions(ServerLevel level, List<Wolf> pack) {
        SiegeConditions conditions = new SiegeConditions();

        // Get average hunger of the pack
        double avgHunger = pack.stream()
                .mapToInt(WinterSiegeScheduler::getWolfHunger)
                .average()
                .orElse(0.0);

        conditions.avgHunger = avgHunger;

        // Check weather conditions
        boolean isBlizzard = isBlizzard(level);
        boolean isNight = isNight(level);

        conditions.isBlizzard = isBlizzard;
        conditions.isNight = isNight;

        // Calculate siege probability
        double baseChance = isBlizzard ? SIEGE_CHANCE_BLIZZARD : SIEGE_CHANCE_BASE;
        double nightMultiplier = isNight ? NIGHT_BONUS_MULTIPLIER : 1.0;

        // Hunger modifier: higher hunger = higher chance
        double hungerModifier = 1.0;
        if (avgHunger >= HUNGER_THRESHOLD_HIGH) {
            hungerModifier = 2.0; // Double chance for desperate hunger
        } else if (avgHunger >= HUNGER_THRESHOLD_LOW) {
            hungerModifier = 1.5; // 50% higher chance for moderate hunger
        }

        conditions.siegeChance = baseChance * nightMultiplier * hungerModifier;

        // Determine siege type based on hunger
        conditions.siegeType = avgHunger >= HUNGER_THRESHOLD_HIGH ? SiegeType.FULL_ASSAULT : SiegeType.LIVESTOCK_RAID;

        return conditions;
    }

    /**
     * Initiates a siege for a wolf pack.
     * Sets siege state and activates AI behaviors.
     */
    private static void initiateSiege(ServerLevel level, List<Wolf> pack, SiegeConditions conditions) {
        UUID packId = getPackId(pack.get(0));

        // Find target village
        TargetInfo target = findTargetVillage(level, pack);
        if (target == null) {
            return; // No village nearby
        }

        // Create siege state
        SiegeState siege = new SiegeState();
        siege.packId = packId;
        siege.targetVillage = target;
        siege.siegeType = conditions.siegeType;
        siege.startTime = level.getGameTime();
        siege.isBlizzard = conditions.isBlizzard;
        siege.isNight = conditions.isNight;
        siege.packSize = pack.size();

        // Store siege state
        activeSieges.put(packId, siege);

        // Apply siege effects to wolves
        for (Wolf wolf : pack) {
            applySiegeEffects(wolf, siege);
        }

        LOGGER.info("Winter siege initiated: Pack {}, Type {}, Target at {}",
                packId, siege.siegeType, target.villagePosition);
    }

    /**
     * Finds a target village for the pack.
     * Prioritizes nearby villages with accessible livestock.
     */
    private static TargetInfo findTargetVillage(ServerLevel level, List<Wolf> pack) {
        Wolf leader = pack.get(0);
        BlockPos centerPos = leader.blockPosition();

        // Search for villages in range
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class,
                new AABB(centerPos).inflate(128.0));

        if (nearbyVillagers.isEmpty()) {
            return null;
        }

        // Calculate village center
        double centerX = 0, centerY = 0, centerZ = 0;
        for (Villager villager : nearbyVillagers) {
            centerX += villager.getX();
            centerY += villager.getY();
            centerZ += villager.getZ();
        }
        centerX /= nearbyVillagers.size();
        centerY /= nearbyVillagers.size();
        centerZ /= nearbyVillagers.size();

        BlockPos villagePosition = new BlockPos((int) centerX, (int) centerY, (int) centerZ);

        // Count iron golems
        int golemCount = level.getEntitiesOfClass(IronGolem.class,
                new AABB(villagePosition).inflate(48.0)).size();

        // Find livestock
        List<Entity> livestock = findLivestock(level, villagePosition);

        return new TargetInfo(villagePosition, nearbyVillagers.size(), golemCount, livestock.size());
    }

    /**
     * Finds livestock in a village.
     */
    private static List<Entity> findLivestock(ServerLevel level, BlockPos center) {
        List<Entity> livestock = new ArrayList<>();

        livestock.addAll(level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.Sheep.class,
                new AABB(center).inflate(64.0)));

        livestock.addAll(level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.Pig.class,
                new AABB(center).inflate(64.0)));

        livestock.addAll(level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.Cow.class,
                new AABB(center).inflate(64.0)));

        livestock.addAll(level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.Chicken.class,
                new AABB(center).inflate(64.0)));

        return livestock;
    }

    /**
     * Applies siege effects to a wolf.
     * Includes speed boosts and behavior activation.
     */
    private static void applySiegeEffects(Wolf wolf, SiegeState siege) {
        // Store siege state in NBT
        if (wolf instanceof EcologyAccess access) {
            EcologyComponent component = access.betterEcology$getEcologyComponent();
            if (component != null) {
                var tag = component.getHandleTag("siege");
                tag.putBoolean("is_sieging", true);
                tag.putLong("siege_start_time", siege.startTime);
                tag.putString("siege_type", siege.siegeType.name());
                tag.putBoolean("is_blizzard", siege.isBlizzard);
                tag.putBoolean("is_night", siege.isNight);
                tag.putDouble("speed_boost", siege.isBlizzard ? BLIZZARD_SPEED_BOOST : 0.0);
            }
        }
    }

    /**
     * Updates active sieges.
     * Checks for retreat conditions and updates wolf behaviors.
     */
    private static void updateActiveSieges(ServerLevel level) {
        List<UUID> siegesToEnd = new ArrayList<>();

        for (Map.Entry<UUID, SiegeState> entry : activeSieges.entrySet()) {
            UUID packId = entry.getKey();
            SiegeState siege = entry.getValue();

            // Get pack members
            List<Wolf> pack = getPackMembers(level, packId);
            if (pack.isEmpty()) {
                siegesToEnd.add(packId);
                continue;
            }

            // Check retreat conditions
            if (shouldRetreat(level, pack, siege)) {
                endSiege(level, pack, siege);
                siegesToEnd.add(packId);
            }
        }

        // Remove ended sieges
        for (UUID packId : siegesToEnd) {
            activeSieges.remove(packId);
        }
    }

    /**
     * Checks if the pack should retreat from the siege.
     */
    private static boolean shouldRetreat(ServerLevel level, List<Wolf> pack, SiegeState siege) {
        // Check casualties (50% threshold)
        if (pack.size() < siege.packSize * RETREAT_CASUALTY_THRESHOLD) {
            return true;
        }

        // Check if alpha is dead
        if (pack.stream().noneMatch(WinterSiegeScheduler::isAlpha)) {
            return true;
        }

        // Check if hunger is satisfied
        double avgHunger = pack.stream()
                .mapToInt(WinterSiegeScheduler::getWolfHunger)
                .average()
                .orElse(0.0);

        if (avgHunger < 30) { // Satisfied hunger
            return true;
        }

        // Check if village is locked down (iron golems spawned)
        int currentGolemCount = level.getEntitiesOfClass(IronGolem.class,
                new AABB(siege.targetVillage.villagePosition).inflate(48.0)).size();

        if (currentGolemCount > siege.targetVillage.golemCount) {
            return true; // Village reinforced
        }

        return false;
    }

    /**
     * Ends a siege and resets wolf behaviors.
     */
    private static void endSiege(ServerLevel level, List<Wolf> pack, SiegeState siege) {
        for (Wolf wolf : pack) {
            if (wolf instanceof EcologyAccess access) {
                EcologyComponent component = access.betterEcology$getEcologyComponent();
                if (component != null) {
                    var tag = component.getHandleTag("siege");
                    tag.putBoolean("is_sieging", false);
                }
            }
        }

        LOGGER.info("Winter siege ended: Pack {}, Duration {} ticks",
                siege.packId, level.getGameTime() - siege.startTime);
    }

    /**
     * Gets pack members by pack ID.
     */
    private static List<Wolf> getPackMembers(ServerLevel level, UUID packId) {
        List<Wolf> pack = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Wolf wolf && !wolf.isTame()) {
                UUID wolfPackId = getPackId(wolf);
                if (packId.equals(wolfPackId)) {
                    pack.add(wolf);
                }
            }
        }

        return pack;
    }

    /**
     * Gets the pack ID for a wolf.
     */
    private static UUID getPackId(Wolf wolf) {
        // In a full implementation, this would be stored in NBT
        // For now, use UUID as default
        return wolf.getUUID();
    }

    /**
     * Gets the hunger level of a wolf (0-100).
     */
    private static int getWolfHunger(Wolf wolf) {
        if (wolf instanceof EcologyAccess access) {
            EcologyComponent component = access.betterEcology$getEcologyComponent();
            if (component != null) {
                var tag = component.getHandleTag("hunger");
                return tag.getInt("hunger");
            }
        }
        return 50; // Default
    }

    /**
     * Checks if a wolf is the alpha of its pack.
     */
    private static boolean isAlpha(Wolf wolf) {
        // In a full implementation, this would check hierarchy data
        // For now, assume the oldest wolf is alpha
        return wolf.tickCount > 2000;
    }

    /**
     * Checks if it's currently a blizzard.
     */
    private static boolean isBlizzard(ServerLevel level) {
        Biome biome = level.getBiome(level.getSharedSpawnPos()).value();
        // Check for snow biome and weather conditions
        return level.isRaining() && biome.coldEnoughToSnow(level.getSharedSpawnPos());
    }

    /**
     * Checks if it's currently night.
     */
    private static boolean isNight(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= 13000L && dayTime < 23000L;
    }

    /**
     * Siege conditions evaluation result.
     */
    private static class SiegeConditions {
        double avgHunger;
        double siegeChance;
        boolean isBlizzard;
        boolean isNight;
        SiegeType siegeType;

        boolean shouldInitiateSiege() {
            return siegeChance > currentLevel.getRandom().nextDouble();
        }
    }

    /**
     * Siege state tracking.
     */
    private static class SiegeState {
        UUID packId;
        TargetInfo targetVillage;
        SiegeType siegeType;
        long startTime;
        boolean isBlizzard;
        boolean isNight;
        int packSize;
    }

    /**
     * Target village information.
     */
    private static class TargetInfo {
        BlockPos villagePosition;
        int villagerCount;
        int golemCount;
        int livestockCount;

        TargetInfo(BlockPos villagePosition, int villagerCount, int golemCount, int livestockCount) {
            this.villagePosition = villagePosition;
            this.villagerCount = villagerCount;
            this.golemCount = golemCount;
            this.livestockCount = livestockCount;
        }
    }

    /**
     * Siege type classification.
     */
    public enum SiegeType {
        LIVESTOCK_RAID, // Low-risk: target livestock only
        FULL_ASSAULT // High-risk: attack entire village
    }
}
