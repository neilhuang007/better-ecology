package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;

/**
 * Creeper detection behavior for cats and ocelots.
 * <p>
 * Cats have a special ability to detect and deter creepers:
 * - Detection range of 16 blocks
 * - Hissing at creepers causes them to flee
 * - Cats will actively move between creepers and their owners
 * - Prevents creeper explosions when nearby
 */
public class CreeperDetectionBehavior extends SteeringBehavior {

    private final double detectionRange;
    private final double deterrenceRange;
    private final double protectionSpeed;

    private Creeper targetCreeper;
    private Entity protectedEntity;

    public CreeperDetectionBehavior(double detectionRange, double deterrenceRange,
                                     double protectionSpeed) {
        super(1.0);
        this.detectionRange = detectionRange;
        this.deterrenceRange = deterrenceRange;
        this.protectionSpeed = protectionSpeed;
    }

    public CreeperDetectionBehavior() {
        this(16.0, 6.0, 0.5);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Find nearby creeper
        Creeper creeper = findNearestCreeper(mob);
        if (creeper == null) {
            targetCreeper = null;
            return new Vec3d();
        }

        targetCreeper = creeper;

        // Find entity to protect (owner if tamed)
        protectedEntity = findEntityToProtect(mob);

        // Move between creeper and protected entity
        if (protectedEntity != null) {
            return moveToIntercept(mob, creeper, protectedEntity);
        }

        // Move toward creeper to hiss/deter
        Vec3d mobPos = context.getPosition();
        Vec3d creeperPos = new Vec3d(creeper.getX(), creeper.getY(), creeper.getZ());
        Vec3d toCreeper = Vec3d.sub(creeperPos, mobPos);
        double distance = toCreeper.magnitude();

        if (distance > deterrenceRange) {
            toCreeper.normalize();
            toCreeper.mult(protectionSpeed);
            return toCreeper;
        }

        return new Vec3d();
    }

    private Creeper findNearestCreeper(Mob mob) {
        Creeper nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Creeper creeper : mob.level().getEntitiesOfClass(
                Creeper.class,
                mob.getBoundingBox().inflate(detectionRange))) {
            if (!creeper.isAlive()) {
                continue;
            }

            if (creeper.isIgnited()) {
                // Already ignited, can't deter
                continue;
            }

            double distance = mob.position().distanceTo(creeper.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = creeper;
            }
        }

        return nearest;
    }

    private Entity findEntityToProtect(Mob mob) {
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat) {
            if (cat.isTame() && cat.getOwner() != null) {
                Entity owner = mob.level().getEntity(cat.getOwnerUUID());
                if (owner != null && owner.isAlive()) {
                    return owner;
                }
            }
        }
        return null;
    }

    private Vec3d moveToIntercept(Mob mob, Creeper creeper, Entity protect) {
        Vec3d mobPos = new Vec3d(mob.getX(), mob.getY(), mob.getZ());
        Vec3d creeperPos = new Vec3d(creeper.getX(), creeper.getY(), creeper.getZ());
        Vec3d protectPos = new Vec3d(protect.getX(), protect.getY(), protect.getZ());

        // Calculate midpoint between creeper and protected entity
        Vec3d midpoint = Vec3d.sub(creeperPos, protectPos);
        midpoint.mult(0.5);
        midpoint.add(protectPos);

        // Move to midpoint
        Vec3d toMidpoint = Vec3d.sub(midpoint, mobPos);
        toMidpoint.normalize();
        toMidpoint.mult(protectionSpeed);

        return toMidpoint;
    }

    public Creeper getTargetCreeper() {
        return targetCreeper;
    }

    public boolean hasDetectedCreeper() {
        return targetCreeper != null && targetCreeper.isAlive();
    }

    public boolean canDeterCreeper(Creeper creeper) {
        if (creeper == null) {
            return false;
        }

        double distance = targetCreeper.position().distanceTo(creeper.position());
        return distance < deterrenceRange && !creeper.isIgnited();
    }
}
