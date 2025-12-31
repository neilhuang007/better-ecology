package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Mob;

/**
 * Creeping behavior for cautious approach.
 * <p>
 * Unlike stalking (which is hunting-focused), creeping is used for
 * general cautious movement. Cats creep when:
 * - Exploring new areas
 * - Approaching unfamiliar objects
 * - Moving through open spaces
 * - Being watchful of threats
 */
public class CreepingBehavior extends SteeringBehavior {

    private final double creepSpeed;
    private final double alertThreshold;
    private final double creepDuration;

    private boolean isCreeping = false;
    private int creepTicks = 0;

    public CreepingBehavior(double creepSpeed, double alertThreshold, double creepDuration) {
        super(0.8);
        this.creepSpeed = creepSpeed;
        this.alertThreshold = alertThreshold;
        this.creepDuration = creepDuration;
    }

    public CreepingBehavior() {
        this(0.2, 0.5, 100);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Check if should be creeping
        if (!shouldCreep(mob, context)) {
            if (isCreeping) {
                creepTicks--;
                if (creepTicks <= 0) {
                    isCreeping = false;
                }
            }
            return new Vec3d();
        }

        isCreeping = true;
        creepTicks = (int) creepDuration;

        // Move slowly and cautiously
        Vec3d creepForce = new Vec3d(
            mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * creepSpeed,
            0,
            mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * creepSpeed
        );

        // Add small random variations for cautious movement
        creepForce.x += (Math.random() - 0.5) * 0.1;
        creepForce.z += (Math.random() - 0.5) * 0.1;

        return creepForce;
    }

    private boolean shouldCreep(Mob mob, BehaviorContext context) {
        // Creep when in unfamiliar territory
        // Creep when recently detected a threat
        // Creep when moving through open areas

        double healthPercent = mob.getHealth() / mob.getMaxHealth();
        return healthPercent < alertThreshold;
    }

    public boolean isCreeping() {
        return isCreeping;
    }

    public void startCreeping() {
        isCreeping = true;
        creepTicks = (int) creepDuration;
    }

    public void stopCreeping() {
        isCreeping = false;
        creepTicks = 0;
    }
}
