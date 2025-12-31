package me.javavirtualenv.ecology.handles.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.handles.SocialHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Handle for managing resource production in entities.
 * <p>
 * This handle tracks production of various resources (honey, slime, ink, etc.)
 * with configurable rates, capacities, and quality factors based on entity state.
 * <p>
 * Production is affected by:
 * - Diet and hunger levels
 * - Environment and conditions
 * - Happiness and social factors
 * - Seasonal modifiers
 */
public final class ResourceProductionHandle implements EcologyHandle {

    private static final String PRODUCTION_DATA_KEY = "production_data";
    private static final String RESOURCE_TYPE_KEY = "resource_type";
    private static final String AMOUNT_KEY = "amount";
    private static final String CAPACITY_KEY = "capacity";
    private static final String QUALITY_KEY = "quality";
    private static final String LAST_PRODUCTION_TIME_KEY = "last_production_time";
    private static final String POLLINATION_TARGET_KEY = "pollination_target";

    @Override
    public String id() {
        return "production";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        return profile.getBool("resource_production.enabled", false);
    }

    @Override
    public void initialize(Mob mob, EcologyComponent component, EcologyProfile profile) {
        CompoundTag productionData = component.getHandleTag(id());

        if (!productionData.contains(RESOURCE_TYPE_KEY)) {
            String resourceType = profile.getString("resource_production.resource_type", "honey");
            productionData.putString(RESOURCE_TYPE_KEY, resourceType);
        }

        if (!productionData.contains(CAPACITY_KEY)) {
            double capacity = profile.getDouble("resource_production.max_capacity", 100.0);
            productionData.putDouble(CAPACITY_KEY, capacity);
        }

        if (!productionData.contains(AMOUNT_KEY)) {
            productionData.putDouble(AMOUNT_KEY, 0.0);
        }

        if (!productionData.contains(QUALITY_KEY)) {
            productionData.putDouble(QUALITY_KEY, 1.0);
        }

        if (!productionData.contains(LAST_PRODUCTION_TIME_KEY)) {
            productionData.putLong(LAST_PRODUCTION_TIME_KEY, 0);
        }
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        CompoundTag productionData = component.getHandleTag(id());
        long currentTime = mob.level().getGameTime();

        long lastProductionTime = productionData.getLong(LAST_PRODUCTION_TIME_KEY);
        int productionInterval = profile.getInt("resource_production.production_interval", 1200);

        if (currentTime - lastProductionTime < productionInterval) {
            return;
        }

        double currentAmount = productionData.getDouble(AMOUNT_KEY);
        double capacity = productionData.getDouble(CAPACITY_KEY);

        if (currentAmount >= capacity) {
            return;
        }

        double productionAmount = calculateProductionAmount(mob, component, profile);
        double newAmount = Math.min(currentAmount + productionAmount, capacity);

        productionData.putDouble(AMOUNT_KEY, newAmount);
        productionData.putLong(LAST_PRODUCTION_TIME_KEY, currentTime);

        double quality = calculateQuality(mob, component, profile);
        productionData.putDouble(QUALITY_KEY, quality);
    }

