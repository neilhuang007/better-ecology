package me.javavirtualenv.behavior.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.handles.production.MilkProductionHandle;
import me.javavirtualenv.ecology.handles.SocialHandle;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

/**
 * AI goal for dairy animals to seek food when milk levels are low.
 *
 * <p>Behaviors:
 * <ul>
 *   <li>Seek high-quality foods (wheat, golden carrots) when milk is low</li>
 *   <li>Prefer grass blocks and wheat over other foods</li>
 *   <li>Social behavior - herd together when approaching milking area</li>
 *   <li>Cooperative response to players (don't flee when approached with bucket)</li>
 * </ul>
 *
 * <p>Scientific basis:
 * <ul>
 *   <li>Dairy cows require additional nutrition for milk production</li>
 *   <li>Preference for high-quality forage increases with lactation demand</li>
 *   <li>Herd animals show synchronized feeding behavior</li>
 * </ul>
 */
public class MilkingGoal extends Goal {
    private final PathfinderMob mob;
    private final MilkingConfig config;

    private Player targetedPlayer;
    private int cooldownTicks;

    public MilkingGoal(PathfinderMob mob, MilkingConfig config) {
        this.mob = mob;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Only activate if milk level is low and animal is hungry
        EcologyComponent component = getEcologyComponent();
        if (component == null || !component.hasProfile()) {
            return false;
        }

        MilkProductionHandle milkHandle = getMilkProductionHandle(component);
        if (milkHandle == null) {
            return false;
        }

        int milkLevel = milkHandle.getMilkLevel(component);
        int hungerLevel = HungerHandle.getHungerLevel(component);

        // Seek food when milk is low and hunger is below threshold
        boolean needsNutrition = milkLevel < config.lowMilkThreshold() ||
                                hungerLevel < config.hungerThreshold();

        if (!needsNutrition) {
            return false;
        }

        // Look for nearby players with food
        this.targetedPlayer = findPlayerWithFood();
        return targetedPlayer != null || findNearbyFood();
    }

    @Override
    public boolean canContinueToUse() {
        if (targetedPlayer != null) {
            // Continue following player if they still have food
            return playerHasFood(targetedPlayer) &&
                   mob.distanceTo(targetedPlayer) < config.followRange();
        }

        // Continue if we're moving toward food
        return mob.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        if (targetedPlayer != null) {
            // Follow the player with food
            mob.getNavigation().moveTo(targetedPlayer, config.moveSpeed());
        }
    }

    @Override
    public void stop() {
        targetedPlayer = null;
        cooldownTicks = config.cooldownTicks();
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetedPlayer != null) {
            // Continue following player
            double distance = mob.distanceTo(targetedPlayer);

            if (distance > config.stopDistance()) {
                mob.getNavigation().moveTo(targetedPlayer, config.moveSpeed());
            }

            // Look at player
            mob.getLookControl().setLookAt(targetedPlayer, 30.0f, 30.0f);
        }
    }

    /**
     * Find a nearby player holding food that the animal likes.
     */
    private Player findPlayerWithFood() {
        return mob.level().getEntitiesOfClass(Player.class,
                mob.getBoundingBox().inflate(config.detectionRange()))
                .stream()
                .filter(this::playerHasFood)
                .filter(player -> !player.isSpectator() && player.isAlive())
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if player is holding food the animal likes.
     */
    private boolean playerHasFood(Player player) {
        var mainHand = player.getMainHandItem();
        var offHand = player.getOffhandItem();

        return isFavoriteFood(mainHand) || isFavoriteFood(offHand);
    }

    /**
     * Check if item stack is a favorite food.
     */
    private boolean isFavoriteFood(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Wheat is the primary favorite food
        if (stack.is(Items.WHEAT)) {
            return true;
        }

        // Golden carrots (high-quality food)
        if (stack.is(Items.GOLDEN_CARROT)) {
            return true;
        }

        // Hay bales (compressed food)
        if (stack.is(Items.HAY_BLOCK)) {
            return true;
        }

        return false;
    }

    /**
     * Check if there's nearby food (grass blocks).
     */
    private boolean findNearbyFood() {
        // Look for grass blocks nearby
        var pos = mob.blockPosition();
        int searchRadius = config.foodSearchRadius();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                var checkPos = pos.offset(x, 0, z);
                Block block = mob.level().getBlockState(checkPos.below()).getBlock();

                if (block == Blocks.GRASS_BLOCK || block == Blocks.TALL_GRASS) {
                    return true;
                }
            }
        }

        return false;
    }

    private EcologyComponent getEcologyComponent() {
        if (!(mob instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    private MilkProductionHandle getMilkProductionHandle(EcologyComponent component) {
        // Get the handle from the component's profile
        EcologyProfile profile = component.getProfile();
        if (!profile.hasHandle("milk_production")) {
            return null;
        }
        return new MilkProductionHandle();
    }

    /**
     * Configuration for milking behavior.
     */
    public static class MilkingConfig {
        private final int lowMilkThreshold;
        private final int hungerThreshold;
        private final double moveSpeed;
        private final int detectionRange;
        private final int followRange;
        private final double stopDistance;
        private final int foodSearchRadius;
        private final int cooldownTicks;

        public MilkingConfig(int lowMilkThreshold, int hungerThreshold, double moveSpeed,
                            int detectionRange, int followRange, double stopDistance,
                            int foodSearchRadius, int cooldownTicks) {
            this.lowMilkThreshold = lowMilkThreshold;
            this.hungerThreshold = hungerThreshold;
            this.moveSpeed = moveSpeed;
            this.detectionRange = detectionRange;
            this.followRange = followRange;
            this.stopDistance = stopDistance;
            this.foodSearchRadius = foodSearchRadius;
            this.cooldownTicks = cooldownTicks;
        }

        public int lowMilkThreshold() {
            return lowMilkThreshold;
        }

        public int hungerThreshold() {
            return hungerThreshold;
        }

        public double moveSpeed() {
            return moveSpeed;
        }

        public int detectionRange() {
            return detectionRange;
        }

        public int followRange() {
            return followRange;
        }

        public double stopDistance() {
            return stopDistance;
        }

        public int foodSearchRadius() {
            return foodSearchRadius;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        /**
         * Create config from YAML profile.
         */
        public static MilkingConfig fromProfile(EcologyProfile profile) {
            int lowMilkThreshold = profile.getInt("milk_production.ai.low_milk_threshold", 50);
            int hungerThreshold = profile.getInt("milk_production.ai.hunger_threshold", 60);
            double moveSpeed = profile.getDouble("milk_production.ai.move_speed", 1.0);
            int detectionRange = profile.getInt("milk_production.ai.detection_range", 16);
            int followRange = profile.getInt("milk_production.ai.follow_range", 24);
            double stopDistance = profile.getDouble("milk_production.ai.stop_distance", 3.0);
            int foodSearchRadius = profile.getInt("milk_production.ai.food_search_radius", 20);
            int cooldownTicks = profile.getInt("milk_production.ai.cooldown_ticks", 200);

            return new MilkingConfig(lowMilkThreshold, hungerThreshold, moveSpeed,
                    detectionRange, followRange, stopDistance, foodSearchRadius, cooldownTicks);
        }
    }
}
