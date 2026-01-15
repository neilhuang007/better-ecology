# Better Ecology

A Minecraft Fabric mod that implements scientifically-based animal behaviors using a component-based architecture with data-driven configurations.

## Features

### Scientifically-Grounded Animal Behaviors

Better Ecology enhances vanilla Minecraft animals with behaviors based on peer-reviewed ethological research. Each animal exhibits realistic behaviors appropriate to its species.

#### Herd Animals (Cows, Sheep, Pigs)

- **Collective decision-making** with quorum-based movement initiation
- **Selfish herd positioning** where vulnerable animals seek center positions
- **Bimodal grazing patterns** with morning and afternoon activity peaks
- **Vigilance sharing** where animals take turns being alert
- **Separation distress** with vocalizations when isolated from the herd

#### Sheep-Specific Behaviors

- **Wool growth system** tied to nutrition and condition
- **Wool quality** affects shearing yield
- **Lamb care** with maternal protection behaviors
- **Flock cohesion** with strong social bonds

#### Pig-Specific Behaviors

- **Rooting behavior** to find truffles in soil
- **Truffle hunting** with soil-type-dependent discovery rates
- **Mud bathing** for thermoregulation
- **Social mud bathing** where pigs attract others to join
- **Crop feeding** with intelligent foraging

#### Birds (Parrots, Chickens)

- **Flocking behaviors** using the boids algorithm
- **Topological neighbor tracking** (6-7 nearest neighbors)
- **Separation, alignment, and cohesion** forces
- **V-formation efficiency** during group flight

#### Parrot-Specific Behaviors

- **Mimicking system** that warns players of nearby hostile mobs
- **Dancing** with style variations based on music disc
- **Music detection** and attraction to jukeboxes
- **Perching preferences** for high, safe locations

#### Predators (Wolves, Foxes)

- **Pack hunting coordination** with social hierarchy
- **Food sharing** with mate and pack priority
- **Territorial defense** behaviors
- **Item carrying** with visual feedback

#### Small Prey (Rabbits)

- **Zigzag evasion** using protean movement
- **Freezing response** to ambush predators
- **Alarm signals** including thumping warnings
- **Burrowing** and refuge-seeking behaviors

#### Bats

- **Light-triggered emergence** at dusk
- **Dawn return behavior** to roosting sites
- **Roost clustering** on ceilings
- **Group emergence patterns**

### Technical Features

- **Data-driven configuration** via JSON/YAML files
- **Hot-reloading** with `/reload` command
- **Component-based architecture** for modular entity attributes
- **Spatial partitioning** for efficient neighbor queries
- **Minimal performance impact** through caching and interval ticks

## Requirements

| Requirement | Version |
|-------------|---------|
| Minecraft | 1.21.1 |
| Java | 21 |
| Fabric Loader | 0.15+ |
| Fabric API | Current |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download Better Ecology from the releases page
4. Place both JAR files in your `mods` folder
5. Launch Minecraft

## Configuration

All behaviors are configurable through data files located in:
```
data/better-ecology/mobs/<category>/<animal>.json
```

Changes take effect after running `/reload` in-game.

See the [Configuration Documentation](docs/wiki/systems/Configuration.md) for detailed parameters.

## Documentation

Comprehensive documentation is available in the [docs/wiki](docs/wiki/Home.md) directory:

- **[Animal Behaviors](docs/wiki/Home.md#animal-behaviors)** - Detailed behavior documentation for each animal
- **[Core Systems](docs/wiki/Home.md#core-systems)** - Technical architecture documentation
- **[Research Basis](docs/wiki/Home.md#research-documentation)** - Scientific papers and research summaries

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/better-ecology.git
cd better-ecology

# Build the mod
./gradlew build

# Run development client
./gradlew runClient

# Run development server
./gradlew runServer
```

### Project Structure

```
src/
  main/
    java/me/javavirtualenv/
      behavior/          # Behavior implementations
        flocking/        # Boids algorithm, flocking
        herd/            # Herding behaviors
        fleeing/         # Escape behaviors
        foraging/        # Grazing, food finding
        parent/          # Parent-offspring bonds
      ecology/           # Core mod systems
        api/             # Public API
        handles/         # System handlers
        spatial/         # Spatial partitioning
        state/           # Entity state
        mixin/           # Vanilla modifications
    resources/
      data/better-ecology/
        archetypes/      # Reusable templates
        mobs/            # Per-entity configs
        templates/       # Base configurations
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `EcologyComponent` | Per-entity modular attributes |
| `EcologyHandle` | System interface for subsystems |
| `EcologyProfile` | Merged configuration data |
| `EcologyHooks` | Mixin dispatch for lifecycle events |

## Scientific Basis

Behaviors are implemented based on academic research:

| Research | Application |
|----------|-------------|
| Reynolds (1987) - Boids algorithm | Flocking behaviors |
| Charnov (1976) - Marginal Value Theorem | Foraging patch selection |
| Ydenberg and Dill (1986) - Economic escape theory | Flight initiation distance |
| Ballerini et al. (2008) - Topological interactions | Bird flock structure |
| Couzin et al. (2002) - Self-organized behavior | Collective movement |

See the [research documentation](docs/wiki/research/) for detailed citations and implementation notes.

## Contributing

Contributions are welcome. Please ensure:

1. Code follows existing patterns and conventions
2. New behaviors include scientific citations where applicable
3. Configuration is data-driven and hot-reloadable
4. Tests cover new functionality

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Acknowledgments

- The Fabric modding community for their tools and documentation
- Researchers whose work informs these behavior implementations
- Contributors and testers who have helped improve the mod
