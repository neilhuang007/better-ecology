/**
 * Sniffer-specific behaviors for the Better Ecology mod.
 * <p>
 * This package contains behaviors for the Sniffer mob, implementing:
 * <ul>
 *   <li>Enhanced smell detection for ancient seeds</li>
 *   <li>Digging behavior with biome-specific seed drops</li>
 *   <li>Parent teaching behaviors for baby sniffers</li>
 *   <li>Social communication between sniffers</li>
 *   <li>Memory of good digging spots</li>
 * </ul>
 * <p>
 * Sniffers are ancient creatures that use their keen sense of smell to locate
 * and dig up rare seeds from various biomes. Their behaviors are influenced by:
 * <ul>
 *   <li>Biome type - affects seed rarity and type</li>
 *   <li>Time of day - more active during daylight</li>
 *   <li>Social context - babies learn from adults</li>
 *   <li>Energy levels - limited daily digging capacity</li>
 * </ul>
 * <p>
 * Main behavior classes:
 * <ul>
 *   <li>{@link me.javavirtualenv.behavior.sniffer.SnifferBehavior} - Base class with scent detection</li>
 *   <li>{@link me.javavirtualenv.behavior.sniffer.SniffingBehavior} - Smell-based search</li>
 *   <li>{@link me.javavirtualenv.behavior.sniffer.DiggingBehavior} - Seed extraction</li>
 *   <li>{@link me.javavirtualenv.behavior.sniffer.SnifferSocialBehavior} - Teaching and communication</li>
 * </ul>
 */
package me.javavirtualenv.behavior.sniffer;
