package me.javavirtualenv.behavior.frog;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * Swimming behavior for frogs.
 * <p>
 * Implements efficient underwater swimming and surface navigation based on real frog swimming patterns:
 * - Powerful leg kicks for propulsion
 * - Prefer to stay near water surface
 * - Navigate toward lily pads and vegetation
 * - Climb vines and reeds from water
 * <p>
 * Scientific basis:
 * - Frogs use powerful hind legs for swimming
 * - Webbed feet provide efficient propulsion
 * - Prefer shallow water with emergent vegetation
 * - Can climb vertical surfaces using adhesive toe pads (some species)
 */
public class FrogSwimmingBehavior extends SteeringBehavior {

    private static final double SWIM_SPEED = 0.4;
    private static final double SURFACE_PREFERENCE = 0.3;
    private static final double LILY_PAD_ATTRACTION = 0.2;
    private static final double VEGETATION_ATTRACTION = 0.15;

    public FrogSwimmingBehavior() {
        super();
        setWeight(1.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getSelf();
        if (!(entity instanceof Frog frog)) {
            return new Vec3d();
        }

        // Only active when in water
        if (!frog.isInWater()) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();
        Vec3d force = new Vec3d();

        // Add surface preference
        Vec3d surfacePref = calculateSurfacePreference(frog, position);
        force.add(surfacePref);

        // Add lily pad attraction
        Vec3d lilyAttraction = calculateLilyPadAttraction(frog, position);
        force.add(lilyAttraction);

        // Add vegetation attraction
        Vec3d vegAttraction = calculateVegetationAttraction(frog, position);
        force.add(vegAttraction);

        // Add random swimming motion
        if (force.magnitude() < 0.01) {
            force = generateRandomSwimForce(frog);
        }

        // Limit the force to max speed
        if (force.magnitude() > SWIM_SPEED) {
            force.normalize();
            force.mult(SWIM_SPEED);
        }

        return force;
    }

    /**
     * Calculates force to keep frog near water surface.
     */
    private Vec3d calculateSurfacePreference(Frog frog, Vec3d position) {
        // Find water surface level
        BlockPos frogPos = new BlockPos((int) frog.getX(), (int) frog.getY(), (int) frog.getZ());
        int surfaceY = findWaterSurfaceY(frog, frogPos);

        double depth = position.y - surfaceY;

        // If too deep, swim up
        if (depth < -0.5) {
            Vec3d upForce = new Vec3d(0, SURFACE_PREFERENCE, 0);
            return upForce;
        }

        // If at surface, slight downward force to stay partially submerged
        if (depth > 0.5) {
            Vec3d downForce = new Vec3d(0, -SURFACE_PREFERENCE * 0.5, 0);
            return downForce;
        }

        return new Vec3d();
    }

    /**
     * Finds the Y level of the water surface.
     */
    private int findWaterSurfaceY(Frog frog, BlockPos pos) {
        int y = pos.getY();

        // Search upward for water surface
        while (y < frog.level().getMaxBuildHeight()) {
            BlockPos checkPos = new BlockPos(pos.getX(), y + 1, pos.getZ());
            BlockState state = frog.level().getBlockState(checkPos);

            if (!state.is(Blocks.WATER) && !state.is(Blocks.SEAGRASS)
                    && !state.is(Blocks.TALL_SEAGRASS)) {
                return y;
            }
            y++;
        }

        return pos.getY();
    }

    /**
     * Calculates attraction to nearby lily pads.
     */
    private Vec3d calculateLilyPadAttraction(Frog frog, Vec3d position) {
        BlockPos nearestLilyPad = findNearestLilyPad(frog, position);

        if (nearestLilyPad == null) {
            return new Vec3d();
        }

        Vec3d lilyPos = new Vec3d(
                nearestLilyPad.getX() + 0.5,
                nearestLilyPad.getY(),
                nearestLilyPad.getZ() + 0.5
        );

        Vec3d direction = Vec3d.sub(lilyPos, position);
        double distance = direction.magnitude();

        if (distance < 0.5) {
            return new Vec3d(); // Already on lily pad
        }

        direction.normalize();
        direction.mult(LILY_PAD_ATTRACTION);

        return direction;
    }

    /**
     * Finds the nearest lily pad within search radius.
     */
    private BlockPos findNearestLilyPad(Frog frog, Vec3d position) {
        int searchRadius = 8;
        BlockPos frogPos = new BlockPos((int) frog.getX(), (int) frog.getY(), (int) frog.getZ());

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = frogPos.offset(x, y, z);
                    BlockState state = frog.level().getBlockState(checkPos);

                    if (state.is(Blocks.LILY_PAD)) {
                        Vec3d blockPos = new Vec3d(
                                checkPos.getX() + 0.5,
                                checkPos.getY(),
                                checkPos.getZ() + 0.5
                        );
                        double distance = position.distanceTo(blockPos);

                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Calculates attraction to vegetation (reeds, vines near water).
     */
    private Vec3d calculateVegetationAttraction(Frog frog, Vec3d position) {
        BlockPos nearestVegetation = findNearestVegetation(frog, position);

        if (nearestVegetation == null) {
            return new Vec3d();
        }

        Vec3d vegPos = new Vec3d(
                nearestVegetation.getX() + 0.5,
                nearestVegetation.getY(),
                nearestVegetation.getZ() + 0.5
        );

        Vec3d direction = Vec3d.sub(vegPos, position);
        double distance = direction.magnitude();

        if (distance < 1.0) {
            return new Vec3d(); // Close enough
        }

        direction.normalize();
        direction.mult(VEGETATION_ATTRACTION);

        return direction;
    }

    /**
     * Finds the nearest climbable vegetation.
     */
    private BlockPos findNearestVegetation(Frog frog, Vec3d position) {
        int searchRadius = 6;
        BlockPos frogPos = new BlockPos((int) frog.getX(), (int) frog.getY(), (int) frog.getZ());

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = frogPos.offset(x, y, z);
                    BlockState state = frog.level().getBlockState(checkPos);

                    // Check for climbable vegetation
                    boolean isVegetation = state.is(Blocks.SUGAR_CANE)
                            || state.is(Blocks.VINE)
                            || state.is(Blocks.TALL_GRASS)
                            || state.is(Blocks.LILY_PAD);

                    if (isVegetation) {
                        Vec3d blockPos = new Vec3d(
                                checkPos.getX() + 0.5,
                                checkPos.getY(),
                                checkPos.getZ() + 0.5
                        );
                        double distance = position.distanceTo(blockPos);

                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Generates random swimming force when no specific target.
     */
    private Vec3d generateRandomSwimForce(Frog frog) {
        double angle = frog.getRandom().nextDouble() * Math.PI * 2;
        double magnitude = 0.05 + frog.getRandom().nextDouble() * 0.1;

        Vec3d force = new Vec3d(
                Math.cos(angle) * magnitude,
                0,
                Math.sin(angle) * magnitude
        );

        return force;
    }
}
