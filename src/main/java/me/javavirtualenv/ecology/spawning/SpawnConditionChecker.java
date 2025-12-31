package me.javavirtualenv.ecology.spawning;

import me.javavirtualenv.ecology.handles.SpawnHandle.SpawnConditions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Efficient spawn condition checker.
 * Validates light level, block type, altitude, cave detection, and weather conditions.
 */
public final class SpawnConditionChecker {

    private SpawnConditionChecker() {
    }

    /**
     * Check all spawn conditions for an entity type.
     */
    public static boolean checkSpawnConditions(
        ServerLevel level,
        BlockPos pos,
        EntityType<?> type,
        SpawnConditions config
    ) {
        // Light level check
        if (!checkLight(level, pos, config)) {
            return false;
        }

        // Block check
        if (!checkBlock(level, pos, config)) {
            return false;
        }

        // Altitude check
        if (!checkAltitude(pos, config)) {
            return false;
        }

        // Cave check for bats
        if (type == EntityType.BAT && !isCave(level, pos)) {
            return false;
        }

        // Weather check
        if (!checkWeather(level, config)) {
            return false;
        }

        return true;
    }

    /**
     * Check if light level is within required range.
     */
    private static boolean checkLight(ServerLevel level, BlockPos pos, SpawnConditions config) {
        int[] lightRange = config.light();
        if (lightRange == null || lightRange.length != 2) {
            return true; // No light constraint
        }

        int lightLevel = level.getRawBrightness(pos, 0);
        int minLight = lightRange[0];
        int maxLight = lightRange[1];

        return lightLevel >= minLight && lightLevel <= maxLight;
    }

    /**
     * Check if the block at spawn position is valid.
     */
    private static boolean checkBlock(ServerLevel level, BlockPos pos, SpawnConditions config) {
        List<String> blocks = config.blocks();
        if (blocks == null || blocks.isEmpty()) {
            return true; // No block constraint
        }

        BlockState blockState = level.getBlockState(pos.below());
        Block block = blockState.getBlock();

        // Check if block matches any allowed block or tag
        for (String blockId : blocks) {
            if (blockId.startsWith("#")) {
                // Tag check
                String tagPath = blockId.substring(1);
                ResourceLocation tagKey = ResourceLocation.tryParse(tagPath);
                if (tagKey != null) {
                    TagKey<Block> blockTag = TagKey.create(
                        BuiltInRegistries.BLOCK.key(),
                        tagKey
                    );
                    if (blockState.is(blockTag)) {
                        return true;
                    }
                }
            } else {
                // Specific block check
                ResourceLocation blockKey = ResourceLocation.tryParse(blockId);
                if (blockKey != null) {
                    Block targetBlock = BuiltInRegistries.BLOCK.get(blockKey);
                    if (block == targetBlock) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if altitude is within required range.
     */
    private static boolean checkAltitude(BlockPos pos, SpawnConditions config) {
        int[] altitudeRange = config.altitude();
        if (altitudeRange == null || altitudeRange.length != 2) {
            return true; // No altitude constraint
        }

        int y = pos.getY();
        int minAltitude = altitudeRange[0];
        int maxAltitude = altitudeRange[1];

        return y >= minAltitude && y <= maxAltitude;
    }

    /**
     * Check if position is in a cave (dark underground area).
     * Used for bats and other cave-dwelling mobs.
     */
    private static boolean isCave(ServerLevel level, BlockPos pos) {
        // Bats spawn in dark areas underground (Y < 63)
        // Must have light level 0
        return pos.getY() < 63 && level.getRawBrightness(pos, 0) == 0;
    }

    /**
     * Check if weather conditions are met.
     */
    private static boolean checkWeather(ServerLevel level, SpawnConditions config) {
        String weather = config.weather();
        if (weather == null) {
            return true; // No weather constraint
        }

        return switch (weather.toLowerCase()) {
            case "clear" -> !level.isRaining() && !level.isThundering();
            case "rain" -> level.isRaining();
            case "thunder" -> level.isThundering();
            case "any" -> true;
            default -> true;
        };
    }
}
