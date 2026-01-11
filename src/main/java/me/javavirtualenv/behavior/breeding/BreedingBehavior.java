package me.javavirtualenv.behavior.breeding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main coordinator class for breeding and courtship behaviors.
 * Integrates all breeding subsystems including mate selection, courtship displays,
 * territorial defense, breeding seasons, mate fidelity, and parental investment.
 *
 * This redesigned version works with the BreedingEntity interface,
 * making it testable without mocking Minecraft classes.
 *
 * Research sources:
 * - Davies, N.B. (1991). Mating systems
 * - Andersson, M. (1994). Sexual Selection
 * - Clutton-Brock, T.H. (1991). The Evolution of Parental Care
 */
public class BreedingBehavior {

    private final BreedingConfig config;
    private final MateSelection mateSelection;
    private final CourtshipDisplay courtshipDisplay;
    private final TerritorialDefense territorialDefense;
    private final BreedingSeason breedingSeason;
    private final MateFidelity mateFidelity;
    private final ParentalInvestment parentalInvestment;
    private final Map<UUID, Long> lastBreedingTime;

    public BreedingBehavior(BreedingConfig config) {
        this.config = config;
        this.mateSelection = new MateSelection(config);
        this.courtshipDisplay = new CourtshipDisplay(config);
        this.territorialDefense = new TerritorialDefense(config);
        this.breedingSeason = new BreedingSeason(config);
        this.mateFidelity = new MateFidelity(config);
        this.parentalInvestment = new ParentalInvestment(config);
        this.lastBreedingTime = new HashMap<>();

        this.mateSelection.setTerritorialDefense(territorialDefense);
    }

