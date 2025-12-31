# Breeding and Courtship Behaviors in Mammals and Birds

## Overview

This document summarizes research on mate selection, courtship rituals, mating systems, territorial behavior, breeding seasons, and parental investment across species.

## Key Findings

### Mate Selection Mechanisms

- **Visual displays**: Female birds of paradise choose males based on combined plumage and dance moves, driving evolution of elaborate traits
- **Vocal signals**: Machine learning reveals cryptic song dialects that influence mate choice in songbirds
- **Honest signals**: Antler size in deer provides honest signals of male phenotypic quality, with positive correlation to testicle size and sperm quality
- **Male mate choice copying**: A newly recognized phenomenon where males copy the mate choices of other males (2025 study)

### Courtship Rituals

- **Birds of paradise**: Combine extravagant plumage, complex vocal arrangements, and shape-shifting dance moves; behaviors are culturally transmitted from fathers to sons
- **Manakins**: Perform physically elaborate displays where females choose based on motor skills; physiological mechanisms enable complex performances
- **Lekking systems**: Males gather in groups to display (peacocks, manakins); males provide only genes with no parental care
- **Dynamic displays**: Courtship intensity varies temporally; some species use "coy" display behaviors strategically

---

## Mating Systems Diversity

| System | Description | Taxa Distribution |
|--------|-------------|-------------------|
| **Monogamy** | Pair bond between one male and one female | More common in birds than mammals |
| **Polygyny** | One male mates with multiple females | More common in mammals than birds |
| **Polyandry** | One female mates with multiple males | Relatively rare in both groups |
| **Polygynandry** | Multiple males mate with multiple females | Occurs in some species |
| **Promiscuity** | Mating without pair bonds | Common in many species |

### Distribution Patterns

- **Polygyny dominates** in mammals; **monogamy more common** in birds
- Lekking systems evolved independently in multiple bird lineages
- Sexual selection intensity varies geographically (higher latitudes show stronger selection)

---

## Territorial Behavior

- Territory size directly correlates with male mating success (dart-poison frogs study)
- Breeding success often increases with territory size
- **Recent breeding success** leads to stronger territorial defense investment (common loons)
- Resource-defense tactics: reproductive success depends on territory location and attractiveness
- In waterfowl, territorial behavior is closely associated with mate defense

---

## Breeding Seasons and Environmental Triggers

### Primary Cue

**Photoperiod (day length)** is the main environmental signal regulating seasonal breeding

### Neuroendocrine Pathway

- Pineal gland and melatonin serve as transducers
- Coordinates with thyroid-stimulating hormone (TSH)
- Integrates multiple environmental cues

### Modulating Factors

- Food availability
- Social interactions
- Stress levels
- Weather patterns
- Temperature
- Humidity
- Rainfall

### Climate Influence

- Climate determines mating behavior in birds globally
- Sexual selection often peaks at higher latitudes
- Some species exhibit flexible mate choice in response to fluctuating environments

---

## Parental Investment Patterns

- **Sexual dimorphism correlation**: Negative correlation between male parental care and sexual size dimorphism (North American birds)
- **Pair bonding**: Regulated by specific neurobiological mechanisms; studied in monogamous rodents
- **Sex role coevolution**: Sex differences in parental investment and mating competition coevolve
- **Cooperative breeding**: Climate influences distribution of cooperative breeding in mammals

---

## Sexual Selection and Ornamentation

### Weapons vs. Ornaments

- **Antlers/horns**: Serve as both weapons in male-male competition and visual signals for female choice
- **Honest signaling**: Exaggerated traits indicate overall male quality, fighting ability, and reproductive capacity
- **Female ornaments**: Female ornamentation and weaponry can evolve through both mate choice and intrasexual competition
- **Multiple signals**: Visual displays often combine with behavioral traits (dance complexity, vocalizations)
- **Costly traits**: Negative association between horn length and survival in species with low sexual size dimorphism

---

## Species-Specific Examples

### Birds

