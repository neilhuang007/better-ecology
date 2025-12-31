package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Territorial behavior for wolf packs.
 * Wolves mark and defend territory from intruders, with aggression levels
 * based on pack size and territory importance.
 * <p>
 * Based on research into wolf territorial behavior:
 * - Territory size: 50-150 square miles (scaled to blocks)
 * - Scent marking at boundaries
 * - Aggressive toward wolves from other packs
 * - Defense intensity increases near territory center
 */
public class PackTerritoryBehavior extends SteeringBehavior {

    private final double territoryRadius;
    private final double coreTerritoryRadius;
    private final double defenseForce;
    private final double patrolSpeed;
    private final int markInterval;

    private UUID packId;
    private Vec3d territoryCenter;
    private int lastMarkTick;
    private int lastHowlTick;

    public PackTerritoryBehavior(double territoryRadius, double coreTerritoryRadius,
                                double defenseForce, double patrolSpeed, int markInterval) {
        this.territoryRadius = territoryRadius;
        this.coreTerritoryRadius = coreTerritoryRadius;
        this.defenseForce = defenseForce;
        this.patrolSpeed = patrolSpeed;
        this.markInterval = markInterval;
    }

    public PackTerritoryBehavior() {
        this(64.0, 32.0, 0.2, 0.6, 1200);
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
        initializePackData(wolf);

        // Check for intruders
        Entity intruder = detectIntruder(wolf, context);
        if (intruder != null) {
            return calculateDefense(wolf, context, intruder);
        }

        // Patrol territory or mark boundaries
        return calculatePatrol(wolf, context);
    }

    /**
     * Initializes or updates pack territory data.
     */
    private void initializePackData(Wolf wolf) {
        if (packId == null) {
            packId = getPackId(wolf);
        }

        if (territoryCenter == null) {
            // Territory center is where the pack formed
            territoryCenter = Vec3d.fromMinecraftVec3(wolf.position());
            saveTerritoryData(wolf);
        }
    }

