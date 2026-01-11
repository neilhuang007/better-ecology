package me.javavirtualenv.behavior.breeding;

import me.javavirtualenv.behavior.core.Vec3d;

import java.util.UUID;

/**
 * Interface representing essential data for breeding behaviors.
 * This abstraction allows breeding algorithms to work with any entity type
 * (Minecraft entities, test fixtures, POJOs) without direct dependencies.
 *
 * The interface captures only the data needed for breeding decisions:
 * - Position for distance calculations
 * - Health and age for mate quality assessment
 * - Love/baby status for breeding eligibility
 * - UUID for tracking mates and offspring
 *
 * Implementations can wrap Minecraft entities or provide test data.
 */
public interface BreedingEntity {

    /**
     * Gets the unique identifier for this entity.
     * Used for mate fidelity tracking and offspring registration.
     *
     * @return The entity's UUID
     */
    UUID getUuid();

    /**
     * Gets the current position of this entity.
     * Used for distance calculations in mate selection and territorial defense.
     *
     * @return The entity's position as a Vec3d
     */
    Vec3d getPosition();

    /**
     * Gets the current health of this entity.
     * Used for mate quality assessment and breeding eligibility.
     *
     * @return Current health value
     */
    double getHealth();

    /**
     * Gets the maximum health of this entity.
     * Used to calculate health ratio for breeding decisions.
     *
     * @return Maximum health value
     */
    double getMaxHealth();

    /**
     * Gets the age of this entity in ticks.
     * Used for maturity assessment and mate quality.
     *
     * @return Age in ticks (positive for adult, negative for baby)
     */
    int getAge();

    /**
     * Checks if this entity is currently a baby.
     * Babies cannot breed.
     *
     * @return True if the entity is a baby
     */
    boolean isBaby();

    /**
     * Checks if this entity is currently in love mode.
     * Entities in love are already breeding and unavailable.
     *
     * @return True if the entity is in love
     */
    boolean isInLove();

    /**
     * Checks if this entity is alive.
     * Dead entities cannot breed.
     *
     * @return True if the entity is alive
     */
    boolean isAlive();

    /**
     * Gets the sex of this entity for mate selection.
     * Used to ensure opposite-sex pairing.
     *
     * @return True if male, false if female
     */
    boolean isMale();

    /**
     * Gets the current game time in ticks.
     * Used for breeding season and cooldown calculations.
     *
     * @return Current game time
     */
    long getGameTime();

    /**
     * Gets the current day time in ticks.
     * Used for photoperiod-based breeding triggers.
     *
     * @return Current day time (0-24000)
     */
    long getDayTime();

    /**
     * Gets the species identifier for this entity.
     * Used for species-specific breeding configurations.
     *
     * @return Species identifier (e.g., "entity.minecraft.wolf")
     */
    String getSpeciesId();

    /**
     * Gets the size of this entity.
     * Used for display trait assessment and honest signals.
     *
     * @return Size value (typically 0.5-2.0)
     */
    double getSize();

    /**
     * Gets the temperature of the entity's environment.
     * Used for modulating factors affecting breeding season.
     *
     * @return Temperature value (0.0-1.0, where 0.5 is temperate)
     */
    double getTemperature();
}
