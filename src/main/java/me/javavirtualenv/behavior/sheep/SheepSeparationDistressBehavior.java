package me.javavirtualenv.behavior.sheep;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;

import java.util.List;

/**
 * Separation distress behavior for sheep.
 * When separated from their flock, sheep will bleat frequently and move more urgently to regroup.
 * This is stronger than the generic parent-offspring separation distress.
 */
public class SheepSeparationDistressBehavior extends SteeringBehavior {

    private final double distressRadius;
    private final double maxSpeed;
    private final double maxForce;
    private final int bleatInterval;
    private int lastBleatTick;

    public SheepSeparationDistressBehavior(double distressRadius, double maxSpeed, double maxForce, int bleatInterval) {
        this.distressRadius = distressRadius;
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
        this.bleatInterval = bleatInterval;
        this.lastBleatTick = 0;
    }

    public SheepSeparationDistressBehavior() {
        this(16.0, 1.2, 0.3, 60); // Bleat every 3 seconds when distressed
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();
        List<Entity> neighbors = context.getNeighbors();

        if (neighbors == null || neighbors.isEmpty()) {
            // No neighbors at all - maximum distress
            triggerDistressBehavior(mob, 1.0);
            return new Vec3d(); // No steering, just emotional response
        }

        Vec3d position = context.getPosition();
        double nearestSheepDistance = Double.MAX_VALUE;
        Vec3d nearestSheepPos = null;
        int sheepCount = 0;

        // Find nearest sheep
        for (Entity neighbor : neighbors) {
            if (neighbor.equals(mob)) {
                continue;
            }

            if (!(neighbor instanceof Sheep)) {
                continue;
            }

            Vec3d neighborPos = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            double distance = position.distanceTo(neighborPos);

            if (distance < distressRadius) {
                sheepCount++;
                if (distance < nearestSheepDistance) {
                    nearestSheepDistance = distance;
                    nearestSheepPos = neighborPos;
                }
            }
        }

        // Calculate distress level (0.0 to 1.0)
        double distressLevel;
        if (sheepCount == 0) {
            distressLevel = 1.0; // Maximum distress
        } else if (nearestSheepDistance < 6.0) {
            distressLevel = 0.0; // Comfortable
        } else {
            distressLevel = Math.min(1.0, (nearestSheepDistance - 6.0) / (distressRadius - 6.0));
        }

        // Trigger distress behaviors if needed
        if (distressLevel > 0.3) {
            triggerDistressBehavior(mob, distressLevel);
        }

        // If distressed and have a target sheep, move toward it
        if (distressLevel > 0.5 && nearestSheepPos != null) {
            Vec3d desired = Vec3d.sub(nearestSheepPos, position);
            desired.normalize();
            desired.mult(maxSpeed * (1.0 + distressLevel * 0.5)); // Move faster when distressed

            Vec3d steer = Vec3d.sub(desired, context.getVelocity());
            steer.limit(maxForce * (1.0 + distressLevel));

            steer.mult(weight);
            return steer;
        }

        return new Vec3d();
    }

    /**
     * Triggers distress behaviors like bleating and particle effects.
     */
    private void triggerDistressBehavior(Mob mob, double distressLevel) {
        int currentTick = mob.tickCount;

        // Bleat more frequently when more distressed
        int adjustedInterval = (int) (bleatInterval * (1.0 - distressLevel * 0.7));
        if (currentTick - lastBleatTick >= adjustedInterval) {
            bleat(mob, distressLevel);
            lastBleatTick = currentTick;
        }

        // Consider marking entity as distressed for other systems
        // This could be used by other behaviors to prioritize regrouping
    }

    /**
     * Plays a distress bleat sound.
     */
    private void bleat(Mob mob, double distressLevel) {
        // Higher pitch for more distressed sheep
        float pitch = 1.0F + (float) (distressLevel * 0.3F);
        float volume = 0.8F + (float) (distressLevel * 0.4F);

        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                             SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, volume, pitch);

        // Spawn particles to indicate distress
        if (mob.level() instanceof ServerLevel serverLevel) {
            spawnDistressParticles(mob, serverLevel, distressLevel);
        }
    }

    private void spawnDistressParticles(Mob mob, ServerLevel level, double distressLevel) {
        // Spawn worried particles (more particles when more distressed)
        int particleCount = (int) (3 + distressLevel * 5);

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (mob.getRandom().nextDouble() - 0.5) * 1.0;
            double offsetY = mob.getRandom().nextDouble() * 0.5;
            double offsetZ = (mob.getRandom().nextDouble() - 0.5) * 1.0;

            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.WORRY,
                    mob.getX() + offsetX,
                    mob.getY() + 1.0 + offsetY,
                    mob.getZ() + offsetZ,
                    1, 0, 0, 0, 0.02
            );
        }
    }

    /**
     * Checks if a sheep is currently in distress.
     */
    public boolean isDistressed(BehaviorContext context) {
        List<Entity> neighbors = context.getNeighbors();
        if (neighbors == null || neighbors.isEmpty()) {
            return true;
        }

        Vec3d position = context.getPosition();
        double nearestDistance = Double.MAX_VALUE;
        int sheepCount = 0;

        for (Entity neighbor : neighbors) {
            if (neighbor.equals(context.getEntity())) {
                continue;
            }

            if (!(neighbor instanceof Sheep)) {
                continue;
            }

            Vec3d neighborPos = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            double distance = position.distanceTo(neighborPos);

            if (distance < distressRadius) {
                sheepCount++;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                }
            }
        }

        return sheepCount == 0 || nearestDistance > 8.0;
    }

    public void setDistressRadius(double distressRadius) {
        this.distressRadius = distressRadius;
    }

    public double getDistressRadius() {
        return distressRadius;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxForce(double maxForce) {
        this.maxForce = maxForce;
    }

    public double getMaxForce() {
        return maxForce;
    }

    public void setBleatInterval(int bleatInterval) {
        this.bleatInterval = bleatInterval;
    }

    public int getBleatInterval() {
        return bleatInterval;
    }
}
