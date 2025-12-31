package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.state.EntityState;
import me.javavirtualenv.pig.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Pig behavior registration.
 * <p>
 * Registers pig entity with the Better Ecology behavior system.
 * All pig-specific behaviors and configurations are defined in:
 * src/main/resources/data/better-ecology/mobs/passive/pig/mod_registry.yaml
 * <p>
 * Pig characteristics:
 * - Farm animal with rideable and saddleable traits
 * - Diurnal activity pattern
 * - Omnivorous diet (carrots, potatoes, beetroots, grass)
 * - Herd social structure
 * - Moderate movement speed (walk: 0.25, run: 0.35)
 * - 10 health (5 hearts)
 * - Breeds with root vegetables
 * - Special behaviors: rooting, truffle hunting, mud bathing, crop feeding
 */
@Mixin(Pig.class)
public abstract class PigMixin extends AnimalMixin {

    /**
     * Constructor injection point for behavior registration.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Pig> entityType, Level level, CallbackInfo ci) {
        if (!areBehaviorsRegistered()) {
            registerBehaviors();
            markBehaviorsRegistered();
        }
    }

    /**
     * Register all pig behaviors and configurations.
     * <p>
     * Creates an AnimalConfig for the pig entity with code-based handles
     * that implement pig-specific behaviors including:
     * - Rooting behavior (finding truffles, converting grass to dirt)
     * - Mud bathing (temperature regulation, condition restoration)
     * - Crop feeding (eating carrots, potatoes, beetroots)
     * <p>
     * Key pig configurations:
     * <ul>
     *   <li>Health: 10 (5 hearts), baby multiplier: 0.5</li>
     *   <li>Size: width 0.9, height 0.9, baby scale 0.38</li>
     *   <li>Movement: walk 0.25, run 0.35</li>
     *   <li>Hunger: max 100, start 75, decay 0.015/tick</li>
     *   <li>Condition: max 100, start 70, affects breeding</li>
     *   <li>Energy: max 100, sprint cost 0.25/tick, flee cost 0.35/tick</li>
     *   <li>Age: baby duration 24000 ticks, maturity at 24000</li>
     *   <li>Social: herd animal, max value 100, decay 0.008/tick when alone</li>
     *   <li>Diet: primary (carrots, potatoes, beetroots), secondary (grass, tall grass)</li>
     *   <li>Reproduction: sexual, min age 24000, min condition 60, min hunger 55</li>
     *   <li>Breeding: cooldown 6000 ticks, instant gestation, 1 offspring</li>
     *   <li>Special: rooting, truffle hunting, mud bathing, crop feeding</li>
     * </ul>
     */
    @Override
    protected void registerBehaviors() {
        AnimalConfig config = AnimalConfig.builder(EntityType.PIG)
                .addHandle(new PigHungerHandle())
                .addHandle(new PigConditionHandle())
                .addHandle(new PigEnergyHandle())
                .addHandle(new PigAgeHandle())
                .addHandle(new PigSocialHandle())
                .addHandle(new PigMovementHandle())
                .addHandle(new PigHealthHandle())
                .addHandle(new PigSizeHandle())
                .addHandle(new PigDietHandle())
                .addHandle(new PigBreedingHandle())
                .addHandle(new PigBehaviorHandle())
                .addHandle(new HungerThirstTriggerHandle())
                .build();

        AnimalBehaviorRegistry.register("minecraft:pig", config);
    }

    // ============================================================================
    // INNER CLASSES - CODE-BASED HANDLES FOR PIG BEHAVIORS
    // ============================================================================

    /**
     * Hunger system for pigs.
     * Pigs have moderate hunger needs due to their omnivorous diet.
     */
    private static final class PigHungerHandle extends CodeBasedHandle {
        private static final String NBT_HUNGER = "hunger";
        private static final String NBT_LAST_DAMAGE = "lastDamageTick";

        private static final int MAX_HUNGER = 100;
        private static final int STARTING_HUNGER = 75;
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
            CompoundTag tag = component.getHandleTag(id());
            int currentHunger = getCurrentHunger(tag);
            long elapsed = component.elapsedTicks();
            long effectiveTicks = Math.max(1, elapsed);
            long scaledDecay = (long) (DECAY_RATE * effectiveTicks);

