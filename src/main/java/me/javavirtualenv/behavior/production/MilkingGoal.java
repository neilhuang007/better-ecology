package me.javavirtualenv.behavior.production;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

/**
 * Goal for dairy animals (cows, goats) to seek food when milk levels are low.
 * <p>
 * Behaviors:
 * <ul>
 *   <li>Seek high-quality foods (wheat, golden carrots) when milk is low</li>
 *   <li>Prefer grass blocks and wheat over other foods</li>
 *   <li>Social behavior - herd together when approaching milking area</li>
 *   <li>Cooperative response to players (don't flee when approached with bucket)</li>
 * </ul>
 * <p>
 * Scientific basis:
 * <ul>
 *   <li>Dairy cows require additional nutrition for milk production</li>
 *   <li>Preference for high-quality forage increases with lactation demand</li>
 *   <li>Herd animals show synchronized feeding behavior</li>
 * </ul>
 */
public class MilkingGoal extends Goal {

    // NBT keys
    private static final String MILK_COOLDOWN_KEY = "milk_production_cooldown";

    // Configuration constants
    private static final int LOW_MILK_THRESHOLD = 50;
    private static final int HUNGER_THRESHOLD = 60;
    private static final double MOVE_SPEED = 1.0;
    private static final int DETECTION_RANGE = 16;
    private static final int FOLLOW_RANGE = 24;
    private static final double STOP_DISTANCE = 3.0;
    private static final int FOOD_SEARCH_RADIUS = 20;
    private static final int COOLDOWN_TICKS = 200;

    // Instance fields
    private final PathfinderMob mob;
    private Player targetedPlayer;
    private BlockPos targetFoodPos;
    private int cooldownTicks;

    // Debug info
    private String lastDebugMessage = "";

    public MilkingGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (mob.level().isClientSide) {
            return false;
        }

        // Handle cooldown
        CompoundTag productionTag = getProductionTag();
        if (productionTag != null) {
            cooldownTicks = productionTag.getInt(MILK_COOLDOWN_KEY);
            if (cooldownTicks > 0) {
                productionTag.putInt(MILK_COOLDOWN_KEY, cooldownTicks - 1);
                return false;
            }
        }

        // Check if nutrition is needed
        if (!needsNutrition()) {
            return false;
        }

        // Look for nearby players with food
        targetedPlayer = findPlayerWithFood();
        if (targetedPlayer != null) {
            debug("STARTING: following player with food (distance=" +
                  String.format("%.1f", mob.distanceTo(targetedPlayer)) + ")");
            return true;
        }

        // Look for nearby grass/food
        targetFoodPos = findNearbyGrass();
        if (targetFoodPos != null) {
            debug("STARTING: seeking grass at " + targetFoodPos.getX() + "," + targetFoodPos.getZ());
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetedPlayer != null) {
            boolean playerHasFood = playerHasFood(targetedPlayer);
            boolean inRange = mob.distanceTo(targetedPlayer) < FOLLOW_RANGE;
            if (!playerHasFood) {
                debug("player no longer has food");
            }
            if (!inRange) {
                debug("player out of range");
            }
            return playerHasFood && inRange;
        }

        if (targetFoodPos != null) {
            boolean closeEnough = targetFoodPos.closerToCenterThan(mob.position(), STOP_DISTANCE);
            boolean moving = mob.getNavigation().isInProgress();
            return !closeEnough && moving;
        }

