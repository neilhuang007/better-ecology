# Rabbit Behavior Implementation Summary

## Overview
Comprehensive rabbit behaviors have been successfully implemented for the Better Ecology Minecraft mod. The implementation includes burrowing, thumping warnings, quick evasion, and foraging with food caching capabilities.

## Files Created/Modified

### Core Behavior Files (Created)
1. **BurrowSystem.java** - Manages persistent burrow storage per level
   - Creates and validates burrow locations
   - Tracks burrow occupancy and capacity
   - Saves/loads burrow data to level storage
   - Biome-appropriate burrow types

2. **BurrowType.java** - Enum defining different burrow types
   - GRASSLAND (capacity: 3)
   - FOREST (capacity: 2)
   - DESERT (capacity: 4)
   - SNOW (capacity: 3)
   - WARREN (capacity: 6)
   - HIDING_HOLE (capacity: 1)

3. **RabbitBurrow.java** - Represents individual burrow
   - Position, type, capacity tracking
   - Occupant management
   - **Food storage system** (up to 16 items)
   - NBT serialization for persistence

4. **ThumpBehavior.java** - Warning thumping system
   - Foot thumping with configurable intervals
   - Chain reaction through nearby rabbits (12 block radius)
   - Visual and audio effects (particles, sound)
   - Cooldown system to prevent spam

5. **RabbitThumpConfig.java** - Configuration for thumping
   - Detection range: 16 blocks
   - Alert range: 12 blocks
   - Min distance: 4 blocks
   - Max thumps: 3
   - Thump interval: 10 ticks
   - Cooldown: 60 ticks

6. **RabbitEvasionBehavior.java** - Zigzag escape patterns
   - Unpredictable zigzag evasion with random intensity
   - Freezing behavior when not yet detected
   - Jump capability for obstacle clearing
   - Threat detection (wolves, foxes, ocelots, players)

7. **RabbitEvasionConfig.java** - Configuration for evasion
   - Evasion speed: 1.8x normal
   - Evasion force: 0.25
   - Detection range: 24 blocks
   - Flight initiation: 12 blocks
   - Safety distance: 48 blocks
   - Zigzag change interval: 8 ticks
   - Freeze duration: 40 ticks
   - Jump chance: 30%

8. **RabbitForagingBehavior.java** - Food gathering system
   - Crop eating (carrots, wheat, potatoes, beetroots)
   - Flower and grass consumption
   - **Snow digging** for winter foraging
   - **Food caching** in burrows (30% chance in snow biomes)
   - Standing on hind legs behavior
   - **Eating from burrow storage** when no external food

9. **RabbitForagingConfig.java** - Configuration for foraging
   - Search radius: 4 blocks
   - Eat cooldown: 40 ticks
   - Destroys crops: true (50% chance)
   - Snow dig chance: 30%
   - Snow dig cooldown: 60 ticks
   - Stand chance: 10%
   - Stand duration: 60 ticks

10. **RabbitBehaviorGoal.java** - Integrated goal managing all behaviors
    - Priority-based behavior execution
    - Evasion (highest priority)
    - Thumping warnings
    - Burrow seeking
    - Foraging

11. **RabbitBurrowCachingGoal.java** - Dedicated food caching goal
    - Seeks and collects extra food
    - Returns to burrow to store
    - Activates in snow biomes
    - Considers hunger level

### Modified Files
1. **RabbitMixin.java** - Registers rabbit behaviors
    - Adds RabbitBehaviorGoal at priority 1
    - Adds RabbitBurrowCachingGoal at priority 6
    - Integrates with EcologyComponent system

2. **mod_registry.yaml** - Configuration file
    - Comprehensive rabbit configuration
    - Predation settings (wolves EXTREME threat, foxes EXTREME, ocelots HIGH)
    - Burrow shelter settings
    - Flee behaviors with zigzag patterns

## Behavior Features Implemented

### 1. Burrowing System
- Rabbits dig burrows in valid locations (dirt, grass, sand)
- Burrows provide shelter from weather and predators
- Capacity limits per burrow (1-6 depending on type)
- Multiple rabbits can share larger burrows
- **Food storage**: Up to 16 items stored per burrow
- Persistent storage across world saves

### 2. Thumping Warning
- Rabbits thump foot when detecting threats
- Chain reaction alerts nearby rabbits
- Different thump patterns for different threats
- Visual particle effects and audio feedback
- Configurable cooldown to prevent spam

