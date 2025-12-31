package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.phys.Vec3;

/**
 * Tadpole metamorphosis and surface-seeking behavior.
 * Tadpoles swim to water surface and metamorphose into frogs over time.
 * <p>
 * Scientific basis: Tadpoles undergo metamorphosis, developing legs and
 * transitioning from aquatic to semi-aquatic lifestyle. They regularly
 * swim to surface to breathe air during development.
 */
public class TadpoleMetamorphosisBehavior extends SteeringBehavior {
    private final AquaticConfig config;

    public TadpoleMetamorphosisBehavior(AquaticConfig config) {
        super(1.0, true);
        this.config = config;
    }

    public TadpoleMetamorphosisBehavior() {
        this(AquaticConfig.createForTadpole());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();

        // Find water surface
        double surfaceY = findWaterSurface(context);

        if (surfaceY <= position.y) {
            return new Vec3d();
        }

        // Calculate steering towards surface
        return seekSurface(context, surfaceY);
    }

    private double findWaterSurface(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        int blockX = self.blockPosition().getX();
        int blockZ = self.blockPosition().getZ();

        // Search upward from current position to find water surface
        for (int y = (int) position.y + 1; y <= self.level().getMaxBuildHeight(); y++) {
            if (!self.level().getBlockState(new net.minecraft.core.BlockPos(blockX, y, blockZ)).isAir()) {
                continue;
            }
            // Found air, surface is one block below
            return y - 1;
        }

        // If no surface found, target sea level
        return 62.0;
    }

    private Vec3d seekSurface(BehaviorContext context, double surfaceY) {
        Vec3d position = context.getPosition();
        Vec3d currentVelocity = context.getVelocity();

        Vec3d target = new Vec3d(position.x, surfaceY, position.z);
        Vec3d desired = Vec3d.sub(target, position);

        double distance = Math.abs(surfaceY - position.y);

        // Slow down when approaching surface
        double speed;
        if (distance < 2.0) {
            speed = config.getMaxSpeed() * (distance / 2.0);
        } else {
            speed = config.getMaxSpeed();
        }

        desired.normalize();
        desired.mult(speed);

        Vec3d steering = Vec3d.sub(desired, currentVelocity);

        // Limit force
        if (steering.magnitude() > config.getMaxForce()) {
            steering.normalize();
            steering.mult(config.getMaxForce());
        }

        return steering;
    }

    /**
     * Check if tadpole is ready to metamorphose into a frog.
     * This should be called from the entity's tick method.
     */
    public boolean shouldMetamorphose(Entity tadpole, int age) {
        if (!(tadpole instanceof net.minecraft.world.entity.animal.Tadpole)) {
            return false;
        }

        return age >= config.getMetamorphosisTime();
    }

    /**
     * Transform tadpole into frog.
     * This should be called from the entity's tick method when metamorphosis is ready.
     */
    public Entity metamorphose(Entity tadpole) {
        if (!(tadpole instanceof net.minecraft.world.entity.animal.Tadpole)) {
            return null;
        }

        if (tadpole.level().isClientSide) {
            return null;
        }

        net.minecraft.world.entity.animal.Tadpole tadpoleEntity = (net.minecraft.world.entity.animal.Tadpole) tadpole;
        ServerLevel level = (ServerLevel) tadpoleEntity.level();

        // Determine frog variant based on biome
        FrogVariant variant = determineFrogVariant(level, tadpoleEntity.blockPosition());

        // Create frog at tadpole's position
        net.minecraft.world.entity.animal.Frog frog = EntityType.FROG.create(level);
        if (frog == null) {
            return null;
        }

        frog.moveTo(tadpoleEntity.getX(), tadpoleEntity.getY(), tadpoleEntity.getZ(),
                   tadpoleEntity.getYRot(), tadpoleEntity.getXRot());
        frog.setVariant(variant);

        // Copy age/persistence
        frog.setPersistenceRequired();

        // Spawn frog and remove tadpole
        level.addFreshEntityWithPassengers(frog);
        tadpoleEntity.discard();

        return frog;
    }

    private FrogVariant determineFrogVariant(ServerLevel level, net.minecraft.core.BlockPos pos) {
        // Get temperature at position
        float temperature = level.getBiome(pos).value().getTemperature();

        // Cold biome = temperate frog
        if (temperature < 0.5f) {
            return FrogVariant.TEMPERATE;
        }
        // Warm biome = warm frog
        else if (temperature > 0.8f) {
            return FrogVariant.WARM;
        }
        // Default = cold frog
        else {
            return FrogVariant.COLD;
        }
    }

    /**
     * Check if tadpole is near water surface (for air breathing behavior).
     */
    public boolean isNearSurface(BehaviorContext context) {
        Vec3d position = context.getPosition();
        double surfaceY = findWaterSurface(context);
        return Math.abs(surfaceY - position.y) < 1.5;
    }
}
