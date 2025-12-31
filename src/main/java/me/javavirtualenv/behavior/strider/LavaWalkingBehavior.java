package me.javavirtualenv.behavior.strider;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Strider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Behavior for strider lava walking mechanics.
 * <p>
 * This behavior:
 * - Enables walking on lava surface
 * - Provides heat resistance in lava
 * - Implements special movement patterns on lava
 * - Avoids freezing in cold/overworld environments
 * - Creates special movement effects on lava
 */
public class LavaWalkingBehavior extends SteeringBehavior {

    private static final double LAVA_SEARCH_RADIUS = 16.0;
    private static final double FREEZING_DAMAGE_THRESHOLD = 0.5;
    private static final double WARM_BONUS_MULTIPLIER = 1.3;

    private boolean isInLava;
    private boolean isFreezing;
    private double heatResistance;
    private int freezingTicks;

    public LavaWalkingBehavior() {
        this(1.0);
    }

    public LavaWalkingBehavior(double weight) {
        super(weight);
        this.isInLava = false;
        this.isFreezing = false;
        this.heatResistance = 1.0;
        this.freezingTicks = 0;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Strider strider)) {
            return new Vec3d();
        }

        Level level = context.getWorld();
        Vec3d position = context.getPosition();

        // Update state
        updateLavaState(strider, level, position);

        // Handle freezing damage
        if (isFreezing) {
            handleFreezing(strider);
        }

        // Calculate movement force
        if (isInLava) {
            return calculateLavaMovement(strider, position, context);
        }

        // If not in lava, seek lava
        return seekLava(strider, position, level, context);
    }

    /**
     * Updates whether the strider is in lava and its heat resistance.
     */
    private void updateLavaState(Strider strider, Level level, Vec3d position) {
        BlockPos blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
        BlockState blockState = level.getBlockState(blockPos);

        // Check if in or on lava
        FluidState fluidState = level.getFluidState(blockPos);
        boolean wasInLava = isInLava;
        isInLava = fluidState.is(Fluids.LAVA) || blockState.is(Blocks.LAVA);

        // Update heat resistance
        if (isInLava) {
            heatResistance = Math.min(2.0, heatResistance + 0.01);
            freezingTicks = Math.max(0, freezingTicks - 2);
        } else {
            heatResistance = Math.max(0.0, heatResistance - 0.005);
            freezingTicks++;

            // Start freezing if out of lava too long
            if (freezingTicks > 300) { // 15 seconds
                isFreezing = true;
            }
        }

        // Apply freezing effects
        if (isFreezing) {
            strider.setSpeed(strider.getSpeed() * 0.5);
        }
    }

    /**
     * Handles freezing damage and effects.
     */
    private void handleFreezing(Strider strider) {
        // Apply freezing damage periodically
        if (strider.level().getGameTime() % 40 == 0) {
            strider.hurt(strider.level().damageSources().onFire(), 1.0f);
        }

        // Spawn shivering particles
        if (!strider.level().isClientSide && strider.level().random.nextFloat() < 0.1f) {
            spawnShiveringParticles(strider);
        }
    }

    /**
     * Spawns shivering particles around the strider.
     */
    private void spawnShiveringParticles(Strider strider) {
        if (strider.level().isClientSide) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            double offsetX = strider.level().random.nextDouble() * 0.5 - 0.25;
            double offsetY = strider.level().random.nextDouble() * 0.5;
            double offsetZ = strider.level().random.nextDouble() * 0.5 - 0.25;

            strider.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                strider.getX() + offsetX,
                strider.getY() + offsetY,
                strider.getZ() + offsetZ,
                0.0, -0.1, 0.0
            );
        }
    }

    /**
     * Calculates movement force when on lava.
     */
    private Vec3d calculateLavaMovement(Strider strider, Vec3d position, BehaviorContext context) {
        // Enhanced movement on lava
        double speedMultiplier = 1.0 + (heatResistance * WARM_BONUS_MULTIPLIER);

        // Continue with current velocity but enhanced
        Vec3d currentVel = context.getVelocity().copy();
        currentVel.normalize();
        currentVel.mult(context.getMaxSpeed() * speedMultiplier);

        Vec3d steer = Vec3d.sub(currentVel, context.getVelocity());
        return limitForce(steer, context.getMaxForce());
    }

    /**
     * Seeks nearby lava when not in lava.
     */
    private Vec3d seekLava(Strider strider, Vec3d position, Level level, BehaviorContext context) {
        BlockPos nearestLava = findNearestLava(strider, position, level);

        if (nearestLava == null) {
            // No lava nearby, stop freezing panic
            if (freezingTicks < 600) {
                isFreezing = false;
            }
            return new Vec3d();
        }

        Vec3d lavaPos = new Vec3d(
            nearestLava.getX() + 0.5,
            nearestLava.getY(),
            nearestLava.getZ() + 0.5
        );

        double distance = position.distanceTo(lavaPos);

        // Calculate steering force toward lava
        Vec3d desired = Vec3d.sub(lavaPos, position);
        desired.normalize();

        // Move faster if freezing
        double speedMultiplier = isFreezing ? 1.5 : 1.0;
        desired.mult(context.getMaxSpeed() * speedMultiplier);

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        return limitForce(steer, context.getMaxForce());
    }

    /**
     * Finds the nearest lava block.
     */
    private BlockPos findNearestLava(Strider strider, Vec3d position, Level level) {
        BlockPos striderPos = strider.blockPosition();
        int searchRadius = (int) LAVA_SEARCH_RADIUS;

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = striderPos.offset(x, y, z);

                    FluidState fluidState = level.getFluidState(testPos);
                    if (fluidState.is(Fluids.LAVA)) {
                        double dist = position.distanceTo(new Vec3d(
                            testPos.getX() + 0.5,
                            testPos.getY(),
                            testPos.getZ() + 0.5
                        ));

                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = testPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Checks if the strider is currently in lava.
     */
    public boolean isInLava() {
        return isInLava;
    }

    /**
     * Checks if the strider is freezing.
     */
    public boolean isFreezing() {
        return isFreezing;
    }

    /**
     * Gets the current heat resistance level (0.0-2.0).
     */
    public double getHeatResistance() {
        return heatResistance;
    }

    /**
     * Gets the number of ticks the strider has been freezing.
     */
    public int getFreezingTicks() {
        return freezingTicks;
    }

    /**
     * Warms up the strider (called when in lava).
     */
    public void warmUp() {
        heatResistance = Math.min(2.0, heatResistance + 0.1);
        freezingTicks = Math.max(0, freezingTicks - 10);
        if (freezingTicks == 0) {
            isFreezing = false;
        }
    }
}
