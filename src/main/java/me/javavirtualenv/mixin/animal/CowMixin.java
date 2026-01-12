package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.cow.CalfCareBehavior;
import me.javavirtualenv.behavior.cow.GrazingHandle;
import me.javavirtualenv.behavior.cow.NursingBehavior;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.BullCompetitionGoal;
import me.javavirtualenv.ecology.ai.CalfFollowMotherGoal;
import me.javavirtualenv.ecology.ai.CowCudChewGoal;
import me.javavirtualenv.ecology.ai.CowGrazeGoal;
import me.javavirtualenv.ecology.ai.CowProtectCalfGoal;
import me.javavirtualenv.ecology.ai.HerdCohesionGoal;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.ai.SeekWaterGoal;
import me.javavirtualenv.ecology.handles.AgeHandle;
import me.javavirtualenv.ecology.handles.BreedingHandle;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import me.javavirtualenv.ecology.handles.DietHandle;
import me.javavirtualenv.ecology.handles.EnergyHandle;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.handles.HungerThirstTriggerHandle;
import me.javavirtualenv.ecology.handles.MovementHandle;
import me.javavirtualenv.ecology.handles.PredationHandle;
import me.javavirtualenv.ecology.handles.SocialHandle;
import me.javavirtualenv.ecology.handles.TemporalHandle;
import me.javavirtualenv.ecology.handles.production.MilkProductionHandle;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mixin for Cow behavior registration.
 * Cows are herd animals with the following key characteristics:
 * - Herbivores that graze on grass and wheat
 * - Social animals that prefer groups (herds)
 * - Diurnal activity pattern
 * - Flee from predators (wolves)
 * - Parental care (offspring follow parents)
 * - Milk production system
 * - Grazing behavior
 * - Calf care and nursing
 * - Bull competition for dominance
 *
 * Cow-specific behaviors:
 * - MilkProductionHandle: Manages milk capacity, regeneration, and milking cooldown
 * - GrazingHandle: Handles grass consumption and hunger restoration
 * - CalfCareBehavior: Mothers protect and care for calves
 * - NursingBehavior: Calves seek mothers for nursing
 * - CowGrazeGoal: Active grass seeking and eating
 * - CowProtectCalfGoal: Maternal protection from predators
 * - BullCompetitionGoal: Dominance displays and competition
 * - CalfFollowMotherGoal: Calves follow their mothers
 * - HerdCohesionGoal: Herd following and leader behavior
 */
@Mixin(Cow.class)
public abstract class CowMixin {

