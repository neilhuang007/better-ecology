# Sniffer Behaviors Implementation Summary

## Overview
Implemented comprehensive sniffer behaviors for the Better Ecology Minecraft mod, adding scientifically-based behaviors including enhanced smell detection, digging for ancient seeds, parent teaching, and social communication.

## Files Created

### Behavior Classes (src/main/java/me/javavirtualenv/behavior/sniffer/)

1. **SnifferBehavior.java** (base class)
   - Extends `SteeringBehavior`
   - Provides common utilities for scent detection and seed location memory
   - Manages scent markers with persistence tracking
   - Evaluates digging sites based on multiple factors
   - Stores digging memory for efficient foraging

2. **SniffingBehavior.java**
   - Implements enhanced smell detection for seed locations
   - Three states: SEARCHING, APPROACHING, SNIFFING
   - Spawns sniffing particles and plays sounds
   - Shares discoveries with other sniffers in range
   - Uses scent trails to locate buried seeds

3. **DiggingBehavior.java**
   - Handles the digging process for ancient seeds
   - Four states: SEARCHING, APPROACHING, DIGGING, EXHAUSTED
   - Biome-specific seed drops:
     - Plains, forests: Torchflower seeds
     - Dark forests, swamps, jungles: Pitcher pods
     - Meadows: Ancient moss
     - Cherry groves: Rare ancient seeds
   - Daily digging limit with automatic reset
   - Particle and sound effects during digging

4. **SnifferSocialBehavior.java**
   - Implements parent teaching for baby sniffers
   - Baby sniffers follow adults and watch them dig
   - Adults teach babies periodically
   - Social communication of seed discoveries
   - Group coordination for efficient foraging

5. **SniffingGoal.java**
   - AI goal integrating SniffingBehavior with Minecraft's goal system
   - Manages sniffing state machine
   - Detects when discoveries are made

6. **SnifferSocialGoal.java**
   - AI goal integrating SnifferSocialBehavior with Minecraft's goal system
   - Handles teaching and communication behaviors
   - Coordinates group activities

7. **package-info.java**
   - Documentation for the sniffer behavior package

### Configuration (src/main/resources/data/better-ecology/mobs/passive/sniffer/)

1. **behaviors.json**
   - Complete configuration for sniffer behaviors
   - Behavior weights for all activities
   - Hunger, condition, energy settings
   - Movement parameters
   - Production settings for ancient seeds
   - Sniffer-specific parameters:
     - Smell radius: 24 blocks
     - Scent persistence: 1200 ticks (60 seconds)
     - Seed memory size: 10 locations
     - Digging duration: 80 ticks (4 seconds)
     - Daily dig limit: 6 digs
     - Teaching range: 16 blocks
     - Communication range: 32 blocks
     - Rare seed chance: 5%
     - Ancient moss chance: 15%

### Modified Files

1. **SnifferMixin.java**
   - Added imports for new sniffer behaviors
   - Registered SniffingGoal (priority 4)
   - Registered SnifferSocialGoal (priority 6)
   - Updated documentation

2. **BehaviorHandle.java**
   - Added imports for sniffer behaviors
   - Registered sniffing, digging, and social behaviors in createRegistry()
   - Added sniffer behavior weights to buildWeights()

3. **BehaviorWeights.java**
   - Added sniffing weight (default 1.5)
   - Added digging weight (default 2.0)
   - Added snifferSocial weight (default 1.2)
   - Added getters and setters for new weights
   - Updated javadoc documentation

## Key Features Implemented

### 1. Enhanced Smell Detection
- Sniffers can detect seeds through smell with a 24-block radius
- Scent markers persist for 60 seconds
- Multiple scents tracked simultaneously
- Visual and audio feedback during sniffing

### 2. Digging for Ancient Seeds
- Sniffers dig up dirt/grass blocks to find ancient seeds
- Biome-specific seed types based on location
- Daily limit of 6 digs per sniffer
- Automatic reset at dawn
- Dirt blocks convert to dirt paths after digging
- Rare ancient seed variants (5% chance)

### 3. Parent Teaching
- Adult sniffers teach babies how to dig
- Babies follow adults within 8 blocks
- Teaching sessions last 200 ticks (10 seconds)
- Babies learn by watching adults dig
- Promotes knowledge transfer between generations

### 4. Social Communication
- Sniffers share seed discoveries with group
- 32-block communication range
- Coordinates group foraging activities
- Prevents redundant digging at same locations

### 5. Seed Location Memory
- Sniffers remember up to 10 good digging spots
- Memory persists based on digging success
- Avoids recently dug locations
- Efficient foraging pattern

### 6. Energy and Condition System
- Digging costs 2.0 energy per action
- Sniffing costs 0.3 energy per action
- Condition bonus when well-fed
- Hunger penalty when starving
- Production quality affected by condition

## Scientific Basis

The implementation is based on research into:
- **Olfaction-based foraging**: Many animals use smell to locate buried food
- **Social learning**: Young animals learn foraging techniques from adults
- **Spatial memory**: Animals remember productive foraging locations
- **Optimal foraging theory**: Balance energy expenditure with food acquisition
- **Fission-fusion societies**: Temporary groups for resource exploitation

## Configuration Options

All behaviors are configurable via JSON for hot-reloading:

```json
"sniffer_behaviors": {
  "smell_radius": 24.0,           // Detection range
  "scent_persistence_ticks": 1200, // How long scents last
  "seed_memory_size": 10,         // Number of locations to remember
  "digging_speed": 0.2,           // Movement speed while digging
  "digging_duration": 80,         // Ticks to complete digging
  "daily_dig_limit": 6,           // Maximum digs per day
  "teaching_range": 16.0,         // Range for parent teaching
  "communication_range": 32.0,    // Range for sharing discoveries
  "teaching_duration": 200,       // Ticks for teaching session
  "rare_seed_chance": 0.05,       // 5% chance for rare seeds
  "ancient_moss_chance": 0.15,    // 15% chance for moss
  "biome_specific_seeds": true    // Enable biome-specific drops
}
```

## Integration Points

1. **With existing systems**:
   - Uses HungerHandle for energy costs
   - Uses EnergyHandle for digging limits
   - Uses ConditionHandle for production quality
   - Uses BehaviorHandle for steering behaviors
   - Integrates with Minecraft's goal system

2. **With other mods**:
   - Compatible with other mob behavior mods
   - Respects vanilla sniffer behaviors
   - Can be disabled entirely via config

## Performance Considerations

- Scent detection uses efficient spatial queries
- Memory size limited to prevent memory issues
- Particle effects spawn periodically, not every tick
- Social queries use bounding box optimization
- Daily limits prevent infinite digging

## Testing Recommendations

1. Verify sniffer spawns with all behaviors registered
2. Test sniffing detection range and accuracy
3. Confirm biome-specific seed drops
4. Verify parent-child interactions
5. Test social communication between sniffers
6. Check daily dig limit resets
7. Validate memory system for digging spots
8. Test energy and condition impacts

## Future Enhancements

Possible additions:
- Sniffer "waggle dance" to communicate exact locations
- Seasonal variations in seed availability
- Different seed types for more biomes
- Baby sniffer learning curve
- Pack hunting for rare seeds
- Nest building behavior
- Long-distance migration patterns

## Files Summary

**Created: 7 new behavior classes, 1 config file, 1 documentation file**
**Modified: 3 existing files**
**Total: ~1500 lines of new code**

All code follows the project's conventions:
- camelCase naming
- Modular, single-responsibility functions
- No deep nesting (max 2 levels)
- Comprehensive documentation
- Configurable via JSON for hot-reloading
