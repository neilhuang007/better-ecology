# Better Ecology

Better Ecology is a Minecraft Fabric mod (1.21.1, Java 21) that implements scientifically-based animal behaviors using a component-based architecture with data-driven behavior configurations.

## Features

### Advanced Animal AI

The mod adds comprehensive, scientifically-grounded behaviors to vanilla animals:

- **Herding Animals** (Cows, Sheep, Pigs): Collective decision-making, quorum-based movement, bimodal grazing patterns, selfish herd positioning
- **Birds** (Parrots, Chickens): Flocking behaviors with separation/alignment/cohesion, topological neighbor tracking, V-formation efficiency
- **Predators** (Wolves, Foxes): Pack hunting coordination, territorial defense, social hierarchy, food sharing behaviors
- **Small Prey** (Rabbits): Zigzag evasion, burrowing systems, food caching, thumping warnings
- **Bats**: Light-triggered emergence, roost clustering, group foraging patterns

### Key Systems

- **EcologyComponent**: Modular entity attributes using a map-based component system
- **Behavior Registry**: Data-driven configurations loaded from JSON with hot-reload support
- **Spatial Partitioning**: Efficient neighbor queries for flocking and herding
- **AnimalItemStorage**: Shared component for animals that carry items (wolves, foxes)
- **Pack Dynamics**: Social hierarchy, food sharing, territorial behaviors for wolves

## Development

See [CLAUDE.md](CLAUDE.md) for development commands, architecture details, and coding conventions.

## Scientific Basis

Behaviors are based on academic research including:
- Reynolds (1987) - Boids algorithm for flocking
- Charnov (1976) - Marginal Value Theorem for foraging
- Ydenberg & Dill (1986) - Economic escape theory
- Ballerini et al. (2008) - Topological interactions in bird flocks
- Couzin et al. (2002) - Self-organized collective behavior

See [docs/behaviours/README.md](docs/behaviours/README.md) for detailed research documentation.
