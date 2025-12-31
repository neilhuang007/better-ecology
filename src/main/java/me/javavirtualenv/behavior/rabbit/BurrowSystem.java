package me.javavirtualenv.behavior.rabbit;

import me.javavirtualenv.behavior.core.BehaviorContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rabbit burrow system for a level.
 * <p>
 * Rabbits create and use burrows for:
 * - Safety from predators
 * - Breeding and raising young
 * - Shelter from weather
 * - Temperature regulation
 * <p>
 * Features:
 * - Persistent burrow storage per level
 * - Biome-appropriate burrow types
 * - Capacity limits per burrow
 * - Burrow creation and validation
 */
public class BurrowSystem {

    private static final String BURROW_DATA_KEY = "better-ecology:rabbit_burrows";
    private static final Map<Level, BurrowSystem> INSTANCES = new ConcurrentHashMap<>();

    private final Level level;
    private final Map<BlockPos, RabbitBurrow> burrows;
    private final Map<BlockPos, Long> lastDigAttempt;

    private BurrowSystem(Level level) {
        this.level = level;
        this.burrows = new HashMap<>();
        this.lastDigAttempt = new HashMap<>();
        loadBurrows();
    }

    public static BurrowSystem get(Level level) {
        return INSTANCES.computeIfAbsent(level, BurrowSystem::new);
    }

    /**
     * Attempts to dig a new burrow at the given position.
     *
     * @param context Behavior context
     * @return true if burrow was created
     */
    public boolean tryDigBurrow(BehaviorContext context) {
        Mob entity = context.getEntity();
        BlockPos pos = context.getBlockPos();

        // Check cooldown to prevent excessive digging
        Long lastAttempt = lastDigAttempt.get(pos);
        long currentTime = level.getGameTime();

        if (lastAttempt != null && currentTime - lastAttempt < 600) {
            return false; // Too soon to try again
        }

        lastDigAttempt.put(pos, currentTime);

        // Check if position is valid for burrow
        if (!isValidBurrowLocation(pos)) {
            return false;
        }

        // Check if burrow already exists nearby
        if (findNearestBurrow(pos, 8.0) != null) {
            return false; // Too close to existing burrow
        }

        // Determine burrow type based on biome
        BurrowType type = determineBurrowType(pos);

        // Create burrow
        RabbitBurrow burrow = new RabbitBurrow(pos, type);
        burrows.put(pos, burrow);

        // Play dig sound
        playDigSound(entity);

        // Save burrow data
        saveBurrows();

        return true;
    }

    /**
     * Finds the nearest burrow to a position.
     *
     * @param pos   Position to search from
     * @param range Maximum search range
     * @return Nearest burrow, or null if none found
     */
    public RabbitBurrow findNearestBurrow(BlockPos pos, double range) {
        RabbitBurrow nearest = null;
        double nearestDist = range;

        for (RabbitBurrow burrow : burrows.values()) {
            if (!burrow.isActive()) {
                continue;
            }

            double dist = burrow.distanceTo(pos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = burrow;
            }
        }

        return nearest;
    }

    /**
     * Finds the nearest available burrow with capacity.
     *
     * @param pos   Position to search from
     * @param range Maximum search range
     * @return Nearest available burrow, or null if none found
     */
    public RabbitBurrow findAvailableBurrow(BlockPos pos, double range) {
        RabbitBurrow nearest = null;
        double nearestDist = range;

        for (RabbitBurrow burrow : burrows.values()) {
            if (!burrow.isActive() || burrow.isFull()) {
                continue;
            }

            double dist = burrow.distanceTo(pos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = burrow;
            }
        }

        return nearest;
    }

    /**
     * Makes a rabbit enter a burrow.
     *
     * @param burrow The burrow to enter
     * @return true if successfully entered
     */
    public boolean enterBurrow(Mob entity, RabbitBurrow burrow) {
        if (burrow.isFull()) {
            return false;
        }

        burrow.addOccupant();
        saveBurrows();
        return true;
    }

