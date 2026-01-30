package me.javavirtualenv.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import me.javavirtualenv.behavior.core.PopulationCounter;

/**
 * Utility class for checking spawn conditions based on ecological requirements.
 *
 * <p>Checks include:
 * <ul>
 *   <li>Water source proximity (for drinking animals)</li>
 *   <li>Height restrictions (surface spawning)</li>
 *   <li>Territorial spacing (minimum distance from same species)</li>
 *   <li>Population density limits</li>
 * </ul>
 */
public final class SpawnConditionChecker {

    private SpawnConditionChecker() {
        // Utility class
    }

    /**
     * Checks if water is nearby within the specified radius.
     *
     * @param level the world level
     * @param pos the position to check from
     * @param radius the search radius in blocks
     * @return true if water is found within radius
     */
    public static boolean hasWaterNearby(Level level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    FluidState fluidState = level.getFluidState(mutable);
                    if (fluidState.is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the position is at a valid height for spawning.
     *
     * @param level the world level
     * @param pos the position to check
     * @param minY the minimum Y coordinate
     * @param maxY the maximum Y coordinate
     * @return true if the position is within the valid height range
     */
    public static boolean isValidHeight(Level level, BlockPos pos, int minY, int maxY) {
        int y = pos.getY();
        return y >= minY && y <= maxY;
    }

    /**
     * Checks if there is space for a new group (territorial spacing).
     *
     * @param level the world level
     * @param pos the proposed spawn position
     * @param type the animal class to check
     * @param territoryRadius the minimum distance from existing groups
     * @return true if no animals of this type exist within territory radius
     */
    public static boolean hasSpaceForGroup(Level level, BlockPos pos,
                                           Class<? extends Animal> type,
                                           int territoryRadius) {
        return PopulationCounter.hasSpaceForGroup(level, pos, type, territoryRadius);
    }

    /**
     * Checks if the population is below carrying capacity.
     *
     * @param level the world level
     * @param pos the position to check from
     * @param type the animal class to count
     * @param checkRadius the radius to check for existing animals
     * @param carryingCapacity the maximum allowed population
     * @return true if population is below carrying capacity
     */
    public static boolean isBelowCarryingCapacity(Level level, BlockPos pos,
                                                   Class<? extends Animal> type,
                                                   int checkRadius, int carryingCapacity) {
        return !PopulationCounter.isAtCarryingCapacity(
            level, pos, type, checkRadius, carryingCapacity);
    }

    /**
     * Checks if the block below is a valid spawn surface.
     *
     * @param level the world level
     * @param pos the position to check (the block the mob would stand on)
     * @return true if the block is a valid spawn surface
     */
    public static boolean isValidSpawnSurface(Level level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        if (!belowState.isSolid()) {
            return false;
        }

        BlockState atPos = level.getBlockState(pos);
        return atPos.isAir() || atPos.canBeReplaced();
    }

    /**
     * Performs a combined check for all spawn requirements.
     *
     * @param level the world level
     * @param pos the proposed spawn position
     * @param type the animal class
     * @param requirements the spawn requirements to check
     * @return true if all requirements are met
     */
    public static boolean canSpawnAt(Level level, BlockPos pos,
                                     Class<? extends Animal> type,
                                     SpawnRequirements requirements) {
        // Check spawn surface
        if (!isValidSpawnSurface(level, pos)) {
            return false;
        }

        // Check height
        if (!isValidHeight(level, pos, requirements.minY(), requirements.maxY())) {
            return false;
        }

        // Check water requirement
        if (requirements.requiresWater()) {
            if (!hasWaterNearby(level, pos, requirements.maxWaterDistance())) {
                return false;
            }
        }

        // Check territorial spacing
        if (requirements.territoryRadius() > 0) {
            if (!hasSpaceForGroup(level, pos, type, requirements.territoryRadius())) {
                return false;
            }
        }

        // Check population density
        if (requirements.maxPopulationInArea() > 0) {
            if (!isBelowCarryingCapacity(level, pos, type,
                    requirements.populationCheckRadius(),
                    requirements.maxPopulationInArea())) {
                return false;
            }
        }

        return true;
    }
}
