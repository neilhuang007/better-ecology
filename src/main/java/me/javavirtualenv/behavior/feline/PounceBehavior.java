package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pounce behavior for feline predators.
 * <p>
 * Cats and ocelots leap at prey with explosive power when close enough.
 * This behavior:
 * - Waits for the right moment (prey distracted or looking away)
 * - Leaps forward with high velocity
 * - Has a cooldown between pounces
 * - Can miss if prey moves quickly
 * - Triggers attack animation when reaching target
 */
public class PounceBehavior extends SteeringBehavior {

    private final double pounceSpeed;
    private final double pounceRange;
    private final double pounceCooldown;
    private final double pounceForce;

    private boolean isPouncing = false;
    private int pounceTick = 0;
    private int cooldownTicks = 0;
    private Entity target;
    private Vec3d pounceDirection;

    public PounceBehavior(double pounceSpeed, double pounceRange,
                          double pounceCooldown, double pounceForce) {
        super(1.0);
        this.pounceSpeed = pounceSpeed;
        this.pounceRange = pounceRange;
        this.pounceCooldown = pounceCooldown;
        this.pounceForce = pounceForce;
    }

    public PounceBehavior() {
        this(1.5, 4.0, 60, 0.8);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Handle cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return new Vec3d();
        }

        // Handle ongoing pounce
        if (isPouncing) {
            return continuePounce(context);
        }

        // Check if we should start a pounce
        Entity potentialTarget = findPounceTarget(mob);
        if (potentialTarget == null) {
            return new Vec3d();
        }

        // Start pounce
        return startPounce(context, potentialTarget);
    }

    private Vec3d startPounce(BehaviorContext context, Entity targetEntity) {
        Mob mob = context.getEntity();
        Vec3d position = context.getPosition();
        Vec3d targetPos = new Vec3d(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());

        // Calculate pounce direction
        pounceDirection = Vec3d.sub(targetPos, position);
        double distance = pounceDirection.magnitude();

        // Check if in range
        if (distance > pounceRange) {
            return new Vec3d();
        }

        pounceDirection.normalize();
        target = targetEntity;
        isPouncing = true;
        pounceTick = 0;

        // Apply initial impulse
        Vec3d impulse = pounceDirection.copy();
        impulse.mult(pounceForce);

        return impulse;
    }

    private Vec3d continuePounce(BehaviorContext context) {
        Mob mob = context.getEntity();
        pounceTick++;

        // Pounce lasts about 10 ticks (0.5 seconds)
        if (pounceTick > 10) {
            finishPounce(mob);
            return new Vec3d();
        }

        // Continue moving in pounce direction
        Vec3d velocity = pounceDirection.copy();
        velocity.mult(pounceSpeed);

        // Check if we hit the target
        if (target != null && target.isAlive()) {
            double distance = mob.position().distanceTo(target.position());
            if (distance < 1.5) {
                // Hit! Trigger attack
                mob.doHurtTarget(target);
                finishPounce(mob);
                return new Vec3d();
            }
        }

        return velocity;
    }

    private void finishPounce(Mob mob) {
        isPouncing = false;
        pounceTick = 0;
        cooldownTicks = (int) pounceCooldown;
        target = null;
        pounceDirection = null;
    }

    private Entity findPounceTarget(Mob mob) {
        // Find nearby small prey
        Entity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : mob.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                mob.getBoundingBox().inflate(pounceRange))) {
            if (entity.equals(mob)) {
                continue;
            }

            if (!isValidPounceTarget(mob, entity)) {
                continue;
            }

            double distance = mob.position().distanceTo(entity.position());
            if (distance < nearestDistance && distance <= pounceRange) {
                nearestDistance = distance;
                nearestTarget = entity;
            }
        }

        return nearestTarget;
    }

    private boolean isValidPounceTarget(Mob mob, Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        // Only pounce on smaller entities
        if (entity.getBbWidth() > mob.getBbWidth() * 1.2) {
            return false;
        }

        // Don't pounce on players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        return true;
    }

    public boolean isPouncing() {
        return isPouncing;
    }

    public boolean canPounce() {
        return cooldownTicks == 0 && !isPouncing;
    }

    public void reset() {
        isPouncing = false;
        pounceTick = 0;
        cooldownTicks = 0;
        target = null;
        pounceDirection = null;
    }
}
