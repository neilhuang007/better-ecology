package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.chicken.EggLayingBehavior;
import me.javavirtualenv.behavior.chicken.GrainEatingBehavior;
import me.javavirtualenv.behavior.chicken.SeedDroppingBehavior;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.ChickenLayEggGoal;
import me.javavirtualenv.ecology.ai.ChickenEatGrainGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle for managing chicken-specific behaviors:
 * - Egg laying with configurable intervals
 * - Seed dropping behavior
 * - Grain/crop eating behavior
 */
public final class EggLayerHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:egg-layer-cache";
    private static final String NBT_EGG_TIMER = "eggLayTimer";
    private static final String NBT_CROPS_EATEN = "cropsEaten";
    private static final String NBT_SEEDS_DROPPED = "seedsDropped";

    @Override
    public String id() {
        return "egg_layer";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        EggLayerCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled();
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Chicken chicken)) {
            return;
        }

        EggLayerCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        EggLayingBehavior eggLayingBehavior = createEggLayingBehavior(cache);
        GrainEatingBehavior grainEatingBehavior = createGrainEatingBehavior(cache);
        SeedDroppingBehavior seedDroppingBehavior = new SeedDroppingBehavior(
            cache.seedDropInterval(),
            cache.seedDropChance()
        );

        ChickenLayEggGoal layEggGoal = new ChickenLayEggGoal(
            chicken,
            eggLayingBehavior,
            cache.minLayInterval(),
            cache.maxLayInterval(),
            cache.hungerThreshold(),
            cache.energyThreshold(),
            cache.goldenEggChance()
        );

        ChickenEatGrainGoal eatGrainGoal = new ChickenEatGrainGoal(
            chicken,
            grainEatingBehavior,
            seedDroppingBehavior,
            cache.grainHungerThreshold(),
            cache.grainSearchCooldown()
        );

        MobAccessor accessor = (MobAccessor) chicken;
        accessor.betterEcology$getGoalSelector().addGoal(cache.layEggPriority(), layEggGoal);
        accessor.betterEcology$getGoalSelector().addGoal(cache.eatGrainPriority(), eatGrainGoal);

        var behaviorTag = component.getHandleTag(id());
        behaviorTag.putInt("layEggPriority", cache.layEggPriority());
        behaviorTag.putInt("eatGrainPriority", cache.eatGrainPriority());
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());

        if (tag.contains(id())) {
            CompoundTag savedTag = tag.getCompound(id());
            if (savedTag.contains(NBT_EGG_TIMER)) {
                handleTag.putInt(NBT_EGG_TIMER, savedTag.getInt(NBT_EGG_TIMER));
            }
            if (savedTag.contains(NBT_CROPS_EATEN)) {
                handleTag.putInt(NBT_CROPS_EATEN, savedTag.getInt(NBT_CROPS_EATEN));
            }
            if (savedTag.contains(NBT_SEEDS_DROPPED)) {
                handleTag.putInt(NBT_SEEDS_DROPPED, savedTag.getInt(NBT_SEEDS_DROPPED));
            }
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        CompoundTag saveTag = new CompoundTag();

        if (handleTag.contains(NBT_EGG_TIMER)) {
            saveTag.putInt(NBT_EGG_TIMER, handleTag.getInt(NBT_EGG_TIMER));
        }
        if (handleTag.contains(NBT_CROPS_EATEN)) {
            saveTag.putInt(NBT_CROPS_EATEN, handleTag.getInt(NBT_CROPS_EATEN));
        }
        if (handleTag.contains(NBT_SEEDS_DROPPED)) {
            saveTag.putInt(NBT_SEEDS_DROPPED, handleTag.getInt(NBT_SEEDS_DROPPED));
        }

        tag.put(id(), saveTag);
    }

    private EggLayerCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBool("egg_laying.enabled", false);

        if (!enabled) {
            return null;
        }

        int layEggPriority = profile.getInt("egg_laying.lay_priority", 8);
        int minLayInterval = profile.getInt("egg_laying.min_interval", 6000);
        int maxLayInterval = profile.getInt("egg_laying.max_interval", 12000);
        double hungerThreshold = profile.getDouble("egg_laying.hunger_threshold", 0.3);
        double energyThreshold = profile.getDouble("egg_laying.energy_threshold", 0.4);
        double goldenEggChance = profile.getDouble("egg_laying.golden_egg_chance", 0.0);

        int eatGrainPriority = profile.getInt("egg_laying.eat_grain_priority", 6);
        double grainHungerThreshold = profile.getDouble("egg_laying.grain_hunger_threshold", 0.6);
        int grainSearchCooldown = profile.getInt("egg_laying.grain_search_cooldown", 200);

        double nestSearchRadius = profile.getDouble("egg_laying.nest_search_radius", 16.0);
        int nestSearchInterval = profile.getInt("egg_laying.nest_search_interval", 100);
        int nestingDuration = profile.getInt("egg_laying.nesting_duration", 60);

        double grainSearchRadius = profile.getDouble("egg_laying.grain_search_radius", 12.0);
        int grainSearchInterval = profile.getInt("egg_laying.grain_search_interval", 160);
        int grainEatingDuration = profile.getInt("egg_laying.grain_eating_duration", 40);

        int seedDropInterval = profile.getInt("egg_laying.seed_drop_interval", 1200);
        double seedDropChance = profile.getDouble("egg_laying.seed_drop_chance", 0.15);

        boolean prefersHay = profile.getBool("egg_laying.prefers_hay", true);
        boolean prefersGrass = profile.getBool("egg_laying.prefers_grass", true);
        boolean eatsWheat = profile.getBool("egg_laying.eats_wheat", true);
        boolean eatsCarrots = profile.getBool("egg_laying.eats_carrots", true);
        boolean eatsPotatoes = profile.getBool("egg_laying.eats_potatoes", true);
        boolean eatsBeetroots = profile.getBool("egg_laying.eats_beetroots", true);

        return new EggLayerCache(
            enabled,
            layEggPriority,
            minLayInterval,
            maxLayInterval,
            hungerThreshold,
            energyThreshold,
            goldenEggChance,
            eatGrainPriority,
            grainHungerThreshold,
            grainSearchCooldown,
            nestSearchRadius,
            nestSearchInterval,
            nestingDuration,
            grainSearchRadius,
            grainSearchInterval,
            grainEatingDuration,
            seedDropInterval,
            seedDropChance,
            prefersHay,
            prefersGrass,
            eatsWheat,
            eatsCarrots,
            eatsPotatoes,
            eatsBeetroots
        );
    }

    private EggLayingBehavior createEggLayingBehavior(EggLayerCache cache) {
        List<net.minecraft.world.level.block.Block> preferredBlocks = new ArrayList<>();

        if (cache.prefersHay()) {
            preferredBlocks.add(Blocks.HAY_BLOCK);
        }
        if (cache.prefersGrass()) {
            preferredBlocks.add(Blocks.GRASS_BLOCK);
            preferredBlocks.add(Blocks.SHORT_GRASS);
        }

        if (preferredBlocks.isEmpty()) {
            preferredBlocks.add(Blocks.DIRT);
        }

        return new EggLayingBehavior(
            cache.nestSearchRadius(),
            cache.nestSearchInterval(),
            preferredBlocks,
            cache.nestingDuration()
        );
    }

    private GrainEatingBehavior createGrainEatingBehavior(EggLayerCache cache) {
        List<net.minecraft.world.level.block.Block> targetCrops = new ArrayList<>();

        if (cache.eatsWheat()) {
            targetCrops.add(Blocks.WHEAT);
        }
        if (cache.eatsCarrots()) {
            targetCrops.add(Blocks.CARROTS);
        }
        if (cache.eatsPotatoes()) {
            targetCrops.add(Blocks.POTATOES);
        }
        if (cache.eatsBeetroots()) {
            targetCrops.add(Blocks.BEETROOTS);
        }

        if (targetCrops.isEmpty()) {
            targetCrops.add(Blocks.WHEAT);
        }

        return new GrainEatingBehavior(
            cache.grainSearchRadius(),
            cache.grainSearchInterval(),
            targetCrops,
            cache.grainEatingDuration()
        );
    }

    private record EggLayerCache(
        boolean enabled,
        int layEggPriority,
        int minLayInterval,
        int maxLayInterval,
        double hungerThreshold,
        double energyThreshold,
        double goldenEggChance,
        int eatGrainPriority,
        double grainHungerThreshold,
        int grainSearchCooldown,
        double nestSearchRadius,
        int nestSearchInterval,
        int nestingDuration,
        double grainSearchRadius,
        int grainSearchInterval,
        int grainEatingDuration,
        int seedDropInterval,
        double seedDropChance,
        boolean prefersHay,
        boolean prefersGrass,
        boolean eatsWheat,
        boolean eatsCarrots,
        boolean eatsPotatoes,
        boolean eatsBeetroots
    ) {
        boolean enabled() {
            return enabled;
        }

        int layEggPriority() {
            return layEggPriority;
        }

        int minLayInterval() {
            return minLayInterval;
        }

        int maxLayInterval() {
            return maxLayInterval;
        }

        double hungerThreshold() {
            return hungerThreshold;
        }

        double energyThreshold() {
            return energyThreshold;
        }

        double goldenEggChance() {
            return goldenEggChance;
        }

        int eatGrainPriority() {
            return eatGrainPriority;
        }

        double grainHungerThreshold() {
            return grainHungerThreshold;
        }

        int grainSearchCooldown() {
            return grainSearchCooldown;
        }

        double nestSearchRadius() {
            return nestSearchRadius;
        }

        int nestSearchInterval() {
            return nestSearchInterval;
        }

        int nestingDuration() {
            return nestingDuration;
        }

        double grainSearchRadius() {
            return grainSearchRadius;
        }

        int grainSearchInterval() {
            return grainSearchInterval;
        }

        int grainEatingDuration() {
            return grainEatingDuration;
        }

        int seedDropInterval() {
            return seedDropInterval;
        }

        double seedDropChance() {
            return seedDropChance;
        }

        boolean prefersHay() {
            return prefersHay;
        }

        boolean prefersGrass() {
            return prefersGrass;
        }

        boolean eatsWheat() {
            return eatsWheat;
        }

        boolean eatsCarrots() {
            return eatsCarrots;
        }

        boolean eatsPotatoes() {
            return eatsPotatoes;
        }

        boolean eatsBeetroots() {
            return eatsBeetroots;
        }
    }
}
