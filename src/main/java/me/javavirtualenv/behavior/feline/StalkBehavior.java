package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import me.javavirtualenv.behavior.predation.PreySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Stalk behavior for feline predators.
 * <p>
 * Cats and ocelots approach prey slowly and stealthily, staying low
 * and avoiding detection until ready to pounce. This behavior:
 * - Moves slowly toward prey (0.3x normal speed)
 * - Crouches when stalking (reduces detection radius)
 * - Prefers approaching from behind
 * - Stops when prey looks toward the cat
 * - Transitions to pounce when within striking distance
 */
public class StalkBehavior extends SteeringBehavior {

    private final double stalkSpeed;
    private final double detectionRange;
    private final double pounceDistance;
    private final double giveUpAngle;
    private final PreySelector preySelector;

    private Entity currentPrey;
    private boolean isStalking = false;
    private int stalkTicks = 0;
    private int waitTicks = 0;

    public StalkBehavior(double stalkSpeed, double detectionRange,
                         double pounceDistance, double giveUpAngle, PreySelector preySelector) {
        super(1.0);
        this.stalkSpeed = stalkSpeed;
        this.detectionRange = detectionRange;
        this.pounceDistance = pounceDistance;
        this.giveUpAngle = giveUpAngle;
        this.preySelector = preySelector != null ? preySelector : new PreySelector();
    }

    public StalkBehavior(double stalkSpeed, double detectionRange,
                         double pounceDistance, double giveUpAngle) {
        this(stalkSpeed, detectionRange, pounceDistance, giveUpAngle, new PreySelector());
    }

    public StalkBehavior() {
        this(0.3, 16.0, 3.0, 45.0, new PreySelector());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();
        Vec3d position = context.getPosition();

        // Find or validate prey
        if (currentPrey == null || !currentPrey.isAlive()) {
            currentPrey = findPrey(mob);
            if (currentPrey == null) {
                isStalking = false;
                return new Vec3d();
            }
        }

        Vec3d preyPos = new Vec3d(currentPrey.getX(), currentPrey.getY(), currentPrey.getZ());
        double distance = position.distanceTo(preyPos);

        // Check if prey has detected the stalker
        if (isPreyLookingAtPrey(mob, currentPrey)) {
            waitTicks++;
            isStalking = false;

            // Wait for prey to look away, give up after 5 seconds
            if (waitTicks > 100) {
                currentPrey = null;
                waitTicks = 0;
            }
            return new Vec3d();
        }

        // Check if close enough to pounce
        if (distance <= pounceDistance) {
            isStalking = false;
            stalkTicks = 0;
            return new Vec3d(); // Signal to transition to pounce
        }

        // Check if too far to stalk
        if (distance > detectionRange) {
            currentPrey = null;
            isStalking = false;
            return new Vec3d();
        }

        // Calculate stalking approach
        isStalking = true;
        stalkTicks++;
        return calculateStalkApproach(position, preyPos, mob, currentPrey);
    }

    /**
     * Calculates a stealthy approach vector.
     * Prefers approaching from behind the prey at an angle.
     */
    private Vec3d calculateStalkApproach(Vec3d position, Vec3d preyPos,
                                          Mob predator, Entity prey) {
        // Get prey's facing direction
        double preyYaw = prey.getYRot();
        double preyLookX = -Math.sin(Math.toRadians(preyYaw));
        double preyLookZ = Math.cos(Math.toRadians(preyYaw));

        // Vector from predator to prey
        Vec3d toPrey = Vec3d.sub(preyPos, position);
        double distanceToPrey = toPrey.magnitude();

        // Calculate angle of approach relative to prey's view
        // We want to approach from behind (180 degrees from prey's facing)
        Vec3d desiredApproach = new Vec3d(-preyLookX, 0, -preyLookZ);

        // If we're not directly behind, curve toward the ideal approach angle
        Vec3d currentApproach = toPrey.copy();
        currentApproach.normalize();

        // Blend current approach with desired stealth approach
        desiredApproach.normalize();
        desiredApproach.mult(0.7);
        currentApproach.mult(0.3);
        desiredApproach.add(currentApproach);
        desiredApproach.normalize();

        // Move at stalking speed
        desiredApproach.mult(stalkSpeed);

        return desiredApproach;
    }

    /**
     * Checks if the prey is looking toward the predator.
     */
    private boolean isPreyLookingAtPrey(Mob predator, Entity prey) {
        Vec3d predatorPos = new Vec3d(predator.getX(), predator.getY(), predator.getZ());
        Vec3d preyPos = new Vec3d(prey.getX(), prey.getY(), prey.getZ());

        Vec3d toPredator = Vec3d.sub(predatorPos, preyPos);
        double distanceToPredator = toPredator.magnitude();

        // Check if prey is looking generally toward predator
        double preyYaw = prey.getYRot();
        double preyLookX = -Math.sin(Math.toRadians(preyYaw));
        double preyLookZ = Math.cos(Math.toRadians(preyYaw));

        Vec3d preyLookDir = new Vec3d(preyLookX, 0, preyLookZ);
        toPredator.normalize();

        double dotProduct = preyLookDir.dot(toPredator);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dotProduct))));

        // Prey detects predator if looking within 45 degrees
        return angle < giveUpAngle && distanceToPredator < detectionRange * 0.6;
    }

    private Entity findPrey(Mob predator) {
        // Use PreySelector to find optimal prey
        Entity selectedPrey = preySelector.selectPrey(predator);

        // Filter to feline-specific prey (small prey only)
        if (selectedPrey != null && isValidPrey(predator, selectedPrey)) {
            return selectedPrey;
        }

        return null;
    }

    private boolean isValidPrey(Mob predator, Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        // Small prey only
        if (entity.getBbWidth() > predator.getBbWidth() * 1.2) {
            return false;
        }

        // Don't stalk players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        return true;
    }

    public Entity getCurrentPrey() {
        return currentPrey;
    }

    public boolean isStalking() {
        return isStalking;
    }

    public void setCurrentPrey(Entity prey) {
        this.currentPrey = prey;
    }

    public void reset() {
        this.currentPrey = null;
        this.isStalking = false;
        this.stalkTicks = 0;
        this.waitTicks = 0;
    }
}