            int newHunger = currentHunger - (int) scaledDecay;
            if (elapsed > 1) {
                newHunger = Math.max(DAMAGE_THRESHOLD + 1, newHunger);
            } else {
                newHunger = Math.max(0, newHunger);
            }

            setHunger(tag, newHunger);

            if (elapsed <= 1 && shouldApplyStarvation(mob, newHunger)) {
                int currentTick = mob.tickCount;
                int lastDamage = getLastDamageTick(tag);
                if (currentTick - lastDamage >= DAMAGE_INTERVAL) {
                    mob.hurt(mob.level().damageSources().starve(), DAMAGE_AMOUNT);
                    setLastDamageTick(tag, currentTick);
                }
            }
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
            tag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentHunger(CompoundTag tag) {
            return tag.contains(NBT_HUNGER) ? tag.getInt(NBT_HUNGER) : STARTING_HUNGER;
        }

        private void setHunger(CompoundTag tag, int value) {
            tag.putInt(NBT_HUNGER, value);
        }

        private int getLastDamageTick(CompoundTag tag) {
            return tag.getInt(NBT_LAST_DAMAGE);
        }

        private void setLastDamageTick(CompoundTag tag, int tick) {
            tag.putInt(NBT_LAST_DAMAGE, tick);
        }

        private boolean shouldApplyStarvation(Mob mob, int hunger) {
            return hunger <= DAMAGE_THRESHOLD && mob.level().getDifficulty() != Difficulty.PEACEFUL;
        }
    }

    /**
     * Condition system for pigs.
     * Pigs' condition affects breeding success and truffle finding ability.
     */
    private static final class PigConditionHandle extends CodeBasedHandle {
        private static final String NBT_CONDITION = "condition";

        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 70;
        private static final double GAIN_WHEN_SATIATED = 0.015;
        private static final double LOSS_WHEN_HUNGRY = 0.02;
        private static final double LOSS_WHEN_STARVING = 0.1;

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

            double change = 0.0;
            EntityState state = component.state();

            if (state.isStarving()) {
                change = -LOSS_WHEN_STARVING;
            } else if (state.isHungry()) {
                change = -LOSS_WHEN_HUNGRY;
            } else if (isSatiated(component)) {
                change = GAIN_WHEN_SATIATED;
            }

            long elapsed = component.elapsedTicks();
            change *= elapsed;

            int newCondition = (int) Math.round(Math.min(MAX_VALUE, Math.max(0, currentCondition + change)));
            setCondition(tag, newCondition);

            boolean isPoor = newCondition < 25;
            if (isPoor) {
                state.setIsHungry(true);
            }
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
            tag.put(id(), component.getHandleTag(id()).copy());
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
     * Energy system for pigs.
     * Pigs have moderate energy needs.
     */
    private static final class PigEnergyHandle extends CodeBasedHandle {
        private static final String NBT_ENERGY = "energy";

        private static final int MAX_VALUE = 100;
        private static final int RECOVERY_RATE = 0;
        private static final double FLEEING_COST = 0.35;
        private static final double SPRINTING_COST = 0.25;

        @Override
        public String id() {
            return "energy";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int currentEnergy = getCurrentEnergy(tag);

            double change = RECOVERY_RATE;
            EntityState state = component.state();

            if (state.isFleeing()) {
                change -= FLEEING_COST;
            }

            long elapsed = component.elapsedTicks();
            change *= elapsed;

            int newEnergy = (int) Math.round(Math.min(MAX_VALUE, Math.max(0, currentEnergy + change)));
            setEnergy(tag, newEnergy);
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
            tag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentEnergy(CompoundTag tag) {
            return tag.contains(NBT_ENERGY) ? tag.getInt(NBT_ENERGY) : MAX_VALUE;
        }

        private void setEnergy(CompoundTag tag, int value) {
            tag.putInt(NBT_ENERGY, value);
        }
    }

