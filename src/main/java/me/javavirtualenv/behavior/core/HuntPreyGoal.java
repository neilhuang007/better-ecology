package me.javavirtualenv.behavior.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal that makes predators hunt prey when hungry and no food items are available.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Activates when {@link AnimalNeeds#isHungry(Mob)} returns true AND no food items nearby</li>
 *   <li>Searches for prey entities within a configurable hunt range</li>
 *   <li>Selects prey based on proximity, health, and isolation</li>
 *   <li>Sets prey as target and pathfinds toward it</li>
 *   <li>Delegates attack behavior to the mob's natural attack mechanics</li>
 *   <li>Restores hunger when prey is killed</li>
 *   <li>Stops hunting when prey dies, escapes, or predator is satisfied</li>
 * </ul>
 *
 * <p>Priority: {@link AnimalThresholds#PRIORITY_HUNT} (4) - Lower than flee/water/food-item goals
 */
public class HuntPreyGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuntPreyGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 5;
    private static final int GIVE_UP_TICKS = 1200;
    private static final int ESCAPE_DISTANCE_SQUARED = 1024;
    private static final int PATH_RECALCULATION_INTERVAL = 10;  // More frequent path updates for smoother hunting
    private static final float HUNGER_RESTORE_ON_KILL = 40f;
    private static final double FOOD_ITEM_CHECK_RADIUS = 16.0;
    private static final double ATTACK_DISTANCE_SQUARED = 4.0;  // 2 blocks
    private static final int ATTACK_COOLDOWN_TICKS = 20;

    private final PathfinderMob mob;
    private final double speedModifier;
    private final int huntRange;
    private final List<Class<? extends LivingEntity>> preyTypes;

    private LivingEntity targetPrey;
    private int searchCooldown;
    private int huntTicks;
    private int pathRecalculationTimer;
    private int attackCooldown;
    private boolean preyWasKilled;

    /**
     * Creates a new HuntPreyGoal.
     *
     * @param mob the predator mob that will hunt
     * @param speedModifier movement speed multiplier when hunting
     * @param huntRange maximum distance to search for prey
     * @param preyTypes valid prey entity types to hunt
     */
    @SafeVarargs
    public HuntPreyGoal(PathfinderMob mob, double speedModifier, int huntRange, Class<? extends LivingEntity>... preyTypes) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.huntRange = huntRange;
        this.preyTypes = Arrays.asList(preyTypes);
        this.searchCooldown = 0;
        this.huntTicks = 0;
        this.pathRecalculationTimer = 0;
        this.attackCooldown = 0;
        this.preyWasKilled = false;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Check if we need to hunt - either we're hungry or a pack member is hungry
        boolean needsFood = AnimalNeeds.isHungry(this.mob) || hasHungryPackMember();

        if (!needsFood) {
            LOGGER.warn("{} canUse: not hungry (hunger={})", this.mob.getName().getString(), AnimalNeeds.getHunger(this.mob));
            return false;
        }

        // ALWAYS check for food items first - don't hunt if easier food is available
        if (hasFoodItemsNearby()) {
            LOGGER.warn("{} found food items nearby, not hunting prey", this.mob.getName().getString());
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);
        boolean foundPrey = findAndTargetPrey();
        LOGGER.warn("{} canUse: foundPrey={}, hunger={}", this.mob.getName().getString(), foundPrey, AnimalNeeds.getHunger(this.mob));
        return foundPrey;
    }

    /**
     * Checks if this mob is a wolf with hungry pack members nearby.
     *
     * @return true if a wolf has hungry pack members
     */
    private boolean hasHungryPackMember() {
        if (this.mob instanceof Wolf wolf) {
            return WolfPackData.hasHungryPackMember(wolf, 32.0);
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we're satisfied AND no pack members are hungry
        if (AnimalNeeds.isSatisfied(this.mob) && !hasHungryPackMember()) {
            LOGGER.debug("{} is now satisfied and no hungry pack members, stopping hunt", this.mob.getName().getString());
            return false;
        }

        if (this.huntTicks >= GIVE_UP_TICKS) {
            LOGGER.debug("{} gave up hunting after {} ticks", this.mob.getName().getString(), this.huntTicks);
            return false;
        }

        if (this.targetPrey == null || !this.targetPrey.isAlive()) {
            if (this.preyWasKilled) {
                restoreHungerFromKill();
                this.preyWasKilled = false;
            }
            LOGGER.debug("{} lost prey target", this.mob.getName().getString());
            return false;
        }

        if (isPreyTooFar()) {
            LOGGER.debug("{} prey escaped beyond range", this.mob.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LOGGER.debug("{} starting hunt for {}", this.mob.getName().getString(),
            this.targetPrey != null ? this.targetPrey.getName().getString() : "null");
        this.huntTicks = 0;
        this.pathRecalculationTimer = 0;
        this.preyWasKilled = false;

        if (this.targetPrey != null) {
            this.mob.setTarget(this.targetPrey);
            pathfindToPrey();
        }
    }

    @Override
    public void stop() {
        LOGGER.debug("{} stopped hunting. Hunger: {}", this.mob.getName().getString(), AnimalNeeds.getHunger(this.mob));
        this.targetPrey = null;
        this.mob.setTarget(null);
        this.mob.getNavigation().stop();
        this.huntTicks = 0;
        this.pathRecalculationTimer = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.huntTicks++;
        this.pathRecalculationTimer++;

        if (this.targetPrey == null) {
            return;
        }

        if (!this.targetPrey.isAlive() && this.mob.getTarget() == this.targetPrey) {
            this.preyWasKilled = true;
            return;
        }

        this.mob.getLookControl().setLookAt(this.targetPrey, 30.0F, 30.0F);

        // Decrement attack cooldown
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        // Check if close enough to attack
        double distanceSq = this.mob.distanceToSqr(this.targetPrey);
        if (distanceSq <= ATTACK_DISTANCE_SQUARED && this.attackCooldown <= 0) {
            // Attack the prey
            this.mob.doHurtTarget(this.targetPrey);
            this.attackCooldown = ATTACK_COOLDOWN_TICKS;
            LOGGER.debug("{} attacked {} at distance {}",
                this.mob.getName().getString(),
                this.targetPrey.getName().getString(),
                Math.sqrt(distanceSq));
        }

        // Continuously ensure we're moving toward prey
        // If navigation stopped or path needs recalculation, update path
        if (this.mob.getNavigation().isDone() || shouldRecalculatePath()) {
            pathfindToPrey();
            this.pathRecalculationTimer = 0;
        }
    }

    /**
     * Checks if meat items are available nearby.
     * Wolves prefer to eat existing meat rather than hunt.
     *
     * @return true if meat items are within detection range
     */
    private boolean hasFoodItemsNearby() {
        AABB searchBox = this.mob.getBoundingBox().inflate(FOOD_ITEM_CHECK_RADIUS);
        List<ItemEntity> meatItems = this.mob.level().getEntitiesOfClass(ItemEntity.class, searchBox, this::isMeatItem);
        return !meatItems.isEmpty();
    }

    /**
     * Checks if an item entity contains meat that wolves can eat.
     *
     * @param itemEntity the item entity to check
     * @return true if the item is meat
     */
    private boolean isMeatItem(ItemEntity itemEntity) {
        if (itemEntity == null || !itemEntity.isAlive()) {
            return false;
        }
        ItemStack stack = itemEntity.getItem();
        return stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF) ||
               stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP) ||
               stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON) ||
               stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN) ||
               stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT) ||
               stack.is(Items.ROTTEN_FLESH);
    }

    /**
     * Finds and targets the best prey within hunt range.
     *
     * @return true if valid prey was found, false otherwise
     */
    private boolean findAndTargetPrey() {
        AABB searchBox = this.mob.getBoundingBox().inflate(this.huntRange);
        List<LivingEntity> potentialPrey = this.mob.level().getEntitiesOfClass(LivingEntity.class, searchBox, this::isValidPrey);

        if (potentialPrey.isEmpty()) {
            LOGGER.debug("{} found no prey within {} blocks", this.mob.getName().getString(), this.huntRange);
            return false;
        }

        LivingEntity selectedPrey = selectBestPrey(potentialPrey);

        if (selectedPrey != null) {
            this.targetPrey = selectedPrey;
            LOGGER.debug("{} selected prey: {} at distance {}",
                this.mob.getName().getString(),
                selectedPrey.getName().getString(),
                Math.sqrt(this.mob.distanceToSqr(selectedPrey)));
            return true;
        }

        return false;
    }

    /**
     * Validates if an entity is valid prey for hunting.
     *
     * @param entity the entity to validate
     * @return true if the entity can be hunted
     */
    private boolean isValidPrey(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        if (entity == this.mob) {
            return false;
        }

        if (entity.isInvulnerable()) {
            return false;
        }

        for (Class<? extends LivingEntity> preyType : this.preyTypes) {
            if (preyType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Selects the best prey from a list of potential targets.
     * Prefers prey that is nearest, weaker (lower health), or isolated.
     *
     * @param potentialPrey list of valid prey entities
     * @return the selected prey, or null if none suitable
     */
    private LivingEntity selectBestPrey(List<LivingEntity> potentialPrey) {
        return potentialPrey.stream()
            .min(Comparator
                .comparingDouble(this::calculatePreyScore))
            .orElse(null);
    }

    /**
     * Calculates a hunting priority score for a prey entity.
     * Lower scores are preferred (nearest, weakest, most isolated).
     *
     * @param prey the prey entity to score
     * @return hunting priority score
     */
    private double calculatePreyScore(LivingEntity prey) {
        double distance = this.mob.distanceToSqr(prey);
        double healthFactor = prey.getHealth() / prey.getMaxHealth();
        double isolationFactor = calculateIsolationFactor(prey);

        return (distance * 0.5) + (healthFactor * 100) + (isolationFactor * 50);
    }

    /**
     * Calculates how isolated a prey entity is from others of its kind.
     * More isolated prey (fewer nearby allies) get lower scores.
     *
     * @param prey the prey entity to check
     * @return isolation factor (0 = very isolated, higher = more allies nearby)
     */
    private double calculateIsolationFactor(LivingEntity prey) {
        AABB nearbyBox = prey.getBoundingBox().inflate(8.0);
        long nearbyCount = this.mob.level().getEntitiesOfClass(LivingEntity.class, nearbyBox)
            .stream()
            .filter(entity -> entity.getClass() == prey.getClass())
            .filter(entity -> entity != prey && entity.isAlive())
            .count();

        return nearbyCount;
    }

    /**
     * Checks if the prey has escaped beyond hunting range.
     *
     * @return true if prey is too far to continue hunting
     */
    private boolean isPreyTooFar() {
        if (this.targetPrey == null) {
            return true;
        }

        double distanceSq = this.mob.distanceToSqr(this.targetPrey);
        return distanceSq > ESCAPE_DISTANCE_SQUARED;
    }

    /**
     * Initiates pathfinding toward the target prey.
     */
    private void pathfindToPrey() {
        if (this.targetPrey == null) {
            return;
        }

        this.mob.getNavigation().moveTo(this.targetPrey, this.speedModifier);
    }

    /**
     * Determines if the path should be recalculated.
     *
     * @return true if path recalculation is needed
     */
    private boolean shouldRecalculatePath() {
        return this.pathRecalculationTimer >= PATH_RECALCULATION_INTERVAL;
    }

    /**
     * Restores hunger when prey is successfully killed.
     */
    private void restoreHungerFromKill() {
        float previousHunger = AnimalNeeds.getHunger(this.mob);
        AnimalNeeds.modifyHunger(this.mob, HUNGER_RESTORE_ON_KILL);
        float newHunger = AnimalNeeds.getHunger(this.mob);

        LOGGER.debug("{} killed prey and restored hunger: {} -> {} (+{})",
            this.mob.getName().getString(),
            String.format("%.1f", previousHunger),
            String.format("%.1f", newHunger),
            String.format("%.1f", HUNGER_RESTORE_ON_KILL));
    }
}
