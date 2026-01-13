package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.WoolHandle;
import me.javavirtualenv.ecology.handles.production.WoolGrowthHandle;
import me.javavirtualenv.ecology.ai.SheepGrazeGoal;
import me.javavirtualenv.ecology.ai.EweProtectLambGoal;
import me.javavirtualenv.ecology.ai.SeekWaterGoal;
import me.javavirtualenv.behavior.production.WoolGrowthGoal;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Sheep that registers all behaviors and configurations from the YAML.
 * This provides a code-based alternative to the YAML-driven configuration system.
 */
@Mixin(Sheep.class)
public abstract class SheepMixin {

    private static final String SHEEP_ID = "minecraft:sheep";
    private static boolean behaviorsRegistered = false;

    /**
     * Register sheep behaviors when the constructor is called.
     * Uses a flag to ensure registration only happens once.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(CallbackInfo ci) {
        if (!behaviorsRegistered) {
            registerBehaviors();
            behaviorsRegistered = true;
        }
    }

    /**
     * Register sheep-specific behaviors and configurations.
     * All values are taken from the sheep YAML configuration.
     */
    protected void registerBehaviors() {
        AnimalConfig config = AnimalConfig.builder(
            net.minecraft.resources.ResourceLocation.parse(SHEEP_ID)
        )
            // Hunger system (lines 88-103)
            .addHandle(new SheepHungerHandle())
            // Thirst system
            .addHandle(new SheepThirstHandle())
            // Condition system (lines 109-124)
            .addHandle(new SheepConditionHandle())
            // Energy system (lines 127-137)
            .addHandle(new SheepEnergyHandle())
            // Age system (lines 140-145)
            .addHandle(new SheepAgeHandle())
            // Social system (lines 148-153)
            .addHandle(new SheepSocialHandle())
            // Movement system (lines 54-69)
            .addHandle(new SheepMovementHandle())
            // Health system (lines 38-40)
            .addHandle(new SheepHealthHandle())
            // Breeding system (lines 371-469)
            .addHandle(new SheepBreedingHandle())
            // Predation as prey (lines 338-365)
            .addHandle(new SheepPredationHandle())
            // Steering behaviors (lines 752-785)
            .addHandle(new SheepBehaviorHandle())
            // Wool system - sheep-specific
            .addHandle(new SheepWoolHandle())
            // Enhanced wool growth system with quality, seasons, and behaviors
            .addHandle(new SheepEnhancedWoolHandle())
            .build();

        AnimalBehaviorRegistry.register(SHEEP_ID, config);
    }

    /**
     * Hunger handle with sheep-specific values.
     * YAML: internal_state.hunger (lines 88-103)
     */
    private static final class SheepHungerHandle extends CodeBasedHandle {
        private static final String NBT_HUNGER = "hunger";
        private static final String NBT_LAST_DAMAGE_TICK = "lastDamageTick";

        // Configuration from YAML
        private static final int MAX_HUNGER = 100;
        private static final int STARTING_HUNGER = 80;
        private static final double DECAY_RATE = 0.015;
        private static final int DAMAGE_THRESHOLD = 5;
        private static final float DAMAGE_AMOUNT = 1.0f;
        private static final int DAMAGE_INTERVAL = 200;
        private static final long MAX_CATCH_UP_TICKS = 24000L; // 1 Minecraft day

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
            long effectiveTicks = Math.min(Math.max(1, elapsedTicks), MAX_CATCH_UP_TICKS);
            long scaledDecay = (long) (DECAY_RATE * effectiveTicks);
            int newHunger = (int) (currentHunger - scaledDecay);

            if (elapsedTicks > 1) {
                int safeMinimum = DAMAGE_THRESHOLD + 1;
                newHunger = Math.max(safeMinimum, newHunger);
            } else {
                newHunger = Math.max(0, newHunger);
            }
            setHunger(handleTag, newHunger);

