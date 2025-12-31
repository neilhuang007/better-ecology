package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Pack hierarchy behavior for wolves.
 * Implements dominance hierarchy with alpha leadership, social bonding,
 * and care for injured pack members.
 * <p>
 * Based on research into wolf pack social structure:
 * - Alpha pair leads the pack
 * - Clear dominance hierarchy
 * - Social bonding through proximity and howling
 * - Pack cares for injured members
 */
public class PackHierarchyBehavior extends SteeringBehavior {

    private final double cohesionDistance;
    private final double followStrength;
    private final double careStrength;

    private UUID packId;
    private HierarchyRank rank = HierarchyRank.UNKNOWN;
    private UUID alphaId;
    private int lastSocialCheckTick;

    public PackHierarchyBehavior(double cohesionDistance, double followStrength,
                                 double careStrength) {
        this.cohesionDistance = cohesionDistance;
        this.followStrength = followStrength;
        this.careStrength = careStrength;
    }

    public PackHierarchyBehavior() {
        this(24.0, 0.15, 0.12);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob entity = context.getEntity();
        if (!(entity instanceof Wolf wolf)) {
            return new Vec3d();
        }

        // Skip if tamed
        if (wolf.isTame()) {
            return new Vec3d();
        }

        // Initialize pack data
        if (packId == null) {
            packId = getPackId(wolf);
        }

        // Update hierarchy rank periodically
        updateHierarchy(wolf, context);

        // Calculate social force based on rank
        return calculateSocialForce(wolf, context);
    }

    /**
     * Calculates social force based on hierarchy.
     * Lower ranks follow higher ranks.
     */
    private Vec3d calculateSocialForce(Wolf wolf, BehaviorContext context) {
        List<Wolf> packMembers = getPackMembers(wolf, context);

        if (packMembers.isEmpty()) {
            return new Vec3d();
        }

        Vec3d wolfPos = context.getPosition();

        // Find highest ranking pack member
        Wolf leader = findLeader(wolf, packMembers);

        if (leader == null || leader.getId().equals(wolf.getId())) {
            // Alpha or no leader - maintain pack cohesion
            return calculatePackCohesion(wolf, packMembers, context);
        }

        // Follow the leader
        return calculateFollowLeader(wolf, leader, context);
    }

    /**
     * Calculates force to maintain pack cohesion (for alpha).
     */
    private Vec3d calculatePackCohesion(Wolf self, List<Wolf> packMembers,
                                       BehaviorContext context) {
        if (packMembers.isEmpty()) {
            return new Vec3d();
        }

        // Calculate center of pack
        Vec3d center = new Vec3d();
        int count = 0;

        Vec3d selfPos = context.getPosition();

        for (Wolf member : packMembers) {
            if (member.getId().equals(self.getId())) {
                continue;
            }

            Vec3d memberPos = new Vec3d(member.getX(), member.getY(), member.getZ());
            center.add(memberPos);
            count++;
        }

        if (count == 0) {
            return new Vec3d();
        }

        center.div(count);

        // Check if pack is too spread out
        double distance = selfPos.distanceTo(center);
        if (distance < cohesionDistance) {
            return new Vec3d(); // Pack is cohesive
        }

        // Move toward pack center
        Vec3d toCenter = Vec3d.sub(center, selfPos);
        toCenter.normalize();
        toCenter.mult(followStrength);

        Vec3d steer = Vec3d.sub(toCenter, context.getVelocity());
        steer.limit(maxForce());

        return steer;
    }

    /**
     * Calculates force to follow the pack leader.
     */
    private Vec3d calculateFollowLeader(Wolf self, Wolf leader, BehaviorContext context) {
        Vec3d selfPos = context.getPosition();
        Vec3d leaderPos = new Vec3d(leader.getX(), leader.getY(), leader.getZ());

        double distance = selfPos.distanceTo(leaderPos);

        // Maintain appropriate following distance
        double desiredDistance = 4.0;

        if (distance < desiredDistance * 0.8) {
            // Too close - separate slightly
            Vec3d away = Vec3d.sub(selfPos, leaderPos);
            away.normalize();
            away.mult(followStrength * 0.5);

            Vec3d steer = Vec3d.sub(away, context.getVelocity());
            steer.limit(maxForce() * 0.5);

            return steer;
        }

        if (distance > cohesionDistance) {
            // Too far - move closer
            Vec3d toLeader = Vec3d.sub(leaderPos, selfPos);
            toLeader.normalize();
            toLeader.mult(followStrength);

            Vec3d steer = Vec3d.sub(toLeader, context.getVelocity());
            steer.limit(maxForce());

            return steer;
        }

        return new Vec3d(); // Good following distance
    }

