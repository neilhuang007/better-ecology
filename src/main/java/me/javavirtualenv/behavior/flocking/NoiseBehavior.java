package me.javavirtualenv.behavior.flocking;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Noise behavior - adds small random perturbations for natural movement.
 * Prevents perfectly uniform flocking and adds realism to entity movement.
 */
public class NoiseBehavior extends SteeringBehavior {

    private final Random random;

    public NoiseBehavior(double weight) {
        this.weight = weight;
        this.random = ThreadLocalRandom.current();
    }

    public NoiseBehavior() {
        this(0.3);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        return new Vec3d(
            random.nextDouble() * 2.0 - 1.0,
            random.nextDouble() * 2.0 - 1.0,
            random.nextDouble() * 2.0 - 1.0
        );
    }
}
