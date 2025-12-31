package me.javavirtualenv.behavior.social;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.social.InteractionHandle;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * AI Goal for cross-species interactions.
 *
 * Implements:
 * - Seeking symbiotic partners
 * - Avoiding competitors
 * - Joining mixed herds
 * - Social behaviors (play, altruism, teaching)
 */
public class InteractionGoal extends Goal {
    private final PathfinderMob mob;
    private final InteractionConfig config;

    private Mob targetEntity;
    private InteractionType currentInteraction;
    private int cooldownTicks;

    public InteractionGoal(PathfinderMob mob, InteractionConfig config) {
        this.mob = mob;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        if (!mob.isAlive()) {
            return false;
        }

        // Check for possible interactions
        targetEntity = findInteractionTarget();
        return targetEntity != null && targetEntity.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (targetEntity == null || !targetEntity.isAlive()) {
            return false;
        }

        double distance = mob.distanceToSqr(targetEntity);
        if (distance > config.maxInteractionDistance * config.maxInteractionDistance) {
            return false;
        }

        return true;
    }

    @Override
    public void stop() {
        targetEntity = null;
        currentInteraction = null;
        cooldownTicks = config.interactionCooldown;
    }

    @Override
    public void tick() {
        if (targetEntity == null || !targetEntity.isAlive()) {
            return;
        }

        double distance = mob.distanceTo(targetEntity);

        switch (currentInteraction) {
            case SEEK_SYMBIOTIC_PARTNER -> handleSymbioticSeek(distance);
            case AVOID_COMPETITOR -> handleCompetitorAvoidance(distance);
            case JOIN_MIXED_HERD -> handleMixedHerding(distance);
            case PLAY -> handlePlayBehavior(distance);
            case ALTRUISM -> handleAltruism(distance);
            case TEACHING -> handleTeaching(distance);
        }
    }

    private Mob findInteractionTarget() {
        List<Mob> nearbyMobs = SpatialIndex.getNearbyMobs(mob, config.detectionRadius);

        for (Mob other : nearbyMobs) {
            if (other == mob || !other.isAlive()) {
                continue;
            }

            InteractionType type = determineInteractionType(other);
            if (type != null) {
                currentInteraction = type;
                return other;
            }
        }

        return null;
    }

    private InteractionType determineInteractionType(Mob other) {
        String mobType = getEntityTypeString(mob);
        String otherType = getEntityTypeString(other);

        // Check for symbiotic relationships
        if (isSymbioticPartner(mobType, otherType)) {
            return InteractionType.SEEK_SYMBIOTIC_PARTNER;
        }

        // Check for competitors
        if (isCompetitor(mobType, otherType)) {
            return InteractionType.AVOID_COMPETITOR;
        }

        // Check for mixed herding
        if (canHerdTogether(mobType, otherType)) {
            double herdDistance = mob.distanceTo(other);
            if (herdDistance > config.herdCohesionRadius) {
                return InteractionType.JOIN_MIXED_HERD;
            }
        }

        // Check for play behavior (young animals)
        if (config.canPlay && isBaby(mob) && isBaby(other)) {
            return InteractionType.PLAY;
        }

        // Check for altruism opportunities
        if (config.canBeAltruistic && isAltruismOpportunity(mob, other)) {
            return InteractionType.ALTRUISM;
        }

        // Check for teaching opportunities
        if (config.canTeach && isTeachingOpportunity(mob, other)) {
            return InteractionType.TEACHING;
        }

        return null;
    }

    private boolean isSymbioticPartner(String mobType, String otherType) {
        // Bees + Flowers (though flowers aren't mobs)
        // Allay + Villagers
        if (mobType.equals("minecraft:allay") && otherType.equals("minecraft:villager")) {
            return true;
        }
        if (mobType.equals("minecraft:villager") && otherType.equals("minecraft:allay")) {
            return true;
        }

        return false;
    }

    private boolean isCompetitor(String mobType, String otherType) {
        // Wolves vs Foxes
        if ((mobType.equals("minecraft:wolf") && otherType.equals("minecraft:fox")) ||
            (mobType.equals("minecraft:fox") && otherType.equals("minecraft:wolf"))) {
            return true;
        }

        // Cats vs Dogs
        if ((mobType.equals("minecraft:cat") && otherType.equals("minecraft:wolf")) ||
            (mobType.equals("minecraft:wolf") && otherType.equals("minecraft:cat"))) {
            return true;
        }

        return false;
    }

