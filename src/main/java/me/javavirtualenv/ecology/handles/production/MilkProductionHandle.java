package me.javavirtualenv.ecology.handles.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.AgeHandle;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import me.javavirtualenv.ecology.handles.HungerHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Handle for milk production in dairy animals (cows, goats, mooshrooms).
 *
 * <p>Features:
 * <ul>
 *   <li>Tracks milk level (0-100%) and regeneration</li>
 *   <li>Quality system based on diet, health, and happiness</li>
 *   <li>Species-specific parameters (capacity, regeneration rate)</li>
 *   <li>Pregnancy/lactation integration</li>
 *   <li>Nursing penalty when calf is feeding</li>
 * </ul>
 *
 * <p>Scientific basis:
 * <ul>
 *   <li>Cow milk production: ~1-3 buckets per full udder, 1-2 hours regeneration per bucket</li>
 *   <li>Goat milk: Faster regeneration, smaller capacity</li>
 *   <li>Quality affected by: Grass consumption, health status, stress levels</li>
 *   <li>Post-pregnancy boost: Milk production increases after giving birth</li>
 * </ul>
 */
public final class MilkProductionHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:milk-production-cache";

    // NBT tag keys
    private static final String NBT_MILK_LEVEL = "milkLevel";
    private static final String NBT_LAST_MILKED = "lastMilked";
    private static final String NBT_MILK_QUALITY = "milkQuality";
    private static final String NBT_IS_NURSING = "isNursing";

    @Override
    public String id() {
        return "milk_production";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        MilkProductionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled();
    }

    @Override
    public int tickInterval() {
        return 20; // Update once per second
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        MilkProductionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        CompoundTag handleTag = component.getHandleTag(id());
        regenerateMilk(mob, handleTag, cache, component);
        updateQuality(mob, handleTag, cache, component);
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    /**
     * Check if the animal can currently be milked.
     *
     * @param mob The animal
     * @param component The ecology component
     * @param profile The ecology profile
     * @return true if milking is possible
     */
    public boolean canBeMilked(Mob mob, EcologyComponent component, EcologyProfile profile) {
        MilkProductionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return false;
        }

        if (!isAdult(component)) {
            return false;
        }

        CompoundTag handleTag = component.getHandleTag(id());
        int milkLevel = getMilkLevel(handleTag);
        int minMilkLevel = cache.minMilkLevel();

        return milkLevel >= minMilkLevel;
    }

    /**
     * Milk the animal, returning the appropriate milk item.
     *
     * @param mob The animal
     * @param player The player milking
     * @param component The ecology component
     * @param profile The ecology profile
     * @return ItemStack containing the milk product, or empty if milking failed
     */
    public ItemStack milk(Mob mob, Player player, EcologyComponent component, EcologyProfile profile) {
        if (!canBeMilked(mob, component, profile)) {
            return ItemStack.EMPTY;
        }

        MilkProductionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return ItemStack.EMPTY;
        }

        CompoundTag handleTag = component.getHandleTag(id());
        ItemStack milk = createMilkItem(handleTag, cache, mob);

        if (!milk.isEmpty()) {
            consumeMilk(handleTag, cache);
            setLastMilked(handleTag, mob.tickCount);
        }

        return milk;
    }

    /**
     * Get current milk level as percentage (0-100).
     */
    public int getMilkLevel(EcologyComponent component) {
        CompoundTag handleTag = component.getHandleTag(id());
        return handleTag.getInt(NBT_MILK_LEVEL);
    }

    /**
     * Get current milk quality tier.
     */
    public MilkQuality getMilkQuality(EcologyComponent component) {
        CompoundTag handleTag = component.getHandleTag(id());
        int qualityScore = handleTag.getInt(NBT_MILK_QUALITY);
        return MilkQuality.fromScore(qualityScore);
    }

    /**
     * Check if the animal is currently nursing a calf.
     */
    public boolean isNursing(EcologyComponent component) {
        CompoundTag handleTag = component.getHandleTag(id());
        return handleTag.getBoolean(NBT_IS_NURSING);
    }

    /**
     * Set whether the animal is nursing a calf.
     */
    public void setNursing(EcologyComponent component, boolean nursing) {
        CompoundTag handleTag = component.getHandleTag(id());
        handleTag.putBoolean(NBT_IS_NURSING, nursing);
    }

    /**
     * Regenerate milk over time based on regeneration rate.
     */
    private void regenerateMilk(Mob mob, CompoundTag handleTag, MilkProductionCache cache,
                                EcologyComponent component) {
        int currentLevel = getMilkLevel(handleTag);
        if (currentLevel >= 100) {
            return;
        }

        long elapsedTicks = component.elapsedTicks();
        double regenerationAmount = cache.regenerationRate() * elapsedTicks;

        // Apply nursing penalty if calf is feeding
        if (isNursing(component)) {
            regenerationAmount *= cache.nursingPenalty();
        }

        // Apply pregnancy boost if recently gave birth
        if (isRecentlyPregnant(component)) {
            regenerationAmount *= cache.postPregnancyBoost();
        }

        int newLevel = (int) Math.min(100, currentLevel + regenerationAmount);
        setMilkLevel(handleTag, newLevel);
    }

    /**
     * Update milk quality based on diet, health, and condition.
     */
    private void updateQuality(Mob mob, CompoundTag handleTag, MilkProductionCache cache,
                              EcologyComponent component) {
        int hunger = HungerHandle.getHungerLevel(component);
        int condition = ConditionHandle.getConditionLevel(component);
        double healthPercent = mob.getHealth() / mob.getMaxHealth();

        // Calculate quality score (0-100)
        double qualityScore = calculateQualityScore(hunger, condition, healthPercent, cache);

        // Smooth quality changes (don't fluctuate too rapidly)
        int currentQuality = handleTag.getInt(NBT_MILK_QUALITY);
        int qualityDelta = (int) Math.round((qualityScore - currentQuality) * 0.1);
        int newQuality = Math.max(0, Math.min(100, currentQuality + qualityDelta));

        handleTag.putInt(NBT_MILK_QUALITY, newQuality);
    }

    /**
     * Calculate quality score from various factors.
     */
    private double calculateQualityScore(int hunger, int condition, double healthPercent,
                                        MilkProductionCache cache) {
        double score = 0.0;

        // Hunger contribution (40%)
        score += (hunger / 100.0) * 40.0;

        // Condition contribution (30%)
        score += (condition / 100.0) * 30.0;

        // Health contribution (20%)
        score += healthPercent * 20.0;

        // Base quality from genetics (10%)
        score += cache.baseQuality() * 10.0;

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Create the appropriate milk item based on quality and animal type.
     */
    private ItemStack createMilkItem(CompoundTag handleTag, MilkProductionCache cache, Mob mob) {
        int qualityScore = handleTag.getInt(NBT_MILK_QUALITY);
        MilkQuality quality = MilkQuality.fromScore(qualityScore);

        // Mooshrooms produce mushroom stew instead of milk
        if (cache.isMooshroom()) {
            return new ItemStack(Items.MUSHROOM_STEW);
        }

        // For cows and goats, return milk bucket
        // Quality could affect the item type (enriched milk, etc.)
        return new ItemStack(Items.MILK_BUCKET);
    }

    /**
     * Consume milk when milking occurs.
     */
    private void consumeMilk(CompoundTag handleTag, MilkProductionCache cache) {
        int currentLevel = getMilkLevel(handleTag);
        int milkConsumed = cache.milkPerBucket();
        int newLevel = Math.max(0, currentLevel - milkConsumed);
        setMilkLevel(handleTag, newLevel);
    }

    /**
     * Check if animal is adult.
     */
    private boolean isAdult(EcologyComponent component) {
        int ageTicks = AgeHandle.getAgeTicks(component);
        return ageTicks >= 24000; // Standard adulthood threshold
    }

    /**
     * Check if animal recently gave birth (simplified check).
     */
    private boolean isRecentlyPregnant(EcologyComponent component) {
        // This would integrate with BreedingHandle to check if recently bred
        // For now, return false as placeholder
        return false;
    }

    // Helper methods for NBT access
    private int getMilkLevel(CompoundTag handleTag) {
        return handleTag.contains(NBT_MILK_LEVEL) ? handleTag.getInt(NBT_MILK_LEVEL) : 100;
    }

    private void setMilkLevel(CompoundTag handleTag, int level) {
        handleTag.putInt(NBT_MILK_LEVEL, Math.max(0, Math.min(100, level)));
    }

    private int getLastMilked(CompoundTag handleTag) {
        return handleTag.getInt(NBT_LAST_MILKED);
    }

    private void setLastMilked(CompoundTag handleTag, int tick) {
        handleTag.putInt(NBT_LAST_MILKED, tick);
    }

    /**
     * Build cache from YAML profile.
     */
    private MilkProductionCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBoolFast("milk_production", "enabled", false);

        if (!enabled) {
            return null;
        }

        int maxMilkAmount = profile.getIntFast("milk_production", "max_milk_amount", 3);
        double regenerationRate = profile.getDoubleFast("milk_production", "regeneration_rate", 0.01);
        int milkQuality = profile.getIntFast("milk_production", "milk_quality", 50);
        int minMilkLevel = profile.getIntFast("milk_production", "min_milk_level", 33);
        double nursingPenalty = profile.getDoubleFast("milk_production", "nursing_penalty", 0.7);
        double postPregnancyBoost = profile.getDoubleFast("milk_production", "post_pregnancy_boost", 1.3);
        boolean isMooshroom = profile.getBoolFast("milk_production", "is_mooshroom", false);

        return new MilkProductionCache(enabled, maxMilkAmount, regenerationRate, milkQuality,
                minMilkLevel, nursingPenalty, postPregnancyBoost, isMooshroom);
    }

    /**
     * Cache for milk production parameters.
     */
    private static final class MilkProductionCache {
        private final boolean enabled;
        private final int maxMilkAmount;
        private final double regenerationRate;
        private final int baseQuality;
        private final int minMilkLevel;
        private final double nursingPenalty;
        private final double postPregnancyBoost;
        private final boolean isMooshroom;

        private MilkProductionCache(boolean enabled, int maxMilkAmount, double regenerationRate,
                                   int baseQuality, int minMilkLevel, double nursingPenalty,
                                   double postPregnancyBoost, boolean isMooshroom) {
            this.enabled = enabled;
            this.maxMilkAmount = maxMilkAmount;
            this.regenerationRate = regenerationRate;
            this.baseQuality = baseQuality;
            this.minMilkLevel = minMilkLevel;
            this.nursingPenalty = nursingPenalty;
            this.postPregnancyBoost = postPregnancyBoost;
            this.isMooshroom = isMooshroom;
        }

        private boolean enabled() {
            return enabled;
        }

        private int maxMilkAmount() {
            return maxMilkAmount;
        }

        private double regenerationRate() {
            return regenerationRate;
        }

        private int baseQuality() {
            return baseQuality;
        }

        private int minMilkLevel() {
            return minMilkLevel;
        }

        private double nursingPenalty() {
            return nursingPenalty;
        }

        private double postPregnancyBoost() {
            return postPregnancyBoost;
        }

        private boolean isMooshroom() {
            return isMooshroom;
        }

        private int milkPerBucket() {
            return 100 / maxMilkAmount; // Percentage consumed per bucket
        }
    }

    /**
     * Milk quality tiers affecting nutrition and potential buffs.
     */
    public enum MilkQuality {
        POOR(0, 39, 0.8f, "Poor"),
        REGULAR(40, 69, 1.0f, "Regular"),
        PREMIUM(70, 89, 1.2f, "Premium"),
        EXCEPTIONAL(90, 100, 1.5f, "Exceptional");

        private final int minScore;
        private final int maxScore;
        private final float nutritionMultiplier;
        private final String displayName;

        MilkQuality(int minScore, int maxScore, float nutritionMultiplier, String displayName) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.nutritionMultiplier = nutritionMultiplier;
            this.displayName = displayName;
        }

        public static MilkQuality fromScore(int score) {
            for (MilkQuality quality : values()) {
                if (score >= quality.minScore && score <= quality.maxScore) {
                    return quality;
                }
            }
            return REGULAR;
        }

        public float getNutritionMultiplier() {
            return nutritionMultiplier;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
