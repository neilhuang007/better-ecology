# Sheep Behavior Implementation Summary

## Overview

This document summarizes the comprehensive sheep behavior system implemented for the Better Ecology Minecraft mod. The implementation includes wool growth, grazing, flock dynamics, and lamb care behaviors.

## Files Created

### 1. Core Behavior Classes

#### `WoolHandle.java`
**Location:** `src/main/java/me/javavirtualenv/ecology/handles/WoolHandle.java`

**Purpose:** Manages wool growth, quality, and shearing mechanics.

**Features:**
- Wool growth in 4 stages based on nutrition
- Quality calculation (0.0-1.0) from health, condition, and hunger
- Integration with hunger system for growth requirements
- Shearing tracking and wool drop calculation
- Grazing integration for hunger restoration

**Key Methods:**
- `growWool()` - Handles wool growth over time
- `tryGrazing()` - Attempts to eat grass blocks
- `updateWoolQuality()` - Calculates wool quality
- `calculateWoolDrop()` - Determines wool drop amount
- `handleShearing()` - Processes shearing events

#### `SheepGrazeGoal.java`
**Location:** `src/main/java/me/javavirtualenv/ecology/ai/SheepGrazeGoal.java`

**Purpose:** AI goal for sheep to actively seek and graze grass.

**Features:**
- Searches for grass within 16 blocks
- Moves toward grass when hungry
- Converts grass to dirt
- Restores 25 hunger points
- Eating sounds and particle effects

**Key Methods:**
- `canUse()` - Determines when grazing should start
- `findNearbyGrass()` - Locates nearest grass blocks
- `eatGrass()` - Performs grass consumption
- `spawnEatParticles()` - Visual feedback

#### `EweProtectLambGoal.java`
**Location:** `src/main/java/me/javavirtualenv/ecology/ai/EweProtectLambGoal.java`

**Purpose:** AI goal for adult ewes to protect and care for lambs.

**Features:**
- Tracks nearby lambs within 24 blocks
- Moves toward lamb if >8 blocks away
- Detects lamb danger (fleeing or hurt)
- Protective response with increased speed
- Communicative bleating

**Key Methods:**
- `findNearestLamb()` - Locates closest lamb
- `isLambInDanger()` - Detects threat to lamb
- `protectLamb()` - Protective response behavior
- `bleat()` - Communication sound

### 2. Steering Behaviors

#### `SheepFlockCohesionBehavior.java`
**Location:** `src/main/java/me/javavirtualenv/behavior/sheep/SheepFlockCohesionBehavior.java`

**Purpose:** Strong flock cohesion behavior specific to sheep.

**Features:**
- Higher weight (1.5) for strong cohesion
- Maintains proximity within 16 blocks
- Steers toward flock center when distant
- Distress factor calculation for separation

**Key Methods:**
- `calculate()` - Computes steering force toward flock
- `calculateDistressFactor()` - Measures separation distress
- `setFlockRadius()` - Configures cohesion radius

#### `SheepSeparationDistressBehavior.java`
**Location:** `src/main/java/me/javavirtualenv/behavior/sheep/SheepSeparationDistressBehavior.java`

**Purpose:** Separation distress when isolated from flock.

**Features:**
- Frequent bleating when distressed
- Higher-pitched sounds with increased distress
- Moves faster to regroup
- Worry particle effects
- Distress level calculation (0.0-1.0)

**Key Methods:**
- `calculate()` - Computes distress-based steering
- `isDistressed()` - Checks distress state
- `bleat()` - Plays distress vocalization
- `triggerDistressBehavior()` - Activates distress responses

### 3. Configuration Files

#### `behaviors.json`
**Location:** `src/main/resources/data/better-ecology/mobs/passive/sheep/behaviors.json`

**Purpose:** Configuration for all sheep behaviors.

**Configuration Sections:**
- `wool` - Wool growth and shearing settings
- `behaviors` - Steering behavior weights
- `grazing` - Grazing AI settings
- `lamb_care` - Ewe protection settings
- `flock_dynamics` - Cohesion and distress settings

### 4. Documentation

#### `sheep_behaviors.md`
**Location:** `docs/sheep_behaviors.md`

**Purpose:** Comprehensive documentation of sheep behaviors.

**Contents:**
- Feature overview and configuration
- Scientific basis for behaviors
- Integration with existing systems
- Performance considerations
- Debugging guide
- Future enhancements

