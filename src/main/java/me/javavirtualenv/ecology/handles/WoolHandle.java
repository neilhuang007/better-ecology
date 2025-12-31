package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Handle for sheep wool growth, shearing, and grazing behaviors.
 * <p>
 * Implements scientifically-based wool growth that depends on:
 * - Nutrition (hunger level)
 * - Time since last shearing
 * - Health and condition
 * - Grazing activity
 */
public final class WoolHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:wool-cache";

    // NBT keys
    private static final String NBT_WOOL_STAGE = "wool_stage";
    private static final String NBT_LAST_SHEAR_TIME = "last_shear_time";
    private static final String NBT_WOOL_QUALITY = "wool_quality";
    private static final String NBT_LAST_GRAZE_TIME = "last_graze_time";
    private static final String NBT_GRAZING_COUNT = "grazing_count";

    @Override
    public String id() {
        return "wool";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        WoolCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled();
    }

    @Override
    public int tickInterval() {
        return 100; // Update every 5 seconds
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Sheep sheep)) {
            return;
        }

        WoolCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());

        // Grow wool over time
        growWool(sheep, tag, cache, component);

        // Try to graze grass if wool needs to grow
        if (!sheep.isSheared() && getWoolStage(tag) < cache.maxWoolStage()) {
            tryGrazing(sheep, tag, cache, component);
        }

        // Update wool quality based on health and nutrition
        updateWoolQuality(sheep, tag, component);

        // Update visual wool state
        updateWoolVisuals(sheep, tag, cache);
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // NBT data is automatically loaded via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    /**
     * Grows wool based on nutrition, time, and health.
     */
    private void growWool(Sheep sheep, CompoundTag tag, WoolCache cache, EcologyComponent component) {
        if (sheep.isSheared()) {
            int lastShearTime = getLastShearTime(tag);
            int timeSinceShear = sheep.tickCount - lastShearTime;

            // Calculate growth chance based on nutrition
            int hunger = component.getHandleTag("hunger").getInt("hunger");
            double nutritionBonus = hunger / 100.0;

            // Growth requires good nutrition
            if (hunger < cache.hungerThreshold()) {
                return; // Too hungry to grow wool
            }

            // Calculate growth chance
            double growthChance = cache.baseGrowthChance() * nutritionBonus;

            // Check if wool should grow this tick
            if (sheep.getRandom().nextDouble() < growthChance) {
                int currentStage = getWoolStage(tag);
                if (currentStage < cache.maxWoolStage()) {
                    setWoolStage(tag, currentStage + 1);

                    // Visual feedback for wool growth
                    if (sheep.level() instanceof ServerLevel serverLevel) {
                        spawnWoolGrowthParticles(sheep, serverLevel);
                    }
                }
            }
        }
    }

    /**
     * Attempts to graze grass at the sheep's current position.
     */
    private void tryGrazing(Sheep sheep, CompoundTag tag, WoolCache cache, EcologyComponent component) {
        BlockPos pos = sheep.blockPosition();
        BlockState blockState = sheep.level().getBlockState(pos);

        // Check if the block is grass
        if (blockState.is(Blocks.GRASS) || blockState.is(Blocks.TALL_GRASS)) {
            // Check if enough time has passed since last graze
            int lastGrazing = getLastGrazingTime(tag);
            int ticksSinceGrazing = sheep.tickCount - lastGrazing;

            if (ticksSinceGrazing >= cache.grazingCooldown()) {
                // Calculate graze chance
                double grazeChance = cache.baseGrazeChance();
                int hunger = component.getHandleTag("hunger").getInt("hunger");

                // Hungrier sheep are more motivated to graze
                if (hunger < 70) {
                    grazeChance *= 1.5;
                }

                if (sheep.getRandom().nextDouble() < grazeChance) {
                    performGrazing(sheep, pos, blockState, tag, cache, component);
                }
            }
        }
    }

    /**
     * Performs the grazing action - eats grass and gains nutrition.
     */
    private void performGrazing(Sheep sheep, BlockPos pos, BlockState blockState,
                                CompoundTag tag, WoolCache cache, EcologyComponent component) {
        // Remove grass
        sheep.level().setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);

        // Update grazing stats
        setLastGrazingTime(tag, sheep.tickCount);
        int grazingCount = getGrazingCount(tag);
        setGrazingCount(tag, grazingCount + 1);

        // Restore hunger
        CompoundTag hungerTag = component.getHandleTag("hunger");
        int currentHunger = hungerTag.getInt("hunger");
        int newHunger = Math.min(100, currentHunger + cache.hungerRestoration());
        hungerTag.putInt("hunger", newHunger);

        // Play eating sound
        sheep.level().playSound(null, sheep.getX(), sheep.getY(), sheep.getZ(),
                               SoundEvents.SHEEP_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        // Spawn particles
        if (sheep.level() instanceof ServerLevel serverLevel) {
            spawnGrazingParticles(sheep, serverLevel);
        }
    }

    /**
     * Updates wool quality based on health and nutrition.
     */
    private void updateWoolQuality(Sheep sheep, CompoundTag tag, EcologyComponent component) {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        int hunger = hungerTag.getInt("hunger");

        CompoundTag conditionTag = component.getHandleTag("condition");
        int condition = conditionTag.getInt("condition");

        // Calculate quality (0.0 to 1.0)
        double quality = 0.0;
        quality += (hunger / 100.0) * 0.5; // 50% from nutrition
        quality += (condition / 100.0) * 0.3; // 30% from condition
        quality += sheep.getHealth() / sheep.getMaxHealth() * 0.2; // 20% from health

        setWoolQuality(tag, (float) Math.max(0.0, Math.min(1.0, quality)));
    }

    /**
     * Updates the visual wool state on the sheep.
     */
    private void updateWoolVisuals(Sheep sheep, CompoundTag tag, WoolCache cache) {
        int woolStage = getWoolStage(tag);
        boolean wasSheared = sheep.isSheared();

        // If wool has grown back, update sheared state
        if (wasSheared && woolStage >= cache.minWoolStageForShear()) {
            // Vanilla handles this automatically
        }
    }

    /**
     * Calculates wool drop amount based on quality.
     */
    public int calculateWoolDrop(Sheep sheep, WoolCache cache, CompoundTag tag) {
        float quality = getWoolQuality(tag);
        int baseDrops = cache.baseWoolDrops();

        // Quality affects drop amount
        if (quality > 0.8) {
            return baseDrops + 2; // Premium quality
        } else if (quality > 0.6) {
            return baseDrops + 1; // Good quality
        } else if (quality > 0.4) {
            return baseDrops; // Normal quality
        } else {
            return Math.max(1, baseDrops - 1); // Poor quality
        }
    }

    /**
     * Handles shearing - resets wool growth.
     */
    public void handleShearing(Sheep sheep, CompoundTag tag, WoolCache cache) {
        setLastShearTime(tag, sheep.tickCount);
        setWoolStage(tag, 0);

        // Play shearing sound
        sheep.level().playSound(null, sheep.getX(), sheep.getY(), sheep.getZ(),
                               SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void spawnWoolGrowthParticles(Sheep sheep, ServerLevel level) {
        // Spawn subtle white particles to indicate wool growth
        for (int i = 0; i < 3; i++) {
            double offsetX = (sheep.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetY = sheep.getRandom().nextDouble() * 0.5;
            double offsetZ = (sheep.getRandom().nextDouble() - 0.5) * 0.5;

            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE,
                    sheep.getX() + offsetX,
                    sheep.getY() + offsetY,
                    sheep.getZ() + offsetZ,
                    1, 0, 0, 0, 0.01
            );
        }
    }

    private void spawnGrazingParticles(Sheep sheep, ServerLevel level) {
        // Spawn green particles to indicate grazing
        for (int i = 0; i < 5; i++) {
            double offsetX = (sheep.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetZ = (sheep.getRandom().nextDouble() - 0.5) * 0.5;

            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    sheep.getX() + offsetX,
                    sheep.getY(),
                    sheep.getZ() + offsetZ,
                    1, 0, 0, 0, 0.02
            );
        }
    }

    private WoolCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBoolFast("wool", "enabled", false);

        if (!enabled) {
            return null;
        }

        int maxWoolStage = profile.getIntFast("wool", "max_wool_stage", 4);
        int minWoolStageForShear = profile.getIntFast("wool", "min_wool_stage_for_shear", 3);
        double baseGrowthChance = profile.getDoubleFast("wool", "base_growth_chance", 0.1);
        int hungerThreshold = profile.getIntFast("wool", "hunger_threshold", 30);
        double baseGrazeChance = profile.getDoubleFast("wool", "base_graze_chance", 0.3);
        int grazingCooldown = profile.getIntFast("wool", "grazing_cooldown", 200);
        int hungerRestoration = profile.getIntFast("wool", "hunger_restoration", 20);
        int baseWoolDrops = profile.getIntFast("wool", "base_wool_drops", 1);

        return new WoolCache(enabled, maxWoolStage, minWoolStageForShear, baseGrowthChance,
                           hungerThreshold, baseGrazeChance, grazingCooldown, hungerRestoration, baseWoolDrops);
    }

    // NBT getters and setters
    private int getWoolStage(CompoundTag tag) {
        return tag.getInt(NBT_WOOL_STAGE);
    }

    private void setWoolStage(CompoundTag tag, int stage) {
        tag.putInt(NBT_WOOL_STAGE, stage);
    }

    private int getLastShearTime(CompoundTag tag) {
        return tag.getInt(NBT_LAST_SHEAR_TIME);
    }

    private void setLastShearTime(CompoundTag tag, int time) {
        tag.putInt(NBT_LAST_SHEAR_TIME, time);
    }

    private float getWoolQuality(CompoundTag tag) {
        return tag.getFloat(NBT_WOOL_QUALITY);
    }

    private void setWoolQuality(CompoundTag tag, float quality) {
        tag.putFloat(NBT_WOOL_QUALITY, quality);
    }

    private int getLastGrazingTime(CompoundTag tag) {
        return tag.getInt(NBT_LAST_GRAZE_TIME);
    }

    private void setLastGrazingTime(CompoundTag tag, int time) {
        tag.putInt(NBT_LAST_GRAZE_TIME, time);
    }

    private int getGrazingCount(CompoundTag tag) {
        return tag.getInt(NBT_GRAZING_COUNT);
    }

    private void setGrazingCount(CompoundTag tag, int count) {
        tag.putInt(NBT_GRAZING_COUNT, count);
    }

    private static final class WoolCache {
        private final boolean enabled;
        private final int maxWoolStage;
        private final int minWoolStageForShear;
        private final double baseGrowthChance;
        private final int hungerThreshold;
        private final double baseGrazeChance;
        private final int grazingCooldown;
        private final int hungerRestoration;
        private final int baseWoolDrops;

        private WoolCache(boolean enabled, int maxWoolStage, int minWoolStageForShear, double baseGrowthChance,
                         int hungerThreshold, double baseGrazeChance, int grazingCooldown, int hungerRestoration, int baseWoolDrops) {
            this.enabled = enabled;
            this.maxWoolStage = maxWoolStage;
            this.minWoolStageForShear = minWoolStageForShear;
            this.baseGrowthChance = baseGrowthChance;
            this.hungerThreshold = hungerThreshold;
            this.baseGrazeChance = baseGrazeChance;
            this.grazingCooldown = grazingCooldown;
            this.hungerRestoration = hungerRestoration;
            this.baseWoolDrops = baseWoolDrops;
        }

        private boolean enabled() {
            return enabled;
        }

        private int maxWoolStage() {
            return maxWoolStage;
        }

        private int minWoolStageForShear() {
            return minWoolStageForShear;
        }

        private double baseGrowthChance() {
            return baseGrowthChance;
        }

        private int hungerThreshold() {
            return hungerThreshold;
        }

        private double baseGrazeChance() {
            return baseGrazeChance;
        }

        private int grazingCooldown() {
            return grazingCooldown;
        }

        private int hungerRestoration() {
            return hungerRestoration;
        }

        private int baseWoolDrops() {
            return baseWoolDrops;
        }
    }
}
