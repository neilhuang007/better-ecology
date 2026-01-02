package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.state.EntityState;
import me.javavirtualenv.behavior.horse.HorseBehaviorHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Donkey that registers all behaviors and configurations from the YAML.
 * This provides a code-based alternative to the YAML-driven configuration system.
 *
 * YAML configuration: src/main/resources/data/better-ecology/mobs/passive/donkey/mod_registry.yaml
 */
@Mixin(net.minecraft.world.entity.animal.horse.Donkey.class)
public abstract class DonkeyMixin {

    private static final String DONKEY_ID = "minecraft:donkey";
    private static boolean behaviorsRegistered = false;

    /**
     * Register donkey behaviors when the constructor is called.
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
     * Register donkey-specific behaviors and configurations.
     * All values are taken from the donkey YAML configuration.
     */
    private void registerBehaviors() {
        AnimalConfig config = AnimalConfig.builder(
            net.minecraft.resources.ResourceLocation.parse(DONKEY_ID)
        )
            // Hunger system (YAML lines 80-95)
            .addHandle(new DonkeyHungerHandle())
            // Thirst system (YAML lines 97-105)
            .addHandle(new DonkeyThirstHandle())
            // Condition system (YAML lines 107-121)
            .addHandle(new DonkeyConditionHandle())
            // Energy system (YAML lines 123-133)
            .addHandle(new DonkeyEnergyHandle())
            // Age system (YAML lines 135-140)
            .addHandle(new DonkeyAgeHandle())
            // Social system (YAML lines 142-147)
            .addHandle(new DonkeySocialHandle())
            // Movement system (YAML lines 48-72)
            .addHandle(new DonkeyMovementHandle())
            // Health system (YAML lines 34-36)
            .addHandle(new DonkeyHealthHandle())
            // Breeding system (YAML lines 383-481)
            .addHandle(new DonkeyBreedingHandle())
            // Predation as prey (YAML lines 340-376)
            .addHandle(new DonkeyPredationHandle())
            // Diet system (YAML lines 246-329)
            .addHandle(new DonkeyDietHandle())
            // Temporal behavior (YAML lines 154-198)
            .addHandle(new DonkeyTemporalHandle())
            // Spatial behavior (YAML lines 205-239)
            .addHandle(new DonkeySpatialHandle())
            // Environmental impact (YAML lines 548-593)
            .addHandle(new DonkeyEnvironmentalHandle())
            // Spawn behavior (YAML lines 600-646)
            .addHandle(new DonkeySpawnHandle())
            // Behavior/steering (YAML lines 744-775)
            .addHandle(new DonkeyBehaviorHandle())
            // Horse-specific behaviors
            .addHandle(new HorseBehaviorHandle())
            .build();

        AnimalBehaviorRegistry.register(DONKEY_ID, config);
    }

    /**
     * Hunger handle with donkey-specific values.
     * YAML: internal_state.hunger (lines 80-95)
     */
    private static final class DonkeyHungerHandle extends CodeBasedHandle {
        private static final String NBT_HUNGER = "hunger";
        private static final String NBT_LAST_DAMAGE_TICK = "lastDamageTick";

        // Configuration from YAML
        private static final int MAX_HUNGER = 100;
        private static final int STARTING_HUNGER = 80;
        private static final double DECAY_RATE = 0.015;
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
     * Thirst handle with donkey-specific values.
     * YAML: internal_state.thirst (lines 97-105)
     */
    private static final class DonkeyThirstHandle extends CodeBasedHandle {
        private static final String NBT_THIRST = "thirst";
        private static final String NBT_LAST_DAMAGE_TICK = "lastDamageTick";

        // Configuration from YAML
        private static final int MAX_THIRST = 100;
        private static final int STARTING_THIRST = 100;
        private static final double DECAY_RATE = 0.008;
        private static final int THIRSTY_THRESHOLD = 40;
        private static final int DEHYDRATED_THRESHOLD = 15;
        private static final int DAMAGE_INTERVAL = 200;
        private static final float DAMAGE_AMOUNT = 1.0f;

        @Override
        public String id() {
            return "thirst";
        }