            boolean isHungry = newHunger < (MAX_HUNGER / 2);
            boolean isStarving = newHunger <= DAMAGE_THRESHOLD;
            EntityState state = component.state();
            state.setIsHungry(isHungry || isStarving);
            state.setIsStarving(isStarving);

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
     * Thirst handle with sheep-specific values.
     */
    private static final class SheepThirstHandle extends CodeBasedHandle {
        private static final String NBT_THIRST = "thirst";
        private static final String NBT_LAST_DAMAGE = "lastThirstDamageTick";

        private static final int MAX_THIRST = 100;
        private static final int STARTING_THIRST = 100;
        private static final double DECAY_RATE = 0.015;
        private static final int DAMAGE_THRESHOLD = 15;
        private static final float DAMAGE_AMOUNT = 1.0f;
        private static final int DAMAGE_INTERVAL = 200;
        private static final long MAX_CATCH_UP_TICKS = 24000L;

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
            long effectiveTicks = Math.min(Math.max(1, elapsed), MAX_CATCH_UP_TICKS);
            long scaledDecay = (long) (DECAY_RATE * effectiveTicks);

            int newThirst = currentThirst - (int) scaledDecay;
            if (elapsed > 1) {
                newThirst = Math.max(DAMAGE_THRESHOLD + 1, newThirst);
            } else {
                newThirst = Math.max(0, newThirst);
            }

            setThirst(tag, newThirst);

            if (elapsed <= 1 && shouldApplyDehydration(mob, newThirst)) {
                int currentTick = mob.tickCount;
                int lastDamage = getLastDamageTick(tag);
                if (currentTick - lastDamage >= DAMAGE_INTERVAL) {
                    mob.hurt(mob.level().damageSources().starve(), DAMAGE_AMOUNT);
                    setLastDamageTick(tag, currentTick);
                }
            }
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
            outputTag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentThirst(CompoundTag tag) {
            return tag.contains(NBT_THIRST) ? tag.getInt(NBT_THIRST) : STARTING_THIRST;
        }

        private void setThirst(CompoundTag tag, int value) {
            tag.putInt(NBT_THIRST, value);
        }

        private int getLastDamageTick(CompoundTag tag) {
            return tag.getInt(NBT_LAST_DAMAGE);
        }

        private void setLastDamageTick(CompoundTag tag, int tick) {
            tag.putInt(NBT_LAST_DAMAGE, tick);
        }

        private boolean shouldApplyDehydration(Mob mob, int thirst) {
            return thirst <= DAMAGE_THRESHOLD && mob.level().getDifficulty() != Difficulty.PEACEFUL;
        }
    }

    /**
     * Condition handle with sheep-specific values.
     * YAML: internal_state.condition (lines 109-124)
     */
    private static final class SheepConditionHandle extends CodeBasedHandle {
        private static final String NBT_CONDITION = "condition";

        // Configuration from YAML
        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 70;
        private static final double GAIN_WHEN_SATIATED = 0.015;
        private static final double LOSS_WHEN_HUNGRY = 0.02;
        private static final double LOSS_WHEN_STARVING = 0.1;
        private static final int POOR_THRESHOLD = 25;

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

            boolean isPoorCondition = newCondition < POOR_THRESHOLD;
            if (isPoorCondition) {
                state.setIsHungry(true);
            }
        }

        private int getCurrentCondition(CompoundTag tag) {
            if (!tag.contains(NBT_CONDITION)) {
                return STARTING_VALUE;
            }
            return tag.getInt(NBT_CONDITION);
        }

        private void setCondition(CompoundTag tag, int value) {
            tag.putInt(NBT_CONDITION, value);
        }

        private boolean isSatiated(EcologyComponent component) {
            CompoundTag hungerTag = component.getHandleTag("hunger");
            if (!hungerTag.contains("hunger")) {
                return false;
            }
            int hunger = hungerTag.getInt("hunger");
            return hunger > 75;
        }
    }

    /**
     * Energy handle with sheep-specific values.
     * YAML: internal_state.energy (lines 127-137)
     */
    private static final class SheepEnergyHandle extends CodeBasedHandle {
        private static final String NBT_ENERGY = "energy";
        private static final String NBT_IS_EXHAUSTED = "isExhausted";

