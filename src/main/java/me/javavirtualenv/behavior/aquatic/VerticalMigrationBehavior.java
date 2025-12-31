package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Vertical migration behavior for squid and glow squid.
 * Simulates diel vertical migration - moving to deeper waters during day
 * and surface waters at night.
 * <p>
 * Scientific basis: Many cephalopods exhibit vertical migration patterns,
 * following prey and avoiding predators. Glow squid in Minecraft naturally
 * spawn in deep water but may migrate vertically.
 */
public class VerticalMigrationBehavior extends SteeringBehavior {
    private final AquaticConfig config;
    private long lastMigrationCheck = 0;

    public VerticalMigrationBehavior(AquaticConfig config) {
        super(0.8, true);
        this.config = config;
    }

    public VerticalMigrationBehavior() {
        this(AquaticConfig.createForSquid());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();
        Level level = context.getLevel();
        long currentTime = level.getGameTime();

        // Only check migration periodically
        if (currentTime - lastMigrationCheck < config.getVerticalMigrationInterval()) {
            return new Vec3d();
        }

        lastMigrationCheck = currentTime;

        // Get current depth
        double currentY = context.getPosition().y;
        long timeOfDay = level.getDayTime() % 24000;

        // Determine target depth based on time of day
        double targetY;
        boolean isDay = timeOfDay >= 0 && timeOfDay < 13000;

        if (isDay) {
            // During day, seek deeper waters (45-55)
            targetY = 50.0;
        } else {
            // At night, move closer to surface (55-62)
            targetY = 58.0;
        }

        // Check if already at target depth (with tolerance)
        if (Math.abs(currentY - targetY) < 3.0) {
            return new Vec3d();
        }

        // Calculate vertical steering force
        return calculateVerticalForce(context, targetY);
    }

    private Vec3d calculateVerticalForce(BehaviorContext context, double targetY) {
        Vec3d position = context.getPosition();
        double currentY = position.y;

        Vec3d desired = new Vec3d(0, targetY - currentY, 0);

        // Normalize and scale
        double distance = Math.abs(targetY - currentY);
        desired.normalize();

        // Slow down as approaching target
        double speed;
        if (distance < 5.0) {
            speed = config.getMaxSpeed() * (distance / 5.0);
        } else {
            speed = config.getMaxSpeed();
        }

        desired.mult(speed);

        // Subtract current velocity to get steering force
        Vec3d steering = Vec3d.sub(desired, context.getVelocity());

        // Limit the force
        if (steering.magnitude() > config.getMaxForce()) {
            steering.normalize();
            steering.mult(config.getMaxForce());
        }

        return steering;
    }

    public boolean shouldSeekSurface(Level level) {
        long timeOfDay = level.getDayTime() % 24000;
        return timeOfDay >= 13000; // Night time
    }

    public boolean shouldSeekDepth(Level level) {
        long timeOfDay = level.getDayTime() % 24000;
        return timeOfDay >= 0 && timeOfDay < 13000; // Day time
    }
}