### 3. Quick Evasion
- **Zigzag patterns**: Random intensity and direction changes
- **Freezing behavior**: Avoids detection when threat is distant
- **Explosive speed**: 1.8x normal movement when fleeing
- **Jump capability**: 30% chance to jump over obstacles
- Threat detection: Wolves, foxes, ocelots, aggressive mobs, players

### 4. Foraging
- **Crop eating**: Carrots, wheat, potatoes, beetroots
- **Flower consumption**: Dandelions, poppies, tulips, etc.
- **Grass grazing**: Grass, tall grass, ferns
- **Snow digging**: Digs through snow in winter biomes
- **Standing on hind legs**: Scouting behavior
- **Food caching**: Stores excess food in burrows (30% chance in snow)
- **Burrow consumption**: Eats from stored food when hungry

### 5. Food Caching System
- Collects extra food when safe
- Stores in burrow for winter/scarce times
- Retrieves from burrow when hungry
- Prioritizes carrots, then dandelions, wheat, potatoes, beetroots
- Visual feedback (heart particles) when caching

## Integration with Other Systems

### Predator-Prey Relationships
- **Wolves**: EXTREME threat, 28 block detection, flee at 1.8x speed
- **Foxes**: EXTREME threat, 24 block detection, flee at 1.8x speed
- **Ocelots**: HIGH threat, 20 block detection, flee at 1.6x speed
- Rabbits are configured as prey in wolf and fox registries

### Component Integration
- **HungerHandle**: Foraging behaviors interact with hunger system
- **EnergyHandle**: Sprinting and fleeing consume energy
- **EcologyComponent**: Stores behavior state and configuration
- **BurrowSystem**: Per-level instance manages all burrows

### Configuration
- All behaviors configurable via YAML (hot-reload enabled)
- Registry-based system allows runtime modifications
- Default values based on scientific research

## Scientific Basis
Behaviors are based on research into lagomorph ecology:
- Zigzag evasion from studies of rabbit escape patterns
- Thumping from rabbit communication research
- Burrowing from European rabbit warren studies
- Food caching from winter survival strategies

## Performance Optimizations
- Per-entity behavior instances avoid shared state
- Cooldown systems prevent excessive computation
- Spatial partitioning for neighbor queries (via BurrowSystem)
- NBT-based persistence for world-save efficiency
- Configurable update intervals

## Testing Recommendations
1. Spawn rabbits and observe burrow creation
2. Approach rabbits and observe thumping chain reactions
3. Chase with wolf/fox to verify evasion patterns
4. Place crops near rabbits and observe foraging
5. Test in snow biome for food caching
6. Verify burrow food storage by waiting and observing retrieval

## Known Limitations
- Burrows are abstract (not physical blocks)
- Food storage is simplified (item IDs only)
- No visual burrow entrance/exit
- Rabbit sharing is capacity-based only

## Future Enhancements
- Physical burrow blocks with tunnel systems
- More sophisticated food caching with item stacks
- Baby rabbit protection behaviors
- Seasonal behavior variations
- More complex warren systems with connected tunnels

## Files Summary
```
src/main/java/me/javavirtualenv/behavior/rabbit/
├── BurrowSystem.java              (327 lines) - Level-wide burrow management
├── BurrowType.java                (70 lines)  - Burrow type enum
├── RabbitBurrow.java              (248 lines) - Individual burrow with food storage
├── ThumpBehavior.java             (342 lines) - Warning thumping system
├── RabbitThumpConfig.java         (115 lines) - Thumping configuration
├── RabbitEvasionBehavior.java     (336 lines) - Zigzag evasion with freezing
├── RabbitEvasionConfig.java       (141 lines) - Evasion configuration
├── RabbitForagingBehavior.java    (462 lines) - Foraging with caching
├── RabbitForagingConfig.java      (127 lines) - Foraging configuration
├── RabbitBehaviorGoal.java        (260 lines) - Integrated goal
└── RabbitBurrowCachingGoal.java   (237 lines) - Food caching goal

src/main/java/me/javavirtualenv/mixin/animal/
└── RabbitMixin.java               (137 lines) - Behavior registration

src/main/resources/data/better-ecology/mobs/passive/rabbit/
├── mod_registry.yaml              (873 lines) - Complete configuration
└── unique_registry.yaml           - Unique behaviors
```

Total: ~3,678 lines of code implementing comprehensive rabbit behaviors.