        // Configuration from YAML
        private static final int MAX_VALUE = 100;
        private static final double RECOVERY_RATE = 0.6;
        private static final int EXHAUSTION_THRESHOLD = 10;
        private static final double SPRINTING_COST = 0.0;
        private static final double HUNTING_COST = 0.0;
        private static final double FLEEING_COST = 0.3;
        private static final double FLYING_COST = 0.0;
        private static final double SWIMMING_COST = 0.25;

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
            tag.putBoolean(NBT_IS_EXHAUSTED, isExhausted);

            if (isExhausted && (state.isHunting() || state.isFleeing())) {
                state.setIsHunting(false);
                state.setIsFleeing(false);
            }
        }

        private int getCurrentEnergy(CompoundTag tag) {
            if (!tag.contains(NBT_ENERGY)) {
                return MAX_VALUE;
            }
            return tag.getInt(NBT_ENERGY);
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
            if (state.isInWater()) {
                return SWIMMING_COST;
            }
            return 0.01;
        }
    }

    /**
     * Age handle with sheep-specific values.
     * YAML: internal_state.age (lines 140-145)
     */
    private static final class SheepAgeHandle extends CodeBasedHandle {
        private static final String NBT_AGE_TICKS = "ageTicks";
        private static final String NBT_IS_ELDERLY = "isElderly";

        // Configuration from YAML
        private static final long MAX_CATCH_UP_TICKS = 24000L; // 1 Minecraft day
        private static final int BABY_DURATION = 24000;
        private static final int MATURITY_AGE = 24000;

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

            if (mob instanceof net.minecraft.world.entity.AgeableMob ageable) {
                boolean shouldBeBaby = ageTicks < BABY_DURATION;
                if (shouldBeBaby) {
                    ageable.setBaby(true);
                } else {
                    ageable.setAge(0);
                }
            }

            boolean isElderly = false;
            tag.putBoolean(NBT_IS_ELDERLY, isElderly);
            component.state().setIsElderly(isElderly);
        }

        private int getAgeTicks(CompoundTag tag) {
            if (!tag.contains(NBT_AGE_TICKS)) {
                return 0;
            }
            return tag.getInt(NBT_AGE_TICKS);
        }

        private void setAgeTicks(CompoundTag tag, int value) {
            tag.putInt(NBT_AGE_TICKS, value);
        }
    }

    /**
     * Social handle with sheep-specific values.
     * YAML: internal_state.social (lines 148-153)
     */
    private static final class SheepSocialHandle extends CodeBasedHandle {
        private static final String NBT_SOCIAL = "social";
        private static final String NBT_LAST_GROUP_CHECK = "lastGroupCheck";

        // Configuration from YAML
        private static final int MAX_VALUE = 100;
        private static final double DECAY_RATE = 0.03;
        private static final double RECOVERY_RATE = 0.15;
        private static final int LONELINESS_THRESHOLD = 40;
        private static final int CHECK_INTERVAL = 100;
        private static final int GROUP_RADIUS = 32;

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

            int currentTick = mob.tickCount;
            int lastCheck = getLastGroupCheck(tag);
            boolean hadGroupNearby = tag.getBoolean("hasGroupNearby");
            int checkInterval = hadGroupNearby ? CHECK_INTERVAL * 2 : CHECK_INTERVAL;

            boolean hasGroupNearby;
            if (currentTick - lastCheck >= checkInterval) {
                hasGroupNearby = checkForGroup(mob);
                setLastGroupCheck(tag, currentTick);
                tag.putBoolean("hasGroupNearby", hasGroupNearby);
            } else {
                hasGroupNearby = hadGroupNearby;
            }

            double change = hasGroupNearby ? RECOVERY_RATE : -DECAY_RATE;
            long elapsed = component.elapsedTicks();
            change *= elapsed;

            int newSocial = (int) Math.round(Math.min(MAX_VALUE, Math.max(0, currentSocial + change)));

            if (newSocial != currentSocial) {
                setSocial(tag, newSocial);
            }

            boolean isLonely = newSocial < LONELINESS_THRESHOLD;
            component.state().setIsLonely(isLonely);
        }

        private int getCurrentSocial(CompoundTag tag) {
            if (!tag.contains(NBT_SOCIAL)) {
                return MAX_VALUE;
            }
            return tag.getInt(NBT_SOCIAL);
        }

        private void setSocial(CompoundTag tag, int value) {
            tag.putInt(NBT_SOCIAL, value);
        }

        private int getLastGroupCheck(CompoundTag tag) {
            return tag.getInt(NBT_LAST_GROUP_CHECK);
        }

        private void setLastGroupCheck(CompoundTag tag, int tick) {
            tag.putInt(NBT_LAST_GROUP_CHECK, tick);
        }

        private boolean checkForGroup(Mob mob) {
            return me.javavirtualenv.ecology.spatial.SpatialIndex.hasNearbySameType(mob, GROUP_RADIUS);
        }
    }

    /**
     * Movement handle with sheep-specific values.
     * YAML: physical.movement (lines 54-69)
     */
    private static final class SheepMovementHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final double WALK_SPEED = 0.23;
        private static final double RUN_SPEED = 0.23;
        private static final boolean AVOIDS_CLIFFS = true;
        private static final double CLIFF_THRESHOLD = 4.0;

        @Override
        public String id() {
            return "movement";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            applyMovementSpeed(mob);
            configurePathfinding(mob);
            registerFloatGoal(mob);
            registerSeekWaterGoal(mob);
            registerStrollGoal(mob);
            registerGrazeGoal(mob);
            registerLambProtectionGoal(mob);
        }

        private void applyMovementSpeed(Mob mob) {
            AttributeInstance movementAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementAttribute != null) {
                double baseValue = movementAttribute.getBaseValue();
                if (baseValue != WALK_SPEED) {
                    movementAttribute.setBaseValue(WALK_SPEED);
                }
            }
        }

        private void configurePathfinding(Mob mob) {
            if (AVOIDS_CLIFFS) {
                mob.setPathfindingMalus(PathType.DANGER_OTHER, (float) CLIFF_THRESHOLD);
            }
        }

        private void registerFloatGoal(Mob mob) {
            int goalPriority = 0;
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(goalPriority, new FloatGoal(mob));
        }

        private void registerSeekWaterGoal(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                return;
            }
            int goalPriority = 3;
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(goalPriority, new SeekWaterGoal(pathfinderMob, 1.0, 16));
        }

        private void registerStrollGoal(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                return;
            }
            int goalPriority = 5;
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            if (AVOIDS_CLIFFS) {
                accessor.betterEcology$getGoalSelector().addGoal(goalPriority,
                    new WaterAvoidingRandomStrollGoal(pathfinderMob, RUN_SPEED));
            } else {
                accessor.betterEcology$getGoalSelector().addGoal(goalPriority,
                    new RandomStrollGoal(pathfinderMob, RUN_SPEED));
            }
        }

        private void registerGrazeGoal(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                return;
            }
            int goalPriority = 4;
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(goalPriority,
                new SheepGrazeGoal(pathfinderMob, 16.0, 0.8));
        }

        private void registerLambProtectionGoal(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                return;
            }
            int goalPriority = 2;
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(goalPriority,
                new EweProtectLambGoal(pathfinderMob, 24.0, 12.0, 1.0));
        }
    }

    /**
     * Health handle with sheep-specific values.
     * YAML: physical.health (lines 38-40)
     */
    private static final class SheepHealthHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final double BASE_HEALTH = 8.0;
        private static final double BABY_MULTIPLIER = 0.5;

        @Override
        public String id() {
            return "health";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            AttributeInstance healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                double healthValue = BASE_HEALTH;
                if (mob.isBaby()) {
                    healthValue *= BABY_MULTIPLIER;
                }
                healthAttribute.setBaseValue(healthValue);
            }
        }
    }

    /**
     * Breeding handle with sheep-specific values.
     * YAML: reproduction (lines 371-469)
     */
    private static final class SheepBreedingHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final int MIN_AGE = 24000;
        private static final double MIN_HEALTH = 0.6;
        private static final double MIN_CONDITION = 60.0;
        private static final int COOLDOWN = 6000;

        @Override
        public String id() {
            return "breeding";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof net.minecraft.world.entity.animal.Animal animal)) {
                return;
            }
            int priority = 6;
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(priority,
                new me.javavirtualenv.ecology.ai.EcologyBreedGoal(animal, 1.0, MIN_AGE, MIN_HEALTH,
                    (int) MIN_CONDITION, COOLDOWN, null));
        }
    }

    /**
     * Predation handle with sheep-specific values (as prey).
     * YAML: predation.as_prey (lines 338-365)
     */
    private static final class SheepPredationHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final double FLEE_DISTANCE = 32.0;
        private static final double DETECTION_RANGE = 20.0;

        @Override
        public String id() {
            return "predation";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                return;
            }

            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;

            // Flee when health is low (priority 1 - very high priority)
            accessor.betterEcology$getGoalSelector().addGoal(1,
                new me.javavirtualenv.ecology.ai.LowHealthFleeGoal(pathfinderMob, 0.70, 1.3));

            // Flee from wolves
            accessor.betterEcology$getGoalSelector().addGoal(2,
                new AvoidEntityGoal<>(pathfinderMob, net.minecraft.world.entity.animal.Wolf.class,
                    (float) FLEE_DISTANCE, 1.0, 1.2));
        }
    }

    /**
     * Behavior handle with sheep-specific steering behaviors.
     * YAML: social.herd_movement (lines 496-500)
     */
    private static final class SheepBehaviorHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "behavior";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Steering behaviors are applied via the BehaviorHandle in the profile system
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Steering behaviors are applied via the goal, not here
        }
    }

    /**
     * Wool handle with sheep-specific wool growth and shearing.
     */
    private static final class SheepWoolHandle extends CodeBasedHandle {
        private static final String NBT_WOOL_STAGE = "wool_stage";
        private static final String NBT_LAST_SHEAR_TIME = "last_shear_time";
        private static final String NBT_WOOL_QUALITY = "wool_quality";
        private static final String NBT_LAST_GRAZE_TIME = "last_graze_time";

        // Configuration
        private static final int MAX_WOOL_STAGE = 4;
        private static final int MIN_WOOL_STAGE_FOR_SHEAR = 3;
        private static final double BASE_GROWTH_CHANCE = 0.15;
        private static final int HUNGER_THRESHOLD = 30;
        private static final double BASE_GRAZE_CHANCE = 0.35;
        private static final int GRAZING_COOLDOWN = 200;
        private static final int HUNGER_RESTORATION = 25;
        private static final int BASE_WOOL_DROPS = 1;

        @Override
        public String id() {
            return "wool";
        }

        @Override
        public int tickInterval() {
            return 100;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof Sheep sheep)) {
                return;
            }

            CompoundTag tag = component.getHandleTag(id());

            // Grow wool based on nutrition
            if (sheep.isSheared()) {
                int lastShearTime = getLastShearTime(tag);
                int timeSinceShear = sheep.tickCount - lastShearTime;

                CompoundTag hungerTag = component.getHandleTag("hunger");
                int hunger = hungerTag.getInt("hunger");
                double nutritionBonus = hunger / 100.0;

                if (hunger < HUNGER_THRESHOLD) {
                    return;
                }

                double growthChance = BASE_GROWTH_CHANCE * nutritionBonus;
                if (sheep.getRandom().nextDouble() < growthChance) {
                    int currentStage = getWoolStage(tag);
                    if (currentStage < MAX_WOOL_STAGE) {
                        setWoolStage(tag, currentStage + 1);
                    }
                }
            }

            // Update wool quality
            updateWoolQuality(mob, tag, component);
        }

        private void updateWoolQuality(Mob mob, CompoundTag tag, EcologyComponent component) {
            CompoundTag hungerTag = component.getHandleTag("hunger");
            int hunger = hungerTag.getInt("hunger");

            CompoundTag conditionTag = component.getHandleTag("condition");
            int condition = conditionTag.getInt("condition");

            double quality = 0.0;
            quality += (hunger / 100.0) * 0.5;
            quality += (condition / 100.0) * 0.3;
            quality += mob.getHealth() / mob.getMaxHealth() * 0.2;

            setWoolQuality(tag, (float) Math.max(0.0, Math.min(1.0, quality)));
        }

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
    }

    /**
     * Enhanced wool growth handle with quality system, seasonal variations,
     * and comprehensive behavioral integration.
     */
    private static final class SheepEnhancedWoolHandle extends CodeBasedHandle {
        private static final String NBT_WOOL_LENGTH = "enhancedWoolLength";
        private static final String NBT_WOOL_QUALITY = "enhancedWoolQuality";
        private static final String NBT_COAT_THICKNESS = "coatThickness";
        private static final String NBT_STORED_COLOR = "storedColor";
        private static final String NBT_IS_PARASITIZED = "isParasitised";

        // Scientific parameters
        private static final double GROWTH_RATE = 0.0278; // 100% in ~3600 ticks (3 minutes)
        private static final double PARASITE_CHANCE = 0.0001;

        // Quality thresholds
        private static final float PREMIUM_THRESHOLD = 75.0f;
        private static final float REGULAR_THRESHOLD = 40.0f;

        // Seasonal modifiers
        private static final float WINTER_THICKNESS = 1.3f;
        private static final float SUMMER_THICKNESS = 0.7f;
        private static final double WINTER_GROWTH_RATE = 0.8;
        private static final double SUMMER_GROWTH_RATE = 1.2;

        @Override
        public String id() {
            return "wool_growth";
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

            CompoundTag tag = component.getHandleTag(id());

            // Initialize if first time
            if (!tag.contains(NBT_WOOL_LENGTH)) {
                initializeWoolState(sheep, tag);
            }

            // Update wool growth
            updateWoolGrowth(sheep, tag, component);

            // Update coat thickness based on biome temperature
            updateCoatThickness(sheep, tag);

            // Recalculate quality
            recalculateWoolQuality(sheep, tag, component);

            // Check for parasites
            checkParasites(sheep, tag);
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                return;
            }

            // Get the component for this mob
            var ecologyComponent = getEcologyComponent(mob);
            if (ecologyComponent.isEmpty()) {
                return;
            }

            // Register wool growth behavior goal
            int priority = 8; // Lower priority to avoid conflict with grazing (priority 4)
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(priority,
                new WoolGrowthGoal(pathfinderMob));
        }

        private void initializeWoolState(Sheep sheep, CompoundTag tag) {
            // Start with full wool
            tag.putFloat(NBT_WOOL_LENGTH, 100.0f);

            // Initial quality based on health
            float initialQuality = (sheep.getHealth() / sheep.getMaxHealth()) * 70.0f;
            tag.putFloat(NBT_WOOL_QUALITY, initialQuality);

            // Store current color
            tag.putInt(NBT_STORED_COLOR, sheep.getColor().getId());

            // Set initial coat thickness
            tag.putFloat(NBT_COAT_THICKNESS, 1.0f);
        }

        private void updateWoolGrowth(Sheep sheep, CompoundTag tag, EcologyComponent component) {
            // Don't grow if sheared
            if (sheep.isSheared()) {
                tag.putFloat(NBT_WOOL_LENGTH, 0.0f);
                return;
            }

            float currentLength = tag.getFloat(NBT_WOOL_LENGTH);
            if (currentLength >= 100.0f) {
                return;
            }

            // Calculate growth rate
            double growthRate = calculateGrowthRate(component, sheep);
            float growth = (float) growthRate;
            float newLength = Math.min(100.0f, currentLength + growth);

            tag.putFloat(NBT_WOOL_LENGTH, newLength);
        }

        private double calculateGrowthRate(EcologyComponent component, Sheep sheep) {
            double baseRate = GROWTH_RATE;

            // Diet factor
            CompoundTag hungerTag = component.getHandleTag("hunger");
            int hunger = hungerTag.getInt("hunger");
            double dietFactor = 0.2 + (hunger / 100.0) * 1.3;

            // Health factor
            CompoundTag conditionTag = component.getHandleTag("condition");
            int condition = conditionTag.getInt("condition");
            double healthFactor = 0.3 + (condition / 100.0) * 0.9;

            // Age factor
            CompoundTag ageTag = component.getHandleTag("age");
            boolean isElderly = ageTag.getBoolean("isElderly");
            double ageFactor = isElderly ? 0.6 : 1.0;

            // Seasonal factor based on biome temperature
            float temperature = sheep.level().getBiome(sheep.blockPosition()).value().getBaseTemperature();
            double seasonFactor;
            if (temperature < 0.0) {
                seasonFactor = WINTER_GROWTH_RATE;
            } else if (temperature > 0.5) {
                seasonFactor = SUMMER_GROWTH_RATE;
            } else {
                seasonFactor = 1.0;
            }

            return baseRate * dietFactor * healthFactor * ageFactor * seasonFactor;
        }

        private void updateCoatThickness(Sheep sheep, CompoundTag tag) {
            float temperature = sheep.level().getBiome(sheep.blockPosition()).value().getBaseTemperature();
            float thickness;

            if (temperature < 0.0) {
                thickness = WINTER_THICKNESS;
            } else if (temperature > 0.5) {
                thickness = SUMMER_THICKNESS;
            } else {
                thickness = 1.0f;
            }

            tag.putFloat(NBT_COAT_THICKNESS, thickness);
        }

        private void recalculateWoolQuality(Sheep sheep, CompoundTag tag, EcologyComponent component) {
            // Get factor scores
            double dietScore = getDietScore(component);
            double healthScore = getHealthScore(component);
            double ageScore = getAgeScore(component);
            double environmentScore = getEnvironmentScore(sheep);

            // Weighted average
            double qualityScore = (dietScore * 1.0 +
                                 healthScore * 1.2 +
                                 ageScore * 0.8 +
                                 environmentScore * 0.5) / 3.5;

            // Parasite penalty
            if (tag.getBoolean(NBT_IS_PARASITIZED)) {
                qualityScore *= 0.4;
            }

            tag.putFloat(NBT_WOOL_QUALITY, (float) Math.max(0, Math.min(100, qualityScore)));
        }

        private double getDietScore(EcologyComponent component) {
            CompoundTag hungerTag = component.getHandleTag("hunger");
            return hungerTag.getInt("hunger");
        }

        private double getHealthScore(EcologyComponent component) {
            CompoundTag conditionTag = component.getHandleTag("condition");
            return conditionTag.getInt("condition");
        }

        private double getAgeScore(EcologyComponent component) {
            CompoundTag ageTag = component.getHandleTag("age");
            if (!ageTag.contains("ageTicks")) {
                return 80.0;
            }
            int ageTicks = ageTag.getInt("ageTicks");
            if (ageTicks < 24000) {
                return 60.0; // Young
            } else if (ageTicks <= 72000) {
                return 100.0; // Prime
            } else {
                return 70.0; // Older
            }
        }

        private double getEnvironmentScore(Sheep sheep) {
            float temperature = sheep.level().getBiome(sheep.blockPosition()).value().getBaseTemperature();
            if (temperature >= 0.0 && temperature <= 0.5) {
                return 90.0;
            } else if (temperature < -0.5 || temperature > 1.0) {
                return 40.0;
            } else {
                return 65.0;
            }
        }

        private void checkParasites(Sheep sheep, CompoundTag tag) {
            if (tag.getBoolean(NBT_IS_PARASITIZED)) {
                return;
            }

            if (sheep.level().random.nextFloat() < PARASITE_CHANCE) {
                var componentOpt = getEcologyComponent(sheep);
                if (componentOpt.isPresent()) {
                    var comp = componentOpt.get();
                    CompoundTag conditionTag = comp.getHandleTag("condition");
                    int condition = conditionTag.getInt("condition");

                    if (condition < 30) {
                        tag.putBoolean(NBT_IS_PARASITIZED, true);
                    }
                }
            }
        }

        private java.util.Optional<EcologyComponent> getEcologyComponent(Mob mob) {
            return java.util.Optional.ofNullable(me.javavirtualenv.ecology.EcologyHooks.getEcologyComponent(mob));
        }
    }
}

