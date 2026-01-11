package me.javavirtualenv.behavior.strider;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.biome.Biome;

/**
 * Behavior for strider temperature seeking mechanics.
 * <p>
 * This behavior:
 * - Actively seeks warm areas (lava)
 * - Freezes in cold/overworld environments
 * - Has shivering animation when cold
 * - Prefers basalt deltas and other warm nether biomes
 * - Will panic when in cold areas too long
 */
public class TemperatureSeekingBehavior extends SteeringBehavior {

    private static final double TEMPERATURE_SEARCH_RADIUS = 24.0;
    private static final double OPTIMAL_TEMPERATURE = 2.0; // Nether-like temperature
    private static final double FREEZING_THRESHOLD = 0.3; // Cold enough to cause damage
    private static final int PANIC_THRESHOLD_TICKS = 600; // 30 seconds before panic

    private double currentTemperature;
    private boolean isFreezing;
    private boolean isPanicking;
    private int coldTicks;
    private BlockPos warmZoneTarget;

    public TemperatureSeekingBehavior() {
        this(1.0);
    }

    public TemperatureSeekingBehavior(double weight) {
        this.currentTemperature = 0.5;
        setWeight(weight);
        this.isFreezing = false;
        this.isPanicking = false;
        this.coldTicks = 0;
        this.warmZoneTarget = null;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Strider strider)) {
            return new Vec3d();
        }

        Level level = context.getWorld();
        Vec3d position = context.getPosition();

        // Update temperature reading
        updateTemperature(strider, level, position);

        // Handle freezing state
        if (isFreezing || isPanicking) {
            return seekWarmth(strider, position, level, context);
        }

        // If comfortable, minimal movement
        return new Vec3d();
    }

    /**
     * Updates the current temperature based on environment.
     */
    private void updateTemperature(Strider strider, Level level, Vec3d position) {
        BlockPos blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);

        // Get biome temperature
        Biome biome = level.getBiome(blockPos).value();
        double biomeTemp = biome.getBaseTemperature();

        // Check for lava proximity
        boolean nearLava = isNearLava(strider, position, level);

        // Calculate environmental temperature
        double environmentalTemp = biomeTemp;
        if (nearLava) {
            environmentalTemp += 1.5; // Lava provides significant warmth
        }

        // Check if in fluid
        FluidState fluidState = level.getFluidState(blockPos);
        if (fluidState.is(Fluids.LAVA)) {
            environmentalTemp = 2.0; // Maximum warmth in lava
        } else if (fluidState.is(Fluids.WATER)) {
            environmentalTemp = 0.0; // Freezing in water
            isFreezing = true;
        }

        // Smooth temperature transition
        currentTemperature = lerp(currentTemperature, environmentalTemp, 0.05);

        // Update freezing state
        if (currentTemperature < FREEZING_THRESHOLD) {
            coldTicks++;
            if (coldTicks > 100) { // 5 seconds of cold exposure
                isFreezing = true;
            }
            if (coldTicks > PANIC_THRESHOLD_TICKS) {
                isPanicking = true;
            }
        } else {
            coldTicks = Math.max(0, coldTicks - 2);
            if (coldTicks == 0) {
                isFreezing = false;
                isPanicking = false;
            }
        }
    }

    /**
     * Checks if the strider is near lava.
     */
    private boolean isNearLava(Strider strider, Vec3d position, Level level) {
        BlockPos striderPos = strider.blockPosition();
        int searchRadius = 8;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = striderPos.offset(x, y, z);
                    FluidState fluidState = level.getFluidState(testPos);
                    if (fluidState.is(Fluids.LAVA)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Seeks warmth when freezing or panicking.
     */
    private Vec3d seekWarmth(Strider strider, Vec3d position, Level level, BehaviorContext context) {
        // Find warm zone target if we don't have one
        if (warmZoneTarget == null || !isValidWarmZone(level, warmZoneTarget)) {
            warmZoneTarget = findWarmestZone(strider, position, level);
        }

        if (warmZoneTarget == null) {
            return new Vec3d();
        }

        Vec3d warmPos = new Vec3d(
            warmZoneTarget.getX() + 0.5,
            warmZoneTarget.getY(),
            warmZoneTarget.getZ() + 0.5
        );

        // Calculate steering force toward warmth
        Vec3d desired = Vec3d.sub(warmPos, position);
        desired.normalize();

        // Move faster if panicking
        double speedMultiplier = isPanicking ? 2.0 : 1.3;
        desired.mult(context.getMaxSpeed() * speedMultiplier);

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        return limitForce(steer, context.getMaxForce());
    }

    /**
     * Finds the warmest zone nearby.
     */
    private BlockPos findWarmestZone(Strider strider, Vec3d position, Level level) {
        BlockPos striderPos = strider.blockPosition();
        int searchRadius = (int) TEMPERATURE_SEARCH_RADIUS;

        BlockPos warmest = null;
        double warmestTemp = Double.NEGATIVE_INFINITY;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = striderPos.offset(x, y, z);

                    double temp = calculateZoneTemperature(level, testPos);
                    if (temp > warmestTemp) {
                        warmestTemp = temp;
                        warmest = testPos;
                    }
                }
            }
        }

        return warmest;
    }

    /**
     * Calculates temperature at a specific zone.
     */
    private double calculateZoneTemperature(Level level, BlockPos pos) {
        // Get biome temperature
        Biome biome = level.getBiome(pos).value();
        double temp = biome.getBaseTemperature();

        // Check for lava
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.is(Fluids.LAVA)) {
            temp = 2.0; // Maximum warmth
        } else if (fluidState.is(Fluids.WATER)) {
            temp = 0.0; // Freezing
        }

        // Check for fire blocks
        if (level.getBlockState(pos).is(Blocks.FIRE) ||
            level.getBlockState(pos).is(Blocks.MAGMA_BLOCK) ||
            level.getBlockState(pos).is(Blocks.CAMPFIRE)) {
            temp += 0.5;
        }

        return temp;
    }

    /**
     * Checks if a warm zone is still valid.
     */
    private boolean isValidWarmZone(Level level, BlockPos pos) {
        double temp = calculateZoneTemperature(level, pos);
        return temp >= OPTIMAL_TEMPERATURE * 0.5;
    }

    /**
     * Linear interpolation utility.
     */
    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Gets the current temperature reading.
     */
    public double getCurrentTemperature() {
        return currentTemperature;
    }

    /**
     * Checks if the strider is freezing.
     */
    public boolean isFreezing() {
        return isFreezing;
    }

    /**
     * Checks if the strider is panicking.
     */
    public boolean isPanicking() {
        return isPanicking;
    }

    /**
     * Gets the number of ticks the strider has been cold.
     */
    public int getColdTicks() {
        return coldTicks;
    }

    /**
     * Warms up the strider.
     */
    public void warmUp(double amount) {
        currentTemperature = Math.min(2.0, currentTemperature + amount);
        coldTicks = Math.max(0, coldTicks - (int) (amount * 100));
        if (coldTicks == 0) {
            isFreezing = false;
            isPanicking = false;
        }
    }
}