    /**
     * Detects intruders in pack territory.
     */
    private Entity detectIntruder(Wolf wolf, BehaviorContext context) {
        Vec3d wolfPos = context.getPosition();

        for (Entity entity : context.getNeighbors()) {
            if (!(entity instanceof Wolf otherWolf)) {
                continue;
            }

            // Skip self and tamed wolves
            if (entity.equals(wolf) || otherWolf.isTame()) {
                continue;
            }

            // Check if different pack
            UUID otherPackId = getPackId(otherWolf);
            if (packId != null && packId.equals(otherPackId)) {
                continue;
            }

            // Check if in our territory
            Vec3d otherPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            double distance = wolfPos.distanceTo(otherPos);

            // More aggressive near territory center
            double distanceFromCenter = otherPos.distanceTo(territoryCenter);
            double detectionRange = distanceFromCenter < coreTerritoryRadius ?
                territoryRadius : coreTerritoryRadius;

            if (distance < detectionRange && distanceFromCenter < territoryRadius) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Calculates defense force against intruder.
     * Stronger near territory center.
     */
    private Vec3d calculateDefense(Wolf wolf, BehaviorContext context, Entity intruder) {
        Vec3d wolfPos = context.getPosition();
        Vec3d intruderPos = new Vec3d(intruder.getX(), intruder.getY(), intruder.getZ());

        // Calculate distance from territory center
        double distanceFromCenter = wolfPos.distanceTo(territoryCenter);

        // Defense is stronger near core territory
        double urgency = 1.0 - (distanceFromCenter / territoryRadius);
        urgency = Math.max(0.3, Math.min(1.0, urgency));

        // Howl to alert pack if in core territory
        if (distanceFromCenter < coreTerritoryRadius) {
            tryHowl(wolf);
        }

        // Seek intruder
        Vec3d toIntruder = Vec3d.sub(intruderPos, wolfPos);
        toIntruder.normalize();
        toIntruder.mult(maxForce() * urgency);

        return toIntruder;
    }

    /**
     * Calculates patrol behavior within territory.
     */
    private Vec3d calculatePatrol(Wolf wolf, BehaviorContext context) {
        Vec3d wolfPos = context.getPosition();
        double distanceFromCenter = wolfPos.distanceTo(territoryCenter);

        // Mark territory at boundaries
        if (distanceFromCenter > territoryRadius * 0.8) {
            tryMarkTerritory(wolf);
            return seekTerritoryCenter(wolfPos, context.getVelocity());
        }

        // Random patrol within territory
        return calculatePatrolForce(wolf, context, distanceFromCenter);
    }

    /**
     * Calculates patrol force for wandering territory.
     */
    private Vec3d calculatePatrolForce(Wolf wolf, BehaviorContext context,
                                       double distanceFromCenter) {
        // If near boundary, return to center
        if (distanceFromCenter > territoryRadius * 0.9) {
            return seekTerritoryCenter(context.getPosition(), context.getVelocity());
        }

        // If too close to center, move outward
        if (distanceFromCenter < coreTerritoryRadius * 0.5) {
            return seekBoundary(context.getPosition(), context.getVelocity());
        }

        // Otherwise, random patrol
        return new Vec3d(); // Let vanilla AI handle random wandering
    }

    /**
     * Seeks territory center.
     */
    private Vec3d seekTerritoryCenter(Vec3d position, Vec3d velocity) {
        Vec3d toCenter = Vec3d.sub(territoryCenter, position);
        toCenter.normalize();
        toCenter.mult(patrolSpeed);

        Vec3d steer = Vec3d.sub(toCenter, velocity);
        steer.limit(defenseForce * 0.5);

        return steer;
    }

    /**
     * Seeks territory boundary.
     */
    private Vec3d seekBoundary(Vec3d position, Vec3d velocity) {
        Vec3d toCenter = Vec3d.sub(territoryCenter, position);
        toCenter.normalize();
        toCenter.mult(-1); // Reverse direction
        toCenter.mult(patrolSpeed);

        Vec3d steer = Vec3d.sub(toCenter, velocity);
        steer.limit(defenseForce * 0.3);

        return steer;
    }

    /**
     * Attempts to mark territory with scent.
     */
    private void tryMarkTerritory(Wolf wolf) {
        int currentTick = wolf.tickCount;

        if (currentTick - lastMarkTick >= markInterval) {
            markTerritory(wolf);
            lastMarkTick = currentTick;
        }
    }

    /**
     * Marks territory with scent (particle effect).
     */
    private void markTerritory(Wolf wolf) {
        Level level = wolf.level();

        // Particle effect for scent marking
        if (!level.isClientSide) {
            // Server-side - sync to clients
            // In a full implementation, would send packet to spawn particles
            // For now, we play a sound as a simple indicator
            level.playSound(null, wolf.blockPosition(), SoundEvents.WOLF_SNIFF,
                SoundSource.NEUTRAL, 0.5f, 1.0f);
        }
    }

    /**
     * Attempts to howl to alert pack of intruders.
     */
    private void tryHowl(Wolf wolf) {
        int currentTick = wolf.tickCount;

        // Howl cooldown (30 seconds)
        if (currentTick - lastHowlTick >= 600) {
            howl(wolf);
            lastHowlTick = currentTick;
        }
    }

    /**
     * Howls to alert pack of territory intrusion.
     */
    private void howl(Wolf wolf) {
        Level level = wolf.level();

        if (!level.isClientSide) {
            level.playSound(null, wolf.blockPosition(), SoundEvents.WOLF_HOWL,
                SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
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
     * Saves territory data to NBT.
     */
    private void saveTerritoryData(Wolf wolf) {
        var persistentData = wolf.getPersistentData();

        if (territoryCenter != null) {
            persistentData.putDouble("WolfTerritoryX", territoryCenter.x);
            persistentData.putDouble("WolfTerritoryY", territoryCenter.y);
            persistentData.putDouble("WolfTerritoryZ", territoryCenter.z);
        }
    }

    /**
     * Loads territory data from NBT.
     */
    private void loadTerritoryData(Wolf wolf) {
        var persistentData = wolf.getPersistentData();

        if (persistentData.contains("WolfTerritoryX")) {
            double x = persistentData.getDouble("WolfTerritoryX");
            double y = persistentData.getDouble("WolfTerritoryY");
            double z = persistentData.getDouble("WolfTerritoryZ");
            territoryCenter = new Vec3d(x, y, z);
        }
    }

    public Vec3d getTerritoryCenter() {
        return territoryCenter;
    }

    public void setTerritoryCenter(Vec3d center) {
        this.territoryCenter = center;
    }

    public double getTerritoryRadius() {
        return territoryRadius;
    }

    public boolean isInTerritory(Vec3d position) {
        if (territoryCenter == null) {
            return true;
        }
        return position.distanceTo(territoryCenter) <= territoryRadius;
    }

    public boolean isInCoreTerritory(Vec3d position) {
        if (territoryCenter == null) {
            return true;
        }
        return position.distanceTo(territoryCenter) <= coreTerritoryRadius;
    }
}
