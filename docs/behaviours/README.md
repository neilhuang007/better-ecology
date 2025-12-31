# Animal Behavior Research Documentation

This directory contains research summaries on various animal behaviors for the Better Ecology Minecraft mod. Each document includes scientific findings, mathematical models, and implementation notes.

## Documents

| Document | Description | Key Behaviors |
|----------|-------------|---------------|
| **[01-herd-movement-leadership](01-herd-movement-leadership.md)** | Collective decision-making, leadership dynamics, following behavior | Quorum-based movement, selfish herd positioning, leadership emergence |
| **[02-bird-flocking-group-flight](02-bird-flocking-group-flight.md)** | Flocking mechanics, murmurations, V-formations | Topological neighbor tracking, separation/alignment/cohesion |
| **[03-parent-offspring-attachment](03-parent-offspring-attachment.md)** | Mother-offspring bonding, carrying, imprinting, weaning | Following behavior, hiding strategies, protection, separation anxiety |
| **[04-bat-crepuscular-emergence](04-bat-crepuscular-emergence.md)** | Bat emergence triggers, roost behavior, echolocation | Light-triggered emergence, roost clustering, group foraging |
| **[05-grazing-foraging-behaviors](05-grazing-foraging-behaviors.md)** | Grazing patterns, optimal foraging theory, patch selection | Marginal Value Theorem, bimodal grazing, giving-up density |
| **[06-sleeping-resting-behaviors](06-sleeping-resting-behaviors.md)** | Sleep cycles, sleep site selection, vigilance | Activity cycles (diurnal/nocturnal/crepuscular), social sleeping |
| **[07-fleeing-panic-behaviors](07-fleeing-panic-behaviors.md)** | Flight initiation, escape strategies, alarm signals | FID, zigzagging, stampeding, group panic |
| **[08-breeding-courtship-behaviors](08-breeding-courtship-behaviors.md)** | Mate selection, courtship rituals, mating systems | Displays, territoriality, seasonal breeding, pair bonding |

## Quick Reference: Minecraft Entity Applications

### Herd Animals (Cows, Sheep, Pigs, Llamas)

From: [01-herd-movement-leadership](01-herd-movement-leadership.md), [05-grazing-foraging-behaviors](05-grazing-foraging-behaviors.md)

- Bimodal grazing patterns (morning/afternoon)
- Quorum-based movement initiation (wait for group)
- Selfish herd positioning (weak animals seek center)
- Following behavior for babies
- Patch selection using Marginal Value Theorem

### Birds (Parrots, Chickens, Bees, Custom Birds)

From: [02-bird-flocking-group-flight](02-bird-flocking-group-flight.md), [06-sleeping-resting-behaviors](06-sleeping-resting-behaviors.md)

- Topological neighbor tracking (6-7 neighbors)
- Separation, alignment, cohesion forces
- V-formation energy efficiency
- Roosting in groups at night
- Sentinel behavior

### Predators (Wolves, Foxes, Ocelots, Cats)

From: [01-herd-movement-leadership](01-herd-movement-leadership.md), [07-fleeing-panic-behaviors](07-fleeing-panic-behaviors.md), [08-breeding-courtship-behaviors](08-breeding-courtship-behaviors.md)

- Pack hunting coordination
- Chase behaviors (cursorial predator response)
- Territorial defense during breeding
- Pair bonding and mate fidelity
- Parental protection of offspring

### Bats

From: [04-bat-crepuscular-emergence](04-bat-crepuscular-emergence.md)

- Light-triggered emergence (dusk)
- Dawn return behavior
- Roost clustering on ceilings
- Group emergence patterns
- Predation avoidance timing

### Small Prey Animals (Rabbits, Chickens)

From: [03-parent-offspring-attachment](03-parent-offspring-attachment.md), [07-fleeing-panic-behaviors](07-fleeing-panic-behaviors.md)

- Zigzag escape patterns
- Freezing before flight
- Hider species behavior (babies hide)
- Alarm signals to group
- High vigilance during rest

## Mathematical Models Reference

### Boids Algorithm (Reynolds, 1987)

From: [02-bird-flocking-group-flight](02-bird-flocking-group-flight.md)

```
separation = steer away from nearby neighbors
alignment = steer toward average heading of neighbors
cohesion = steer toward average position of neighbors
```

### Marginal Value Theorem (Charnov, 1976)

From: [05-grazing-foraging-behaviors](05-grazing-foraging-behaviors.md)

```
Leave patch when: instantaneous intake rate = average habitat intake rate
```

### Flight Initiation Distance (Ydenberg & Dill, 1986)

From: [07-fleeing-panic-behaviors](07-fleeing-panic-behaviors.md)

```
Flee when: cost of staying = cost of fleeing
```

## Common Configuration Parameters

| Parameter | Default Range | Description |
|-----------|---------------|-------------|
| `quorumThreshold` | 0.3 - 0.7 | % of group needed to initiate movement |
| `topologicalNeighbors` | 6 - 7 | Number of neighbors to track (flocking) |
| `flightInitiationDistance` | 8 - 32 blocks | Distance to trigger flight |
| `givingUpDensity` | 0.1 - 0.5 | Patch depletion threshold |
| `territoryRadius` | 16 - 64 blocks | Breeding territory size |
| `separationDistance` | 1 - 5 blocks | Minimum distance between group members |
| `perceptionAngle` | 270° | Visual field for neighbor detection |

## Implementation Priority

1. **High Priority**: Herd movement, flocking (birds), parent-offspring following
2. **Medium Priority**: Grazing patterns, fleeing behaviors, bat emergence
3. **Lower Priority**: Complex courtship displays, advanced territoriality

## Research Sources

All documents include citations to academic papers and research sources. Key foundational works include:

- Reynolds (1987) - Boids algorithm
- Charnov (1976) - Marginal Value Theorem
- Ydenberg & Dill (1986) - Economic escape theory
- Ballerini et al. (2008) - Topological interactions in bird flocks
- Couzin et al. (2002) - Self-organized collective behavior
