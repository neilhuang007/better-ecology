package me.javavirtualenv.behavior.herd;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Combined herd movement system integrating quorum sensing, leader following, and cohesion.
 * Coordinates collective movement decisions based on research findings from ungulate behavior.
 *
 * This system implements:
 * - Quorum-based movement initiation (wait for threshold of group)
 * - Age/dominance-based shared leadership
 * - Selfish herd positioning for vulnerable individuals
 * - Species-specific configurations
 */
public class HerdBehavior extends SteeringBehavior {

    private final HerdConfig config;
    private final QuorumMovement quorumMovement;
    private final LeaderFollowing leaderFollowing;
    private final HerdCohesion herdCohesion;

    private UUID herdId;
    private List<Entity> cachedHerdMembers;
    private double lastCacheUpdate = 0.0;
    private static final double CACHE_UPDATE_INTERVAL = 0.5; // seconds

    public HerdBehavior(HerdConfig config) {
        this.config = config;
        this.quorumMovement = new QuorumMovement(config);
        this.leaderFollowing = new LeaderFollowing(config);
        this.herdCohesion = new HerdCohesion(config);
        this.herdId = UUID.randomUUID();
    }

    public HerdBehavior(HerdConfig config, double weight) {
        this(config);
        this.weight = weight;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        List<Entity> herdMembers = getHerdMembers(context);

        if (herdMembers.isEmpty()) {
            return new Vec3d();
        }

        Vec3d totalForce = new Vec3d();

        // Check quorum first - only move if threshold is met
        quorumMovement.updateQuorumStatus(context, herdMembers);
        if (!quorumMovement.isMovementAllowed()) {
            return new Vec3d();
        }

        // Calculate leader following force
        leaderFollowing.updateLeader(context, herdMembers);
        Vec3d leaderForce = leaderFollowing.calculateWeighted(context);
        totalForce.add(leaderForce);

        // Calculate cohesion force with selfish positioning (pass herd members)
        Vec3d cohesionForce = herdCohesion.calculateCohesion(context, herdMembers);
        cohesionForce.mult(herdCohesion.getWeight());
        totalForce.add(cohesionForce);

        // Add separation to prevent crowding
        Vec3d separationForce = herdCohesion.calculateSeparation(context, herdMembers);
        separationForce.mult(1.2); // Separation has higher priority
        totalForce.add(separationForce);

        totalForce.limit(config.getMaxForce());
        return totalForce;
    }

    /**
     * Gets nearby herd members of the same species.
     * Caches results to avoid repeated searches.
     */
    public List<Entity> getHerdMembers(BehaviorContext context) {
        double currentTime = context.getEntity().level().getGameTime() / 20.0;

        if (cachedHerdMembers != null && currentTime - lastCacheUpdate < CACHE_UPDATE_INTERVAL) {
            return cachedHerdMembers;
        }

        lastCacheUpdate = currentTime;
        cachedHerdMembers = findHerdMembers(context);
        return cachedHerdMembers;
    }

    /**
     * Finds nearby entities of the same species.
     */
    private List<Entity> findHerdMembers(BehaviorContext context) {
        List<Entity> herdMembers = new ArrayList<>();
        Entity entity = context.getEntity();
        Level level = context.getLevel();

        // Get all entities within cohesion radius
        List<Entity> nearbyEntities = level.getEntities(
            entity,
            entity.getBoundingBox().inflate(config.getCohesionRadius())
        );

        String entityTypeId = entity.getType().toString();

        for (Entity nearby : nearbyEntities) {
            // Filter by same species
            if (!nearby.getType().toString().equals(entityTypeId)) {
                continue;
            }

            // Only include animals
            if (!(nearby instanceof Animal)) {
                continue;
            }

            // Exclude self
            if (nearby.equals(entity)) {
                continue;
            }

            herdMembers.add(nearby);
        }

        return herdMembers;
    }

    /**
     * Forces a cache refresh (call after major herd changes).
     */
    public void refreshHerdCache() {
        this.lastCacheUpdate = 0.0;
    }

