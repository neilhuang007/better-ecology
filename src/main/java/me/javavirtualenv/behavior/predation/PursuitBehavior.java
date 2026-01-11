package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Pursuit behavior for predators chasing prey.
 * Implements interception course calculation to predict prey movement.
 * Based on research into predator pursuit strategies in carnivores.
 */
public class PursuitBehavior extends SteeringBehavior {

    private final double maxPursuitSpeed;
    private final double maxPursuitForce;
    private final double predictionTime;
    private final double giveUpDistance;
    private final PreySelector preySelector;

    private Entity currentPrey;
    private HuntingState currentState = HuntingState.IDLE;
    private int chaseTimer = 0;

    public PursuitBehavior(double maxPursuitSpeed, double maxPursuitForce,
                          double predictionTime, double giveUpDistance,
                          PreySelector preySelector) {
        this.maxPursuitSpeed = maxPursuitSpeed;
        this.maxPursuitForce = maxPursuitForce;
        this.predictionTime = predictionTime;
        this.giveUpDistance = giveUpDistance;
        this.preySelector = preySelector != null ? preySelector : new PreySelector();
    }

    public PursuitBehavior(double maxPursuitSpeed, double maxPursuitForce,
                          double predictionTime, double giveUpDistance) {
        this(maxPursuitSpeed, maxPursuitForce, predictionTime, giveUpDistance, new PreySelector());
    }

    public PursuitBehavior() {
        this(1.2, 0.15, 1.0, 64.0, new PreySelector());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof Mob predator)) {
            return new Vec3d();
        }

        // Update chase state
        updateChaseState(predator);

        // Find or validate current prey
        Entity prey = findPrey(predator);
        if (prey == null || !prey.isAlive()) {
            currentPrey = null;
            currentState = HuntingState.IDLE;
            return new Vec3d();
        }

        currentPrey = prey;

        // Check if prey is too far away
        Vec3d predatorPos = context.getPosition();
        Vec3d preyPos = new Vec3d(prey.getX(), prey.getY(), prey.getZ());
        double distance = predatorPos.distanceTo(preyPos);

        if (distance > giveUpDistance) {
            currentState = HuntingState.IDLE;
            currentPrey = null;
            return new Vec3d();
        }

        // Determine behavior based on state
        if (distance < 2.0) {
            currentState = HuntingState.ATTACKING;
            return new Vec3d(); // Close enough to attack
        } else if (distance < 16.0) {
            currentState = HuntingState.CHASING;
            return calculatePursuit(predatorPos, context.getVelocity(), prey, preyPos);
        } else {
            currentState = HuntingState.STALKING;
            return calculateStalk(predatorPos, prey, preyPos);
        }
    }

    /**
     * Calculates pursuit force with interception prediction using constant bearing strategy.
     * Based on research: predators use interception course rather than direct pursuit.
     * The constant bearing strategy predicts where prey will be based on relative velocities.
     */
    private Vec3d calculatePursuit(Vec3d predatorPos, Vec3d predatorVelocity,
                                    Entity prey, Vec3d preyPos) {
        // Get prey velocity
        Vec3d preyVelocity = new Vec3d(
            prey.getDeltaMovement().x,
            prey.getDeltaMovement().y,
            prey.getDeltaMovement().z
        );

        // Calculate relative velocity (prey relative to predator)
        Vec3d relativeVelocity = Vec3d.sub(preyVelocity, predatorVelocity);

        // Calculate current distance to prey
        Vec3d toPrey = Vec3d.sub(preyPos, predatorPos);
        double distance = toPrey.magnitude();

        // Calculate time to intercept using constant bearing strategy
        // t = distance / closing_speed where closing_speed is speed along the line of sight
        double closingSpeed = 0.0;
        if (distance > 0.001) {
            Vec3d toPreyNormalized = toPrey.copy();
            toPreyNormalized.normalize();
            closingSpeed = -relativeVelocity.dot(toPreyNormalized);
        }

        // Only predict if we're closing in
        double interceptTime = predictionTime;
        if (closingSpeed > 0.01) {
            interceptTime = Math.min(distance / closingSpeed, predictionTime * 2.0);
        }

        // Predict where prey will be at intercept time
        Vec3d predictedPreyPos = preyPos.copy();
        Vec3d prediction = preyVelocity.copy();
        prediction.mult(interceptTime);
        predictedPreyPos.add(prediction);

        // Seek predicted position (constant bearing interception)
        Vec3d steer = seek(predatorPos, predatorVelocity, predictedPreyPos, maxPursuitSpeed);
        steer.limit(maxPursuitForce);
        return steer;
    }

    /**
     * Calculates stalking behavior - slower, more approach.
     */
    private Vec3d calculateStalk(Vec3d predatorPos, Entity prey, Vec3d preyPos) {
        // Move slowly toward prey, staying downwind if possible
        Vec3d toPrey = Vec3d.sub(preyPos, predatorPos);
        toPrey.normalize();
        toPrey.mult(maxPursuitSpeed * 0.5); // Slower when stalking
        return toPrey;
    }

    private Entity findPrey(Mob predator) {
        if (currentPrey != null && currentPrey.isAlive()) {
            double distance = predator.position().distanceTo(currentPrey.position());
            if (distance < giveUpDistance) {
                return currentPrey;
            }
        }

        return preySelector.selectPrey(predator);
    }

    private void updateChaseState(Mob predator) {
        if (currentState == HuntingState.CHASING || currentState == HuntingState.ATTACKING) {
            chaseTimer++;

            // Give up if chasing too long without success
            if (chaseTimer > 600) { // 30 seconds
                currentState = HuntingState.RESTING;
                chaseTimer = 0;
                currentPrey = null;
            }
        } else if (currentState == HuntingState.RESTING) {
            chaseTimer++;
            if (chaseTimer > 200) { // 10 seconds rest
                currentState = HuntingState.SEARCHING;
                chaseTimer = 0;
            }
        }
    }

    public Entity getCurrentPrey() {
        return currentPrey;
    }

    public HuntingState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(HuntingState state) {
        this.currentState = state;
    }

    public void setCurrentPrey(Entity prey) {
        this.currentPrey = prey;
    }
}
