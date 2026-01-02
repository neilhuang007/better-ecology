# Better Ecology Framework Architecture

Purpose: data-driven ecology for vanilla mobs with minimal per-mob code. All behavior derives from YAML profiles and archetypes, compiled once on reload, and dispatched through unified hooks.

## Principles
- Single source of truth: YAML profiles + archetypes.
- Minimal mixins: one Mob mixin + one Animal mixin.
- Handle-based systems: each subsystem is a handle that enables itself via config.
- Performance: parse once, cache aggressively, avoid per-tick heavy work.

## Data Layout
- Base template: `data/better-ecology/templates/mod_registry.yaml`
- Mob profiles: `data/<ns>/mobs/**/<mob>.yaml` or `.yml`
- Archetypes: `data/<ns>/archetypes/<path>.yaml`

Profiles can include:
```yaml
archetypes:
  - "better-ecology:passive/grazer"
  - "better-ecology:herd/diurnal"
```

## Merge Rules
Order: base template -> archetypes (in listed order) -> mob profile.

Overlay semantics:
- null does not override
- empty list does not override
- non-empty list replaces
- maps merge recursively

If `identity.mob_id` is missing or invalid, the profile is skipped with a warning.

## Runtime Flow
1) Reload phase
   - `EcologyResourceReloader` reads YAMLs on datapack reload
   - builds `EcologyProfile` objects
   - `EcologyProfileRegistry.reload()` stores profiles, resolves handles, increments generation

2) Entity lifecycle
   - `MobEcologyMixin` attaches `EcologyComponent` to every Mob
   - goal registration -> `EcologyHooks.onRegisterGoals()` (idempotent)
   - tick -> `EcologyHooks.onTick()`
   - NBT -> `EcologyHooks.onSave()` / `EcologyHooks.onLoad()`

3) Food checks
   - `AnimalEcologyMixin` redirects `Animal.mobInteract` isFood check to `EcologyHooks.overrideIsFood(...)`

## Core Classes
- `EcologyProfile`: merged map + typed getters + cached values
- `EcologyMerge`: deep merge with null/empty list rules
- `EcologyProfileLoader`: reads base, profiles, archetypes; normalizes YAML
- `EcologyProfileRegistry`: profile map + handle cache + generation counter
- `EcologyComponent`: per-mob cache, handle NBT tags, goal registration flag
- `EcologyHandle`: system interface (goals, tick, NBT, food override)
- `EcologyHandleRegistry`: register and resolve handles per profile
- `EcologyHooks`: mixin dispatch for lifecycle, NBT, food override
- `EcologyResourceReloader`: Fabric resource reload listener
- `EcologyBootstrap`: registers handles and reload listener
- `EcologyAccess`: mixin-access interface for component access
- `AnimalItemStorage`: shared component for animals that carry items (wolves, foxes)

## Mixins
### MobEcologyMixin
Inject points:
- `<init>` after `registerGoals()` call
- `registerGoals` tail (secondary safeguard)
- `tick` tail
- `addAdditionalSaveData` tail
- `readAdditionalSaveData` tail

Reason: many mobs override `registerGoals()` without calling super, so the constructor hook is the only universal point. The hook is idempotent to avoid double registration.

### AnimalEcologyMixin
Redirect:
- `Animal.mobInteract` call to `isFood(ItemStack)` -> `EcologyHooks.overrideIsFood(...)`

## NBT Format
Root key: `BetterEcology`
Per-handle:
```
BetterEcology.<handle_id> -> CompoundTag
```
Each handle reads/writes only its own tag.

## Handle Model
Each handle:
1) `supports(profile)` checks if config fields are present
2) `registerGoals()` adds vanilla goals using config values
3) `tick()` updates internal state or performs actions
4) `readNbt()/writeNbt()` persists handle state

Example: `DietHandle`
- Reads:
  - `player_interaction.player_breeding.items`
  - `diet.food_sources.primary` entries with `type: ITEM`
- Supports item ids and tags (`#tag`)
- Caches item and tag sets per profile
- `overrideIsFood()` returns true when stack matches

## Performance Guardrails
- No YAML parsing during tick
- Profile caches per reload
- Handle caches per profile
- Use interval ticks for expensive logic

## Dependencies
- SnakeYAML (`org.yaml:snakeyaml`)
- Fabric API resource reload listener

## Source Reference (Minecraft)
Use `.minecraft-sources`:
- `common/net/minecraft/world/entity/Mob.java` (registerGoals, tick, NBT)
- `common/net/minecraft/world/entity/animal/Animal.java` (mobInteract, isFood)
- `common/net/minecraft/server/packs/resources/ResourceManager.java`
