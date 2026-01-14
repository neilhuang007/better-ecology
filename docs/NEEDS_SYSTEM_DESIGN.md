# Animal Needs System Design

## Overview

A lightweight, robust framework implementing thirst, hunger, and fleeing behaviors for Minecraft animals. This system follows Minecraft's design philosophy and the architecture outlined in AI_REWRITE_PLAN.md.

## Design Principles (from Minecraft Game Design Guide)

1. **Simplicity** - Intuitive without explanation
2. **One Block at a Time** - Player interaction model
3. **Bad Things Happen, But Technically Player's Fault** - Animals can die from neglect, but players could have prevented it
4. **Real Animals Are Friendly** - Wolves don't hunt players, only other animals
5. **Mobs Need Personality** - Each behavior should be observable and create character

## Core Architecture

### 1. AnimalNeeds - Centralized State Access

Single entry point for reading/writing hunger and thirst values:

```java
public final class AnimalNeeds {
    // NBT keys
    public static final String HUNGER_KEY = "BetterEcology.Hunger";
    public static final String THIRST_KEY = "BetterEcology.Thirst";

    public static float getHunger(Mob mob);
    public static void setHunger(Mob mob, float value);
    public static void modifyHunger(Mob mob, float delta);

    public static float getThirst(Mob mob);
    public static void setThirst(Mob mob, float value);
    public static void modifyThirst(Mob mob, float delta);

    public static boolean isHungry(Mob mob);    // < HUNGRY threshold
    public static boolean isStarving(Mob mob);  // < STARVING threshold
    public static boolean isThirsty(Mob mob);   // < THIRSTY threshold
    public static boolean isDehydrated(Mob mob);// < DEHYDRATED threshold
}
```

### 2. AnimalThresholds - Unified Constants

```java
public final class AnimalThresholds {
    // Hunger thresholds (0-100, lower = hungrier)
    public static final float HUNGRY = 50f;       // Starts seeking food
    public static final float STARVING = 20f;     // Takes damage, desperate
    public static final float SATISFIED = 80f;    // Stops eating

    // Thirst thresholds (0-100, lower = thirstier)
    public static final float THIRSTY = 45f;      // Starts seeking water
    public static final float DEHYDRATED = 15f;   // Takes damage
    public static final float HYDRATED = 75f;     // Stops drinking

    // Default decay rates (per tick)
    public static final float HUNGER_DECAY = 0.01f;
    public static final float THIRST_DECAY = 0.015f;
}
```

### 3. Goal Priority System

| Priority | Category | Goal Examples |
|----------|----------|---------------|
| 0 | Survival | FloatGoal (swim), BreathAirGoal |
| 1 | Emergency | FleeFromPredatorGoal, PanicGoal |
| 2 | Critical Needs | SeekWaterGoal (dehydrated), SeekFoodGoal (starving) |
| 3 | Normal Needs | SeekWaterGoal (thirsty), SeekFoodGoal (hungry) |
| 4 | Hunting | HuntPreyGoal (predators only) |
| 5 | Social | FollowHerdGoal, BreedingGoal |
| 6-7 | Idle | WanderGoal, LookAroundGoal |

### 4. Core Goals

#### SeekWaterGoal
- Triggers when thirst < THIRSTY threshold
- Priority increases to 2 when thirst < DEHYDRATED
- Pathfinds to nearest water source (uses BlockTags.WATER)
- Drinks when adjacent to water, restores thirst over time
- Works for all land animals

#### SeekFoodGoal (Herbivores)
- Triggers when hunger < HUNGRY threshold
- Pathfinds to food items on ground OR grass blocks (grazing)
- Grass grazing: plays animation, removes grass block layer
- Item eating: picks up and consumes valid food items

#### SeekFoodGoal (Predators/Omnivores)
- Triggers when hunger < HUNGRY threshold
- Pathfinds to meat items on ground
- Uses item tag `better-ecology:meat` for valid items

#### HuntPreyGoal (Predators)
- Triggers when hunger < HUNGRY and no food items nearby
- Targets valid prey types based on predator config
- Attacks until prey dies
- Post-kill: drops meat item that predator can eat

#### FleeFromPredatorGoal
- High priority (1) when predator detected
- Detects predators from config (e.g., wolves, foxes for prey)
- Uses Flight Initiation Distance from config
- Runs away at fleeing speed multiplier

### 5. Damage System

When animals reach critical thresholds:
- **Starving** (hunger < 20): Take damage every `damage_interval` ticks
- **Dehydrated** (thirst < 15): Take damage every `damage_interval` ticks

