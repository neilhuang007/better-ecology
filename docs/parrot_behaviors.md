# Parrot Behaviors

This document describes the comprehensive parrot behavior system implemented for Better Ecology.

## Overview

Parrots in Better Ecology have been enhanced with scientifically-inspired behaviors that make them fun and useful companions:

- **Mimicking System**: Parrots mimic hostile mob sounds as a warning system
- **Dancing Behavior**: Parrots dance to music with different styles based on the disc
- **Music Detection**: Parrots detect and fly toward music sources
- **Flight and Perching**: Parrots seek high perches and can ride on player shoulders

## Implementation

### Core Classes

#### Behavior Classes
- `MimicBehavior` - Handles mimicking hostile mob sounds
- `MusicDetectionBehavior` - Detects and tracks music sources
- `DanceBehavior` - Performs dance moves based on music style
- `PerchBehavior` - Manages perching and flight behavior

#### AI Goals
- `ParrotMimicGoal` - Executes mimicking behavior
- `ParrotMusicGoal` - Handles music detection and dancing
- `ParrotPerchGoal` - Manages perching and flight
- `ParrotBehaviorGoal` - Orchestrates all parrot behaviors

#### Handle
- `ParrotBehaviorHandle` - Integrates all behaviors with the EcologyComponent system

## Mimicking System

### Features
- Mimics 12 different hostile mob types
- Individual parrot accuracy variation (55-95% base)
- Warning mimics when hostiles are nearby
- Cooldown system to prevent spam
- Configurable weights for different mob types

### Mimic Types
| Mob Type | Sound Event | Rarity |
|----------|-------------|--------|
| Zombie | PARROT_IMITATE_ZOMBIE | Common |
| Skeleton | PARROT_IMITATE_SKELETON | Common |
| Spider | PARROT_IMITATE_SPIDER | Common |
| Creeper | PARROT_IMITATE_CREEPER | Uncommon |
| Enderman | PARROT_IMITATE_ENDERMAN | Rare |
| Witch | PARROT_IMITATE_WITCH | Rare |
| Wither Skeleton | PARROT_IMITATE_WITHER_SKELETON | Rare |
| Ravager | PARROT_IMITATE_RAVAGER | Rare |
| Phantom | PARROT_IMITATE_PHANTOM | Rare |
| Hoglin | PARROT_IMITATE_HOGLIN | Rare |
| Piglin | PARROT_IMITATE_PIGLIN | Rare |
| Vindicator | PARROT_IMITATE_VINDICATOR | Rare |

### Warning System
When a hostile mob is within 32 blocks:
- 80% chance to perform a warning mimic
- Accuracy increases by 15% for warnings
- Warns players of nearby threats
- Shorter cooldown (100 ticks vs 200 ticks)

## Dancing Behavior

### Dance Styles
Different music discs trigger different dance styles:

| Disc | Dance Style | Description |
|------|-------------|-------------|
| 13, cat | BOUNCE | Rhythmic jumping every 10 ticks |
| blocks, chirp | SPIN | 45-degree rotations every 5 ticks |
| far, mall | WIGGLE | Rapid small movements and head shaking |
| mellohi, stal | HEAD_BOB | Vertical bobbing motion |
| strad, ward | WING_FLAP | Frequent jumping with rotations |
| 11, wait | PARTY | Energetic bouncing and spinning |
| otherside, 5, pigstep, relic | RAVE | Fast spinning with strobe jumping |
| pigstep | DISCO | Smooth rhythmic movements |

### Party Effect
- Other parrots within 16 blocks have a 30% chance to join
- Dancing parrots emit note particles
- Creates a fun visual spectacle
- All parrots dance in sync

### Dance Mechanics
- **BOUNCE**: Jump every 10 ticks with slight rotation changes
- **SPIN**: Rotate 45 degrees every 5 ticks
- **WIGGLE**: Rapid small rotations (15 degrees) and forward/back movement
- **HEAD_BOB**: Jump every 8 ticks with head angle changes
- **WING_FLAP**: Jump every 6 ticks with 30-degree rotations
- **PARTY**: Jump every 5 ticks with random spins and direction changes
- **RAVE**: Spin 30 degrees per tick with strobe jumping
- **DISCO**: Smooth sine-wave based bobbing and rotation

## Music Detection

### Detection Range
- **Default**: 64 blocks
- Configurable via `music.detection_radius`

### Music Sources
- **Jukeboxes**: Detects any playing disc
- **Note Blocks**: Remembers plays for 5 seconds (100 ticks)

### Behavior Sequence
1. Parrot detects music within range
2. Determines dance style from the disc
3. Flies toward music source at 1.2x speed
4. Perches nearby (within 6 blocks)
5. Starts dancing with appropriate style

### Note Block Integration
- Mixin intercepts note block plays
- Notifies all parrots within 64 blocks
- Parrots remember note block location and time
- Allows dancing to player music

## Flight and Perching