    /**
     * Age system for pigs.
     * Pigs take 20 minutes to mature from baby to adult.
     */
    private static final class PigAgeHandle extends CodeBasedHandle {
        private static final String NBT_AGE = "age";
        private static final String NBT_IS_BABY = "isBaby";

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
            int currentAge = getCurrentAge(tag);

            long elapsed = component.elapsedTicks();
            int newAge = currentAge + (int) elapsed;
            setAge(tag, newAge);

            boolean isBaby = newAge < BABY_DURATION;
            tag.putBoolean(NBT_IS_BABY, isBaby);

            if (mob instanceof Pig pig) {
                boolean currentlyBaby = pig.isBaby();
                if (currentlyBaby != isBaby) {
                    if (isBaby) {
                        pig.setBaby(true);
                    } else {
                        pig.setAge(0);
                    }
                }
            }
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
            tag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentAge(CompoundTag tag) {
            return tag.contains(NBT_AGE) ? tag.getInt(NBT_AGE) : MATURITY_AGE;
        }

        private void setAge(CompoundTag tag, int value) {
            tag.putInt(NBT_AGE, value);
        }
    }

    /**
     * Social system for pigs.
     * Pigs are herd animals and become stressed when alone.
     */
    private static final class PigSocialHandle extends CodeBasedHandle {
        private static final String NBT_SOCIAL = "social";

        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 100;
        private static final double DECAY_RATE = 0.008;
        private static final double RECOVERY_RATE = 0.1;
        private static final int LONELINESS_THRESHOLD = 40;
        private static final int GROUP_RADIUS = 16;

        @Override
        public String id() {
            return "social";
        }

        @Override
        public int tickInterval() {
            return 40;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int currentSocial = getCurrentSocial(tag);

            double change = -DECAY_RATE;
            if (isNearHerdMembers(mob)) {
                change = RECOVERY_RATE;
            }

            long elapsed = component.elapsedTicks();
            change *= elapsed;

            int newSocial = (int) Math.round(Math.min(MAX_VALUE, Math.max(0, currentSocial + change)));
            setSocial(tag, newSocial);

            boolean isLonely = newSocial < LONELINESS_THRESHOLD;
            EntityState state = component.state();
            state.setIsLonely(isLonely);
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
            tag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentSocial(CompoundTag tag) {
            return tag.contains(NBT_SOCIAL) ? tag.getInt(NBT_SOCIAL) : STARTING_VALUE;
        }

        private void setSocial(CompoundTag tag, int value) {
            tag.putInt(NBT_SOCIAL, value);
        }

        private boolean isNearHerdMembers(Mob mob) {
            return mob.level().getEntitiesOfClass(mob.getClass(), mob.getBoundingBox().inflate(GROUP_RADIUS))
                    .size() > 1;
        }
    }

    /**
     * Movement system for pigs.
     * Pigs have moderate movement speed and avoid water.
     */
    private static final class PigMovementHandle extends CodeBasedHandle {
        private static final double WALK_SPEED = 0.25;
        private static final double RUN_SPEED = 0.35;
        private static final boolean AVOIDS_CLIFFS = false;
        private static final double CLIFF_THRESHOLD = 4.0;

        @Override
        public String id() {
            return "movement";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            AttributeInstance movementAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementAttribute != null && movementAttribute.getBaseValue() != WALK_SPEED) {
                movementAttribute.setBaseValue(WALK_SPEED);
            }

            if (AVOIDS_CLIFFS) {
                mob.setPathfindingMalus(PathType.DANGER_OTHER, (float) CLIFF_THRESHOLD);
            }

            if (!(mob instanceof Pig pig)) {
                return;
            }

            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;

            accessor.betterEcology$getGoalSelector().addGoal(0, new FloatGoal(pig));
            accessor.betterEcology$getGoalSelector().addGoal(3, new PigTruffleSeekGoal(pig));
            accessor.betterEcology$getGoalSelector().addGoal(4, new WaterAvoidingRandomStrollGoal(pig, 0.3));
            accessor.betterEcology$getGoalSelector().addGoal(5, new PigRootingGoal(pig, 0.6));
            accessor.betterEcology$getGoalSelector().addGoal(5, new PigMudBathingGoal(pig, 0.7));
            accessor.betterEcology$getGoalSelector().addGoal(5, new PigSocialMudBathingGoal(pig));
            accessor.betterEcology$getGoalSelector().addGoal(5, new PigFeedingGoal(pig, 0.5));
            accessor.betterEcology$getGoalSelector().addGoal(6, new PigSniffTrufflesGoal(pig));
        }
    }

