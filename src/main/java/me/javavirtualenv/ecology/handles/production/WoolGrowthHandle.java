package me.javavirtualenv.ecology.handles.production;

import java.util.List;
import java.util.Map;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * Handles wool growth, quality, and shearing mechanics for sheep.
 *
 * Scientific basis: Real sheep wool growth takes 3-6 months, with growth rates
 * affected by nutrition, health, age, and seasonal factors. Quality varies based
 * on diet, health, and environmental conditions.
 */
public final class WoolGrowthHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:wool-growth-cache";

    // NBT keys
    private static final String NBT_WOOL_LENGTH = "woolLength";
    private static final String NBT_WOOL_QUALITY = "woolQuality";
    private static final String NBT_LAST_SHEARED = "lastShearedTime";
    private static final String NBT_COAT_THICKNESS = "coatThickness";
    private static final String NBT_STORED_COLOR = "storedColor";
    private static final String NBT_DYE_EXPIRES = "dyeExpires";
    private static final String NBT_IS_PARASITIZED = "isParasitised";

    // Wool quality levels
    public enum WoolQuality {
        POOR(0.5f, 1, "matted", "dirty"),
        REGULAR(1.0f, 2, "normal", "clean"),
        PREMIUM(1.5f, 3, "pristine", "thick");

        private final float dropMultiplier;
        private final int baseDrops;
        private final String[] descriptors;

        WoolQuality(float dropMultiplier, int baseDrops, String... descriptors) {
            this.dropMultiplier = dropMultiplier;
            this.baseDrops = baseDrops;
            this.descriptors = descriptors;
        }

        public float getDropMultiplier() {
            return dropMultiplier;
        }

        public int getBaseDrops() {
            return baseDrops;
        }

        public static WoolQuality fromScore(double score) {
            if (score >= 75) {
                return PREMIUM;
            } else if (score >= 40) {
                return REGULAR;
            } else {
                return POOR;
            }
        }
    }

    @Override
    public String id() {
        return "wool_growth";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return profile.getBool("production.wool_growth.enabled", false);
    }

    @Override
    public int tickInterval() {
        return 20; // Update every second
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Sheep sheep)) {
            return;
        }

        WoolGrowthConfig config = profile.cached(CACHE_KEY, () -> buildConfig(profile));
        CompoundTag tag = component.getHandleTag(id());

        // Initialize if first time
        if (!tag.contains(NBT_WOOL_LENGTH)) {
            initializeWoolState(sheep, tag, config);
        }

        // Update wool growth
        updateWoolGrowth(sheep, tag, component, config);

        // Update coat thickness based on season
        updateCoatThickness(sheep, tag, config);

        // Recalculate quality based on current factors
        recalculateWoolQuality(sheep, tag, component, config);

        // Check for parasites
        checkParasites(sheep, tag, config);

        // Update dye expiration if temporary dye
        updateTemporaryDye(tag);
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Loaded automatically via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private void initializeWoolState(Sheep sheep, CompoundTag tag, WoolGrowthConfig config) {
        // Start with full wool length if not sheared
        tag.putFloat(NBT_WOOL_LENGTH, 100.0f);

        // Initial quality based on health
        float initialQuality = calculateInitialQuality(sheep);
        tag.putFloat(NBT_WOOL_QUALITY, initialQuality);

        // Store current color
        tag.putInt(NBT_STORED_COLOR, sheep.getColor().getId());

        // Set initial coat thickness
        tag.putFloat(NBT_COAT_THICKNESS, 1.0f);
    }

    private void updateWoolGrowth(Sheep sheep, CompoundTag tag, EcologyComponent component, WoolGrowthConfig config) {
        // Don't grow if already sheared (no wool)
        if (sheep.isSheared()) {
            tag.putFloat(NBT_WOOL_LENGTH, 0.0f);
            return;
        }

        float currentLength = getWoolLength(tag);
        if (currentLength >= 100.0f) {
            tag.putFloat(NBT_WOOL_LENGTH, 100.0f);
            return;
        }

        // Calculate growth rate based on factors
        double growthRate = calculateGrowthRate(component, config);

        // Apply growth
        long elapsed = component.elapsedTicks();
        float growth = (float) (growthRate * elapsed);
        float newLength = Math.min(100.0f, currentLength + growth);

        tag.putFloat(NBT_WOOL_LENGTH, newLength);
    }

    private double calculateGrowthRate(EcologyComponent component, WoolGrowthConfig config) {
        double baseRate = config.growthRate();

        // Diet factor - well-fed sheep grow wool faster
        double dietFactor = getDietFactor(component);

        // Health factor - healthier sheep produce better wool
        double healthFactor = getHealthFactor(component);

        // Age factor - elderly sheep grow slower
        double ageFactor = getAgeFactor(component);

        // Seasonal modifier
        Level level = component.ecologyMob().level();
        double seasonModifier = getSeasonalModifier(level, config);

        return baseRate * dietFactor * healthFactor * ageFactor * seasonModifier;
    }

    private double getDietFactor(EcologyComponent component) {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (!hungerTag.contains("hunger")) {
            return 0.5;
        }
        int hunger = hungerTag.getInt("hunger");
        // Scale from 0.2x at 0 hunger to 1.5x at 100 hunger
        return 0.2 + (hunger / 100.0) * 1.3;
    }

    private double getHealthFactor(EcologyComponent component) {
        CompoundTag conditionTag = component.getHandleTag("condition");
        if (!conditionTag.contains("condition")) {
            return 0.5;
        }
        int condition = conditionTag.getInt("condition");
        // Scale from 0.3x at 0 condition to 1.2x at 100 condition
        return 0.3 + (condition / 100.0) * 0.9;
    }

    private double getAgeFactor(EcologyComponent component) {
        CompoundTag ageTag = component.getHandleTag("age");
        if (!ageTag.contains("isElderly")) {
            return 1.0;
        }
        boolean isElderly = ageTag.getBoolean("isElderly");
        return isElderly ? 0.6 : 1.0;
    }

    private double getSeasonalModifier(Level level, WoolGrowthConfig config) {
        // Get season from temporal handle
        CompoundTag temporalTag = ((EcologyComponent) component.ecologyComponent()).getHandleTag("temporal");
        if (!temporalTag.contains("season")) {
            return 1.0;
        }
        String season = temporalTag.getString("season");

        // Winter: thicker coat but slower growth
        // Summer: thinner coat but faster growth
        return switch (season) {
            case "winter" -> config.seasonalThicknessModifier().winterGrowthRate();
            case "summer" -> config.seasonalThicknessModifier().summerGrowthRate();
            default -> 1.0;
        };
    }

    private void updateCoatThickness(Sheep sheep, CompoundTag tag, WoolGrowthConfig config) {
        Level level = sheep.level();
        long dayTime = level.getDayTime() % 24000;

        // Simple season detection based on day cycle
        // In a full implementation, this would use a seasonal calendar
        boolean isWinterSeason = false;
        boolean isSummerSeason = false;

        // For now, use temperature to determine season
        if (level.getBiome(sheep.blockPosition()).value().getBaseTemperature() < 0.0) {
            isWinterSeason = true;
        } else if (level.getBiome(sheep.blockPosition()).value().getBaseTemperature() > 0.5) {
            isSummerSeason = true;
        }

        float thickness;
        if (isWinterSeason) {
            thickness = config.seasonalThicknessModifier().winterThickness();
        } else if (isSummerSeason) {
            thickness = config.seasonalThicknessModifier().summerThickness();
        } else {
            thickness = 1.0f;
        }

        tag.putFloat(NBT_COAT_THICKNESS, thickness);
    }

    private void recalculateWoolQuality(Sheep sheep, CompoundTag tag, EcologyComponent component, WoolGrowthConfig config) {
        // Get quality factors
        double dietScore = getDietScore(component);
        double healthScore = getHealthScore(component);
        double ageScore = getAgeScore(component);
        double environmentScore = getEnvironmentScore(sheep, tag);

        // Weighted average
        double qualityScore = (dietScore * config.qualityFactors().dietWeight() +
                            healthScore * config.qualityFactors().healthWeight() +
                            ageScore * config.qualityFactors().ageWeight() +
                            environmentScore * config.qualityFactors().environmentWeight()) /
                            (config.qualityFactors().dietWeight() +
                            config.qualityFactors().healthWeight() +
                            config.qualityFactors().ageWeight() +
                            config.qualityFactors().environmentWeight());

        // Parasite penalty
        if (tag.getBoolean(NBT_IS_PARASITIZED)) {
            qualityScore *= 0.4;
        }

        tag.putFloat(NBT_WOOL_QUALITY, (float) qualityScore);
    }

    private double getDietScore(EcologyComponent component) {
        CompoundTag hungerTag = component.getHandleTag("hunger");
        if (!hungerTag.contains("hunger")) {
            return 50.0;
        }
        return hungerTag.getInt("hunger");
    }

    private double getHealthScore(EcologyComponent component) {
        CompoundTag conditionTag = component.getHandleTag("condition");
        if (!conditionTag.contains("condition")) {
            return 50.0;
        }
        return conditionTag.getInt("condition");
    }

    private double getAgeScore(EcologyComponent component) {
        CompoundTag ageTag = component.getHandleTag("age");
        if (!ageTag.contains(NBT_WOOL_QUALITY) || !ageTag.contains("ageTicks")) {
            return 80.0;
        }
        int ageTicks = ageTag.getInt("ageTicks");
        // Prime age: 24000-72000 ticks (1-3 in-game days)
        if (ageTicks < 24000) {
            return 60.0; // Young
        } else if (ageTicks <= 72000) {
            return 100.0; // Prime
        } else {
            return 70.0; // Older
        }
    }

    private double getEnvironmentScore(Sheep sheep, CompoundTag tag) {
        // Check if sheep has shelter
        // Check if biome is suitable
        float temperature = sheep.level().getBiome(sheep.blockPosition()).value().getBaseTemperature();

        // Ideal temperature range: 0.0 to 0.5
        if (temperature >= 0.0 && temperature <= 0.5) {
            return 90.0;
        } else if (temperature < -0.5 || temperature > 1.0) {
            return 40.0;
        } else {
            return 65.0;
        }
    }

    private void checkParasites(Sheep sheep, CompoundTag tag, WoolGrowthConfig config) {
        if (tag.getBoolean(NBT_IS_PARASITIZED)) {
            return;
        }

        // Random chance to get parasites if in poor conditions
        if (sheep.level().random.nextFloat() < config.parasiteChance()) {
            CompoundTag conditionTag = ((EcologyComponent) sheep).getEcologyComponentIfExists()
                .map(c -> c.getHandleTag("condition")).orElse(new CompoundTag());

            if (conditionTag.contains("condition") && conditionTag.getInt("condition") < 30) {
                tag.putBoolean(NBT_IS_PARASITIZED, true);
            }
        }
    }

    private void updateTemporaryDye(CompoundTag tag) {
        if (!tag.contains(NBT_DYE_EXPIRES)) {
            return;
        }

        long expires = tag.getLong(NBT_DYE_EXPIRES);
        if (System.currentTimeMillis() > expires) {
            // Temporary dye expired, revert to stored color
            tag.putInt(NBT_STORED_COLOR, tag.getInt(NBT_STORED_COLOR));
            tag.remove(NBT_DYE_EXPIRES);
        }
    }

    private float calculateInitialQuality(Sheep sheep) {
        // Based on health percentage
        float healthPercent = sheep.getHealth() / sheep.getMaxHealth();
        return healthPercent * 100.0f;
    }

    // Public API methods

    public static boolean canBeSheared(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("wool_growth");
        if (!tag.contains(NBT_WOOL_LENGTH)) {
            return true; // Vanilla sheep
        }
        float length = tag.getFloat(NBT_WOOL_LENGTH);
        return length >= 50.0f; // Must have at least 50% wool
    }

    public static int shear(EcologyComponent component, Sheep sheep) {
        CompoundTag tag = component.getHandleTag("wool_growth");
        WoolQuality quality = getWoolQuality(tag);

        int drops = calculateWoolDrops(tag, quality);

        // Reset wool length
        tag.putFloat(NBT_WOOL_LENGTH, 0.0f);

        // Update last sheared time
        tag.putLong(NBT_LAST_SHEARED, System.currentTimeMillis());

        // Remove parasites if present
        tag.putBoolean(NBT_IS_PARASITIZED, false);

        return drops;
    }

    public static float getWoolLength(CompoundTag tag) {
        if (!tag.contains(NBT_WOOL_LENGTH)) {
            return 100.0f; // Default full length
        }
        return tag.getFloat(NBT_WOOL_LENGTH);
    }

    public static WoolQuality getWoolQuality(CompoundTag tag) {
        if (!tag.contains(NBT_WOOL_QUALITY)) {
            return WoolQuality.REGULAR;
        }
        float quality = tag.getFloat(NBT_WOOL_QUALITY);
        return WoolQuality.fromScore(quality);
    }

    public static float getCoatThickness(CompoundTag tag) {
        if (!tag.contains(NBT_COAT_THICKNESS)) {
            return 1.0f;
        }
        return tag.getFloat(NBT_COAT_THICKNESS);
    }

    public static DyeColor getStoredColor(CompoundTag tag) {
        if (!tag.contains(NBT_STORED_COLOR)) {
            return DyeColor.WHITE;
        }
        int colorId = tag.getInt(NBT_STORED_COLOR);
        return DyeColor.byId(colorId);
    }

    public static void setStoredColor(CompoundTag tag, DyeColor color, boolean isTemporary) {
        tag.putInt(NBT_STORED_COLOR, color.getId());
        if (isTemporary) {
            // Temporary dye lasts 5 minutes
            tag.putLong(NBT_DYE_EXPIRES, System.currentTimeMillis() + 300000);
        }
    }

    private static int calculateWoolDrops(CompoundTag tag, WoolQuality quality) {
        float thickness = getCoatThickness(tag);
        float length = getWoolLength(tag);

        // Base drops modified by quality, thickness, and length
        int baseDrops = quality.getBaseDrops();
        float multiplier = quality.getDropMultiplier() * thickness * (length / 100.0f);

        // Add randomness
        int minDrops = Math.max(1, (int) (baseDrops * multiplier * 0.8));
        int maxDrops = Math.min(4, (int) (baseDrops * multiplier * 1.2));

        return minDrops + (int) (Math.random() * (maxDrops - minDrops + 1));
    }

    private WoolGrowthConfig buildConfig(EcologyProfile profile) {
        // Growth rate: percent per tick (0.0278 = 100% in 3600 ticks = 3 minutes)
        double growthRate = profile.getDouble("production.wool_growth.growth_rate", 0.0278);

        int maxWoolDrops = profile.getInt("production.wool_growth.max_wool_drops", 4);

        QualityFactors qualityFactors = buildQualityFactors(profile);
        SeasonalModifiers seasonalModifiers = buildSeasonalModifiers(profile);

        double parasiteChance = profile.getDouble("production.wool_growth.parasite_chance", 0.0001);

        return new WoolGrowthConfig(growthRate, maxWoolDrops, qualityFactors, seasonalModifiers, parasiteChance);
    }

    private QualityFactors buildQualityFactors(EcologyProfile profile) {
        Map<String, Object> factors = profile.getMap("production.wool_growth.quality_factors");
        if (factors == null) {
            factors = Map.of(
                "diet", 1.0,
                "health", 1.2,
                "age", 0.8,
                "environment", 0.5
            );
        }

        double dietWeight = ((Number) factors.getOrDefault("diet", 1.0)).doubleValue();
        double healthWeight = ((Number) factors.getOrDefault("health", 1.2)).doubleValue();
        double ageWeight = ((Number) factors.getOrDefault("age", 0.8)).doubleValue();
        double environmentWeight = ((Number) factors.getOrDefault("environment", 0.5)).doubleValue();

        return new QualityFactors(dietWeight, healthWeight, ageWeight, environmentWeight);
    }

    private SeasonalModifiers buildSeasonalModifiers(EcologyProfile profile) {
        Map<String, Object> seasonal = profile.getMap("production.wool_growth.seasonal_thickness_modifier");
        if (seasonal == null) {
            return new SeasonalModifiers(1.3f, 0.7f, 0.8, 1.2);
        }

        float winterThickness = ((Number) seasonal.getOrDefault("winter_thickness", 1.3f)).floatValue();
        float summerThickness = ((Number) seasonal.getOrDefault("summer_thickness", 0.7f)).floatValue();
        double winterGrowthRate = ((Number) seasonal.getOrDefault("winter_growth_rate", 0.8)).doubleValue();
        double summerGrowthRate = ((Number) seasonal.getOrDefault("summer_growth_rate", 1.2)).doubleValue();

        return new SeasonalModifiers(winterThickness, summerThickness, winterGrowthRate, summerGrowthRate);
    }

    private record WoolGrowthConfig(
        double growthRate,
        int maxWoolDrops,
        QualityFactors qualityFactors,
        SeasonalModifiers seasonalThicknessModifier,
        double parasiteChance
    ) {}

    private record QualityFactors(
        double dietWeight,
        double healthWeight,
        double ageWeight,
        double environmentWeight
    ) {}

    private record SeasonalModifiers(
        float winterThickness,
        float summerThickness,
        double winterGrowthRate,
        double summerGrowthRate
    ) {}
}
