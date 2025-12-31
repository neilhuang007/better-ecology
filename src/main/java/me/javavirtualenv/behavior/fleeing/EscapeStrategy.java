package me.javavirtualenv.behavior.fleeing;

/**
 * Defines escape strategy types based on anti-predator behavior research.
 * Different species use different strategies based on their morphology,
 * habitat, and predator types they face.
 * <p>
 * Based on research findings:
 * - Moore et al. (2017) - Unpredictability of escape trajectories
 * - Broom & Ruxton (2005) - Immediate flight as optimal strategy
 * - Humphries & Driver (1970) - Protean movement (unpredictable evasion)
 */
public enum EscapeStrategy {
    /**
     * Run directly away from threat in a straight line.
     * Effective when:
     * - Refuge is nearby and directly accessible
     * - Open terrain with no obstacles
     * - Speed is primary advantage over predator
     * <p>
     * Common in: Deer, antelope, horses (cursorial species)
     */
    STRAIGHT,

    /**
     * Unpredictable path changes with zigzagging motion.
     * Also known as "protean movement" - adaptively unpredictable behavior.
     * Effective when:
     * - Predator uses interception (constant bearing strategy)
     * - Tight maneuverability advantage over predator
     * - Confusion effect can be employed
     * <p>
     * Common in: Rabbits, hares, small rodents
     */
    ZIGZAG,

    /**
     * Head toward nearest shelter or refuge.
     * Prioritizes reaching safety over distance from threat.
     * Effective when:
     * - Known refuge locations nearby (burrows, dense cover)
     * - Environmental features provide concealment
     * - Risk of being caught in open is high
     * <p>
     * Common in: Rabbits (to warrens), birds (to cover), fish (to coral)
     */
    REFUGE,

    /**
     * Stay immobile briefly to avoid detection.
     * Alternative to immediate flight when predator detection is uncertain.
     * Effective when:
     * - Predator may not have detected prey yet
     * - Cryptic coloration provides camouflage
     * - Ambush predator response (freeze before fleeing)
     * Controlled by amygdala-PAG neural circuits
     * <p>
     * Common in: Rodents, deer fawns, some birds (before flight)
     */
    FREEZE
}
