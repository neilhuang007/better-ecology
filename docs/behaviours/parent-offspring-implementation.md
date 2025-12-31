# Parent-Offspring Behaviors Implementation

## Overview

This package implements parent-offspring attachment and care behaviors for Minecraft mobs, based on research into mammalian maternal behaviors, filial imprinting, and separation distress.

## Files

### 1. FollowMotherBehavior.java
Implements offspring following behavior for "follower" species (e.g., cows, sheep).

**Key Features:**
- Distance-based following that increases with age
- Uses mother's UUID for persistent tracking
- Seeks nearest adult if mother not found
- Arrive steering for smooth approach

**Configuration:**
- `baseFollowDistance`: Starting distance (default: 4.0 blocks)
- `maxFollowDistance`: Maximum distance at adulthood (default: 12.0 blocks)
- `followSpeed`: Movement speed modifier (default: 1.0)
- `slowingRadius`: Distance to start decelerating (default: 2.0 blocks)
- `adulthoodAge`: Age when independent (default: -24000 ticks)

**Methods:**
- `calculate(BehaviorContext)`: Main steering calculation
- `findMother(AgeableMob)`: Locates mother entity
- `calculateFollowDistance(AgeableMob)`: Scales distance with age

### 2. MotherProtectionBehavior.java
Implements maternal aggression and protection of offspring.

**Key Features:**
- Scans for threats near babies within protection range
- Prioritizes most threatened baby
- Attacks predators (wolves, zombies, etc.)
- Scales aggression with threat level

**Configuration:**
- `protectionRange`: Radius to protect babies (default: 16.0 blocks)
- `threatDetectionRange`: Range to detect predators (default: 24.0 blocks)
- `attackSpeed`: Speed toward threats (default: 1.5)
- `aggressionLevel`: Attack intensity multiplier (default: 1.0)

**Methods:**
- `calculate(BehaviorContext)`: Returns steering toward threats
- `findNearestThreatToOffspring(AgeableMob)`: Locates nearest predator
- `isPredator(Entity)`: Identifies predatory entities

### 3. SeparationDistressBehavior.java
Implements separation distress calls and mother response.

**Key Features:**
- Offspring vocalize when separated beyond threshold
- Mother responds and moves toward distressed baby
- Cooldown prevents spam vocalizations
- Species-specific sounds

**Configuration:**
- `distressThreshold`: Distance causing distress (default: 16.0 blocks)
- `callCooldown`: Minimum time between calls (default: 60.0 seconds)
- `responseSpeed`: Mother's speed to respond (default: 1.2)
- `isMother`: True for mother, false for offspring

**Methods:**
- `calculateMotherResponse()`: Mother responds to calls
- `calculateOffspringDistress()`: Baby calls when separated
- `getDistressSoundForType()`: Species-specific vocalizations

### 4. HidingBehavior.java
Implements "hider" species behavior (e.g., deer fawns, rabbits).

**Key Features:**
- Offspring hide in tall grass/bushes when mother away
- Stays immobile at hiding spot
- Emerges when mother returns
- Finds optimal hiding locations

**Configuration:**
- `motherReturnThreshold`: Distance to trigger hiding (default: 8.0 blocks)
- `hidingDetectionRange`: Search radius for hiding spots (default: 32.0 blocks)
- `emergeSpeed`: Speed when rejoining mother (default: 1.3)
- `maxHideDuration`: Maximum time in hiding (default: 6000 ticks)

**Methods:**
- `findHidingSpot(AgeableMob)`: Locates optimal cover
- `evaluateHidingSpot()`: Scores locations by concealment
- `enterHidingState()`: Activates hiding behavior
- `exitHidingState()`: Returns to mother

### 5. ParentOffspringConfig.java
Configuration for species-specific parameters.

**Predefined Species:**
- **Followers**: cow, sheep, pig, chicken, wolf, cat, horse, fox, panda, polar_bear, turtle, bee, axolotl, frog, goat, llama, mooshroom, ocelot, parrot, dolphin, squid, glow_squid, sniffer, armadillo
- **Hiders**: rabbit, deer

**SpeciesConfig Parameters:**
```java
SpeciesConfig config = new SpeciesConfig(
    SpeciesType.FOLLOWER,           // FOLLOWER or HIDER
    baseFollowDistance,              // Starting follow distance
    maxFollowDistance,               // Maximum follow distance
    followSpeed,                     // Movement speed
    slowingRadius,                   // Deceleration radius
    adulthoodAge,                    // Age of independence
    protectionRange,                 // Mother protection radius
    threatDetectionRange,            // Threat detection radius
    attackSpeed,                     // Attack speed
    aggressionLevel,                 // Aggression multiplier
    motherReturnThreshold,           // Hider: distance to emerge
    separationDistressThreshold,     // Distance for distress calls
    isHider                          // True for hider species
);
```

## Usage Example

```java
// Create config
ParentOffspringConfig config = new ParentOffspringConfig();
SpeciesConfig cowConfig = config.getSpeciesConfig("cow");

// Create behaviors for a baby cow
FollowMotherBehavior followBehavior = new FollowMotherBehavior(
    cowConfig.baseFollowDistance,
    cowConfig.maxFollowDistance,
    cowConfig.followSpeed,
    cowConfig.slowingRadius,
    cowConfig.adulthoodAge
);

MotherProtectionBehavior protectionBehavior = new MotherProtectionBehavior(
    cowConfig.protectionRange,
    cowConfig.threatDetectionRange,
    cowConfig.attackSpeed,
    cowConfig.aggressionLevel
);

SeparationDistressBehavior distressBehavior = new SeparationDistressBehavior(
    cowConfig.separationDistressThreshold,
    60.0,  // call cooldown
    1.2,   // response speed
    false  // isMother (false for baby)
);

// Add to behavior system
behaviorController.addBehavior(followBehavior);
behaviorController.addBehavior(protectionBehavior);
behaviorController.addBehavior(distressBehavior);
```

## Research-Based Design

### Following Behaviors
- Based on filial imprinting in precocial mammals
- Distance increases with age (gradual independence)
- Strong attachment to first moving object (mother)

### Hider vs Follower Strategies
- **Hider species** (deer, rabbits): Offspring remain concealed
- **Follower species** (cows, sheep): Offspring follow immediately
- Dichotomy well-documented in ungulate research

### Maternal Protection
- Transient aggression during lactation period
- Triggered by predator proximity to offspring
- Prioritizes most threatened baby

### Separation Distress
- Universal mammalian response to maternal separation
- Immediate cessation upon reunion
- Age-dependent intensity

## Technical Notes

### Entity Tracking
- Uses UUID for persistent mother-offspring bonds
- Falls back to nearest adult if mother not found
- Supports both getParent() and manual UUID assignment

### Performance Considerations
- Limited search radius for entity queries
- Cooldown timers prevent excessive vocalizations
- Caching of last known positions

### Minecraft Integration
- Works with AgeableMob (vanilla baby animal class)
- Uses PersistentData for state flags
- Respects vanilla age system (getAge())

## Future Enhancements

1. **Allomothering**: Other adults help protect young (elephant-like)
2. **Carrying behaviors**: Mothers transport offspring (primates)
3. **Weaning**: Gradual reduction of dependency
4. **Den-based rearing**: Carnivore den behavior
5. **Social learning**: Mothers teach offspring behaviors

## References

See `docs/behaviours/03-parent-offspring-attachment.md` for full research references.
