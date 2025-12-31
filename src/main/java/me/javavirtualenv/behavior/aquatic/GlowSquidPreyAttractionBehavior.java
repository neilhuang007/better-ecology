package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.GlowSquid;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Glow squid prey attraction behavior using glowing particles.
 * Glow squid particles attract small prey (other fish) which then get hunted.
 * <p>
 * Scientific basis: Bioluminescence in deep-sea creatures attracts prey
 * through curiosity feeding responses. Small organisms are drawn to light sources.
 */
public class GlowSquidPreyAttractionBehavior extends SteeringBehavior {
    private final AquaticConfig config;

    public GlowSquidPreyAttractionBehavior(AquaticConfig config) {
        super(0.5, true);
        this.config = config;
    }

    public GlowSquidPreyAttractionBehavior() {
        this(AquaticConfig.createForSquid());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();

        // Only applies to glow squid
        if (!(self instanceof GlowSquid)) {
            return new Vec3d();
        }

        // Find nearby prey attracted by glow
        List<Entity> nearbyPrey = findAttractedPrey(context);

        if (nearbyPrey.isEmpty()) {
            return new Vec3d();
        }

        // Glow squid should move towards attracted prey
        return moveTowardsNearestPrey(context, nearbyPrey);
    }

    private List<Entity> findAttractedPrey(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        double attractionRadius = 8.0;

        AABB searchBox = new AABB(
            position.x - attractionRadius, position.y - attractionRadius, position.z - attractionRadius,
            position.x + attractionRadius, position.y + attractionRadius, position.z + attractionRadius
        );

        return self.level().getEntities(self, searchBox, entity -> {
            if (entity == self) return false;
            if (!entity.isAlive()) return false;

            String entityId = net.minecraft.core.Registry.ENTITY_TYPE.getKey(entity.getType()).toString();

            // Small fish that are attracted to glow
            return entityId.equals("minecraft:cod") ||
                   entityId.equals("minecraft:salmon") ||
                   entityId.equals("minecraft:tropical_fish");
        });
    }

    private Vec3d moveTowardsNearestPrey(BehaviorContext context, List<Entity> prey) {
        Vec3d position = context.getPosition();
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity p : prey) {
            Vec3d preyPos = new Vec3d(p.getX(), p.getY(), p.getZ());
            double distance = position.distanceTo(preyPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = p;
            }
        }

        if (nearest == null) {
            return new Vec3d();
        }

        // Seek towards prey
        Vec3d targetPos = new Vec3d(nearest.getX(), nearest.getY(), nearest.getZ());
        Vec3d desired = Vec3d.sub(targetPos, position);
        desired.normalize();
        desired.mult(config.getMaxSpeed() * 0.8);

        Vec3d steering = Vec3d.sub(desired, context.getVelocity());

        // Limit force
        if (steering.magnitude() > config.getMaxForce() * 0.5) {
            steering.normalize();
            steering.mult(config.getMaxForce() * 0.5);
        }

        return steering;
    }

    /**
     * Check if prey is in attack range.
     */
    public boolean isPreyInAttackRange(BehaviorContext context, Entity prey) {
        Vec3d position = context.getPosition();
        Vec3d preyPos = new Vec3d(prey.getX(), prey.getY(), prey.getZ());
        return position.distanceTo(preyPos) < 2.0;
    }
}