Damage is gradual, giving players time to intervene (Minecraft design principle).

## File Structure

```
src/main/java/me/javavirtualenv/
├── behavior/
│   └── core/
│       ├── AnimalNeeds.java       # State access utility
│       ├── AnimalThresholds.java  # Constants
│       ├── SeekWaterGoal.java     # Water seeking
│       ├── SeekFoodGoal.java      # Food seeking (herbivore/predator variants)
│       ├── HuntPreyGoal.java      # Predator hunting
│       └── FleeFromPredatorGoal.java # Prey fleeing
├── ecology/
│   └── handles/
│       ├── HungerHandle.java      # Hunger decay tick
│       └── ThirstHandle.java      # Thirst decay tick
├── mixin/
│   ├── MobEcologyMixin.java       # Inject NBT save/load
│   └── animal/
│       ├── WolfMixin.java         # Register predator goals
│       ├── SheepMixin.java        # Register prey goals
│       └── ... (other animals)
└── debug/
    └── GoalTestCommand.java       # Testing commands
```

## Data-Driven Configuration

All thresholds are configurable via JSON at `data/better-ecology/mobs/passive/<animal>/behaviors.json`:

```json
{
  "hunger": {
    "enabled": true,
    "max_value": 100,
    "starting_value": 80,
    "decay_rate": 0.01,
    "damage_threshold": 10,
    "damage_amount": 1.0,
    "damage_interval": 200
  },
  "thirst": {
    "enabled": true,
    "max_value": 100,
    "starting_value": 100,
    "decay_rate": 0.015,
    "satisfied": 75,
    "thirsty": 45,
    "dehydrated": 15
  },
  "predation": {
    "as_prey": {
      "detection_range": 24,
      "flee_speed_multiplier": 1.3,
      "flee_distance": 32
    }
  }
}
```

## Testing Strategy

### Game Tests (src/testmod/java/me/javavirtualenv/gametest/)

Each behavior needs dedicated game tests:

1. **ThirstBehaviorTests**
   - `testAnimalSeeksWaterWhenThirsty` - Animal moves toward water when thirst drops
   - `testAnimalDrinksAndRestoresThirst` - Thirst increases after drinking
   - `testAnimalTakesDamageWhenDehydrated` - Health decreases when critically dehydrated

2. **HungerBehaviorTests**
   - `testHerbivoreSeeksGrassWhenHungry` - Sheep/cow moves to grass
   - `testPredatorSeeksMeatWhenHungry` - Wolf moves to dropped meat item
   - `testAnimalTakesDamageWhenStarving` - Health decreases when starving

3. **FleeingBehaviorTests**
   - `testPreyFleesFromPredator` - Sheep runs when wolf approaches
   - `testFleeingStopsWhenSafe` - Sheep stops running after reaching safe distance
   - `testFleeingTrumpsOtherGoals` - Fleeing overrides hunger/thirst seeking

4. **PredatorBehaviorTests**
   - `testWolfPicksUpMeatItem` - Wolf pathfinds to and picks up dropped meat
   - `testWolfHuntsWhenHungryNoMeat` - Wolf targets prey when hungry and no meat nearby
   - `testWolfEatsMeatRestoresHunger` - Hunger increases after eating

### Unit Tests (src/test/java/)
- AnimalNeedsTest - Test NBT read/write
- AnimalThresholdsTest - Test threshold comparisons

## Implementation Order

1. Create `AnimalThresholds.java` and `AnimalNeeds.java` (utility classes)
2. Create `MobEcologyMixin.java` (NBT save/load for hunger/thirst)
3. Create `SeekWaterGoal.java` with game tests
4. Create `SeekFoodGoal.java` (herbivore variant) with game tests
5. Create `FleeFromPredatorGoal.java` with game tests
6. Create `SeekFoodGoal.java` (predator variant) with game tests
7. Create `HuntPreyGoal.java` with game tests
8. Wire up goals in animal mixins
9. Integration testing with `/goaltest` commands

## Success Criteria

1. Animals seek water when thirsty and drink to restore thirst
2. Herbivores graze grass or eat food items when hungry
3. Predators pick up meat items and eat them when hungry
4. Predators hunt prey when hungry and no food available
5. Prey animals flee from predators with appropriate priority
6. Animals take gradual damage when critically starving/dehydrated
7. All behaviors are observable (players can see what animals are doing)
8. All game tests pass consistently
