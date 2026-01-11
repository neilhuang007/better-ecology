package me.javavirtualenv.behavior.herd;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import java.util.*;

/**
 * Leader following behavior based on age, dominance, and experience.
 * Supports shared leadership where multiple individuals can lead depending on context.
 * Research shows older, dominant individuals in ungulate herds typically lead movements.
 */
public class LeaderFollowing extends SteeringBehavior {

    private static final String DOMINANCE_KEY = "dominance";
    private static final String LAST_DECAY_KEY = "lastDecayTick";
    private static final String NBT_DOMINANCE = "dominance";
    private static final double LEADER_UPDATE_INTERVAL = 2.0; // seconds
    private static final int DECAY_INTERVAL_TICKS = 1200; // 1 minute between decay checks
    private static final double DOMINANCE_DECAY_RATE = 0.01; // Decay per check

    private final HerdConfig config;
    private Entity currentLeader;
    private double lastLeaderUpdate = 0.0;

    public LeaderFollowing(HerdConfig config) {
        this.config = config;
    }

    public LeaderFollowing(HerdConfig config, double weight) {
        this.config = config;
        this.weight = weight;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();

        // Get neighbors from context (set by SteeringBehaviorGoal)
        List<Entity> neighbors = context.getNeighbors();

        // Update leader periodically using available neighbors
        double currentTime = context.getEntity().level().getGameTime() / 20.0;
        if (currentTime - lastLeaderUpdate > LEADER_UPDATE_INTERVAL) {
            // Use neighbors for leader selection
            if (neighbors != null && !neighbors.isEmpty()) {
                updateLeader(context, neighbors);
            } else {
                // No neighbors available, check if current leader is still valid
                if (currentLeader != null && (!currentLeader.isAlive() ||
                    position.distanceTo(new Vec3d(currentLeader.getX(), currentLeader.getY(), currentLeader.getZ())) > config.getLeaderFollowRadius())) {
                    currentLeader = null;
                }
            }
            lastLeaderUpdate = currentTime;
        }

        // If no leader identified, return zero force
        if (currentLeader == null || !currentLeader.isAlive()) {
            currentLeader = null;
            return new Vec3d();
        }

        // Check if leader is within follow radius
        Vec3d leaderPos = new Vec3d(
            currentLeader.getX(),
            currentLeader.getY(),
            currentLeader.getZ()
        );

        double distance = position.distanceTo(leaderPos);
        if (distance > config.getLeaderFollowRadius()) {
            currentLeader = null;
            return new Vec3d();
        }

        // Calculate steering toward leader
        Vec3d steer = seek(position, velocity, leaderPos, config.getMaxSpeed());
        steer.limit(config.getMaxForce());
        return steer;
    }

    /**
     * Identifies and selects leaders from the herd based on age and dominance.
     * Implements shared leadership - multiple individuals can lead.
     */
    public void updateLeader(BehaviorContext context, List<Entity> herdMembers) {
        if (herdMembers == null || herdMembers.isEmpty()) {
            currentLeader = null;
            return;
        }

        // Score all potential leaders
        List<LeaderCandidate> candidates = new ArrayList<>();

        for (Entity member : herdMembers) {
            if (member.equals(context.getEntity())) {
                continue;
            }

            if (!(member instanceof Animal)) {
                continue;
            }

            double score = calculateLeadershipScore(member, context);
            if (score > 0) {
                candidates.add(new LeaderCandidate(member, score));
            }
        }

        // Sort by leadership score (highest first)
        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        // Select from top leaders (shared leadership)
        if (candidates.isEmpty()) {
            currentLeader = null;
            return;
        }

        int leaderCount = Math.min(config.getMaxLeaders(), candidates.size());
        List<Entity> topLeaders = new ArrayList<>();
        for (int i = 0; i < leaderCount; i++) {
            topLeaders.add(candidates.get(i).entity);
        }

        // Choose closest top leader as current target
        Entity closestLeader = null;
        double closestDistance = Double.MAX_VALUE;
        Vec3d position = context.getPosition();

        for (Entity leader : topLeaders) {
            Vec3d leaderPos = new Vec3d(leader.getX(), leader.getY(), leader.getZ());
            double distance = position.distanceTo(leaderPos);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestLeader = leader;
            }
        }

