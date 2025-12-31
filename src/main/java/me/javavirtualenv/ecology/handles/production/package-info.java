/**
 * Production-related handles for animal resource generation.
 *
 * <p>This package contains handles for managing various animal production systems:
 * <ul>
 *   <li>{@link me.javavirtualenv.ecology.handles.production.MilkProductionHandle} - Dairy milk production</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Data-driven production parameters via YAML</li>
 *   <li>Quality systems based on animal health and diet</li>
 *   <li>Regeneration mechanics with scientific basis</li>
 *   <li>Integration with breeding, hunger, and condition systems</li>
 * </ul>
 *
 * <h2>Scientific Basis</h2>
 *
 * <p>Milk production parameters are based on real-world animal agriculture:
 * <ul>
 *   <li><b>Cows:</b> 1-3 buckets per full udder, 1-2 hours regeneration per bucket</li>
 *   <li><b>Goats:</b> Faster regeneration, smaller capacity</li>
 *   <li><b>Quality factors:</b> Diet (grass consumption), health status, stress levels</li>
 *   <li><b>Lactation boost:</b> Production increases after giving birth</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 *
 * <p>Production handles are configured via YAML under the {@code milk_production} key:
 * <pre>
 * milk_production:
 *   enabled: true
 *   max_milk_amount: 3           # Maximum buckets when full
 *   regeneration_rate: 0.01      # Percentage per tick
 *   milk_quality: 50             # Base quality (0-100)
 *   min_milk_level: 33           # Minimum % to be milkable
 *   nursing_penalty: 0.7         # Rate reduction when calf nursing
 *   post_pregnancy_boost: 1.3    # Rate boost after birth
 *   is_mooshroom: false          # Special mooshroom behavior
 * </pre>
 *
 * @see me.javavirtualenv.ecology.EcologyHandle
 * @see me.javavirtualenv.ecology.handles.HungerHandle
 * @see me.javavirtualenv.ecology.handles.BreedingHandle
 */
package me.javavirtualenv.ecology.handles.production;
