package me.javavirtualenv.mixin.animal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.WolfBehaviorHandle;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Mixin for Wolf entity behavior registration.
 * Wolves are pack hunters with complex social behaviors, territorial instincts,
 * and taming capabilities. This mixin implements comprehensive wolf behaviors
 * including:
 * <p>
 * - Pack Hunting: Coordinate hunting with pack members, flanking behavior,
 * alpha leadership
 * - Territorial Behavior: Mark and defend territory from intruder packs
 * - Social Hierarchy: Alpha/beta/omega hierarchy, dominance displays, pack
 * bonding
 * - Predator Interactions: Hunt sheep, rabbits, foxes; avoid bears and stronger
 * predators
 * <p>
 * Behaviors are based on scientific research into wolf pack dynamics.
 */
@Mixin(Wolf.class)
public abstract class WolfMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("BetterEcology/WolfMixin");
    private static final String WOLF_ID = "minecraft:wolf";

    // Static initializer to register behaviors before any wolf is created
    // This ensures the config is available when Mob.registerGoals() is called
    static {
        LOGGER.info("WolfMixin static initializer - registering wolf behaviors");
        registerWolfBehaviors();
        LOGGER.info("Wolf behaviors registered successfully");
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Wolf> entityType, Level level, CallbackInfo ci) {
        // Behaviors are already registered in static initializer
        // This injection is kept for potential future initialization per-instance
    }

    private static void registerWolfBehaviors() {
        AnimalConfig config = AnimalConfig.builder(
                net.minecraft.resources.ResourceLocation.parse(WOLF_ID))
                // Basic survival handles
                .addHandle(new WolfHungerHandle())
                .addHandle(new WolfThirstHandle())
                .addHandle(new WolfConditionHandle())
                .addHandle(new WolfEnergyHandle())
                .addHandle(new WolfAgeHandle())
                .addHandle(new WolfSocialHandle())
                .addHandle(new WolfHealthHandle())
                .addHandle(new WolfMovementHandle())
                .addHandle(new WolfTemporalHandle())
                .addHandle(new WolfDietHandle())
                .addHandle(new WolfPredationHandle())
                .addHandle(new WolfBreedingHandle())
                // Wolf-specific behavior handle
                .addHandle(new WolfBehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(WOLF_ID, config);
    }

    /**
     * Hunger handle with wolf-specific values.
     * Wolves have higher hunger decay due to active hunting lifestyle.
     */
    private static final class WolfHungerHandle extends CodeBasedHandle {
        private static final String NBT_HUNGER = "hunger";
        private static final String NBT_LAST_DAMAGE_TICK = "lastDamageTick";

        private static final int MAX_HUNGER = 100;
        private static final int STARTING_HUNGER = 70;
        private static final double DECAY_RATE = 0.025;
        private static final int DAMAGE_THRESHOLD = 5;
        private static final float DAMAGE_AMOUNT = 1.0f;
        private static final int DAMAGE_INTERVAL = 200;

        @Override
        public String id() {
            return "hunger";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag handleTag = component.getHandleTag(id());
            int currentHunger = getCurrentHunger(handleTag);

            long elapsedTicks = component.elapsedTicks();
            long effectiveTicks = Math.max(1, elapsedTicks);
            long scaledDecay = (long) (DECAY_RATE * effectiveTicks);
            int newHunger = (int) (currentHunger - scaledDecay);

            if (elapsedTicks > 1) {
                int safeMinimum = DAMAGE_THRESHOLD + 1;
                newHunger = Math.max(safeMinimum, newHunger);
            } else {
                newHunger = Math.max(0, newHunger);
            }
            setHunger(handleTag, newHunger);

            if (elapsedTicks <= 1 && shouldApplyStarvation(mob, newHunger)) {
                int currentTick = mob.tickCount;
                int lastDamageTick = getLastDamageTick(handleTag);
                int ticksSinceDamage = currentTick - lastDamageTick;

                if (ticksSinceDamage >= DAMAGE_INTERVAL) {
                    mob.hurt(mob.level().damageSources().starve(), DAMAGE_AMOUNT);
                    setLastDamageTick(handleTag, currentTick);
                }
            }
        }

        private int getCurrentHunger(CompoundTag handleTag) {
            if (!handleTag.contains(NBT_HUNGER)) {
                return STARTING_HUNGER;
            }
            return handleTag.getInt(NBT_HUNGER);
        }

        private void setHunger(CompoundTag handleTag, int value) {
            handleTag.putInt(NBT_HUNGER, value);
        }

        private int getLastDamageTick(CompoundTag handleTag) {
            return handleTag.getInt(NBT_LAST_DAMAGE_TICK);
        }

        private void setLastDamageTick(CompoundTag handleTag, int tick) {
            handleTag.putInt(NBT_LAST_DAMAGE_TICK, tick);
        }

        private boolean shouldApplyStarvation(Mob mob, int hunger) {
            return mob.level().getDifficulty() != Difficulty.PEACEFUL && hunger <= DAMAGE_THRESHOLD;
        }
    }

    /**
     * Thirst handle with wolf-specific values.
     */
    private static final class WolfThirstHandle extends CodeBasedHandle {
        private static final String NBT_THIRST = "thirst";
        private static final String NBT_LAST_DAMAGE_TICK = "lastThirstDamageTick";

        private static final long MAX_CATCH_UP_TICKS = 24000L; // 1 Minecraft day
        private static final int MAX_THIRST = 100;
        private static final int STARTING_THIRST = 10; // Start thirsty so wolves immediately seek water
        private static final double DECAY_RATE = 0.02;
        private static final int THIRST_THRESHOLD = 20;
        private static final int DEHYDRATION_THRESHOLD = 5;
        private static final float DEHYDRATION_DAMAGE = 1.0f;
        private static final int DAMAGE_INTERVAL = 200;

        @Override
        public String id() {
            return "thirst";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int currentThirst = getCurrentThirst(tag);

            long elapsed = component.elapsedTicks();
            // Cap elapsed ticks to prevent instant dehydration
            long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
            boolean isCatchUp = elapsed > 1;

            double decay = DECAY_RATE * effectiveTicks;
            int newThirst;

            if (isCatchUp) {
                // During catch-up, keep thirst above dehydration threshold
                int safeMinimum = DEHYDRATION_THRESHOLD + 1;
                newThirst = Math.max(safeMinimum, (int) (currentThirst - decay));
            } else {
                newThirst = (int) Math.max(0, currentThirst - decay);
            }
            setThirst(tag, newThirst);

            boolean isThirsty = newThirst < THIRST_THRESHOLD;
            component.state().setIsThirsty(isThirsty);

            // Only apply dehydration damage during active updates
            if (!isCatchUp && newThirst <= DEHYDRATION_THRESHOLD) {
                int currentTick = mob.tickCount;
                int lastDamageTick = tag.getInt(NBT_LAST_DAMAGE_TICK);
                if (currentTick - lastDamageTick >= DAMAGE_INTERVAL) {
                    if (mob.level().getDifficulty() != Difficulty.PEACEFUL) {
                        mob.hurt(mob.level().damageSources().starve(), DEHYDRATION_DAMAGE);
                        tag.putInt(NBT_LAST_DAMAGE_TICK, currentTick);
                    }
                }
            }
        }

        private int getCurrentThirst(CompoundTag tag) {
            return tag.contains(NBT_THIRST) ? tag.getInt(NBT_THIRST) : STARTING_THIRST;
        }

        private void setThirst(CompoundTag tag, int value) {
            tag.putInt(NBT_THIRST, value);
        }
    }

    /**
     * Condition handle with wolf-specific values.
     */
    private static final class WolfConditionHandle extends CodeBasedHandle {
        private static final String NBT_CONDITION = "condition";

        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 80;
        private static final double GAIN_WHEN_SATIATED = 0.02;
        private static final double LOSS_WHEN_HUNGRY = 0.025;
        private static final double LOSS_WHEN_STARVING = 0.12;

        @Override
        public String id() {
            return "condition";
        }

        @Override
        public int tickInterval() {
            return 10;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int currentCondition = getCurrentCondition(tag);

            EntityState state = component.state();
            double change;

            if (state.isStarving()) {
                change = -LOSS_WHEN_STARVING;
            } else if (state.isHungry()) {
                change = -LOSS_WHEN_HUNGRY;
            } else if (isSatiated(component)) {
                change = GAIN_WHEN_SATIATED;
            } else {
                change = 0.0;
            }

            long elapsed = component.elapsedTicks();
            change *= elapsed;

            int newCondition = (int) Math.round(Math.min(MAX_VALUE, Math.max(0, currentCondition + change)));
            setCondition(tag, newCondition);
        }

        private int getCurrentCondition(CompoundTag tag) {
            return tag.contains(NBT_CONDITION) ? tag.getInt(NBT_CONDITION) : STARTING_VALUE;
        }

        private void setCondition(CompoundTag tag, int value) {
            tag.putInt(NBT_CONDITION, value);
        }

        private boolean isSatiated(EcologyComponent component) {
            CompoundTag hungerTag = component.getHandleTag("hunger");
            if (!hungerTag.contains("hunger")) {
                return false;
            }
            return hungerTag.getInt("hunger") > 75;
        }
    }

    /**
     * Energy handle with wolf-specific values.
     * Wolves have high stamina for hunting.
     */
    private static final class WolfEnergyHandle extends CodeBasedHandle {
        private static final String NBT_ENERGY = "energy";

        private static final int MAX_VALUE = 100;
        private static final double RECOVERY_RATE = 0.8;
        private static final int EXHAUSTION_THRESHOLD = 10;
        private static final double HUNTING_COST = 0.15;
        private static final double FLEEING_COST = 0.3;

        @Override
        public String id() {
            return "energy";
        }

        @Override
        public int tickInterval() {
            return 2;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            EntityState state = component.state();
            CompoundTag tag = component.getHandleTag(id());
            int currentEnergy = getCurrentEnergy(tag);

            double cost = determineEnergyCost(state);
            double recovery = RECOVERY_RATE;

            long elapsed = component.elapsedTicks();

            int newEnergy;
            if (cost > 0) {
                newEnergy = Math.max(0, (int) Math.floor(currentEnergy - cost * elapsed));
            } else {
                newEnergy = Math.min(MAX_VALUE, (int) Math.ceil(currentEnergy + recovery * elapsed));
            }
            setEnergy(tag, newEnergy);

            boolean isExhausted = newEnergy < EXHAUSTION_THRESHOLD;
            if (isExhausted && state.isHunting()) {
                state.setIsHunting(false);
            }
        }

        private int getCurrentEnergy(CompoundTag tag) {
            return tag.contains(NBT_ENERGY) ? tag.getInt(NBT_ENERGY) : MAX_VALUE;
        }

        private void setEnergy(CompoundTag tag, int value) {
            tag.putInt(NBT_ENERGY, value);
        }

        private double determineEnergyCost(EntityState state) {
            if (state.isFleeing()) {
                return FLEEING_COST;
            }
            if (state.isHunting()) {
                return HUNTING_COST;
            }
            return 0.01;
        }
    }

    /**
     * Age handle with wolf-specific values.
     */
    private static final class WolfAgeHandle extends CodeBasedHandle {
        private static final String NBT_AGE_TICKS = "ageTicks";

        private static final long MAX_CATCH_UP_TICKS = 24000L; // 1 Minecraft day
        private static final int BABY_DURATION = 24000;

        @Override
        public String id() {
            return "age";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int ageTicks = getAgeTicks(tag);
            long elapsed = component.elapsedTicks();
            long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
            ageTicks += effectiveTicks;
            setAgeTicks(tag, ageTicks);
        }

        private int getAgeTicks(CompoundTag tag) {
            return tag.contains(NBT_AGE_TICKS) ? tag.getInt(NBT_AGE_TICKS) : 0;
        }

        private void setAgeTicks(CompoundTag tag, int value) {
            tag.putInt(NBT_AGE_TICKS, value);
        }
    }

    /**
     * Social handle for wolf pack behavior.
     */
    private static final class WolfSocialHandle extends CodeBasedHandle {
        private static final String NBT_SOCIAL = "social";

        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 70;
        private static final double DECAY_RATE = 0.02;
        private static final double RECOVERY_RATE = 0.2;
        private static final int LONELINESS_THRESHOLD = 30;
        private static final int GROUP_RADIUS = 48;

        @Override
        public String id() {
            return "social";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int currentSocial = getCurrentSocial(tag);

            boolean hasPackNearby = checkForPack(mob);

            double change = hasPackNearby ? RECOVERY_RATE : -DECAY_RATE;
            long elapsed = component.elapsedTicks();
            change *= elapsed;

            int newSocial = (int) Math.round(Math.min(MAX_VALUE, Math.max(0, currentSocial + change)));

            // Instant recovery when pack is nearby - immediately clear loneliness
            if (hasPackNearby && newSocial < LONELINESS_THRESHOLD) {
                newSocial = LONELINESS_THRESHOLD + 10; // Set above threshold
            }

            setSocial(tag, newSocial);

            boolean isLonely = newSocial < LONELINESS_THRESHOLD;
            component.state().setIsLonely(isLonely);
        }

        private int getCurrentSocial(CompoundTag tag) {
            return tag.contains(NBT_SOCIAL) ? tag.getInt(NBT_SOCIAL) : STARTING_VALUE;
        }

        private void setSocial(CompoundTag tag, int value) {
            tag.putInt(NBT_SOCIAL, value);
        }

        private boolean checkForPack(Mob mob) {
            return me.javavirtualenv.ecology.spatial.SpatialIndex.hasNearbySameType(mob, GROUP_RADIUS);
        }
    }

    /**
     * Health handle with wolf-specific values.
     */
    private static final class WolfHealthHandle extends CodeBasedHandle {

        private static final double BASE_HEALTH = 8.0;

        @Override
        public String id() {
            return "health";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            AttributeInstance healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                healthAttribute.setBaseValue(BASE_HEALTH);
            }
        }
    }

    /**
     * Movement handle with wolf-specific values.
     * Wolves are fast runners but use walk speed for casual strolling.
     */
    private static final class WolfMovementHandle extends CodeBasedHandle {

        private static final double WALK_SPEED = 0.3;
        private static final double STROLL_SPEED = 1.0; // Speed modifier for strolling (1.0 = normal walk speed)
        private static final float NORMAL_WATER_MALUS = 8.0F;
        private static final float THIRSTY_WATER_MALUS = 1.0F; // Allow pathfinding to water when thirsty

        @Override
        public String id() {
            return "movement";
        }

        @Override
        public int tickInterval() {
            return 10; // Update water malus every 0.5 seconds
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof Wolf wolf)) {
                return;
            }

            // Reduce water malus when thirsty so wolves can reach water to drink
            boolean isThirsty = component.state().isThirsty();
            float newMalus = isThirsty ? THIRSTY_WATER_MALUS : NORMAL_WATER_MALUS;
            wolf.setPathfindingMalus(PathType.WATER, newMalus);
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            AttributeInstance movementAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementAttribute != null) {
                movementAttribute.setBaseValue(WALK_SPEED);
            }

            if (!(mob instanceof Wolf wolf)) {
                return;
            }

            wolf.setPathfindingMalus(PathType.WATER, NORMAL_WATER_MALUS);

            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(0, new FloatGoal(wolf));
            // Use WaterAvoidingRandomStrollGoal to prevent wolves from walking into water
            accessor.betterEcology$getGoalSelector().addGoal(5, new WaterAvoidingRandomStrollGoal(wolf, STROLL_SPEED));
        }
    }

    /**
     * Temporal handle - wolves are crepuscular (active dawn/dusk).
     */
    private static final class WolfTemporalHandle extends CodeBasedHandle {

        @Override
        public String id() {
            return "temporal";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Wolves are active throughout the day but hunt more at dawn/dusk
            long dayTime = mob.level().getDayTime() % 24000;
            boolean isDawnOrDusk = (dayTime > 22000 || dayTime < 2000) || (dayTime > 11000 && dayTime < 14000);

            // Boost hunting state during preferred hunting times
            if (isDawnOrDusk) {
                component.state().setIsHunting(true);
            }
        }
    }

    /**
     * Diet handle - wolves are carnivores.
     */
    private static final class WolfDietHandle extends CodeBasedHandle {

        @Override
        public String id() {
            return "diet";
        }

        @Override
        public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile,
                net.minecraft.world.item.ItemStack stack, boolean original) {
            // Wolves eat all meat (vanilla behavior is fine)
            return original;
        }
    }

    /**
     * Predation handle - wolves hunt sheep, rabbits, foxes.
     * Also adds siege targeting for winter village attacks.
     */
    private static final class WolfPredationHandle extends CodeBasedHandle {

        @Override
        public String id() {
            return "predation";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof Wolf wolf)) {
                return;
            }

            LOGGER.info("WolfPredationHandle.registerGoals called for wolf {}", mob.getId());
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;

            // Register low health flee goal (high priority)
            accessor.betterEcology$getGoalSelector().addGoal(1,
                new me.javavirtualenv.ecology.ai.LowHealthFleeGoal(wolf, 0.45, 1.5));

            // Register wolf drink water goal (high priority when thirsty)
            accessor.betterEcology$getGoalSelector().addGoal(2,
                new me.javavirtualenv.behavior.wolf.WolfDrinkWaterGoal(wolf));

            // Register wolf food sharing goals - pickup BEFORE sharing BEFORE feeding
            accessor.betterEcology$getGoalSelector().addGoal(3,
                new me.javavirtualenv.behavior.wolf.WolfPickupItemGoal(wolf));

            accessor.betterEcology$getGoalSelector().addGoal(4,
                new me.javavirtualenv.behavior.wolf.WolfShareFoodGoal(wolf));

            // Register predator feeding goal (find and eat meat items) - LOWER priority than pickup
            accessor.betterEcology$getGoalSelector().addGoal(5,
                new me.javavirtualenv.behavior.predation.PredatorFeedingGoal(wolf));

            // Register melee attack goal (executes when target is set)
            accessor.betterEcology$getGoalSelector().addGoal(6,
                new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(wolf, 1.2, true));

            // Hunt prey animals using HungryPredatorTargetGoal (only hunts when hungry)
            // Uses hunger threshold of 40 to match WolfBehaviorHandle.isHungry
            // This handles pack hunting coordination automatically
            accessor.betterEcology$getTargetSelector().addGoal(1,
                    me.javavirtualenv.ecology.ai.HungryPredatorTargetGoal.forCommonPrey(wolf, 40));

            // NOTE: Siege targeting removed to avoid conflicts with pack hunting
            // WolfSiegeAttackGoal will manually set targets when in siege mode
        }
    }

    /**
     * Breeding handle with wolf-specific values.
     */
    private static final class WolfBreedingHandle extends CodeBasedHandle {

        private static final int MIN_AGE = 24000;
        private static final double MIN_HEALTH = 0.5;
        private static final int COOLDOWN = 6000;

        @Override
        public String id() {
            return "breeding";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof Wolf wolf)) {
                return;
            }

            // Vanilla wolf breeding is handled by the game
            // No additional goals needed
        }
    }
}