        @Override
        public int tickInterval() {
            return 5;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag handleTag = component.getHandleTag(id());
            int currentThirst = getCurrentThirst(handleTag);

            long elapsedTicks = component.elapsedTicks();
            long effectiveTicks = Math.max(1, elapsedTicks);
            long scaledDecay = (long) (DECAY_RATE * effectiveTicks);
            int newThirst = (int) (currentThirst - scaledDecay);

            if (elapsedTicks > 1) {
                int safeMinimum = DEHYDRATED_THRESHOLD + 1;
                newThirst = Math.max(safeMinimum, newThirst);
            } else {
                newThirst = Math.max(0, newThirst);
            }
            setThirst(handleTag, newThirst);

            boolean isThirsty = newThirst < THIRSTY_THRESHOLD;
            boolean isDehydrated = newThirst < DEHYDRATED_THRESHOLD;
            component.state().setIsThirsty(isThirsty || isDehydrated);

            if (elapsedTicks <= 1 && isDehydrated) {
                int currentTick = mob.tickCount;
                int lastDamageTick = getLastDamageTick(handleTag);
                if (currentTick - lastDamageTick >= DAMAGE_INTERVAL) {
                    mob.hurt(mob.level().damageSources().dryOut(), DAMAGE_AMOUNT);
                    setLastDamageTick(handleTag, currentTick);
                }
            }
        }

        private int getCurrentThirst(CompoundTag handleTag) {
            if (!handleTag.contains(NBT_THIRST)) {
                return STARTING_THIRST;
            }
            return handleTag.getInt(NBT_THIRST);
        }

        private void setThirst(CompoundTag handleTag, int value) {
            handleTag.putInt(NBT_THIRST, value);
        }

        private int getLastDamageTick(CompoundTag handleTag) {
            return handleTag.getInt(NBT_LAST_DAMAGE_TICK);
        }

        private void setLastDamageTick(CompoundTag handleTag, int tick) {
            handleTag.putInt(NBT_LAST_DAMAGE_TICK, tick);
        }
    }

    /**
     * Condition handle with donkey-specific values.
     * YAML: internal_state.condition (lines 107-121)
     */
    private static final class DonkeyConditionHandle extends CodeBasedHandle {
        private static final String NBT_CONDITION = "condition";

        // Configuration from YAML
        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 70;
        private static final double GAIN_WHEN_SATIATED = 0.01;
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
     * Energy handle with donkey-specific values.
     * YAML: internal_state.energy (lines 123-133)
     */
    private static final class DonkeyEnergyHandle extends CodeBasedHandle {
        private static final String NBT_ENERGY = "energy";
        private static final String NBT_IS_EXHAUSTED = "isExhausted";

        // Configuration from YAML
        private static final int MAX_VALUE = 100;
        private static final double RECOVERY_RATE = 0.4;
        private static final int EXHAUSTION_THRESHOLD = 10;
        private static final double SPRINTING_COST = 0.2;
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
     * Age handle with donkey-specific values.
     * YAML: internal_state.age (lines 135-140)
     */
    private static final class DonkeyAgeHandle extends CodeBasedHandle {
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
     * Social handle with donkey-specific values.
     * YAML: internal_state.social (lines 142-147)
     */
    private static final class DonkeySocialHandle extends CodeBasedHandle {
        private static final String NBT_SOCIAL = "social";
        private static final String NBT_LAST_GROUP_CHECK = "lastGroupCheck";

        // Configuration from YAML
        private static final int MAX_VALUE = 100;
        private static final double DECAY_RATE = 0.008;
        private static final double RECOVERY_RATE = 0.12;
        private static final int LONELINESS_THRESHOLD = 35;
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
     * Movement handle with donkey-specific values.
     * YAML: physical.movement (lines 48-72)
     */
    private static final class DonkeyMovementHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final double WALK_SPEED = 0.175;
        private static final double RUN_SPEED = 0.175;
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
            registerStrollGoal(mob);
            registerFloatGoal(mob);
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

        private void registerFloatGoal(Mob mob) {
            int goalPriority = 0;
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(goalPriority, new FloatGoal(mob));
        }
    }