### Perch Preferences
- **High perches**: Prefer locations 5+ blocks above minimum build height
- **Isolation**: Avoid perches near threats (cats, foxes, wolves, ocelots)
- **Tree coverage**: Bonus score for leaf blocks
- **Block preferences**: Fences and logs get bonus points

### Perch Scoring
```
Score = (height * height_bonus)
        - (distance * distance_penalty)
        + (leaf_bonus if leaves)
        + (preferred_block_bonus if preferred block)
        - (threat_penalty / threat_distance)
```

### Shoulder Perching
- Only tamed parrots can perch on shoulders
- 3-block detection range
- Uses vanilla shoulder riding system
- Can be spooked off by nearby threats

### Flight Patterns
- **Short bursts**: Direct flight to target perch
- **Gliding**: Safe descent to ground
- **Speed**: 0.8x for gliding, 1.2x for music flights

### Perch Duration
- Maximum: 1200 ticks (60 seconds)
- Random adjustment movements every 40 ticks
- Leave immediately if threatened

## Configuration

### JSON Configuration Location
`src/main/resources/data/better-ecology/mobs/passive/parrot.json`

### Key Settings

#### Mimic Configuration
```json
{
  "mimic": {
    "base_accuracy": 0.75,
    "accuracy_variation": 0.2,
    "mimic_chance": 0.05,
    "warning_range": 32.0,
    "warning_chance": 0.8
  }
}
```

#### Music Configuration
```json
{
  "music": {
    "detection_radius": 64,
    "flight_speed": 1.2,
    "prefer_high_perches": true
  }
}
```

#### Dance Configuration
```json
{
  "dance": {
    "show_particles": true,
    "enable_party_effect": true,
    "party_radius": 16.0,
    "party_join_chance": 0.3
  }
}
```

#### Perch Configuration
```json
{
  "perch": {
    "search_radius": 32,
    "prefer_high_perches": true,
    "min_perch_height": 5,
    "spook_radius": 8.0,
    "max_perch_ticks": 1200
  }
}
```

## Behavior Priorities

The `ParrotBehaviorGoal` orchestrates behaviors in priority order:

1. **Shoulder Perching** (highest) - If riding a player
2. **Dancing** - If music is playing and parrot has arrived
3. **Flying to Music** - If music detected within range
4. **Perching** - If currently on a perch
5. **Flying to Perch** - Seeking a new perch
6. **Idle** - Random wandering

## Scientific Inspiration

### Mimicry
Based on real-world parrot ability to mimic sounds:
- African Grey Parrots can learn 100+ words
- Alarm calls warn flock members of predators
- Individual variation in mimicry accuracy

### Dancing
Based on research into birds responding to music:
- Cockatoos bob to beat (Snowball the cockatoo)
- Parrots have innate sense of rhythm
- Social bonding through synchronized movement

### Perching
Based on wild parrot behavior:
- Prefer high perches for predator detection
- Sleep in roosting flocks for safety
- Seek isolated spots away from threats

## Performance Considerations

### Optimization Strategies
- **Reduced scanning**: Music detection only every 60 ticks
- **Early exits**: Skip processing if condition not met
- **Cached lookups**: Component data cached per tick
- **Spatial indexing**: Uses existing SpatialIndex for neighbor queries

### Memory Usage
- Component tags store minimal state
- No persistent entity references
- Efficient NBT storage for mimics and perches

## Future Enhancements

### Planned Features
- More dance styles for custom music discs
- Parrot vocalizations based on biome
- Flock behavior with coordinated flight
- Nest building integration with perching
- Teaching young parrots to mimic

### Potential Expansions
- Parrot "conversations" between multiple parrots
- Mimic learning from player actions
- Regional mimic variations
- Night roosting behavior

## Troubleshooting

### Parrots Not Dancing
- Check if jukebox has a disc inserted
- Verify parrot is within 64 blocks
- Ensure parrot is not on player's shoulder
- Check that music behavior is enabled in config

### Parrots Not Mimicking
- Verify hostile mobs are nearby for warning mimics
- Check mimic cooldown (200 ticks between random mimics)
- Ensure mimic behavior is enabled in config
- Individual parrots have lower accuracy (55-95%)

### Parrots Not Perching
- Check if parrot is tamed for shoulder perching
- Verify there are suitable perches within 32 blocks
- Ensure perch behavior is enabled in config
- Parrots may be spooked by nearby threats

## References

### Scientific Papers
- [Bird vocal mimicry](https://doi.org/10.1111/j.1474-919X.2006.00517.x)
- [Rhythm perception in birds](https://doi.org/10.1038/s41586-019-1273-0)
- [Parrot social behavior](https://doi.org/10.1016/j.anbehav.2010.01.025)

### Minecraft Documentation
- [Parrot - Minecraft Wiki](https://minecraft.fandom.com/wiki/Parrot)
- [Music Discs](https://minecraft.fandom.com/wiki/Music_Disc)
- [Note Block](https://minecraft.fandom.com/wiki/Note_Block)