    /**
     * Updates hierarchy rank based on pack composition.
     */
    private void updateHierarchy(Wolf wolf, BehaviorContext context) {
        int currentTick = wolf.tickCount;

        // Check hierarchy every 5 seconds
        if (currentTick - lastSocialCheckTick < 100) {
            return;
        }

        lastSocialCheckTick = currentTick;

        List<Wolf> packMembers = getPackMembers(wolf, context);

        if (packMembers.isEmpty()) {
            rank = HierarchyRank.ALPHA;
            return;
        }

        // Calculate strength for all pack members
        List<WolfStrength> strengths = new ArrayList<>();

        double selfStrength = calculateStrength(wolf);
        strengths.add(new WolfStrength(wolf, selfStrength));

        for (Wolf member : packMembers) {
            if (member.getId().equals(wolf.getId())) {
                continue;
            }
            double strength = calculateStrength(member);
            strengths.add(new WolfStrength(member, strength));
        }

        // Sort by strength (descending)
        strengths.sort(Comparator.comparingDouble(WolfStrength::strength).reversed());

        // Determine rank based on position in sorted list
        int selfIndex = 0;
        for (int i = 0; i < strengths.size(); i++) {
            if (strengths.get(i).wolf().getId().equals(wolf.getId())) {
                selfIndex = i;
                break;
            }
        }

        // Map index to rank
        if (selfIndex == 0) {
            rank = HierarchyRank.ALPHA;
            alphaId = wolf.getId();
        } else if (selfIndex == 1) {
            rank = HierarchyRank.BETA;
        } else if (selfIndex < strengths.size() * 0.5) {
            rank = HierarchyRank.MID;
        } else {
            rank = HierarchyRank.OMEGA;
        }

        // Store alpha ID
        if (!strengths.isEmpty()) {
            alphaId = strengths.get(0).wolf().getId();
        }
    }

    /**
     * Calculates wolf strength for hierarchy determination.
     * Considers health, age, and condition.
     */
    private double calculateStrength(Wolf wolf) {
        double health = wolf.getHealth();
        double maxHealth = wolf.getMaxHealth();
        double healthFactor = health / maxHealth;

        double ageFactor = wolf.isBaby() ? 0.5 : 1.0;

        // Slightly random to prevent ties
        double randomFactor = wolf.getRandom().nextDouble() * 0.1;

        return healthFactor * ageFactor + randomFactor;
    }

    /**
     * Finds the pack leader (highest ranking member).
     */
    private Wolf findLeader(Wolf self, List<Wolf> packMembers) {
        if (packMembers.isEmpty()) {
            return null;
        }

        Wolf leader = self;
        double maxStrength = calculateStrength(self);

        for (Wolf member : packMembers) {
            if (member.getId().equals(self.getId())) {
                continue;
            }

            double strength = calculateStrength(member);
            if (strength > maxStrength) {
                maxStrength = strength;
                leader = member;
            }
        }

        return leader;
    }

    /**
     * Gets all pack members within range.
     */
    private List<Wolf> getPackMembers(Wolf wolf, BehaviorContext context) {
        List<Wolf> pack = new ArrayList<>();

        for (Entity entity : context.getNeighbors()) {
            if (!(entity instanceof Wolf otherWolf)) {
                continue;
            }

            if (otherWolf.isTame()) {
                continue;
            }

            if (entity.equals(wolf)) {
                continue;
            }

            UUID otherPackId = getPackId(otherWolf);
            if (packId != null && packId.equals(otherPackId)) {
                pack.add(otherWolf);
            }
        }

        return pack;
    }

    /**
     * Gets or generates pack ID from NBT.
     */
    private UUID getPackId(Wolf wolf) {
        if (packId != null) {
            return packId;
        }

        var persistentData = wolf.getPersistentData();
        if (persistentData.contains("WolfPackId")) {
            packId = persistentData.getUUID("WolfPackId");
            return packId;
        }

        packId = UUID.randomUUID();
        persistentData.putUUID("WolfPackId", packId);
        return packId;
    }

    /**
     * Attempts to howl for social bonding.
     */
    public void tryBondingHowl(Wolf wolf) {
        Level level = wolf.level();

        if (!level.isClientSide && rank == HierarchyRank.ALPHA) {
            // Alpha initiates group howl
            level.playSound(null, wolf.blockPosition(), SoundEvents.WOLF_HOWL,
                SoundSource.NEUTRAL, 0.8f, 1.0f);

            // Other pack members respond (in full implementation)
        }
    }

    public HierarchyRank getRank() {
        return rank;
    }

    public UUID getAlphaId() {
        return alphaId;
    }

    public boolean isAlpha() {
        return rank == HierarchyRank.ALPHA;
    }

    public UUID getPackId() {
        return packId;
    }

    public void setPackId(UUID packId) {
        this.packId = packId;
    }

    /**
     * Hierarchy ranks in wolf pack.
     */
    public enum HierarchyRank {
        ALPHA,  // Pack leader
        BETA,   // Second in command
        MID,    // Regular pack members
        OMEGA,  // Lowest ranking
        UNKNOWN // Not yet determined
    }

    /**
     * Record for tracking wolf strength.
     */
    private record WolfStrength(Wolf wolf, double strength) {}
}
