package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Selects optimal prey from available targets based on multiple factors.
 * Implements optimal foraging theory - predators select prey that maximizes
 * energy gain while minimizing risk and energy expenditure.
 * <p>
 * Based on research by:
 * - Emlen (1966) - Optimal choice in foraging behavior
 * - MacArthur & Pianka (1966) - On optimal use of a patchy environment
 * - Curio (1976) - The ethology of predation
 */
public class PreySelector {

    private final double maxPreyDistance;
    private final double sizePreference;
    private final double injuryBonus;
    private final double babyBonus;

    public PreySelector(double maxPreyDistance, double sizePreference,
                       double injuryBonus, double babyBonus) {
        this.maxPreyDistance = maxPreyDistance;
        this.sizePreference = sizePreference;
        this.injuryBonus = injuryBonus;
        this.babyBonus = babyBonus;
    }

    public PreySelector() {
        this(32.0, 1.0, 1.5, 2.0);
    }

    /**
     * Selects the best prey from available entities.
     *
     * @param predator The predator doing the hunting
     * @return The selected prey, or null if no valid prey found
     */
    public Entity selectPrey(Mob predator) {
        List<LivingEntity> nearbyEntities = predator.level().getEntitiesOfClass(
            LivingEntity.class,
            predator.getBoundingBox().inflate(maxPreyDistance)
        );

        List<PreyCandidate> candidates = new ArrayList<>();

        for (LivingEntity entity : nearbyEntities) {
            if (entity.equals(predator)) {
                continue;
            }

            if (!isValidPrey(predator, entity)) {
                continue;
            }

            double score = scorePrey(predator, entity);
            candidates.add(new PreyCandidate(entity, score));
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Sort by score (lower is better - it's a "cost" score)
        candidates.sort(Comparator.comparingDouble(c -> c.score));

        return candidates.get(0).entity;
    }

    /**
     * Scores a potential prey item.
     * Lower score = better prey (easier catch, closer).
     */
    private double scorePrey(Mob predator, LivingEntity prey) {
        double score = 0.0;

        // Distance cost
        double distance = predator.position().distanceTo(prey.position());
        score += distance;

        // Size cost - prefer smaller prey
        double sizeRatio = prey.getBbHeight() / predator.getBbHeight();
        score += sizeRatio * sizePreference * 10.0;

        // Speed cost - faster prey are harder to catch
        double preySpeed = prey.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        score += preySpeed * 5.0;

        // Health cost - injured prey are easier
        if (prey instanceof Animal animal) {
            double healthPercent = animal.getHealth() / animal.getMaxHealth();
            if (healthPercent < 0.5) {
                score -= injuryBonus * 5.0; // Bonus for injured prey
            }
            if (animal.isBaby()) {
                score -= babyBonus * 3.0; // Bonus for baby prey
            }
        }

        // Group size cost - lone prey easier than groups
        int nearbyConspecifics = countNearbyConspecifics(prey);
        score += nearbyConspecifics * 2.0;

        return score;
    }

    /**
     * Determines if an entity is valid prey for this predator.
     */
    private boolean isValidPrey(Mob predator, LivingEntity entity) {
        // Must be alive
        if (!entity.isAlive()) {
            return false;
        }

        // Don't hunt players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        // Prey should be appropriately sized
        double predatorSize = predator.getBbHeight();
        double preySize = entity.getBbHeight();

        // Too big to hunt
        if (preySize > predatorSize * 1.5) {
            return false;
        }

        // Too small to be worth it
        if (preySize < predatorSize * 0.1) {
            return false;
        }

        return true;
    }

    /**
     * Counts nearby entities of the same species as the prey.
     * Used to assess group protection benefits.
     */
    private int countNearbyConspecifics(LivingEntity prey) {
        int count = 0;
        double detectionRange = 16.0;

        List<LivingEntity> nearby = prey.level().getEntitiesOfClass(
            LivingEntity.class,
            prey.getBoundingBox().inflate(detectionRange)
        );

        for (LivingEntity entity : nearby) {
            if (!entity.equals(prey) && entity.getType().equals(prey.getType())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Gets all valid prey in range, sorted by preference.
     */
    public List<LivingEntity> getRankedPrey(Mob predator) {
        List<LivingEntity> nearbyEntities = predator.level().getEntitiesOfClass(
            LivingEntity.class,
            predator.getBoundingBox().inflate(maxPreyDistance)
        );

        List<PreyCandidate> candidates = new ArrayList<>();

        for (LivingEntity entity : nearbyEntities) {
            if (entity.equals(predator)) {
                continue;
            }

            if (!isValidPrey(predator, entity)) {
                continue;
            }

            double score = scorePrey(predator, entity);
            candidates.add(new PreyCandidate(entity, score));
        }

        candidates.sort(Comparator.comparingDouble(c -> c.score));

        List<LivingEntity> result = new ArrayList<>();
        for (PreyCandidate candidate : candidates) {
            result.add((LivingEntity) candidate.entity);
        }

        return result;
    }

    /**
     * Checks if specific prey is in range.
     */
    public boolean isPreyInRange(Mob predator, Entity prey) {
        if (!prey.isAlive()) {
            return false;
        }

        double distance = predator.position().distanceTo(prey.position());
        if (distance > maxPreyDistance) {
            return false;
        }

        return isValidPrey(predator, (LivingEntity) prey);
    }

    /**
     * Internal class for tracking prey candidates.
     */
    private static class PreyCandidate {
        final Entity entity;
        final double score;

        PreyCandidate(Entity entity, double score) {
            this.entity = entity;
            this.score = score;
        }
    }

    public double getMaxPreyDistance() {
        return maxPreyDistance;
    }

    public double getSizePreference() {
        return sizePreference;
    }

    public double getInjuryBonus() {
        return injuryBonus;
    }

    public double getBabyBonus() {
        return babyBonus;
    }
}
