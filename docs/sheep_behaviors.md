# Sheep Behaviors - Better Ecology Mod

## Overview

The sheep behavior system implements scientifically-based sheep behaviors including wool growth, grazing, flock dynamics, and lamb care. All behaviors are configurable via JSON and can be hot-reloaded.

## Features

### 1. Wool Growth System

**Location:** `me.javavirtualenv.ecology.handles.WoolHandle`

Sheep grow wool over time based on:
- **Nutrition:** Hunger level affects growth rate (requires hunger > 30%)
- **Time:** Wool grows in stages (0-4)
- **Quality:** Determined by health, condition, and nutrition (0.0-1.0)
- **Shearing:** Resets wool growth, drops wool based on quality

**Configuration:**
```json
{
  "wool": {
    "enabled": true,
    "max_wool_stage": 4,
    "min_wool_stage_for_shear": 3,
    "base_growth_chance": 0.15,
    "hunger_threshold": 30,
    "base_graze_chance": 0.35,
    "grazing_cooldown": 200,
    "hunger_restoration": 25,
    "base_wool_drops": 1
  }
}
```

**Wool Quality Effects:**
- Quality > 0.8: Premium (base + 2 wool)
- Quality > 0.6: Good (base + 1 wool)
- Quality > 0.4: Normal (base wool)
- Quality ≤ 0.4: Poor (base - 1 wool, minimum 1)

### 2. Grazing Behavior

**Location:** `me.javavirtualenv.ecology.ai.SheepGrazeGoal`

Sheep actively seek and eat grass blocks to restore hunger.

**Behaviors:**
- Searches for grass within 16 blocks
- Moves toward grass when hungry
- Converts grass to dirt
- Restores 25 hunger
- Plays eating sounds and particles

**Configuration:**
```json
{
  "grazing": {
    "enabled": true,
    "search_radius": 16.0,
    "speed_modifier": 0.8,
    "priority": 4
  }
}
```

### 3. Flock Dynamics

**Location:** `me.javavirtualenv.behavior.sheep.SheepFlockCohesionBehavior`

Sheep have strong flock cohesion instincts.

**Behaviors:**
- Stay within 16 blocks of flock center
- Higher weight (1.5) for strong cohesion
- Experience separation distress when isolated
- Move toward flock when too far (>6 blocks)

**Configuration:**
```json
{
  "behaviors": {
    "cohesion": 1.5,
    "herd_cohesion": 1.6,
    "separation": 1.8
  },
  "flock_dynamics": {
    "cohesion_radius": 16.0,
    "distress_radius": 20.0,
    "min_flock_size": 3
  }
}
```

### 4. Separation Distress

**Location:** `me.javavirtualenv.behavior.sheep.SheepSeparationDistressBehavior`

When separated from their flock, sheep exhibit distress behaviors.

**Distress Levels:**
- **0.0-0.3:** Calm (within 6 blocks of flock)
- **0.3-0.5:** Mildly concerned (6-12 blocks)
- **0.5-1.0:** Distressed (>12 blocks, increases urgency)

**Distress Behaviors:**
- Frequent bleating (interval reduces with distress)
- Higher-pitched bleats when distressed
- Move faster to regroup
- Visual particle effects (worry particles)

**Configuration:**
```json
{
  "behaviors": {
    "separation_distress": 1.7
  }
}
```

### 5. Lamb Care

**Location:** `me.javavirtualenv.ecology.ai.EweProtectLambGoal`

Adult ewes protect and care for lambs.

**Behaviors:**
- Stay within 24 blocks of lambs
- Move toward lamb if >8 blocks away
- Detect when lamb is in danger (fleeing or hurt)
- Protective response when lamb threatened
- Communicative bleating with lamb

**Lamb Danger Detection:**
- Lamb moving quickly (>0.3 speed)
- Lamb health below 70%
- Ewe moves between threat and lamb

**Configuration:**
```json
{
  "lamb_care": {
    "enabled": true,
    "follow_range": 24.0,
    "protection_range": 12.0,
    "speed_modifier": 1.0,
    "priority": 2
  }
}
```

### 6. Sound Effects

Sheep use different vocalizations for communication:

- **Eating:** `SoundEvents.SHEEP_EAT` when grazing
- **Shearing:** `SoundEvents.SHEEP_SHEAR` when sheared
- **Ambient:** `SoundEvents.SHEEP_AMBIENT` for:
  - General communication
  - Lamb care (higher pitch)
  - Separation distress (higher pitch and volume)

## Integration with Existing Systems

### Component System

All sheep behaviors integrate with the ecology component system:

```java
EcologyComponent component = sheep.getComponent();
CompoundTag woolTag = component.getHandleTag("wool");
int woolStage = woolTag.getInt("wool_stage");
float woolQuality = woolTag.getFloat("wool_quality");
```

### Hunger System

Wool growth depends on hunger:
- Requires hunger > 30% to grow
- Growth chance scales with hunger level
- Grazing restores 25 hunger

### Condition System

Wool quality considers condition:
- 30% from nutrition
- 30% from condition
- 20% from health percentage

### Social System

Separation distress affects social needs:
- Isolated sheep become lonely faster
- Social recovery when grouped

## Scientific Basis

### Wool Growth
Based on real sheep wool growth cycles:
- Growth rate depends on nutrition (protein intake)
- Quality affected by overall health
- Shearing stimulates regrowth

### Flocking Behavior
Based on ungulate flocking research:
- Sheep are highly gregarious (social animals)
- Strong separation anxiety when isolated
- Follow leadership hierarchies
- Group protection against predators

### Lamb Care
Based on maternal behavior in sheep:
- Strong mother-young bonding
- Ewes recognize their lambs by sound and smell
- Protective behaviors when lambs threatened
- Communication via vocalizations

## Performance Considerations

### Optimizations

1. **Tick Intervals:** Wool handle updates every 5 seconds (100 ticks)
2. **Spatial Queries:** Flock behaviors use cached neighbor lists
3. **Cooldowns:** Grazing and bleating have cooldown timers
4. **Lazy Evaluation:** Behaviors only calculate when needed

### Thread Safety

- All state stored in NBT tags per-entity
- No shared static state between entities
- Component system provides thread-safe access

## Configuration Hot-Reloading

All behaviors support hot-reloading via `/reload`:

1. Modify JSON config files
2. Run `/reload` in-game
3. New behaviors apply immediately
4. Existing entities update gracefully

## Debugging

### NBT Data Inspection

Inspect sheep wool data:
```
/data get entity @e[type=sheep,limit=1] better-ecology.wool
```

Expected output:
```
{
  wool_stage: 3,
  last_shear_time: 12345,
  wool_quality: 0.75,
  last_graze_time: 12350,
  grazing_count: 15
}
```

### Behavior Weights

Check applied behavior weights:
```
/data get entity @e[type=sheep,limit=1] better-ecology.behavior
```

## Future Enhancements

Potential additions:
- Wool dyeing from grazing on flowers
- Seasonal wool growth variations
- Ram dominance behaviors
- Flock leadership inheritance
- Lamb play behaviors
- Nighttime grouping behaviors

## References

- Sheep ethology research
- Flocking behavior in ungulates
- Maternal behavior in sheep
- Wool growth biology
- Animal communication research
