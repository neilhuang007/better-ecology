package me.javavirtualenv.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for spawning animals in natural groups.
 *
 * <p>Based on ecological research:
 * <ul>
 *   <li>Animals spawn in social groups, not individually</li>
 *   <li>Groups have age diversity (adults, juveniles, babies)</li>
 *   <li>Group sizes vary by species</li>
 * </ul>
 */
public final class GroupSpawnHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupSpawnHandler.class);

    private GroupSpawnHandler() {
        // Utility class
    }

    /**
     * Spawns a group of animals with age diversity.
     *
     * @param level the server level
     * @param type the entity type to spawn
     * @param center the center position for the group
     * @param minSize the minimum group size
     * @param maxSize the maximum group size
     * @param babyPercentage the percentage of the group that should be babies (0.0 to 1.0)
     * @return the list of spawned animals
     */
    public static List<Animal> spawnGroup(
            ServerLevel level,
            EntityType<? extends Animal> type,
            BlockPos center,
            int minSize, int maxSize,
            float babyPercentage) {

        List<Animal> spawned = new ArrayList<>();

        // Determine group size
        int groupSize = minSize + level.random.nextInt(maxSize - minSize + 1);
        int babyCount = Math.round(groupSize * babyPercentage);

        LOGGER.debug("Spawning group of {} {} at {} ({} babies)",
            groupSize, type.getDescriptionId(), center, babyCount);

        int spreadRadius = 4; // Spread group within this radius

        for (int i = 0; i < groupSize; i++) {
            // Find spawn position near center
            BlockPos spawnPos = findSpawnPosition(level, center, spreadRadius);
            if (spawnPos == null) {
                continue;
            }

            // Spawn the animal
            Animal animal = type.create(level, null, spawnPos, MobSpawnType.NATURAL, true, false);
            if (animal == null) {
                continue;
            }

            // Set position
            animal.moveTo(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0f,
                0.0f
            );

            // Make some of them babies
            if (i < babyCount) {
                animal.setBaby(true);
            }

            // Add to world
            level.addFreshEntity(animal);
            spawned.add(animal);
        }

        LOGGER.debug("Successfully spawned {} of {} {} at {}",
            spawned.size(), groupSize, type.getDescriptionId(), center);

        return spawned;
    }

    /**
     * Finds a valid spawn position near the center.
     *
     * @param level the server level
     * @param center the center position
     * @param radius the search radius
     * @return a valid spawn position, or null if none found
     */
    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos center, int radius) {
        // Try random positions within radius
        for (int attempt = 0; attempt < 10; attempt++) {
            int offsetX = level.random.nextInt(radius * 2 + 1) - radius;
            int offsetZ = level.random.nextInt(radius * 2 + 1) - radius;

            BlockPos testPos = center.offset(offsetX, 0, offsetZ);

            // Find ground level
            BlockPos groundPos = findGroundLevel(level, testPos);
            if (groundPos != null && SpawnConditionChecker.isValidSpawnSurface(level, groundPos)) {
                return groundPos;
            }
        }

        // Fall back to center if no valid position found
        BlockPos groundCenter = findGroundLevel(level, center);
        if (groundCenter != null && SpawnConditionChecker.isValidSpawnSurface(level, groundCenter)) {
            return groundCenter;
        }

        return null;
    }

    /**
     * Finds the ground level at a position.
     *
     * @param level the server level
     * @param pos the position to check
     * @return the ground level position, or null if not found
     */
    private static BlockPos findGroundLevel(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        if (level.getBlockState(mutable).isAir()) {
            // Move down to find ground
            while (mutable.getY() > minY && level.getBlockState(mutable.below()).isAir()) {
                mutable.move(0, -1, 0);
            }
        } else {
            // Move up to find air
            while (mutable.getY() < maxY && !level.getBlockState(mutable).isAir()) {
                mutable.move(0, 1, 0);
            }
        }

        if (mutable.getY() <= minY || mutable.getY() >= maxY) {
            return null;
        }

        return mutable.immutable();
    }

    /**
     * Spawns a group with default baby percentage (15%).
     *
     * @param level the server level
     * @param type the entity type to spawn
     * @param center the center position for the group
     * @param minSize the minimum group size
     * @param maxSize the maximum group size
     * @return the list of spawned animals
     */
    public static List<Animal> spawnGroup(
            ServerLevel level,
            EntityType<? extends Animal> type,
            BlockPos center,
            int minSize, int maxSize) {
        return spawnGroup(level, type, center, minSize, maxSize, 0.15f);
    }
}