    @Override
    public int tickInterval() {
        return 20;
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (tag.contains(id())) {
            CompoundTag productionTag = tag.getCompound(id());
            component.getHandleTag(id()).merge(productionTag);
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    /**
     * Checks if the entity can be harvested.
     * Entity must have enough accumulated production and be in valid state.
     */
    public boolean canHarvest(Mob mob, EcologyComponent component) {
        if (!mob.isAlive()) {
            return false;
        }

        CompoundTag productionData = component.getHandleTag(id());
        double amount = productionData.getDouble(AMOUNT_KEY);
        double minHarvestAmount = 10.0;

        return amount >= minHarvestAmount;
    }

    /**
     * Harvests resources from the entity.
     * Returns the harvested item stack or null if harvest failed.
     */
    public ItemStack harvest(Mob mob, EcologyComponent component, ServerPlayer player) {
        if (!canHarvest(mob, component)) {
            return ItemStack.EMPTY;
        }

        CompoundTag productionData = component.getHandleTag(id());
        double amount = productionData.getDouble(AMOUNT_KEY);
        double quality = productionData.getDouble(QUALITY_KEY);
        String resourceType = productionData.getString(RESOURCE_TYPE_KEY);

        ItemStack result = createHarvestItem(resourceType, amount, quality);

        productionData.putDouble(AMOUNT_KEY, 0.0);
        productionData.putDouble(QUALITY_KEY, 1.0);

        return result;
    }

    /**
     * Gets the current production amount.
     */
    public double getProductionAmount(Mob mob, EcologyComponent component) {
        CompoundTag productionData = component.getHandleTag(id());
        return productionData.getDouble(AMOUNT_KEY);
    }

    /**
     * Gets the current resource quality.
     */
    public double getQuality(Mob mob, EcologyComponent component) {
        CompoundTag productionData = component.getHandleTag(id());
        return productionData.getDouble(QUALITY_KEY);
    }

    /**
     * Gets the resource type being produced.
     */
    public String getResourceType(Mob mob, EcologyComponent component) {
        CompoundTag productionData = component.getHandleTag(id());
        return productionData.getString(RESOURCE_TYPE_KEY);
    }

    /**
     * Sets the current pollination target for bees.
     */
    public void setPollinationTarget(Mob mob, EcologyComponent component, String flowerType) {
        CompoundTag productionData = component.getHandleTag(id());
        productionData.putString(POLLINATION_TARGET_KEY, flowerType);
    }

    /**
     * Gets the current pollination target for bees.
     */
    public String getPollinationTarget(Mob mob, EcologyComponent component) {
        CompoundTag productionData = component.getHandleTag(id());
        if (productionData.contains(POLLINATION_TARGET_KEY)) {
            return productionData.getString(POLLINATION_TARGET_KEY);
        }
        return null;
    }

    /**
     * Calculates the production amount based on entity state and configuration.
     */
    private double calculateProductionAmount(Mob mob, EcologyComponent component, EcologyProfile profile) {
        double baseRate = profile.getDouble("resource_production.production_rate", 1.0);

        double conditionFactor = getConditionFactor(component);
        double hungerFactor = getHungerFactor(component);
        double socialFactor = getSocialFactor(component);
        double seasonalFactor = getSeasonalFactor(mob, profile);

        return baseRate * conditionFactor * hungerFactor * socialFactor * seasonalFactor;
    }

    /**
     * Calculates resource quality based on entity state.
     */
    private double calculateQuality(Mob mob, EcologyComponent component, EcologyProfile profile) {
        double baseQuality = profile.getDouble("resource_production.base_quality", 1.0);

        double conditionBonus = getConditionBonus(component);
        double happinessBonus = getHappinessBonus(component);
        double environmentBonus = getEnvironmentBonus(mob, profile);

        return baseQuality + conditionBonus + happinessBonus + environmentBonus;
    }

    /**
     * Gets condition factor (0.0-1.0) based on entity condition.
     */
    private double getConditionFactor(EcologyComponent component) {
        CompoundTag conditionData = component.getHandleTag("condition");
        if (conditionData.isEmpty()) {
            return 0.5;
        }

        double condition = conditionData.getDouble("value");
        return Math.max(0.2, Math.min(1.0, condition / 100.0));
    }

    /**
     * Gets hunger factor (0.5-1.0) based on hunger level.
     */
    private double getHungerFactor(EcologyComponent component) {
        CompoundTag hungerData = component.getHandleTag("hunger");
        if (hungerData.isEmpty()) {
            return 0.8;
        }

        double hunger = hungerData.getDouble("value");
        return Math.max(0.5, Math.min(1.0, hunger / 80.0));
    }

    /**
     * Gets social factor (0.5-1.0) based on social fulfillment.
     */
    private double getSocialFactor(EcologyComponent component) {
        CompoundTag socialData = component.getHandleTag("social");
        if (socialData.isEmpty()) {
            return 0.7;
        }

        double social = socialData.getDouble("value");
        return Math.max(0.5, Math.min(1.0, social / 100.0));
    }

    /**
     * Gets seasonal modifier for production.
     */
    private double getSeasonalFactor(Mob mob, EcologyProfile profile) {
        long dayTime = mob.level().getDayTime() % 24000;

        boolean isNight = dayTime >= 13000 && dayTime < 23000;
        boolean isRaining = mob.level().isRaining();
        boolean isThunder = mob.level().isThundering();

        if (isNight || isRaining || isThunder) {
            return profile.getDouble("resource_production.inactive_modifier", 0.0);
        }

        return profile.getDouble("resource_production.active_modifier", 1.0);
    }

    /**
     * Gets quality bonus from condition.
     */
    private double getConditionBonus(EcologyComponent component) {
        CompoundTag conditionData = component.getHandleTag("condition");
        if (conditionData.isEmpty()) {
            return 0.0;
        }

        double condition = conditionData.getDouble("value");
        if (condition >= 85) {
            return 0.2;
        } else if (condition >= 65) {
            return 0.1;
        }
        return 0.0;
    }

    /**
     * Gets quality bonus from happiness/social.
     */
    private double getHappinessBonus(EcologyComponent component) {
        CompoundTag socialData = component.getHandleTag("social");
        if (socialData.isEmpty()) {
            return 0.0;
        }

        double social = socialData.getDouble("value");
        if (social >= 80) {
            return 0.15;
        } else if (social >= 60) {
            return 0.05;
        }
        return 0.0;
    }

    /**
     * Gets quality bonus from environment.
     */
    private double getEnvironmentBonus(Mob mob, EcologyProfile profile) {
        long dayTime = mob.level().getDayTime() % 24000;
        boolean isDay = dayTime >= 0 && dayTime < 13000;
        boolean isClear = !mob.level().isRaining() && !mob.level().isThundering();

        if (isDay && isClear) {
            return profile.getDouble("resource_quality.environment_bonus", 0.1);
        }
        return 0.0;
    }

    /**
     * Creates the harvested item stack based on resource type and quality.
     */
    private ItemStack createHarvestItem(String resourceType, double amount, double quality) {
        return switch (resourceType) {
            case "honey" -> createHoneyItem(amount, quality);
            case "slime" -> createSlimeItem(amount, quality);
            case "ink" -> createInkItem(amount, quality);
            case "glow_ink" -> createGlowInkItem(amount, quality);
            case "ancient_seed" -> createAncientSeedItem(amount, quality);
            case "moss" -> createMossItem(amount, quality);
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack createHoneyItem(double amount, double quality) {
        int honeyBottles = (int) Math.floor(amount / 20.0);
        if (honeyBottles < 1) {
            honeyBottles = 1;
        }

        ItemStack honeyBottle = new ItemStack(net.minecraft.world.item.Items.HONEY_BOTTLE, honeyBottles);

        if (quality > 1.2) {
            honeyBottle.setHoverName(
                net.minecraft.network.chat.Component.literal("Premium " + honeyBottle.getHoverName().getString())
            );
        }

        return honeyBottle;
    }

    private ItemStack createSlimeItem(double amount, double quality) {
        int slimeBalls = (int) Math.floor(amount / 15.0);
        if (slimeBalls < 1) {
            slimeBalls = 1;
        }

        return new ItemStack(net.minecraft.world.item.Items.SLIME_BALL, slimeBalls);
    }

    private ItemStack createInkItem(double amount, double quality) {
        int inkSacs = (int) Math.floor(amount / 10.0);
        if (inkSacs < 1) {
            inkSacs = 1;
        }

        return new ItemStack(net.minecraft.world.item.Items.INK_SAC, inkSacs);
    }

    private ItemStack createGlowInkItem(double amount, double quality) {
        int inkSacs = (int) Math.floor(amount / 10.0);
        if (inkSacs < 1) {
            inkSacs = 1;
        }

        return new ItemStack(net.minecraft.world.item.Items.GLOW_INK_SAC, inkSacs);
    }

    private ItemStack createAncientSeedItem(double amount, double quality) {
        int seeds = (int) Math.floor(amount / 25.0);
        if (seeds < 1) {
            seeds = 1;
        }

        return new ItemStack(net.minecraft.world.item.Items.TORCHFLOWER_SEEDS, seeds);
    }

    private ItemStack createMossItem(double amount, double quality) {
        int moss = (int) Math.floor(amount / 20.0);
        if (moss < 1) {
            moss = 1;
        }

        return new ItemStack(net.minecraft.world.item.Items.MOSS_BLOCK, moss);
    }
}