    /**
     * Checks if two entities can breed with each other.
     * Considers breeding season, health, age, and compatibility.
     *
     * @param entity1 First potential partner
     * @param entity2 Second potential partner
     * @return True if breeding is possible
     */
    public boolean canBreed(BreedingEntity entity1, BreedingEntity entity2) {
        if (entity1 == null || entity2 == null) {
            return false;
        }

        if (!entity1.isAlive() || !entity2.isAlive()) {
            return false;
        }

        if (entity1.isBaby() || entity2.isBaby()) {
            return false;
        }

        if (entity1.isInLove() || entity2.isInLove()) {
            return false;
        }

        double health1Ratio = entity1.getHealth() / entity1.getMaxHealth();
        double health2Ratio = entity2.getHealth() / entity2.getMaxHealth();

        if (health1Ratio < config.getMinHealthForBreeding()) {
            return false;
        }

        if (health2Ratio < config.getMinHealthForBreeding()) {
            return false;
        }

        if (!breedingSeason.isBreedingSeason(entity1)) {
            return false;
        }

        if (!breedingSeason.isBreedingSeason(entity2)) {
            return false;
        }

        if (!canBreedNow(entity1)) {
            return false;
        }

        if (!canBreedNow(entity2)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if an entity can breed right now (cooldown check).
     *
     * @param entity The entity to check
     * @return True if breeding cooldown has elapsed
     */
    public boolean canBreedNow(BreedingEntity entity) {
        if (entity == null) {
            return false;
        }

        UUID entityId = entity.getUuid();

        if (!lastBreedingTime.containsKey(entityId)) {
            return true;
        }

        long lastBred = lastBreedingTime.get(entityId);
        long currentTime = entity.getGameTime();
        long timeSinceBreeding = currentTime - lastBred;

        return timeSinceBreeding >= config.getBreedingCooldown();
    }

    /**
     * Records that a breeding event occurred for an entity.
     * Updates cooldown timers and mate fidelity tracking.
     *
     * @param entity The entity that bred
     */
    public void recordBreeding(BreedingEntity entity) {
        if (entity == null) {
            return;
        }

        UUID entityId = entity.getUuid();
        long currentTime = entity.getGameTime();
        lastBreedingTime.put(entityId, currentTime);
    }

    /**
     * Records breeding for a pair, updating mate fidelity.
     *
     * @param entity1 First partner
     * @param entity2 Second partner
     */
    public void recordBreeding(BreedingEntity entity1, BreedingEntity entity2) {
        if (entity1 == null || entity2 == null) {
            return;
        }

        recordBreeding(entity1);
        recordBreeding(entity2);

        mateFidelity.recordPreviousMate(entity1, entity2.getUuid());
        mateFidelity.recordPreviousMate(entity2, entity1.getUuid());
    }

    /**
     * Selects the best mate from a list of potential partners.
     *
     * @param chooser The entity seeking a mate
     * @param potentialMates List of potential mates
     * @return The best mate, or null if none suitable
     */
    public BreedingEntity selectBestMate(BreedingEntity chooser, List<BreedingEntity> potentialMates) {
        List<BreedingEntity> suitableMates = mateSelection.filterSuitableMates(chooser, potentialMates);
        return mateFidelity.applyMateFidelity(chooser, suitableMates);
    }

    /**
     * Checks if courtship should be initiated with a potential mate.
     *
     * @param performer The entity performing the display
     * @param potentialMate The potential mate
     * @return True if courtship should begin
     */
    public boolean shouldInitiateCourtship(BreedingEntity performer, BreedingEntity potentialMate) {
        return courtshipDisplay.shouldInitiateCourtship(performer, potentialMate);
    }

    /**
     * Starts a courtship display.
     *
     * @param performer The entity performing the display
     */
    public void startCourtship(BreedingEntity performer) {
        courtshipDisplay.startCourtship(performer);
    }

    /**
     * Updates courtship state, called each tick.
     */
    public void tick() {
        courtshipDisplay.tick();
    }

    /**
     * Checks if courtship is complete.
     *
     * @param performer The performing entity
     * @return True if courtship duration has elapsed
     */
    public boolean isCourtshipComplete(BreedingEntity performer) {
        return courtshipDisplay.isCourtshipComplete(performer);
    }

    /**
     * Handles rejection of courtship.
     *
     * @param performer The entity that was rejected
     */
    public void onRejected(BreedingEntity performer) {
        courtshipDisplay.onRejected(performer);
    }

    /**
     * Sets the territory center for territorial defense.
     *
     * @param center The center position of the territory
     * @param ownerId The UUID of the territory owner
     */
    public void setTerritoryCenter(UUID ownerId, me.javavirtualenv.behavior.core.Vec3d center) {
        territorialDefense.setTerritoryCenter(ownerId, center);
    }

    /**
     * Checks if an entity is an intruder in the territory.
     *
     * @param owner The territory owner
     * @param potential The entity to check
     * @return True if the entity is an intruder
     */
    public boolean isIntruder(BreedingEntity owner, BreedingEntity potential) {
        return territorialDefense.isIntruder(owner, potential);
    }

    /**
     * Calculates the time since an entity last bred.
     *
     * @param entity The entity to check
     * @return Ticks since last breeding, or 0 if never bred
     */
    public long getTimeSinceLastBreeding(BreedingEntity entity) {
        UUID entityId = entity.getUuid();

        if (!lastBreedingTime.containsKey(entityId)) {
            return 0;
        }

        long lastBred = lastBreedingTime.get(entityId);
        return entity.getGameTime() - lastBred;
    }

    /**
     * Gets the current breeding season progress.
     *
     * @param entity The entity to check
     * @return Progress from 0.0 to 1.0
     */
    public double getSeasonProgress(BreedingEntity entity) {
        return breedingSeason.getSeasonProgress(entity);
    }

    /**
     * Checks if breeding season is ending soon.
     *
     * @param entity The entity to check
     * @param thresholdDays Days threshold for "soon"
     * @return True if season ends within threshold
     */
    public boolean isSeasonEndingSoon(BreedingEntity entity, int thresholdDays) {
        return breedingSeason.isSeasonEndingSoon(entity, thresholdDays);
    }

    /**
     * Registers an offspring to a parent.
     *
     * @param parent The parent entity
     * @param offspring The offspring UUID
     */
    public void registerOffspring(BreedingEntity parent, UUID offspring) {
        parentalInvestment.registerOffspring(parent, offspring);
    }

    /**
     * Calculates the parental care level for a parent.
     *
     * @param parent The parent entity
     * @return Care level from 0.0 to 1.0
     */
    public double calculateCareLevel(BreedingEntity parent) {
        return parentalInvestment.calculateCareLevel(parent);
    }

    /**
     * Checks if both parents invest in offspring.
     *
     * @param mother The mother entity
     * @param father The father entity
     * @param offspring The offspring UUID
     * @return True if both parents care for offspring
     */
    public boolean bothParentsInvest(BreedingEntity mother, BreedingEntity father, UUID offspring) {
        return parentalInvestment.bothParentsInvest(mother, father, offspring);
    }

    /**
     * Gets the breeding configuration.
     *
     * @return The breeding configuration
     */
    public BreedingConfig getConfig() {
        return config;
    }

    /**
     * Gets the mate selection subsystem.
     *
     * @return The mate selection instance
     */
    public MateSelection getMateSelection() {
        return mateSelection;
    }

    /**
     * Gets the courtship display subsystem.
     *
     * @return The courtship display instance
     */
    public CourtshipDisplay getCourtshipDisplay() {
        return courtshipDisplay;
    }

    /**
     * Gets the territorial defense subsystem.
     *
     * @return The territorial defense instance
     */
    public TerritorialDefense getTerritorialDefense() {
        return territorialDefense;
    }

    /**
     * Gets the breeding season subsystem.
     *
     * @return The breeding season instance
     */
    public BreedingSeason getBreedingSeason() {
        return breedingSeason;
    }

    /**
     * Gets the mate fidelity subsystem.
     *
     * @return The mate fidelity instance
     */
    public MateFidelity getMateFidelity() {
        return mateFidelity;
    }

    /**
     * Gets the parental investment subsystem.
     *
     * @return The parental investment instance
     */
    public ParentalInvestment getParentalInvestment() {
        return parentalInvestment;
    }

    /**
     * Cleans up stale data in all subsystems.
     * Should be called periodically to prevent memory leaks.
     */
    public void cleanup(BreedingEntity context) {
        courtshipDisplay.cleanup();
        territorialDefense.cleanup();
        mateFidelity.cleanup(context);
        parentalInvestment.cleanup();
    }
}