        return false;
    }

    @Override
    public void start() {
        if (targetedPlayer != null) {
            mob.getNavigation().moveTo(targetedPlayer, MOVE_SPEED);
            debug("pathing to player with food");
        } else if (targetFoodPos != null) {
            mob.getNavigation().moveTo(targetFoodPos.getX(), targetFoodPos.getY(),
                targetFoodPos.getZ(), MOVE_SPEED * 0.8);
            debug("pathing to grass block");
        }
    }

    @Override
    public void stop() {
        debug("goal stopped");
        targetedPlayer = null;
        targetFoodPos = null;
        mob.getNavigation().stop();

        // Set cooldown in NBT
        CompoundTag productionTag = getProductionTag();
        if (productionTag != null) {
            productionTag.putInt(MILK_COOLDOWN_KEY, COOLDOWN_TICKS);
        }
    }

    @Override
    public void tick() {
        if (targetedPlayer != null) {
            double distance = mob.distanceTo(targetedPlayer);

            if (distance > STOP_DISTANCE) {
                mob.getNavigation().moveTo(targetedPlayer, MOVE_SPEED);
            }

            // Look at player
            mob.getLookControl().setLookAt(targetedPlayer, 30.0f, 30.0f);

            // Log progress every second
            if (mob.tickCount % 20 == 0) {
                debug("following player, distance=" + String.format("%.1f", distance));
            }
        } else if (targetFoodPos != null) {
            double distance = mob.position().distanceTo(
                net.minecraft.world.phys.Vec3.atCenterOf(targetFoodPos));

            if (distance > STOP_DISTANCE) {
                mob.getNavigation().moveTo(targetFoodPos.getX(), targetFoodPos.getY(),
                targetFoodPos.getZ(), MOVE_SPEED * 0.8);
            }

            mob.getLookControl().setLookAt(
                targetFoodPos.getX() + 0.5,
                targetFoodPos.getY(),
                targetFoodPos.getZ() + 0.5,
                30.0f, 30.0f
            );

            // Log progress every second
            if (mob.tickCount % 20 == 0) {
                debug("moving to grass, distance=" + String.format("%.1f", distance));
            }

            // Eat grass when close enough
            if (distance < 1.5) {
                eatGrass(targetFoodPos);
                targetFoodPos = null;
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Check if animal needs nutrition based on milk and hunger levels.
     */
    private boolean needsNutrition() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return false;
        }

        CompoundTag milkTag = component.getHandleTag("milk_production");
        CompoundTag hungerTag = component.getHandleTag("hunger");

        int milkLevel = milkTag != null ? milkTag.getInt("milk_amount") : 100;
        int hungerLevel = hungerTag != null ? hungerTag.getInt("hunger") : 100;

        boolean milkLow = milkLevel < LOW_MILK_THRESHOLD;
        boolean hungerLow = hungerLevel < HUNGER_THRESHOLD;

        return milkLow || hungerLow;
    }

    /**
     * Find a nearby player holding food.
     */
    private Player findPlayerWithFood() {
        return mob.level().getEntitiesOfClass(
            Player.class,
            mob.getBoundingBox().inflate(DETECTION_RANGE)
        ).stream()
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
     * Find nearby grass blocks to eat.
     */
    private BlockPos findNearbyGrass() {
        BlockPos mobPos = mob.blockPosition();
        BlockPos nearestGrass = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -FOOD_SEARCH_RADIUS; x <= FOOD_SEARCH_RADIUS; x++) {
            for (int z = -FOOD_SEARCH_RADIUS; z <= FOOD_SEARCH_RADIUS; z++) {
                BlockPos checkPos = mobPos.offset(x, 0, z);
                Block block = mob.level().getBlockState(checkPos.below()).getBlock();

                if (block == Blocks.GRASS_BLOCK) {
                    double distance = mobPos.distSqr(checkPos);
                    if (distance < nearestDist) {
                        nearestDist = distance;
                        nearestGrass = checkPos;
                    }
                }
            }
        }

        return nearestGrass;
    }

    /**
     * Eat grass at the specified position.
     */
    private void eatGrass(BlockPos pos) {
        Block blockBelow = mob.level().getBlockState(pos.below()).getBlock();

        if (blockBelow == Blocks.GRASS_BLOCK) {
            // Turn grass to dirt (eating it)
            mob.level().setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);

            // Restore some hunger
            EcologyComponent component = getComponent();
            if (component != null) {
                CompoundTag hungerTag = component.getHandleTag("hunger");
                int currentHunger = hungerTag.getInt("hunger");
                hungerTag.putInt("hunger", Math.min(100, currentHunger + 10));
            }

            debug("ate grass at " + pos.getX() + "," + pos.getZ());
        }
    }

    /**
     * Get the production tag for cooldown tracking.
     */
    private CompoundTag getProductionTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }

        CompoundTag tag = component.getHandleTag("milk_production");
        if (!tag.contains(MILK_COOLDOWN_KEY)) {
            tag.putInt(MILK_COOLDOWN_KEY, 0);
        }
        return tag;
    }

    /**
     * Get the ecology component for this mob.
     */
    private EcologyComponent getComponent() {
        if (!(mob instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Get current milk level.
     */
    private int getMilkLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100;
        }

        CompoundTag tag = component.getHandleTag("milk_production");
        return tag.contains("milk_amount") ? tag.getInt("milk_amount") : 100;
    }

    /**
     * Get current hunger level.
     */
    private int getHungerLevel() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return 100;
        }

        CompoundTag tag = component.getHandleTag("hunger");
        return tag.contains("hunger") ? tag.getInt("hunger") : 100;
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[Milking] " + mob.getType().getDescription().getString() + " #" +
                           mob.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        return String.format(
            "milk=%d, hunger=%d, target=%s, cooldown=%d, moving=%s",
            getMilkLevel(),
            getHungerLevel(),
            targetedPlayer != null ? "player" : (targetFoodPos != null ? "grass" : "none"),
            cooldownTicks,
            mob.getNavigation().isInProgress() ? "yes" : "no"
        );
    }
}
