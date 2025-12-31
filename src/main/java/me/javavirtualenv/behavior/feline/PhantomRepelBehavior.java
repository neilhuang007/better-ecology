package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;

/**
 * Phantom repelling behavior for sleeping cats.
 * <p>
 * Cats repel phantoms when sleeping:
 * - Works in a 16-block radius
 * - Phantoms will not attack players near a sleeping cat
 * - Cats will hiss at phantoms that approach
 * - Special affinity for scaring away phantoms
 */
public class PhantomRepelBehavior extends SteeringBehavior {

    private final double repelRange;
    private final double detectionRange;

    private Phantom targetPhantom;
    private boolean isSleeping = false;

    public PhantomRepelBehavior(double repelRange, double detectionRange) {
        super(0.0);
        this.repelRange = repelRange;
        this.detectionRange = detectionRange;
    }

    public PhantomRepelBehavior() {
        this(16.0, 32.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Check if cat is sleeping
        if (!isCatSleeping(mob)) {
            isSleeping = false;
            targetPhantom = null;
            return new Vec3d();
        }

        isSleeping = true;

        // Find nearby phantom
        Phantom phantom = findNearestPhantom(mob);
        if (phantom == null) {
            targetPhantom = null;
            return new Vec3d();
        }

        targetPhantom = phantom;

        // Hiss at phantom (already handled by HissBehavior)
        // This behavior mainly marks the phantom as deterred
        double distance = mob.position().distanceTo(phantom.position());

        if (distance < repelRange) {
            // Phantom is repelled - this is handled by phantom's AI
            // We just need to track it here
        }

        return new Vec3d(); // No movement when sleeping
    }

    private boolean isCatSleeping(Mob mob) {
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat) {
            return cat.isInSittingPose();
        }
        return false;
    }

    private Phantom findNearestPhantom(Mob mob) {
        Phantom nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Phantom phantom : mob.level().getEntitiesOfClass(
                Phantom.class,
                mob.getBoundingBox().inflate(detectionRange))) {
            if (!phantom.isAlive()) {
                continue;
            }

            double distance = mob.position().distanceTo(phantom.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = phantom;
            }
        }

        return nearest;
    }

    /**
     * Check if a phantom is within repel range.
     */
    public boolean isPhantomRepelled(Phantom phantom) {
        if (!isSleeping || targetPhantom == null) {
            return false;
        }

        double distance = targetPhantom.position().distanceTo(phantom.position());
        return distance < repelRange;
    }

    public boolean isSleeping() {
        return isSleeping;
    }

    public Phantom getTargetPhantom() {
        return targetPhantom;
    }

    public double getRepelRange() {
        return repelRange;
    }
}
