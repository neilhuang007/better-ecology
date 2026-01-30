package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility class for detecting player-protected areas.
 *
 * <p>This respects Minecraft's design principle: "It's up to the players to build the world."
 * If a player has fenced an area, animals should not eat crops or modify blocks there.
 *
 * <p>Detection methods:
 * <ul>
 *   <li>Fence blocks (any type) nearby indicate player ownership</li>
 *   <li>Fence gates indicate player ownership</li>
 *   <li>Walls (cobblestone, brick, etc.) indicate player ownership</li>
 *   <li>Farmland blocks suggest cultivated areas</li>
 * </ul>
 */
public final class ProtectedAreaDetection {

    private ProtectedAreaDetection() {
        // Utility class
    }

    /** Default radius to check for protection markers */
    private static final int DEFAULT_CHECK_RADIUS = 4;

    /**
     * Checks if a position is in a player-protected area.
     * Returns true if fences, walls, or other protection markers are nearby.
     *
     * @param level the world level
     * @param pos the position to check
     * @return true if the position appears to be player-protected
     */
    public static boolean isProtectedArea(Level level, BlockPos pos) {
        return isProtectedArea(level, pos, DEFAULT_CHECK_RADIUS);
    }

    /**
     * Checks if a position is in a player-protected area within a given radius.
     *
     * @param level the world level
     * @param pos the position to check
     * @param radius the radius to search for protection markers
     * @return true if the position appears to be player-protected
     */
    public static boolean isProtectedArea(Level level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    BlockState state = level.getBlockState(mutable);

                    if (isProtectionMarker(state)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a block state is a protection marker (fence, wall, gate, etc.).
     *
     * @param state the block state to check
     * @return true if this block indicates player ownership
     */
    public static boolean isProtectionMarker(BlockState state) {
        Block block = state.getBlock();

        // Fences of any type
        if (block instanceof FenceBlock) {
            return true;
        }

        // Fence gates
        if (block instanceof FenceGateBlock) {
            return true;
        }

        // Walls (cobblestone, brick, etc.)
        if (block instanceof WallBlock) {
            return true;
        }

        // Check fence tag for modded fences
        if (state.is(BlockTags.FENCES)) {
            return true;
        }

        // Check wall tag for modded walls
        if (state.is(BlockTags.WALLS)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a position is cultivated farmland (player-placed crops).
     *
     * @param level the world level
     * @param pos the position to check
     * @return true if the position or adjacent blocks are farmland
     */
    public static boolean isNearFarmland(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    BlockState state = level.getBlockState(mutable);

                    if (state.is(Blocks.FARMLAND)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