    private static final ResourceLocation COW_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "cow");
    private static boolean behaviorsRegistered = false;

    /**
     * Register cow behaviors using code-based handles and AI goals.
     */
    protected void registerBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(COW_ID)
                // Internal state systems
                .addHandle(new CowHungerHandle())
                .addHandle(new CowThirstHandle())
                .addHandle(new CowConditionHandle())
                .addHandle(new CowEnergyHandle())
                .addHandle(new CowAgeHandle())
                .addHandle(new CowSocialHandle())

                // Physical capabilities
                .addHandle(new CowMovementHandle())

                // Behavioral systems
                .addHandle(new CowDietHandle())
                .addHandle(new CowPredationHandle())
                .addHandle(new CowBreedingHandle())
                .addHandle(new CowTemporalHandle())

                // Production systems
                .addHandle(new MilkProductionHandle())

                // Cow-specific behaviors
                .addHandle(new GrazingHandle())
                // Note: BehaviorHandle is NOT added here - it comes from profile via mergeHandles
                .build();

        AnimalBehaviorRegistry.register(COW_ID.toString(), config);
        behaviorsRegistered = true;
    }

    /**
     * Injection point after Cow constructor.
     * Registers cow behaviors once when the first Cow entity is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Animal> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();

        // Register AI goals for this specific cow
        Cow cow = (Cow) (Object) this;
        registerCowGoals(cow);
    }

    /**
     * Register cow-specific AI goals.
     */
    private void registerCowGoals(Cow cow) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) cow;

        // Priority 0: Float
        accessor.betterEcology$getGoalSelector().addGoal(0, new FloatGoal(cow));

        // Priority 1: Low health flee (critical survival)
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(cow, 0.60, 1.2));

        // Priority 2: Protect calf (highest after floating)
        accessor.betterEcology$getGoalSelector().addGoal(2, new CowProtectCalfGoal(cow, 16.0, 24.0));

        // Priority 3: Bull competition (for adults)
        if (!cow.isBaby()) {
            accessor.betterEcology$getGoalSelector().addGoal(3, new BullCompetitionGoal(cow, 20.0));
        }

        // Priority 4: Seek water when thirsty
        accessor.betterEcology$getGoalSelector().addGoal(4, new SeekWaterGoal(cow, 1.0, 16));

        // Priority 5: Breed (using vanilla breeding goal with our constraints)
        accessor.betterEcology$getGoalSelector().addGoal(5, new BreedGoal(cow, 1.0));

        // Priority 6: Grazing
        accessor.betterEcology$getGoalSelector().addGoal(6, new CowGrazeGoal(cow, 16.0, 0.8));

        // Priority 7: Herd cohesion (adults) or follow mother (calves)
        if (cow.isBaby()) {
            accessor.betterEcology$getGoalSelector().addGoal(7, new CalfFollowMotherGoal(cow, 24.0, 1.0));
        } else {
            accessor.betterEcology$getGoalSelector().addGoal(7, new HerdCohesionGoal(cow, 24.0, 0.8));
        }

        // Priority 8: Cud chewing (idle behavior)
        accessor.betterEcology$getGoalSelector().addGoal(8, new CowCudChewGoal(cow));

        // Priority 9: Random stroll (fallback)
        accessor.betterEcology$getGoalSelector().addGoal(9, new WaterAvoidingRandomStrollGoal(cow, 0.6));
    }

    // ============================================================================
    // INNER CLASSES - CODE-BASED HANDLES FOR COW BEHAVIORS
    // ============================================================================

    /**
     * Hunger system for cows.
     */
    private static final class CowHungerHandle extends CodeBasedHandle {
        private static final String NBT_HUNGER = "hunger";
        private static final String NBT_LAST_DAMAGE = "lastDamageTick";

        private static final int MAX_HUNGER = 100;
        private static final int STARTING_HUNGER = 80;
        private static final double DECAY_RATE = 0.01;
        private static final int DAMAGE_THRESHOLD = 10;
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
            CompoundTag tag = component.getHandleTag(id());
            int currentHunger = getCurrentHunger(tag);
            long elapsed = component.elapsedTicks();
            long effectiveTicks = Math.min(Math.max(1, elapsed), MAX_CATCH_UP_TICKS);
            long scaledDecay = (long) (DECAY_RATE * effectiveTicks);

            int newHunger = currentHunger - (int) scaledDecay;
            if (elapsed > 1) {
                newHunger = Math.max(DAMAGE_THRESHOLD + 1, newHunger);
            } else {
                newHunger = Math.max(0, newHunger);
            }

            setHunger(tag, newHunger);

            // Update entity state flags
            component.state().setIsHungry(newHunger < 60);
            component.state().setIsStarving(newHunger <= DAMAGE_THRESHOLD);

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
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
            outputTag.put(id(), component.getHandleTag(id()).copy());
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
     * Thirst system for cows.
     */
    private static final class CowThirstHandle extends CodeBasedHandle {
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

            // Update entity state flag
            component.state().setIsThirsty(newThirst < 30);

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
     * Condition system for cows.
     */
    private static final class CowConditionHandle extends CodeBasedHandle {
        private static final String NBT_CONDITION = "condition";

        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 70;
        private static final double GAIN_WHEN_SATIATED = 0.01;
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
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
            outputTag.put(id(), component.getHandleTag(id()).copy());
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
     * Energy system for cows.
     */
    private static final class CowEnergyHandle extends CodeBasedHandle {
        private static final String NBT_ENERGY = "energy";

        private static final int MAX_VALUE = 100;
        private static final double RECOVERY_RATE = 0.2;
        private static final double FLEEING_COST = 0.4;

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
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
            outputTag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentEnergy(CompoundTag tag) {
            return tag.contains(NBT_ENERGY) ? tag.getInt(NBT_ENERGY) : MAX_VALUE;
        }

        private void setEnergy(CompoundTag tag, int value) {
            tag.putInt(NBT_ENERGY, value);
        }
    }

    /**
     * Age system for cows.
     */
    private static final class CowAgeHandle extends CodeBasedHandle {
        private static final String NBT_AGE = "age";
        private static final String NBT_IS_BABY = "isBaby";

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
            int currentAge = getCurrentAge(tag);

            long elapsed = component.elapsedTicks();
            long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
            int newAge = currentAge + (int) effectiveTicks;
            setAge(tag, newAge);

            boolean isBaby = newAge < BABY_DURATION;
            tag.putBoolean(NBT_IS_BABY, isBaby);

            if (mob instanceof Cow cow) {
                boolean currentlyBaby = cow.isBaby();
                if (currentlyBaby != isBaby) {
                    if (isBaby) {
                        cow.setBaby(true);
                    } else {
                        cow.setAge(0);
                    }
                }
            }
        }

        @Override
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
            outputTag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentAge(CompoundTag tag) {
            return tag.contains(NBT_AGE) ? tag.getInt(NBT_AGE) : MATURITY_AGE;
        }

        private void setAge(CompoundTag tag, int value) {
            tag.putInt(NBT_AGE, value);
        }
    }

    /**
     * Social system for cows.
     */
    private static final class CowSocialHandle extends CodeBasedHandle {
        private static final String NBT_SOCIAL = "social";

        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 100;
        private static final double DECAY_RATE = 0.01;
        private static final double RECOVERY_RATE = 0.1;
        private static final int LONELINESS_THRESHOLD = 30;

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
        public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag outputTag) {
            outputTag.put(id(), component.getHandleTag(id()).copy());
        }

        private int getCurrentSocial(CompoundTag tag) {
            return tag.contains(NBT_SOCIAL) ? tag.getInt(NBT_SOCIAL) : STARTING_VALUE;
        }

        private void setSocial(CompoundTag tag, int value) {
            tag.putInt(NBT_SOCIAL, value);
        }

        private boolean isNearHerdMembers(Mob mob) {
            return mob.level().getEntitiesOfClass(mob.getClass(), mob.getBoundingBox().inflate(12.0))
                    .size() > 1;
        }
    }

    /**
     * Movement system for cows.
     */
    private static final class CowMovementHandle extends CodeBasedHandle {
        private static final double WALK_SPEED = 0.2;
        private static final double RUN_SPEED = 0.3;
        private static final boolean AVOIDS_CLIFFS = true;
        private static final double CLIFF_THRESHOLD = 4.0;

        @Override
        public String id() {
            return "movement";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            AttributeInstance attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null && attr.getBaseValue() != WALK_SPEED) {
                attr.setBaseValue(WALK_SPEED);
            }

            if (AVOIDS_CLIFFS) {
                mob.setPathfindingMalus(PathType.DANGER_OTHER, (float) CLIFF_THRESHOLD);
            }
        }
    }

    /**
     * Diet system for cows.
     */
    private static final class CowDietHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "diet";
        }

        @Override
        public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile,
                                      net.minecraft.world.item.ItemStack stack, boolean original) {
            // Cows eat wheat (player breeding)
            return stack.is(net.minecraft.world.item.Items.WHEAT);
        }
    }

    /**
     * Predation system for cows (as prey).
     */
    private static final class CowPredationHandle extends CodeBasedHandle {
        private static final int DETECTION_RANGE = 24;
        private static final double FLEE_SPEED_MULTIPLIER = 1.3;
        private static final int FLEE_DISTANCE = 32;

        @Override
        public String id() {
            return "predation";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Flee from wolves within detection range
            var wolves = mob.level().getEntitiesOfClass(net.minecraft.world.entity.animal.Wolf.class,
                    mob.getBoundingBox().inflate(DETECTION_RANGE));

            if (!wolves.isEmpty()) {
                EntityState state = component.state();
                state.setIsFleeing(true);

                var nearestWolf = wolves.get(0);
                if (mob.distanceTo(nearestWolf) < FLEE_DISTANCE) {
                    var away = mob.position().subtract(nearestWolf.position()).normalize();
                    var moveSpeed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    mob.getMoveControl().setWantedPosition(
                            mob.getX() + away.x * 5,
                            mob.getY(),
                            mob.getZ() + away.z * 5,
                            (float) (moveSpeed * FLEE_SPEED_MULTIPLIER)
                    );
                }
            } else {
                component.state().setIsFleeing(false);
            }
        }

        @Override
        public int tickInterval() {
            return 10;
        }
    }

    /**
     * Breeding system for cows.
     */
    private static final class CowBreedingHandle extends CodeBasedHandle {
        private static final int COOLDOWN = 24000;
        private static final int MIN_AGE = 24000;
        private static final int MIN_CONDITION = 65;
        private static final int MIN_HUNGER = 60;

        @Override
        public String id() {
            return "breeding";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Breeding goal is registered in registerCowGoals()
        }
    }

    /**
     * Temporal behavior for cows (diurnal).
     */
    private static final class CowTemporalHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "temporal";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            long dayTime = mob.level().getDayTime() % 24000;
            double activityLevel = getActivityLevel(dayTime);

            var temporalTag = component.getHandleTag("temporal");
            if (activityLevel < 0.5) {
                temporalTag.putBoolean("is_resting", true);
            } else {
                temporalTag.putBoolean("is_resting", false);
            }
            component.setHandleTag("temporal", temporalTag);
        }

        @Override
        public int tickInterval() {
            return 100;
        }

        private double getActivityLevel(long dayTime) {
            if (dayTime >= 0 && dayTime < 6000) return 1.0; // Morning
            if (dayTime >= 6000 && dayTime < 12000) return 0.9; // Afternoon
            if (dayTime >= 12000 && dayTime < 13500) return 0.7; // Dusk
            if (dayTime >= 13500 && dayTime < 22500) return 0.3; // Night
            return 0.5; // Pre-dawn
        }
    }
}
