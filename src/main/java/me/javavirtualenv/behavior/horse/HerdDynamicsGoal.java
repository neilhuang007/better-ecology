package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * AI Goal for horse herd dynamics behavior.
 * <p>
 * Wild horses form bands with a lead stallion that protects the herd.
 * This behavior implements:
 * <ul>
 *   <li>Lead stallion protection behavior</li>
 *   <li>Herd member following behavior</li>
 *   <li>Threat detection and alarm calls</li>
 *   <li>Herd cohesion maintenance</li>
 * </ul>
 * <p>
 * Only wild (untamed) horses participate in herd behavior.
 */
public class HerdDynamicsGoal extends Goal {

    // Configuration constants
    private static final String FLEEING_KEY = "fleeing";

    private static final double HERD_RADIUS = 32.0; // Search radius for herd members
    private static final double COHESION_DISTANCE = 12.0; // Distance to maintain cohesion
    private static final double MIN_DISTANCE_TO_LEADER = 3.0; // Minimum distance from leader
    private static final double THREAT_DETECTION_RANGE = 24.0; // Range to detect threats
    private static final double PROTECTION_RANGE = 16.0; // Range for lead stallion protection
    private static final double ALARM_CALL_RANGE = 32.0; // Range for alarm calls
    private static final double LEAD_MOVE_CHANCE = 0.02; // Chance for leader to initiate movement
    private static final int HERD_CHECK_INTERVAL = 100; // Ticks between herd checks
    private static final int MOVE_SPEED_LEADER = 12; // Speed for leading (1.2)
    private static final int MOVE_SPEED_FOLLOW = 10; // Speed for following (1.0)
    private static final int MOVE_SPEED_AWAY = 6; // Speed for moving away (0.6)

    // Instance fields
    private final AbstractHorse horse;
    private final EntityType<?> horseType;

    private AbstractHorse leadStallion;
    private List<AbstractHorse> herdMembers;
    private int checkIntervalTicks;
    private boolean isLeadStallion;
    private boolean isWild;

    // Debug info
    private String lastDebugMessage = "";
    private boolean hadHerdLastCheck = false;

    public HerdDynamicsGoal(AbstractHorse horse) {
        this.horse = horse;
        this.horseType = horse.getType();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (horse.level().isClientSide) {
            return false;
        }

        // Must be alive
        if (!horse.isAlive()) {
            return false;
        }

        // Only wild horses form herds
        if (!isWildHorse()) {
            if (hadHerdLastCheck) {
                debug("horse tamed, leaving herd");
                hadHerdLastCheck = false;
            }
            return false;
        }

        // Update herd information periodically
        if (checkIntervalTicks <= 0) {
            updateHerdInformation();
            checkIntervalTicks = HERD_CHECK_INTERVAL;
        } else {
            checkIntervalTicks--;
        }

        // Only herd behavior if we have herd members nearby
        boolean hasHerd = herdMembers != null && !herdMembers.isEmpty();

        if (!hasHerd && hadHerdLastCheck) {
            debug("no herd members nearby");
            hadHerdLastCheck = false;
        } else if (hasHerd && !hadHerdLastCheck) {
            debug("herd detected, members=" + herdMembers.size());
            hadHerdLastCheck = true;
        }

        return hasHerd;
    }

    @Override
    public boolean canContinueToUse() {
        if (!horse.isAlive()) {
            return false;
        }

        // Check if herd is still nearby
        if (herdMembers == null || herdMembers.isEmpty()) {
            return false;
        }

        // If tamed, stop herd behavior
        if (horse.isTamed()) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        updateHerdInformation();
        String role = isLeadStallion ? "lead stallion" : "herd member";
        String leaderId = leadStallion != null ? "#" + leadStallion.getId() : "none";
        debug("STARTING: role=" + role + ", leader=" + leaderId);
    }

    @Override
    public void stop() {
        leadStallion = null;
        herdMembers = null;
        debug("herd behavior stopped");
    }