## Files Modified

### `SheepMixin.java`
**Location:** `src/main/java/me/javavirtualenv/mixin/animal/SheepMixin.java`

**Changes:**
1. Added imports for new wool and AI classes
2. Added `SheepWoolHandle` to behavior registry
3. Updated `SheepMovementHandle` to register new goals:
   - `registerGrazeGoal()` - Priority 4
   - `registerLambProtectionGoal()` - Priority 2
4. Implemented `SheepWoolHandle` inner class with:
   - Wool growth tracking (0-4 stages)
   - Quality calculation
   - Shearing time tracking
   - Grazing time tracking

## Behavior Integration

### Component System
All behaviors integrate with `EcologyComponent`:
```java
EcologyComponent component = sheep.getComponent();
CompoundTag woolTag = component.getHandleTag("wool");
```

### Hunger System
Wool growth depends on hunger (requires >30%):
```java
int hunger = component.getHandleTag("hunger").getInt("hunger");
```

### Condition System
Wool quality considers condition:
```java
int condition = component.getHandleTag("condition").getInt("condition");
```

### Social System
Separation distress affects social needs:
```java
boolean isLonely = component.state().isLonely();
```

## NBT Data Structure

### Wool Data
```nbt
{
  wool_stage: 4,
  last_shear_time: 12345,
  wool_quality: 0.75,
  last_graze_time: 12350,
  grazing_count: 15
}
```

## Scientific Basis

### Wool Growth
- Based on real sheep wool cycles
- Nutrition affects growth rate
- Quality determined by overall health
- 4 stages represent growth phases

### Flocking Behavior
- Based on ungulate research
- Sheep are highly gregarious
- Strong separation anxiety
- Group protection instincts

### Lamb Care
- Maternal bonding research
- Vocal recognition
- Protective behaviors
- Distance-based care

## Performance Optimizations

1. **Tick Intervals** - Wool updates every 5 seconds
2. **Spatial Queries** - Cached neighbor lists
3. **Cooldowns** - Grazing and bleating timers
4. **Lazy Evaluation** - Calculate only when needed
5. **Thread Safety** - Per-entity NBT storage

## Hot-Reloading

All configurations support hot-reload via `/reload`:
1. Modify JSON files
2. Run `/reload` in-game
3. Behaviors update immediately

## Testing Recommendations

### Manual Testing
1. Spawn sheep in creative mode
2. Observe wool growth over time
3. Test grazing behavior on grass
4. Separate sheep from flock to observe distress
5. Breed sheep and observe ewe-lamb interactions
6. Shear sheep and observe regrowth

### NBT Inspection
```
/data get entity @e[type=sheep,limit=1] better-ecology.wool
```

### Configuration Testing
- Adjust weights in `behaviors.json`
- Test different grazing radii
- Modify wool growth rates
- Experiment with distress thresholds

## Future Enhancements

Potential additions:
- Wool dyeing from flower grazing
- Seasonal growth variations
- Ram dominance behaviors
- Lamb play behaviors
- Nighttime grouping
- Flock leadership inheritance

## Dependencies

### Required
- Minecraft 1.21.1
- Fabric Loader
- Java 21

### Internal
- `EcologyComponent` - State management
- `BehaviorHandle` - Steering behaviors
- `HungerHandle` - Nutrition tracking
- `ConditionHandle` - Health tracking
- `SocialHandle` - Flock needs

## Troubleshooting

### Sheep Not Growing Wool
- Check hunger is >30%
- Verify wool stage <4
- Ensure tick interval passing

### Sheep Not Grazing
- Confirm grass blocks nearby
- Check grazing cooldown expired
- Verify grazing enabled in config

### Sheep Not Flocking
- Check behavior weights
- Verify nearby sheep within radius
- Ensure distress not overriding

### Ewes Not Protecting Lambs
- Confirm ewe is adult (not baby)
- Check lamb within 24 blocks
- Verify protection goal priority

## Conclusion

This implementation provides a comprehensive, scientifically-based sheep behavior system that integrates seamlessly with the Better Ecology mod's existing architecture. All behaviors are configurable, hot-reloadable, and optimized for performance.

The system demonstrates the mod's core philosophy: data-driven, component-based entity behaviors that can be extended and configured without code changes.