**Birds of paradise**:
- Elaborate dances and plumage
- Cultural transmission of display behaviors
- Multiple signal types combined

**Manakins**:
- Motor skill-based displays
- Physiological control of elaborate courtship

**Peacocks**:
- Lek mating with relatives
- Group display benefits

**Songbirds**:
- Dialect-based mate choice
- Machine learning reveals cryptic patterns

### Mammals

**Deer/elk**:
- Antlers as honest signals of male quality
- Testosterone-linked development
- Female preference for large antlers

**Seals**:
- Territorial defense correlates with breeding success

**Primates**:
- Neurobiological mechanisms of pair bonding similar to monogamous rodents

**Various mammals**:
- Polygyny more common
- Male-male competition drives weaponry evolution

---

## Key Academic References

### Recent Papers (2024-2025)

1. **Janicke et al. (2025)** - "Sexual selection and speciation: a meta-analysis" - Evolution Letters
2. **Staerk et al. (2025)** - "Sexual selection drives sex difference in adult life expectancy" - PMC
3. **Shen et al. (2024)** - "Study on mate choice in animals" - ScienceDirect
4. **(2025)** - "Male mate-choice copying" - Oikos
5. **November 2024** - "Climate and bird mating behavior" - PLoS Biology
6. **Vieira et al. (2025)** - "Regulation of Seasonal Reproduction in Wild Birds" - MDPI

### Foundational Studies

7. **Mitoyen et al. (2019)** - "Evolution and function of multimodal courtship displays" (156 citations)
8. **Hollon et al. (2023)** - "The evolution of dynamic and flexible courtship displays"
9. **Kotiaho (2002)** - "Sexual selection and condition dependence of courtship" (118 citations)
10. **Fusani et al. (2014)** - "Physiological control of elaborate male courtship" (87 citations)
11. **Clutton-Brock (1989)** - "Mammalian Mating Systems" (1,948 citations)
12. **Lukas (2020)** - "Monotocy and the evolution of plural breeding in mammals"

### Other Key References

13. - "Light and Hormones in Seasonal Regulation of Reproduction"
14. - "Seasonal Breeding in Mammals: From Basic Science"
15. - "Climate Change and Seasonal Reproduction in Mammals"
16. - "Neurobiology of Pair Bonding"
17. - "The evolution of female ornaments and weaponry"
18. - "What explains the diversity of sexually selected traits"
19. - "Antler Size Provides an Honest Signal of Male Phenotypic Quality"
20. - "Antlers honestly advertise sperm production and quality"

---

## Implementation Notes for Minecraft Mod

### Key Behaviors to Implement

1. **Mate selection**: Females choose mates based on display traits/health
2. **Courtship displays**: Visual and/or behavioral displays before mating
3. **Territorial defense**: Defend breeding territories from rivals
4. **Breeding season triggers**: Environmental cues for breeding availability
5. **Parental investment**: Both parents may care for young (species-dependent)
6. **Mate fidelity**: Some species form long-term pair bonds

### Configuration Parameters

| Parameter | Default Range | Description |
|-----------|---------------|-------------|
| `matingSystem` | MONOGAMY/POLYGYNY/LEKKING | Breeding system type |
| `breedingSeasonStart` | Any month | When breeding begins |
| `breedingSeasonEnd` | Any month | When breeding ends |
| `breedingSeasonLength` | 1-12 months | Duration of breeding availability |
| `territorySize` | 16-64 blocks | Radius of breeding territory |
| `courtshipDuration` | 5-60 seconds | Time spent displaying |
| `displayRange` | 8-32 blocks | Range of courtship display |
| `mateFidelity` | 0.0-1.0 | Chance of re-mating same partner |

### Minecraft Entity Considerations

**Vanilla breeding mechanics**:
- Currently: Feed specific item → baby spawns
- Could enhance with: courtship period, mate selection, territorial behavior

**Animals that could benefit**:
- **Wolves**: Already have mate fidelity (tamed pair), could expand
- **Cats, Parrots**: Could add courtship displays
- **Horses, Llamas**: Could add territorial behavior
- **Foxes**: Already have some unique behaviors (night sleep, holding items)
- **Pandas**: Could enhance breeding with more realism

