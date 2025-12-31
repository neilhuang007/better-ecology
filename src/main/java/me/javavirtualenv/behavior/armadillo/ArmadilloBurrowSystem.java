package me.javavirtualenv.behavior.armadillo;

import me.javavirtualenv.behavior.core.BehaviorContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages armadillo burrow system for a level.
 * <p>
 * Armadillos create and use burrows for:
 * - Daytime shelter (they're crepuscular/nocturnal)
 * - Temperature regulation
 * - Safety from predators
 * - Raising young
 * <p>
 * Features:
 * - Persistent burrow storage per level
 * - Larger capacity than rabbit burrows
 * - Temperature tracking for comfort
 * - Burrow sharing behavior
 */
public class ArmadilloBurrowSystem {

    private static final String BURROW_DATA_KEY = "better-ecology:armadillo_burrows";
    private static final Map<Level, ArmadilloBurrowSystem> INSTANCES = new ConcurrentHashMap<>();

    private final Level level;
    private final Map<BlockPos, ArmadilloBurrow> burrows;
    private final Map<BlockPos, Long> lastDigAttempt;

    private ArmadilloBurrowSystem(Level level) {
        this.level = level;
        this.burrows = new HashMap<>();
        this.lastDigAttempt = new HashMap<>();
        loadBurrows();
    }

    public static ArmadilloBurrowSystem get(Level level) {
        return INSTANCES.computeIfAbsent(level, ArmadilloBurrowSystem::new);
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

        if (lastAttempt != null && currentTime - lastAttempt < 1200) {
            return false;
        }

        lastDigAttempt.put(pos, currentTime);

        // Check if position is valid for burrow
        if (!isValidBurrowLocation(pos)) {
            return false;
        }

        // Check if burrow already exists nearby
        if (findNearestBurrow(pos, 12.0) != null) {
            return false;
        }

        // Create burrow
        ArmadilloBurrow burrow = new ArmadilloBurrow(pos);
        burrows.put(pos, burrow);

        // Play dig sound
        playDigSound(entity);

        // Create particle effect
        spawnDigParticles(entity);

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
    public ArmadilloBurrow findNearestBurrow(BlockPos pos, double range) {
        ArmadilloBurrow nearest = null;
        double nearestDist = range;

        for (ArmadilloBurrow burrow : burrows.values()) {
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
    public ArmadilloBurrow findAvailableBurrow(BlockPos pos, double range) {
        ArmadilloBurrow nearest = null;
        double nearestDist = range;

        for (ArmadilloBurrow burrow : burrows.values()) {
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
     * Makes an armadillo enter a burrow.
     *
     * @param burrow The burrow to enter
     * @return true if successfully entered
     */
    public boolean enterBurrow(Mob entity, ArmadilloBurrow burrow) {
        if (burrow.isFull()) {
            return false;
        }

        burrow.addOccupant();
        burrow.setLastUsedTick(level.getGameTime());
        saveBurrows();
        return true;
    }

    /**
     * Makes an armadillo leave a burrow.
     *
     * @param burrow The burrow to leave
     */
    public void leaveBurrow(Mob entity, ArmadilloBurrow burrow) {
        burrow.removeOccupant();
        burrow.setLastUsedTick(level.getGameTime());
        saveBurrows();
    }

    /**
     * Removes a burrow (abandoned or destroyed).
     *
     * @param burrow The burrow to remove
     */
    public void removeBurrow(ArmadilloBurrow burrow) {
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
            !groundState.is(Blocks.SANDSTONE)) {
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
                SoundEvents.FOX_SNIFF, // Reuse sniff sound for digging
                SoundSource.NEUTRAL,
                0.3F,
                0.7F
            );
        }
    }

    /**
     * Spawns digging particles.
     */
    private void spawnDigParticles(Mob entity) {
        if (!entity.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) entity.level();
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.BLOCK,
                entity.getX(),
                entity.getY() + 0.5,
                entity.getZ(),
                10,
                0.3,
                0.3,
                0.3,
                0.1
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
                    ArmadilloBurrow burrow = ArmadilloBurrow.fromNbt(burrowTag);
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

            for (ArmadilloBurrow burrow : burrows.values()) {
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
    public Collection<ArmadilloBurrow> getAllBurrows() {
        return Collections.unmodifiableCollection(burrows.values());
    }

    /**
     * Gets burrow count.
     */
    public int getBurrowCount() {
        return burrows.size();
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
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.merge(data);
            return tag;
        }
    }
}
