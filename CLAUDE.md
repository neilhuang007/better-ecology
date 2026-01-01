# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Better Ecology is a Minecraft Fabric mod (1.21.1, Java 21) that implements scientifically-based animal behaviors. The mod uses a component-based architecture with data-driven behavior configurations loaded from JSON files, allowing hot-reloading without restarting the game.

## Development Commands

```bash
# Build
./gradlew build                    # Full build
./gradlew jar                      # Build JAR only
./gradlew remapJar                 # Remap for Fabric

# Development
./gradlew runClient                # Run in development client
./gradlew runServer                # Run in development server

# Data Generation
./gradlew runDatagen               # Generate data files
```

## Architecture

### Core Systems

**Behavior System** (`src/main/java/me/javavirtualenv/behavior/`)
- AI steering behaviors, flocking (boids), herding, predation, fleeing
- Scientifically-based implementations from research docs in `docs/behaviours/`
- Each behavior package is self-contained: ai/, core/, crepuscular/, fleeing/, flocking/, foraging/, herd/, parent/, predation/, steering/, territorial/

**Ecology System** (`src/main/java/me/javavirtualenv/ecology/`)
- `api/` - Public API for external integrations
- `handles/` - System handlers (MovementHandle, BehaviorHandle, etc.)
- `spatial/` - Spatial partitioning for neighbor queries
- `state/` - Entity state management

**Component Pattern**
- `EcologyComponent` - Modular entity attributes using a map-based component system
- `EcologyHandle` - Registry pattern for modular subsystem registration
- `EcologyBootstrap` - Initializes all handlers on mod startup

### Data-Driven Configuration

Behavior configurations live in `src/main/resources/data/better-ecology/`:
- `archetypes/` - Reusable behavior templates
- `mobs/passive/` - Per-entity configurations (cows, sheep, pigs, etc.)
- `templates/` - Behavior-specific templates

These JSON files define weights, thresholds, and parameters for each behavior. The resource pack integration allows reloading behaviors via `/reload`.

### Mixin Integration

Vanilla entity modifications use Fabric mixins:
- Main mixins: `src/main/java/me/javavirtualenv/ecology/mixin/`
- Client mixins: `src/main/java/me/javavirtualenv/mixin/client/`
- Config files: `better-ecology.mixins.json`, `better-ecology.client.mixins.json`

### Entry Points

- `BetterEcology.java` - Main mod entry point (server/common)
- `BetterEcologyClient.java` - Client-side entry point
- `BetterEcologyDataGenerator.java` - Data generation for resource packs

## Key Conventions

- **Package structure**: `behavior/` contains algorithms, `ecology/` contains mod integration
- **Goal-based AI**: Behaviors extend `Goal` class from Minecraft's goal system
- **Scientific basis**: Behaviors reference research in `docs/behaviours/README.md`
- **Naming**: Classes use PascalCase, methods camelCase, constants UPPER_SNAKE_CASE

FOR TESTING: you HAVE to use run client to try and test, NOT just building

## Configuration Parameters

Common behavior parameters (from research docs):
- `quorumThreshold`: 0.3-0.7 - % of group needed to initiate movement
- `topologicalNeighbors`: 6-7 - Neighbors tracked for flocking
- `flightInitiationDistance`: 8-32 blocks - Distance to trigger flight
- `givingUpDensity`: 0.1-0.5 - Patch depletion threshold
- `territoryRadius`: 16-64 blocks - Breeding territory size
