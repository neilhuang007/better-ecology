package me.javavirtualenv.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom spawn predicate that enforces ecological spawn conditions.
 *
 * <p>This predicate checks:
 * <ul>
 *   <li>Water proximity requirements</li>
 *   <li>Height restrictions</li>
 *   <li>Territorial spacing from existing groups</li>
 *   <li>Population density limits</li>
 * </ul>
 *
 * <p>Usage: Register with SpawnPlacements for each entity type.
 *
 * @param <T> the animal type
 */
public class EcologySpawnPredicate<T extends Animal> implements SpawnPlacements.SpawnPredicate<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EcologySpawnPredicate.class);

    private final SpawnRequirements requirements;
    private final Class<T> animalClass;

    /**
     * Creates a new ecology spawn predicate.
     *
     * @param animalClass the class of animal this predicate applies to
     * @param requirements the spawn requirements to enforce
     */
    public EcologySpawnPredicate(Class<T> animalClass, SpawnRequirements requirements) {
        this.animalClass = animalClass;
        this.requirements = requirements;
    }

    @Override
    public boolean test(EntityType<T> entityType, ServerLevelAccessor level,
                        MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Always allow player-placed spawns (spawn eggs, commands)
        if (spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
            return true;
        }

        // Check ecological conditions
        boolean canSpawn = SpawnConditionChecker.canSpawnAt(
            level.getLevel(),
            pos,
            animalClass,
            requirements
        );

        if (!canSpawn) {
            LOGGER.debug("Ecology spawn check failed for {} at {}",
                entityType.getDescriptionId(), pos);
        }

        return canSpawn;
    }

    /**
     * Registers this predicate for an entity type.
     *
     * @param entityType the entity type to register for
     */
    public void register(EntityType<T> entityType) {
        SpawnPlacements.register(
            entityType,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            this
        );
        LOGGER.info("Registered ecology spawn predicate for {}", entityType.getDescriptionId());
    }

    /**
     * Creates a predicate for land animals that need water nearby.
     *
     * @param <T> the animal type
     * @param animalClass the animal class
     * @return a configured predicate
     */
    public static <T extends Animal> EcologySpawnPredicate<T> landAnimalNeedsWater(Class<T> animalClass) {
        return new EcologySpawnPredicate<>(animalClass, SpawnRequirements.LAND_ANIMAL_NEEDS_WATER);
    }

    /**
     * Creates a predicate for land animals that don't need water.
     *
     * @param <T> the animal type
     * @param animalClass the animal class
     * @return a configured predicate
     */
    public static <T extends Animal> EcologySpawnPredicate<T> landAnimal(Class<T> animalClass) {
        return new EcologySpawnPredicate<>(animalClass, SpawnRequirements.LAND_ANIMAL_NO_WATER);
    }

    /**
     * Creates a predicate for predators (larger territory).
     *
     * @param <T> the animal type
     * @param animalClass the animal class
     * @return a configured predicate
     */
    public static <T extends Animal> EcologySpawnPredicate<T> predator(Class<T> animalClass) {
        return new EcologySpawnPredicate<>(animalClass, SpawnRequirements.PREDATOR);
    }
}
