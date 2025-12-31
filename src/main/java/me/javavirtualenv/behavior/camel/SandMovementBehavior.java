package me.javavirtualenv.behavior.camel;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Sand movement behavior for camels.
 * <p>
 * Camels are specially adapted for moving on sand:
 * - No sinking in sand or soul sand
 * - Speed bonus on sand blocks
 * - Efficient desert traversal
 * - Special footprints (would need custom rendering)
 * <p>
 * Scientific basis: Camels have wide, padded feet that distribute weight
 * to prevent sinking in sand. Their unique gait allows efficient movement
 * across loose terrain.
 */
public class SandMovementBehavior extends SteeringBehavior {

    private final CamelConfig config;

    public SandMovementBehavior(CamelConfig config) {
        this.config = config;
    }

    public SandMovementBehavior() {
        this(CamelConfig.createDefault());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        if (!enabled) {
            return new Vec3d();
        }

        // This behavior doesn't add steering force
        // Instead, it modifies movement properties via mixin/flags

        return new Vec3d();
    }

    /**
     * Checks if the camel is on sand.
     */
    public static boolean isOnSand(Mob mob) {
        BlockPos belowPos = new BlockPos(mob.getX(), mob.getY() - 0.2, mob.getZ());
        BlockState blockState = mob.level().getBlockState(belowPos);
        Block block = blockState.getBlock();

        return block == Blocks.SAND ||
               block == Blocks.RED_SAND ||
               block == Blocks.SANDSTONE ||
               block == Blocks.RED_SANDSTONE;
    }

    /**
     * Checks if the camel is on soul sand.
     */
    public static boolean isOnSoulSand(Mob mob) {
        BlockPos belowPos = new BlockPos(mob.getX(), mob.getY() - 0.2, mob.getZ());
        BlockState blockState = mob.level().getBlockState(belowPos);
        Block block = blockState.getBlock();

        return block == Blocks.SOUL_SAND ||
               block == Blocks.SOUL_SOIL;
    }

    /**
     * Applies speed bonus when on sand.
     */
    public static double applySandSpeedBonus(Mob mob, double baseSpeed) {
        CamelConfig config = CamelConfig.createDefault();

        if (isOnSand(mob)) {
            return baseSpeed * (1.0 + config.getSandSpeedBonus());
        }

        if (isOnSoulSand(mob)) {
            return baseSpeed * (1.0 + config.getSoulSandSpeedBonus());
        }

        return baseSpeed;
    }

    /**
     * Checks if camel should avoid sinking in sand.
     */
    public static boolean shouldAvoidSinking(Mob mob) {
        CamelConfig config = CamelConfig.createDefault();

        if (isOnSand(mob)) {
            return config.isNoSandSink();
        }

        if (isOnSoulSand(mob)) {
            return config.isNoSoulSandSink();
        }

        return false;
    }

    /**
     * Gets the movement modifier for the current block.
     */
    public static double getMovementModifier(Mob mob) {
        if (isOnSand(mob)) {
            return 1.0 + CamelConfig.createDefault().getSandSpeedBonus();
        }

        if (isOnSoulSand(mob)) {
            return 1.0 + CamelConfig.createDefault().getSoulSandSpeedBonus();
        }

        return 1.0;
    }

    /**
     * Spawns footprint particles when moving on sand.
     */
    public static void spawnFootprintParticles(Mob mob) {
        if (!mob.level().isClientSide) {
            return;
        }

        if (!(isOnSand(mob) || isOnSoulSand(mob))) {
            return;
        }

        Vec3 pos = mob.position();
        double speed = mob.getDeltaMovement().length();

        // Only spawn footprints when moving
        if (speed < 0.05) {
            return;
        }

        // Spawn footprint particles at feet position
        // This would be handled by a client-side renderer in a full implementation
        // For now, we just note where footprints would appear
    }
}
