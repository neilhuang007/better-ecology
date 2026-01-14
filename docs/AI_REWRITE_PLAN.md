# Animal AI Rewrite Plan

## Problems with Current Implementation

1. **Inconsistent hunger thresholds**: 40, 50, 75 used in different goals
2. **Broken state machines**: WolfPackAttackGoal.getHuntingState() always returns IDLE
3. **Overlapping goals**: Multiple goals compete for same behavior
4. **Complex NBT access**: Multiple ways to read/write hunger/thirst
5. **Missing logging**: Hard to debug what's happening

## New Architecture

### Core Principles

1. **Single hunger/thirst access**: Use `AnimalNeeds.getHunger(mob)` everywhere
2. **Unified thresholds**: Define once in `AnimalThresholds` class
3. **Simple goal hierarchy**: Clear priorities, no overlap
4. **Behavior composition**: Animals compose from shared goal classes

### Unified Thresholds

```java
public class AnimalThresholds {
    // Hunger thresholds (0-100, lower = hungrier)
    public static final int HUNGRY = 50;      // Seeks food
    public static final int STARVING = 20;    // Takes damage, desperate
    public static final int SATISFIED = 80;   // Stops eating

    // Thirst thresholds
    public static final int THIRSTY = 30;     // Seeks water
    public static final int DEHYDRATED = 10;  // Takes damage
    public static final int HYDRATED = 80;    // Stops drinking
}
```

### Shared Goal Classes

#### 1. SeekFoodGoal (for herbivores and predators eating items)
- Finds nearest food item on ground
- Pathfinds to it
- Eats it, restores hunger
- Works for: wolves eating meat, sheep eating wheat, etc.

#### 2. SeekWaterGoal (already exists, keep it)
- Finds nearest water
- Pathfinds to drinking position
- Drinks, restores thirst

#### 3. HuntPreyGoal (for predators)
- Targets valid prey when hungry
- Attacks until prey dies
- Handles post-kill hunger restoration

#### 4. FleeFromPredatorGoal (for prey)
- Detects nearby predators
- Flees at high priority

### Per-Animal Configuration

```java
// Example: Wolf configuration
WolfGoals.register(wolf, goals -> {
    goals.add(1, new FleeWhenLowHealthGoal(wolf));
    goals.add(2, new SeekWaterGoal(wolf, 1.3, 20));
    goals.add(3, new SeekFoodGoal(wolf, MEAT_ITEMS, 1.2));
    goals.add(4, new HuntPreyGoal(wolf, PREY_TYPES, 1.2));
    goals.add(5, new WanderGoal(wolf));
});

// Example: Sheep configuration
SheepGoals.register(sheep, goals -> {
    goals.add(1, new FleeFromPredatorGoal(sheep, Wolf.class, Fox.class));
    goals.add(2, new SeekWaterGoal(sheep, 1.0, 16));
    goals.add(3, new GrazeGoal(sheep));
    goals.add(4, new WanderGoal(sheep));
});
```

### Goal Priority Guidelines

| Priority | Category | Examples |
|----------|----------|----------|
| 0 | Survival | FloatGoal (swim) |
| 1 | Emergency | FleeWhenLowHealth, Panic |
| 2 | Critical needs | SeekWater when dehydrated |
| 3 | Normal needs | SeekFood, SeekWater when hungry/thirsty |
| 4 | Hunting | HuntPreyGoal |
| 5 | Social | FollowPack, Breeding |
| 6-7 | Idle | Wander, LookAround |

### Implementation Order

1. Create `AnimalThresholds` and `AnimalNeeds` utility classes
2. Rewrite `SeekFoodGoal` as simple, reliable base class
3. Rewrite `SeekWaterGoal` to use unified thresholds
4. Create `HuntPreyGoal` for predators
5. Update wolf mixin to use new goals
6. Test wolves thoroughly
7. Apply same pattern to other animals

## Files to Create/Modify

### New Files
- `src/main/java/me/javavirtualenv/behavior/core/AnimalThresholds.java`
- `src/main/java/me/javavirtualenv/behavior/core/AnimalNeeds.java`
- `src/main/java/me/javavirtualenv/behavior/core/SeekFoodGoal.java`
- `src/main/java/me/javavirtualenv/behavior/core/HuntPreyGoal.java`

### Files to Simplify/Remove
- Remove: WolfPickupItemGoal (replaced by SeekFoodGoal)
- Remove: WolfShareFoodGoal (complex, rarely works)
- Simplify: PredatorFeedingGoal (merge into SeekFoodGoal)
- Fix: WolfPackAttackGoal (or remove if not needed)

## Testing Strategy

1. Use `/goaltest` commands to create test environments
2. Check log files for goal activation
3. Verify animals complete behaviors (drink water, eat food, hunt prey)
4. Test edge cases (no food nearby, blocked paths, etc.)