**New behaviors to add**:
- **Courtship period**: Animals display before mating (spinning, dancing, vocalizing)
- **Mate selection**: Females prefer healthier/better-displaying males
- **Territoriality**: Males defend areas from rivals during breeding
- **Seasonal breeding**: Only breed during certain times (configurable)
- **Pair bonding**: Some animals mate for life or season
- **Lekking**: Males gather to display (like peacocks)

### Code Structure Suggestion

```java
public class CourtshipBehavior {
    private final MatingSystem matingSystem;
    private final int courtshipDuration;

    public void seekMate(AnimalEntity animal) {
        // Only breed during breeding season
        if (!isBreedingSeason(animal.level())) {
            return;
        }

        // Find potential mates
        List<AnimalEntity> potentialMates = getPotentialMates(animal);
        if (potentialMates.isEmpty()) {
            return;
        }

        // Select best mate based on traits
        AnimalEntity bestMate = selectBestMate(animal, potentialMates);

        // Begin courtship display
        if (animal.distanceTo(bestMate) < displayRange) {
            startCourtship(animal, bestMate);
        }
    }

    private AnimalEntity selectBestMate(AnimalEntity chooser, List<AnimalEntity> candidates) {
        return candidates.stream()
            .max(Comparator.comparingDouble(mate ->
                assessMateQuality(chooser, mate)))
            .orElse(null);
    }

    private double assessMateQuality(AnimalEntity chooser, AnimalEntity potential) {
        double quality = 0.0;

        // Health factor
        quality += potential.getHealth() / potential.getMaxHealth();

        // Size factor (for some species)
        if (prefersLargerMates(chooser)) {
            quality += potential.getBB().getSize() * 0.1;
        }

        // Display traits (could be custom NBT data)
        quality += getDisplayTraitScore(potential);

        return quality;
    }
}

public enum MatingSystem {
    MONOGAMOUS,      // One mate for life/season
    POLYGYNOUS,      // Male mates with multiple females
    POLYANDROUS,     // Female mates with multiple males
    PROMISCUOUS,     // No pair bonds
    LEKKING          // Males display in groups
}
```

### Breeding Triggers

| Trigger | Effect | Implementation |
|---------|--------|----------------|
| **Photoperiod** | Seasonal breeding | Day time / season check |
| **Food availability** | Better nutrition = more likely | Recent food intake |
| **Temperature** | Affects breeding timing | Biome temperature |
| **Population** | Overpopulation suppresses | Nearby same-species count |
| **Social status** | Dominant animals breed first | Custom dominance value |

### Territorial Behavior

```java
public class TerritorialBehavior {
    private final int territoryRadius;
    private final BlockPos territoryCenter;

    public boolean isInTerritory(AnimalEntity owner, AnimalEntity intruder) {
        double distance = intruder.blockPosition().distSqr(territoryCenter);
        return distance <= territoryRadius * territoryRadius;
    }

    public void defendTerritory(AnimalEntity owner) {
        owner.level().getEntitiesOfClass(owner.getClass(),
            owner.getBoundingBox().inflate(territoryRadius))
            .stream()
            .filter(intruder -> intruder != owner && isSameSex(owner, intruder))
            .filter(intruder -> isInTerritory(owner, intruder))
            .forEach(rival -> {
                // Aggressive display or attack
                owner.setTarget(rival);
                // Roar, charge, or posture
                performAggressiveDisplay(owner, rival);
            });
    }
}
```

### Display Types by Species

| Species | Display Type | Visual/Behavioral |
|---------|--------------|-------------------|
| **Birds** | Dancing | Spinning, bobbing, wing spreading |
| **Deer** | Posturing | Antler display, parallel walk |
| **Canines** | Scent marking | Urine marking, howling |
| **Felines** | Vocalization | Roaring, purring, chirping |
| **Primates** | Coloration | Red face, chest puffing |
