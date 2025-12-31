# Pre-Computed Spawn Position System

## Overview

This spawning system uses a **pre-computed spawn position** approach inspired by Distant Horizons' chunk loading system. Instead of checking spawn conditions every tick, spawn positions are calculated **once during chunk generation** and stored for later use.

## Architecture

```
World Generation Phase                    Chunk Loading Phase
======================                    ====================
ChunkGenerated Event                      ChunkLoaded Event
        |                                        |
        v                                        v
SpawnPositionCalculator                 ChunkLoadSpawner
        |                                        |
        v                                        v
  Sample 16 positions                 Check player proximity
        |                                        |
        v                                        v
Validate spawn conditions              Spawn pre-computed mobs
        |                                        |
        v                                        v
Store in ChunkSpawnData              Remove used positions
        |
        v
LevelChunkMixin.saveAll()
        |
        v
Write to chunk NBT
```

## Components

### 1. ChunkSpawnData

Data structure holding spawn positions per entity type for a single chunk.

```java
// Stores positions as: Map<EntityType<?>, Set<BlockPos>>
// Uses Set to prevent duplicate positions
// Serialized to chunk NBT for disk persistence
```

**Key Methods:**
- `addSpawnPosition(EntityType, BlockPos)` - Add a spawn position
- `getSpawnPositions(EntityType)` - Get all positions for a type
- `removeSpawnPosition(EntityType, BlockPos)` - Remove after spawning
- `toNbt() / fromNbt()` - Serialization

### 2. SpawnPositionCalculator

Calculates valid spawn positions during chunk generation.

**Process:**
1. For each ecology profile with spawning enabled
2. Sample up to 16 random positions in the chunk
3. Validate each position against spawn conditions
4. Store valid positions in ChunkSpawnData

**Spawn Conditions Checked:**
- Biome matching (supports `namespace:*` wildcards)
- Light level (min/max)
- Block requirements (grass, water, etc.)
- Altitude range
- Cave detection (for bats)
- Weather conditions

### 3. ChunkLoadSpawner

Spawns mobs when chunks load near players.

**Process:**
1. On chunk load, check if any player is within 8 chunks
2. If yes, retrieve ChunkSpawnData for the chunk
3. For each pending spawn position:
   - Validate the position is still safe
   - Spawn the entity
   - Remove the position from ChunkSpawnData

### 4. ChunkSpawnDataStorage

In-memory cache for chunk spawn data. Uses `ConcurrentHashMap<ChunkPos, ChunkSpawnData>` for thread-safe access.

**Key Methods:**
- `get(LevelChunk)` - Retrieve spawn data
- `set(LevelChunk, ChunkSpawnData)` - Store spawn data
- `remove(LevelChunk)` - Cleanup after spawns complete
- `clear()` - Server shutdown cleanup

### 5. LevelChunkMixin

Mixin injecting into LevelChunk to enable NBT persistence.

**Injections:**
- `loadAll()` - Load spawn data from chunk NBT
- `saveAll()` - Save spawn data to chunk NBT
- Implements `ChunkPersistentData` interface

### 6. SpawnBootstrap

Registers event handlers and initializes the system.

**Events:**
- `ServerChunkEvents.CHUNK_GENERATE` - Calculate spawn positions
- `ServerChunkEvents.CHUNK_LOAD` - Spawn pre-computed mobs
- `ServerTickEvents.END_SERVER_TICK` - Update density tracking

## Configuration

Spawn behavior is configured via YAML profiles:

```yaml
# Example: minecraft:cow profile
population:
  spawning:
    enabled: true
    weight: 10
    group_size: [2, 4]        # Min-max group size
    conditions:
      biomes: ["minecraft:plains", "minecraft:*"]
      light: [0, 15]          # Min-max light level
      blocks: ["minecraft:grass_block"]
      altitude: [60, 128]     # Y-level range
      in_caves: false         # Only for bats
      weather: "any"          # any/clear/rain/thunder
```

## Performance Benefits

| Traditional Spawning | Pre-Computed System |
|---------------------|---------------------|
| Check conditions every tick | Check once during generation |
| O(entities × chunks) per tick | O(1) lookup per chunk load |
| Random position sampling | Cached valid positions |
| Can spawn anywhere | Only pre-validated positions |

## Flow Diagram

```
Server Startup
       |
       v
EcologyBootstrap.init()
       |
       v
SpawnBootstrap.init()
       |
       +-- Register CHUNK_GENERATE -> SpawnPositionCalculator
       +-- Register CHUNK_LOAD -> ChunkLoadSpawner
       +-- Register END_SERVER_TICK -> SpawnDensityTracker

During World Generation
       |
       v
ChunkGenerated
       |
       v
SpawnPositionCalculator.calculateForChunk()
       |
       +-- For each ecology profile
       +-- Sample 16 random positions
       +-- Validate spawn conditions
       +-- Store in ChunkSpawnData
       +-- Save to ChunkSpawnDataStorage
       |
       v
Chunk saved to disk (NBT)

When Chunk Loads Near Player
       |
       v
ChunkLoaded
       |
       v
ChunkLoadSpawner.onChunkLoad()
       |
       +-- Check player proximity (< 8 chunks)
       +-- Get ChunkSpawnData from storage
       +-- For each pending spawn:
       |      +-- Validate position still safe
       |      +-- Spawn entity
       |      +-- Remove from ChunkSpawnData
       |
       v
Entities spawned naturally
```

## Density Tracking

The `SpawnDensityTracker` maintains entity counts per chunk to prevent overpopulation:

- Updates every 200 ticks (10 seconds)
- Uses `Long2IntMap` for O(1) chunk key lookups
- Chunk key: `chunkX << 32 | chunkZ`
- Tracks actual spawned entities, not pending spawns

## Integration Points

1. **EcologyProfile** - Reads spawn config from YAML
2. **SpawnHandle** - Provides typed access to spawn configuration
3. **SpawnConditionChecker** - Validates individual spawn positions
4. **BiomeSpawnModifier** - Injects spawn settings into vanilla biomes
5. **NaturalSpawnerMixin** - Enforces density limits on vanilla spawning

## Files

| File | Purpose |
|------|---------|
| `ChunkSpawnData.java` | Data structure for spawn positions |
| `ChunkSpawnDataStorage.java` | In-memory storage cache |
| `SpawnPositionCalculator.java` | Calculate positions during generation |
| `ChunkLoadSpawner.java` | Spawn mobs when chunks load |
| `SpawnBootstrap.java` | Event registration and initialization |
| `SpawnConditionChecker.java` | Validate spawn conditions |
| `SpawnDensityTracker.java` | Track entity counts per chunk |
| `BiomeSpawnModifier.java` | Modify biome spawn settings |
| `LevelChunkMixin.java` | NBT persistence for spawn data |
| `ChunkPersistentData.java` | Interface for chunk persistent data |
