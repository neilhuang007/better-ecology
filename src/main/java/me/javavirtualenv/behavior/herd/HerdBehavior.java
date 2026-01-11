package me.javavirtualenv.behavior.herd;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final double HERD_SYNC_DISTANCE = 32.0; // blocks
    private boolean hasSyncedHerdId = false;

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
        // Synchronize herd ID with nearby entities of same species
        synchronizeHerdId(context);

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
     * Synchronizes herd ID with nearby entities of the same species.
     * This ensures that animals in proximity share the same herd ID,
     * allowing coordinated herd behavior without requiring persistent storage.
     * <p>
     * Uses a distributed consensus approach:
     * - If unsynced, look for nearby animals with a herd ID
     * - Join the largest nearby herd (most animals sharing same ID)
     * - If no nearby herds, keep current ID (may become new herd leader)
     * - Periodically re-sync to handle herd splitting/merging
     */
    private void synchronizeHerdId(BehaviorContext context) {
        Entity entity = context.getEntity();
        Level level = context.getLevel();

        // Only sync occasionally to avoid performance issues
        if (hasSyncedHerdId && level.getGameTime() % 40 != 0) {
            return;
        }

        // Get all nearby same-species animals
        List<Entity> nearbyAnimals = level.getEntities(
            entity,
            entity.getBoundingBox().inflate(HERD_SYNC_DISTANCE)
        );

        String entityTypeId = entity.getType().toString();
        Set<UUID> nearbyHerdIds = new HashSet<>();
        List<Entity> herdAnimals = new ArrayList<>();

        // Collect herd IDs from nearby same-species animals
        for (Entity nearby : nearbyAnimals) {
            if (!nearby.getType().toString().equals(entityTypeId)) {
                continue;
            }
            if (!(nearby instanceof Animal)) {
                continue;
            }
            if (nearby.equals(entity)) {
                continue;
            }

            herdAnimals.add(nearby);
        }

        // If no nearby animals, keep current ID and mark as synced
        if (herdAnimals.isEmpty()) {
            hasSyncedHerdId = true;
            return;
        }

        // For now, use a simple approach: adopt the herd ID of the nearest animal
        // This allows natural herd formation through proximity
        Entity nearestAnimal = findNearestAnimal(entity, herdAnimals);

        if (nearestAnimal != null) {
            // Calculate nearest animal's herd ID using a deterministic hash
            // In a full implementation, this would access the animal's herd behavior directly
            UUID nearestHerdId = generateHerdIdForEntity(nearestAnimal);

            // If we haven't synced yet, join the nearest herd
            if (!hasSyncedHerdId) {
                this.herdId = nearestHerdId;
                hasSyncedHerdId = true;
            }
        }
    }

    /**
     * Finds the nearest animal from a list of candidates.
     */
    private Entity findNearestAnimal(Entity fromEntity, List<Entity> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity candidate : candidates) {
            double dx = fromEntity.getX() - candidate.getX();
            double dy = fromEntity.getY() - candidate.getY();
            double dz = fromEntity.getZ() - candidate.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    /**
     * Generates a deterministic herd ID for an entity.
     * This is a simplified approach that creates a consistent herd ID
     * based on the entity's UUID and position.
     * In a full implementation, this would access the entity's actual herd behavior.
     */
    private UUID generateHerdIdForEntity(Entity entity) {
        // Create a deterministic herd ID based on entity position and type
        // Animals close together will get the same herd ID
        int herdRegionX = (int) Math.floor(entity.getX() / HERD_SYNC_DISTANCE);
        int herdRegionZ = (int) Math.floor(entity.getZ() / HERD_SYNC_DISTANCE);

        long mostSigBits = entity.getType().toString().hashCode();
        long leastSigBits = ((long) herdRegionX << 32) | (herdRegionZ & 0xFFFFFFFFL);

        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Forces a cache refresh (call after major herd changes).
     */
    public void refreshHerdCache() {
        this.lastCacheUpdate = 0.0;
        this.hasSyncedHerdId = false;
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
