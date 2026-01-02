package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.cow.GrazingHandle;
import me.javavirtualenv.behavior.cow.MooshroomMilkProductionHandle;
import me.javavirtualenv.ecology.ai.BullCompetitionGoal;
import me.javavirtualenv.ecology.ai.CalfFollowMotherGoal;
import me.javavirtualenv.ecology.ai.CowCudChewGoal;
import me.javavirtualenv.ecology.ai.CowGrazeGoal;
import me.javavirtualenv.ecology.ai.CowProtectCalfGoal;
import me.javavirtualenv.ecology.ai.HerdCohesionGoal;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.production.MilkProductionHandle;
import me.javavirtualenv.ecology.state.EntityState;
import me.javavirtualenv.ecology.ai.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Mooshroom-specific mixin for registering custom behaviors.
 * Applies to {@link MushroomCow} (Mooshroom) and registers behaviors
 * using code-based handles and AI goals.
 *
 * Mooshrooms are fungal symbiotes that live in mushroom fields biomes.
 * They graze on mycelium and mushrooms, providing unique milk products.
 *
 * Mooshroom-specific behaviors:
 * - MooshroomMilkProductionHandle: Extended milk system with mushroom stew and suspicious stew
 * - GrazingHandle: Mycelium grazing behavior
 * - CalfCareBehavior: Mothers protect calves in mushroom fields
 * - NursingBehavior: Calves seek mothers for nursing
 * - CowGrazeGoal: Active mycelium seeking and eating
 * - LowHealthFleeGoal: Flee when health drops below 60% during combat
 * - CowProtectCalfGoal: Maternal protection from predators
 * - BullCompetitionGoal: Dominance displays and competition
 * - CalfFollowMotherGoal: Calves follow their mothers
 * - HerdCohesionGoal: Herd following and leader behavior
 */
@Mixin(MushroomCow.class)
public abstract class MooshroomMixin {