    @Override
    public void tick() {
        if (isLeadStallion) {
            behaveAsLeadStallion();
        } else {
            behaveAsHerdMember();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Check if this horse is wild (untamed).
     */
    private boolean isWildHorse() {
        if (horse.isTamed()) {
            isWild = false;
            return false;
        }

        isWild = horseType == EntityType.HORSE ||
                 horseType == EntityType.DONKEY ||
                 horseType == EntityType.MULE;

        return isWild;
    }

    /**
     * Update herd information including members and leader.
     */
    private void updateHerdInformation() {
        // Find all nearby horses of same type
        List<AbstractHorse> nearbyHorses = horse.level().getEntitiesOfClass(
            AbstractHorse.class,
            horse.getBoundingBox().inflate(HERD_RADIUS)
        );

        // Filter for same type and wild
        herdMembers = nearbyHorses.stream()
            .filter(h -> h.getType() == horseType)
            .filter(h -> !h.isTamed())
            .filter(h -> h.isAlive())
            .filter(h -> h != horse)
            .toList();

        // Find the lead stallion (adult with highest health)
        if (herdMembers.isEmpty()) {
            // This horse is alone
            isLeadStallion = !horse.isBaby();
            leadStallion = isLeadStallion ? horse : null;
        } else {
            // Include self in potential leaders
            List<AbstractHorse> allHorses = new ArrayList<>(herdMembers);
            allHorses.add(horse);

            // Find adult with highest health (simplified stallion detection)
            leadStallion = allHorses.stream()
                .filter(h -> !h.isBaby())
                .max((h1, h2) -> Double.compare(h1.getHealth(), h2.getHealth()))
                .orElse(null);

            isLeadStallion = (leadStallion == horse);
        }
    }

    /**
     * Behavior for lead stallion - protects and leads the herd.
     */
    private void behaveAsLeadStallion() {
        // Check for threats
        Entity threat = findThreat();

        if (threat != null) {
            // Warn the herd
            warnHerdOfThreat(threat);

            // Move towards threat to protect herd
            double distance = horse.distanceTo(threat);
            if (distance > PROTECTION_RANGE) {
                moveToEntity(threat, MOVE_SPEED_LEADER / 10.0);
            }
        } else {
            // Lead the herd to grazing
            if (horse.getRandom().nextFloat() < LEAD_MOVE_CHANCE) {
                Vec3 targetPos = findGrazingTarget();
                if (targetPos != null) {
                    PathNavigation navigation = horse.getNavigation();
                    Path path = navigation.createPath(
                        net.minecraft.core.BlockPos.containing(targetPos),
                        0
                    );

                    if (path != null && path.canReach()) {
                        navigation.moveTo(
                            targetPos.x,
                            targetPos.y,
                            targetPos.z,
                            0.8
                        );
                    }
                }
            }
        }

        // Check herd cohesion
        checkHerdCohesion();
    }

    /**
     * Behavior for herd member - follows the leader.
     */
    private void behaveAsHerdMember() {
        if (leadStallion == null || !leadStallion.isAlive()) {
            // No leader, find new one
            updateHerdInformation();
            return;
        }

        double distanceToLeader = horse.distanceTo(leadStallion);

        // Stay close to leader
        if (distanceToLeader > COHESION_DISTANCE) {
            moveToEntity(leadStallion, MOVE_SPEED_FOLLOW / 10.0);
        } else if (distanceToLeader < MIN_DISTANCE_TO_LEADER) {
            // Too close, move away slightly
            Vec3 awayFromLeader = horse.position().subtract(
                leadStallion.position()
            ).normalize().scale(3.0);

            Vec3 targetPos = horse.position().add(awayFromLeader);

            PathNavigation navigation = horse.getNavigation();
            Path path = navigation.createPath(
                net.minecraft.core.BlockPos.containing(targetPos),
                0
            );

            if (path != null && path.canReach()) {
                navigation.moveTo(
                    targetPos.x,
                    targetPos.y,
                    targetPos.z,
                    MOVE_SPEED_AWAY / 10.0
                );
            }
        }

        // Look at leader occasionally
        if (horse.getRandom().nextFloat() < 0.1) {
            horse.getLookControl().setLookAt(leadStallion);
        }
    }

    /**
     * Find a threat to the herd.
     */
    private Entity findThreat() {
        List<LivingEntity> threats = horse.level().getEntitiesOfClass(
            LivingEntity.class,
            horse.getBoundingBox().inflate(THREAT_DETECTION_RANGE),
            this::isThreat
        );

        return threats.isEmpty() ? null : threats.get(0);
    }

    /**
     * Check if an entity is a threat.
     */
    private boolean isThreat(Entity entity) {
        if (entity == horse) {
            return false;
        }

        EntityType<?> type = entity.getType();

        // Hostile mobs
        if (type == EntityType.WOLF ||
            type == EntityType.POLAR_BEAR ||
            type == EntityType.ZOMBIE ||
            type == EntityType.DROWNED ||
            type == EntityType.HUSK ||
            type == EntityType.ZOMBIFIED_PIGLIN ||
            type == EntityType.VINDICATOR ||
            type == EntityType.EVOKER ||
            type == EntityType.RAVAGER) {
            return true;
        }

        // Players that are attacking
        if (type == EntityType.PLAYER) {
            return horse.getLastHurtByMob() == entity;
        }

        return false;
    }

    /**
     * Warn herd members of a threat.
     */
    private void warnHerdOfThreat(Entity threat) {
        // Play alarm call sound
        horse.level().playSound(null, horse.blockPosition(),
            getAlarmCallSound(),
            net.minecraft.sounds.SoundSource.HOSTILE,
            1.2f, 1.1f
        );

        // Alert nearby herd members
        if (herdMembers == null) {
            return;
        }

        for (AbstractHorse herdMember : herdMembers) {
            if (!herdMember.isAlive() || herdMember.distanceTo(horse) >= ALARM_CALL_RANGE) {
                continue;
            }

            // Mark herd member as aware of threat
            EcologyComponent component = getComponent(herdMember);
            if (component == null) {
                continue;
            }

            CompoundTag fleeingTag = component.getHandleTag(FLEEING_KEY);
            fleeingTag.putBoolean("is_fleeing", true);
            fleeingTag.putUUID("flee_from", threat.getUUID());
            fleeingTag.putLong("flee_until", horse.level().getGameTime() + 200);
        }
    }

    /**
     * Find a grazing target position.
     */
    private Vec3 findGrazingTarget() {
        double angle = horse.getRandom().nextDouble() * Math.PI * 2;
        double distance = 8.0 + horse.getRandom().nextDouble() * 8.0;

        double x = horse.getX() + Math.cos(angle) * distance;
        double z = horse.getZ() + Math.sin(angle) * distance;
        double y = horse.getY();

        return new Vec3(x, y, z);
    }

    /**
     * Check and maintain herd cohesion.
     */
    private void checkHerdCohesion() {
        if (herdMembers == null || herdMembers.isEmpty()) {
            return;
        }

        // Count nearby herd members
        long nearbyCount = herdMembers.stream()
            .filter(h -> h.isAlive())
            .filter(h -> horse.distanceTo(h) < COHESION_DISTANCE)
            .count();

        // If herd is scattered, call them
        if (nearbyCount < herdMembers.size() * 0.5) {
            horse.level().playSound(null, horse.blockPosition(),
                getCohesionCallSound(),
                net.minecraft.sounds.SoundSource.NEUTRAL,
                1.0f, 1.0f
            );
        }
    }

    /**
     * Move towards an entity with path validation.
     */
    private void moveToEntity(Entity target, double speed) {
        PathNavigation navigation = horse.getNavigation();
        Path path = navigation.createPath(target, 0);

        if (path != null && path.canReach()) {
            navigation.moveTo(target, speed);
        }
    }

    /**
     * Get the alarm call sound for this horse type.
     */
    private net.minecraft.sounds.SoundEvent getAlarmCallSound() {
        if (horseType == EntityType.DONKEY || horseType == EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_CHEST;
        }
        return net.minecraft.sounds.SoundEvents.HORSE_GALLOP;
    }

    /**
     * Get the cohesion call sound for this horse type.
     */
    private net.minecraft.sounds.SoundEvent getCohesionCallSound() {
        if (horseType == EntityType.DONKEY || horseType == EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        }
        return net.minecraft.sounds.SoundEvents.HORSE_AMBIENT;
    }

    /**
     * Get the ecology component for a horse.
     */
    private EcologyComponent getComponent(AbstractHorse horse) {
        if (!(horse instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[HerdDynamics] Horse #" + horse.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        String role = isLeadStallion ? "leader" : "follower";
        String leaderId = leadStallion != null ? "#" + leadStallion.getId() : "none";
        int memberCount = herdMembers != null ? herdMembers.size() : 0;
        return String.format("role=%s, leader=%s, members=%d, wild=%b",
            role, leaderId, memberCount, isWild);
    }
}