    /**
     * Makes a rabbit leave a burrow.
     *
     * @param burrow The burrow to leave
     */
    public void leaveBurrow(Mob entity, RabbitBurrow burrow) {
        burrow.removeOccupant();
        saveBurrows();
    }

    /**
     * Removes a burrow (abandoned or destroyed).
     *
     * @param burrow The burrow to remove
     */
    public void removeBurrow(RabbitBurrow burrow) {
        burrows.remove(burrow.getPosition());
        saveBurrows();
    }

    /**
     * Validates if a position is suitable for a burrow.
     */
    private boolean isValidBurrowLocation(BlockPos pos) {
        // Check if on solid ground
        BlockState groundState = level.getBlockState(pos.below());
        if (!groundState.isSolid() && !groundState.is(Blocks.GRASS_BLOCK) &&
            !groundState.is(Blocks.DIRT) && !groundState.is(Blocks.SAND) &&
            !groundState.is(Blocks.SNOW_BLOCK)) {
            return false;
        }

        // Check if space above is clear
        BlockState aboveState = level.getBlockState(pos.above());
        if (aboveState.isSuffocating(level, pos.above())) {
            return false;
        }

        // Check if not in water
        BlockState currentState = level.getBlockState(pos);
        if (currentState.is(Blocks.WATER)) {
            return false;
        }

        return true;
    }

    /**
     * Determines burrow type based on biome.
     */
    private BurrowType determineBurrowType(BlockPos pos) {
        String biomeId = level.getBiome(pos).unwrapKey().location().toString();
        return BurrowType.forBiome(biomeId);
    }

    /**
     * Plays digging sound effect.
     */
    private void playDigSound(Mob entity) {
        if (!entity.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) entity.level();
            serverLevel.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.RABBIT_JUMP, // Reuse rabbit sound
                SoundSource.NEUTRAL,
                0.4F,
                0.8F
            );
        }
    }

    /**
     * Loads burrow data from level storage.
     */
    private void loadBurrows() {
        if (level instanceof ServerLevel serverLevel) {
            CompoundTag data = serverLevel.getDataStorage()
                .get(BurrowData::load, BURROW_DATA_KEY);

            if (data != null) {
                burrows.clear();

                CompoundTag burrowsTag = data.getCompound("burrows");
                for (String key : burrowsTag.getAllKeys()) {
                    CompoundTag burrowTag = burrowsTag.getCompound(key);
                    RabbitBurrow burrow = RabbitBurrow.fromNbt(burrowTag);
                    burrows.put(burrow.getPosition(), burrow);
                }
            }
        }
    }

    /**
     * Saves burrow data to level storage.
     */
    private void saveBurrows() {
        if (level instanceof ServerLevel serverLevel) {
            CompoundTag burrowsTag = new CompoundTag();

            for (RabbitBurrow burrow : burrows.values()) {
                String key = burrow.getPosition().getX() + "_" +
                            burrow.getPosition().getY() + "_" +
                            burrow.getPosition().getZ();
                burrowsTag.put(key, burrow.toNbt());
            }

            CompoundTag data = new CompoundTag();
            data.put("burrows", burrowsTag);

            serverLevel.getDataStorage().set(BURROW_DATA_KEY,
                new BurrowData(data));
        }
    }

    /**
     * Gets all burrows.
     */
    public Collection<RabbitBurrow> getAllBurrows() {
        return Collections.unmodifiableCollection(burrows.values());
    }

    /**
     * Gets burrow count.
     */
    public int getBurrowCount() {
        return burrows.size();
    }

    /**
     * Clears all burrows (for testing/debugging).
     */
    public void clearBurrows() {
        burrows.clear();
        saveBurrows();
    }

    /**
     * Data storage class for burrow persistence.
     */
    private static class BurrowData extends net.minecraft.world.level.saveddata.SavedData {
        private final CompoundTag data;

        public BurrowData(CompoundTag data) {
            this.data = data;
        }

        public static BurrowData load(CompoundTag tag) {
            return new BurrowData(tag);
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.merge(data);
            return tag;
        }
    }
}