        currentLeader = closestLeader;
    }

    /**
     * Calculates leadership score based on age, dominance, and experience.
     * Higher score indicates more likely to lead.
     */
    public double calculateLeadershipScore(Entity entity, BehaviorContext context) {
        if (!(entity instanceof Animal animal)) {
            return 0.0;
        }

        double score = 0.0;
        Vec3d position = context.getPosition();
        Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        double distance = position.distanceTo(entityPos);

        // Only consider entities within reasonable range
        if (distance > config.getLeaderFollowRadius()) {
            return 0.0;
        }

        // Age bonus - older animals are more experienced
        double ageBonus = calculateAgeBonus(animal);
        score += ageBonus * config.getLeadershipAgeBonus();

        // Dominance bonus - from stored NBT data
        double dominanceBonus = getDominance(animal);
        score += dominanceBonus * config.getLeadershipDominanceBonus();

        // Movement confidence bonus - animals already moving are more confident
        Vec3d entityVelocity = new Vec3d(
            entity.getDeltaMovement().x,
            entity.getDeltaMovement().y,
            entity.getDeltaMovement().z
        );
        double speed = entityVelocity.magnitude();
        if (speed > 0.05) {
            score += 0.2;
        }

        // Health bonus - healthier animals more likely to lead
        double healthPercent = animal.getHealth() / animal.getMaxHealth();
        score += healthPercent * 0.1;

        return score;
    }

    /**
     * Calculates age-based bonus for leadership.
     * Adults with more experience get higher bonus.
     * Uses config-specified max age for realistic species behavior.
     */
    private double calculateAgeBonus(Animal animal) {
        if (animal.isBaby()) {
            return 0.0; // Babies never lead
        }

        if (animal instanceof AgeableMob ageable) {
            int age = ageable.getAge();
            // Age is negative for babies, 0 for just matured, positive for older adults
            // Cap age bonus at configured max age for realistic behavior
            int maxAge = config.getMaxAgeForLeadership();
            return Math.min(1.0, Math.max(0.3, (double) age / maxAge));
        }

        return 0.5; // Default adult bonus
    }

    /**
     * Gets dominance value from entity's persistent NBT storage.
     * Returns 0.0-1.0 range.
     */
    public double getDominance(Animal animal) {
        EcologyComponent component = getEcologyComponent(animal);
        if (component == null) {
            return 0.5; // Default value if component unavailable
        }

        CompoundTag dominanceTag = component.getHandleTag(DOMINANCE_KEY);
        if (!dominanceTag.contains(NBT_DOMINANCE)) {
            double initialDominance = animal.getRandom().nextDouble();
            dominanceTag.putDouble(NBT_DOMINANCE, initialDominance);
            return initialDominance;
        }

        return dominanceTag.getDouble(NBT_DOMINANCE);
    }

    /**
     * Sets dominance value for an animal with NBT persistence.
     */
    public void setDominance(Animal animal, double dominance) {
        EcologyComponent component = getEcologyComponent(animal);
        if (component == null) {
            return;
        }

        double clampedDominance = Math.max(0.0, Math.min(1.0, dominance));
        CompoundTag dominanceTag = component.getHandleTag(DOMINANCE_KEY);
        dominanceTag.putDouble(NBT_DOMINANCE, clampedDominance);
    }

    /**
     * Increases dominance slightly (positive reinforcement).
     */
    public void increaseDominance(Animal animal, double amount) {
        double current = getDominance(animal);
        setDominance(animal, Math.min(1.0, current + amount));
    }

    /**
     * Decreases dominance slightly (negative reinforcement).
     */
    public void decreaseDominance(Animal animal, double amount) {
        double current = getDominance(animal);
        setDominance(animal, Math.max(0.0, current - amount));
    }

    /**
     * Applies periodic dominance decay to maintain dynamic hierarchy.
     * Should be called periodically to prevent dominance from becoming static.
     */
    public void applyDominanceDecay(Animal animal) {
        EcologyComponent component = getEcologyComponent(animal);
        if (component == null) {
            return;
        }

        CompoundTag dominanceTag = component.getHandleTag(DOMINANCE_KEY);
        int currentTick = animal.tickCount;
        int lastDecayTick = dominanceTag.getInt(LAST_DECAY_KEY);

        if (currentTick - lastDecayTick >= DECAY_INTERVAL_TICKS) {
            double currentDominance = getDominance(animal);
            double decayedDominance = Math.max(0.1, currentDominance - DOMINANCE_DECAY_RATE);
            dominanceTag.putDouble(NBT_DOMINANCE, decayedDominance);
            dominanceTag.putInt(LAST_DECAY_KEY, currentTick);
        }
    }

    /**
     * Gets EcologyComponent from an animal entity.
     */
    private EcologyComponent getEcologyComponent(Animal animal) {
        if (!(animal instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Gets the current leader being followed.
     */
    public Entity getCurrentLeader() {
        return currentLeader;
    }

    /**
     * Gets all potential leaders from the herd, sorted by leadership score.
     */
    public List<Entity> getPotentialLeaders(BehaviorContext context, List<Entity> herdMembers) {
        if (herdMembers == null || herdMembers.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaderCandidate> candidates = new ArrayList<>();

        for (Entity member : herdMembers) {
            if (member.equals(context.getEntity())) {
                continue;
            }

            if (!(member instanceof Animal)) {
                continue;
            }

            double score = calculateLeadershipScore(member, context);
            if (score > 0) {
                candidates.add(new LeaderCandidate(member, score));
            }
        }

        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        List<Entity> leaders = new ArrayList<>();
        for (LeaderCandidate candidate : candidates) {
            leaders.add(candidate.entity);
        }

        return leaders;
    }

    public HerdConfig getConfig() {
        return config;
    }

    /**
     * Internal class for tracking leader candidates.
     */
    private static class LeaderCandidate {
        final Entity entity;
        final double score;

        LeaderCandidate(Entity entity, double score) {
            this.entity = entity;
            this.score = score;
        }
    }
}
