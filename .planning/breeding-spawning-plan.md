# Breeding and Spawning Behaviors Implementation Plan

## Overview

This plan implements population-aware breeding and ecologically-realistic spawning for Better Ecology. The guiding principle is to **reuse vanilla Minecraft elements** as much as possible.

## Research Summary

### Similar Projects Found
- **AntiMobSpawn** (Bukkit): Provides spawn height limits, spawn zone configuration, and per-world settings. Uses `uk.samlex.ams` package. Not directly reusable (Bukkit vs Fabric).
- **VanillaRemover**: Removes spawn registries - shows how to modify spawn settings.
- No open-source Fabric mods found with population-aware breeding or carrying capacity systems.

### Academic Foundation
The **logistic growth model** is the standard ecological approach:
```
dN/dt = rN(1 - N/K)

Where:
- N = current population
- K = carrying capacity (maximum sustainable population)
- r = intrinsic growth rate
- As N approaches K, growth rate approaches 0
```

### Fabric API Available
From Context7 docs:
```java
// Add spawn to biomes
BiomeModifications.addSpawn(
    BiomeSelectors.foundInOverworld(),
    SpawnGroup.CREATURE,
    EntityType.COW,
    weight, minGroupSize, maxGroupSize
);

// Spawn restrictions
FabricEntityTypeBuilder.Mob.spawnRestriction(
    SpawnLocation,
    Heightmap.Type,
    SpawnPredicate<T>
);
```

---

## Implementation Tasks

### Phase 1: Population Counting System

**Goal**: Create a system to count nearby animals for density-dependent behaviors.

#### Task 1.1: PopulationCounter Utility Class
Create `src/main/java/me/javavirtualenv/behavior/core/PopulationCounter.java`

```java
public final class PopulationCounter {
    // Count animals of a specific type within radius
    public static int countNearby(Level level, BlockPos center,
                                   Class<? extends Animal> type, int radius);

    // Count all animals within radius
    public static int countAllNearby(Level level, BlockPos center, int radius);

    // Get density (animals per chunk)
    public static float getDensity(Level level, BlockPos center,
                                   Class<? extends Animal> type, int radiusChunks);
}
```

**Vanilla elements used**: `Level.getEntitiesOfClass()`, `AABB`

---

### Phase 2: Population-Aware Breeding

**Goal**: Breeding rate slows as population approaches carrying capacity; emergency breeding when population is critically low.

#### Task 2.1: Modify BreedingBehaviorGoal.java

Add population awareness to existing `BreedingBehaviorGoal`:

```java
// Constants
private static final int POPULATION_CHECK_RADIUS = 64; // blocks
private static final int CARRYING_CAPACITY = 20;       // max animals in area
private static final int EMERGENCY_THRESHOLD = 3;      // trigger emergency breeding
private static final float EMERGENCY_BREED_BOOST = 0.8f; // 80% faster courtship

// New method: calculateBreedingModifier()
private float calculateBreedingModifier() {
    int population = PopulationCounter.countNearby(
        level, animal.blockPosition(), partnerClass, POPULATION_CHECK_RADIUS);

    if (population <= EMERGENCY_THRESHOLD) {
        // Emergency breeding: speed up courtship
        return EMERGENCY_BREED_BOOST;
    }

    // Logistic slowdown: breeding chance decreases as population approaches K
    // Formula: modifier = 1 - (N/K)
    float densityRatio = (float) population / CARRYING_CAPACITY;
    return Math.max(0.0f, 1.0f - densityRatio);
}
```

#### Task 2.2: Modify canAttemptBreeding()

Add population cap check:
```java
private boolean canAttemptBreeding() {
    // ... existing checks ...

    // Population cap: don't breed if at carrying capacity
    int population = PopulationCounter.countNearby(
        level, animal.blockPosition(), partnerClass, POPULATION_CHECK_RADIUS);
    if (population >= CARRYING_CAPACITY) {
        LOGGER.debug("{} population at carrying capacity ({}), skipping breeding",
            animal.getName().getString(), population);
        return false;
    }

    // Random chance based on population density (logistic model)
    float breedChance = calculateBreedingModifier();
    if (animal.getRandom().nextFloat() > breedChance && population > EMERGENCY_THRESHOLD) {
        return false;
    }

    return true;
}
```

#### Task 2.3: Emergency Breeding Visual Feedback

When emergency breeding is triggered, show vanilla particles:
```java
// In performCourtshipDisplay()
if (isEmergencyBreeding) {
    // Extra hearts to indicate urgency
    serverLevel.sendParticles(ParticleTypes.HEART, ...count: 5...);
}
```

**Vanilla elements used**: `ParticleTypes.HEART`, existing `playAmbientSound()`

---

### Phase 3: Spawning System

**Goal**: Control initial animal placement based on biome, water sources, and existing population.

#### Task 3.1: SpawnConditionChecker Utility Class

Create `src/main/java/me/javavirtualenv/spawning/SpawnConditionChecker.java`

```java
public final class SpawnConditionChecker {
    // Check if water source is nearby (for drinking animals)
    public static boolean hasWaterNearby(Level level, BlockPos pos, int radius);

    // Check height is appropriate (not too high, not in caves)
    public static boolean isValidHeight(Level level, BlockPos pos,
                                        int minY, int maxY);

    // Check for existing population (territorial spacing)
    public static boolean hasSpaceForGroup(Level level, BlockPos pos,
                                           Class<? extends Animal> type,
                                           int territoryRadius);

    // Combined check for spawn eligibility
    public static boolean canSpawnAt(Level level, BlockPos pos,
                                     Class<? extends Animal> type,
                                     SpawnRequirements requirements);
}
```

