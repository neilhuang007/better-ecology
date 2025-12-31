package me.javavirtualenv.behavior.bee;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Behavior for hive defense in bees.
 * <p>
 * This behavior:
 * - Triggers aggressive defense when hive is threatened
 * - Coordinates swarm attacks on threats
 * - Alarms other bees to danger
 * - Prioritizes protection of hive over individual safety
 * <p>
 * Bees will attack players or mobs that:
 * - Attack another bee near the hive
 * - Harvest honey without a campfire
 * - Destroy the hive
 */
public class HiveDefenseBehavior extends SteeringBehavior {

    private static final double DEFENSE_RADIUS = 22.0;
    private static final double ATTACK_RADIUS = 8.0;
    private static final double SWARM_COORDINATION_RADIUS = 32.0;
    private static final int ANGER_DURATION = 400; // 20 seconds

    private Entity target;
    private boolean isDefending;
    private int angerTimer;

    public HiveDefenseBehavior() {
        this(1.5); // High priority for defense
    }

    public HiveDefenseBehavior(double weight) {
        super(weight);
        this.target = null;
        this.isDefending = false;
        this.angerTimer = 0;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Bee bee)) {
            return new Vec3d();
        }

        Level level = context.getWorld();
        Vec3d position = context.getPosition();

        // Update anger timer
        if (angerTimer > 0) {
            angerTimer--;
            if (angerTimer <= 0) {
                isDefending = false;
                target = null;
            }
        }

        // Check for threats
        Entity threat = detectThreat(bee, level, context);

        if (threat != null) {
            target = threat;
            isDefending = true;
            angerTimer = ANGER_DURATION;

            // Make bee angry (vanilla mechanic)
            if (!bee.isAngry()) {
                bee.setRemainingPersistentAngerTime(ANGER_DURATION);
                if (threat instanceof Player player) {
                    bee.setTarget(player);
                }
            }

            // Alarm other bees
            alarmNearbyBees(bee, level, threat);

            // Calculate attack steering
            return calculateAttackForce(context, threat);
        }

        // If currently angry, chase target
        if (isDefending && target != null && target.isAlive()) {
            Vec3d targetPos = new Vec3d(target.position());
            double distance = position.distanceTo(targetPos);

            if (distance <= ATTACK_RADIUS) {
                // In attack range - stop moving and let vanilla attack handle it
                return new Vec3d();
            }

            return calculateAttackForce(context, target);
        }

        return new Vec3d();
    }

    /**
     * Detects potential threats to the hive.
     */
    private Entity detectThreat(Bee bee, Level level, BehaviorContext context) {
        Vec3 position = bee.position();

        // Check if bee is already angry
        if (bee.isAngry() && bee.getTarget() != null) {
            Entity angryTarget = bee.getTarget();
            if (angryTarget.isAlive()) {
                return angryTarget;
            }
        }

        // Check nearby entities for threats
        AABB searchBox = AABB.ofSize(position, DEFENSE_RADIUS * 2, DEFENSE_RADIUS * 2, DEFENSE_RADIUS * 2);

        // Check for players (threatening if close to hive)
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox);
        for (Player player : nearbyPlayers) {
            if (isThreateningToHive(player, bee, level)) {
                return player;
            }
        }

        // Check if any nearby bee is being attacked
        List<Bee> nearbyBees = level.getEntitiesOfClass(Bee.class, searchBox);
        for (Entity otherBee : nearbyBees) {
            if (otherBee != bee && otherBee instanceof Bee) {
                Bee other = (Bee) otherBee;
                if (other.isAngry() && other.getTarget() != null) {
                    // Join the defense
                    return other.getTarget();
                }
            }
        }

        return null;
    }

    /**
     * Checks if an entity is threatening to the hive.
     */
    private boolean isThreateningToHive(Entity entity, Bee bee, Level level) {
        Vec3 entityPos = entity.position();
        Vec3 beePos = bee.position();

        // Check if entity is close to a bee hive
        BlockPos nearbyHive = findNearbyHive(level, new Vec3d(entityPos));
        if (nearbyHive == null) {
            return false;
        }

        double distanceToHive = new Vec3d(entityPos).distanceTo(new Vec3d(
            nearbyHive.getX() + 0.5,
            nearbyHive.getY() + 0.5,
            nearbyHive.getZ() + 0.5
        ));

        // Consider threatening if within 8 blocks of hive
        return distanceToHive <= 8.0;
    }

    /**
     * Finds a nearby bee hive.
     */
    private BlockPos findNearbyHive(Level level, Vec3d position) {
        BlockPos searchPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
        int searchRadius = 8;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = searchPos.offset(x, y, z);
                    if (level.getBlockState(testPos).is(net.minecraft.world.level.block.Blocks.BEEHIVE) ||
                        level.getBlockState(testPos).is(net.minecraft.world.level.block.Blocks.BEE_NEST)) {
                        return testPos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Alarms nearby bees to join the defense.
     */
    private void alarmNearbyBees(Bee alarmBee, Level level, Entity threat) {
        Vec3 position = alarmBee.position();
        AABB searchBox = AABB.ofSize(position, SWARM_COORDINATION_RADIUS * 2,
            SWARM_COORDINATION_RADIUS * 2, SWARM_COORDINATION_RADIUS * 2);

        List<Bee> nearbyBees = level.getEntitiesOfClass(Bee.class, searchBox);

        for (Bee otherBee : nearbyBees) {
            if (otherBee != alarmBee && !otherBee.isAngry()) {
                // Trigger anger in nearby bees
                otherBee.setRemainingPersistentAngerTime(ANGER_DURATION);
                otherBee.setTarget(threat);
            }
        }
    }

    /**
     * Calculates steering force for attacking a threat.
     */
    private Vec3d calculateAttackForce(BehaviorContext context, Entity threat) {
        Vec3d position = context.getPosition();
        Vec3d targetPos = new Vec3d(threat.position().add(0, 0.5, 0));

        Vec3d desired = Vec3d.sub(targetPos, position);
        desired.normalize();
        desired.mult(context.getMaxSpeed() * 1.5); // Move faster when attacking

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        steer = limitForce(steer, context.getMaxForce() * 1.5);

        return steer;
    }

    public boolean isDefending() {
        return isDefending;
    }

    public Entity getTarget() {
        return target;
    }

    public int getAngerTimer() {
        return angerTimer;
    }

    public void triggerDefense(Entity threat) {
        this.target = threat;
        this.isDefending = true;
        this.angerTimer = ANGER_DURATION;
    }
}
