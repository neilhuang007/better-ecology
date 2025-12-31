# Hunger and Thirst Trigger System

## Overview

The HungerThirstTrigger system implements scientifically-based animal behavior where hunger and thirst act as primary triggers for behavior prioritization. Research shows that when these physiological needs become urgent, animals shift focus from other activities to satisfy them.

## Architecture

### Components

1. **HungerThirstTriggerGoal** (`me.javavirtualenv.behavior.core.HungerThirstTriggerGoal`)
   - Monitors hunger and thirst levels
   - Updates entity state with priority information
   - Runs continuously to track physiological state changes

2. **HungerThirstTriggerHandle** (`me.javavirtualenv.ecology.handles.HungerThirstTriggerHandle`)
   - Registers the trigger goal with the entity's goal selector
   - Loads configuration from YAML profiles
   - Configures thresholds per mob type

3. **HungerThirstPriority** (`me.javavirtualenv.behavior.core.HungerThirstPriority`)
   - Utility class for checking hunger/thirst state
   - Provides static methods for other goals to query priority
   - Enables other behaviors to defer to survival needs

## Configuration

### YAML Configuration

Add to mob's YAML configuration (e.g., `mod_registry.yaml`):

```yaml
internal_state:
  hunger:
    enabled: true
    max_value: 100
    starting_value: 80
    decay_rate: 0.02
    thresholds:
      satiated: 80
      hungry: 50
      very_hungry: 25
      starving: 10

  thirst:
    enabled: true
    max_value: 100
    starting_value: 100
    decay_rate: 0.015
    thresholds:
      satisfied: 75
      thirsty: 45
      dehydrated: 15

  hunger_thirst_trigger:
    enabled: true              # Enable the trigger system
    priority: 4                # AI priority (see below)
    hunger_threshold: 50       # Below this, prioritize foraging
    thirst_threshold: 45       # Below this, prioritize drinking
```

### Code-Based Registration

For mobs using code-based handles (like Pig), add the handle to the AnimalConfig:

```java
AnimalConfig config = AnimalConfig.builder(EntityType.PIG)
    .addHandle(new PigHungerHandle())
    .addHandle(new PigThirstHandle())
    .addHandle(new HungerThirstTriggerHandle())  // Add this
    // ... other handles
    .build();
```

## AI Priority Framework

The trigger system integrates with the mod's AI priority framework:

```yaml
ai_priority_framework:
  survival:
    breathe: 0
    escape_danger: 1
    flee_predator: 2

  physiological:
    critical_health: 3
    hunger_thirst_trigger: 4      # <- This system
    drink_water: null
    eat_food: 5
    find_shelter: null
```

- **Priority 4**: Runs above critical_health but below eat_food
- Sets state flags that lower-priority goals can check
- Allows adaptive prioritization based on urgency

## Usage in Other Goals

### Example 1: Defer Non-Essential Activities

```java
public class SocialInteractionGoal extends Goal {
    @Override
    public boolean canUse() {
        // Don't socialize when survival needs are urgent
        if (HungerThirstPriority.shouldPrioritizeSurvival(mob)) {
            return false;
        }
        return true;
    }
}
```

### Example 2: Increase Foraging Urgency

```java
public class ForagingGoal extends Goal {
    @Override
    public boolean canUse() {
        // Always forage when starving
        if (HungerThirstPriority.isStarving(mob)) {
            return true;
        }

        // High hunger priority increases foraging chance
        double hungerPriority = HungerThirstPriority.getHungerPriority(mob);
        if (hungerPriority > 0.7) {
            return random.nextFloat() < 0.8;
        }

        return random.nextFloat() < 0.2;
    }
}
```

### Example 3: Thirst vs Hunger Priority

```java
public class SeekWaterGoal extends Goal {
    @Override
    public boolean canUse() {
        // Only seek water if thirsty
        if (!HungerThirstPriority.needsWater(mob)) {
            return false;
        }

        // If also hungry, check which is more critical
        if (HungerThirstPriority.needsFood(mob)) {
            return HungerThirstPriority.isThirstMoreCritical(mob);
        }

        return true;
    }
}
```

## API Reference

### HungerThirstPriority Methods

```java
// Check if needs food/water
boolean needsFood(Mob mob)
boolean needsWater(Mob mob)

// Check critical levels
boolean isStarving(Mob mob)
boolean isDehydrated(Mob mob)

// Get current levels (0-100)
int getHungerLevel(Mob mob)
int getThirstLevel(Mob mob)

// Get priority values (0.0 to 1.0, higher = more urgent)
double getHungerPriority(Mob mob)
double getThirstPriority(Mob mob)

// Compare urgency
boolean isThirstMoreCritical(Mob mob)
boolean isHungerMoreCritical(Mob mob)

// Check if any survival need is urgent
boolean shouldPrioritizeSurvival(Mob mob)
double getMaxPhysiologicalPriority(Mob mob)
```

## Scientific Basis

Research in animal behavior demonstrates:

1. **Hunger as a Trigger**: When hunger drops below thresholds, animals initiate foraging behavior and suppress non-essential activities

2. **Adaptive Prioritization**: Animals dynamically adjust behavior priorities based on:
   - Current hunger/thirst levels
   - Which need is more urgent
   - Environmental conditions (food/water availability)

3. **Quorum Sensing**: In group-living animals, hunger triggers can cascade through the group, leading to coordinated foraging

4. **Energy Budgeting**: Animals allocate energy budgets based on survival needs, prioritizing food acquisition over other activities when hungry

## Implementation Notes

### State Storage

The trigger goal stores state in NBT tags under the `hunger_thirst_state` handle:

```nbt
hunger_thirst_state: {
    needs_food: true|false,
    needs_water: true|false,
    starving: true|false,
    dehydrated: true|false,
    hunger_priority: 0.0-1.0,
    thirst_priority: 0.0-1.0,
    thirst_is_critical: true|false,
    hunger_is_critical: true|false
}
```

### Performance Considerations

- Trigger goal checks every 40 ticks (2 seconds) by default
- State queries are cached per-tick in EcologyComponent
- Minimal overhead: only reads NBT, no complex calculations

### Integration with Existing Systems

- Works with existing HungerHandle and ThirstHandle
- Entity state flags (`isHungry`, `isThirsty`) are also set
- Compatible with all existing behavior systems

## Examples in Codebase

See these files for usage examples:

1. **PigFeedingGoal.java** - Uses `needsFood()` and `getHungerPriority()`
2. **CowMixin.java** - Registers the trigger handle
3. `cow/mod_registry.yaml` - Configuration example

## Future Enhancements

Potential improvements:

1. **Quorum Integration**: Feed hunger state into herd quorum calculations
2. **Learning**: Remember food/water locations better when starving
3. **Risk Assessment**: Balance hunger urgency against predation risk
4. **Social Signaling**: Communicate hunger state to group members
5. **Seasonal Adjustment**: Adjust thresholds based on food availability

## Troubleshooting

### Trigger Not Working

1. Check that `hunger_thirst_trigger.enabled: true` in config
2. Ensure hunger or thirst systems are enabled
3. Verify the handle is registered in mixin
4. Check priority is not conflicting with other goals

### Goals Not Deferring

1. Ensure goals use `HungerThirstPriority` utility methods
2. Check that priority values are correct
3. Verify state is being updated (add debug logging)
4. Test with extreme hunger values (0-10) to confirm

## References

- Original research documents in `docs/behaviours/README.md`
- Animal behavior literature on motivational systems
- Foraging theory and optimal patch selection
