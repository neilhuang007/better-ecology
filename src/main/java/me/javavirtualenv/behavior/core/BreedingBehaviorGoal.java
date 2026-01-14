package me.javavirtualenv.behavior.core;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal that implements enhanced breeding behavior for animals.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when animal is well-fed and ready to breed</li>
 *   <li>Searches for suitable mates within range</li>
 *   <li>Selects best mate based on health and proximity</li>
 *   <li>Performs courtship display (circling behavior)</li>
 *   <li>Triggers vanilla breeding when courtship is complete</li>
 * </ul>
 *
 * <p>Based on research from breeding-courtship-behaviors.md:
 * - Mate selection based on health/quality
 * - Courtship duration before breeding
 * - Hunger requirements (can't breed if starving)
 */
public class BreedingBehaviorGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreedingBehaviorGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 40;
    private static final int COURTSHIP_DURATION_TICKS = 60;  // 3 seconds
    private static final double COURTSHIP_DISTANCE = 2.5;
    private static final double SEARCH_RANGE = 16.0;
    private static final float MIN_HUNGER_TO_BREED = 60f;  // Must be well-fed to breed

    private final Animal animal;
    private final double speedModifier;
    private final Class<? extends Animal> partnerClass;

    private Animal partner;
    private int searchCooldown;
    private int courtshipTicks;
    private boolean inCourtship;

    /**
     * Creates a new BreedingBehaviorGoal.
     *
     * @param animal the animal that will seek mates
     * @param speedModifier movement speed multiplier when approaching mate
     * @param partnerClass the class of animals this animal can mate with
     */
    public BreedingBehaviorGoal(Animal animal, double speedModifier, Class<? extends Animal> partnerClass) {
        this.animal = animal;
        this.speedModifier = speedModifier;
        this.partnerClass = partnerClass;
        this.searchCooldown = 0;
        this.courtshipTicks = 0;
        this.inCourtship = false;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Creates a new BreedingBehaviorGoal for same-species breeding.
     *
     * @param animal the animal that will seek mates
     * @param speedModifier movement speed multiplier when approaching mate
     */
    @SuppressWarnings("unchecked")
    public BreedingBehaviorGoal(Animal animal, double speedModifier) {
        this(animal, speedModifier, (Class<? extends Animal>) animal.getClass());
    }

    @Override
    public boolean canUse() {
        // Check basic breeding requirements
        if (!canAttemptBreeding()) {
            return false;
        }

        // Search cooldown
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        return findMate();
    }

    /**
     * Checks if the animal can attempt to breed.
     *
     * @return true if all breeding prerequisites are met
     */
    private boolean canAttemptBreeding() {
        // Must be in love mode from vanilla breeding
        if (!this.animal.isInLove()) {
            return false;
        }

        // Must be well-fed (not just hungry)
        float hunger = AnimalNeeds.getHunger((Mob) this.animal);
        if (hunger < MIN_HUNGER_TO_BREED) {
            LOGGER.debug("{} too hungry to breed: {} < {}",
                this.animal.getName().getString(), hunger, MIN_HUNGER_TO_BREED);
            return false;
        }

        // Must be an adult
        if (this.animal.isBaby()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if partner is gone or no longer valid
        if (this.partner == null || !this.partner.isAlive() || !this.partner.isInLove()) {
            return false;
        }

        // Stop if we're no longer in love
        if (!this.animal.isInLove()) {
            return false;
        }

        // Stop if courtship is complete
        if (this.courtshipTicks >= COURTSHIP_DURATION_TICKS && this.inCourtship) {
            // Trigger breeding
            triggerBreeding();
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting courtship with {}",
            this.animal.getName().getString(),
            this.partner != null ? this.partner.getName().getString() : "null");
        this.courtshipTicks = 0;
        this.inCourtship = false;

        if (this.partner != null) {
            navigateToPartner();
        }
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped courtship",
            this.animal.getName().getString());
        this.partner = null;
        this.courtshipTicks = 0;
        this.inCourtship = false;
        this.animal.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }

        // Look at partner
        this.animal.getLookControl().setLookAt(this.partner, 30.0F, 30.0F);

        double distanceSq = this.animal.distanceToSqr(this.partner);
        double courtshipDistSq = COURTSHIP_DISTANCE * COURTSHIP_DISTANCE;

        if (distanceSq <= courtshipDistSq) {
            // In courtship range
            this.inCourtship = true;
            this.courtshipTicks++;

            // Perform courtship display (stop moving and face partner)
            this.animal.getNavigation().stop();

            // Every 20 ticks, circle around partner (visual display)
            if (this.courtshipTicks % 20 == 0) {
                performCourtshipDisplay();
            }
        } else {
            // Still approaching partner
            this.inCourtship = false;

            if (this.animal.getNavigation().isDone()) {
                navigateToPartner();
            }
        }
    }

    /**
     * Finds a suitable mate within range.
     *
     * @return true if a mate was found
     */
    private boolean findMate() {
        AABB searchBox = this.animal.getBoundingBox().inflate(SEARCH_RANGE);
        List<Animal> potentialMates = this.animal.level().getEntitiesOfClass(
            Animal.class, searchBox, this::isValidMate);

        if (potentialMates.isEmpty()) {
            return false;
        }

        // Select best mate based on health and proximity
        Animal selectedMate = selectBestMate(potentialMates);

        if (selectedMate != null) {
            this.partner = selectedMate;
            LOGGER.debug("{} selected mate: {} (quality: {})",
                this.animal.getName().getString(),
                selectedMate.getName().getString(),
                String.format("%.2f", calculateMateQuality(selectedMate)));
            return true;
        }

        return false;
    }

    /**
     * Checks if an entity is a valid mate.
     *
     * @param candidate the candidate mate
     * @return true if the candidate is a valid mate
     */
    private boolean isValidMate(Animal candidate) {
        if (candidate == null || !candidate.isAlive()) {
            return false;
        }

        // Can't mate with self
        if (candidate == this.animal) {
            return false;
        }

        // Must be in love
        if (!candidate.isInLove()) {
            return false;
        }

        // Must be adult
        if (candidate.isBaby()) {
            return false;
        }

        // Must be same species
        if (!this.partnerClass.isInstance(candidate)) {
            return false;
        }

        // Must also be well-fed
        float candidateHunger = AnimalNeeds.getHunger((Mob) candidate);
        if (candidateHunger < MIN_HUNGER_TO_BREED) {
            return false;
        }

        return true;
    }

    /**
     * Selects the best mate from a list of candidates.
     * Prefers mates that are healthier and closer.
     *
     * @param candidates list of potential mates
     * @return the best mate, or null if none suitable
     */
    private Animal selectBestMate(List<Animal> candidates) {
        return candidates.stream()
            .max(Comparator.comparingDouble(this::calculateMateQuality))
            .orElse(null);
    }

    /**
     * Calculates mate quality score.
     * Higher scores are better.
     *
     * @param candidate the candidate mate
     * @return quality score (0.0 to ~3.0)
     */
    private double calculateMateQuality(Animal candidate) {
        double quality = 0.0;

        // Health factor (0.0 to 1.0)
        double healthRatio = candidate.getHealth() / candidate.getMaxHealth();
        quality += healthRatio;

        // Hunger factor (0.0 to 1.0)
        float hunger = AnimalNeeds.getHunger((Mob) candidate);
        double hungerRatio = hunger / AnimalNeeds.MAX_VALUE;
        quality += hungerRatio;

        // Proximity factor (closer is better, 0.0 to 1.0)
        double distance = this.animal.distanceTo(candidate);
        double proximityFactor = 1.0 - Math.min(distance / SEARCH_RANGE, 1.0);
        quality += proximityFactor;

        return quality;
    }

    /**
     * Navigates to the partner.
     */
    private void navigateToPartner() {
        if (this.partner != null) {
            this.animal.getNavigation().moveTo(this.partner, this.speedModifier);
        }
    }

    /**
     * Performs a courtship display (visual effect).
     */
    private void performCourtshipDisplay() {
        // Rotate around partner position
        LOGGER.debug("{} performing courtship display for {}",
            this.animal.getName().getString(),
            this.partner.getName().getString());

        // The visual effect would be handled client-side
        // Here we just log and update state
    }

    /**
     * Triggers vanilla breeding between the two animals.
     */
    private void triggerBreeding() {
        if (this.partner == null) {
            return;
        }

        LOGGER.debug("{} completed courtship and breeding with {}",
            this.animal.getName().getString(),
            this.partner.getName().getString());

        // Reduce hunger after breeding (breeding costs energy)
        AnimalNeeds.modifyHunger((Mob) this.animal, -20f);
        AnimalNeeds.modifyHunger((Mob) this.partner, -20f);

        // Let vanilla handle the actual baby creation
        // The animals are already in love, so vanilla BreedGoal will trigger
    }
}
