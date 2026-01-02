# Changelog

All notable changes to the Better Ecology mod are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

#### Wolf Food Sharing and Pack Dynamics (2026-01-01)
- Implemented comprehensive wolf food sharing system with altruistic behavior
- Added AnimalItemStorage component for item carrying shared across species
- Wolves pick up meat items when hungry or when pack members are hungry
- Food sharing priority: mate > alpha > other hungry pack members
- Pack identification via UUID-based pack IDs stored in NBT
- Social hierarchy system (alpha/beta/omega ranks)
- PredatorFeedingGoal for opportunistic scavenging behavior
- Client-side visual rendering for wolves carrying items
- Refactored fox item storage to use shared AnimalItemStorage component

### Key Features of Wolf Food Sharing
- Wolves search 16 blocks for meat items tagged with `minecraft:meat`
- 32 block search radius for locating hungry pack members to share food
- Altruistic behavior: wolves pick up food even when not hungry if pack is hungry
- Respects pack boundaries: tamed wolves excluded from wild pack behaviors
- NBT persistence for pack state, hierarchy, and carried items
- Integration with existing hunger and predation systems

### Technical Implementation
- New goal classes: WolfPickupItemGoal, WolfShareFoodGoal, PredatorFeedingGoal
- Shared component: AnimalItemStorage for mob item storage
- WolfBehaviorHandle manages pack state with static query API
- Client rendering: WolfHeldItemLayer for visual feedback
- Goal priority system to prevent behavior conflicts

## [0.1.0] - Previous Release

### Initial Features
- EcologyComponent system for modular entity attributes
- Behavior system with data-driven JSON configurations
- Herding behaviors (cows, sheep, pigs) with quorum-based movement
- Flocking behaviors for birds with topological neighbor tracking
- Hunger, thirst, and condition management systems
- Debug overlay system with keybinding
- Spatial partitioning for efficient neighbor queries
- Steering behavior framework for complex movement patterns