**Vanilla elements used**:
- `BlockTags.WATER` for water detection
- `level.getHeight()` for terrain height
- `Level.getEntitiesOfClass()` for population check

#### Task 3.2: SpawnRequirements Configuration

Create data class for spawn requirements:
```java
public record SpawnRequirements(
    int minWaterDistance,      // 0 = no water required
    int maxWaterDistance,      // -1 = unlimited
    int minY,                  // minimum spawn height
    int maxY,                  // maximum spawn height
    int territoryRadius,       // minimum distance from same species
    int maxPopulationInArea,   // carrying capacity for spawn area
    int populationCheckRadius  // radius to check for existing animals
) {}
```

#### Task 3.3: Modify Biome Spawn Registrations

In `BetterEcology.java`, add spawn modifications:
```java
private void registerSpawnModifications() {
    // Example: Cows need water nearby, not in mountains
    BiomeModifications.addSpawn(
        context -> BiomeSelectors.foundInOverworld().test(context)
            && !BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN).test(context),
        SpawnGroup.CREATURE,
        EntityType.COW,
        10,  // weight
        3,   // min group
        6    // max group
    );
}
```

#### Task 3.4: Custom SpawnPredicate

Create spawn predicates that check conditions:
```java
public class EcologySpawnPredicate<T extends Animal>
    implements SpawnRestriction.SpawnPredicate<T> {

    private final SpawnRequirements requirements;

    @Override
    public boolean test(EntityType<T> type, ServerLevelAccessor level,
                       MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return SpawnConditionChecker.canSpawnAt(
            level.getLevel(), pos, type.getBaseClass(), requirements);
    }
}
```

---

### Phase 4: Group Spawning

**Goal**: Animals spawn in natural group sizes, not individuals.

#### Task 4.1: GroupSpawnHandler

Create `src/main/java/me/javavirtualenv/spawning/GroupSpawnHandler.java`

```java
public final class GroupSpawnHandler {
    // Spawn a group with age diversity
    public static List<Animal> spawnGroup(
        ServerLevel level,
        EntityType<? extends Animal> type,
        BlockPos center,
        int minSize, int maxSize,
        float babyPercentage  // e.g., 0.1 = 10% babies
    );
}
```

This uses vanilla spawning internally but ensures groups spawn together.

---

### Phase 5: Species-Specific Configuration

#### Task 5.1: Spawn Config Files

Create YAML configs in `data/better-ecology/spawn/`:
```yaml
# cow.yaml
entity: minecraft:cow
spawn_requirements:
  min_water_distance: 0
  max_water_distance: 32
  min_y: 60
  max_y: 128
  territory_radius: 48
  max_population: 20
  population_radius: 96
group_size:
  min: 3
  max: 8
  baby_percentage: 0.15
biomes:
  primary:
    - minecraft:plains
    - minecraft:sunflower_plains
  secondary:
    - minecraft:forest
    - minecraft:flower_forest
```

#### Task 5.2: Config Loader

Create loader that reads YAML and applies to spawn system.

---

## Implementation Order

1. **PopulationCounter** (foundation for everything)
2. **BreedingBehaviorGoal modifications** (population-aware breeding)
3. **SpawnConditionChecker** (spawn condition validation)
4. **SpawnRequirements** (configuration data class)
5. **EcologySpawnPredicate** (applies conditions to vanilla spawning)
6. **GroupSpawnHandler** (group spawning logic)
7. **Config files** (per-species customization)

## Testing Strategy

### Unit Tests (GameTest)
1. `testBreedingStopsAtCarryingCapacity` - Spawn 20+ cows, verify no more babies
2. `testEmergencyBreedingTriggered` - Spawn 2 cows, verify faster breeding
3. `testWaterRequirementForSpawning` - Verify cows don't spawn far from water
4. `testTerritorialSpacing` - Verify groups don't spawn too close together

### Manual Testing
1. Start new world, observe group spawning patterns
2. Breed animals in enclosed area, observe population cap
3. Kill most animals, observe emergency breeding behavior

## Vanilla Elements Reused

| Feature | Vanilla Element |
|---------|-----------------|
| Population counting | `Level.getEntitiesOfClass()`, `AABB` |
| Water detection | `BlockTags.WATER`, `Fluids.WATER` |
| Height check | `level.getHeight()`, `Heightmap.Types` |
| Spawn registration | `BiomeModifications.addSpawn()` |
| Spawn predicates | `SpawnRestriction.SpawnPredicate` |
| Group spawning | `EntityType.spawn()` |
| Visual feedback | `ParticleTypes.HEART` |
| Audio feedback | `mob.playAmbientSound()` |

## Files to Create

1. `src/main/java/me/javavirtualenv/behavior/core/PopulationCounter.java`
2. `src/main/java/me/javavirtualenv/spawning/SpawnConditionChecker.java`
3. `src/main/java/me/javavirtualenv/spawning/SpawnRequirements.java`
4. `src/main/java/me/javavirtualenv/spawning/EcologySpawnPredicate.java`
5. `src/main/java/me/javavirtualenv/spawning/GroupSpawnHandler.java`
6. `src/main/resources/data/better-ecology/spawn/*.yaml` (per species)

## Files to Modify

1. `src/main/java/me/javavirtualenv/behavior/core/BreedingBehaviorGoal.java`
2. `src/main/java/me/javavirtualenv/BetterEcology.java` (spawn registrations)

---

## Questions for Clarification

1. **Carrying capacity values**: Should these be configurable per-biome, or use fixed defaults?
2. **Emergency breeding threshold**: Is 3 animals a good threshold, or should it be species-specific?
3. **Water requirement**: Should all land animals need water nearby, or only certain species?
4. **Territory radius**: Should wolves have larger territories than cows?
