package me.javavirtualenv.behavior.cow;

import me.javavirtualenv.behavior.foraging.GrazingBehavior;
import me.javavirtualenv.behavior.foraging.ForagingConfig;
import me.javavirtualenv.behavior.foraging.ForagingScheduler;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;

/**
 * Grazing handle for cows and mooshrooms.
 * <p>
 * Manages grass consumption behavior:
 * - Eating grass blocks and converting to dirt
 * - Grass regeneration after grazing
 * - Hunger restoration from grazing
 * - Herd grazing synchronization
 * <p>
 * Based on research into cattle grazing patterns:
 * - Bimodal feeding pattern (dawn and dusk)
 * - Patch selection based on grass quality
 * - Social facilitation of grazing
 * - Giving-up density when patch is depleted
 */
public class GrazingHandle implements EcologyHandle {
    private static final String NBT_IS_GRAZING = "isGrazing";
    private static final String NBT_LAST_GRAZE_TIME = "lastGrazeTick";
    private static final String NBT_CURRENT_GRAZE_TARGET = "currentGrazeTarget";
    private static final String NBT_GRASS_EATEN_COUNT = "grassEatenCount";

    private static final int GRAZE_INTERVAL = 30; // Ticks between graze actions
    private static final int GRAZING_HUNGER_RESTORE = 5;
    private static final int MAX_GRASS_PER_PATCH = 20;
    private static final int GRAZE_COOLDOWN = 60;

    @Override
    public String id() {
        return "grazing";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return profile.getBool("grazing.enabled", true);
    }

    @Override
    public int tickInterval() {
        return 20; // Update every second
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Cow cow)) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());
        Level level = mob.level();
        int currentTick = mob.tickCount;
        int lastGraze = tag.getInt(NBT_LAST_GRAZE_TIME);

        // Check if we should attempt to graze
        if (currentTick - lastGraze < GRAZE_COOLDOWN) {
            return;
        }

        // Check if cow is on grass
        BlockPos blockPos = cow.blockPosition();
        BlockPos belowPos = blockPos.below();
        BlockState belowState = level.getBlockState(belowPos);

        if (belowState.is(Blocks.GRASS_BLOCK)) {
            attemptGraze(cow, belowPos, tag, component, level);
        } else if (belowState.is(Blocks.MYCELIUM) && isMooshroom(cow)) {
            attemptGrazeMycelium(cow, belowPos, tag, component, level);
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
        CompoundTag handleTag = component.getHandleTag(id());
        outputTag.put(id(), handleTag.copy());
    }

    /**
     * Check if cow is currently grazing.
     */
    public boolean isGrazing(Mob mob, EcologyComponent component) {
        CompoundTag tag = component.getHandleTag(id());
        return tag.getBoolean(NBT_IS_GRAZING);
    }

    /**
     * Find a nearby grass block to graze.
     * Used by AI pathfinding.
     */
    public BlockPos findNearbyGrass(Mob mob, int searchRadius) {
        Level level = mob.level();
        BlockPos center = mob.blockPosition();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.is(Blocks.GRASS_BLOCK) || (isMooshroom(mob) && state.is(Blocks.MYCELIUM))) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if nearby herd members are grazing (social facilitation).
     */
    public boolean areHerdMembersGrazing(Mob mob) {
        int grazingCount = 0;
        int nearbyCount = 0;

        for (Mob nearby : mob.level().getEntitiesOfClass(mob.getClass(), mob.getBoundingBox().inflate(16.0))) {
            if (!nearby.equals(mob) && nearby.isAlive()) {
                nearbyCount++;
                if (nearby.getPersistentData().getBoolean("better-ecology:isGrazing")) {
                    grazingCount++;
                }
            }
        }

        // If 30% or more of nearby herd is grazing, return true
        return nearbyCount > 0 && (double) grazingCount / nearbyCount >= 0.3;
    }

    private void attemptGraze(Cow cow, BlockPos grassPos, CompoundTag tag, EcologyComponent component, Level level) {
        int grassEaten = tag.getInt(NBT_GRASS_EATEN_COUNT);

        // Check giving-up density
        if (grassEaten >= MAX_GRASS_PER_PATCH) {
            tag.putInt(NBT_GRASS_EATEN_COUNT, 0);
            return;
        }

        // Convert grass to dirt
        level.setBlock(grassPos, Blocks.DIRT.defaultBlockState(), 3);

        // Restore hunger
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (hungerTag.contains("hunger")) {
            int currentHunger = hungerTag.getInt("hunger");
            hungerTag.putInt("hunger", Math.min(100, currentHunger + GRAZING_HUNGER_RESTORE));
        }

        // Update tracking
        tag.putInt(NBT_LAST_GRAZE_TIME, cow.tickCount);
        tag.putInt(NBT_GRASS_EATEN_COUNT, grassEaten + 1);
        tag.putBoolean(NBT_IS_GRAZING, true);

        // Store grazing state in entity data for other herd members to see
        cow.getPersistentData().putBoolean("better-ecology:isGrazing", true);

        // Play eating sound
        level.playSound(null, cow.blockPosition(), SoundEvents.COW_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        // Spawn particles
        spawnEatParticles(level, cow.blockPosition());
    }

    private void attemptGrazeMycelium(Cow cow, BlockPos myceliumPos, CompoundTag tag, EcologyComponent component, Level level) {
        int grassEaten = tag.getInt(NBT_GRASS_EATEN_COUNT);

        if (grassEaten >= MAX_GRASS_PER_PATCH) {
            tag.putInt(NBT_GRASS_EATEN_COUNT, 0);
            return;
        }

        // Convert mycelium to dirt (mooshrooms eat the fungal network)
        level.setBlock(myceliumPos, Blocks.DIRT.defaultBlockState(), 3);

        // Restore more hunger (mooshrooms are more efficient)
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (hungerTag.contains("hunger")) {
            int currentHunger = hungerTag.getInt("hunger");
            hungerTag.putInt("hunger", Math.min(100, currentHunger + GRAZING_HUNGER_RESTORE + 2));
        }

        tag.putInt(NBT_LAST_GRAZE_TIME, cow.tickCount);
        tag.putInt(NBT_GRASS_EATEN_COUNT, grassEaten + 1);
        tag.putBoolean(NBT_IS_GRAZING, true);

        cow.getPersistentData().putBoolean("better-ecology:isGrazing", true);

        level.playSound(null, cow.blockPosition(), SoundEvents.COW_EAT, SoundSource.NEUTRAL, 1.0F, 1.2F);

        spawnEatParticles(level, cow.blockPosition());
    }

    private void spawnEatParticles(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            // Server-side particle spawn will be handled by packet
            // For now, just mark the position for particle effects
            level.levelEvent(2005, pos, 0); // Block break particle
        }
    }

    private boolean isMooshroom(Mob mob) {
        return mob.getClass().getSimpleName().equals("MushroomCow");
    }
}