    private boolean canHerdTogether(String mobType, String otherType) {
        // Horses + Donkeys
        if ((mobType.equals("minecraft:horse") && otherType.equals("minecraft:donkey")) ||
            (mobType.equals("minecraft:donkey") && otherType.equals("minecraft:horse"))) {
            return true;
        }

        // Cows + Sheep
        if ((mobType.equals("minecraft:cow") || mobType.equals("minecraft:mooshroom")) &&
            (otherType.equals("minecraft:sheep"))) {
            return true;
        }
        if (mobType.equals("minecraft:sheep") &&
            (otherType.equals("minecraft:cow") || otherType.equals("minecraft:mooshroom"))) {
            return true;
        }

        return false;
    }

    private boolean isAltruismOpportunity(Mob actor, Mob recipient) {
        // Strong individuals protect weak ones
        if (isBaby(recipient)) {
            // Check if actor is healthy and adult
            return !isBaby(actor) && actor.getHealth() > actor.getMaxHealth() * 0.7;
        }

        // Protect injured herd members
        if (recipient.getHealth() < recipient.getMaxHealth() * 0.5) {
            return actor.getHealth() > actor.getMaxHealth() * 0.8;
        }

        return false;
    }

    private boolean isTeachingOpportunity(Mob teacher, Mob learner) {
        // Adults teach babies
        return !isBaby(teacher) && isBaby(learner) &&
               getEntityTypeString(teacher).equals(getEntityTypeString(learner));
    }

    private void handleSymbioticSeek(double distance) {
        if (distance > config.interactionDistance) {
            mob.getNavigation().moveTo(targetEntity, 1.0);
        }
    }

    private void handleCompetitorAvoidance(double distance) {
        if (distance < config.competitorAvoidDistance) {
            Vec3 fleeDirection = getFleeDirection();
            if (fleeDirection != null) {
                double speed = config.fleeSpeedMultiplier;
                mob.getNavigation().moveTo(
                    mob.getX() + fleeDirection.x * 16,
                    mob.getY() + fleeDirection.y * 8,
                    mob.getZ() + fleeDirection.z * 16,
                    speed
                );
            }
        }
    }

    private void handleMixedHerding(double distance) {
        if (distance > config.herdCohesionRadius) {
            mob.getNavigation().moveTo(targetEntity, 1.0);
        }
    }

    private void handlePlayBehavior(double distance) {
        if (distance > config.playDistance) {
            mob.getNavigation().moveTo(targetEntity, 1.2);
        } else if (distance < 2) {
            // Play interaction - jump around, etc.
            if (mob.getRandom().nextFloat() < 0.1) {
                mob.getJumpControl().jump();
            }
        }
    }

    private void handleAltruism(double distance) {
        if (distance > config.protectionRadius) {
            mob.getNavigation().moveTo(targetEntity, 1.3);
        } else {
            // Stay close and protect
            mob.getLookControl().setLookAt(targetEntity);
        }
    }

    private void handleTeaching(double distance) {
        if (distance > config.teachingDistance) {
            mob.getNavigation().moveTo(targetEntity, 1.0);
        } else {
            // Teaching behavior - demonstrate actions
            mob.getLookControl().setLookAt(targetEntity);
        }
    }

    private Vec3 getFleeDirection() {
        if (targetEntity == null) {
            return null;
        }

        Vec3 direction = mob.position().subtract(targetEntity.position()).normalize();
        return direction;
    }

    private boolean isBaby(Mob mob) {
        return mob.isBaby();
    }

    private String getEntityTypeString(Mob mob) {
        try {
            return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(mob.getType()).toString();
        } catch (Exception e) {
            return "";
        }
    }

    public enum InteractionType {
        SEEK_SYMBIOTIC_PARTNER,
        AVOID_COMPETITOR,
        JOIN_MIXED_HERD,
        PLAY,
        ALTRUISM,
        TEACHING
    }

    public static class InteractionConfig {
        public double detectionRadius = 32;
        public double interactionDistance = 4;
        public double maxInteractionDistance = 64;
        public int interactionCooldown = 200;

        public double competitorAvoidDistance = 16;
        public double fleeSpeedMultiplier = 1.5;

        public double herdCohesionRadius = 12;

        public double playDistance = 6;

        public double protectionRadius = 8;
        public double teachingDistance = 6;

        public boolean canPlay = true;
        public boolean canBeAltruistic = true;
        public boolean canTeach = true;
    }
}
