# Pig Behaviors - Better Ecology Mod

## Overview
The Better Ecology mod implements scientifically-based pig behaviors including rooting, truffle hunting, mud bathing, and crop feeding.

## Implemented Behaviors

### 1. Rooting Behavior (`PigRootingGoal`)
- **What it does**: Pigs root in grass blocks, mycelium, and podzol to find truffles
- **Particle effects**: Dirt particles when rooting
- **Sound effects**: Pig ambient sounds during rooting
- **Mechanics**:
  - Converts grass blocks to dirt (30% chance after full rooting)
  - Finds truffles based on soil type:
    - Grass blocks: 5% chance
    - Mycelium: 15% chance (highest truffle yield)
    - Podzol: 12% chance
    - Regular dirt: No truffles
  - Rooting duration: 40 ticks (2 seconds)
  - Search range: 8 blocks

### 2. Truffle Hunting
- **Truffle Item**: New food item added by the mod
  - Nutrition: 6 hunger points
  - Saturation: 0.6
  - Rarity: Rare (blue item name)
  - Tooltip: "A rare delicacy found by pigs. Can be eaten or used in cooking."
- **Pig Excitement**: Pigs show heart particles when truffles are nearby (within 8 blocks)
- **Soil Type Bonuses**: Different soil types affect truffle discovery rates

### 3. Mud Bathing (`PigMudBathingGoal`)
- **What it does**: Pigs seek out mud, water, or clay to bathe
- **Benefits**:
  - Restores condition (+2 per second while bathing)
  - Provides temperature regulation in hot weather
  - Applies visual mud effect (5 minutes / 6000 ticks)
- **Trigger conditions**:
  - Random chance (5% for adults, 10% for babies)
  - Increased likelihood when health is below 80%
  - Increased likelihood during hot daytime
- **Mechanics**:
  - Bathing duration: 200 ticks (10 seconds)
  - Search range: 16 blocks
  - Visual effects: Water dripping particles
  - Sound effects: Pig ambient sounds at lower pitch

### 4. Crop Feeding (`PigFeedingGoal`)
- **What it does**: Pigs seek out and eat fully grown crops
- **Crops eaten**:
  - Carrots (fully grown)
  - Potatoes (fully grown)
  - Beetroots (fully grown)
- **Benefits**:
  - Restores hunger based on crop type:
    - Carrots: +20 hunger
    - Potatoes: +25 hunger
    - Beetroots: +15 hunger
  - Heals 1 HP (0.5 hearts)
- **Trigger conditions**:
  - Random chance (15% normally, 20% for babies)
  - Increased to 40% when hunger is below 50
- **Mechanics**:
  - Eating duration: 30 ticks (1.5 seconds)
  - Destroys the crop block
  - Search range: 12 blocks

### 5. Truffle Seeking (`PigTruffleSeekGoal`)
- **What it does**: Pigs actively pursue dropped truffles they detect
- **Detection range**: 16 blocks
- **Movement speed**: 1.2x normal speed when seeking
- **Cooldown**: 30 seconds after seeking
- **Effects**:
  - Sniffing particles while tracking
  - Heart particles when close to truffle
  - Excitement sounds when found
- **Priority**: High (overrides most other behaviors)

### 6. Truffle Sniffing (`PigSniffTrufflesGoal`)
- **What it does**: Pigs pause and sniff when detecting interesting scents
- **Detection targets**:
  - Dropped truffle items (highest priority)
  - Nearby food crops
  - Mycelium or podzol blocks (high truffle potential)
- **Detection range**: 12 blocks
- **Sniff duration**: 3 seconds
- **Cooldown**: 10 seconds
- **Effects**:
  - Pig looks at scent source
  - Sneeze particle effects
  - Sniffing sounds

### 7. Social Mud Bathing (`PigSocialMudBathingGoal`)
- **What it does**: Pigs in mud attract other pigs to join them
- **Social mechanics**:
  - Pigs already bathing call nearby pigs
  - Maximum 4 pigs per group bath
  - Happy villager particles when group forms
- **Attraction range**: 12 blocks
- **Chance to attract**: 30% per nearby pig
- **Social duration**: 20 seconds
- **Effects**:
  - Pig sounds to attract others
  - Social bonding through group activity
  - Enhanced particle effects for groups

## Pig Attributes
- **Health**: 10 HP (5 hearts)
- **Size**: 0.9 x 0.9 blocks
- **Movement Speed**: 0.25 walk, 0.35 run
- **Diet**: Carrots, potatoes, beetroots, beetroot soup
- **Maturity**: 24000 ticks (20 minutes)
- **Hunger System**:
  - Max: 100
  - Starting: 75
  - Decay rate: 0.015 per tick
  - Starvation damage: 1 HP every 10 seconds below 5 hunger
- **Condition System**:
  - Max: 100
  - Starting: 70
  - Affects breeding success and truffle finding
  - Gains when satiated, loses when hungry/starving
- **Social System**:
  - Herd animal (needs other pigs within 16 blocks)
  - Decay rate: 0.008 per tick when alone
  - Recovery rate: 0.1 per tick near herd
  - Loneliness threshold: 40

## Configuration
All pig behaviors are implemented via code-based handles in `PigMixin.java`. The behaviors are not configurable via JSON files - they use hardcoded scientific constants based on real pig behavior research.

## Files Created/Modified

### New Files
- `src/main/java/me/javavirtualenv/pig/PigRootingGoal.java` - Rooting behavior goal
- `src/main/java/me/javavirtualenv/pig/PigMudBathingGoal.java` - Mud bathing behavior goal
- `src/main/java/me/javavirtualenv/pig/PigFeedingGoal.java` - Crop feeding behavior goal
- `src/main/java/me/javavirtualenv/pig/PigBehaviorHandle.java` - Pig-specific behavior state management
- `src/main/java/me/javavirtualenv/item/ModItems.java` - Truffle item registration
- `src/main/resources/assets/better-ecology/lang/en_us.json` - Truffle item localization
- `src/main/resources/assets/better-ecology/models/item/truffle.json` - Truffle item model
- `src/client/java/me/javavirtualenv/mixin/client/PigRendererMixin.java` - Client-side rendering for mud effect

### Modified Files
- `src/main/java/me/javavirtualenv/BetterEcology.java` - Added ModItems.register() call
- `src/main/java/me/javavirtualenv/mixin/animal/PigMixin.java` - Complete rewrite with pig-specific handles
- `src/main/resources/better-ecology.client.mixins.json` - Added PigRendererMixin
- `src/client/resources/better-ecology.client.mixins.json` - Added PigRendererMixin

## Testing Recommendations
1. Spawn pigs in various biomes to test rooting behavior
2. Create a carrot/potato/beetroot farm to test crop feeding
3. Place water/mud blocks near pigs to test mud bathing
4. Check for truffle drops when pigs root in mycelium
5. Verify mud effect timer and condition restoration
6. Test breeding with the new hunger/condition requirements

## Future Enhancements
- Add visual mud texture overlay on pig model
- Implement truffle recipes (cooking with truffles)
- Add pig breeding with genetic traits
- Implement more sophisticated herd movement patterns
- Add sounds specific to rooting and mud bathing