    @Unique
    private static final ResourceLocation MOOSHROOM_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "mooshroom");

    @Unique
    private static boolean behaviorsRegistered = false;

    /**
     * Injection point after constructor to register behaviors.
     * Ensures behaviors are registered only once when the first Mooshroom is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (!areBehaviorsRegistered()) {
            registerBehaviors();
            markBehaviorsRegistered();
        }

        // Register AI goals for this specific mooshroom
        MushroomCow mooshroom = (MushroomCow) (Object) this;
        registerMooshroomGoals(mooshroom);
    }

    @Unique
    private static boolean areBehaviorsRegistered() {
        return behaviorsRegistered;
    }

    @Unique
    private static void markBehaviorsRegistered() {
        behaviorsRegistered = true;
    }

    /**
     * Register all Mooshroom behaviors using the builder pattern.
     * Creates code-based handles that mirror the YAML configuration values.
     */
    @Unique
    private static void registerBehaviors() {
        AnimalConfig config = AnimalConfig.builder(MOOSHROOM_ID)
                .addHandle(new MooshroomHungerHandle())
                .addHandle(new MooshroomConditionHandle())
                .addHandle(new MooshroomEnergyHandle())
                .addHandle(new MooshroomAgeHandle())
                .addHandle(new MooshroomSocialHandle())
                .addHandle(new MooshroomMovementHandle())
                .addHandle(new MooshroomDietHandle())
                .addHandle(new MooshroomPredationHandle())
                .addHandle(new MooshroomTemporalHandle())
                .addHandle(new MooshroomBreedingHandle())

                // Production systems (milk with mushroom stew)
                .addHandle(new MooshroomMilkProductionHandle())

                // Mooshroom-specific behaviors
                .addHandle(new GrazingHandle())
                // Note: BehaviorHandle comes from profile via mergeHandles
                .build();

        AnimalBehaviorRegistry.register(MOOSHROOM_ID.toString(), config);
    }

    /**
     * Register mooshroom-specific AI goals.
     */
    @Unique
    private void registerMooshroomGoals(MushroomCow mooshroom) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mooshroom;

        // Priority 0: Float
        accessor.betterEcology$getGoalSelector().addGoal(0, new FloatGoal(mooshroom));

        // Priority 1: Low health flee (flee when health drops below 60%)
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(mooshroom, 0.60, 1.2));

        // Priority 2: Protect calf (highest after floating)
        accessor.betterEcology$getGoalSelector().addGoal(2, new CowProtectCalfGoal(mooshroom, 16.0, 24.0));

        // Priority 3: Bull competition (for adults)
        if (!mooshroom.isBaby()) {
            accessor.betterEcology$getGoalSelector().addGoal(3, new BullCompetitionGoal(mooshroom, 20.0));
        }

        // Priority 4: Breed (using vanilla breeding goal with our constraints)
        accessor.betterEcology$getGoalSelector().addGoal(4, new BreedGoal(mooshroom, 1.0));

        // Priority 5: Grazing (modified for mooshrooms - mycelium instead of grass)
        accessor.betterEcology$getGoalSelector().addGoal(5, new CowGrazeGoal(mooshroom, 16.0, 0.8));

        // Priority 6: Herd cohesion (adults) or follow mother (calves)
        if (mooshroom.isBaby()) {
            accessor.betterEcology$getGoalSelector().addGoal(6, new CalfFollowMotherGoal(mooshroom, 24.0, 1.0));
        } else {
            accessor.betterEcology$getGoalSelector().addGoal(6, new HerdCohesionGoal(mooshroom, 24.0, 0.8));
        }

        // Priority 7: Cud chewing (idle behavior)
        accessor.betterEcology$getGoalSelector().addGoal(7, new CowCudChewGoal(mooshroom));

        // Priority 8: Random stroll (fallback)
        accessor.betterEcology$getGoalSelector().addGoal(8, new WaterAvoidingRandomStrollGoal(mooshroom, 0.6));
    }

    // ============================================================================
    // INNER CLASSES - CODE-BASED HANDLES FOR MOOSHROOM BEHAVIORS
    // ============================================================================

    /**
     * Hunger system for Mooshrooms.
     */
    private static final class MooshroomHungerHandle extends CodeBasedHandle {
        private static final String NBT_HUNGER = "hunger";
        private static final String NBT_LAST_DAMAGE = "lastDamageTick";

        private static final int MAX_HUNGER = 100;
        private static final int STARTING_HUNGER = 80;
        private static final double DECAY_RATE = 0.018;
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
     * Condition system for Mooshrooms.
     */
    private static final class MooshroomConditionHandle extends CodeBasedHandle {
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
     * Energy system for Mooshrooms.
     */
    private static final class MooshroomEnergyHandle extends CodeBasedHandle {
        private static final String NBT_ENERGY = "energy";

        private static final int MAX_VALUE = 100;
        private static final int RECOVERY_RATE = 0;
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
     * Age system for Mooshrooms.
     */
    private static final class MooshroomAgeHandle extends CodeBasedHandle {
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

            if (mob instanceof MushroomCow mooshroom) {
                boolean currentlyBaby = mooshroom.isBaby();
                if (currentlyBaby != isBaby) {
                    if (isBaby) {
                        mooshroom.setBaby(true);
                    } else {
                        mooshroom.setAge(0);
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
     * Social system for Mooshrooms.
     */
    private static final class MooshroomSocialHandle extends CodeBasedHandle {
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
            return mob.level().getEntitiesOfClass(mob.getClass(), mob.getBoundingBox().inflate(12.0))
                    .size() > 1;
        }
    }

    /**
     * Movement system for Mooshrooms.
     */
    private static final class MooshroomMovementHandle extends CodeBasedHandle {
        private static final double WALK_SPEED = 0.25;
        private static final double RUN_SPEED = 0.35;
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
     * Diet system for Mooshrooms.
     */
    private static final class MooshroomDietHandle extends CodeBasedHandle {
        @Override
        public String id() {
            return "diet";
        }

        @Override
        public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile,
                                      net.minecraft.world.item.ItemStack stack, boolean original) {
            // Mooshrooms eat wheat (player breeding)
            // They also graze on mycelium and mushrooms (handled separately)
            return stack.is(net.minecraft.world.item.Items.WHEAT);
        }
    }

    /**
     * Predation system for Mooshrooms.
     */
    private static final class MooshroomPredationHandle extends CodeBasedHandle {
        private static final int DETECTION_RANGE = 24;
        private static final double FLEE_SPEED_MULTIPLIER = 1.3;
        private static final int FLEE_DISTANCE = 32;

        @Override
        public String id() {
            return "predation";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Flee from players within detection range
            var players = mob.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                    mob.getBoundingBox().inflate(DETECTION_RANGE));

            if (!players.isEmpty()) {
                EntityState state = component.state();
                state.setIsFleeing(true);

                var nearestPlayer = players.get(0);
                if (mob.distanceTo(nearestPlayer) < FLEE_DISTANCE) {
                    var away = mob.position().subtract(nearestPlayer.position()).normalize();
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
     * Temporal behavior for Mooshrooms.
     */
    private static final class MooshroomTemporalHandle extends CodeBasedHandle {
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

    /**
     * Breeding system for Mooshrooms.
     */
    private static final class MooshroomBreedingHandle extends CodeBasedHandle {
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
            // Breeding goal is registered in registerMooshroomGoals()
        }
    }
}