    /**
     * Gets the current quorum movement behavior.
     */
    public QuorumMovement getQuorumMovement() {
        return quorumMovement;
    }

    /**
     * Gets the leader following behavior.
     */
    public LeaderFollowing getLeaderFollowing() {
        return leaderFollowing;
    }

    /**
     * Gets the herd cohesion behavior.
     */
    public HerdCohesion getHerdCohesion() {
        return herdCohesion;
    }

    /**
     * Gets the herd configuration.
     */
    public HerdConfig getConfig() {
        return config;
    }

    /**
     * Gets the unique identifier for this herd.
     */
    public UUID getHerdId() {
        return herdId;
    }

    /**
     * Sets the herd identifier (for coordinating across entities).
     */
    public void setHerdId(UUID herdId) {
        this.herdId = herdId;
    }

    /**
     * Gets the current leader being followed.
     */
    public Entity getCurrentLeader() {
        return leaderFollowing.getCurrentLeader();
    }

    /**
     * Checks if movement is currently allowed based on quorum.
     */
    public boolean isMovementAllowed() {
        return quorumMovement.isMovementAllowed();
    }

    /**
     * Gets statistics about the current herd state.
     */
    public HerdStats getHerdStats(BehaviorContext context) {
        List<Entity> herdMembers = getHerdMembers(context);

        HerdStats stats = new HerdStats();
        stats.totalMembers = herdMembers.size() + 1; // Include self
        stats.adults = 0;
        stats.babies = 0;
        stats.averageHealth = 0.0;
        stats.quorumRatio = quorumMovement.calculateQuorumRatio(context, herdMembers);

        for (Entity member : herdMembers) {
            if (member instanceof Animal animal) {
                if (animal.isBaby()) {
                    stats.babies++;
                } else {
                    stats.adults++;
                }
                stats.averageHealth += animal.getHealth() / animal.getMaxHealth();
            }
        }

        // Include self in counts
        if (context.getEntity() instanceof Animal self) {
            if (self.isBaby()) {
                stats.babies++;
            } else {
                stats.adults++;
            }
            stats.averageHealth += self.getHealth() / self.getMaxHealth();
        }

        if (stats.totalMembers > 0) {
            stats.averageHealth /= stats.totalMembers;
        }

        return stats;
    }

    /**
     * Resets all herd state (useful for testing or major changes).
     */
    public void reset() {
        quorumMovement.resetQuorum();
        // Note: LeaderFollowing doesn't expose a public reset, so we just clear the cache
        cachedHerdMembers = null;
        lastCacheUpdate = 0.0;
    }

    /**
     * Statistics about herd state.
     */
    public static class HerdStats {
        public int totalMembers;
        public int adults;
        public int babies;
        public double averageHealth;
        public double quorumRatio;

        public HerdStats() {
            this.totalMembers = 0;
            this.adults = 0;
            this.babies = 0;
            this.averageHealth = 0.0;
            this.quorumRatio = 0.0;
        }

        public boolean hasQuorum() {
            return quorumRatio >= 0.47; // Bison research threshold
        }

        public double getAdultRatio() {
            return totalMembers > 0 ? (double) adults / totalMembers : 0.0;
        }

        @Override
        public String toString() {
            return String.format("HerdStats{members=%d, adults=%d, babies=%d, avgHealth=%.2f, quorum=%.2f}",
                totalMembers, adults, babies, averageHealth, quorumRatio);
        }
    }

    /**
     * Creates a species-specific herd behavior with default config.
     */
    public static HerdBehavior forSpecies(String speciesId) {
        HerdConfig config = HerdConfig.forSpecies(speciesId);
        return new HerdBehavior(config);
    }

    /**
     * Creates a species-specific herd behavior with custom weight.
     */
    public static HerdBehavior forSpecies(String speciesId, double weight) {
        HerdConfig config = HerdConfig.forSpecies(speciesId);
        return new HerdBehavior(config, weight);
    }
}
