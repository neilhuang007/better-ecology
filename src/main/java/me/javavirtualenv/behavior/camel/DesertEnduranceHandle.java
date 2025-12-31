package me.javavirtualenv.behavior.camel;

import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.level.biome.Biome;

/**
 * Handle for camel desert endurance traits.
 * <p>
 * This handle implements camel's desert adaptations:
 * - Reduced hunger and thirst decay in desert biomes
 * - Heat damage immunity (no damage from hot biomes)
 * - Water storage in hump (can survive longer without water)
 * - Efficient temperature regulation
 * <p>
 * Scientific basis: Camels are uniquely adapted to desert life with
 * physiological traits that allow survival in extreme heat and aridity.
 */
public class DesertEnduranceHandle extends CodeBasedHandle {

    private static final String NBT_WATER_STORAGE = "waterStorage";
    private static final String NBT_LAST_UPDATE_TICK = "lastUpdateTick";
    private static final String NBT_IN_DESERT = "inDesert";

    private static final CamelConfig CONFIG = CamelConfig.createDefault();

    @Override
    public String id() {
        return "desert_endurance";
    }

    @Override
    public int tickInterval() {
        return 40; // Update every 2 seconds
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Camel)) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());

        // Check if in desert biome
        boolean inDesert = isInDesertBiome(mob);
        boolean wasInDesert = tag.getBoolean(NBT_IN_DESERT);

        // Handle desert biome effects
        if (inDesert) {
            applyDesertBenefits(mob, component, tag);
        } else {
            // Decay water storage when not in desert
            decayWaterStorage(tag);
        }

        // Update desert status
        tag.putBoolean(NBT_IN_DESERT, inDesert);
        tag.putInt(NBT_LAST_UPDATE_TICK, mob.tickCount);

        // Apply heat resistance (prevent heat damage)
        if (CONFIG.isHeatDamageImmunity()) {
            preventHeatDamage(mob);
        }
    }

    /**
     * Checks if the camel is currently in a desert biome.
     */
    private boolean isInDesertBiome(Mob mob) {
        BlockPos pos = mob.blockPosition();
        ResourceKey<Biome> biomeKey = mob.level().getBiome(pos).unwrapKey().orElse(null);

        if (biomeKey == null) {
            return false;
        }

        ResourceLocation biomeId = biomeKey.location();
        String biomePath = biomeId.getPath();

        // Check for desert variants
        return biomePath.contains("desert") ||
               biomePath.contains("savanna") ||
               biomePath.contains("badlands");
    }

    /**
     * Applies desert biome benefits to the camel.
     */
    private void applyDesertBenefits(Mob mob, EcologyComponent component, CompoundTag tag) {
        // Reduce hunger decay in desert
        modifyHungerDecay(mob, component);

        // Reduce thirst decay in desert
        modifyThirstDecay(mob, component);

        // Store water in hump
        storeWater(mob, tag);
    }

    /**
     * Reduces hunger decay rate while in desert.
     * Camels are more efficient at converting food to energy in their native habitat.
     */
    private void modifyHungerDecay(Mob mob, EcologyComponent component) {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (!hungerTag.contains("lastDecayMultiplier")) {
            hungerTag.putDouble("lastDecayMultiplier", CONFIG.getDesertHungerMultiplier());
        } else {
            hungerTag.putDouble("decayMultiplier", CONFIG.getDesertHungerMultiplier());
        }
    }

    /**
     * Reduces thirst decay rate while in desert.
     * Camels can go long periods without water thanks to their water storage.
     */
    private void modifyThirstDecay(Mob mob, EcologyComponent component) {
        CompoundTag thirstTag = component.getHandleTag("thirst");
        if (!thirstTag.contains("lastDecayMultiplier")) {
            thirstTag.putDouble("lastDecayMultiplier", CONFIG.getDesertThirstMultiplier());
        } else {
            thirstTag.putDouble("decayMultiplier", CONFIG.getDesertThirstMultiplier());
        }
    }

    /**
     * Stores water in the camel's hump for later use.
     */
    private void storeWater(Mob mob, CompoundTag tag) {
        int currentStorage = tag.getInt(NBT_WATER_STORAGE);
        if (currentStorage < CONFIG.getWaterStorageMax()) {
            // Slowly accumulate water while in desert
            // Camels can drink large amounts when water is available
            int storageIncrease = 2;
            int newStorage = Math.min(CONFIG.getWaterStorageMax(), currentStorage + storageIncrease);
            tag.putInt(NBT_WATER_STORAGE, newStorage);
        }
    }

    /**
     * Decays water storage when not in desert.
     * The stored water helps camels survive in non-desert biomes.
     */
    private void decayWaterStorage(CompoundTag tag) {
        int currentStorage = tag.getInt(NBT_WATER_STORAGE);
        if (currentStorage > 0) {
            int newStorage = Math.max(0, currentStorage - CONFIG.getWaterStorageDecayRate());
            tag.putInt(NBT_WATER_STORAGE, newStorage);
        }
    }

    /**
     * Prevents heat damage to camels.
     * Camels are adapted to high temperatures and don't take heat damage.
     */
    private void preventHeatDamage(Mob mob) {
        // Camels don't take damage from hot biomes
        // This is handled by checking for heat damage sources
        // and canceling them if the entity is a camel

        // Note: This would need integration with the damage system
        // For now, we rely on the vanilla camel's heat resistance
    }

    /**
     * Gets the current water storage level.
     */
    public static int getWaterStorage(Mob mob) {
        if (!(mob instanceof Camel)) {
            return 0;
        }

        if (mob instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            EcologyComponent component = access.betterEcology$getEcologyComponent();
            CompoundTag tag = component.getHandleTag("desert_endurance");
            return tag.getInt(NBT_WATER_STORAGE);
        }

        return 0;
    }

    /**
     * Consumes water from storage.
     * Returns true if water was available and consumed.
     */
    public static boolean consumeWaterStorage(Mob mob, int amount) {
        if (!(mob instanceof Camel)) {
            return false;
        }

        if (mob instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            EcologyComponent component = access.betterEcology$getEcologyComponent();
            CompoundTag tag = component.getHandleTag("desert_endurance");

            int currentStorage = tag.getInt(NBT_WATER_STORAGE);
            if (currentStorage >= amount) {
                tag.putInt(NBT_WATER_STORAGE, currentStorage - amount);
                return true;
            }
        }

        return false;
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (!tag.contains(id())) {
            return;
        }

        CompoundTag handleTag = tag.getCompound(id());
        component.setHandleTag(id(), handleTag);
    }
}
