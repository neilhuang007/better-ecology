# Pig Behaviors Implementation Summary

## Overview
Comprehensive pig behaviors have been implemented for the Better Ecology Minecraft mod, including rooting, truffle hunting, mud bathing, social interactions, and crop feeding.

## Files Created

### Behavior Classes
1. **PigRootingGoal.java** - Rooting behavior for finding truffles
   - Searches for rootable blocks (grass, mycelium, podzol, dirt)
   - Spawns truffles based on soil type probabilities
   - Converts grass to dirt (30% chance)
   - Enhanced particle and sound effects

2. **PigMudBathingGoal.java** - Mud bathing for temperature regulation
   - Seeks out water, mud, or clay
   - Restores condition over time
   - Applies 5-minute mud effect
   - Triggered by heat, low health, or random chance

3. **PigFeedingGoal.java** - Crop foraging behavior
   - Seeks and eats mature crops
   - Restores hunger based on crop type
   - Heals 1 HP when eating
   - Higher priority when hungry

4. **PigTruffleSeekGoal.java** - Active truffle tracking
   - Detects truffles within 16 blocks
   - Moves at 1.2x speed toward truffles
   - Shows sniffing and excitement effects
   - 30-second cooldown after seeking

5. **PigSniffTrufflesGoal.java** - Scent detection behavior
   - Detects truffles, crops, and mycelium
   - Pauses to sniff with particle effects
   - Looks toward scent source
   - 10-second cooldown

6. **PigSocialMudBathingGoal.java** - Social bathing behavior
   - Pigs in mud attract other pigs (30% chance)
   - Maximum 4 pigs per group
   - Happy villager particles for groups
   - Social bonding through shared activity

7. **PigBehaviorHandle.java** - Pig-specific state management
   - Tracks mud effect timer
   - Tracks excitement timer
   - Checks for nearby truffles
   - Provides static API for pig state

### Configuration
8. **behaviors.json** - Pig behavior configuration
   - Rooting parameters and truffle chances
   - Mud bathing settings
   - Social interaction ranges
   - Sound and particle configurations

### Client Rendering
9. **PigRenderFeature.java** - Mud effect visual overlay
   - Renders brown tint when pig has mud effect
   - Alpha fades over 5 minutes
   - Registered in client initializer

10. **BetterEcologyClient.java** - Updated client initialization
    - Registers pig renderer with mud layer
    - EntityRendererRegistry integration

### Integration
11. **PigMixin.java** - Complete pig behavior integration
    - Registers all pig goals
    - Implements code-based handles
    - Priority: TruffleSeek (3) > Stroll (4) > Rooting/Mud/Feeding (5) > Sniffing (6)

## Behavior Priority System

Goals are registered with the following priorities:
- Priority 0: FloatGoal (vanilla)
- Priority 3: PigTruffleSeekGoal (high - truffles are valuable!)
- Priority 4: WaterAvoidingRandomStrollGoal (vanilla)
- Priority 5: Rooting, MudBathing, SocialMudBathing, Feeding (medium)
- Priority 6: Sniffing (low - passive behavior)

## Key Features

### Truffle System
- New food item with rare rarity
- 6 hunger, 0.6 saturation
- Found via rooting (5-15% chance depending on soil)
- Pigs detect and seek dropped truffles
- Sniffing behavior when truffles are nearby

### Mud Bathing
- Condition restoration (+2 per second)
- Visual brown overlay for 5 minutes
- Social attraction (other pigs join)
- Temperature regulation in hot weather
- Triggered by low health or heat

### Crop Feeding
- Smart crop detection (only mature crops)
- Hunger restoration based on crop value
- Can destroy entire sections of farmland
- Higher priority when hungry
- Realistic foraging behavior

### Rooting
- Environmental impact (grass → dirt)
- Particle effects show rooting action
- Sniffing sounds during rooting
- Truffle discovery varies by soil type

## Technical Implementation

### EcologyComponent Integration
All pig data stored in handles:
- hunger (current hunger, last damage tick)
- condition (affects breeding/truffle finding)
- energy (depleted by fleeing/sprinting)
- age (tracks maturity)
- social (herd proximity tracking)
- pig_behavior (mud timer, excitement)

### Performance Optimizations
- Cooldown timers prevent excessive checks
- Particle effects limited to server-side
- NBT data cached per-tick
- Bounding box queries for efficiency
- Goal priority system prevents conflicts

### Scientific Accuracy
- Truffle hunting based on real practices
- Mud bathing for thermoregulation
- Rooting behavior from natural foraging
- Social herd dynamics
- Crop feeding as opportunistic foraging

## Testing Checklist

- [ ] Spawn pigs and observe rooting behavior
- [ ] Test truffle drops from different soil types
- [ ] Create mud/water pool and test bathing
- [ ] Verify mud effect visual overlay
- [ ] Build crop farm and test feeding
- [ ] Drop truffle and test seeking behavior
- [ ] Check social bathing with multiple pigs
- [ ] Verify sniffing triggers appropriately
- [ ] Test breeding with new requirements
- [ ] Verify hunger/condition/energy systems

## Future Enhancements

Potential additions:
1. Truffle cooking recipes
2. Pig personality traits
3. Enhanced herd movement
4. Pig-sty shelter seeking
5. More truffle varieties
6. Genetic breeding system
7. Seasonal behavior changes

## Configuration

All behaviors configurable via:
`src/main/resources/data/better-ecology/mobs/passive/pig/behaviors.json`

Parameters include:
- Search ranges and durations
- Truffle spawn chances
- Social attraction rates
- Sound volumes and pitches
- Particle effect counts

## Compatibility

- Integrates with vanilla pig entity
- Compatible with other mods via EcologyComponent API
- Hot-reload support for configuration
- Client-server synchronization
- NBT data persistence

## Credits

Implementation follows Better Ecology architecture patterns:
- Code-based handles in PigMixin
- Goal-based AI system
- EcologyComponent state management
- Scientific research basis
- Performance-focused design