    /**
     * Health system for pigs.
     * Pigs have 10 health (5 hearts).
     */
    private static final class PigHealthHandle extends CodeBasedHandle {
        private static final int BASE_HEALTH = 10;
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
     * Size system for pigs.
     * Pigs are 0.9 blocks wide and 0.9 blocks tall.
     */
    private static final class PigSizeHandle extends CodeBasedHandle {
        private static final float WIDTH = 0.9f;
        private static final float HEIGHT = 0.9f;
        private static final float BABY_SCALE = 0.38f;

        @Override
        public String id() {
            return "size";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Size is handled by Minecraft's pig entity
        }
    }

    /**
     * Diet system for pigs.
     * Pigs eat carrots, potatoes, beetroots, and will root for truffles.
     */
    private static final class PigDietHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "diet";
        }

        @Override
        public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile,
                                      net.minecraft.world.item.ItemStack stack, boolean original) {
            return stack.is(net.minecraft.world.item.Items.CARROT) ||
                   stack.is(net.minecraft.world.item.Items.POTATO) ||
                   stack.is(net.minecraft.world.item.Items.BEETROOT) ||
                   stack.is(net.minecraft.world.item.Items.BEETROOT_SOUP);
        }
    }

    /**
     * Breeding system for pigs.
     * Pigs breed when they have sufficient hunger and condition.
     */
    private static final class PigBreedingHandle extends CodeBasedHandle {
        private static final int COOLDOWN = 6000;
        private static final int MIN_AGE = 24000;
        private static final int MIN_CONDITION = 60;
        private static final int MIN_HUNGER = 55;
        private static final String NBT_COOLDOWN = "breedingCooldown";
        private static final String NBT_LAST_BRED = "lastBredTick";

        @Override
        public String id() {
            return "breeding";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int lastBred = getLastBred(tag);
            int currentTick = mob.tickCount;

            boolean canBreed = (currentTick - lastBred >= COOLDOWN) &&
                    meetsAgeRequirement(component) &&
                    meetsConditionRequirement(component) &&
                    meetsHungerRequirement(component);

            component.state().setCanBreed(canBreed);
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof net.minecraft.world.entity.animal.Animal animal)) {
                return;
            }

            me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
            accessor.betterEcology$getGoalSelector().addGoal(6,
                new me.javavirtualenv.ecology.ai.EcologyBreedGoal(animal, 1.0, MIN_AGE, 0.6,
                    MIN_CONDITION, COOLDOWN));
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
            tag.put(id(), component.getHandleTag(id()).copy());
        }

        @Override
        public void onBreed(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            setLastBred(tag, mob.tickCount);
        }

        private int getLastBred(CompoundTag tag) {
            return tag.contains(NBT_LAST_BRED) ? tag.getInt(NBT_LAST_BRED) : -COOLDOWN;
        }

        private void setLastBred(CompoundTag tag, int tick) {
            tag.putInt(NBT_LAST_BRED, tick);
        }

        private boolean meetsAgeRequirement(EcologyComponent component) {
            CompoundTag ageTag = component.getHandleTag("age");
            int age = ageTag.getInt("age");
            return age >= MIN_AGE;
        }

        private boolean meetsConditionRequirement(EcologyComponent component) {
            CompoundTag conditionTag = component.getHandleTag("condition");
            int condition = conditionTag.getInt("condition");
            return condition >= MIN_CONDITION;
        }

        private boolean meetsHungerRequirement(EcologyComponent component) {
            CompoundTag hungerTag = component.getHandleTag("hunger");
            int hunger = hungerTag.getInt("hunger");
            return hunger >= MIN_HUNGER;
        }
    }
}