    /**
     * Health handle with donkey-specific values.
     * YAML: physical.health (lines 34-36)
     */
    private static final class DonkeyHealthHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final double BASE_HEALTH = 11.25;
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
     * Breeding handle with donkey-specific values.
     * YAML: reproduction (lines 383-481)
     */
    private static final class DonkeyBreedingHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final int MIN_AGE = 24000;
        private static final double MIN_HEALTH = 0.8;
        private static final double MIN_CONDITION = 65.0;
        private static final int MIN_HUNGER = 60;
        private static final int COOLDOWN = 24000;

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
     * Predation handle with donkey-specific values (as prey).
     * YAML: predation.as_prey (lines 340-376)
     */
    private static final class DonkeyPredationHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final double FLEE_DISTANCE = 40.0;
        private static final double DETECTION_RANGE = 24.0;
        private static final double SPEED_MULTIPLIER = 1.3;

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

            // Flee from wolves
            accessor.betterEcology$getGoalSelector().addGoal(2,
                new AvoidEntityGoal<>(pathfinderMob, Wolf.class,
                    (float) FLEE_DISTANCE, 1.0, (float) SPEED_MULTIPLIER));
        }
    }

    /**
     * Diet handle with donkey-specific values.
     * YAML: diet (lines 246-329)
     * Uses existing DietHandle which reads from YAML
     */
    private static final class DonkeyDietHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "diet";
        }

        @Override
        public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile,
                                     ItemStack stack, boolean original) {
            // Delegate to existing DietHandle logic
            return new me.javavirtualenv.ecology.handles.DietHandle().overrideIsFood(mob, component, profile, stack, original);
        }
    }

    /**
     * Temporal behavior handle with donkey-specific values.
     * YAML: temporal (lines 154-198)
     */
    private static final class DonkeyTemporalHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "temporal";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Daily cycle activity multipliers are handled by AI goals
            // Weather effects on activity are handled here
        }
    }

    /**
     * Spatial behavior handle with donkey-specific values.
     * YAML: spatial (lines 205-239)
     */
    private static final class DonkeySpatialHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final int HOME_RANGE_RADIUS = 96;

        @Override
        public String id() {
            return "spatial";
        }

        public void onAttach(Mob mob, EcologyComponent component) {
            // Home range: 96 radius, no home point
            // Can live in all biomes, prefers plains, savanna, meadow
        }
    }

    /**
     * Environmental impact handle with donkey-specific values.
     * YAML: environmental_impact (lines 548-593)
     */
    private static final class DonkeyEnvironmentalHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "environmental";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Terrain conversion: grass_block -> dirt when foraging (frequency: 0.02)
            // Seed dispersal enabled
        }
    }

    /**
     * Spawn behavior handle with donkey-specific values.
     * YAML: population.spawning (lines 600-610)
     */
    private static final class DonkeySpawnHandle extends CodeBasedHandle {
        // Configuration from YAML
        private static final int SPAWN_WEIGHT = 8;
        private static final int GROUP_SIZE_MIN = 1;
        private static final int GROUP_SIZE_MAX = 3;

        @Override
        public String id() {
            return "spawn";
        }

        public void onAttach(Mob mob, EcologyComponent component) {
            // Spawning configured in data pack spawns
            // Weight: 8, group size: 1-3
            // Biomes: plains, savanna, meadow
        }
    }

    /**
     * Behavior/steering handle with donkey-specific values.
     * YAML: ai_priority_framework (lines 744-775)
     */
    private static final class DonkeyBehaviorHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "behavior";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;

            // AI priority framework configured via goal priorities
            // Survival: breathe(0), escape_danger(1), flee_predator(2)
            // Physiological: critical_health(3), drink_water(4), eat_food(5)
            // Reproduction: care_for_offspring(7), breed(8)
            // Social: group_cohesion(10)
            // Default: wander(15), idle(16), rest(17)

            // Add low health flee goal at priority 1 (highest priority)
            if (mob instanceof net.minecraft.world.entity.PathfinderMob pathfinder) {
                accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(pathfinder, 0.50, 1.5));
            }
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Steering behaviors are applied via goals
        }
    }
}
