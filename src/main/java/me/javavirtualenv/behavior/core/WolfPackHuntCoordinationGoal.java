package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Pack hunting coordination goal for wolves.
 *
 * <p>Scientific Basis: Wolves coordinate positions to encircle large prey.
 * Alpha initiates hunt, pack members move to flanking positions.
 * Success rates increase from 10-15% (solo) to 20-30% (pack hunting).
 *
 * <p>Behavior:
 * <ul>
 *   <li>Requires minimum 3 wolves in the same pack</li>
 *   <li>Alpha wolf marks target prey (large prey only: sheep, pigs, cows)</li>
 *   <li>Beta/omega wolves pathfind to flanking positions (90-120 degrees around prey)</li>
 *   <li>Coordinated attack when prey is surrounded</li>
 *   <li>Pack shares kill based on existing hierarchy</li>
 * </ul>
 *
 * <p>Parameters:
 * <ul>
 *   <li>Minimum pack size: 3 wolves</li>
 *   <li>Flanking angles: 90-120 degrees around prey</li>
 *   <li>Detection range: 32 blocks</li>
 *   <li>Coordination timeout: 10 seconds (200 ticks) to position before attack</li>
 *   <li>Target prey: Sheep, Pig, Cow (large prey only)</li>
 * </ul>
 */
public class WolfPackHuntCoordinationGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(WolfPackHuntCoordinationGoal.class);

    private static final int MIN_PACK_SIZE = 3;
    private static final double DETECTION_RANGE = 32.0;
    private static final double MIN_FLANKING_ANGLE = Math.toRadians(90);
    private static final double MAX_FLANKING_ANGLE = Math.toRadians(120);
    private static final int COORDINATION_TIMEOUT_TICKS = 200; // 10 seconds
    private static final double FLANKING_DISTANCE = 8.0;
    private static final double ATTACK_DISTANCE_SQUARED = 9.0; // 3 blocks
    private static final int PATH_RECALCULATION_INTERVAL = 10;
    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final int POSITION_TOLERANCE_SQUARED = 4; // 2 blocks

    private final Wolf wolf;
    private LivingEntity targetPrey;
    private Vec3 flankingPosition;
    private int coordinationTicks;
    private int pathRecalculationTimer;
    private int searchCooldown;
    private boolean isAlphaLeader;
    private boolean packPositioned;

    public WolfPackHuntCoordinationGoal(Wolf wolf) {
        this.wolf = wolf;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only activate if wolf is hungry or has hungry pack members
        if (!AnimalNeeds.isHungry(wolf) && !hasHungryPackMember()) {
            return false;
        }

        // Check pack size
        List<Wolf> packMembers = getNearbyPackMembers();
        if (packMembers.size() < MIN_PACK_SIZE - 1) { // -1 because we don't include self
            return false;
        }

        // Search cooldown
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }

        searchCooldown = SEARCH_INTERVAL_TICKS;

        // Determine if this wolf is the alpha
        WolfPackData.PackRank myRank = WolfPackData.getPackData(wolf).rank();
        isAlphaLeader = (myRank == WolfPackData.PackRank.ALPHA);

        // Only alpha initiates the hunt by targeting prey
        if (isAlphaLeader) {
            return findLargePrey();
        } else {
            // Beta/omega wolves follow the alpha's target
            return checkAlphaHasTarget(packMembers);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPrey == null || !targetPrey.isAlive()) {
            return false;
        }

        if (coordinationTicks >= COORDINATION_TIMEOUT_TICKS) {
            LOGGER.debug("{} pack hunt timed out after {} ticks", wolf.getName().getString(), coordinationTicks);
            return false;
        }

        // Check if prey is still in range
        if (wolf.distanceToSqr(targetPrey) > DETECTION_RANGE * DETECTION_RANGE) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting pack hunt for {}",
            wolf.getName().getString(),
            targetPrey != null ? targetPrey.getName().getString() : "null");

        coordinationTicks = 0;
        pathRecalculationTimer = 0;
        packPositioned = false;

        if (isAlphaLeader) {
            // Alpha directly targets the prey
            wolf.setTarget(targetPrey);
            wolf.getNavigation().moveTo(targetPrey, 1.3);
        } else {
            // Beta/omega wolves calculate flanking positions
            calculateFlankingPosition();
            if (flankingPosition != null) {
                pathfindToFlankingPosition();
            }
        }
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped pack hunt", wolf.getName().getString());
        targetPrey = null;
        flankingPosition = null;
        coordinationTicks = 0;
        pathRecalculationTimer = 0;
        packPositioned = false;

        if (!isAlphaLeader) {
            wolf.setTarget(null);
        }
        wolf.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        coordinationTicks++;
        pathRecalculationTimer++;

        if (targetPrey == null) {
            return;
        }

        wolf.getLookControl().setLookAt(targetPrey, 30.0F, 30.0F);

        if (isAlphaLeader) {
            tickAlphaBehavior();
        } else {
            tickFlankingBehavior();
        }
    }

    /**
     * Ticks the alpha wolf's hunting behavior.
     * Alpha directly pursues prey and attacks when in range.
     */
    private void tickAlphaBehavior() {
        double distanceSq = wolf.distanceToSqr(targetPrey);

        if (distanceSq <= ATTACK_DISTANCE_SQUARED) {
            wolf.doHurtTarget(targetPrey);
        }

        // Keep moving toward prey
        if (wolf.getNavigation().isDone() || pathRecalculationTimer >= PATH_RECALCULATION_INTERVAL) {
            wolf.getNavigation().moveTo(targetPrey, 1.3);
            pathRecalculationTimer = 0;
        }
    }

    /**
     * Ticks the flanking wolf's behavior.
     * Beta/omega wolves position themselves around prey and attack when positioned.
     */
    private void tickFlankingBehavior() {
        if (flankingPosition == null) {
            calculateFlankingPosition();
            if (flankingPosition == null) {
                return;
            }
        }

        double distanceToFlankingSq = wolf.distanceToSqr(flankingPosition);

        // Check if we've reached our flanking position
        if (!packPositioned && distanceToFlankingSq <= POSITION_TOLERANCE_SQUARED) {
            packPositioned = true;
            LOGGER.debug("{} reached flanking position", wolf.getName().getString());
        }

        // If positioned, attack the prey
        if (packPositioned || isPackPositioned()) {
            wolf.setTarget(targetPrey);
            double distanceToPreySq = wolf.distanceToSqr(targetPrey);

            if (distanceToPreySq <= ATTACK_DISTANCE_SQUARED) {
                wolf.doHurtTarget(targetPrey);
            } else {
                // Move toward prey for attack
                if (wolf.getNavigation().isDone() || pathRecalculationTimer >= PATH_RECALCULATION_INTERVAL) {
                    wolf.getNavigation().moveTo(targetPrey, 1.3);
                    pathRecalculationTimer = 0;
                }
            }
        } else {
            // Still moving to flanking position
            if (wolf.getNavigation().isDone() || pathRecalculationTimer >= PATH_RECALCULATION_INTERVAL) {
                pathfindToFlankingPosition();
                pathRecalculationTimer = 0;
            }
        }
    }

    /**
     * Checks if this wolf has hungry pack members.
     */
    private boolean hasHungryPackMember() {
        return WolfPackData.hasHungryPackMember(wolf, DETECTION_RANGE);
    }

    /**
     * Finds large prey (sheep, pig, cow) within detection range.
     */
    private boolean findLargePrey() {
        AABB searchBox = wolf.getBoundingBox().inflate(DETECTION_RANGE);
        List<LivingEntity> potentialPrey = wolf.level().getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            this::isLargePrey
        );

        if (potentialPrey.isEmpty()) {
            return false;
        }

        // Select closest prey
        targetPrey = potentialPrey.stream()
            .min((a, b) -> Double.compare(wolf.distanceToSqr(a), wolf.distanceToSqr(b)))
            .orElse(null);

        if (targetPrey != null) {
            LOGGER.debug("Alpha {} selected pack hunt target: {}",
                wolf.getName().getString(),
                targetPrey.getName().getString());
            return true;
        }

        return false;
    }

    /**
     * Checks if the alpha has marked a target that we should help hunt.
     */
    private boolean checkAlphaHasTarget(List<Wolf> packMembers) {
        // Find the alpha in the pack
        Wolf alpha = null;
        for (Wolf member : packMembers) {
            WolfPackData.PackRank rank = WolfPackData.getPackData(member).rank();
            if (rank == WolfPackData.PackRank.ALPHA) {
                alpha = member;
                break;
            }
        }

        if (alpha == null || alpha.getTarget() == null) {
            return false;
        }

        // Check if alpha's target is large prey
        LivingEntity alphaTarget = alpha.getTarget();
        if (isLargePrey(alphaTarget)) {
            targetPrey = alphaTarget;
            LOGGER.debug("{} joining pack hunt for alpha's target: {}",
                wolf.getName().getString(),
                targetPrey.getName().getString());
            return true;
        }

        return false;
    }

    /**
     * Gets all nearby pack members.
     */
    private List<Wolf> getNearbyPackMembers() {
        AABB searchBox = wolf.getBoundingBox().inflate(DETECTION_RANGE);
        WolfPackData myData = WolfPackData.getPackData(wolf);

        List<Wolf> packMembers = new ArrayList<>();
        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(Wolf.class, searchBox,
            other -> other != wolf && other.isAlive());

        for (Wolf other : nearbyWolves) {
            WolfPackData otherData = WolfPackData.getPackData(other);
            if (myData.packId().equals(otherData.packId())) {
                packMembers.add(other);
            }
        }

        return packMembers;
    }

    /**
     * Checks if an entity is large prey (sheep, pig, cow).
     */
    private boolean isLargePrey(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        if (entity.isInvulnerable()) {
            return false;
        }

        return entity instanceof Sheep || entity instanceof Pig || entity instanceof Cow;
    }

    /**
     * Calculates the flanking position for this wolf around the prey.
     */
    private void calculateFlankingPosition() {
        if (targetPrey == null) {
            return;
        }

        // Get pack members participating in hunt
        List<Wolf> packMembers = getNearbyPackMembers();

        // Determine this wolf's position index in the pack (excluding alpha)
        List<Wolf> flankers = new ArrayList<>();
        for (Wolf member : packMembers) {
            WolfPackData.PackRank rank = WolfPackData.getPackData(member).rank();
            if (rank != WolfPackData.PackRank.ALPHA) {
                flankers.add(member);
            }
        }
        flankers.add(wolf);
        flankers.sort((a, b) -> a.getUUID().compareTo(b.getUUID())); // Consistent ordering

        int myIndex = flankers.indexOf(wolf);
        int totalFlankers = flankers.size();

        if (myIndex < 0 || totalFlankers == 0) {
            return;
        }

        // Calculate angle for this wolf
        // Distribute flankers evenly between 90-120 degrees on each side
        double angleRange = MAX_FLANKING_ANGLE - MIN_FLANKING_ANGLE;
        double angleStep = angleRange / Math.max(1, totalFlankers - 1);
        double baseAngle = MIN_FLANKING_ANGLE + (angleStep * myIndex);

        // Alternate sides (left/right of prey direction)
        boolean isLeftSide = (myIndex % 2 == 0);

        // Get prey's position and velocity direction
        Vec3 preyPos = targetPrey.position();
        Vec3 preyVelocity = targetPrey.getDeltaMovement();

        // Use velocity direction if moving, otherwise use direction from alpha to prey
        Vec3 baseDirection;
        if (preyVelocity.lengthSqr() > 0.01) {
            baseDirection = preyVelocity.normalize();
        } else {
            // Find alpha for directional reference
            Wolf alpha = null;
            for (Wolf member : packMembers) {
                if (WolfPackData.getPackData(member).rank() == WolfPackData.PackRank.ALPHA) {
                    alpha = member;
                    break;
                }
            }
            if (alpha != null) {
                baseDirection = preyPos.subtract(alpha.position()).normalize();
            } else {
                baseDirection = preyPos.subtract(wolf.position()).normalize();
            }
        }

        // Calculate flanking position
        double finalAngle = isLeftSide ? baseAngle : -baseAngle;
        double x = Math.cos(finalAngle) * baseDirection.x - Math.sin(finalAngle) * baseDirection.z;
        double z = Math.sin(finalAngle) * baseDirection.x + Math.cos(finalAngle) * baseDirection.z;

        Vec3 flankingOffset = new Vec3(x, 0, z).normalize().scale(FLANKING_DISTANCE);
        flankingPosition = preyPos.add(flankingOffset);

        LOGGER.debug("{} calculated flanking position at angle {} degrees (index {}/{})",
            wolf.getName().getString(),
            Math.toDegrees(finalAngle),
            myIndex,
            totalFlankers);
    }

    /**
     * Pathfinds to the calculated flanking position.
     */
    private void pathfindToFlankingPosition() {
        if (flankingPosition == null) {
            return;
        }

        wolf.getNavigation().moveTo(flankingPosition.x, flankingPosition.y, flankingPosition.z, 1.2);
    }

    /**
     * Checks if the pack is properly positioned for coordinated attack.
     */
    private boolean isPackPositioned() {
        List<Wolf> packMembers = getNearbyPackMembers();
        if (packMembers.isEmpty()) {
            return false;
        }

        // Count how many pack members are in attack range
        int positionedCount = 0;
        for (Wolf member : packMembers) {
            if (member.getTarget() == targetPrey) {
                double distSq = member.distanceToSqr(targetPrey);
                if (distSq <= ATTACK_DISTANCE_SQUARED * 4) { // Within reasonable range
                    positionedCount++;
                }
            }
        }

        // Consider positioned if at least 2 pack members are ready
        return positionedCount >= 2;
    }
}
