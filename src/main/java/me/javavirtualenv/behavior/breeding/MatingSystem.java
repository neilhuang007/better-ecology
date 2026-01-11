package me.javavirtualenv.behavior.breeding;

/**
 * Mating system types based on animal behavior research.
 * Defines the patterns of mate selection and pair bonding across species.
 *
 * Research sources:
 * - Kleiman, M.G. (1977). Monogamy in mammals
 * - Davies, N.B. (1991). Mating systems
 * - Andersson, M. (1994). Sexual selection
 */
public enum MatingSystem {
    /**
     * One mate for life/season.
     * Examples: Wolves, albatross, swans, gibbons, beavers, otters
     * Characterized by high mate fidelity, biparental care, territory defense
     */
    MONOGAMY,

    /**
     * Male mates with multiple females.
     * Examples: Deer, elk, seals, lions, elk, horses, cattle
     * Characterized by male-male competition, female choice, sexual dimorphism
     */
    POLYGYNY,

    /**
     * Males display in groups for females to choose.
     * Examples: Birds of paradise, peacocks, manakins, grouse, sage grouse
     * Characterized by elaborate displays, minimal male parental care, female choice
     */
    LEKKING,

    /**
     * Female mates with multiple males.
     * Examples: Jacana (jacana birds), spotted sandpiper, red-necked phalarope
     * Characterized by role reversal, male parental care, female-female competition
     */
    POLYANDRY,

    /**
     * Mating without pair bonds.
     * Examples: Many fish species, some rodents, chimpanzees, bonobos
     * Characterized by no mate fidelity, minimal parental care, multiple partners
     */
    PROMISCUITY
}
