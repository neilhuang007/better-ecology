package me.javavirtualenv.behavior.breeding;

/**
 * Courtship display types from animal behavior research.
 * Defines the various modalities animals use to attract and select mates.
 *
 * Research sources:
 * - Darwin, C. (1871). The Descent of Man, and Selection in Relation to Sex
 * - Bradbury, J.W., & Vehrencamp, S.L. (2011). Principles of Animal Communication
 * - Hebets, E.A., & Papaj, D.R. (2005). Complex signal function
 */
public enum DisplayType {
    /**
     * Elaborate movement displays.
     * Examples: Birds of paradise dances, manakin leaps, bowerbird constructions
     * Characterized by precise choreography, stamina demonstrations, motor skill
     */
    DANCING,

    /**
     * Songs and calls for mate attraction.
     * Examples: Songbird songs, whale songs, frog calls, insect chirps
     * Characterized by frequency modulation, temporal patterns, repertoire size
     */
    VOCALIZATION,

    /**
     * Visual displays of size and weapons.
     * Examples: Deer antler displays, crab claw waving, lizard head-bobbing
     * Characterized by size exaggeration, threat displays, physical posturing
     */
    POSTURING,

    /**
     * Display of plumage and physical traits.
     * Examples: Peacock tails, bird plumage, primate coloration, fish colors
     * Characterized by bright colors, symmetry, condition-dependent traits
     */
    COLORATION,

    /**
     * Presenting food or objects to potential mates.
     * Examples: Kingfisher fish offerings, tern fish deliveries, bowerbird bowers
     * Characterized: resource demonstration, foraging ability, provisioning potential
     */
    GIFT_GIVING,

    /**
     * Chemical signals for mate attraction.
     * Examples: Canine urine marking, feline cheek rubbing, insect pheromones
     * Characterized by territorial marking, individual identity, reproductive state
     */
    SCENT_MARKING
}
